/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

import icyllis.arc3d.engine.RecordingContext;
import icyllis.arc3d.granite.SurfaceDrawContext;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
 * The Canvas supports multiple color buffers in one off-screen rendering target,
 * which can be used for many complex color blending and temporary operations.
 * Note that depth buffer and depth test is not enabled, Z ordering or transparency
 * sorting is required on the client pipeline before drawing onto the Canvas. All
 * layers are considered translucent and drawn from far to near.
 * <p>
 * You may never access the pixels of the canvas directly on the current thread,
 * because canvas is backed by GPU and uses deferred rendering. You must wait for
 * GPU to finish all rendering tasks and then use thread scheduling before you can
 * download the surface data to CPU side memory.
 */
@SuppressWarnings("unused")
public class Canvas implements AutoCloseable {

    /**
     * SaveLayerFlags provides options that may be used in any combination in SaveLayerRec,
     * defining how layer allocated by saveLayer() operates.
     */
    @MagicConstant(flags = {INIT_WITH_PREVIOUS_SAVE_LAYER_FLAG, F16_COLOR_TYPE_SAVE_LAYER_FLAG})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SaveLayerFlag {
    }

    /**
     * Initializes with previous contents.
     */
    public static final int INIT_WITH_PREVIOUS_SAVE_LAYER_FLAG = 1 << 2;

    /**
     * Instead of matching previous layer's color type, use F16.
     */
    public static final int F16_COLOR_TYPE_SAVE_LAYER_FLAG = 1 << 4;

    // getSaveLayerStrategy()'s return value may suppress full layer allocation.
    protected static final int
            FULL_LAYER_SAVE_LAYER_STRATEGY = 0,
            NO_LAYER_SAVE_LAYER_STRATEGY = 1;

    // cache some objects for performance
    private static final int MAX_MC_POOL_SIZE = 32;

    // a cached identity matrix for resetMatrix() call
    private static final Matrix4 IDENTITY_MATRIX = Matrix4.identity();

    // the bottom-most device in the stack, only changed by init(). Image properties and the final
    // canvas pixels are determined by this device
    @SharedPtr
    private Device mBaseDevice;

    // keep track of the device clip bounds in the canvas' global space to reject draws before
    // invoking the top-level device.
    private final Rect2f mQuickRejectBounds = new Rect2f();

    // the surface we are associated with, may be null
    Surface mSurface;

    // local MCRec stack
    private MCRec[] mMCStack = new MCRec[MAX_MC_POOL_SIZE];
    // points to top of stack
    private int mMCIndex = 0;

    // value returned by getSaveCount()
    private int mSaveCount;

    private final MarkerStack mMarkerStack = new MarkerStack();

    // a temp rect that used with arguments
    private final Rect2f mTmpRect = new Rect2f();
    private final RoundRect mTmpRoundRect = new RoundRect();
    private final Rect2f mTmpQuickBounds1 = new Rect2f();
    private final Rect2f mTmpQuickBounds2 = new Rect2f();
    private final Matrix4 mTmpMatrix = new Matrix4();
    private final Paint mTmpPaint = new Paint();

    private final GlyphRunBuilder mScratchGlyphRunBuilder = new GlyphRunBuilder();

    /**
     * Creates an empty Canvas with no backing device or pixels, with
     * a width and height of zero.
     */
    public Canvas() {
        this(0, 0);
    }

    /**
     * Creates Canvas of the specified dimensions without a Surface.
     * Used by subclasses with custom implementations for draw member functions.
     *
     * @param width  zero or greater
     * @param height zero or greater
     */
    public Canvas(int width, int height) {
        this(new NoPixelsDevice(0, 0, Math.max(width, 0), Math.max(height, 0)));
    }

    @ApiStatus.Internal
    public Canvas(@SharedPtr Device device) {
        mSaveCount = 1;
        mMCStack[0] = new MCRec(device);
        mBaseDevice = device;
        computeQuickRejectBounds();
    }

    /**
     * Returns ImageInfo for Canvas. If Canvas is not associated with raster surface or
     * GPU surface, returned ColorType is set to {@link ColorInfo#CT_UNKNOWN}.
     *
     * @return dimensions and ColorType of Canvas
     */
    @Nonnull
    public ImageInfo getImageInfo() {
        return mBaseDevice.imageInfo();
    }

    /**
     * Gets the size of the base or root layer in global canvas coordinates. The
     * origin of the base layer is always (0,0). The area available for drawing may be
     * smaller (due to clipping or saveLayer).
     *
     * @return integral width of base layer
     */
    public final int getBaseLayerWidth() {
        return mBaseDevice.width();
    }

    /**
     * Gets the size of the base or root layer in global canvas coordinates. The
     * origin of the base layer is always (0,0). The area available for drawing may be
     * smaller (due to clipping or saveLayer).
     *
     * @return integral height of base layer
     */
    public final int getBaseLayerHeight() {
        return mBaseDevice.height();
    }

    /**
     * Creates Surface matching info, and associates it with Canvas.
     * Returns null if no match found.
     *
     * @param info width, height, ColorType and AlphaType
     * @return Surface matching info, or null if no match is available
     */
    @Nullable
    public Surface makeSurface(ImageInfo info) {
        return getTopDevice().makeSurface(info);
    }

    /**
     * Returns GPU context of the GPU surface associated with Canvas.
     *
     * @return GPU context, if available; null otherwise
     */
    @Nullable
    public RecordingContext getRecordingContext() {
        return getTopDevice().getRecordingContext();
    }

    /**
     * Sometimes a canvas is owned by a surface. If it is, getSurface() will return a bare
     * pointer to that surface, else this will return null.
     */
    @Nullable
    public final Surface getSurface() {
        return mSurface;
    }

    /**
     * Saves the current matrix and clip onto a private stack.
     * <p>
     * Subsequent calls to translate, scale, rotate, skew, concat or clipRect,
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
    public final int save() {
        final int i = mSaveCount++;
        top().mDeferredSaveCount++;
        return i;
    }

    /**
     * This behaves the same as {@link #save()}, but in addition it allocates and
     * redirects drawing to an offscreen rendering target (a drawing layer).
     * <p class="note"><strong>Note:</strong> this method is very expensive,
     * incurring more than double rendering cost for contained content. Avoid
     * using this method when possible.
     * <p>
     * All drawing calls are directed to a newly allocated offscreen rendering target.
     * Only when the balancing call to restore() is made, is that offscreen
     * buffer drawn back to the current target of the Canvas (which can potentially
     * be a previous layer if these calls are nested).
     * <p>
     * Optional paint applies alpha, ColorFilter, ImageFilter, and BlendMode
     * when restore() is called.
     * <p>
     * Call restoreToCount() with returned value to restore this and subsequent saves.
     *
     * @param bounds the maximum size the offscreen render target needs to be
     *               (in local coordinates), may be null
     * @param paint  the paint is applied to the offscreen when restore() is
     *               called, may be null
     * @return depth of saved stack to pass to restoreToCount() to balance this call
     */
    public final int saveLayer(@Nullable Rect2f bounds, @Nullable Paint paint) {
        return saveLayer(bounds, paint, null, 0);
    }

    /**
     * Convenience for {@link #saveLayer(Rect2f, Paint)} that takes the four float coordinates
     * of the bounds' rectangle.
     *
     * @see #saveLayer(Rect2f, Paint)
     */
    public final int saveLayer(float left, float top, float right, float bottom, @Nullable Paint paint) {
        mTmpRect.set(left, top, right, bottom);
        return saveLayer(mTmpRect, paint, null, 0);
    }

    /**
     * This behaves the same as {@link #save()}, but in addition it allocates and
     * redirects drawing to an offscreen rendering target (a drawing layer).
     * <p class="note"><strong>Note:</strong> this method is very expensive,
     * incurring more than double rendering cost for contained content. Avoid
     * using this method when possible.
     * <p>
     * All drawing calls are directed to a newly allocated offscreen rendering target.
     * Only when the balancing call to restore() is made, is that offscreen
     * buffer drawn back to the current target of the Canvas (which can potentially
     * be a previous layer if these calls are nested).
     * <p>
     * Alpha of zero is fully transparent, 255 is fully opaque.
     * <p>
     * Call restoreToCount() with returned value to restore this and subsequent saves.
     *
     * @param bounds the maximum size the offscreen render target needs to be
     *               (in local coordinates), may be null
     * @param alpha  the alpha to apply to the offscreen when restore() is called
     * @return depth of saved stack to pass to restoreToCount() to balance this call
     */
    public final int saveLayerAlpha(@Nullable Rect2f bounds, int alpha) {
        alpha = MathUtil.clamp(alpha, 0, 0xFF);
        if (alpha == 0xFF) {
            return saveLayer(bounds, null, null, 0);
        } else {
            mTmpPaint.setAlpha(alpha);
            final int i = saveLayer(bounds, mTmpPaint, null, 0);
            mTmpPaint.reset();
            return i;
        }
    }

    /**
     * Convenience for {@link #saveLayerAlpha(Rect2f, int)} that takes the four float
     * coordinates of the bounds' rectangle.
     *
     * @see #saveLayerAlpha(Rect2f, int)
     */
    public final int saveLayerAlpha(float left, float top, float right, float bottom, int alpha) {
        mTmpRect.set(left, top, right, bottom);
        return saveLayerAlpha(mTmpRect, alpha);
    }

    /**
     * This behaves the same as {@link #save()}, but in addition it allocates and
     * redirects drawing to an offscreen rendering target (a drawing layer).
     * <p class="note"><strong>Note:</strong> this method is very expensive,
     * incurring more than double rendering cost for contained content. Avoid
     * using this method when possible.
     * <p>
     * All drawing calls are directed to a newly allocated offscreen rendering target.
     * Only when the balancing call to restore() is made, is that offscreen
     * buffer drawn back to the current target of the Canvas (which can potentially
     * be a previous layer if these calls are nested).
     * <p>
     * Optional paint applies alpha, ColorFilter, ImageFilter, and BlendMode
     * when restore() is called.
     * <p>
     * If backdrop is not null, it triggers the same initialization behavior as setting
     * {@link #INIT_WITH_PREVIOUS_SAVE_LAYER_FLAG} on saveLayerFlags: the current layer
     * is copied into the new layer, rather than initializing the new layer with
     * transparent-black. This is then filtered by backdrop (respecting the current clip).
     * <p>
     * Call restoreToCount() with returned value to restore this and subsequent saves.
     *
     * @param bounds         the maximum size the offscreen render target needs to be
     *                       (in local coordinates), may be null
     * @param paint          the paint is applied to the offscreen when restore() is
     *                       called, may be null
     * @param backdrop       if not null, this causes the current layer to be filtered by
     *                       backdrop, and then drawn into the new layer (respecting the
     *                       current clip)
     * @param saveLayerFlags options to modify layer, may be zero
     * @return depth of saved stack to pass to restoreToCount() to balance this call
     */
    public final int saveLayer(@Nullable Rect2f bounds, @Nullable Paint paint,
                               @Nullable ImageFilter backdrop, @SaveLayerFlag int saveLayerFlags) {
        if (paint != null && paint.nothingToDraw()) {
            // no need for the layer (or any of the draws until the matching restore()
            final int i = save();
            clipRect(0, 0, 0, 0);
            return i;
        } else {
            int strategy = getSaveLayerStrategy(bounds, paint, backdrop, saveLayerFlags);
            final int i = mSaveCount++;
            internalSaveLayer(bounds, paint, backdrop, saveLayerFlags, strategy);
            return i;
        }
    }

    /**
     * Convenience for {@link #saveLayer(Rect2f, Paint, ImageFilter, int)} that takes the
     * four float coordinates of the bounds' rectangle.
     *
     * @see #saveLayer(Rect2f, Paint, ImageFilter, int)
     */
    public final int saveLayer(float left, float top, float right, float bottom, @Nullable Paint paint,
                               @Nullable ImageFilter backdrop, @SaveLayerFlag int saveLayerFlags) {
        mTmpRect.set(left, top, right, bottom);
        return saveLayer(mTmpRect, paint, backdrop, saveLayerFlags);
    }

    /**
     * This call balances a previous call to save(), and is used to remove all
     * modifications to the matrix/clip state since the last save call. The
     * state is removed from the stack. It is an error to call restore() more
     * or less times than save() was called in the final state.
     *
     * @throws IllegalStateException stack underflow
     */
    public final void restore() {
        if (top().mDeferredSaveCount > 0) {
            mSaveCount--;
            top().mDeferredSaveCount--;
        } else {
            // check for underflow
            if (mMCIndex > 0) {
                willRestore();
                mSaveCount--;
                internalRestore();
                didRestore();
            } else {
                throw new IllegalStateException("Stack underflow");
            }
        }
    }

    /**
     * Returns the depth of saved matrix/clip states on the Canvas' private stack.
     * This will equal save() calls minus restore() calls, and the number of save()
     * calls less the number of restore() calls plus one. The save count of a new
     * canvas is one.
     *
     * @return depth of save state stack
     */
    public final int getSaveCount() {
        return mSaveCount;
    }

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
    public final void restoreToCount(int saveCount) {
        if (saveCount < 1) {
            throw new IllegalStateException("Stack underflow");
        }
        int n = getSaveCount() - saveCount;
        for (int i = 0; i < n; ++i) {
            restore();
        }
    }

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
    public final void translate(float dx, float dy) {
        if (dx != 0.0f || dy != 0.0f) {
            checkForDeferredSave();
            Matrix4 transform = top().mMatrix;
            transform.preTranslate(dx, dy);
            getTopDevice().setGlobalCTM(transform);
            didTranslate(dx, dy);
        }
    }

    /**
     * Scales the current matrix by sx on the x-axis and sy on the y-axis.
     * Mathematically, pre-multiply the current matrix with a scale matrix.
     * <p>
     * This has the effect of scaling the drawing by (sx, sy) before transforming
     * the result with the current matrix.
     *
     * @param sx the amount to scale on x-axis
     * @param sy the amount to scale on y-axis
     */
    public final void scale(float sx, float sy) {
        if (sx != 1.0f || sy != 1.0f) {
            checkForDeferredSave();
            Matrix4 transform = top().mMatrix;
            transform.preScale(sx, sy);
            getTopDevice().setGlobalCTM(transform);
            didScale(sx, sy);
        }
    }

    /**
     * Scales the current matrix by sx on the x-axis and sy on the y-axis at (px, py).
     * Mathematically, pre-multiply the current matrix with a translation matrix;
     * pre-multiply the current matrix with a scale matrix; then pre-multiply the
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
    public final void scale(float sx, float sy, float px, float py) {
        if (sx != 1.0f || sy != 1.0f) {
            checkForDeferredSave();
            Matrix4 transform = top().mMatrix;
            transform.preTranslate(px, py);
            transform.preScale(sx, sy);
            transform.preTranslate(-px, -py);
            getTopDevice().setGlobalCTM(transform);
            didScale(sx, sy, px, py);
        }
    }

    /**
     * Rotates the current matrix by degrees clockwise about the Z axis.
     * Mathematically, pre-multiply the current matrix with a rotation matrix;
     * <p>
     * This has the effect of rotating the drawing by degrees before transforming
     * the result with the current matrix.
     *
     * @param degrees the amount to rotate, in degrees
     */
    public final void rotate(float degrees) {
        if (degrees != 0.0f) {
            checkForDeferredSave();
            Matrix4 transform = top().mMatrix;
            transform.preRotateZ(degrees * MathUtil.DEG_TO_RAD);
            getTopDevice().setGlobalCTM(transform);
            didRotate(degrees);
        }
    }

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
    public final void rotate(float degrees, float px, float py) {
        if (degrees != 0.0f) {
            checkForDeferredSave();
            Matrix4 transform = top().mMatrix;
            transform.preTranslate(px, py);
            transform.preRotateZ(degrees * MathUtil.DEG_TO_RAD);
            transform.preTranslate(-px, -py);
            getTopDevice().setGlobalCTM(transform);
            didRotate(degrees, px, py);
        }
    }

    /**
     * Pre-multiply the current matrix by the specified matrix.
     * <p>
     * This has the effect of transforming the drawn geometry by matrix, before
     * transforming the result with the current matrix.
     *
     * @param matrix the matrix to premultiply with the current matrix
     */
    public final void concat(Matrix4 matrix) {
        if (!matrix.isIdentity()) {
            checkForDeferredSave();
            Matrix4 transform = top().mMatrix;
            transform.preConcat(matrix);
            getTopDevice().setGlobalCTM(matrix);
            didConcat(matrix);
        }
    }

    /**
     * Record a marker (provided by caller) for the current transform. This does not change anything
     * about the transform or clip, but does "name" this matrix value, so it can be referenced by
     * custom effects (who access it by specifying the same name).
     * <p>
     * Within a save frame, marking with the same name more than once just replaces the previous
     * value. However, between save frames, marking with the same name does not lose the marker
     * in the previous save frame. It is "visible" when the current save() is balanced with
     * a restore().
     */
    public final void setMarker(String name) {
        if (validateMarker(name)) {
            mMarkerStack.setMarker(name.hashCode(), top().mMatrix, mMCIndex);
        }
    }

    /**
     * @see #setMarker(String)
     */
    public final boolean findMarker(String name, Matrix4 out) {
        return validateMarker(name) && mMarkerStack.findMarker(name.hashCode(), out);
    }

    /**
     * Replaces the current matrix with the specified matrix.
     * Unlike concat(), any prior matrix state is overwritten.
     *
     * @param matrix matrix to copy, replacing the current matrix
     */
    public final void setMatrix(Matrix4 matrix) {
        checkForDeferredSave();
        internalSetMatrix(matrix);
        didSetMatrix(matrix);
    }

    /**
     * Sets the current matrix to the identity matrix.
     * Any prior matrix state is overwritten.
     */
    public final void resetMatrix() {
        setMatrix(IDENTITY_MATRIX);
    }

    /**
     * Intersect the current clip with the specified rectangle and updates
     * the stencil buffer if changed, which is expressed in local coordinates.
     * Resulting clip is aliased. The clip bounds cannot be expanded unless
     * restore() is called.
     *
     * @param rect the rectangle to intersect with the current clip
     */
    public final void clipRect(Rect2i rect) {
        mTmpRect.set(rect);
        clipRect(mTmpRect, false);
    }

    /**
     * Intersect the current clip with the specified rectangle and updates
     * the stencil buffer if changed, which is expressed in local coordinates.
     * Resulting clip is aliased. The clip bounds cannot be expanded unless
     * restore() is called.
     *
     * @param left   the left side of the rectangle to intersect with the
     *               current clip
     * @param top    the top of the rectangle to intersect with the current clip
     * @param right  the right side of the rectangle to intersect with the
     *               current clip
     * @param bottom the bottom of the rectangle to intersect with the current
     *               clip
     */
    public final void clipRect(int left, int top, int right, int bottom) {
        mTmpRect.set(left, top, right, bottom);
        clipRect(mTmpRect, false);
    }

    /**
     * Intersect the current clip with the specified rectangle and updates
     * the stencil buffer if changed, which is expressed in local coordinates.
     * Resulting clip is aliased. The clip bounds cannot be expanded unless
     * restore() is called.
     *
     * @param rect the rectangle to intersect with the current clip
     */
    public final void clipRect(Rect2f rect) {
        clipRect(rect, false);
    }

    /**
     * Intersect the current clip with the specified rectangle and updates
     * the stencil buffer if changed, which is expressed in local coordinates.
     * Resulting clip is aliased. The clip bounds cannot be expanded unless
     * restore() is called.
     *
     * @param left   the left side of the rectangle to intersect with the
     *               current clip
     * @param top    the top of the rectangle to intersect with the current clip
     * @param right  the right side of the rectangle to intersect with the
     *               current clip
     * @param bottom the bottom of the rectangle to intersect with the current
     *               clip
     */
    public final void clipRect(float left, float top, float right, float bottom) {
        mTmpRect.set(left, top, right, bottom);
        clipRect(mTmpRect, false);
    }

    /**
     * Intersect the current clip with the specified rectangle and updates
     * the stencil buffer if changed, which is expressed in local coordinates,
     * with an aliased or anti-aliased clip edge. The clip bounds cannot be
     * expanded unless restore() is called.
     *
     * @param rect the rectangle to intersect with the current clip
     * @param doAA true if clip is to be anti-aliased
     */
    public final void clipRect(Rect2f rect, boolean doAA) {
        if (rect.isFinite()) {
            checkForDeferredSave();
            rect.sort();
            onClipRect(rect, doAA);
        }
    }

    /**
     * Intersect the current clip with the specified rectangle and updates
     * the stencil buffer if changed, which is expressed in local coordinates,
     * with an aliased or anti-aliased clip edge. The clip bounds cannot be
     * expanded unless restore() is called.
     *
     * @param left   the left side of the rectangle to intersect with the
     *               current clip
     * @param top    the top of the rectangle to intersect with the current clip
     * @param right  the right side of the rectangle to intersect with the
     *               current clip
     * @param bottom the bottom of the rectangle to intersect with the current
     *               clip
     * @param doAA   true if clip is to be anti-aliased
     */
    public final void clipRect(float left, float top, float right, float bottom, boolean doAA) {
        mTmpRect.set(left, top, right, bottom);
        clipRect(mTmpRect, doAA);
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
     * @param rect the rect to compare with the current clip
     * @return true if the given rect (transformed by the canvas' matrix)
     * intersecting with the maximum rect representing the canvas' clip is empty
     */
    public final boolean quickReject(Rect2fc rect) {
        var devRect = mTmpQuickBounds2;
        top().mMatrix.mapRect(rect, devRect);
        return !devRect.isFinite() || !devRect.intersects(mQuickRejectBounds);
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
    public final boolean quickReject(float left, float top, float right, float bottom) {
        var devRect = mTmpQuickBounds2;
        top().mMatrix.mapRect(left, top, right, bottom, devRect);
        return !devRect.isFinite() || !devRect.intersects(mQuickRejectBounds);
    }

    /**
     * Returns the bounds of clip, transformed by inverse of the transform
     * matrix. If clip is empty, return false, and set bounds to empty, where
     * all rect sides equal zero.
     * <p>
     * The <code>bounds</code> is outset by one to account for partial pixel
     * coverage if clip is anti-aliased.
     *
     * @param bounds the bounds of clip in local coordinates
     * @return true if clip bounds is not empty
     */
    public final boolean getLocalClipBounds(Rect2f bounds) {
        Device device = getTopDevice();
        if (device.isClipEmpty()) {
            bounds.setEmpty();
            return false;
        } else {
            // if we can't invert the matrix, we can't return local clip bounds
            if (!top().mMatrix.invert(mTmpMatrix)) {
                bounds.setEmpty();
                return false;
            }
            bounds.set(device.getClipBounds());
            device.getDeviceToGlobal().mapRect(bounds);
            bounds.roundOut(bounds);
            // adjust it outwards in case we are antialiasing
            bounds.outset(1.0f, 1.0f);
            mTmpMatrix.mapRect(bounds);
            return !bounds.isEmpty();
        }
    }

    /**
     * Returns the bounds of clip, unaffected by the transform matrix. If clip
     * is empty, return false, and set bounds to empty, where all rect sides
     * equal zero.
     * <p>
     * Unlike getLocalClipBounds(), bounds is not outset.
     *
     * @param bounds the bounds of clip in device coordinates
     * @return true if clip bounds is not empty
     */
    public final boolean getDeviceClipBounds(Rect2i bounds) {
        Device device = getTopDevice();
        if (device.isClipEmpty()) {
            bounds.setEmpty();
            return false;
        } else {
            bounds.set(device.getClipBounds());
            device.getDeviceToGlobal().mapRectOut(bounds, bounds);
            return !bounds.isEmpty();
        }
    }

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
     * Makes SkCanvas contents undefined. Subsequent calls that read SkCanvas pixels,
     * such as drawing with SkBlendMode, return undefined results. discard() does
     * not change clip or SkMatrix.
     * <p>
     * discard() may do nothing, depending on the implementation of SkSurface or SkDevice
     * that created SkCanvas.
     * <p>
     * discard() allows optimized performance on subsequent draws by removing
     * cached data associated with SkSurface or SkDevice.
     * It is not necessary to call discard() once done with SkCanvas;
     * any cached data is deleted when owning SkSurface or SkDevice is deleted.
     */
    public final void discard() {
        onDiscard();
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
    public final void drawColor(@ColorInt int color, BlendMode mode) {
        Paint paint = mTmpPaint;
        paint.setColor(color);
        paint.setBlendMode(mode);
        drawPaint(paint);
        paint.reset();
    }

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
    public final void drawColor(float r, float g, float b, float a, BlendMode mode) {
        Paint paint = mTmpPaint;
        paint.setColor4f(r, g, b, a);
        paint.setBlendMode(mode);
        drawPaint(paint);
        paint.reset();
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
    public final void drawPaint(Paint paint) {
        onDrawPaint(paint);
    }

    /**
     * The {@code PointMode} selects if an array of points is drawn as discrete points,
     * as lines, or as an open polygon.
     */
    @MagicConstant(intValues = {POINT_MODE_POINTS, POINT_MODE_LINES, POINT_MODE_POLYGON})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PointMode {
    }

    /**
     * Draw each point separately.
     */
    public static final int POINT_MODE_POINTS = 0;
    /**
     * Draw each pair of points as a line segment.
     */
    public static final int POINT_MODE_LINES = 1;
    /**
     * Draw the array of points as an open polygon.
     */
    public static final int POINT_MODE_POLYGON = 2;

    /**
     * Draws pts using the current matrix, clip and specified paint.
     * <var>count</var> is the number of points; if count is less than one, has no effect.
     * mode may be one of: {@link #POINT_MODE_POINTS}, {@link #POINT_MODE_LINES}, or
     * {@link #POINT_MODE_POLYGON}.
     * <p>
     * If mode is {@link #POINT_MODE_POINTS}, the shape of point drawn depends on paint
     * Cap. If paint is set to {@link Paint#CAP_ROUND}, each point draws a
     * circle of diameter paint stroke width. If paint is set to {@link Paint#CAP_SQUARE}
     * or {@link Paint#CAP_BUTT}, each point draws a square of width and height
     * paint stroke width.
     * <p>
     * If mode is {@link #POINT_MODE_LINES}, each pair of points draws a line segment.
     * One line is drawn for every two points; each point is used once. If count is odd,
     * the last point is ignored.
     * <p>
     * If mode is {@link #POINT_MODE_POLYGON}, each adjacent pair of points draws a line segment.
     * count minus one lines are drawn; the first and last point are used once.
     * <p>
     * Each line segment respects paint Cap and paint stroke width.
     * Paint style is ignored, as if were set to {@link Paint#STROKE}.
     * <p>
     * Always draws each element one at a time; is not affected by paint join,
     * and unlike drawPath(), does not create a mask from all points
     * and lines before drawing.
     *
     * @param mode   whether pts draws points or lines
     * @param pts    array of points to draw
     * @param offset offset in pts array, i.e., number of floats to skip
     * @param count  number of points in pts array
     * @param paint  stroke, blend, color, and so on, used to draw
     */
    public final void drawPoints(@PointMode int mode,
                                 float[] pts, int offset, int count,
                                 Paint paint) {
        if (count <= 0) {
            return;
        }
        assert pts.length >= offset + count * 2;
        var oldStyle = paint.getStyle();
        var oldCap = paint.getStrokeCap();
        paint.setStyle(Paint.STROKE);
        if (mode == POINT_MODE_POINTS && oldCap == Paint.CAP_BUTT) {
            paint.setStrokeCap(Paint.CAP_SQUARE);
        }
        onDrawPoints(mode, pts, offset, count, paint);
        paint.setStyle(oldStyle);
        paint.setStrokeCap(oldCap);
    }

    /**
     * Draws point at (x, y) using the current matrix, clip and specified paint.
     * <p>
     * The shape of point drawn depends on paint cap.
     * If paint is set to {@link Paint#CAP_ROUND}, draw a circle of diameter
     * paint stroke width. If paint is set to {@link Paint#CAP_SQUARE} or {@link Paint#CAP_BUTT},
     * draw a square of width and height paint stroke width.
     * Paint style is ignored, as if were set to {@link Paint#STROKE}.
     *
     * @param x     center X of circle or square
     * @param y     center Y of circle or square
     * @param paint stroke, blend, color, and so on, used to draw
     */
    public final void drawPoint(float x, float y,
                                Paint paint) {
        // draw a point by filling the stroke
        var oldStyle = paint.getStyle();
        paint.setStyle(Paint.FILL);
        drawPoint(x, y,
                paint.getStrokeCap(),
                paint.getStrokeWidth(),
                paint);
        paint.setStyle(oldStyle);
    }

    /**
     * Draws point at (x, y) using the current matrix, clip and specified paint.
     * <p>
     * The shape of point drawn depends on <var>cap</var>.
     * If <var>cap</var> is {@link Paint#CAP_ROUND}, draw a circle of diameter
     * <var>size</var>. If <var>cap</var> is {@link Paint#CAP_SQUARE} or {@link Paint#CAP_BUTT},
     * draw a square of width and height <var>size</var>. In paint: Style determines if point
     * is stroked or filled; If stroked (i.e. double stroke), stroke width describes the
     * shape outline thickness, Cap describes line ends, Join draws the corners rounded
     * or square, and Align determines the position or direction to stroke.
     *
     * @param x     center X of circle or square
     * @param y     center Y of circle or square
     * @param cap   the line end used to determine the shape of point
     * @param size  the diameter of circle or width and height of square
     * @param paint stroke, blend, color, and so on, used to draw
     */
    public final void drawPoint(float x, float y,
                                @Paint.Cap int cap, float size, Paint paint) {
        if (size >= 0) {
            float radius = size * 0.5f;
            if (cap == Paint.CAP_ROUND) {
                drawCircle(x, y, radius, paint);
            } else {
                drawRect(x - radius, y - radius, x + radius, y + radius, paint);
            }
        }
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
    public final void drawLine(float x0, float y0, float x1, float y1,
                               Paint paint) {
        // draw a line by filling the stroke
        var oldStyle = paint.getStyle();
        paint.setStyle(Paint.FILL);
        drawLine(x0, y0, x1, y1,
                paint.getStrokeCap(),
                paint.getStrokeWidth(),
                paint);
        paint.setStyle(oldStyle);
    }

    /**
     * Draw a line segment from (x0, y0) to (x1, y1) using the current matrix,
     * clip and specified paint. Note that line shape is a stroked line rather
     * than a line path, you can fill or stroke the stroked line.
     * <p>
     * The <var>cap</var> describes the end of the line shape, the <var>width</var>
     * describes the line shape thickness. In paint: Style determines if line shape
     * is stroked or filled; If stroked (i.e. double stroke), stroke width describes the
     * shape outline thickness, Cap describes line ends, Join draws the corners rounded
     * or square, and Align determines the position or direction to stroke.
     *
     * @param x0    the start of line segment on x-axis
     * @param y0    the start of line segment on y-axis
     * @param x1    the end of line segment on x-axis
     * @param y1    the end of line segment on y-axis
     * @param cap   the line end used to determine the shape of the line
     * @param width the thickness of line segment
     * @param paint the paint used to draw the line
     */
    public final void drawLine(float x0, float y0, float x1, float y1,
                               @Paint.Cap int cap, float width, Paint paint) {
        if (width >= 0) {
            onDrawLine(x0, y0, x1, y1, cap, width, paint);
        }
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
    public final void drawRect(Rect2fc r, Paint paint) {
        mTmpRect.set(r);
        mTmpRect.sort();
        onDrawRect(mTmpRect, paint);
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
    public final void drawRect(Rect2ic r, Paint paint) {
        mTmpRect.set(r);
        mTmpRect.sort();
        onDrawRect(mTmpRect, paint);
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
    public void drawRect(float left, float top, float right, float bottom, Paint paint) {
        mTmpRect.set(left, top, right, bottom);
        mTmpRect.sort();
        onDrawRect(mTmpRect, paint);
    }

    /**
     * Draw a rectangle with rounded corners within a rectangular bounds. The round
     * rectangle will be filled or framed based on the Style in the paint.
     *
     * @param rect   The rectangular bounds of the round rect to be drawn
     * @param radius the radius used to round the corners
     * @param paint  the paint used to draw the round rectangle
     */
    public final void drawRoundRect(Rect2fc rect, float radius, Paint paint) {
        mTmpRoundRect.setRectXY(rect, radius, radius);
        onDrawRoundRect(mTmpRoundRect, paint);
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
        mTmpRoundRect.setRectXY(left, top, right, bottom, radius, radius);
        onDrawRoundRect(mTmpRoundRect, paint);
    }

    /**
     * Draw a rectangle with rounded corners within a rectangular bounds. The round
     * rectangle will be filled or framed based on the Style in the paint.
     *
     * @param rect  The rectangular bounds of the round rect to be drawn
     * @param rUL   the radius used to round the upper left corner
     * @param rUR   the radius used to round the upper right corner
     * @param rLR   the radius used to round the lower right corner
     * @param rLL   the radius used to round the lower left corner
     * @param paint the paint used to draw the round rectangle
     */
    public final void drawRoundRect(Rect2f rect, float rUL, float rUR, float rLR, float rLL, Paint paint) {
        drawRoundRect(rect.mLeft, rect.mTop, rect.mRight, rect.mBottom, rUL, rUR, rLR, rLL, paint);
    }

    /**
     * Draw a rectangle with rounded corners within a rectangular bounds. The round
     * rectangle will be filled or framed based on the Style in the paint.
     *
     * @param left   the left of the rectangular bounds
     * @param top    the top of the rectangular bounds
     * @param right  the right of the rectangular bounds
     * @param bottom the bottom of the rectangular bounds
     * @param rUL    the radius used to round the upper left corner
     * @param rUR    the radius used to round the upper right corner
     * @param rLR    the radius used to round the lower right corner
     * @param rLL    the radius used to round the lower left corner
     * @param paint  the paint used to draw the round rectangle
     */
    public void drawRoundRect(float left, float top, float right, float bottom,
                              float rUL, float rUR, float rLR, float rLL, Paint paint) {
    }

    public void drawRoundRect(RoundRect roundRect, Paint paint) {
        onDrawRoundRect(roundRect, paint);
    }

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
    public final void drawCircle(float cx, float cy, float radius, Paint paint) {
        onDrawCircle(cx, cy, Math.max(radius, 0.0f), paint);
    }

    //TODO draw ellipse and oval

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
    public final void drawArc(float cx, float cy, float radius, float startAngle,
                              float sweepAngle, Paint paint) {
        if (radius > 0 && sweepAngle != 0) {
            // draw an arc by filling the stroke
            var oldStyle = paint.getStyle();
            paint.setStyle(Paint.FILL);
            drawArc(cx, cy, radius,
                    startAngle, sweepAngle,
                    paint.getStrokeCap(),
                    paint.getStrokeWidth(),
                    paint);
            paint.setStyle(oldStyle);
        }
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
     * @param width      the thickness of arc segment
     * @param paint      the paint used to draw the arc
     */
    public final void drawArc(float cx, float cy, float radius, float startAngle,
                              float sweepAngle, @Paint.Cap int cap, float width, Paint paint) {
        onDrawArc(cx, cy, radius, startAngle, sweepAngle, cap, width, paint);
    }

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
    public final void drawPie(float cx, float cy, float radius, float startAngle,
                              float sweepAngle, Paint paint) {
        if (radius > 0 && sweepAngle != 0) {
            onDrawPie(cx, cy, radius, startAngle, sweepAngle, paint);
        }
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
    public final void drawChord(float cx, float cy, float radius, float startAngle,
                                float sweepAngle, Paint paint) {
        if (radius > 0 && sweepAngle != 0) {
            onDrawChord(cx, cy, radius, startAngle, sweepAngle, paint);
        }
    }

    //TODO draw path

    /**
     * Draw a triangle using the specified paint. The three vertices are in counter-clockwise order.
     * <p>
     * The Style is ignored in the paint, triangles are always "filled".
     * The smooth radius and gradient color are ignored as well.
     *
     * @param x0    the x-coordinate of the first vertex
     * @param y0    the y-coordinate of the first vertex
     * @param x1    the x-coordinate of the second vertex
     * @param y1    the y-coordinate of the second vertex
     * @param x2    the x-coordinate of the last vertex
     * @param y2    the y-coordinate of the last vertex
     * @param paint the paint used to draw the triangle
     */
    public void drawTriangle(float x0, float y0, float x1, float y1, float x2, float y2,
                             Paint paint) {
        //TODO
    }

    /**
     * The {@code SrcRectConstraint} controls the behavior at the edge of source rect,
     * provided to drawImageRect() when there is any filtering. If STRICT is set,
     * then extra code is used to ensure it never samples outside the src-rect.
     * {@link #SRC_RECT_CONSTRAINT_STRICT} disables the use of mipmaps and anisotropic filtering.
     */
    @MagicConstant(intValues = {SRC_RECT_CONSTRAINT_FAST, SRC_RECT_CONSTRAINT_STRICT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SrcRectConstraint {
    }

    /**
     * Sample outside bounds; faster
     */
    public static final int SRC_RECT_CONSTRAINT_FAST = 0;
    /**
     * Sample only inside bounds; slower.
     */
    public static final int SRC_RECT_CONSTRAINT_STRICT = 1;

    /**
     * Draw the specified image with its top/left corner at (x,y), using the
     * specified paint, transformed by the current matrix. The Style and smooth
     * radius is ignored in the paint, images are always filled.
     *
     * @param image the image to be drawn
     * @param x     the position of the left side of the image being drawn
     * @param y     the position of the top side of the image being drawn
     * @param paint the paint used to draw the image, null meaning a default paint
     */
    public final void drawImage(@RawPtr Image image, float x, float y, SamplingOptions sampling,
                                @Nullable Paint paint) {
        if (image == null) {
            return;
        }
        var src = new Rect2f(0, 0, image.getWidth(), image.getHeight());
        var dst = new Rect2f(src);
        dst.offset(x, y);
        drawImageRect(image, src, dst, sampling, paint, SRC_RECT_CONSTRAINT_FAST);
    }

    public final void drawImageRect(@RawPtr Image image, Rect2fc dst, SamplingOptions sampling,
                                    @Nullable Paint paint) {
        if (image == null) {
            return;
        }
        var src = new Rect2f(0, 0, image.getWidth(), image.getHeight());
        drawImageRect(image, src, dst, sampling, paint, SRC_RECT_CONSTRAINT_FAST);
    }

    public final void drawImageRect(@RawPtr Image image, Rect2fc src, Rect2fc dst, SamplingOptions sampling,
                                    @Nullable Paint paint, @SrcRectConstraint int constraint) {
        if (image == null) {
            return;
        }
        if (!src.isFinite() || src.isEmpty() || !dst.isFinite() || dst.isEmpty()) {
            return;
        }
        var cleanedPaint = mTmpPaint;
        if (paint != null) {
            cleanedPaint.set(paint);
            cleanedPaint.setStyle(Paint.FILL);
            cleanedPaint.setPathEffect(null);
        }
        if (constraint == SRC_RECT_CONSTRAINT_STRICT) {
            if (sampling.mMipmapMode != SamplingOptions.MIPMAP_MODE_NONE) {
                // Use linear filter if either is linear
                sampling = SamplingOptions.make(sampling.mMinFilter | sampling.mMagFilter);
            } else if (sampling.isAnisotropy()) {
                sampling = SamplingOptions.LINEAR;
            }
        }
        onDrawImageRect(image, src, dst, sampling, cleanedPaint, constraint);
        cleanedPaint.reset();
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
    public final void drawImage(Image image, @Nullable Rect2i src, Rect2f dst, @Nullable Paint paint) {
        if (src == null) {
            drawImage(image, 0, 0, image.getWidth(), image.getHeight(),
                    dst.mLeft, dst.mTop, dst.mRight, dst.mBottom, paint);
        } else {
            drawImage(image, src.mLeft, src.mTop, src.mRight, src.mBottom,
                    dst.mLeft, dst.mTop, dst.mRight, dst.mBottom, paint);
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
    public final void drawImage(Image image, @Nullable Rect2i src, Rect2i dst, @Nullable Paint paint) {
        if (src == null) {
            drawImage(image, 0, 0, image.getWidth(), image.getHeight(),
                    dst.mLeft, dst.mTop, dst.mRight, dst.mBottom, paint);
        } else {
            drawImage(image, src.mLeft, src.mTop, src.mRight, src.mBottom,
                    dst.mLeft, dst.mTop, dst.mRight, dst.mBottom, paint);
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
    public void drawImage(Image image, float srcLeft, float srcTop, float srcRight, float srcBottom,
                          float dstLeft, float dstTop, float dstRight, float dstBottom, @Nullable Paint paint) {

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
    public void drawRoundImage(Image image, float left, float top,
                               float radius, Paint paint) {

    }

    public void drawGlyphs(int[] glyphs, int glyphOffset,
                           float[] positions, int positionOffset,
                           int glyphCount,
                           float originX, float originY,
                           Font font, Paint paint) {
        if (glyphCount <= 0) {
            return;
        }

        GlyphRunList glyphRunList = mScratchGlyphRunBuilder.setGlyphRunList(
                glyphs, glyphOffset, positions, positionOffset, glyphCount, font, originX, originY, paint);
        onDrawGlyphRunList(glyphRunList, paint);
    }

    /**
     * Returns true if clip is empty; that is, nothing will draw.
     * <p>
     * May do work when called; it should not be called more often than needed.
     * However, once called, subsequent calls perform no work until clip changes.
     *
     * @return true if clip is empty
     */
    public final boolean isClipEmpty() {
        return getTopDevice().isClipEmpty();
    }

    /**
     * Returns true if clip is a Rect and not empty.
     * Returns false if the clip is empty, or if it is complex.
     *
     * @return true if clip is a Rect and not empty
     */
    public final boolean isClipRect() {
        return getTopDevice().isClipRect();
    }

    /**
     * Returns the current transform from local coordinates to the 'device', which for most
     * purposes means pixels.
     *
     * @param storage transformation from local coordinates to device / pixels.
     */
    public final void getLocalToDevice(Matrix4 storage) {
        storage.set(top().mMatrix);
    }

    /**
     * Draws saved layers, if any.
     * Frees up resources used by Canvas.
     */
    @Override
    public void close() {
        mBaseDevice = RefCnt.move(mBaseDevice);
        if (mSurface != null) {
            throw new IllegalStateException("Surface-created canvas is owned by Surface, use Surface#close instead");
        }
    }

    protected void willSave() {
    }

    protected int getSaveLayerStrategy(@Nullable Rect2f bounds, @Nullable Paint paint,
                                       @Nullable ImageFilter backdrop,
                                       @SaveLayerFlag int saveLayerFlags) {
        return FULL_LAYER_SAVE_LAYER_STRATEGY;
    }

    protected void willRestore() {
    }

    protected void didRestore() {
    }

    protected void didTranslate(float dx, float dy) {
    }

    protected void didScale(float sx, float sy) {
    }

    protected void didScale(float sx, float sy, float px, float py) {
    }

    protected void didRotate(float degrees) {
    }

    protected void didRotate(float degrees, float px, float py) {
    }

    protected void didConcat(Matrix4 matrix) {
    }

    protected void didSetMarker(String name) {
    }

    protected void didSetMatrix(Matrix4 matrix) {
    }

    protected void onClipRect(Rect2f rect, boolean doAA) {
        getTopDevice().clipRect(rect, ClipOp.CLIP_OP_INTERSECT, doAA);
        computeQuickRejectBounds();
    }

    @Nonnull
    private MCRec push() {
        final int i = ++mMCIndex;
        MCRec[] stack = mMCStack;
        if (i == stack.length) {
            mMCStack = new MCRec[i + (i >> 1)];
            System.arraycopy(stack, 0, mMCStack, 0, i);
            stack = mMCStack;
        }
        MCRec rec = stack[i];
        if (rec == null) {
            stack[i] = rec = new MCRec();
        }
        return rec;
    }

    private void pop() {
        if (mMCIndex-- >= MAX_MC_POOL_SIZE) {
            mMCStack[mMCIndex + 1] = null;
        }
    }

    @Nonnull
    private MCRec top() {
        return mMCStack[mMCIndex];
    }

    // the top-most device in the stack, will change within saveLayer()'s. All drawing and clipping
    // operations should route to this device.
    @Nonnull
    private Device getTopDevice() {
        return top().mDevice;
    }

    @Nullable
    private SurfaceDrawContext topDeviceSurfaceDrawContext() {
        return getTopDevice().getSurfaceDrawContext();
    }

    private void checkForDeferredSave() {
        if (top().mDeferredSaveCount > 0) {
            doSave();
        }
    }

    private void doSave() {
        willSave();
        top().mDeferredSaveCount--;
        internalSave();
    }

    private void internalSave() {
        // get before push stack
        MCRec rec = top();
        push().set(rec);

        getTopDevice().save();
    }

    private void internalRestore() {
        mMarkerStack.restore(mMCIndex);

        // now do the normal restore()
        pop();

        if (mMCIndex == -1) {
            // this was the last record, restored during the destruction of the Canvas
            return;
        }

        getTopDevice().restore(top().mMatrix);

        // Update the quick-reject bounds in case the restore changed the top device or the
        // removed save record had included modifications to the clip stack.
        computeQuickRejectBounds();
    }

    private void internalSaveLayer(@Nullable Rect2f bounds, @Nullable Paint paint,
                                   @Nullable ImageFilter backdrop,
                                   @SaveLayerFlag int saveLayerFlags,
                                   int saveLayerStrategy) {
        // if we have a backdrop filter, then we must apply it to the entire layer (clip-bounds)
        // regardless of any hint-rect from the caller.
        if (backdrop != null) {
            bounds = null;
        }

        ImageFilter imageFilter = /*paint != null ? paint.getImageFilter() : */null;
        Matrix4 stashedMatrix = top().mMatrix;

        if (imageFilter != null) {
            //TODO
        }

        // do this before we create the layer. We don't call the public save() since
        // that would invoke a possibly overridden virtual
        internalSave();
    }

    private void internalSetMatrix(Matrix4 matrix) {
        Matrix4 transform = top().mMatrix;
        transform.set(matrix);
        getTopDevice().setGlobalCTM(transform);
    }

    private void internalDrawPaint(Paint paint) {
        // drawPaint does not call internalQuickReject() because computing its geometry is not free
        // (see getLocalClipBounds()), and the two conditions below are sufficient.
        if (paint.nothingToDraw() || isClipEmpty()) {
            return;
        }

        if (aboutToDraw(paint)) {
            getTopDevice().drawPaint(paint);
        }
    }

    /**
     * Compute the clip's bounds based on all clipped Device's reported device bounds transformed
     * into the canvas' global space.
     */
    private void computeQuickRejectBounds() {
        Device device = getTopDevice();
        if (device.isClipEmpty()) {
            mQuickRejectBounds.setEmpty();
        } else {
            mQuickRejectBounds.set(device.getClipBounds());
            device.getDeviceToGlobal().mapRect(mQuickRejectBounds);
            // Expand bounds out by 1 in case we are anti-aliasing.  We store the
            // bounds as floats to enable a faster quick reject implementation.
            mQuickRejectBounds.outset(1.0f, 1.0f);
        }
    }

    private boolean aboutToDraw(Paint paint) {
        return predrawNotify(false);
    }

    // notify our surface (if we have one) that we are about to draw, so it
    // can perform copy-on-write or invalidate any cached images
    // returns false if the copy failed
    private boolean predrawNotify(boolean willOverwritesEntireSurface) {
        if (mSurface != null) {
            return mSurface.aboutToDraw(willOverwritesEntireSurface
                    ? Surface.kDiscard_ContentChangeMode
                    : Surface.kPreserve_ContentChangeMode);
        }
        return true;
    }

    private boolean internalQuickReject(Rect2fc bounds, Paint paint) {
        return internalQuickReject(bounds, paint, null);
    }

    private boolean internalQuickReject(Rect2fc bounds, Paint paint,
                                        Matrixc matrix) {
        if (!bounds.isFinite() || paint.nothingToDraw()) {
            return true;
        }

        if (paint.canComputeFastBounds(null)) {
            var tmp = mTmpQuickBounds1;
            if (matrix != null) {
                matrix.mapRect(bounds, tmp);
            } else {
                tmp.set(bounds);
            }
            paint.computeFastBounds(null, tmp, tmp);
            return quickReject(tmp);
        }

        return false;
    }

    protected void onDiscard() {
        if (mSurface != null) {
            mSurface.aboutToDraw(Surface.kDiscard_ContentChangeMode);
        }
    }

    protected void onDrawPaint(Paint paint) {
        internalDrawPaint(paint);
    }

    protected void onDrawPoints(int mode, float[] pts, int offset,
                                int count, Paint paint) {
        if (count <= 0 || paint.nothingToDraw()) {
            return;
        }
        Rect2f bounds = new Rect2f();
        bounds.setBounds(pts, offset, count);
        if (internalQuickReject(bounds, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            getTopDevice().drawPoints(mode, pts, offset, count, paint);
        }
    }

    protected void onDrawLine(float x0, float y0, float x1, float y1,
                              @Paint.Cap int cap, float width, Paint paint) {
        Rect2f bounds = new Rect2f();
        bounds.set(x0, y0, x1, y1);
        bounds.sort();
        float outset;
        if (cap == Paint.CAP_SQUARE) {
            outset = MathUtil.SQRT2 * width * 0.5f;
        } else {
            outset = width * 0.5f;
        }
        bounds.outset(outset, outset);

        if (internalQuickReject(bounds, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            getTopDevice().drawLine(x0, y0, x1, y1, cap, width, paint);
        }
    }

    protected void onDrawRect(Rect2fc r, Paint paint) {
        if (internalQuickReject(r, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            getTopDevice().drawRect(r, paint);
        }
    }

    protected void onDrawRoundRect(RoundRect roundRect, Paint paint) {
        var bounds = mTmpRect;
        roundRect.getBounds(bounds);
        if (roundRect.isRect()) {
            bounds.sort();
            onDrawRect(bounds, paint);
            return;
        }

        if (internalQuickReject(bounds, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            getTopDevice().drawRoundRect(roundRect, paint);
        }
    }

    protected void onDrawCircle(float cx, float cy, float radius, Paint paint) {
        var bounds = mTmpRect;
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius);

        if (internalQuickReject(bounds, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            getTopDevice().drawCircle(cx, cy, radius, paint);
        }
    }

    protected void onDrawArc(float cx, float cy, float radius, float startAngle,
                             float sweepAngle, @Paint.Cap int cap, float width, Paint paint) {
        Rect2f bounds = new Rect2f();
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius);
        float outset = cap == Paint.CAP_SQUARE ? MathUtil.SQRT2 * width * 0.5f : width * 0.5f;
        bounds.outset(outset, outset);

        if (internalQuickReject(bounds, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            getTopDevice().drawArc(cx, cy, radius, startAngle, sweepAngle, cap, width, paint);
        }
    }

    protected void onDrawPie(float cx, float cy, float radius, float startAngle,
                             float sweepAngle, Paint paint) {
        Rect2f bounds = new Rect2f();
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius);

        if (internalQuickReject(bounds, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            getTopDevice().drawPie(cx, cy, radius, startAngle, sweepAngle, paint);
        }
    }

    protected void onDrawChord(float cx, float cy, float radius, float startAngle,
                               float sweepAngle, Paint paint) {
        Rect2f bounds = new Rect2f();
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius);

        if (internalQuickReject(bounds, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            getTopDevice().drawChord(cx, cy, radius, startAngle, sweepAngle, paint);
        }
    }

    protected void onDrawImageRect(Image image, Rect2fc src, Rect2fc dst,
                                   SamplingOptions sampling, Paint paint,
                                   int constraint) {
        if (internalQuickReject(dst, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            getTopDevice().drawImageRect(image, src, dst, sampling, paint, constraint);
        }
    }

    protected void onDrawGlyphRunList(GlyphRunList glyphRunList, Paint paint) {
        Rect2f bounds = new Rect2f(glyphRunList.mSourceBounds);
        bounds.offset(glyphRunList.mOriginX, glyphRunList.mOriginY);
        if (internalQuickReject(bounds, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            getTopDevice().drawGlyphRunList(this, glyphRunList, paint);
        }
    }

    /**
     * This is the record we keep for each save/restore level in the stack.
     * Since a level optionally copies the matrix and/or stack, we have pointers
     * for these fields. If the value is copied for this level, the copy is
     * stored in the ...Storage field, and the pointer points to that. If the
     * value is not copied for this level, we ignore ...Storage, and just point
     * at the corresponding value in the previous level in the stack.
     */
    private static final class MCRec {

        // This points to the device of the top-most layer (which may be lower in the stack), or
        // to the canvas's fBaseDevice. The MCRec does not own the device.
        Device mDevice;

        final Matrix4 mMatrix = new Matrix4();
        int mDeferredSaveCount;

        MCRec() {
        }

        MCRec(Device device) {
            mDevice = device;
            mMatrix.setIdentity();
            mDeferredSaveCount = 0;
        }

        void set(MCRec prev) {
            mDevice = prev.mDevice;
            mMatrix.set(prev.mMatrix);
            mDeferredSaveCount = 0;
        }
    }

    private static boolean validateMarker(String name) {
        if (name.isEmpty()) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c != '_' && !Character.isLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }
}
