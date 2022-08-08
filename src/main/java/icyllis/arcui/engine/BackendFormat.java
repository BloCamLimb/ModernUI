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

package icyllis.arcui.engine;

import icyllis.arcui.core.Image;
import icyllis.arcui.opengl.GLBackendFormat;
import icyllis.arcui.opengl.GLTypes;
import icyllis.arcui.vulkan.VkBackendFormat;
import icyllis.arcui.vulkan.VkCore;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Describes the backend texture format, immutable.
 */
@Immutable
public abstract class BackendFormat {

    private static final Int2ObjectOpenHashMap<GLBackendFormat> sGLBackendFormats =
            new Int2ObjectOpenHashMap<>(25, 0.8f);
    private static final Int2ObjectOpenHashMap<VkBackendFormat> sVkBackendFormats =
            new Int2ObjectOpenHashMap<>(25, 0.8f);

    protected BackendFormat() {
    }

    @Nonnull
    public static GLBackendFormat makeGL(@NativeType("GLenum") int format, int textureType) {
        // if this failed, use long key
        assert (format < (1 << 30)) && (textureType >= 0 && textureType <= 3);
        int key = (format) | (textureType << 30);
        // harmless race
        GLBackendFormat backendFormat = sGLBackendFormats.get(key);
        if (backendFormat != null) {
            return backendFormat;
        }
        backendFormat = new GLBackendFormat(format, textureType);
        // cache only known formats
        if (backendFormat.getBytesPerBlock() != 0) {
            sGLBackendFormats.put(key, backendFormat);
        }
        return backendFormat;
    }

    @Nonnull
    public static VkBackendFormat makeVk(@NativeType("VkFormat") int format, boolean isExternal) {
        // if this failed, use long key
        assert (format >= 0);
        int key = (format) | (isExternal ? Integer.MIN_VALUE : 0);
        // harmless race
        VkBackendFormat backendFormat = sVkBackendFormats.get(key);
        if (backendFormat != null) {
            return backendFormat;
        }
        backendFormat = new VkBackendFormat(format, isExternal);
        // cache only known formats
        if (backendFormat.getBytesPerBlock() != 0) {
            sVkBackendFormats.put(key, backendFormat);
        }
        return backendFormat;
    }

    /**
     * @return see Types
     */
    public abstract int getBackend();

    /**
     * @return see Types
     */
    public abstract int getTextureType();

    /**
     * Gets the channels present in the format as a bitfield of ColorChannelFlag values.
     * Luminance channels (compatibility mode) are reported as GRAY_CHANNEL_FLAG.
     */
    public abstract int getChannelMask();

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

    // Utility methods

    public abstract boolean isSRGB();

    /**
     * @see Image#COMPRESSION_NONE
     */
    public abstract int getCompressionType();

    public final boolean isCompressed() {
        return getCompressionType() != Image.COMPRESSION_NONE;
    }

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
