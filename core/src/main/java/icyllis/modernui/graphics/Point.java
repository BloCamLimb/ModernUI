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
 * Represents a point holding two integer values.
 */
public class Point {

    public int x;
    public int y;

    public Point() {
    }

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point(@NonNull Point p) {
        x = p.x;
        y = p.y;
    }

    @NonNull
    public static Point copy(@Nullable Point p) {
        return p == null ? new Point() : p.copy();
    }

    public void set(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void set(@NonNull Point p) {
        x = p.x;
        y = p.y;
    }

    public void negate() {
        x = -x;
        y = -y;
    }

    public void offset(int dx, int dy) {
        x += dx;
        y += dy;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Point p) {
            return this.x == p.x && this.y == p.y;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Point(" +
                x + ", " + y +
                ')';
    }

    @NonNull
    public Point copy() {
        return new Point(x, y);
    }
}
