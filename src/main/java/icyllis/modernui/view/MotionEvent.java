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

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * An object that indicates movement events (mouse, touchpad etc)
 *
 * @see net.minecraft.client.MouseHelper
 */
public final class MotionEvent extends InputEvent {


    /**
     * Bit mask of the parts of the action code that are the action itself.
     */
    public static final int ACTION_MASK = 0xff;

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
     * root {@link View} or in the appropriate {@link Window.Callback}
     * (e.g. {@link android.app.Activity} or {@link android.app.Dialog}).
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
    public static final int ACTION_POINTER_DOWN = 5;

    /**
     * Constant for {@link #getActionMasked}: A non-primary pointer has gone up.
     * <p>
     * Use {@link #getActionIndex} to retrieve the index of the pointer that changed.
     * </p><p>
     * The index is encoded in the {@link #ACTION_POINTER_INDEX_MASK} bits of the
     * unmasked action returned by {@link #getAction}.
     * </p>
     */
    public static final int ACTION_POINTER_UP = 6;

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
    public static final int ACTION_BUTTON_PRESS = 11;

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
    public static final int ACTION_BUTTON_RELEASE = 12;

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
    public static final int ACTION_POINTER_INDEX_MASK = 0xff00;

    /**
     * Bit shift for the action bits holding the pointer index as
     * defined by {@link #ACTION_POINTER_INDEX_MASK}.
     *
     * @see #getActionIndex
     */
    public static final int ACTION_POINTER_INDEX_SHIFT = 8;


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

    public static final int ACTION_DRAG = 2;

    public static final int ACTION_PRESS = 4;
    public static final int ACTION_RELEASE = 5;
    public static final int ACTION_DOUBLE_CLICK = 6;

    private static final int MAX_RECYCLED = 10;
    private static final Object sRecyclerLock = new Object();
    private static MotionEvent sRecyclerTop;
    private static int sRecyclerUsed;

    private MotionEvent mNext;

    private int mAction;

    private int mButtonState;

    float x;
    float y;

    int button;

    double scrollDelta;

    final Map<Integer, View> pressMap = new Int2ObjectArrayMap<>();
    @Nullable
    View clicked;

    private MotionEvent() {

    }

    @Nonnull
    static MotionEvent obtain() {
        final MotionEvent event;
        synchronized (sRecyclerLock) {
            event = sRecyclerTop;
            if (event == null) {
                return new MotionEvent();
            }
            sRecyclerTop = event.mNext;
            sRecyclerUsed--;
        }
        event.mNext = null;
        event.prepareForReuse();
        return event;
    }

    /**
     * Recycles the event. This object should not be ever used
     * after recycling.
     * <p>
     * This method should only be called by system.
     */
    @Override
    public final void recycle() {
        super.recycle();
        synchronized (sRecyclerLock) {
            if (sRecyclerUsed < MAX_RECYCLED) {
                sRecyclerUsed++;
                mNext = sRecyclerTop;
                sRecyclerTop = this;
            }
        }
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
     * {@link #getX(int)}, {@link #getY(int)}, {@link #getPressure(int)},
     * and {@link #getSize(int)} to get information about the pointer that has
     * gone down or up.
     *
     * @return The index associated with the action.
     */
    public final int getActionIndex() {
        return (mAction & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
    }

    /**
     * Sets this event's action.
     */
    public final void setAction(int action) {
        mAction = action;
    }


    /**
     * Gets the state of all buttons that are pressed such as a mouse,
     * use an OR operation to get the state of a button.
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
     * Checks if a mouse or stylus button (or combination of buttons) is pressed.
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

    @Override
    public InputEvent copy() {
        return null;
    }

    @Override
    public long getEventTime() {
        return 0;
    }

    @Override
    public long getEventTimeNano() {
        return 0;
    }

    @Override
    public void cancel() {

    }

    /*
     * Returns the action type of this event
     *
     * @return either {@link #TYPE_DYNAMIC} or {@link #TYPE_OPERATION} or {@link #TYPE_NOTIFY}
     */
    /*public int getType() {
        return action >> TYPE_SHIFT;
    }*/

    /*
     * Returns the action of this event
     *
     * @return action, such as {@link #ACTION_PRESS}
     */
    /*public int getAction() {
        return action;
    }*/

    /**
     * Returns the X coordinate of this event,
     *
     * @return X
     */
    public float getX() {
        return x;
    }

    /**
     * Returns the Y coordinate of this event,
     *
     * @return Y
     */
    public float getY() {
        return y;
    }

    /*
     * Returns the original raw X coordinate of this event.  For touch
     * events on the screen, this is the original location of the event
     * on the screen, before it had been adjusted for the containing window
     * and views.
     *
     * @return raw X
     */
    /*public double getRawX() {
        return rawX;
    }*/

    /*
     * Returns the original raw Y coordinate of this event.  For touch
     * events on the screen, this is the original location of the event
     * on the screen, before it had been adjusted for the containing window
     * and views.
     *
     * @return raw Y
     */
    /*public double getRawY() {
        return rawY;
    }*/

    /**
     * Returns the mouse button of this event.
     *
     * @return mouse button
     * @see org.lwjgl.glfw.GLFW
     */
    public int getButton() {
        return button;
    }
}
