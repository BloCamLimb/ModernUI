/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.engine;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Arrays;

/**
 * Used to build a packed array as the storage or lookup key of a hash map.
 * <p>
 * Note: A {@link #flush()} is expected at the end of key building.
 */
public class KeyBuilder {

    /**
     * The hash strategy. Accepts <code>int[]</code> as storage key or <code>KeyBuilder</code> as lookup key.
     */
    public static final Hash.Strategy<Object> HASH_STRATEGY = new Hash.Strategy<>() {
        @Override
        public int hashCode(Object o) {
            return o instanceof int[] key ? Arrays.hashCode(key) : o.hashCode();
        }

        @Override
        public boolean equals(Object a, Object b) {
            // 'b' should be always int[]
            return a instanceof int[] key ? Arrays.equals(key, (int[]) b) : a.equals(b);
        }
    };

    protected final IntArrayList mData;
    private int mCurValue = 0;
    private int mBitsUsed = 0;  // ... in current value

    public KeyBuilder() {
        mData = new IntArrayList();
    }

    public KeyBuilder(IntArrayList data) {
        mData = data;
    }

    /**
     * Resets this key builder to initial state.
     */
    public final void reset() {
        assert (mCurValue == 0 && mBitsUsed == 0);
        mData.clear();
    }

    /**
     * @return the number of ints
     */
    public final int length() {
        assert (mCurValue == 0 && mBitsUsed == 0);
        return mData.size();
    }

    /**
     * @return true if this key builder contains no bits
     */
    public final boolean isEmpty() {
        assert (mCurValue == 0 && mBitsUsed == 0);
        return mData.isEmpty();
    }

    public void addBits(int numBits, int value, String label) {
        assert (numBits > 0 && numBits <= Integer.SIZE);
        assert (numBits == Integer.SIZE || (Integer.SIZE - numBits <= Integer.numberOfLeadingZeros(value)));

        mCurValue |= (value << mBitsUsed);
        mBitsUsed += numBits;

        if (mBitsUsed >= Integer.SIZE) {
            // Overflow, start a new working value
            mData.add(mCurValue);
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

    public void appendComment(String comment) {
    }

    /**
     * Introduces a word-boundary in the key. Must be called before using the key with any cache,
     * but can also be called to create a break between generic data and backend-specific data.
     */
    public final void flush() {
        if (mBitsUsed != 0) {
            mData.add(mCurValue);
            mCurValue = 0;
            mBitsUsed = 0;
        }
    }

    /**
     * Trims the backing store so that the capacity is equal to the size.
     */
    public final void trim() {
        assert (mCurValue == 0 && mBitsUsed == 0);
        mData.trim();
    }

    /**
     * @return a copy of packed int array as storage key
     */
    public final int[] toKey() {
        assert (mCurValue == 0 && mBitsUsed == 0);
        return mData.toIntArray();
    }

    /**
     * Same as {@link Arrays#hashCode(int[])}.
     */
    @Override
    public final int hashCode() {
        assert (mCurValue == 0 && mBitsUsed == 0); // ensure flushed
        int[] e = mData.elements();
        int h = 1, s = mData.size();
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
        return o instanceof int[] key && // check for null
                Arrays.equals(mData.elements(), 0, mData.size(), key, 0, key.length);
    }

    public static class StringKeyBuilder extends KeyBuilder {

        public final StringBuilder mStringBuilder = new StringBuilder();

        public StringKeyBuilder() {
        }

        public StringKeyBuilder(IntArrayList data) {
            super(data);
        }

        @Override
        public void addBits(int numBits, int value, String label) {
            super.addBits(numBits, value, label);
            mStringBuilder.append(label)
                    .append(": ")
                    .append(value & 0xFFFFFFFFL) // to unsigned int
                    .append('\n');
        }

        @Override
        public void appendComment(String comment) {
            mStringBuilder.append(comment)
                    .append('\n');
        }

        @Override
        public String toString() {
            return mStringBuilder.toString();
        }
    }
}
