/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.view;

import javax.annotation.Nonnull;

/**
 * The position and size relationship of objects in containers.
 */
@SuppressWarnings("unused")
public class Gravity {

    /**
     * Raw bit indicating the gravity for an axis has been specified.
     */
    public static final int AXIS_SPECIFIED = 0x0001;


    /**
     * Raw bit controlling how the left/top edge is placed.
     */
    public static final int AXIS_PULL_BEFORE = 0x0002;
    /**
     * Raw bit controlling how the right/bottom edge is placed.
     */
    public static final int AXIS_PULL_AFTER  = 0x0004;
    /**
     * Raw bit controlling whether the right/bottom edge is clipped to its
     * container, based on the gravity direction being applied.
     */
    public static final int AXIS_CLIP        = 0x0008;


    /**
     * Bits defining the horizontal axis.
     */
    public static final int AXIS_X_SHIFT = 0;
    /**
     * Bits defining the vertical axis.
     */
    public static final int AXIS_Y_SHIFT = 4;


    /**
     * Push object to the top of its container, not changing its size.
     */
    public static final int TOP    = (AXIS_PULL_BEFORE | AXIS_SPECIFIED) << AXIS_Y_SHIFT;
    /**
     * Push object to the bottom of its container, not changing its size.
     */
    public static final int BOTTOM = (AXIS_PULL_AFTER | AXIS_SPECIFIED) << AXIS_Y_SHIFT;
    /**
     * Push object to the left of its container, not changing its size.
     */
    public static final int LEFT   = (AXIS_PULL_BEFORE | AXIS_SPECIFIED) << AXIS_X_SHIFT;
    /**
     * Push object to the right of its container, not changing its size.
     */
    public static final int RIGHT  = (AXIS_PULL_AFTER | AXIS_SPECIFIED) << AXIS_X_SHIFT;


    /**
     * Push object to the top left of its container, not changing its size.
     */
    public static final int TOP_LEFT     = TOP | LEFT;
    /**
     * Push object to the top right of its container, not changing its size.
     */
    public static final int TOP_RIGHT    = TOP | RIGHT;
    /**
     * Push object to the bottom left of its container, not changing its size.
     */
    public static final int BOTTOM_LEFT  = BOTTOM | LEFT;
    /**
     * Push object to the bottom right of its container, not changing its size.
     */
    public static final int BOTTOM_RIGHT = BOTTOM | RIGHT;


    /**
     * Place object in the vertical center of its container, not changing its
     * size.
     */
    public static final int VERTICAL_CENTER = AXIS_SPECIFIED << AXIS_Y_SHIFT;
    /**
     * Place object in the vertical center left of its container, not changing
     * its size.
     */
    public static final int LEFT_CENTER     = LEFT | VERTICAL_CENTER;
    /**
     * Place object in the vertical center right of its container, not changing
     * its size.
     */
    public static final int RIGHT_CENTER    = RIGHT | VERTICAL_CENTER;
    /**
     * Grow the vertical size of the object if needed so it completely fills
     * its container.
     */
    public static final int FILL_VERTICAL   = TOP | BOTTOM;


    /**
     * Place object in the horizontal center of its container, not changing its
     * size.
     */
    public static final int HORIZONTAL_CENTER = AXIS_SPECIFIED << AXIS_X_SHIFT;
    /**
     * Place object in the top horizontal center of its container, not changing
     * its size.
     */
    public static final int TOP_CENTER        = TOP | HORIZONTAL_CENTER;
    /**
     * Place object in the bottom horizontal center of its container, not changing
     * its size.
     */
    public static final int BOTTOM_CENTER     = BOTTOM | HORIZONTAL_CENTER;
    /**
     * Grow the horizontal size of the object if needed so it completely fills
     * its container.
     */
    public static final int FILL_HORIZONTAL   = LEFT | RIGHT;


    /**
     * Place the object in the center of its container in both the vertical
     * and horizontal axis, not changing its size.
     */
    public static final int CENTER = VERTICAL_CENTER | HORIZONTAL_CENTER;
    /**
     * Grow the horizontal and vertical size of the object if needed so it
     * completely fills its container.
     */
    public static final int FILL   = FILL_VERTICAL | FILL_HORIZONTAL;


    /**
     * Flag to clip the edges of the object to its container along the
     * vertical axis.
     */
    public static final int CLIP_VERTICAL   = AXIS_CLIP << AXIS_Y_SHIFT;
    /**
     * Flag to clip the edges of the object to its container along the
     * horizontal axis.
     */
    public static final int CLIP_HORIZONTAL = AXIS_CLIP << AXIS_X_SHIFT;


    /**
     * Binary mask to get the absolute horizontal gravity of a gravity.
     */
    public static final int HORIZONTAL_GRAVITY_MASK = (AXIS_SPECIFIED |
            AXIS_PULL_BEFORE | AXIS_PULL_AFTER) << AXIS_X_SHIFT;
    /**
     * Binary mask to get the vertical gravity of a gravity.
     */
    public static final int VERTICAL_GRAVITY_MASK   = (AXIS_SPECIFIED |
            AXIS_PULL_BEFORE | AXIS_PULL_AFTER) << AXIS_Y_SHIFT;


    /**
     * Apply a gravity constant to an object.
     *
     * @param gravity   The desired placement of the object, as defined by the
     *                  constants in this class.
     * @param w         The horizontal size of the object.
     * @param h         The vertical size of the object.
     * @param container The frame of the containing space, in which the object
     *                  will be placed.  Should be large enough to contain the
     *                  width and height of the object. (L,T,R,B)
     * @param xAdj      Offset to apply to the X axis.  If gravity is LEFT this
     *                  pushes it to the right; if gravity is RIGHT it pushes it to
     *                  the left; if gravity is CENTER_HORIZONTAL it pushes it to the
     *                  right or left; otherwise it is ignored.
     * @param yAdj      Offset to apply to the Y axis.  If gravity is TOP this pushes
     *                  it down; if gravity is BOTTOM it pushes it up; if gravity is
     *                  CENTER_VERTICAL it pushes it down or up; otherwise it is
     *                  ignored.
     * @param outRect   Receives the computed frame of the object in its
     *                  container. (L,T,R,B)
     */
    public static void apply(int gravity, int w, int h, @Nonnull int[] container,
                             int xAdj, int yAdj, int[] outRect) {
        if (container.length < 4 || outRect.length < 4) {
            throw new IllegalArgumentException();
        }
        switch (gravity & ((AXIS_PULL_BEFORE | AXIS_PULL_AFTER) << AXIS_X_SHIFT)) {
            case 0:
                outRect[0] = container[0]
                        + ((container[2] - container[0] - w) / 2) + xAdj;
                outRect[2] = outRect[0] + w;
                if ((gravity & (AXIS_CLIP << AXIS_X_SHIFT))
                        == (AXIS_CLIP << AXIS_X_SHIFT)) {
                    if (outRect[0] < container[0]) {
                        outRect[0] = container[0];
                    }
                    if (outRect[2] > container[2]) {
                        outRect[2] = container[2];
                    }
                }
                break;
            case AXIS_PULL_BEFORE << AXIS_X_SHIFT:
                outRect[0] = container[0] + xAdj;
                outRect[2] = outRect[0] + w;
                if ((gravity & (AXIS_CLIP << AXIS_X_SHIFT))
                        == (AXIS_CLIP << AXIS_X_SHIFT)) {
                    if (outRect[2] > container[2]) {
                        outRect[2] = container[2];
                    }
                }
                break;
            case AXIS_PULL_AFTER << AXIS_X_SHIFT:
                outRect[2] = container[2] - xAdj;
                outRect[0] = outRect[2] - w;
                if ((gravity & (AXIS_CLIP << AXIS_X_SHIFT))
                        == (AXIS_CLIP << AXIS_X_SHIFT)) {
                    if (outRect[0] < container[0]) {
                        outRect[0] = container[0];
                    }
                }
                break;
            default:
                outRect[0] = container[0] + xAdj;
                outRect[2] = container[2] + xAdj;
                break;
        }

        switch (gravity & ((AXIS_PULL_BEFORE | AXIS_PULL_AFTER) << AXIS_Y_SHIFT)) {
            case 0:
                outRect[1] = container[1]
                        + ((container[3] - container[1] - h) / 2) + yAdj;
                outRect[3] = outRect[1] + h;
                if ((gravity & (AXIS_CLIP << AXIS_Y_SHIFT))
                        == (AXIS_CLIP << AXIS_Y_SHIFT)) {
                    if (outRect[1] < container[1]) {
                        outRect[1] = container[1];
                    }
                    if (outRect[3] > container[3]) {
                        outRect[3] = container[3];
                    }
                }
                break;
            case AXIS_PULL_BEFORE << AXIS_Y_SHIFT:
                outRect[1] = container[1] + yAdj;
                outRect[3] = outRect[1] + h;
                if ((gravity & (AXIS_CLIP << AXIS_Y_SHIFT))
                        == (AXIS_CLIP << AXIS_Y_SHIFT)) {
                    if (outRect[3] > container[3]) {
                        outRect[3] = container[3];
                    }
                }
                break;
            case AXIS_PULL_AFTER << AXIS_Y_SHIFT:
                outRect[3] = container[3] - yAdj;
                outRect[1] = outRect[3] - h;
                if ((gravity & (AXIS_CLIP << AXIS_Y_SHIFT))
                        == (AXIS_CLIP << AXIS_Y_SHIFT)) {
                    if (outRect[1] < container[1]) {
                        outRect[1] = container[1];
                    }
                }
                break;
            default:
                outRect[1] = container[1] + yAdj;
                outRect[3] = container[3] + yAdj;
                break;
        }
    }
}
