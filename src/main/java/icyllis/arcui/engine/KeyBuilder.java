/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.engine;

import it.unimi.dsi.fastutil.ints.IntList;

public class KeyBuilder implements AutoCloseable {

    private final IntList mData;
    private int mCurValue = 0;
    private int mBitsUsed = 0;  // ... in current value

    public KeyBuilder(IntList data) {
        mData = data;
    }

    @Override
    public void close() {
        flush();
    }

    public void addBits(int numBits, int val, String label) {
        assert numBits > 0 && numBits <= Integer.SIZE;
        assert numBits == Integer.SIZE || (Integer.SIZE - Integer.numberOfLeadingZeros(val) <= numBits);

        mCurValue |= (val << mBitsUsed);
        mBitsUsed += numBits;

        if (mBitsUsed >= Integer.SIZE) {
            // Overflow, start a new working value
            mData.add(mCurValue);
            int excess = mBitsUsed - Integer.SIZE;
            mCurValue = excess != 0 ? (val >>> (numBits - excess)) : 0;
            mBitsUsed = excess;
        }

        assert (Integer.SIZE - Integer.numberOfLeadingZeros(mCurValue) <= mBitsUsed);
    }

    public final void addBool(boolean b, String label) {
        addBits(1, b ? 1 : 0, label);
    }

    public final void add32(int v) {
        addBits(Integer.SIZE, v, "unknown");
    }

    public final void add32(int v, String label) {
        addBits(Integer.SIZE, v, label);
    }

    public void appendComment(String comment) {
    }

    // Introduces a word-boundary in the key. Must be called before using the key with any cache,
    // but can also be called to create a break between generic data and backend-specific data.
    public final void flush() {
        if (mBitsUsed != 0) {
            mData.add(mCurValue);
            mCurValue = 0;
            mBitsUsed = 0;
        }
    }

    // for debug purposes
    public static class StringKeyBuilder extends KeyBuilder {

        private final StringBuilder mStringBuilder = new StringBuilder();

        public StringKeyBuilder(IntList data) {
            super(data);
        }

        @Override
        public void addBits(int numBits, int val, String label) {
            super.addBits(numBits, val, label);
            mStringBuilder.append(label)
                    .append(": ")
                    .append(val & 0xFFFFFFFFL) // to unsigned int
                    .append('\n');
        }

        @Override
        public void appendComment(String comment) {
            mStringBuilder.append(comment)
                    .append('\n');
        }

        public StringBuilder getStringBuilder() {
            return mStringBuilder;
        }

        @Override
        public String toString() {
            return mStringBuilder.toString();
        }
    }
}
