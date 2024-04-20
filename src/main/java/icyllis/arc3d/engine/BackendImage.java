/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import javax.annotation.Nonnull;

/**
 * Descriptor of 3D API image/texture that can be shared between
 * recording contexts, OpenGL shared contexts and cross-API usage,
 * including its dimension, image view type, and memory allocation.
 * <p>
 * A BackendImage instance is initialized once, and may be shared.
 */
public abstract class BackendImage {

    protected final ImageInfo mInfo;
    protected final ImageMutableState mMutableState;

    protected BackendImage(ImageInfo info,
                           ImageMutableState mutableState) {
        mInfo = info;
        mMutableState = mutableState;
    }

    /**
     * @return see {@link Engine.BackendApi}
     */
    public abstract int getBackend();

    /**
     * @return width in pixels
     */
    public final int getWidth() {
        return mInfo.mWidth;
    }

    /**
     * @return height in pixels
     */
    public final int getHeight() {
        return mInfo.mHeight;
    }

    public final int getDepth() {
        return mInfo.mDepth;
    }

    public final int getArraySize() {
        return mInfo.mArraySize;
    }

    /**
     * @return see {@link Engine.ImageType}
     */
    public final byte getImageType() {
        return mInfo.getImageType();
    }

    /**
     * @return external texture
     */
    public abstract boolean isExternal();

    /**
     * @return whether the image has mip levels allocated or not
     */
    public final boolean isMipmapped() {
        return mInfo.isMipmapped();
    }

    public final int getMipLevelCount() {
        return mInfo.mMipLevelCount;
    }

    /**
     * Get the backend info for this image/texture.
     */
    @Nonnull
    public final ImageInfo getInfo() {
        return mInfo;
    }

    /**
     * Get the backend mutable state for this image/texture.
     */
    @Nonnull
    public final ImageMutableState getMutableState() {
        return mMutableState;
    }

    /**
     * Call this to indicate that the texture parameters have been modified in the GL context
     * externally to Context.
     * <p>
     * Tells client that these parameters of the texture are changed out of client control
     * (for example, you called glTexParameteri without using {@link icyllis.arc3d.opengl.GLDevice}).
     * The local states will be forced to reset to a known state when next use.
     */
    public void glTextureParametersModified() {
    }

    /**
     * Anytime the client changes the VkImageLayout of the VkImage captured by this
     * {@link BackendImage}, they must call this function to notify pipeline of the changed layout.
     */
    public void setVkImageLayout(int layout) {
    }

    /**
     * Anytime the client changes the QueueFamilyIndex of the VkImage captured by this
     * {@link BackendImage}, they must call this function to notify pipeline of the changed layout.
     */
    public void setVkQueueFamilyIndex(int queueFamilyIndex) {
    }

    /**
     * Get the BackendFormat for this image/texture.
     */
    @Nonnull
    public abstract BackendFormat getBackendFormat();

    /**
     * Returns true if we are working with protected content.
     */
    public abstract boolean isProtected();

    /**
     * Returns true if both images are valid and refer to the same API image handle.
     */
    public abstract boolean isSameImage(BackendImage image);
}
