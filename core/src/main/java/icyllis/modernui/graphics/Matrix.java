/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

import icyllis.arc3d.core.Matrix4c;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;

/**
 * This class represents a 3x3 matrix and a 2D transformation, its components
 * correspond to x, y, and w of a 4x4 matrix, where z is discarded.
 */
@SuppressWarnings("unused")
public class Matrix extends icyllis.arc3d.sketch.Matrix {

    /**
     * Create a new identity matrix.
     */
    public Matrix() {
        super();
    }

    /**
     * Create a new matrix copied from the given matrix.
     */
    public Matrix(@Nullable Matrix m) {
        super();
        if (m != null) {
            m.store(this);
        }
    }

    /**
     * Create a new matrix from the given elements.
     * The order matches GLSL's column major.
     *
     * @param scaleX the value of m11
     * @param shearY the value of m12
     * @param persp0 the value of m14
     * @param shearX the value of m21
     * @param scaleY the value of m22
     * @param persp1 the value of m24
     * @param transX the value of m41
     * @param transY the value of m42
     * @param persp2 the value of m44
     */
    public Matrix(float scaleX, float shearY, float persp0,
                  float shearX, float scaleY, float persp1,
                  float transX, float transY, float persp2) {
        super(scaleX, shearY, persp0, shearX, scaleY, persp1, transX, transY, persp2);
    }

    /**
     * Converts this 4x4 matrix to 3x3 matrix, the third row and column are discarded.
     * <pre>{@code
     * [ a b x c ]      [ a b c ]
     * [ d e x f ]  ->  [ d e f ]
     * [ x x x x ]      [ g h i ]
     * [ g h x i ]
     * }</pre>
     */
    @ApiStatus.Experimental
    public Matrix(@NonNull Matrix4c m) {
        super(m);
    }

    /**
     * Copy the given matrix into this matrix. If m is null, reset this matrix to the
     * identity matrix.
     */
    public void set(@Nullable Matrix m) {
        if (m != null) {
            m.store(this);
        } else {
            setIdentity();
        }
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param r the rectangle to transform
     */
    //@formatter:off
    public void mapRect(@NonNull RectF r) {
        int typeMask = getType();
        if (typeMask <= kTranslate_Mask) {
            r.left   = r.left   + m41;
            r.top    = r.top    + m42;
            r.right  = r.right  + m41;
            r.bottom = r.bottom + m42;
            return;
        }
        if ((typeMask & ~(kScale_Mask | kTranslate_Mask)) == 0) {
            float x1 = r.left   * m11 + m41;
            float y1 = r.top    * m22 + m42;
            float x2 = r.right  * m11 + m41;
            float y2 = r.bottom * m22 + m42;
            r.left   = Math.min(x1, x2);
            r.top    = Math.min(y1, y2);
            r.right  = Math.max(x1, x2);
            r.bottom = Math.max(y1, y2);
            return;
        }
        float x1 = m11 * r.left +  m21 * r.top    + m41;
        float y1 = m12 * r.left +  m22 * r.top    + m42;
        float x2 = m11 * r.right + m21 * r.top    + m41;
        float y2 = m12 * r.right + m22 * r.top    + m42;
        float x3 = m11 * r.left +  m21 * r.bottom + m41;
        float y3 = m12 * r.left +  m22 * r.bottom + m42;
        float x4 = m11 * r.right + m21 * r.bottom + m41;
        float y4 = m12 * r.right + m22 * r.bottom + m42;
        if ((typeMask & kPerspective_Mask) != 0) {
            float w;
            w = 1.0f / (m14 * r.left  + m24 * r.top    + m44);
            x1 *= w;
            y1 *= w;
            w = 1.0f / (m14 * r.right + m24 * r.top    + m44);
            x2 *= w;
            y2 *= w;
            w = 1.0f / (m14 * r.left  + m24 * r.bottom + m44);
            x3 *= w;
            y3 *= w;
            w = 1.0f / (m14 * r.right + m24 * r.bottom + m44);
            x4 *= w;
            y4 *= w;
        }
        r.left   = MathUtil.min(x1, x2, x3, x4);
        r.top    = MathUtil.min(y1, y2, y3, y4);
        r.right  = MathUtil.max(x1, x2, x3, x4);
        r.bottom = MathUtil.max(y1, y2, y3, y4);
    }
    //@formatter:on

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param out the round values
     */
    public void mapRect(@NonNull RectF r, @NonNull Rect out) {
        mapRect(r.left, r.top, r.right, r.bottom, out);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param out the round values
     */
    public void mapRect(@NonNull Rect r, @NonNull Rect out) {
        mapRect(r.left, r.top, r.right, r.bottom, out);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param out the round values
     */
    //@formatter:off
    public void mapRect(float left, float top, float right, float bottom, @NonNull Rect out) {
        int typeMask = getType();
        if (typeMask <= kTranslate_Mask) {
            out.left   = Math.round(left   + m41);
            out.top    = Math.round(top    + m42);
            out.right  = Math.round(right  + m41);
            out.bottom = Math.round(bottom + m42);
            return;
        }
        if ((typeMask & ~(kScale_Mask | kTranslate_Mask)) == 0) {
            out.left =   Math.round(left   * m11 + m41);
            out.top =    Math.round(top    * m22 + m42);
            out.right =  Math.round(right  * m11 + m41);
            out.bottom = Math.round(bottom * m22 + m42);
            out.sort();
            return;
        }
        float x1 = m11 * left +  m21 * top    + m41;
        float y1 = m12 * left +  m22 * top    + m42;
        float x2 = m11 * right + m21 * top    + m41;
        float y2 = m12 * right + m22 * top    + m42;
        float x3 = m11 * left +  m21 * bottom + m41;
        float y3 = m12 * left +  m22 * bottom + m42;
        float x4 = m11 * right + m21 * bottom + m41;
        float y4 = m12 * right + m22 * bottom + m42;
        if ((typeMask & kPerspective_Mask) != 0) {
            float w;
            w = 1.0f / (m14 * left  + m24 * top    + m44);
            x1 *= w;
            y1 *= w;
            w = 1.0f / (m14 * right + m24 * top    + m44);
            x2 *= w;
            y2 *= w;
            w = 1.0f / (m14 * left  + m24 * bottom + m44);
            x3 *= w;
            y3 *= w;
            w = 1.0f / (m14 * right + m24 * bottom + m44);
            x4 *= w;
            y4 *= w;
        }
        out.left   = Math.round(MathUtil.min(x1, x2, x3, x4));
        out.top    = Math.round(MathUtil.min(y1, y2, y3, y4));
        out.right  = Math.round(MathUtil.max(x1, x2, x3, x4));
        out.bottom = Math.round(MathUtil.max(y1, y2, y3, y4));
    }
    //@formatter:on

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param out the round out values
     */
    public void mapRectOut(@NonNull RectF r, @NonNull Rect out) {
        mapRectOut(r.left, r.top, r.right, r.bottom, out);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param out the round out values
     */
    public void mapRectOut(@NonNull Rect r, @NonNull Rect out) {
        mapRectOut(r.left, r.top, r.right, r.bottom, out);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param out the round out values
     */
    //@formatter:off
    public void mapRectOut(float left, float top, float right, float bottom, @NonNull Rect out) {
        int typeMask = getType();
        if (typeMask <= kTranslate_Mask) {
            out.left   = (int) Math.floor(left   + m41);
            out.top    = (int) Math.floor(top    + m42);
            out.right  = (int) Math.ceil (right  + m41);
            out.bottom = (int) Math.ceil (bottom + m42);
            return;
        }
        if ((typeMask & ~(kScale_Mask | kTranslate_Mask)) == 0) {
            out.left =   (int) Math.floor(left   * m11 + m41);
            out.top =    (int) Math.floor(top    * m22 + m42);
            out.right =  (int) Math.ceil (right  * m11 + m41);
            out.bottom = (int) Math.ceil (bottom * m22 + m42);
            out.sort();
            return;
        }
        float x1 = m11 * left +  m21 * top    + m41;
        float y1 = m12 * left +  m22 * top    + m42;
        float x2 = m11 * right + m21 * top    + m41;
        float y2 = m12 * right + m22 * top    + m42;
        float x3 = m11 * left +  m21 * bottom + m41;
        float y3 = m12 * left +  m22 * bottom + m42;
        float x4 = m11 * right + m21 * bottom + m41;
        float y4 = m12 * right + m22 * bottom + m42;
        if ((typeMask & kPerspective_Mask) != 0) {
            float w;
            w = 1.0f / (m14 * left  + m24 * top    + m44);
            x1 *= w;
            y1 *= w;
            w = 1.0f / (m14 * right + m24 * top    + m44);
            x2 *= w;
            y2 *= w;
            w = 1.0f / (m14 * left  + m24 * bottom + m44);
            x3 *= w;
            y3 *= w;
            w = 1.0f / (m14 * right + m24 * bottom + m44);
            x4 *= w;
            y4 *= w;
        }
        out.left   = (int) Math.floor(MathUtil.min(x1, x2, x3, x4));
        out.top    = (int) Math.floor(MathUtil.min(y1, y2, y3, y4));
        out.right  = (int) Math.ceil (MathUtil.max(x1, x2, x3, x4));
        out.bottom = (int) Math.ceil (MathUtil.max(y1, y2, y3, y4));
    }
    //@formatter:on

    /**
     * Map a point in the X-Y plane.
     *
     * @param p the point to transform
     */
    public void mapPoint(@NonNull PointF p) {
        if (!hasPerspective()) {
            p.set(m11 * p.x + m21 * p.y + m41,
                    m12 * p.x + m22 * p.y + m42);
        } else {
            // project
            final float x = m11 * p.x + m21 * p.y + m41;
            final float y = m12 * p.x + m22 * p.y + m42;
            float w = m14 * p.x + m24 * p.y + m44;
            if (w != 0) {
                w = 1 / w;
            }
            p.x = x * w;
            p.y = y * w;
        }
    }
}
