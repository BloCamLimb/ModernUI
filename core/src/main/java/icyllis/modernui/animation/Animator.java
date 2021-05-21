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

import icyllis.modernui.platform.RenderCore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class Animator implements AnimationHandler.FrameCallback {

    private static float sDurationScale = 1.0f;

    /**
     * When the animation reaches the end and <code>repeatCount</code> is INFINITE
     * or a positive value, the animation restarts from the beginning.
     */
    public static final int RESTART = 1;

    /**
     * When the animation reaches the end and <code>repeatCount</code> is INFINITE
     * or a positive value, the animation reverses direction on every iteration.
     */
    public static final int REVERSE = 2;

    /**
     * This value used used with the {@link #setRepeatCount(int)} property to repeat
     * the animation indefinitely.
     */
    public static final int INFINITE = -1;

    /**
     * The value used to indicate infinite duration (e.g. when Animators repeat infinitely).
     */
    public static final long DURATION_INFINITE = -1;

    /**
     * Additional playing state to indicate whether an animator has been start()'d. There is
     * some lag between a call to start() and the first animation frame. We should still note
     * that the animation has been started, even if it's first animation frame has not yet
     * happened, and reflect that state in isRunning().
     * Note that delayed animations are different: they are not started until their first
     * animation frame, which occurs after their delay elapses.
     */
    private boolean mRunning = false;

    /**
     * Additional playing state to indicate whether an animator has been start()'d, whether or
     * not there is a nonzero startDelay.
     */
    private boolean mStarted = false;

    /**
     * Whether this animator is currently in a paused state.
     */
    private boolean mPaused = false;

    /**
     * Set when an animator is resumed. This triggers logic in the next frame which
     * actually resumes the animator.
     */
    private boolean mResumed = false;

    /**
     * Flag to indicate whether this animator is playing in reverse mode, specifically
     * by being started or interrupted by a call to reverse(). This flag is different than
     * mPlayingBackwards, which indicates merely whether the current iteration of the
     * animator is playing in reverse. It is used in corner cases to determine proper end
     * behavior.
     */
    private boolean mReversing;

    /**
     * Tracks whether we've notified listeners of the onAnimationStart() event. This can be
     * complex to keep track of since we notify listeners at different times depending on
     * startDelay and whether start() was called before end().
     */
    private boolean mStartListenersCalled = false;

    /**
     * Flag that denotes whether the animation is set up and ready to go. Used to
     * set up animation that has not yet been started.
     */
    private boolean mInitialized = false;

    /**
     * Flag that tracks whether animation has been requested to end.
     */
    private boolean mAnimationEndRequested = false;

    /**
     * The first time that the animation's animateFrame() method is called. This time is used to
     * determine elapsed time (and therefore the elapsed fraction) in subsequent calls
     * to animateFrame().
     * <p>
     * Whenever mStartTime is set, you must also update mStartTimeCommitted.
     */
    private long mStartTime = -1;

    /**
     * Set on the next frame after pause() is called, used to calculate a new startTime
     * or delayStartTime which allows the animator to continue from the point at which
     * it was paused. If negative, has not yet been set.
     */
    private long mPauseTime;

    /**
     * Set when setCurrentPlayTime() is called. If negative, animation is not currently seeked
     * to a value.
     */
    private float mSeekFraction = -1;

    /**
     * How long the animation should last in ms
     */
    private long mDuration = 300;

    /**
     * The amount of time in ms to delay starting the animation after start() is called. Note
     * that this start delay is unscaled. When there is a duration scale set on the animator, the
     * scaling factor will be applied to this delay.
     */
    private long mStartDelay = 0;

    /**
     * The number of times the animation will repeat. The default is 0, which means the animation
     * will play only once
     */
    private int mRepeatCount = 0;

    /**
     * The type of repetition that will occur when repeatMode is nonzero. RESTART means the
     * animation will start from the beginning on every new cycle. REVERSE means the animation
     * will reverse directions on each iteration.
     */
    private int mRepeatMode = RESTART;

    /**
     * Tracks the overall fraction of the animation, ranging from 0 to mRepeatCount + 1
     */
    private float mOverallFraction = 0f;

    /**
     * Tracks current elapsed/eased fraction, for querying in getAnimatedFraction().
     * This is calculated by interpolating the fraction (range: [0, 1]) in the current iteration.
     */
    private float mCurrentFraction = 0f;

    /**
     * Tracks the time (in milliseconds) when the last frame arrived.
     */
    private long mLastFrameTime = -1;

    /**
     * The time interpolator to be used. The elapsed fraction of the animation will be passed
     * through this interpolator to calculate the interpolated fraction, which is then used to
     * calculate the animated values.
     */
    @Nonnull
    private Interpolator mInterpolator = Interpolator.ACCELERATE_DECELERATE;

    /**
     * The property/value sets being animated.
     */
    private PropertyValuesHolder[] mValues;

    /**
     * The set of listeners to be sent events through the life of an animation.
     */
    private Set<Listener> mListeners;

    public Animator() {
    }

    /**
     * Starts this animation. If the animation has a nonzero startDelay, the animation will start
     * running after that delay elapses. A non-delayed animation will have its initial
     * value(s) set immediately, followed by calls to
     * {@link Listener#onAnimationStart(Animator, boolean)} for any listeners of this animator.
     *
     * <p>The animation started by calling this method will be run on the thread that called
     * this method. This thread should have a Looper on it (a runtime exception will be thrown if
     * this is not the case). Also, if the animation will animate
     * properties of objects in the view hierarchy, then the calling thread should be the UI
     * thread for that view hierarchy.</p>
     */
    public void start() {
        start(false);
    }

    private void start(boolean reverse) {
        mReversing = reverse;
        // Special case: reversing from seek-to-0 should act as if not seeked at all.
        if (reverse && mSeekFraction != -1 && mSeekFraction != 0) {
            if (mRepeatCount == INFINITE) {
                // Calculate the fraction of the current iteration.
                float fraction = (float) (mSeekFraction - Math.floor(mSeekFraction));
                mSeekFraction = 1 - fraction;
            } else {
                mSeekFraction = 1 + mRepeatCount - mSeekFraction;
            }
        }
        mStarted = true;
        mPaused = false;
        mRunning = false;
        mAnimationEndRequested = false;
        // Resets mLastFrameTime when start() is called, so that if the animation was running,
        // calling start() would put the animation in the
        // started-but-not-yet-reached-the-first-frame phase.
        mLastFrameTime = -1;
        mStartTime = -1;
        AnimationHandler.get().register(this, 0);

        if (mStartDelay == 0 || mSeekFraction >= 0 || mReversing) {
            // If there's no start delay, init the animation and notify start listeners right away
            // to be consistent with the previous behavior. Otherwise, postpone this until the first
            // frame after the start delay.
            startAnimation();
            if (mSeekFraction == -1) {
                // No seek, start at play time 0. Note that the reason we are not using fraction 0
                // is because for animations with 0 duration, we want to be consistent with pre-N
                // behavior: skip to the final value immediately.
                setCurrentPlayTime(0);
            } else {
                setCurrentFraction(mSeekFraction);
            }
        }
    }

    private void startAnimation() {
        mAnimationEndRequested = false;
        initAnimation();
        mRunning = true;
        if (mSeekFraction >= 0) {
            mOverallFraction = mSeekFraction;
        } else {
            mOverallFraction = 0f;
        }
        if (mListeners != null) {
            notifyStartListeners();
        }
    }

    /**
     * Called internally to end an animation by removing it from the animations list. Must be
     * called on the UI thread.
     */
    private void endAnimation() {
        if (mAnimationEndRequested) {
            return;
        }
        AnimationHandler.get().unregister(this);

        mAnimationEndRequested = true;
        mPaused = false;
        boolean notify = (mStarted || mRunning) && mListeners != null;
        if (notify && !mRunning) {
            // If it's not yet running, then start listeners weren't called. Call them now.
            notifyStartListeners();
        }
        mRunning = false;
        mStarted = false;
        mStartListenersCalled = false;
        mLastFrameTime = -1;
        mStartTime = -1;
        if (notify && mListeners != null) {
            // iterate the snapshot
            for (Listener l : mListeners) {
                l.onAnimationEnd(this, mReversing);
            }
        }
        // mReversing needs to be reset *after* notifying the listeners for the end callbacks.
        mReversing = false;
    }

    private void notifyStartListeners() {
        if (mListeners != null && !mStartListenersCalled) {
            // iterate the snapshot
            for (Listener l : mListeners) {
                l.onAnimationStart(this, mReversing);
            }
        }
        mStartListenersCalled = true;
    }

    /**
     * Adds a listener to the set of listeners that are sent events through the life of an
     * animation, such as start, repeat, and end.
     *
     * @param listener the listener to be added to the current set of listeners for this animation.
     */
    public void addListener(@Nonnull Listener listener) {
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
    public void removeListener(@Nonnull Listener listener) {
        mListeners.remove(listener);
        if (mListeners.isEmpty()) {
            mListeners = null;
        }
    }

    /**
     * Removes all {@link #addListener(Listener)} listeners} from this object.
     */
    public void removeAllListeners() {
        if (mListeners != null) {
            mListeners.clear();
            mListeners = null;
        }
    }

    /**
     * Defines how many times the animation should repeat. The default value
     * is 0.
     *
     * @return the number of times the animation should repeat, or {@link #INFINITE}
     */
    public int getRepeatCount() {
        return mRepeatCount;
    }

    /**
     * Sets how many times the animation should be repeated. If the repeat
     * count is 0, the animation is never repeated. If the repeat count is
     * greater than 0 or {@link #INFINITE}, the repeat mode will be taken
     * into account. The repeat count is 0 by default.
     *
     * @param value the number of times the animation should be repeated
     */
    public void setRepeatCount(int value) {
        mRepeatCount = value;
    }

    /**
     * The time interpolator used in calculating the elapsed fraction of the
     * animation. The interpolator determines whether the animation runs with
     * linear or non-linear motion, such as acceleration and deceleration. The
     * default value is {@link Interpolator#ACCELERATE_DECELERATE}.
     *
     * @param value the interpolator to be used by this animation
     */
    public void setInterpolator(@Nullable Interpolator value) {
        mInterpolator = Objects.requireNonNullElse(value, Interpolator.LINEAR);
    }

    /**
     * Returns the timing interpolator that this animation uses.
     *
     * @return The timing interpolator for this animation.
     */
    @Nonnull
    public Interpolator getInterpolator() {
        return mInterpolator;
    }

    /**
     * Returns whether this Animator is currently running (having been started and gone past any
     * initial startDelay period and not yet ended).
     *
     * @return whether the Animator is running.
     */
    public boolean isRunning() {
        return mRunning;
    }

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
    public boolean isStarted() {
        return mStarted;
    }

    /**
     * Returns whether this animator is currently in a paused state.
     *
     * @return True if the animator is currently paused, false otherwise.
     * @see #pause()
     * @see #resume()
     */
    public boolean isPaused() {
        return mPaused;
    }

    private long getScaledDuration() {
        return (long) (mDuration * sDurationScale);
    }

    /**
     * Gets the length of the animation. The default duration is 300 milliseconds.
     *
     * @return The length of the animation, in milliseconds.
     */
    public long getDuration() {
        return mDuration;
    }

    /**
     * Gets the total duration of the animation, accounting for animation sequences, start delay,
     * and repeating. Return {@link #DURATION_INFINITE} if the duration is infinite.
     *
     * @return Total time an animation takes to finish, starting from the time {@link #start()}
     * is called. {@link #DURATION_INFINITE} will be returned if the animation or any
     * child animation repeats infinite times.
     */
    public long getTotalDuration() {
        if (mRepeatCount == INFINITE) {
            return DURATION_INFINITE;
        } else {
            return mStartDelay + (mDuration * (mRepeatCount + 1));
        }
    }

    /**
     * Sets the position of the animation to the specified point in time. This time should
     * be between 0 and the total duration of the animation, including any repetition. If
     * the animation has not yet been started, then it will not advance forward after it is
     * set to this time; it will simply set the time to this value and perform any appropriate
     * actions based on that time. If the animation is already running, then setCurrentPlayTime()
     * will set the current playing time to this value and continue playing from that point.
     *
     * @param playTime The time, in milliseconds, to which the animation is advanced or rewound.
     */
    public void setCurrentPlayTime(long playTime) {
        float fraction = mDuration > 0 ? (float) playTime / mDuration : 1;
        setCurrentFraction(fraction);
    }

    /**
     * Sets the position of the animation to the specified fraction. This fraction should
     * be between 0 and the total fraction of the animation, including any repetition. That is,
     * a fraction of 0 will position the animation at the beginning, a value of 1 at the end,
     * and a value of 2 at the end of a reversing animator that repeats once. If
     * the animation has not yet been started, then it will not advance forward after it is
     * set to this fraction; it will simply set the fraction to this value and perform any
     * appropriate actions based on that fraction. If the animation is already running, then
     * setCurrentFraction() will set the current fraction to this value and continue
     * playing from that point. {@link Listener} events are not called
     * due to changing the fraction; those events are only processed while the animation
     * is running.
     *
     * @param fraction The fraction to which the animation is advanced or rewound. Values
     *                 outside the range of 0 to the maximum fraction for the animator will be clamped to
     *                 the correct range.
     */
    public void setCurrentFraction(float fraction) {
        initAnimation();
        fraction = clampFraction(fraction);
        if (isPulsingInternal()) {
            long seekTime = (long) (getScaledDuration() * fraction);
            long currentTime = RenderCore.timeMillis();
            // Only modify the start time when the animation is running. Seek fraction will ensure
            // non-running animations skip to the correct start time.
            mStartTime = currentTime - seekTime;
        } else {
            // If the animation loop hasn't started, or during start delay, the startTime will be
            // adjusted once the delay has passed based on seek fraction.
            mSeekFraction = fraction;
        }
        mOverallFraction = fraction;
        final float currentIterationFraction = getCurrentIterationFraction(fraction, mReversing);
        animateValue(currentIterationFraction);
    }

    /**
     * This function is called immediately before processing the first animation
     * frame of an animation. If there is a nonzero <code>startDelay</code>, the
     * function is called after that delay ends.
     * It takes care of the final initialization steps for the
     * animation.
     *
     * <p>Overrides of this method should call the superclass method to ensure
     * that internal mechanisms for the animation are set up correctly.</p>
     */
    private void initAnimation() {
        if (!mInitialized) {
            for (PropertyValuesHolder value : mValues) {
                //TODO
                //value.init();
            }
            mInitialized = true;
        }
    }

    /**
     * Calculates current iteration based on the overall fraction. The overall fraction will be
     * in the range of [0, mRepeatCount + 1]. Both current iteration and fraction in the current
     * iteration can be derived from it.
     */
    private int getCurrentIteration(float fraction) {
        fraction = clampFraction(fraction);
        // If the overall fraction is a positive integer, we consider the current iteration to be
        // complete. In other words, the fraction for the current iteration would be 1, and the
        // current iteration would be overall fraction - 1.
        double iteration = Math.floor(fraction);
        if (fraction == iteration && fraction > 0) {
            iteration--;
        }
        return (int) iteration;
    }

    /**
     * Calculates the fraction of the current iteration, taking into account whether the animation
     * should be played backwards. E.g. When the animation is played backwards in an iteration,
     * the fraction for that iteration will go from 1f to 0f.
     */
    private float getCurrentIterationFraction(float fraction, boolean inReverse) {
        fraction = clampFraction(fraction);
        int iteration = getCurrentIteration(fraction);
        float currentFraction = fraction - iteration;
        return shouldPlayBackward(iteration, inReverse) ? 1f - currentFraction : currentFraction;
    }

    /**
     * Clamps fraction into the correct range: [0, mRepeatCount + 1]. If repeat count is infinite,
     * no upper bound will be set for the fraction.
     *
     * @param fraction fraction to be clamped
     * @return fraction clamped into the range of [0, mRepeatCount + 1]
     */
    private float clampFraction(float fraction) {
        if (fraction < 0) {
            fraction = 0;
        } else if (mRepeatCount != INFINITE) {
            fraction = Math.min(fraction, mRepeatCount + 1);
        }
        return fraction;
    }

    /**
     * Calculates the direction of animation playing (i.e. forward or backward), based on 1)
     * whether the entire animation is being reversed, 2) repeat mode applied to the current
     * iteration.
     */
    private boolean shouldPlayBackward(int iteration, boolean inReverse) {
        if (iteration > 0 && mRepeatMode == REVERSE &&
                (iteration < (mRepeatCount + 1) || mRepeatCount == INFINITE)) {
            // if we were seeked to some other iteration in a reversing animator,
            // figure out the correct direction to start playing based on the iteration
            if (inReverse) {
                return (iteration % 2) == 0;
            } else {
                return (iteration % 2) != 0;
            }
        } else {
            return inReverse;
        }
    }

    @Override
    public void doAnimationFrame(long frameTime) {
    }

    /**
     * Cancels the animation. Unlike {@link #end()}, <code>cancel()</code> causes the animation to
     * stop in its tracks, sending an
     * {@link android.animation.Animator.AnimatorListener#onAnimationCancel(Animator)} to
     * its listeners, followed by an
     * {@link android.animation.Animator.AnimatorListener#onAnimationEnd(Animator)} message.
     *
     * <p>This method must be called on the thread that is running the animation.</p>
     */
    public void cancel() {
        // If end has already been requested, through a previous end() or cancel() call, no-op
        // until animation starts again.
        if (mAnimationEndRequested) {
            return;
        }
        // Only cancel if the animation is actually running or has been started and is about
        // to run
        // Only notify listeners if the animator has actually started
        if ((mStarted || mRunning) && mListeners != null) {
            if (!mRunning) {
                // If it's not yet running, then start listeners weren't called. Call them now.
                notifyStartListeners();
            }
            // iterate the snapshot
            for (Listener l : mListeners) {
                l.onAnimationCancel(this);
            }
        }
        endAnimation();
    }

    /**
     * Internal only: This tracks whether the animation has gotten on the animation loop. Note
     * this is different than {@link #isRunning()} in that the latter tracks the time after start()
     * is called (or after start delay if any), which may be before the animation loop starts.
     */
    private boolean isPulsingInternal() {
        return mLastFrameTime >= 0;
    }

    private void animateValue(float fraction) {
        fraction = mInterpolator.getInterpolation(fraction);
        mCurrentFraction = fraction;
        for (PropertyValuesHolder value : mValues) {
            //TODO
            //value.calculateValue(fraction);
        }
        /*if (mUpdateListeners != null) {
            int numListeners = mUpdateListeners.size();
            for (int i = 0; i < numListeners; ++i) {
                mUpdateListeners.get(i).onAnimationUpdate(this);
            }
        }*/
    }

    /**
     * <p>An animation listener receives notifications from an animation.
     * Notifications indicate animation related events, such as the end or the
     * repetition of the animation.</p>
     */
    @FunctionalInterface
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
        void onAnimationEnd(@Nonnull Animator animation, boolean isReverse);

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
    }
}
