/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.text;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.text.GetChars;

/**
 * This is the class for text whose content and markup are immutable.
 * For mutable markup, see {@link SpannableString}.
 * <p>
 * Although a SpannedString is immutable, it is thread-safe only if you safely
 * publish it.
 */
public final class SpannedString extends SpannableStringInternal implements Spanned, GetChars {

    /**
     * A static, final, empty SpannedString, no spans, thread-safe.
     */
    public static final SpannedString EMPTY = new SpannedString("");

    /**
     * Note: {@link NoCopySpan} will not be copied into this.
     *
     * @param source source object to copy from
     */
    public SpannedString(@NonNull CharSequence source) {
        super(source, 0, source.length());
    }

    /**
     * Note: {@link NoCopySpan} will not be copied into this.
     *
     * @param source source object to copy from
     */
    public SpannedString(@NonNull CharSequence source, int start, int end) {
        super(source, start, end);
    }

    @NonNull
    public static SpannedString valueOf(@NonNull CharSequence source) {
        if (source instanceof SpannedString) {
            return (SpannedString) source;
        } else {
            return new SpannedString(source);
        }
    }

    @NonNull
    @Override
    public CharSequence subSequence(int start, int end) {
        if (start == 0 && end == length()) {
            return this;
        }
        return new SpannedString(this, start, end);
    }
}
