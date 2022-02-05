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
 * Constants to be used to perform haptic feedback effects via
 * {@link View#performHapticFeedback(int)}
 */
public final class HapticFeedbackConstants {

    private HapticFeedbackConstants() {
    }

    /**
     * The user has performed a long press on an object that is resulting
     * in an action being performed.
     */
    public static final int LONG_PRESS = 0;

    /**
     * The user has pressed on a virtual on-screen key.
     */
    public static final int VIRTUAL_KEY = 1;

    /**
     * The user has pressed a soft keyboard key.
     */
    public static final int KEYBOARD_TAP = 3;

    /**
     * The user has pressed either an hour or minute tick of a Clock.
     */
    public static final int CLOCK_TICK = 4;

    /**
     * The user has pressed either a day or month or year date of a Calendar.
     *
     * @hide
     */
    public static final int CALENDAR_DATE = 5;

    /**
     * The user has performed a context click on an object.
     */
    public static final int CONTEXT_CLICK = 6;

    /**
     * The user has pressed a virtual or software keyboard key.
     */
    public static final int KEYBOARD_PRESS = KEYBOARD_TAP;

    /**
     * The user has released a virtual keyboard key.
     */
    public static final int KEYBOARD_RELEASE = 7;

    /**
     * The user has released a virtual key.
     */
    public static final int VIRTUAL_KEY_RELEASE = 8;

    /**
     * The user has performed a selection/insertion handle move on text field.
     */
    public static final int TEXT_HANDLE_MOVE = 9;

    /**
     * The user unlocked the device
     *
     * @hide
     */
    public static final int ENTRY_BUMP = 10;

    /**
     * The user has moved the dragged object within a droppable area.
     *
     * @hide
     */
    public static final int DRAG_CROSSING = 11;

    /**
     * The user has started a gesture (e.g. on the soft keyboard).
     */
    public static final int GESTURE_START = 12;

    /**
     * The user has finished a gesture (e.g. on the soft keyboard).
     */
    public static final int GESTURE_END = 13;

    /**
     * The user's squeeze crossed the gesture's initiation threshold.
     *
     * @hide
     */
    public static final int EDGE_SQUEEZE = 14;

    /**
     * The user's squeeze crossed the gesture's release threshold.
     *
     * @hide
     */
    public static final int EDGE_RELEASE = 15;

    /**
     * A haptic effect to signal the confirmation or successful completion of a user
     * interaction.
     */
    public static final int CONFIRM = 16;

    /**
     * A haptic effect to signal the rejection or failure of a user interaction.
     */
    public static final int REJECT = 17;

    /**
     * The phone has booted with safe mode enabled.
     * This is a private constant.  Feel free to renumber as desired.
     *
     * @hide
     */
    public static final int SAFE_MODE_ENABLED = 10001;

    /**
     * Invocation of the voice assistant via hardware button.
     *
     * @hide
     */
    public static final int ASSISTANT_BUTTON = 10002;

    /**
     * Flag for {@link View#performHapticFeedback(int, int)
     * View.performHapticFeedback(int, int)}: Ignore the setting in the
     * view for whether to perform haptic feedback, do it always.
     */
    public static final int FLAG_IGNORE_VIEW_SETTING = 0x0001;

    /**
     * Flag for {@link View#performHapticFeedback(int, int)
     * View.performHapticFeedback(int, int)}: Ignore the global setting
     * for whether to perform haptic feedback, do it always.
     */
    public static final int FLAG_IGNORE_GLOBAL_SETTING = 0x0002;
}
