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

@file:Suppress("NOTHING_TO_INLINE")

package icyllis.modernui.text

/**
 * Returns a new [Spannable] from [CharSequence],
 * or the source itself if it is already an instance of [SpannableString].
 */
inline fun CharSequence.toSpannable(): Spannable = SpannableString.valueOf(this)

/**
 * Add [span] to the range [start]&hellip;[end] of the text.
 *
 * ```
 * val s = "Hello, World!".toSpannable()
 * s[0, 5] = UnderlineSpan()
 * ```
 *
 * Note: The [end] value is exclusive.
 *
 * @see Spannable.setSpan
 */
inline operator fun Spannable.set(start: Int, end: Int, span: Any) {
    setSpan(span, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
}

/**
 * Add [span] to the [range] of the text.
 *
 * ```
 * val s = "Hello, World!".toSpannable()
 * s[0..5] = UnderlineSpan()
 * ```
 *
 * Note: The range end value is exclusive.
 *
 * @see Spannable.setSpan
 */
inline operator fun Spannable.set(range: IntRange, span: Any) {
    setSpan(span, range.first, range.last, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
}