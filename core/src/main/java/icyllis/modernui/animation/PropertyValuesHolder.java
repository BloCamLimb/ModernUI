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

/**
 * This class holds information about a property and the values that that property
 * should take on during an animation. PropertyValuesHolder objects can be used to create
 * animations with ValueAnimator or ObjectAnimator that operate on several properties
 * in parallel.
 */
@SuppressWarnings({"unchecked", "rawtypes", "unused"})
public sealed class PropertyValuesHolder implements Cloneable {

    Property mProperty;

    /**
     * The set of keyframes (time/value pairs) that define this animation.
     */
    Keyframes mKeyframes;

    /**
     * The value most recently calculated by calculateValue(). This is set during
     * that function and might be retrieved later either by ValueAnimator.animatedValue() or
     * by the property-setting logic in ObjectAnimator.animatedValue().
     */
    private Object mAnimatedValue;

    /**
     * The type evaluator used to calculate the animated values. This evaluator is determined
     * automatically based on the type of the start/end objects passed into the constructor,
     * but the system only knows about the primitive types int and float. Any other
     * type will need to set the evaluator to a custom evaluator for that type.
     */
    private TypeEvaluator mEvaluator;

    /**
     * Converts from the source Object type to the setter Object type.
     */
    private TypeConverter mConverter;

    /**
     * Internal utility constructor, used by the factory methods.
     */
    private PropertyValuesHolder() {
    }

    /**
     * Internal utility constructor, used by the factory methods to set the property.
     *
     * @param property The property for this holder.
     */
    private PropertyValuesHolder(Property<?, ?> property) {
        mProperty = property;
    }

    /**
     * Constructs and returns a PropertyValuesHolder with a given set of int values.
     *
     * @param values The values that the named property will animate between.
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     */
    @Nonnull
    public static PropertyValuesHolder ofInt(@Nonnull int... values) {
        return new IntPropertyValuesHolder(values);
    }

    /**
     * Constructs and returns a PropertyValuesHolder with a given property and
     * set of int values.
     *
     * @param property The property being animated.
     * @param values   The values that the property will animate between.
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     */
    @Nonnull
    public static PropertyValuesHolder ofInt(IntProperty<?> property,
                                             @Nonnull int... values) {
        return new IntPropertyValuesHolder(property, values);
    }

    /**
     * Constructs and returns a PropertyValuesHolder with a given set of float values.
     *
     * @param values The values that the named property will animate between.
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     */
    @Nonnull
    public static PropertyValuesHolder ofFloat(@Nonnull float... values) {
        return new FloatPropertyValuesHolder(values);
    }

    /**
     * Constructs and returns a PropertyValuesHolder with a given property and
     * set of float values.
     *
     * @param property The property being animated.
     * @param values   The values that the property will animate between.
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     */
    @Nonnull
    public static PropertyValuesHolder ofFloat(FloatProperty<?> property,
                                               @Nonnull float... values) {
        return new FloatPropertyValuesHolder(property, values);
    }

    /**
     * Constructs and returns a PropertyValuesHolder with a given set of Object values.
     * This variant also takes a TypeEvaluator because the system
     * cannot automatically interpolate between objects of unknown type.
     *
     * <p><strong>Note:</strong> The Object values are stored as references to the original
     * objects, which means that changes to those objects after this method is called will
     * affect the values on the PropertyValuesHolder. If the objects will be mutated externally
     * after this method is called, callers should pass a copy of those objects instead.
     *
     * @param evaluator A TypeEvaluator that will be called on each animation frame to
     *                  provide the necessary interpolation between the Object values to derive the animated
     *                  value.
     * @param values    The values that the named property will animate between.
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     */
    @Nonnull
    @SafeVarargs
    public static <V> PropertyValuesHolder ofObject(TypeEvaluator<V> evaluator, @Nonnull V... values) {
        PropertyValuesHolder pvh = new PropertyValuesHolder();
        pvh.setObjectValues((Object[]) values);
        pvh.setEvaluator(evaluator);
        return pvh;
    }

    /**
     * Constructs and returns a PropertyValuesHolder with a given property and
     * set of Object values. This variant also takes a TypeEvaluator because the system
     * cannot automatically interpolate between objects of unknown type.
     *
     * <p><strong>Note:</strong> The Object values are stored as references to the original
     * objects, which means that changes to those objects after this method is called will
     * affect the values on the PropertyValuesHolder. If the objects will be mutated externally
     * after this method is called, callers should pass a copy of those objects instead.
     *
     * @param property  The property being animated.
     * @param evaluator A TypeEvaluator that will be called on each animation frame to
     *                  provide the necessary interpolation between the Object values to derive the animated
     *                  value.
     * @param values    The values that the property will animate between.
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     */
    @Nonnull
    @SafeVarargs
    public static <V> PropertyValuesHolder ofObject(Property<?, V> property, TypeEvaluator<V> evaluator,
                                                    @Nonnull V... values) {
        PropertyValuesHolder pvh = new PropertyValuesHolder(property);
        pvh.setObjectValues((Object[]) values);
        pvh.setEvaluator(evaluator);
        return pvh;
    }

    /**
     * Constructs and returns a PropertyValuesHolder with a given property and
     * set of Object values. This variant also takes a TypeEvaluator because the system
     * cannot automatically interpolate between objects of unknown type. This variant also
     * takes a <code>TypeConverter</code> to convert from animated values to the type
     * of the property. If only one value is supplied, the <code>TypeConverter</code>
     * must be a {@link BidirectionalTypeConverter} to retrieve the current
     * value.
     *
     * <p><strong>Note:</strong> The Object values are stored as references to the original
     * objects, which means that changes to those objects after this method is called will
     * affect the values on the PropertyValuesHolder. If the objects will be mutated externally
     * after this method is called, callers should pass a copy of those objects instead.
     *
     * @param property  The property being animated.
     * @param converter Converts the animated object to the Property type.
     * @param evaluator A TypeEvaluator that will be called on each animation frame to
     *                  provide the necessary interpolation between the Object values to derive the animated
     *                  value.
     * @param values    The values that the property will animate between.
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     * @see #setConverter(TypeConverter)
     * @see TypeConverter
     */
    @Nonnull
    @SafeVarargs
    public static <V, P> PropertyValuesHolder ofObject(Property<?, P> property, TypeConverter<V, P> converter,
                                                       TypeEvaluator<V> evaluator, @Nonnull V... values) {
        PropertyValuesHolder pvh = new PropertyValuesHolder(property);
        pvh.setConverter(converter);
        pvh.setObjectValues((Object[]) values);
        pvh.setEvaluator(evaluator);
        return pvh;
    }

    /**
     * Constructs and returns a PropertyValuesHolder object with the specified property name and set
     * of values. These values can be of any type, but the type should be consistent so that
     * an appropriate {@link TypeEvaluator} can be found that matches
     * the common type.
     * <p>If there is only one value, it is assumed to be the end value of an animation,
     * and an initial value will be derived, if possible, by calling a getter function
     * on the object. Also, if any value is null, the value will be filled in when the animation
     * starts in the same way. This mechanism of automatically getting null values only works
     * if the PropertyValuesHolder object is used in conjunction
     * {@link ObjectAnimator}, and with a getter function
     * derived automatically from <code>propertyName</code>, since otherwise PropertyValuesHolder has
     * no way of determining what the value should be.
     *
     * @param values The set of values to animate between.
     */
    @Nonnull
    public static PropertyValuesHolder ofKeyframe(@Nonnull Keyframe... values) {
        Keyframes keyframes = KeyframeSet.ofKeyframe(values);
        if (keyframes instanceof Keyframes.IntKeyframes) {
            return new IntPropertyValuesHolder((Keyframes.IntKeyframes) keyframes);
        } else if (keyframes instanceof Keyframes.FloatKeyframes) {
            return new FloatPropertyValuesHolder((Keyframes.FloatKeyframes) keyframes);
        } else {
            PropertyValuesHolder pvh = new PropertyValuesHolder();
            pvh.mKeyframes = keyframes;
            return pvh;
        }
    }

    /**
     * Constructs and returns a PropertyValuesHolder object with the specified property and set
     * of values. These values can be of any type, but the type should be consistent so that
     * an appropriate {@link TypeEvaluator} can be found that matches
     * the common type.
     * <p>If there is only one value, it is assumed to be the end value of an animation,
     * and an initial value will be derived, if possible, by calling the property's
     * {@link Property#get(Object)} function.
     * Also, if any value is null, the value will be filled in when the animation
     * starts in the same way. This mechanism of automatically getting null values only works
     * if the PropertyValuesHolder object is used in conjunction with
     * {@link ObjectAnimator}, since otherwise PropertyValuesHolder has
     * no way of determining what the value should be.
     *
     * @param property The property associated with this set of values.
     * @param values   The set of values to animate between.
     */
    @Nonnull
    public static PropertyValuesHolder ofKeyframe(Property<?, ?> property, @Nonnull Keyframe... values) {
        PropertyValuesHolder pvh = new PropertyValuesHolder(property);
        pvh.setKeyframes(values);
        return pvh;
    }

    /**
     * Constructs and returns a PropertyValuesHolder object with the specified property and set
     * of values. These values can be of any type, but the type should be consistent so that
     * an appropriate {@link TypeEvaluator} can be found that matches
     * the common type.
     * <p>If there is only one value, it is assumed to be the end value of an animation,
     * and an initial value will be derived, if possible, by calling the property's
     * {@link Property#get(Object)} function.
     * Also, if any value is null, the value will be filled in when the animation
     * starts in the same way. This mechanism of automatically getting null values only works
     * if the PropertyValuesHolder object is used in conjunction with
     * {@link ObjectAnimator}, since otherwise PropertyValuesHolder has
     * no way of determining what the value should be.
     *
     * @param property  The property associated with this set of values.
     * @param converter Converts the animated object to the Property type.
     * @param values    The set of values to animate between.
     */
    @Nonnull
    public static <P> PropertyValuesHolder ofKeyframe(Property<?, P> property, TypeConverter<?, P> converter,
                                                      @Nonnull Keyframe... values) {
        PropertyValuesHolder pvh = new PropertyValuesHolder(property);
        pvh.setConverter(converter);
        pvh.setKeyframes(values);
        return pvh;
    }

    /**
     * Constructs and returns a PropertyValuesHolder object with the specified int property and set
     * of values.
     * <p>
     * Also, if any value is null, the value will be filled in when the animation
     * starts in the same way. This mechanism of automatically getting null values only works
     * if the PropertyValuesHolder object is used in conjunction with
     * {@link ObjectAnimator}, since otherwise PropertyValuesHolder has
     * no way of determining what the value should be.
     *
     * @param property The property associated with this set of values.
     * @param values   The set of values to animate between.
     * @throws IllegalArgumentException some keyframes are not int keyframes, or less than two
     * @see Keyframe#ofInt(float, int)
     */
    @Nonnull
    public static PropertyValuesHolder ofKeyframe(IntProperty<?> property, @Nonnull Keyframe... values) {
        Keyframes keyframes = KeyframeSet.ofKeyframe(values);
        if (keyframes instanceof Keyframes.IntKeyframes) {
            return new IntPropertyValuesHolder(property, (Keyframes.IntKeyframes) keyframes);
        } else {
            throw new IllegalArgumentException("Some keyframes are not int keyframes");
        }
    }

    /**
     * Constructs and returns a PropertyValuesHolder object with the specified float property and set
     * of values.
     * <p>
     * Also, if any value is null, the value will be filled in when the animation
     * starts in the same way. This mechanism of automatically getting null values only works
     * if the PropertyValuesHolder object is used in conjunction with
     * {@link ObjectAnimator}, since otherwise PropertyValuesHolder has
     * no way of determining what the value should be.
     *
     * @param property The property associated with this set of values.
     * @param values   The set of values to animate between.
     * @throws IllegalArgumentException some keyframes are not float keyframes, or less than two
     * @see Keyframe#ofFloat(float, float)
     */
    @Nonnull
    public static PropertyValuesHolder ofKeyframe(FloatProperty<?> property, @Nonnull Keyframe... values) {
        Keyframes keyframes = KeyframeSet.ofKeyframe(values);
        if (keyframes instanceof Keyframes.FloatKeyframes) {
            return new FloatPropertyValuesHolder(property, (Keyframes.FloatKeyframes) keyframes);
        } else {
            throw new IllegalArgumentException("Some keyframes are not float keyframes");
        }
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
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    /**
     * Set the animated values for this object to this set of Keyframes.
     *
     * @param values One or more values that the animation will animate between.
     */
    public void setKeyframes(@Nonnull Keyframe... values) {
        mKeyframes = KeyframeSet.ofKeyframe(values);
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
    public void setObjectValues(@Nonnull Object... values) {
        mKeyframes = KeyframeSet.ofObject(values);
        if (mEvaluator != null) {
            mKeyframes.setEvaluator(mEvaluator);
        }
    }

    /**
     * Sets the converter to convert from the values type to the setter's parameter type.
     * If only one value is supplied or target is changeable, <var>converter</var> must
     * be a {@link BidirectionalTypeConverter}.
     *
     * @param converter The converter to use to convert values.
     */
    public void setConverter(TypeConverter<?, ?> converter) {
        mConverter = converter;
    }

    /**
     * Internal function (called from ObjectAnimator) to set up the setter and getter
     * prior to running the animation. If the setter has not been manually set for this
     * object, it will be derived automatically given the property name, target object, and
     * types of values supplied. If no getter has been set, it will be supplied if any of the
     * supplied values was null. If there is a null value, then the getter (supplied or derived)
     * will be called to set those null values to the current value of the property
     * on the target object.
     *
     * @param target The object on which the setter (and possibly getter) exist.
     */
    void setupSetterAndGetter(@Nonnull Object target) {
        if (mProperty != null) {
            Object testValue = null;
            Keyframe[] keyframes = mKeyframes.getKeyframes();
            int count = keyframes == null ? 0 : keyframes.length;
            for (int i = 0; i < count; i++) {
                Keyframe kf = keyframes[i];
                if (!kf.hasValue() || kf.mValueWasSetOnStart) {
                    if (testValue == null) {
                        testValue = convertBack(mProperty.get(target));
                    }
                    kf.setValue(testValue);
                    kf.mValueWasSetOnStart = true;
                }
            }
        }
    }

    private Object convertBack(Object value) {
        if (mConverter != null) {
            if (!(mConverter instanceof BidirectionalTypeConverter)) {
                throw new IllegalArgumentException("Converter "
                        + mConverter.getClass().getName()
                        + " must be a BidirectionalTypeConverter");
            }
            return ((BidirectionalTypeConverter) mConverter).convertBack(value);
        }
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
    void setupStartValue(@Nonnull Object target) {
        if (mProperty == null) {
            return;
        }
        Keyframe[] keyframes = mKeyframes.getKeyframes();
        if (keyframes.length > 0) {
            Object value = convertBack(mProperty.get(target));
            keyframes[0].setValue(value);
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
    void setupEndValue(@Nonnull Object target) {
        if (mProperty == null) {
            return;
        }
        Keyframe[] keyframes = mKeyframes.getKeyframes();
        if (keyframes.length > 0) {
            Object value = convertBack(mProperty.get(target));
            keyframes[keyframes.length - 1].setValue(value);
        }
    }

    @Override
    public PropertyValuesHolder clone() {
        try {
            PropertyValuesHolder newPVH = (PropertyValuesHolder) super.clone();
            newPVH.mProperty = mProperty;
            newPVH.mKeyframes = mKeyframes.copy();
            newPVH.mEvaluator = mEvaluator;
            return newPVH;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
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
    void setAnimatedValue(@Nonnull Object target) {
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
    public void setEvaluator(TypeEvaluator<?> evaluator) {
        mEvaluator = evaluator;
        mKeyframes.setEvaluator(mEvaluator);
    }

    /**
     * Inverts all keyframes on the track. This will produce an inverted animation
     * that plays from endValue to startValue. Note that this is different from reversing
     * that plays in reverse order, this operation keeps the order of interpolation
     * and timeline still forwards.
     */
    public void invert() {
        Keyframe[] keyframes = mKeyframes.getKeyframes();
        if (keyframes != null) {
            for (Keyframe keyframe : keyframes) {
                keyframe.setFraction(1.0f - keyframe.getFraction());
            }
        }
    }

    /**
     * Function used to calculate the value according to the evaluator set up for
     * this PropertyValuesHolder object.
     *
     * @param fraction The elapsed, interpolated fraction of the animation.
     */
    void calculateValue(float fraction) {
        Object value = mKeyframes.getValue(fraction);
        mAnimatedValue = mConverter == null ? value : mConverter.convert(value);
    }

    /**
     * Sets the property that will be animated.
     *
     * <p>Note that if this PropertyValuesHolder object is used with ObjectAnimator, the property
     * must exist on the target object specified in that ObjectAnimator.</p>
     *
     * @param property The property being animated.
     */
    public void setProperty(Property<?, ?> property) {
        mProperty = property;
    }

    /**
     * Internal function, called by ValueAnimator and ObjectAnimator, to retrieve the value
     * most recently calculated in calculateValue().
     */
    Object getAnimatedValue() {
        return mAnimatedValue;
    }

    @Override
    public String toString() {
        return (mProperty != null ? mProperty.getName() : "") + ": " + mKeyframes.toString();
    }

    static final class IntPropertyValuesHolder extends PropertyValuesHolder {

        private int mIntAnimatedValue;

        private IntPropertyValuesHolder(@Nonnull Keyframes.IntKeyframes keyframes) {
            mKeyframes = keyframes;
        }

        /**
         * Internal utility constructor, used by the factory methods to set the property.
         *
         * @param property The property for this holder.
         */
        private IntPropertyValuesHolder(IntProperty<?> property, @Nonnull Keyframes.IntKeyframes keyframes) {
            super(property);
            mKeyframes = keyframes;
        }

        private IntPropertyValuesHolder(@Nonnull int... values) {
            setIntValues(values);
        }

        private IntPropertyValuesHolder(IntProperty<?> property, @Nonnull int... values) {
            super(property);
            setIntValues(values);
        }

        @Override
        public void setProperty(Property<?, ?> property) {
            if (property != null && !(property instanceof IntProperty)) {
                throw new IllegalArgumentException();
            }
            super.setProperty(property);
        }

        @Override
        public void setIntValues(@Nonnull int... values) {
            mKeyframes = KeyframeSet.ofInt(values);
        }

        @Override
        public void setKeyframes(@Nonnull Keyframe... values) {
            super.setKeyframes(values);
            if (!(mKeyframes instanceof Keyframes.IntKeyframes)) {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public void setObjectValues(@Nonnull Object... values) {
            throw new UnsupportedOperationException();
        }

        @Override
        void setAnimatedValue(@Nonnull Object target) {
            if (mProperty != null) {
                ((IntProperty) mProperty).setValue(target, mIntAnimatedValue);
            }
        }

        @Override
        void calculateValue(float fraction) {
            mIntAnimatedValue = ((Keyframes.IntKeyframes) mKeyframes).getIntValue(fraction);
        }

        @Override
        public void setConverter(TypeConverter<?, ?> converter) {
            throw new UnsupportedOperationException();
        }

        @Override
        Integer getAnimatedValue() {
            return mIntAnimatedValue;
        }

        @Override
        public IntPropertyValuesHolder clone() {
            return (IntPropertyValuesHolder) super.clone();
        }
    }

    static final class FloatPropertyValuesHolder extends PropertyValuesHolder {

        private float mFloatAnimatedValue;

        private FloatPropertyValuesHolder(@Nonnull Keyframes.FloatKeyframes keyframes) {
            mKeyframes = keyframes;
        }

        /**
         * Internal utility constructor, used by the factory methods to set the property.
         *
         * @param property The property for this holder.
         */
        private FloatPropertyValuesHolder(FloatProperty<?> property,
                                          @Nonnull Keyframes.FloatKeyframes keyframes) {
            super(property);
            mKeyframes = keyframes;
        }

        private FloatPropertyValuesHolder(@Nonnull float... values) {
            setFloatValues(values);
        }

        private FloatPropertyValuesHolder(FloatProperty<?> property, @Nonnull float... values) {
            super(property);
            setFloatValues(values);
        }

        @Override
        public void setProperty(Property<?, ?> property) {
            if (property != null && !(property instanceof FloatProperty)) {
                throw new IllegalArgumentException();
            }
            super.setProperty(property);
        }

        @Override
        public void setFloatValues(@Nonnull float... values) {
            mKeyframes = KeyframeSet.ofFloat(values);
        }

        @Override
        public void setKeyframes(@Nonnull Keyframe... values) {
            super.setKeyframes(values);
            if (!(mKeyframes instanceof Keyframes.FloatKeyframes)) {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public void setObjectValues(@Nonnull Object... values) {
            throw new UnsupportedOperationException();
        }

        @Override
        void setAnimatedValue(@Nonnull Object target) {
            if (mProperty != null) {
                ((FloatProperty) mProperty).setValue(target, mFloatAnimatedValue);
            }
        }

        @Override
        void calculateValue(float fraction) {
            mFloatAnimatedValue = ((Keyframes.FloatKeyframes) mKeyframes).getFloatValue(fraction);
        }

        @Override
        public void setConverter(TypeConverter<?, ?> converter) {
            throw new UnsupportedOperationException();
        }

        @Override
        Float getAnimatedValue() {
            return mFloatAnimatedValue;
        }

        @Override
        public FloatPropertyValuesHolder clone() {
            return (FloatPropertyValuesHolder) super.clone();
        }
    }
}
