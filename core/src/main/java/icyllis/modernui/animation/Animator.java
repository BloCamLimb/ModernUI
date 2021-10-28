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

/**
 * This is the base for classes which provide basic support for animations which can be
 * started, ended, and have listeners added to them.
 */
public abstract class Animator implements Cloneable {

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
    @Nullable
    Set<AnimatorListener> mListeners;

    /**
     * Starts this animation. If the animation has a nonzero startDelay, the animation will start
     * running after that delay elapses. A non-delayed animation will have its initial
     * value(s) set immediately, followed by calls to
     * {@link AnimatorListener#onAnimationStart(Animator, boolean)} for any listeners of this animator.
     *
     * <p>The animation started by calling this method will be run on the thread that called
     * this method.</p>
     */
    public abstract void start();

    /**
     * Cancels the animation. Unlike {@link #end()}, <code>cancel()</code> causes the animation to
     * stop in its tracks, sending an {@link AnimatorListener#onAnimationCancel(Animator)} to
     * its listeners, followed by an {@link AnimatorListener#onAnimationEnd(Animator, boolean)} message.
     *
     * <p>This method must be called on the thread that is running the animation.</p>
     */
    public abstract void cancel();

    /**
     * Ends the animation. This causes the animation to assign the end value of the property being
     * animated, then calling the {@link AnimatorListener#onAnimationEnd(Animator, boolean)} method on its
     * listeners.
     *
     * <p>This method must be called on the thread that is running the animation.</p>
     */
    public abstract void end();

    /**
     * Pauses a running animation. This method should only be called on the same thread on
     * which the animation was started. If the animation has not yet been {@link
     * #isStarted() started} or has since ended, then the call is ignored. Paused
     * animations can be resumed by calling {@link #resume()}.
     *
     * @see #resume()
     * @see #isPaused()
     * @see AnimatorListener#onAnimationPause(Animator)
     */
    public void pause() {
        if (isStarted() && !mPaused) {
            mPaused = true;
            if (mListeners != null) {
                for (AnimatorListener l : mListeners) {
                    l.onAnimationPause(this);
                }
            }
        }
    }

    /**
     * Resumes a paused animation, causing the animator to pick up where it left off
     * when it was paused. This method should only be called on the same thread on
     * which the animation was started. Calls to resume() on an animator that is
     * not currently paused will be ignored.
     *
     * @see #pause()
     * @see #isPaused()
     * @see AnimatorListener#onAnimationResume(Animator)
     */
    public void resume() {
        if (mPaused) {
            mPaused = false;
            if (mListeners != null) {
                for (AnimatorListener l : mListeners) {
                    l.onAnimationResume(this);
                }
            }
        }
    }

    /**
     * Returns whether this animator is currently in a paused state.
     *
     * @return True if the animator is currently paused, false otherwise.
     * @see #pause()
     * @see #resume()
     */
    public final boolean isPaused() {
        return mPaused;
    }

    /**
     * The amount of time, in milliseconds, to delay processing the animation
     * after {@link #start()} is called.
     *
     * @param startDelay The amount of the delay, in milliseconds
     */
    public abstract void setStartDelay(long startDelay);

    /**
     * The amount of time, in milliseconds, to delay processing the animation
     * after {@link #start()} is called.
     *
     * @return the number of milliseconds to delay running the animation
     */
    public abstract long getStartDelay();

    /**
     * Sets the duration of the animation.
     *
     * @param duration The length of the animation, in milliseconds.
     */
    public abstract void setDuration(long duration);

    /**
     * Gets the duration of the animation.
     *
     * @return The length of the animation, in milliseconds.
     */
    public abstract long getDuration();

    /**
     * Gets the total duration of the animation, accounting for animation sequences, start delay,
     * and repeating. Return {@link #DURATION_INFINITE} if the duration is infinite.
     *
     * @return Total time an animation takes to finish, starting from the time {@link #start()}
     * is called. {@link #DURATION_INFINITE} will be returned if the animation or any
     * child animation repeats infinite times.
     */
    public abstract long getTotalDuration();

    /**
     * The time interpolator used in calculating the elapsed fraction of the
     * animation. The interpolator determines whether the animation runs with
     * linear or non-linear motion, such as acceleration and deceleration. The
     * default value is {@link Interpolator#ACCELERATE_DECELERATE}.
     *
     * @param value the interpolator to be used by this animation
     */
    public abstract void setInterpolator(Interpolator value);

    /**
     * Returns the timing interpolator that this animation uses.
     *
     * @return The timing interpolator for this animation.
     */
    public abstract Interpolator getInterpolator();

    /**
     * Returns whether this Animator is currently running (having been started and gone past any
     * initial startDelay period and not yet ended).
     *
     * @return Whether the Animator is running.
     */
    public abstract boolean isRunning();

    /**
     * Returns whether this Animator has been started and not yet ended. For reusable
     * Animators (which most Animators are, apart from the one-shot animator, this state
     * is a superset of {@link #isRunning()}, because an Animator with a nonzero
     * {@link #getStartDelay()} will return true during the delay phase, whereas
     * {@link #isRunning()} will return true only after the delay phase is complete.
     * Non-reusable animators will always return true after they have been started,
     * because they cannot return to a non-started state.
     *
     * @return whether the Animator has been started and not yet ended.
     */
    public abstract boolean isStarted();

    /**
     * Adds a listener to the set of listeners that are sent events through the life of an
     * animation, such as start, repeat, and end.
     *
     * @param listener the listener to be added to the current set of listeners for this animation.
     */
    public final void addListener(@Nonnull AnimatorListener listener) {
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
    public final void removeListener(@Nonnull AnimatorListener listener) {
        if (mListeners == null) {
            return;
        }
        mListeners.remove(listener);
        if (mListeners.isEmpty()) {
            mListeners = null;
        }
    }

    /**
     * Removes all {@link #addListener(AnimatorListener)} listeners} from this object.
     */
    public final void removeAllListeners() {
        if (mListeners != null) {
            mListeners.clear();
            mListeners = null;
        }
    }

    /**
     * Gets the set of {@link AnimatorListener} objects that are currently listening for events
     * on this <code>Animator</code> object.
     *
     * @return The set of listeners.
     */
    @Nullable
    public final Set<AnimatorListener> getListeners() {
        return mListeners;
    }

    @Override
    public Animator clone() {
        try {
            final Animator anim = (Animator) super.clone();
            if (mListeners != null) {
                anim.mListeners = new CopyOnWriteArraySet<>(mListeners);
            }
            return anim;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    /**
     * This method tells the object to use appropriate information to extract
     * starting values for the animation. For example, a AnimatorSet object will pass
     * this call to its child objects to tell them to set up the values. A
     * ObjectAnimator object will use the information it has about its target object
     * and PropertyValuesHolder objects to get the start values for its properties.
     * A ValueAnimator object will ignore the request since it does not have enough
     * information (such as a target object) to gather these values.
     */
    public void setupStartValues() {
    }

    /**
     * This method tells the object to use appropriate information to extract
     * ending values for the animation. For example, a AnimatorSet object will pass
     * this call to its child objects to tell them to set up the values. A
     * ObjectAnimator object will use the information it has about its target object
     * and PropertyValuesHolder objects to get the start values for its properties.
     * A ValueAnimator object will ignore the request since it does not have enough
     * information (such as a target object) to gather these values.
     */
    public void setupEndValues() {
    }

    /**
     * Sets the target object whose property will be animated by this animation. Not all subclasses
     * operate on target objects, but this method is on the superclass for the convenience of dealing
     * generically with those subclasses that do handle targets.
     * <p>
     * <strong>Note:</strong> The target is stored as a weak reference internally to avoid leaking
     * resources by having animators directly reference old targets. Therefore, you should
     * ensure that animator targets always have a hard reference elsewhere.
     *
     * @param target The object being animated
     */
    public void setTarget(@Nullable Object target) {
    }

    /**
     * Internal use only.
     * This call starts the animation in regular or reverse direction without requiring them to
     * register frame callbacks. The caller will be responsible for all the subsequent animation
     * pulses. Specifically, the caller needs to call doAnimationFrame(...) for the animation on
     * every frame.
     *
     * @param inReverse whether the animation should play in reverse direction
     */
    void startWithoutPulsing(boolean inReverse) {
    }

    /**
     * Internal use only.
     * <p>
     * Returns whether the animation has start/end values setup. For most of the animations, this
     * should always be true. For ObjectAnimators, the start values are setup in the initialization
     * of the animation.
     */
    boolean isInitialized() {
        return true;
    }

    /**
     * <p>An animation listener receives notifications from an animation.
     * Notifications indicate animation related events, such as the end or the
     * repetition of the animation.</p>
     */
    public interface AnimatorListener {

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
