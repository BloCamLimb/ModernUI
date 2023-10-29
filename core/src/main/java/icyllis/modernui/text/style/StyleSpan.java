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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.text.*;
import icyllis.modernui.util.Parcel;

/**
 * Span that allows setting the style of the text it's attached to.
 * Possible styles are: {@link Typeface#NORMAL}, {@link Typeface#BOLD},
 * {@link Typeface#ITALIC} and {@link Typeface#BOLD_ITALIC}.
 * <p>
 * Note that styles are cumulative -- if both bold and italic are set in
 * separate spans, or if the base style is bold and a span calls for italic,
 * you get bold italic.
 * <br>You can't turn off a style from the base style.
 * <p>
 * For example, the <code>StyleSpan</code> can be used like this:
 * <pre>
 * SpannableString string = new SpannableString("Bold and italic text");
 * string.setSpan(new StyleSpan(Typeface.BOLD), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
 * string.setSpan(new StyleSpan(Typeface.ITALIC), 9, 15, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
 * </pre>
 */
public class StyleSpan extends MetricAffectingSpan implements ParcelableSpan {

    private final int mStyle;

    /**
     * Creates a {@link StyleSpan} from a style.
     *
     * @param style An integer constant describing the style for this span. Examples
     *              include bold, italic, and normal. Values are constants defined
     *              in {@link TextPaint}.
     */
    public StyleSpan(@Paint.TextStyle int style) {
        mStyle = style;
    }

    /**
     * Creates a {@link StyleSpan} from a parcel.
     */
    public StyleSpan(@NonNull Parcel src) {
        mStyle = src.readInt();
    }

    @Override
    public int getSpanTypeId() {
        return TextUtils.STYLE_SPAN;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mStyle);
    }

    /**
     * Returns the style constant defined in {@link TextPaint}.
     */
    @Paint.TextStyle
    public int getStyle() {
        return mStyle;
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint paint) {
        paint.setTextStyle(mStyle);
    }
}
