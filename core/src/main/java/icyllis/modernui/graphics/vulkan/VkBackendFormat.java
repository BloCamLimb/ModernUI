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

package icyllis.modernui.graphics.vulkan;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.engine.BackendFormat;
import icyllis.modernui.graphics.engine.Engine;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.lwjgl.system.NativeType;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

import static icyllis.modernui.graphics.vulkan.VkCore.*;

@Immutable
public final class VkBackendFormat extends BackendFormat {

    private static final Int2ObjectMap<VkBackendFormat> FORMATS =
            new Int2ObjectOpenHashMap<>(25);

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
    @NonNull
    public static VkBackendFormat make(@NativeType("VkFormat") int format, boolean isExternal) {
        Objects.checkIndex(format, Integer.MAX_VALUE);
        int key = (format) | (isExternal ? Integer.MIN_VALUE : 0);
        VkBackendFormat backendFormat = FORMATS.get(key);
        if (backendFormat != null) {
            return backendFormat;
        }
        backendFormat = new VkBackendFormat(format, isExternal);
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

    @NonNull
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
                ", isExternal=" + mIsExternal +
                '}';
    }
}
