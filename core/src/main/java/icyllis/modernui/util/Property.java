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

package icyllis.modernui.util;

/**
 * A property is an abstraction that can be used to represent a <emb>mutable</em> value that is held
 * in a <em>host</em> object. The Property's {@link #set(Object, Object)} or {@link #get(Object)}
 * methods can be implemented in terms of the private fields of the host object, or via "setter" and
 * "getter" methods or by some other mechanism, as appropriate.
 *
 * @param <T> the class on which the property is declared.
 * @param <V> the type that this property represents.
 */
public abstract class Property<T, V> {

    private final String mName;
    private final Class<V> mType;

    /**
     * A constructor that takes an identifying name and {@link #getType() type} for the property.
     */
    public Property(Class<V> type, String name) {
        mName = name;
        mType = type;
    }

    /**
     * Sets the value on <code>object</code> which this property represents. If the method is unable
     * to set the value on the target object it will throw an {@link UnsupportedOperationException}
     * exception.
     */
    public abstract void set(T object, V value);

    /**
     * Returns the current value that this property represents on the given <code>object</code>.
     */
    public abstract V get(T object);

    /**
     * Returns the name for this property.
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns the type for this property.
     */
    public Class<V> getType() {
        return mType;
    }
}
