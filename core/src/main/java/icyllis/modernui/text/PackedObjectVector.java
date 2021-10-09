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

package icyllis.modernui.text;

import icyllis.modernui.util.GrowingArrayUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrays;

public class PackedObjectVector<E> {

    private final int mColumns;
    private int mRows;

    private int mRowGapStart;
    private int mRowGapLength;

    private Object[] mValues;

    public PackedObjectVector(int columns) {
        mColumns = columns;
        mValues = ObjectArrays.EMPTY_ARRAY;
        mRows = 0;

        mRowGapStart = 0;
    }

    @SuppressWarnings("unchecked")
    public E getValue(int row, int column) {
        if (row >= mRowGapStart)
            row += mRowGapLength;

        Object value = mValues[row * mColumns + column];

        return (E) value;
    }

    public void setValue(int row, int column, E value) {
        if (row >= mRowGapStart)
            row += mRowGapLength;

        mValues[row * mColumns + column] = value;
    }

    public void insertAt(int row, E[] values) {
        moveRowGapTo(row);

        if (mRowGapLength == 0)
            growBuffer();

        mRowGapStart++;
        mRowGapLength--;

        if (values == null)
            for (int i = 0; i < mColumns; i++)
                setValue(row, i, null);
        else
            for (int i = 0; i < mColumns; i++)
                setValue(row, i, values[i]);
    }

    public void deleteAt(int row, int count) {
        moveRowGapTo(row + count);

        mRowGapStart -= count;
        mRowGapLength += count;

        //TODO
        if (mRowGapLength > size() * 2) {
            // dump();
            // growBuffer();
        }
    }

    public int size() {
        return mRows - mRowGapLength;
    }

    public int width() {
        return mColumns;
    }

    private void growBuffer() {
        Object[] newValues = new Object[GrowingArrayUtils.growSize(size()) * mColumns];
        int newSize = newValues.length / mColumns;
        int after = mRows - (mRowGapStart + mRowGapLength);

        System.arraycopy(mValues, 0, newValues, 0, mColumns * mRowGapStart);
        System.arraycopy(mValues, (mRows - after) * mColumns, newValues, (newSize - after) * mColumns,
                after * mColumns);

        mRowGapLength += newSize - mRows;
        mRows = newSize;
        mValues = newValues;
    }

    private void moveRowGapTo(int where) {
        if (where == mRowGapStart)
            return;

        if (where > mRowGapStart) {
            int moving = where + mRowGapLength - (mRowGapStart + mRowGapLength);

            for (int i = mRowGapStart + mRowGapLength; i < mRowGapStart + mRowGapLength + moving; i++) {
                int dstRow = i - (mRowGapStart + mRowGapLength) + mRowGapStart;

                if (mColumns >= 0)
                    System.arraycopy(mValues, i * mColumns, mValues, dstRow * mColumns, mColumns);
            }
        } else /* where < mRowGapStart */ {
            int moving = mRowGapStart - where;

            for (int i = where + moving - 1; i >= where; i--) {
                int dstRow = i - where + mRowGapStart + mRowGapLength - moving;

                if (mColumns >= 0)
                    System.arraycopy(mValues, i * mColumns, mValues, dstRow * mColumns, mColumns);
            }
        }

        mRowGapStart = where;
    }
}
