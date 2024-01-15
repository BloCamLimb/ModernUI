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
 * Returns "left", the first component of the rectangle.
 *
 * This method allows to use destructuring declarations when working with rectangles,
 * for example:
 * ```
 * val (left, top, right, bottom) = myRectangle
 * ```
 */
inline operator fun Rect.component1(): Int = this.left

/**
 * Returns "top", the second component of the rectangle.
 *
 * This method allows to use destructuring declarations when working with rectangles,
 * for example:
 * ```
 * val (left, top, right, bottom) = myRectangle
 * ```
 */
inline operator fun Rect.component2(): Int = this.top

/**
 * Returns "right", the third component of the rectangle.
 *
 * This method allows to use destructuring declarations when working with rectangles,
 * for example:
 * ```
 * val (left, top, right, bottom) = myRectangle
 * ```
 */
inline operator fun Rect.component3(): Int = this.right

/**
 * Returns "bottom", the fourth component of the rectangle.
 *
 * This method allows to use destructuring declarations when working with rectangles,
 * for example:
 * ```
 * val (left, top, right, bottom) = myRectangle
 * ```
 */
inline operator fun Rect.component4(): Int = this.bottom

/**
 * Returns "left", the first component of the rectangle.
 *
 * This method allows to use destructuring declarations when working with rectangles,
 * for example:
 * ```
 * val (left, top, right, bottom) = myRectangle
 * ```
 */
inline operator fun RectF.component1(): Float = this.left

/**
 * Returns "top", the second component of the rectangle.
 *
 * This method allows to use destructuring declarations when working with rectangles,
 * for example:
 * ```
 * val (left, top, right, bottom) = myRectangle
 * ```
 */
inline operator fun RectF.component2(): Float = this.top

/**
 * Returns "right", the third component of the rectangle.
 *
 * This method allows to use destructuring declarations when working with rectangles,
 * for example:
 * ```
 * val (left, top, right, bottom) = myRectangle
 * ```
 */
inline operator fun RectF.component3(): Float = this.right

/**
 * Returns "bottom", the fourth component of the rectangle.
 *
 * This method allows to use destructuring declarations when working with rectangles,
 * for example:
 * ```
 * val (left, top, right, bottom) = myRectangle
 * ```
 */
inline operator fun RectF.component4(): Float = this.bottom

/**
 * Performs the union of this rectangle and the specified rectangle and returns
 * the result as a new rectangle.
 */
inline operator fun Rect.plus(r: Rect): Rect {
    return Rect(this).apply {
        union(r)
    }
}

/**
 * Performs the union of this rectangle and the specified rectangle.
 */
inline operator fun Rect.plusAssign(r: Rect): Unit = union(r)

/**
 * Performs the union of this rectangle and the specified rectangle and returns
 * the result as a new rectangle.
 */
inline operator fun RectF.plus(r: RectF): RectF {
    return RectF(this).apply {
        union(r)
    }
}

/**
 * Performs the union of this rectangle and the specified rectangle.
 */
inline operator fun RectF.plusAssign(r: RectF): Unit = union(r)

/**
 * Returns a new rectangle representing this rectangle offset by the specified
 * amount on both X and Y axis.
 */
inline operator fun Rect.plus(xy: Int): Rect {
    return Rect(this).apply {
        offset(xy, xy)
    }
}

/**
 * Offsets this rectangle by the specified amount on both X and Y axis.
 */
inline operator fun Rect.plusAssign(xy: Int): Unit = offset(xy, xy)

/**
 * Returns a new rectangle representing this rectangle offset by the specified
 * amount on both X and Y axis.
 */
inline operator fun RectF.plus(xy: Float): RectF {
    return RectF(this).apply {
        offset(xy, xy)
    }
}

/**
 * Offsets this rectangle by the specified amount on both X and Y axis.
 */
inline operator fun RectF.plusAssign(xy: Float): Unit = offset(xy, xy)

/**
 * Returns a new rectangle representing this rectangle offset by the specified
 * point.
 */
inline operator fun Rect.plus(xy: Point): Rect {
    return Rect(this).apply {
        offset(xy.x, xy.y)
    }
}

/**
 * Returns a new rectangle representing this rectangle offset by the specified
 * point.
 */
inline operator fun RectF.plus(xy: PointF): RectF {
    return RectF(this).apply {
        offset(xy.x, xy.y)
    }
}

/**
 * Returns a new rectangle representing this rectangle offset by the negation
 * of the specified amount on both X and Y axis.
 */
inline operator fun Rect.minus(xy: Int): Rect {
    return Rect(this).apply {
        offset(-xy, -xy)
    }
}

/**
 * Offsets this rectangle by the specified amount on both X and Y axis.
 */
inline operator fun Rect.minusAssign(xy: Int): Unit = offset(-xy, -xy)

/**
 * Returns a new rectangle representing this rectangle offset by the negation
 * of the specified amount on both X and Y axis.
 */
inline operator fun RectF.minus(xy: Float): RectF {
    return RectF(this).apply {
        offset(-xy, -xy)
    }
}

/**
 * Offsets this rectangle by the specified amount on both X and Y axis.
 */
inline operator fun RectF.minusAssign(xy: Float): Unit = offset(-xy, -xy)

/**
 * Returns a new rectangle representing this rectangle offset by the negation of
 * the specified point.
 */
inline operator fun Rect.minus(xy: Point): Rect {
    return Rect(this).apply {
        offset(-xy.x, -xy.y)
    }
}

/**
 * Returns a new rectangle representing this rectangle offset by the negation of
 * the specified point.
 */
inline operator fun RectF.minus(xy: PointF): RectF {
    return RectF(this).apply {
        offset(-xy.x, -xy.y)
    }
}

/**
 * Returns a new rectangle representing this rectangle's components each scaled by [factor].
 */
inline operator fun Rect.times(factor: Int): Rect {
    return Rect(this).apply {
        top *= factor
        left *= factor
        right *= factor
        bottom *= factor
    }
}

/**
 * Scales rectangle's components by [factor].
 */
inline operator fun Rect.timesAssign(factor: Int) {
    top *= factor
    left *= factor
    right *= factor
    bottom *= factor
}

/**
 * Returns a new rectangle representing this rectangle's components each scaled by [factor].
 */
inline operator fun RectF.times(factor: Int): RectF = times(factor.toFloat())

/**
 * Returns a new rectangle representing this rectangle's components each scaled by [factor].
 */
inline operator fun RectF.times(factor: Float): RectF {
    return RectF(this).apply {
        top *= factor
        left *= factor
        right *= factor
        bottom *= factor
    }
}

/**
 * Scales rectangle's components by [factor].
 */
inline operator fun RectF.timesAssign(factor: Int): Unit = timesAssign(factor.toFloat())

/**
 * Scales rectangle's components by [factor].
 */
inline operator fun RectF.timesAssign(factor: Float) {
    top *= factor
    left *= factor
    right *= factor
    bottom *= factor
}

/**
 * Returns the union of two rectangles as a new rectangle.
 */
inline infix fun Rect.or(r: Rect): Rect = this + r

/**
 * Returns the union of two rectangles as a new rectangle.
 */
inline infix fun RectF.or(r: RectF): RectF = this + r

/**
 * Returns the intersection of two rectangles as a new rectangle.
 * If the rectangles do not intersect, returns a copy of the left hand side
 * rectangle.
 */
inline infix fun Rect.and(r: Rect): Rect {
    return Rect(this).apply {
        intersect(r)
    }
}

/**
 * Returns the intersection of two rectangles as a new rectangle.
 * If the rectangles do not intersect, returns a copy of the left hand side
 * rectangle.
 */
inline infix fun RectF.and(r: RectF): RectF {
    return RectF(this).apply {
        intersect(r)
    }
}