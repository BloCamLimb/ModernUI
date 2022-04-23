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

import static org.lwjgl.vulkan.VK11.*;

/**
 * When wrapping a BackendTexture or BackendRenderTarget, the mCurrentQueueFamily should
 * either be VK_QUEUE_FAMILY_IGNORED, VK_QUEUE_FAMILY_EXTERNAL, or VK_QUEUE_FAMILY_FOREIGN_EXT. If
 * mSharingMode is VK_SHARING_MODE_EXCLUSIVE, then mCurrentQueueFamily can also be the graphics
 * queue index passed into pipeline.
 */
public final class VkImageInfo {

    public long mImage = VK_NULL_HANDLE;
    public long mAlloc = VK_NULL_HANDLE;
    public int mImageTiling = VK_IMAGE_TILING_OPTIMAL;
    public int mImageLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    public int mFormat = VK_FORMAT_UNDEFINED;
    public int mSharingMode = VK_SHARING_MODE_EXCLUSIVE;
    public int mImageUsageFlags = 0;
    public int mSampleCount = 1;
    public int mLevelCount = 0;
    public int mCurrentQueueFamily = VK_QUEUE_FAMILY_IGNORED;
    public boolean mProtected = false;

    public void set(VkImageInfo info) {
        mImage = info.mImage;
        mAlloc = info.mAlloc;
        mImageTiling = info.mImageTiling;
        mImageLayout = info.mImageLayout;
        mFormat = info.mFormat;
        mSharingMode = info.mSharingMode;
        mImageUsageFlags = info.mImageUsageFlags;
        mSampleCount = info.mSampleCount;
        mLevelCount = info.mLevelCount;
        mCurrentQueueFamily = info.mCurrentQueueFamily;
        mProtected = info.mProtected;
    }
}
