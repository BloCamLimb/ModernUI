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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;

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

    public PointF(@NonNull Point p) {
        x = p.x;
        y = p.y;
    }

    public PointF(@NonNull PointF p) {
        x = p.x;
        y = p.y;
    }

    @NonNull
    public static PointF copy(@Nullable PointF p) {
        return p == null ? new PointF() : p.copy();
    }

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(@NonNull Point p) {
        x = p.x;
        y = p.y;
    }

    public void set(@NonNull PointF p) {
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
     * Return the Euclidean distance from (0,0) to the point
     */
    public float length() {
        return icyllis.arc3d.sketch.Point.length(x, y);
    }

    public boolean normalize() {
        double x = this.x;
        double y = this.y;
        double dmag = Math.sqrt(x * x + y * y);
        double dscale = 1.0 / dmag;
        float newX = (float) (x * dscale);
        float newY = (float) (y * dscale);
        if (icyllis.arc3d.sketch.Point.isDegenerate(newX, newY)) {
            return false;
        }
        this.x = newX;
        this.y = newY;
        return true;
    }

    public void round(@NonNull Point dst) {
        dst.set(Math.round(x), Math.round(y));
    }

    @Override
    public int hashCode() {
        int result = (x != 0.0f ? Float.floatToIntBits(x) : 0);
        result = 31 * result + (y != 0.0f ? Float.floatToIntBits(y) : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof PointF p) {
            return this.x == p.x && this.y == p.y;
        }
        return false;
    }

    @Override
    public String toString() {
        return "PointF(" +
                x + ", " + y +
                ')';
    }

    @NonNull
    public PointF copy() {
        return new PointF(x, y);
    }
}
