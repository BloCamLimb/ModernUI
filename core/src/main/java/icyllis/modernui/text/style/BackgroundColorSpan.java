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

import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.text.*;

import javax.annotation.Nonnull;
import java.io.*;

/**
 * Changes the background color of the text to which the span is attached.
 * <p>
 * For example, to set a green background color for a text you would create a {@link
 * SpannableString} based on the text and set the span.
 * <pre>{@code
 * SpannableString string = new SpannableString("Text with a background color span");
 * string.setSpan(new BackgroundColorSpan(color), 12, 28, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}</pre>
 */
public class BackgroundColorSpan extends CharacterStyle
        implements UpdateAppearance, ParcelableSpan {

    private final int mColor;

    /**
     * Creates a {@link BackgroundColorSpan} from a color integer.
     *
     * @param color color integer that defines the background color
     */
    public BackgroundColorSpan(@ColorInt int color) {
        mColor = color;
    }

    /**
     * Creates a {@link BackgroundColorSpan} from a stream.
     */
    public BackgroundColorSpan(@Nonnull DataInput src) throws IOException {
        mColor = src.readInt();
    }

    @Override
    public int getSpanTypeId() {
        return TextUtils.BACKGROUND_COLOR_SPAN;
    }

    @Override
    public void write(@Nonnull DataOutput dest) throws IOException {
        dest.writeInt(mColor);
    }

    /**
     * @return the background color of this span.
     * @see BackgroundColorSpan#BackgroundColorSpan(int)
     */
    @ColorInt
    public int getBackgroundColor() {
        return mColor;
    }

    /**
     * Updates the background color of the TextPaint.
     */
    @Override
    public void updateDrawState(@Nonnull TextPaint paint) {
        paint.bgColor = mColor;
    }
}
