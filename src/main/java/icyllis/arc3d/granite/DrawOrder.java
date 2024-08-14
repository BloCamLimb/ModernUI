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
 * DrawOrder aggregates the three separate sequences that Granite uses to re-order draws and their
 * sub-steps as much as possible while preserving the painter's order semantics of the public API.
 * <p>
 * To build the full DrawOrder for a draw, start with its assigned PaintersDepth (i.e. the original
 * painter's order of the draw call). From there, the DrawOrder can be updated to reflect
 * dependencies on previous draws, either from depth-only clip draws or because the draw is
 * transparent and must blend with the previous color values. Lastly, once the
 * CompressedPaintersOrder is finalized, the DrawOrder can be updated to reflect whether
 * the draw will involve the stencil buffer--and if so, specify the disjoint stencil set it
 * belongs to.
 * <p>
 * The original and effective order that draws are executed in is defined by the PaintersDepth.
 * However, the actual execution order is defined by first the CompressedPaintersOrder and then
 * the DisjointStencilIndex. This means that draws with much higher depths can be executed earlier
 * if painter's order compression allows for it.
 * <p>
 * Orders are 16-bit unsigned integers.
 * <p>
 * CompressedPaintersOrder is an ordinal number that allows draw commands to be re-ordered so long
 * as when they are executed, the read/writes to the color|depth attachments respect the original
 * painter's order. Logical draws with the same CompressedPaintersOrder can be assumed to be
 * executed in any order, however that may have been determined (e.g. BoundsManager or relying on
 * a depth test during rasterization).
 * <p>
 * Each DisjointStencilIndex specifies an implicit set of non-overlapping draws. Assuming that two
 * draws have the same CompressedPaintersOrder and the same DisjointStencilIndex, their sub-steps
 * for multi-pass rendering (stencil-then-cover, etc.) can be intermingled with each other and
 * produce the same results as if each draw's sub-steps were executed in order before moving on to
 * the next draw's.
 * <p>
 * Ordering within a set can be entirely arbitrary (i.e. all stencil steps can go before all cover
 * steps). Ordering between sets is also arbitrary since all draws share the same
 * CompressedPaintersOrder, so long as one set is entirely drawn before the next.
 * <p>
 * Two draws that have different CompressedPaintersOrders but the same DisjointStencilIndex are
 * unrelated, they may or may not overlap. The painters order scopes the disjoint sets.
 * <p>
 * Every draw has an associated depth value. The value is constant across the entire draw and is
 * not related to any varying Z coordinate induced by a 4x4 transform. The painter's depth is stored
 * in the depth attachment and the GREATER depth test is used to reject or accept pixels/samples
 * relative to what has already been rendered into the depth attachment. This allows draws that do
 * not depend on the previous color to be radically re-ordered relative to their original painter's
 * order while producing correct results.
 */
public class DrawOrder {

    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 0xFFFF;

    // The first PaintersDepth is reserved for clearing the depth attachment; any draw using this
    // depth will always fail the depth test.
    public static final int CLEAR_DEPTH = MIN_VALUE;
    // The first CompressedPaintersOrder is reserved to indicate there is no previous draw that
    // must come before a draw.
    public static final int NO_INTERSECTION = MIN_VALUE;

    // 32-48 bits: CompressedPaintersOrder
    // 16-32 bits: DisjointStencilIndex
    // 0-16 bits: PaintersDepth
    public static final int PAINTERS_ORDER_SHIFT = 32;
    public static final int STENCIL_INDEX_SHIFT = 16;
    public static final int DEPTH_SHIFT = 0;
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
        return (packedDrawOrder & 0x00000000_FFFFFFFFL) | ((long) order << PAINTERS_ORDER_SHIFT);
    }

    public static long updateWithStencilIndex(long packedDrawOrder, int disjointSet) {
        assert getStencilIndex(packedDrawOrder) == MIN_VALUE;
        return packedDrawOrder | ((long) disjointSet << STENCIL_INDEX_SHIFT);
    }
}
