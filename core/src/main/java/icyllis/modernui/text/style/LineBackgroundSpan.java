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

import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.text.ParcelableSpan;
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.util.Parcel;

/**
 * Used to change the background of lines where the span is attached to.
 */
public interface LineBackgroundSpan extends ParagraphStyle {

    /**
     * Draw the background on the canvas.
     *
     * @param canvas     canvas on which the span should be rendered
     * @param paint      paint used to draw text, which should be left unchanged on exit
     * @param left       left position of the line relative to input canvas, in pixels
     * @param right      right position of the line relative to input canvas, in pixels
     * @param top        top position of the line relative to input canvas, in pixels
     * @param baseline   baseline of the text relative to input canvas, in pixels
     * @param bottom     bottom position of the line relative to input canvas, in pixels
     * @param text       current text
     * @param start      start character index of the line
     * @param end        end character index of the line
     * @param lineNumber line number in the current text layout
     */
    void drawBackground(@NonNull Canvas canvas, @NonNull Paint paint,
                        int left, int right,
                        int top, int baseline, int bottom,
                        @NonNull CharSequence text, int start, int end,
                        int lineNumber);

    /**
     * Default implementation of the {@link LineBackgroundSpan}, which changes the background
     * color of the lines to which the span is attached.
     */
    class Standard implements LineBackgroundSpan, ParcelableSpan {

        private final int mColor;

        /**
         * Constructor taking a color integer.
         *
         * @param color Color integer that defines the background color.
         */
        public Standard(@ColorInt int color) {
            mColor = color;
        }

        /**
         * Creates a {@link LineBackgroundSpan.Standard} from a parcel
         */
        public Standard(@NonNull Parcel src) {
            mColor = src.readInt();
        }

        @Override
        public int getSpanTypeId() {
            return TextUtils.LINE_BACKGROUND_SPAN;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeInt(mColor);
        }

        /**
         * @return the color of this span.
         * @see Standard#Standard(int)
         */
        @ColorInt
        public final int getColor() {
            return mColor;
        }

        @Override
        public void drawBackground(@NonNull Canvas canvas, @NonNull Paint paint,
                                   int left, int right,
                                   int top, int baseline, int bottom,
                                   @NonNull CharSequence text, int start, int end,
                                   int lineNumber) {
            final int color = paint.getColor();
            paint.setColor(mColor);
            canvas.drawRect(left, top, right, bottom, paint);
            paint.setColor(color);
        }
    }
}
