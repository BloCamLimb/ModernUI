/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.mock;

import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.core.MathUtil;
import icyllis.arc3d.engine.BackendFormat;

import javax.annotation.Nonnull;

import static icyllis.arc3d.engine.Engine.BackendApi;

public final class MockBackendFormat extends BackendFormat {

    private final int mColorType;
    private final int mCompressionType;
    private final short mDepthBits;
    private final boolean mIsStencilFormat;

    /**
     * @see #make(int, int, int, boolean)
     */
    public MockBackendFormat(int colorType, int compressionType, short depthBits, boolean isStencilFormat) {
        mColorType = colorType;
        mCompressionType = compressionType;
        mDepthBits = depthBits;
        mIsStencilFormat = isStencilFormat;
    }

    @Nonnull
    public static MockBackendFormat make(int colorType, int compressionType) {
        return make(colorType, compressionType, 0, false);
    }

    @Nonnull
    public static MockBackendFormat make(int colorType, int compressionType, int depthBits, boolean isStencilFormat) {
        return new MockBackendFormat(colorType, compressionType, (short) depthBits, isStencilFormat);
    }

    @Override
    public int getBackend() {
        return BackendApi.kMock;
    }

    @Override
    public int getChannelFlags() {
        return ImageInfo.colorTypeChannelFlags(mColorType);
    }

    @Override
    public boolean isSRGB() {
        return mCompressionType == ImageInfo.COMPRESSION_NONE && mColorType == ImageInfo.CT_RGBA_8888_SRGB;
    }

    @Override
    public int getCompressionType() {
        return mCompressionType;
    }

    @Override
    public int getBytesPerBlock() {
        if (mCompressionType != ImageInfo.COMPRESSION_NONE) {
            return 8; // 1 * ETC1Block or BC1Block
        } else if (mDepthBits > 0 || mIsStencilFormat) {
            return MathUtil.ceilPow2((mDepthBits >>> 3) + (mIsStencilFormat ? 1 : 0));
        } else {
            return ImageInfo.bytesPerPixel(mColorType);
        }
    }

    @Override
    public int getDepthBits() {
        return mDepthBits;
    }

    @Override
    public int getStencilBits() {
        return mIsStencilFormat ? 8 : 0;
    }

    @Override
    public int getFormatKey() {
        return mColorType;
    }
}
