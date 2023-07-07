/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.engine.ops.OpsTask;
import icyllis.modernui.graphics.RefCnt;
import icyllis.arc3d.SharedPtr;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.ArrayList;

public class DrawingManager {

    private final RecordingContext mContext;
    private final DirectContext mDirect;

    @SharedPtr
    private final ArrayList<RenderTask> mDAG = new ArrayList<>();

    private final Reference2ObjectOpenHashMap<Object, RenderTask> mLastRenderTasks =
            new Reference2ObjectOpenHashMap<>();
    private OpsTask mActiveOpsTask = null;

    private final OpFlushState mFlushState;
    private final ResourceAllocator mResourceAllocator;

    private boolean mFlushing;

    public DrawingManager(RecordingContext context) {
        mContext = context;
        if (context instanceof DirectContext direct) {
            mDirect = direct;
            mFlushState = new OpFlushState(direct.getEngine(), direct.getResourceProvider());
            mResourceAllocator = new ResourceAllocator(direct);
        } else {
            // deferred
            mDirect = null;
            mFlushState = null;
            mResourceAllocator = null;
        }
    }

    void destroy() {
        closeTasks();
        clearTasks();
    }

    RecordingContext getContext() {
        return mContext;
    }

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
        final Engine engine = context.getEngine();
        assert (engine != null);

        closeTasks();
        mActiveOpsTask = null;

        TopologicalSort.topologicalSort(mDAG, RenderTask.SORT_ADAPTER);

        for (RenderTask task : mDAG) {
            task.gatherProxyIntervals(mResourceAllocator);
        }
        mResourceAllocator.simulate();
        mResourceAllocator.allocate();

        boolean purge = false;
        if (!mResourceAllocator.isInstantiationFailed()) {
            purge = executeRenderTasks();
        }

        mResourceAllocator.reset();

        clearTasks();

        if (purge) {
            context.getResourceCache().purge();
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

    public void setLastRenderTask(SurfaceProxy proxy, RenderTask task) {
        if (task != null) {
            mLastRenderTasks.put(proxy.getUniqueID(), task);
        } else {
            mLastRenderTasks.remove(proxy);
        }
    }

    public RenderTask getLastRenderTask(SurfaceProxy proxy) {
        return mLastRenderTasks.get(proxy.getUniqueID());
    }

    @SharedPtr
    public OpsTask newOpsTask(SurfaceProxyView writeView) {

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
