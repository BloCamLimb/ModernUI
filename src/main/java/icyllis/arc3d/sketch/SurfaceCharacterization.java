/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.sketch;

import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.trash.SharedContext;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import static icyllis.arc3d.engine.Engine.BackendApi;

/**
 * A surface characterization contains all the information Engine requires to make its internal
 * rendering decisions. When passed into a {@link DisplayListRecorder} it will copy the
 * data and pass it on to the {@link DisplayList} if/when it is created. Note that both of
 * those objects (the Recorder and the DisplayList) will take a ref on the
 * {@link SharedContext} object.
 */
public final class SurfaceCharacterization {

    private final SharedContext mContextInfo;
    private final long mCacheMaxResourceBytes;

    private final ImageInfo mImageInfo;
    private final BackendFormat mBackendFormat;
    private final int mOrigin;
    private final int mSampleCount;
    private final boolean mTexturable;
    private final boolean mMipmapped;
    private final boolean mGLWrapDefaultFramebuffer;
    private final boolean mVkSupportInputAttachment;
    private final boolean mVkSecondaryCommandBuffer;
    private final boolean mIsProtected;

    /**
     * Create via {@link SharedContext#createCharacterization}.
     */
    @ApiStatus.Internal
    public SurfaceCharacterization(SharedContext contextInfo,
                                   long cacheMaxResourceBytes,
                                   ImageInfo imageInfo,
                                   BackendFormat backendFormat,
                                   int origin,
                                   int sampleCount,
                                   boolean texturable,
                                   boolean mipmapped,
                                   boolean glWrapDefaultFramebuffer,
                                   boolean vkSupportInputAttachment,
                                   boolean vkSecondaryCommandBuffer,
                                   boolean isProtected) {
        mContextInfo = contextInfo;
        mCacheMaxResourceBytes = cacheMaxResourceBytes;
        mImageInfo = imageInfo;
        mBackendFormat = backendFormat;
        mOrigin = origin;
        mSampleCount = sampleCount;
        mTexturable = texturable;
        mMipmapped = mipmapped;
        mGLWrapDefaultFramebuffer = glWrapDefaultFramebuffer;
        mVkSupportInputAttachment = vkSupportInputAttachment;
        mVkSecondaryCommandBuffer = vkSecondaryCommandBuffer;
        mIsProtected = isProtected;
        assert validate();
    }

    private boolean validate() {
        final Caps caps = mContextInfo.getCaps();

        int ct = getColorType();
        assert mSampleCount > 0 && caps.isFormatRenderable(ct, mBackendFormat, mSampleCount);

        assert caps.isFormatCompatible(ct, mBackendFormat);

        assert !mMipmapped || mTexturable;
        assert !mTexturable || !mGLWrapDefaultFramebuffer;
        int backend = mBackendFormat.getBackend();
        assert !mGLWrapDefaultFramebuffer || backend == BackendApi.kOpenGL;
        assert (!mVkSecondaryCommandBuffer && !mVkSupportInputAttachment) || backend == BackendApi.kVulkan;
        assert !mVkSecondaryCommandBuffer || !mVkSupportInputAttachment;
        assert !mTexturable || !mVkSecondaryCommandBuffer;

        return true;
    }

    /**
     * Return a new surface characterization with the only difference being a different width
     * and height
     */
    @Nullable
    public SurfaceCharacterization createResized(int width, int height) {
        final Caps caps = mContextInfo.getCaps();
        if (caps == null) {
            return null;
        }

        if (width <= 0 || height <= 0 || width > caps.maxRenderTargetSize() ||
                height > caps.maxRenderTargetSize()) {
            return null;
        }

        return new SurfaceCharacterization(mContextInfo, mCacheMaxResourceBytes,
                mImageInfo.makeWH(width, height), mBackendFormat,
                mOrigin, mSampleCount, mTexturable, mMipmapped,
                mGLWrapDefaultFramebuffer, mVkSupportInputAttachment,
                mVkSecondaryCommandBuffer, mIsProtected);
    }

    /**
     * Return a new surface characterization with the backend format replaced. A colorType
     * must also be supplied to indicate the interpretation of the new format.
     */
    @Nullable
    public SurfaceCharacterization createBackendFormat(int colorType, BackendFormat backendFormat) {
        if (backendFormat == null) {
            return null;
        }

        return new SurfaceCharacterization(mContextInfo, mCacheMaxResourceBytes,
                mImageInfo.makeColorType(colorType), backendFormat,
                mOrigin, mSampleCount, mTexturable, mMipmapped,
                mGLWrapDefaultFramebuffer, mVkSupportInputAttachment,
                mVkSecondaryCommandBuffer, mIsProtected);
    }

    /**
     * Return a new surface characterization with just a different use of FBO 0 (in GL).
     */
    @Nullable
    public SurfaceCharacterization createDefaultFramebuffer(boolean useDefaultFramebuffer) {
        // We can't create an FBO0 characterization that is texturable or has any non-gl specific flags
        if (mTexturable || mVkSupportInputAttachment || mVkSecondaryCommandBuffer) {
            return null;
        }

        return new SurfaceCharacterization(mContextInfo, mCacheMaxResourceBytes,
                mImageInfo, mBackendFormat,
                mOrigin, mSampleCount, false, mMipmapped,
                useDefaultFramebuffer, false,
                false, mIsProtected);
    }

    @ApiStatus.Internal
    public SharedContext getContextInfo() {
        return mContextInfo;
    }

    public long getCacheMaxResourceBytes() {
        return mCacheMaxResourceBytes;
    }

    public ImageInfo getImageInfo() {
        return mImageInfo;
    }

    public BackendFormat getBackendFormat() {
        return mBackendFormat;
    }

    public int getOrigin() {
        return mOrigin;
    }

    public int getWidth() {
        return mImageInfo.width();
    }

    public int getHeight() {
        return mImageInfo.height();
    }

    public int getColorType() {
        return mImageInfo.colorType();
    }

    public int getSampleCount() {
        return mSampleCount;
    }

    public boolean isTexturable() {
        return mTexturable;
    }

    public boolean isMipmapped() {
        return mMipmapped;
    }

    public boolean glWrapDefaultFramebuffer() {
        return mGLWrapDefaultFramebuffer;
    }

    public boolean vkSupportInputAttachment() {
        return mVkSupportInputAttachment;
    }

    public boolean vkSecondaryCommandBuffer() {
        return mVkSecondaryCommandBuffer;
    }

    public boolean isProtected() {
        return mIsProtected;
    }

    /**
     * Is the provided backend texture compatible with this surface characterization?
     */
    public boolean isCompatible(BackendImage texture) {
        if (mGLWrapDefaultFramebuffer) {
            // It is a backend texture so can't be wrapping default framebuffer
            return false;
        }

        if (mVkSecondaryCommandBuffer) {
            return false;
        }

        if (mIsProtected != texture.isProtected()) {
            return false;
        }

        if (mMipmapped && !texture.isMipmapped()) {
            // backend texture is allowed to have mipmaps even if the characterization doesn't require
            // them.
            return false;
        }

        if (getWidth() != texture.getWidth() || getHeight() != texture.getHeight()) {
            return false;
        }

        if (!mBackendFormat.equals(texture.getBackendFormat())) {
            return false;
        }

        //TODO
        /*if (mVkSupportInputAttachment) {
            if (!(texture instanceof VulkanBackendImage)) {
                return false;
            }
            VulkanImageInfo vkInfo = new VulkanImageInfo();
            ((VulkanBackendImage) texture).getVulkanImageInfo(vkInfo);
            return (vkInfo.mImageUsageFlags & VKCore.VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT) != 0;
        } else {
            return true;
        }*/
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SurfaceCharacterization that = (SurfaceCharacterization) o;
        if (mCacheMaxResourceBytes != that.mCacheMaxResourceBytes) return false;
        if (mOrigin != that.mOrigin) return false;
        if (mSampleCount != that.mSampleCount) return false;
        if (mTexturable != that.mTexturable) return false;
        if (mMipmapped != that.mMipmapped) return false;
        if (mGLWrapDefaultFramebuffer != that.mGLWrapDefaultFramebuffer) return false;
        if (mVkSupportInputAttachment != that.mVkSupportInputAttachment) return false;
        if (mVkSecondaryCommandBuffer != that.mVkSecondaryCommandBuffer) return false;
        if (mIsProtected != that.mIsProtected) return false;
        if (!mContextInfo.equals(that.mContextInfo)) return false;
        if (!mImageInfo.equals(that.mImageInfo)) return false;
        return mBackendFormat.equals(that.mBackendFormat);
    }

    @Override
    public int hashCode() {
        int result = mContextInfo.hashCode();
        result = 31 * result + (int) (mCacheMaxResourceBytes ^ (mCacheMaxResourceBytes >>> 32));
        result = 31 * result + mImageInfo.hashCode();
        result = 31 * result + mBackendFormat.hashCode();
        result = 31 * result + mOrigin;
        result = 31 * result + mSampleCount;
        result = 31 * result + (mTexturable ? 1 : 0);
        result = 31 * result + (mMipmapped ? 1 : 0);
        result = 31 * result + (mGLWrapDefaultFramebuffer ? 1 : 0);
        result = 31 * result + (mVkSupportInputAttachment ? 1 : 0);
        result = 31 * result + (mVkSecondaryCommandBuffer ? 1 : 0);
        result = 31 * result + (mIsProtected ? 1 : 0);
        return result;
    }
}
