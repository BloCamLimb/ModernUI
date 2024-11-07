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

package icyllis.modernui.widget;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.*;
import icyllis.modernui.graphics.drawable.*;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.view.MeasureSpec;
import icyllis.modernui.view.View;

/**
 * Displays image resources, for example {@link icyllis.modernui.graphics.Image}
 * or {@link icyllis.modernui.graphics.drawable.Drawable} resources.
 * ImageView is also commonly used to
 * <a href="#setImageTintMode(android.graphics.PorterDuff.Mode)">apply tints to an image</a> and
 * handle <a href="#setScaleType(android.widget.ImageView.ScaleType)">image scaling</a>.
 *
 * <p>
 * To learn more about Drawables, see:
 * <a href="https://developer.android.com/guide/topics/resources/drawable-resource">Drawable
 * Resources</a>.
 * </p>
 */
public class ImageView extends View {

    private final Matrix mMatrix = new Matrix();
    private ScaleType mScaleType = ScaleType.FIT_CENTER;
    private boolean mAdjustViewBounds = false;
    private int mMaxWidth = Integer.MAX_VALUE;
    private int mMaxHeight = Integer.MAX_VALUE;

    // these are applied to the drawable
    private ColorFilter mImageColorFilter = null;
    private boolean mHasImageColorFilter = false;
    private int mImageAlpha = 255;
    private boolean mHasImageAlpha = false;

    private Drawable mDrawable = null;
    private ImageDrawable mRecycleImageDrawable = null;
    private ColorStateList mDrawableTintList = null;
    private BlendMode mDrawableBlendMode = null;
    private boolean mHasDrawableTint = false;
    private boolean mHasDrawableBlendMode = false;

    private int[] mState = null;
    private boolean mMergeState = false;
    private boolean mHasLevelSet = false;
    private int mLevel = 0;
    private int mDrawableWidth;
    private int mDrawableHeight;
    private Matrix mDrawMatrix = null;

    private boolean mCropToPadding = false;

    private int mBaseline = -1;
    private boolean mBaselineAlignBottom = false;

    public ImageView(Context context) {
        super(context);
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable dr) {
        return mDrawable == dr || super.verifyDrawable(dr);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mDrawable != null) {
            mDrawable.jumpToCurrentState();
        }
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable dr) {
        if (dr == mDrawable) {
            // update cached drawable dimensions if they've changed
            final int w = dr.getIntrinsicWidth();
            final int h = dr.getIntrinsicHeight();
            if (w != mDrawableWidth || h != mDrawableHeight) {
                mDrawableWidth = w;
                mDrawableHeight = h;
                // updates the matrix, which is dependent on the bounds
                configureBounds();
            }
            /* we invalidate the whole view in this case because it's very
             * hard to know where the drawable actually is. This is made
             * complicated because of the offsets and transformations that
             * can be applied. In theory, we could get the drawable's bounds
             * and run them through the transformation and offsets, but this
             * is probably not worth the effort.
             */
            invalidate();
        } else {
            super.invalidateDrawable(dr);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return (getBackground() != null && getBackground().getCurrent() != null);
    }

    /**
     * True when ImageView is adjusting its bounds
     * to preserve the aspect ratio of its drawable
     *
     * @return whether to adjust the bounds of this view
     * to preserve the original aspect ratio of the drawable
     * @see #setAdjustViewBounds(boolean)
     */
    public boolean getAdjustViewBounds() {
        return mAdjustViewBounds;
    }

    /**
     * Set this to true if you want the ImageView to adjust its bounds
     * to preserve the aspect ratio of its drawable.
     *
     * @param adjustViewBounds Whether to adjust the bounds of this view
     *                         to preserve the original aspect ratio of the drawable.
     * @see #getAdjustViewBounds()
     */
    public void setAdjustViewBounds(boolean adjustViewBounds) {
        mAdjustViewBounds = adjustViewBounds;
        if (adjustViewBounds) {
            setScaleType(ScaleType.FIT_CENTER);
        }
    }

    /**
     * The maximum width of this view.
     *
     * @return The maximum width of this view
     * @see #setMaxWidth(int)
     */
    public int getMaxWidth() {
        return mMaxWidth;
    }

    /**
     * An optional argument to supply a maximum width for this view. Only valid if
     * {@link #setAdjustViewBounds(boolean)} has been set to true. To set an image to be a maximum
     * of 100 x 100 while preserving the original aspect ratio, do the following: 1) set
     * adjustViewBounds to true 2) set maxWidth and maxHeight to 100 3) set the height and width
     * layout params to WRAP_CONTENT.
     *
     * <p>
     * Note that this view could be still smaller than 100 x 100 using this approach if the original
     * image is small. To set an image to a fixed size, specify that size in the layout params and
     * then use {@link #setScaleType(ScaleType)} to determine how to fit
     * the image within the bounds.
     * </p>
     *
     * @param maxWidth maximum width for this view
     * @see #getMaxWidth()
     */
    public void setMaxWidth(int maxWidth) {
        mMaxWidth = maxWidth;
    }

    /**
     * The maximum height of this view.
     *
     * @return The maximum height of this view
     * @see #setMaxHeight(int)
     */
    public int getMaxHeight() {
        return mMaxHeight;
    }

    /**
     * An optional argument to supply a maximum height for this view. Only valid if
     * {@link #setAdjustViewBounds(boolean)} has been set to true. To set an image to be a
     * maximum of 100 x 100 while preserving the original aspect ratio, do the following: 1) set
     * adjustViewBounds to true 2) set maxWidth and maxHeight to 100 3) set the height and width
     * layout params to WRAP_CONTENT.
     *
     * <p>
     * Note that this view could be still smaller than 100 x 100 using this approach if the original
     * image is small. To set an image to a fixed size, specify that size in the layout params and
     * then use {@link #setScaleType(ScaleType)} to determine how to fit
     * the image within the bounds.
     * </p>
     *
     * @param maxHeight maximum height for this view
     * @see #getMaxHeight()
     */
    public void setMaxHeight(int maxHeight) {
        mMaxHeight = maxHeight;
    }

    /**
     * Gets the current Drawable, or null if no Drawable has been
     * assigned.
     *
     * @return the view's drawable, or null if no drawable has been
     * assigned.
     */
    @Nullable
    public Drawable getDrawable() {
        if (mDrawable == mRecycleImageDrawable) {
            // Consider our cached version dirty since app code now has a reference to it
            mRecycleImageDrawable = null;
        }
        return mDrawable;
    }

    /**
     * Sets a drawable as the content of this ImageView.
     *
     * @param drawable the Drawable to set, or {@code null} to clear the
     *                 content
     */
    public void setImageDrawable(@Nullable Drawable drawable) {
        if (mDrawable != drawable) {
            final int oldWidth = mDrawableWidth;
            final int oldHeight = mDrawableHeight;

            updateDrawable(drawable);

            if (oldWidth != mDrawableWidth || oldHeight != mDrawableHeight) {
                requestLayout();
            }
            invalidate();
        }
    }

    /**
     * Applies a tint to the image drawable. Does not modify the current tint
     * mode, which is {@link BlendMode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link #setImageDrawable(Drawable)} will automatically
     * mutate the drawable and apply the specified tint and tint mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     * @see #getImageTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setImageTintList(@Nullable ColorStateList tint) {
        mDrawableTintList = tint;
        mHasDrawableTint = true;

        applyImageTint();
    }

    /**
     * Get the current {@link ColorStateList} used to tint the image Drawable,
     * or null if no tint is applied.
     *
     * @return the tint applied to the image drawable
     * @see #setImageTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getImageTintList() {
        return mDrawableTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setImageTintList(ColorStateList)}} to the image drawable. The default
     * mode is {@link BlendMode#SRC_IN}.
     *
     * @param blendMode the blending mode used to apply the tint, may be
     *                  {@code null} to clear tint
     * @see #getImageTintBlendMode()
     * @see Drawable#setTintBlendMode(BlendMode)
     */
    public void setImageTintBlendMode(@Nullable BlendMode blendMode) {
        mDrawableBlendMode = blendMode;
        mHasDrawableBlendMode = true;

        applyImageTint();
    }

    /**
     * Gets the blending mode used to apply the tint to the image Drawable
     *
     * @return the blending mode used to apply the tint to the image Drawable
     * @see #setImageTintBlendMode(BlendMode)
     */
    @Nullable
    public BlendMode getImageTintBlendMode() {
        return mDrawableBlendMode;
    }

    private void applyImageTint() {
        if (mDrawable != null && (mHasDrawableTint || mHasDrawableBlendMode)) {
            mDrawable = mDrawable.mutate();

            if (mHasDrawableTint) {
                mDrawable.setTintList(mDrawableTintList);
            }

            if (mHasDrawableBlendMode) {
                mDrawable.setTintBlendMode(mDrawableBlendMode);
            }

            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (mDrawable.isStateful()) {
                mDrawable.setState(getDrawableState());
            }
        }
    }

    /**
     * Sets a Image as the content of this ImageView.
     *
     * @param image the image to set
     */
    public void setImage(@Nullable Image image) {
        // Hacky fix to force setImageDrawable to do a full setImageDrawable
        // instead of doing an object reference comparison
        mDrawable = null;
        if (mRecycleImageDrawable == null) {
            mRecycleImageDrawable = new ImageDrawable(getContext().getResources(), image);
        } else {
            mRecycleImageDrawable.setImage(image);
        }
        setImageDrawable(mRecycleImageDrawable);
    }

    /**
     * Set the state of the current {@link icyllis.modernui.graphics.drawable.StateListDrawable}.
     * For more information about State List Drawables, see:
     * <a href="https://developer.android.com/guide/topics/resources/drawable-resource.html#StateList">the
     * Drawable Resource Guide</a>.
     *
     * @param state the state to set for the StateListDrawable
     * @param merge if true, merges the state values for the state you specify into the current state
     */
    public void setImageState(int[] state, boolean merge) {
        mState = state;
        mMergeState = merge;
        if (mDrawable != null) {
            refreshDrawableState();
            resizeFromDrawable();
        }
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        resizeFromDrawable();
    }

    /**
     * Sets the image level, when it is constructed from a
     * {@link LevelListDrawable}.
     *
     * @param level The new level for the image.
     */
    public void setImageLevel(int level) {
        mLevel = level;
        mHasLevelSet = true;
        if (mDrawable != null) {
            mDrawable.setLevel(level);
            resizeFromDrawable();
        }
    }

    /**
     * Controls how the image should be resized or moved to match the size
     * of this ImageView.
     *
     * @param scaleType The desired scaling mode.
     */
    public void setScaleType(@NonNull ScaleType scaleType) {
        if (mScaleType != scaleType) {
            mScaleType = scaleType;

            requestLayout();
            invalidate();
        }
    }

    /**
     * Returns the current ScaleType that is used to scale the bounds of an image to the bounds of the ImageView.
     *
     * @return The ScaleType used to scale the image.
     * @see ImageView.ScaleType
     */
    @NonNull
    public ScaleType getScaleType() {
        return mScaleType;
    }

    /**
     * Returns the view's optional matrix. This is applied to the
     * view's drawable when it is drawn. If there is no matrix,
     * this method will return an identity matrix.
     * Do not change this matrix in place but make a copy.
     * If you want a different matrix applied to the drawable,
     * be sure to call setImageMatrix().
     */
    public Matrix getImageMatrix() {
        if (mDrawMatrix == null) {
            return new Matrix();
        }
        return mDrawMatrix;
    }

    /**
     * Adds a transformation {@link Matrix} that is applied
     * to the view's drawable when it is drawn.  Allows custom scaling,
     * translation, and perspective distortion.
     *
     * @param matrix The transformation parameters in matrix form.
     */
    public void setImageMatrix(@Nullable Matrix matrix) {
        // collapse null and identity to just null
        if (matrix != null && matrix.isIdentity()) {
            matrix = null;
        }

        // don't invalidate unless we're actually changing our matrix
        if (matrix == null && !mMatrix.isIdentity() ||
                matrix != null && !mMatrix.equals(matrix)) {
            mMatrix.set(matrix);
            configureBounds();
            invalidate();
        }
    }

    /**
     * Return whether this ImageView crops to padding.
     *
     * @return whether this ImageView crops to padding
     * @see #setCropToPadding(boolean)
     */
    public boolean getCropToPadding() {
        return mCropToPadding;
    }

    /**
     * Sets whether this ImageView will crop to padding.
     *
     * @param cropToPadding whether this ImageView will crop to padding
     * @see #getCropToPadding()
     */
    public void setCropToPadding(boolean cropToPadding) {
        if (mCropToPadding != cropToPadding) {
            mCropToPadding = cropToPadding;
            requestLayout();
            invalidate();
        }
    }

    @NonNull
    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        if (mState == null) {
            return super.onCreateDrawableState(extraSpace);
        } else if (!mMergeState) {
            return mState;
        } else {
            return mergeDrawableStates(
                    super.onCreateDrawableState(extraSpace + mState.length), mState);
        }
    }

    private void updateDrawable(@Nullable Drawable d) {
        if (d != mRecycleImageDrawable && mRecycleImageDrawable != null) {
            mRecycleImageDrawable.setImage(null);
        }

        boolean sameDrawable = false;

        if (mDrawable != null) {
            sameDrawable = mDrawable == d;
            mDrawable.setCallback(null);
            unscheduleDrawable(mDrawable);
            if (!sameDrawable && isAttachedToWindow()) {
                mDrawable.setVisible(false, false);
            }
        }

        mDrawable = d;

        if (d != null) {
            d.setCallback(this);
            d.setLayoutDirection(getLayoutDirection());
            if (d.isStateful()) {
                d.setState(getDrawableState());
            }
            if (!sameDrawable) {
                final boolean visible = isAttachedToWindow() && getWindowVisibility() == VISIBLE && isShown();
                d.setVisible(visible, true);
            }
            if (mHasLevelSet) {
                d.setLevel(mLevel);
            }
            mDrawableWidth = d.getIntrinsicWidth();
            mDrawableHeight = d.getIntrinsicHeight();
            applyImageTint();
            applyColorFilter();
            applyAlpha();

            configureBounds();
        } else {
            mDrawableWidth = mDrawableHeight = -1;
        }
    }

    private void resizeFromDrawable() {
        final Drawable d = mDrawable;
        if (d != null) {
            int w = d.getIntrinsicWidth();
            if (w < 0) w = mDrawableWidth;
            int h = d.getIntrinsicHeight();
            if (h < 0) h = mDrawableHeight;
            if (w != mDrawableWidth || h != mDrawableHeight) {
                mDrawableWidth = w;
                mDrawableHeight = h;
                requestLayout();
            }
        }
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        if (mDrawable != null) {
            mDrawable.setLayoutDirection(layoutDirection);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w;
        int h;

        // Desired aspect ratio of the view's contents (not including padding)
        float desiredAspect = 0.0f;

        // We are allowed to change the view's width
        boolean resizeWidth = false;

        // We are allowed to change the view's height
        boolean resizeHeight = false;

        final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        if (mDrawable == null) {
            // If no drawable, its intrinsic size is 0.
            mDrawableWidth = -1;
            mDrawableHeight = -1;
            w = h = 0;
        } else {
            w = mDrawableWidth;
            h = mDrawableHeight;
            if (w <= 0) w = 1;
            if (h <= 0) h = 1;

            // We are supposed to adjust view bounds to match the aspect
            // ratio of our drawable. See if that is possible.
            if (mAdjustViewBounds) {
                resizeWidth = widthSpecMode != MeasureSpec.EXACTLY;
                resizeHeight = heightSpecMode != MeasureSpec.EXACTLY;

                desiredAspect = (float) w / (float) h;
            }
        }

        final int pleft = mPaddingLeft;
        final int pright = mPaddingRight;
        final int ptop = mPaddingTop;
        final int pbottom = mPaddingBottom;

        int widthSize;
        int heightSize;

        if (resizeWidth || resizeHeight) {
            /* If we get here, it means we want to resize to match the
                drawables aspect ratio, and we have the freedom to change at
                least one dimension.
            */

            // Get the max possible width given our constraints
            widthSize = resolveAdjustedSize(w + pleft + pright, mMaxWidth, widthMeasureSpec);

            // Get the max possible height given our constraints
            heightSize = resolveAdjustedSize(h + ptop + pbottom, mMaxHeight, heightMeasureSpec);

            if (desiredAspect != 0.0f) {
                // See what our actual aspect ratio is
                final float actualAspect = (float) (widthSize - pleft - pright) /
                        (heightSize - ptop - pbottom);

                if (Math.abs(actualAspect - desiredAspect) > 0.0000001) {

                    boolean done = false;

                    // Try adjusting width to be proportional to height
                    if (resizeWidth) {
                        int newWidth = (int) (desiredAspect * (heightSize - ptop - pbottom)) +
                                pleft + pright;

                        // Allow the width to outgrow its original estimate if height is fixed.
                        if (!resizeHeight) {
                            widthSize = resolveAdjustedSize(newWidth, mMaxWidth, widthMeasureSpec);
                        }

                        if (newWidth <= widthSize) {
                            widthSize = newWidth;
                            done = true;
                        }
                    }

                    // Try adjusting height to be proportional to width
                    if (!done && resizeHeight) {
                        int newHeight = (int) ((widthSize - pleft - pright) / desiredAspect) +
                                ptop + pbottom;

                        // Allow the height to outgrow its original estimate if width is fixed.
                        if (!resizeWidth) {
                            heightSize = resolveAdjustedSize(newHeight, mMaxHeight,
                                    heightMeasureSpec);
                        }

                        if (newHeight <= heightSize) {
                            heightSize = newHeight;
                        }
                    }
                }
            }
        } else {
            /* We either don't want to preserve the drawables aspect ratio,
               or we are not allowed to change view dimensions. Just measure in
               the normal way.
            */
            w += pleft + pright;
            h += ptop + pbottom;

            w = Math.max(w, getSuggestedMinimumWidth());
            h = Math.max(h, getSuggestedMinimumHeight());

            widthSize = resolveSizeAndState(w, widthMeasureSpec, 0);
            heightSize = resolveSizeAndState(h, heightMeasureSpec, 0);
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    private int resolveAdjustedSize(int desiredSize, int maxSize,
                                    int measureSpec) {
        int result = desiredSize;
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case MeasureSpec.UNSPECIFIED ->
                // Parent says we can be as big as we want. Just don't be larger
                // than max size imposed on ourselves.
                    result = Math.min(desiredSize, maxSize);
            case MeasureSpec.AT_MOST ->
                // Parent says we can be as big as we want, up to specSize.
                // Don't be larger than specSize, and don't be larger than
                // the max size imposed on ourselves.
                    result = Math.min(Math.min(desiredSize, specSize), maxSize);
            case MeasureSpec.EXACTLY ->
                // No choice. Do what we are told.
                    result = specSize;
        }
        return result;
    }

    @Override
    protected void onSizeChanged(int width, int height, int prevWidth, int prevHeight) {
        super.onSizeChanged(width, height, prevWidth, prevHeight);
        configureBounds();
    }

    private void configureBounds() {
        if (mDrawable == null || !isAttachedToWindow()) {
            return;
        }

        final int dwidth = mDrawableWidth;
        final int dheight = mDrawableHeight;

        final int vwidth = getWidth() - mPaddingLeft - mPaddingRight;
        final int vheight = getHeight() - mPaddingTop - mPaddingBottom;

        final boolean fits = (dwidth < 0 || vwidth == dwidth)
                && (dheight < 0 || vheight == dheight);

        if (dwidth <= 0 || dheight <= 0 || ScaleType.FIT_XY == mScaleType) {
            /* If the drawable has no intrinsic size, or we're told to
                scaletofit, then we just fill our entire view.
            */
            mDrawable.setBounds(0, 0, vwidth, vheight);
            mDrawMatrix = null;
        } else {
            // We need to do the scaling ourself, so have the drawable
            // use its native size.
            mDrawable.setBounds(0, 0, dwidth, dheight);

            if (ScaleType.MATRIX == mScaleType) {
                // Use the specified matrix as-is.
                if (mMatrix.isIdentity()) {
                    mDrawMatrix = null;
                } else {
                    mDrawMatrix = mMatrix;
                }
            } else if (fits) {
                // The image fits exactly, no transform needed.
                mDrawMatrix = null;
            } else if (ScaleType.CENTER == mScaleType) {
                // Center image in view, no scaling.
                mDrawMatrix = mMatrix;
                mDrawMatrix.setTranslate(Math.round((vwidth - dwidth) * 0.5f),
                        Math.round((vheight - dheight) * 0.5f));
            } else if (ScaleType.CENTER_CROP == mScaleType) {
                mDrawMatrix = mMatrix;

                float scale;
                float dx = 0, dy = 0;

                if (dwidth * vheight > vwidth * dheight) {
                    scale = (float) vheight / (float) dheight;
                    dx = (vwidth - dwidth * scale) * 0.5f;
                } else {
                    scale = (float) vwidth / (float) dwidth;
                    dy = (vheight - dheight * scale) * 0.5f;
                }

                mDrawMatrix.setScaleTranslate(scale, scale, Math.round(dx), Math.round(dy));
            } else if (ScaleType.CENTER_INSIDE == mScaleType) {
                mDrawMatrix = mMatrix;
                float scale;
                float dx;
                float dy;

                if (dwidth <= vwidth && dheight <= vheight) {
                    scale = 1.0f;
                } else {
                    scale = Math.min((float) vwidth / (float) dwidth,
                            (float) vheight / (float) dheight);
                }

                dx = Math.round((vwidth - dwidth * scale) * 0.5f);
                dy = Math.round((vheight - dheight * scale) * 0.5f);

                mDrawMatrix.setScaleTranslate(scale, scale, dx, dy);
            } else {
                mDrawMatrix = mMatrix;
                float tx = 0, sx = (float) vwidth / dwidth;
                float ty = 0, sy = (float) vheight / dheight;
                boolean xLarger = false;

                if (mScaleType != ScaleType.FIT_XY) {
                    if (sx > sy) {
                        xLarger = true;
                        sx = sy;
                    } else {
                        sy = sx;
                    }
                }

                if (mScaleType == ScaleType.FIT_CENTER || mScaleType == ScaleType.FIT_END) {
                    float diff;

                    if (xLarger) {
                        diff = vwidth - dwidth * sy;
                    } else {
                        diff = vheight - dheight * sy;
                    }

                    if (mScaleType == ScaleType.FIT_CENTER) {
                        diff *= 0.5f;
                    }

                    if (xLarger) {
                        tx += diff;
                    } else {
                        ty += diff;
                    }
                }

                mDrawMatrix.setScaleTranslate(sx, sy, tx, ty);
            }
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        final Drawable drawable = mDrawable;
        if (drawable != null && drawable.isStateful()
                && drawable.setState(getDrawableState())) {
            invalidateDrawable(drawable);
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);

        if (mDrawable != null) {
            mDrawable.setHotspot(x, y);
        }
    }

    /**
     * Applies a temporary transformation {@link Matrix} to the view's drawable when it is drawn.
     * Allows custom scaling, translation, and perspective distortion during an animation.
     * <p>
     * This method is a lightweight analogue of {@link ImageView#setImageMatrix(Matrix)} to use
     * only during animations as this matrix will be cleared after the next drawable
     * update or view's bounds change.
     *
     * @param matrix The transformation parameters in matrix form.
     */
    public void animateTransform(@Nullable Matrix matrix) {
        if (mDrawable == null) {
            return;
        }
        if (matrix == null) {
            final int vwidth = getWidth() - mPaddingLeft - mPaddingRight;
            final int vheight = getHeight() - mPaddingTop - mPaddingBottom;
            mDrawable.setBounds(0, 0, vwidth, vheight);
            mDrawMatrix = null;
        } else {
            mDrawable.setBounds(0, 0, mDrawableWidth, mDrawableHeight);
            if (mDrawMatrix == null) {
                mDrawMatrix = new Matrix();
            }
            mDrawMatrix.set(matrix);
        }
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (mDrawable == null) {
            return;
        }
        if (mDrawableWidth == 0 || mDrawableHeight == 0) {
            return;     // nothing to draw (empty bounds)
        }

        if (mDrawMatrix == null && mPaddingTop == 0 && mPaddingLeft == 0) {
            mDrawable.draw(canvas);
        } else {
            final int saveCount = canvas.getSaveCount();
            canvas.save();

            if (mCropToPadding) {
                final int scrollX = mScrollX;
                final int scrollY = mScrollY;
                canvas.clipRect(scrollX + mPaddingLeft, scrollY + mPaddingTop,
                        scrollX + getWidth() - mPaddingRight,
                        scrollY + getHeight() - mPaddingBottom);
            }

            canvas.translate(mPaddingLeft, mPaddingTop);

            if (mDrawMatrix != null) {
                canvas.concat(mDrawMatrix);
            }
            mDrawable.draw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    /**
     * <p>Return the offset of the widget's text baseline from the widget's top
     * boundary. </p>
     *
     * @return the offset of the baseline within the widget's bounds or -1
     * if baseline alignment is not supported.
     */
    @Override
    public int getBaseline() {
        if (mBaselineAlignBottom) {
            return getMeasuredHeight();
        } else {
            return mBaseline;
        }
    }

    /**
     * <p>Set the offset of the widget's text baseline from the widget's top
     * boundary.  This value is overridden by the {@link #setBaselineAlignBottom(boolean)}
     * property.</p>
     *
     * @param baseline The baseline to use, or -1 if none is to be provided.
     * @see #setBaseline(int)
     */
    public void setBaseline(int baseline) {
        if (mBaseline != baseline) {
            mBaseline = baseline;
            requestLayout();
        }
    }

    /**
     * Sets whether the baseline of this view to the bottom of the view.
     * Setting this value overrides any calls to setBaseline.
     *
     * @param aligned If true, the image view will be baseline aligned by its bottom edge.
     */
    public void setBaselineAlignBottom(boolean aligned) {
        if (mBaselineAlignBottom != aligned) {
            mBaselineAlignBottom = aligned;
            requestLayout();
        }
    }

    /**
     * Checks whether this view's baseline is considered the bottom of the view.
     *
     * @return True if the ImageView's baseline is considered the bottom of the view, false if otherwise.
     * @see #setBaselineAlignBottom(boolean)
     */
    public boolean getBaselineAlignBottom() {
        return mBaselineAlignBottom;
    }

    /**
     * Returns the active color filter for this ImageView.
     *
     * @return the active color filter for this ImageView
     * @see #setColorFilter(ColorFilter)
     */
    @Nullable
    public ColorFilter getColorFilter() {
        return mImageColorFilter;
    }

    /**
     * Apply an arbitrary color filter to the image.
     *
     * @param colorFilter the color filter to apply (can be null)
     * @see #getColorFilter()
     */
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        if (mImageColorFilter != colorFilter) {
            mImageColorFilter = colorFilter;
            mHasImageColorFilter = true;
            applyColorFilter();
            invalidate();
        }
    }

    /**
     * Returns the alpha that will be applied to the drawable of this ImageView.
     *
     * @return the alpha value that will be applied to the drawable of this
     * ImageView (between 0 and 255 inclusive, with 0 being transparent and
     * 255 being opaque)
     * @see #setImageAlpha(int)
     */
    public int getImageAlpha() {
        return mImageAlpha;
    }

    /**
     * Sets the alpha value that should be applied to the image.
     *
     * @param alpha the alpha value that should be applied to the image (between
     *              0 and 255 inclusive, with 0 being transparent and 255 being opaque)
     * @see #getImageAlpha()
     */
    public void setImageAlpha(int alpha) {
        alpha += alpha >> 7; // keep it legal
        if (mImageAlpha != alpha) {
            mImageAlpha = alpha;
            mHasImageAlpha = true;
            applyAlpha();
            invalidate();
        }
    }

    private void applyColorFilter() {
        if (mDrawable != null && mHasImageColorFilter) {
            mDrawable = mDrawable.mutate();
            mDrawable.setColorFilter(mImageColorFilter);
        }
    }

    private void applyAlpha() {
        if (mDrawable != null && mHasImageAlpha) {
            mDrawable = mDrawable.mutate();
            mDrawable.setAlpha(mImageAlpha);
        }
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        if (mDrawable != null) {
            mDrawable.setVisible(isVisible, false);
        }
    }

    /**
     * Options for scaling the bounds of an image to the bounds of this view.
     */
    public enum ScaleType {
        /**
         * Scale using the image matrix when drawing. The image matrix can be set using
         * {@link ImageView#setImageMatrix(Matrix)}.
         */
        MATRIX,
        /**
         * Scale in X and Y independently, so that src matches dst exactly. This may change the
         * aspect ratio of the src.
         */
        FIT_XY,
        /**
         * Compute a scale that will maintain the original src aspect ratio, but will also ensure
         * that src fits entirely inside dst. At least one axis (X or Y) will fit exactly. START
         * aligns the result to the left and top edges of dst.
         */
        FIT_START,
        /**
         * Compute a scale that will maintain the original src aspect ratio, but will also ensure
         * that src fits entirely inside dst. At least one axis (X or Y) will fit exactly. The
         * result is centered inside dst.
         */
        FIT_CENTER,
        /**
         * Compute a scale that will maintain the original src aspect ratio, but will also ensure
         * that src fits entirely inside dst. At least one axis (X or Y) will fit exactly. END
         * aligns the result to the right and bottom edges of dst.
         */
        FIT_END,
        /**
         * Center the image in the view, but perform no scaling.
         */
        CENTER,
        /**
         * Scale the image uniformly (maintain the image's aspect ratio) so
         * that both dimensions (width and height) of the image will be equal
         * to or larger than the corresponding dimension of the view
         * (minus padding). The image is then centered in the view.
         */
        CENTER_CROP,
        /**
         * Scale the image uniformly (maintain the image's aspect ratio) so
         * that both dimensions (width and height) of the image will be equal
         * to or less than the corresponding dimension of the view
         * (minus padding). The image is then centered in the view.
         */
        CENTER_INSIDE
    }
}
