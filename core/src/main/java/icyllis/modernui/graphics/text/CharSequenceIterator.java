/*
 * Modern UI.
 * Copyright (C) 2021-2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.text;

import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import java.text.CharacterIterator;
import java.util.Objects;

/**
 * An implementation of {@link CharacterIterator} that iterates over a given CharSequence.
 *
 * @hidden
 */
@ApiStatus.Internal
public class CharSequenceIterator implements CharacterIterator {

    private final CharSequence text;
    private final int start;
    private final int end;
    // invariant: start <= pos <= end
    private int pos;

    public CharSequenceIterator(@Nonnull CharSequence text) {
        this(text, 0, text.length());
    }

    public CharSequenceIterator(@Nonnull CharSequence text, int start, int end) {
        pos = Objects.checkFromToIndex(start, end, text.length());
        this.text = text;
        this.start = start;
        this.end = end;
    }

    @Override
    public char first() {
        pos = start;
        return current();
    }

    @Override
    public char last() {
        pos = end != start ? end - 1 : end;
        return current();
    }

    @Override
    public char current() {
        assert start <= pos && pos <= end;
        return pos < end ? text.charAt(pos) : DONE;
    }

    @Override
    public char next() {
        if (pos + 1 >= end) {
            pos = end;
            return DONE;
        } else {
            pos++;
            return text.charAt(pos);
        }
    }

    @Override
    public char previous() {
        if (pos <= start) {
            return DONE;
        } else {
            pos--;
            return text.charAt(pos);
        }
    }

    @Override
    public char setIndex(int position) {
        if (start <= position && position <= end) {
            pos = position;
            return current();
        } else {
            throw new IllegalArgumentException("invalid position");
        }
    }

    @Override
    public int getBeginIndex() {
        return start;
    }

    @Override
    public int getEndIndex() {
        return end;
    }

    @Override
    public int getIndex() {
        return pos;
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
