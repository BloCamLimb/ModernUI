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
 * An implementation of {@link Property} to be used specifically with fields of type
 * <code>int</code>. This type-specific subclass enables performance benefit by allowing
 * calls to a {@link #set(Object, int) setValue()} function that takes the primitive
 * <code>int</code> type and avoids autoboxing and other overhead associated with the
 * <code>Integer</code> class.
 *
 * @param <T> The class on which the Property is declared.
 */
public interface IntProperty<T> extends Property<T, Integer> {

    void set(T obj, int val);

    @Deprecated
    @Override
    default void set(T obj, @Nonnull Integer val) {
        set(obj, val.intValue());
    }
}
