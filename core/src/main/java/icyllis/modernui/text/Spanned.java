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

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package icyllis.modernui.text;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * This is the interface for text that has markup objects attached to
 * ranges of it. This class is modified from Android.
 */
public interface Spanned extends CharSequence {

    /**
     * Bitmask of bits that are relevant for controlling point/mark behavior
     * of spans.
     * <p>
     * MARK and POINT are conceptually located <i>between</i> two adjacent characters.
     * A MARK is "attached" to the character before, while a POINT will stick to the character
     * after. The insertion cursor is conceptually located between the MARK and the POINT.
     * <p>
     * As a result, inserting a new character between a MARK and a POINT will leave the MARK
     * unchanged, while the POINT will be shifted, now located after the inserted character and
     * still glued to the same character after it.
     * <p>
     * Depending on whether the insertion happens at the beginning or the end of a span, the span
     * will hence be expanded to <i>include</i> the new character (when the span is using a MARK at
     * its beginning or a POINT at its end) or it will be <i>excluded</i>.
     * <p>
     * Note that <i>before</i> and <i>after</i> here refer to offsets in the String, which are
     * independent from the visual representation of the text (left-to-right or right-to-left).
     */
    int SPAN_POINT_MARK_MASK = 0x33;

    /**
     * 0-length spans with type SPAN_MARK_MARK behave like text marks:
     * they remain at their original offset when text is inserted
     * at that offset. Conceptually, the text is added after the mark.
     */
    int SPAN_MARK_MARK = 0x11;
    /**
     * SPAN_MARK_POINT is a synonym for {@link #SPAN_INCLUSIVE_INCLUSIVE}.
     */
    int SPAN_MARK_POINT = 0x12;
    /**
     * SPAN_POINT_MARK is a synonym for {@link #SPAN_EXCLUSIVE_EXCLUSIVE}.
     */
    int SPAN_POINT_MARK = 0x21;

    /**
     * 0-length spans with type SPAN_POINT_POINT behave like cursors:
     * they are pushed forward by the length of the insertion when text
     * is inserted at their offset.
     * The text is conceptually inserted before the point.
     */
    int SPAN_POINT_POINT = 0x22;

    /**
     * SPAN_PARAGRAPH behaves like SPAN_INCLUSIVE_EXCLUSIVE
     * (SPAN_MARK_MARK), except that if either end of the span is
     * at the end of the buffer, that end behaves like _POINT
     * instead (so SPAN_INCLUSIVE_INCLUSIVE if it starts in the
     * middle and ends at the end, or SPAN_EXCLUSIVE_INCLUSIVE
     * if it both starts and ends at the end).
     * <p>
     * Its endpoints must be the start or end of the buffer or
     * immediately after a \n character, and if the \n
     * that anchors it is deleted, the endpoint is pulled to the
     * next \n that follows in the buffer (or to the end of
     * the buffer). If a span with SPAN_PARAGRAPH flag is pasted
     * into another text and the paragraph boundary constraint
     * is not satisfied, the span is discarded.
     */
    int SPAN_PARAGRAPH = 0x33;

    /**
     * Non-0-length spans of type SPAN_INCLUSIVE_EXCLUSIVE expand
     * to include text inserted at their starting point but not at their
     * ending point.  When 0-length, they behave like marks.
     */
    int SPAN_INCLUSIVE_EXCLUSIVE = SPAN_MARK_MARK;

    /**
     * Spans of type SPAN_INCLUSIVE_INCLUSIVE expand
     * to include text inserted at either their starting or ending point.
     */
    int SPAN_INCLUSIVE_INCLUSIVE = SPAN_MARK_POINT;

    /**
     * Spans of type SPAN_EXCLUSIVE_EXCLUSIVE do not expand
     * to include text inserted at either their starting or ending point.
     * They can never have a length of 0 and are automatically removed
     * from the buffer if all the text they cover is removed.
     */
    int SPAN_EXCLUSIVE_EXCLUSIVE = SPAN_POINT_MARK;

    /**
     * Non-0-length spans of type SPAN_EXCLUSIVE_INCLUSIVE expand
     * to include text inserted at their ending point but not at their
     * starting point.  When 0-length, they behave like points.
     */
    int SPAN_EXCLUSIVE_INCLUSIVE = SPAN_POINT_POINT;

    /**
     * This flag is set on spans that are being used to apply temporary
     * styling information on the composing text of an input method, so that
     * they can be found and removed when the composing text is being
     * replaced.
     */
    int SPAN_COMPOSING = 0x100;

    /**
     * This flag will be set for intermediate span changes, meaning there
     * is guaranteed to be another change following it.  Typically it is
     * used for {@link Selection} which automatically uses this with the first
     * offset it sets when updating the selection.
     */
    int SPAN_INTERMEDIATE = 0x200;

    /**
     * The bits numbered SPAN_USER_SHIFT and above are available
     * for callers to use to store scalar data associated with their
     * span object.
     */
    int SPAN_USER_SHIFT = 24;
    /**
     * The bits specified by the SPAN_USER bitfield are available
     * for callers to use to store scalar data associated with their
     * span object.
     */
    int SPAN_USER = 0xFFFFFFFF << SPAN_USER_SHIFT;

    /**
     * The bits numbered just above SPAN_PRIORITY_SHIFT determine the order
     * of change notifications -- higher numbers go first.  You probably
     * don't need to set this; it is used so that when text changes, the
     * text layout gets the chance to update itself before any other
     * callbacks can inquire about the layout of the text.
     */
    int SPAN_PRIORITY_SHIFT = 16;
    /**
     * The bits specified by the SPAN_PRIORITY bitmap determine the order
     * of change notifications -- higher numbers go first.  You probably
     * don't need to set this; it is used so that when text changes, the
     * text layout gets the chance to update itself before any other
     * callbacks can inquire about the layout of the text.
     */
    int SPAN_PRIORITY = 0xFF << SPAN_PRIORITY_SHIFT;

    /**
     * Query a set of the markup objects attached to the specified
     * slice of this {@link CharSequence} and whose type is the specified type
     * or a subclass of it.  Specify {@code Object.class} for the type
     * if you want all the objects regardless of type.
     * <p>
     * If <code>out</code> list is not null, then it will be filled with the
     * method result and returns null. If <code>out</code> list is <code>null</code>, then
     * method will return an array of the method result. If result is empty,
     * returns <code>null</code>. This method will never return an empty array.
     *
     * @param <T>   markup type
     * @param start start char index of the slice
     * @param end   end char index of the slice
     * @param type  markup class
     * @param out   the list to receive the result
     * @return an array of the markup objects, or null
     */
    @Nullable
    <T> T[] getSpans(int start, int end, Class<T> type, @Nullable List<T> out);

    /**
     * Return the beginning of the range of text to which the specified
     * markup object is attached, or {@code -1} if the object is not attached.
     *
     * @param span markup object
     * @return the start char index
     */
    int getSpanStart(@Nonnull Object span);

    /**
     * Return the end of the range of text to which the specified
     * markup object is attached, or {@code -1} if the object is not attached.
     *
     * @param span markup object
     * @return the end char index
     */
    int getSpanEnd(@Nonnull Object span);

    /**
     * Return the flags that were specified when {@link Spannable#setSpan} was
     * used to attach the specified markup object, or {@code 0} if the specified
     * object has not been attached.
     *
     * @param span markup object
     * @return the flags
     */
    int getSpanFlags(@Nonnull Object span);

    /**
     * Return the first offset greater than {@code start} where a markup
     * object of class {@code type} begins or ends, or {@code limit}
     * if there are no starts or ends greater than <code>start</code> but less
     * than {@code limit}. Specify {@code null} or {@code Object.class} for
     * the type if you want every transition regardless of type.
     *
     * @param start start char index of the slice
     * @param limit end char index of the slice
     * @param type  the markup type
     * @return transition point
     */
    int nextSpanTransition(int start, int limit, @Nullable Class<?> type);
}
