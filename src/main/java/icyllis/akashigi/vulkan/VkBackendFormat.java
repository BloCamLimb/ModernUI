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

package icyllis.akashigi.vulkan;

import icyllis.akashigi.engine.BackendFormat;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;
import java.util.Objects;

import static icyllis.akashigi.engine.Engine.BackendApi;
import static icyllis.akashigi.vulkan.VkCore.*;

public final class VkBackendFormat extends BackendFormat {

    private static final Int2ObjectOpenHashMap<VkBackendFormat> sVkBackendFormats =
            new Int2ObjectOpenHashMap<>(25, 0.8f);

    private final int mFormat;
    private final boolean mIsExternal;

    /**
     * @see #make(int, boolean)
     */
    VkBackendFormat(@NativeType("VkFormat") int format, boolean isExternal) {
        mFormat = format;
        mIsExternal = isExternal;
    }

    //TODO cache only known formats
    @Nonnull
    public static VkBackendFormat make(@NativeType("VkFormat") int format, boolean isExternal) {
        Objects.checkIndex(format, Integer.MAX_VALUE);
        int key = (format) | (isExternal ? Integer.MIN_VALUE : 0);
        VkBackendFormat backendFormat = sVkBackendFormats.get(key);
        if (backendFormat != null) {
            return backendFormat;
        }
        backendFormat = new VkBackendFormat(format, isExternal);
        if (backendFormat.getBytesPerBlock() != 0) {
            sVkBackendFormats.put(key, backendFormat);
        }
        return backendFormat;
    }

    @Override
    public int getBackend() {
        return BackendApi.kVulkan;
    }

    @Override
    public boolean isExternal() {
        return mIsExternal;
    }

    @Override
    public int getChannelFlags() {
        return vkFormatChannels(mFormat);
    }

    @Override
    public int getVkFormat() {
        return mFormat;
    }

    @Nonnull
    @Override
    public BackendFormat makeInternal() {
        if (mIsExternal) {
            return make(mFormat, false);
        }
        return this;
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
    public int getFormatKey() {
        return mFormat;
    }

    @Override
    public int hashCode() {
        return HashCommon.mix(mFormat);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return mFormat == ((VkBackendFormat) o).mFormat;
    }

    @Override
    public String toString() {
        return "{mBackend=Vulkan" +
                ", mFormat=" + vkFormatName(mFormat) +
                ", mIsExternal=" + mIsExternal +
                '}';
    }
}
