/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.util.Pools;
import icyllis.modernui.util.SparseBooleanArray;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.sdl.SDLKeycode;
import org.lwjgl.system.Platform;

/**
 * Object that reports key events (keyboard etc.).
 * Modified for desktop application.
 */
@SuppressWarnings("unused")
public class KeyEvent extends InputEvent {

    public static final boolean IS_MACOS = Platform.get() == Platform.MACOSX;

    public static final int KEY_UNKNOWN = 0;

    public static final int KEY_A = 65;
    public static final int KEY_B = 66;
    public static final int KEY_C = 67;
    public static final int KEY_D = 68;
    public static final int KEY_E = 69;
    public static final int KEY_F = 70;
    public static final int KEY_G = 71;
    public static final int KEY_H = 72;
    public static final int KEY_I = 73;
    public static final int KEY_J = 74;
    public static final int KEY_K = 75;
    public static final int KEY_L = 76;
    public static final int KEY_M = 77;
    public static final int KEY_N = 78;
    public static final int KEY_O = 79;
    public static final int KEY_P = 80;
    public static final int KEY_Q = 81;
    public static final int KEY_R = 82;
    public static final int KEY_S = 83;
    public static final int KEY_T = 84;
    public static final int KEY_U = 85;
    public static final int KEY_V = 86;
    public static final int KEY_W = 87;
    public static final int KEY_X = 88;
    public static final int KEY_Y = 89;
    public static final int KEY_Z = 90;
    public static final int KEY_0 = 48;
    public static final int KEY_1 = 49;
    public static final int KEY_2 = 50;
    public static final int KEY_3 = 51;
    public static final int KEY_4 = 52;
    public static final int KEY_5 = 53;
    public static final int KEY_6 = 54;
    public static final int KEY_7 = 55;
    public static final int KEY_8 = 56;
    public static final int KEY_9 = 57;

    public static final int KEY_ENTER = 257;
    public static final int KEY_ESCAPE = 256;
    public static final int KEY_BACKSPACE = 259;
    public static final int KEY_TAB = 258;
    public static final int KEY_SPACE = 32;
    public static final int KEY_MINUS = 45;
    public static final int KEY_EQUAL = 61;
    public static final int KEY_LEFT_BRACKET = 91;
    public static final int KEY_RIGHT_BRACKET = 93;
    public static final int KEY_BACKSLASH = 92;
    public static final int KEY_NON_US_HASH = 161;
    public static final int KEY_SEMICOLON = 59;
    public static final int KEY_APOSTROPHE = 39;
    public static final int KEY_GRAVE = 96;
    public static final int KEY_COMMA = 44;
    public static final int KEY_PERIOD = 46;
    public static final int KEY_SLASH = 47;
    public static final int KEY_CAPS_LOCK = 280;

    public static final int KEY_F1 = 290;
    public static final int KEY_F2 = 291;
    public static final int KEY_F3 = 292;
    public static final int KEY_F4 = 293;
    public static final int KEY_F5 = 294;
    public static final int KEY_F6 = 295;
    public static final int KEY_F7 = 296;
    public static final int KEY_F8 = 297;
    public static final int KEY_F9 = 298;
    public static final int KEY_F10 = 299;
    public static final int KEY_F11 = 300;
    public static final int KEY_F12 = 301;

    public static final int KEY_PRINT_SCREEN = 283;
    public static final int KEY_SCROLL_LOCK = 281;
    public static final int KEY_PAUSE = 284;

    public static final int KEY_INSERT = 260;
    public static final int KEY_HOME = 268;
    public static final int KEY_PAGE_UP = 266;
    public static final int KEY_DELETE = 261;
    public static final int KEY_END = 269;
    public static final int KEY_PAGE_DOWN = 267;
    public static final int KEY_RIGHT = 262;
    public static final int KEY_LEFT = 263;
    public static final int KEY_DOWN = 264;
    public static final int KEY_UP = 265;

    public static final int KEY_NUM_LOCK = 282;
    public static final int KEY_KP_DIVIDE = 331;
    public static final int KEY_KP_MULTIPLY = 332;
    public static final int KEY_KP_SUBTRACT = 333;
    public static final int KEY_KP_ADD = 334;
    public static final int KEY_KP_ENTER = 335;
    public static final int KEY_KP_1 = 321;
    public static final int KEY_KP_2 = 322;
    public static final int KEY_KP_3 = 323;
    public static final int KEY_KP_4 = 324;
    public static final int KEY_KP_5 = 325;
    public static final int KEY_KP_6 = 326;
    public static final int KEY_KP_7 = 327;
    public static final int KEY_KP_8 = 328;
    public static final int KEY_KP_9 = 329;
    public static final int KEY_KP_0 = 320;
    public static final int KEY_KP_DOT = 330;
    public static final int KEY_NON_US_BACKSLASH = 162;
    public static final int KEY_MENU = 348;
    public static final int KEY_POWER = 102;
    public static final int KEY_KP_EQUAL = 336;

    public static final int KEY_F13 = 302;
    public static final int KEY_F14 = 303;
    public static final int KEY_F15 = 304;
    public static final int KEY_F16 = 305;
    public static final int KEY_F17 = 306;
    public static final int KEY_F18 = 307;
    public static final int KEY_F19 = 308;
    public static final int KEY_F20 = 309;
    public static final int KEY_F21 = 310;
    public static final int KEY_F22 = 311;
    public static final int KEY_F23 = 312;
    public static final int KEY_F24 = 313;

    public static final int KEY_LEFT_CTRL = 341;
    public static final int KEY_LEFT_SHIFT = 340;
    public static final int KEY_LEFT_ALT = 342;
    public static final int KEY_LEFT_META = 343;
    public static final int KEY_RIGHT_CTRL = 345;
    public static final int KEY_RIGHT_SHIFT = 344;
    public static final int KEY_RIGHT_ALT = 346;
    public static final int KEY_RIGHT_META = 347;

    public static final int META_SHIFT_ON = 0x01;
    public static final int META_CTRL_ON = 0x02;
    public static final int META_ALT_ON = 0x04;
    public static final int META_META_ON = 0x08;

    public static final int META_LEFT_SHIFT_ON = 0x10;
    public static final int META_RIGHT_SHIFT_ON = 0x20;

    public static final int META_LEFT_CTRL_ON = SDLKeycode.SDL_KMOD_LCTRL;
    public static final int META_RIGHT_CTRL_ON = SDLKeycode.SDL_KMOD_RCTRL;

    public static final int META_LEFT_ALT_ON = SDLKeycode.SDL_KMOD_LALT;
    public static final int META_RIGHT_ALT_ON = SDLKeycode.SDL_KMOD_RALT;

    public static final int META_LEFT_META_ON = SDLKeycode.SDL_KMOD_LGUI;
    public static final int META_RIGHT_META_ON = SDLKeycode.SDL_KMOD_RGUI;

    public static final int META_NUM_LOCK_ON = SDLKeycode.SDL_KMOD_NUM;
    public static final int META_CAPS_LOCK_ON = SDLKeycode.SDL_KMOD_CAPS;
    public static final int META_SCROLL_LOCK_ON = SDLKeycode.SDL_KMOD_SCROLL;

    /**
     * This is a runtime constant that should be used as the default shortcut key modifier.
     * On macOS, this is COMMAND key; otherwise, this is CTRL key.
     */
    public static final int META_SHORTCUT_ON = IS_MACOS ? META_META_ON : META_CTRL_ON;

    // Mask of all modifier key meta states.  Specifically excludes locked keys like caps lock.
    private static final int META_MODIFIER_MASK =
            META_SHIFT_ON | META_LEFT_SHIFT_ON | META_RIGHT_SHIFT_ON
                    | META_ALT_ON | META_LEFT_ALT_ON | META_RIGHT_ALT_ON
                    | META_CTRL_ON | META_LEFT_CTRL_ON | META_RIGHT_CTRL_ON
                    | META_META_ON | META_LEFT_META_ON | META_RIGHT_META_ON;

    // Mask of all lock key meta states.
    private static final int META_LOCK_MASK =
            META_CAPS_LOCK_ON | META_NUM_LOCK_ON | META_SCROLL_LOCK_ON;

    // Mask of all valid meta states.
    private static final int META_ALL_MASK = META_MODIFIER_MASK | META_LOCK_MASK;

    // Mask of all meta states that are not valid use in specifying a modifier key.
    // These bits are known to be used for purposes other than specifying modifiers.
    private static final int META_INVALID_MODIFIER_MASK =
            META_LOCK_MASK;

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
     * Set when a key event is a key repeat.
     */
    public static final int FLAG_REPEAT = 0x10000000;

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
    private int mFlags;
    private int mMetaState;
    private int mDeviceId;
    private int mUnicodeChar;

    private long mEventTime;

    private KeyEvent() {
    }

    @NonNull
    private static KeyEvent obtain() {
        final KeyEvent event = sPool.acquire();
        if (event == null) {
            return new KeyEvent();
        }
        return event;
    }

    @NonNull
    public static KeyEvent obtain(long eventTime, int action,
                                  int code, int metaState,
                                  int deviceId, int scanCode, int flags, int unicodeChar) {
        KeyEvent ev = obtain();
        ev.mEventTime = eventTime;
        ev.mAction = action;
        ev.mKeyCode = code;
        ev.mScanCode = scanCode;
        ev.mFlags = flags;
        ev.mMetaState = metaState;
        ev.mDeviceId = deviceId;
        ev.mUnicodeChar = unicodeChar;
        return ev;
    }

    private void copyFrom(@NonNull KeyEvent other) {
        mEventTime = other.mEventTime;
        mAction = other.mAction;
        mKeyCode = other.mKeyCode;
        mScanCode = other.mScanCode;
        mFlags = other.mFlags;
        mMetaState = other.mMetaState;
        mDeviceId = other.mDeviceId;
        mUnicodeChar = other.mUnicodeChar;
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
     * Returns the alphabetic character represented by the key. You should check if this is a DOWN action.
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

    @NonNull
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
     * Returns whether the key event is a key repeat.
     */
    public final boolean isRepeat() {
        return (mFlags & FLAG_REPEAT) != 0;
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
     * Gets a mask that includes all valid modifier key meta state bits.
     * <p>
     * For the purposes of this function, {@link #KEY_CAPS_LOCK},
     * {@link #KEY_SCROLL_LOCK}, and {@link #KEY_NUM_LOCK} are
     * not considered modifier keys.  Consequently, the mask specifically excludes
     * {@link #META_CAPS_LOCK_ON}, {@link #META_SCROLL_LOCK_ON} and {@link #META_NUM_LOCK_ON}.
     * </p>
     *
     * @return The modifier meta state mask which is a combination of
     * {@link #META_SHIFT_ON}, {@link #META_LEFT_SHIFT_ON}, {@link #META_RIGHT_SHIFT_ON},
     * {@link #META_ALT_ON}, {@link #META_LEFT_ALT_ON}, {@link #META_RIGHT_ALT_ON},
     * {@link #META_CTRL_ON}, {@link #META_LEFT_CTRL_ON}, {@link #META_RIGHT_CTRL_ON},
     * {@link #META_META_ON}, {@link #META_LEFT_META_ON}, {@link #META_RIGHT_META_ON},
     */
    public static int getModifierMetaStateMask() {
        return META_MODIFIER_MASK;
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
            case KEY_LEFT_SHIFT, KEY_RIGHT_SHIFT,
                 KEY_LEFT_ALT, KEY_RIGHT_ALT,
                 KEY_LEFT_CTRL, KEY_RIGHT_CTRL,
                 KEY_LEFT_META, KEY_RIGHT_META -> true;
            default -> false;
        };
    }

    /**
     * Normalizes the specified meta state.
     * <p>
     * The meta state is normalized such that if either the left or right modifier meta state
     * bits are set then the result will also include the universal bit for that modifier.
     * </p><p>
     * If the specified meta state contains {@link #META_LEFT_ALT_ON} then
     * the result will also contain {@link #META_ALT_ON} in addition to {@link #META_LEFT_ALT_ON}
     * and the other bits that were specified in the input.  The same is process is
     * performed for shift, control and meta.
     * </p><p>
     * Undefined meta state bits are removed.
     * </p>
     *
     * @param metaState The meta state.
     * @return The normalized meta state.
     */
    public static int normalizeMetaState(int metaState) {
        if ((metaState & (META_LEFT_SHIFT_ON | META_RIGHT_SHIFT_ON)) != 0) {
            metaState |= META_SHIFT_ON;
        }
        if ((metaState & (META_LEFT_ALT_ON | META_RIGHT_ALT_ON)) != 0) {
            metaState |= META_ALT_ON;
        }
        if ((metaState & (META_LEFT_CTRL_ON | META_RIGHT_CTRL_ON)) != 0) {
            metaState |= META_CTRL_ON;
        }
        if ((metaState & (META_LEFT_META_ON | META_RIGHT_META_ON)) != 0) {
            metaState |= META_META_ON;
        }
        return metaState & META_ALL_MASK;
    }

    /**
     * Returns true if no modifiers keys are pressed according to the specified meta state.
     * <p>
     * For the purposes of this function, {@link #KEY_CAPS_LOCK},
     * {@link #KEY_SCROLL_LOCK}, and {@link #KEY_NUM_LOCK} are
     * not considered modifier keys.  Consequently, this function ignores
     * {@link #META_CAPS_LOCK_ON}, {@link #META_SCROLL_LOCK_ON} and {@link #META_NUM_LOCK_ON}.
     * </p><p>
     * The meta state is normalized prior to comparison using {@link #normalizeMetaState(int)}.
     * </p>
     *
     * @param metaState The meta state to consider.
     * @return True if no modifier keys are pressed.
     * @see #hasNoModifiers()
     */
    public static boolean metaStateHasNoModifiers(int metaState) {
        return (normalizeMetaState(metaState) & META_MODIFIER_MASK) == 0;
    }

    /**
     * Returns true if only the specified modifier keys are pressed according to
     * the specified meta state.  Returns false if a different combination of modifier
     * keys are pressed.
     * <p>
     * For the purposes of this function, {@link #KEY_CAPS_LOCK},
     * {@link #KEY_SCROLL_LOCK}, and {@link #KEY_NUM_LOCK} are
     * not considered modifier keys.  Consequently, this function ignores
     * {@link #META_CAPS_LOCK_ON}, {@link #META_SCROLL_LOCK_ON} and {@link #META_NUM_LOCK_ON}.
     * </p><p>
     * If the specified modifier mask includes directional modifiers, such as
     * {@link #META_LEFT_SHIFT_ON}, then this method ensures that the
     * modifier is pressed on that side.
     * If the specified modifier mask includes non-directional modifiers, such as
     * {@link #META_SHIFT_ON}, then this method ensures that the modifier
     * is pressed on either side.
     * If the specified modifier mask includes both directional and non-directional modifiers
     * for the same type of key, such as {@link #META_SHIFT_ON} and {@link #META_LEFT_SHIFT_ON},
     * then this method throws an illegal argument exception.
     * </p>
     *
     * @param metaState The meta state to consider.
     * @param modifiers The meta state of the modifier keys to check.  May be a combination
     * of modifier meta states as defined by {@link #getModifierMetaStateMask()}.  May be 0 to
     * ensure that no modifier keys are pressed.
     * @return True if only the specified modifier keys are pressed.
     * @throws IllegalArgumentException if the modifiers parameter contains invalid modifiers
     * @see #hasModifiers
     */
    public static boolean metaStateHasModifiers(int metaState, int modifiers) {
        // Note: For forward compatibility, we allow the parameter to contain meta states
        //       that we do not recognize but we explicitly disallow meta states that
        //       are not valid modifiers.
        if ((modifiers & META_INVALID_MODIFIER_MASK) != 0) {
            throw new IllegalArgumentException("modifiers must not contain "
                    + "META_CAPS_LOCK_ON, META_NUM_LOCK_ON, META_SCROLL_LOCK_ON, "
                    + "META_CAP_LOCKED, META_ALT_LOCKED, META_SYM_LOCKED, "
                    + "or META_SELECTING");
        }

        metaState = normalizeMetaState(metaState) & META_MODIFIER_MASK;
        metaState = metaStateFilterDirectionalModifiers(metaState, modifiers,
                META_SHIFT_ON, META_LEFT_SHIFT_ON, META_RIGHT_SHIFT_ON);
        metaState = metaStateFilterDirectionalModifiers(metaState, modifiers,
                META_ALT_ON, META_LEFT_ALT_ON, META_RIGHT_ALT_ON);
        metaState = metaStateFilterDirectionalModifiers(metaState, modifiers,
                META_CTRL_ON, META_LEFT_CTRL_ON, META_RIGHT_CTRL_ON);
        metaState = metaStateFilterDirectionalModifiers(metaState, modifiers,
                META_META_ON, META_LEFT_META_ON, META_RIGHT_META_ON);
        return metaState == modifiers;
    }

    private static int metaStateFilterDirectionalModifiers(int metaState,
                                                           int modifiers, int basic, int left, int right) {
        final boolean wantBasic = (modifiers & basic) != 0;
        final int directional = left | right;
        final boolean wantLeftOrRight = (modifiers & directional) != 0;

        if (wantBasic) {
            if (wantLeftOrRight) {
                throw new IllegalArgumentException("modifiers must not contain "
                        + Integer.toHexString(basic) + " combined with "
                        + Integer.toHexString(left) + " or " + Integer.toHexString(right));
            }
            return metaState & ~directional;
        } else if (wantLeftOrRight) {
            return metaState & ~basic;
        } else {
            return metaState;
        }
    }

    /**
     * <p>Returns the state of the meta keys.</p>
     *
     * @return an integer in which each bit set to 1 represents a pressed
     *         meta key
     *
     * @see #isAltPressed()
     * @see #isShiftPressed()
     * @see #isCtrlPressed()
     * @see #isMetaPressed()
     * @see #isCapsLockOn()
     * @see #isNumLockOn()
     * @see #isScrollLockOn()
     * @see #META_ALT_ON
     * @see #META_LEFT_ALT_ON
     * @see #META_RIGHT_ALT_ON
     * @see #META_SHIFT_ON
     * @see #META_LEFT_SHIFT_ON
     * @see #META_RIGHT_SHIFT_ON
     * @see #META_CTRL_ON
     * @see #META_LEFT_CTRL_ON
     * @see #META_RIGHT_CTRL_ON
     * @see #META_META_ON
     * @see #META_LEFT_META_ON
     * @see #META_RIGHT_META_ON
     * @see #META_CAPS_LOCK_ON
     * @see #META_NUM_LOCK_ON
     * @see #META_SCROLL_LOCK_ON
     * @see #getModifiers
     */
    public final int getMetaState() {
        return mMetaState;
    }

    /**
     * Returns the state of the modifier keys.
     * <p>
     * For the purposes of this function, {@link #KEY_CAPS_LOCK},
     * {@link #KEY_SCROLL_LOCK}, and {@link #KEY_NUM_LOCK} are
     * not considered modifier keys.  Consequently, this function specifically masks out
     * {@link #META_CAPS_LOCK_ON}, {@link #META_SCROLL_LOCK_ON} and {@link #META_NUM_LOCK_ON}.
     * </p><p>
     * The value returned consists of the meta state (from {@link #getMetaState})
     * normalized using {@link #normalizeMetaState(int)} and then masked with
     * {@link #getModifierMetaStateMask} so that only valid modifier bits are retained.
     * </p>
     *
     * @return An integer in which each bit set to 1 represents a pressed modifier key.
     * @see #getMetaState
     */
    public final int getModifiers() {
        return normalizeMetaState(mMetaState) & META_MODIFIER_MASK;
    }

    /**
     * Returns true if no modifier keys are pressed.
     * <p>
     * For the purposes of this function, {@link #KEY_CAPS_LOCK},
     * {@link #KEY_SCROLL_LOCK}, and {@link #KEY_NUM_LOCK} are
     * not considered modifier keys.  Consequently, this function ignores
     * {@link #META_CAPS_LOCK_ON}, {@link #META_SCROLL_LOCK_ON} and {@link #META_NUM_LOCK_ON}.
     * </p><p>
     * The meta state is normalized prior to comparison using {@link #normalizeMetaState(int)}.
     * </p>
     *
     * @return True if no modifier keys are pressed.
     * @see #metaStateHasNoModifiers
     */
    public final boolean hasNoModifiers() {
        return metaStateHasNoModifiers(mMetaState);
    }

    /**
     * Returns true if only the specified modifiers keys are pressed.
     * Returns false if a different combination of modifier keys are pressed.
     * <p>
     * For the purposes of this function, {@link #KEY_CAPS_LOCK},
     * {@link #KEY_SCROLL_LOCK}, and {@link #KEY_NUM_LOCK} are
     * not considered modifier keys.  Consequently, this function ignores
     * {@link #META_CAPS_LOCK_ON}, {@link #META_SCROLL_LOCK_ON} and {@link #META_NUM_LOCK_ON}.
     * </p><p>
     * If the specified modifier mask includes directional modifiers, such as
     * {@link #META_LEFT_SHIFT_ON}, then this method ensures that the
     * modifier is pressed on that side.
     * If the specified modifier mask includes non-directional modifiers, such as
     * {@link #META_SHIFT_ON}, then this method ensures that the modifier
     * is pressed on either side.
     * If the specified modifier mask includes both directional and non-directional modifiers
     * for the same type of key, such as {@link #META_SHIFT_ON} and {@link #META_LEFT_SHIFT_ON},
     * then this method throws an illegal argument exception.
     * </p>
     *
     * @param modifiers The meta state of the modifier keys to check.  May be a combination
     * of modifier meta states as defined by {@link #getModifierMetaStateMask()}.  May be 0 to
     * ensure that no modifier keys are pressed.
     * @return True if only the specified modifier keys are pressed.
     * @throws IllegalArgumentException if the modifiers parameter contains invalid modifiers
     * @see #metaStateHasModifiers
     */
    public final boolean hasModifiers(int modifiers) {
        return metaStateHasModifiers(mMetaState, modifiers);
    }

    /**
     * Returns the pressed state of the SHIFT key.
     *
     * @return true if the SHIFT key is pressed, false otherwise
     */
    public final boolean isShiftPressed() {
        return (mMetaState & META_SHIFT_ON) != 0;
    }

    /**
     * Returns the pressed state of the CTRL key.
     *
     * @return true if the CTRL key is pressed, false otherwise
     */
    public final boolean isCtrlPressed() {
        return (mMetaState & META_CTRL_ON) != 0;
    }

    /**
     * Returns the pressed state of the ALT key (a.k.a. OPTION key).
     *
     * @return true if the ALT key is pressed, false otherwise
     */
    public final boolean isAltPressed() {
        return (mMetaState & META_ALT_ON) != 0;
    }

    /**
     * Returns the pressed state of the META key (a.k.a. SUPER or GUI or WINDOWS or COMMAND key).
     *
     * @return true if the META key is pressed, false otherwise
     */
    public final boolean isMetaPressed() {
        return (mMetaState & META_META_ON) != 0;
    }

    /**
     * If it's running on macOS, returns the pressed state of the COMMAND key.
     * Otherwise, returns the pressed state of the CTRL key.
     *
     * @return true if the shortcut key is pressed, false otherwise
     */
    public final boolean isShortcutPressed() {
        return (mMetaState & META_SHORTCUT_ON) != 0;
    }

    /**
     * Returns the locked state of the CAPS LOCK key.
     *
     * @return true if the CAPS LOCK key is on, false otherwise
     */
    public final boolean isCapsLockOn() {
        return (mMetaState & META_CAPS_LOCK_ON) != 0;
    }

    /**
     * Returns the locked state of the NUM LOCK key.
     *
     * @return true if the NUM LOCK key is on, false otherwise
     */
    public final boolean isNumLockOn() {
        return (mMetaState & META_NUM_LOCK_ON) != 0;
    }

    /**
     * <p>Returns the locked state of the SCROLL LOCK meta key.</p>
     *
     * @return true if the SCROLL LOCK key is on, false otherwise
     *
     * @see #KEY_SCROLL_LOCK
     * @see #META_SCROLL_LOCK_ON
     */
    public final boolean isScrollLockOn() {
        return (mMetaState & META_SCROLL_LOCK_ON) != 0;
    }

    public static class DispatcherState {

        int mDownKeyCode;
        Object mDownTarget;
        SparseBooleanArray mActiveLongPresses = new SparseBooleanArray();

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
        public void startTracking(@NonNull KeyEvent event, Object target) {
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
        public boolean isTracking(@NonNull KeyEvent event) {
            return mDownKeyCode == event.getKeyCode();
        }

        /**
         * Keep track of the given event's key code as having performed an
         * action with a long press, so no action should occur on the up.
         * <p>This is only needed if you are directly dispatching events, rather
         * than handling them in {@link Callback#onKeyLongPress}.
         */
        public void performedLongPress(@NonNull KeyEvent event) {
            mActiveLongPresses.put(event.getKeyCode(), true);
        }

        /**
         * Handle key up event to stop tracking.  This resets the dispatcher state,
         * and updates the key event state based on it.
         * <p>This is only needed if you are directly dispatching events, rather
         * than handling them in {@link Callback#onKeyUp}.
         */
        public void handleUpEvent(@NonNull KeyEvent event) {
            final int keyCode = event.getKeyCode();
            int index = mActiveLongPresses.indexOfKey(keyCode);
            if (index >= 0) {
                event.mFlags |= FLAG_CANCELED | FLAG_CANCELED_LONG_PRESS;
                mActiveLongPresses.removeAt(index);
            }
            if (mDownKeyCode == keyCode) {
                event.mFlags |= FLAG_TRACKING;
                mDownKeyCode = 0;
                mDownTarget = null;
            }
        }
    }
}
