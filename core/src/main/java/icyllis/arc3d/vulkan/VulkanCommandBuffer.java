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

package icyllis.arc3d.vulkan;

import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;

import static icyllis.arc3d.vulkan.VKCore.vkCmdDraw;

public abstract class VulkanCommandBuffer extends VkCommandBuffer {

    protected boolean mIsRecording = false;

    public VulkanCommandBuffer(VkDevice device, long handle) {
        super(handle, device);
    }

    public void draw(int vertexCount,
                     int instanceCount,
                     int firstVertex,
                     int firstInstance) {
        vkCmdDraw(this,
                vertexCount,
                instanceCount,
                firstVertex,
                firstInstance);
    }

    public boolean isRecording() {
        return mIsRecording;
    }
}
