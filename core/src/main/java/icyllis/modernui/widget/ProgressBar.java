/*
 * Modern UI.
 * Copyright (C) 2023-2025 BloCamLimb. All rights reserved.
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

import icyllis.modernui.R;
import icyllis.modernui.animation.Animator;
import icyllis.modernui.animation.AnimatorListener;
import icyllis.modernui.animation.ObjectAnimator;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.annotation.StyleableRes;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.BlendMode;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.graphics.drawable.Animatable;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.drawable.LayerDrawable;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.TypedArray;
import icyllis.modernui.util.AttributeSet;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.FloatProperty;
import icyllis.modernui.view.View;

import java.text.NumberFormat;
import java.util.Locale;

public class ProgressBar extends View {

    private int mMaxWidth = 1000;
    private int mMaxHeight = 1000;

    private int mProgress;
    private int mSecondaryProgress;
    private int mMin;
    private int mMax;

    private boolean mIndeterminate;

    private Drawable mIndeterminateDrawable;
    private Drawable mProgressDrawable;
    private Drawable mCurrentDrawable;
    private ProgressTintInfo mProgressTintInfo;

    private boolean mShouldStartAnimationDrawable;

    /**
     * Value used to track progress animation, in the range [0..1].
     */
    private float mVisualProgress;

    private boolean mMirrorForRtl = false;

    private boolean mAggregatedIsVisible;

    private ObjectAnimator mLastProgressAnimator;

    private NumberFormat mPercentFormat;
    private Locale mCachedLocale;

    /**
     * Property wrapper around the visual state of the {@code progress} functionality
     * handled by the {@link ProgressBar#setProgress(int, boolean)} method. This does
     * not correspond directly to the actual progress -- only the visual state.
     */
    protected static final FloatProperty<ProgressBar> VISUAL_PROGRESS = new FloatProperty<>("visualProgress") {
        @Override
        public void setValue(ProgressBar object, float value) {
            object.setVisualProgress(R.id.progress, value);
        }

        @Override
        public Float get(ProgressBar object) {
            return object.mVisualProgress;
        }
    };

    @StyleableRes
    private static final String[] STYLEABLE = {
            /*0*/R.ns, R.attr.indeterminate,
            /*1*/R.ns, R.attr.indeterminateDrawable,
            /*2*/R.ns, R.attr.max,
            /*3*/R.ns, R.attr.maxHeight,
            /*4*/R.ns, R.attr.maxWidth,
            /*5*/R.ns, R.attr.min,
            /*6*/R.ns, R.attr.mirrorForRtl,
            /*7*/R.ns, R.attr.progress,
            /*8*/R.ns, R.attr.progressDrawable,
            /*9*/R.ns, R.attr.secondaryProgress,
    };

    @AttrRes
    private static final ResourceId DEF_STYLE_ATTR =
            ResourceId.attr(R.ns, R.attr.progressBarStyle);

    /**
     * Create a new progress bar with range 0...10000 and initial progress of 0.
     *
     * @param context the application environment
     */
    public ProgressBar(Context context) {
        this(context, null);
    }

    public ProgressBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, DEF_STYLE_ATTR);
    }

    public ProgressBar(Context context, @Nullable AttributeSet attrs,
                       @Nullable @AttrRes ResourceId defStyleAttr) {
        this(context, attrs, defStyleAttr, null);
    }

    public ProgressBar(Context context, @Nullable AttributeSet attrs,
                       @Nullable @AttrRes ResourceId defStyleAttr,
                       @Nullable @StyleRes ResourceId defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        final TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, defStyleAttr, defStyleRes, STYLEABLE);

        final Drawable progressDrawable = a.getDrawable(8); // progressDrawable
        if (progressDrawable != null) {
            setProgressDrawable(progressDrawable);
        }

        mMaxWidth = a.getDimensionPixelSize(4, mMaxWidth);
        mMaxHeight = a.getDimensionPixelSize(3, mMaxHeight);

        // initialize min/max/progress/secondaryProgress
        mMin = a.getInt(5, 0);
        mMax = Math.max(a.getInt(2, 10000), mMin);
        mProgress = MathUtil.clamp(0, mMin, mMax);
        mSecondaryProgress = MathUtil.clamp(0, mMin, mMax);

        setProgress(a.getInt(7, mProgress));

        setSecondaryProgress(a.getInt(9, mSecondaryProgress));

        final Drawable indeterminateDrawable = a.getDrawable(1);
        if (indeterminateDrawable != null) {
            setIndeterminateDrawable(indeterminateDrawable);
        }

        setIndeterminate(a.getBoolean(0, mIndeterminate));

        mMirrorForRtl = a.getBoolean(6, mMirrorForRtl);

        a.recycle();
    }

    /**
     * Sets the maximum width the progress bar can have.
     *
     * @param maxWidth the maximum width to be set, in pixels
     */
    public void setMaximumWidth(int maxWidth) {
        mMaxWidth = maxWidth;
        requestLayout();
    }

    /**
     * @return the maximum width the progress bar can have, in pixels
     */
    public int getMaximumWidth() {
        return mMaxWidth;
    }

    /**
     * Sets the maximum height the progress bar can have.
     *
     * @param maxHeight the maximum height to be set, in pixels
     */
    public void setMaximumHeight(int maxHeight) {
        mMaxHeight = maxHeight;
        requestLayout();
    }

    /**
     * @return the maximum height the progress bar can have, in pixels
     */
    public int getMaximumHeight() {
        return mMaxHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int dw = 0;
        int dh = 0;

        final Drawable d = mCurrentDrawable;
        if (d != null) {
            dw = MathUtil.clamp(d.getIntrinsicWidth(), getMinimumWidth(), mMaxWidth);
            dh = MathUtil.clamp(d.getIntrinsicHeight(), getMinimumHeight(), mMaxHeight);
        }

        updateDrawableState();

        dw += mPaddingLeft + mPaddingRight;
        dh += mPaddingTop + mPaddingBottom;

        final int measuredWidth = resolveSizeAndState(dw, widthMeasureSpec, 0);
        final int measuredHeight = resolveSizeAndState(dh, heightMeasureSpec, 0);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    /**
     * <p>Indicate whether this progress bar is in indeterminate mode.</p>
     *
     * @return true if the progress bar is in indeterminate mode
     */
    public boolean isIndeterminate() {
        return mIndeterminate;
    }

    /**
     * <p>Change the indeterminate mode for this progress bar. In indeterminate
     * mode, the progress is ignored and the progress bar shows an infinite
     * animation instead.</p>
     * <p>
     * If this progress bar's style only supports indeterminate mode (such as the circular
     * progress bars), then this will be ignored.
     *
     * @param indeterminate true to enable the indeterminate mode
     */
    public void setIndeterminate(boolean indeterminate) {
        if (indeterminate != mIndeterminate) {
            mIndeterminate = indeterminate;

            if (indeterminate) {
                // swap between indeterminate and regular backgrounds
                swapCurrentDrawable(mIndeterminateDrawable);
                startAnimation();
            } else {
                swapCurrentDrawable(mProgressDrawable);
                stopAnimation();
            }
        }
    }

    private void swapCurrentDrawable(Drawable newDrawable) {
        final Drawable oldDrawable = mCurrentDrawable;
        mCurrentDrawable = newDrawable;

        if (oldDrawable != mCurrentDrawable) {
            if (oldDrawable != null) {
                oldDrawable.setVisible(false, false);
            }
            if (mCurrentDrawable != null) {
                mCurrentDrawable.setVisible(getWindowVisibility() == VISIBLE && isShown(), false);
            }
        }
    }

    /**
     * <p>Get the drawable used to draw the progress bar in
     * indeterminate mode.</p>
     *
     * @return a {@link Drawable} instance
     * @see #setIndeterminateDrawable(Drawable)
     * @see #setIndeterminate(boolean)
     */
    public Drawable getIndeterminateDrawable() {
        return mIndeterminateDrawable;
    }

    /**
     * Define the drawable used to draw the progress bar in indeterminate mode.
     *
     * <p>For the Drawable to animate, it must implement {@link Animatable}.
     * A Drawable that implements Animatable will be animated
     * via that interface and therefore provides the greatest amount of customization.
     *
     * @param d the new drawable
     * @see #getIndeterminateDrawable()
     * @see #setIndeterminate(boolean)
     */
    public void setIndeterminateDrawable(Drawable d) {
        if (mIndeterminateDrawable != d) {
            if (mIndeterminateDrawable != null) {
                mIndeterminateDrawable.setCallback(null);
                unscheduleDrawable(mIndeterminateDrawable);
            }

            mIndeterminateDrawable = d;

            if (d != null) {
                d.setCallback(this);
                d.setLayoutDirection(getLayoutDirection());
                if (d.isStateful()) {
                    d.setState(getDrawableState());
                }
                applyIndeterminateTint();
            }

            if (mIndeterminate) {
                swapCurrentDrawable(d);
                postInvalidate();
            }
        }
    }

    /**
     * Applies a tint to the indeterminate drawable. Does not modify the
     * current tint mode, which is {@link BlendMode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link #setIndeterminateDrawable(Drawable)} will
     * automatically mutate the drawable and apply the specified tint and
     * tint mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     * @see #getIndeterminateTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setIndeterminateTintList(@Nullable ColorStateList tint) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mIndeterminateTintList = tint;
        mProgressTintInfo.mHasIndeterminateTint = true;

        applyIndeterminateTint();
    }

    /**
     * @return the tint applied to the indeterminate drawable
     * @see #setIndeterminateTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getIndeterminateTintList() {
        return mProgressTintInfo != null ? mProgressTintInfo.mIndeterminateTintList : null;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setIndeterminateTintList(ColorStateList)} to the indeterminate
     * drawable. The default mode is {@link BlendMode#SRC_IN}.
     *
     * @param blendMode the blending mode used to apply the tint, may be
     *                  {@code null} to clear tint
     * @see #setIndeterminateTintList(ColorStateList)
     * @see Drawable#setTintBlendMode(BlendMode)
     */
    public void setIndeterminateTintBlendMode(@Nullable BlendMode blendMode) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mIndeterminateBlendMode = blendMode;
        mProgressTintInfo.mHasIndeterminateTintMode = true;

        applyIndeterminateTint();
    }

    /**
     * Returns the blending mode used to apply the tint to the indeterminate
     * drawable, if specified.
     *
     * @return the blending mode used to apply the tint to the indeterminate
     * drawable
     * @see #setIndeterminateTintBlendMode(BlendMode)
     */
    @Nullable
    public BlendMode getIndeterminateTintBlendMode() {
        return mProgressTintInfo != null ? mProgressTintInfo.mIndeterminateBlendMode : null;
    }

    private void applyIndeterminateTint() {
        if (mIndeterminateDrawable != null && mProgressTintInfo != null) {
            final ProgressTintInfo tintInfo = mProgressTintInfo;
            if (tintInfo.mHasIndeterminateTint || tintInfo.mHasIndeterminateTintMode) {
                mIndeterminateDrawable = mIndeterminateDrawable.mutate();

                if (tintInfo.mHasIndeterminateTint) {
                    mIndeterminateDrawable.setTintList(tintInfo.mIndeterminateTintList);
                }

                if (tintInfo.mHasIndeterminateTintMode) {
                    mIndeterminateDrawable.setTintBlendMode(tintInfo.mIndeterminateBlendMode);
                }

                // The drawable (or one of its children) may not have been
                // stateful before applying the tint, so let's try again.
                if (mIndeterminateDrawable.isStateful()) {
                    mIndeterminateDrawable.setState(getDrawableState());
                }
            }
        }
    }

    /**
     * <p>Get the drawable used to draw the progress bar in
     * progress mode.</p>
     *
     * @return a {@link Drawable} instance
     * @see #setProgressDrawable(Drawable)
     * @see #setIndeterminate(boolean)
     */
    public Drawable getProgressDrawable() {
        return mProgressDrawable;
    }

    /**
     * Define the drawable used to draw the progress bar in progress mode.
     *
     * @param d the new drawable
     * @see #getProgressDrawable()
     * @see #setIndeterminate(boolean)
     */
    public void setProgressDrawable(Drawable d) {
        if (mProgressDrawable != d) {
            if (mProgressDrawable != null) {
                mProgressDrawable.setCallback(null);
                unscheduleDrawable(mProgressDrawable);
            }

            mProgressDrawable = d;

            if (d != null) {
                d.setCallback(this);
                d.setLayoutDirection(getLayoutDirection());
                if (d.isStateful()) {
                    d.setState(getDrawableState());
                }

                // Make sure the ProgressBar is always tall enough
                int drawableHeight = d.getMinimumHeight();
                if (mMaxHeight < drawableHeight) {
                    mMaxHeight = drawableHeight;
                    requestLayout();
                }

                applyProgressTints();
            }

            if (!mIndeterminate) {
                swapCurrentDrawable(d);
                postInvalidate();
            }

            updateDrawableBounds(getWidth(), getHeight());
            updateDrawableState();

            doRefreshProgress(R.id.progress, mProgress, false, false, false);
            doRefreshProgress(R.id.secondaryProgress, mSecondaryProgress, false, false, false);
        }
    }

    /**
     * Applies the progress tints in order of increasing specificity.
     */
    private void applyProgressTints() {
        if (mProgressDrawable != null && mProgressTintInfo != null) {
            applyPrimaryProgressTint();
            applyProgressBackgroundTint();
            applySecondaryProgressTint();
        }
    }

    /**
     * Should only be called if we've already verified that mProgressDrawable
     * and mProgressTintInfo are non-null.
     */
    private void applyPrimaryProgressTint() {
        if (mProgressTintInfo.mHasProgressTint
                || mProgressTintInfo.mHasProgressTintMode) {
            final Drawable target = getTintTarget(R.id.progress, true);
            if (target != null) {
                if (mProgressTintInfo.mHasProgressTint) {
                    target.setTintList(mProgressTintInfo.mProgressTintList);
                }
                if (mProgressTintInfo.mHasProgressTintMode) {
                    target.setTintBlendMode(mProgressTintInfo.mProgressBlendMode);
                }

                // The drawable (or one of its children) may not have been
                // stateful before applying the tint, so let's try again.
                if (target.isStateful()) {
                    target.setState(getDrawableState());
                }
            }
        }
    }

    /**
     * Should only be called if we've already verified that mProgressDrawable
     * and mProgressTintInfo are non-null.
     */
    private void applyProgressBackgroundTint() {
        if (mProgressTintInfo.mHasProgressBackgroundTint
                || mProgressTintInfo.mHasProgressBackgroundTintMode) {
            final Drawable target = getTintTarget(R.id.background, false);
            if (target != null) {
                if (mProgressTintInfo.mHasProgressBackgroundTint) {
                    target.setTintList(mProgressTintInfo.mProgressBackgroundTintList);
                }
                if (mProgressTintInfo.mHasProgressBackgroundTintMode) {
                    target.setTintBlendMode(mProgressTintInfo.mProgressBackgroundBlendMode);
                }

                // The drawable (or one of its children) may not have been
                // stateful before applying the tint, so let's try again.
                if (target.isStateful()) {
                    target.setState(getDrawableState());
                }
            }
        }
    }

    /**
     * Should only be called if we've already verified that mProgressDrawable
     * and mProgressTintInfo are non-null.
     */
    private void applySecondaryProgressTint() {
        if (mProgressTintInfo.mHasSecondaryProgressTint
                || mProgressTintInfo.mHasSecondaryProgressTintMode) {
            final Drawable target = getTintTarget(R.id.secondaryProgress, false);
            if (target != null) {
                if (mProgressTintInfo.mHasSecondaryProgressTint) {
                    target.setTintList(mProgressTintInfo.mSecondaryProgressTintList);
                }
                if (mProgressTintInfo.mHasSecondaryProgressTintMode) {
                    target.setTintBlendMode(mProgressTintInfo.mSecondaryProgressBlendMode);
                }

                // The drawable (or one of its children) may not have been
                // stateful before applying the tint, so let's try again.
                if (target.isStateful()) {
                    target.setState(getDrawableState());
                }
            }
        }
    }

    /**
     * Applies a tint to the progress indicator, if one exists, or to the
     * entire progress drawable otherwise. Does not modify the current tint
     * mode, which is {@link BlendMode#SRC_IN} by default.
     * <p>
     * The progress indicator should be specified as a layer with
     * id {@link R.id#progress} in a {@link LayerDrawable}
     * used as the progress drawable.
     * <p>
     * Subsequent calls to {@link #setProgressDrawable(Drawable)} will
     * automatically mutate the drawable and apply the specified tint and
     * tint mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     * @see #getProgressTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setProgressTintList(@Nullable ColorStateList tint) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mProgressTintList = tint;
        mProgressTintInfo.mHasProgressTint = true;

        if (mProgressDrawable != null) {
            applyPrimaryProgressTint();
        }
    }

    /**
     * Returns the tint applied to the progress drawable, if specified.
     *
     * @return the tint applied to the progress drawable
     * @see #setProgressTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getProgressTintList() {
        return mProgressTintInfo != null ? mProgressTintInfo.mProgressTintList : null;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setProgressTintList(ColorStateList)}} to the progress
     * indicator. The default mode is {@link BlendMode#SRC_IN}.
     *
     * @param blendMode the blending mode used to apply the tint, may be
     *                  {@code null} to clear tint
     * @see #getProgressTintBlendMode()
     * @see Drawable#setTintBlendMode(BlendMode)
     */
    public void setProgressTintBlendMode(@Nullable BlendMode blendMode) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mProgressBlendMode = blendMode;
        mProgressTintInfo.mHasProgressTintMode = true;

        if (mProgressDrawable != null) {
            applyPrimaryProgressTint();
        }
    }

    /**
     * Returns the blending mode used to apply the tint to the progress
     * drawable, if specified.
     *
     * @return the blending mode used to apply the tint to the progress
     * drawable
     * @see #setProgressTintBlendMode(BlendMode)
     */
    @Nullable
    public BlendMode getProgressTintBlendMode() {
        return mProgressTintInfo != null ? mProgressTintInfo.mProgressBlendMode : null;
    }

    /**
     * Applies a tint to the progress background, if one exists. Does not
     * modify the current tint mode, which is
     * {@link BlendMode#SRC_ATOP} by default.
     * <p>
     * The progress background must be specified as a layer with
     * id {@link R.id#background} in a {@link LayerDrawable}
     * used as the progress drawable.
     * <p>
     * Subsequent calls to {@link #setProgressDrawable(Drawable)} where the
     * drawable contains a progress background will automatically mutate the
     * drawable and apply the specified tint and tint mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     * @see #getProgressBackgroundTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setProgressBackgroundTintList(@Nullable ColorStateList tint) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mProgressBackgroundTintList = tint;
        mProgressTintInfo.mHasProgressBackgroundTint = true;

        if (mProgressDrawable != null) {
            applyProgressBackgroundTint();
        }
    }

    /**
     * Returns the tint applied to the progress background, if specified.
     *
     * @return the tint applied to the progress background
     * @see #setProgressBackgroundTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getProgressBackgroundTintList() {
        return mProgressTintInfo != null ? mProgressTintInfo.mProgressBackgroundTintList : null;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setProgressBackgroundTintList(ColorStateList)}} to the progress
     * background. The default mode is {@link BlendMode#SRC_IN}.
     *
     * @param blendMode the blending mode used to apply the tint, may be
     *                  {@code null} to clear tint
     * @see #setProgressBackgroundTintList(ColorStateList)
     * @see Drawable#setTintBlendMode(BlendMode)
     */
    public void setProgressBackgroundTintBlendMode(@Nullable BlendMode blendMode) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mProgressBackgroundBlendMode = blendMode;
        mProgressTintInfo.mHasProgressBackgroundTintMode = true;

        if (mProgressDrawable != null) {
            applyProgressBackgroundTint();
        }
    }

    /**
     * @return the blending mode used to apply the tint to the progress
     * background
     * @see #setProgressBackgroundTintBlendMode(BlendMode)
     */
    @Nullable
    public BlendMode getProgressBackgroundTintBlendMode() {
        return mProgressTintInfo != null ? mProgressTintInfo.mProgressBackgroundBlendMode : null;
    }

    /**
     * Applies a tint to the secondary progress indicator, if one exists.
     * Does not modify the current tint mode, which is
     * {@link BlendMode#SRC_ATOP} by default.
     * <p>
     * The secondary progress indicator must be specified as a layer with
     * id {@link R.id#secondaryProgress} in a {@link LayerDrawable}
     * used as the progress drawable.
     * <p>
     * Subsequent calls to {@link #setProgressDrawable(Drawable)} where the
     * drawable contains a secondary progress indicator will automatically
     * mutate the drawable and apply the specified tint and tint mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     * @see #getSecondaryProgressTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setSecondaryProgressTintList(@Nullable ColorStateList tint) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mSecondaryProgressTintList = tint;
        mProgressTintInfo.mHasSecondaryProgressTint = true;

        if (mProgressDrawable != null) {
            applySecondaryProgressTint();
        }
    }

    /**
     * Returns the tint applied to the secondary progress drawable, if
     * specified.
     *
     * @return the tint applied to the secondary progress drawable
     * @see #setSecondaryProgressTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getSecondaryProgressTintList() {
        return mProgressTintInfo != null ? mProgressTintInfo.mSecondaryProgressTintList : null;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setSecondaryProgressTintList(ColorStateList)}} to the secondary
     * progress indicator. The default mode is
     * {@link BlendMode#SRC_ATOP}.
     *
     * @param blendMode the blending mode used to apply the tint, may be
     *                  {@code null} to clear tint
     * @see #setSecondaryProgressTintList(ColorStateList)
     * @see Drawable#setTintBlendMode(BlendMode)
     */
    public void setSecondaryProgressTintBlendMode(@Nullable BlendMode blendMode) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mSecondaryProgressBlendMode = blendMode;
        mProgressTintInfo.mHasSecondaryProgressTintMode = true;

        if (mProgressDrawable != null) {
            applySecondaryProgressTint();
        }
    }

    /**
     * Returns the blending mode used to apply the tint to the secondary
     * progress drawable, if specified.
     *
     * @return the blending mode used to apply the tint to the secondary
     * progress drawable
     * @see #setSecondaryProgressTintBlendMode(BlendMode)
     */
    @Nullable
    public BlendMode getSecondaryProgressTintBlendMode() {
        return mProgressTintInfo != null ? mProgressTintInfo.mSecondaryProgressBlendMode : null;
    }

    /**
     * Returns the drawable to which a tint or tint mode should be applied.
     *
     * @param layerId        id of the layer to modify
     * @param shouldFallback whether the base drawable should be returned
     *                       if the id does not exist
     * @return the drawable to modify
     */
    @Nullable
    private Drawable getTintTarget(int layerId, boolean shouldFallback) {
        Drawable layer = null;

        final Drawable d = mProgressDrawable;
        if (d != null) {
            mProgressDrawable = d.mutate();

            if (d instanceof LayerDrawable) {
                layer = ((LayerDrawable) d).findDrawableByLayerId(layerId);
            }

            if (shouldFallback && layer == null) {
                layer = d;
            }
        }

        return layer;
    }

    public boolean getMirrorForRtl() {
        return mMirrorForRtl;
    }

    public void setMirrorForRtl(boolean mirrorForRtl) {
        mMirrorForRtl = mirrorForRtl;
        invalidate();
    }

    /**
     * Sets the current progress to the specified value. Does not do anything
     * if the progress bar is in indeterminate mode.
     * <p>
     * This method will immediately update the visual position of the progress
     * indicator. To animate the visual position to the target value, use
     * {@link #setProgress(int, boolean)}}.
     *
     * @param progress the new progress, between {@link #getMin()} and {@link #getMax()}
     * @see #setIndeterminate(boolean)
     * @see #isIndeterminate()
     * @see #getProgress()
     * @see #incrementProgressBy(int)
     */
    public void setProgress(int progress) {
        setProgressInternal(progress, false, false);
    }

    /**
     * Sets the current progress to the specified value, optionally animating
     * the visual position between the current and target values.
     * <p>
     * Animation does not affect the result of {@link #getProgress()}, which
     * will return the target value immediately after this method is called.
     *
     * @param progress the new progress value, between {@link #getMin()} and {@link #getMax()}
     * @param animate  {@code true} to animate between the current and target
     *                 values or {@code false} to not animate
     */
    public void setProgress(int progress, boolean animate) {
        setProgressInternal(progress, false, animate);
    }

    synchronized boolean setProgressInternal(int progress, boolean fromUser, boolean animate) {
        if (mIndeterminate) {
            // Not applicable.
            return false;
        }

        progress = MathUtil.clamp(progress, mMin, mMax);

        if (progress == mProgress) {
            // No change from current.
            return false;
        }

        mProgress = progress;
        doRefreshProgress(R.id.progress, mProgress, fromUser, true, animate);
        return true;
    }

    /**
     * <p>
     * Set the current secondary progress to the specified value. Does not do
     * anything if the progress bar is in indeterminate mode.
     * </p>
     *
     * @param secondaryProgress the new secondary progress, between {@link #getMin()} and
     *                          {@link #getMax()}
     * @see #setIndeterminate(boolean)
     * @see #isIndeterminate()
     * @see #getSecondaryProgress()
     * @see #incrementSecondaryProgressBy(int)
     */
    public void setSecondaryProgress(int secondaryProgress) {
        if (mIndeterminate) {
            return;
        }

        secondaryProgress = MathUtil.clamp(secondaryProgress, mMin, mMax);

        if (secondaryProgress != mSecondaryProgress) {
            mSecondaryProgress = secondaryProgress;
            doRefreshProgress(R.id.secondaryProgress, mSecondaryProgress, false, true, false);
        }
    }

    /**
     * <p>Get the progress bar's current level of secondary progress. Return 0 when the
     * progress bar is in indeterminate mode.</p>
     *
     * @return the current secondary progress, between {@link #getMin()} and {@link #getMax()}
     * @see #setIndeterminate(boolean)
     * @see #isIndeterminate()
     * @see #setSecondaryProgress(int)
     * @see #setMax(int)
     * @see #getMax()
     */
    public int getSecondaryProgress() {
        return mIndeterminate ? 0 : mSecondaryProgress;
    }

    /**
     * <p>Get the progress bar's current level of progress. Return 0 when the
     * progress bar is in indeterminate mode.</p>
     *
     * @return the current progress, between {@link #getMin()} and {@link #getMax()}
     * @see #setIndeterminate(boolean)
     * @see #isIndeterminate()
     * @see #setProgress(int)
     * @see #setMax(int)
     * @see #getMax()
     */
    public int getProgress() {
        return mIndeterminate ? 0 : mProgress;
    }

    /**
     * <p>Increase the progress bar's progress by the specified amount.</p>
     *
     * @param diff the amount by which the progress must be increased
     * @see #setProgress(int)
     */
    public final void incrementProgressBy(int diff) {
        setProgress(mProgress + diff);
    }

    /**
     * <p>Increase the progress bar's secondary progress by the specified amount.</p>
     *
     * @param diff the amount by which the secondary progress must be increased
     * @see #setSecondaryProgress(int)
     */
    public final void incrementSecondaryProgressBy(int diff) {
        setSecondaryProgress(mSecondaryProgress + diff);
    }

    /**
     * <p>Return the lower limit of this progress bar's range.</p>
     *
     * @return a positive integer
     * @see #setMin(int)
     * @see #getProgress()
     * @see #getSecondaryProgress()
     */
    public int getMin() {
        return mMin;
    }

    /**
     * <p>Return the upper limit of this progress bar's range.</p>
     *
     * @return a positive integer
     * @see #setMax(int)
     * @see #getProgress()
     * @see #getSecondaryProgress()
     */
    public int getMax() {
        return mMax;
    }

    /**
     * <p>Set the lower range of the progress bar to <tt>min</tt>.</p>
     *
     * @param min the lower range of this progress bar
     * @see #getMin()
     * @see #setProgress(int)
     * @see #setSecondaryProgress(int)
     */
    public void setMin(int min) {
        if (min > mMax) {
            min = mMax;
        }
        if (min != mMin) {
            mMin = min;
            postInvalidate();

            if (mProgress < min) {
                mProgress = min;
            }
            doRefreshProgress(R.id.progress, mProgress, false, true, false);
        }
    }

    /**
     * <p>Set the upper range of the progress bar <tt>max</tt>.</p>
     *
     * @param max the upper range of this progress bar
     * @see #getMax()
     * @see #setProgress(int)
     * @see #setSecondaryProgress(int)
     */
    public void setMax(int max) {
        if (max < mMin) {
            max = mMin;
        }
        if (max != mMax) {
            mMax = max;
            postInvalidate();

            if (mProgress > max) {
                mProgress = max;
            }
            doRefreshProgress(R.id.progress, mProgress, false, true, false);
        }
    }

    /**
     * Sets the visual state of a progress indicator.
     *
     * @param id       the identifier of the progress indicator
     * @param progress the visual progress in the range [0..1]
     */
    private void setVisualProgress(int id, float progress) {
        mVisualProgress = progress;

        Drawable d = mCurrentDrawable;

        if (d instanceof LayerDrawable) {
            d = ((LayerDrawable) d).findDrawableByLayerId(id);
            if (d == null) {
                // If we can't find the requested layer, fall back to setting
                // the level of the entire drawable. This will break if
                // progress is set on multiple elements, but the theme-default
                // drawable will always have all layer IDs present.
                d = mCurrentDrawable;
            }
        }

        if (d != null) {
            final int level = (int) (progress * Drawable.MAX_LEVEL + 0.5f);
            d.setLevel(level);
        } else {
            invalidate();
        }

        onVisualProgressChanged(id, progress);
    }

    /**
     * Called when the visual state of a progress indicator changes.
     *
     * @param id       the identifier of the progress indicator
     * @param progress the visual progress in the range [0..1]
     */
    void onVisualProgressChanged(int id, float progress) {
    }

    private void doRefreshProgress(int id, int progress, boolean fromUser,
                                   boolean callBackToApp, boolean animate) {
        int range = mMax - mMin;
        final float scale = range > 0 ? (progress - mMin) / (float) range : 0;
        final boolean isPrimary = id == R.id.progress;

        if (isPrimary && animate) {
            final ObjectAnimator animator = ObjectAnimator.ofFloat(this, VISUAL_PROGRESS, scale);
            animator.setAutoCancel(true);
            animator.setDuration(80);
            animator.setInterpolator(TimeInterpolator.DECELERATE);
            animator.addListener(new AnimatorListener() {
                @Override
                public void onAnimationEnd(@NonNull Animator animation) {
                    mLastProgressAnimator = null;
                }
            });
            animator.start();
            mLastProgressAnimator = animator;
        } else {
            if (isPrimary && mLastProgressAnimator != null) {
                mLastProgressAnimator.cancel();
                mLastProgressAnimator = null;
            }
            setVisualProgress(id, scale);
        }

        if (isPrimary && callBackToApp) {
            onProgressRefresh(scale, fromUser, progress);
        }
    }

    void onProgressRefresh(float scale, boolean fromUser, int progress) {
    }

    void startAnimation() {
        if (getVisibility() != VISIBLE || getWindowVisibility() != VISIBLE) {
            return;
        }

        if (mIndeterminateDrawable instanceof Animatable) {
            mShouldStartAnimationDrawable = true;
        }
        postInvalidate();
    }

    void stopAnimation() {
        if (mIndeterminateDrawable instanceof Animatable) {
            ((Animatable) mIndeterminateDrawable).stop();
            mShouldStartAnimationDrawable = false;
        }
        postInvalidate();
    }

    private static final class ProgressTintInfo {
        ColorStateList mIndeterminateTintList;
        BlendMode mIndeterminateBlendMode;
        boolean mHasIndeterminateTint;
        boolean mHasIndeterminateTintMode;

        ColorStateList mProgressTintList;
        BlendMode mProgressBlendMode;
        boolean mHasProgressTint;
        boolean mHasProgressTintMode;

        ColorStateList mProgressBackgroundTintList;
        BlendMode mProgressBackgroundBlendMode;
        boolean mHasProgressBackgroundTint;
        boolean mHasProgressBackgroundTintMode;

        ColorStateList mSecondaryProgressTintList;
        BlendMode mSecondaryProgressBlendMode;
        boolean mHasSecondaryProgressTint;
        boolean mHasSecondaryProgressTintMode;
    }

    /**
     * Returns the drawable currently used to draw the progress bar. This will be
     * either {@link #getProgressDrawable()} or {@link #getIndeterminateDrawable()}
     * depending on whether the progress bar is in determinate or indeterminate mode.
     *
     * @return the drawable currently used to draw the progress bar
     */
    @Nullable
    public Drawable getCurrentDrawable() {
        return mCurrentDrawable;
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return who == mProgressDrawable || who == mIndeterminateDrawable
                || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mProgressDrawable != null) mProgressDrawable.jumpToCurrentState();
        if (mIndeterminateDrawable != null) mIndeterminateDrawable.jumpToCurrentState();
    }

    @Override
    public void onResolveDrawables(int layoutDirection) {
        final Drawable d = mCurrentDrawable;
        if (d != null) {
            d.setLayoutDirection(layoutDirection);
        }
        if (mIndeterminateDrawable != null) {
            mIndeterminateDrawable.setLayoutDirection(layoutDirection);
        }
        if (mProgressDrawable != null) {
            mProgressDrawable.setLayoutDirection(layoutDirection);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        drawTrack(canvas);
    }

    /**
     * Draws the progress bar track.
     */
    void drawTrack(Canvas canvas) {
        final Drawable d = mCurrentDrawable;
        if (d != null) {
            // Translate canvas so an indeterminate circular progress bar with padding
            // rotates properly in its animation
            final int saveCount = canvas.save();

            if (isLayoutRtl() && mMirrorForRtl) {
                canvas.translate(getWidth() - mPaddingRight, mPaddingTop);
                canvas.scale(-1.0f, 1.0f);
            } else {
                canvas.translate(mPaddingLeft, mPaddingTop);
            }

            d.draw(canvas);
            canvas.restoreToCount(saveCount);

            if (mShouldStartAnimationDrawable && d instanceof Animatable) {
                ((Animatable) d).start();
                mShouldStartAnimationDrawable = false;
            }
        }
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);

        if (isVisible != mAggregatedIsVisible) {
            mAggregatedIsVisible = isVisible;

            if (mIndeterminate) {
                // let's be nice with the UI thread
                if (isVisible) {
                    startAnimation();
                } else {
                    stopAnimation();
                }
            }

            if (mCurrentDrawable != null) {
                mCurrentDrawable.setVisible(isVisible, false);
            }
        }
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable dr) {
        // fixed by Modern UI: workaround dirty rect
        if (super.verifyDrawable(dr)) {
            super.invalidateDrawable(dr);
        } else if (verifyDrawable(dr)) {
            // invalidate full view due to translate padding, thumb offset and possible RTL
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateDrawableBounds(w, h);
    }

    private void updateDrawableBounds(int w, int h) {
        // onDraw will translate the canvas so we draw starting at 0,0.
        // Subtract out padding for the purposes of the calculations below.
        w -= mPaddingRight + mPaddingLeft;
        h -= mPaddingTop + mPaddingBottom;

        int right = w;
        int bottom = h;
        int top = 0;
        int left = 0;

        if (mIndeterminateDrawable != null) {
            mIndeterminateDrawable.setBounds(left, top, right, bottom);
        }

        if (mProgressDrawable != null) {
            mProgressDrawable.setBounds(0, 0, right, bottom);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateDrawableState();
    }

    private void updateDrawableState() {
        final int[] state = getDrawableState();
        boolean changed = false;

        final Drawable progressDrawable = mProgressDrawable;
        if (progressDrawable != null && progressDrawable.isStateful()) {
            changed |= progressDrawable.setState(state);
        }

        final Drawable indeterminateDrawable = mIndeterminateDrawable;
        if (indeterminateDrawable != null && indeterminateDrawable.isStateful()) {
            changed |= indeterminateDrawable.setState(state);
        }

        if (changed) {
            invalidate();
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);

        if (mProgressDrawable != null) {
            mProgressDrawable.setHotspot(x, y);
        }

        if (mIndeterminateDrawable != null) {
            mIndeterminateDrawable.setHotspot(x, y);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mIndeterminate) {
            startAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mIndeterminate) {
            stopAnimation();
        }
        // This should come after stopAnimation(), otherwise an invalidate message remains in the
        // queue, which can prevent the entire view hierarchy from being GC'ed during a rotation
        super.onDetachedFromWindow();
    }
}
