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

/**
 * Multiplies this [Matrix] by another matrix and returns the result as
 * a new matrix.
 */
inline operator fun Matrix.times(m: Matrix): Matrix = Matrix(this).apply { preConcat(m) }

/**
 * Multiplies this [Matrix] by another matrix.
 */
inline operator fun Matrix.timesAssign(m: Matrix): Unit = preConcat(m)

/**
 * Retrieves elements of this [Matrix].
 */
operator fun Matrix.get(index: Int): Float {
    return when (index) {
        0 -> m11()
        1 -> m12()
        2 -> m14()
        3 -> m21()
        4 -> m22()
        5 -> m24()
        6 -> m41()
        7 -> m42()
        8 -> m44()
        else -> throw IndexOutOfBoundsException(index)
    }
}