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

    public static class ParagraphInfo {

        public final int paragraphEnd;
        @Nonnull
        public final MeasuredParagraph measured;

        /**
         * @param paraEnd  the end offset of this paragraph
         * @param measured a measured paragraph
         */
        public ParagraphInfo(int paraEnd, @Nonnull MeasuredParagraph measured) {
            this.paragraphEnd = paraEnd;
            this.measured = measured;
        }
    }

    // create new paras
    public static ParagraphInfo[] createMeasuredParagraphs(@Nonnull TextPaint paint, @Nonnull CharSequence text,
                                                           int start, int end, @Nonnull TextDirectionHeuristic dir) {
        List<ParagraphInfo> list = new ArrayList<>();
        for (int paraStart = start, paraEnd; paraStart < end; paraStart = paraEnd) {
            paraEnd = TextUtils.indexOf(text, '\n', paraStart, end);
            if (paraEnd < 0) {
                // No LINE_FEED(U+000A) character found. Use end of the text as the paragraph
                // end.
                paraEnd = end;
            } else {
                paraEnd++;  // Includes LINE_FEED(U+000A) to the prev paragraph.
            }
            final ParagraphInfo info = new ParagraphInfo(paraEnd, MeasuredParagraph.buildForStaticLayout(
                    paint, text, paraStart, paraEnd, dir, null));
            if (info.measured.getTextLength() > 0) {
                list.add(info);
            }
        }
        return list.toArray(new ParagraphInfo[0]);
    }
}
