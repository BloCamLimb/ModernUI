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

import javax.annotation.Nonnull;

/**
 * Represents a 3x3 row-major matrix.
 */
@SuppressWarnings("unused")
public class Matrix extends icyllis.arc3d.core.Matrix {

    /**
     * Create a new identity matrix.
     */
    public Matrix() {
    }

    public Matrix(Matrix m) {
        super(m);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param r the rectangle to transform
     */
    public void mapRect(@Nonnull RectF r) {
        float x1 = m11 * r.left + m21 * r.top + m41;
        float y1 = m12 * r.left + m22 * r.top + m42;
        float x2 = m11 * r.right + m21 * r.top + m41;
        float y2 = m12 * r.right + m22 * r.top + m42;
        float x3 = m11 * r.left + m21 * r.bottom + m41;
        float y3 = m12 * r.left + m22 * r.bottom + m42;
        float x4 = m11 * r.right + m21 * r.bottom + m41;
        float y4 = m12 * r.right + m22 * r.bottom + m42;
        if (hasPerspective()) {
            // project
            float w = 1.0f / (m14 * r.left + m24 * r.top + m44);
            x1 *= w;
            y1 *= w;
            w = 1.0f / (m14 * r.right + m24 * r.top + m44);
            x2 *= w;
            y2 *= w;
            w = 1.0f / (m14 * r.left + m24 * r.bottom + m44);
            x3 *= w;
            y3 *= w;
            w = 1.0f / (m14 * r.right + m24 * r.bottom + m44);
            x4 *= w;
            y4 *= w;
        }
        r.left = MathUtil.min(x1, x2, x3, x4);
        r.top = MathUtil.min(y1, y2, y3, y4);
        r.right = MathUtil.max(x1, x2, x3, x4);
        r.bottom = MathUtil.max(y1, y2, y3, y4);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param out the round values
     */
    public void mapRect(@Nonnull RectF r, @Nonnull Rect out) {
        mapRect(r.left, r.top, r.right, r.bottom, out);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param out the round values
     */
    public void mapRect(@Nonnull Rect r, @Nonnull Rect out) {
        mapRect(r.left, r.top, r.right, r.bottom, out);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param out the round values
     */
    public void mapRect(float l, float t, float r, float b, @Nonnull Rect out) {
        float x1 = m11 * l + m21 * t + m41;
        float y1 = m12 * l + m22 * t + m42;
        float x2 = m11 * r + m21 * t + m41;
        float y2 = m12 * r + m22 * t + m42;
        float x3 = m11 * l + m21 * b + m41;
        float y3 = m12 * l + m22 * b + m42;
        float x4 = m11 * r + m21 * b + m41;
        float y4 = m12 * r + m22 * b + m42;
        if (hasPerspective()) {
            // project
            float w = 1.0f / (m14 * l + m24 * t + m44);
            x1 *= w;
            y1 *= w;
            w = 1.0f / (m14 * r + m24 * t + m44);
            x2 *= w;
            y2 *= w;
            w = 1.0f / (m14 * l + m24 * b + m44);
            x3 *= w;
            y3 *= w;
            w = 1.0f / (m14 * r + m24 * b + m44);
            x4 *= w;
            y4 *= w;
        }
        out.left = Math.round(MathUtil.min(x1, x2, x3, x4));
        out.top = Math.round(MathUtil.min(y1, y2, y3, y4));
        out.right = Math.round(MathUtil.max(x1, x2, x3, x4));
        out.bottom = Math.round(MathUtil.max(y1, y2, y3, y4));
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param out the round out values
     */
    public void mapRectOut(@Nonnull RectF r, @Nonnull Rect out) {
        mapRectOut(r.left, r.top, r.right, r.bottom, out);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param out the round out values
     */
    public void mapRectOut(@Nonnull Rect r, @Nonnull Rect out) {
        mapRectOut(r.left, r.top, r.right, r.bottom, out);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param out the round out values
     */
    public void mapRectOut(float l, float t, float r, float b, @Nonnull Rect out) {
        float x1 = m11 * l + m21 * t + m41;
        float y1 = m12 * l + m22 * t + m42;
        float x2 = m11 * r + m21 * t + m41;
        float y2 = m12 * r + m22 * t + m42;
        float x3 = m11 * l + m21 * b + m41;
        float y3 = m12 * l + m22 * b + m42;
        float x4 = m11 * r + m21 * b + m41;
        float y4 = m12 * r + m22 * b + m42;
        if (hasPerspective()) {
            // project
            float w = 1.0f / (m14 * l + m24 * t + m44);
            x1 *= w;
            y1 *= w;
            w = 1.0f / (m14 * r + m24 * t + m44);
            x2 *= w;
            y2 *= w;
            w = 1.0f / (m14 * l + m24 * b + m44);
            x3 *= w;
            y3 *= w;
            w = 1.0f / (m14 * r + m24 * b + m44);
            x4 *= w;
            y4 *= w;
        }
        out.left = (int) Math.floor(MathUtil.min(x1, x2, x3, x4));
        out.top = (int) Math.floor(MathUtil.min(y1, y2, y3, y4));
        out.right = (int) Math.ceil(MathUtil.max(x1, x2, x3, x4));
        out.bottom = (int) Math.ceil(MathUtil.max(y1, y2, y3, y4));
    }

    /**
     * Map a point in the X-Y plane.
     *
     * @param p the point to transform
     */
    public void mapPoint(@Nonnull PointF p) {
        if (getType() <= kAffine_Mask) {
            p.set(m11 * p.x + m21 * p.y + m41,
                    m12 * p.x + m22 * p.y + m42);
        } else {
            // project
            final float x = m11 * p.x + m21 * p.y + m41;
            final float y = m12 * p.x + m22 * p.y + m42;
            float w = 1.0f / (m14 * p.x + m24 * p.y + m44);
            p.x = x * w;
            p.y = y * w;
        }
    }

    /**
     * Map a point in the X-Y plane.
     *
     * @param p the point to transform
     */
    public void mapPoint(@Nonnull float[] p) {
        if (getType() <= kAffine_Mask) {
            final float x = m11 * p[0] + m21 * p[1] + m41;
            final float y = m12 * p[0] + m22 * p[1] + m42;
            p[0] = x;
            p[1] = y;
        } else {
            final float x = m11 * p[0] + m21 * p[1] + m41;
            final float y = m12 * p[0] + m22 * p[1] + m42;
            float w = 1.0f / (m14 * p[0] + m24 * p[1] + m44);
            p[0] = x * w;
            p[1] = y * w;
        }
    }
}
