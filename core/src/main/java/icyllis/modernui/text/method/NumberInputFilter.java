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

package icyllis.modernui.text.method;

import icyllis.modernui.text.InputFilter;
import icyllis.modernui.text.SpannableStringBuilder;
import icyllis.modernui.text.Spanned;

import javax.annotation.Nonnull;

/**
 * Input filter for numeric text.
 */
public abstract class NumberInputFilter implements InputFilter {

    /**
     * You can say which characters you can accept.
     */
    @Nonnull
    protected abstract char[] getAcceptedChars();

    @Override
    public CharSequence filter(@Nonnull CharSequence source, int start, int end,
                               @Nonnull Spanned dest, int dstart, int dend) {
        char[] accept = getAcceptedChars();

        int i;
        for (i = start; i < end; i++) {
            if (not(accept, source.charAt(i))) {
                break;
            }
        }

        if (i == end) {
            // It was all OK.
            return null;
        }

        if (end - start == 1) {
            // It was not OK, and there is only one char, so nothing remains.
            return "";
        }

        SpannableStringBuilder filtered =
                new SpannableStringBuilder(source, start, end);
        i -= start;
        end -= start;

        // Only count down to i because the chars before that were all OK.
        for (int j = end - 1; j >= i; j--) {
            if (not(accept, source.charAt(j))) {
                filtered.delete(j, j + 1);
            }
        }

        return filtered;
    }

    protected static boolean not(@Nonnull char[] accept, char c) {
        for (int i = accept.length - 1; i >= 0; i--) {
            if (accept[i] == c) {
                return false;
            }
        }

        return true;
    }
}
