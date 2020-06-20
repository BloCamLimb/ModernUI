/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.layout;

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

}
