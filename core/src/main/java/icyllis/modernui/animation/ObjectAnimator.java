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

import it.unimi.dsi.fastutil.objects.ObjectArraySet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * ObjectAnimator provides support for animating properties on target objects.
 * <p>
 * The constructors of this class take parameters to define the target object that will be animated
 * as well as the name of the property that will be animated. Appropriate set/get functions
 * are then determined internally and the animation will call these functions as necessary to
 * animate the property.
 * <p>
 * Use {@link PropertyValuesHolder} and {@link Keyframe} can create more complex animations.
 * Using PropertyValuesHolders allows animators to animate several properties in parallel.
 * <p>
 * Using Keyframes allows animations to follow more complex paths from the start
 * to the end values. Note that you can specify explicit fractional values (from 0 to 1) for
 * each keyframe to determine when, in the overall duration, the animation should arrive at that
 * value. Alternatively, you can leave the fractions off and the keyframes will be equally
 * distributed within the total duration. Also, a keyframe with no value will derive its value
 * from the target object when the animator starts, just like animators with only one
 * value specified. In addition, an optional interpolator can be specified. The interpolator will
 * be applied on the interval between the keyframe that the interpolator is set on and the previous
 * keyframe. When no interpolator is supplied, the default {@link TimeInterpolator#ACCELERATE_DECELERATE}
 * will be used.
 */
@SuppressWarnings("unused")
public final class ObjectAnimator extends Animator implements AnimationHandler.FrameCallback {

    /**
     * Internal usage, global config value
     */
    public static float sDurationScale = 1.0f;

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
     * A weak reference to the target object on which the property exists, set
     * in the constructor. We'll cancel the animation if this goes away.
     */
    @Nullable
    private WeakReference<Object> mTarget;

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
     * Whether or not the animator should register for its own animation callback to receive
     * animation pulse.
     */
    private boolean mSelfPulse = true;

    /**
     * Whether or not the animator has been requested to start without pulsing. This flag gets set
     * in startWithoutPulsing(), and reset in start().
     */
    private boolean mSuppressSelfPulseRequested = false;

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
     * The set of listeners to be sent events through the life of an animation.
     */
    @Nullable
    Set<AnimatorUpdateListener> mUpdateListeners = null;

    /**
     * The time interpolator to be used. The elapsed fraction of the animation will be passed
     * through this interpolator to calculate the interpolated fraction, which is then used to
     * calculate the animated values.
     */
    @Nonnull
    private TimeInterpolator mInterpolator = TimeInterpolator.ACCELERATE_DECELERATE;

    /**
     * The property/value sets being animated.
     */
    private PropertyValuesHolder<Object, ?, ?>[] mValues;

    private boolean mAutoCancel = false;

    /**
     * Creates a new ObjectAnimator object. This default constructor is primarily for
     * use internally; the other constructors which take parameters are more generally
     * useful.
     */
    public ObjectAnimator() {
    }

    /**
     * Constructs and returns an ObjectAnimator that animates between int values. A single
     * value implies that that value is the one being animated to, in which case the start value
     * will be derived from the property being animated and the target object when {@link #start()}
     * is called for the first time. Two values imply starting and ending values. More than two
     * values imply a starting value, values to animate through along the way, and an ending value
     * (these values will be distributed evenly across the duration of the animation).
     *
     * @param target   The object whose property is to be animated.
     * @param property The property being animated.
     * @param values   A set of values (at least 1) that the animation will animate between over time.
     * @return An ObjectAnimator object that is set up to animate between the given values.
     */
    @Nonnull
    public static <T> ObjectAnimator ofInt(@Nullable T target, @Nonnull IntProperty<T> property,
                                           @Nonnull int... values) {
        return ofPropertyValuesHolder(target, PropertyValuesHolder.ofInt(property, values));
    }

    /**
     * Constructs and returns an ObjectAnimator that animates between color values. A single
     * value implies that that value is the one being animated to, in which case the start value
     * will be derived from the property being animated and the target object when {@link #start()}
     * is called for the first time. Two values imply starting and ending values. More than two
     * values imply a starting value, values to animate through along the way, and an ending value
     * (these values will be distributed evenly across the duration of the animation).
     *
     * @param target   The object whose property is to be animated.
     * @param property The property being animated.
     * @param values   A set of values (at least 1) that the animation will animate between over time.
     * @return An ObjectAnimator object that is set up to animate between the given values.
     */
    @Nonnull
    public static <T> ObjectAnimator ofColor(@Nullable T target, @Nonnull IntProperty<T> property,
                                             @Nonnull int... values) {
        PropertyValuesHolder<T, Integer, Integer> v = PropertyValuesHolder.ofInt(property, values);
        v.setEvaluator(ColorEvaluator.getInstance());
        return ofPropertyValuesHolder(target, v);
    }

    /**
     * Constructs and returns an ObjectAnimator that animates between float values. A single
     * value implies that that value is the one being animated to, in which case the start value
     * will be derived from the property being animated and the target object when {@link #start()}
     * is called for the first time. Two values imply starting and ending values. More than two
     * values imply a starting value, values to animate through along the way, and an ending value
     * (these values will be distributed evenly across the duration of the animation).
     *
     * @param target   The object whose property is to be animated.
     * @param property The property being animated.
     * @param values   A set of values (at least 1) that the animation will animate between over time.
     * @return An ObjectAnimator object that is set up to animate between the given values.
     */
    @Nonnull
    public static <T> ObjectAnimator ofFloat(@Nullable T target, @Nonnull FloatProperty<T> property,
                                             @Nonnull float... values) {
        return ofPropertyValuesHolder(target, PropertyValuesHolder.ofFloat(property, values));
    }

    /**
     * Constructs and returns an ObjectAnimator that animates between Object values. A single
     * value implies that that value is the one being animated to, in which case the start value
     * will be derived from the property being animated and the target object when {@link #start()}
     * is called for the first time. Two values imply starting and ending values. More than two
     * values imply a starting value, values to animate through along the way, and an ending value
     * (these values will be distributed evenly across the duration of the animation).
     *
     * <p><strong>Note:</strong> The values are stored as references to the original
     * objects, which means that changes to those objects after this method is called will
     * affect the values on the animator. If the objects will be mutated externally after
     * this method is called, callers should pass a copy of those objects instead.
     *
     * @param target    The object whose property is to be animated.
     * @param property  The property being animated.
     * @param evaluator A TypeEvaluator that will be called on each animation frame to
     *                  provide the necessary interpolation between the Object values to derive the animated
     *                  value.
     * @param values    A set of values (at least 1) that the animation will animate between over time.
     * @return An ObjectAnimator object that is set up to animate between the given values.
     */
    @Nonnull
    @SafeVarargs
    public static <T, V> ObjectAnimator ofObject(@Nullable T target, @Nonnull Property<T, V> property,
                                                 @Nonnull TypeEvaluator<V> evaluator, @Nonnull V... values) {
        return ofPropertyValuesHolder(target, PropertyValuesHolder.ofObject(property, evaluator, values));
    }

    /**
     * Constructs and returns an ObjectAnimator that animates between Object values. A single
     * value implies that that value is the one being animated to, in which case the start value
     * will be derived from the property being animated and the target object when {@link #start()}
     * is called for the first time. Two values imply starting and ending values. More than two
     * values imply a starting value, values to animate through along the way, and an ending value
     * (these values will be distributed evenly across the duration of the animation).
     * This variant supplies a <code>TypeConverter</code> to convert from the animated values to the
     * type of the property. If only one value is supplied, the <code>TypeConverter</code> must be a
     * {@link BidirectionalTypeConverter} to retrieve the current value.
     *
     * <p><strong>Note:</strong> The values are stored as references to the original
     * objects, which means that changes to those objects after this method is called will
     * affect the values on the animator. If the objects will be mutated externally after
     * this method is called, callers should pass a copy of those objects instead.
     *
     * @param target    The object whose property is to be animated.
     * @param property  The property being animated.
     * @param converter Converts the animated object to the Property type.
     * @param evaluator A TypeEvaluator that will be called on each animation frame to
     *                  provide the necessary interpolation between the Object values to derive the animated
     *                  value.
     * @param values    A set of values (at least 1) that the animation will animate between over time.
     * @return An ObjectAnimator object that is set up to animate between the given values.
     */
    @Nonnull
    @SafeVarargs
    public static <T, V, P> ObjectAnimator ofObject(@Nullable T target, @Nonnull Property<T, P> property,
                                                    @Nonnull TypeConverter<V, P> converter,
                                                    @Nonnull TypeEvaluator<V> evaluator, @Nonnull V... values) {
        return ofPropertyValuesHolder(target,
                PropertyValuesHolder.ofObject(property, converter, evaluator, values));
    }

    /**
     * Constructs and returns an ObjectAnimator that animates between the sets of values specified
     * in <code>PropertyValueHolder</code> objects. This variant should be used when animating
     * several properties at once with the same ObjectAnimator, since PropertyValuesHolder allows
     * you to associate a set of animation values with a property name.
     *
     * @param target The object whose property is to be animated. Depending on how the
     *               PropertyValuesObjects were constructed, the target object should either have the {@link
     *               Property} objects used to construct the PropertyValuesHolder objects or (if the
     *               PropertyValuesHOlder objects were created with property names) the target object should have
     *               public methods on it called <code>setName()</code>, where <code>name</code> is the name of
     *               the property passed in as the <code>propertyName</code> parameter for each of the
     *               PropertyValuesHolder objects.
     * @param values A set of PropertyValuesHolder objects whose values will be animated between
     *               over time. Must not null, but can be empty.
     * @return An ObjectAnimator object that is set up to animate between the given values.
     */
    @Nonnull
    @SafeVarargs
    public static <T> ObjectAnimator ofPropertyValuesHolder(@Nullable T target,
                                                            @Nonnull PropertyValuesHolder<T, ?, ?>... values) {
        ObjectAnimator anim = new ObjectAnimator();
        anim.setTarget(target);
        anim.setValues(values);
        return anim;
    }

    /**
     * AutoCancel controls whether an ObjectAnimator will be canceled automatically
     * when any other ObjectAnimator with the same target and properties is started.
     * Setting this flag may make it easier to run different animators on the same target
     * object without having to keep track of whether there are conflicting animators that
     * need to be manually canceled. Canceling animators must have the same exact set of
     * target properties, in the same order.
     *
     * @param cancel Whether future ObjectAnimators with the same target and properties
     *               as this ObjectAnimator will cause this ObjectAnimator to be canceled.
     */
    public void setAutoCancel(boolean cancel) {
        mAutoCancel = cancel;
    }

    /**
     * Starts this animation. If the animation has a nonzero startDelay, the animation will start
     * running after that delay elapses. A non-delayed animation will have its initial
     * value(s) set immediately, followed by calls to
     * {@link AnimatorListener#onAnimationStart(Animator, boolean)} for any listeners of this animator.
     */
    @Override
    public void start() {
        AnimationHandler.getInstance().autoCancelBasedOn(this);
        start(false);
    }

    /**
     * Start the animation playing. This version of start() takes a boolean flag that indicates
     * whether the animation should play in reverse. The flag is usually false, but may be set
     * to true if called from the reverse() method.
     *
     * @param playBackwards Whether the ValueAnimator should start playing in reverse.
     */
    private void start(boolean playBackwards) {
        mReversing = playBackwards;
        mSelfPulse = !mSuppressSelfPulseRequested;
        // Special case: reversing from seek-to-0 should act as if not seeked at all.
        if (playBackwards && mSeekFraction != -1 && mSeekFraction != 0) {
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
        addAnimationCallback();

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
        removeAnimationCallback();

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
            for (AnimatorListener l : mListeners) {
                l.onAnimationEnd(this, mReversing);
            }
        }
        // mReversing needs to be reset *after* notifying the listeners for the end callbacks.
        mReversing = false;
    }

    private void notifyStartListeners() {
        if (mListeners != null && !mStartListenersCalled) {
            // iterate the snapshot
            for (AnimatorListener l : mListeners) {
                l.onAnimationStart(this, mReversing);
            }
        }
        mStartListenersCalled = true;
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
     * Defines what this animation should do when it reaches the end.
     *
     * @return either one of {@link #REVERSE} or {@link #RESTART}
     */
    public int getRepeatMode() {
        return mRepeatMode;
    }

    /**
     * Defines what this animation should do when it reaches the end. This
     * setting is applied only when the repeat count is either greater than
     * 0 or {@link #INFINITE}. Defaults to {@link #RESTART}.
     *
     * @param value {@link #RESTART} or {@link #REVERSE}
     */
    public void setRepeatMode(int value) {
        mRepeatMode = value;
    }

    /**
     * Adds a listener to the set of listeners that are sent update events through the life of
     * an animation. This method is called on all listeners for every frame of the animation,
     * after the values for the animation have been calculated.
     *
     * @param listener the listener to be added to the current set of listeners for this animation.
     */
    public void addUpdateListener(@Nonnull AnimatorUpdateListener listener) {
        if (mUpdateListeners == null) {
            mUpdateListeners = new ObjectArraySet<>();
        }
        mUpdateListeners.add(listener);
    }

    /**
     * Removes all listeners from the set listening to frame updates for this animation.
     */
    public void removeAllUpdateListeners() {
        if (mUpdateListeners != null) {
            mUpdateListeners.clear();
            mUpdateListeners = null;
        }
    }

    /**
     * Removes a listener from the set listening to frame updates for this animation.
     *
     * @param listener the listener to be removed from the current set of update listeners
     *                 for this animation.
     */
    public void removeUpdateListener(@Nonnull AnimatorUpdateListener listener) {
        if (mUpdateListeners == null) {
            return;
        }
        mUpdateListeners.remove(listener);
        if (mUpdateListeners.isEmpty()) {
            mUpdateListeners = null;
        }
    }

    /**
     * The time interpolator used in calculating the elapsed fraction of the
     * animation. The interpolator determines whether the animation runs with
     * linear or non-linear motion, such as acceleration and deceleration. The
     * default value is {@link TimeInterpolator#ACCELERATE_DECELERATE}.
     *
     * @param value the interpolator to be used by this animation. A value of <code>null</code>
     *              will result in linear interpolation.
     */
    @Override
    public void setInterpolator(@Nullable TimeInterpolator value) {
        mInterpolator = Objects.requireNonNullElse(value, TimeInterpolator.LINEAR);
    }

    /**
     * Returns the timing interpolator that this animation uses.
     *
     * @return The timing interpolator for this animation.
     */
    @Nonnull
    @Override
    public TimeInterpolator getInterpolator() {
        return mInterpolator;
    }

    /**
     * Returns whether this Animator is currently running (having been started and gone past any
     * initial startDelay period and not yet ended).
     *
     * @return whether the Animator is running.
     */
    @Override
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
    @Override
    public boolean isStarted() {
        return mStarted;
    }

    @Nonnull
    @Override
    public ObjectAnimator clone() {
        final ObjectAnimator anim = (ObjectAnimator) super.clone();
        if (mUpdateListeners != null) {
            anim.mUpdateListeners = new CopyOnWriteArraySet<>(mUpdateListeners);
        }
        anim.mSeekFraction = -1;
        anim.mReversing = false;
        anim.mInitialized = false;
        anim.mStarted = false;
        anim.mRunning = false;
        anim.mPaused = false;
        anim.mResumed = false;
        anim.mStartListenersCalled = false;
        anim.mStartTime = -1;
        anim.mAnimationEndRequested = false;
        anim.mPauseTime = -1;
        anim.mLastFrameTime = -1;
        anim.mOverallFraction = 0;
        anim.mCurrentFraction = 0;
        anim.mSelfPulse = true;
        anim.mSuppressSelfPulseRequested = false;

        PropertyValuesHolder<Object, ?, ?>[] oldValues = mValues;
        if (oldValues != null) {
            int numValues = oldValues.length;
            @SuppressWarnings("unchecked")
            PropertyValuesHolder<Object, ?, ?>[] valuesHolder = (PropertyValuesHolder<Object, ?, ?>[])
                    Array.newInstance(oldValues.getClass().getComponentType(), numValues);
            anim.mValues = valuesHolder;
            for (int i = 0; i < numValues; ++i) {
                anim.mValues[i] = oldValues[i].clone();
            }
        }
        return anim;
    }

    /**
     * Plays the ValueAnimator in reverse. If the animation is already running,
     * it will stop itself and play backwards from the point reached when reverse was called.
     * If the animation is not currently running, then it will start from the end and
     * play backwards. This behavior is only set for the current animation; future playing
     * of the animation will use the default behavior of playing forward.
     */
    public void reverse() {
        if (isPulsingInternal()) {
            long currentTime = AnimationHandler.currentTimeMillis();
            long currentPlayTime = currentTime - mStartTime;
            long timeLeft = getScaledDuration() - currentPlayTime;
            mStartTime = currentTime - timeLeft;
            mReversing = !mReversing;
        } else if (mStarted) {
            mReversing = !mReversing;
            end();
        } else {
            start(true);
        }
    }

    private long getScaledDuration() {
        return (long) (mDuration * sDurationScale);
    }

    /**
     * Gets the length of the animation. The default duration is 300 milliseconds.
     *
     * @return The length of the animation, in milliseconds.
     */
    @Override
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
    @Override
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
     * playing from that point. {@link AnimatorListener} events are not called
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
            long currentTime = AnimationHandler.currentTimeMillis();
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

    @Override
    public void setupStartValues() {
        initAnimation();
        final Object target = getTarget();
        if (target != null) {
            for (var value : mValues) {
                value.setupStartValue(target);
            }
        }
    }

    @Override
    public void setupEndValues() {
        initAnimation();
        final Object target = getTarget();
        if (target != null) {
            for (var value : mValues) {
                value.setupEndValue(target);
            }
        }
    }

    /**
     * Sets the values, per property, being animated between. This function is called internally
     * by the constructors of ValueAnimator that take a list of values. But a ValueAnimator can
     * be constructed without values and this method can be called to set the values manually
     * instead.
     *
     * @param values The set of values, per property, being animated between.
     */
    @SuppressWarnings("unchecked")
    public void setValues(@Nonnull PropertyValuesHolder<?, ?, ?>... values) {
        mValues = (PropertyValuesHolder<Object, ?, ?>[]) values;
        // New property/values/target should cause re-initialization prior to starting
        mInitialized = false;
    }

    /**
     * Returns the values that this ValueAnimator animates between. These values are stored in
     * PropertyValuesHolder objects, even if the ValueAnimator was created with a simple list
     * of value objects instead.
     *
     * @return The array of PropertyValuesHolder objects which hold the values, per property,
     * that define the animation.
     */
    @Nonnull
    public PropertyValuesHolder<Object, ?, ?>[] getValues() {
        return mValues;
    }

    /**
     * This function is called immediately before processing the first animation
     * frame of an animation. If there is a nonzero <code>startDelay</code>, the
     * function is called after that delay ends.
     * It takes care of the final initialization steps for the
     * animation.
     */
    private void initAnimation() {
        if (!mInitialized) {
            final Object target = getTarget();
            for (var value : mValues) {
                // mValueType may change due to setter/getter setup; do this before calling init(),
                // which uses mValueType to set up the default type evaluator.
                if (target != null) {
                    value.setupSetterAndGetter(target);
                }
                value.init();
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
                return (iteration & 1) == 0;
            } else {
                return (iteration & 1) != 0;
            }
        } else {
            return inReverse;
        }
    }

    /**
     * Gets the current position of the animation in time, which is equal to the current
     * time minus the time that the animation started. An animation that is not yet started will
     * return a value of zero, unless the animation has has its play time set via
     * {@link #setCurrentPlayTime(long)} or {@link #setCurrentFraction(float)}, in which case
     * it will return the time that was set.
     *
     * @return The current position in time of the animation.
     */
    public long getCurrentPlayTime() {
        if (!mInitialized || (!mStarted && mSeekFraction < 0)) {
            return 0;
        }
        if (mSeekFraction >= 0) {
            return (long) (mDuration * mSeekFraction);
        }
        float durationScale = sDurationScale;
        if (durationScale == 0f) {
            durationScale = 1f;
        }
        return (long) ((AnimationHandler.currentTimeMillis() - mStartTime) / durationScale);
    }

    private void removeAnimationCallback() {
        if (mSelfPulse) {
            AnimationHandler.getInstance().unregister(this);
        }
    }

    private void addAnimationCallback() {
        if (mSelfPulse) {
            AnimationHandler.getInstance().register(this, 0);
        }
    }

    @Override
    public void doAnimationFrame(long frameTime) {
        if (mStartTime < 0) {
            // First frame. If there is start delay, start delay count down will happen *after* this
            // frame.
            mStartTime = mReversing
                    ? frameTime
                    : frameTime + (long) (mStartDelay * sDurationScale);
        }

        // Handle pause/resume
        if (mPaused) {
            mPauseTime = frameTime;
            removeAnimationCallback();
            return;
        } else if (mResumed) {
            mResumed = false;
            if (mPauseTime > 0) {
                // Offset by the duration that the animation was paused
                mStartTime += (frameTime - mPauseTime);
            }
        }

        if (!mRunning) {
            // If not running, that means the animation is in the start delay phase of a forward
            // running animation. In the case of reversing, we want to run start delay in the end.
            if (mStartTime > frameTime && mSeekFraction == -1) {
                // This is when no seek fraction is set during start delay. If developers change the
                // seek fraction during the delay, animation will start from the seeked position
                // right away.
                return;
            } else {
                // If mRunning is not set by now, that means non-zero start delay,
                // no seeking, not reversing. At this point, start delay has passed.
                mRunning = true;
                startAnimation();
            }
        }

        if (mLastFrameTime < 0) {
            if (mSeekFraction >= 0) {
                long seekTime = (long) (getScaledDuration() * mSeekFraction);
                mStartTime = frameTime - seekTime;
                mSeekFraction = -1;
            }
        }
        mLastFrameTime = frameTime;
        // The frame time might be before the start time during the first frame of
        // an animation.  The "current time" must always be on or after the start
        // time to avoid animating frames at negative time intervals.  In practice, this
        // is very rare and only happens when seeking backwards.
        final long currentTime = Math.max(frameTime, mStartTime);
        boolean finished = animateBasedOnTime(currentTime);
        if (finished) {
            endAnimation();
        }
    }

    /**
     * This internal function processes a single animation frame for a given animation. The
     * currentTime parameter is the timing pulse sent by the handler, used to calculate the
     * elapsed duration, and therefore
     * the elapsed fraction, of the animation. The return value indicates whether the animation
     * should be ended (which happens when the elapsed time of the animation exceeds the
     * animation's duration, including the repeatCount).
     *
     * @param currentTime The current time, as tracked by the static timing handler
     * @return true if the animation's duration, including any repetitions due to
     * <code>repeatCount</code> has been exceeded and the animation should be ended.
     */
    private boolean animateBasedOnTime(long currentTime) {
        boolean done = false;
        if (mRunning) {
            final long scaledDuration = getScaledDuration();
            final float fraction = scaledDuration > 0 ?
                    (float) (currentTime - mStartTime) / scaledDuration : 1f;
            final float lastFraction = mOverallFraction;
            final boolean newIteration = (int) fraction > (int) lastFraction;
            final boolean lastIterationFinished = (fraction >= mRepeatCount + 1) &&
                    (mRepeatCount != INFINITE);
            if (scaledDuration == 0) {
                // 0 duration animator, ignore the repeat count and skip to the end
                done = true;
            } else if (newIteration && !lastIterationFinished) {
                // Time to repeat
                if (mListeners != null) {
                    for (AnimatorListener l : mListeners) {
                        l.onAnimationRepeat(this);
                    }
                }
            } else if (lastIterationFinished) {
                done = true;
            }
            mOverallFraction = clampFraction(fraction);
            float currentIterationFraction = getCurrentIterationFraction(
                    mOverallFraction, mReversing);
            animateValue(currentIterationFraction);
        }
        return done;
    }

    @Override
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
            for (AnimatorListener l : mListeners) {
                l.onAnimationCancel(this);
            }
        }
        endAnimation();
    }

    @Override
    public void end() {
        if (!mRunning) {
            // Special case if the animation has not yet started; get it ready for ending
            startAnimation();
            mStarted = true;
        } else if (!mInitialized) {
            initAnimation();
        }
        animateValue(shouldPlayBackward(mRepeatCount, mReversing) ? 0f : 1f);
        endAnimation();
    }

    @Override
    public void resume() {
        if (mPaused && !mResumed) {
            mResumed = true;
            if (mPauseTime > 0) {
                addAnimationCallback();
            }
        }
        super.resume();
    }

    @Override
    public void pause() {
        boolean prev = mPaused;
        super.pause();
        if (!prev && mPaused) {
            mPauseTime = -1;
            mResumed = false;
        }
    }

    @Override
    public void setStartDelay(long startDelay) {
        if (startDelay < 0) {
            startDelay = 0;
        }
        mStartDelay = startDelay;
    }

    /**
     * The amount of time, in milliseconds, to delay starting the animation after
     * {@link #start()} is called.
     *
     * @return the number of milliseconds to delay running the animation
     */
    @Override
    public long getStartDelay() {
        return mStartDelay;
    }

    /**
     * Sets the length of the animation. The default duration is 300 milliseconds.
     *
     * @param duration The length of the animation, in milliseconds.
     */
    @Override
    public void setDuration(long duration) {
        if (duration < 0) {
            duration = 0;
        }
        mDuration = duration;
    }

    /**
     * The target object whose property will be animated by this animation
     *
     * @return The object being animated
     */
    @Nullable
    public Object getTarget() {
        return mTarget == null ? null : mTarget.get();
    }

    @Override
    public void setTarget(@Nullable Object target) {
        final Object oldTarget = getTarget();
        if (oldTarget != target) {
            if (isStarted()) {
                cancel();
            }
            mTarget = target == null ? null : new WeakReference<>(target);
            // New target should cause re-initialization prior to starting
            mInitialized = false;
        }
    }

    /**
     * Internal only: This tracks whether the animation has gotten on the animation loop. Note
     * this is different than {@link #isRunning()} in that the latter tracks the time after start()
     * is called (or after start delay if any), which may be before the animation loop starts.
     */
    private boolean isPulsingInternal() {
        return mLastFrameTime >= 0;
    }

    /**
     * Returns the current animation fraction, which is the elapsed/interpolated fraction used in
     * the most recent frame update on the animation.
     *
     * @return Elapsed/interpolated fraction of the animation.
     */
    public float getAnimatedFraction() {
        return mCurrentFraction;
    }

    private void animateValue(float fraction) {
        final Object target = getTarget();
        if (mTarget != null && target == null) {
            // We lost the target reference, cancel and clean up. Note: we allow null target if the
            // target has never been set, that is, listener mode.
            cancel();
            return;
        }
        fraction = mInterpolator.getInterpolation(fraction);
        mCurrentFraction = fraction;

        for (var value : mValues) {
            value.calculateValue(fraction);
        }
        if (mUpdateListeners != null) {
            for (AnimatorUpdateListener l : mUpdateListeners) {
                l.onAnimationUpdate(this);
            }
        }
        if (target != null) {
            for (var value : mValues) {
                value.setAnimatedValue(target);
            }
        }
    }

    boolean shouldAutoCancel(AnimationHandler.FrameCallback anim) {
        // if this animation started and start() again before ending, cancel self
        if (anim == this && mAutoCancel) {
            return true;
        }
        if (anim instanceof final ObjectAnimator it) {
            if (it.mAutoCancel) {
                PropertyValuesHolder<?, ?, ?>[] itsValues = it.getValues();
                if (it.getTarget() == getTarget() && mValues.length == itsValues.length) {
                    for (int i = 0; i < mValues.length; i++) {
                        if (!Objects.equals(mValues[i], itsValues[i])) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    void startWithoutPulsing(boolean inReverse) {
        mSuppressSelfPulseRequested = true;
        if (inReverse) {
            reverse();
        } else {
            start();
        }
        mSuppressSelfPulseRequested = false;
    }

    @Override
    boolean isInitialized() {
        return mInitialized;
    }

    /**
     * Implementors of this interface can add themselves as update listeners
     * to an <code>ValueAnimator</code> instance to receive callbacks on every animation
     * frame, after the current frame's values have been calculated for that
     * <code>ValueAnimator</code>.
     */
    @FunctionalInterface
    public interface AnimatorUpdateListener {

        /**
         * <p>Notifies the occurrence of another frame of the animation.</p>
         *
         * @param animation The animation which was repeated.
         */
        void onAnimationUpdate(@Nonnull ObjectAnimator animation);
    }
}
