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

package icyllis.modernui.graphics;

import icyllis.modernui.math.Matrix4;
import icyllis.modernui.math.Rect;
import icyllis.modernui.math.RectF;
import icyllis.modernui.text.MeasuredText;
import icyllis.modernui.text.TextPaint;

import javax.annotation.Nonnull;

/**
 * A canvas is used to draw contents for View, using shaders, such as
 * rectangles, round rectangles, circular arcs, lines, images etc.
 * <p>
 * A canvas contains a matrix/clip stack, build vertex/uniform buffers
 * and record draw commands for each primitives. It also controls the
 * color/stencil buffer. However, where to draw is undefined. For GUI,
 * all the contents are eventually drawn to the UI framebuffer.
 * <p>
 * Note: there's no depth buffer in the UI framebuffer so depth test
 * appears to be disabled. All layers are considered translucent and
 * drawn from far to near, you cannot handle the z-order manually.
 *
 * @since Core 1.6
 */
public abstract class Canvas {

    /**
     * Bit flags representing the sides.
     */
    public static final int TOP = 1;
    public static final int BOTTOM = 1 << 1;
    public static final int LEFT = 1 << 2;
    public static final int RIGHT = 1 << 3;

    /**
     * You cannot create just a base Canvas since there's no software rendering.
     */
    protected Canvas() {
    }

    /**
     * Saves the current matrix and clip onto a private stack.
     * <p>
     * Subsequent calls to translate,scale,rotate,skew,concat or clipRect,
     * clipPath will all operate as usual, but when the balancing call to
     * restore() is made, those calls will be forgotten, and the settings that
     * existed before the save() will be reinstated.
     *
     * @return The value to pass to restoreToCount() to balance this save()
     */
    public abstract int save();

    /**
     * This call balances a previous call to save(), and is used to remove all
     * modifications to the matrix/clip state since the last save call. It is
     * an error to call restore() more or less times than save() was called in
     * the final state.
     * <p>
     * If current clip doesn't change, it won't generate overhead for modifying
     * the stencil buffer.
     */
    public abstract void restore();

    /**
     * Returns the number of matrix/clip states on the Canvas' private stack.
     * This will equal # save() calls - # restore() calls. The initial and
     * minimum value are 1.
     */
    public abstract int getSaveCount();

    /**
     * Efficient way to pop any calls to save() that happened after the save
     * count reached saveCount. It is an error for saveCount to be less than 1.
     * <p>
     * Example:
     * <pre>
     * int count = canvas.save();
     * ... // more calls potentially to save()
     * canvas.restoreToCount(count);
     * // now the canvas is back in the same state it
     * // was before the initial call to save().
     * </pre>
     *
     * @param saveCount The save level to restore to.
     */
    public abstract void restoreToCount(int saveCount);

    /**
     * Premultiply the current matrix by the specified translation
     *
     * @param dx The distance to translate in X
     * @param dy The distance to translate in Y
     */
    public abstract void translate(float dx, float dy);

    /**
     * Premultiply the current matrix by the specified scale.
     *
     * @param sx The amount to scale in X
     * @param sy The amount to scale in Y
     */
    public abstract void scale(float sx, float sy);

    /**
     * Premultiply the current matrix by the specified scale.
     *
     * @param sx The amount to scale in X
     * @param sy The amount to scale in Y
     * @param px The x-coord for the pivot point (unchanged by the scale)
     * @param py The y-coord for the pivot point (unchanged by the scale)
     */
    public final void scale(float sx, float sy, float px, float py) {
        if (sx == 1.0f && sy == 1.0f) return;
        translate(px, py);
        scale(sx, sy);
        translate(-px, -py);
    }

    /**
     * Premultiply the current matrix by the specified rotation.
     *
     * @param degrees The angle to rotate, in degrees
     */
    public abstract void rotate(float degrees);

    /**
     * Rotate canvas clockwise around the pivot point with specified angle, this is
     * equivalent to premultiply the current matrix by the specified rotation.
     *
     * @param degrees The amount to rotate, in degrees
     * @param px      The x-coord for the pivot point (unchanged by the rotation)
     * @param py      The y-coord for the pivot point (unchanged by the rotation)
     */
    public final void rotate(float degrees, float px, float py) {
        if (degrees == 0.0f) return;
        translate(px, py);
        rotate(degrees);
        translate(-px, -py);
    }

    /**
     * Premultiply the current matrix by the specified matrix.
     *
     * @param matrix the matrix to multiply
     */
    public abstract void multiply(@Nonnull Matrix4 matrix);

    /**
     * Intersect the current clip with the specified rectangle and updates
     * the stencil buffer if changed, which is expressed in local coordinates.
     * The clip bounds cannot be expanded unless restore() is called.
     *
     * @param rect The rectangle to intersect with the current clip.
     * @return true if the resulting clip is non-empty, otherwise further
     * drawing will be always quick rejected until restore() is called
     */
    public boolean clipRect(@Nonnull Rect rect) {
        return clipRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    /**
     * Intersect the current clip with the specified rectangle and updates
     * the stencil buffer if changed, which is expressed in local coordinates.
     * The clip bounds cannot be expanded unless restore() is called.
     *
     * @param rect The rectangle to intersect with the current clip.
     * @return true if the resulting clip is non-empty, otherwise further
     * drawing will be always quick rejected until restore() is called
     */
    public boolean clipRect(@Nonnull RectF rect) {
        return clipRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    /**
     * Intersect the current clip with the specified rectangle and updates
     * the stencil buffer if changed, which is expressed in local coordinates.
     * The clip bounds cannot be expanded unless restore() is called.
     *
     * @param left   The left side of the rectangle to intersect with the
     *               current clip
     * @param top    The top of the rectangle to intersect with the current clip
     * @param right  The right side of the rectangle to intersect with the
     *               current clip
     * @param bottom The bottom of the rectangle to intersect with the current
     *               clip
     * @return true if the resulting clip is non-empty, otherwise further
     * drawing will be always quick rejected until restore() is called
     */
    public abstract boolean clipRect(float left, float top, float right, float bottom);

    /**
     * Return true if the specified rectangle, after being transformed by the
     * current matrix, would lie completely outside of the current clip. Call
     * this to check if an area you intend to draw into is clipped out (and
     * therefore you can skip making the draw calls). Note that all drawing
     * methods call this method by default.
     *
     * @param rect the rect to compare with the current clip
     * @return true if the given rect (transformed by the canvas' matrix)
     * intersecting with the maximum rect representing the canvas' clip is empty
     */
    public boolean quickReject(@Nonnull RectF rect) {
        return quickReject(rect.left, rect.top, rect.right, rect.bottom);
    }

    /**
     * Return true if the specified rectangle, after being transformed by the
     * current matrix, would lie completely outside of the current clip. Call
     * this to check if an area you intend to draw into is clipped out (and
     * therefore you can skip making the draw calls). Note that all drawing
     * methods call this method by default.
     *
     * @param left   The left side of the rectangle to compare with the
     *               current clip
     * @param top    The top of the rectangle to compare with the current
     *               clip
     * @param right  The right side of the rectangle to compare with the
     *               current clip
     * @param bottom The bottom of the rectangle to compare with the
     *               current clip
     * @return true if the given rect (transformed by the canvas' matrix)
     * intersecting with the maximum rect representing the canvas' clip is empty
     */
    public abstract boolean quickReject(float left, float top, float right, float bottom);

    /**
     * Draw a circular arc.
     * <p>
     * If the start angle is negative or >= 360, the start angle is treated as start angle modulo
     * 360. If the sweep angle is >= 360, then the circle is drawn completely. If the sweep angle is
     * negative, the sweep angle is treated as sweep angle modulo 360
     * <p>
     * The arc is drawn clockwise. An angle of 0 degrees correspond to the geometric angle of 0
     * degrees (3 o'clock on a watch.)
     *
     * @param cx         The x-coordinate of the center of the arc to be drawn
     * @param cy         The y-coordinate of the center of the arc to be drawn
     * @param radius     The radius of the circular arc to be drawn
     * @param startAngle Starting angle (in degrees) where the arc begins
     * @param sweepAngle Sweep angle (in degrees) measured clockwise
     * @param paint      The paint used to draw the arc
     */
    public abstract void drawArc(float cx, float cy, float radius, float startAngle,
                                 float sweepAngle, @Nonnull Paint paint);

    /**
     * Draw the specified circle using the specified paint. If radius is <= 0, then nothing will be
     * drawn. The circle will be filled or framed based on the Style in the paint.
     *
     * @param cx     The x-coordinate of the center of the circle to be drawn
     * @param cy     The y-coordinate of the center of the circle to be drawn
     * @param radius The radius of the circle to be drawn
     * @param paint  The paint used to draw the circle
     */
    public abstract void drawCircle(float cx, float cy, float radius, @Nonnull Paint paint);

    /**
     * Draw the specified Rect using the specified Paint. The rectangle will be filled or framed
     * based on the Style in the paint. The smooth radius is ignored in the paint.
     *
     * @param r     The rectangle to be drawn.
     * @param paint The paint used to draw the rectangle
     */
    public void drawRect(@Nonnull Rect r, @Nonnull Paint paint) {
        drawRect(r.left, r.top, r.right, r.bottom, paint);
    }

    /**
     * Draw the specified Rect using the specified Paint. The rectangle will be filled or framed
     * based on the Style in the paint. The smooth radius is ignored in the paint.
     *
     * @param r     The rectangle to be drawn.
     * @param paint The paint used to draw the rectangle
     */
    public void drawRect(@Nonnull RectF r, @Nonnull Paint paint) {
        drawRect(r.left, r.top, r.right, r.bottom, paint);
    }

    /**
     * Draw the specified Rect using the specified paint. The rectangle will be filled or framed
     * based on the Style in the paint. The smooth radius is ignored in the paint.
     *
     * @param left   The left side of the rectangle to be drawn
     * @param top    The top side of the rectangle to be drawn
     * @param right  The right side of the rectangle to be drawn
     * @param bottom The bottom side of the rectangle to be drawn
     * @param paint  The paint used to draw the rect
     */
    public abstract void drawRect(float left, float top, float right, float bottom, @Nonnull Paint paint);

    /**
     * Draw the specified image with its top/left corner at (x,y), using the
     * specified paint, transformed by the current matrix. The Style and smooth
     * radius is ignored in the paint, images are always filled.
     *
     * @param image the image to be drawn
     * @param left  the position of the left side of the image being drawn
     * @param top   the position of the top side of the image being drawn
     * @param paint the paint used to draw the round image
     */
    public abstract void drawImage(@Nonnull Image image, float left, float top, @Nonnull Paint paint);

    /**
     * Draw a line segment with the specified start and stop x,y coordinates, using
     * the specified paint. The Style is ignored in the paint, lines are always "framed".
     * Stroke width in the paint represents the width of the line.
     * <p>
     * Actually, a line is drawn as a filled round rectangle, rotated around the
     * midpoint of the line. So it's a bit heavy to draw.
     *
     * @param startX The x-coordinate of the start point of the line
     * @param startY The y-coordinate of the start point of the line
     * @param stopX  The x-coordinate of the stop point of the line
     * @param stopY  The y-coordinate of the stop point of the line
     * @param paint  The paint used to draw the line
     */
    public abstract void drawLine(float startX, float startY, float stopX, float stopY,
                                  @Nonnull Paint paint);

    /**
     * Draw a series of lines. Each line is taken from 4 consecutive values in the pts array. Thus
     * to draw 1 line, the array must contain at least 4 values. This is logically the same as
     * drawing the array as follows: drawLine(pts[0], pts[1], pts[2], pts[3]) followed by
     * drawLine(pts[4], pts[5], pts[6], pts[7]) and so on.
     *
     * @param pts    The array of points of the lines to draw [x0 y0 x1 y1 x2 y2 ...]
     * @param offset Number of values in the array to skip before drawing.
     * @param count  The number of values in the array to process, after skipping "offset" of them.
     *               Since each line uses 4 values, the number of "lines" that are drawn is really
     *               (count >> 2).
     * @param paint  The paint used to draw the lines
     */
    public void drawLines(@Nonnull float[] pts, int offset, int count, @Nonnull Paint paint) {
        if (offset + count > pts.length) {
            throw new IllegalArgumentException();
        }
        count >>= 2;
        for (int i = 0; i < count; i++) {
            drawLine(pts[offset++], pts[offset++], pts[offset++], pts[offset++], paint);
        }
    }

    /**
     * A helper version of {@link #drawLines(float[], int, int, Paint)}, with its offset is 0
     * and count is the length of the pts array.
     *
     * @param pts   The array of points of the lines to draw [x0 y0 x1 y1 x2 y2 ...]
     * @param paint The paint used to draw the lines
     * @see #drawLines(float[], int, int, Paint)
     */
    public final void drawLines(@Nonnull float[] pts, @Nonnull Paint paint) {
        drawLines(pts, 0, pts.length, paint);
    }

    /**
     * Draw a series of lines. The first line is taken from 4 consecutive values in the pts
     * array, and each remaining line is taken from last 2 values and next 2 values in the array.
     * Thus to draw 1 line, the array must contain at least 4 values. This is logically the same as
     * drawing the array as follows: drawLine(pts[0], pts[1], pts[2], pts[3]) followed by
     * drawLine(pts[2], pts[3], pts[4], pts[5]) and so on.
     *
     * @param pts    The array of points of the lines to draw [x0 y0 x1 y1 x2 y2 ...]
     * @param offset Number of values in the array to skip before drawing.
     * @param count  The number of values in the array to process, after skipping "offset" of them.
     *               Since each line uses 4 values, the number of "lines" that are drawn is really
     *               ((count - 2) >> 1).
     * @param paint  The paint used to draw the lines
     */
    public void drawStripLines(@Nonnull float[] pts, int offset, int count, @Nonnull Paint paint) {
        if (offset + count > pts.length) {
            throw new IllegalArgumentException();
        }
        if (count < 4) {
            return;
        }
        float x, y;
        drawLine(pts[offset++], pts[offset++], x = pts[offset++], y = pts[offset++], paint);
        count = (count - 4) >> 1;
        for (int i = 0; i < count; i++) {
            drawLine(x, y, x = pts[offset++], y = pts[offset++], paint);
        }
    }

    /**
     * A helper version of {@link #drawStripLines(float[], int, int, Paint)}, with its offset is 0
     * and count is the length of the pts array.
     *
     * @param pts   The array of points of the lines to draw [x0 y0 x1 y1 x2 y2 ...]
     * @param paint The paint used to draw the lines
     * @see #drawStripLines(float[], int, int, Paint)
     */
    public final void drawStripLines(@Nonnull float[] pts, @Nonnull Paint paint) {
        drawStripLines(pts, 0, pts.length, paint);
    }

    /**
     * Draw a rectangle with rounded corners within a rectangular bounds. The round
     * rectangle will be filled or framed based on the Style in the paint.
     *
     * @param rect   The rectangular bounds of the round rect to be drawn
     * @param radius the radius used to round the corners
     * @param paint  the paint used to draw the round rectangle
     */
    public void drawRoundRect(@Nonnull RectF rect, float radius, @Nonnull Paint paint) {
        drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, radius, 0, paint);
    }

    /**
     * Draw a rectangle with rounded corners within a rectangular bounds. The round
     * rectangle will be filled or framed based on the Style in the paint.
     *
     * @param rect   The rectangular bounds of the round rect to be drawn
     * @param radius the radius used to round the corners
     * @param side   the side to round, accepted values are 0 (all sides),
     *               or one of {@link #TOP}, {@link #BOTTOM}, {@link #LEFT},
     *               {@link #RIGHT}, or any combination of two adjacent sides
     * @param paint  the paint used to draw the round rectangle
     */
    public void drawRoundRect(@Nonnull RectF rect, float radius, int side, @Nonnull Paint paint) {
        drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, radius, side, paint);
    }

    /**
     * Draw a rectangle with rounded corners within a rectangular bounds. The round
     * rectangle will be filled or framed based on the Style in the paint.
     *
     * @param left   the left of the rectangular bounds
     * @param top    the top of the rectangular bounds
     * @param right  the right of the rectangular bounds
     * @param bottom the bottom of the rectangular bounds
     * @param radius the radius used to round the corners
     * @param paint  the paint used to draw the round rectangle
     */
    public void drawRoundRect(float left, float top, float right, float bottom,
                              float radius, @Nonnull Paint paint) {
        drawRoundRect(left, top, right, bottom, radius, 0, paint);
    }

    /**
     * Draw a rectangle with rounded corners within a rectangular bounds. The round
     * rectangle will be filled or framed based on the Style in the paint.
     *
     * @param left   the left of the rectangular bounds
     * @param top    the top of the rectangular bounds
     * @param right  the right of the rectangular bounds
     * @param bottom the bottom of the rectangular bounds
     * @param radius the radius used to round the corners
     * @param side   the side to round, accepted values are 0 (all sides),
     *               or one of {@link #TOP}, {@link #BOTTOM}, {@link #LEFT},
     *               {@link #RIGHT}, or any combination of two adjacent sides
     * @param paint  the paint used to draw the round rectangle
     */
    public abstract void drawRoundRect(float left, float top, float right, float bottom,
                                       float radius, int side, @Nonnull Paint paint);

    /**
     * Draw the specified image with rounded corners, whose top/left corner at (x,y)
     * using the specified paint, transformed by the current matrix. The Style is
     * ignored in the paint, images are always filled.
     *
     * @param image  the image to be drawn
     * @param left   the position of the left side of the image being drawn
     * @param top    the position of the top side of the image being drawn
     * @param radius the radius used to round the corners
     * @param paint  the paint used to draw the round image
     */
    public abstract void drawRoundImage(@Nonnull Image image, float left, float top,
                                        float radius, @Nonnull Paint paint);

    /**
     * Draw a run of text.
     *
     * @param text  the text to render
     * @param start context start of the text for shaping and rendering
     * @param end   context end of the text for shaping and rendering
     * @param x     the horizontal position at which to draw the text between runs inside a line
     * @param y     the vertical baseline of the line of text
     * @param isRtl whether the run is in right-to-left direction
     * @param paint the paint used to draw the text
     */
    @Deprecated
    public abstract void drawTextRun(@Nonnull CharSequence text, int start, int end,
                                     float x, float y, boolean isRtl, @Nonnull TextPaint paint);

    /**
     * Draw a run of text. The given range cannot excess a style run or break grapheme cluster.
     * If you don't know what a text run is, do not call this method directly.
     *
     * @param text  the text to draw, which has been measured and computed glyph layout
     * @param start context start of the text for shaping and rendering
     * @param end   context end of the text for shaping and rendering
     * @param x     the horizontal position at which to draw the text between runs inside a line
     * @param y     the vertical baseline of the line of text
     * @param paint the paint used to draw the text
     */
    public abstract void drawTextRun(@Nonnull MeasuredText text, int start, int end,
                                     float x, float y, @Nonnull TextPaint paint);
}
