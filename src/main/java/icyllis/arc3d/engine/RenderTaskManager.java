/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.trash.ops.OpsTask;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

@Deprecated
public class RenderTaskManager {

    private final RecordingContext mContext;
    private final ImmediateContext mDirect;

    @SharedPtr
    private final ArrayList<RenderTask> mDAG = new ArrayList<>();

    private final Reference2ObjectOpenHashMap<UniqueID, RenderTask> mLastRenderTasks =
            new Reference2ObjectOpenHashMap<>();
    private OpsTask mActiveOpsTask = null;

    private final OpFlushState mFlushState;
    private final SurfaceAllocator mSurfaceAllocator;

    private boolean mFlushing;

    public RenderTaskManager(RecordingContext context) {
        mContext = context;
        /*if (context instanceof ImmediateContext direct) {
            mDirect = direct;
            mFlushState = new OpFlushState(direct.getDevice(), direct.getResourceProvider());
            mSurfaceAllocator = new SurfaceAllocator(direct);
        } else {*/
            // deferred
            mDirect = null;
            mFlushState = null;
            mSurfaceAllocator = null;
        //}
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
                    info.mFinishedCallback.onFinished(false);
                }
            }
            return false;
        }
        mFlushing = true;

        final ImmediateContext context = mDirect;
        assert (context != null);
        final Device device = context.getDevice();
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
            //context.getResourceCache().cleanup();
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

    public void setLastRenderTask(@Nonnull SurfaceProxy surfaceProxy,
                                  @Nullable RenderTask task) {
        var key = surfaceProxy.getUniqueID();
        if (task != null) {
            mLastRenderTasks.put(key, task);
        } else {
            mLastRenderTasks.remove(key);
        }
    }

    // nullable
    public RenderTask getLastRenderTask(@Nonnull SurfaceProxy proxy) {
        return mLastRenderTasks.get(proxy.getUniqueID());
    }

    @SharedPtr
    public OpsTask newOpsTask(ImageProxyView writeView) {

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
