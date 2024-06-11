/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import it.unimi.dsi.fastutil.ints.IntArrays;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * Used to build a packed array as the storage or lookup key of a hash map.
 * <p>
 * Note: A {@link #flush()} is expected at the end of key building.
 */
public non-sealed class KeyBuilder extends Key {

    private int mSize;
    private transient int mCurValue = 0;
    private transient int mBitsUsed = 0;  // ... in current value

    public KeyBuilder() {
    }

    @SuppressWarnings("IncompleteCopyConstructor")
    public KeyBuilder(@Nonnull KeyBuilder other) {
        assert (other.mCurValue == 0 && other.mBitsUsed == 0);
        int size = other.mSize;
        mData = size == 0 ? IntArrays.EMPTY_ARRAY : Arrays.copyOf(other.mData, size);
        mSize = size;
    }

    /**
     * Resets this key builder to initial state.
     */
    public final void clear() {
        assert (mCurValue == 0 && mBitsUsed == 0);
        mSize = 0;
    }

    /**
     * @return the number of ints
     */
    public final int size() {
        assert (mCurValue == 0 && mBitsUsed == 0);
        return mSize;
    }

    /**
     * @return true if this key builder contains no bits
     */
    public final boolean isEmpty() {
        assert (mCurValue == 0 && mBitsUsed == 0);
        return mSize == 0;
    }

    private void grow(int capacity) {
        if (capacity > mData.length) {
            if (mData != IntArrays.DEFAULT_EMPTY_ARRAY) {
                capacity = (int)Math.max(Math.min((long) mData.length + (long)(mData.length >> 1), Integer.MAX_VALUE - 8), capacity);
            } else if (capacity < 10) {
                capacity = 10;
            }

            mData = IntArrays.forceCapacity(mData, capacity, mSize);

        }
    }

    private void add(int k) {
        grow(mSize + 1);
        mData[mSize++] = k;
    }

    public void addBits(int numBits, int value, String label) {
        assert (numBits > 0 && numBits <= Integer.SIZE);
        assert (numBits == Integer.SIZE || (Integer.SIZE - numBits <= Integer.numberOfLeadingZeros(value)));

        mCurValue |= (value << mBitsUsed);
        mBitsUsed += numBits;

        if (mBitsUsed >= Integer.SIZE) {
            // Overflow, start a new working value
            add(mCurValue);
            int excess = mBitsUsed - Integer.SIZE;
            mCurValue = excess != 0 ? (value >>> (numBits - excess)) : 0;
            mBitsUsed = excess;
        }

        assert (Integer.SIZE - mBitsUsed <= Integer.numberOfLeadingZeros(mCurValue));
    }

    public final void addBool(boolean b, String label) {
        addBits(1, b ? 1 : 0, label);
    }

    public final void addInt32(int v, String label) {
        addBits(Integer.SIZE, v, label);
    }

    /**
     * Makes a word-boundary and adds a full word.
     */
    public final void addInt(int v) {
        flush();
        add(v);
    }

    /**
     * Makes a word-boundary and adds an array of words.
     */
    public final void addInts(int[] v, int off, int len) {
        flush();
        grow(mSize + len);
        System.arraycopy(v, off, mData, mSize, len);
        mSize += len;
    }

    /**
     * Introduces a word-boundary in the key. Must be called before using the key with any cache,
     * but can also be called to create a break between generic data and backend-specific data.
     */
    public final void flush() {
        if (mBitsUsed != 0) {
            add(mCurValue);
            mCurValue = 0;
            mBitsUsed = 0;
        }
    }

    /**
     * Trims the backing store so that the capacity is equal to the size.
     */
    public final void trim() {
        assert (mCurValue == 0 && mBitsUsed == 0);
        if (0 < mData.length && mSize != mData.length) {
            int[] t = new int[mSize];
            System.arraycopy(mData, 0, t, 0, mSize);
            mData = t;
        }
    }

    /**
     * @return a copy of packed int array as storage key
     */
    public final Key toStorageKey() {
        assert (mCurValue == 0 && mBitsUsed == 0);
        if (mSize == 0) {
            return Key.EMPTY;
        } else {
            int[] t = new int[mSize];
            System.arraycopy(mData, 0, t, 0, mSize);
            return new Key(t);
        }
    }

    /**
     * Same as {@link Arrays#hashCode(int[])}.
     */
    @Override
    public final int hashCode() {
        assert (mCurValue == 0 && mBitsUsed == 0); // ensure flushed
        int[] e = mData;
        int h = 1, s = size();
        for (int i = 0; i < s; i++)
            h = 31 * h + e[i];
        return h;
    }

    /**
     * Compares with packed int array (storage key).
     */
    @Override
    public final boolean equals(Object o) {
        assert (mCurValue == 0 && mBitsUsed == 0); // ensure flushed
        return o instanceof Key key && // check for null
                Arrays.equals(mData, 0, mSize, key.mData, 0, key.mData.length);
    }
}
