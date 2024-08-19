/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import org.intellij.lang.annotations.MagicConstant;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Shader used to draw an image as a texture. The image can be repeated or
 * mirrored by setting the tiling mode.
 */
public class ImageShader {

    /**
     * The {@code FilterMode} specifies the sampling method on transformed texture images.
     * The default is {@link #FILTER_MODE_LINEAR}.
     */
    @MagicConstant(intValues = {FILTER_MODE_NEAREST, FILTER_MODE_LINEAR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FilterMode {
    }

    /**
     * Single sample point (nearest neighbor).
     */
    public static final int FILTER_MODE_NEAREST = 0;

    /**
     * Interpolate between 2x2 sample points (bilinear interpolation).
     */
    public static final int FILTER_MODE_LINEAR = 1;

    /**
     * The {@code MipmapMode} specifies the interpolation method for MIP image levels when
     * down-sampling texture images. The default is {@link #MIPMAP_MODE_NONE}.
     */
    @MagicConstant(intValues = {MIPMAP_MODE_NONE, MIPMAP_MODE_NEAREST, MIPMAP_MODE_LINEAR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MipmapMode {
    }

    /**
     * Ignore mipmap levels, sample from the "base".
     */
    public static final int MIPMAP_MODE_NONE = 0;

    /**
     * Sample from the nearest level.
     */
    public static final int MIPMAP_MODE_NEAREST = 1;

    /**
     * Interpolate between the two nearest levels.
     */
    public static final int MIPMAP_MODE_LINEAR = 2;
}
