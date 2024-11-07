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

import icyllis.arc3d.engine.*;
import org.lwjgl.vulkan.EXTQueueFamilyForeign;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_SRGB;

/**
 * Descriptor to create Vulkan images.
 * <p>
 * When wrapping a {@link BackendImage} or {@link BackendRenderTarget}, the {@link #mCurrentQueueFamily}
 * should either be {@link VKUtil#VK_QUEUE_FAMILY_IGNORED}, {@link VKUtil#VK_QUEUE_FAMILY_EXTERNAL},
 * or {@link EXTQueueFamilyForeign#VK_QUEUE_FAMILY_FOREIGN_EXT}. If {@link #mSharingMode} is
 * {@link VKUtil#VK_SHARING_MODE_EXCLUSIVE}, then {@link #mCurrentQueueFamily} can also be the graphics
 * queue index passed into pipeline.
 * <p>
 * When importing external memory,
 * {@link #mMemoryHandle} is POSIX file descriptor or Win32 NT handle (though <code>HANDLE</code> is defined
 * as <code>void*</code>, we can safely truncate it because Win32 handles are 32-bit significant).
 * If it is an NT handle, it must be released manually by the memory exporter (e.g. Vulkan).
 */
public final class VulkanImageDesc extends ImageDesc {

    // add Vk prefix to distinguish between this and base class
    public final /*VkImageCreateFlags*/ int mVkFlags;
    public final /*VkImageType*/ int mVkImageType;
    public final /*VkFormat*/ int mFormat;
    public final /*VkImageTiling*/ int mImageTiling;
    public final /*VkImageUsageFlags*/ int mImageUsageFlags;
    public final /*VkSharingMode*/ int mSharingMode;

    public VulkanImageDesc(int vkFlags, int vkImageType,
                           int format, int imageTiling, int imageUsageFlags, int sharingMode,
                           int imageType, int width, int height, int depth, int arraySize,
                           int mipLevelCount, int sampleCount, int flags) {
        super(imageType, width, height, depth, arraySize, mipLevelCount, sampleCount, flags);
        mVkFlags = vkFlags;
        mVkImageType = vkImageType;
        mFormat = format;
        mImageTiling = imageTiling;
        mImageUsageFlags = imageUsageFlags;
        mSharingMode = sharingMode;
    }

    @Override
    public int getBackend() {
        return Engine.BackendApi.kVulkan;
    }

    @Override
    public int getVkFormat() {
        return mFormat;
    }

    @Override
    public int getChannelFlags() {
        return VKUtil.vkFormatChannels(mFormat);
    }

    @Override
    public boolean isSRGB() {
        return mFormat == VK_FORMAT_R8G8B8A8_SRGB;
    }

    @Override
    public int getCompressionType() {
        return VKUtil.vkFormatCompressionType(mFormat);
    }

    @Override
    public int getBytesPerBlock() {
        return VKUtil.vkFormatBytesPerBlock(mFormat);
    }

    @Override
    public int getDepthBits() {
        return VKUtil.vkFormatDepthBits(mFormat);
    }

    @Override
    public int getStencilBits() {
        return VKUtil.vkFormatStencilBits(mFormat);
    }

    @Override
    public int hashCode() {
        int result = mVkFlags;
        result = 31 * result + mVkImageType;
        result = 31 * result + mFormat;
        result = 31 * result + mImageTiling;
        result = 31 * result + mImageUsageFlags;
        result = 31 * result + mSharingMode;
        result = 31 * result + mWidth;
        result = 31 * result + mHeight;
        result = 31 * result + mDepth;
        result = 31 * result + mArraySize;
        result = 31 * result + mMipLevelCount;
        result = 31 * result + mSampleCount;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof VulkanImageDesc desc) {
            return mVkFlags == desc.mVkFlags &&
                    mVkImageType == desc.mVkImageType &&
                    mFormat == desc.mFormat &&
                    mImageTiling == desc.mImageTiling &&
                    mImageUsageFlags == desc.mImageUsageFlags &&
                    mSharingMode == desc.mSharingMode &&
                    mWidth == desc.mWidth &&
                    mHeight == desc.mHeight &&
                    mDepth == desc.mDepth &&
                    mArraySize == desc.mArraySize &&
                    mMipLevelCount == desc.mMipLevelCount &&
                    mSampleCount == desc.mSampleCount;
        }
        return false;
    }

    @Override
    public String toString() {
        return '{' +
                "vkFlags=0x" + Integer.toHexString(mVkFlags) +
                ", vkImageType=" + mVkImageType +
                ", vkFormat=" + VKUtil.vkFormatName(mFormat) +
                ", imageTiling=" + mImageTiling +
                ", imageUsageFlags=0x" + Integer.toHexString(mImageUsageFlags) +
                ", sharingMode=" + mSharingMode +
                ", width=" + mWidth +
                ", height=" + mHeight +
                ", depth=" + mDepth +
                ", arraySize=" + mArraySize +
                ", mipLevelCount=" + mMipLevelCount +
                ", sampleCount=" + mSampleCount +
                '}';
    }
}
