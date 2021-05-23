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

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class Animator {

    public static final Marker MARKER = MarkerManager.getMarker("Animator");

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
     * {@link Listener#onAnimationStart(Animator, boolean)} for any listeners of this animator.
     */
    public abstract void start();

    /**
     * Adds a listener to the set of listeners that are sent events through the life of an
     * animation, such as start, repeat, and end.
     *
     * @param listener the listener to be added to the current set of listeners for this animation.
     */
    public final void addListener(@Nonnull Listener listener) {
        if (mListeners == null) {
            mListeners = new CopyOnWriteArraySet<>();
        }
        mListeners.add(listener);
    }

    /**
     * Removes a listener from the set listening to this animation.
     *
     * @param listener the listener to be removed from the current set of listeners for this
     *                 animation.
     */
    public final void removeListener(@Nonnull Listener listener) {
        mListeners.remove(listener);
        if (mListeners.isEmpty()) {
            mListeners = null;
        }
    }

    /**
     * Removes all {@link #addListener(Listener)} listeners} from this object.
     */
    public final void removeAllListeners() {
        if (mListeners != null) {
            mListeners.clear();
            mListeners = null;
        }
    }

    /**
     * Gets the set of {@link Listener} objects that are currently listening for events
     * on this <code>Animator</code> object.
     *
     * @return Set<Listener> The set of listeners.
     */
    @Nullable
    public final Set<Listener> getListeners() {
        return mListeners;
    }

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
        default void onAnimationStart(@Nonnull Animator animation, boolean isReverse) {
        }

        /**
         * <p>Notifies the end of the animation. This callback is not invoked
         * for animations with repeat count set to INFINITE.</p>
         *
         * @param animation The animation which reached its end.
         * @param isReverse Whether the animation is playing in reverse.
         */
        default void onAnimationEnd(@Nonnull Animator animation, boolean isReverse) {
        }

        /**
         * <p>Notifies the cancellation of the animation. This callback is not invoked
         * for animations with repeat count set to INFINITE.</p>
         *
         * @param animation The animation which was canceled.
         */
        default void onAnimationCancel(@Nonnull Animator animation) {
        }

        /**
         * <p>Notifies the repetition of the animation.</p>
         *
         * @param animation The animation which was repeated.
         */
        default void onAnimationRepeat(@Nonnull Animator animation) {
        }

        /**
         * <p>Notifies that the animation was paused.</p>
         *
         * @param animation The animation being paused.
         * @see #pause()
         */
        default void onAnimationPause(@Nonnull Animator animation) {
        }

        /**
         * <p>Notifies that the animation was resumed, after being
         * previously paused.</p>
         *
         * @param animation The animation being resumed.
         * @see #resume()
         */
        default void onAnimationResume(@Nonnull Animator animation) {
        }
    }
}
