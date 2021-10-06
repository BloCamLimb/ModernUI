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
 * This is the interface for text whose content and markup can be changed (as opposed
 * to immutable text like Strings). If you make a {@link DynamicLayout} of an Editable,
 * the layout will be re-flowed as the text is changed.
 */
public interface Editable extends Spannable, GetChars, Appendable {

    /**
     * The standard Editable Factory.
     */
    Factory DEFAULT_FACTORY = SpannableStringBuilder::new;

    /**
     * Replaces the specified range (<code>st&hellip;en</code>) of text in this
     * Editable with a copy of the slice <code>start&hellip;end</code> from
     * <code>source</code>.  The destination slice may be empty, in which case
     * the operation is an insertion, or the source slice may be empty,
     * in which case the operation is a deletion.
     * <p>
     * Before the change is committed, each filter that was set with
     * {@link #setFilters} is given the opportunity to modify the
     * <code>source</code> text.
     * <p>
     * If <code>source</code>
     * is Spanned, the spans from it are preserved into the Editable.
     * Existing spans within the Editable that entirely cover the replaced
     * range are retained, but any that were strictly within the range
     * that was replaced are removed. If the <code>source</code> contains a span
     * with {@link Spanned#SPAN_PARAGRAPH} flag, and it does not satisfy the
     * paragraph boundary constraint, it is not retained. As a special case, the
     * cursor position is preserved even when the entire range where it is located
     * is replaced.
     *
     * @return a reference to this object.
     * @see Spanned#SPAN_PARAGRAPH
     */
    Editable replace(int st, int en, CharSequence source, int start, int end);

    /**
     * Convenience for replace(st, en, text, 0, text.length())
     *
     * @see #replace(int, int, CharSequence, int, int)
     */
    Editable replace(int st, int en, CharSequence text);

    /**
     * Convenience for replace(where, where, text, start, end)
     *
     * @see #replace(int, int, CharSequence, int, int)
     */
    Editable insert(int where, CharSequence text, int start, int end);

    /**
     * Convenience for replace(where, where, text, 0, text.length());
     *
     * @see #replace(int, int, CharSequence, int, int)
     */
    Editable insert(int where, CharSequence text);

    /**
     * Convenience for replace(st, en, "", 0, 0)
     *
     * @see #replace(int, int, CharSequence, int, int)
     */
    Editable delete(int st, int en);

    /**
     * Convenience for replace(length(), length(), text, 0, text.length())
     *
     * @see #replace(int, int, CharSequence, int, int)
     */
    @Override
    Editable append(CharSequence text);

    /**
     * Convenience for replace(length(), length(), text, start, end)
     *
     * @see #replace(int, int, CharSequence, int, int)
     */
    @Override
    Editable append(CharSequence text, int start, int end);

    /**
     * Convenience for append(String.valueOf(text)).
     *
     * @see #replace(int, int, CharSequence, int, int)
     */
    @Override
    Editable append(char text);

    /**
     * Convenience for replace(0, length(), "", 0, 0).
     * Note that this clears the text, not the spans;
     * use {@link #clearSpans} if you need that.
     *
     * @see #replace(int, int, CharSequence, int, int)
     */
    void clear();

    /**
     * Removes all spans from the Editable, as if by calling
     * {@link #removeSpan} on each of them.
     */
    void clearSpans();

    /**
     * Sets the series of filters that will be called in succession
     * whenever the text of this Editable is changed, each of which has
     * the opportunity to limit or transform the text that is being inserted.
     */
    void setFilters(InputFilter[] filters);

    /**
     * Returns the array of input filters that are currently applied
     * to changes to this Editable.
     */
    InputFilter[] getFilters();

    /**
     * Factory used by TextView to create new {@link Editable Editables}. You can subclass
     * it to provide something other than {@link SpannableStringBuilder}.
     *
     * @see #DEFAULT_FACTORY
     */
    @FunctionalInterface
    interface Factory {

        /**
         * Returns a new SpannedStringBuilder from the specified
         * CharSequence.  You can override this to provide
         * a different kind of Spanned.
         */
        @Nonnull
        Editable newEditable(@Nonnull CharSequence source);
    }
}
