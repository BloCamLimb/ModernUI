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

import icyllis.arc3d.core.*;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Core;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Shader used to draw an image as a texture. The image can be repeated or
 * mirrored by setting the tiling mode.
 */
public class ImageShader extends Shader {

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
    public static final int FILTER_MODE_NEAREST = SamplingOptions.FILTER_MODE_NEAREST;

    /**
     * Interpolate between 2x2 sample points (bilinear interpolation).
     */
    public static final int FILTER_MODE_LINEAR = SamplingOptions.FILTER_MODE_LINEAR;

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
    public static final int MIPMAP_MODE_NONE = SamplingOptions.MIPMAP_MODE_NONE;

    /**
     * Sample from the nearest level.
     */
    public static final int MIPMAP_MODE_NEAREST = SamplingOptions.FILTER_MODE_NEAREST;

    /**
     * Interpolate between the two nearest levels.
     */
    public static final int MIPMAP_MODE_LINEAR = SamplingOptions.MIPMAP_MODE_LINEAR;

    /**
     * Use nearest-neighbour sampling for minification, magnification; no mipmapping.
     * Also known as point sampling.
     */
    public static final Object FILTER_POINT = SamplingOptions.POINT;

    /**
     * Use linear interpolation for minification, magnification; no mipmapping.
     * Also known as triangle sampling and bilinear sampling.
     */
    public static final Object FILTER_LINEAR = SamplingOptions.LINEAR;

    /**
     * Use bicubic sampling, the cubic B-spline with B=1, C=0.
     */
    public static final Object FILTER_CUBIC_BSPLINE = SamplingOptions.CUBIC_BSPLINE;

    /**
     * Use bicubic sampling, the Mitchell–Netravali filter with B=1/3, C=1/3.
     */
    public static final Object FILTER_MITCHELL = SamplingOptions.MITCHELL;

    /**
     * Use bicubic sampling, the Photoshop bicubic filter with B=0, C=0.75.
     */
    public static final Object FILTER_PHOTOSHOP_BICUBIC = SamplingOptions.PHOTOSHOP_BICUBIC;

    /**
     * Use bicubic sampling, the Catmull-Rom spline with B=0, C=0.5.
     */
    public static final Object FILTER_CATMULLROM = SamplingOptions.CATMULLROM;

    private final Matrix mLocalMatrix;

    // closed by cleaner
    @Nullable
    private final icyllis.arc3d.core.shaders.Shader mShader;

    /**
     * Create a new shader for the given image.
     * <p>
     * If local matrix is not null, then it transforms image's local coordinates
     * to geometry's local coordinates.
     *
     * @param image       the image to use inside the shader
     * @param tileModeX   the tiling mode on x-axis to draw the image
     * @param tileModeY   the tiling mode on y-axis to draw the image
     * @param filter      the filter mode to draw the image
     * @param localMatrix the local matrix, null means identity
     */
    public ImageShader(@NonNull Image image, @NonNull TileMode tileModeX,
                       @NonNull TileMode tileModeY, @FilterMode int filter,
                       @Nullable Matrix localMatrix) {
        this(image, tileModeX, tileModeY,
                SamplingOptions.make(filter),
                localMatrix);
    }

    /**
     * Create a new shader for the given image.
     * <p>
     * If the image has no mipmaps, then mipmap mode will be ignored when
     * drawing.
     * <p>
     * If local matrix is not null, then it transforms image's local coordinates
     * to geometry's local coordinates.
     *
     * @param image       the image to use inside the shader
     * @param tileModeX   the tiling mode on x-axis to draw the image
     * @param tileModeY   the tiling mode on y-axis to draw the image
     * @param filter      the filter mode to draw the image
     * @param mipmap      the mipmap mode to draw the image
     * @param localMatrix the local matrix, null means identity
     */
    public ImageShader(@NonNull Image image, @NonNull TileMode tileModeX,
                       @NonNull TileMode tileModeY, @FilterMode int filter,
                       @MipmapMode int mipmap, @Nullable Matrix localMatrix) {
        this(image, tileModeX, tileModeY,
                SamplingOptions.make(filter, mipmap),
                localMatrix);
    }

    /**
     * Create a new shader for the given image.
     * <p>
     * The filter must be one of constants in this class, which are {@link #FILTER_POINT},
     * {@link #FILTER_LINEAR}, {@link #FILTER_CUBIC_BSPLINE}, {@link #FILTER_MITCHELL},
     * {@link #FILTER_PHOTOSHOP_BICUBIC} and {@link #FILTER_CATMULLROM}.
     * <p>
     * If local matrix is not null, then it transforms image's local coordinates
     * to geometry's local coordinates.
     *
     * @param image       the image to use inside the shader
     * @param tileModeX   the tiling mode on x-axis to draw the image
     * @param tileModeY   the tiling mode on y-axis to draw the image
     * @param filter      the filter to draw the image
     * @param localMatrix the local matrix, null means identity
     */
    public ImageShader(@NonNull Image image, @NonNull TileMode tileModeX,
                       @NonNull TileMode tileModeY, @NonNull Object filter,
                       @Nullable Matrix localMatrix) {
        this(image, tileModeX, tileModeY,
                (SamplingOptions) filter,
                localMatrix);
    }

    private ImageShader(@NonNull Image image, @NonNull TileMode tileModeX,
                        @NonNull TileMode tileModeY, @NonNull SamplingOptions sampling,
                        @Nullable Matrix localMatrix) {
        mShader = icyllis.arc3d.core.shaders.ImageShader.make(
                RefCnt.create(image.getNativeImage()),
                tileModeX.nativeInt, tileModeY.nativeInt,
                sampling, localMatrix
        );
        if (mShader == null) {
            throw new IllegalArgumentException();
        }
        if (localMatrix != null && !localMatrix.isIdentity()) {
            mLocalMatrix = new Matrix(localMatrix);
        } else {
            mLocalMatrix = null;
        }
        Core.registerNativeResource(this, mShader);
    }

    /**
     * Return true if the shader has a non-identity local matrix.
     *
     * @param localMatrix Set to the local matrix of the shader, if the shader's matrix is non-null.
     * @return true if the shader has a non-identity local matrix
     */
    public boolean getLocalMatrix(@NonNull Matrix localMatrix) {
        if (mLocalMatrix != null) {
            localMatrix.set(mLocalMatrix);
            return true; // presence of mLocalMatrix means it's not identity
        }
        return false;
    }

    /**
     * Return true if the local matrix of the shader is equal to the given local matrix.
     *
     * @param localMatrix the local matrix to compare, null means identity
     * @return true if local matrix equals
     */
    public boolean localMatrixEquals(@Nullable Matrix localMatrix) {
        if (localMatrix == null || localMatrix.isIdentity()) {
            return mLocalMatrix == null;
        } else if (mLocalMatrix == null) {
            return false;
        } else {
            return mLocalMatrix.equals(localMatrix);
        }
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    @RawPtr
    @Override
    public icyllis.arc3d.core.shaders.Shader getNativeShader() {
        return mShader;
    }
}
