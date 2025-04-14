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

package icyllis.arc3d.vulkan;

import icyllis.arc3d.engine.BackendFormat;
import icyllis.arc3d.engine.BackendRenderTarget;
import org.jspecify.annotations.NonNull;

import static icyllis.arc3d.engine.Engine.BackendApi;
import static org.lwjgl.vulkan.VK11.*;

@Deprecated
public final class VkBackendRenderTarget extends BackendRenderTarget {

    private static final int DEFAULT_USAGE_FLAGS = VK_IMAGE_USAGE_TRANSFER_DST_BIT |
            VK_IMAGE_USAGE_TRANSFER_SRC_BIT |
            VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;

    private final VulkanImageDesc mInfo;
    final VulkanImageMutableState mState;

    private VkBackendFormat mBackendFormat;

    // The VkImageInfo can NOT be modified anymore.
    public VkBackendRenderTarget(int width, int height, VulkanImageDesc info) {
        this(width, height, info, null);
    }

    VkBackendRenderTarget(int width, int height, VulkanImageDesc info, VulkanImageMutableState state) {
        super(width, height);

        mInfo = info;
        mState = state;
    }

    @Override
    public int getBackend() {
        return BackendApi.kVulkan;
    }

    @Override
    public int getSampleCount() {
        return 1;
    }

    @Override
    public int getDepthBits() {
        return 0;
    }

    @Override
    public int getStencilBits() {
        // We always create stencil buffers internally for vulkan
        return 0;
    }

    @Override
    public boolean getVkImageInfo(VulkanImageDesc info) {
        return true;
    }

    @Override
    public void setVkImageLayout(int layout) {
        mState.setImageLayout(layout);
    }

    @Override
    public void setVkQueueFamilyIndex(int queueFamilyIndex) {
        mState.setQueueFamilyIndex(queueFamilyIndex);
    }

    @NonNull
    @Override
    public BackendFormat getBackendFormat() {
        if (mBackendFormat == null) {
            mBackendFormat = VkBackendFormat.make(mInfo.mFormat);
        }
        return mBackendFormat;
    }

    @Override
    public boolean isProtected() {
        return mInfo.isProtected();
    }
}
