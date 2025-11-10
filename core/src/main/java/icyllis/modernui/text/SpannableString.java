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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.text.GetChars;
import org.jetbrains.annotations.ApiStatus;

/**
 * This is the class for text whose content is immutable but to which
 * markup objects can be attached and detached.
 */
public non-sealed class SpannableString extends SpannableStringInternal implements Spannable, GetChars {

    /**
     * Note: {@link NoCopySpan} will not be copied into this.
     *
     * @param source source object to copy from
     */
    public SpannableString(@NonNull CharSequence source) {
        super(source, 0, source.length());
    }

    /**
     * Note: {@link NoCopySpan} will not be copied into this.
     *
     * @param source source object to copy from
     */
    public SpannableString(@NonNull CharSequence source, int start, int end) {
        super(source, start, end);
    }

    @NonNull
    public static SpannableString valueOf(@NonNull CharSequence source) {
        if (source instanceof SpannableString) {
            return (SpannableString) source;
        } else {
            return new SpannableString(source);
        }
    }

    @Override
    public void setSpan(@NonNull Object span, int start, int end, int flags) {
        super.setSpan(true, span, start, end, flags, true);
    }

    @Override
    public void removeSpan(@NonNull Object span) {
        super.removeSpan(span, 0);
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    @Override
    public void removeSpan(@NonNull Object span, int flags) {
        super.removeSpan(span, flags);
    }

    @NonNull
    @Override
    public CharSequence subSequence(int start, int end) {
        return new SpannableString(this, start, end);
    }
}
