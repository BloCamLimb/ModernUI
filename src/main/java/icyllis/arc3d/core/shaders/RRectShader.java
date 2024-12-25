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

import icyllis.arc3d.core.Matrix;
import icyllis.arc3d.core.Matrixc;
import icyllis.arc3d.core.RRect;
import icyllis.arc3d.core.SharedPtr;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A shader that produces rounded rectangle effects, only 4-slice circular
 * rounded rect is supported. This shader emits single channel coverage modulated
 * by white alpha values.
 */
public final class RRectShader implements Shader {

    private final float mLeft;
    private final float mTop;
    private final float mRight;
    private final float mBottom;

    private final float mTopLeftRadius;
    private final float mTopRightRadius;
    private final float mBottomRightRadius;
    private final float mBottomLeftRadius;

    private final float mSmoothRadius;
    private final boolean mInverseFill;

    RRectShader(RRect rect, float topLeftRadius, float topRightRadius,
                       float bottomRightRadius, float bottomLeftRadius,
                       float smoothRadius, boolean inverseFill) {
        mLeft = rect.left();
        mTop = rect.top();
        mRight = rect.right();
        mBottom = rect.bottom();
        mTopLeftRadius = topLeftRadius;
        mTopRightRadius = topRightRadius;
        mBottomRightRadius = bottomRightRadius;
        mBottomLeftRadius = bottomLeftRadius;
        mSmoothRadius = smoothRadius;
        mInverseFill = inverseFill;
    }

    @Nullable
    @SharedPtr
    public static Shader make(@NonNull RRect rrect,
                              float smoothRadius,
                              boolean inverseFill,
                              @Nullable Matrixc localMatrix) {
        if (rrect.isEmpty() ||
                !Float.isFinite(smoothRadius)) {
            // empty, infinite or NaN
            return new EmptyShader();
        }
        if (!RRect.allCornersAreCircular(rrect)) {
            // elliptical corner is not supported
            return null;
        }
        float topLeftRad = rrect.getRadius(0);
        float topRightRad = rrect.getRadius(2);
        float bottomRightRad = rrect.getRadius(4);
        float bottomLeftRad = rrect.getRadius(6);
        if (topLeftRad + bottomRightRad > rrect.width() ||
                topRightRad + bottomLeftRad > rrect.width() ||
                topLeftRad + bottomRightRad > rrect.height() ||
                topRightRad + bottomLeftRad > rrect.height()) {
            // not 4-slice mesh
            return null;
        }

        @SharedPtr
        Shader s = new RRectShader(rrect,
                topLeftRad, topRightRad,
                bottomRightRad, bottomLeftRad,
                smoothRadius, inverseFill);
        Matrix lm = localMatrix != null ? new Matrix(localMatrix) : new Matrix();
        return new LocalMatrixShader(s, // move
                lm);
    }

    public float getLeft() {
        return mLeft;
    }

    public float getTop() {
        return mTop;
    }

    public float getRight() {
        return mRight;
    }

    public float getBottom() {
        return mBottom;
    }

    public float getTopLeftRadius() {
        return mTopLeftRadius;
    }

    public float getTopRightRadius() {
        return mTopRightRadius;
    }

    public float getBottomRightRadius() {
        return mBottomRightRadius;
    }

    public float getBottomLeftRadius() {
        return mBottomLeftRadius;
    }

    /**
     * Returns a center point that can divide the rounded rectangle into
     * 4 slices.
     */
    public float getCenterX() {
        return mLeft + Math.max(mBottomLeftRadius, mTopLeftRadius);
    }

    public float getCenterY() {
        return mTop + Math.max(mTopRightRadius, mTopLeftRadius);
    }

    public float getSmoothRadius() {
        return mSmoothRadius;
    }

    public boolean isInverseFill() {
        return mInverseFill;
    }

    @Override
    public void ref() {
    }

    @Override
    public void unref() {
    }

    @Override
    public boolean isTriviallyCounted() {
        return true;
    }
}
