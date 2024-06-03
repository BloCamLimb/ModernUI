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

package icyllis.arc3d.opengl;

import icyllis.arc3d.engine.*;
import org.jetbrains.annotations.ApiStatus;

public final class GLQueueManager extends QueueManager {

    private final GLDevice mDevice;

    /**
     * Use {@link GLUtil} to create context.
     */
    @ApiStatus.Internal
    public GLQueueManager(GLDevice device) {
        super(device);
        mDevice = device;
    }

    @Override
    protected GLCommandBuffer createNewCommandBuffer(ResourceProvider resourceProvider) {
        var glResourceProvider = (GLResourceProvider) resourceProvider;
        return new GLCommandBuffer(mDevice, glResourceProvider);
    }

    @Override
    protected boolean onSubmit(CommandBuffer commandBuffer) {
        var glCommandBuffer = (GLCommandBuffer) commandBuffer;
        glCommandBuffer.submit();
        return true;
    }
}
