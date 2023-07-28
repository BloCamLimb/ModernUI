/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.text.style;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.text.ParcelableSpan;
import icyllis.modernui.text.TextUtils;

import java.io.*;

/**
 * A paragraph style affecting the trailing margin.
 * <p>
 * TrailingMarginSpans should be attached from the first character to the last
 * character of a single paragraph.
 * <p>
 * Added by Modern UI.
 *
 * @see LeadingMarginSpan
 * @since 3.8
 */
public interface TrailingMarginSpan extends ParagraphStyle {

    /**
     * Returns the amount by which to adjust the trailing margin. Positive values
     * move away from the trailing edge of the paragraph, negative values move
     * towards it.
     * <p>
     * The trailing margin is on the right for lines in a left-to-right paragraph,
     * and on the left for lines in a right-to-left paragraph.
     *
     * @return the offset for the margin.
     */
    int getTrailingMargin();

    class Standard implements TrailingMarginSpan, ParcelableSpan {

        private final int mTrailing;

        /**
         * Constructor taking an indent for the trailing margin.
         *
         * @param trailing the indent for the trailing edge of the paragraph
         */
        public Standard(int trailing) {
            mTrailing = trailing;
        }

        public Standard(@NonNull DataInput src) throws IOException {
            mTrailing = src.readInt();
        }

        @Override
        public int getSpanTypeId() {
            return TextUtils.TRAILING_MARGIN_SPAN;
        }

        @Override
        public void write(@NonNull DataOutput dest) throws IOException {
            dest.writeInt(mTrailing);
        }

        @Override
        public int getTrailingMargin() {
            return mTrailing;
        }
    }
}
