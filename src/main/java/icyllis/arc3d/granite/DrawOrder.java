/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite;

/**
 * Orders are unsigned shorts.
 */
public class DrawOrder {

    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 0xFFFF;

    public static final int PAINTERS_ORDER_SHIFT = 48;
    public static final int STENCIL_INDEX_SHIFT = 32;
    public static final int DEPTH_SHIFT = 16;
    public static final int BIT_MASK = 0xFFFF;

    public static long makeFromDepth(int depth) {
        return (long) depth << DEPTH_SHIFT;
    }

    public static long makeFromDepthAndPaintersOrder(int depth, int paintersOrder) {
        return ((long) paintersOrder << PAINTERS_ORDER_SHIFT) | ((long) depth << DEPTH_SHIFT);
    }

    public static int getPaintersOrder(long packedDrawOrder) {
        return (int) (packedDrawOrder >>> PAINTERS_ORDER_SHIFT) & BIT_MASK;
    }

    public static int getStencilIndex(long packedDrawOrder) {
        return (int) (packedDrawOrder >>> STENCIL_INDEX_SHIFT) & BIT_MASK;
    }

    public static int getDepth(long packedDrawOrder) {
        return (int) (packedDrawOrder >>> DEPTH_SHIFT) & BIT_MASK;
    }

    public static float getDepthAsFloat(long packedDrawOrder) {
        return (float) getDepth(packedDrawOrder) / MAX_VALUE;
    }

    public static long updateWithPaintersOrder(long packedDrawOrder, int prevPaintersOrder) {
        int nextOrder = prevPaintersOrder + 1;
        int order = Math.max(nextOrder, getPaintersOrder(packedDrawOrder));
        return (packedDrawOrder & 0x0000FFFF_FFFF0000L) | ((long) order << PAINTERS_ORDER_SHIFT);
    }

    public static long updateWithStencilIndex(long packedDrawOrder, int disjointSet) {
        assert getStencilIndex(packedDrawOrder) == MIN_VALUE;
        return packedDrawOrder | ((long) disjointSet << STENCIL_INDEX_SHIFT);
    }
}
