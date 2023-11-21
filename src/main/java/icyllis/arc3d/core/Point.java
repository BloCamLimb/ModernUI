/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

public class Point {

    public static float distanceToLineSegmentSq(
            float px, float py,
            float ax, float ay,
            float bx, float by
    ) {
        float ux = bx - ax;
        float uy = by - ay;
        float vx = px - ax;
        float vy = py - ay;

        float uDotV = ux * vx + uy * vy;

        // closest point is point A
        if (uDotV <= 0) {
            return vx * vx + vy * vy;
        }

        float uLengthSq = ux * ux + uy * uy;

        // closest point is point B
        if (uDotV > uLengthSq) {
            return distanceToSq(bx, by, px, py);
        }

        // closest point is inside segment
        float det = ux * vy - uy * vx;
        float temp = det / uLengthSq * det;
        // It's possible we have a degenerate segment, or we're so far away it looks degenerate
        // In this case, return squared distance to point A.
        if (!Float.isFinite(temp)) {
            return vx * vx + vy * vy;
        }
        return temp;
    }

    public static float distanceToLineSegment(
            float px, float py,
            float ax, float ay,
            float bx, float by
    ) {
        return (float) Math.sqrt(distanceToLineSegmentSq(px, py, ax, ay, bx, by));
    }

    public static float distanceToSq(
            float px, float py,
            float ax, float ay
    ) {
        float dx = px - ax;
        float dy = py - ay;
        return dx * dx + dy * dy;
    }
}
