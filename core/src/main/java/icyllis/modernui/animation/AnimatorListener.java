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

package icyllis.modernui.animation;

import icyllis.modernui.annotation.NonNull;

/**
 * <p>An animation listener receives notifications from an animation.
 * Notifications indicate animation related events, such as the end or the
 * repetition of the animation.</p>
 */
public interface AnimatorListener {

    /**
     * <p>Notifies the start of the animation as well as the animation's overall play direction.
     * This method's default behavior is to call {@link #onAnimationStart(Animator)}. This
     * method can be overridden, though not required, to get the additional play direction info
     * when an animation starts. Skipping calling super when overriding this method results in
     * {@link #onAnimationStart(Animator)} not getting called.
     *
     * @param animation The started animation.
     * @param isReverse Whether the animation is playing in reverse.
     */
    default void onAnimationStart(@NonNull Animator animation, boolean isReverse) {
        onAnimationStart(animation);
    }

    /**
     * <p>Notifies the end of the animation. This callback is not invoked
     * for animations with repeat count set to INFINITE.</p>
     *
     * <p>This method's default behavior is to call {@link #onAnimationEnd(Animator)}. This
     * method can be overridden, though not required, to get the additional play direction info
     * when an animation ends. Skipping calling super when overriding this method results in
     * {@link #onAnimationEnd(Animator)} not getting called.
     *
     * @param animation The animation which reached its end.
     * @param isReverse Whether the animation is playing in reverse.
     */
    default void onAnimationEnd(@NonNull Animator animation, boolean isReverse) {
        onAnimationEnd(animation);
    }

    /**
     * <p>Notifies the start of the animation.</p>
     *
     * @param animation The started animation.
     */
    default void onAnimationStart(@NonNull Animator animation) {
    }

    /**
     * <p>Notifies the end of the animation. This callback is not invoked
     * for animations with repeat count set to INFINITE.</p>
     *
     * @param animation The animation which reached its end.
     */
    default void onAnimationEnd(@NonNull Animator animation) {
    }

    /**
     * <p>Notifies the cancellation of the animation. This callback is not invoked
     * for animations with repeat count set to INFINITE.</p>
     *
     * @param animation The animation which was canceled.
     */
    default void onAnimationCancel(@NonNull Animator animation) {
    }

    /**
     * <p>Notifies the repetition of the animation.</p>
     *
     * @param animation The animation which was repeated.
     */
    default void onAnimationRepeat(@NonNull Animator animation) {
    }

    /**
     * <p>Notifies that the animation was paused.</p>
     *
     * @param animation The animation being paused.
     * @see Animator#pause()
     */
    default void onAnimationPause(@NonNull Animator animation) {
    }

    /**
     * <p>Notifies that the animation was resumed, after being
     * previously paused.</p>
     *
     * @param animation The animation being resumed.
     * @see Animator#resume()
     */
    default void onAnimationResume(@NonNull Animator animation) {
    }
}
