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
 * Returns the x coordinate of this point.
 *
 * This method allows to use destructuring declarations when working with points,
 * for example:
 * ```
 * val (x, y) = myPoint
 * ```
 */
inline operator fun Point.component1(): Int = this.x

/**
 * Returns the y coordinate of this point.
 *
 * This method allows to use destructuring declarations when working with points,
 * for example:
 * ```
 * val (x, y) = myPoint
 * ```
 */
inline operator fun Point.component2(): Int = this.y

/**
 * Returns the x coordinate of this point.
 *
 * This method allows to use destructuring declarations when working with points,
 * for example:
 * ```
 * val (x, y) = myPoint
 * ```
 */
inline operator fun PointF.component1(): Float = this.x

/**
 * Returns the y coordinate of this point.
 *
 * This method allows to use destructuring declarations when working with points,
 * for example:
 * ```
 * val (x, y) = myPoint
 * ```
 */
inline operator fun PointF.component2(): Float = this.y

/**
 * Offsets this point by the specified point and returns the result as a new point.
 */
inline operator fun Point.plus(p: Point): Point {
    return Point(x, y).apply {
        offset(p.x, p.y)
    }
}

/**
 * Offsets this point by the specified point.
 */
inline operator fun Point.plusAssign(p: Point): Unit = offset(p.x, p.y)

/**
 * Offsets this point by the specified point and returns the result as a new point.
 */
inline operator fun PointF.plus(p: PointF): PointF {
    return PointF(x, y).apply {
        offset(p.x, p.y)
    }
}

/**
 * Offsets this point by the specified point.
 */
inline operator fun PointF.plusAssign(p: PointF): Unit = offset(p.x, p.y)

/**
 * Offsets this point by the specified amount on both X and Y axis and returns the
 * result as a new point.
 */
inline operator fun Point.plus(xy: Int): Point {
    return Point(x, y).apply {
        offset(xy, xy)
    }
}

/**
 * Offsets this point by the specified amount on both X and Y axis.
 */
inline operator fun Point.plusAssign(xy: Int): Unit = offset(xy, xy)

/**
 * Offsets this point by the specified amount on both X and Y axis and returns the
 * result as a new point.
 */
inline operator fun PointF.plus(xy: Float): PointF {
    return PointF(x, y).apply {
        offset(xy, xy)
    }
}

/**
 * Offsets this point by the specified amount on both X and Y axis.
 */
inline operator fun PointF.plusAssign(xy: Float): Unit = offset(xy, xy)

/**
 * Offsets this point by the negation of the specified point and returns the result
 * as a new point.
 */
inline operator fun Point.minus(p: Point): Point {
    return Point(x, y).apply {
        offset(-p.x, -p.y)
    }
}

/**
 * Offsets this point by the negation of the specified point.
 */
inline operator fun Point.minusAssign(p: Point): Unit = offset(-p.x, -p.y)

/**
 * Offsets this point by the negation of the specified point and returns the result
 * as a new point.
 */
inline operator fun PointF.minus(p: PointF): PointF {
    return PointF(x, y).apply {
        offset(-p.x, -p.y)
    }
}

/**
 * Offsets this point by the negation of the specified point.
 */
inline operator fun PointF.minusAssign(p: PointF): Unit = offset(-p.x, -p.y)

/**
 * Offsets this point by the negation of the specified amount on both X and Y axis and
 * returns the result as a new point.
 */
inline operator fun Point.minus(xy: Int): Point {
    return Point(x, y).apply {
        offset(-xy, -xy)
    }
}

/**
 * Offsets this point by the negation of the specified amount on both X and Y axis.
 */
inline operator fun Point.minusAssign(xy: Int): Unit = offset(-xy, -xy)

/**
 * Offsets this point by the negation of the specified amount on both X and Y axis and
 * returns the result as a new point.
 */
inline operator fun PointF.minus(xy: Float): PointF {
    return PointF(x, y).apply {
        offset(-xy, -xy)
    }
}

/**
 * Offsets this point by the negation of the specified amount on both X and Y axis.
 */
inline operator fun PointF.minusAssign(xy: Float): Unit = offset(-xy, -xy)

/**
 * Returns a new point representing the negation of this point.
 */
inline operator fun Point.unaryMinus(): Point = Point(-x, -y)

/**
 * Returns a new point representing the negation of this point.
 */
inline operator fun PointF.unaryMinus(): PointF = PointF(-x, -y)

/**
 * Multiplies this point by the specified scalar value and returns the result as a new point.
 */
inline operator fun Point.times(scalar: Float): Point {
    return Point(Math.round(this.x * scalar), Math.round(this.y * scalar))
}

/**
 * Multiplies this point by the specified scalar value and returns the result as a new point.
 */
inline operator fun PointF.times(scalar: Float): PointF {
    return PointF(this.x * scalar, this.y * scalar)
}

/**
 * Divides this point by the specified scalar value and returns the result as a new point.
 */
inline operator fun Point.div(scalar: Float): Point {
    return Point(Math.round(this.x / scalar), Math.round(this.y / scalar))
}

/**
 * Divides this point by the specified scalar value and returns the result as a new point.
 */
inline operator fun PointF.div(scalar: Float): PointF {
    return PointF(this.x / scalar, this.y / scalar)
}