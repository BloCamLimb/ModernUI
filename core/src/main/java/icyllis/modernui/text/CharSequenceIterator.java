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

import javax.annotation.Nonnull;
import java.text.CharacterIterator;
import java.util.Objects;

/**
 * An implementation of {@link java.text.CharacterIterator} that iterates over a given CharSequence.
 */
public class CharSequenceIterator implements CharacterIterator {

    private final int mBeginIndex, mEndIndex;
    private int mIndex;
    private final CharSequence mCharSeq;

    public CharSequenceIterator(@Nonnull CharSequence text, int start, int end) {
        mIndex = Objects.checkFromToIndex(start, end, text.length());
        mCharSeq = text;
        mBeginIndex = start;
        mEndIndex = end;
    }

    @Override
    public char first() {
        mIndex = mBeginIndex;
        return current();
    }

    @Override
    public char last() {
        if (mBeginIndex == mEndIndex) {
            mIndex = mEndIndex;
            return DONE;
        } else {
            mIndex = mEndIndex - 1;
            return mCharSeq.charAt(mIndex);
        }
    }

    @Override
    public char current() {
        return (mIndex == mEndIndex) ? DONE : mCharSeq.charAt(mIndex);
    }

    @Override
    public char next() {
        mIndex++;
        if (mIndex >= mEndIndex) {
            mIndex = mEndIndex;
            return DONE;
        } else {
            return mCharSeq.charAt(mIndex);
        }
    }

    @Override
    public char previous() {
        if (mIndex <= mBeginIndex) {
            return DONE;
        } else {
            mIndex--;
            return mCharSeq.charAt(mIndex);
        }
    }

    @Override
    public char setIndex(int position) {
        if (mBeginIndex <= position && position <= mEndIndex) {
            mIndex = position;
            return current();
        } else {
            throw new IllegalArgumentException("invalid position");
        }
    }

    @Override
    public int getBeginIndex() {
        return mBeginIndex;
    }

    @Override
    public int getEndIndex() {
        return mEndIndex;
    }

    @Override
    public int getIndex() {
        return mIndex;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
}
