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

package icyllis.modernui.widget;

import icyllis.modernui.animation.*;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.MathUtil;

import javax.annotation.Nonnull;

public class SwitchButton extends CompoundButton {

    private int mButtonLeft;
    private int mButtonTop;
    private int mButtonRight;
    private int mButtonBottom;

    private float mThumbPosition;

    private int mInsideColor;
    private float mInsideRadius;

    private int mCheckedColor;
    private int mUncheckedColor;

    private int mBorderWidth;

    private final Animator mAnimator;

    public SwitchButton() {
        mCheckedColor = 0xFF51D367;
        mUncheckedColor = 0xFFDDDDDD;
        mBorderWidth = 2;

        mInsideColor = mUncheckedColor;

        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(this);
        animator.setInterpolator(null);
        animator.addUpdateListener(this::onAnimationUpdate);

        mAnimator = animator;
    }

    private void onAnimationUpdate(@Nonnull ValueAnimator animator) {
        float fraction = animator.getAnimatedFraction();
        if (!isChecked()) {
            fraction = 1f - fraction;
            mThumbPosition = TimeInterpolator.ACCELERATE.getInterpolation(fraction);
        } else {
            mThumbPosition = TimeInterpolator.DECELERATE.getInterpolation(fraction);
        }
        mInsideRadius = (mButtonBottom - mButtonTop) * 0.5f * fraction;
        mInsideColor = ColorEvaluator.evaluate(fraction, mUncheckedColor, mCheckedColor);

        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int measuredWidth = getMeasuredWidth();
        int maxHeight = (int) (measuredWidth / 1.2f);
        if (getMeasuredHeight() > maxHeight) {
            setMeasuredDimension(measuredWidth, maxHeight);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int thickness = (int) Math.ceil(mBorderWidth / 2f);
        mButtonLeft = thickness;
        mButtonTop = thickness;
        mButtonRight = right - left - thickness;
        mButtonBottom = bottom - top - thickness;

        if (isChecked()) {
            mInsideRadius = (mButtonBottom - mButtonTop) * 0.5f;
        }
    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = Paint.get();

        float buttonRadius = (mButtonBottom - mButtonTop) * 0.5f;
        float thumbX = mButtonLeft + buttonRadius + getThumbOffset();
        float thumbY = mButtonTop + buttonRadius;

        // draw inside background
        paint.setColor(mInsideColor);
        if (MathUtil.isApproxEqual(mInsideRadius, buttonRadius)) {
            // check a final state and simplify the drawing
            paint.setStyle(Paint.FILL);
            canvas.drawRoundRect(mButtonLeft, mButtonTop, mButtonRight, mButtonBottom, buttonRadius, paint);
        } else if (mInsideRadius > 0) {
            float thickness = mInsideRadius * 0.5f;
            paint.setStyle(Paint.STROKE);
            paint.setStrokeWidth(mInsideRadius);
            canvas.drawRoundRect(mButtonLeft + thickness, mButtonTop + thickness,
                    mButtonRight - thickness, mButtonBottom - thickness,
                    buttonRadius - thickness, paint);
            // fill in unchecked blanks
            paint.setStyle(Paint.FILL);
            if (isLayoutRtl()) {
                canvas.drawCircle(mButtonRight - buttonRadius, thumbY,
                        buttonRadius - Math.max(thickness - 2f, 0), paint);
                canvas.drawRect(thumbX, mButtonTop + 2f,
                        mButtonRight - buttonRadius, mButtonBottom - 2f, paint);
            } else {
                canvas.drawCircle(mButtonLeft + buttonRadius, thumbY,
                        buttonRadius - Math.max(thickness - 2f, 0), paint);
                canvas.drawRect(mButtonLeft + buttonRadius,
                        mButtonTop + 2f, thumbX, mButtonBottom - 2f, paint);
            }
        }

        // draw border
        paint.setStyle(Paint.STROKE);
        paint.setStrokeWidth(mBorderWidth);
        paint.setColor(mUncheckedColor);
        canvas.drawRoundRect(mButtonLeft, mButtonTop, mButtonRight, mButtonBottom, buttonRadius, paint);

        // draw thumb
        paint.setStyle(Paint.FILL);
        paint.setColor(0xFFFFFFFF);
        canvas.drawCircle(thumbX, thumbY, buttonRadius, paint);

        paint.setStyle(Paint.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(0xFFDDDDDD);
        canvas.drawCircle(thumbX, thumbY, buttonRadius - 1f, paint);
    }

    @Override
    public void setChecked(boolean checked) {
        boolean oldChecked = isChecked();
        super.setChecked(checked);
        if (oldChecked != checked) {
            mAnimator.start();
            if (!isAttachedToWindow()) {
                mAnimator.end();
            }
        }
    }

    public void setCheckedColor(int checkedColor) {
        if (checkedColor != mCheckedColor) {
            mCheckedColor = checkedColor;
            if (isChecked()) {
                mInsideColor = checkedColor;
            }
            invalidate();
        }
    }

    public void setUncheckedColor(int uncheckedColor) {
        if (uncheckedColor != mUncheckedColor) {
            mUncheckedColor = uncheckedColor;
            if (!isChecked()) {
                mInsideColor = uncheckedColor;
            }
            invalidate();
        }
    }

    public void setBorderWidth(int borderWidth) {
        if (borderWidth != mBorderWidth) {
            mBorderWidth = borderWidth;
            requestLayout();
        }
    }

    private int getThumbOffset() {
        final float thumbPosition;
        if (isLayoutRtl()) {
            thumbPosition = 1f - mThumbPosition;
        } else {
            thumbPosition = mThumbPosition;
        }
        return (int) (thumbPosition * getThumbScrollRange() + 0.5f);
    }

    private int getThumbScrollRange() {
        return mButtonTop - mButtonBottom + mButtonRight - mButtonLeft;
    }
}
