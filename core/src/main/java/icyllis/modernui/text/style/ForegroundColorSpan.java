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
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.text.*;
import icyllis.modernui.util.Parcel;

/**
 * Changes the color of the text to which the span is attached.
 * <p>
 * For example, to set a green text color you would create a {@link
 * SpannableString} based on the text and set the span.
 * <pre>{@code
 * SpannableString string = new SpannableString("Text with a foreground color span");
 * string.setSpan(new ForegroundColorSpan(color), 12, 28, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}</pre>
 */
public class ForegroundColorSpan extends CharacterStyle
        implements UpdateAppearance, ParcelableSpan {

    private final int mColor;

    /**
     * Creates a {@link ForegroundColorSpan} from a color integer.
     *
     * @param color color integer that defines the text color
     */
    public ForegroundColorSpan(@ColorInt int color) {
        mColor = color;
    }

    /**
     * Creates a {@link ForegroundColorSpan} from a parcel.
     */
    public ForegroundColorSpan(@NonNull Parcel src) {
        mColor = src.readInt();
    }

    @Override
    public int getSpanTypeId() {
        return TextUtils.FOREGROUND_COLOR_SPAN;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mColor);
    }

    /**
     * @return the foreground color of this span.
     * @see ForegroundColorSpan#ForegroundColorSpan(int)
     */
    @ColorInt
    public int getForegroundColor() {
        return mColor;
    }

    /**
     * Updates the color of the TextPaint to the foreground color.
     */
    @Override
    public void updateDrawState(@NonNull TextPaint paint) {
        paint.setColor(mColor);
    }
}
