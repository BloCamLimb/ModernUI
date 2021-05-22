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

package icyllis.modernui.animation;

import javax.annotation.Nonnull;
import java.util.Set;

public abstract class Animator {

    /**
     * The value used to indicate infinite duration (e.g. when Animators repeat infinitely).
     */
    public static final long DURATION_INFINITE = -1;

    /**
     * Whether this animator is currently in a paused state.
     */
    boolean mPaused = false;

    /**
     * The set of listeners to be sent events through the life of an animation.
     */
    Set<Listener> mListeners;

    /**
     * Starts this animation. If the animation has a nonzero startDelay, the animation will start
     * running after that delay elapses. A non-delayed animation will have its initial
     * value(s) set immediately, followed by calls to
     * {@link Listener#onAnimationStart(ObjectAnimator, boolean)} for any listeners of this animator.
     */
    public abstract void start();

    /**
     * <p>An animation listener receives notifications from an animation.
     * Notifications indicate animation related events, such as the end or the
     * repetition of the animation.</p>
     */
    public interface Listener {

        /**
         * <p>Notifies the start of the animation as well as the animation's overall play direction.
         *
         * @param animation The started animation.
         * @param isReverse Whether the animation is playing in reverse.
         */
        default void onAnimationStart(@Nonnull ObjectAnimator animation, boolean isReverse) {
        }

        /**
         * <p>Notifies the end of the animation. This callback is not invoked
         * for animations with repeat count set to INFINITE.</p>
         *
         * @param animation The animation which reached its end.
         * @param isReverse Whether the animation is playing in reverse.
         */
        default void onAnimationEnd(@Nonnull ObjectAnimator animation, boolean isReverse) {
        }

        /**
         * <p>Notifies the cancellation of the animation. This callback is not invoked
         * for animations with repeat count set to INFINITE.</p>
         *
         * @param animation The animation which was canceled.
         */
        default void onAnimationCancel(@Nonnull ObjectAnimator animation) {
        }

        /**
         * <p>Notifies the repetition of the animation.</p>
         *
         * @param animation The animation which was repeated.
         */
        default void onAnimationRepeat(@Nonnull ObjectAnimator animation) {
        }
    }
}
