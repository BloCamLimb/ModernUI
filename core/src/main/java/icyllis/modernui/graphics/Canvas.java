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

import icyllis.arc3d.core.*;
import icyllis.modernui.annotation.*;
import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.graphics.text.*;
import icyllis.modernui.graphics.text.Font;
import icyllis.modernui.view.Gravity;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;

import java.nio.*;

/**
 * A Canvas provides an interface for drawing 2D geometries, images, and how the
 * drawing is clipped and transformed. 2D geometries may include points, lines,
 * triangles, rectangles, rounded rectangles, circular arcs, quadratic curves, etc.
 * <p>
 * A Canvas and a Paint together provide the state to draw into Surface or Device.
 * Each Canvas draw call transforms the geometry of the object by the pre-multiplication
 * of all transform matrices in the stack. The transformed geometry is clipped by
 * the intersection of all clip values in the stack. The Canvas draw calls use Paint
 * to supply drawing state such as color, text size, stroke width, filter and so on.
 * <p>
 * A surface canvas will quickly compute, optimize and generate the data required by
 * the underlying 3D graphics API on the application side (client rendering pipeline).
 * The render tasks will be transmitted to GPU on render thread (deferred rendering),
 * determined by the drawing device.
 * <p>
 * A recording canvas records draw calls to a surface canvas. Although deferred
 * rendering has been done in surface canvas, sometimes we don't need to update
 * animations or transformations on the client side, so we can save draw calls, and
 * don't need to update the buffer in GPU. This is not recommended for frequently
 * updated scenes.
 * <p>
 * The Canvas uses analytic geometry to draw geometry, instead of meshing or
 * tessellation. This will produce very high quality rendering results (analytical
 * solution rather than approximate solution), but requires GPU to solve cubic
 * equations for quadratic curves.
 * <p>
 * The Canvas supports multiple color buffers in one off-screen rendering target,
 * which can be used for many complex color blending and temporary operations.
 * Note that depth buffer and depth test is not enabled, Z ordering or transparency
 * sorting is required on the client pipeline before drawing onto the Canvas. All
 * layers are considered translucent and drawn from far to near.
 * <p>
 * For tree structures, each child canvas may use its transition layers for rendering.
 * These transition layers are designed for short-time alpha animation, and it avoids
 * creating a large number framebuffers.
 * <p>
 * The root canvas and device is backed by the main render target (a custom framebuffer).
 * Multisampling anti-aliasing (MSAA) should be always enabled.
 * <p>
 * The projection matrix is globally shared, and the one of surface canvas must be
 * an orthographic projection matrix. Canvas's matrix stack is the local model view
 * matrix on client side, it should be an affine transformation. The pipeline will
 * calculate the world coordinates in advance.
 * <p>
 * This API is stable.
 *
 * @author BloCamLimb
 * @since 1.6
 */
public abstract class Canvas {

    public static final Marker MARKER = MarkerManager.getMarker("Canvas");

    protected Canvas() {
    }

    /**
     * Saves the current matrix and clip onto a private stack.
     * <p>
     * Subsequent calls to translate, scale, rotate, shear, concat or clipRect,
     * clipPath will all operate as usual, but when the balancing call to
     * restore() is made, those calls will be forgotten, and the settings that
     * existed before the save() will be reinstated.
     * <p>
     * Saved Canvas state is put on a stack; multiple calls to save() should be balance
     * by an equal number of calls to restore(). Call restoreToCount() with the return
     * value of this method to restore this and subsequent saves.
     *
     * @return depth of saved stack to pass to restoreToCount() to balance this call
     */
    public abstract int save();

    /**
     * This behaves the same as {@link #save()}, but in addition it allocates and
     * redirects drawing to an offscreen rendering target.
     * <p class="note"><strong>Note:</strong> this method is very expensive,
     * incurring more than double rendering cost for contained content. Avoid
     * using this method when possible.
     * <p>
     * All drawing calls are directed to a newly allocated offscreen rendering target.
     * Only when the balancing call to restore() is made, is that offscreen
     * buffer drawn back to the current target of the Canvas (which can potentially be a previous
     * layer if these calls are nested).
     *
     * @param bounds May be null. The maximum size the offscreen render target
     *               needs to be (in local coordinates)
     * @param alpha  The alpha to apply to the offscreen when it is
     *               drawn during restore()
     * @return value to pass to restoreToCount() to balance this call
     * @deprecated this method does nothing, you should manually create a layer, or possibly manage a
     * pool of layers, or use {@link icyllis.modernui.view.View#LAYER_TYPE_HARDWARE}.
     */
    @Deprecated
    public final int saveLayer(@Nullable RectF bounds, int alpha) {
        if (bounds == null) {
            // ok, it's big enough
            return saveLayer(0, 0, 0x8000, 0x8000, alpha);
        } else {
            return saveLayer(bounds.left, bounds.top, bounds.right, bounds.bottom, alpha);
        }
    }

    /**
     * Convenience for {@link #saveLayer(RectF, int)} that takes the four float coordinates of the
     * bounds' rectangle.
     *
     * @see #saveLayer(RectF, int)
     * @deprecated this method does nothing, you should manually create a layer, or possibly manage a
     * pool of layers, or use {@link icyllis.modernui.view.View#LAYER_TYPE_HARDWARE}.
     */
    @Deprecated
    public abstract int saveLayer(float left, float top, float right, float bottom, int alpha);

    /**
     * This call balances a previous call to save(), and is used to remove all
     * modifications to the matrix/clip state since the last save call. The
     * state is removed from the stack. It is an error to call restore() more
     * or less times than save() was called in the final state.
     */
    public abstract void restore();

    /**
     * Returns the depth of saved matrix/clip states on the Canvas' private stack.
     * This will equal save() calls minus restore() calls, and the number of save()
     * calls less the number of restore() calls plus one. The save count of a new
     * canvas is one.
     *
     * @return depth of save state stack
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
     * @param saveCount the depth of state stack to restore
     * @throws IllegalStateException stack underflow (i.e. saveCount is less than 1)
     */
    public abstract void restoreToCount(int saveCount);

    /**
     * Translates the current matrix by dx along the x-axis and dy along the y-axis.
     * Mathematically, pre-multiply the current matrix with a translation matrix.
     * <p>
     * This has the effect of moving the drawing by (dx, dy) before transforming
     * the result with the current matrix.
     *
     * @param dx the distance to translate on x-axis
     * @param dy the distance to translate on y-axis
     */
    public abstract void translate(float dx, float dy);

    /**
     * Translates the current matrix by dx along the x-axis, dy along the y-axis,
     * and dz along the z-axis. Mathematically, pre-multiply the current matrix with
     * a translation matrix.
     * <p>
     * This has the effect of moving the drawing by (dx, dy, dz) before transforming
     * the result with the current matrix.
     *
     * @param dx the distance to translate on x-axis
     * @param dy the distance to translate on y-axis
     * @param dz the distance to translate on z-axis
     */
    public abstract void translate(float dx, float dy, float dz);

    /**
     * Scales the current matrix by sx on the x-axis and sy on the y-axis.
     * Mathematically, pre-multiply the current matrix with a scaling matrix.
     * <p>
     * This has the effect of scaling the drawing by (sx, sy) before transforming
     * the result with the current matrix.
     *
     * @param sx the amount to scale on x-axis
     * @param sy the amount to scale on y-axis
     */
    public abstract void scale(float sx, float sy);

    /**
     * Scales the current matrix by sx on the x-axis, sy on the y-axis, and
     * sz on the z-axis. Mathematically, pre-multiply the current matrix with a
     * scaling matrix.
     * <p>
     * This has the effect of scaling the drawing by (sx, sy, sz) before transforming
     * the result with the current matrix.
     *
     * @param sx the amount to scale on x-axis
     * @param sy the amount to scale on y-axis
     * @param sz the amount to scale on z-axis
     */
    public abstract void scale(float sx, float sy, float sz);

    /**
     * Scales the current matrix by sx on the x-axis and sy on the y-axis at (px, py).
     * Mathematically, pre-multiply the current matrix with a translation matrix;
     * pre-multiply the current matrix with a scaling matrix; then pre-multiply the
     * current matrix with a negative translation matrix;
     * <p>
     * This has the effect of scaling the drawing by (sx, sy) before transforming
     * the result with the current matrix.
     *
     * @param sx the amount to scale on x-axis
     * @param sy the amount to scale on y-axis
     * @param px the x-coord for the pivot point (unchanged by the scale)
     * @param py the y-coord for the pivot point (unchanged by the scale)
     */
    public abstract void scale(float sx, float sy, float px, float py);

    /**
     * Rotates the current matrix by degrees clockwise about the Z axis.
     * Mathematically, pre-multiply the current matrix with a rotation matrix;
     * <p>
     * This has the effect of rotating the drawing by degrees before transforming
     * the result with the current matrix.
     *
     * @param degrees the amount to rotate, in degrees
     */
    public abstract void rotate(float degrees);

    /**
     * Rotates the current matrix by degrees clockwise about the Z axis at (px, py).
     * Mathematically, pre-multiply the current matrix with a translation matrix;
     * pre-multiply the current matrix with a rotation matrix; then pre-multiply the
     * current matrix with a negative translation matrix;
     * <p>
     * This has the effect of rotating the drawing by degrees before transforming
     * the result with the current matrix.
     *
     * @param degrees the amount to rotate, in degrees
     * @param px      the x-coord for the pivot point (unchanged by the rotation)
     * @param py      the y-coord for the pivot point (unchanged by the rotation)
     */
    public abstract void rotate(float degrees, float px, float py);

    /**
     * Pre-multiply the current matrix by the specified skew.
     * This method is equivalent to calling {@link #shear}.
     *
     * @param sx The amount to skew in X
     * @param sy The amount to skew in Y
     */
    public final void skew(float sx, float sy) {
        shear(sx, sy);
    }

    /**
     * Pre-multiply the current matrix by the specified skew.
     * This method is equivalent to calling {@link #shear}.
     *
     * @param sx The amount to skew in X
     * @param sy The amount to skew in Y
     * @param px The x-coord for the pivot point (unchanged by the skew)
     * @param py The y-coord for the pivot point (unchanged by the skew)
     */
    public final void skew(float sx, float sy, float px, float py) {
        shear(sx, sy, px, py);
    }

    /**
     * Shears the current matrix by sx on the x-axis and sy on the y-axis. A positive value
     * of sx shears the drawing right as y-axis values increase; a positive value of sy shears
     * the drawing down as x-axis values increase.
     * <p>
     * Mathematically, pre-multiply the current matrix with a shearing matrix.
     * <p>
     * This has the effect of shearing the drawing by (sx, sy) before transforming
     * the result with the current matrix.
     *
     * @param sx the amount to shear on x-axis
     * @param sy the amount to shear on y-axis
     */
    public abstract void shear(float sx, float sy);

    /**
     * Pre-multiply the current matrix by the specified shearing.
     *
     * @param sx the x-component of the shearing, y is unchanged
     * @param sy the y-component of the shearing, x is unchanged
     * @param px the x-component of the pivot (unchanged by the shear)
     * @param py the y-component of the pivot (unchanged by the shear)
     */
    public abstract void shear(float sx, float sy, float px, float py);

    /**
     * Pre-multiply the current matrix by the specified matrix.
     * <p>
     * This has the effect of transforming the drawn geometry by matrix, before
     * transforming the result with the current matrix.
     *
     * @param matrix the matrix to premultiply with the current matrix
     */
    public abstract void concat(@NonNull Matrix matrix);

    /**
     * Pre-multiply the current matrix by the specified matrix.
     * <p>
     * This has the effect of transforming the drawn geometry by matrix, before
     * transforming the result with the current matrix.
     *
     * @param matrix the matrix to premultiply with the current matrix
     */
    @ApiStatus.Experimental
    public abstract void concat(@NonNull Matrix4 matrix);

    /**
     * @return current model view matrix
     * @hidden
     * @deprecated internal use only
     */
    @Deprecated
    @ApiStatus.Internal
    @NonNull
    public Matrix4 getMatrix() {
        return new Matrix4();
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
    public final boolean clipRect(@NonNull Rect rect) {
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
    public final boolean clipRect(@NonNull RectF rect) {
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
     * Set the clip to the difference of the current clip and the specified rectangle, which is
     * expressed in local coordinates.
     *
     * @param rect The rectangle to perform a difference op with the current clip.
     * @return true if the resulting clip is non-empty
     */
    public final boolean clipOutRect(@NonNull Rect rect) {
        return clipOutRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    /**
     * Set the clip to the difference of the current clip and the specified rectangle, which is
     * expressed in local coordinates.
     *
     * @param rect The rectangle to perform a difference op with the current clip.
     * @return true if the resulting clip is non-empty
     */
    public final boolean clipOutRect(@NonNull RectF rect) {
        return clipOutRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    /**
     * Set the clip to the difference of the current clip and the specified rectangle, which is
     * expressed in local coordinates.
     *
     * @param left   The left side of the rectangle used in the difference operation
     * @param top    The top of the rectangle used in the difference operation
     * @param right  The right side of the rectangle used in the difference operation
     * @param bottom The bottom of the rectangle used in the difference operation
     * @return       true if the resulting clip is non-empty
     */
    public abstract boolean clipOutRect(float left, float top, float right, float bottom);

    /**
     * Return true if the specified rectangle, after being transformed by the
     * current matrix, would lie completely outside the current clip. Call
     * this to check if an area you intend to draw into is clipped out (and
     * therefore you can skip making the draw calls). May return false even
     * though rect is outside of clip (conservative).
     * <p>
     * Note that each draw all will check this internally. This is used to
     * skip subsequent draw calls.
     *
     * @param rect the rect to compare with the current clip
     * @return true if the given rect (transformed by the canvas' matrix)
     * intersecting with the maximum rect representing the canvas' clip is empty
     */
    public final boolean quickReject(@NonNull RectF rect) {
        return quickReject(rect.left, rect.top, rect.right, rect.bottom);
    }

    /**
     * Return true if the specified rectangle, after being transformed by the
     * current matrix, would lie completely outside the current clip. Call
     * this to check if an area you intend to draw into is clipped out (and
     * therefore you can skip making the draw calls). May return false even
     * though rect is outside of clip (conservative).
     * <p>
     * Note that each draw all will check this internally. This is used to
     * skip subsequent draw calls.
     *
     * @param left   the left side of the rectangle to compare with the
     *               current clip
     * @param top    the top of the rectangle to compare with the current
     *               clip
     * @param right  the right side of the rectangle to compare with the
     *               current clip
     * @param bottom the bottom of the rectangle to compare with the
     *               current clip
     * @return true if the given rect (transformed by the canvas' matrix)
     * intersecting with the maximum rect representing the canvas' clip is empty
     */
    public abstract boolean quickReject(float left, float top, float right, float bottom);

    /**
     * Fills the current clip with the specified color, using SRC blend mode.
     * This has the effect of replacing all pixels contained by clip with color.
     *
     * @param color the straight color to draw onto the canvas
     */
    public final void clear(@ColorInt int color) {
        drawColor(color, BlendMode.SRC);
    }

    /**
     * Fill the current clip with the specified color, using SRC_OVER blend mode.
     *
     * @param color the straight color to draw onto the canvas
     */
    public final void drawColor(@ColorInt int color) {
        drawColor(color, BlendMode.SRC_OVER);
    }

    /**
     * Fill the current clip with the specified color, the blend mode determines
     * how color is combined with destination.
     *
     * @param color the straight color to draw onto the canvas
     * @param mode  the blend mode used to combine source color and destination
     */
    public void drawColor(@ColorInt int color, BlendMode mode) {
    }

    /**
     * Fills the current clip with the specified paint. Paint components, Shader,
     * ColorFilter, ImageFilter, and BlendMode affect drawing.
     * <p>
     * This is equivalent (but faster) to drawing an infinitely large rectangle
     * with the specified paint.
     *
     * @param paint the paint used to draw onto the canvas
     */
    public void drawPaint(Paint paint) {
    }

    /**
     * Draws a point centered at (x, y) using the specified paint.
     * <p>
     * The shape of point drawn depends on paint's cap. If paint is set to
     * {@link Paint#CAP_ROUND}, draw a circle of diameter paint stroke width
     * (as transformed by the canvas' matrix), with special treatment for
     * a stroke width of 0, which always draws exactly 1 pixel (or at most 4
     * if anti-aliasing is enabled). Otherwise, draw a square of width and
     * height paint stroke width. Paint's style is ignored, as if were set to
     * {@link Paint#FILL}.
     *
     * @param p     the center point of circle or square
     * @param paint the paint used to draw the point
     */
    public final void drawPoint(PointF p, Paint paint) {
        drawPoint(p.x, p.y, paint);
    }

    /**
     * Draws a point centered at (x, y) using the specified paint.
     * <p>
     * The shape of point drawn depends on paint's cap. If paint is set to
     * {@link Paint#CAP_ROUND}, draw a circle of diameter paint stroke width
     * (as transformed by the canvas' matrix), with special treatment for
     * a stroke width of 0, which always draws exactly 1 pixel (or at most 4
     * if anti-aliasing is enabled). Otherwise, draw a square of width and
     * height paint stroke width. Paint's style is ignored, as if were set to
     * {@link Paint#STROKE}.
     *
     * @param x     the center x of circle or square
     * @param y     the center y of circle or square
     * @param paint the paint used to draw the point
     */
    public void drawPoint(float x, float y, Paint paint) {
    }

    /**
     * Draw a series of points. Each point is centered at the coordinate
     * specified by pts[], and its diameter is specified by the paint's
     * stroke width (as transformed by the canvas' CTM), with special
     * treatment for a stroke width of 0, which always draws exactly 1 pixel
     * (or at most 4 if anti-aliasing is enabled). The shape of the point
     * is controlled by the paint's Cap type. The shape is a square, unless
     * the cap type is Round, in which case the shape is a circle.
     * If count is odd, the last value is ignored.
     *
     * @param pts    the array of points to draw [x0 y0 x1 y1 x2 y2 ...]
     * @param offset the number of values to skip before starting to draw.
     * @param count  the number of values to process, after skipping offset of them. Since one point
     *               uses two values, the number of "points" that are drawn is really (count >> 1).
     * @param paint  the paint used to draw the points
     */
    public void drawPoints(float[] pts, int offset, int count, Paint paint) {
        if (count < 2) {
            return;
        }
        count >>= 1;
        for (int i = 0; i < count; i++) {
            drawPoint(pts[offset++], pts[offset++], paint);
        }
    }

    /**
     * Draw a series of points. Each point is centered at the coordinate
     * specified by pts[], and its diameter is specified by the paint's
     * stroke width (as transformed by the canvas' CTM), with special
     * treatment for a stroke width of 0, which always draws exactly 1 pixel
     * (or at most 4 if anti-aliasing is enabled). The shape of the point
     * is controlled by the paint's Cap type. The shape is a square, unless
     * the cap type is Round, in which case the shape is a circle.
     * If count is odd, the last value is ignored.
     *
     * @param pts   the array of points to draw [x0 y0 x1 y1 x2 y2 ...]
     * @param paint the paint used to draw the points
     */
    public final void drawPoints(float[] pts, Paint paint) {
        drawPoints(pts, 0, pts.length, paint);
    }

    /**
     * Draw a line segment from (x0, y0) to (x1, y1) using the specified paint.
     * <p>
     * Line covers an area, and is not a stroke path in the concept of Modern UI.
     * Therefore, a line may be either "filled" (path) or "stroked" (annular, hollow),
     * depending on paint's style. Additionally, paint's cap draws the end rounded
     * or square, if filled; the other properties of paint work as intended.
     * <p>
     * See {@link #drawLinePath(float, float, float, float, Paint)} for the path version.
     * <p>
     * If thickness = 0 (also known as hairline), then this uses the mesh version.
     * See {@link #drawLineListMesh(FloatBuffer, IntBuffer, Paint)} for the mesh version.
     *
     * @param x0        the start of the line segment on x-axis
     * @param y0        the start of the line segment on y-axis
     * @param x1        the end of the line segment on x-axis
     * @param y1        the end of the line segment on y-axis
     * @param thickness the thickness of the line segment
     * @param paint     the paint used to draw the line segment
     */
    public void drawLine(float x0, float y0, float x1, float y1,
                         float thickness, @NonNull Paint paint) {
        drawLine(x0, y0, x1, y1, Paint.Cap.ROUND, thickness, paint);
    }

    /**
     * Draw a line segment from (x0, y0) to (x1, y1) using the specified paint.
     * <p>
     * Line covers an area, and is not a stroke path in the concept of Modern UI.
     * Therefore, a line may be either "filled" (path) or "stroked" (annular, hollow),
     * depending on paint's style. Additionally, paint's cap draws the end rounded
     * or square, if filled; the other properties of paint work as intended.
     * <p>
     * See {@link #drawLinePath(float, float, float, float, Paint)} for the path version.
     * <p>
     * If thickness = 0 (also known as hairline), then this uses the mesh version.
     * See {@link #drawLineListMesh(FloatBuffer, IntBuffer, Paint)} for the mesh version.
     *
     * @param x0        the start of the line segment on x-axis
     * @param y0        the start of the line segment on y-axis
     * @param x1        the end of the line segment on x-axis
     * @param y1        the end of the line segment on y-axis
     * @param thickness the thickness of the line segment
     * @param paint     the paint used to draw the line segment
     */
    public void drawLine(float x0, float y0, float x1, float y1,
                         @NonNull Paint.Cap cap, float thickness, @NonNull Paint paint) {

    }

    /**
     * Variant version of {@link #drawLine(float, float, float, float, float, Paint)}.
     *
     * @param p0        start of line segment
     * @param p1        end of line segment
     * @param thickness the thickness of the line segment
     * @param paint     the paint used to draw the line
     */
    public final void drawLine(@NonNull PointF p0, @NonNull PointF p1,
                               float thickness, @NonNull Paint paint) {
        drawLine(p0.x, p0.y, p1.x, p1.y, thickness, paint);
    }

    /**
     * Variant version of {@link #drawLine(float, float, float, float, float, Paint)}.
     * Draw a "filled" line, Paint's stroke width describes the line thickness.
     *
     * @param x0    the start of the line segment on x-axis
     * @param y0    the start of the line segment on y-axis
     * @param x1    the end of the line segment on x-axis
     * @param y1    the end of the line segment on y-axis
     * @param paint the paint used to draw the line
     */
    public void drawLine(float x0, float y0, float x1, float y1, @NonNull Paint paint) {
        var pStyle = paint.getStyle();
        paint.setStyle(Paint.FILL);
        drawLine(x0, y0, x1, y1, paint.getStrokeWidth(), paint);
        paint.setStyle(pStyle);
    }

    /**
     * Variant version of {@link #drawLine(float, float, float, float, float, Paint)}.
     * Draw a "filled" line, Paint's stroke width describes the line thickness.
     *
     * @param p0    start of line segment
     * @param p1    end of line segment
     * @param paint the paint used to draw the line
     */
    public final void drawLine(@NonNull PointF p0, @NonNull PointF p1, @NonNull Paint paint) {
        drawLine(p0.x, p0.y, p1.x, p1.y, paint);
    }

    /**
     * Draw the specified Rect using the specified paint.
     * In paint: Paint's style determines if rectangle is stroked or filled;
     * if stroked, Paint's stroke width describes the line thickness, and
     * Paint's join draws the corners rounded or square.
     *
     * @param r     the rectangle to be drawn.
     * @param paint the paint used to draw the rectangle
     */
    public final void drawRect(RectF r, Paint paint) {
        drawRect(r.left, r.top, r.right, r.bottom, paint);
    }

    /**
     * Draw the specified Rect using the specified paint.
     * In paint: Paint's style determines if rectangle is stroked or filled;
     * if stroked, Paint's stroke width describes the line thickness, and
     * Paint's join draws the corners rounded or square.
     *
     * @param r     the rectangle to be drawn.
     * @param paint the paint used to draw the rectangle
     */
    public final void drawRect(Rect r, Paint paint) {
        drawRect(r.left, r.top, r.right, r.bottom, paint);
    }

    /**
     * Draw the specified Rect using the specified paint.
     * In paint: Paint's style determines if rectangle is stroked or filled;
     * if stroked, Paint's stroke width describes the line thickness, and
     * Paint's join draws the corners rounded or square.
     *
     * @param left   the left side of the rectangle to be drawn
     * @param top    the top side of the rectangle to be drawn
     * @param right  the right side of the rectangle to be drawn
     * @param bottom the bottom side of the rectangle to be drawn
     * @param paint  the paint used to draw the rect
     */
    public abstract void drawRect(float left, float top, float right, float bottom, Paint paint);

    /**
     * Similar to {@link #drawRect(float, float, float, float, Paint)},
     * but paint's color and shader are ignored in this case. Instead, draw a bilinear gradient
     * with the four given colors, in 0xAARRGGBB format, in the sRGB color space.
     *
     * @param left    the left side of the rectangle to be drawn
     * @param top     the top side of the rectangle to be drawn
     * @param right   the right side of the rectangle to be drawn
     * @param bottom  the bottom side of the rectangle to be drawn
     * @param colorUL the color of the upper left corner
     * @param colorUR the color of the upper right corner
     * @param colorLR the color of the lower right corner
     * @param colorLL the color of the lower left corner
     * @param paint   the paint used to draw the rect
     */
    @ApiStatus.Experimental
    public abstract void drawRectGradient(float left, float top, float right, float bottom,
                                          int colorUL, int colorUR, int colorLR, int colorLL,
                                          Paint paint);

    /**
     * Draw a rectangle with rounded corners within a rectangular bounds. The round
     * rectangle will be filled or framed based on the Style in the paint.
     *
     * @param rect   The rectangular bounds of the round rect to be drawn
     * @param radius the radius used to round the corners
     * @param paint  the paint used to draw the round rectangle
     */
    public final void drawRoundRect(RectF rect, float radius, Paint paint) {
        drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, radius, Gravity.NO_GRAVITY, paint);
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
    public final void drawRoundRect(float left, float top, float right, float bottom,
                                    float radius, Paint paint) {
        drawRoundRect(left, top, right, bottom, radius, Gravity.NO_GRAVITY, paint);
    }

    /**
     * Draw a rectangle with rounded corners within a rectangular bounds. The round
     * rectangle will be filled or framed based on the Style in the paint.
     *
     * @param rect   The rectangular bounds of the round rect to be drawn
     * @param radius the radius used to round the corners
     * @param sides  the side to round, accepted values are 0 (all sides), or one of
     *               {@link Gravity#TOP}, {@link Gravity#BOTTOM},
     *               {@link Gravity#LEFT}, {@link Gravity#RIGHT}, or any
     *               combination of two adjacent sides
     * @param paint  the paint used to draw the round rectangle
     */
    public final void drawRoundRect(RectF rect, float radius, int sides, Paint paint) {
        drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, radius, sides, paint);
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
     * @param sides  the side to round, accepted values are 0 (all sides), or one of
     *               {@link Gravity#TOP}, {@link Gravity#BOTTOM},
     *               {@link Gravity#LEFT}, {@link Gravity#RIGHT}, or any
     *               combination of two adjacent sides
     * @param paint  the paint used to draw the round rectangle
     */
    public abstract void drawRoundRect(float left, float top, float right, float bottom,
                                       float radius, int sides, Paint paint);

    /**
     * Similar to {@link #drawRoundRect(float, float, float, float, float, Paint)},
     * but paint's color and shader are ignored in this case. Instead, draw a bilinear gradient
     * with the four given colors, in 0xAARRGGBB format, in the sRGB color space.
     *
     * @param left    the left of the rectangular bounds
     * @param top     the top of the rectangular bounds
     * @param right   the right of the rectangular bounds
     * @param bottom  the bottom of the rectangular bounds
     * @param colorUL the color of the upper left corner
     * @param colorUR the color of the upper right corner
     * @param colorLR the color of the lower right corner
     * @param colorLL the color of the lower left corner
     * @param radius  the radius used to round the corners
     * @param paint   the paint used to draw the round rectangle
     */
    @ApiStatus.Experimental
    public abstract void drawRoundRectGradient(float left, float top, float right, float bottom,
                                               int colorUL, int colorUR, int colorLR, int colorLL,
                                               float radius, Paint paint);

    /**
     * Draw the specified circle at (cx, cy) with radius using the specified paint.
     * If radius is zero or less, nothing is drawn.
     * In paint: Paint's style determines if circle is stroked or filled;
     * if stroked, paint's stroke width describes the line thickness.
     *
     * @param cx     the x-coordinate of the center of the circle to be drawn
     * @param cy     the y-coordinate of the center of the circle to be drawn
     * @param radius the radius of the circle to be drawn
     * @param paint  the paint used to draw the circle
     */
    public abstract void drawCircle(float cx, float cy, float radius,
                                    @NonNull Paint paint);

    /**
     * Variant of {@link #drawCircle(float, float, float, Paint)}.
     *
     * @param center the center point of the circle to be drawn
     * @param radius the radius of the circle to be drawn
     * @param paint  the paint used to draw the circle
     */
    public final void drawCircle(@NonNull PointF center, float radius,
                                 @NonNull Paint paint) {
        drawCircle(center.x, center.y, radius, paint);
    }

    /**
     * Draw a circular arc at (cx, cy) with radius using the specified paint.
     * <p>
     * If the start angle is negative or >= 360, the start angle is treated as
     * start angle modulo 360. If the sweep angle is >= 360, then the circle is
     * drawn completely. If the sweep angle is negative, the sweep angle is
     * treated as sweep angle modulo 360.
     * <p>
     * The arc is drawn clockwise. An angle of 0 degrees correspond to the
     * geometric angle of 0 degrees (3 o'clock on a watch). If radius is
     * not positive or sweep angle is zero, nothing is drawn.
     * <p>
     * Special note: Arc is a shape rather than a curved line in the concept
     * of Modern UI. Therefore, an arc may be either filled (path) or stroked
     * (annular, hollow).
     * <p>
     * The implementation is guaranteed to be analytical in Modern UI.
     *
     * @param cx         the x-coordinate of the center of the arc to be drawn
     * @param cy         the y-coordinate of the center of the arc to be drawn
     * @param radius     the radius of the circular arc to be drawn
     * @param startAngle starting angle (in degrees) where the arc begins
     * @param sweepAngle sweep angle or angular extent (in degrees); positive is clockwise
     * @param paint      the paint used to draw the arc
     * @see #drawPie(float, float, float, float, float, Paint)
     */
    public abstract void drawArc(float cx, float cy, float radius,
                                 float startAngle, float sweepAngle,
                                 @NonNull Paint paint);

    /**
     * Variant version of {@link #drawArc(float, float, float, float, float, Paint)}.
     *
     * @param center     the center point of the arc to be drawn
     * @param radius     the radius of the circular arc to be drawn
     * @param startAngle starting angle (in degrees) where the arc begins
     * @param sweepAngle sweep angle or angular extent (in degrees); positive is clockwise
     * @param paint      the paint used to draw the arc
     */
    public final void drawArc(@NonNull PointF center, float radius,
                              float startAngle, float sweepAngle,
                              @NonNull Paint paint) {
        drawArc(center.x, center.y, radius, startAngle, sweepAngle, paint);
    }

    /**
     * Draw a circular sector at (cx, cy) with radius using the specified paint.
     * <p>
     * Similar to {@link #drawArc(float, float, float, float, float, Paint)}, but
     * when the shape is not a full circle, the geometry is closed by the arc and
     * two line segments from the end of the arc to the center of the circle.
     * <p>
     * The implementation is guaranteed to be analytical in Modern UI.
     *
     * @param cx         the x-coordinate of the center of the arc to be drawn
     * @param cy         the y-coordinate of the center of the arc to be drawn
     * @param radius     the radius of the circular arc to be drawn
     * @param startAngle starting angle (in degrees) where the arc begins
     * @param sweepAngle sweep angle or angular extent (in degrees); positive is clockwise
     * @param paint      the paint used to draw the arc
     */
    public abstract void drawPie(float cx, float cy, float radius,
                                 float startAngle, float sweepAngle,
                                 @NonNull Paint paint);

    /**
     * Variant version of {@link #drawPie(float, float, float, float, float, Paint)}.
     *
     * @param center     the center point of the arc to be drawn
     * @param radius     the radius of the circular arc to be drawn
     * @param startAngle starting angle (in degrees) where the arc begins
     * @param sweepAngle sweep angle or angular extent (in degrees); positive is clockwise
     * @param paint      the paint used to draw the arc
     */
    public final void drawPie(@NonNull PointF center, float radius,
                              float startAngle, float sweepAngle,
                              @NonNull Paint paint) {
        drawPie(center.x, center.y, radius, startAngle, sweepAngle, paint);
    }

    //WIP
    public void drawArcPath(float cx, float cy, float radius,
                            float startAngle, float sweepAngle,
                            @NonNull Paint paint) {
    }

    /**
     * Draw a quadratic Bézier curve using the specified paint. The three points represent
     * the starting point, the first control point and the end control point respectively.
     * <p>
     * The style is ignored in the paint, Bézier curves are always stroked. The stroke width
     * in the paint represents the width of the curve.
     * <p>
     * Note that the distance from a point to the quadratic curve requires the GPU to solve
     * cubic equations. Therefore, this method has higher overhead to the GPU.
     *
     * @param p0    the starting point of the Bézier curve
     * @param p1    the first control point of the Bézier curve
     * @param p2    the end control point of the Bézier curve
     * @param paint the paint used to draw the Bézier curve
     */
    public final void drawBezier(PointF p0, PointF p1, PointF p2,
                                 Paint paint) {
        drawBezier(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, paint);
    }

    /**
     * Draw a quadratic Bézier curve using the specified paint. The three points represent
     * the starting point, the first control point and the end control point respectively.
     * <p>
     * The style is ignored in the paint, Bézier curves are always stroked. The stroke width
     * in the paint represents the width of the curve.
     * <p>
     * Note that the distance from a point to the quadratic curve requires the GPU to solve
     * cubic equations. Therefore, this method has higher overhead to the GPU.
     *
     * @param x0    the x-coordinate of the starting point of the Bézier curve
     * @param y0    the y-coordinate of the starting point of the Bézier curve
     * @param x1    the x-coordinate of the first control point of the Bézier curve
     * @param y1    the y-coordinate of the first control point of the Bézier curve
     * @param x2    the x-coordinate of the end control point of the Bézier curve
     * @param y2    the y-coordinate of the end control point of the Bézier curve
     * @param paint the paint used to draw the Bézier curve
     */
    public abstract void drawBezier(float x0, float y0, float x1, float y1, float x2, float y2,
                                    Paint paint);

    /**
     * Draw the specified image with its top/left corner at (x,y), using the
     * specified paint, transformed by the current matrix. The Style and smooth
     * radius is ignored in the paint, images are always filled.
     *
     * @param image the image to be drawn
     * @param left  the position of the left side of the image being drawn
     * @param top   the position of the top side of the image being drawn
     * @param paint the paint used to draw the image, null meaning a default paint
     */
    public abstract void drawImage(Image image, float left, float top, @Nullable Paint paint);

    /**
     * Draw the specified image, scaling/translating automatically to fill the destination
     * rectangle. If the source rectangle is not null, it specifies the subset of the image to
     * draw. The Style and smooth radius is ignored in the paint, images are always filled.
     *
     * @param image the image to be drawn
     * @param src   the subset of the image to be drawn, null meaning full image
     * @param dst   the rectangle that the image will be scaled/translated to fit into
     * @param paint the paint used to draw the image, null meaning a default paint
     */
    public final void drawImage(Image image, @Nullable Rect src, RectF dst, @Nullable Paint paint) {
        if (src == null) {
            drawImage(image, 0, 0, image.getWidth(), image.getHeight(),
                    dst.left, dst.top, dst.right, dst.bottom, paint);
        } else {
            drawImage(image, src.left, src.top, src.right, src.bottom,
                    dst.left, dst.top, dst.right, dst.bottom, paint);
        }
    }

    /**
     * Draw the specified image, scaling/translating automatically to fill the destination
     * rectangle. If the source rectangle is not null, it specifies the subset of the image to
     * draw. The Style and smooth radius is ignored in the paint, images are always filled.
     *
     * @param image the image to be drawn
     * @param src   the subset of the image to be drawn, null meaning full image
     * @param dst   the rectangle that the image will be scaled/translated to fit into
     * @param paint the paint used to draw the image, null meaning a default paint
     */
    public final void drawImage(Image image, @Nullable Rect src, Rect dst, @Nullable Paint paint) {
        if (src == null) {
            drawImage(image, 0, 0, image.getWidth(), image.getHeight(),
                    dst.left, dst.top, dst.right, dst.bottom, paint);
        } else {
            drawImage(image, src.left, src.top, src.right, src.bottom,
                    dst.left, dst.top, dst.right, dst.bottom, paint);
        }
    }

    /**
     * Draw the specified image, scaling/translating automatically to fill the destination
     * rectangle. If the source rectangle is not null, it specifies the subset of the image to
     * draw. The Style and smooth radius is ignored in the paint, images are always filled.
     *
     * @param image the image to be drawn
     * @param paint the paint used to draw the image, null meaning a default paint
     */
    public abstract void drawImage(Image image, float srcLeft, float srcTop, float srcRight, float srcBottom,
                                   float dstLeft, float dstTop, float dstRight, float dstBottom, @Nullable Paint paint);

    /**
     * Draw a series of round lines.
     * <p>
     * When discontinuous, each line is taken from 4 consecutive values in the pts array. Thus,
     * to draw 1 line, the array must contain at least 4 values. This is logically the same as
     * drawing the array as follows: drawLine(pts[0], pts[1], pts[2], pts[3]) followed by
     * drawLine(pts[4], pts[5], pts[6], pts[7]) and so on.
     * <p>
     * When continuous, the first line is taken from 4 consecutive values in the pts
     * array, and each remaining line is taken from last 2 values and next 2 values in the array.
     * Thus, to draw 1 line, the array must contain at least 4 values. This is logically the same as
     * drawing the array as follows: drawLine(pts[0], pts[1], pts[2], pts[3]) followed by
     * drawLine(pts[2], pts[3], pts[4], pts[5]) and so on.
     *
     * @param pts    The array of points of the lines to draw [x0 y0 x1 y1 x2 y2 ...]
     * @param offset Number of values in the array to skip before drawing.
     * @param count  The number of values in the array to process, after skipping "offset" of them.
     *               Since each line uses 4 values, the number of "lines" that are drawn is really
     *               (count >> 2) if continuous, or ((count - 2) >> 1) if discontinuous.
     * @param strip  Whether line points are continuous
     * @param paint  The paint used to draw the lines
     */
    @Deprecated
    public void drawRoundLines(float[] pts, int offset, int count, boolean strip,
                               Paint paint) {
        if ((offset | count | pts.length - offset - count) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (count < 4) {
            return;
        }
        float thick = paint.getStrokeWidth();
        if (strip) {
            float x, y;
            drawLine(pts[offset++], pts[offset++], x = pts[offset++], y = pts[offset++], Paint.Cap.ROUND, thick, paint);
            count = (count - 4) >> 1;
            for (int i = 0; i < count; i++) {
                drawLine(x, y, x = pts[offset++], y = pts[offset++], Paint.Cap.ROUND, thick, paint);
            }
        } else {
            count >>= 2;
            for (int i = 0; i < count; i++) {
                drawLine(pts[offset++], pts[offset++], pts[offset++], pts[offset++], Paint.Cap.ROUND, thick, paint);
            }
        }
    }

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
    public abstract void drawRoundImage(Image image, float left, float top,
                                        float radius, Paint paint);

    /**
     * Draw array of glyphs with specified font in order <em>visually left-to-right</em>.
     * The Paint must be the same as the one passed to any of {@link icyllis.modernui.text.TextShaper} methods.
     * All the given arrays <b>MUST BE IMMUTABLE!</b>
     *
     * @param glyphs         Array of glyph IDs. The length of array must be greater than or equal to
     *                       {@code glyphStart + glyphCount}.
     * @param glyphOffset    Number of elements to skip before drawing in <code>glyphIds</code>
     *                       array.
     * @param positions      A flattened X and Y position array. The first glyph X position must be
     *                       stored at {@code positionOffset}. The first glyph Y position must be stored
     *                       at {@code positionOffset + 1}, then the second glyph X position must be
     *                       stored at {@code positionOffset + 2}.
     *                       The length of array must be greater than or equal to
     *                       {@code positionOffset + glyphCount * 2}.
     * @param positionOffset Number of elements to skip before drawing in {@code positions}.
     *                       The first glyph X position must be stored at {@code positionOffset}.
     *                       The first glyph Y position must be stored at
     *                       {@code positionOffset + 1}, then the second glyph X position must be
     *                       stored at {@code positionOffset + 2}.
     * @param glyphCount     Number of glyphs to be drawn.
     * @param font           FontFace used for drawing.
     * @param x              Additional amount of x offset of the glyph X positions.
     * @param y              Additional amount of y offset of the glyph Y positions.
     * @param paint          Paint used for drawing.
     * @see icyllis.modernui.text.TextShaper
     * @see #drawShapedText
     */
    public abstract void drawGlyphs(@NonNull int[] glyphs,
                                    int glyphOffset,
                                    @NonNull float[] positions,
                                    int positionOffset,
                                    int glyphCount,
                                    @NonNull Font font,
                                    float x, float y,
                                    @NonNull Paint paint);

    /**
     * Draw a single style run of positioned glyphs in order <em>visually left-to-right</em>,
     * where a single style run may contain multiple BiDi runs and font runs.
     * The Paint must be the same as the one passed to any of {@link icyllis.modernui.text.TextShaper} methods.
     *
     * @param text  A sequence of positioned glyphs.
     * @param x     Additional amount of x offset of the glyph X positions, i.e. left position
     * @param y     Additional amount of y offset of the glyph Y positions, i.e. baseline position
     * @param paint Paint used for drawing.
     * @see icyllis.modernui.text.TextShaper
     */
    public final void drawShapedText(@NonNull ShapedText text,
                                     float x, float y, @NonNull Paint paint) {
        drawShapedText(text, 0, text.getGlyphCount(), x, y, paint);
    }

    /**
     * Draw a single style run of positioned glyphs in order <em>visually left-to-right</em>,
     * where a single style run may contain multiple BiDi runs and font runs.
     * The Paint must be the same as the one passed to any of {@link icyllis.modernui.text.TextShaper} methods.
     *
     * @param text       A sequence of positioned glyphs.
     * @param glyphStart Number of glyphs to skip before drawing text.
     * @param glyphCount Number of glyphs to be drawn.
     * @param x          Additional amount of x offset of the glyph X positions, i.e. left position
     * @param y          Additional amount of y offset of the glyph Y positions, i.e. baseline position
     * @param paint      Paint used for drawing.
     * @see icyllis.modernui.text.TextShaper
     */
    public final void drawShapedText(@NonNull ShapedText text, int glyphStart, int glyphCount,
                                     float x, float y, @NonNull Paint paint) {
        if ((glyphStart | glyphCount | glyphStart + glyphCount |
                text.getGlyphCount() - glyphStart - glyphCount) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (glyphCount == 0) {
            return;
        }
        var lastFont = text.getFont(glyphStart);
        int lastPos = glyphStart;
        int currPos = glyphStart + 1;
        for (int end = glyphStart + glyphCount; currPos < end; currPos++) {
            var curFont = text.getFont(currPos);
            if (lastFont != curFont) {
                drawGlyphs(text.getGlyphs(), lastPos,
                        text.getPositions(), lastPos << 1, currPos - lastPos,
                        lastFont, x, y, paint);
                lastFont = curFont;
                lastPos = currPos;
            }
        }
        drawGlyphs(text.getGlyphs(), lastPos,
                text.getPositions(), lastPos << 1, currPos - lastPos,
                lastFont, x, y, paint);
    }

    public final void drawSimpleText(@NonNull char[] text, @NonNull Font font,
                                     float x, float y, @NonNull Paint paint) {
        if (text.length == 0) {
            return;
        }
        if (font instanceof OutlineFont of) {
            var f = of.chooseFont(paint.getFontSize());
            var frc = OutlineFont.getFontRenderContext(
                    FontPaint.computeRenderFlags(paint));
            var gv = f.createGlyphVector(frc, text);
            int nGlyphs = gv.getNumGlyphs();
            drawGlyphs(gv.getGlyphCodes(0, nGlyphs, null),
                    0,
                    gv.getGlyphPositions(0, nGlyphs, null),
                    0,
                    nGlyphs,
                    font,
                    x, y, paint);
        }
    }

    public final void drawSimpleText(@NonNull String text, @NonNull Font font,
                                     float x, float y, @NonNull Paint paint) {
        if (!text.isBlank()) {
            drawSimpleText(text.toCharArray(), font, x, y, paint);
        }
    }

    /**
     * Supported primitive topologies, corresponding to OpenGL and Vulkan defined values.
     */
    public enum VertexMode {
        POINTS(Vertices.kPoints_VertexMode),
        LINES(Vertices.kLines_VertexMode),
        LINE_STRIP(Vertices.kLineStrip_VertexMode),
        TRIANGLES(Vertices.kTriangles_VertexMode),
        TRIANGLE_STRIP(Vertices.kTriangleStrip_VertexMode);

        final int nativeInt;

        VertexMode(int nativeInt) {
            this.nativeInt = nativeInt;
        }
    }

    /**
     * Draw an instance of a custom mesh with the given vertex data, the vertices are
     * interpreted as the given primitive topology.
     * <p>
     * The <var>pos</var> is required, and specifies the x,y pairs for each vertex in
     * local coordinates, numbered 0, 1, 2, ..., N-1, where N is the number of vertices
     * and N = pos.remaining() / 2.
     * <p>
     * If there is <var>color</var>, but there is no <var>tex</var>, then each color
     * (AARRGGBB) blends with paint color using the given <var>blender</var>, and
     * is interpolated across its corresponding topology in a gradient. The default is
     * {@link BlendMode#DST} in this case.
     * <p>
     * If there is <var>tex</var>, then it is used to specify the coordinate in UV
     * coordinates to use at each vertex (the paint must have a shader in this case).
     * If there is also <var>color</var>, then they behave as before, but blend with
     * paint shader. The default is {@link BlendMode#MODULATE} in this case.
     * <p>
     * If there is <var>indices</var> array, then it is used to specify the index of
     * each topology, rather than just walking through the arrays in order.
     * <p>
     * For {@link VertexMode#TRIANGLES} and {@link VertexMode#TRIANGLE_STRIP},
     * counterclockwise triangles face forward.
     * <p>
     * MaskFilter and anti-aliasing are ignored, anti-aliasing state only depends on
     * MSAA state of the current render target.
     *
     * @param mode    how to interpret the array of vertices
     * @param pos     array of positions for the mesh, remaining should be multiple of 2
     * @param color   if not null, specifies a color for each vertex, to be interpolated
     *                across the topology, remaining >= N
     * @param tex     if not null, specifies the coordinates to sample into the current
     *                shader (e.g. bitmap tile or gradient), remaining >= 2*N
     * @param indices if not null, array of indices to reference into the vertex array
     * @param blender blends vertices' colors with shader if present or paint color if
     *                not, ignored if there is no color array, or null to use the default
     * @param paint   specifies the texture to use if there is tex array and constant
     *                color (a shader uniform) if there is no color array
     * @throws java.nio.BufferUnderflowException insufficient vertex data
     */
    public void drawMesh(@NonNull VertexMode mode, @NonNull FloatBuffer pos,
                         @Nullable IntBuffer color, @Nullable FloatBuffer tex,
                         @Nullable ShortBuffer indices, @Nullable Blender blender,
                         @NonNull Paint paint) {
    }

    /**
     * Special case of {@link #drawMesh(VertexMode, FloatBuffer, IntBuffer, FloatBuffer, ShortBuffer, Blender, Paint)}.
     * <p>
     * Each point is always 1-pixel in screen-space (device space), no matter
     * what transformation is applied. It has zero area.
     *
     * @param pos   array of positions for the mesh, remaining should be multiple of 2
     * @param color if not null, specifies a color for each vertex, to be interpolated
     *              across the topology, remaining >= N
     * @param paint specifies a constant color (a shader uniform) if there is no color array
     */
    public final void drawPointListMesh(@NonNull FloatBuffer pos, @Nullable IntBuffer color, @NonNull Paint paint) {
        drawMesh(VertexMode.POINTS, pos, color, null, null, null, paint);
    }

    /**
     * Special case of {@link #drawMesh(VertexMode, FloatBuffer, IntBuffer, FloatBuffer, ShortBuffer, Blender, Paint)}.
     * <p>
     * Each line is always 1-pixel wide in screen-space (device space), no matter
     * what transformation is applied. This is known as hairline and it has zero area.
     *
     * @param pos   array of positions for the mesh, remaining should be multiple of 2
     * @param color if not null, specifies a color for each vertex, to be interpolated
     *              across the topology, remaining >= N
     * @param paint specifies a constant color (a shader uniform) if there is no color array
     */
    public final void drawLineListMesh(@NonNull FloatBuffer pos, @Nullable IntBuffer color, @NonNull Paint paint) {
        drawMesh(VertexMode.LINES, pos, color, null, null, null, paint);
    }

    /**
     * Special case of {@link #drawMesh(VertexMode, FloatBuffer, IntBuffer, FloatBuffer, ShortBuffer, Blender, Paint)}.
     *
     * @param pos   array of positions for the mesh, remaining should be multiple of 2
     * @param color if not null, specifies a color for each vertex, to be interpolated
     *              across the topology, remaining >= N
     * @param paint specifies a constant color (a shader uniform) if there is no color array
     */
    public final void drawTriangleListMesh(@NonNull FloatBuffer pos, @Nullable IntBuffer color, @NonNull Paint paint) {
        drawMesh(VertexMode.TRIANGLES, pos, color, null, null, null, paint);
    }

    @ApiStatus.Experimental
    public void drawCustomDrawable(@NonNull CustomDrawable drawable, @Nullable Matrix4 matrix) {
    }

    public final void drawCustomDrawable(@NonNull CustomDrawable drawable) {
        drawCustomDrawable(drawable, null);
    }

    /**
     * Returns true if clip is empty; that is, nothing will draw.
     * <p>
     * May do work when called; it should not be called more often than needed.
     * However, once called, subsequent calls perform no work until clip changes.
     *
     * @return true if clip is empty
     */
    public boolean isClipEmpty() {
        return false;
    }

    /**
     * Returns true if clip is a Rect and not empty.
     * Returns false if the clip is empty, or if it is complex.
     *
     * @return true if clip is a Rect and not empty
     */
    public boolean isClipRect() {
        return false;
    }
}
