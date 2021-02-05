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

import com.ibm.icu.text.Bidi;
import icyllis.modernui.text.style.ReplacementSpan;
import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

public class MeasuredParagraph {

    private static final Pool<MeasuredParagraph> sPool = Pools.concurrent(1);

    // The casted original text.
    //
    // This may be null if the passed text is not a Spanned.
    @Nullable
    private Spanned mSpanned;

    // The start offset of the target range in the original text (mSpanned);
    private int mTextStart;

    // The length of the target range in the original text.
    private int mTextLength;

    // The copied character buffer for measuring text.
    //
    // The length of this array is mTextLength.
    private @Nullable
    char[] mCopiedBuffer;

    // The whole/first paragraph direction.
    private int mParaDir;

    // True if the text is LTR direction and doesn't contain any bidi characters.
    private boolean mLtrWithoutBidi;

    // The bidi level for individual characters.
    //
    // This is empty if mLtrWithoutBidi is true.
    @Nonnull
    private final ByteArrayList mLevels = new ByteArrayList();

    private MeasuredParagraph() {
    }

    /**
     * Resets the internal state for starting new text.
     */
    private void reset() {
        mSpanned = null;
        mCopiedBuffer = null;
        //mWholeWidth = 0;
        mLevels.clear();
        /*mWidths.clear();
        mFontMetrics.clear();
        mSpanEndCache.clear();
        mMeasuredText = null;*/
    }

    @Nonnull
    private static MeasuredParagraph obtain() {
        final MeasuredParagraph c = sPool.acquire();
        return c == null ? new MeasuredParagraph() : c;
    }

    @Nonnull
    public static MeasuredParagraph buildForMeasurement(@Nonnull CharSequence text, int start, int end,
                                                        @Nonnull TextDirectionHeuristic dir,
                                                        @Nullable MeasuredParagraph recycle) {
        final MeasuredParagraph c = recycle == null ? obtain() : recycle;
        c.startBidiAnalysis(text, start, end, dir);
        if (c.mTextLength == 0) {
            return c;
        }
        if (c.mSpanned == null) {

        }
        return c;
    }

    private void startBidiAnalysis(@Nonnull CharSequence text, int start, int end, @Nonnull TextDirectionHeuristic dir) {
        reset();
        mSpanned = text instanceof Spanned ? (Spanned) text : null;
        mTextStart = start;
        mTextLength = end - start;

        if (mCopiedBuffer == null || mCopiedBuffer.length != mTextLength) {
            mCopiedBuffer = new char[mTextLength];
        }
        TextUtils.getChars(text, start, end, mCopiedBuffer, 0);

        // Replace characters associated with ReplacementSpan to U+FFFC.
        if (mSpanned != null) {
            final ReplacementSpan[] spans = mSpanned.getSpans(start, end, ReplacementSpan.class);
            for (ReplacementSpan span : spans) {
                int startInPara = mSpanned.getSpanStart(span) - start;
                int endInPara = mSpanned.getSpanEnd(span) - start;
                // The span interval may be larger and must be restricted to [start, end)
                if (startInPara < 0) startInPara = 0;
                if (endInPara > mTextLength) endInPara = mTextLength;
                Arrays.fill(mCopiedBuffer, startInPara, endInPara, '\ufffc');
            }
        }

        if ((dir == TextDirectionHeuristics.LTR
                || dir == TextDirectionHeuristics.FIRSTSTRONG_LTR
                || dir == TextDirectionHeuristics.ANYRTL_LTR)
                && !Bidi.requiresBidi(mCopiedBuffer, 0, mTextLength)) {
            mLevels.clear();
            mLevels.trim();
            mParaDir = Bidi.DIRECTION_LEFT_TO_RIGHT;
            mLtrWithoutBidi = true;
        } else {
            final byte paraLevel;
            if (dir == TextDirectionHeuristics.LTR) {
                paraLevel = Bidi.LTR;
            } else if (dir == TextDirectionHeuristics.RTL) {
                paraLevel = Bidi.RTL;
            } else if (dir == TextDirectionHeuristics.FIRSTSTRONG_LTR) {
                paraLevel = Bidi.LEVEL_DEFAULT_LTR;
            } else if (dir == TextDirectionHeuristics.FIRSTSTRONG_RTL) {
                paraLevel = Bidi.LEVEL_DEFAULT_RTL;
            } else {
                final boolean isRtl = dir.isRtl(mCopiedBuffer, 0, mTextLength);
                paraLevel = isRtl ? Bidi.RTL : Bidi.LTR;
            }
            mLevels.size(mTextLength);
            mLevels.trim(mTextLength);
            final Bidi icuBidi = new Bidi(mTextLength, 0);
            icuBidi.setPara(mCopiedBuffer, paraLevel, null);
            for (int i = 0; i < mTextLength; i++) {
                mLevels.set(i, icuBidi.getLevelAt(i));
            }
            // an odd numbers indicates RTL
            mParaDir = (icuBidi.getParaLevel() & 0x1) == 0 ? Bidi.DIRECTION_LEFT_TO_RIGHT : Bidi.DIRECTION_RIGHT_TO_LEFT;
            mLtrWithoutBidi = false;
        }
    }
}
