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

import icyllis.modernui.text.style.CharacterStyle;
import icyllis.modernui.text.style.ParagraphStyle;
import icyllis.modernui.widget.TextView;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;

/**
 * This is the interface for text to which markup objects can be
 * attached and detached.
 * <br>Not all {@code Spannable} classes have mutable text; see
 * {@link Editable} for that.
 */
public interface Spannable extends Spanned {

    /**
     * Standard factory.
     */
    Factory DEFAULT_FACTORY = SpannableString::new;

    /**
     * Attach the specified markup object to the range <code>start&hellip;end</code>
     * of the text, or move the object to that range if it was already
     * attached elsewhere.  See {@link Spanned} for an explanation of
     * what the flags mean.  The object can be one that has meaning only
     * within your application, or it can be one that the text system will
     * use to affect text display or behavior.  Some noteworthy ones are
     * the subclasses of {@link CharacterStyle} and {@link ParagraphStyle},
     * and {@link TextWatcher} and {@link SpanWatcher}.
     *
     * @param span  the markup object
     * @param start the start char index of the span
     * @param end   the end char index of the span
     * @param flags the flags of the span
     */
    void setSpan(@Nonnull Object span, int start, int end, int flags);

    /**
     * Remove the specified object from the range of text to which it
     * was attached, if any.
     * <br>It is OK to remove an object that was never attached in the
     * first place.
     *
     * @param span markup object to remove
     */
    void removeSpan(@Nonnull Object span);

    /**
     * Remove the specified object from the range of text to which it
     * was attached, if any.
     * <br>It is OK to remove an object that was never attached in the
     * first place.
     * <p>
     * See {@link Spanned} for an explanation of what the flags mean.
     *
     * @param span  markup object to remove
     * @param flags flags
     */
    @ApiStatus.Internal
    default void removeSpan(@Nonnull Object span, int flags) {
        removeSpan(span);
    }

    /**
     * Factory used by {@link TextView} to create new {@link Spannable Spannables}.
     * You can subclass it to provide something other than {@link SpannableString}.
     *
     * @see #DEFAULT_FACTORY
     * @see TextView#setSpannableFactory(Factory)
     */
    @FunctionalInterface
    interface Factory {

        /**
         * Returns a new {@link Spannable} from the specified {@link CharSequence}.
         * You can override this to provide a different kind of {@link Spannable}.
         *
         * @param source the source to be created from
         * @return a new {@link Spannable}
         */
        @Nonnull
        Spannable newSpannable(@Nonnull CharSequence source);
    }
}
