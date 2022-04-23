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

package icyllis.arcui.vk;

import icyllis.arcui.hgi.BackendFormat;
import icyllis.arcui.hgi.Types;
import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;

public final class VkBackendFormat extends BackendFormat {

    private final int mFormat;
    private final int mTextureType;

    public VkBackendFormat(@NativeType("VkFormat") int format, boolean useDRMModifier) {
        mFormat = format;
        mTextureType = useDRMModifier ? Types.TEXTURE_TYPE_EXTERNAL : Types.TEXTURE_TYPE_2D;
    }

    @Override
    public int getBackend() {
        return Types.VULKAN;
    }

    @Override
    public int getTextureType() {
        return mTextureType;
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
        if (mTextureType == Types.TEXTURE_TYPE_2D) {
            return this;
        }
        return new VkBackendFormat(mFormat, false);
    }

    @Override
    public int getCompressionType() {
        return VkUtil.getVkFormatCompressionType(mFormat);
    }

    @Override
    public int getBytesPerBlock() {
        return VkUtil.getVkFormatBytesPerBlock(mFormat);
    }

    @Override
    public int getStencilBits() {
        return VkUtil.getVkFormatStencilBits(mFormat);
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
