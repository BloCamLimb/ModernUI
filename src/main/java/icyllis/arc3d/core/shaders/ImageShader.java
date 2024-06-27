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

package icyllis.arc3d.core.shaders;

import icyllis.arc3d.core.*;

import javax.annotation.Nullable;

public class ImageShader extends Shader {

    // TileModes sync with SamplerDesc::AddressMode
    /**
     * Repeat the shader's image horizontally and vertically.
     */
    public static final int TILE_MODE_REPEAT = 0;
    /**
     * Repeat the shader's image horizontally and vertically, alternating
     * mirror images so that adjacent images always seam.
     */
    public static final int TILE_MODE_MIRROR = 1;
    /**
     * Replicate the edge color if the shader draws outside of its
     * original bounds.
     */
    public static final int TILE_MODE_CLAMP = 2;
    /**
     * Only draw within the original domain, return transparent-black everywhere else.
     */
    public static final int TILE_MODE_DECAL = 3;

    @SharedPtr
    public final Image mImage;
    public final SamplingOptions mSampling;
    public final int mTileModeX;
    public final int mTileModeY;

    public final Rect2f mSubset;

    ImageShader(Image image, Rect2fc subset, int tileModeX, int tileModeY, SamplingOptions sampling) {
        mImage = image;
        mSampling = sampling;
        mTileModeX = tileModeX;
        mTileModeY = tileModeY;
        mSubset = new Rect2f(subset);
    }

    @Nullable
    @SharedPtr
    public static Shader make(@SharedPtr Image image,
                              int tileModeX, int tileModeY,
                              SamplingOptions sampling,
                              @Nullable Matrixc localMatrix) {
        Rect2fc subset = image != null ? new Rect2f(0, 0, image.getWidth(), image.getHeight()) : Rect2f.empty();
        return makeSubset(image, subset, tileModeX, tileModeY, sampling, localMatrix);
    }

    @Nullable
    @SharedPtr
    public static Shader makeSubset(@SharedPtr Image image,
                                    Rect2fc subset,
                                    int tileModeX, int tileModeY,
                                    SamplingOptions sampling,
                                    @Nullable Matrixc localMatrix) {
        if (sampling.mUseCubic) {
            if (!(sampling.mCubicB >= 0 && sampling.mCubicB <= 1) || // also capture NaN
                    !(sampling.mCubicC >= 0 && sampling.mCubicC <= 1)) {
                return null;
            }
        }
        if (image == null || subset.isEmpty()) {
            return new EmptyShader();
        }

        if (!(0 <= subset.left() && 0 <= subset.top() && // also capture NaN
                image.getWidth() >= subset.right() &&
                image.getHeight() >= subset.bottom())) {
            return null;
        }

        @SharedPtr
        Shader s = new ImageShader(image,
                subset,
                tileModeX, tileModeY,
                sampling);
        Matrix lm = localMatrix != null ? new Matrix(localMatrix) : new Matrix();
        return new LocalMatrixShader(s, lm);
    }

    @Override
    protected void deallocate() {
        RefCnt.move(mImage);
    }

    @RawPtr
    public Image getImage() {
        return mImage;
    }

    public SamplingOptions getSampling() {
        return mSampling;
    }

    public int getTileModeX() {
        return mTileModeX;
    }

    public int getTileModeY() {
        return mTileModeY;
    }

    public Rect2fc getSubset() {
        return mSubset;
    }
}
