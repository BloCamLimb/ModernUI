/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.core;

public class SamplingOptions {

    // FilterModes and MipmapModes sync with SamplerDesc::Filter and SamplerDesc::MipmapMode
    /**
     * Filter modes.
     */
    public static final int
            FILTER_MODE_NEAREST = 0, // single sample point (nearest neighbor)
            FILTER_MODE_LINEAR = 1; // interpolate between 2x2 sample points (bilinear interpolation)

    /**
     * Mipmap modes.
     */
    public static final int
            MIPMAP_MODE_NONE = 0, // ignore mipmap levels, sample from the "base"
            MIPMAP_MODE_NEAREST = 1, // sample from the nearest level
            MIPMAP_MODE_LINEAR = 2; // interpolate between the two nearest levels

    public final int mMinFilter;
    public final int mMagFilter;
    public final int mMipmap;
    public final int mMaxAniso;
    public final boolean mUseCubic;
    public final float mCubicB;
    public final float mCubicC;

    public SamplingOptions(int minFilter, int magFilter, int mipmap, int maxAniso, boolean useCubic, float cubicB, float cubicC) {
        mMinFilter = minFilter;
        mMagFilter = magFilter;
        mMipmap = mipmap;
        mMaxAniso = maxAniso;
        mUseCubic = useCubic;
        mCubicB = cubicB;
        mCubicC = cubicC;
    }
}