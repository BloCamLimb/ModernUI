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
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.text.*;
import icyllis.modernui.util.BinaryIO;

import java.io.DataOutput;
import java.io.IOException;

public class TypefaceSpan extends MetricAffectingSpan implements ParcelableSpan {

    @Nullable
    private final String mFamily;

    @Nullable
    private final Typeface mTypeface;

    /**
     * Constructs a {@link TypefaceSpan} based on the font family. The previous style of the
     * TextPaint is kept. If the font family is null, the text paint is not modified.
     *
     * @param family The font family for this typeface.  Examples include
     *               "monospace", "serif", and "sans-serif"
     */
    public TypefaceSpan(@Nullable String family) {
        this(family, null);
    }

    /**
     * Constructs a {@link TypefaceSpan} from a {@link Typeface}. The previous style of the
     * TextPaint is overridden and the style of the typeface is used.
     *
     * @param typeface the typeface
     */
    public TypefaceSpan(@NonNull Typeface typeface) {
        this(null, typeface);
    }

    private TypefaceSpan(@Nullable String family, @Nullable Typeface typeface) {
        mFamily = family;
        mTypeface = typeface;
    }

    @Override
    public int getSpanTypeId() {
        return TextUtils.TYPEFACE_SPAN;
    }

    @Override
    public void write(@NonNull DataOutput dest) throws IOException {
        BinaryIO.writeString(dest, mFamily);
    }

    /**
     * Returns the font family name set in the span.
     *
     * @return the font family name
     * @see #TypefaceSpan(String)
     */
    @Nullable
    public String getFamily() {
        return mFamily;
    }

    /**
     * Returns the typeface set in the span.
     *
     * @return the typeface set
     * @see #TypefaceSpan(Typeface)
     */
    @Nullable
    public Typeface getTypeface() {
        return mTypeface;
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint paint) {
        if (mTypeface != null) {
            paint.setTypeface(mTypeface);
        } else if (mFamily != null) {
            Typeface typeface = Typeface.getSystemFont(mFamily);
            paint.setTypeface(typeface);
        }
    }
}
