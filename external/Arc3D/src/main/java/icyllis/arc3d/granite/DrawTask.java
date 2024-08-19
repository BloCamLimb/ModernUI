/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.task.Task;
import icyllis.arc3d.engine.task.TaskList;

public final class DrawTask extends Task {

    private ImageViewProxy mTargetView;
    private TaskList mChildTasks;

    public DrawTask(ImageViewProxy targetView, TaskList childTasks) {
        mTargetView = targetView;
        mChildTasks = childTasks;
    }

    @Override
    protected void deallocate() {
        super.deallocate();
        mTargetView = RefCnt.move(mTargetView);
        mChildTasks.close();
        mChildTasks = null;
    }

    @Override
    public int prepare(RecordingContext context) {
        return mChildTasks.prepare(context);
    }

    @Override
    public int execute(ImmediateContext context, CommandBuffer commandBuffer) {
        assert mTargetView.isInstantiated();
        return mChildTasks.execute(context, commandBuffer);
    }
}
