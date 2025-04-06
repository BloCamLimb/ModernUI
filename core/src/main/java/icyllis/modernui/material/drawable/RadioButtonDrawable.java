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
 */

package icyllis.modernui.material.drawable;

import icyllis.modernui.R;
import icyllis.modernui.animation.Animator;
import icyllis.modernui.animation.AnimatorSet;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.animation.ValueAnimator;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.material.MaterialDrawable;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.resources.TypedValue;
import org.jetbrains.annotations.ApiStatus;

/**
 * A button drawable for RadioButton, similar to Material Design.
 *
 * @hidden
 */
@ApiStatus.Internal
public class RadioButtonDrawable extends MaterialDrawable {

    private static final float SIZE = 24;
    private final int mSize; // 24dp
    private boolean mAnimated;
    private boolean mHasStateLayer;
    private boolean mHasOuterLayer;

    private float mRingOuterScale = 1.0f;
    private float mRingOuterStrokeWidth = 2.0f;
    private float mDotGroupScale = 1.0f;

    // -1: init, 0: unchecked, 1: checked
    private int mCurState = -1;
    private int mTransitionToState = -1;

    // current transition, if any
    private Animator mTransition;

    public RadioButtonDrawable(Resources res, boolean animated, boolean hasStateLayer, boolean hasOuterLayer) {
        mSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DP, SIZE, res.getDisplayMetrics()));
        mAnimated = animated;
        mHasStateLayer = hasStateLayer;
        mHasOuterLayer = hasOuterLayer;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Paint paint = Paint.obtain();
        paint.setColor(mColor);
        paint.setAlpha(ShapeDrawable.modulateAlpha(paint.getAlpha(), mAlpha));
        if (paint.getAlpha() != 0) {
            final Rect r = getBounds();
            canvas.save();
            canvas.translate(r.exactCenterX(), r.exactCenterY());

            boolean doSave;
            if (mHasOuterLayer) {
                doSave = mRingOuterScale != 1;
                if (doSave) {
                    canvas.save();
                    canvas.scale(mRingOuterScale, mRingOuterScale);
                }
                paint.setStyle(Paint.STROKE);
                paint.setStrokeWidth(mRingOuterStrokeWidth * mSize * (1 / SIZE));
                // radius 9dp
                canvas.drawCircle(0, 0, mSize * (9 / SIZE), paint);
                if (doSave) {
                    canvas.restore();
                }
            }

            if (mDotGroupScale > 0) {
                doSave = mDotGroupScale != 1;
                if (doSave) {
                    canvas.save();
                    canvas.scale(mDotGroupScale, mDotGroupScale);
                }
                paint.setStyle(Paint.FILL);
                // radius 5dp
                canvas.drawCircle(0, 0, mSize * (5 / SIZE), paint);
                if (doSave) {
                    canvas.restore();
                }
            }

            canvas.restore();
        }
        paint.recycle();
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    protected boolean onStateChange(@NonNull int[] stateSet) {
        boolean changed = super.onStateChange(stateSet);

        int toState = 0;
        for (int state : stateSet) {
            if (state == R.attr.state_checked) {
                toState = 1;
                break;
            }
        }
        changed |= (mAnimated && selectTransition(toState)) || selectDrawable(toState);

        return changed;
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);

        if (mTransition != null && (changed || restart)) {
            if (visible) {
                mTransition.start();
            } else {
                jumpToCurrentState();
            }
        }

        return changed;
    }

    private boolean selectTransition(int toState) {
        int fromState;
        if (mTransition != null) {
            if (toState == mTransitionToState) {
                return true;
            }
            mTransition.cancel();

            fromState = mTransitionToState;
        } else {
            fromState = mCurState;
        }

        mTransition = null;
        mTransitionToState = -1;

        if (fromState == -1) {
            return false;
        }
        if (fromState == toState) {
            return false;
        }

        Animator transition;
        if (toState == 1) {
            transition = createOffToOnAnimator();
        } else {
            transition = createOnToOffAnimator();
        }

        transition.start();

        mTransition = transition;
        mTransitionToState = toState;
        return true;
    }

    private boolean selectDrawable(int toState) {
        if (mCurState == toState) {
            return false;
        }

        mCurState = toState;

        if (toState == 1) {
            mDotGroupScale = 1.0f;
        } else {
            mDotGroupScale = 0.0f;
        }

        invalidateSelf();

        return true;
    }

    @Override
    public void jumpToCurrentState() {
        super.jumpToCurrentState();

        if (mTransition != null) {
            mTransition.cancel();
            mTransition = null;

            selectDrawable(mTransitionToState);
        }
    }

    private Animator createOffToOnAnimator() {
        ValueAnimator step1 = new ValueAnimator();
        step1.setDuration(166);
        step1.setInterpolator(TimeInterpolator.FAST_OUT_SLOW_IN);
        step1.addUpdateListener(animation -> {
            mRingOuterScale = MathUtil.lerp(1.0f, 0.5f, animation.getAnimatedFraction());
            mRingOuterStrokeWidth = MathUtil.lerp(2.0f, 16.0f, animation.getAnimatedFraction());
            mDotGroupScale = 0.0f;
            invalidateSelf();
        });

        ValueAnimator step2 = new ValueAnimator();
        step2.setDuration(16);
        step2.setInterpolator(TimeInterpolator.FAST_OUT_SLOW_IN);
        step2.addUpdateListener(animation -> {
            mRingOuterScale = MathUtil.lerp(0.5f, 0.9f, animation.getAnimatedFraction());
            mRingOuterStrokeWidth = MathUtil.lerp(16.0f, 2.0f, animation.getAnimatedFraction());
            mDotGroupScale = MathUtil.lerp(0.0f, 1.5f, animation.getAnimatedFraction());
            invalidateSelf();
        });

        ValueAnimator step3 = new ValueAnimator();
        step3.setDuration(316);
        step3.setInterpolator(TimeInterpolator.FAST_OUT_SLOW_IN);
        step3.addUpdateListener(animation -> {
            mRingOuterScale = MathUtil.lerp(0.9f, 1.0f, animation.getAnimatedFraction());
            mRingOuterStrokeWidth = 2.0f;
            mDotGroupScale = MathUtil.lerp(1.5f, 1.0f, animation.getAnimatedFraction());
            invalidateSelf();
        });

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(step1, step2, step3);
        return set;
    }

    private Animator createOnToOffAnimator() {
        ValueAnimator step1 = new ValueAnimator();
        step1.setDuration(183);
        step1.setInterpolator(TimeInterpolator.FAST_OUT_SLOW_IN);
        step1.addUpdateListener(animation -> {
            mRingOuterScale = MathUtil.lerp(1.0f, 0.9f, animation.getAnimatedFraction());
            mRingOuterStrokeWidth = 2.0f;
            mDotGroupScale = MathUtil.lerp(1.0f, 1.4f, animation.getAnimatedFraction());
            invalidateSelf();
        });

        ValueAnimator step2 = new ValueAnimator();
        step2.setDuration(16);
        step2.setInterpolator(TimeInterpolator.FAST_OUT_SLOW_IN);
        step2.addUpdateListener(animation -> {
            mRingOuterScale = MathUtil.lerp(0.9f, 0.5f, animation.getAnimatedFraction());
            mRingOuterStrokeWidth = MathUtil.lerp(2.0f, 16.0f, animation.getAnimatedFraction());
            mDotGroupScale = MathUtil.lerp(1.4f, 0.0f, animation.getAnimatedFraction());
            invalidateSelf();
        });

        ValueAnimator step3 = new ValueAnimator();
        step3.setDuration(300);
        step3.setInterpolator(TimeInterpolator.FAST_OUT_SLOW_IN);
        step3.addUpdateListener(animation -> {
            mRingOuterScale = MathUtil.lerp(0.5f, 1.0f, animation.getAnimatedFraction());
            mRingOuterStrokeWidth = MathUtil.lerp(16.0f, 2.0f, animation.getAnimatedFraction());
            mDotGroupScale = 0.0f;
            invalidateSelf();
        });

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(step1, step2, step3);
        return set;
    }

    @Override
    public int getIntrinsicWidth() {
        if (mHasStateLayer) {
            return mSize * 3 / 2;
        }
        return mSize;
    }

    @Override
    public int getIntrinsicHeight() {
        if (mHasStateLayer) {
            return mSize * 3 / 2;
        }
        return mSize;
    }
}
