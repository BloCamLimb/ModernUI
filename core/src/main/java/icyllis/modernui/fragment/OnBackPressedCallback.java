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
import icyllis.modernui.lifecycle.LifecycleOwner;

import javax.annotation.Nonnull;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class for handling {@link OnBackPressedDispatcher#onBackPressed()} callbacks.
 * <p>
 * This class maintains its own {@link #isEnabled() enabled state}. Only when this callback
 * is enabled will it receive callbacks to {@link #handleOnBackPressed()}.
 * <p>
 * Note that the enabled state is an additional layer on top of the
 * {@link LifecycleOwner} passed to
 * {@link OnBackPressedDispatcher#addCallback(LifecycleOwner, OnBackPressedCallback)}
 * which controls when the callback is added and removed to the dispatcher.
 * <p>
 * By calling {@link #remove()}, this callback will be removed from any
 * {@link OnBackPressedDispatcher} it has been added to. It is strongly recommended
 * to instead disable this callback to handle temporary changes in state.
 */
public abstract class OnBackPressedCallback {

    private boolean mEnabled;
    private final CopyOnWriteArrayList<Cancellable> mCancellables = new CopyOnWriteArrayList<>();

    /**
     * Create a {@link OnBackPressedCallback}.
     *
     * @param enabled The default enabled state for this callback.
     * @see #setEnabled(boolean)
     */
    public OnBackPressedCallback(boolean enabled) {
        mEnabled = enabled;
    }

    /**
     * Set the enabled state of the callback. Only when this callback
     * is enabled will it receive callbacks to {@link #handleOnBackPressed()}.
     * <p>
     * Note that the enabled state is an additional layer on top of the
     * {@link LifecycleOwner} passed to
     * {@link OnBackPressedDispatcher#addCallback(LifecycleOwner, OnBackPressedCallback)}
     * which controls when the callback is added and removed to the dispatcher.
     *
     * @param enabled whether the callback should be considered enabled
     */
    @UiThread
    public final void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    /**
     * Checks whether this callback should be considered enabled. Only when this callback
     * is enabled will it receive callbacks to {@link #handleOnBackPressed()}.
     *
     * @return Whether this callback should be considered enabled.
     */
    @UiThread
    public final boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Removes this callback from any {@link OnBackPressedDispatcher} it is currently
     * added to.
     */
    @UiThread
    public final void remove() {
        for (Cancellable cancellable : mCancellables) {
            cancellable.cancel();
        }
    }

    /**
     * Callback for handling the {@link OnBackPressedDispatcher#onBackPressed()} event.
     */
    @UiThread
    public abstract void handleOnBackPressed();

    void addCancellable(@Nonnull Cancellable cancellable) {
        mCancellables.add(cancellable);
    }

    void removeCancellable(@Nonnull Cancellable cancellable) {
        mCancellables.remove(cancellable);
    }

    /**
     * Token representing a cancellable operation.
     */
    interface Cancellable {

        /**
         * Cancel the subscription. This call should be idempotent, making it safe to
         * call multiple times.
         */
        void cancel();
    }
}
