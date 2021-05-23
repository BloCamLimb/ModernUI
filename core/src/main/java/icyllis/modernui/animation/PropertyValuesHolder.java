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

/**
 * This class holds information about a property and the values that that property
 * should take on during an animation. PropertyValuesHolder objects can be used to create
 * animations with ValueAnimator or ObjectAnimator that operate on several different properties
 * in parallel.
 */
public class PropertyValuesHolder<V> {

    Property<Object, V> mProperty;

    /**
     * The set of keyframes (time/value pairs) that define this animation.
     */
    Keyframes<V> mKeyframes;

    /**
     * The value most recently calculated by calculateValue(). This is set during
     * that function and might be retrieved later either by ValueAnimator.animatedValue() or
     * by the property-setting logic in ObjectAnimator.animatedValue().
     */
    private V mAnimatedValue;

    /**
     * The type evaluator used to calculate the animated values. This evaluator is determined
     * automatically based on the type of the start/end objects passed into the constructor,
     * but the system only knows about the primitive types int and float. Any other
     * type will need to set the evaluator to a custom evaluator for that type.
     */
    private TypeEvaluator<V> mEvaluator;

    /**
     * Internal utility constructor, used by the factory methods to set the property.
     *
     * @param property The property for this holder.
     */
    @SuppressWarnings("unchecked")
    private PropertyValuesHolder(Property<?, V> property) {
        mProperty = (Property<Object, V>) property;
    }

    /**
     * Constructs and returns a PropertyValuesHolder with a given property and
     * set of int values.
     *
     * @param property The property being animated. Should not be null.
     * @param values   The values that the property will animate between.
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     */
    @Nonnull
    public static PropertyValuesHolder<Integer> ofInt(IntProperty<?> property, @Nonnull int... values) {
        return new IntPropertyValuesHolder(property, values);
    }

    /**
     * Constructs and returns a PropertyValuesHolder with a given property and
     * set of float values.
     *
     * @param property The property being animated. Should not be null.
     * @param values   The values that the property will animate between.
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     */
    @Nonnull
    public static PropertyValuesHolder<Float> ofFloat(FloatProperty<?> property, @Nonnull float... values) {
        return new FloatPropertyValuesHolder(property, values);
    }

    /**
     * Set the animated values for this object to this set of ints.
     * If there is only one value, it is assumed to be the end value of an animation,
     * and an initial value will be derived, if possible, by calling a getter function
     * on the object. Also, if any value is null, the value will be filled in when the animation
     * starts in the same way. This mechanism of automatically getting null values only works
     * if the PropertyValuesHolder object is used in conjunction
     * {@link ObjectAnimator}, and with a getter function
     * derived automatically from <code>propertyName</code>, since otherwise PropertyValuesHolder has
     * no way of determining what the value should be.
     *
     * @param values One or more values that the animation will animate between.
     */
    public void setIntValues(@Nonnull int... values) {
        throw new IllegalStateException();
    }

    /**
     * Set the animated values for this object to this set of floats.
     * If there is only one value, it is assumed to be the end value of an animation,
     * and an initial value will be derived, if possible, by calling a getter function
     * on the object. Also, if any value is null, the value will be filled in when the animation
     * starts in the same way. This mechanism of automatically getting null values only works
     * if the PropertyValuesHolder object is used in conjunction
     * {@link ObjectAnimator}, and with a getter function
     * derived automatically from <code>propertyName</code>, since otherwise PropertyValuesHolder has
     * no way of determining what the value should be.
     *
     * @param values One or more values that the animation will animate between.
     */
    public void setFloatValues(@Nonnull float... values) {
        throw new IllegalStateException();
    }

    /**
     * Set the animated values for this object to this set of Objects.
     * If there is only one value, it is assumed to be the end value of an animation,
     * and an initial value will be derived, if possible, by calling a getter function
     * on the object. Also, if any value is null, the value will be filled in when the animation
     * starts in the same way. This mechanism of automatically getting null values only works
     * if the PropertyValuesHolder object is used in conjunction
     * {@link ObjectAnimator}, and with a getter function
     * derived automatically from <code>propertyName</code>, since otherwise PropertyValuesHolder has
     * no way of determining what the value should be.
     *
     * <p><strong>Note:</strong> The Object values are stored as references to the original
     * objects, which means that changes to those objects after this method is called will
     * affect the values on the PropertyValuesHolder. If the objects will be mutated externally
     * after this method is called, callers should pass a copy of those objects instead.
     *
     * @param values One or more values that the animation will animate between.
     */
    @SafeVarargs
    public final void setObjectValues(@Nonnull V... values) {
        mKeyframes = KeyframeSet.ofObject(values);
        if (mEvaluator != null) {
            mKeyframes.setEvaluator(mEvaluator);
        }
    }

    private Object convertBack(Object value) {
        /*if (mConverter != null) {
            if (!(mConverter instanceof BidirectionalTypeConverter)) {
                throw new IllegalArgumentException("Converter "
                        + mConverter.getClass().getName()
                        + " must be a BidirectionalTypeConverter");
            }
            value = ((BidirectionalTypeConverter) mConverter).convertBack(value);
        }*/
        return value;
    }

    /**
     * This function is called by ObjectAnimator when setting the start values for an animation.
     * The start values are set according to the current values in the target object. The
     * property whose value is extracted is whatever is specified by the propertyName of this
     * PropertyValuesHolder object.
     *
     * @param target The object which holds the start values that should be set.
     */
    void setupStartValue(Object target) {
        if (mProperty != null) {
            Keyframe[] keyframes = mKeyframes.getKeyframes();
            if (keyframes.length > 0) {
                Object value = convertBack(mProperty.get(target));
                keyframes[0].setValue(value);
            }
        }
    }

    /**
     * This function is called by ObjectAnimator when setting the end values for an animation.
     * The end values are set according to the current values in the target object. The
     * property whose value is extracted is whatever is specified by the propertyName of this
     * PropertyValuesHolder object.
     *
     * @param target The object which holds the start values that should be set.
     */
    void setupEndValue(Object target) {
        if (mProperty != null) {
            Keyframe[] keyframes = mKeyframes.getKeyframes();
            if (keyframes.length > 0) {
                Object value = convertBack(mProperty.get(target));
                keyframes[keyframes.length - 1].setValue(value);
            }
        }
    }

    /**
     * Internal function to set the value on the target object, using the setter set up
     * earlier on this PropertyValuesHolder object. This function is called by ObjectAnimator
     * to handle turning the value calculated by ValueAnimator into a value set on the object
     * according to the name of the property.
     *
     * @param target The target object on which the value is set
     */
    void setAnimatedValue(Object target) {
        if (mProperty != null) {
            mProperty.set(target, getAnimatedValue());
        }
    }

    /**
     * Internal function, called by ValueAnimator, to set up the TypeEvaluator that will be used
     * to calculate animated values.
     */
    void init() {
        if (mEvaluator != null) {
            // KeyframeSet knows how to evaluate the common types - only give it a custom
            // evaluator if one has been set on this class
            mKeyframes.setEvaluator(mEvaluator);
        }
    }

    /**
     * The TypeEvaluator will be automatically determined based on the type of values
     * supplied to PropertyValuesHolder. The evaluator can be manually set, however, if so
     * desired. This may be important in cases where either the type of the values supplied
     * do not match the way that they should be interpolated between, or if the values
     * are of a custom type or one not currently understood by the animation system. Currently,
     * only values of type float and int (and their Object equivalents: Float
     * and Integer) are  correctly interpolated; all other types require setting a TypeEvaluator.
     */
    public void setEvaluator(TypeEvaluator<V> evaluator) {
        mEvaluator = evaluator;
        mKeyframes.setEvaluator(evaluator);
    }

    /**
     * Function used to calculate the value according to the evaluator set up for
     * this PropertyValuesHolder object. This function is called by ValueAnimator.animateValue().
     *
     * @param fraction The elapsed, interpolated fraction of the animation.
     */
    void calculateValue(float fraction) {
        mAnimatedValue = mKeyframes.getValue(fraction);
    }

    /**
     * Sets the property that will be animated.
     *
     * <p>Note that if this PropertyValuesHolder object is used with ObjectAnimator, the property
     * must exist on the target object specified in that ObjectAnimator.</p>
     *
     * @param property The property being animated.
     */
    public void setProperty(Property<Object, V> property) {
        mProperty = property;
    }

    /**
     * Internal function, called by ValueAnimator and ObjectAnimator, to retrieve the value
     * most recently calculated in calculateValue().
     */
    V getAnimatedValue() {
        return mAnimatedValue;
    }

    static class IntPropertyValuesHolder extends PropertyValuesHolder<Integer> {

        private int mIntAnimatedValue;

        /**
         * Internal utility constructor, used by the factory methods to set the property.
         *
         * @param property The property for this holder.
         */
        private IntPropertyValuesHolder(IntProperty<?> property, Keyframes.IntKeyframes keyframes) {
            super(property);
            mKeyframes = keyframes;
        }

        private IntPropertyValuesHolder(IntProperty<?> property, int... values) {
            super(property);
            setIntValues(values);
        }

        @Override
        public void setIntValues(@Nonnull int... values) {
            mKeyframes = KeyframeSet.ofInt(values);
        }

        @Override
        void setAnimatedValue(Object target) {
            if (mProperty != null) {
                ((IntProperty<Object>) mProperty).set(target, mIntAnimatedValue);
            }
        }

        @Override
        void calculateValue(float fraction) {
            mIntAnimatedValue = ((Keyframes.IntKeyframes) mKeyframes).getIntValue(fraction);
        }

        @Override
        Integer getAnimatedValue() {
            return mIntAnimatedValue;
        }
    }

    static class FloatPropertyValuesHolder extends PropertyValuesHolder<Float> {

        private float mFloatAnimatedValue;

        /**
         * Internal utility constructor, used by the factory methods to set the property.
         *
         * @param property The property for this holder.
         */
        private FloatPropertyValuesHolder(FloatProperty<?> property, Keyframes.FloatKeyframes keyframes) {
            super(property);
            mKeyframes = keyframes;
        }

        private FloatPropertyValuesHolder(FloatProperty<?> property, float... values) {
            super(property);
            setFloatValues(values);
        }

        @Override
        public void setFloatValues(@Nonnull float... values) {
            mKeyframes = KeyframeSet.ofFloat(values);
        }

        @Override
        void setAnimatedValue(Object target) {
            if (mProperty != null) {
                ((FloatProperty<Object>) mProperty).set(target, mFloatAnimatedValue);
            }
        }

        @Override
        void calculateValue(float fraction) {
            mFloatAnimatedValue = ((Keyframes.FloatKeyframes) mKeyframes).getFloatValue(fraction);
        }

        @Override
        Float getAnimatedValue() {
            return mFloatAnimatedValue;
        }
    }
}
