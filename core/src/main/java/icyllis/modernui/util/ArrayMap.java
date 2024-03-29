/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

import icyllis.modernui.ModernUI;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * <code>ArrayMap</code> is a generic key->value mapping data structure that is
 * designed to be more memory efficient than a {@link HashMap}.
 * It keeps its mappings in an array data structure -- an integer array of hash
 * codes for each item, and an Object array of the key/value pairs.  This allows it to
 * avoid having to create an extra object for every entry put in to the map, and it
 * also tries to control the growth of the size of these arrays more aggressively
 * (since growing them only requires copying the entries in the array, not rebuilding
 * a hash map).
 *
 * <p>Note that this implementation is not intended to be appropriate for data structures
 * that may contain large numbers of items.  It is generally slower than a traditional
 * HashMap, since lookups require a binary search and adds and removes require inserting
 * and deleting entries in the array.  For containers holding up to hundreds of items,
 * the performance difference is not significant, less than 50%.</p>
 *
 * <p>Because this container is intended to better balance memory use, unlike most other
 * standard Java containers it will shrink its array as items are removed from it.  Currently
 * you have no control over this shrinking -- if you set a capacity and then remove an
 * item, it may reduce the capacity to better match the current size.  In the future an
 * explicit call to set the capacity should turn off this aggressive shrinking behavior.</p>
 *
 * <p>This structure is <b>NOT</b> thread-safe.</p>
 *
 * @see it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 */
@SuppressWarnings("unchecked")
public class ArrayMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {

    private static final Marker MARKER = MarkerManager.getMarker("ArrayMap");

    /**
     * The minimum amount by which the capacity of a ArrayMap will increase.
     * This is tuned to be relatively space-efficient.
     */
    private static final int BASE_SIZE = 4;

    /**
     * Maximum number of entries to have in array caches.
     */
    private static final int CACHE_SIZE = 10;

    /**
     * Caches of small array objects to avoid spamming garbage.  The cache
     * Object[] variable is a pointer to a linked list of array objects.
     * The first entry in the array is a pointer to the next array in the
     * list; the second entry is a pointer to the int[] hash code array for it.
     */
    private static Object[] mBaseCache;
    private static int mBaseCacheSize;
    private static Object[] mTwiceBaseCache;
    private static int mTwiceBaseCacheSize;

    /**
     * Separate locks for each cache since each can be accessed independently of the other without
     * risk of a deadlock.
     */
    private static final Object sBaseCacheLock = new Object();
    private static final Object sTwiceBaseCacheLock = new Object();

    private final boolean mIdentityHashCode;

    int[] mHashes;
    Object[] mArray;
    int mSize;

    EntrySet mEntrySet;
    KeySet mKeySet;
    ValuesCollection mValues;

    /**
     * Create a new empty ArrayMap.  The default capacity of an array map is 0, and
     * will grow once items are added to it.
     */
    public ArrayMap() {
        this(0, false);
    }

    /**
     * Create a new ArrayMap with a given initial capacity.
     */
    public ArrayMap(int initialCapacity) {
        this(initialCapacity, false);
    }

    @ApiStatus.Internal
    public ArrayMap(int initialCapacity, boolean identityHashCode) {
        mIdentityHashCode = identityHashCode;
        if (initialCapacity == 0) {
            mHashes = IntArrays.EMPTY_ARRAY;
            mArray = ObjectArrays.EMPTY_ARRAY;
        } else if (initialCapacity > 0) {
            allocArrays(initialCapacity);
        } else {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
    }

    /**
     * Create a new ArrayMap with the mappings from the given Map.
     */
    public ArrayMap(@Nonnull Map<K, V> map) {
        this(0, false);
        putAll(map);
    }

    /**
     * Make the array map empty.  All storage is released.
     */
    @Override
    public void clear() {
        if (mSize > 0) {
            final int[] hashes = mHashes;
            final Object[] array = mArray;
            final int size = mSize;
            mHashes = IntArrays.EMPTY_ARRAY;
            mArray = ObjectArrays.EMPTY_ARRAY;
            mSize = 0;
            freeArrays(hashes, array, size);
        }
        if (mSize > 0) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Like {@link #clear}, but doesn't reduce the capacity of the ArrayMap.
     */
    @ApiStatus.Internal
    public void erase() {
        if (mSize > 0) {
            final int N = mSize << 1;
            final Object[] array = mArray;
            for (int i = 0; i < N; i++) {
                array[i] = null;
            }
            mSize = 0;
        }
    }

    /**
     * Ensure the array map can hold at least <var>minimumCapacity</var>
     * items.
     */
    public void ensureCapacity(int minimumCapacity) {
        final int size = mSize;
        if (mHashes.length < minimumCapacity) {
            final int[] hashes = mHashes;
            final Object[] array = mArray;
            allocArrays(minimumCapacity);
            if (mSize > 0) {
                System.arraycopy(hashes, 0, mHashes, 0, size);
                System.arraycopy(array, 0, mArray, 0, size << 1);
            }
            freeArrays(hashes, array, size);
        }
        if (mSize != size) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Check whether a key exists in the array.
     *
     * @param key The key to search for.
     * @return Returns true if the key exists, else false.
     */
    @Override
    public boolean containsKey(Object key) {
        return indexOfKey(key) >= 0;
    }

    /**
     * Returns the index of a key in the set.
     *
     * @param key The key to search for.
     * @return Returns the index of the key if it exists, else a negative integer.
     */
    public int indexOfKey(Object key) {
        return key == null ? indexOfNull()
                : indexOf(key, mIdentityHashCode ? System.identityHashCode(key) : key.hashCode());
    }

    /**
     * Returns an index for which {@link #valueAt} would return the
     * specified value, or a negative number if no keys map to the
     * specified value.
     * Beware that this is a linear search, unlike lookups by key,
     * and that multiple keys can map to the same value and this will
     * find only one of them.
     */
    public int indexOfValue(Object value) {
        final int N = mSize * 2;
        final Object[] array = mArray;
        if (value == null) {
            for (int i = 1; i < N; i += 2) {
                if (array[i] == null) {
                    return i >> 1;
                }
            }
        } else {
            for (int i = 1; i < N; i += 2) {
                if (value.equals(array[i])) {
                    return i >> 1;
                }
            }
        }
        return -1;
    }

    /**
     * Check whether a value exists in the array.  This requires a linear search
     * through the entire array.
     *
     * @param value The value to search for.
     * @return Returns true if the value exists, else false.
     */
    @Override
    public boolean containsValue(Object value) {
        return indexOfValue(value) >= 0;
    }

    /**
     * Retrieve a value from the array.
     *
     * @param key The key of the value to retrieve.
     * @return Returns the value associated with the given key,
     * or null if there is no such key.
     */
    @Override
    public V get(Object key) {
        final int index = indexOfKey(key);
        return index >= 0 ? (V) mArray[(index << 1) + 1] : null;
    }

    /**
     * Return the key at the given index in the array.
     *
     * <p>For indices outside of the range <code>0...size()-1</code>, an
     * {@link ArrayIndexOutOfBoundsException} is thrown.</p>
     *
     * @param index The desired index, must be between 0 and {@link #size()}-1.
     * @return Returns the key stored at the given index.
     */
    public K keyAt(int index) {
        if (index >= mSize) {
            // The array might be slightly bigger than mSize, in which case, indexing won't fail.
            // Check if exception should be thrown outside of the critical path.
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return (K) mArray[index << 1];
    }

    /**
     * Return the value at the given index in the array.
     *
     * <p>For indices outside of the range <code>0...size()-1</code>, an
     * {@link ArrayIndexOutOfBoundsException} is thrown.</p>
     *
     * @param index The desired index, must be between 0 and {@link #size()}-1.
     * @return Returns the value stored at the given index.
     */
    public V valueAt(int index) {
        if (index >= mSize) {
            // The array might be slightly bigger than mSize, in which case, indexing won't fail.
            // Check if exception should be thrown outside of the critical path.
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return (V) mArray[(index << 1) + 1];
    }

    /**
     * Set the value at a given index in the array.
     *
     * <p>For indices outside of the range <code>0...size()-1</code>, an
     * {@link ArrayIndexOutOfBoundsException} is thrown.</p>
     *
     * @param index The desired index, must be between 0 and {@link #size()}-1.
     * @param value The new value to store at this index.
     * @return Returns the previous value at the given index.
     */
    public V setValueAt(int index, V value) {
        if (index >= mSize) {
            // The array might be slightly bigger than mSize, in which case, indexing won't fail.
            // Check if exception should be thrown outside of the critical path.
            throw new ArrayIndexOutOfBoundsException(index);
        }
        index = (index << 1) + 1;
        V old = (V) mArray[index];
        mArray[index] = value;
        return old;
    }

    /**
     * Return true if the array map contains no items.
     */
    @Override
    public boolean isEmpty() {
        return mSize == 0;
    }

    /**
     * Add a new value to the array map.
     *
     * @param key   The key under which to store the value.  If
     *              this key already exists in the array, its value will be replaced.
     * @param value The value to store for the given key.
     * @return Returns the old value that was stored for the given key, or null if there
     * was no such key.
     */
    @Nullable
    @Override
    public V put(K key, V value) {
        final int oldSize = mSize;
        final int hash;
        int index;
        if (key == null) {
            hash = 0;
            index = indexOfNull();
        } else {
            hash = mIdentityHashCode ? System.identityHashCode(key) : key.hashCode();
            index = indexOf(key, hash);
        }
        if (index >= 0) {
            index = (index << 1) + 1;
            final V old = (V) mArray[index];
            mArray[index] = value;
            return old;
        }

        index = ~index;
        if (oldSize >= mHashes.length) {
            final int n = oldSize >= (BASE_SIZE * 2) ? (oldSize + (oldSize >> 1))
                    : (oldSize >= BASE_SIZE ? (BASE_SIZE * 2) : BASE_SIZE);

            final int[] hashes = mHashes;
            final Object[] array = mArray;
            allocArrays(n);

            if (oldSize != mSize) {
                throw new ConcurrentModificationException();
            }

            if (mHashes.length > 0) {
                System.arraycopy(hashes, 0, mHashes, 0, hashes.length);
                System.arraycopy(array, 0, mArray, 0, array.length);
            }

            freeArrays(hashes, array, oldSize);
        }

        if (index < oldSize) {
            System.arraycopy(mHashes, index, mHashes, index + 1, oldSize - index);
            System.arraycopy(mArray, index << 1, mArray, (index + 1) << 1, (mSize - index) << 1);
        }

        if (oldSize != mSize || index >= mHashes.length) {
            throw new ConcurrentModificationException();
        }
        mHashes[index] = hash;
        mArray[index << 1] = key;
        mArray[(index << 1) + 1] = value;
        mSize++;
        return null;
    }

    /**
     * Special fast path for appending items to the end of the array without validation.
     * The array must already be large enough to contain the item.
     */
    @ApiStatus.Internal
    public void append(K key, V value) {
        int index = mSize;
        final int hash = key == null ? 0
                : (mIdentityHashCode ? System.identityHashCode(key) : key.hashCode());
        if (index >= mHashes.length) {
            throw new IllegalStateException("Array is full");
        }
        if (index > 0 && mHashes[index - 1] > hash) {
            ModernUI.LOGGER.warn(MARKER, "New hash " + hash
                            + " is before end of array hash " + mHashes[index - 1]
                            + " at index " + index + " key " + key,
                    new RuntimeException("here").fillInStackTrace());
            put(key, value);
            return;
        }
        mSize = index + 1;
        mHashes[index] = hash;
        index <<= 1;
        mArray[index] = key;
        mArray[index + 1] = value;
    }

    /**
     * The use of the {@link #append(Object, Object)} function can result in invalid array maps, in particular
     * an array map where the same key appears multiple times.  This function verifies that
     * the array map is valid, throwing IllegalArgumentException if a problem is found.  The
     * main use for this method is validating an array map after unpacking from an IPC, to
     * protect against malicious callers.
     */
    @ApiStatus.Internal
    public void validate() {
        final int size = mSize;
        if (size <= 1) {
            // There can't be duplicated.
            return;
        }
        int baseHash = mHashes[0];
        int baseIndex = 0;
        for (int i = 1; i < size; i++) {
            int hash = mHashes[i];
            if (hash != baseHash) {
                baseHash = hash;
                baseIndex = i;
                continue;
            }
            // We are in a run of entries with the same hash code.  Go backwards through
            // the array to see if any keys are the same.
            final Object cur = mArray[i << 1];
            for (int j = i - 1; j >= baseIndex; j--) {
                final Object prev = mArray[j << 1];
                if (cur == prev) {
                    throw new IllegalArgumentException("Duplicate key in ArrayMap: " + cur);
                }
                if (cur != null && cur.equals(prev)) {
                    throw new IllegalArgumentException("Duplicate key in ArrayMap: " + cur);
                }
            }
        }
    }

    /**
     * Performs the given action for all elements in the stored order. This implementation overrides
     * the default implementation to avoid iterating using the {@link #entrySet()} and iterates in
     * the key-value order consistent with {@link #keyAt(int)} and {@link #valueAt(int)}.
     *
     * @param action The action to be performed for each element
     */
    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        final int size = mSize;
        for (int i = 0; i < size; ++i) {
            if (size != mSize) {
                throw new ConcurrentModificationException();
            }
            action.accept(keyAt(i), valueAt(i));
        }
    }

    /**
     * Perform a {@link #put(Object, Object)} of all key/value pairs in <var>map</var>
     *
     * @param map The map whose contents are to be retrieved.
     */
    @Override
    public void putAll(@Nonnull Map<? extends K, ? extends V> map) {
        if (map instanceof ArrayMap<? extends K, ? extends V> array) {
            final int size = array.mSize;
            ensureCapacity(mSize + size);
            if (mSize == 0) {
                if (size > 0) {
                    System.arraycopy(array.mHashes, 0, mHashes, 0, size);
                    System.arraycopy(array.mArray, 0, mArray, 0, size << 1);
                    mSize = size;
                }
            } else {
                for (int i = 0; i < size; i++) {
                    put(array.keyAt(i), array.valueAt(i));
                }
            }
        } else {
            ensureCapacity(mSize + map.size());
            for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Remove an existing key from the array map.
     *
     * @param key The key of the mapping to remove.
     * @return Returns the value that was stored under the key, or null if there
     * was no such key.
     */
    @Override
    public V remove(Object key) {
        final int index = indexOfKey(key);
        if (index >= 0) {
            return removeAt(index);
        }

        return null;
    }

    /**
     * Remove the key/value mapping at the given index.
     *
     * <p>For indices outside of the range <code>0...size()-1</code> an
     * {@link ArrayIndexOutOfBoundsException} is thrown.</p>
     *
     * @param index The desired index, must be between 0 and {@link #size()}-1.
     * @return Returns the value that was stored at this index.
     */
    public V removeAt(int index) {
        if (index >= mSize) {
            // The array might be slightly bigger than mSize, in which case, indexing won't fail.
            // Check if exception should be thrown outside of the critical path.
            throw new ArrayIndexOutOfBoundsException(index);
        }

        final Object old = mArray[(index << 1) + 1];
        final int oldSize = mSize;
        final int newSize;
        if (oldSize <= 1) {
            // Now empty.
            final int[] hashes = mHashes;
            final Object[] array = mArray;
            mHashes = IntArrays.EMPTY_ARRAY;
            mArray = ObjectArrays.EMPTY_ARRAY;
            freeArrays(hashes, array, oldSize);
            newSize = 0;
        } else {
            newSize = oldSize - 1;
            if (mHashes.length > (BASE_SIZE * 2) && mSize < mHashes.length / 3) {
                // Shrunk enough to reduce size of arrays.  We don't allow it to
                // shrink smaller than (BASE_SIZE*2) to avoid flapping between
                // that and BASE_SIZE.
                final int n = oldSize > (BASE_SIZE * 2) ? (oldSize + (oldSize >> 1)) : (BASE_SIZE * 2);

                final int[] hashes = mHashes;
                final Object[] array = mArray;
                allocArrays(n);

                if (oldSize != mSize) {
                    throw new ConcurrentModificationException();
                }

                if (index > 0) {
                    System.arraycopy(hashes, 0, mHashes, 0, index);
                    System.arraycopy(array, 0, mArray, 0, index << 1);
                }
                if (index < newSize) {
                    System.arraycopy(hashes, index + 1, mHashes, index, newSize - index);
                    System.arraycopy(array, (index + 1) << 1, mArray, index << 1,
                            (newSize - index) << 1);
                }
            } else {
                if (index < newSize) {
                    System.arraycopy(mHashes, index + 1, mHashes, index, newSize - index);
                    System.arraycopy(mArray, (index + 1) << 1, mArray, index << 1,
                            (newSize - index) << 1);
                }
                mArray[newSize << 1] = null;
                mArray[(newSize << 1) + 1] = null;
            }
        }
        if (oldSize != mSize) {
            throw new ConcurrentModificationException();
        }
        mSize = newSize;
        return (V) old;
    }

    /**
     * Return the number of items in this array map.
     */
    @Override
    public int size() {
        return mSize;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns false if the object is not a map, or
     * if the maps have different sizes. Otherwise, for each key in this map,
     * values of both maps are compared. If the values for any key are not
     * equal, the method returns false, otherwise it returns true.
     */
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Map<?, ?> map) {
            if (size() != map.size()) {
                return false;
            }

            try {
                for (int i = 0; i < mSize; i++) {
                    K key = keyAt(i);
                    V mine = valueAt(i);
                    Object theirs = map.get(key);
                    if (mine == null) {
                        if (theirs != null || !map.containsKey(key)) {
                            return false;
                        }
                    } else if (!mine.equals(theirs)) {
                        return false;
                    }
                }
            } catch (NullPointerException | ClassCastException ignored) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int[] hashes = mHashes;
        final Object[] array = mArray;
        int result = 0;
        for (int i = 0, v = 1, s = mSize; i < s; i++, v += 2) {
            Object value = array[v];
            result += hashes[i] ^ (value == null ? 0 : value.hashCode());
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation composes a string by iterating over its mappings. If
     * this map contains itself as a key or a value, the string "(this Map)"
     * will appear in its place.
     */
    @Nonnull
    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }

        StringBuilder buffer = new StringBuilder(mSize * 28);
        buffer.append('{');
        for (int i = 0; i < mSize; i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            Object key = keyAt(i);
            if (key != this) {
                buffer.append(key);
            } else {
                buffer.append("(this Map)");
            }
            buffer.append('=');
            Object value = valueAt(i);
            if (value != this) {
                buffer.append(deepToString(value));
            } else {
                buffer.append("(this Map)");
            }
        }
        buffer.append('}');
        return buffer.toString();
    }

    private static String deepToString(Object value) {
        if (value != null && value.getClass().isArray()) {
            if (value.getClass() == boolean[].class) {
                return Arrays.toString((boolean[]) value);
            } else if (value.getClass() == byte[].class) {
                return Arrays.toString((byte[]) value);
            } else if (value.getClass() == char[].class) {
                return Arrays.toString((char[]) value);
            } else if (value.getClass() == double[].class) {
                return Arrays.toString((double[]) value);
            } else if (value.getClass() == float[].class) {
                return Arrays.toString((float[]) value);
            } else if (value.getClass() == int[].class) {
                return Arrays.toString((int[]) value);
            } else if (value.getClass() == long[].class) {
                return Arrays.toString((long[]) value);
            } else if (value.getClass() == short[].class) {
                return Arrays.toString((short[]) value);
            } else {
                return Arrays.deepToString((Object[]) value);
            }
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * Determine if the array map contains all the keys in the given collection.
     *
     * @param collection The collection whose contents are to be checked against.
     * @return Returns true if this array map contains a key for every entry
     * in <var>collection</var>, else returns false.
     */
    public boolean containsAll(@Nonnull Collection<?> collection) {
        for (Object o : collection) {
            if (!containsKey(o)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Remove all keys in the array map that exist in the given collection.
     *
     * @param collection The collection whose contents are to be used to remove keys.
     * @return Returns true if any keys were removed from the array map, else false.
     */
    public boolean removeAll(@Nonnull Collection<?> collection) {
        int oldSize = mSize;
        for (Object o : collection) {
            remove(o);
        }
        return oldSize != mSize;
    }

    /**
     * Remove all keys in the array map that do <b>not</b> exist in the given collection.
     *
     * @param collection The collection whose contents are to be used to determine which
     *                   keys to keep.
     * @return Returns true if any keys were removed from the array map, else false.
     */
    public boolean retainAll(@Nonnull Collection<?> collection) {
        int oldSize = mSize;
        Iterator<K> it = new ArrayIterator<>(0);
        while (it.hasNext()) {
            if (!collection.contains(it.next())) {
                it.remove();
            }
        }
        return oldSize != mSize;
    }

    /**
     * Return a {@link Set} for iterating over and interacting with all mappings
     * in the array map.
     *
     * <p><b>Note:</b> this is a very inefficient way to access the array contents, it
     * requires generating a number of temporary objects and allocates additional state
     * information associated with the container that will remain for the life of the container.</p>
     *
     * <p><b>Note:</b></p> the semantics of this
     * Set are subtly different from that of a {@link HashMap}: most important,
     * the {@link Map.Entry Map.Entry} object returned by its iterator is a single
     * object that exists for the entire iterator, so you can <b>not</b> hold on to it
     * after calling {@link Iterator#next() Iterator.next}.</p>
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        if (mEntrySet == null) {
            mEntrySet = new EntrySet();
        }
        return mEntrySet;
    }

    final class EntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        public boolean add(Entry<K, V> kvEntry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(@Nonnull Collection<? extends Map.Entry<K, V>> collection) {
            int oldSize = mSize;
            for (Map.Entry<K, V> entry : collection) {
                put(entry.getKey(), entry.getValue());
            }
            return oldSize != mSize;
        }

        @Override
        public void clear() {
            ArrayMap.this.clear();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry<?, ?> e))
                return false;
            int index = indexOfKey(e.getKey());
            if (index < 0) {
                return false;
            }
            return Objects.equals(mArray[(index << 1) + 1], e.getValue());
        }

        @Override
        public boolean isEmpty() {
            return mSize == 0;
        }

        @Nonnull
        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new MapIterator();
        }

        @Override
        public boolean remove(Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(@Nonnull Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(@Nonnull Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return mSize;
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(@Nonnull T[] array) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o == this)
                return true;
            if (!(o instanceof Set))
                return false;
            Collection<?> c = (Collection<?>) o;
            if (c.size() != size())
                return false;
            try {
                return containsAll(c);
            } catch (ClassCastException | NullPointerException ignored) {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int result = 0;
            for (int i = mSize - 1; i >= 0; i--) {
                final Object key = mArray[(i << 1)];
                final Object value = mArray[(i << 1) + 1];
                result += ((key == null ? 0 : key.hashCode()) ^
                        (value == null ? 0 : value.hashCode()));
            }
            return result;
        }
    }

    /**
     * Return a {@link Set} for iterating over and interacting with all keys
     * in the array map.
     *
     * <p><b>Note:</b> this is a fairly inefficient way to access the array contents, it
     * requires generating a number of temporary objects and allocates additional state
     * information associated with the container that will remain for the life of the container.</p>
     */
    @Override
    public Set<K> keySet() {
        if (mKeySet == null) {
            mKeySet = new KeySet();
        }
        return mKeySet;
    }

    final class KeySet extends AbstractSet<K> {

        @Override
        public boolean add(K object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(@Nonnull Collection<? extends K> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            ArrayMap.this.clear();
        }

        @Override
        public boolean contains(Object object) {
            return indexOfKey(object) >= 0;
        }

        @Override
        public boolean containsAll(@Nonnull Collection<?> collection) {
            return ArrayMap.this.containsAll(collection);
        }

        @Override
        public boolean isEmpty() {
            return mSize == 0;
        }

        @Nonnull
        @Override
        public Iterator<K> iterator() {
            return new ArrayIterator<>(0);
        }

        @Override
        public boolean remove(Object object) {
            int index = indexOfKey(object);
            if (index >= 0) {
                removeAt(index);
                return true;
            }
            return false;
        }

        @Override
        public boolean removeAll(@Nonnull Collection<?> collection) {
            return ArrayMap.this.removeAll(collection);
        }

        @Override
        public boolean retainAll(@Nonnull Collection<?> collection) {
            return ArrayMap.this.retainAll(collection);
        }

        @Override
        public int size() {
            return mSize;
        }

        @Nonnull
        @Override
        public Object[] toArray() {
            final int N = mSize;
            Object[] result = new Object[N];
            for (int i = 0; i < N; i++) {
                result[i] = mArray[(i << 1)];
            }
            return result;
        }

        @Nonnull
        @Override
        public <T> T[] toArray(@Nonnull T[] array) {
            final int N = mSize;
            if (array.length < N) {
                array = (T[]) Array.newInstance(array.getClass().getComponentType(), N);
            }
            for (int i = 0; i < N; i++) {
                array[i] = (T) mArray[(i << 1)];
            }
            if (array.length > N) {
                array[N] = null;
            }
            return array;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o == this)
                return true;
            if (!(o instanceof Set))
                return false;
            Collection<?> c = (Collection<?>) o;
            if (c.size() != size())
                return false;
            try {
                return containsAll(c);
            } catch (ClassCastException | NullPointerException ignored) {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int result = 0;
            for (int i = mSize - 1; i >= 0; i--) {
                Object obj = mArray[(i << 1)];
                result += obj == null ? 0 : obj.hashCode();
            }
            return result;
        }
    }

    /**
     * Return a {@link Collection} for iterating over and interacting with all values
     * in the array map.
     *
     * <p><b>Note:</b> this is a fairly inefficient way to access the array contents, it
     * requires generating a number of temporary objects and allocates additional state
     * information associated with the container that will remain for the life of the container.</p>
     */
    @Override
    public Collection<V> values() {
        if (mValues == null) {
            mValues = new ValuesCollection();
        }
        return mValues;
    }

    final class ValuesCollection extends AbstractCollection<V> {

        @Override
        public boolean add(V object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(@Nonnull Collection<? extends V> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            ArrayMap.this.clear();
        }

        @Override
        public boolean contains(Object object) {
            return indexOfValue(object) >= 0;
        }

        @Override
        public boolean containsAll(@Nonnull Collection<?> collection) {
            for (Object o : collection) {
                if (!contains(o)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean isEmpty() {
            return mSize == 0;
        }

        @Nonnull
        @Override
        public Iterator<V> iterator() {
            return new ArrayIterator<>(1);
        }

        @Override
        public boolean remove(Object object) {
            int index = indexOfValue(object);
            if (index >= 0) {
                removeAt(index);
                return true;
            }
            return false;
        }

        @Override
        public boolean removeAll(@Nonnull Collection<?> collection) {
            int size = mSize;
            boolean changed = false;
            for (int i = 0; i < size; i++) {
                Object cur = mArray[(i << 1) + 1];
                if (collection.contains(cur)) {
                    removeAt(i);
                    i--;
                    size--;
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public boolean retainAll(@Nonnull Collection<?> collection) {
            int size = mSize;
            boolean changed = false;
            for (int i = 0; i < size; i++) {
                Object cur = mArray[(i << 1) + 1];
                if (!collection.contains(cur)) {
                    removeAt(i);
                    i--;
                    size--;
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public int size() {
            return mSize;
        }

        @Nonnull
        @Override
        public Object[] toArray() {
            final int size = mSize;
            Object[] result = new Object[size];
            for (int i = 0; i < size; i++) {
                result[i] = mArray[(i << 1) + 1];
            }
            return result;
        }

        @Nonnull
        @Override
        public <T> T[] toArray(@Nonnull T[] array) {
            final int size = mSize;
            if (array.length < size) {
                array = (T[]) Array.newInstance(array.getClass().getComponentType(), size);
            }
            for (int i = 0; i < size; i++) {
                array[i] = (T) mArray[(i << 1) + 1];
            }
            if (array.length > size) {
                array[size] = null;
            }
            return array;
        }
    }

    final class ArrayIterator<T> implements Iterator<T> {

        final int mOffset;
        int mIndex;
        boolean mCanRemove = false;

        ArrayIterator(int offset) {
            mOffset = offset;
        }

        @Override
        public boolean hasNext() {
            return mIndex < mSize;
        }

        @Override
        public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            Object res = mArray[(mIndex << 1) + mOffset];
            mIndex++;
            mCanRemove = true;
            return (T) res;
        }

        @Override
        public void remove() {
            if (!mCanRemove) {
                throw new IllegalStateException();
            }
            mIndex--;
            mSize--;
            mCanRemove = false;
            removeAt(mIndex);
        }
    }

    final class MapIterator implements Iterator<Map.Entry<K, V>>, Map.Entry<K, V> {

        int mEnd;
        int mIndex;
        boolean mEntryValid = false;

        MapIterator() {
            mEnd = mSize - 1;
            mIndex = -1;
        }

        @Override
        public boolean hasNext() {
            return mIndex < mEnd;
        }

        @Override
        public Map.Entry<K, V> next() {
            if (!hasNext()) throw new NoSuchElementException();
            mIndex++;
            mEntryValid = true;
            return this;
        }

        @Override
        public void remove() {
            if (!mEntryValid) {
                throw new IllegalStateException();
            }
            removeAt(mIndex);
            mIndex--;
            mEnd--;
            mEntryValid = false;
        }

        @Override
        public K getKey() {
            if (!mEntryValid) {
                throw new IllegalStateException(
                        "This container does not support retaining Map.Entry objects");
            }
            return (K) mArray[(mIndex << 1)];
        }

        @Override
        public V getValue() {
            if (!mEntryValid) {
                throw new IllegalStateException(
                        "This container does not support retaining Map.Entry objects");
            }
            return (V) mArray[(mIndex << 1) + 1];
        }

        @Override
        public V setValue(V object) {
            if (!mEntryValid) {
                throw new IllegalStateException(
                        "This container does not support retaining Map.Entry objects");
            }
            return setValueAt(mIndex, object);
        }

        @Override
        public boolean equals(Object o) {
            if (!mEntryValid) {
                throw new IllegalStateException(
                        "This container does not support retaining Map.Entry objects");
            }
            if (!(o instanceof Entry<?, ?> e)) {
                return false;
            }
            return Objects.equals(e.getKey(), mArray[(mIndex << 1)])
                    && Objects.equals(e.getValue(), mArray[(mIndex << 1) + 1]);
        }

        @Override
        public int hashCode() {
            if (!mEntryValid) {
                throw new IllegalStateException(
                        "This container does not support retaining Map.Entry objects");
            }
            final Object key = mArray[(mIndex << 1)];
            final Object value = mArray[(mIndex << 1) + 1];
            return (key == null ? 0 : key.hashCode()) ^
                    (value == null ? 0 : value.hashCode());
        }

        @Nonnull
        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    int indexOf(@Nonnull Object key, int hash) {
        final int N = mSize;

        // Important fast case: if nothing is in here, nothing to look for.
        if (N == 0) {
            return ~0;
        }

        int index;
        try {
            index = Arrays.binarySearch(mHashes, 0, N, hash);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ConcurrentModificationException();
        }

        // If the hash code wasn't found, then we have no entry for this key.
        if (index < 0) {
            return index;
        }

        // If the key at the returned index matches, that's what we want.
        if (key.equals(mArray[index << 1])) {
            return index;
        }

        // Search for a matching key after the index.
        int end;
        for (end = index + 1; end < N && mHashes[end] == hash; end++) {
            if (key.equals(mArray[end << 1])) return end;
        }

        // Search for a matching key before the index.
        for (int i = index - 1; i >= 0 && mHashes[i] == hash; i--) {
            if (key.equals(mArray[i << 1])) return i;
        }

        // Key not found -- return negative value indicating where a
        // new entry for this key should go.  We use the end of the
        // hash chain to reduce the number of array entries that will
        // need to be copied when inserting.
        return ~end;
    }

    int indexOfNull() {
        final int N = mSize;

        // Important fast case: if nothing is in here, nothing to look for.
        if (N == 0) {
            return ~0;
        }

        int index;
        try {
            index = Arrays.binarySearch(mHashes, 0, N, 0);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ConcurrentModificationException();
        }

        // If the hash code wasn't found, then we have no entry for this key.
        if (index < 0) {
            return index;
        }

        // If the key at the returned index matches, that's what we want.
        if (null == mArray[index << 1]) {
            return index;
        }

        // Search for a matching key after the index.
        int end;
        for (end = index + 1; end < N && mHashes[end] == 0; end++) {
            if (null == mArray[end << 1]) return end;
        }

        // Search for a matching key before the index.
        for (int i = index - 1; i >= 0 && mHashes[i] == 0; i--) {
            if (null == mArray[i << 1]) return i;
        }

        // Key not found -- return negative value indicating where a
        // new entry for this key should go.  We use the end of the
        // hash chain to reduce the number of array entries that will
        // need to be copied when inserting.
        return ~end;
    }

    private void allocArrays(final int size) {
        if (size == (BASE_SIZE * 2)) {
            synchronized (sTwiceBaseCacheLock) {
                if (mTwiceBaseCache != null) {
                    final Object[] array = mTwiceBaseCache;
                    mArray = array;
                    try {
                        mTwiceBaseCache = (Object[]) array[0];
                        mHashes = (int[]) array[1];
                        if (mHashes != null) {
                            array[0] = array[1] = null;
                            mTwiceBaseCacheSize--;
                            return;
                        }
                    } catch (ClassCastException ignored) {
                    }
                    // Whoops!  Someone trampled the array (probably due to not protecting
                    // their access with a lock).  Our cache is corrupt; report and give up.
                    ModernUI.LOGGER.fatal(MARKER, "Found corrupt ArrayMap cache: [0]=" + array[0]
                            + " [1]=" + array[1]);
                    mTwiceBaseCache = null;
                    mTwiceBaseCacheSize = 0;
                }
            }
        } else if (size == BASE_SIZE) {
            synchronized (sBaseCacheLock) {
                if (mBaseCache != null) {
                    final Object[] array = mBaseCache;
                    mArray = array;
                    try {
                        mBaseCache = (Object[]) array[0];
                        mHashes = (int[]) array[1];
                        if (mHashes != null) {
                            array[0] = array[1] = null;
                            mBaseCacheSize--;
                            return;
                        }
                    } catch (ClassCastException ignored) {
                    }
                    // Whoops!  Someone trampled the array (probably due to not protecting
                    // their access with a lock).  Our cache is corrupt; report and give up.
                    ModernUI.LOGGER.fatal(MARKER, "Found corrupt ArrayMap cache: [0]=" + array[0]
                            + " [1]=" + array[1]);
                    mBaseCache = null;
                    mBaseCacheSize = 0;
                }
            }
        }

        mHashes = new int[size];
        mArray = new Object[size << 1];
    }

    /**
     * Make sure <b>NOT</b> to call this method with arrays that can still be modified. In other
     * words, don't pass mHashes or mArray in directly.
     */
    private static void freeArrays(@Nonnull final int[] hashes, @Nonnull final Object[] array, final int size) {
        if (hashes.length == (BASE_SIZE * 2)) {
            synchronized (sTwiceBaseCacheLock) {
                if (mTwiceBaseCacheSize < CACHE_SIZE) {
                    array[0] = mTwiceBaseCache;
                    array[1] = hashes;
                    for (int i = (size << 1) - 1; i >= 2; i--) {
                        array[i] = null;
                    }
                    mTwiceBaseCache = array;
                    mTwiceBaseCacheSize++;
                }
            }
        } else if (hashes.length == BASE_SIZE) {
            synchronized (sBaseCacheLock) {
                if (mBaseCacheSize < CACHE_SIZE) {
                    array[0] = mBaseCache;
                    array[1] = hashes;
                    for (int i = (size << 1) - 1; i >= 2; i--) {
                        array[i] = null;
                    }
                    mBaseCache = array;
                    mBaseCacheSize++;
                }
            }
        }
    }
}
