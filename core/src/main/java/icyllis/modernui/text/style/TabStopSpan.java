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

package icyllis.modernui.text.style;

/**
 * Paragraph affecting span that changes the position of the tab with respect to
 * the leading margin of the line. <code>TabStopSpan</code> will only affect the first tab
 * encountered on the first line of the text.
 */
public interface TabStopSpan extends ParagraphStyle {

    /**
     * Returns the offset of the tab stop from the leading margin of the line, in pixels.
     *
     * @return the offset, in pixels
     */
    int getTabStop();

    /**
     * The default implementation of TabStopSpan that allows setting the offset of the tab stop
     * from the leading margin of the first line of text.
     * <p>
     * Let's consider that we have the following text: <i>"\tParagraph text beginning with tab."</i>
     * and we want to move the tab stop with 100px. This can be achieved like this:
     * <pre>
     * SpannableString string = new SpannableString("\tParagraph text beginning with tab.");
     * string.setSpan(new TabStopSpan.Standard(100), 0, string.length(),
     * Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);</pre>
     */
    class Standard implements TabStopSpan {

        private final int mTabOffset;

        /**
         * Constructs a {@link TabStopSpan.Standard} based on an offset.
         *
         * @param offset the offset of the tab stop from the leading margin of
         *               the line, in pixels
         */
        public Standard(int offset) {
            mTabOffset = offset;
        }

        @Override
        public int getTabStop() {
            return mTabOffset;
        }
    }
}
