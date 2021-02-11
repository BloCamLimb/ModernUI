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

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the result of text shaping and layout of the single paragraph.
 */
public class MeasuredText {

    @Nonnull
    private final char[] mTextBuf;
    @Nonnull
    protected final List<Run> mRuns;
    protected float[] mAdvances;

    private MeasuredText(@Nonnull char[] textBuf, @Nonnull List<Run> runs) {
        mTextBuf = textBuf;
        mRuns = runs;
    }

    // the text buf
    @Nonnull
    public char[] getTextBuf() {
        return mTextBuf;
    }

    public static class Builder {

        private final List<Run> mRuns = new ArrayList<>();

        @Nonnull
        private final char[] mText;
        private int mCurrentOffset = 0;

        public Builder(@Nonnull char[] text) {
            mText = text;
        }

        public Builder appendStyleRun(@Nonnull TextPaint paint, int length, boolean isRtl) {
            Preconditions.checkArgument(length > 0, "length can not be negative");
            final int end = mCurrentOffset + length;
            Preconditions.checkArgument(end <= mText.length, "Style exceeds the text length");
            mRuns.add(new StyleRun(mCurrentOffset, end, paint, isRtl));
            mCurrentOffset = end;
            return this;
        }

        /**
         * Starts laying-out the text and creates a MeasuredText for the result.
         * <p>
         * Once you called this method, you can't touch this Builder again.
         *
         * @return text measurement result
         */
        public MeasuredText build() {
            Preconditions.checkState(mCurrentOffset >= 0, "Builder can not be reused.");
            Preconditions.checkState(mCurrentOffset == mText.length, "Style info has not been provided for all text.");
            mCurrentOffset = -1;
            return new MeasuredText(mText, mRuns);
        }
    }

    // logical run, child of bidi run
    public static abstract class Run {

        // piece range in context
        public final int mStart;
        public final int mEnd;

        public Run(int start, int end) {
            mStart = start;
            mEnd = end;
        }

        public abstract boolean canBreak();
    }

    public static class StyleRun extends Run {

        //TODO a copy of paint
        public final TextPaint mPaint;
        public final boolean mIsRtl;

        public StyleRun(int start, int end, TextPaint paint, boolean isRtl) {
            super(start, end);
            mPaint = paint;
            mIsRtl = isRtl;
        }

        @Override
        public boolean canBreak() {
            return true;
        }
    }
}
