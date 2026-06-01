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

package icyllis.modernui.transition;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.view.ViewGroup;

/**
 * A transition listener receives notifications from a transition.
 * Notifications indicate transition lifecycle events.
 */
public interface TransitionListener {

    /**
     * Notification about the start of the transition.
     *
     * @param transition The started transition.
     */
    default void onTransitionStart(@NonNull Transition transition) {
    }

    /**
     * Notification about the end of the transition. Canceled transitions
     * will always notify listeners of both the cancellation and end
     * events. That is, this method is always called,
     * regardless of whether the transition was canceled or played
     * through to completion.
     *
     * @param transition The transition which reached its end.
     */
    default void onTransitionEnd(@NonNull Transition transition) {
    }

    /**
     * Notification about the cancellation of the transition.
     * Note that cancel may be called by a parent {@link TransitionSet} on
     * a child transition which has not yet started. This allows the child
     * transition to restore state on target objects which was set at
     * {@link Transition#createAnimator(ViewGroup, TransitionValues, TransitionValues)
     * createAnimator()} time.
     *
     * @param transition The transition which was canceled.
     */
    default void onTransitionCancel(@NonNull Transition transition) {
    }

    /**
     * Notification when a transition is paused.
     * Note that createAnimator() may be called by a parent {@link TransitionSet} on
     * a child transition which has not yet started. This allows the child
     * transition to restore state on target objects which was set at
     * {@link Transition#createAnimator(ViewGroup, TransitionValues, TransitionValues)
     * createAnimator()} time.
     *
     * @param transition The transition which was paused.
     */
    default void onTransitionPause(@NonNull Transition transition) {
    }

    /**
     * Notification when a transition is resumed.
     * Note that resume() may be called by a parent {@link TransitionSet} on
     * a child transition which has not yet started. This allows the child
     * transition to restore state which may have changed in an earlier call
     * to {@link #onTransitionPause(Transition)}.
     *
     * @param transition The transition which was resumed.
     */
    default void onTransitionResume(@NonNull Transition transition) {
    }
}
