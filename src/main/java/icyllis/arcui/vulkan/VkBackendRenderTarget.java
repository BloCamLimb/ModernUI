/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.vulkan;

import icyllis.arcui.graphics.*;

import javax.annotation.Nonnull;

import static org.lwjgl.vulkan.VK11.*;

public final class VkBackendRenderTarget extends BackendRenderTarget {

    private static final int DEFAULT_USAGE_FLAGS = VK_IMAGE_USAGE_TRANSFER_DST_BIT |
            VK_IMAGE_USAGE_TRANSFER_SRC_BIT |
            VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;

    private final VkImageInfo mInfo;
    final VkSharedImageInfo mState;

    // lazy
    private VkBackendFormat mBackendFormat;

    // The VkImageInfo can NOT be modified anymore.
    public VkBackendRenderTarget(int width, int height, VkImageInfo info) {
        this(width, height, info, new VkSharedImageInfo(info.mImageLayout, info.mCurrentQueueFamily));
    }

    VkBackendRenderTarget(int width, int height, VkImageInfo info, VkSharedImageInfo state) {
        super(width, height);
        if (info.mImageUsageFlags == 0) {
            info.mImageUsageFlags = DEFAULT_USAGE_FLAGS;
        }
        mInfo = info;
        mState = state;
    }

    @Override
    public int getBackend() {
        return Types.VULKAN;
    }

    @Override
    public int getSampleCount() {
        return mInfo.mSampleCount;
    }

    @Override
    public int getStencilBits() {
        // We always create stencil buffers internally for vulkan
        return 0;
    }

    @Override
    public boolean getVkImageInfo(VkImageInfo info) {
        info.set(mInfo);
        info.mImageLayout = mState.getImageLayout();
        info.mCurrentQueueFamily = mState.getQueueFamilyIndex();
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

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        if (mBackendFormat == null) {
            mBackendFormat = new VkBackendFormat(mInfo.mFormat);
        }
        return mBackendFormat;
    }

    @Override
    public boolean isProtected() {
        return mInfo.mProtected;
    }
}
