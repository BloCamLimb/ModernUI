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
import icyllis.modernui.text.*;
import icyllis.modernui.util.Parcel;

/**
 * A span that underlines the text it's attached to.
 * <p>
 * The span can be used like this:
 * <pre>{@code
 * SpannableString string = new SpannableString("Text with underline span");
 * string.setSpan(new UnderlineSpan(), 10, 19, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}</pre>
 */
public class UnderlineSpan extends CharacterStyle
        implements UpdateAppearance, ParcelableSpan {

    /**
     * Creates an {@link UnderlineSpan}.
     */
    public UnderlineSpan() {
    }

    /**
     * Creates an {@link UnderlineSpan} from a parcel.
     */
    public UnderlineSpan(@NonNull Parcel src) {
    }

    @Override
    public int getSpanTypeId() {
        return TextUtils.UNDERLINE_SPAN;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
    }

    @Override
    public void updateDrawState(@NonNull TextPaint paint) {
        paint.setUnderline(true);
    }
}
