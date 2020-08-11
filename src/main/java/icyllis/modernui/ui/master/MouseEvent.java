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

package icyllis.modernui.ui.master;

/**
 * Dispatch of all events shares the same instance.
 * Don't change the inner logic.
 */
public final class MouseEvent {

    /*
     * Dynamic action represents high frequency events
     *
     * @see #ACTION_MOVE
     * @see #ACTION_DRAG
     * @see #ACTION_HOVER_MOVE
     * @see #ACTION_SCROLL
     */
    //public static final int TYPE_DYNAMIC = 1;

    /*
     * Operation action represents low frequency events
     *
     * @see #ACTION_PRESS
     * @see #ACTION_RELEASE
     * @see #ACTION_CLICK
     * @see #ACTION_DOUBLE_CLICK
     */
    //public static final int TYPE_OPERATION = 2;

    /*
     * Notify action represents having view states updated
     *
     * @see #ACTION_HOVER_ENTER
     * @see #ACTION_TREE_ENTER
     * @see #ACTION_HOVER_EXIT
     * @see #ACTION_TREE_EXIT
     */
    //public static final int TYPE_NOTIFY = 3;

    // EXT
    //private static final int TYPE_SHIFT = 3;

    public static final int ACTION_MOVE       = 1;
    public static final int ACTION_DRAG       = 2;
    public static final int ACTION_SCROLL     = 3;

    public static final int ACTION_PRESS        = 4;
    public static final int ACTION_RELEASE      = 5;
    public static final int ACTION_CLICK        = 6;
    public static final int ACTION_DOUBLE_CLICK = 7;

    /*public static final int ACTION_HOVER_ENTER = TYPE_NOTIFY << TYPE_SHIFT;
    public static final int ACTION_TREE_ENTER  = (TYPE_NOTIFY << TYPE_SHIFT) + 1;
    public static final int ACTION_HOVER_EXIT  = (TYPE_NOTIFY << TYPE_SHIFT) + 2;
    public static final int ACTION_TREE_EXIT   = (TYPE_NOTIFY << TYPE_SHIFT) + 3;*/

    int action;

    double x;
    double y;

    double rawX;
    double rawY;

    int button;

    double scrollDelta;

    // singleton, created by system
    MouseEvent() {

    }

    /*
     * Returns the action type of this event
     *
     * @return either {@link #TYPE_DYNAMIC} or {@link #TYPE_OPERATION} or {@link #TYPE_NOTIFY}
     */
    /*public int getType() {
        return action >> TYPE_SHIFT;
    }*/

    /**
     * Returns the action of this event
     *
     * @return action, such as {@link #ACTION_PRESS}
     */
    public int getAction() {
        return action;
    }

    /**
     * Returns the X coordinate of this event,
     *
     * @return X
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the Y coordinate of this event,
     *
     * @return Y
     */
    public double getY() {
        return y;
    }

    /**
     * Returns the original raw X coordinate of this event.  For touch
     * events on the screen, this is the original location of the event
     * on the screen, before it had been adjusted for the containing window
     * and views.
     *
     * @return raw X
     */
    public double getRawX() {
        return rawX;
    }

    /**
     * Returns the original raw Y coordinate of this event.  For touch
     * events on the screen, this is the original location of the event
     * on the screen, before it had been adjusted for the containing window
     * and views.
     *
     * @return raw Y
     */
    public double getRawY() {
        return rawY;
    }

    /**
     * Returns the mouse button of this event.
     *
     * @return mouse button
     * @see org.lwjgl.glfw.GLFW
     */
    public int getButton() {
        return button;
    }

    public double getScrollDelta() {
        return scrollDelta;
    }
}
