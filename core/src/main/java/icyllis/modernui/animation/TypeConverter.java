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
 * Base interface used convert type T to another type V. This
 * is necessary when the value types of in animation are different
 * from the property type.
 *
 * @see PropertyValuesHolder#setConverter(TypeConverter)
 */
@FunctionalInterface
public interface TypeConverter<T, V> {

    /**
     * Converts a value from one type to another.
     * @param value The Object to convert.
     * @return A value of type V, converted from <code>value</code>.
     */
    V convert(T value);
}
