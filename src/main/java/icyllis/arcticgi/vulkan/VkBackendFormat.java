/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.vulkan;

import icyllis.arcticgi.engine.BackendFormat;
import icyllis.arcticgi.engine.Engine;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;

import static icyllis.arcticgi.vulkan.VkCore.*;

public final class VkBackendFormat extends BackendFormat {

    private static final Long2ObjectOpenHashMap<VkBackendFormat> sVkBackendFormats =
            new Long2ObjectOpenHashMap<>(25, 0.8f);

    private final int mFormat;
    private final int mTextureType;

    /**
     * @see #make(int, boolean)
     */
    public VkBackendFormat(@NativeType("VkFormat") int format, boolean isExternal) {
        mFormat = format;
        mTextureType = isExternal ? Engine.TextureType_External : Engine.TextureType_2D;
    }

    @Nonnull
    public static VkBackendFormat make(@NativeType("VkFormat") int format, boolean isExternal) {
        // if this failed, use long key
        assert (format >= 0);
        int key = (format) | (isExternal ? Integer.MIN_VALUE : 0);
        // harmless race
        VkBackendFormat backendFormat = sVkBackendFormats.get(key);
        if (backendFormat != null) {
            return backendFormat;
        }
        backendFormat = new VkBackendFormat(format, isExternal);
        //TODO cache only known formats
        if (backendFormat.getBytesPerBlock() != 0) {
            sVkBackendFormats.put(key, backendFormat);
        }
        return backendFormat;
    }

    @Override
    public int getBackend() {
        return Engine.Vulkan;
    }

    @Override
    public int getTextureType() {
        return mTextureType;
    }

    @Override
    public int getChannelMask() {
        return vkFormatChannels(mFormat);
    }

    @Override
    public int getVkFormat() {
        return mFormat;
    }

    @Nonnull
    @Override
    public BackendFormat makeTexture2D() {
        if (mTextureType == Engine.TextureType_2D) {
            return this;
        }
        return make(mFormat, false);
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
    public int getStencilBits() {
        return vkFormatStencilBits(mFormat);
    }

    @Override
    public int getKey() {
        return mFormat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return mFormat == ((VkBackendFormat) o).mFormat;
    }

    @Override
    public int hashCode() {
        return mFormat;
    }
}
