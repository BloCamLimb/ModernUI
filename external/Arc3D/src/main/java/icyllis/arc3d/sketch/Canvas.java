/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.sketch;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.Context;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

    // the bottom-most device in the stack, only changed by init(). Image properties and the final
    // canvas pixels are determined by this device
    @SharedPtr
    private Device mRootDevice;

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

    // a temp rect that used with arguments
    private final Rect2f mTmpRect = new Rect2f();
    private final Rect2f mTmpRect2 = new Rect2f();
    private final Rect2f mTmpQuickBounds = new Rect2f();
    private final Rect2f mTmpQuickBounds2 = new Rect2f();
    private final Matrix mTmpMatrix = new Matrix();
    private final Matrix4 mTmpMatrix44 = new Matrix4();
    private final Paint mTmpPaint = new Paint();

    private final GlyphRunBuilder mScratchGlyphRunBuilder = new GlyphRunBuilder();

    /**
     * Creates an empty Canvas with no backing device or pixels, with
     * a width and height of zero.
     */
    public Canvas() {
        this((Device) null);
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

    protected Canvas(@NonNull Rect2ic bounds) {
        this(new NoPixelsDevice(bounds.isEmpty() ? Rect2i.empty() : bounds));
    }

    @ApiStatus.Internal
    public Canvas(@Nullable @SharedPtr Device device) {
        if (device == null) {
            device = new NoPixelsDevice(Rect2i.empty());
        }
        mSaveCount = 1;
        mMCStack[0] = new MCRec(device);
        mRootDevice = device;
        computeQuickRejectBounds();
    }

    /**
     * Returns ImageInfo for Canvas. If Canvas is not associated with raster surface or
     * GPU surface, returned ColorType is set to {@link ColorInfo#CT_UNKNOWN}.
     *
     * @return dimensions and ColorType of Canvas
     */
    @NonNull
    public final ImageInfo getImageInfo() {
        return onGetImageInfo();
    }

    /**
     * Gets the size of the base or root layer in global canvas coordinates. The
     * origin of the base layer is always (0,0). The area available for drawing may be
     * smaller (due to clipping or saveLayer).
     *
     * @return integral width of base layer
     */
    public int getBaseLayerWidth() {
        return mRootDevice.getWidth();
    }

    /**
     * Gets the size of the base or root layer in global canvas coordinates. The
     * origin of the base layer is always (0,0). The area available for drawing may be
     * smaller (due to clipping or saveLayer).
     *
     * @return integral height of base layer
     */
    public int getBaseLayerHeight() {
        return mRootDevice.getHeight();
    }

    /**
     * Creates Surface matching info, and associates it with Canvas.
     * Returns null if no match found.
     *
     * @param info width, height, ColorType and AlphaType
     * @return Surface matching info, or null if no match is available
     */
    @Nullable
    @SharedPtr
    public final Surface makeSurface(ImageInfo info) {
        return onNewSurface(info);
    }

    /**
     * Returns GPU context of the GPU surface associated with Canvas.
     *
     * @return GPU context, if available; null otherwise
     */
    @RawPtr
    @Nullable
    public Context getCommandContext() {
        return topDevice().getCommandContext();
    }

    /**
     * Sometimes a canvas is owned by a surface. If it is, getSurface() will return a bare
     * pointer to that surface, else this will return null.
     */
    @RawPtr
    @Nullable
    public final Surface getSurface() {
        return mSurface;
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
    public final int save() {
        final int i = mSaveCount++;
        top().mDeferredSaveCount++;
        return i; // return our prev value
    }

    //TODO these saveLayer() methods are WIP

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
            assert mSaveCount > 1;
            mSaveCount--;
            top().mDeferredSaveCount--;
        } else {
            // check for underflow
            if (mMCIndex > 0) {
                willRestore();
                assert mSaveCount > 1;
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
        int n = mSaveCount - saveCount;
        for (int i = 0; i < n; ++i) {
            restore();
        }
    }

    //////////////////////////////////////////////////////////////////////////////

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
            topDevice().setGlobalCTM(transform);
            didTranslate(dx, dy, 0);
        }
    }

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
    public final void translate(float dx, float dy, float dz) {
        if (dx != 0.0f || dy != 0.0f || dz != 0.0f) {
            checkForDeferredSave();
            Matrix4 transform = top().mMatrix;
            transform.preTranslate(dx, dy, dz);
            topDevice().setGlobalCTM(transform);
            didTranslate(dx, dy, dz);
        }
    }

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
    public final void scale(float sx, float sy) {
        if (sx != 1.0f || sy != 1.0f) {
            checkForDeferredSave();
            Matrix4 transform = top().mMatrix;
            transform.preScale(sx, sy);
            topDevice().setGlobalCTM(transform);
            didScale(sx, sy, 1);
        }
    }

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
    public final void scale(float sx, float sy, float sz) {
        if (sx != 1.0f || sy != 1.0f || sz != 1.0f) {
            checkForDeferredSave();
            Matrix4 transform = top().mMatrix;
            transform.preScale(sx, sy, sz);
            topDevice().setGlobalCTM(transform);
            didScale(sx, sy, sz);
        }
    }

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
    public final void scale(float sx, float sy, float px, float py) {
        if (sx != 1.0f || sy != 1.0f) {
            mTmpMatrix.setScale(sx, sy, px, py);
            concat(mTmpMatrix);
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
            mTmpMatrix.setRotate(degrees * MathUtil.DEG_TO_RAD);
            concat(mTmpMatrix);
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
            mTmpMatrix.setRotate(degrees * MathUtil.DEG_TO_RAD, px, py);
            concat(mTmpMatrix);
        }
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
    public final void shear(float sx, float sy) {
        mTmpMatrix.setShear(sx, sy);
        concat(mTmpMatrix);
    }

    /**
     * Pre-multiply the current matrix by the specified shearing.
     *
     * @param sx the x-component of the shearing, y is unchanged
     * @param sy the y-component of the shearing, x is unchanged
     * @param px the x-component of the pivot (unchanged by the shear)
     * @param py the y-component of the pivot (unchanged by the shear)
     */
    public final void shear(float sx, float sy, float px, float py) {
        mTmpMatrix.setShear(sx, sy, px, py);
        concat(mTmpMatrix);
    }

    /**
     * Pre-multiply the current matrix by the specified matrix.
     * <p>
     * This has the effect of transforming the drawn geometry by matrix, before
     * transforming the result with the current matrix.
     *
     * @param matrix the matrix to premultiply with the current matrix
     */
    public final void concat(@NonNull Matrixc matrix) {
        if (matrix.isIdentity()) {
            return;
        }
        matrix.toMatrix4(mTmpMatrix44);
        concat(mTmpMatrix44);
    }

    /**
     * Pre-multiply the current matrix by the specified matrix.
     * <p>
     * This has the effect of transforming the drawn geometry by matrix, before
     * transforming the result with the current matrix.
     *
     * @param matrix the matrix to premultiply with the current matrix
     */
    public final void concat(@NonNull Matrix4c matrix) {
        checkForDeferredSave();
        Matrix4 transform = top().mMatrix;
        transform.preConcat(matrix);
        topDevice().setGlobalCTM(transform);
        didConcat(matrix);
    }

    /**
     * Replaces the current matrix with the specified matrix.
     * Unlike concat(), any prior matrix state is overwritten.
     *
     * @param matrix matrix to copy, replacing the current matrix
     */
    public final void setMatrix(@NonNull Matrix4c matrix) {
        checkForDeferredSave();
        Matrix4 transform = top().mMatrix;
        transform.set(matrix);
        topDevice().setGlobalCTM(transform);
        didSetMatrix(matrix);
    }

    /**
     * Sets the current matrix to the identity matrix.
     * Any prior matrix state is overwritten.
     */
    public final void resetMatrix() {
        mTmpMatrix44.setIdentity();
        setMatrix(mTmpMatrix44);
    }

    //////////////////////////////////////////////////////////////////////////////

    /**
     * Intersect the current clip with the specified rectangle and updates
     * the stencil buffer if changed, which is expressed in local coordinates.
     * Resulting clip is aliased. The clip bounds cannot be expanded unless
     * restore() is called.
     *
     * @param rect the rectangle to intersect with the current clip
     */
    public final void clipRect(Rect2ic rect) {
        mTmpRect.set(rect);
        clipRect(mTmpRect, ClipOp.CLIP_OP_INTERSECT, false);
    }

    /**
     * Intersect the current clip with the specified rectangle and updates
     * the stencil buffer if changed, which is expressed in local coordinates.
     * Resulting clip is aliased. The clip bounds cannot be expanded unless
     * restore() is called.
     *
     * @param rect the rectangle to intersect with the current clip
     */
    public final void clipRect(Rect2fc rect) {
        clipRect(rect, ClipOp.CLIP_OP_INTERSECT, false);
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
        clipRect(mTmpRect, ClipOp.CLIP_OP_INTERSECT, false);
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
    public final void clipRect(Rect2fc rect, boolean doAA) {
        clipRect(rect, ClipOp.CLIP_OP_INTERSECT, doAA);
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
        clipRect(mTmpRect, ClipOp.CLIP_OP_INTERSECT, doAA);
    }

    /**
     * Replaces the current clip with the intersection or difference of the current clip
     * and the given rect, with an aliased or anti-aliased clip edge. The rectangle is
     * transformed by the current matrix before it is combined with clip.
     *
     * @param rect   rectangle to combine with clip
     * @param clipOp ClipOp to apply to clip
     */
    public final void clipRect(Rect2ic rect, int clipOp) {
        mTmpRect.set(rect);
        clipRect(mTmpRect, clipOp, false);
    }

    /**
     * Replaces the current clip with the intersection or difference of the current clip
     * and the given rect, with an aliased or anti-aliased clip edge. The rectangle is
     * transformed by the current matrix before it is combined with clip.
     *
     * @param rect   rectangle to combine with clip
     * @param clipOp ClipOp to apply to clip
     */
    public final void clipRect(Rect2fc rect, int clipOp) {
        clipRect(rect, clipOp, false);
    }

    /**
     * Replaces the current clip with the intersection or difference of the current clip
     * and the given rect, with an aliased or anti-aliased clip edge. The rectangle is
     * transformed by the current matrix before it is combined with clip.
     *
     * @param clipOp ClipOp to apply to clip
     */
    public final void clipRect(float left, float top, float right, float bottom, int clipOp) {
        mTmpRect.set(left, top, right, bottom);
        clipRect(mTmpRect, clipOp, false);
    }

    /**
     * Replaces the current clip with the intersection or difference of the current clip
     * and the given rect, with an aliased or anti-aliased clip edge. The rectangle is
     * transformed by the current matrix before it is combined with clip.
     *
     * @param rect   rectangle to combine with clip
     * @param clipOp ClipOp to apply to clip
     * @param doAA   true if clip mask is requested to be anti-aliased
     */
    public final void clipRect(Rect2fc rect, int clipOp, boolean doAA) {
        if (!rect.isFinite()) {
            return;
        }
        checkForDeferredSave();
        mTmpRect2.set(rect);
        mTmpRect2.sort();
        onClipRect(mTmpRect2, clipOp, doAA);
    }

    //TODO clip round rect, clip path

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
        Device device = topDevice();
        if (device.isClipEmpty()) {
            bounds.setEmpty();
            return false;
        } else {
            Matrix inverse = new Matrix(top().mMatrix);
            // if we can't invert the matrix, we can't return local clip bounds
            if (!inverse.invert()) {
                bounds.setEmpty();
                return false;
            }
            bounds.set(device.getClipBounds());
            device.getDeviceToGlobal().mapRect(bounds);
            bounds.roundOut(bounds);
            // adjust it outwards in case we are antialiasing
            bounds.outset(1.0f, 1.0f);
            inverse.mapRect(bounds);
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
        Device device = topDevice();
        if (device.isClipEmpty()) {
            bounds.setEmpty();
            return false;
        } else {
            device.getClipBounds(bounds);
            device.getDeviceToGlobal().mapRectOut(bounds, bounds);
            return !bounds.isEmpty();
        }
    }

    //////////////////////////////////////////////////////////////////////////////

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
     * Makes Canvas contents undefined. Subsequent calls that read Canvas pixels,
     * such as drawing with BlendMode, return undefined results. discard() does
     * not change clip or Matrix.
     * <p>
     * discard() may do nothing, depending on the implementation of Surface or Device
     * that created Canvas.
     * <p>
     * discard() allows optimized performance on subsequent draws by removing
     * cached data associated with Surface or Device.
     * It is not necessary to call discard() once done with Canvas;
     * any cached data is deleted when owning Surface or Device is deleted.
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
        onDrawPaint(paint);
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
        onDrawPaint(paint);
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
        var cleanedPaint = mTmpPaint;
        cleanedPaint.set(paint);
        cleanedPaint.setStyle(Paint.FILL);
        cleanedPaint.setPathEffect(null);
        onDrawPaint(cleanedPaint);
        cleanedPaint.reset();
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
     * Draws RRect using the current matrix, clip and specified paint.
     * In paint: Style determines if rrect is stroked or filled;
     * if stroked, Paint stroke width describes the line thickness.
     * <p>
     * rrect may represent a rectangle, circle, oval, uniformly rounded rectangle, or
     * may have any combination of positive non-square radii for the four corners.
     *
     * @param rrect SkRRect with up to eight corner radii to draw
     * @param paint SkPaint stroke or fill, blend, color, and so on, used to draw
     */
    public final void drawRRect(RRect rrect, Paint paint) {
        onDrawRRect(rrect, paint);
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
        drawEllipse(cx, cy, radius, radius, paint);
    }

    /**
     * Draw the specified ellipse at (cx, cy) with radii using the specified paint.
     * If either radiusX or radiusY is zero or less, nothing is drawn.
     * In paint: Paint's style determines if ellipse is stroked or filled;
     * if stroked, paint's stroke width describes the line thickness.
     *
     * @param cx      the x-coordinate of the center of the ellipse to be drawn
     * @param cy      the y-coordinate of the center of the ellipse to be drawn
     * @param radiusX the x-axis radius of the ellipse to be drawn
     * @param radiusY the y-axis radius of the ellipse to be drawn
     * @param paint   the paint used to draw the ellipse
     */
    public final void drawEllipse(float cx, float cy, float radiusX, float radiusY, Paint paint) {
        if (radiusX < 0)
            radiusX = 0;
        if (radiusY < 0)
            radiusY = 0;
        onDrawEllipse(cx, cy, radiusX, radiusY, paint);
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

    /**
     * Draw the specified image, scaling/translating automatically to fill the destination
     * rectangle. If the source rectangle is not null, it specifies the subset of the image to
     * draw. The Style and smooth radius is ignored in the paint, images are always filled.
     *
     * @param image the image to be drawn
     * @param dst   the rectangle that the image will be scaled/translated to fit into
     * @param paint the paint used to draw the image, null meaning a default paint
     */
    public final void drawImageRect(@RawPtr Image image, Rect2fc dst, SamplingOptions sampling,
                                    @Nullable Paint paint) {
        if (image == null) {
            return;
        }
        var src = new Rect2f(0, 0, image.getWidth(), image.getHeight());
        drawImageRect(image, src, dst, sampling, paint, SRC_RECT_CONSTRAINT_FAST);
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
                int filter = sampling.mMinFilter | sampling.mMagFilter;
                sampling = filter == SamplingOptions.FILTER_MODE_LINEAR
                        ? SamplingOptions.LINEAR
                        : SamplingOptions.POINT;
            } else if (sampling.isAnisotropy()) {
                sampling = SamplingOptions.LINEAR;
            }
        }
        onDrawImageRect(image, src, dst, sampling, cleanedPaint, constraint);
        cleanedPaint.reset();
    }

    /**
     * Draws count glyphs, at positions relative to origin styled with font and paint.
     * <p>
     * This function draw glyphs at the given positions relative to the given origin.
     * It does not perform typeface fallback for glyphs not found in the Typeface in font.
     * <p>
     * The drawing obeys the current transform matrix and clipping.
     * <p>
     * All elements of paint: PathEffect, Shader, and ColorFilter; apply to text. By
     * default, draws filled white glyphs.
     *
     * @param glyphs     the array of glyphIDs to draw
     * @param positions  where to draw each glyph relative to origin
     * @param glyphCount number of glyphs to draw
     * @param originX    the origin X of all the positions
     * @param originY    the origin Y of all the positions
     * @param font       typeface, text size and so, used to describe the text
     * @param paint      blend, color, and so on, used to draw
     */
    public final void drawGlyphs(int @NonNull[] glyphs, int glyphOffset,
            float @NonNull[] positions, int positionOffset,
                                 int glyphCount,
                                 float originX, float originY,
                                 @NonNull Font font, @NonNull Paint paint) {
        if (glyphCount <= 0) {
            return;
        }

        GlyphRunList glyphRunList = mScratchGlyphRunBuilder.setGlyphRunList(
                glyphs, glyphOffset,
                positions, positionOffset,
                glyphCount, font,
                paint, originX, originY
        );
        onDrawGlyphRunList(glyphRunList, paint);
        mScratchGlyphRunBuilder.clear();
    }

    /**
     * Draws TextBlob blob at (x, y), using the current matrix, clip and specified paint.
     * <p>
     * <var>blob</var> contains glyphs, their positions, and Font.
     * <p>
     * Elements of paint: anti-alias, Blender, color including alpha,
     * ColorFilter, Paint dither, PathEffect, Shader, and
     * Paint style; apply to blob. If Paint contains stroke:
     * Paint miter limit, Cap, Join, and Paint stroke width;
     * apply to Path created from blob.
     *
     * @param blob    glyphs, positions, and their paints' text size, typeface, and so on
     * @param originX horizontal offset applied to blob
     * @param originY vertical offset applied to blob
     * @param paint   blend, color, stroking, and so on, used to draw
     */
    public final void drawTextBlob(TextBlob blob, float originX, float originY,
                                   @NonNull Paint paint) {
        if (blob == null) {
            return;
        }
        blob.getBounds(mTmpRect2);
        mTmpRect2.offset(originX, originY);
        if (!mTmpRect2.isFinite()) {
            return;
        }
        onDrawTextBlob(blob, originX, originY, paint);
    }

    /**
     * Draws the vertices, a triangle mesh, using current clip and matrix.
     * If paint contains a Shader and vertices does not contain texCoords, the shader
     * is mapped using the vertices' positions.
     * <p>
     * Blender is ignored if vertices does not have colors. Otherwise, it combines
     * - the Shader if Paint contains Shader
     * - or the solid Paint color if Paint does not contain Shader
     * as the src of the blend and the interpolated vertex colors as the dst.
     * <p>
     * PathEffect, and antialiasing on Paint are ignored.
     *
     * @param vertices triangle mesh to draw
     * @param blender  combines vertices' colors with Shader if present or Paint opaque color
     *                 if not. Ignored if the vertices do not contain color.
     * @param paint    specifies the Shader, used as Vertices texture, and ColorFilter.
     */
    public final void drawVertices(Vertices vertices,
                                   Blender blender,
                                   Paint paint) {
        if (vertices == null) {
            return;
        }
        var cleanedPaint = mTmpPaint;
        cleanedPaint.set(paint);
        cleanedPaint.setStyle(Paint.FILL);
        cleanedPaint.setPathEffect(null);
        onDrawVertices(vertices, blender, cleanedPaint);
        cleanedPaint.reset();
    }

    @ApiStatus.Experimental
    public static final int
            QUAD_AA_FLAG_LEFT = 0b0001,
            QUAD_AA_FLAG_TOP = 0b0010,
            QUAD_AA_FLAG_RIGHT = 0b0100,
            QUAD_AA_FLAG_BOTTOM = 0b1000,
            QUAD_AA_FLAGS_NONE = 0b0000,
            QUAD_AA_FLAGS_ALL = QUAD_AA_FLAG_LEFT|QUAD_AA_FLAG_TOP|QUAD_AA_FLAG_RIGHT|QUAD_AA_FLAG_BOTTOM;

    @ApiStatus.Experimental
    public final void drawEdgeAAQuad(Rect2fc rect, @Size(8) float[] clip,
                                     @MagicConstant(flags = {QUAD_AA_FLAG_LEFT, QUAD_AA_FLAG_TOP, QUAD_AA_FLAG_RIGHT,
                                             QUAD_AA_FLAG_BOTTOM}) int edgeFlags, Paint paint) {
        var cleanedPaint = mTmpPaint;
        cleanedPaint.set(paint);
        cleanedPaint.setStyle(Paint.FILL);
        cleanedPaint.setPathEffect(null);
        onDrawEdgeAAQuad(rect, clip, edgeFlags, cleanedPaint);
        cleanedPaint.reset();
    }

    @ApiStatus.Internal
    public final void drawBlurredRRect(RRect rr, Paint paint, float blurRadius, float noiseAlpha) {
        var cleanedPaint = mTmpPaint;
        cleanedPaint.set(paint);
        cleanedPaint.setStyle(Paint.FILL);
        cleanedPaint.setPathEffect(null);
        onDrawBlurredRRect(rr, cleanedPaint, blurRadius, noiseAlpha);
        cleanedPaint.reset();
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
        return topDevice().isClipEmpty();
    }

    /**
     * Returns true if clip is a Rect and not empty.
     * Returns false if the clip is empty, or if it is complex.
     *
     * @return true if clip is a Rect and not empty
     */
    public boolean isClipRect() {
        return topDevice().isClipRect();
    }

    /**
     * Returns the current transform from local coordinates to the 'device', which for most
     * purposes means pixels.
     *
     * @param storage transformation from local coordinates to device / pixels.
     */
    public final void getLocalToDevice(@NonNull Matrix4 storage) {
        top().mMatrix.store(storage);
    }

    /**
     * Returns the current transform from local coordinates to the 'device', which for most
     * purposes means pixels. Discards the 3rd row and column in the matrix, so be warned.
     *
     * @param storage transformation from local coordinates to device / pixels.
     */
    public final void getLocalToDevice(@NonNull Matrix storage) {
        storage.set(top().mMatrix);
    }

    /**
     * Draws saved layers, if any.
     * Frees up resources used by Canvas.
     */
    @Override
    public void close() {
        restoreToCount(1); // restore everything but the last
        internalRestore(); // restore the last, since we're going away
        mRootDevice = RefCnt.move(mRootDevice);
        if (mSurface != null) {
            throw new IllegalStateException("Surface-created canvas is owned by Surface, use Surface#unref instead");
        }
    }

    @NonNull
    protected ImageInfo onGetImageInfo() {
        return mRootDevice.getImageInfo();
    }

    @Nullable
    @SharedPtr
    protected Surface onNewSurface(ImageInfo info) {
        return mRootDevice.makeSurface(info);
    }

    // The bottom-most device in the stack, only changed by init(). Image properties and the final
    // canvas pixels are determined by this device.
    @RawPtr
    protected Device getRootDevice() {
        return mRootDevice;
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

    protected void didTranslate(float dx, float dy, float dz) {
    }

    protected void didScale(float sx, float sy, float sz) {
    }

    protected void didConcat(Matrix4c matrix) {
    }

    protected void didSetMatrix(Matrix4c matrix) {
    }

    @NonNull
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
        final int i = mMCIndex--;
        if (i >= MAX_MC_POOL_SIZE) {
            mMCStack[i] = null;
        }
    }

    @NonNull
    private MCRec top() {
        return mMCStack[mMCIndex];
    }

    // the top-most device in the stack, will change within saveLayer()'s. All drawing and clipping
    // operations should route to this device.
    @NonNull
    private Device topDevice() {
        return top().mDevice;
    }

    private void checkForDeferredSave() {
        if (top().mDeferredSaveCount > 0) {
            doSave();
        }
    }

    private void doSave() {
        willSave();
        assert top().mDeferredSaveCount > 0;
        top().mDeferredSaveCount--;
        internalSave();
    }

    private void internalSave() {
        // get before push stack
        MCRec rec = top();
        push().set(rec);

        topDevice().pushClipStack();
    }

    private void internalRestore() {
        assert mMCIndex >= 0;

        // now do the normal restore()
        pop();

        if (mMCIndex == -1) {
            // this was the last record, restored during the destruction of the Canvas
            return;
        }

        var top = top();
        top.mDevice.popClipStack();
        top.mDevice.setGlobalCTM(top.mMatrix);

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

    private void internalDrawPaint(Paint paint) {
        // drawPaint does not call internalQuickReject() because computing its geometry is not free
        // (see getLocalClipBounds()), and the two conditions below are sufficient.
        if (paint.nothingToDraw() || isClipEmpty()) {
            return;
        }

        if (aboutToDraw(paint)) {
            topDevice().drawPaint(paint);
        }
    }

    /**
     * Compute the clip's bounds based on all clipped Device's reported device bounds transformed
     * into the canvas' global space.
     */
    private void computeQuickRejectBounds() {
        Device device = topDevice();
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
                                        @Nullable Matrixc matrix) {
        if (!bounds.isFinite() || paint.nothingToDraw()) {
            return true;
        }

        if (paint.canComputeFastBounds(null)) {
            var tmp = mTmpQuickBounds;
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
        var bounds = mTmpRect2;
        bounds.setBounds(pts, offset, count);
        if (internalQuickReject(bounds, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            topDevice().drawPoints(mode, pts, offset, count, paint);
        }
    }

    protected void onDrawLine(float x0, float y0, float x1, float y1,
                              @Paint.Cap int cap, float width, Paint paint) {
        var bounds = mTmpRect2;
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
            topDevice().drawLine(x0, y0, x1, y1, cap, width, paint);
        }
    }

    protected void onDrawRect(Rect2fc r, Paint paint) {
        if (internalQuickReject(r, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            topDevice().drawRect(r, paint);
        }
    }

    protected void onDrawRRect(RRect rr, Paint paint) {
        var bounds = mTmpRect2;
        rr.getBounds(bounds);
        if (rr.getType() <= RRect.kRect_Type) {
            bounds.sort();
            onDrawRect(bounds, paint);
            return;
        }

        if (internalQuickReject(bounds, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            topDevice().drawRRect(rr, paint);
        }
    }

    protected void onDrawEllipse(float cx, float cy, float rx, float ry, Paint paint) {
        var bounds = mTmpRect2;
        bounds.set(cx - rx, cy - ry, cx + rx, cy + ry);

        if (internalQuickReject(bounds, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            topDevice().drawEllipse(cx, cy, rx, ry, paint);
        }
    }

    protected void onDrawArc(float cx, float cy, float radius, float startAngle,
                             float sweepAngle, @Paint.Cap int cap, float width, Paint paint) {
        var bounds = mTmpRect2;
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius);
        float outset = cap == Paint.CAP_SQUARE ? MathUtil.SQRT2 * width * 0.5f : width * 0.5f;
        bounds.outset(outset, outset);

        if (internalQuickReject(bounds, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            topDevice().drawArc(cx, cy, radius, startAngle, sweepAngle, cap, width, paint);
        }
    }

    protected void onDrawPie(float cx, float cy, float radius, float startAngle,
                             float sweepAngle, Paint paint) {
        var bounds = mTmpRect2;
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius);

        if (internalQuickReject(bounds, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            topDevice().drawPie(cx, cy, radius, startAngle, sweepAngle, paint);
        }
    }

    protected void onDrawChord(float cx, float cy, float radius, float startAngle,
                               float sweepAngle, Paint paint) {
        var bounds = mTmpRect2;
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius);

        if (internalQuickReject(bounds, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            topDevice().drawChord(cx, cy, radius, startAngle, sweepAngle, paint);
        }
    }

    protected void onDrawImageRect(@RawPtr Image image, Rect2fc src, Rect2fc dst,
                                   SamplingOptions sampling, Paint paint,
                                   int constraint) {
        if (internalQuickReject(dst, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            topDevice().drawImageRect(image, src, dst, sampling, paint, constraint);
        }
    }

    protected void onDrawTextBlob(TextBlob blob, float originX, float originY,
                                  Paint paint) {
        GlyphRunList glyphRunList = mScratchGlyphRunBuilder.blobToGlyphRunList(
                blob, originX, originY
        );
        onDrawGlyphRunList(glyphRunList, paint);
        mScratchGlyphRunBuilder.clear();
    }

    protected void onDrawGlyphRunList(GlyphRunList glyphRunList, Paint paint) {
        var bounds = mTmpRect2;
        glyphRunList.getSourceBoundsWithOrigin(bounds);
        if (internalQuickReject(bounds, paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            topDevice().drawGlyphRunList(this, glyphRunList, paint);
        }
    }

    protected void onDrawVertices(Vertices vertices, Blender blender,
                                  Paint paint) {
        if (internalQuickReject(vertices.getBounds(), paint)) {
            return;
        }

        if (aboutToDraw(paint)) {
            topDevice().drawVertices(vertices, blender, paint);
        }
    }

    protected void onDrawEdgeAAQuad(Rect2fc rect, @Size(8) float[] clip, int edgeFlags, Paint paint) {
        if ((clip == null || rect != null) && internalQuickReject(rect, paint)) {
            return;
        }

        if (predrawNotify(false)) {
            topDevice().drawEdgeAAQuad(rect, clip, edgeFlags, paint);
        }
    }

    protected void onDrawBlurredRRect(RRect rr, Paint paint, float blurRadius, float noiseAlpha) {
        if (predrawNotify(false)) {
            topDevice().drawBlurredRRect(rr, paint, blurRadius, noiseAlpha);
        }
    }

    protected void onClipRect(Rect2fc rect, int clipOp, boolean doAA) {
        topDevice().clipRect(rect, clipOp, doAA);
        computeQuickRejectBounds();
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
            mMatrix.setIdentity();
            mDeferredSaveCount = 0;
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
}
