/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui;

/**
 * GPU hierarchical clipping using stencil test.
 */
public class ClipStack {

    /**
     * Clip ops.
     */
    public static final byte
            OP_DIFFERENCE = 0,  // target minus operand
            OP_INTERSECT = 1;   // target intersected with operand

    /**
     * Clip states.
     */
    public static final byte
            STATE_EMPTY = 0,
            STATE_WIDE_OPEN = 1,
            STATE_DEVICE_RECT = 2,
            STATE_DEVICE_ROUND_RECT = 3,
            STATE_COMPLEX = 4;

    public static final class Clip {

        final Rect mShape = new Rect();

        // model view matrix
        final Matrix4 mMatrix = Matrix4.identity();
    }
}
