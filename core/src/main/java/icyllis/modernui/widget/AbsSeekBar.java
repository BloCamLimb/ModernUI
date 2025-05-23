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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *   Copyright (C) 2007 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package icyllis.modernui.widget;

import icyllis.modernui.R;
import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.annotation.StyleableRes;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.BlendMode;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.TypedArray;
import icyllis.modernui.util.AttributeSet;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.ViewConfiguration;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.view.ViewParent;

// Modified from Android
public abstract class AbsSeekBar extends ProgressBar {

    private Drawable mThumb;
    private ColorStateList mThumbTintList = null;
    private BlendMode mThumbBlendMode = null;
    private boolean mHasThumbTint = false;
    private boolean mHasThumbBlendMode = false;

    private Drawable mTickMark;
    private ColorStateList mTickMarkTintList = null;
    private BlendMode mTickMarkBlendMode = null;
    private boolean mHasTickMarkTint = false;
    private boolean mHasTickMarkBlendMode = false;

    private int mThumbOffset;
    private boolean mSplitTrack;

    /**
     * On touch, this offset plus the scaled value from the position of the
     * touch will form the progress value. Usually 0.
     */
    float mTouchProgressOffset;

    /**
     * Whether this is user seekable.
     */
    boolean mIsUserSeekable = true;

    // Added by Modern UI
    boolean mIsUserAnimatable = false;

    /**
     * On key presses (right or left), the amount to increment/decrement the
     * progress.
     */
    private int mKeyProgressIncrement = 1;

    private int mThumbExclusionMaxSize;
    private int mScaledTouchSlop;
    private float mTouchDownX;
    private boolean mIsDragging;
    private float mTouchThumbOffset = 0.0f;

    private final Rect mTempRect = new Rect();
    private final Rect mThumbRect = new Rect();

    @StyleableRes
    private static final String[] STYLEABLE = {
            /*0*/R.ns, R.attr.splitTrack,
            /*1*/R.ns, R.attr.thumb,
            /*2*/R.ns, R.attr.thumbOffset,
            /*3*/R.ns, R.attr.tickMark,
    };

    public AbsSeekBar(Context context) {
        super(context);
    }

    public AbsSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, null);
    }

    public AbsSeekBar(Context context, @Nullable AttributeSet attrs,
                      @Nullable @AttrRes ResourceId defStyleAttr) {
        this(context, attrs, defStyleAttr, null);
    }

    public AbsSeekBar(Context context, @Nullable AttributeSet attrs,
                      @Nullable @AttrRes ResourceId defStyleAttr,
                      @Nullable @StyleRes ResourceId defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        final TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, defStyleAttr, defStyleRes, STYLEABLE);

        final Drawable thumb = a.getDrawable(1);
        setThumb(thumb);

        final Drawable tickMark = a.getDrawable(3);
        setTickMark(tickMark);

        mSplitTrack = a.getBoolean(0, false);

        // Guess thumb offset if thumb != null, but allow layout to override.
        final int thumbOffset = a.getDimensionPixelOffset(
                2, getThumbOffset());
        setThumbOffset(thumbOffset);

        a.recycle();

        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    /**
     * Sets the thumb that will be drawn at the end of the progress meter within the SeekBar.
     * <p>
     * If the thumb is a valid drawable (i.e. not null), half its width will be
     * used as the new thumb offset (@see #setThumbOffset(int)).
     *
     * @param thumb Drawable representing the thumb
     */
    public void setThumb(Drawable thumb) {
        final boolean needUpdate;
        // This way, calling setThumb again with the same bitmap will result in
        // it recalcuating mThumbOffset (if for example it the bounds of the
        // drawable changed)
        if (mThumb != null && thumb != mThumb) {
            mThumb.setCallback(null);
            needUpdate = true;
        } else {
            needUpdate = false;
        }

        if (thumb != null) {
            thumb.setCallback(this);
            if (canResolveLayoutDirection()) {
                thumb.setLayoutDirection(getLayoutDirection());
            }

            // Assuming the thumb drawable is symmetric, set the thumb offset
            // such that the thumb will hang halfway off either edge of the
            // progress bar.
            mThumbOffset = thumb.getIntrinsicWidth() / 2;

            // If we're updating get the new states
            if (needUpdate &&
                    (thumb.getIntrinsicWidth() != mThumb.getIntrinsicWidth()
                            || thumb.getIntrinsicHeight() != mThumb.getIntrinsicHeight())) {
                requestLayout();
            }
        }

        mThumb = thumb;

        applyThumbTint();
        invalidate();

        if (needUpdate) {
            updateThumbAndTrackPos(getWidth(), getHeight());
            if (thumb != null && thumb.isStateful()) {
                // Note that if the states are different this won't work.
                // For now, let's consider that an app bug.
                int[] state = getDrawableState();
                thumb.setState(state);
            }
        }
    }

    /**
     * Return the drawable used to represent the scroll thumb - the component that
     * the user can drag back and forth indicating the current value by its position.
     *
     * @return The current thumb drawable
     */
    public Drawable getThumb() {
        return mThumb;
    }

    /**
     * Applies a tint to the thumb drawable.
     * <p>
     * Subsequent calls to {@link #setThumb(Drawable)} will automatically
     * mutate the drawable and apply the specified tint and tint mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     * @see #getThumbTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setThumbTintList(@Nullable ColorStateList tint) {
        mThumbTintList = tint;
        mHasThumbTint = true;

        applyThumbTint();
    }

    /**
     * Returns the tint applied to the thumb drawable, if specified.
     *
     * @return the tint applied to the thumb drawable
     * @see #setThumbTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getThumbTintList() {
        return mThumbTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setThumbTintList(ColorStateList)}} to the thumb drawable. The
     * default mode is {@link BlendMode#SRC_IN}.
     *
     * @param blendMode the blending mode used to apply the tint, may be
     *                  {@code null} to clear tint
     * @see Drawable#setTintBlendMode(BlendMode)
     */
    public void setThumbTintBlendMode(@Nullable BlendMode blendMode) {
        mThumbBlendMode = blendMode;
        mHasThumbBlendMode = true;
        applyThumbTint();
    }

    /**
     * Returns the blending mode used to apply the tint to the thumb drawable,
     * if specified.
     *
     * @return the blending mode used to apply the tint to the thumb drawable
     * @see #setThumbTintBlendMode(BlendMode)
     */
    @Nullable
    public BlendMode getThumbTintBlendMode() {
        return mThumbBlendMode;
    }

    private void applyThumbTint() {
        if (mThumb != null && (mHasThumbTint || mHasThumbBlendMode)) {
            mThumb = mThumb.mutate();

            if (mHasThumbTint) {
                mThumb.setTintList(mThumbTintList);
            }

            if (mHasThumbBlendMode) {
                mThumb.setTintBlendMode(mThumbBlendMode);
            }

            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (mThumb.isStateful()) {
                mThumb.setState(getDrawableState());
            }
        }
    }

    /**
     * @see #setThumbOffset(int)
     */
    public int getThumbOffset() {
        return mThumbOffset;
    }

    /**
     * Sets the thumb offset that allows the thumb to extend out of the range of
     * the track.
     *
     * @param thumbOffset The offset amount in pixels.
     */
    public void setThumbOffset(int thumbOffset) {
        mThumbOffset = thumbOffset;
        invalidate();
    }

    /**
     * Specifies whether the track should be split by the thumb. When true,
     * the thumb's optical bounds will be clipped out of the track drawable,
     * then the thumb will be drawn into the resulting gap.
     *
     * @param splitTrack Whether the track should be split by the thumb
     */
    public void setSplitTrack(boolean splitTrack) {
        mSplitTrack = splitTrack;
        invalidate();
    }

    /**
     * Returns whether the track should be split by the thumb.
     */
    public boolean getSplitTrack() {
        return mSplitTrack;
    }

    /**
     * Sets the drawable displayed at each progress position, e.g. at each
     * possible thumb position.
     *
     * @param tickMark the drawable to display at each progress position
     */
    public void setTickMark(Drawable tickMark) {
        if (mTickMark != null) {
            mTickMark.setCallback(null);
        }

        mTickMark = tickMark;

        if (tickMark != null) {
            tickMark.setCallback(this);
            tickMark.setLayoutDirection(getLayoutDirection());
            if (tickMark.isStateful()) {
                tickMark.setState(getDrawableState());
            }
            applyTickMarkTint();
        }

        invalidate();
    }

    /**
     * @return the drawable displayed at each progress position
     */
    public Drawable getTickMark() {
        return mTickMark;
    }

    /**
     * Applies a tint to the tick mark drawable.
     * <p>
     * Subsequent calls to {@link #setTickMark(Drawable)} will automatically
     * mutate the drawable and apply the specified tint and tint mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     * @see #getTickMarkTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setTickMarkTintList(@Nullable ColorStateList tint) {
        mTickMarkTintList = tint;
        mHasTickMarkTint = true;

        applyTickMarkTint();
    }

    /**
     * Returns the tint applied to the tick mark drawable, if specified.
     *
     * @return the tint applied to the tick mark drawable
     * @see #setTickMarkTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getTickMarkTintList() {
        return mTickMarkTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setTickMarkTintList(ColorStateList)}} to the tick mark drawable. The
     * default mode is {@link BlendMode#SRC_IN}.
     *
     * @param blendMode the blending mode used to apply the tint, may be
     *                  {@code null} to clear tint
     * @see Drawable#setTintBlendMode(BlendMode)
     */
    public void setTickMarkTintBlendMode(@Nullable BlendMode blendMode) {
        mTickMarkBlendMode = blendMode;
        mHasTickMarkBlendMode = true;

        applyTickMarkTint();
    }

    /**
     * Returns the blending mode used to apply the tint to the tick mark drawable,
     * if specified.
     *
     * @return the blending mode used to apply the tint to the tick mark drawable
     */
    @Nullable
    public BlendMode getTickMarkTintBlendMode() {
        return mTickMarkBlendMode;
    }

    private void applyTickMarkTint() {
        if (mTickMark != null && (mHasTickMarkTint || mHasTickMarkBlendMode)) {
            mTickMark = mTickMark.mutate();

            if (mHasTickMarkTint) {
                mTickMark.setTintList(mTickMarkTintList);
            }

            if (mHasTickMarkBlendMode) {
                mTickMark.setTintBlendMode(mTickMarkBlendMode);
            }

            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (mTickMark.isStateful()) {
                mTickMark.setState(getDrawableState());
            }
        }
    }

    /**
     * Sets the amount of progress changed via the arrow keys.
     *
     * @param increment The amount to increment or decrement when the user
     *                  presses the arrow keys.
     */
    public void setKeyProgressIncrement(int increment) {
        mKeyProgressIncrement = Math.abs(increment);
    }

    /**
     * Returns the amount of progress changed via the arrow keys.
     * <p>
     * By default, this will be a value that is derived from the progress range.
     *
     * @return The amount to increment or decrement when the user presses the
     * arrow keys. This will be positive.
     */
    public int getKeyProgressIncrement() {
        return mKeyProgressIncrement;
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return who == mThumb || who == mTickMark || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();

        if (mThumb != null) {
            mThumb.jumpToCurrentState();
        }

        if (mTickMark != null) {
            mTickMark.jumpToCurrentState();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        final Drawable thumb = mThumb;
        if (thumb != null && thumb.isStateful()
                && thumb.setState(getDrawableState())) {
            invalidateDrawable(thumb);
        }

        final Drawable tickMark = mTickMark;
        if (tickMark != null && tickMark.isStateful()
                && tickMark.setState(getDrawableState())) {
            invalidateDrawable(tickMark);
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);

        if (mThumb != null) {
            mThumb.setHotspot(x, y);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        updateThumbAndTrackPos(w, h);
    }

    private void updateThumbAndTrackPos(int w, int h) {
        final int paddedHeight = h - mPaddingTop - mPaddingBottom;
        final Drawable track = getCurrentDrawable();
        final Drawable thumb = mThumb;

        // The max height does not incorporate padding, whereas the height
        // parameter does.
        final int trackHeight = Math.min(getMaximumHeight(), paddedHeight);
        final int thumbHeight = thumb == null ? 0 : thumb.getIntrinsicHeight();

        // Apply offset to whichever item is taller.
        final int trackOffset;
        final int thumbOffset;
        if (thumbHeight > trackHeight) {
            final int offsetHeight = (paddedHeight - thumbHeight) / 2;
            trackOffset = offsetHeight + (thumbHeight - trackHeight) / 2;
            thumbOffset = offsetHeight;
        } else {
            final int offsetHeight = (paddedHeight - trackHeight) / 2;
            trackOffset = offsetHeight;
            thumbOffset = offsetHeight + (trackHeight - thumbHeight) / 2;
        }

        if (track != null) {
            final int trackWidth = w - mPaddingRight - mPaddingLeft;
            track.setBounds(0, trackOffset, trackWidth, trackOffset + trackHeight);
        }

        if (thumb != null) {
            setThumbPos(w, thumb, getScale(), thumbOffset);
        }
    }

    private float getScale() {
        int min = getMin();
        int max = getMax();
        int range = max - min;
        return range > 0 ? (getProgress() - min) / (float) range : 0;
    }

    /**
     * Updates the thumb drawable bounds.
     *
     * @param w      Width of the view, including padding
     * @param thumb  Drawable used for the thumb
     * @param scale  Current progress between 0 and 1
     * @param offset Vertical offset for centering. If set to
     *               {@link Integer#MIN_VALUE}, the current offset will be used.
     */
    private void setThumbPos(int w, Drawable thumb, float scale, int offset) {
        int available = w - mPaddingLeft - mPaddingRight;
        final int thumbWidth = thumb.getIntrinsicWidth();
        final int thumbHeight = thumb.getIntrinsicHeight();
        available -= thumbWidth;

        // The extra space for the thumb to move on the track
        available += mThumbOffset * 2;

        final int thumbPos = (int) (scale * available + 0.5f);

        final int top, bottom;
        if (offset == Integer.MIN_VALUE) {
            final Rect oldBounds = thumb.getBounds();
            top = oldBounds.top;
            bottom = oldBounds.bottom;
        } else {
            top = offset;
            bottom = offset + thumbHeight;
        }

        final int left = (isLayoutRtl() && mMirrorForRtl) ? available - thumbPos : thumbPos;
        final int right = left + thumbWidth;

        final Drawable background = getBackground();
        if (background != null) {
            final int offsetX = mPaddingLeft - mThumbOffset;
            final int offsetY = mPaddingTop;
            background.setHotspotBounds(left + offsetX, top + offsetY,
                    right + offsetX, bottom + offsetY);
        }

        // Canvas will be translated, so 0,0 is where we start drawing
        thumb.setBounds(left, top, right, bottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable d = getCurrentDrawable();

        int thumbHeight = mThumb == null ? 0 : mThumb.getIntrinsicHeight();
        int dw = 0;
        int dh = 0;
        if (d != null) {
            dw = MathUtil.clamp(d.getIntrinsicWidth(), getMinimumWidth(), getMaximumWidth());
            dh = MathUtil.clamp(d.getIntrinsicHeight(), getMinimumHeight(), getMaximumHeight());
            dh = Math.max(thumbHeight, dh);
        }
        dw += mPaddingLeft + mPaddingRight;
        dh += mPaddingTop + mPaddingBottom;

        setMeasuredDimension(resolveSizeAndState(dw, widthMeasureSpec, 0),
                resolveSizeAndState(dh, heightMeasureSpec, 0));
    }

    @Override
    void onVisualProgressChanged(int id, float scale) {
        super.onVisualProgressChanged(id, scale);

        if (id == R.id.progress) {
            final Drawable thumb = mThumb;
            if (thumb != null) {
                setThumbPos(getWidth(), thumb, scale, Integer.MIN_VALUE);

                // Since we draw translated, the drawable's bounds that it signals
                // for invalidation won't be the actual bounds we want invalidated,
                // so just invalidate this whole view.
                invalidate();
            }
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        drawThumb(canvas);
    }

    @Override
    void drawTrack(Canvas canvas) {
        final Drawable thumbDrawable = mThumb;
        if (thumbDrawable != null && mSplitTrack) {
            //TODO
            //final Insets insets = thumbDrawable.getOpticalInsets();
            final Rect tempRect = mTempRect;
            thumbDrawable.copyBounds(tempRect);
            tempRect.offset(mPaddingLeft - mThumbOffset, mPaddingTop);
            // Modern UI changed: outset the rect instead of inset
            tempRect.left -= thumbDrawable.getIntrinsicWidth();
            tempRect.right += thumbDrawable.getIntrinsicWidth();

            final int saveCount = canvas.save();
            canvas.clipOutRect(tempRect);
            super.drawTrack(canvas);
            drawTickMarks(canvas);
            canvas.restoreToCount(saveCount);
        } else {
            super.drawTrack(canvas);
            drawTickMarks(canvas);
        }
    }

    protected void drawTickMarks(Canvas canvas) {
        if (mTickMark != null) {
            int count = getMax() - getMin();
            if (count <= 1) {
                return;
            }
            final int w = mTickMark.getIntrinsicWidth();
            final int h = mTickMark.getIntrinsicHeight();
            final int halfW = w >= 0 ? w / 2 : 1;
            final int halfH = h >= 0 ? h / 2 : 1;
            mTickMark.setBounds(-halfW, -halfH, halfW, halfH);
            if (halfW == 0 || halfH == 0) {
                return;
            }

            final int saveCount = canvas.save();

            // Modern UI changed: respect thumbOffset
            final Drawable thumb = mThumb;
            int available = getWidth() - mPaddingLeft - mPaddingRight;
            if (thumb != null) {
                int thumbWidth = thumb.getIntrinsicWidth();
                available -= thumbWidth;
                available += mThumbOffset * 2;
                int offset = mThumbOffset - thumbWidth / 2;
                canvas.translate(mPaddingLeft - offset, getHeight() / 2f);
            } else {
                canvas.translate(mPaddingLeft, getHeight() / 2f);
            }
            // Modern UI changed: cannot less than 1px per tick
            count = Math.min(count, available / w);
            if (count > 1) {
                final float spacing = available / (float) count;
                for (int i = 0; i <= count; i++) {
                    mTickMark.draw(canvas);
                    canvas.translate(spacing, 0);
                }
            }
            canvas.restoreToCount(saveCount);
        }
    }

    /**
     * Draw the thumb.
     */
    void drawThumb(Canvas canvas) {
        if (mThumb != null) {
            final int saveCount = canvas.save();
            // Translate the padding. For the x, we need to allow the thumb to
            // draw in its extra space
            canvas.translate(mPaddingLeft - mThumbOffset, mPaddingTop);
            mThumb.draw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    // Modern UI added
    private boolean isInVerticalScrollingContainer() {
        ViewParent p = getParent();
        while (p instanceof ViewGroup parent) {
            boolean canScrollVertically = parent.canScrollVertically(1) || parent.canScrollVertically(-1);
            if (canScrollVertically && parent.shouldDelayChildPressedState()) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (!mIsUserSeekable || !isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN -> {
                if (mThumb != null) {
                    final int availableWidth = getWidth() - mPaddingLeft - mPaddingRight;
                    mTouchThumbOffset = (getProgress() - getMin()) / (float) (getMax()
                            - getMin()) - (event.getX() - mPaddingLeft) / availableWidth;
                    if (Math.abs(mTouchThumbOffset * availableWidth) > getThumbOffset()) {
                        mTouchThumbOffset = 0;
                    }
                }
                if (isInVerticalScrollingContainer()) {
                    mTouchDownX = event.getX();
                } else {
                    startDrag(event);
                }
            }
            case MotionEvent.ACTION_MOVE -> {
                if (mIsDragging) {
                    trackTouchEvent(event);
                } else {
                    final float x = event.getX();
                    if (Math.abs(x - mTouchDownX) > mScaledTouchSlop) {
                        startDrag(event);
                    }
                }
            }
            case MotionEvent.ACTION_UP -> {
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    // Touch up when we never crossed the touch slop threshold should
                    // be interpreted as a tap-seek to that location.
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }
                // ProgressBar doesn't know to repaint the thumb drawable
                // in its inactive state when the touch stops (because the
                // value has not apparently changed)
                invalidate();
            }
            case MotionEvent.ACTION_CANCEL -> {
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
            }
        }
        return true;
    }

    private void startDrag(MotionEvent event) {
        setPressed(true);

        if (mThumb != null) {
            // This may be within the padding region.
            invalidate();
        }

        onStartTrackingTouch();
        trackTouchEvent(event);
        attemptClaimDrag();
    }

    private void setHotspot(float x, float y) {
        final Drawable bg = getBackground();
        if (bg != null) {
            bg.setHotspot(x, y);
        }
    }

    private void trackTouchEvent(MotionEvent event) {
        final int x = Math.round(event.getX());
        final int y = Math.round(event.getY());
        final int width = getWidth();
        final int availableWidth = width - mPaddingLeft - mPaddingRight;

        final float scale;
        float progress = 0.0f;
        if (isLayoutRtl() && mMirrorForRtl) {
            if (x > width - mPaddingRight) {
                scale = 0.0f;
            } else if (x < mPaddingLeft) {
                scale = 1.0f;
            } else {
                scale = (availableWidth - x + mPaddingLeft) / (float) availableWidth
                        + mTouchThumbOffset;
                progress = mTouchProgressOffset;
            }
        } else {
            if (x < mPaddingLeft) {
                scale = 0.0f;
            } else if (x > width - mPaddingRight) {
                scale = 1.0f;
            } else {
                scale = (x - mPaddingLeft) / (float) availableWidth + mTouchThumbOffset;
                progress = mTouchProgressOffset;
            }
        }

        final int range = getMax() - getMin();
        progress += scale * range + getMin();

        setHotspot(x, y);
        boolean animate;
        if (mIsUserAnimatable) {
            // Dragging on a continuous slider is not animated
            animate = mTickMark != null || event.getAction() != MotionEvent.ACTION_MOVE;
        } else {
            animate = false;
        }
        setProgressInternal(Math.round(progress), true, animate);
    }

    /**
     * Tries to claim the user's drag motion, and requests disallowing any
     * ancestors from stealing events in the drag.
     */
    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    /**
     * This is called when the user has started touching this widget.
     */
    void onStartTrackingTouch() {
        mIsDragging = true;
    }

    /**
     * This is called when the user either releases their touch or the touch is
     * canceled.
     */
    void onStopTrackingTouch() {
        mIsDragging = false;
    }

    /**
     * Called when the user changes the seekbar's progress by using a key event.
     */
    void onKeyChange() {
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (isEnabled()) {
            int increment = mKeyProgressIncrement;
            switch (keyCode) {
                case KeyEvent.KEY_LEFT:
                case KeyEvent.KEY_MINUS:
                case KeyEvent.KEY_KP_SUBTRACT:
                    increment = -increment;
                    // fallthrough
                case KeyEvent.KEY_RIGHT:
                case KeyEvent.KEY_EQUAL:
                case KeyEvent.KEY_KP_ADD:
                    increment = isLayoutRtl() ? -increment : increment;

                    if (setProgressInternal(getProgress() + increment, true, true)) {
                        onKeyChange();
                        return true;
                    }
                    break;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        final Drawable thumb = mThumb;
        if (thumb != null) {
            setThumbPos(getWidth(), thumb, getScale(), Integer.MIN_VALUE);

            // Since we draw translated, the drawable's bounds that it signals
            // for invalidation won't be the actual bounds we want invalidated,
            // so just invalidate this whole view.
            invalidate();
        }
    }
}
