/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.engine;

import icyllis.akashigi.core.ImageInfo;
import icyllis.akashigi.opengl.GLTypes;
import icyllis.akashigi.vulkan.VkCore;
import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Describes the backend texture format, immutable.
 */
@Immutable
public abstract class BackendFormat {

    /**
     * @see Engine#OpenGL
     * @see Engine#Vulkan
     * @see Engine#Mock
     */
    public abstract int getBackend();

    /**
     * Gets the channels present in the format as a bitfield of ColorChannelFlag values.
     * Luminance channels (compatibility mode) are reported as GRAY_CHANNEL_FLAG.
     */
    public abstract int getChannelMask();

    public abstract boolean isSRGB();

    /**
     * May be memory object, imports POSIX FD or Win32 NT handle (Windows 8+, KMT is not used).
     * Currently, OpenGL texture wraps Vulkan image, Vulkan image wraps Vulkan image or Linux DRM.
     * We assume external textures are read-only and don't track their memory usage.
     */
    public abstract boolean isExternal();

    /**
     * @see ImageInfo#COMPRESSION_TYPE_NONE
     */
    public abstract int getCompressionType();

    public final boolean isCompressed() {
        return getCompressionType() != ImageInfo.COMPRESSION_TYPE_NONE;
    }

    /**
     * If the backend API is Open GL this gets the format as a GLFormat. Otherwise, returns
     * {@link GLTypes#FORMAT_UNKNOWN}.
     */
    public int getGLFormat() {
        return GLTypes.FORMAT_UNKNOWN;
    }

    /**
     * If the backend API is Open GL this gets the format as a GLEnum. Otherwise, returns 0.
     */
    @NativeType("GLenum")
    public int getGLFormatEnum() {
        return 0;
    }

    /**
     * If the backend API is Vulkan this gets the format as a VkFormat. Otherwise, returns
     * {@link VkCore#VK_FORMAT_UNDEFINED}.
     */
    @NativeType("VkFormat")
    public int getVkFormat() {
        return VkCore.VK_FORMAT_UNDEFINED;
    }

    /**
     * If possible, copies the BackendFormat and forces the texture type to be Texture2D.
     */
    @Nonnull
    public abstract BackendFormat makeTexture2D();

    /**
     * @return if compressed, bytes per block, otherwise bytes per pixel
     */
    public abstract int getBytesPerBlock();

    public abstract int getStencilBits();

    /**
     * @return a key that is unique in the backend
     */
    public abstract int getKey();
}
