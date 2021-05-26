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

/**
 * Abstract base class used convert type T to another type V and back again. This
 * is necessary when the value types of in animation are different from the property
 * type. BidiTypeConverter is needed when only the final value for the animation
 * is supplied to animators.
 *
 * @see PropertyValuesHolder#setConverter(TypeConverter)
 */
public abstract class BidiTypeConverter<T, V> implements TypeConverter<T, V> {

    BidiTypeConverter<V, T> mInvertedConverter;

    /**
     * Does a conversion from the target type back to the source type. The subclass
     * must implement this when a TypeConverter is used in animations and current
     * values will need to be read for an animation.
     *
     * @param value The Object to convert.
     * @return A value of type T, converted from <code>value</code>.
     */
    public abstract T convertBack(V value);

    /**
     * Returns the inverse of this converter, where the from and to classes are reversed.
     * The inverted converter uses this convert to call {@link #convertBack(Object)} for
     * {@link #convert(Object)} calls and {@link #convert(Object)} for
     * {@link #convertBack(Object)} calls.
     *
     * @return The inverse of this converter, where the from and to classes are reversed.
     */
    public final BidiTypeConverter<V, T> invert() {
        if (mInvertedConverter == null) {
            mInvertedConverter = new InvertedConverter<>(this);
        }
        return mInvertedConverter;
    }

    private static class InvertedConverter<From, To> extends BidiTypeConverter<From, To> {

        public InvertedConverter(BidiTypeConverter<To, From> converter) {
            mInvertedConverter = converter;
        }

        @Override
        public From convertBack(To value) {
            return mInvertedConverter.convert(value);
        }

        @Override
        public To convert(From value) {
            return mInvertedConverter.convertBack(value);
        }
    }
}
