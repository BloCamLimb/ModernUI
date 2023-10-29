/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.ops.OpsTask;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.ArrayList;

public class RenderTaskManager {

    private final RecordingContext mContext;
    private final DirectContext mDirect;

    @SharedPtr
    private final ArrayList<RenderTask> mDAG = new ArrayList<>();

    private final Reference2ObjectOpenHashMap<Object, RenderTask> mLastRenderTasks =
            new Reference2ObjectOpenHashMap<>();
    private OpsTask mActiveOpsTask = null;

    private final OpFlushState mFlushState;
    private final SurfaceAllocator mSurfaceAllocator;

    private boolean mFlushing;

    public RenderTaskManager(RecordingContext context) {
        mContext = context;
        if (context instanceof DirectContext direct) {
            mDirect = direct;
            mFlushState = new OpFlushState(direct.getDevice(), direct.getResourceProvider());
            mSurfaceAllocator = new SurfaceAllocator(direct);
        } else {
            // deferred
            mDirect = null;
            mFlushState = null;
            mSurfaceAllocator = null;
        }
    }

    void destroy() {
        closeTasks();
        clearTasks();
    }

    RecordingContext getContext() {
        return mContext;
    }

    // for test
    public OpFlushState getFlushState() {
        return mFlushState;
    }

    public boolean flush(FlushInfo info) {
        if (mFlushing || mContext.isDiscarded()) {
            if (info != null) {
                if (info.mSubmittedCallback != null) {
                    info.mSubmittedCallback.onSubmitted(false);
                }
                if (info.mFinishedCallback != null) {
                    info.mFinishedCallback.onFinished();
                }
            }
            return false;
        }
        mFlushing = true;

        final DirectContext context = mDirect;
        assert (context != null);
        final GpuDevice device = context.getDevice();
        assert (device != null);

        closeTasks();
        mActiveOpsTask = null;

        TopologicalSort.topologicalSort(mDAG, RenderTask.SORT_ACCESS);

        for (RenderTask task : mDAG) {
            task.gatherSurfaceIntervals(mSurfaceAllocator);
        }
        mSurfaceAllocator.simulate();
        mSurfaceAllocator.allocate();

        boolean cleanup = false;
        if (!mSurfaceAllocator.isInstantiationFailed()) {
            cleanup = executeRenderTasks();
        }

        mSurfaceAllocator.reset();

        clearTasks();

        if (cleanup) {
            context.getResourceCache().cleanup();
        }
        mFlushing = false;

        return true;
    }

    public void closeTasks() {
        for (RenderTask task : mDAG) {
            task.makeClosed(mContext);
        }
    }

    public void clearTasks() {
        for (RenderTask task : mDAG) {
            task.detach(this);
            task.unref();
        }
        mDAG.clear();
        mLastRenderTasks.clear();
    }

    public RenderTask appendTask(@SharedPtr RenderTask task) {
        mDAG.add(RefCnt.create(task));
        return task;
    }

    public RenderTask prependTask(@SharedPtr RenderTask task) {
        if (mDAG.isEmpty()) {
            mDAG.add(task);
        } else {
            int pos = mDAG.size() - 1;
            mDAG.add(mDAG.get(pos));
            mDAG.set(pos, task);
        }
        return task;
    }

    public void setLastRenderTask(Surface surface, RenderTask task) {
        if (task != null) {
            mLastRenderTasks.put(surface.getUniqueID(), task);
        } else {
            mLastRenderTasks.remove(surface);
        }
    }

    public RenderTask getLastRenderTask(Surface proxy) {
        return mLastRenderTasks.get(proxy.getUniqueID());
    }

    @SharedPtr
    public OpsTask newOpsTask(SurfaceView writeView) {

        OpsTask opsTask = new OpsTask(this, writeView);

        appendTask(opsTask);

        return opsTask;
    }

    private boolean executeRenderTasks() {
        boolean executed = false;

        for (RenderTask task : mDAG) {
            if (!task.isInstantiated()) {
                continue;
            }
            task.prepare(mFlushState);
        }

        for (RenderTask task : mDAG) {
            if (!task.isInstantiated()) {
                continue;
            }
            executed |= task.execute(mFlushState);
        }

        mFlushState.reset();

        return executed;
    }
}
