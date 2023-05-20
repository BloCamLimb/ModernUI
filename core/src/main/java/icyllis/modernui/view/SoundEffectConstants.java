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

package icyllis.modernui.view;

/**
 * Constants to be used to play sound effects via {@link View#playSoundEffect(int)}
 */
public final class SoundEffectConstants {

    private SoundEffectConstants() {
    }

    public static final int CLICK = 0;

    /**
     * Effect id for a navigation left
     */
    public static final int NAVIGATION_LEFT = 1;
    /**
     * Effect id for a navigation up
     */
    public static final int NAVIGATION_UP = 2;
    /**
     * Effect id for a navigation right
     */
    public static final int NAVIGATION_RIGHT = 3;
    /**
     * Effect id for a navigation down
     */
    public static final int NAVIGATION_DOWN = 4;
    /**
     * Effect id for a repeatedly triggered navigation left, e.g. due to long pressing a button
     */
    public static final int NAVIGATION_REPEAT_LEFT = 5;
    /**
     * Effect id for a repeatedly triggered navigation up, e.g. due to long pressing a button
     */
    public static final int NAVIGATION_REPEAT_UP = 6;
    /**
     * Effect id for a repeatedly triggered navigation right, e.g. due to long pressing a button
     */
    public static final int NAVIGATION_REPEAT_RIGHT = 7;
    /**
     * Effect id for a repeatedly triggered navigation down, e.g. due to long pressing a button
     */
    public static final int NAVIGATION_REPEAT_DOWN = 8;

    /**
     * Get the sonification constant for the focus directions.
     *
     * @param direction The direction of the focus.
     * @return The appropriate sonification constant.
     * @throws IllegalArgumentException when the passed direction is not one of the
     *                                  documented values.
     */
    public static int getContantForFocusDirection(@View.FocusDirection int direction) {
        return switch (direction) {
            case View.FOCUS_RIGHT -> SoundEffectConstants.NAVIGATION_RIGHT;
            case View.FOCUS_FORWARD, View.FOCUS_DOWN -> SoundEffectConstants.NAVIGATION_DOWN;
            case View.FOCUS_LEFT -> SoundEffectConstants.NAVIGATION_LEFT;
            case View.FOCUS_BACKWARD, View.FOCUS_UP -> SoundEffectConstants.NAVIGATION_UP;
            default -> throw new IllegalArgumentException("direction must be one of "
                    + "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, FOCUS_FORWARD, FOCUS_BACKWARD}.");
        };
    }
}
