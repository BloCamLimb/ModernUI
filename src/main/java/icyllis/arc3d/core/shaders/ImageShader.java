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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ImageShader extends RefCnt implements Shader {

    @SharedPtr
    public final Image mImage;
    public final SamplingOptions mSampling;
    public final int mTileModeX;
    public final int mTileModeY;

    // If subset == (0,0,w,h) of the image, then no subset is applied. Subset will not be empty.
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
        Rect2fc subset = image != null
                ? new Rect2f(0, 0, image.getWidth(), image.getHeight())
                : Rect2f.empty();
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
                RefCnt.move(image);
                return null;
            }
        }
        if (image == null || subset.isEmpty()) {
            RefCnt.move(image);
            return new EmptyShader();
        }

        if (!(0 <= subset.left() && 0 <= subset.top() && // also capture NaN
                image.getWidth() >= subset.right() &&
                image.getHeight() >= subset.bottom())) {
            image.unref();
            return null;
        }

        @SharedPtr
        Shader s = new ImageShader(image, // move
                subset,
                tileModeX, tileModeY,
                sampling);
        Matrix lm = localMatrix != null ? new Matrix(localMatrix) : new Matrix();
        return new LocalMatrixShader(s, // move
                lm);
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

    /**
     * Create a 4x4 row major matrix for Mitchell–Netravali filters.
     */
    //@formatter:off
    public static float[] makeCubicMatrix(float B, float C) {
        return new float[]{
                  (1.f/6)*B      ,  1 - ( 2.f/6)*B    ,      ( 1.f/6)*B      ,      0       ,
                - (3.f/6)*B - C  ,         0          ,      ( 3.f/6)*B + C  ,      0       ,
                  (3.f/6)*B + 2*C, -3 + (12.f/6)*B + C,  3 - (15.f/6)*B - 2*C,     -C       ,
                - (1.f/6)*B - C  ,  2 - ( 9.f/6)*B - C, -2 + ( 9.f/6)*B + C  , (1.f/6)*B + C
        };
    }
    //@formatter:on

    @NonNull
    public static Rect2f preparePaintForDrawImageRect(@RawPtr Image image,
                                                      SamplingOptions sampling,
                                                      Rect2fc src, Rect2fc dst,
                                                      boolean strictSubset,
                                                      Paint paint) {
        // The paint should have already been cleaned for a regular drawImageRect, e.g. no path
        // effect and is a fill.
        assert (paint.getStyle() == Paint.FILL && paint.getPathEffect() == null);

        Rect2f imageBounds = new Rect2f();
        image.getBounds(imageBounds);

        assert (src.isFinite() && dst.isFinite() && dst.isSorted());
        Matrix localMatrix = new Matrix();
        localMatrix.setRectToRect(src, dst);
        Rect2f modifiedSrc = new Rect2f(src);
        Rect2f modifiedDst = new Rect2f(dst);
        if (!imageBounds.contains(modifiedSrc)) {
            if (!modifiedSrc.intersect(imageBounds)) {
                return modifiedDst; // Nothing to draw for this entry
            }
            // Update dst to match smaller src
            localMatrix.mapRect(modifiedSrc, modifiedDst);
        }

        boolean imageIsAlphaOnly = image.isAlphaOnly();

        @SharedPtr
        Shader imageShader;
        if (strictSubset) {
            imageShader = ImageShader.makeSubset(RefCnt.create(image), modifiedSrc,
                    TILE_MODE_CLAMP, TILE_MODE_CLAMP,
                    sampling, localMatrix);
        } else {
            imageShader = ImageShader.make(RefCnt.create(image),
                    TILE_MODE_CLAMP, TILE_MODE_CLAMP,
                    sampling, localMatrix);
        }
        if (imageShader == null) {
            modifiedDst.setEmpty();
            return modifiedDst;
        }
        if (imageIsAlphaOnly && paint.getShader() != null) {
            // Compose the image shader with the paint's shader. Alpha images+shaders should output the
            // texture's alpha multiplied by the shader's color. DstIn (d*sa) will achieve this with
            // the source image and dst shader (MakeBlend takes dst first, src second).
            imageShader = BlendShader.make(BlendMode.DST_IN,
                    /*src*/ imageShader, /*dst*/ paint.refShader());
        }

        paint.setShader(imageShader); // move
        return modifiedDst;
    }
}
