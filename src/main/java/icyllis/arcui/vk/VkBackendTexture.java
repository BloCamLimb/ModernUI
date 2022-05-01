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

package icyllis.arcui.vk;

import icyllis.arcui.hgi.*;

import javax.annotation.Nonnull;

import static org.lwjgl.vulkan.EXTImageDrmFormatModifier.VK_IMAGE_TILING_DRM_FORMAT_MODIFIER_EXT;
import static org.lwjgl.vulkan.VK11.*;

public final class VkBackendTexture extends BackendTexture {

    // We don't know if the backend texture is made renderable or not, so we default the usage flags
    // to include color attachment as well.
    private static final int DEFAULT_USAGE_FLAGS = VK_IMAGE_USAGE_TRANSFER_DST_BIT |
            VK_IMAGE_USAGE_TRANSFER_SRC_BIT |
            VK_IMAGE_USAGE_SAMPLED_BIT |
            VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;

    private final VkImageInfo mInfo;
    final VkSharedImageInfo mState;

    private VkBackendFormat mBackendFormat;

    // The VkImageInfo can NOT be modified anymore.
    public VkBackendTexture(int width, int height, VkImageInfo info) {
        this(width, height, info, new VkSharedImageInfo(info.mImageLayout, info.mCurrentQueueFamily));
    }

    VkBackendTexture(int width, int height, VkImageInfo info, VkSharedImageInfo state) {
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
    public int getTextureType() {
        if (mInfo.mImageTiling == VK_IMAGE_TILING_DRM_FORMAT_MODIFIER_EXT) {
            return Types.TEXTURE_TYPE_EXTERNAL;
        }
        return Types.TEXTURE_TYPE_2D;
    }

    @Override
    public boolean isMipmapped() {
        return mInfo.mLevelCount > 1;
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
            boolean useDRMModifier = mInfo.mImageTiling == VK_IMAGE_TILING_DRM_FORMAT_MODIFIER_EXT;
            mBackendFormat = new VkBackendFormat(mInfo.mFormat, useDRMModifier);
        }
        return mBackendFormat;
    }

    @Override
    public boolean isProtected() {
        return mInfo.mProtected;
    }

    @Override
    public boolean isSameTexture(BackendTexture texture) {
        if (texture instanceof VkBackendTexture t) {
            return mInfo.mImage == t.mInfo.mImage;
        }
        return false;
    }
}
