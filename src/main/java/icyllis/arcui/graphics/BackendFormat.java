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

package icyllis.arcui.graphics;

import icyllis.arcui.opengl.GLTypes;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.NativeType;
import org.lwjgl.vulkan.VK11;

import javax.annotation.Nonnull;

/**
 * Describes the backend texture format, immutable.
 */
public abstract class BackendFormat {

    protected BackendFormat() {
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
     * If the backend API is Open GL this gets the format as a GLEnum. Otherwise, returns
     * {@link GL45C#GL_NONE}.
     */
    @NativeType("GLenum")
    public int getGLFormatEnum() {
        return GL45C.GL_NONE;
    }

    /**
     * If the backend API is Vulkan this gets the format as a VkFormat. Otherwise, returns
     * {@link VK11#VK_FORMAT_UNDEFINED}.
     */
    @NativeType("VkFormat")
    public int getVkFormat() {
        return VK11.VK_FORMAT_UNDEFINED;
    }

    /**
     * If possible, copies the BackendFormat and forces the texture type to be Texture2D.
     */
    @Nonnull
    public abstract BackendFormat makeTexture2D();
}
