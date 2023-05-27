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

package icyllis.modernui.widget;

import icyllis.modernui.core.Core;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewConfiguration;
import icyllis.modernui.view.ViewParent;
import icyllis.modernui.view.menu.ShowableListMenu;

import javax.annotation.Nonnull;

/**
 * Abstract class that forwards touch events to a {@link ShowableListMenu}.
 */
public abstract class ForwardingListener implements View.OnTouchListener, View.OnAttachStateChangeListener {

    /**
     * Scaled touch slop, used for detecting movement outside bounds.
     */
    private final float mScaledTouchSlop;

    /**
     * Timeout before disallowing intercept on the source's parent.
     */
    private final int mTapTimeout;

    /**
     * Timeout before accepting a long-press to start forwarding.
     */
    private final int mLongPressTimeout;

    /**
     * Source view from which events are forwarded.
     */
    private final View mView;

    /**
     * Runnable used to prevent conflicts with scrolling parents.
     */
    private Runnable mDisallowIntercept;

    /**
     * Runnable used to trigger forwarding on long-press.
     */
    private Runnable mTriggerLongPress;

    /**
     * Whether this listener is currently forwarding touch events.
     */
    private boolean mForwarding;

    public ForwardingListener(@Nonnull View view) {
        mView = view;
        view.setLongClickable(true);
        view.addOnAttachStateChangeListener(this);

        mScaledTouchSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
        mTapTimeout = ViewConfiguration.getTapTimeout();

        // Use a medium-press timeout. Halfway between tap and long-press.
        mLongPressTimeout = (mTapTimeout + ViewConfiguration.getLongPressTimeout()) / 2;
    }

    /**
     * Returns the popup to which this listener is forwarding events.
     * <p>
     * Override this to return the correct popup. If the popup is displayed
     * asynchronously, you may also need to override
     * {@link #onForwardingStopped} to prevent premature cancellation of
     * forwarding.
     *
     * @return the popup to which this listener is forwarding events
     */
    public abstract ShowableListMenu getPopup();

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final boolean wasForwarding = mForwarding;
        final boolean forwarding;
        if (wasForwarding) {
            forwarding = onTouchForwarded(event) || !onForwardingStopped();
        } else {
            forwarding = onTouchObserved(event) && onForwardingStarted();

            if (forwarding) {
                // Make sure we cancel any ongoing source event stream.
                final long now = Core.timeNanos();
                final MotionEvent e = MotionEvent.obtain(now, MotionEvent.ACTION_CANCEL,
                        0.0f, 0.0f, 0);
                mView.onTouchEvent(e);
                e.recycle();
            }
        }

        mForwarding = forwarding;
        return forwarding || wasForwarding;
    }

    @Override
    public void onViewAttachedToWindow(View v) {
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        mForwarding = false;

        if (mDisallowIntercept != null) {
            mView.removeCallbacks(mDisallowIntercept);
        }
    }

    /**
     * Called when forwarding would like to start.
     * <p>
     * By default, this will show the popup returned by {@link #getPopup()}.
     * It may be overridden to perform another action, like clicking the
     * source view or preparing the popup before showing it.
     *
     * @return true to start forwarding, false otherwise
     */
    protected boolean onForwardingStarted() {
        final ShowableListMenu popup = getPopup();
        if (popup != null && !popup.isShowing()) {
            popup.show();
        }
        return true;
    }

    /**
     * Called when forwarding would like to stop.
     * <p>
     * By default, this will dismiss the popup returned by
     * {@link #getPopup()}. It may be overridden to perform some other
     * action.
     *
     * @return true to stop forwarding, false otherwise
     */
    protected boolean onForwardingStopped() {
        final ShowableListMenu popup = getPopup();
        if (popup != null && popup.isShowing()) {
            popup.dismiss();
        }
        return true;
    }

    /**
     * Observes motion events and determines when to start forwarding.
     *
     * @param event motion event in source view coordinates
     * @return true to start forwarding motion events, false otherwise
     */
    private boolean onTouchObserved(MotionEvent event) {
        final View view = mView;
        if (!view.isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN -> {
                if (mDisallowIntercept == null) {
                    mDisallowIntercept = new DisallowIntercept();
                }
                view.postDelayed(mDisallowIntercept, mTapTimeout);
                if (mTriggerLongPress == null) {
                    mTriggerLongPress = new TriggerLongPress();
                }
                view.postDelayed(mTriggerLongPress, mLongPressTimeout);
            }
            case MotionEvent.ACTION_MOVE -> {
                final float x = event.getX();
                final float y = event.getY();

                // Has the pointer moved outside of the view?
                if (!view.pointInView(x, y, mScaledTouchSlop)) {
                    clearCallbacks();

                    // Don't let the parent intercept our events.
                    final ViewParent parent = view.getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    return true;
                }
            }
            case MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> clearCallbacks();
        }

        return false;
    }

    private void clearCallbacks() {
        if (mTriggerLongPress != null) {
            mView.removeCallbacks(mTriggerLongPress);
        }

        if (mDisallowIntercept != null) {
            mView.removeCallbacks(mDisallowIntercept);
        }
    }

    private void onLongPress() {
        clearCallbacks();

        final View view = mView;
        if (!view.isEnabled() || view.isLongClickable()) {
            // Ignore long-press if the view is disabled or has its own
            // handler.
            return;
        }

        if (!onForwardingStarted()) {
            return;
        }

        // Don't let the parent intercept our events.
        final ViewParent parent = view.getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }

        // Make sure we cancel any ongoing source event stream.
        final long now = Core.timeNanos();
        final MotionEvent e = MotionEvent.obtain(now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
        view.onTouchEvent(e);
        e.recycle();

        mForwarding = true;
    }

    /**
     * Handles forwarded motion events and determines when to stop
     * forwarding.
     *
     * @param event motion event in source view coordinates
     * @return true to continue forwarding motion events, false to cancel
     */
    private boolean onTouchForwarded(MotionEvent event) {
        final ShowableListMenu popup = getPopup();
        if (popup == null || !popup.isShowing()) {
            return false;
        }

        final DropDownListView target = (DropDownListView) popup.getListView();
        if (target == null || !target.isShown()) {
            return false;
        }

        // Convert event to destination-local coordinates.
        final MotionEvent targetEvent = event.copy();
        mView.toGlobalMotionEvent(targetEvent);
        target.toLocalMotionEvent(targetEvent);

        // Forward converted event to destination view, then recycle it.
        final boolean handled = target.onForwardedEvent(targetEvent);
        targetEvent.recycle();

        // Always cancel forwarding when the touch stream ends.
        final int action = event.getAction();
        final boolean keepForwarding = action != MotionEvent.ACTION_UP
                && action != MotionEvent.ACTION_CANCEL;

        return handled && keepForwarding;
    }

    private class DisallowIntercept implements Runnable {

        @Override
        public void run() {
            final ViewParent parent = mView.getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
        }
    }

    private class TriggerLongPress implements Runnable {

        @Override
        public void run() {
            onLongPress();
        }
    }
}
