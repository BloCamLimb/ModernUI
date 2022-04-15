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

package icyllis.arcui.vulkan;

import icyllis.arcui.graphics.BackendFormat;
import icyllis.arcui.graphics.Types;
import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;

public final class VkBackendFormat extends BackendFormat {

    private final int mFormat;

    public VkBackendFormat(@NativeType("VkFormat") int format) {
        mFormat = format;
    }

    @Override
    public int getBackend() {
        return Types.VULKAN;
    }

    @Override
    public int getTextureType() {
        return Types.TEXTURE_TYPE_2D;
    }

    @Override
    public int getChannelMask() {
        return VkUtil.getVkFormatChannels(mFormat);
    }

    @Override
    public int getVkFormat() {
        return mFormat;
    }

    @Nonnull
    @Override
    public BackendFormat makeTexture2D() {
        return this;
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
