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

//TODO wip
public final class RenderNode {

    private static final int
            CLIP_TO_BOUNDS = 0x1,
            CLIP_TO_CLIP_BOUNDS = 0x1 << 1;

    private Canvas mCurrentRecordingCanvas;

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
    private Matrix4 mAnimationMatrix;

    // Local rendering properties (on UI thread)
    private int mLeft;
    private int mTop;
    private int mRight;
    private int mBottom;
    private int mWidth;
    private int mHeight;
    private int mClippingFlags = CLIP_TO_BOUNDS;
    private float mAlpha = 1;
    private float mTranslationX;
    private float mTranslationY;
    private float mTranslationZ;
    private float mElevation;
    private float mRotation;
    private float mRotationX;
    private float mRotationY;
    private float mScaleX = 1;
    private float mScaleY = 1;
    private float mPivotX;
    private float mPivotY;
    private boolean mHasOverlappingRendering;
    private boolean mPivotExplicitlySet;
    private boolean mMatrixOrPivotDirty;
    @Nullable
    private Rect mClipBounds;

    /**
     * Creates a new RenderNode that can be used to record batches of
     * drawing operations, and store / apply render properties when drawn.
     */
    public RenderNode() {
    }

    /**
     * Starts recording a display list for the render node. All
     * operations performed on the returned canvas are recorded and
     * stored in this display list.
     * <p>
     * {@link #endRecording()} must be called when the recording is finished in order to apply
     * the updated display list. Failing to call {@link #endRecording()} will result in an
     * {@link IllegalStateException} if this method is called again.
     *
     * @param width  The width of the recording viewport. This will not alter the width of the
     *               RenderNode itself, that must be set with {@link #setPosition(Rect)}.
     * @param height The height of the recording viewport. This will not alter the height of the
     *               RenderNode itself, that must be set with {@link #setPosition(Rect)}.
     * @return A canvas to record drawing operations.
     * @throws IllegalStateException If a recording is already in progress. That is, the previous
     *                               call to this method did not call {@link #endRecording()} first.
     * @see #endRecording()
     * @see #hasDisplayList()
     */
    @Nonnull
    public Canvas beginRecording(int width, int height) {
        if (mCurrentRecordingCanvas != null) {
            throw new IllegalStateException("Recording currently in progress - missing #endRecording() call?");
        }
        //mCurrentRecordingCanvas = RecordingCanvas.obtain(this, width, height);
        return mCurrentRecordingCanvas;
    }

    /**
     * Ends the recording for this display list. Calling this method marks
     * the display list valid and {@link #hasDisplayList()} will return true.
     *
     * @see #beginRecording(int, int)
     * @see #hasDisplayList()
     */
    public void endRecording() {
        if (mCurrentRecordingCanvas == null) {
            throw new IllegalStateException("No recording in progress, forgot to call #beginRecording()?");
        }
        Canvas canvas = mCurrentRecordingCanvas;
        mCurrentRecordingCanvas = null;
        //canvas.finishRecording(this);
        //canvas.recycle();
    }

    /**
     * Gets the current transform matrix. The return value may be null, indicating that it is identity.
     * Note that the returned matrix is <b>READ-ONLY</b> at this moment. If you need to store this matrix,
     * you need to make a copy.
     *
     * @return The current transform matrix, may be null
     */
    @Nullable
    public Matrix4 getMatrix() {
        if (mMatrixOrPivotDirty) {
            mMatrixOrPivotDirty = false;
            if (mMatrix == null) {
                mMatrix = Matrix4.identity();
            } else {
                mMatrix.setIdentity();
            }
            if (!mPivotExplicitlySet) {
                mPivotX = mWidth / 2.0f;
                mPivotY = mHeight / 2.0f;
            }
            final Matrix4 matrix = mMatrix;
            // the order is important, also no Z translation, we don't need depth test
            // depth and translucency sorting occurs at the application layer
            matrix.translate(mPivotX + mTranslationX, mPivotY + mTranslationY);
            matrix.rotateX((float) Math.toRadians(mRotationX));
            matrix.rotateY((float) Math.toRadians(mRotationY));
            matrix.rotateZ((float) Math.toRadians(mRotation));
            matrix.scale(mScaleX, mScaleY);
            matrix.translate(-mPivotX, -mPivotY);
            return matrix;
        }
        return mMatrix;
    }

    /**
     * Gets the current transform inverted. The return value may be null, indicating that it is identity.
     * Note that the returned matrix is <b>READ-ONLY</b> at this moment. If you need to store this matrix,
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
        // should not happen, we assume it identity if it's not invertible
        return null;
    }

    /**
     * Sets an additional clip on the RenderNode. If null, the extra clip is removed from the
     * RenderNode. If non-null, the RenderNode will be clipped to this rect. In addition,  if
     * {@link #setClipToBounds(boolean)} is true, then the RenderNode will be clipped to the
     * intersection of this rectangle and the bounds of the render node, which is set with
     * {@link #setPosition(Rect)}.
     *
     * <p>This is equivalent to do a {@link Canvas#clipRect(Rect)} at the start of this
     * RenderNode's renderer. However, as this is a property of the RenderNode instead
     * of part of the renderer it can be more easily animated for transient additional
     * clipping. An example usage of this would be the {@link ChangeBounds}
     * transition animation with the resizeClip=true option.
     *
     * @param rect the bounds to clip to. If null, the additional clip is removed.
     * @return True if the value changed, false if the new value was the same as the previous value.
     */
    public boolean setClipRect(@Nullable Rect rect) {
        if (rect == null) {
            if ((mClippingFlags & CLIP_TO_CLIP_BOUNDS) != 0) {
                mClippingFlags &= ~CLIP_TO_CLIP_BOUNDS;
                return true;
            }
            return false;
        } else {
            final boolean dirty;
            if ((mClippingFlags & CLIP_TO_CLIP_BOUNDS) == 0) {
                mClippingFlags |= CLIP_TO_CLIP_BOUNDS;
                dirty = true;
            } else {
                dirty = false;
            }
            if (mClipBounds == null) {
                mClipBounds = rect.copy();
            } else if (mClipBounds.equals(rect)) {
                return dirty;
            } else {
                mClipBounds.set(rect);
            }
            return true;
        }
    }

    /**
     * Set whether the Render node should clip itself to its bounds. This defaults to true,
     * and is useful to the renderer in enable quick-rejection of chunks of the tree as well as
     * better partial invalidation support. Clipping can be further restricted or controlled
     * through the combination of this property as well as {@link #setClipRect(Rect)}, which
     * allows for a different clipping rectangle to be used in addition to or instead of the
     * {@link #setPosition(int, int, int, int)} or the RenderNode.
     *
     * @param clipToBounds true if the renderer should clip to its bounds, false otherwise.
     * @return True if the value changed, false if the new value was the same as the previous value.
     */
    public boolean setClipToBounds(boolean clipToBounds) {
        if ((mClippingFlags & CLIP_TO_BOUNDS) == 0 != clipToBounds) {
            mClippingFlags ^= CLIP_TO_BOUNDS;
            return true;
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
     * Set the Animation matrix on the renderer. This matrix exists if an Animation is
     * currently playing on a View, and is set on the renderer during at draw() time. When
     * the Animation finishes, the matrix should be cleared by sending <code>null</code>
     * for the matrix parameter.
     *
     * @param matrix The matrix, null indicates that the matrix should be cleared.
     * @see #getAnimationMatrix()
     */
    public boolean setAnimationMatrix(@Nullable Matrix4 matrix) {
        if (mAnimationMatrix != matrix && (matrix == null || !matrix.equivalent(mAnimationMatrix))) {
            mAnimationMatrix = matrix;
            return true;
        }
        return false;
    }

    /**
     * Returns the previously set Animation matrix. This matrix exists if an Animation is
     * currently playing on a View, and is set on the renderer during at draw() time.
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
     * Sets the opacity level for the renderer.
     *
     * @param alpha The opacity of the renderer, must be a value between 0.0f and 1.0f
     * @return True if the value changed, false if the new value was the same as the previous value.
     * @see View#setAlpha(float)
     * @see #getAlpha()
     */
    public boolean setAlpha(float alpha) {
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
     * Returns the opacity level of this renderer.
     *
     * @return A value between 0.0f and 1.0f
     * @see #setAlpha(float)
     */
    public float getAlpha() {
        return mAlpha;
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
     * Sets the translation value for the renderer on the X axis.
     *
     * @param translationX The X axis translation value of the renderer, in pixels
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
     * Returns the translation value for this renderer on the X axis, in pixels.
     *
     * @see #setTranslationX(float)
     */
    public float getTranslationX() {
        return mTranslationX;
    }

    /**
     * Sets the translation value for the renderer on the Y axis.
     *
     * @param translationY The Y axis translation value of the renderer, in pixels
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
     * Returns the translation value for this renderer on the Y axis, in pixels.
     *
     * @see #setTranslationY(float)
     */
    public float getTranslationY() {
        return mTranslationY;
    }

    /**
     * Sets the translation value for the renderer on the Z axis.
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
     * Returns the translation value for this renderer on the Z axis.
     *
     * @see #setTranslationZ(float)
     */
    public float getTranslationZ() {
        return mTranslationZ;
    }

    /**
     * Sets the rotation value for the renderer around the Z axis.
     *
     * @param rotation The rotation value of the renderer, in degrees
     * @return True if the value changed, false if the new value was the same as the previous value.
     * @see View#setRotation(float)
     * @see #getRotationZ()
     */
    public boolean setRotationZ(float rotation) {
        if (mRotation != rotation) {
            mRotation = rotation;
            mMatrixOrPivotDirty = true;
            return true;
        }
        return false;
    }

    /**
     * Returns the rotation value for this renderer around the Z axis, in degrees.
     *
     * @see #setRotationZ(float)
     */
    public float getRotationZ() {
        return mRotation;
    }

    /**
     * Sets the rotation value for the renderer around the X axis.
     *
     * @param rotationX The rotation value of the renderer, in degrees
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
     * Returns the rotation value for this renderer around the X axis, in degrees.
     *
     * @see #setRotationX(float)
     */
    public float getRotationX() {
        return mRotationX;
    }

    /**
     * Sets the rotation value for the renderer around the Y axis.
     *
     * @param rotationY The rotation value of the renderer, in degrees
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
     * Returns the rotation value for this renderer around the Y axis, in degrees.
     *
     * @see #setRotationY(float)
     */
    public float getRotationY() {
        return mRotationY;
    }

    /**
     * Sets the scale value for the renderer on the X axis.
     *
     * @param scaleX The scale value of the renderer
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
     * Returns the scale value for this renderer on the X axis.
     *
     * @see #setScaleX(float)
     */
    public float getScaleX() {
        return mScaleX;
    }

    /**
     * Sets the scale value for the renderer on the Y axis.
     *
     * @param scaleY The scale value of the renderer
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
     * Returns the scale value for this renderer on the Y axis.
     *
     * @see #setScaleY(float)
     */
    public float getScaleY() {
        return mScaleY;
    }

    /**
     * Sets the pivot value for the renderer on the X axis
     *
     * @param pivotX The pivot value of the renderer on the X axis, in pixels
     * @return True if the value changed, false if the new value was the same as the previous value.
     * @see View#setPivotX(float)
     * @see #getPivotX()
     */
    public boolean setPivotX(float pivotX) {
        final boolean dirty;
        if (mPivotX != pivotX) {
            mPivotX = pivotX;
            dirty = true;
        } else {
            dirty = false;
        }
        if (dirty || !mPivotExplicitlySet) {
            mMatrixOrPivotDirty = true;
            mPivotExplicitlySet = true;
            return true;
        }
        return false;
    }

    /**
     * Returns the pivot value for this renderer on the X axis, in pixels.
     *
     * @see #setPivotX(float)
     */
    public float getPivotX() {
        return mPivotX;
    }

    /**
     * Sets the pivot value for the renderer on the Y axis
     *
     * @param pivotY The pivot value of the renderer on the Y axis, in pixels
     * @return True if the value changed, false if the new value was the same as the previous value.
     * @see View#setPivotY(float)
     * @see #getPivotY()
     */
    public boolean setPivotY(float pivotY) {
        final boolean dirty;
        if (mPivotY != pivotY) {
            mPivotY = pivotY;
            dirty = true;
        } else {
            dirty = false;
        }
        if (dirty || !mPivotExplicitlySet) {
            mMatrixOrPivotDirty = true;
            mPivotExplicitlySet = true;
            return true;
        }
        return false;
    }

    /**
     * Returns the pivot value for this renderer on the Y axis, in pixels.
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
     * Gets the left position for the RenderNode.
     *
     * @return the left position in pixels
     */
    public int getLeft() {
        return mLeft;
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
     * Gets the right position for the RenderNode.
     *
     * @return the right position in pixels
     */
    public int getRight() {
        return mRight;
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
        if (left != mLeft || top != mTop ||
                right != mRight || bottom != mBottom) {
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

    public boolean isTransitionLayer() {
        return (mAlpha < 1 && mAlpha >= 0.001f && mHasOverlappingRendering);
    }
}
