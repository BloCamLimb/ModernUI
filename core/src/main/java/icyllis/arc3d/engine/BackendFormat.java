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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.ImageInfo;
import icyllis.modernui.graphics.*;
import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Describes the backend texture format, immutable.
 */
@Immutable
public abstract class BackendFormat {

    /**
     * @see Server.BackendApi#kOpenGL
     * @see Server.BackendApi#kVulkan
     * @see Server.BackendApi#kMock
     */
    public abstract int getBackend();

    /**
     * If the backend API is OpenGL this gets the format as a GLenum.
     */
    @NativeType("GLenum")
    public int getGLFormat() {
        throw new IllegalStateException();
    }

    /**
     * If the backend API is Vulkan this gets the format as a VkFormat.
     */
    @NativeType("VkFormat")
    public int getVkFormat() {
        throw new IllegalStateException();
    }

    /**
     * Gets the channels present in the format as a bitfield of ColorChannelFlag values.
     *
     * @see Color#COLOR_CHANNEL_FLAGS_RGBA
     */
    public abstract int getChannelFlags();

    public abstract boolean isSRGB();

    /**
     * Hints that a texture comes from external resources, and our engine cannot create such a texture
     * with this format. It will be read-only (can be read/sampled from but cannot be written/rendered to).
     */
    public abstract boolean isExternal();

    /**
     * @see ImageInfo#COMPRESSION_NONE
     */
    public abstract int getCompressionType();

    public final boolean isCompressed() {
        return getCompressionType() != ImageInfo.COMPRESSION_NONE;
    }

    /**
     * If possible, copies the BackendFormat and forces the isExternal to false.
     */
    @Nonnull
    public abstract BackendFormat makeInternal();

    /**
     * @return if compressed, bytes per block, otherwise bytes per pixel
     */
    public abstract int getBytesPerBlock();

    public abstract int getStencilBits();

    /**
     * @return a key that is unique in the backend
     */
    public abstract int getFormatKey();
}
