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
import javax.annotation.Nullable;

/**
 * InputFilters can be attached to {@link Editable Editables} to constrain the
 * changes that can be made to them.
 */
@FunctionalInterface
public interface InputFilter {

    /**
     * This method is called when the buffer is going to replace the
     * range <code>dstart &hellip; dend</code> of <code>dest</code>
     * with the new text from the range <code>start &hellip; end</code>
     * of <code>source</code>.  Return the CharSequence that you would
     * like to have placed there instead, including an empty string
     * if appropriate, or <code>null</code> to accept the original
     * replacement.  Be careful to not to reject 0-length replacements,
     * as this is what happens when you delete text.  Also beware that
     * you should not attempt to make any changes to <code>dest</code>
     * from this method; you may only examine it for context.
     * <p>
     * Note: If <var>source</var> is an instance of {@link Spanned} or
     * {@link Spannable}, the span objects in the <var>source</var> should be
     * copied into the filtered result (i.e. the non-null return value).
     * {@link TextUtils#copySpansFrom} can be used for convenience if the
     * span boundary indices would be remaining identical relative to the source.
     */
    @Nullable
    CharSequence filter(@Nonnull CharSequence source, int start, int end,
                        @Nonnull Spanned dest, int dstart, int dend);

    /**
     * This filter will constrain edits not to make the length of the text
     * greater than the specified length.
     */
    class LengthFilter implements InputFilter {

        private final int mMax;

        public LengthFilter(int max) {
            mMax = max;
        }

        @Nullable
        @Override
        public CharSequence filter(@Nonnull CharSequence source, int start, int end,
                                   @Nonnull Spanned dest, int dstart, int dend) {
            int keep = mMax - (dest.length() - (dend - dstart));
            if (keep <= 0) {
                return "";
            } else if (keep >= end - start) {
                return null; // keep original
            } else {
                keep += start;
                if (Character.isHighSurrogate(source.charAt(keep - 1))) {
                    --keep;
                    if (keep == start) {
                        return "";
                    }
                }
                return source.subSequence(start, keep);
            }
        }

        /**
         * @return the maximum length enforced by this input filter
         */
        public int getMax() {
            return mMax;
        }
    }
}
