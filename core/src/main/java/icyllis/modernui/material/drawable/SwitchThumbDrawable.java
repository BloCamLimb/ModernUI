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
import icyllis.modernui.animation.ObjectAnimator;
import icyllis.modernui.animation.PropertyValuesHolder;
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
import icyllis.modernui.util.FloatProperty;
import org.jetbrains.annotations.ApiStatus;

/**
 * A thumb drawable for Switch, similar to Material Design.
 */
@ApiStatus.Internal
public class SwitchThumbDrawable extends MaterialDrawable {

    private static final float SIZE = 32;
    private final int mSize; // 32dp
    private boolean mAnimated;
    private boolean mUsePressState;

    // the circle can sometimes be rounded rectangle
    private float mCircleWidth;
    private float mCircleHeight;

    // -1: init, 0: unchecked, 1: checked, 2: pressed
    private int mCurState = -1;
    private int mTransitionToState = -1;

    // current transition, if any
    private Animator mTransition;

    public SwitchThumbDrawable(Resources res, boolean animated, boolean usePressState) {
        mSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DP, SIZE, res.getDisplayMetrics()));
        mAnimated = animated;
        mUsePressState = usePressState;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Paint paint = Paint.obtain();
        paint.setColor(mColor);
        paint.setAlpha(ShapeDrawable.modulateAlpha(paint.getAlpha(), mAlpha));
        if (paint.getAlpha() != 0) {
            final Rect r = getBounds();
            float cx = r.exactCenterX();
            float cy = r.exactCenterY();
            float extentX = mCircleWidth * mSize * (0.5f / SIZE);
            float extentY = mCircleHeight * mSize * (0.5f / SIZE);
            float rad = Math.min(extentX, extentY);
            canvas.drawRoundRect(cx - extentX, cy - extentY, cx + extentX, cy + extentY, rad, paint);
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
            if (mUsePressState && state == R.attr.state_pressed) {
                toState = 2;
                break;
            }
            if (state == R.attr.state_checked) {
                toState = 1;
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
        if (fromState == 0) {
            if (toState == 1) {
                transition = createUncheckedToCheckedAnimator();
            } else if (toState == 2) {
                transition = createAnyToPressedAnimator();
            } else {
                return false;
            }
        } else if (fromState == 1) {
            if (toState == 2) {
                transition = createAnyToPressedAnimator();
            } else if (toState == 0) {
                transition = createCheckedToUncheckedAnimator();
            } else {
                return false;
            }
        } else if (fromState == 2) {
            if (toState == 1) {
                transition = createPressedToCheckedAnimator();
            } else {
                transition = createPressedToUncheckedAnimator();
            }
        } else {
            return false;
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

        if (toState == 2) {
            // pressed
            mCircleWidth = mCircleHeight = 28;
        } else if (toState == 1) {
            // checked
            mCircleWidth = mCircleHeight = 24;
        } else {
            // unchecked
            mCircleWidth = mCircleHeight = 16;
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

    private Animator createPressedToCheckedAnimator() {
        var pvh1 = PropertyValuesHolder.ofFloat(CIRCLE_WIDTH, 24);
        var pvh2 = PropertyValuesHolder.ofFloat(CIRCLE_HEIGHT, 24);
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(this, pvh1, pvh2);
        anim.setDuration(150);
        anim.setInterpolator(TimeInterpolator.MOTION_EASING_STANDARD);
        return anim;
    }

    private Animator createPressedToUncheckedAnimator() {
        var pvh1 = PropertyValuesHolder.ofFloat(CIRCLE_WIDTH, 16);
        var pvh2 = PropertyValuesHolder.ofFloat(CIRCLE_HEIGHT, 16);
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(this, pvh1, pvh2);
        anim.setDuration(150);
        anim.setInterpolator(TimeInterpolator.MOTION_EASING_STANDARD);
        return anim;
    }

    private Animator createAnyToPressedAnimator() {
        var pvh1 = PropertyValuesHolder.ofFloat(CIRCLE_WIDTH, 28);
        var pvh2 = PropertyValuesHolder.ofFloat(CIRCLE_HEIGHT, 28);
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(this, pvh1, pvh2);
        anim.setDuration(150);
        anim.setInterpolator(TimeInterpolator.FAST_OUT_SLOW_IN);
        return anim;
    }

    private Animator createUncheckedToCheckedAnimator() {
        ValueAnimator step1 = new ValueAnimator();
        step1.setDuration(150);
        step1.setInterpolator(TimeInterpolator.FAST_OUT_SLOW_IN);
        step1.addUpdateListener(animation -> {
            mCircleWidth = MathUtil.lerp(16, 32, animation.getAnimatedFraction());
            mCircleHeight = MathUtil.lerp(16, 22, animation.getAnimatedFraction());
            invalidateSelf();
        });

        ValueAnimator step2 = new ValueAnimator();
        step2.setDuration(100);
        step2.setInterpolator(TimeInterpolator.FAST_OUT_SLOW_IN);
        step2.addUpdateListener(animation -> {
            mCircleWidth = MathUtil.lerp(32, 24, animation.getAnimatedFraction());
            mCircleHeight = MathUtil.lerp(22, 24, animation.getAnimatedFraction());
            invalidateSelf();
        });

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(step1, step2);
        return set;
    }

    private Animator createCheckedToUncheckedAnimator() {
        ValueAnimator step1 = new ValueAnimator();
        step1.setDuration(100);
        step1.setInterpolator(TimeInterpolator.FAST_OUT_SLOW_IN);
        step1.addUpdateListener(animation -> {
            mCircleWidth = MathUtil.lerp(24, 32, animation.getAnimatedFraction());
            mCircleHeight = MathUtil.lerp(24, 22, animation.getAnimatedFraction());
            invalidateSelf();
        });

        ValueAnimator step2 = new ValueAnimator();
        step2.setDuration(150);
        step2.setInterpolator(TimeInterpolator.FAST_OUT_SLOW_IN);
        step2.addUpdateListener(animation -> {
            mCircleWidth = MathUtil.lerp(32, 16, animation.getAnimatedFraction());
            mCircleHeight = MathUtil.lerp(22, 16, animation.getAnimatedFraction());
            invalidateSelf();
        });

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(step1, step2);
        return set;
    }

    @Override
    public int getIntrinsicWidth() {
        return mSize;
    }

    @Override
    public int getIntrinsicHeight() {
        return mSize;
    }

    private static final FloatProperty<SwitchThumbDrawable> CIRCLE_WIDTH = new FloatProperty<>("circleWidth") {
        @Override
        public void setValue(SwitchThumbDrawable object, float value) {
            object.mCircleWidth = value;
            object.invalidateSelf();
        }

        @Override
        public Float get(SwitchThumbDrawable object) {
            return object.mCircleWidth;
        }
    };

    private static final FloatProperty<SwitchThumbDrawable> CIRCLE_HEIGHT = new FloatProperty<>("circleHeight") {
        @Override
        public void setValue(SwitchThumbDrawable object, float value) {
            object.mCircleHeight = value;
        }

        @Override
        public Float get(SwitchThumbDrawable object) {
            return object.mCircleHeight;
        }
    };
}
