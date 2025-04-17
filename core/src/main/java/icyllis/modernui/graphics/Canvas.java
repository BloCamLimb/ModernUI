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

import icyllis.arc3d.core.Matrix4;
import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.Size;
import icyllis.modernui.graphics.text.Font;
import icyllis.modernui.graphics.text.OutlineFont;
import icyllis.modernui.graphics.text.ShapedText;
import icyllis.modernui.view.Gravity;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

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
     * @return true if the resulting clip is non-empty
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
     * Return the bounds of the current clip (in local coordinates) in the
     * bounds parameter, and return true if it is non-empty. This can be useful
     * in a way similar to quickReject, in that it tells you that drawing
     * outside of these bounds will be clipped out.
     *
     * @param bounds Return the clip bounds here.
     * @return true if the current clip is non-empty.
     */
    public abstract boolean getLocalClipBounds(@NonNull RectF bounds);

    /**
     * Fills the current clip with the specified color, using SRC blend mode.
     * This has the effect of replacing all pixels contained by clip with color.
     *
     * @param color the un-premultiplied (straight) color to draw onto the canvas
     */
    public final void clear(@ColorInt int color) {
        drawColor(color, BlendMode.SRC);
    }

    /**
     * Fills the current clip with the specified color, using SRC blend mode.
     * This has the effect of replacing all pixels contained by clip with color.
     *
     * @param r the red component of the straight color to draw onto the canvas
     * @param g the red component of the straight color to draw onto the canvas
     * @param b the red component of the straight color to draw onto the canvas
     * @param a the red component of the straight color to draw onto the canvas
     */
    public final void clear(float r, float g, float b, float a) {
        drawColor(r, g, b, a, BlendMode.SRC);
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
     * Fill the current clip with the specified color, using SRC_OVER blend mode.
     *
     * @param r the red component of the straight color to draw onto the canvas
     * @param g the red component of the straight color to draw onto the canvas
     * @param b the red component of the straight color to draw onto the canvas
     * @param a the red component of the straight color to draw onto the canvas
     */
    public final void drawColor(float r, float g, float b, float a) {
        drawColor(r, g, b, a, BlendMode.SRC_OVER);
    }

    /**
     * Fill the current clip with the specified color, the blend mode determines
     * how color is combined with destination.
     *
     * @param color the straight color to draw onto the canvas
     * @param mode  the blend mode used to combine source color and destination
     */
    public abstract void drawColor(@ColorInt int color, @NonNull BlendMode mode);

    /**
     * Fill the current clip with the specified color, the blend mode determines
     * how color is combined with destination.
     *
     * @param r    the red component of the straight color to draw onto the canvas
     * @param g    the red component of the straight color to draw onto the canvas
     * @param b    the red component of the straight color to draw onto the canvas
     * @param a    the red component of the straight color to draw onto the canvas
     * @param mode the blend mode used to combine source color and destination
     */
    public abstract void drawColor(float r, float g, float b, float a, @NonNull BlendMode mode);

    /**
     * Fills the current clip with the specified paint. Paint components, Shader,
     * ColorFilter, ImageFilter, and BlendMode affect drawing.
     * <p>
     * This is equivalent (but faster) to drawing an infinitely large rectangle
     * with the specified paint.
     *
     * @param paint the paint used to draw onto the canvas
     */
    public abstract void drawPaint(@NonNull Paint paint);

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
    public abstract void drawPoint(float x, float y, @NonNull Paint paint);

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
    public final void drawPoint(@NonNull PointF p, @NonNull Paint paint) {
        drawPoint(p.x, p.y, paint);
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
    public abstract void drawPoints(@Size(multiple = 2) @NonNull float[] pts, int offset, int count,
                                    @NonNull Paint paint);

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
    public final void drawPoints(@Size(multiple = 2) @NonNull float[] pts, @NonNull Paint paint) {
        drawPoints(pts, 0, pts.length, paint);
    }

    /**
     * Draws line segment from (x0, y0) to (x1, y1) using the current matrix,
     * clip and specified paint. In paint: stroke width describes the line thickness;
     * Cap draws the end rounded or square; Style is ignored, as if were set to
     * {@link Paint#STROKE}.
     *
     * @param x0    start of line segment on x-axis
     * @param y0    start of line segment on y-axis
     * @param x1    end of line segment on x-axis
     * @param y1    end of line segment on y-axis
     * @param paint the paint used to draw the line
     */
    public abstract void drawLine(float x0, float y0, float x1, float y1, @NonNull Paint paint);

    /**
     * Draws line segment from (x0, y0) to (x1, y1) using the current matrix,
     * clip and specified paint. In paint: stroke width describes the line thickness;
     * Cap draws the end rounded or square; Style is ignored, as if were set to
     * {@link Paint#STROKE}.
     *
     * @param p0    start of line segment
     * @param p1    end of line segment
     * @param paint the paint used to draw the line
     */
    public final void drawLine(@NonNull PointF p0, @NonNull PointF p1, @NonNull Paint paint) {
        drawLine(p0.x, p0.y, p1.x, p1.y, paint);
    }

    /**
     * Draw a line segment from (x0, y0) to (x1, y1) using the specified paint.
     * <p>
     * Line covers an area, and is not a stroke path in the concept of Modern UI.
     * Therefore, a line may be either "filled" (path) or "stroked" (annular, hollow),
     * depending on paint's style. Additionally, paint's cap draws the end rounded
     * or square, if filled; the other properties of paint work as intended.
     * <p>
     * If thickness = 0 (also known as hairline), then this uses the mesh version.
     * See {@link #drawLineListMesh(FloatBuffer, IntBuffer, Paint)} for the mesh version.
     *
     * @param x0        the start of the line segment on x-axis
     * @param y0        the start of the line segment on y-axis
     * @param x1        the end of the line segment on x-axis
     * @param y1        the end of the line segment on y-axis
     * @param cap       the line end used to determine the shape of the line
     * @param thickness the thickness of the line segment
     * @param paint     the paint used to draw the line segment
     */
    public abstract void drawLine(float x0, float y0, float x1, float y1,
                                  @NonNull Paint.Cap cap, float thickness, @NonNull Paint paint);

    /**
     * Equivalent to {@link #drawLine(float, float, float, float, Paint.Cap, float, Paint)}.
     * with a round cap.
     *
     * @param x0        the start of the line segment on x-axis
     * @param y0        the start of the line segment on y-axis
     * @param x1        the end of the line segment on x-axis
     * @param y1        the end of the line segment on y-axis
     * @param thickness the thickness of the line segment
     * @param paint     the paint used to draw the line segment
     */
    public final void drawLine(float x0, float y0, float x1, float y1,
                               float thickness, @NonNull Paint paint) {
        drawLine(x0, y0, x1, y1, Paint.Cap.ROUND, thickness, paint);
    }

    /**
     * Equivalent to {@link #drawLine(float, float, float, float, Paint.Cap, float, Paint)}.
     * with a round cap.
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
     * Draw a series of lines.
     * <p>
     * If <var>connected</var> is false, each line is taken from 4 consecutive values in the pts array.
     * Thus, to draw 1 line, the array must contain at least 4 values. This is logically the same as
     * drawing the array as follows: drawLine(pts[0], pts[1], pts[2], pts[3]) followed by
     * drawLine(pts[4], pts[5], pts[6], pts[7]) and so on.
     * <p>
     * If <var>connected</var> is true, the first line is taken from 4 consecutive values in the pts
     * array, and each remaining line is taken from last 2 values and next 2 values in the array.
     * Thus, to draw 1 line, the array must contain at least 4 values. This is logically the same as
     * drawing the array as follows: drawLine(pts[0], pts[1], pts[2], pts[3]) followed by
     * drawLine(pts[2], pts[3], pts[4], pts[5]) and so on.
     *
     * @param pts       The array of points of the lines to draw [x0 y0 x1 y1 x2 y2 ...]
     * @param offset    Number of values in the array to skip before drawing.
     * @param count     The number of values in the array to process, after skipping "offset" of them.
     *                  Since each line uses 4 values, the number of "lines" that are drawn is really
     *                  (count >> 2) if connected, or ((count - 2) >> 1) if separate.
     * @param connected Whether line points are connected or separate
     * @param paint     The paint used to draw the lines
     */
    public abstract void drawLines(@Size(multiple = 2) @NonNull float[] pts, int offset, int count,
                                   boolean connected, @NonNull Paint paint);

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
     * @deprecated use {@link #drawLines(float[], int, int, boolean, Paint)} instead
     */
    @Deprecated
    public void drawRoundLines(float[] pts, int offset, int count, boolean strip,
                               Paint paint) {
        drawLines(pts, offset, count, strip, paint);
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
    public final void drawRect(@NonNull RectF r, @NonNull Paint paint) {
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
    public final void drawRect(@NonNull Rect r, @NonNull Paint paint) {
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
    public abstract void drawRect(float left, float top, float right, float bottom, @NonNull Paint paint);

    /**
     * Draw a rectangle with rounded corners within a rectangular bounds. The round
     * rectangle will be filled or framed based on the Style in the paint.
     *
     * @param rect   The rectangular bounds of the round rect to be drawn
     * @param radius the radius used to round the corners
     * @param paint  the paint used to draw the round rectangle
     */
    public final void drawRoundRect(@NonNull RectF rect, float radius, @NonNull Paint paint) {
        drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, radius, paint);
    }

    /**
     * Draw a rectangle with rounded corners within a rectangular bounds. The round
     * rectangle will be filled or framed based on the Style in the paint.
     * <p>
     * The corner radius will be clamped to {@code min(right-left, bottom-top) / 2}
     * to fit within bounds.
     *
     * @param left   the left of the rectangular bounds
     * @param top    the top of the rectangular bounds
     * @param right  the right of the rectangular bounds
     * @param bottom the bottom of the rectangular bounds
     * @param radius the radius used to round the corners
     * @param paint  the paint used to draw the round rectangle
     */
    public abstract void drawRoundRect(float left, float top, float right, float bottom,
                                       float radius, @NonNull Paint paint);

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
     * @deprecated use {@link #drawRoundRect(RectF, float, float, float, float, Paint)}
     */
    @Deprecated
    public final void drawRoundRect(@NonNull RectF rect, float radius, int sides, @NonNull Paint paint) {
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
     * @deprecated use {@link #drawRoundRect(float, float, float, float, float, float, float, float, Paint)}
     */
    @Deprecated
    public void drawRoundRect(float left, float top, float right, float bottom,
                              float radius, int sides, @NonNull Paint paint) {
        if (radius <= 0) {
            drawRect(left, top, right, bottom, paint);
            return;
        }
        float topLeftRadius = 0;
        float topRightRadius = 0;
        float bottomRightRadius = 0;
        float bottomLeftRadius = 0;
        switch (sides) {
            case Gravity.TOP -> {
                topLeftRadius = radius;
                topRightRadius = radius;
            }
            case Gravity.BOTTOM -> {
                bottomRightRadius = radius;
                bottomLeftRadius = radius;
            }
            case Gravity.LEFT -> {
                topLeftRadius = radius;
                bottomLeftRadius = radius;
            }
            case Gravity.RIGHT -> {
                topRightRadius = radius;
                bottomRightRadius = radius;
            }
            case Gravity.TOP | Gravity.LEFT -> topLeftRadius = radius;
            case Gravity.TOP | Gravity.RIGHT -> topRightRadius = radius;
            case Gravity.BOTTOM | Gravity.LEFT -> bottomLeftRadius = radius;
            case Gravity.BOTTOM | Gravity.RIGHT -> bottomRightRadius = radius;
            case 0 -> {
                drawRoundRect(left, top, right, bottom, radius, paint);
                return;
            }
            default -> {
                drawRect(left, top, right, bottom, paint);
                return;
            }
        }
        drawRoundRect(left, top, right, bottom,
                topLeftRadius, topRightRadius,
                bottomRightRadius, bottomLeftRadius, paint);
    }

    /**
     * Draw a rectangle with rounded corners within a rectangular bounds. The round
     * rectangle will be filled or framed based on the Style in the paint.
     * <p>
     * If any of the given corner radii is NaN or infinite, the result is a rectangle
     * (all corners are square).<br>
     * If corner radius is zero or less: radius are stored as zero; corner is square.
     * If corner curves overlap, radii are proportionally scaled to fit within bounds.
     *
     * @param rect              The rectangular bounds of the round rect to be drawn
     * @param topLeftRadius     the radius used to round the upper left corner
     * @param topRightRadius    the radius used to round the upper right corner
     * @param bottomRightRadius the radius used to round the lower right corner
     * @param bottomLeftRadius  the radius used to round the lower left corner
     * @param paint             the paint used to draw the round rectangle
     * @since 3.12
     */
    public final void drawRoundRect(@NonNull RectF rect, float topLeftRadius, float topRightRadius,
                                    float bottomRightRadius, float bottomLeftRadius, @NonNull Paint paint) {
        drawRoundRect(rect.left, rect.top, rect.right, rect.bottom,
                topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius, paint);
    }

    /**
     * Draw a rectangle with rounded corners within a rectangular bounds. The round
     * rectangle will be filled or framed based on the Style in the paint.
     * <p>
     * If any of the given corner radii is NaN or infinite, the result is a rectangle
     * (all corners are square).<br>
     * If corner radius is zero or less: radius are stored as zero; corner is square.
     * If corner curves overlap, radii are proportionally scaled to fit within bounds.
     *
     * @param left              the left of the rectangular bounds
     * @param top               the top of the rectangular bounds
     * @param right             the right of the rectangular bounds
     * @param bottom            the bottom of the rectangular bounds
     * @param topLeftRadius     the radius used to round the upper left corner
     * @param topRightRadius    the radius used to round the upper right corner
     * @param bottomRightRadius the radius used to round the lower right corner
     * @param bottomLeftRadius  the radius used to round the lower left corner
     * @param paint             the paint used to draw the round rectangle
     * @since 3.12
     */
    public abstract void drawRoundRect(float left, float top, float right, float bottom,
                                       float topLeftRadius, float topRightRadius,
                                       float bottomRightRadius, float bottomLeftRadius,
                                       @NonNull Paint paint);

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
     * Draw a circular arc at (cx, cy) with radius using the current matrix,
     * clip and specified paint.
     * <p>
     * If the start angle is negative or >= 360, the start angle is treated as
     * start angle modulo 360. If the sweep angle is >= 360, then the circle is
     * drawn completely. If the sweep angle is negative, the sweep angle is
     * treated as sweep angle modulo 360.
     * <p>
     * The arc is drawn clockwise. An angle of 0 degrees correspond to the
     * geometric angle of 0 degrees (3 o'clock on a watch). If radius is
     * non-positive or sweep angle is zero, nothing is drawn.
     *
     * @param cx         the x-coordinate of the center of the arc to be drawn
     * @param cy         the y-coordinate of the center of the arc to be drawn
     * @param radius     the radius of the circular arc to be drawn
     * @param startAngle starting angle (in degrees) where the arc begins
     * @param sweepAngle sweep angle or angular extent (in degrees); positive is clockwise
     * @param paint      the paint used to draw the arc
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
     * Draw a circular arc at (cx, cy) with radius using the current matrix,
     * clip and specified paint. Note that arc shape is a stroked arc rather
     * than an arc path, you can fill or stroke the stroked arc.
     * <p>
     * If the start angle is negative or >= 360, the start angle is treated as
     * start angle modulo 360. If the sweep angle is >= 360, then the circle is
     * drawn completely. If the sweep angle is negative, the sweep angle is
     * treated as sweep angle modulo 360.
     * <p>
     * The arc is drawn clockwise. An angle of 0 degrees correspond to the
     * geometric angle of 0 degrees (3 o'clock on a watch). If radius is
     * non-positive or sweep angle is zero, nothing is drawn.
     * <p>
     * The <var>cap</var> describes the end of the arc shape, the <var>width</var>
     * describes the arc shape thickness. In paint: Style determines if arc shape
     * is stroked or filled; If stroked (i.e. double stroke), stroke width describes the
     * shape outline thickness, Cap describes line ends, Join draws the corners rounded
     * or square, and Align determines the position or direction to stroke.
     *
     * @param cx         the x-coordinate of the center of the arc to be drawn
     * @param cy         the y-coordinate of the center of the arc to be drawn
     * @param radius     the radius of the circular arc to be drawn
     * @param startAngle starting angle (in degrees) where the arc begins
     * @param sweepAngle sweep angle or angular extent (in degrees); positive is clockwise
     * @param cap        the line end used to determine the shape of the arc
     * @param thickness  the thickness of arc segment
     * @param paint      the paint used to draw the arc
     */
    public abstract void drawArc(float cx, float cy, float radius,
                                 float startAngle, float sweepAngle,
                                 @NonNull Paint.Cap cap, float thickness, @NonNull Paint paint);

    /**
     * Draw a circular sector (i.e., a pie) at (cx, cy) with radius using the
     * current matrix, clip and specified paint.
     * <p>
     * Similar to {@link #drawArc(float, float, float, float, float, Paint)}, but
     * when the shape is not a full circle, the geometry is closed by the arc and
     * two line segments from the end of the arc to the center of the circle.
     * <p>
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

    /**
     * Draw a circular segment (i.e., a cut disk) at (cx, cy) with radius using the
     * current matrix, clip and specified paint.
     * <p>
     * Similar to {@link #drawArc(float, float, float, float, float, Paint)}, but
     * when the shape is not a full circle, the geometry is closed by the arc and
     * a line segment from the start of the arc segment to the end of the arc segment.
     * <p>
     *
     * @param cx         the x-coordinate of the center of the arc to be drawn
     * @param cy         the y-coordinate of the center of the arc to be drawn
     * @param radius     the radius of the circular arc to be drawn
     * @param startAngle starting angle (in degrees) where the arc begins
     * @param sweepAngle sweep angle or angular extent (in degrees); positive is clockwise
     * @param paint      the paint used to draw the arc
     */
    public abstract void drawChord(float cx, float cy, float radius,
                                   float startAngle, float sweepAngle,
                                   @NonNull Paint paint);

    /**
     * Variant version of {@link #drawChord(float, float, float, float, float, Paint)}.
     *
     * @param center     the center point of the arc to be drawn
     * @param radius     the radius of the circular arc to be drawn
     * @param startAngle starting angle (in degrees) where the arc begins
     * @param sweepAngle sweep angle or angular extent (in degrees); positive is clockwise
     * @param paint      the paint used to draw the arc
     */
    public final void drawChord(@NonNull PointF center, float radius,
                                float startAngle, float sweepAngle,
                                @NonNull Paint paint) {
        drawChord(center.x, center.y, radius, startAngle, sweepAngle, paint);
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
     * @deprecated use drawPath() instead
     */
    @Deprecated
    public void drawBezier(float x0, float y0, float x1, float y1, float x2, float y2,
                           Paint paint) {
        //TODO drawPath() needs to be implemented in future
        drawLines(new float[]{x0, y0, x1, y1, x2, y2}, 0, 6, true, paint);
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
     * @deprecated use drawPath() instead
     */
    @Deprecated
    public final void drawBezier(PointF p0, PointF p1, PointF p2,
                                 Paint paint) {
        drawBezier(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, paint);
    }

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
    public final void drawImage(Image image, @Nullable Rect src, @NonNull Rect dst, @Nullable Paint paint) {
        if (image == null) {
            return;
        }
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
    public final void drawImage(Image image, @Nullable Rect src, @NonNull RectF dst, @Nullable Paint paint) {
        if (image == null) {
            return;
        }
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
     * Draw the specified image with rounded corners, whose top/left corner at (x,y)
     * using the specified paint, transformed by the current matrix. The Style is
     * ignored in the paint, images are always filled.
     *
     * @param image  the image to be drawn
     * @param left   the position of the left side of the image being drawn
     * @param top    the position of the top side of the image being drawn
     * @param radius the radius used to round the corners
     * @param paint  the paint used to draw the round image
     * @deprecated use {@link ImageShader} and {@link #drawRoundRect} instead
     */
    @Deprecated
    public void drawRoundImage(Image image, float left, float top,
                               float radius, @Nullable Paint paint) {
        drawImage(image, left, top, paint);
    }

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

    //TODO remove this once RecordingCanvas is done
    @ApiStatus.Experimental
    public abstract void drawTextBlob(icyllis.arc3d.sketch.TextBlob blob, float x, float y,
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
    public void drawShapedText(@NonNull ShapedText text,
                                     float x, float y, @NonNull Paint paint) {
        drawTextBlob(text.getTextBlob(), x, y, paint);
    }

    public void drawSimpleText(@NonNull char[] text, @NonNull Font font,
                                     float x, float y, @NonNull Paint paint) {
        if (text.length == 0) {
            return;
        }
        if (font instanceof OutlineFont outlineFont) {
            var face = outlineFont.chooseFont(paint);
            var frc = OutlineFont.getFontRenderContext(paint);
            var gv = face.createGlyphVector(frc, text);
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

    public void drawSimpleText(@NonNull String text, @NonNull Font font,
                                     float x, float y, @NonNull Paint paint) {
        if (!text.isBlank()) {
            drawSimpleText(text.toCharArray(), font, x, y, paint);
        }
    }

    /**
     * Supported primitive topologies, corresponding to OpenGL and Vulkan defined values.
     */
    public enum VertexMode {
        POINTS(icyllis.arc3d.sketch.Vertices.kPoints_VertexMode),
        LINES(icyllis.arc3d.sketch.Vertices.kLines_VertexMode),
        LINE_STRIP(icyllis.arc3d.sketch.Vertices.kLineStrip_VertexMode),
        TRIANGLES(icyllis.arc3d.sketch.Vertices.kTriangles_VertexMode),
        TRIANGLE_STRIP(icyllis.arc3d.sketch.Vertices.kTriangleStrip_VertexMode);

        final int nativeInt;

        VertexMode(int nativeInt) {
            this.nativeInt = nativeInt;
        }
    }

    /**
     * Draw an instance of a custom mesh with the given vertex data, the vertices are
     * interpreted as the given primitive topology.
     * <p>
     * The <var>positions</var> is required, and specifies the x,y pairs for each vertex in
     * local coordinates, numbered 0, 1, 2, ..., N-1, where N is the number of vertices
     * and N = <var>vertexCount</var> / 2.
     * <p>
     * If there is <var>colors</var>, but there is no <var>texCoords</var>, then each colors
     * (AARRGGBB) blends with paint colors using the given <var>blendMode</var>, and
     * is interpolated across its corresponding topology in a gradient. The default is
     * {@link BlendMode#DST} in this case.
     * <p>
     * If there is <var>texCoords</var>, then it is used to specify the coordinate in UV
     * coordinates to use at each vertex (the paint must have a shader in this case).
     * If there is also <var>colors</var>, then they behave as before, but blend with
     * paint shader. The default is {@link BlendMode#MODULATE} in this case.
     * <p>
     * If there is <var>indices</var> array, then it is used to specify the index of
     * each topology, rather than just walking through the arrays in order.
     * <p>
     * For {@link VertexMode#TRIANGLES} and {@link VertexMode#TRIANGLE_STRIP},
     * counterclockwise triangles face forward.
     * <p>
     * MaskFilter and antialiasing are ignored, antialiasing state only depends on
     * MSAA state of the current render target.
     *
     * @param mode           How to interpret the array of vertices
     * @param vertexCount    The number of values in the vertices array (and corresponding texCoords and
     *                       colors arrays if non-null). Each logical vertex is two values (x, y), vertexCount
     *                       must be a multiple of 2.
     * @param positions      Array of vertices for the mesh
     * @param positionOffset Number of values in the positions to skip before drawing.
     * @param texCoords      May be null. If not null, specifies the coordinates to sample into the current
     *                       shader (e.g. bitmap tile or gradient)
     * @param texCoordOffset Number of values in texCoords to skip before drawing.
     * @param colors         May be null. If not null, specifies a color for each vertex, to be interpolated
     *                       across the triangle.
     * @param colorOffset    Number of values in colors to skip before drawing.
     * @param indices        If not null, array of indices to reference into the vertex (texCoords, colors)
     *                       array.
     * @param indexCount     Number of entries in the indices array (if not null).
     * @param blendMode      Blends vertices' colors with shader if present or paint colors if
     *                       not, ignored if there is no colors array, or null to use the default
     * @param paint          Specifies the shader to use if the texCoords array is non-null. Antialiasing is not
     *                       supported.
     */
    public abstract void drawVertices(@NonNull VertexMode mode, int vertexCount,
                                      @Size(multiple = 2) @NonNull float[] positions, int positionOffset,
                                      @Size(multiple = 2) @Nullable float[] texCoords, int texCoordOffset,
                                      @Nullable int[] colors, int colorOffset,
                                      @Nullable short[] indices, int indexOffset, int indexCount,
                                      @Nullable BlendMode blendMode, @NonNull Paint paint);

    /**
     * Draw an instance of a custom mesh with the given vertex data, the vertices are
     * interpreted as the given primitive topology.
     * <p>
     * The <var>positions</var> is required, and specifies the x,y pairs for each vertex in
     * local coordinates, numbered 0, 1, 2, ..., N-1, where N is the number of vertices
     * and N = positions.remaining() / 2.
     * <p>
     * If there is <var>colors</var>, but there is no <var>texCoords</var>, then each colors
     * (AARRGGBB) blends with paint colors using the given <var>blendMode</var>, and
     * is interpolated across its corresponding topology in a gradient. The default is
     * {@link BlendMode#DST} in this case.
     * <p>
     * If there is <var>texCoords</var>, then it is used to specify the coordinate in UV
     * coordinates to use at each vertex (the paint must have a shader in this case).
     * If there is also <var>colors</var>, then they behave as before, but blend with
     * paint shader. The default is {@link BlendMode#MODULATE} in this case.
     * <p>
     * If there is <var>indices</var> array, then it is used to specify the index of
     * each topology, rather than just walking through the arrays in order.
     * <p>
     * For {@link VertexMode#TRIANGLES} and {@link VertexMode#TRIANGLE_STRIP},
     * counterclockwise triangles face forward.
     * <p>
     * MaskFilter and antialiasing are ignored, antialiasing state only depends on
     * MSAA state of the current render target.
     *
     * @param mode      how to interpret the array of vertices
     * @param positions array of positions for the mesh, remaining should be multiple of 2
     * @param texCoords if not null, specifies the coordinates to sample into the current
     *                  shader (e.g. bitmap tile or gradient), remaining >= 2*N
     * @param colors    if not null, specifies a colors for each vertex, to be interpolated
     *                  across the topology, remaining >= N
     * @param indices   if not null, array of indices to reference into the vertex array
     * @param blendMode blends vertices' colors with shader if present or paint colors if
     *                  not, ignored if there is no colors array, or null to use the default
     * @param paint     specifies the texture to use if there is texCoords array and constant
     *                  colors (a shader uniform) if there is no colors array
     * @throws java.nio.BufferUnderflowException insufficient vertex data
     */
    public abstract void drawMesh(@NonNull VertexMode mode, @NonNull FloatBuffer positions,
                                  @Nullable FloatBuffer texCoords, @Nullable IntBuffer colors,
                                  @Nullable ShortBuffer indices, @Nullable BlendMode blendMode,
                                  @NonNull Paint paint);

    /**
     * Special case of
     * {@link #drawMesh(VertexMode, FloatBuffer, FloatBuffer, IntBuffer, ShortBuffer, BlendMode, Paint)}.
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
        drawMesh(VertexMode.POINTS, pos, null, color, null, null, paint);
    }

    /**
     * Special case of
     * {@link #drawMesh(VertexMode, FloatBuffer, FloatBuffer, IntBuffer, ShortBuffer, BlendMode, Paint)}.
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
        drawMesh(VertexMode.LINES, pos, null, color, null, null, paint);
    }

    /**
     * Special case of
     * {@link #drawMesh(VertexMode, FloatBuffer, FloatBuffer, IntBuffer, ShortBuffer, BlendMode, Paint)}.
     *
     * @param pos   array of positions for the mesh, remaining should be multiple of 2
     * @param color if not null, specifies a color for each vertex, to be interpolated
     *              across the topology, remaining >= N
     * @param paint specifies a constant color (a shader uniform) if there is no color array
     */
    public final void drawTriangleListMesh(@NonNull FloatBuffer pos, @Nullable IntBuffer color, @NonNull Paint paint) {
        drawMesh(VertexMode.TRIANGLES, pos, null, color, null, null, paint);
    }

    @ApiStatus.Experimental
    public static final int
            QUAD_AA_FLAG_LEFT = icyllis.arc3d.sketch.Canvas.QUAD_AA_FLAG_LEFT,
            QUAD_AA_FLAG_TOP = icyllis.arc3d.sketch.Canvas.QUAD_AA_FLAG_TOP,
            QUAD_AA_FLAG_RIGHT = icyllis.arc3d.sketch.Canvas.QUAD_AA_FLAG_RIGHT,
            QUAD_AA_FLAG_BOTTOM = icyllis.arc3d.sketch.Canvas.QUAD_AA_FLAG_BOTTOM,
            QUAD_AA_FLAGS_NONE = 0,
            QUAD_AA_FLAGS_ALL = QUAD_AA_FLAG_LEFT|QUAD_AA_FLAG_TOP|QUAD_AA_FLAG_RIGHT|QUAD_AA_FLAG_BOTTOM;

    // From Chromium project
    @ApiStatus.Experimental
    public void drawEdgeAAQuad(@Nullable RectF rect, @Nullable @Size(8) float[] clip,
                               @MagicConstant(flags = {QUAD_AA_FLAG_LEFT, QUAD_AA_FLAG_TOP, QUAD_AA_FLAG_RIGHT,
                                       QUAD_AA_FLAG_BOTTOM}) int edgeFlags, @Nullable Paint paint) {
    }

    @Deprecated
    public void drawCustomDrawable(@NonNull CustomDrawable drawable, @Nullable Matrix4 matrix) {
    }

    @Deprecated
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
    public abstract boolean isClipEmpty();

    /**
     * Returns true if clip is a Rect and not empty.
     * Returns false if the clip is empty, or if it is complex.
     *
     * @return true if clip is a Rect and not empty
     */
    public abstract boolean isClipRect();
}
