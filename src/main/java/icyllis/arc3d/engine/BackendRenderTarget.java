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

package icyllis.arc3d.engine;

import icyllis.arc3d.opengl.GLFramebufferInfo;
import icyllis.arc3d.vulkan.VulkanImageDesc;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A BackendRenderTarget instance is initialized once, and may be shared.
 */
@Deprecated()
public abstract class BackendRenderTarget {

    private final int mWidth;
    private final int mHeight;

    public BackendRenderTarget(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    /**
     * @return see Types
     */
    public abstract int getBackend();

    /**
     * @return width in pixels
     */
    public final int getWidth() {
        return mWidth;
    }

    /**
     * @return height in pixels
     */
    public final int getHeight() {
        return mHeight;
    }

    public abstract int getSampleCount();

    public abstract int getDepthBits();

    public abstract int getStencilBits();

    /**
     * If the backend API is OpenGL, copies a snapshot of the GLFramebufferInfo struct into the passed
     * in pointer and returns true. Otherwise, returns false if the backend API is not OpenGL.
     */
    public boolean getGLFramebufferInfo(GLFramebufferInfo info) {
        return false;
    }

    /**
     * If the backend API is Vulkan, copies a snapshot of the VkImageInfo struct into the passed
     * in pointer and returns true. This snapshot will set the mImageLayout to the current layout
     * state. Otherwise, returns false if the backend API is not Vulkan.
     */
    public boolean getVkImageInfo(VulkanImageDesc info) {
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
    @NonNull
    public abstract BackendFormat getBackendFormat();

    /**
     * Returns true if we are working with protected content.
     */
    public abstract boolean isProtected();
}
