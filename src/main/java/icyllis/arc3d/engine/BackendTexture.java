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

import icyllis.arc3d.opengl.GLDevice;
import icyllis.arc3d.opengl.GLImageInfo;
import icyllis.arc3d.vulkan.VulkanImageInfo;

import javax.annotation.Nonnull;

/**
 * Descriptor of 3D API texture.
 * <p>
 * A BackendTexture instance is initialized once, and may be shared.
 */
public abstract class BackendTexture {

    protected final int mWidth;
    protected final int mHeight;

    protected BackendTexture(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    /**
     * @return see {@link Engine.BackendApi}
     */
    public abstract int getBackend();

    /**
     * @return width in texels
     */
    public final int getWidth() {
        return mWidth;
    }

    /**
     * @return height in texels
     */
    public final int getHeight() {
        return mHeight;
    }

    /**
     * @return see {@link Engine.TextureType}
     */
    public abstract int getTextureType();

    /**
     * @return external texture
     */
    public abstract boolean isExternal();

    /**
     * @return whether the texture has mip levels allocated or not
     */
    public abstract boolean isMipmapped();

    /**
     * If the backend API is OpenGL, copies a snapshot of the GLTextureInfo struct into the passed
     * in pointer and returns true. Otherwise, returns false if the backend API is not OpenGL.
     */
    public boolean getGLImageInfo(GLImageInfo info) {
        return false;
    }

    /**
     * Call this to indicate that the texture parameters have been modified in the GL context
     * externally to Context.
     * <p>
     * Tells client that these parameters of the texture are changed out of client control
     * (for example, you called glTexParameteri without using {@link GLDevice}).
     * The local states will be forced to reset to a known state when next use.
     */
    public void glTextureParametersModified() {
    }

    /**
     * If the backend API is Vulkan, copies a snapshot of the VkImageInfo struct into the passed
     * in pointer and returns true. This snapshot will set the mImageLayout to the current layout
     * state. Otherwise, returns false if the backend API is not Vulkan.
     */
    public boolean getVulkanImageInfo(VulkanImageInfo info) {
        return false;
    }

    /**
     * Anytime the client changes the VkImageLayout of the VkImage captured by this
     * BackendTexture, they must call this function to notify pipeline of the changed layout.
     */
    public void setVkImageLayout(int layout) {
    }

    /**
     * Anytime the client changes the QueueFamilyIndex of the VkImage captured by this
     * BackendTexture, they must call this function to notify pipeline of the changed layout.
     */
    public void setVkQueueFamilyIndex(int queueFamilyIndex) {
    }

    /**
     * Get the BackendFormat for this texture.
     */
    @Nonnull
    public abstract BackendFormat getBackendFormat();

    /**
     * Returns true if we are working with protected content.
     */
    public abstract boolean isProtected();

    /**
     * Returns true if both textures are valid and refer to the same API texture.
     */
    public abstract boolean isSameTexture(BackendTexture texture);
}
