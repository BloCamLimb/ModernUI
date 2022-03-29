/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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
import icyllis.modernui.view.View;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Data structure that holds the properties for a RenderNode.
 */
public class RenderProperties {

    static final int CLIP_TO_BOUNDS = 0x1;
    static final int CLIP_TO_CLIP_BOUNDS = 0x1 << 1;

    /**
     * Stores the total transformation of the RenderNode based upon its
     * translate/rotate/scale properties.
     * <p>
     * In the Z-translation-only case, the matrix isn't necessarily allocated,
     * and the mTranslation properties are used directly.
     */
    @Nullable
    private Matrix4 mMatrix;
    @Nullable
    private Matrix4 mInverseMatrix;

    @Nullable
    private Rect mClipBounds; // lazy
    @Nullable
    private Matrix4 mAnimationMatrix; // copy or null

    private boolean mForceToLayer = false;
    private int mLayerAlpha;
    private BlendMode mLayerMode;

    private int mLeft = 0, mTop = 0, mRight = 0, mBottom = 0;
    private int mWidth = 0, mHeight = 0;
    private int mClippingFlags = CLIP_TO_BOUNDS;
    private float mAlpha = 1;
    private float mTranslationX = 0, mTranslationY = 0, mTranslationZ = 0;
    private float mElevation;
    private float mRotationX = 0, mRotationY = 0, mRotationZ = 0;
    private float mScaleX = 1, mScaleY = 1;
    private float mPivotX = 0, mPivotY = 0;
    private boolean mHasOverlappingRendering = false;
    private boolean mPivotExplicitlySet = false;
    private boolean mMatrixOrPivotDirty = false;

    public RenderProperties() {
        setLayerPaint(null);
    }

    /**
     * Gets the current transform matrix. The return value may be null, indicating that it is identity.
     * Note that the returned matrix is <b>READ-ONLY</b> currently. If you want to record this matrix,
     * you need to make a copy.
     *
     * @return The current transform matrix, may be null
     */
    @Nullable
    public Matrix4 getMatrix() {
        if (mMatrixOrPivotDirty) {
            mMatrixOrPivotDirty = false;
            if (!mPivotExplicitlySet) {
                mPivotX = mWidth / 2.0f;
                mPivotY = mHeight / 2.0f;
            }
            if (mMatrix == null) {
                mMatrix = new Matrix4();
            }
            final Matrix4 matrix = mMatrix;
            // because it's 2D rendering, the Z value is used only for transparency sorting
            // which happens on the application layer, so there's no need to report the Z value to GPU
            matrix.setTranslate(mPivotX + mTranslationX, mPivotY + mTranslationY, 0);
            matrix.rotate((float) Math.toRadians(mRotationX),
                    (float) Math.toRadians(mRotationY),
                    (float) Math.toRadians(mRotationZ));
            matrix.scale(mScaleX, mScaleY);
            matrix.translate(-mPivotX, -mPivotY);
            return matrix;
        }
        return mMatrix;
    }

    /**
     * Gets the current transform inverted. The return value may be null, indicating that it is identity.
     * Note that the returned matrix is <b>READ-ONLY</b> currently. If you want to record this matrix,
     * you need to make a copy.
     *
     * @return The inverse of current transform matrix, may be null
     */
    @Nullable
    public Matrix4 getInverseMatrix() {
        Matrix4 matrix = getMatrix();
        if (matrix == null) {
            return null;
        }
        if (mInverseMatrix == null) {
            mInverseMatrix = new Matrix4();
        }
        if (matrix.invert(mInverseMatrix)) {
            return mInverseMatrix;
        }
        // we assume it identity if it's not invertible
        return null;
    }

    /**
     * Controls whether to force this RenderNode to render to an intermediate render target.
     * Internally RenderNode will already promote itself to a composition layer if it's useful
     * for performance or required for the current combination of {@link #setAlpha(float)} and
     * {@link #setHasOverlappingRendering(boolean)}.
     *
     * <p>The usage of this is instead to allow for either overriding of the internal behavior
     * if it's measured to be necessary for the particular rendering content in question or, more
     * usefully, to add a composition effect to the RenderNode via the optional paint parameter.
     *
     * <p>Note: When a RenderNode is using a compositing layer it will also result in
     * clipToBounds=true behavior.
     *
     * @param forceToLayer if true this forces the RenderNode to use an intermediate buffer.
     *                     Default & generally recommended value is false.
     * @param paint        The blend mode, alpha, and ColorFilter to apply to the compositing layer.
     *                     Only applies if forceToLayer is true. The paint's alpha is multiplied
     *                     with {@link #getAlpha()} to resolve the final alpha of the RenderNode.
     *                     If null then no additional composition effects are applied on top of the
     *                     composition layer.
     * @return True if the value changed, false if the new value was the same as the previous value.
     */
    public boolean setUseCompositingLayer(boolean forceToLayer, @Nullable Paint paint) {
        boolean changed = mForceToLayer != forceToLayer;
        if (changed) {
            mForceToLayer = forceToLayer;
        }
        // ensure it's null
        changed |= setLayerPaint(forceToLayer ? paint : null);
        return changed;
    }

    /**
     * Gets whether a compositing layer is forced to be used. The default & recommended
     * is false, as it is typically faster to avoid using compositing layers.
     * See {@link #setUseCompositingLayer(boolean, Paint)}.
     *
     * @return true if a compositing layer is forced, false otherwise
     */
    public boolean getUseCompositingLayer() {
        return mForceToLayer;
    }

    /**
     * @see #setUseCompositingLayer(boolean, Paint)
     */
    private boolean setLayerPaint(@Nullable Paint paint) {
        boolean changed = false;
        int alpha = Paint.getAlphaDirect(paint);
        if (mLayerAlpha != alpha) {
            mLayerAlpha = alpha;
            changed = true;
        }
        BlendMode mode = Paint.getBlendModeDirect(paint);
        if (mLayerMode != mode) {
            mLayerMode = mode;
            changed = true;
        }
        return changed;
    }

    /**
     * Gets whether a compositing layer should be used. This pushes the RenderNode into a
     * cached color buffer stack and avoids creating a new layer.
     *
     * @return true if a compositing layer should be used, false otherwise
     */
    public boolean getUseTransientLayer() {
        return mAlpha < 1 && mAlpha > 0 && mHasOverlappingRendering;
    }

    /**
     * Returns the alpha value if a compositing layer is used. This alpha is multiplied
     * with {@link #getAlpha()} to resolve the final alpha of the RenderNode.
     *
     * @return the layer's alpha
     */
    public int getLayerAlpha() {
        return mLayerAlpha;
    }

    /**
     * Returns the blend mode if a compositing layer is used.
     *
     * @return the layer's blend mode
     */
    @Nonnull
    public BlendMode getLayerBlendMode() {
        return mLayerMode;
    }

    /**
     * Sets an additional clip on the RenderNode. If null, the extra clip is removed from the
     * RenderNode. If non-null, the RenderNode will be clipped to this rect. In addition, if
     * {@link #setClipToBounds(boolean)} is true, then the RenderNode will be clipped to the
     * intersection of this rectangle and the bounds of the RenderNode, which is set with
     * {@link #setPosition(Rect)}.
     *
     * @param clipBounds the bounds to clip to. If null, the additional clip is removed.
     * @return True if the value changed, false if the new value was the same as the previous value.
     */
    public boolean setClipBounds(@Nullable Rect clipBounds) {
        if (clipBounds == null) {
            if ((mClippingFlags & CLIP_TO_CLIP_BOUNDS) != 0) {
                mClippingFlags &= ~CLIP_TO_CLIP_BOUNDS;
                return true;
            }
            return false;
        } else {
            final boolean ret = (mClippingFlags & CLIP_TO_CLIP_BOUNDS) == 0;
            if (ret) {
                mClippingFlags |= CLIP_TO_CLIP_BOUNDS;
            }
            if (mClipBounds == null) {
                mClipBounds = new Rect(clipBounds);
            } else if (mClipBounds.equals(clipBounds)) {
                return ret;
            } else {
                mClipBounds.set(clipBounds);
            }
            return true;
        }
    }

    /**
     * Set whether the RenderNode should clip itself to its bounds. This defaults to true,
     * and is useful to the RenderNode in enable quick-rejection of chunks of the tree as well as
     * better partial invalidation support. Clipping can be further restricted or controlled
     * through the combination of this property as well as {@link #setClipBounds(Rect)}, which
     * allows for a different clipping rectangle to be used in addition to or instead of the
     * {@link #setPosition(int, int, int, int)} or the RenderNode.
     *
     * @param clipToBounds true if the RenderNode should clip to its bounds, false otherwise.
     * @return True if the value changed, false if the new value was the same as the previous value.
     */
    public boolean setClipToBounds(boolean clipToBounds) {
        if (clipToBounds) {
            if ((mClippingFlags & CLIP_TO_BOUNDS) == 0) {
                mClippingFlags |= CLIP_TO_BOUNDS;
                return true;
            }
        } else {
            if ((mClippingFlags & CLIP_TO_BOUNDS) != 0) {
                mClippingFlags &= ~CLIP_TO_BOUNDS;
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the RenderNode is clipping to its bounds. See
     * {@link #setClipToBounds(boolean)} and {@link #setPosition(int, int, int, int)}
     *
     * @return true if the render node clips to its bounds, false otherwise.
     */
    public boolean getClipToBounds() {
        return (mClippingFlags & CLIP_TO_BOUNDS) != 0;
    }

    /**
     * Set the animation matrix on the RenderNode. This matrix exists if an animation is
     * currently playing on a View, and is set on the RenderNode during at draw time. When
     * the animation finishes, the matrix should be cleared by sending <code>null</code>
     * for the matrix parameter.
     *
     * @param matrix The matrix, null indicates that the matrix should be cleared.
     * @see #getAnimationMatrix()
     */
    public boolean setAnimationMatrix(@Nullable Matrix4 matrix) {
        if (matrix == null) {
            if (mMatrix != null) {
                mMatrix = null;
                return true;
            }
            return false;
        }
        if (mAnimationMatrix == null) {
            mAnimationMatrix = new Matrix4(matrix);
        } else if (!mAnimationMatrix.approxEqual(matrix)) {
            mAnimationMatrix.set(matrix);
        } else {
            return false;
        }
        return true;
    }

    /**
     * Returns the previously set animation matrix. This matrix exists if an animation is
     * currently playing on a View, and is set on the RenderNode during at draw time.
     * Returns <code>null</code> when there is no transformation provided by
     * {@link #setAnimationMatrix(Matrix4)}.
     *
     * @return the current Animation matrix.
     * @see #setAnimationMatrix(Matrix4)
     */
    @Nullable
    public Matrix4 getAnimationMatrix() {
        return mAnimationMatrix;
    }

    /**
     * Sets the translucency level for the RenderNode.
     *
     * @param alpha The translucency of the RenderNode, must be a value between 0.0f and 1.0f
     * @return True if the value changed, false if the new value was the same as the previous value.
     * @see #getAlpha()
     */
    public boolean setAlpha(float alpha) {
        // clamp the alpha value
        if (alpha <= 0.001f) {
            alpha = 0;
        } else if (alpha >= 0.999f) {
            alpha = 1;
        }
        if (mAlpha != alpha) {
            mAlpha = alpha;
            return true;
        }
        return false;
    }

    /**
     * Returns the translucency level of this RenderNode.
     *
     * @return A value between 0.0f and 1.0f
     * @see #setAlpha(float)
     */
    public float getAlpha() {
        return mAlpha;
    }

    /**
     * Sets whether the RenderNode renders content which overlaps. Non-overlapping rendering
     * can use a fast path for alpha that avoids rendering to an offscreen buffer. By default,
     * RenderNodes consider they do not have overlapping content.
     *
     * @param hasOverlappingRendering False if the content is guaranteed to be non-overlapping,
     *                                true otherwise.
     * @see #getHasOverlappingRendering()
     */
    public boolean setHasOverlappingRendering(boolean hasOverlappingRendering) {
        if (mHasOverlappingRendering != hasOverlappingRendering) {
            mHasOverlappingRendering = hasOverlappingRendering;
            return true;
        }
        return false;
    }

    /**
     * Indicates whether the content of this RenderNode overlaps.
     *
     * @return True if this RenderNode renders content which overlaps, false otherwise.
     * @see #setHasOverlappingRendering(boolean)
     */
    public boolean getHasOverlappingRendering() {
        return mHasOverlappingRendering;
    }

    /**
     * Sets the base elevation of this RenderNode in pixels
     *
     * @param lift the elevation in pixels
     * @return True if the value changed, false if the new value was the same as the previous value.
     */
    public boolean setElevation(float lift) {
        if (mElevation != lift) {
            mElevation = lift;
            // mMatrixOrPivotDirty not set, since matrix doesn't respect Z
            return true;
        }
        return false;
    }

    /**
     * See {@link #setElevation(float)}
     *
     * @return The RenderNode's current elevation
     */
    public float getElevation() {
        return mElevation;
    }

    /**
     * Sets the translation value for the RenderNode on the X axis.
     *
     * @param translationX The X axis translation value of the RenderNode, in pixels
     * @return True if the value changed, false if the new value was the same as the previous value.
     * @see View#setTranslationX(float)
     * @see #getTranslationX()
     */
    public boolean setTranslationX(float translationX) {
        if (mTranslationX != translationX) {
            mTranslationX = translationX;
            mMatrixOrPivotDirty = true;
            return true;
        }
        return false;
    }

    /**
     * Returns the translation value for this RenderNode on the X axis, in pixels.
     *
     * @see #setTranslationX(float)
     */
    public float getTranslationX() {
        return mTranslationX;
    }

    /**
     * Sets the translation value for the RenderNode on the Y axis.
     *
     * @param translationY The Y axis translation value of the RenderNode, in pixels
     * @return True if the value changed, false if the new value was the same as the previous value.
     * @see View#setTranslationY(float)
     * @see #getTranslationY()
     */
    public boolean setTranslationY(float translationY) {
        if (mTranslationY != translationY) {
            mTranslationY = translationY;
            mMatrixOrPivotDirty = true;
            return true;
        }
        return false;
    }

    /**
     * Returns the translation value for this RenderNode on the Y axis, in pixels.
     *
     * @see #setTranslationY(float)
     */
    public float getTranslationY() {
        return mTranslationY;
    }

    /**
     * Sets the translation value for the RenderNode on the Z axis.
     *
     * @return True if the value changed, false if the new value was the same as the previous value.
     * @see View#setTranslationZ(float)
     * @see #getTranslationZ()
     */
    public boolean setTranslationZ(float translationZ) {
        if (mTranslationZ != translationZ) {
            mTranslationZ = translationZ;
            // mMatrixOrPivotDirty not set, since matrix doesn't respect Z
            return true;
        }
        return false;
    }

    /**
     * Returns the translation value for this RenderNode on the Z axis.
     *
     * @see #setTranslationZ(float)
     */
    public float getTranslationZ() {
        return mTranslationZ;
    }

    /**
     * Sets the visual x position of this view, in pixels. This is equivalent to setting the
     * {@link #setTranslationX(float) translationX} property to be the difference between
     * the x value passed in and the current {@link #getLeft() left} property.
     *
     * @param x The visual x position of this view, in pixels.
     */
    public boolean setX(float x) {
        return setTranslationX(x - mLeft);
    }

    /**
     * The visual x position of this view, in pixels. This is equivalent to the
     * {@link #setTranslationX(float) translationX} property plus the current
     * {@link #getLeft() left} property.
     *
     * @return The visual x position of this view, in pixels.
     */
    public float getX() {
        return mLeft + mTranslationX;
    }

    /**
     * Sets the visual y position of this view, in pixels. This is equivalent to setting the
     * {@link #setTranslationY(float) translationY} property to be the difference between
     * the y value passed in and the current {@link #getTop() top} property.
     *
     * @param y The visual y position of this view, in pixels.
     */
    public boolean setY(float y) {
        return setTranslationY(y - mTop);
    }

    /**
     * The visual y position of this view, in pixels. This is equivalent to the
     * {@link #setTranslationY(float) translationY} property plus the current
     * {@link #getTop() top} property.
     *
     * @return The visual y position of this view, in pixels.
     */
    public float getY() {
        return mTop + mTranslationY;
    }

    /**
     * Sets the visual z position of this view, in pixels. This is equivalent to setting the
     * {@link #setTranslationZ(float) translationZ} property to be the difference between
     * the z value passed in and the current {@link #getElevation() elevation} property.
     *
     * @param z The visual z position of this view, in pixels.
     */
    public boolean setZ(float z) {
        return setTranslationZ(z - mElevation);
    }

    /**
     * The visual z position of this view, in pixels. This is equivalent to the
     * {@link #setTranslationZ(float) translationZ} property plus the current
     * {@link #getElevation() elevation} property.
     *
     * @return The visual z position of this view, in pixels.
     */
    public float getZ() {
        return mElevation + mTranslationZ;
    }

    /**
     * Sets the rotation value for the RenderNode around the X axis.
     *
     * @param rotationX The rotation value of the RenderNode, in degrees
     * @return True if the value changed, false if the new value was the same as the previous value.
     * @see View#setRotationX(float)
     * @see #getRotationX()
     */
    public boolean setRotationX(float rotationX) {
        if (mRotationX != rotationX) {
            mRotationX = rotationX;
            mMatrixOrPivotDirty = true;
            return true;
        }
        return false;
    }

    /**
     * Returns the rotation value for this RenderNode around the X axis, in degrees.
     *
     * @see #setRotationX(float)
     */
    public float getRotationX() {
        return mRotationX;
    }

    /**
     * Sets the rotation value for the RenderNode around the Y axis.
     *
     * @param rotationY The rotation value of the RenderNode, in degrees
     * @return True if the value changed, false if the new value was the same as the previous value.
     * @see View#setRotationY(float)
     * @see #getRotationY()
     */
    public boolean setRotationY(float rotationY) {
        if (mRotationY != rotationY) {
            mRotationY = rotationY;
            mMatrixOrPivotDirty = true;
            return true;
        }
        return false;
    }

    /**
     * Returns the rotation value for this RenderNode around the Y axis, in degrees.
     *
     * @see #setRotationY(float)
     */
    public float getRotationY() {
        return mRotationY;
    }

    /**
     * Sets the rotation value for the RenderNode around the Z axis.
     *
     * @param rotationZ The rotation value of the RenderNode, in degrees
     * @return True if the value changed, false if the new value was the same as the previous value.
     * @see #getRotationZ()
     */
    public boolean setRotationZ(float rotationZ) {
        if (mRotationZ != rotationZ) {
            mRotationZ = rotationZ;
            mMatrixOrPivotDirty = true;
            return true;
        }
        return false;
    }

    /**
     * Returns the rotation value for this RenderNode around the Z axis, in degrees.
     *
     * @see #setRotationZ(float)
     */
    public float getRotationZ() {
        return mRotationZ;
    }

    /**
     * Sets the scale value for the RenderNode on the X axis.
     *
     * @param scaleX The scale value of the RenderNode
     * @return True if the value changed, false if the new value was the same as the previous value.
     * @see View#setScaleX(float)
     * @see #getScaleX()
     */
    public boolean setScaleX(float scaleX) {
        if (mScaleX != scaleX) {
            mScaleX = scaleX;
            mMatrixOrPivotDirty = true;
            return true;
        }
        return false;
    }

    /**
     * Returns the scale value for this RenderNode on the X axis.
     *
     * @see #setScaleX(float)
     */
    public float getScaleX() {
        return mScaleX;
    }

    /**
     * Sets the scale value for the RenderNode on the Y axis.
     *
     * @param scaleY The scale value of the RenderNode
     * @return True if the value changed, false if the new value was the same as the previous value.
     * @see View#setScaleY(float)
     * @see #getScaleY()
     */
    public boolean setScaleY(float scaleY) {
        if (mScaleY != scaleY) {
            mScaleY = scaleY;
            mMatrixOrPivotDirty = true;
            return true;
        }
        return false;
    }

    /**
     * Returns the scale value for this RenderNode on the Y axis.
     *
     * @see #setScaleY(float)
     */
    public float getScaleY() {
        return mScaleY;
    }

    /**
     * Sets the pivot value for the RenderNode on the X axis
     *
     * @param pivotX The pivot value of the RenderNode on the X axis, in pixels
     * @return True if the value changed, false if the new value was the same as the previous value.
     * @see View#setPivotX(float)
     * @see #getPivotX()
     */
    public boolean setPivotX(float pivotX) {
        final boolean dirty = mPivotX != pivotX;
        if (dirty) {
            mPivotX = pivotX;
        }
        if (dirty || !mPivotExplicitlySet) {
            mMatrixOrPivotDirty = true;
            mPivotExplicitlySet = true;
            return true;
        }
        return false;
    }

    /**
     * Returns the pivot value for this RenderNode on the X axis, in pixels.
     *
     * @see #setPivotX(float)
     */
    public float getPivotX() {
        return mPivotX;
    }

    /**
     * Sets the pivot value for the RenderNode on the Y axis
     *
     * @param pivotY The pivot value of the RenderNode on the Y axis, in pixels
     * @return True if the value changed, false if the new value was the same as the previous value.
     * @see View#setPivotY(float)
     * @see #getPivotY()
     */
    public boolean setPivotY(float pivotY) {
        final boolean dirty = mPivotY != pivotY;
        if (dirty) {
            mPivotY = pivotY;
        }
        if (dirty || !mPivotExplicitlySet) {
            mMatrixOrPivotDirty = true;
            mPivotExplicitlySet = true;
            return true;
        }
        return false;
    }

    /**
     * Returns the pivot value for this RenderNode on the Y axis, in pixels.
     *
     * @see #setPivotY(float)
     */
    public float getPivotY() {
        return mPivotY;
    }

    /**
     * @return Whether a pivot was explicitly set with {@link #setPivotX(float)} or
     * {@link #setPivotY(float)}. If no pivot has been set then the pivot will be the center
     * of the RenderNode.
     */
    public boolean isPivotExplicitlySet() {
        return mPivotExplicitlySet;
    }

    /**
     * Clears any pivot previously set by a call to  {@link #setPivotX(float)} or
     * {@link #setPivotY(float)}. After calling this {@link #isPivotExplicitlySet()} will be false
     * and the pivot used for rotation will return to default of being centered on the view.
     *
     * @return True if the value changed, false if the new value was the same as the previous value.
     */
    public boolean resetPivot() {
        if (mPivotExplicitlySet) {
            mPivotExplicitlySet = false;
            mMatrixOrPivotDirty = true;
            return true;
        }
        return false;
    }

    /**
     * Sets the left position for the RenderNode.
     *
     * @param left The left position, in pixels, of the RenderNode
     * @return true if the value changed, false otherwise
     */
    public boolean setLeft(int left) {
        if (mLeft != left) {
            mLeft = left;
            mWidth = mRight - left;
            if (!mPivotExplicitlySet) {
                mMatrixOrPivotDirty = true;
            }
            return true;
        }
        return false;
    }

    /**
     * Gets the left position for the RenderNode.
     *
     * @return the left position in pixels
     */
    public int getLeft() {
        return mLeft;
    }

    /**
     * Sets the top position for the RenderNode.
     *
     * @param top The top position, in pixels, of the RenderNode
     * @return true if the value changed, false otherwise.
     */
    public boolean setTop(int top) {
        if (mTop != top) {
            mTop = top;
            mHeight = mBottom - top;
            if (!mPivotExplicitlySet) {
                mMatrixOrPivotDirty = true;
            }
            return true;
        }
        return false;
    }

    /**
     * Gets the top position for the RenderNode.
     *
     * @return the top position in pixels
     */
    public int getTop() {
        return mTop;
    }

    /**
     * Sets the right position for the RenderNode.
     *
     * @param right The right position, in pixels, of the RenderNode
     * @return true if the value changed, false otherwise.
     */
    public boolean setRight(int right) {
        if (mRight != right) {
            mRight = right;
            mWidth = right - mLeft;
            if (!mPivotExplicitlySet) {
                mMatrixOrPivotDirty = true;
            }
            return true;
        }
        return false;
    }

    /**
     * Gets the right position for the RenderNode.
     *
     * @return the right position in pixels
     */
    public int getRight() {
        return mRight;
    }

    /**
     * Sets the bottom position for the RenderNode.
     *
     * @param bottom The bottom position, in pixels, of the RenderNode
     * @return true if the value changed, false otherwise.
     */
    public boolean setBottom(int bottom) {
        if (mBottom != bottom) {
            mBottom = bottom;
            mHeight = bottom - mTop;
            if (!mPivotExplicitlySet) {
                mMatrixOrPivotDirty = true;
            }
            return true;
        }
        return false;
    }

    /**
     * Gets the bottom position for the RenderNode.
     *
     * @return the bottom position in pixels
     */
    public int getBottom() {
        return mBottom;
    }

    /**
     * Sets the position of the RenderNode.
     *
     * @param left   The left position of the RenderNode, in pixels
     * @param top    The top position of the RenderNode, in pixels
     * @param right  The right position of the RenderNode, in pixels
     * @param bottom The bottom position of the RenderNode, in pixels
     * @return True if the value changed, false if the new value was the same as the previous value.
     */
    public boolean setPosition(int left, int top, int right, int bottom) {
        if (left != mLeft || top != mTop || right != mRight || bottom != mBottom) {
            mLeft = left;
            mTop = top;
            mRight = right;
            mBottom = bottom;
            mWidth = right - left;
            mHeight = bottom - top;
            if (!mPivotExplicitlySet) {
                mMatrixOrPivotDirty = true;
            }
            return true;
        }
        return false;
    }

    /**
     * Sets the position of the RenderNode.
     *
     * @param position The position rectangle in pixels
     * @return True if the value changed, false if the new value was the same as the previous value.
     */
    public boolean setPosition(@Nonnull Rect position) {
        return setPosition(position.left, position.top, position.right, position.bottom);
    }

    /**
     * Offsets the left and right positions for the RenderNode
     *
     * @param offset The amount that the left and right positions are offset in pixels
     * @return True if the value changed, false if the new value was the same as the previous value.
     */
    public boolean offsetLeftAndRight(int offset) {
        if (offset != 0) {
            mLeft += offset;
            mRight += offset;
            return true;
        }
        return false;
    }

    /**
     * Offsets the top and bottom values for the RenderNode
     *
     * @param offset The amount that the left and right positions are offset in pixels
     * @return True if the value changed, false if the new value was the same as the previous value.
     */
    public boolean offsetTopAndBottom(int offset) {
        if (offset != 0) {
            mTop += offset;
            mBottom += offset;
            return true;
        }
        return false;
    }

    /**
     * Gets the width of the RenderNode, which is the right - left.
     *
     * @return the width of the RenderNode
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Gets the height of the RenderNode, which is the bottom - top.
     *
     * @return the height of the RenderNode
     */
    public int getHeight() {
        return mHeight;
    }
}
