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

import icyllis.arc3d.engine.BackendFormat;
import icyllis.arc3d.engine.Engine;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import static icyllis.arc3d.vulkan.VKCore.*;

@Immutable
public final class VkBackendFormat extends BackendFormat {

    private static final Int2ObjectMap<VkBackendFormat> FORMATS =
            new Int2ObjectOpenHashMap<>(25);

    private final int mFormat;

    /**
     * @see #make(int)
     */
    VkBackendFormat(@NativeType("VkFormat") int format) {
        mFormat = format;
    }

    //TODO cache only known formats
    @Nonnull
    public static VkBackendFormat make(@NativeType("VkFormat") int format) {
        int key = (format);
        VkBackendFormat backendFormat = FORMATS.get(key);
        if (backendFormat != null) {
            return backendFormat;
        }
        backendFormat = new VkBackendFormat(format);
        if (backendFormat.getBytesPerBlock() != 0) {
            FORMATS.put(key, backendFormat);
        }
        return backendFormat;
    }

    @Override
    public int getBackend() {
        return Engine.BackendApi.kVulkan;
    }

    @Override
    public int getChannelFlags() {
        return vkFormatChannels(mFormat);
    }

    @Override
    public int getVkFormat() {
        return mFormat;
    }

    @Override
    public boolean isSRGB() {
        return mFormat == VK_FORMAT_R8G8B8A8_SRGB;
    }

    @Override
    public int getCompressionType() {
        return vkFormatCompressionType(mFormat);
    }

    @Override
    public int getBytesPerBlock() {
        return vkFormatBytesPerBlock(mFormat);
    }

    @Override
    public int getDepthBits() {
        return vkFormatDepthBits(mFormat);
    }

    @Override
    public int getStencilBits() {
        return vkFormatStencilBits(mFormat);
    }

    @Override
    public int getFormatKey() {
        return mFormat;
    }

    @Override
    public int hashCode() {
        return mFormat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return mFormat == ((VkBackendFormat) o).mFormat;
    }

    @Override
    public String toString() {
        return "{backend=Vulkan" +
                ", format=" + vkFormatName(mFormat) +
                '}';
    }
}
