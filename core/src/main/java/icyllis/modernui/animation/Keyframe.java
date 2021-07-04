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
import javax.annotation.Nullable;

/**
 * This class holds a time/value pair for an animation. The Keyframe class is used
 * by {@link ObjectAnimator} to define the values that the animation target will have over the course
 * of the animation. As the time proceeds from one keyframe to the other, the value of the
 * target object will animate between the value at the previous keyframe and the value at the
 * next keyframe. Each keyframe also holds an optional {@link Interpolator}
 * object, which defines the time interpolation over the inter-value preceding the keyframe.
 *
 * <p>The Keyframe class itself is abstract. The type-specific factory methods will return
 * a subclass of Keyframe specific to the type of value being stored. This is done to improve
 * performance when dealing with the most common cases (e.g., <code>float</code> and
 * <code>int</code> values). Other types will fall into a more general Keyframe class that
 * treats its values as Objects. Unless your animation requires dealing with a custom type
 * or a data structure that needs to be animated directly (and evaluated using an implementation
 * of {@link TypeEvaluator}), you should stick to using float and int as animations using those
 * types have lower runtime overhead than other types.</p>
 */
public abstract class Keyframe {

    /**
     * Flag to indicate whether this keyframe has a valid value. This flag is used when an
     * animation first starts, to populate placeholder keyframes with real values derived
     * from the target object.
     */
    boolean mHasValue;

    /**
     * Flag to indicate whether the value in the keyframe was read from the target object or not.
     * If so, its value will be recalculated if target changes.
     */
    boolean mValueWasSetOnStart;

    /**
     * The time at which mValue will hold true.
     */
    float mFraction;

    /**
     * The optional time interpolator for the interval preceding this keyframe. A null interpolator
     * (the default) results in linear interpolation over the interval.
     */
    @Nullable
    private Interpolator mInterpolator = null;

    /**
     * Constructs a Keyframe object with the given time and value. The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     *                 of time elapsed of the overall animation duration.
     * @param value    The value that the object will animate to as the animation time approaches
     *                 the time in this keyframe, and the value animated from as the time passes the time in
     *                 this keyframe.
     */
    @Nonnull
    public static Keyframe ofInt(float fraction, int value) {
        return new IntKeyframe(fraction, value);
    }

    /**
     * Constructs a Keyframe object with the given time. The value at this time will be derived
     * from the target object when the animation first starts (note that this implies that keyframes
     * with no initial value must be used as part of an {@link ObjectAnimator}).
     * The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     *                 of time elapsed of the overall animation duration.
     */
    @Nonnull
    public static Keyframe ofInt(float fraction) {
        return new IntKeyframe(fraction);
    }

    /**
     * Constructs a Keyframe object with the given time and value. The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     *                 of time elapsed of the overall animation duration.
     * @param value    The value that the object will animate to as the animation time approaches
     *                 the time in this keyframe, and the value animated from as the time passes the time in
     *                 this keyframe.
     */
    @Nonnull
    public static Keyframe ofFloat(float fraction, float value) {
        return new FloatKeyframe(fraction, value);
    }

    /**
     * Constructs a Keyframe object with the given time. The value at this time will be derived
     * from the target object when the animation first starts (note that this implies that keyframes
     * with no initial value must be used as part of an {@link ObjectAnimator}).
     * The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     *                 of time elapsed of the overall animation duration.
     */
    @Nonnull
    public static Keyframe ofFloat(float fraction) {
        return new FloatKeyframe(fraction);
    }

    /**
     * Constructs a Keyframe object with the given time and value. The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     *                 of time elapsed of the overall animation duration.
     * @param value    The value that the object will animate to as the animation time approaches
     *                 the time in this keyframe, and the value animated from as the time passes the time in
     *                 this keyframe.
     */
    @Nonnull
    public static Keyframe ofObject(float fraction, Object value) {
        return new ObjectKeyframe(fraction, value);
    }

    /**
     * Constructs a Keyframe object with the given time. The value at this time will be derived
     * from the target object when the animation first starts (note that this implies that keyframes
     * with no initial value must be used as part of an {@link ObjectAnimator}).
     * The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     *                 of time elapsed of the overall animation duration.
     */
    @Nonnull
    public static Keyframe ofObject(float fraction) {
        return new ObjectKeyframe(fraction, null);
    }

    /**
     * Indicates whether this keyframe has a valid value. This method is called internally when
     * an {@link ObjectAnimator} first starts; keyframes without values are assigned values at
     * that time by deriving the value for the property from the target object.
     *
     * @return boolean Whether this object has a value assigned.
     */
    public boolean hasValue() {
        return mHasValue;
    }

    /**
     * Gets the value for this Keyframe.
     *
     * @return The value for this Keyframe.
     */
    public abstract Object getValue();

    /**
     * Sets the value for this Keyframe.
     *
     * @param value value for this Keyframe.
     */
    public abstract void setValue(Object value);

    /**
     * Gets the time for this keyframe, as a fraction of the overall animation duration.
     *
     * @return The time associated with this keyframe, as a fraction of the overall animation
     * duration. This should be a value between 0 and 1.
     */
    public float getFraction() {
        return mFraction;
    }

    /**
     * Sets the time for this keyframe, as a fraction of the overall animation duration.
     *
     * @param fraction time associated with this keyframe, as a fraction of the overall animation
     *                 duration. This should be a value between 0 and 1.
     */
    public void setFraction(float fraction) {
        mFraction = fraction;
    }

    /**
     * Gets the optional interpolator for this Keyframe. A value of <code>null</code> indicates
     * that there is no interpolation, which is the same as linear interpolation.
     *
     * @return The optional interpolator for this Keyframe.
     */
    @Nullable
    public Interpolator getInterpolator() {
        return mInterpolator;
    }

    /**
     * Sets the optional interpolator for this Keyframe. A value of <code>null</code> indicates
     * that there is no interpolation, which is the same as linear interpolation.
     */
    public void setInterpolator(@Nullable Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    static class ObjectKeyframe extends Keyframe {

        /**
         * The value of the animation at the time mFraction.
         */
        Object mValue;

        ObjectKeyframe(float fraction, Object value) {
            mFraction = fraction;
            mValue = value;
            mHasValue = value != null;
        }

        @Override
        public Object getValue() {
            return mValue;
        }

        @Override
        public void setValue(Object value) {
            mValue = value;
            mHasValue = (value != null);
        }
    }

    /**
     * Internal subclass used when the keyframe value is of primitive type int.
     */
    static class IntKeyframe extends Keyframe {

        int mValue;

        IntKeyframe(float fraction, int value) {
            mFraction = fraction;
            mValue = value;
            mHasValue = true;
        }

        IntKeyframe(float fraction) {
            mFraction = fraction;
        }

        public int getIntValue() {
            return mValue;
        }

        @Override
        public Object getValue() {
            return mValue;
        }

        @Override
        public void setValue(Object value) {
            if (value instanceof Integer) {
                mValue = (Integer) value;
                mHasValue = true;
            }
        }
    }

    /**
     * Internal subclass used when the keyframe value is of primitive type float.
     */
    static class FloatKeyframe extends Keyframe {

        float mValue;

        FloatKeyframe(float fraction, float value) {
            mFraction = fraction;
            mValue = value;
            mHasValue = true;
        }

        FloatKeyframe(float fraction) {
            mFraction = fraction;
        }

        public float getFloatValue() {
            return mValue;
        }

        @Override
        public Object getValue() {
            return mValue;
        }

        @Override
        public void setValue(Object value) {
            if (value instanceof Float) {
                mValue = (Float) value;
                mHasValue = true;
            }
        }
    }
}
