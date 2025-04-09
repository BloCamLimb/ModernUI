/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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
 *   Copyright (C) 2014 The Android Open Source Project
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
import icyllis.modernui.animation.ObjectAnimator;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.FloatRange;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.annotation.StyleableRes;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.BlendMode;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.TypedArray;
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.util.AttributeSet;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.FloatProperty;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.SoundEffectConstants;
import icyllis.modernui.view.ViewConfiguration;

/**
 * A Switch is a two-state toggle widget. Users can drag the switch "thumb" back
 * and forth to select either of two options or simply tap the switch to toggle
 * between options.
 *
 * @since 3.12
 */
public class Switch extends Button implements Checkable2 {
    private static final int THUMB_ANIMATION_DURATION = 250;

    private static final int TOUCH_MODE_IDLE = 0;
    private static final int TOUCH_MODE_DOWN = 1;
    private static final int TOUCH_MODE_DRAGGING = 2;

    private boolean mChecked;
    private boolean mBroadcasting;

    private Drawable mThumbDrawable;
    private ColorStateList mThumbTintList;
    private BlendMode mThumbBlendMode;
    private boolean mHasThumbTint;
    private boolean mHasThumbTintMode;

    private Drawable mTrackDrawable;
    private ColorStateList mTrackTintList;
    private BlendMode mTrackBlendMode;
    private boolean mHasTrackTint;
    private boolean mHasTrackTintMode;

    private OnCheckedChangeListener mOnCheckedChangeListener;

    private int mSwitchMinWidth;
    private int mSwitchPadding;
    private boolean mSplitTrack;

    private int mTouchMode;
    private int mTouchSlop;
    private float mTouchX;
    private float mTouchY;

    private float mThumbPosition;

    /**
     * Width required to draw the switch track and thumb. Includes padding and
     * optical bounds for both the track and thumb.
     */
    private int mSwitchWidth;

    /**
     * Height required to draw the switch track and thumb. Includes padding and
     * optical bounds for both the track and thumb.
     */
    private int mSwitchHeight;

    /**
     * Width of the thumb's content region. Does not include padding or
     * optical bounds.
     */
    private int mThumbWidth;

    /**
     * Left bound for drawing the switch track and thumb.
     */
    private int mSwitchLeft;

    /**
     * Top bound for drawing the switch track and thumb.
     */
    private int mSwitchTop;

    /**
     * Right bound for drawing the switch track and thumb.
     */
    private int mSwitchRight;

    /**
     * Bottom bound for drawing the switch track and thumb.
     */
    private int mSwitchBottom;

    private ObjectAnimator mPositionAnimator;

    private final Rect mTempRect = new Rect();

    private CharSequence mTextOn;
    private CharSequence mTextOff;

    @StyleableRes
    private static final String[] STYLEABLE = {
            R.ns, R.attr.checked,
            R.ns, R.attr.switchMinWidth,
            R.ns, R.attr.switchPadding,
            R.ns, R.attr.thumb,
            R.ns, R.attr.track,
    };

    @AttrRes
    private static final ResourceId DEF_STYLE_ATTR =
            ResourceId.attr(R.ns, R.attr.switchStyle);

    /**
     * Construct a new Switch with default styling.
     *
     * @param context The Context that will determine this widget's theming.
     */
    public Switch(Context context) {
        this(context, null);
    }

    /**
     * Construct a new Switch with default styling, overriding specific style
     * attributes as requested.
     *
     * @param context The Context that will determine this widget's theming.
     * @param attrs   Specification of attributes that should deviate from default styling.
     */
    public Switch(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, DEF_STYLE_ATTR);
    }

    /**
     * Construct a new Switch with a default style determined by the given theme attribute,
     * overriding specific style attributes as requested.
     *
     * @param context      The Context that will determine this widget's theming.
     * @param attrs        Specification of attributes that should deviate from the default styling.
     * @param defStyleAttr An attribute in the current theme that contains a
     *                     reference to a style resource that supplies default values for
     *                     the view. Can be null to not look for defaults.
     */
    public Switch(Context context, @Nullable AttributeSet attrs,
                  @Nullable @AttrRes ResourceId defStyleAttr) {
        this(context, attrs, defStyleAttr, null);
    }

    /**
     * Construct a new Switch with a default style determined by the given theme
     * attribute or style resource, overriding specific style attributes as
     * requested.
     *
     * @param context      The Context that will determine this widget's theming.
     * @param attrs        Specification of attributes that should deviate from the
     *                     default styling.
     * @param defStyleAttr An attribute in the current theme that contains a
     *                     reference to a style resource that supplies default values for
     *                     the view. Can be null to not look for defaults.
     * @param defStyleRes  A resource identifier of a style resource that
     *                     supplies default values for the view, used only if
     *                     defStyleAttr is 0 or can not be found in the theme. Can be null
     *                     to not look for defaults.
     */
    public Switch(Context context, @Nullable AttributeSet attrs,
                  @Nullable @AttrRes ResourceId defStyleAttr,
                  @Nullable @StyleRes ResourceId defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        final TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, defStyleAttr, defStyleRes, STYLEABLE);
        mThumbDrawable = a.getDrawable(3);
        if (mThumbDrawable != null) {
            mThumbDrawable.setCallback(this);
        }
        mTrackDrawable = a.getDrawable(4);
        if (mTrackDrawable != null) {
            mTrackDrawable.setCallback(this);
        }
        mSwitchMinWidth = a.getDimensionPixelSize(
                1, 0);
        mSwitchPadding = a.getDimensionPixelSize(
                2, 0);
        setChecked(a.getBoolean(0, false));
        a.recycle();

        final ViewConfiguration config = ViewConfiguration.get(context);
        mTouchSlop = config.getScaledTouchSlop();
    }

    /**
     * Set the amount of horizontal padding between the switch and the associated text.
     *
     * @param pixels Amount of padding in pixels
     */
    public void setSwitchPadding(int pixels) {
        mSwitchPadding = pixels;
        requestLayout();
    }

    /**
     * Get the amount of horizontal padding between the switch and the associated text.
     *
     * @return Amount of padding in pixels
     */
    public int getSwitchPadding() {
        return mSwitchPadding;
    }

    /**
     * Set the minimum width of the switch in pixels. The switch's width will be the maximum
     * of this value and its measured width as determined by the switch drawables and text used.
     *
     * @param pixels Minimum width of the switch in pixels
     */
    public void setSwitchMinWidth(int pixels) {
        mSwitchMinWidth = pixels;
        requestLayout();
    }

    /**
     * Get the minimum width of the switch in pixels. The switch's width will be the maximum
     * of this value and its measured width as determined by the switch drawables and text used.
     *
     * @return Minimum width of the switch in pixels
     */
    public int getSwitchMinWidth() {
        return mSwitchMinWidth;
    }

    /**
     * Set the drawable used for the track that the switch slides within.
     *
     * @param track Track drawable
     */
    public void setTrackDrawable(Drawable track) {
        if (mTrackDrawable != null) {
            mTrackDrawable.setCallback(null);
        }
        mTrackDrawable = track;
        if (track != null) {
            track.setCallback(this);
        }
        requestLayout();
    }

    /**
     * Get the drawable used for the track that the switch slides within.
     *
     * @return Track drawable
     */
    public Drawable getTrackDrawable() {
        return mTrackDrawable;
    }

    /**
     * Applies a tint to the track drawable. Does not modify the current
     * tint mode, which is {@link BlendMode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link #setTrackDrawable(Drawable)} will
     * automatically mutate the drawable and apply the specified tint and tint
     * mode using {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     * @see #getTrackTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setTrackTintList(@Nullable ColorStateList tint) {
        mTrackTintList = tint;
        mHasTrackTint = true;

        applyTrackTint();
    }

    /**
     * @return the tint applied to the track drawable
     * @see #setTrackTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getTrackTintList() {
        return mTrackTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setTrackTintList(ColorStateList)}} to the track drawable.
     * The default mode is {@link BlendMode#SRC_IN}.
     *
     * @param blendMode the blending mode used to apply the tint, may be
     *                  {@code null} to clear tint
     * @see #getTrackTintBlendMode()
     * @see Drawable#setTintBlendMode(BlendMode)
     */
    public void setTrackTintBlendMode(@Nullable BlendMode blendMode) {
        mTrackBlendMode = blendMode;
        mHasTrackTintMode = true;

        applyTrackTint();
    }

    /**
     * @return the blending mode used to apply the tint to the track
     * drawable
     * @see #setTrackTintBlendMode(BlendMode)
     */
    @Nullable
    public BlendMode getTrackTintBlendMode() {
        return mTrackBlendMode;
    }

    private void applyTrackTint() {
        if (mTrackDrawable != null && (mHasTrackTint || mHasTrackTintMode)) {
            mTrackDrawable = mTrackDrawable.mutate();

            if (mHasTrackTint) {
                mTrackDrawable.setTintList(mTrackTintList);
            }

            if (mHasTrackTintMode) {
                mTrackDrawable.setTintBlendMode(mTrackBlendMode);
            }

            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (mTrackDrawable.isStateful()) {
                mTrackDrawable.setState(getDrawableState());
            }
        }
    }

    /**
     * Set the drawable used for the switch "thumb" - the piece that the user
     * can physically touch and drag along the track.
     *
     * @param thumb Thumb drawable
     */
    public void setThumbDrawable(Drawable thumb) {
        if (mThumbDrawable != null) {
            mThumbDrawable.setCallback(null);
        }
        mThumbDrawable = thumb;
        if (thumb != null) {
            thumb.setCallback(this);
        }
        requestLayout();
    }

    /**
     * Get the drawable used for the switch "thumb" - the piece that the user
     * can physically touch and drag along the track.
     *
     * @return Thumb drawable
     */
    public Drawable getThumbDrawable() {
        return mThumbDrawable;
    }

    /**
     * Applies a tint to the thumb drawable. Does not modify the current
     * tint mode, which is {@link BlendMode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link #setThumbDrawable(Drawable)} will
     * automatically mutate the drawable and apply the specified tint and tint
     * mode using {@link Drawable#setTintList(ColorStateList)}.
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
     * @return the tint applied to the thumb drawable
     * @see #setThumbTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getThumbTintList() {
        return mThumbTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setThumbTintList(ColorStateList)}} to the thumb drawable.
     * The default mode is {@link BlendMode#SRC_IN}.
     *
     * @param blendMode the blending mode used to apply the tint, may be
     *                  {@code null} to clear tint
     * @see #getThumbTintBlendMode()
     * @see Drawable#setTintBlendMode(BlendMode)
     */
    public void setThumbTintBlendMode(@Nullable BlendMode blendMode) {
        mThumbBlendMode = blendMode;
        mHasThumbTintMode = true;

        applyThumbTint();
    }

    /**
     * @return the blending mode used to apply the tint to the thumb
     * drawable
     * @see #setThumbTintBlendMode(BlendMode)
     */
    @Nullable
    public BlendMode getThumbTintBlendMode() {
        return mThumbBlendMode;
    }

    private void applyThumbTint() {
        if (mThumbDrawable != null && (mHasThumbTint || mHasThumbTintMode)) {
            mThumbDrawable = mThumbDrawable.mutate();

            if (mHasThumbTint) {
                mThumbDrawable.setTintList(mThumbTintList);
            }

            if (mHasThumbTintMode) {
                mThumbDrawable.setTintBlendMode(mThumbBlendMode);
            }

            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (mThumbDrawable.isStateful()) {
                mThumbDrawable.setState(getDrawableState());
            }
        }
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
     * Returns the text for when the button is in the checked state.
     *
     * @return The text.
     */
    @Nullable
    public CharSequence getTextOn() {
        return mTextOn;
    }

    /**
     * Sets the text for when the button is in the checked state.
     * If not null, it overrides the text previously set by {@link Button#setText}.
     *
     * @param textOn The text.
     */
    public void setTextOn(@Nullable CharSequence textOn) {
        mTextOn = textOn;
        if (isChecked() && mTextOn != null) {
            setText(mTextOn);
        }
    }

    /**
     * Returns the text for when the button is not in the checked state.
     *
     * @return The text.
     */
    @Nullable
    public CharSequence getTextOff() {
        return mTextOff;
    }

    /**
     * Sets the text for when the button is not in the checked state.
     * If not null, it overrides the text previously set by {@link Button#setText}.
     *
     * @param textOff The text.
     */
    public void setTextOff(@Nullable CharSequence textOff) {
        mTextOff = textOff;
        if (!isChecked() && mTextOff != null) {
            setText(mTextOff);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final Rect padding = mTempRect;
        final int thumbWidth;
        final int thumbHeight;
        if (mThumbDrawable != null) {
            // Cached thumb width does not include padding.
            mThumbDrawable.getPadding(padding);
            thumbWidth = mThumbDrawable.getIntrinsicWidth() - padding.left - padding.right;
            thumbHeight = mThumbDrawable.getIntrinsicHeight();
        } else {
            thumbWidth = 0;
            thumbHeight = 0;
        }

        mThumbWidth = Math.max(0, thumbWidth);

        final int trackHeight;
        if (mTrackDrawable != null) {
            mTrackDrawable.getPadding(padding);
            trackHeight = mTrackDrawable.getIntrinsicHeight();
        } else {
            padding.setEmpty();
            trackHeight = 0;
        }

        // Adjust left and right padding to ensure there's enough room for the
        // thumb's padding (when present).
        int paddingLeft = padding.left;
        int paddingRight = padding.right;
        if (mThumbDrawable != null) {
            //TODO optical insets
            /*final Insets inset = mThumbDrawable.getOpticalInsets();
            paddingLeft = Math.max(paddingLeft, inset.left);
            paddingRight = Math.max(paddingRight, inset.right);*/
        }

        // Modern UI changed: not double the mThumbWidth
        final int switchWidth = Math.max(mSwitchMinWidth,
                mThumbWidth + paddingLeft + paddingRight);
        final int switchHeight = Math.max(trackHeight, thumbHeight);
        mSwitchWidth = switchWidth;
        mSwitchHeight = switchHeight;

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int measuredHeight = getMeasuredHeight();
        if (measuredHeight < switchHeight) {
            setMeasuredDimension(getMeasuredWidthAndState(), switchHeight);
        }
    }

    /**
     * @return true if (x, y) is within the target area of the switch thumb
     */
    private boolean hitThumb(float x, float y) {
        if (mThumbDrawable == null) {
            return false;
        }

        // Relies on mTempRect, MUST be called first!
        final int thumbOffset = getThumbOffset();

        mThumbDrawable.getPadding(mTempRect);
        final int thumbTop = mSwitchTop - mTouchSlop;
        final int thumbLeft = mSwitchLeft + thumbOffset - mTouchSlop;
        final int thumbRight = thumbLeft + mThumbWidth +
                mTempRect.left + mTempRect.right + mTouchSlop;
        final int thumbBottom = mSwitchBottom + mTouchSlop;
        return x > thumbLeft && x < thumbRight && y > thumbTop && y < thumbBottom;
    }

    //TODO drag the thumb

    private void animateThumbToCheckedState(boolean newCheckedState) {
        final float targetPosition = newCheckedState ? 1 : 0;
        mPositionAnimator = ObjectAnimator.ofFloat(this, THUMB_POS, targetPosition);
        mPositionAnimator.setDuration(THUMB_ANIMATION_DURATION);
        mPositionAnimator.setInterpolator(TimeInterpolator.FAST_OUT_SLOW_IN);
        mPositionAnimator.setAutoCancel(true);
        mPositionAnimator.start();
    }

    private void cancelPositionAnimator() {
        if (mPositionAnimator != null) {
            mPositionAnimator.cancel();
        }
    }

    private boolean getTargetCheckedState() {
        return mThumbPosition > 0.5f;
    }

    /**
     * @return the current thumb position as a decimal value between 0 (off) and 1 (on).
     */
    @FloatRange(from = 0.0, to = 1.0)
    protected final float getThumbPosition() {
        return mThumbPosition;
    }

    /**
     * Sets the thumb position as a decimal value between 0 (off) and 1 (on).
     *
     * @param position new position between [0,1]
     */
    private void setThumbPosition(float position) {
        mThumbPosition = position;
        invalidate();
    }

    @Override
    public void toggle() {
        setChecked(!isChecked());
    }

    @Override
    public boolean performClick() {
        toggle();

        final boolean handled = super.performClick();
        if (!handled) {
            // View only makes a sound effect if the onClickListener was
            // called, so we'll need to make one here instead.
            playSoundEffect(SoundEffectConstants.CLICK);
        }

        return handled;
    }

    @Override
    public final boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            refreshDrawableState();

            if (mBroadcasting) {
                return;
            }

            mBroadcasting = true;
            if (mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener.onCheckedChanged(this, mChecked);
            }

            mBroadcasting = false;

            // Calling the listener may result in setChecked() getting called
            // recursively with a different value, so load the REAL value...
            if (isAttachedToWindow() && isLaidOut()) {
                animateThumbToCheckedState(mChecked);
            } else {
                // Immediately move the thumb to the new position.
                cancelPositionAnimator();
                setThumbPosition(mChecked ? 1 : 0);
            }

            if (mChecked && mTextOn != null) {
                setText(mTextOn);
            } else if (!mChecked && mTextOff != null) {
                setText(mTextOff);
            }
        }
    }

    /**
     * Register a callback to be invoked when the checked state of this button
     * changes.
     *
     * @param listener the callback to call on checked state change
     */
    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int opticalInsetLeft = 0;
        int opticalInsetRight = 0;
        //TODO optical
        /*if (mThumbDrawable != null) {
            final Rect trackPadding = mTempRect;
            if (mTrackDrawable != null) {
                mTrackDrawable.getPadding(trackPadding);
            } else {
                trackPadding.setEmpty();
            }

            final Insets insets = mThumbDrawable.getOpticalInsets();
            opticalInsetLeft = Math.max(0, insets.left - trackPadding.left);
            opticalInsetRight = Math.max(0, insets.right - trackPadding.right);
        }*/

        final int switchRight;
        final int switchLeft;
        if (isLayoutRtl()) {
            switchLeft = getPaddingLeft() + opticalInsetLeft;
            switchRight = switchLeft + mSwitchWidth - opticalInsetLeft - opticalInsetRight;
        } else {
            switchRight = getWidth() - getPaddingRight() - opticalInsetRight;
            switchLeft = switchRight - mSwitchWidth + opticalInsetLeft + opticalInsetRight;
        }

        final int switchTop;
        final int switchBottom;
        switch (getGravity() & Gravity.VERTICAL_GRAVITY_MASK) {
            default:
            case Gravity.TOP:
                switchTop = getPaddingTop();
                switchBottom = switchTop + mSwitchHeight;
                break;

            case Gravity.CENTER_VERTICAL:
                switchTop = (getPaddingTop() + getHeight() - getPaddingBottom()) / 2 -
                        mSwitchHeight / 2;
                switchBottom = switchTop + mSwitchHeight;
                break;

            case Gravity.BOTTOM:
                switchBottom = getHeight() - getPaddingBottom();
                switchTop = switchBottom - mSwitchHeight;
                break;
        }

        mSwitchLeft = switchLeft;
        mSwitchTop = switchTop;
        mSwitchBottom = switchBottom;
        mSwitchRight = switchRight;
    }

    @Override
    public void draw(@NonNull Canvas c) {
        final Rect padding = mTempRect;
        final int switchLeft = mSwitchLeft;
        final int switchTop = mSwitchTop;
        final int switchRight = mSwitchRight;
        final int switchBottom = mSwitchBottom;

        int thumbInitialLeft = switchLeft + getThumbOffset();

        //TODO optical
        /*final Insets thumbInsets;
        if (mThumbDrawable != null) {
            thumbInsets = mThumbDrawable.getOpticalInsets();
        } else {
            thumbInsets = Insets.NONE;
        }*/

        // Layout the track.
        if (mTrackDrawable != null) {
            mTrackDrawable.getPadding(padding);

            // Adjust thumb position for track padding.
            thumbInitialLeft += padding.left;

            // If necessary, offset by the optical insets of the thumb asset.
            int trackLeft = switchLeft;
            int trackTop = switchTop;
            int trackRight = switchRight;
            int trackBottom = switchBottom;
            /*if (thumbInsets != Insets.NONE) {
                if (thumbInsets.left > padding.left) {
                    trackLeft += thumbInsets.left - padding.left;
                }
                if (thumbInsets.top > padding.top) {
                    trackTop += thumbInsets.top - padding.top;
                }
                if (thumbInsets.right > padding.right) {
                    trackRight -= thumbInsets.right - padding.right;
                }
                if (thumbInsets.bottom > padding.bottom) {
                    trackBottom -= thumbInsets.bottom - padding.bottom;
                }
            }*/
            mTrackDrawable.setBounds(trackLeft, trackTop, trackRight, trackBottom);
        }

        // Layout the thumb.
        if (mThumbDrawable != null) {
            mThumbDrawable.getPadding(padding);

            final int thumbLeft = thumbInitialLeft - padding.left;
            final int thumbRight = thumbInitialLeft + mThumbWidth + padding.right;
            mThumbDrawable.setBounds(thumbLeft, switchTop, thumbRight, switchBottom);

            final Drawable background = getBackground();
            if (background != null) {
                background.setHotspotBounds(thumbLeft, switchTop, thumbRight, switchBottom);
            }
        }

        // Draw the background.
        super.draw(c);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        final Rect padding = mTempRect;
        final Drawable trackDrawable = mTrackDrawable;
        if (trackDrawable != null) {
            trackDrawable.getPadding(padding);
        } else {
            padding.setEmpty();
        }

        final int switchTop = mSwitchTop;
        final int switchBottom = mSwitchBottom;
        final int switchInnerTop = switchTop + padding.top;
        final int switchInnerBottom = switchBottom - padding.bottom;

        final Drawable thumbDrawable = mThumbDrawable;
        if (trackDrawable != null) {
            //TODO
            /*if (mSplitTrack && thumbDrawable != null) {
                final Insets insets = thumbDrawable.getOpticalInsets();
                thumbDrawable.copyBounds(padding);
                padding.left += insets.left;
                padding.right -= insets.right;

                final int saveCount = canvas.save();
                canvas.clipRect(padding, Op.DIFFERENCE);
                trackDrawable.draw(canvas);
                canvas.restoreToCount(saveCount);
            } else {*/
            trackDrawable.draw(canvas);
            //}
        }

        final int saveCount = canvas.save();

        if (thumbDrawable != null) {
            thumbDrawable.draw(canvas);
        }

        canvas.restoreToCount(saveCount);
    }

    @Override
    public int getCompoundPaddingLeft() {
        if (!isLayoutRtl()) {
            return super.getCompoundPaddingLeft();
        }
        int padding = super.getCompoundPaddingLeft() + mSwitchWidth;
        if (!TextUtils.isEmpty(getText())) {
            padding += mSwitchPadding;
        }
        return padding;
    }

    @Override
    public int getCompoundPaddingRight() {
        if (isLayoutRtl()) {
            return super.getCompoundPaddingRight();
        }
        int padding = super.getCompoundPaddingRight() + mSwitchWidth;
        if (!TextUtils.isEmpty(getText())) {
            padding += mSwitchPadding;
        }
        return padding;
    }

    /**
     * Translates thumb position to offset according to current RTL setting and
     * thumb scroll range. Accounts for both track and thumb padding.
     *
     * @return thumb offset
     */
    private int getThumbOffset() {
        final float thumbPosition;
        if (isLayoutRtl()) {
            thumbPosition = 1 - mThumbPosition;
        } else {
            thumbPosition = mThumbPosition;
        }
        return (int) (thumbPosition * getThumbScrollRange() + 0.5f);
    }

    private int getThumbScrollRange() {
        if (mTrackDrawable != null) {
            final Rect padding = mTempRect;
            mTrackDrawable.getPadding(padding);

            /*final Insets insets;
            if (mThumbDrawable != null) {
                insets = mThumbDrawable.getOpticalInsets();
            } else {
                insets = Insets.NONE;
            }*/

            return mSwitchWidth - mThumbWidth - padding.left - padding.right
                    /*- insets.left - insets.right*/;
        } else {
            return 0;
        }
    }

    @NonNull
    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CompoundButton.CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        final int[] state = getDrawableState();
        boolean changed = false;

        final Drawable thumbDrawable = mThumbDrawable;
        if (thumbDrawable != null && thumbDrawable.isStateful()) {
            changed |= thumbDrawable.setState(state);
        }

        final Drawable trackDrawable = mTrackDrawable;
        if (trackDrawable != null && trackDrawable.isStateful()) {
            changed |= trackDrawable.setState(state);
        }

        if (changed) {
            invalidate();
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);

        if (mThumbDrawable != null) {
            mThumbDrawable.setHotspot(x, y);
        }

        if (mTrackDrawable != null) {
            mTrackDrawable.setHotspot(x, y);
        }
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return super.verifyDrawable(who) || who == mThumbDrawable || who == mTrackDrawable;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();

        if (mThumbDrawable != null) {
            mThumbDrawable.jumpToCurrentState();
        }

        if (mTrackDrawable != null) {
            mTrackDrawable.jumpToCurrentState();
        }

        if (mPositionAnimator != null && mPositionAnimator.isStarted()) {
            mPositionAnimator.end();
            mPositionAnimator = null;
        }
    }

    private static final FloatProperty<Switch> THUMB_POS = new FloatProperty<>("thumbPos") {
        @Override
        public Float get(Switch object) {
            return object.mThumbPosition;
        }

        @Override
        public void setValue(Switch object, float value) {
            object.setThumbPosition(value);
        }
    };
}
