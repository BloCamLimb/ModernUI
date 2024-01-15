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

package icyllis.modernui.graphics

import icyllis.modernui.annotation.ColorInt

/**
 * Return the alpha component of a color int. This is equivalent to calling:
 * ```
 * Color.alpha(myInt)
 * ```
 */
inline val @receiver:ColorInt Int.alpha: Int get() = this ushr 24

/**
 * Return the red component of a color int. This is equivalent to calling:
 * ```
 * Color.red(myInt)
 * ```
 */
inline val @receiver:ColorInt Int.red: Int get() = (this shr 16) and 0xff

/**
 * Return the green component of a color int. This is equivalent to calling:
 * ```
 * Color.green(myInt)
 * ```
 */
inline val @receiver:ColorInt Int.green: Int get() = (this shr 8) and 0xff

/**
 * Return the blue component of a color int. This is equivalent to calling:
 * ```
 * Color.blue(myInt)
 * ```
 */
inline val @receiver:ColorInt Int.blue: Int get() = this and 0xff

/**
 * Return the alpha component of a color int. This is equivalent to calling:
 * ```
 * Color.alpha(myInt)
 * ```
 *
 * This method allows to use destructuring declarations when working with colors,
 * for example:
 * ```
 * val (alpha, red, green, blue) = myColor
 * ```
 */
inline operator fun @receiver:ColorInt Int.component1(): Int = this ushr 24

/**
 * Return the red component of a color int. This is equivalent to calling:
 * ```
 * Color.red(myInt)
 * ```
 *
 * This method allows to use destructuring declarations when working with colors,
 * for example:
 * ```
 * val (alpha, red, green, blue) = myColor
 * ```
 */
inline operator fun @receiver:ColorInt Int.component2(): Int = (this shr 16) and 0xff

/**
 * Return the green component of a color int. This is equivalent to calling:
 * ```
 * Color.green(myInt)
 * ```
 *
 * This method allows to use destructuring declarations when working with colors,
 * for example:
 * ```
 * val (alpha, red, green, blue) = myColor
 * ```
 */
inline operator fun @receiver:ColorInt Int.component3(): Int = (this shr 8) and 0xff

/**
 * Return the blue component of a color int. This is equivalent to calling:
 * ```
 * Color.blue(myInt)
 * ```
 *
 * This method allows to use destructuring declarations when working with colors,
 * for example:
 * ```
 * val (alpha, red, green, blue) = myColor
 * ```
 */
inline operator fun @receiver:ColorInt Int.component4(): Int = this and 0xff

/**
 * Return a corresponding [Int] color of this [String].
 *
 * Supported formats are:
 * ```
 * #RRGGBB
 * #AARRGGBB
 * 0xRRGGBB
 * 0xAARRGGBB
 * ```
 *
 * @throws IllegalArgumentException if this [String] cannot be parsed.
 */
@ColorInt
inline fun String.toColorInt(): Int = Color.parseColor(this)