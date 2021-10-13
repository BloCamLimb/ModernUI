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

/**
 * This is the class for text whose content is immutable but to which
 * markup objects can be attached and detached.
 */
public class SpannableString extends SpannableStringInternal implements Spannable, GetChars {

    /**
     * @param source           source object to copy from
     * @param ignoreNoCopySpan whether to copy NoCopySpans in the {@code source}
     */
    public SpannableString(@Nonnull CharSequence source, boolean ignoreNoCopySpan) {
        super(source, 0, source.length(), ignoreNoCopySpan);
    }

    public SpannableString(@Nonnull CharSequence source) {
        this(source, false);
    }

    private SpannableString(@Nonnull CharSequence source, int start, int end) {
        super(source, start, end, false);
    }

    @Nonnull
    public static SpannableString valueOf(@Nonnull CharSequence source) {
        if (source instanceof SpannableString) {
            return (SpannableString) source;
        } else {
            return new SpannableString(source);
        }
    }

    @Nonnull
    @Override
    public final CharSequence subSequence(int start, int end) {
        return new SpannableString(this, start, end);
    }
}
