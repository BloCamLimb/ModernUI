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

public class PrecomputedText {

    @Nonnull
    private final SpannableString mText;

    @Nonnull
    private final FontPaint mPaint;

    @Nonnull
    private final TextDirectionHeuristic mDir;

    @Nonnull
    private final MeasuredParagraph[] mParagraphs;

    private PrecomputedText(@Nonnull SpannableString text, @Nonnull FontPaint paint,
                            @Nonnull TextDirectionHeuristic dir, @Nonnull MeasuredParagraph[] paragraphs) {
        mText = text;
        mPaint = paint;
        mDir = dir;
        mParagraphs = paragraphs;
    }

    @Nonnull
    public static PrecomputedText create(@Nonnull FontPaint paint, @Nonnull CharSequence text,
                                         @Nonnull TextDirectionHeuristic dir) {
        return new PrecomputedText(new SpannableString(text, true), paint, dir,
                createMeasuredParagraphs(paint, text, 0, text.length(), dir));
    }

    // create new paras
    @Nonnull
    public static MeasuredParagraph[] createMeasuredParagraphs(
            @Nonnull FontPaint paint, @Nonnull CharSequence text, int start, int end,
            @Nonnull TextDirectionHeuristic dir) {
        List<MeasuredParagraph> list = new ArrayList<>();
        for (int paraStart = start, paraEnd; paraStart < end; paraStart = paraEnd) {
            paraEnd = TextUtils.indexOf(text, '\n', paraStart, end);
            if (paraEnd < 0) {
                // No LINE_FEED(U+000A) character found. Use end of the text as the paragraph
                // end.
                paraEnd = end;
            } else {
                paraEnd++;  // Includes LINE_FEED(U+000A) to the prev paragraph.
            }
            list.add(MeasuredParagraph.buildForStaticLayout(
                    paint, text, paraStart, paraEnd, dir, null));
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
}
