/*
 * ModernUI.
 * Copyright (C) 2020-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.view;

import icyllis.modernui.graphics.Rect;
import org.jspecify.annotations.NonNull;

/**
 * Standard constants and tools for placing an object within a potentially
 * larger container.
 *
 * @deprecated use {@link icyllis.modernui.graphics.Gravity} since 3.14
 */
@Deprecated
public class Gravity {

    /**
     * Constant indicating that no gravity has been set
     */
    public static final int NO_GRAVITY = 0x0000;

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
    public static final int AXIS_PULL_AFTER = 0x0004;
    /**
     * Raw bit controlling whether the right/bottom edge is clipped to its
     * container, based on the gravity direction being applied.
     */
    public static final int AXIS_CLIP = 0x0008;

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
    public static final int TOP = (AXIS_PULL_BEFORE | AXIS_SPECIFIED) << AXIS_Y_SHIFT;
    /**
     * Push object to the bottom of its container, not changing its size.
     */
    public static final int BOTTOM = (AXIS_PULL_AFTER | AXIS_SPECIFIED) << AXIS_Y_SHIFT;
    /**
     * Push object to the left of its container, not changing its size.
     */
    public static final int LEFT = (AXIS_PULL_BEFORE | AXIS_SPECIFIED) << AXIS_X_SHIFT;
    /**
     * Push object to the right of its container, not changing its size.
     */
    public static final int RIGHT = (AXIS_PULL_AFTER | AXIS_SPECIFIED) << AXIS_X_SHIFT;

    /**
     * Place object in the vertical center of its container, not changing its
     * size.
     */
    public static final int CENTER_VERTICAL = AXIS_SPECIFIED << AXIS_Y_SHIFT;
    /**
     * Grow the vertical size of the object if needed so it completely fills
     * its container.
     */
    public static final int FILL_VERTICAL = TOP | BOTTOM;

    /**
     * Place object in the horizontal center of its container, not changing its
     * size.
     */
    public static final int CENTER_HORIZONTAL = AXIS_SPECIFIED << AXIS_X_SHIFT;
    /**
     * Grow the horizontal size of the object if needed so it completely fills
     * its container.
     */
    public static final int FILL_HORIZONTAL = LEFT | RIGHT;

    /**
     * Place the object in the center of its container in both the vertical
     * and horizontal axis, not changing its size.
     */
    public static final int CENTER = CENTER_VERTICAL | CENTER_HORIZONTAL;

    /**
     * Grow the horizontal and vertical size of the object if needed so it
     * completely fills its container.
     */
    public static final int FILL = FILL_VERTICAL | FILL_HORIZONTAL;

    /**
     * Flag to clip the edges of the object to its container along the
     * vertical axis.
     */
    public static final int CLIP_VERTICAL = AXIS_CLIP << AXIS_Y_SHIFT;

    /**
     * Flag to clip the edges of the object to its container along the
     * horizontal axis.
     */
    public static final int CLIP_HORIZONTAL = AXIS_CLIP << AXIS_X_SHIFT;

    /**
     * Raw bit controlling whether the layout direction is relative or not (START/END instead of
     * absolute LEFT/RIGHT).
     */
    public static final int RELATIVE_LAYOUT_DIRECTION = 0x00800000;

    /**
     * Binary mask to get the absolute horizontal gravity of a gravity.
     */
    public static final int HORIZONTAL_GRAVITY_MASK = (AXIS_SPECIFIED |
            AXIS_PULL_BEFORE | AXIS_PULL_AFTER) << AXIS_X_SHIFT;
    /**
     * Binary mask to get the vertical gravity of a gravity.
     */
    public static final int VERTICAL_GRAVITY_MASK = (AXIS_SPECIFIED |
            AXIS_PULL_BEFORE | AXIS_PULL_AFTER) << AXIS_Y_SHIFT;

    /**
     * Special constant to enable clipping to an overall display along the
     * vertical dimension.  This is not applied by default by
     * {@link #apply(int, int, int, Rect, int, int, Rect)}; you must do so
     * yourself by calling {@link #applyDisplay}.
     */
    public static final int DISPLAY_CLIP_VERTICAL = 0x10000000;

    /**
     * Special constant to enable clipping to an overall display along the
     * horizontal dimension.  This is not applied by default by
     * {@link #apply(int, int, int, Rect, int, int, Rect)}; you must do so
     * yourself by calling {@link #applyDisplay}.
     */
    public static final int DISPLAY_CLIP_HORIZONTAL = 0x01000000;

    /**
     * Push object to x-axis position at the start of its container, not changing its size.
     */
    public static final int START = RELATIVE_LAYOUT_DIRECTION | LEFT;

    /**
     * Push object to x-axis position at the end of its container, not changing its size.
     */
    public static final int END = RELATIVE_LAYOUT_DIRECTION | RIGHT;

    /**
     * Binary mask for the horizontal gravity and script specific direction bit.
     */
    public static final int RELATIVE_HORIZONTAL_GRAVITY_MASK = START | END;

    /**
     * Apply a gravity constant to an object. This supposes that the layout direction is LTR.
     *
     * @param gravity   The desired placement of the object, as defined by the
     *                  constants in this class.
     * @param w         The horizontal size of the object.
     * @param h         The vertical size of the object.
     * @param container The frame of the containing space, in which the object
     *                  will be placed.  Should be large enough to contain the
     *                  width and height of the object.
     * @param outRect   Receives the computed frame of the object in its
     *                  container.
     */
    public static void apply(int gravity, int w, int h, @NonNull Rect container,
                             @NonNull Rect outRect) {
        apply(gravity, w, h, container, 0, 0, outRect);
    }

    /**
     * Apply a gravity constant to an object and take care if layout direction is RTL or not.
     *
     * @param gravity         The desired placement of the object, as defined by the
     *                        constants in this class.
     * @param w               The horizontal size of the object.
     * @param h               The vertical size of the object.
     * @param container       The frame of the containing space, in which the object
     *                        will be placed.  Should be large enough to contain the
     *                        width and height of the object.
     * @param outRect         Receives the computed frame of the object in its
     *                        container.
     * @param layoutDirection The layout direction.
     * @see View#LAYOUT_DIRECTION_LTR
     * @see View#LAYOUT_DIRECTION_RTL
     */
    public static void apply(int gravity, int w, int h, @NonNull Rect container,
                             @NonNull Rect outRect, int layoutDirection) {
        int absGravity = getAbsoluteGravity(gravity, layoutDirection);
        apply(absGravity, w, h, container, 0, 0, outRect);
    }

    /**
     * Apply a gravity constant to an object.
     *
     * @param gravity   The desired placement of the object, as defined by the
     *                  constants in this class.
     * @param w         The horizontal size of the object.
     * @param h         The vertical size of the object.
     * @param container The frame of the containing space, in which the object
     *                  will be placed.  Should be large enough to contain the
     *                  width and height of the object.
     * @param xAdj      Offset to apply to the X axis.  If gravity is LEFT this
     *                  pushes it to the right; if gravity is RIGHT it pushes it to
     *                  the left; if gravity is CENTER_HORIZONTAL it pushes it to the
     *                  right or left; otherwise it is ignored.
     * @param yAdj      Offset to apply to the Y axis.  If gravity is TOP this pushes
     *                  it down; if gravity is BOTTOM it pushes it up; if gravity is
     *                  CENTER_VERTICAL it pushes it down or up; otherwise it is
     *                  ignored.
     * @param outRect   Receives the computed frame of the object in its
     *                  container.
     */
    public static void apply(int gravity, int w, int h, @NonNull Rect container,
                             int xAdj, int yAdj, @NonNull Rect outRect) {
        icyllis.modernui.graphics.Gravity.apply(gravity, w, h, container, xAdj, yAdj, outRect);
    }

    /**
     * Apply a gravity constant to an object.
     *
     * @param gravity         The desired placement of the object, as defined by the
     *                        constants in this class.
     * @param w               The horizontal size of the object.
     * @param h               The vertical size of the object.
     * @param container       The frame of the containing space, in which the object
     *                        will be placed.  Should be large enough to contain the
     *                        width and height of the object.
     * @param xAdj            Offset to apply to the X axis.  If gravity is LEFT this
     *                        pushes it to the right; if gravity is RIGHT it pushes it to
     *                        the left; if gravity is CENTER_HORIZONTAL it pushes it to the
     *                        right or left; otherwise it is ignored.
     * @param yAdj            Offset to apply to the Y axis.  If gravity is TOP this pushes
     *                        it down; if gravity is BOTTOM it pushes it up; if gravity is
     *                        CENTER_VERTICAL it pushes it down or up; otherwise it is
     *                        ignored.
     * @param outRect         Receives the computed frame of the object in its
     *                        container.
     * @param layoutDirection The layout direction.
     * @see View#LAYOUT_DIRECTION_LTR
     * @see View#LAYOUT_DIRECTION_RTL
     */
    public static void apply(int gravity, int w, int h, @NonNull Rect container,
                             int xAdj, int yAdj, @NonNull Rect outRect, int layoutDirection) {
        int absGravity = getAbsoluteGravity(gravity, layoutDirection);
        apply(absGravity, w, h, container, xAdj, yAdj, outRect);
    }

    /**
     * Apply additional gravity behavior based on the overall "display" that an
     * object exists in.  This can be used after
     * {@link #apply(int, int, int, Rect, int, int, Rect)} to place the object
     * within a visible display.  By default this moves or clips the object
     * to be visible in the display; the gravity flags
     * {@link #DISPLAY_CLIP_HORIZONTAL} and {@link #DISPLAY_CLIP_VERTICAL}
     * can be used to change this behavior.
     *
     * @param gravity  Gravity constants to modify the placement within the
     *                 display.
     * @param display  The rectangle of the display in which the object is
     *                 being placed.
     * @param inoutObj Supplies the current object position; returns with it
     *                 modified if needed to fit in the display.
     */
    public static void applyDisplay(int gravity, @NonNull Rect display, @NonNull Rect inoutObj) {
        icyllis.modernui.graphics.Gravity.applyDisplay(gravity, display, inoutObj);
    }

    /**
     * Apply additional gravity behavior based on the overall "display" that an
     * object exists in.  This can be used after
     * {@link #apply(int, int, int, Rect, int, int, Rect)} to place the object
     * within a visible display.  By default this moves or clips the object
     * to be visible in the display; the gravity flags
     * {@link #DISPLAY_CLIP_HORIZONTAL} and {@link #DISPLAY_CLIP_VERTICAL}
     * can be used to change this behavior.
     *
     * @param gravity         Gravity constants to modify the placement within the
     *                        display.
     * @param display         The rectangle of the display in which the object is
     *                        being placed.
     * @param inoutObj        Supplies the current object position; returns with it
     *                        modified if needed to fit in the display.
     * @param layoutDirection The layout direction.
     * @see View#LAYOUT_DIRECTION_LTR
     * @see View#LAYOUT_DIRECTION_RTL
     */
    public static void applyDisplay(int gravity, @NonNull Rect display, @NonNull Rect inoutObj,
                                    int layoutDirection) {
        int absGravity = getAbsoluteGravity(gravity, layoutDirection);
        applyDisplay(absGravity, display, inoutObj);
    }

    /**
     * <p>Indicate whether the supplied gravity has a vertical pull.</p>
     *
     * @param gravity the gravity to check for vertical pull
     * @return true if the supplied gravity has a vertical pull
     */
    public static boolean isVertical(int gravity) {
        return gravity > 0 && (gravity & VERTICAL_GRAVITY_MASK) != 0;
    }

    /**
     * <p>Indicate whether the supplied gravity has an horizontal pull.</p>
     *
     * @param gravity the gravity to check for horizontal pull
     * @return true if the supplied gravity has an horizontal pull
     */
    public static boolean isHorizontal(int gravity) {
        return gravity > 0 && (gravity & RELATIVE_HORIZONTAL_GRAVITY_MASK) != 0;
    }

    /**
     * <p>Convert script specific gravity to absolute horizontal value.</p>
     * <p>
     * if horizontal direction is LTR, then START will set LEFT and END will set RIGHT.
     * if horizontal direction is RTL, then START will set RIGHT and END will set LEFT.
     *
     * @param gravity         The gravity to convert to absolute (horizontal) values.
     * @param layoutDirection The layout direction.
     * @return gravity converted to absolute (horizontal) values.
     */
    public static int getAbsoluteGravity(int gravity, int layoutDirection) {
        return icyllis.modernui.graphics.Gravity.getAbsoluteGravity(gravity, layoutDirection);
    }
}
