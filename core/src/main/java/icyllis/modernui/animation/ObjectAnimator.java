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

import icyllis.modernui.util.FloatProperty;
import icyllis.modernui.util.IntProperty;
import icyllis.modernui.util.Property;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Objects;

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
public final class ObjectAnimator extends ValueAnimator {

    /**
     * A weak reference to the target object on which the property exists, set
     * in the constructor. We'll cancel the animation if this goes away.
     */
    @Nullable
    private WeakReference<Object> mTarget;

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
        return ofPropertyValuesHolder(target, PropertyValuesHolder.ofObject(property, converter, evaluator, values));
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
        super.start();
    }

    @Override
    public ObjectAnimator clone() {
        return (ObjectAnimator) super.clone();
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
     * This function is called immediately before processing the first animation
     * frame of an animation. If there is a nonzero <code>startDelay</code>, the
     * function is called after that delay ends.
     * It takes care of the final initialization steps for the
     * animation.
     */
    @Override
    void initAnimation() {
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
     * Sets the length of the animation. The default duration is 300 milliseconds.
     *
     * @param duration The length of the animation, in milliseconds.
     * @return ObjectAnimator The object called with setDuration(). This return
     * value makes it easier to compose statements together that construct and then set the
     * duration, as in
     * <code>ObjectAnimator.ofInt(target, propertyName, 0, 10).setDuration(500).start()</code>.
     */
    @Override
    public ObjectAnimator setDuration(long duration) {
        super.setDuration(duration);
        return this;
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

    @Override
    void animateValue(float fraction) {
        final Object target = getTarget();
        if (mTarget != null && target == null) {
            // We lost the target reference, cancel and clean up. Note: we allow null target if the
            /// target has never been set.
            cancel();
            return;
        }

        super.animateValue(fraction);
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
}
