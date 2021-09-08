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
 * This is the interface for text to which markup objects can be
 * attached and detached. This class is modified from Android.
 */
public interface Spannable extends Spanned {

    /**
     * Standard factory.
     */
    Factory DEFAULT_FACTORY = SpannableString::new;

    /**
     * Attach the specified markup object to the range <code>start&hellip;end</code>
     * of the text, or move the object to that range if it was already
     * attached elsewhere.
     *
     * @param span  markup object
     * @param start start char index of the span
     * @param end   end char index of the span
     * @param flags flags
     */
    void setSpan(@Nonnull Object span, int start, int end, int flags);

    /**
     * Remove the specified object from the range of text to which it
     * was attached, if any.  It is OK to remove an object that was never
     * attached in the first place.
     *
     * @param span markup object to remove
     */
    void removeSpan(@Nonnull Object span);

    /**
     * Remove the specified object from the range of text to which it
     * was attached, if any.  It is OK to remove an object that was never
     * attached in the first place.
     *
     * @param span  markup object to remove
     * @param flags flags
     */
    default void removeSpan(@Nonnull Object span, int flags) {
        removeSpan(span);
    }

    /**
     * Factory used by TextView to create new {@link Spannable Spannables}. You can subclass
     * it to provide something other than {@link SpannableString}.
     */
    @FunctionalInterface
    interface Factory {

        /**
         * Creates a new spannable from the specified CharSequence.
         *
         * @param source the source that created from
         * @return a new spannable
         */
        @Nonnull
        Spannable newSpannable(@Nonnull CharSequence source);
    }
}
