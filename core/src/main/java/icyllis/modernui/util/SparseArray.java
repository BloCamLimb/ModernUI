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

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package icyllis.modernui.util;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

/**
 * <code>SparseArray</code> maps integers to Objects and, unlike a normal array of Objects,
 * its indices can contain gaps. <code>SparseArray</code> is intended to be more memory-efficient
 * than a <code>HashMap</code>, because it avoids auto-boxing keys and its data structure doesn't
 * rely on an extra entry object for each mapping.
 *
 * <p>Note that this container keeps its mappings in an array data structure,
 * using a binary search to find keys. The implementation is not intended to be appropriate for
 * data structures that may contain large numbers of items. It is generally slower than a
 * <code>HashMap</code> because lookups require a binary search,
 * and adds and removes require inserting
 * and deleting entries in the array. For containers holding up to hundreds of items,
 * the performance difference is less than 50%.
 *
 * <p>To help with performance, the container includes an optimization when removing
 * keys: instead of compacting its array immediately, it leaves the removed entry marked
 * as deleted. The entry can then be re-used for the same key or compacted later in
 * a single garbage collection of all removed entries. This garbage collection
 * must be performed whenever the array needs to be grown, or when the map size or
 * entry values are retrieved.
 *
 * <p>It is possible to iterate over the items in this container using
 * {@link #keyAt(int)} and {@link #valueAt(int)}. Iterating over the keys using
 * <code>keyAt(int)</code> with ascending values of the index returns the
 * keys in ascending order. In the case of <code>valueAt(int)</code>, the
 * values corresponding to the keys are returned in ascending order.
 *
 * <p>Modified from Android.
 */
@SuppressWarnings("unchecked")
public class SparseArray<E> implements Cloneable {

    private static final Object DELETED = new Object();
    private boolean mGarbage = false;

    private int[] mKeys;
    private Object[] mValues;
    private int mSize;

    /**
     * Creates a new SparseArray containing no mappings.
     */
    public SparseArray() {
        this(10);
    }

    /**
     * Creates a new SparseArray containing no mappings that will not
     * require any additional memory allocation to store the specified
     * number of mappings.  If you supply an initial capacity of 0, the
     * sparse array will be initialized with a light-weight representation
     * not requiring any additional array allocations.
     */
    public SparseArray(int initialCapacity) {
        if (initialCapacity == 0) {
            mKeys = IntArrays.EMPTY_ARRAY;
            mValues = ObjectArrays.EMPTY_ARRAY;
        } else if (initialCapacity > 0) {
            mKeys = new int[initialCapacity];
            mValues = new Object[initialCapacity];
        } else {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
    }

    /**
     * Gets the Object mapped from the specified key, or <code>null</code>
     * if no such mapping has been made.
     */
    public E get(int key) {
        return get(key, null);
    }

    /**
     * Gets the Object mapped from the specified key, or the specified Object
     * if no such mapping has been made.
     */
    public E get(int key, E defaultValue) {
        int i = Arrays.binarySearch(mKeys, 0, mSize, key);

        if (i < 0 || mValues[i] == DELETED) {
            return defaultValue;
        } else {
            return (E) mValues[i];
        }
    }

    /**
     * Removes the mapping from the specified key, if there was any.
     */
    public void delete(int key) {
        int i = Arrays.binarySearch(mKeys, 0, mSize, key);

        if (i >= 0) {
            if (mValues[i] != DELETED) {
                mValues[i] = DELETED;
                mGarbage = true;
            }
        }
    }

    /**
     * Removes the mapping from the specified key, if there was any, returning the old value.
     */
    public E remove(int key) {
        int i = Arrays.binarySearch(mKeys, 0, mSize, key);

        if (i >= 0) {
            if (mValues[i] != DELETED) {
                final E old = (E) mValues[i];
                mValues[i] = DELETED;
                mGarbage = true;
                return old;
            }
        }

        return null;
    }

    /**
     * Remove an existing key from the array map only if it is currently mapped to {@code value}.
     *
     * @param key   The key of the mapping to remove.
     * @param value The value expected to be mapped to the key.
     * @return Returns true if the mapping was removed.
     */
    public boolean remove(int key, Object value) {
        int index = indexOfKey(key);
        if (index >= 0) {
            E mapValue = valueAt(index);
            if (Objects.equals(value, mapValue)) {
                deleteAt(index);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the mapping at the specified index, if there was any.
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= size()})
     */
    public void deleteAt(int index) {
        Objects.checkIndex(index, mSize);
        if (mValues[index] != DELETED) {
            mValues[index] = DELETED;
            mGarbage = true;
        }
    }

    /**
     * Removes the mapping at the specified index, if there was any, returning the old value.
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= size()})
     */
    public E removeAt(int index) {
        Objects.checkIndex(index, mSize);
        if (mValues[index] != DELETED) {
            final E old = (E) mValues[index];
            mValues[index] = DELETED;
            mGarbage = true;
            return old;
        }
        return null;
    }

    /**
     * Remove a range of mappings as a batch.
     *
     * @param index Index to begin at
     * @param size  Number of mappings to remove
     */
    public void removeAtRange(int index, int size) {
        Objects.checkFromIndexSize(index, size, mSize);
        while (size-- > 0) {
            if (mValues[index] != DELETED) {
                mValues[index] = DELETED;
                mGarbage = true;
            }
            index++;
        }
    }

    /**
     * Replace the mapping for {@code key} only if it is already mapped to a value.
     *
     * @param key   The key of the mapping to replace.
     * @param value The value to store for the given key.
     * @return Returns the previous mapped value or null.
     */
    @Nullable
    public E replace(int key, E value) {
        int index = indexOfKey(key);
        if (index >= 0) {
            E oldValue = (E) mValues[index];
            mValues[index] = value;
            return oldValue;
        }
        return null;
    }

    /**
     * Replace the mapping for {@code key} only if it is already mapped to a value.
     *
     * @param key      The key of the mapping to replace.
     * @param oldValue The value expected to be mapped to the key.
     * @param newValue The value to store for the given key.
     * @return Returns true if the value was replaced.
     */
    public boolean replace(int key, E oldValue, E newValue) {
        int index = indexOfKey(key);
        if (index >= 0) {
            Object mapValue = mValues[index];
            if (Objects.equals(oldValue, mapValue)) {
                mValues[index] = newValue;
                return true;
            }
        }
        return false;
    }

    private void gc() {
        int n = mSize;
        int o = 0;
        int[] keys = mKeys;
        Object[] values = mValues;

        for (int i = 0; i < n; i++) {
            Object val = values[i];

            if (val != DELETED) {
                if (i != o) {
                    keys[o] = keys[i];
                    values[o] = val;
                    values[i] = null;
                }

                o++;
            }
        }

        mGarbage = false;
        mSize = o;
    }

    /**
     * Adds a mapping from the specified key to the specified value,
     * replacing the previous mapping from the specified key if there
     * was one.
     *
     * @return Returns the previous mapped value or null.
     */
    @Nullable
    public E put(int key, E value) {
        int i = Arrays.binarySearch(mKeys, 0, mSize, key);

        if (i >= 0) {
            E oldValue = (E) mValues[i];
            mValues[i] = value;
            return oldValue;
        } else {
            i = ~i;

            if (i < mSize && mValues[i] == DELETED) {
                mKeys[i] = key;
                mValues[i] = value;
                return null;
            }

            if (mGarbage && mSize >= mKeys.length) {
                gc();

                // Search again because indices may have changed.
                i = ~Arrays.binarySearch(mKeys, 0, mSize, key);
            }

            mKeys = GrowingArrayUtils.insert(mKeys, mSize, i, key);
            mValues = GrowingArrayUtils.insert(mValues, mSize, i, value);
            mSize++;
            return null;
        }
    }

    /**
     * Copies all the mappings from the {@code other} to this map. The effect of this call is
     * equivalent to that of calling {@link #put(int, Object)} on this map once for each mapping
     * from key to value in {@code other}.
     */
    public void putAll(@Nonnull SparseArray<? extends E> other) {
        for (int i = 0, size = other.size(); i < size; i++) {
            put(other.keyAt(i), other.valueAt(i));
        }
    }

    /**
     * Add a new value to the array map only if the key does not already have a value or it is
     * mapped to {@code null}.
     *
     * @param key   The key under which to store the value.
     * @param value The value to store for the given key.
     * @return Returns the value that was stored for the given key, or null if there
     * was no such key.
     */
    @Nullable
    public E putIfAbsent(int key, E value) {
        E mapValue = get(key);
        if (mapValue == null) {
            put(key, value);
        }
        return mapValue;
    }

    /**
     * Returns the number of key-value mappings that this SparseArray
     * currently stores.
     */
    public int size() {
        if (mGarbage) {
            gc();
        }

        return mSize;
    }

    /**
     * Return true if size() is 0.
     *
     * @return true if size() is 0.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Given an index in the range <code>0...size()-1</code>, returns
     * the key from the <code>index</code>th key-value mapping that this
     * SparseArray stores.
     *
     * <p>The keys corresponding to indices in ascending order are guaranteed to
     * be in ascending order, e.g., <code>keyAt(0)</code> will return the
     * smallest key and <code>keyAt(size()-1)</code> will return the largest
     * key.</p>
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= size()})
     */
    public int keyAt(int index) {
        Objects.checkIndex(index, mSize);
        if (mGarbage) {
            gc();
        }

        return mKeys[index];
    }

    /**
     * Given an index in the range <code>0...size()-1</code>, returns
     * the value from the <code>index</code>th key-value mapping that this
     * SparseArray stores.
     *
     * <p>The values corresponding to indices in ascending order are guaranteed
     * to be associated with keys in ascending order, e.g.,
     * <code>valueAt(0)</code> will return the value associated with the
     * smallest key and <code>valueAt(size()-1)</code> will return the value
     * associated with the largest key.</p>
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= size()})
     */
    public E valueAt(int index) {
        Objects.checkIndex(index, mSize);
        if (mGarbage) {
            gc();
        }

        return (E) mValues[index];
    }

    /**
     * Given an index in the range <code>0...size()-1</code>, sets a new
     * value for the <code>index</code>th key-value mapping that this
     * SparseArray stores.
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= size()})
     */
    public void setValueAt(int index, E value) {
        Objects.checkIndex(index, mSize);
        if (mGarbage) {
            gc();
        }

        mValues[index] = value;
    }

    /**
     * Returns the index for which {@link #keyAt} would return the
     * specified key, or a negative number if the specified
     * key is not mapped.
     */
    public int indexOfKey(int key) {
        if (mGarbage) {
            gc();
        }

        return Arrays.binarySearch(mKeys, 0, mSize, key);
    }

    /**
     * Returns an index for which {@link #valueAt} would return the
     * specified value, or a negative number if no keys map to the
     * specified value.
     * <p>Beware that this is a linear search, unlike lookups by key,
     * and that multiple keys can map to the same value and this will
     * find only one of them.
     * <p>Note also that unlike most collections' {@code indexOf} methods,
     * this method compares values using {@code ==} rather than {@code equals}.
     */
    public int indexOfValue(E value) {
        if (mGarbage) {
            gc();
        }

        for (int i = 0; i < mSize; i++) {
            if (mValues[i] == value) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Returns an index for which {@link #valueAt} would return the
     * specified value, or a negative number if no keys map to the
     * specified value.
     * <p>Beware that this is a linear search, unlike lookups by key,
     * and that multiple keys can map to the same value and this will
     * find only one of them.
     * <p>Note also that this method uses {@code equals} unlike {@code indexOfValue}.
     */
    public int indexOfValueByValue(E value) {
        if (mGarbage) {
            gc();
        }

        for (int i = 0; i < mSize; i++) {
            if (value == null) {
                if (mValues[i] == null) {
                    return i;
                }
            } else {
                if (value.equals(mValues[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Returns true if the key exists in the array. This is equivalent to
     * {@link #indexOfKey(int)} >= 0.
     *
     * @param key Potential key in the mapping
     * @return true if the key is defined in the mapping
     */
    public boolean containsKey(int key) {
        return indexOfKey(key) >= 0;
    }

    /**
     * Returns true if the specified value is mapped from any key.
     */
    public boolean containsValue(E value) {
        return indexOfValue(value) >= 0;
    }

    /**
     * Removes all key-value mappings from this SparseArray.
     */
    public void clear() {
        int n = mSize;
        Object[] values = mValues;

        for (int i = 0; i < n; i++) {
            values[i] = null;
        }

        mSize = 0;
        mGarbage = false;
    }

    /**
     * Puts a key/value pair into the array, optimizing for the case where
     * the key is greater than all existing keys in the array.
     */
    public void append(int key, E value) {
        if (mSize != 0 && key <= mKeys[mSize - 1]) {
            put(key, value);
            return;
        }

        if (mGarbage && mSize >= mKeys.length) {
            gc();
        }

        mKeys = GrowingArrayUtils.append(mKeys, mSize, key);
        mValues = GrowingArrayUtils.append(mValues, mSize, value);
        mSize++;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation composes a string by iterating over its mappings. If
     * this map contains itself as a value, the string "(this Map)"
     * will appear in its place.
     */
    @Override
    public String toString() {
        if (size() <= 0) {
            return "{}";
        }

        StringBuilder buffer = new StringBuilder(mSize * 28);
        buffer.append('{');
        for (int i = 0; i < mSize; i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            int key = keyAt(i);
            buffer.append(key);
            buffer.append('=');
            Object value = valueAt(i);
            if (value != this) {
                buffer.append(value);
            } else {
                buffer.append("(this Map)");
            }
        }
        buffer.append('}');
        return buffer.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SparseArray<?> other = (SparseArray<?>) o;

        int size = size();
        if (size != other.size()) {
            return false;
        }

        for (int index = 0; index < size; index++) {
            int key = keyAt(index);
            if (!Objects.equals(valueAt(index), other.get(key))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        int size = size();
        for (int index = 0; index < size; index++) {
            int key = keyAt(index);
            E value = valueAt(index);
            hash = 31 * hash + Integer.hashCode(key);
            hash = 31 * hash + Objects.hashCode(value);
        }
        return hash;
    }

    @Override
    public SparseArray<E> clone() {
        try {
            SparseArray<E> clone = (SparseArray<E>) super.clone();
            clone.mKeys = mKeys.clone();
            clone.mValues = mValues.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
