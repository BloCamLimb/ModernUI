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

package icyllis.modernui.lifecycle;

import icyllis.modernui.annotation.NonNull;

/**
 * Callback interface for listening to {@link LifecycleOwner} state changes.
 * <p>
 * Methods like {@link #onCreate(LifecycleOwner)} will be called first, and then
 * followed by the call of {@link #onStateChanged(LifecycleOwner, Lifecycle.Event)}.
 */
public interface LifecycleObserver {

    /**
     * Notifies that {@code ON_CREATE} event occurred.
     * <p>
     * This method will be called after the {@link LifecycleOwner}'s {@code onCreate}
     * method returns.
     *
     * @param owner the component, whose state was changed
     */
    default void onCreate(@NonNull LifecycleOwner owner) {
    }

    /**
     * Notifies that {@code ON_START} event occurred.
     * <p>
     * This method will be called after the {@link LifecycleOwner}'s {@code onStart} method returns.
     *
     * @param owner the component, whose state was changed
     */
    default void onStart(@NonNull LifecycleOwner owner) {
    }

    /**
     * Notifies that {@code ON_RESUME} event occurred.
     * <p>
     * This method will be called after the {@link LifecycleOwner}'s {@code onResume}
     * method returns.
     *
     * @param owner the component, whose state was changed
     */
    default void onResume(@NonNull LifecycleOwner owner) {
    }

    /**
     * Notifies that {@code ON_PAUSE} event occurred.
     * <p>
     * This method will be called before the {@link LifecycleOwner}'s {@code onPause} method
     * is called.
     *
     * @param owner the component, whose state was changed
     */
    default void onPause(@NonNull LifecycleOwner owner) {
    }

    /**
     * Notifies that {@code ON_STOP} event occurred.
     * <p>
     * This method will be called before the {@link LifecycleOwner}'s {@code onStop} method
     * is called.
     *
     * @param owner the component, whose state was changed
     */
    default void onStop(@NonNull LifecycleOwner owner) {
    }

    /**
     * Notifies that {@code ON_DESTROY} event occurred.
     * <p>
     * This method will be called before the {@link LifecycleOwner}'s {@code onStop} method
     * is called.
     *
     * @param owner the component, whose state was changed
     */
    default void onDestroy(@NonNull LifecycleOwner owner) {
    }

    /**
     * Called when a state transition event happens.
     *
     * @param source The source of the event
     * @param event  The event
     */
    default void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
    }
}
