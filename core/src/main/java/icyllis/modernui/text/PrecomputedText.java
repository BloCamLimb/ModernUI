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
import java.util.ArrayList;
import java.util.List;

//TODO
public class PrecomputedText {

    private final SpannableString mText;
    private final FontPaint mPaint;
    private final TextDirectionHeuristic mTextDir;
    private final MeasuredParagraph[] mParagraphs;

    private PrecomputedText(@Nonnull SpannableString text, @Nonnull FontPaint paint,
                            @Nonnull TextDirectionHeuristic textDir, @Nonnull MeasuredParagraph[] paragraphs) {
        mText = text;
        mPaint = paint;
        mTextDir = textDir;
        mParagraphs = paragraphs;
    }

    @Nonnull
    public static PrecomputedText create(@Nonnull CharSequence text, @Nonnull FontPaint paint,
                                         @Nonnull TextDirectionHeuristic textDir) {
        // always create new spannable, in case of original text changed but we don't have watchers
        return new PrecomputedText(new SpannableString(text, true), paint, textDir,
                createMeasuredParagraphs(text, 0, text.length(), paint, textDir, true));
    }

    @Nonnull
    public static MeasuredParagraph[] createMeasuredParagraphs(
            @Nonnull CharSequence text, int start, int end, @Nonnull FontPaint paint,
            @Nonnull TextDirectionHeuristic textDir, boolean computeLayout) {
        List<MeasuredParagraph> list = new ArrayList<>();
        int paraEnd;
        for (int paraStart = start; paraStart < end; paraStart = paraEnd) {
            paraEnd = TextUtils.indexOf(text, '\n', paraStart, end);
            if (paraEnd < 0) {
                // No LINE_FEED(U+000A) character found. Use end of the text as the paragraph
                // end.
                paraEnd = end;
            } else {
                paraEnd++;  // Includes LINE_FEED(U+000A) to the prev paragraph.
            }
            list.add(MeasuredParagraph.buildForStaticLayout(
                    paint, text, paraStart, paraEnd, textDir, computeLayout, null));
        }
        return list.toArray(new MeasuredParagraph[0]);
    }

    public int findParaIndex(int pos) {
        // TODO: Maybe good to remove paragraph concept from PrecomputedText and add substring
        //       layout support to StaticLayout.
        for (int i = mParagraphs.length - 1; i >= 0; --i) {
            if (pos > mParagraphs[i].getTextStart()) {
                return i;
            }
        }
        return -1;
    }

    public FontPaint getPaint() {
        return mPaint;
    }

    public float getWidth(int i, int i1) {
        return 0;
    }
}
