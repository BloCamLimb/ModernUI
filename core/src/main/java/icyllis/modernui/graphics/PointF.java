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
import javax.annotation.Nullable;

/**
 * Represents a point holding two float values.
 */
public class PointF {

    public float x;
    public float y;

    public PointF() {
    }

    public PointF(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public PointF(@Nonnull Point p) {
        x = p.x;
        y = p.y;
    }

    @Nonnull
    public static PointF copy(@Nullable PointF p) {
        return p == null ? new PointF() : p.copy();
    }

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(@Nonnull Point p) {
        x = p.x;
        y = p.y;
    }

    public void set(@Nonnull PointF p) {
        x = p.x;
        y = p.y;
    }

    public void negate() {
        x = -x;
        y = -y;
    }

    public void offset(float dx, float dy) {
        x += dx;
        y += dy;
    }

    /**
     * Return the euclidean distance from (0,0) to the point
     */
    public float length() {
        return MathUtil.hypot(x, y);
    }

    public void round(@Nonnull Point dst) {
        dst.set(Math.round(x), Math.round(y));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PointF pointF = (PointF) o;

        if (Float.compare(pointF.x, x) != 0) return false;
        return Float.compare(pointF.y, y) == 0;
    }

    @Override
    public int hashCode() {
        int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
        result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PointF(" +
                x + ", " + y +
                ')';
    }

    @Nonnull
    public PointF copy() {
        return new PointF(x, y);
    }
}
