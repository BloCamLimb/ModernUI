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

package icyllis.arc3d.granite.task;

import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.RecordingContext;

public abstract class Task extends RefCnt implements icyllis.arc3d.engine.Task {

    /**
     * Prepare resources on {@link RecordingContext} using its {@link ResourceProvider}.
     */
    public abstract int prepare(RecordingContext context);

    /**
     * Add commands to command buffer on {@link ImmediateContext}.
     * The {@link ResourceProvider} of {@link ImmediateContext} can also be used to create resources.
     */
    public abstract int execute(ImmediateContext context, CommandBuffer commandBuffer);

    /**
     * Cleanup resources.
     */
    @Override
    protected void deallocate() {
    }
}
