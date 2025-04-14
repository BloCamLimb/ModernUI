/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.CommandBuffer;
import icyllis.arc3d.engine.ImmediateContext;
import icyllis.arc3d.engine.Resource;
import icyllis.arc3d.engine.Task;
import icyllis.arc3d.granite.task.TaskList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * The task of rendering a frame, created by {@link RecordingContext},
 * will replay on {@link ImmediateContext}.
 */
public final class Recording implements Task, AutoCloseable {

    private final TaskList mRootTaskList;
    private final ObjectArrayList<@SharedPtr Resource> mExtraResourceRefs;

    public Recording(TaskList rootTaskList,
                     ObjectArrayList<@SharedPtr Resource> extraResourceRefs) {
        mRootTaskList = rootTaskList;
        mExtraResourceRefs = extraResourceRefs;
    }

    @Override
    public int execute(ImmediateContext context,
                       CommandBuffer commandBuffer) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < mExtraResourceRefs.size(); i++) {
            commandBuffer.trackResource(RefCnt.create(mExtraResourceRefs.get(i)));
        }
        return mRootTaskList.execute(context, commandBuffer);
    }

    @Override
    public void close() {
        mRootTaskList.close();
        mExtraResourceRefs.forEach(Resource::unref);
        mExtraResourceRefs.clear();
    }

    @Override
    public String toString() {
        return "RootTask{" +
                "mRootTaskList=" + mRootTaskList +
                ", mExtraResourceRefs=" + mExtraResourceRefs +
                '}';
    }
}
