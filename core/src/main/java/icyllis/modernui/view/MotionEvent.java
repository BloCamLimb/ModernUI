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

import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.Platform;

import javax.annotation.Nonnull;

/**
 * Object that indicates movement events (mouse, touchpad etc).
 * Modified for desktop application, multiple pointers are disabled.
 */
@SuppressWarnings("unused")
public final class MotionEvent extends InputEvent {

    /**
     * An invalid pointer id.
     * <p>
     * This value (-1) can be used as a placeholder to indicate that a pointer id
     * has not been assigned or is not available.  It cannot appear as
     * a pointer id inside a {@link MotionEvent}.
     */
    private static final int INVALID_POINTER_ID = -1;

    /**
     * Bit mask of the parts of the action code that are the action itself.
     */
    private static final int ACTION_MASK = 0xff;

    /**
     * Constant for {@link #getActionMasked}: A pressed gesture has started, the
     * motion contains the initial starting location.
     * <p>
     * This is also a good time to check the button state to distinguish
     * secondary and tertiary button clicks and handle them appropriately.
     * Use {@link #getButtonState} to retrieve the button state.
     * </p>
     */
    public static final int ACTION_DOWN = 0;

    /**
     * Constant for {@link #getActionMasked}: A pressed gesture has finished, the
     * motion contains the final release location as well as any intermediate
     * points since the last down or move event.
     */
    public static final int ACTION_UP = 1;

    /**
     * Constant for {@link #getActionMasked}: A change has happened during a
     * press gesture (between {@link #ACTION_DOWN} and {@link #ACTION_UP}).
     * The motion contains the most recent point, as well as any intermediate
     * points since the last down or move event.
     */
    public static final int ACTION_MOVE = 2;

    /**
     * Constant for {@link #getActionMasked}: The current gesture has been aborted.
     * You will not receive any more points in it.  You should treat this as
     * an up event, but not perform any action that you normally would.
     */
    public static final int ACTION_CANCEL = 3;

    /**
     * Constant for {@link #getActionMasked}: A movement has happened outside of the
     * normal bounds of the UI element.  This does not provide a full gesture,
     * but only the initial location of the movement/touch.
     * <p>
     * Note: Because the location of any event will be outside the
     * bounds of the view hierarchy, it will not get dispatched to
     * any children of a ViewGroup by default. Therefore,
     * movements with ACTION_OUTSIDE should be handled in either the
     * root {@link View}.
     * </p>
     */
    public static final int ACTION_OUTSIDE = 4;

    /**
     * Constant for {@link #getActionMasked}: A non-primary pointer has gone down.
     * <p>
     * Use {@link #getActionIndex} to retrieve the index of the pointer that changed.
     * </p><p>
     * The index is encoded in the {@link #ACTION_POINTER_INDEX_MASK} bits of the
     * unmasked action returned by {@link #getAction}.
     * </p>
     */
    private static final int ACTION_POINTER_DOWN = 5;

    /**
     * Constant for {@link #getActionMasked}: A non-primary pointer has gone up.
     * <p>
     * Use {@link #getActionIndex} to retrieve the index of the pointer that changed.
     * </p><p>
     * The index is encoded in the {@link #ACTION_POINTER_INDEX_MASK} bits of the
     * unmasked action returned by {@link #getAction}.
     * </p>
     */
    private static final int ACTION_POINTER_UP = 6;

    /**
     * Constant for {@link #getActionMasked}: A change happened but the pointer
     * is not down (unlike {@link #ACTION_MOVE}).  The motion contains the most
     * recent point, as well as any intermediate points since the last
     * hover move event.
     * <p>
     * This action is always delivered to the window or view under the pointer.
     * </p><p>
     * This action is not a touch event so it is delivered to
     * {@link View#onGenericMotionEvent(MotionEvent)} rather than
     * {@link View#onTouchEvent(MotionEvent)}.
     * </p>
     */
    public static final int ACTION_HOVER_MOVE = 7;

    /**
     * Constant for {@link #getActionMasked}: The motion event contains relative
     * vertical and/or horizontal scroll offsets.  Use {@link #getAxisValue(int)}
     * to retrieve the information from {@link #AXIS_VSCROLL} and {@link #AXIS_HSCROLL}.
     * The pointer may or may not be down when this event is dispatched.
     * <p>
     * This action is always delivered to the window or view under the pointer, which
     * may not be the window or view currently touched.
     * </p><p>
     * This action is not a touch event so it is delivered to
     * {@link View#onGenericMotionEvent(MotionEvent)} rather than
     * {@link View#onTouchEvent(MotionEvent)}.
     * </p>
     */
    public static final int ACTION_SCROLL = 8;

    /**
     * Constant for {@link #getActionMasked}: The pointer is not down but has entered the
     * boundaries of a window or view.
     * <p>
     * This action is always delivered to the window or view under the pointer.
     * </p><p>
     * This action is not a touch event so it is delivered to
     * {@link View#onGenericMotionEvent(MotionEvent)} rather than
     * {@link View#onTouchEvent(MotionEvent)}.
     * </p>
     */
    public static final int ACTION_HOVER_ENTER = 9;

    /**
     * Constant for {@link #getActionMasked}: The pointer is not down but has exited the
     * boundaries of a window or view.
     * <p>
     * This action is always delivered to the window or view that was previously under the pointer.
     * </p><p>
     * This action is not a touch event so it is delivered to
     * {@link View#onGenericMotionEvent(MotionEvent)} rather than
     * {@link View#onTouchEvent(MotionEvent)}.
     * </p>
     */
    public static final int ACTION_HOVER_EXIT = 10;

    /**
     * Constant for {@link #getActionMasked}: A button has been pressed.
     *
     * <p>
     * Use {@link #getActionButton()} to get which button was pressed.
     * </p><p>
     * This action is not a touch event so it is delivered to
     * {@link View#onGenericMotionEvent(MotionEvent)} rather than
     * {@link View#onTouchEvent(MotionEvent)}.
     * </p>
     */
    private static final int ACTION_BUTTON_PRESS = 11;

    /**
     * Constant for {@link #getActionMasked}: A button has been released.
     *
     * <p>
     * Use {@link #getActionButton()} to get which button was released.
     * </p><p>
     * This action is not a touch event so it is delivered to
     * {@link View#onGenericMotionEvent(MotionEvent)} rather than
     * {@link View#onTouchEvent(MotionEvent)}.
     * </p>
     */
    private static final int ACTION_BUTTON_RELEASE = 12;

    /**
     * Bits in the action code that represent a pointer index, used with
     * {@link #ACTION_POINTER_DOWN} and {@link #ACTION_POINTER_UP}.  Shifting
     * down by {@link #ACTION_POINTER_INDEX_SHIFT} provides the actual pointer
     * index where the data for the pointer going up or down can be found; you can
     * get its identifier with {@link #getPointerId(int)} and the actual
     * data with {@link #getX(int)} etc.
     *
     * @see #getActionIndex
     */
    private static final int ACTION_POINTER_INDEX_MASK = 0xff00;

    /**
     * Bit shift for the action bits holding the pointer index as
     * defined by {@link #ACTION_POINTER_INDEX_MASK}.
     *
     * @see #getActionIndex
     */
    private static final int ACTION_POINTER_INDEX_SHIFT = 8;


    /**
     * This private flag is only set on {@link #ACTION_HOVER_MOVE} events and indicates that
     * this event will be immediately followed by a {@link #ACTION_HOVER_EXIT}. It is used to
     * prevent generating redundant {@link #ACTION_HOVER_ENTER} events.
     */
    public static final int FLAG_HOVER_EXIT_PENDING = 0x4;


    /**
     * Button constant: Primary button (left mouse button).
     * <p>
     * This button constant is not set in response to simple touches with a finger
     * or stylus tip.  The user must actually push a button.
     *
     * @see #getButtonState
     */
    public static final int BUTTON_PRIMARY = 1;

    /**
     * Button constant: Secondary button (right mouse button).
     *
     * @see #getButtonState
     */
    public static final int BUTTON_SECONDARY = 1 << 1;

    /**
     * Button constant: Tertiary button (middle mouse button).
     *
     * @see #getButtonState
     */
    public static final int BUTTON_TERTIARY = 1 << 2;

    /**
     * Button constant: Back button pressed (mouse back button).
     *
     * @see #getButtonState
     */
    public static final int BUTTON_BACK = 1 << 3;

    /**
     * Button constant: Forward button pressed (mouse forward button).
     *
     * @see #getButtonState
     */
    public static final int BUTTON_FORWARD = 1 << 4;


    /**
     * Tool type constant: Unknown tool type.
     * This constant is used when the tool type is not known or is not relevant,
     * such as for a trackball or other non-pointing device.
     *
     * @see #getToolType
     */
    private static final int TOOL_TYPE_UNKNOWN = 0;

    /**
     * Tool type constant: The tool is a finger.
     *
     * @see #getToolType
     */
    private static final int TOOL_TYPE_FINGER = 1;

    /**
     * Tool type constant: The tool is a stylus.
     *
     * @see #getToolType
     */
    private static final int TOOL_TYPE_STYLUS = 2;

    /**
     * Tool type constant: The tool is a mouse.
     *
     * @see #getToolType
     */
    private static final int TOOL_TYPE_MOUSE = 3;

    /**
     * Tool type constant: The tool is an eraser or a stylus being used in an inverted posture.
     *
     * @see #getToolType
     */
    private static final int TOOL_TYPE_ERASER = 4;


    /**
     * Axis constant: X axis of a motion event.
     * <p>
     * <ul>
     * <li>For a touch screen, reports the absolute X screen position of the center of
     * the touch contact area.  The units are display pixels.
     * <li>For a touch pad, reports the absolute X surface position of the center of the touch
     * contact area.
     * <li>For a mouse, reports the absolute X screen position of the mouse pointer.
     * The units are display pixels.
     * <li>For a trackball, reports the relative horizontal displacement of the trackball.
     * The value is normalized to a range from -1.0 (left) to 1.0 (right).
     * <li>For a joystick, reports the absolute X position of the joystick.
     * The value is normalized to a range from -1.0 (left) to 1.0 (right).
     * </ul>
     * </p>
     *
     * @see #getX(int)
     * @see MotionEvent.PointerCoords#getAxisValue(int)
     */
    public static final int AXIS_X = 0;

    /**
     * Axis constant: Y axis of a motion event.
     * <p>
     * <ul>
     * <li>For a touch screen, reports the absolute Y screen position of the center of
     * the touch contact area.  The units are display pixels.
     * <li>For a touch pad, reports the absolute Y surface position of the center of the touch
     * contact area.
     * <li>For a mouse, reports the absolute Y screen position of the mouse pointer.
     * The units are display pixels.
     * <li>For a trackball, reports the relative vertical displacement of the trackball.
     * The value is normalized to a range from -1.0 (up) to 1.0 (down).
     * <li>For a joystick, reports the absolute Y position of the joystick.
     * The value is normalized to a range from -1.0 (up or far) to 1.0 (down or near).
     * </ul>
     * </p>
     *
     * @see #getY(int)
     * @see MotionEvent.PointerCoords#getAxisValue(int)
     */
    public static final int AXIS_Y = 1;

    /**
     * Axis constant: Vertical Scroll axis of a motion event.
     * <p>
     * <ul>
     * <li>For a mouse, reports the relative movement of the vertical scroll wheel.
     * The value is normalized to a range from -1.0 (down) to 1.0 (up).
     * </ul>
     * </p><p>
     * This axis should be used to scroll views vertically.
     * </p>
     *
     * @see #getAxisValue(int, int)
     * @see MotionEvent.PointerCoords#getAxisValue(int)
     */
    public static final int AXIS_VSCROLL = 9;

    /**
     * Axis constant: Horizontal Scroll axis of a motion event.
     * <p>
     * <ul>
     * <li>For a mouse, reports the relative movement of the horizontal scroll wheel.
     * The value is normalized to a range from -1.0 (left) to 1.0 (right).
     * </ul>
     * </p><p>
     * This axis should be used to scroll views horizontally.
     * </p>
     *
     * @see #getAxisValue(int, int)
     * @see MotionEvent.PointerCoords#getAxisValue(int)
     */
    public static final int AXIS_HSCROLL = 10;

    private static final float INVALID_CURSOR_POSITION = Float.NaN;

    private static final Pool<MotionEvent> sPool = Pools.concurrent(10);

    // Shared temporary objects used when translating coordinates supplied by
    // the caller into single element PointerCoords and pointer id arrays.
    private static final Object gSharedTempLock = new Object();
    private static PointerCoords[] gSharedTempPointerCoords;
    private static PointerProperties[] gSharedTempPointerProperties;

    @SuppressWarnings("SameParameterValue")
    private static void ensureSharedTempPointerCapacity(int desiredCapacity) {
        if (gSharedTempPointerCoords == null
                || gSharedTempPointerCoords.length < desiredCapacity) {
            int capacity = gSharedTempPointerCoords != null ? gSharedTempPointerCoords.length : 1;
            while (capacity < desiredCapacity) {
                capacity *= 2;
            }
            gSharedTempPointerCoords = PointerCoords.createArray(capacity);
            gSharedTempPointerProperties = PointerProperties.createArray(capacity);
        }
    }

    private int mAction;
    private int mActionButton;
    private int mFlags;
    private int mModifiers;
    private int mButtonState;

    private float mXOffset;
    private float mYOffset;
    private float mRawXCursorPosition;
    private float mRawYCursorPosition;

    private long mDownTime;
    private long mEventTime;

    private final ObjectArrayList<PointerProperties> mPointerProperties = new ObjectArrayList<>();
    private final ObjectArrayList<PointerCoords> mPointerCoords = new ObjectArrayList<>();

    private MotionEvent() {
    }

    @Nonnull
    private static MotionEvent obtain() {
        final MotionEvent event = sPool.acquire();
        if (event == null) {
            return new MotionEvent();
        }
        return event;
    }

    /**
     * Create a new MotionEvent, filling in all of the basic values that
     * define the motion.
     *
     * @param downTime     The time (in ns) when the user originally pressed down to start
     *                     a stream of position events.  This must be obtained from {@link GLFW#glfwGetTime()}.
     * @param eventTime    The the time (in ns) when this specific event was generated.  This
     *                     must be obtained from {@link GLFW#glfwGetTime()}.
     * @param action       The kind of action being performed, such as {@link #ACTION_DOWN}.
     * @param pointerCount The number of pointers that will be in this event.
     * @param properties   An array of <em>pointerCount</em> values providing
     *                     a {@link PointerProperties} property object for each pointer, which must
     *                     include the pointer identifier.
     * @param coords       An array of <em>pointerCount</em> values providing
     *                     a {@link PointerCoords} coordinate object for each pointer.
     * @param modifiers    The modifier keys that were in effect when the event was generated.
     * @param buttonState  The state of buttons that are pressed.
     * @param flags        The motion event flags.
     */
    @Nonnull
    public static MotionEvent obtain(long downTime, long eventTime, int action,
                                     int pointerCount, PointerProperties[] properties,
                                     PointerCoords[] coords, int modifiers,
                                     int buttonState, int flags) {
        MotionEvent event = obtain();
        event.initialize(action, 0, flags, modifiers, buttonState,
                0, 0, INVALID_CURSOR_POSITION, INVALID_CURSOR_POSITION,
                downTime, eventTime, pointerCount, properties, coords);
        return event;
    }

    @Nonnull
    public static MotionEvent obtain(long downTime, long eventTime, int action,
                                     float x, float y, int modifiers) {
        return obtain(downTime, eventTime, action, 0, x, y, modifiers, 0, 0);
    }

    /**
     * Create a new MotionEvent, filling in a subset of the basic motion
     * values.
     *
     * @param downTime     The time (in ns) when the user originally pressed down to start
     *                     a stream of position events.  This must be obtained from {@link GLFW#glfwGetTime()}.
     * @param eventTime    The the time (in ns) when this specific event was generated.  This
     *                     must be obtained from {@link GLFW#glfwGetTime()}.
     * @param action       The kind of action being performed, such as {@link #ACTION_DOWN}.
     * @param actionButton The button of press or release action, such as {@link #BUTTON_PRIMARY}
     * @param x            The X coordinate of this event.
     * @param y            The Y coordinate of this event.
     * @param modifiers    The modifier keys that were in effect when the event was generated.
     * @param buttonState  The state of buttons that are pressed.
     * @param flags        The motion event flags.
     */
    @Nonnull
    public static MotionEvent obtain(long downTime, long eventTime, int action,
                                     int actionButton, float x, float y,
                                     int modifiers, int buttonState, int flags) {
        final int actionMasked = action & ACTION_MASK;
        if ((actionMasked == ACTION_BUTTON_PRESS
                || actionMasked == ACTION_BUTTON_RELEASE) && actionButton == 0) {
            throw new IllegalArgumentException("actionButton should be defined for action press or release");
        }
        MotionEvent event = obtain();
        synchronized (gSharedTempLock) {
            ensureSharedTempPointerCapacity(1);
            final PointerProperties[] properties = gSharedTempPointerProperties;
            properties[0].reset();
            properties[0].id = 0;

            final PointerCoords[] coords = gSharedTempPointerCoords;
            coords[0].reset();
            coords[0].setAxisValue(AXIS_X, x);
            coords[0].setAxisValue(AXIS_Y, y);

            event.initialize(action, actionButton, flags, modifiers, buttonState,
                    0, 0, INVALID_CURSOR_POSITION, INVALID_CURSOR_POSITION,
                    downTime, eventTime,
                    1, properties, coords);
            return event;
        }
    }

    /**
     * Create a new MotionEvent, copying from an existing one.
     */
    @Nonnull
    public static MotionEvent obtain(@Nonnull MotionEvent other) {
        MotionEvent ev = obtain();
        ev.copyFrom(other);
        return ev;
    }

    private void copyFrom(@Nonnull MotionEvent other) {
        mAction = other.mAction;
        mActionButton = other.mActionButton;
        mFlags = other.mFlags;
        mModifiers = other.mModifiers;
        mButtonState = other.mButtonState;
        mXOffset = other.mXOffset;
        mYOffset = other.mYOffset;
        mRawXCursorPosition = other.mRawXCursorPosition;
        mRawYCursorPosition = other.mRawYCursorPosition;
        mDownTime = other.mDownTime;
        mEventTime = other.mEventTime;
        mPointerProperties.clear();
        mPointerProperties.addAll(other.mPointerProperties);
        mPointerCoords.clear();
        mPointerCoords.addAll(other.mPointerCoords);
    }

    @SuppressWarnings("SameParameterValue")
    private void initialize(int action, int actionButton, int flags, int modifiers, int buttonState,
                            float xOffset, float yOffset, float rawXCursorPosition, float rawYCursorPosition,
                            long downTime, long eventTime, int pointerCount,
                            @Nonnull PointerProperties[] pointerProperties, @Nonnull PointerCoords[] pointerCoords) {
        if (pointerCount < 1) {
            throw new IllegalArgumentException("pointerCount must be at least 1");
        }
        if (pointerProperties.length < pointerCount) {
            throw new IllegalArgumentException("pointerProperties array must be large enough to hold all pointers");
        }
        if (pointerCoords.length < pointerCount) {
            throw new IllegalArgumentException("pointerCoords array must be large enough to hold all pointers");
        }
        mAction = action;
        mActionButton = actionButton;
        mFlags = flags;
        mModifiers = modifiers;
        mButtonState = buttonState;
        mXOffset = xOffset;
        mYOffset = yOffset;
        mRawXCursorPosition = rawXCursorPosition;
        mRawYCursorPosition = rawYCursorPosition;
        mDownTime = downTime;
        mEventTime = eventTime;
        mPointerProperties.clear();
        mPointerProperties.addElements(0, pointerProperties, 0, pointerCount);
        mPointerCoords.clear();
        mPointerCoords.addElements(0, pointerCoords, 0, pointerCount);
        updateCursorPosition();
    }

    /**
     * Calculate new cursor position for events from mouse. This is used to split, clamp and inject
     * events.
     *
     * <p>If the source is mouse, it sets cursor position to the centroid of all pointers because
     * InputReader maps multiple fingers on a touchpad to locations around cursor position in screen
     * coordinates so that the mouse cursor is at the centroid of all pointers.
     *
     * <p>If the source is not mouse it sets cursor position to NaN.
     */
    private void updateCursorPosition() {
        // to-add: check source is mouse
        float x = 0;
        float y = 0;

        final int pointerCount = getPointerCount();
        for (int i = 0; i < pointerCount; ++i) {
            x += getX(i);
            y += getY(i);
        }

        // If pointer count is 0, divisions below yield NaN, which is an acceptable result for this
        // corner case.
        x /= pointerCount;
        y /= pointerCount;
        setCursorPosition(x, y);
    }

    private void setCursorPosition(float x, float y) {
        mRawXCursorPosition = x - mXOffset;
        mRawYCursorPosition = y - mYOffset;
    }

    /**
     * Recycles the event. This object should not be ever used
     * after recycling.
     * <p>
     * This method should only be called by system.
     */
    @Override
    public final void recycle() {
        sPool.release(this);
    }

    /**
     * Return the kind of action being performed.
     * Consider using {@link #getActionMasked} and {@link #getActionIndex} to retrieve
     * the separate masked action and pointer index.
     *
     * @return The action, such as {@link #ACTION_DOWN} or
     * the combination of {@link #ACTION_POINTER_DOWN} with a shifted pointer index.
     */
    public final int getAction() {
        return mAction;
    }

    /**
     * Return the masked action being performed, without pointer index information.
     * Use {@link #getActionIndex} to return the index associated with pointer actions.
     *
     * @return The action, such as {@link #ACTION_DOWN} or {@link #ACTION_POINTER_DOWN}.
     */
    public final int getActionMasked() {
        return mAction & ACTION_MASK;
    }

    /**
     * For {@link #ACTION_POINTER_DOWN} or {@link #ACTION_POINTER_UP}
     * as returned by {@link #getActionMasked}, this returns the associated
     * pointer index.
     * The index may be used with {@link #getPointerId(int)},
     * {@link #getX(int)} and {@link #getY(int)} to get information about
     * the pointer that has gone down or up.
     *
     * @return The index associated with the action.
     */
    public final int getActionIndex() {
        return (mAction & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
    }

    /**
     * Returns true if this motion event is a touch event.
     * <p>
     * Specifically excludes pointer events with action {@link #ACTION_HOVER_MOVE},
     * {@link #ACTION_HOVER_ENTER}, {@link #ACTION_HOVER_EXIT}, {@link #ACTION_SCROLL},
     * {@link #ACTION_BUTTON_PRESS} or {@link #ACTION_BUTTON_RELEASE}, because they
     * are not actually touch events (the pointer is not down).
     * </p>
     *
     * @return true if this motion event is a touch event.
     */
    public final boolean isTouchEvent() {
        // to-add: check source is a pointer device
        switch (mAction & ACTION_MASK) {
            case ACTION_DOWN:
            case ACTION_UP:
            case ACTION_MOVE:
            case ACTION_CANCEL:
            case ACTION_OUTSIDE:
            case ACTION_POINTER_DOWN:
            case ACTION_POINTER_UP:
                return true;
        }
        return false;
    }

    /**
     * Sets this event's action.
     */
    public final void setAction(int action) {
        mAction = action;
    }

    /**
     * Gets the state of all buttons that are pressed such as a mouse,
     * use an AND operation to get the state of a button.
     *
     * @return The button state.
     * @see #BUTTON_PRIMARY
     * @see #BUTTON_SECONDARY
     * @see #BUTTON_TERTIARY
     * @see #BUTTON_BACK
     * @see #BUTTON_FORWARD
     * @see #isButtonPressed(int)
     */
    public final int getButtonState() {
        return mButtonState;
    }

    /**
     * Checks if a mouse button (or combination of buttons) is pressed.
     *
     * @param button Button (or combination of buttons).
     * @return {@code true} if specified buttons are pressed.
     * @see #BUTTON_PRIMARY
     * @see #BUTTON_SECONDARY
     * @see #BUTTON_TERTIARY
     * @see #BUTTON_BACK
     * @see #BUTTON_FORWARD
     * @see #getButtonState()
     */
    public final boolean isButtonPressed(int button) {
        if (button == 0) {
            return false;
        }
        return (mButtonState & button) == button;
    }

    /**
     * The number of pointers of data contained in this event.  Always
     * >= 1.
     */
    public final int getPointerCount() {
        return mPointerProperties.size();
    }

    /**
     * Return the pointer identifier associated with a particular pointer
     * data index in this event.  The identifier tells you the actual pointer
     * number associated with the data, accounting for individual pointers
     * going up and down since the start of the current gesture.
     *
     * @param pointerIndex Raw index of pointer to retrieve.  Value may be from 0
     *                     (the first pointer that is down) to {@link #getPointerCount()}-1.
     */
    public final int getPointerId(int pointerIndex) {
        if (pointerIndex < 0 || pointerIndex >= getPointerCount()) {
            throw new IllegalArgumentException("pointerIndex out of range");
        }
        return mPointerProperties.get(pointerIndex).id;
    }

    /**
     * Gets the tool type of a pointer for the given pointer index.
     * The tool type indicates the type of tool used to make contact such
     * as a finger or stylus, if known.
     *
     * @param pointerIndex Raw index of pointer to retrieve.  Value may be from 0
     *                     (the first pointer that is down) to {@link #getPointerCount()}-1.
     * @return The tool type of the pointer.
     * @see #TOOL_TYPE_UNKNOWN
     * @see #TOOL_TYPE_FINGER
     * @see #TOOL_TYPE_STYLUS
     * @see #TOOL_TYPE_MOUSE
     */
    public final int getToolType(int pointerIndex) {
        if (pointerIndex < 0 || pointerIndex >= getPointerCount()) {
            throw new IllegalArgumentException("pointerIndex out of range");
        }
        return mPointerProperties.get(pointerIndex).toolType;
    }

    public final boolean isHoverExitPending() {
        return (mFlags & FLAG_HOVER_EXIT_PENDING) != 0;
    }

    public void setHoverExitPending(boolean hoverExitPending) {
        mFlags = hoverExitPending
                ? mFlags | FLAG_HOVER_EXIT_PENDING
                : mFlags & ~FLAG_HOVER_EXIT_PENDING;
    }

    @Nonnull
    @Override
    public MotionEvent copy() {
        return obtain(this);
    }

    /**
     * Returns the time (in ms) when the user originally pressed down to start
     * a stream of position events.
     */
    public final long getDownTime() {
        return mDownTime / 1000000;
    }

    @Override
    public long getEventTime() {
        return mEventTime / 1000000;
    }

    @Override
    public long getEventTimeNano() {
        return mEventTime;
    }

    @Override
    public void cancel() {
        setAction(ACTION_CANCEL);
    }

    /*private long getEventTimeNanos(int historyPos) {
        if (historyPos == HISTORY_CURRENT) {
            return mSampleEventTimes.getLong(getHistorySize());
        } else {
            if (historyPos < 0 || historyPos >= getHistorySize()) {
                throw new IllegalArgumentException("historyPos out of range");
            }
            return mSampleEventTimes.getLong(historyPos);
        }
    }*/

    private PointerCoords getRawPointerCoords(int pointerIndex) {
        return mPointerCoords.get(pointerIndex);
    }

    /*private PointerCoords getHistoricalRawPointerCoords(int pointerIndex, int historyIndex) {
        return mSamplePointerCoords.get(historyIndex * getPointerCount() + pointerIndex);
    }*/

    /*private float getAxisValue(int axis, int pointerIndex, int historyPos) {
        if (pointerIndex < 0 || pointerIndex >= getPointerCount()) {
            throw new IllegalArgumentException("pointerIndex out of range");
        }
        if (historyPos == HISTORY_CURRENT) {
            return getAxisValue(axis, pointerIndex);
        } else {
            if (historyPos < 0 || historyPos >= getHistorySize()) {
                throw new IllegalArgumentException("historyPos out of range");
            }
            return getHistoricalAxisValue(axis, pointerIndex, historyPos);
        }
    }*/

    private float getRawAxisValue(int axis, int pointerIndex) {
        if (pointerIndex < 0 || pointerIndex >= getPointerCount()) {
            throw new IllegalArgumentException("pointerIndex out of range");
        }
        return getRawPointerCoords(pointerIndex).getAxisValue(axis);
    }

    private float getAxisValue(int axis, int pointerIndex) {
        if (pointerIndex < 0 || pointerIndex >= getPointerCount()) {
            throw new IllegalArgumentException("pointerIndex out of range");
        }
        float value = getRawPointerCoords(pointerIndex).getAxisValue(axis);
        switch (axis) {
            case AXIS_X:
                return value + mXOffset;
            case AXIS_Y:
                return value + mYOffset;
        }
        return value;
    }

    // internal method
    public void setRawAxisValue(int axis, float value) {
        getRawPointerCoords(0).setAxisValue(axis, value);
    }

    // internal method
    public void setRawAxisValue(int axis, float value, int pointerIndex) {
        if (pointerIndex < 0 || pointerIndex >= getPointerCount()) {
            throw new IllegalArgumentException("pointerIndex out of range");
        }
        getRawPointerCoords(pointerIndex).setAxisValue(axis, value);
    }

    /*private float getHistoricalAxisValue(int axis, int pointerIndex, int historyIndex) {
        float value = getHistoricalRawPointerCoords(pointerIndex, historyIndex).getAxisValue(axis);
        switch (axis) {
            case AXIS_X:
                return value + mXOffset;
            case AXIS_Y:
                return value + mYOffset;
        }
        return value;
    }*/

    /**
     * Get axis value for the first pointer index (may be an
     * arbitrary pointer identifier).
     *
     * @param axis The axis identifier for the axis value to retrieve.
     * @see #AXIS_X
     * @see #AXIS_Y
     */
    public final float getAxisValue(int axis) {
        return getAxisValue(axis, 0);
    }

    /**
     * {@link #getX(int)} for the first pointer index (may be an
     * arbitrary pointer identifier).
     *
     * @see #AXIS_X
     */
    public final float getX() {
        return getAxisValue(AXIS_X, 0);
    }

    /**
     * {@link #getY(int)} for the first pointer index (may be an
     * arbitrary pointer identifier).
     *
     * @see #AXIS_Y
     */
    public final float getY() {
        return getAxisValue(AXIS_Y, 0);
    }

    /**
     * Returns the X coordinate of this event for the given pointer
     * <em>index</em> (use {@link #getPointerId(int)} to find the pointer
     * identifier for this index).
     * Whole numbers are pixels; the
     * value may have a fraction for input devices that are sub-pixel precise.
     *
     * @param pointerIndex Raw index of pointer to retrieve.  Value may be from 0
     *                     (the first pointer that is down) to {@link #getPointerCount()}-1.
     * @see #AXIS_X
     */
    public final float getX(int pointerIndex) {
        return getAxisValue(AXIS_X, pointerIndex);
    }

    /**
     * Returns the Y coordinate of this event for the given pointer
     * <em>index</em> (use {@link #getPointerId(int)} to find the pointer
     * identifier for this index).
     * Whole numbers are pixels; the
     * value may have a fraction for input devices that are sub-pixel precise.
     *
     * @param pointerIndex Raw index of pointer to retrieve.  Value may be from 0
     *                     (the first pointer that is down) to {@link #getPointerCount()}-1.
     * @see #AXIS_Y
     */
    public final float getY(int pointerIndex) {
        return getAxisValue(AXIS_Y, pointerIndex);
    }

    /**
     * Returns the original raw X coordinate of this event.  For touch
     * events on the screen, this is the original location of the event
     * on the screen, before it had been adjusted for the containing window
     * and views.
     *
     * @see #getX(int)
     * @see #AXIS_X
     */
    public final float getRawX() {
        return getRawAxisValue(AXIS_X, 0);
    }

    /**
     * Returns the original raw Y coordinate of this event.  For touch
     * events on the screen, this is the original location of the event
     * on the screen, before it had been adjusted for the containing window
     * and views.
     *
     * @see #getY(int)
     * @see #AXIS_Y
     */
    public final float getRawY() {
        return getRawAxisValue(AXIS_Y, 0);
    }

    /**
     * Returns the original raw X coordinate of this event.  For touch
     * events on the screen, this is the original location of the event
     * on the screen, before it had been adjusted for the containing window
     * and views.
     *
     * @param pointerIndex Raw index of pointer to retrieve.  Value may be from 0
     *                     (the first pointer that is down) to {@link #getPointerCount()}-1.
     * @see #getX(int)
     * @see #AXIS_X
     */
    public float getRawX(int pointerIndex) {
        return getRawAxisValue(AXIS_X, pointerIndex);
    }

    /**
     * Returns the original raw Y coordinate of this event.  For touch
     * events on the screen, this is the original location of the event
     * on the screen, before it had been adjusted for the containing window
     * and views.
     *
     * @param pointerIndex Raw index of pointer to retrieve.  Value may be from 0
     *                     (the first pointer that is down) to {@link #getPointerCount()}-1.
     * @see #getY(int)
     * @see #AXIS_Y
     */
    public float getRawY(int pointerIndex) {
        return getRawAxisValue(AXIS_Y, pointerIndex);
    }

    /**
     * Gets which button has been modified during a press or release action.
     * <p>
     * For actions other than {@link #ACTION_BUTTON_PRESS} and {@link #ACTION_BUTTON_RELEASE}
     * the returned value is undefined.
     *
     * @see #getButtonState()
     */
    public int getActionButton() {
        return mActionButton;
    }

    /**
     * Returns the x coordinate of mouse cursor position when this event is
     * reported. This value is only valid if the device is a mouse.
     */
    public float getXCursorPosition() {
        return mRawXCursorPosition + mXOffset;
    }

    /**
     * Returns the y coordinate of mouse cursor position when this event is
     * reported. This value is only valid if the device is a mouse.
     */
    public float getYCursorPosition() {
        return mRawYCursorPosition + mYOffset;
    }

    /**
     * Adjust this event's location.
     *
     * @param deltaX Amount to add to the current X coordinate of the event.
     * @param deltaY Amount to add to the current Y coordinate of the event.
     */
    public final void offsetLocation(float deltaX, float deltaY) {
        mXOffset += deltaX;
        mYOffset += deltaY;
    }

    /**
     * Set this event's location.  Applies {@link #offsetLocation} with a
     * delta from the current location to the given new location.
     *
     * @param x New absolute X location.
     * @param y New absolute Y location.
     */
    public final void setLocation(float x, float y) {
        float oldX = getX();
        float oldY = getY();
        offsetLocation(x - oldX, y - oldY);
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
        return (mModifiers & modifiers) == modifiers;
    }

    /**
     * Returns the pressed state of the SHIFT key.
     *
     * @return true if the SHIFT key is pressed, false otherwise
     */
    public final boolean isShiftPressed() {
        return (mModifiers & GLFW.GLFW_MOD_SHIFT) != 0;
    }

    /**
     * Returns the pressed state of the CTRL key. If it's running on OSX,
     * returns the pressed state of the SUPER key.
     *
     * @return true if the CTRL key is pressed, false otherwise
     */
    public final boolean isCtrlPressed() {
        if (Platform.get() == Platform.MACOSX) {
            return (mModifiers & GLFW.GLFW_MOD_SUPER) != 0;
        }
        return (mModifiers & GLFW.GLFW_MOD_CONTROL) != 0;
    }

    /**
     * Returns the pressed state of the ALT key.
     *
     * @return true if the ALT key is pressed, false otherwise
     */
    public final boolean isAltPressed() {
        return (mModifiers & GLFW.GLFW_MOD_ALT) != 0;
    }

    /**
     * Returns the pressed state of the SUPER key (a.k.a META or WIN key).
     *
     * @return true if the SUPER key is pressed, false otherwise
     */
    public final boolean isSuperPressed() {
        return (mModifiers & GLFW.GLFW_MOD_SUPER) != 0;
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

    @Nonnull
    @Override
    public String toString() {
        StringBuilder msg = new StringBuilder();
        msg.append("MotionEvent{action=")
                .append(actionToString(getAction()));
        msg.append(", x=")
                .append(getX());
        msg.append(", y=")
                .append(getY());
        if (mFlags != 0) {
            msg.append(", flags=0x")
                    .append(Integer.toHexString(mFlags));
        }
        msg.append(", eventTime=")
                .append(getEventTime());
        msg.append("}");
        return msg.toString();
    }

    /**
     * Returns a string that represents the symbolic name of the specified unmasked action
     * such as "ACTION_DOWN", "ACTION_POINTER_DOWN(3)" or an equivalent numeric constant
     * such as "35" if unknown.
     *
     * @param action The unmasked action.
     * @return The symbolic name of the specified action.
     * @see #getAction()
     */
    public static String actionToString(int action) {
        switch (action) {
            case ACTION_DOWN:
                return "ACTION_DOWN";
            case ACTION_UP:
                return "ACTION_UP";
            case ACTION_CANCEL:
                return "ACTION_CANCEL";
            case ACTION_OUTSIDE:
                return "ACTION_OUTSIDE";
            case ACTION_MOVE:
                return "ACTION_MOVE";
            case ACTION_HOVER_MOVE:
                return "ACTION_HOVER_MOVE";
            case ACTION_SCROLL:
                return "ACTION_SCROLL";
            case ACTION_HOVER_ENTER:
                return "ACTION_HOVER_ENTER";
            case ACTION_HOVER_EXIT:
                return "ACTION_HOVER_EXIT";
            case ACTION_BUTTON_PRESS:
                return "ACTION_BUTTON_PRESS";
            case ACTION_BUTTON_RELEASE:
                return "ACTION_BUTTON_RELEASE";
        }
        int index = (action & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
        switch (action & ACTION_MASK) {
            case ACTION_POINTER_DOWN:
                return "ACTION_POINTER_DOWN(" + index + ")";
            case ACTION_POINTER_UP:
                return "ACTION_POINTER_UP(" + index + ")";
            default:
                return Integer.toString(action);
        }
    }

    /**
     * Transfer object for pointer coordinates.
     * <p>
     * Objects of this type can be used to specify the pointer coordinates when
     * creating new {@link MotionEvent} objects and to query pointer coordinates
     * in bulk.
     */
    public static final class PointerCoords {

        private static final int INITIAL_PACKED_AXIS_VALUES = 8;

        private long mPackedAxisBits;
        private float[] mPackedAxisValues;

        /**
         * Creates a pointer coords object with all axes initialized to zero.
         */
        public PointerCoords() {

        }

        /**
         * Creates a pointer coords object as a copy of the
         * contents of another pointer coords object.
         *
         * @param other The pointer coords object to copy.
         */
        public PointerCoords(PointerCoords other) {
            copyFrom(other);
        }

        @Nonnull
        public static PointerCoords[] createArray(int size) {
            PointerCoords[] array = new PointerCoords[size];
            for (int i = 0; i < size; i++) {
                array[i] = new PointerCoords();
            }
            return array;
        }

        /**
         * Clears the contents of this object.
         * Resets all axes to zero.
         */
        public void reset() {
            mPackedAxisBits = 0;
        }

        /**
         * Copies the contents of another pointer coords object.
         *
         * @param other The pointer coords object to copy.
         */
        public void copyFrom(@Nonnull PointerCoords other) {
            final long bits = other.mPackedAxisBits;
            mPackedAxisBits = bits;
            if (bits != 0) {
                final float[] otherValues = other.mPackedAxisValues;
                final int count = Long.bitCount(bits);
                float[] values = mPackedAxisValues;
                if (values == null || count > values.length) {
                    values = new float[otherValues.length];
                    mPackedAxisValues = values;
                }
                System.arraycopy(otherValues, 0, values, 0, count);
            }
        }

        /**
         * Gets the value associated with the specified axis.
         *
         * @param axis The axis identifier for the axis value to retrieve.
         * @return The value associated with the axis, or 0 if none.
         * @see MotionEvent#AXIS_X
         * @see MotionEvent#AXIS_Y
         */
        public float getAxisValue(int axis) {
            if (axis < 0 || axis > 63) {
                throw new IllegalArgumentException("Axis out of range.");
            }
            final long bits = mPackedAxisBits;
            final long axisBit = 0x8000000000000000L >>> axis;
            if ((bits & axisBit) == 0) {
                return 0;
            }
            final int index = Long.bitCount(bits & ~(0xFFFFFFFFFFFFFFFFL >>> axis));
            return mPackedAxisValues[index];
        }

        /**
         * Sets the value associated with the specified axis.
         *
         * @param axis  The axis identifier for the axis value to assign.
         * @param value The value to set.
         * @see MotionEvent#AXIS_X
         * @see MotionEvent#AXIS_Y
         */
        public void setAxisValue(int axis, float value) {
            if (axis < 0 || axis > 63) {
                throw new IllegalArgumentException("Axis out of range.");
            }
            final long bits = mPackedAxisBits;
            final long axisBit = 0x8000000000000000L >>> axis;
            final int index = Long.bitCount(bits & ~(0xFFFFFFFFFFFFFFFFL >>> axis));
            float[] values = mPackedAxisValues;
            if ((bits & axisBit) == 0) {
                if (values == null) {
                    values = new float[INITIAL_PACKED_AXIS_VALUES];
                    mPackedAxisValues = values;
                } else {
                    final int count = Long.bitCount(bits);
                    if (count < values.length) {
                        if (index != count) {
                            System.arraycopy(values, index, values, index + 1,
                                    count - index);
                        }
                    } else {
                        float[] newValues = new float[count * 2];
                        System.arraycopy(values, 0, newValues, 0, index);
                        System.arraycopy(values, index, newValues, index + 1,
                                count - index);
                        values = newValues;
                        mPackedAxisValues = values;
                    }
                }
                mPackedAxisBits = bits | axisBit;
            }
            values[index] = value;
        }
    }

    /**
     * Transfer object for pointer properties.
     * <p>
     * Objects of this type can be used to specify the pointer id and tool type
     * when creating new {@link MotionEvent} objects and to query pointer properties in bulk.
     */
    private static final class PointerProperties {

        /**
         * The pointer id, range between 0 and 31.
         * Initially set to {@link #INVALID_POINTER_ID} (-1).
         *
         * @see MotionEvent#getPointerId(int)
         */
        public int id;

        /**
         * The pointer tool type.
         * Initially set to 0.
         *
         * @see MotionEvent#getToolType(int)
         */
        public int toolType;

        /**
         * Creates a pointer properties object with an invalid pointer id.
         */
        public PointerProperties() {
            reset();
        }

        /**
         * Creates a pointer properties object as a copy of the contents of
         * another pointer properties object.
         *
         * @param other The pointer properties object that copied from
         */
        public PointerProperties(PointerProperties other) {
            copyFrom(other);
        }

        @Nonnull
        public static PointerProperties[] createArray(int size) {
            PointerProperties[] array = new PointerProperties[size];
            for (int i = 0; i < size; i++) {
                array[i] = new PointerProperties();
            }
            return array;
        }

        /**
         * Resets the pointer properties to their initial values.
         */
        public void reset() {
            id = INVALID_POINTER_ID;
            toolType = TOOL_TYPE_UNKNOWN;
        }

        /**
         * Copies the contents of another pointer properties object.
         *
         * @param other The pointer properties object to copy.
         */
        public void copyFrom(@Nonnull PointerProperties other) {
            id = other.id;
            toolType = other.toolType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PointerProperties that = (PointerProperties) o;
            return id == that.id && toolType == that.toolType;
        }

        @Override
        public int hashCode() {
            return id | (toolType << 8);
        }
    }
}
