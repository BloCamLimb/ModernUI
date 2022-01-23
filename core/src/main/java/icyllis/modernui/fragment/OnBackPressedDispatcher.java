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

package icyllis.modernui.fragment;

import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.lifecycle.Lifecycle;
import icyllis.modernui.lifecycle.LifecycleObserver;
import icyllis.modernui.lifecycle.LifecycleOwner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Dispatcher that can be used to register {@link OnBackPressedCallback} instances for handling
 * the <code>host.onBackPressed()</code> callback via composition.
 */
public final class OnBackPressedDispatcher {

    @Nullable
    private final Runnable mFallbackOnBackPressed;

    final ArrayDeque<OnBackPressedCallback> mOnBackPressedCallbacks = new ArrayDeque<>();

    /**
     * Create a new OnBackPressedDispatcher that dispatches System back button pressed events
     * to one or more {@link OnBackPressedCallback} instances.
     */
    public OnBackPressedDispatcher() {
        this(null);
    }

    /**
     * Create a new OnBackPressedDispatcher that dispatches System back button pressed events
     * to one or more {@link OnBackPressedCallback} instances.
     *
     * @param fallbackOnBackPressed The Runnable that should be triggered if
     *                              {@link #onBackPressed()} is called when {@link #hasEnabledCallbacks()} returns
     *                              false.
     */
    public OnBackPressedDispatcher(@Nullable Runnable fallbackOnBackPressed) {
        mFallbackOnBackPressed = fallbackOnBackPressed;
    }

    /**
     * Add a new {@link OnBackPressedCallback}. Callbacks are invoked in the reverse order in which
     * they are added, so this newly added {@link OnBackPressedCallback} will be the first
     * callback to receive a callback if {@link #onBackPressed()} is called.
     * <p>
     * This method is <strong>not</strong> {@link Lifecycle} aware - if you'd like to ensure that
     * you only get callbacks when at least {@link Lifecycle.State#STARTED started}, use
     * {@link #addCallback(LifecycleOwner, OnBackPressedCallback)}. It is expected that you
     * call {@link OnBackPressedCallback#remove()} to manually remove your callback.
     *
     * @param onBackPressedCallback The callback to add
     * @see #onBackPressed()
     */
    @UiThread
    public void addCallback(@Nonnull OnBackPressedCallback onBackPressedCallback) {
        addCancellableCallback(onBackPressedCallback);
    }

    /**
     * Internal implementation of {@link #addCallback(OnBackPressedCallback)} that gives
     * access to the {@link OnBackPressedCallback.Cancellable} that specifically removes this callback from
     * the dispatcher without relying on {@link OnBackPressedCallback#remove()} which
     * is what external developers should be using.
     *
     * @param onBackPressedCallback The callback to add
     * @return a {@link OnBackPressedCallback.Cancellable} which can be used to {@link
     * OnBackPressedCallback.Cancellable#cancel() cancel}
     * the callback and remove it from the set of OnBackPressedCallbacks.
     */
    @UiThread
    @Nonnull
    OnBackPressedCallback.Cancellable addCancellableCallback(@Nonnull OnBackPressedCallback onBackPressedCallback) {
        mOnBackPressedCallbacks.add(onBackPressedCallback);
        OnBackPressedCancellable cancellable = new OnBackPressedCancellable(onBackPressedCallback);
        onBackPressedCallback.addCancellable(cancellable);
        return cancellable;
    }

    /**
     * Receive callbacks to a new {@link OnBackPressedCallback} when the given
     * {@link LifecycleOwner} is at least {@link Lifecycle.State#STARTED started}.
     * <p>
     * This will automatically call {@link #addCallback(OnBackPressedCallback)} and
     * remove the callback as the lifecycle state changes.
     * As a corollary, if your lifecycle is already at least
     * {@link Lifecycle.State#STARTED started}, calling this method will result in an immediate
     * call to {@link #addCallback(OnBackPressedCallback)}.
     * <p>
     * When the {@link LifecycleOwner} is {@link Lifecycle.State#DESTROYED destroyed}, it will
     * automatically be removed from the list of callbacks. The only time you would need to
     * manually call {@link OnBackPressedCallback#remove()} is if
     * you'd like to remove the callback prior to destruction of the associated lifecycle.
     *
     * <p>
     * If the Lifecycle is already {@link Lifecycle.State#DESTROYED destroyed}
     * when this method is called, the callback will not be added.
     *
     * @param owner    The LifecycleOwner which controls when the callback should be invoked
     * @param callback The callback to add
     * @see #onBackPressed()
     */
    @UiThread
    public void addCallback(@Nonnull LifecycleOwner owner, @Nonnull OnBackPressedCallback callback) {
        Lifecycle lifecycle = owner.getLifecycle();
        if (lifecycle.getCurrentState() == Lifecycle.State.DESTROYED) {
            return;
        }
        callback.addCancellable(new LifecycleOnBackPressedCancellable(lifecycle, callback));
    }

    /**
     * Checks if there is at least one {@link OnBackPressedCallback#isEnabled enabled}
     * callback registered with this dispatcher.
     *
     * @return True if there is at least one enabled callback.
     */
    @UiThread
    public boolean hasEnabledCallbacks() {
        Iterator<OnBackPressedCallback> iterator =
                mOnBackPressedCallbacks.descendingIterator();
        while (iterator.hasNext()) {
            if (iterator.next().isEnabled()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Trigger a call to the currently added {@link OnBackPressedCallback callbacks} in reverse
     * order in which they were added. Only if the most recently added callback is not
     * {@link OnBackPressedCallback#isEnabled() enabled}
     * will any previously added callback be called.
     * <p>
     * If {@link #hasEnabledCallbacks()} is <code>false</code> when this method is called, the
     * fallback Runnable set by {@link #OnBackPressedDispatcher(Runnable) the constructor}
     * will be triggered.
     */
    @UiThread
    public void onBackPressed() {
        Iterator<OnBackPressedCallback> iterator =
                mOnBackPressedCallbacks.descendingIterator();
        while (iterator.hasNext()) {
            OnBackPressedCallback callback = iterator.next();
            if (callback.isEnabled()) {
                callback.handleOnBackPressed();
                return;
            }
        }
        if (mFallbackOnBackPressed != null) {
            mFallbackOnBackPressed.run();
        }
    }

    private class OnBackPressedCancellable implements OnBackPressedCallback.Cancellable {

        private final OnBackPressedCallback mOnBackPressedCallback;

        OnBackPressedCancellable(OnBackPressedCallback onBackPressedCallback) {
            mOnBackPressedCallback = onBackPressedCallback;
        }

        @Override
        public void cancel() {
            mOnBackPressedCallbacks.remove(mOnBackPressedCallback);
            mOnBackPressedCallback.removeCancellable(this);
        }
    }

    private class LifecycleOnBackPressedCancellable implements LifecycleObserver, OnBackPressedCallback.Cancellable {

        private final Lifecycle mLifecycle;
        private final OnBackPressedCallback mOnBackPressedCallback;

        @Nullable
        private OnBackPressedCallback.Cancellable mCurrentCancellable;

        LifecycleOnBackPressedCancellable(@Nonnull Lifecycle lifecycle,
                                          @Nonnull OnBackPressedCallback onBackPressedCallback) {
            mLifecycle = lifecycle;
            mOnBackPressedCallback = onBackPressedCallback;
            lifecycle.addObserver(this);
        }

        @Override
        public void onStateChanged(@Nonnull LifecycleOwner source, @Nonnull Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_START) {
                mCurrentCancellable = addCancellableCallback(mOnBackPressedCallback);
            } else if (event == Lifecycle.Event.ON_STOP) {
                // Should always be non-null
                if (mCurrentCancellable != null) {
                    mCurrentCancellable.cancel();
                }
            } else if (event == Lifecycle.Event.ON_DESTROY) {
                cancel();
            }
        }

        @Override
        public void cancel() {
            mLifecycle.removeObserver(this);
            mOnBackPressedCallback.removeCancellable(this);
            if (mCurrentCancellable != null) {
                mCurrentCancellable.cancel();
                mCurrentCancellable = null;
            }
        }
    }
}
