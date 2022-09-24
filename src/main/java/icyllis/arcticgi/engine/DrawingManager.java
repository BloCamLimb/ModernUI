/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.engine;

import icyllis.arcticgi.core.SharedPtr;
import icyllis.arcticgi.engine.ops.OpsTask;

import java.util.*;

public class DrawingManager {

    private final RecordingContext mContext;

    @SharedPtr
    private final List<RenderTask> mDAG = new ArrayList<>();
    private final Map<SurfaceProxy, RenderTask> mLastRenderTasks = new IdentityHashMap<>();

    public DrawingManager(RecordingContext context) {
        mContext = context;
    }

    void destroy() {
        closeTasks();
        clearTasks();
    }

    public boolean flush() {
        closeTasks();
        TopologicalSort.topologicalSort(mDAG, RenderTask.SORT_ACCESSOR);
        clearTasks();
        return false;
    }

    public void closeTasks() {
        for (var task : mDAG) {
            task.makeClosed(mContext);
        }
    }

    public void clearTasks() {
        for (var task : mDAG) {
            task.detach(this);
            task.unref();
        }
        mDAG.clear();
        mLastRenderTasks.clear();
    }

    public RenderTask appendTask(@SharedPtr RenderTask task) {
        mDAG.add(task);
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
            mLastRenderTasks.put(proxy, task);
        } else {
            mLastRenderTasks.remove(proxy);
        }
    }

    public RenderTask getLastRenderTask(SurfaceProxy proxy) {
        return mLastRenderTasks.get(proxy);
    }

    @SharedPtr
    public OpsTask newOpsTask(SurfaceProxyView writeView) {

        OpsTask opsTask = new OpsTask(this, writeView);

        appendTask(opsTask);

        return opsTask;
    }
}
