/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

import icyllis.modernui.util.Pools;
import it.unimi.dsi.fastutil.ints.Int2BooleanArrayMap;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.Platform;

import javax.annotation.Nonnull;

/**
 * Object that reports key events (keyboard etc.).
 * Modified for desktop application.
 */
@SuppressWarnings("unused")
public class KeyEvent extends InputEvent {

    public static final int KEY_UNKNOWN = GLFW.GLFW_KEY_UNKNOWN;

    public static final int KEY_SPACE = GLFW.GLFW_KEY_SPACE;
    public static final int KEY_APOSTROPHE = GLFW.GLFW_KEY_APOSTROPHE;
    public static final int KEY_COMMA = GLFW.GLFW_KEY_COMMA;
    public static final int KEY_MINUS = GLFW.GLFW_KEY_MINUS;
    public static final int KEY_PERIOD = GLFW.GLFW_KEY_PERIOD;
    public static final int KEY_SLASH = GLFW.GLFW_KEY_SLASH;
    public static final int KEY_0 = GLFW.GLFW_KEY_0;
    public static final int KEY_1 = GLFW.GLFW_KEY_1;
    public static final int KEY_2 = GLFW.GLFW_KEY_2;
    public static final int KEY_3 = GLFW.GLFW_KEY_3;
    public static final int KEY_4 = GLFW.GLFW_KEY_4;
    public static final int KEY_5 = GLFW.GLFW_KEY_5;
    public static final int KEY_6 = GLFW.GLFW_KEY_6;
    public static final int KEY_7 = GLFW.GLFW_KEY_7;
    public static final int KEY_8 = GLFW.GLFW_KEY_8;
    public static final int KEY_9 = GLFW.GLFW_KEY_9;
    public static final int KEY_SEMICOLON = GLFW.GLFW_KEY_SEMICOLON;
    public static final int KEY_EQUAL = GLFW.GLFW_KEY_EQUAL;
    public static final int KEY_A = GLFW.GLFW_KEY_A;
    public static final int KEY_B = GLFW.GLFW_KEY_B;
    public static final int KEY_C = GLFW.GLFW_KEY_C;
    public static final int KEY_D = GLFW.GLFW_KEY_D;
    public static final int KEY_E = GLFW.GLFW_KEY_E;
    public static final int KEY_F = GLFW.GLFW_KEY_F;
    public static final int KEY_G = GLFW.GLFW_KEY_G;
    public static final int KEY_H = GLFW.GLFW_KEY_H;
    public static final int KEY_I = GLFW.GLFW_KEY_I;
    public static final int KEY_J = GLFW.GLFW_KEY_J;
    public static final int KEY_K = GLFW.GLFW_KEY_K;
    public static final int KEY_L = GLFW.GLFW_KEY_L;
    public static final int KEY_M = GLFW.GLFW_KEY_M;
    public static final int KEY_N = GLFW.GLFW_KEY_N;
    public static final int KEY_O = GLFW.GLFW_KEY_O;
    public static final int KEY_P = GLFW.GLFW_KEY_P;
    public static final int KEY_Q = GLFW.GLFW_KEY_Q;
    public static final int KEY_R = GLFW.GLFW_KEY_R;
    public static final int KEY_S = GLFW.GLFW_KEY_S;
    public static final int KEY_T = GLFW.GLFW_KEY_T;
    public static final int KEY_U = GLFW.GLFW_KEY_U;
    public static final int KEY_V = GLFW.GLFW_KEY_V;
    public static final int KEY_W = GLFW.GLFW_KEY_W;
    public static final int KEY_X = GLFW.GLFW_KEY_X;
    public static final int KEY_Y = GLFW.GLFW_KEY_Y;
    public static final int KEY_Z = GLFW.GLFW_KEY_Z;
    public static final int KEY_LEFT_BRACKET = GLFW.GLFW_KEY_LEFT_BRACKET;
    public static final int KEY_BACKSLASH = GLFW.GLFW_KEY_BACKSLASH;
    public static final int KEY_RIGHT_BRACKET = GLFW.GLFW_KEY_RIGHT_BRACKET;
    public static final int KEY_GRAVE_ACCENT = GLFW.GLFW_KEY_GRAVE_ACCENT;
    public static final int KEY_WORLD_1 = GLFW.GLFW_KEY_WORLD_1;
    public static final int KEY_WORLD_2 = GLFW.GLFW_KEY_WORLD_2;

    public static final int KEY_ESCAPE = GLFW.GLFW_KEY_ESCAPE;
    public static final int KEY_ENTER = GLFW.GLFW_KEY_ENTER;
    public static final int KEY_TAB = GLFW.GLFW_KEY_TAB;
    public static final int KEY_BACKSPACE = GLFW.GLFW_KEY_BACKSPACE;
    public static final int KEY_INSERT = GLFW.GLFW_KEY_INSERT;
    public static final int KEY_DELETE = GLFW.GLFW_KEY_DELETE;
    public static final int KEY_RIGHT = GLFW.GLFW_KEY_RIGHT;
    public static final int KEY_LEFT = GLFW.GLFW_KEY_LEFT;
    public static final int KEY_DOWN = GLFW.GLFW_KEY_DOWN;
    public static final int KEY_UP = GLFW.GLFW_KEY_UP;
    public static final int KEY_PAGE_UP = GLFW.GLFW_KEY_PAGE_UP;
    public static final int KEY_PAGE_DOWN = GLFW.GLFW_KEY_PAGE_DOWN;
    public static final int KEY_HOME = GLFW.GLFW_KEY_HOME;
    public static final int KEY_END = GLFW.GLFW_KEY_END;
    public static final int KEY_CAPS_LOCK = GLFW.GLFW_KEY_CAPS_LOCK;
    public static final int KEY_SCROLL_LOCK = GLFW.GLFW_KEY_SCROLL_LOCK;
    public static final int KEY_NUM_LOCK = GLFW.GLFW_KEY_NUM_LOCK;
    public static final int KEY_PRINT_SCREEN = GLFW.GLFW_KEY_PRINT_SCREEN;
    public static final int KEY_PAUSE = GLFW.GLFW_KEY_PAUSE;
    public static final int KEY_F1 = GLFW.GLFW_KEY_F1;
    public static final int KEY_F2 = GLFW.GLFW_KEY_F2;
    public static final int KEY_F3 = GLFW.GLFW_KEY_F3;
    public static final int KEY_F4 = GLFW.GLFW_KEY_F4;
    public static final int KEY_F5 = GLFW.GLFW_KEY_F5;
    public static final int KEY_F6 = GLFW.GLFW_KEY_F6;
    public static final int KEY_F7 = GLFW.GLFW_KEY_F7;
    public static final int KEY_F8 = GLFW.GLFW_KEY_F8;
    public static final int KEY_F9 = GLFW.GLFW_KEY_F9;
    public static final int KEY_F10 = GLFW.GLFW_KEY_F10;
    public static final int KEY_F11 = GLFW.GLFW_KEY_F11;
    public static final int KEY_F12 = GLFW.GLFW_KEY_F12;
    public static final int KEY_F13 = GLFW.GLFW_KEY_F13;
    public static final int KEY_F14 = GLFW.GLFW_KEY_F14;
    public static final int KEY_F15 = GLFW.GLFW_KEY_F15;
    public static final int KEY_F16 = GLFW.GLFW_KEY_F16;
    public static final int KEY_F17 = GLFW.GLFW_KEY_F17;
    public static final int KEY_F18 = GLFW.GLFW_KEY_F18;
    public static final int KEY_F19 = GLFW.GLFW_KEY_F19;
    public static final int KEY_F20 = GLFW.GLFW_KEY_F20;
    public static final int KEY_F21 = GLFW.GLFW_KEY_F21;
    public static final int KEY_F22 = GLFW.GLFW_KEY_F22;
    public static final int KEY_F23 = GLFW.GLFW_KEY_F23;
    public static final int KEY_F24 = GLFW.GLFW_KEY_F24;
    public static final int KEY_F25 = GLFW.GLFW_KEY_F25;
    public static final int KEY_KP_0 = GLFW.GLFW_KEY_KP_0;
    public static final int KEY_KP_1 = GLFW.GLFW_KEY_KP_1;
    public static final int KEY_KP_2 = GLFW.GLFW_KEY_KP_2;
    public static final int KEY_KP_3 = GLFW.GLFW_KEY_KP_3;
    public static final int KEY_KP_4 = GLFW.GLFW_KEY_KP_4;
    public static final int KEY_KP_5 = GLFW.GLFW_KEY_KP_5;
    public static final int KEY_KP_6 = GLFW.GLFW_KEY_KP_6;
    public static final int KEY_KP_7 = GLFW.GLFW_KEY_KP_7;
    public static final int KEY_KP_8 = GLFW.GLFW_KEY_KP_8;
    public static final int KEY_KP_9 = GLFW.GLFW_KEY_KP_9;
    public static final int KEY_KP_DECIMAL = GLFW.GLFW_KEY_KP_DECIMAL;
    public static final int KEY_KP_DIVIDE = GLFW.GLFW_KEY_KP_DIVIDE;
    public static final int KEY_KP_MULTIPLY = GLFW.GLFW_KEY_KP_MULTIPLY;
    public static final int KEY_KP_SUBTRACT = GLFW.GLFW_KEY_KP_SUBTRACT;
    public static final int KEY_KP_ADD = GLFW.GLFW_KEY_KP_ADD;
    public static final int KEY_KP_ENTER = GLFW.GLFW_KEY_KP_ENTER;
    public static final int KEY_KP_EQUAL = GLFW.GLFW_KEY_KP_EQUAL;
    public static final int KEY_LEFT_SHIFT = GLFW.GLFW_KEY_LEFT_SHIFT;
    public static final int KEY_LEFT_CONTROL = GLFW.GLFW_KEY_LEFT_CONTROL;
    public static final int KEY_LEFT_ALT = GLFW.GLFW_KEY_LEFT_ALT;
    public static final int KEY_LEFT_SUPER = GLFW.GLFW_KEY_LEFT_SUPER;
    public static final int KEY_RIGHT_SHIFT = GLFW.GLFW_KEY_RIGHT_SHIFT;
    public static final int KEY_RIGHT_CONTROL = GLFW.GLFW_KEY_RIGHT_CONTROL;
    public static final int KEY_RIGHT_ALT = GLFW.GLFW_KEY_RIGHT_ALT;
    public static final int KEY_RIGHT_SUPER = GLFW.GLFW_KEY_RIGHT_SUPER;
    public static final int KEY_MENU = GLFW.GLFW_KEY_MENU;

    public static final int META_SHIFT_ON = GLFW.GLFW_MOD_SHIFT;
    public static final int META_CTRL_ON;
    public static final int META_ALT_ON = GLFW.GLFW_MOD_ALT;
    public static final int META_SUPER_ON = GLFW.GLFW_MOD_SUPER;

    /**
     * This mask is set if the key event was generated by a software keyboard.
     */
    public static final int FLAG_SOFT_KEYBOARD = 0x2;

    /**
     * This mask is set if we don't want the key event to cause us to leave
     * touch mode.
     */
    public static final int FLAG_KEEP_TOUCH_MODE = 0x4;

    /**
     * This mask is set if an event was known to come from a trusted part
     * of the system.  That is, the event is known to come from the user,
     * and could not have been spoofed by a third party component.
     */
    public static final int FLAG_FROM_SYSTEM = 0x8;

    /**
     * This mask is used for compatibility, to identify enter keys that are
     * coming from an IME whose enter key has been auto-labelled "next" or
     * "done".  This allows TextView to dispatch these as normal enter keys
     * for old applications, but still do the appropriate action when
     * receiving them.
     */
    public static final int FLAG_EDITOR_ACTION = 0x10;

    /**
     * When associated with up key events, this indicates that the key press
     * has been canceled.  Typically this is used with virtual touch screen
     * keys, where the user can slide from the virtual key area on to the
     * display: in that case, the application will receive a canceled up
     * event and should not perform the action normally associated with the
     * key.  Note that for this to work, the application can not perform an
     * action for a key until it receives an up or the long press timeout has
     * expired.
     */
    public static final int FLAG_CANCELED = 0x20;

    /**
     * This key event was generated by a virtual (on-screen) hard key area.
     * Typically this is an area of the touchscreen, outside of the regular
     * display, dedicated to "hardware" buttons.
     */
    public static final int FLAG_VIRTUAL_HARD_KEY = 0x40;

    /**
     * This flag is set for the first key repeat that occurs after the
     * long press timeout.
     */
    public static final int FLAG_LONG_PRESS = 0x80;

    /**
     * Set when a key event has {@link #FLAG_CANCELED} set because a long
     * press action was executed while it was down.
     */
    public static final int FLAG_CANCELED_LONG_PRESS = 0x100;

    /**
     * Set for {@link #ACTION_UP} when this event's key code is still being
     * tracked from its initial down.  That is, somebody requested that tracking
     * started on the key down and a long press has not caused
     * the tracking to be canceled.
     */
    public static final int FLAG_TRACKING = 0x200;

    /**
     * Set when a key event has been synthesized to implement default behavior
     * for an event that the application did not handle.
     * Fallback key events are generated by unhandled trackball motions
     * (to emulate a directional keypad) and by certain unhandled key presses
     * that are declared in the key map (such as special function numeric keypad
     * keys when numlock is off).
     */
    public static final int FLAG_FALLBACK = 0x400;

    /**
     * Signifies that the key is being predispatched.
     *
     * @hide
     */
    public static final int FLAG_PREDISPATCH = 0x20000000;

    /**
     * Private control to determine when an app is tracking a key sequence.
     *
     * @hide
     */
    public static final int FLAG_START_TRACKING = 0x40000000;

    static {
        if (Platform.get() == Platform.MACOSX) {
            META_CTRL_ON = GLFW.GLFW_MOD_SUPER;
        } else {
            META_CTRL_ON = GLFW.GLFW_MOD_CONTROL;
        }
    }

    /**
     * {@link #getAction} value: the key has been pressed down.
     */
    public static final int ACTION_DOWN = 0;

    /**
     * {@link #getAction} value: the key has been released.
     */
    public static final int ACTION_UP = 1;

    private static final Pools.Pool<KeyEvent> sPool = Pools.newSynchronizedPool(10);

    private int mAction;
    private int mKeyCode;
    private int mScanCode;
    private int mRepeatCount;
    private int mFlags;
    private int mModifiers;

    private long mEventTime;

    private KeyEvent() {
    }

    @Nonnull
    private static KeyEvent obtain() {
        final KeyEvent event = sPool.acquire();
        if (event == null) {
            return new KeyEvent();
        }
        return event;
    }

    @Nonnull
    public static KeyEvent obtain(long eventTime, int action,
                                  int code, int repeat, int modifiers,
                                  int scancode, int flags) {
        KeyEvent ev = obtain();
        ev.mEventTime = eventTime;
        ev.mAction = action;
        ev.mKeyCode = code;
        ev.mScanCode = scancode;
        ev.mRepeatCount = repeat;
        ev.mFlags = flags;
        ev.mModifiers = modifiers;
        return ev;
    }

    private void copyFrom(@Nonnull KeyEvent other) {
        mEventTime = other.mEventTime;
        mAction = other.mAction;
        mKeyCode = other.mKeyCode;
        mScanCode = other.mScanCode;
        mRepeatCount = other.mRepeatCount;
        mFlags = other.mFlags;
        mModifiers = other.mModifiers;
    }

    /**
     * Retrieve the action of this key event.  May be either
     * {@link #ACTION_DOWN} or {@link #ACTION_UP}.
     *
     * @return The event action: ACTION_DOWN or ACTION_UP.
     */
    public final int getAction() {
        return mAction;
    }

    /**
     * Retrieve the key code of the key event.
     *
     * @return The key code of the event.
     */
    public final int getKeyCode() {
        return mKeyCode;
    }

    /**
     * Retrieve the hardware key id of this key event.  These values are not
     * reliable and vary from device to device.
     */
    public final int getScanCode() {
        return mScanCode;
    }

    /**
     * Retrieve the repeat count of the event.  For key down events,
     * this is the number of times the key has repeated with the first
     * down starting at 0 and counting up from there.  For key up events,
     * this is always equal to zero. For multiple key events,
     * this is the number of down/up pairs that have occurred.
     *
     * @return The number of times the key has repeated.
     */
    public final int getRepeatCount() {
        return mRepeatCount;
    }

    /**
     * Returns the state of the modifier keys.
     *
     * @return An integer in which each bit set to 1 represents a pressed modifier key.
     */
    public final int getModifiers() {
        return mModifiers;
    }

    /**
     * Returns true if no modifier keys are pressed.
     *
     * @return True if no modifier keys are pressed.
     */
    public final boolean hasNoModifiers() {
        return mModifiers == 0;
    }

    /**
     * Returns true if only the specified modifiers keys are pressed.
     * Returns false if a different combination of modifier keys are pressed.
     * <p>
     * If the specified modifier mask includes non-directional modifiers, such as
     * {@link GLFW#GLFW_MOD_SHIFT}, then this method ensures that the modifier
     * is pressed on either side.
     * </p>
     *
     * @param modifiers The modifier keys to check.  May be a combination of modifier keys.
     *                  May be 0 to ensure that no modifier keys are pressed.
     * @return true if only the specified modifier keys are pressed.
     */
    public final boolean hasModifiers(int modifiers) {
        if (modifiers == 0) {
            return mModifiers == 0;
        }
        //TODO review
        return mModifiers == modifiers;
    }

    /**
     * Returns the pressed state of the SHIFT key.
     *
     * @return true if the SHIFT key is pressed, false otherwise
     */
    public final boolean isShiftPressed() {
        return (mModifiers & META_SHIFT_ON) != 0;
    }

    /**
     * Returns the pressed state of the CTRL key. If it's running on OSX,
     * returns the pressed state of the SUPER key.
     *
     * @return true if the CTRL key is pressed, false otherwise
     */
    public final boolean isCtrlPressed() {
        return (mModifiers & META_CTRL_ON) != 0;
    }

    /**
     * Returns the pressed state of the ALT key.
     *
     * @return true if the ALT key is pressed, false otherwise
     */
    public final boolean isAltPressed() {
        return (mModifiers & META_ALT_ON) != 0;
    }

    /**
     * Returns the pressed state of the SUPER key (a.k.a. META or WIN key).
     *
     * @return true if the SUPER key is pressed, false otherwise
     */
    public final boolean isSuperPressed() {
        return (mModifiers & META_SUPER_ON) != 0;
    }

    /**
     * Returns the locked state of the CAPS LOCK key.
     *
     * @return true if the CAPS LOCK key is on, false otherwise
     */
    public final boolean isCapsLockOn() {
        return (mModifiers & GLFW.GLFW_MOD_CAPS_LOCK) != 0;
    }

    /**
     * Returns the locked state of the NUM LOCK key.
     *
     * @return true if the NUM LOCK key is on, false otherwise
     */
    public final boolean isNumLockOn() {
        return (mModifiers & GLFW.GLFW_MOD_NUM_LOCK) != 0;
    }

    /**
     * Returns the alphabetic character represented by the key. You should check if this is a PRESS action.
     *
     * @return the alphabetic character represented by the key
     */
    public final char getMappedChar() {
        String s = GLFW.glfwGetKeyName(mKeyCode, mScanCode);
        if (s != null && s.length() == 1) {
            return s.charAt(0);
        }
        return 0;
    }

    /**
     * Recycles the event. This object should not be ever used
     * after recycling.
     * <p>
     * This method should only be called by system.
     */
    @Override
    public void recycle() {
        sPool.release(this);
    }

    @Nonnull
    @Override
    public InputEvent copy() {
        KeyEvent ev = obtain();
        ev.copyFrom(this);
        return ev;
    }

    @Override
    public long getEventTime() {
        return mEventTime / 1000000;
    }

    @Override
    public long getEventTimeNano() {
        return mEventTime;
    }

    /**
     * For {@link #ACTION_UP} events, indicates that the event has been
     * canceled as per {@link #FLAG_CANCELED}.
     */
    public final boolean isCanceled() {
        return (mFlags & FLAG_CANCELED) != 0;
    }

    @Override
    public final void cancel() {
        mFlags |= FLAG_CANCELED;
    }

    /**
     * Call this during {@link Callback#onKeyDown} to have the system track
     * the key through its final up (possibly including a long press).  Note
     * that only one key can be tracked at a time -- if another key down
     * event is received while a previous one is being tracked, tracking is
     * stopped on the previous event.
     */
    public final void startTracking() {
        mFlags |= FLAG_START_TRACKING;
    }

    /**
     * For {@link #ACTION_UP} events, indicates that the event is still being
     * tracked from its initial down event as per
     * {@link #FLAG_TRACKING}.
     */
    public final boolean isTracking() {
        return (mFlags & FLAG_TRACKING) != 0;
    }

    /**
     * For {@link #ACTION_DOWN} events, indicates that the event has been
     * canceled as per {@link #FLAG_LONG_PRESS}.
     */
    public final boolean isLongPress() {
        return (mFlags & FLAG_LONG_PRESS) != 0;
    }

    /**
     * Returns true if this key code is a modifier key.
     * <p>
     * For the purposes of this function, {@link #KEY_CAPS_LOCK},
     * {@link #KEY_SCROLL_LOCK}, and {@link #KEY_NUM_LOCK} are
     * not considered modifier keys.  Consequently, this function return false
     * for those keys.
     * </p>
     *
     * @return True if the key code is one of modifier keys.
     */
    public static boolean isModifierKey(int keyCode) {
        return switch (keyCode) {
            case KEY_LEFT_SHIFT, KEY_RIGHT_SHIFT, KEY_LEFT_ALT, KEY_RIGHT_ALT, KEY_LEFT_CONTROL, KEY_RIGHT_CONTROL,
                    KEY_LEFT_SUPER, KEY_RIGHT_SUPER -> true;
            default -> false;
        };
    }

    public static class DispatcherState {

        int mDownKeyCode;
        Object mDownTarget;
        Int2BooleanArrayMap mActiveLongPresses = new Int2BooleanArrayMap();

        /**
         * Reset back to initial state.
         */
        public void reset() {
            mDownKeyCode = 0;
            mDownTarget = null;
            mActiveLongPresses.clear();
        }

        /**
         * Stop any tracking associated with this target.
         */
        public void reset(Object target) {
            if (mDownTarget == target) {
                mDownKeyCode = 0;
                mDownTarget = null;
            }
        }

        /**
         * Start tracking the key code associated with the given event.  This
         * can only be called on a key down.  It will allow you to see any
         * long press associated with the key, and will result in
         * {@link KeyEvent#isTracking} return true on the long press and up
         * events.
         *
         * <p>This is only needed if you are directly dispatching events, rather
         * than handling them in {@link Callback#onKeyDown}.
         */
        public void startTracking(@Nonnull KeyEvent event, Object target) {
            if (event.getAction() != ACTION_DOWN) {
                throw new IllegalArgumentException(
                        "Can only start tracking on a down event");
            }
            mDownKeyCode = event.getKeyCode();
            mDownTarget = target;
        }

        /**
         * Return true if the key event is for a key code that is currently
         * being tracked by the dispatcher.
         */
        public boolean isTracking(@Nonnull KeyEvent event) {
            return mDownKeyCode == event.getKeyCode();
        }

        /**
         * Keep track of the given event's key code as having performed an
         * action with a long press, so no action should occur on the up.
         * <p>This is only needed if you are directly dispatching events, rather
         * than handling them in {@link Callback#onKeyLongPress}.
         */
        public void performedLongPress(@Nonnull KeyEvent event) {
            mActiveLongPresses.put(event.getKeyCode(), true);
        }

        /**
         * Handle key up event to stop tracking.  This resets the dispatcher state,
         * and updates the key event state based on it.
         * <p>This is only needed if you are directly dispatching events, rather
         * than handling them in {@link Callback#onKeyUp}.
         */
        public void handleUpEvent(@Nonnull KeyEvent event) {
            final int keyCode = event.getKeyCode();
            if (mActiveLongPresses.remove(keyCode)) {
                event.mFlags |= FLAG_CANCELED | FLAG_CANCELED_LONG_PRESS;
            }
            if (mDownKeyCode == keyCode) {
                event.mFlags |= FLAG_TRACKING;
                mDownKeyCode = 0;
                mDownTarget = null;
            }
        }
    }
}
