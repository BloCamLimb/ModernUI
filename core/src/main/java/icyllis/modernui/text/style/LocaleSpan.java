/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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
import icyllis.modernui.util.Parcel;

import java.util.Locale;
import java.util.Objects;

/**
 * Changes the {@link Locale} of the text to which the span is attached.
 */
public class LocaleSpan extends MetricAffectingSpan implements ParcelableSpan {

    @Nullable
    private final Locale mLocale;

    /**
     * Creates a {@link LocaleSpan} from a well-formed {@link Locale}.  Note that only
     * {@link Locale} objects that can be created by {@link Locale#forLanguageTag(String)} are
     * supported.
     *
     * <p><b>Caveat:</b> Do not specify any {@link Locale} object that cannot be created by
     * {@link Locale#forLanguageTag(String)}.  {@code new Locale(" a ", " b c", " d")} is an
     * example of such a malformed {@link Locale} object.</p>
     *
     * @param locale The {@link Locale} of the text to which the span is attached.
     */
    public LocaleSpan(@Nullable Locale locale) {
        mLocale = locale;
    }

    public LocaleSpan(@NonNull Parcel source) {
        String tag = source.readString8();
        if (tag == null || tag.isEmpty()) {
            mLocale = null;
        } else {
            mLocale = Locale.forLanguageTag(tag);
        }
    }

    @Override
    public int getSpanTypeId() {
        return TextUtils.LOCALE_SPAN;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString8(mLocale != null ? mLocale.toLanguageTag() : "");
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint paint) {
        paint.setTextLocale(Objects.requireNonNull(mLocale, "locale cannot be null"));
    }

    /**
     * @return The {@link Locale} for this span.  If multiple locales are associated with this
     * span, only the first locale is returned.  {@code null} if no {@link Locale} is specified.
     */
    @Nullable
    public Locale getLocale() {
        return mLocale;
    }
}
