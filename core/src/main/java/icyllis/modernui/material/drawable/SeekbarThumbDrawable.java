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
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.animation.ValueAnimator;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.material.MaterialDrawable;
import icyllis.modernui.view.View;
import org.jetbrains.annotations.ApiStatus;

/**
 * A thumb drawable for SeekBar, similar to Material Design, vertical bar style.
 */
@ApiStatus.Internal
public class SeekbarThumbDrawable extends MaterialDrawable {

    private final int mWidth; // 4dp
    private final int mHeight; // 28dp

    // actual width = width * thickness ratio
    private float mThickness = 0.5f;

    // -1: init, 0: unpressed, 1: pressed or focused
    private int mCurState = -1;
    private int mTransitionToState = -1;

    // current transition, if any
    private Animator mTransition;

    public SeekbarThumbDrawable(View slider) {
        mWidth = slider.dp(4);
        mHeight = slider.dp(28);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Paint paint = Paint.obtain();
        paint.setColor(mColor);
        paint.setAlpha(ShapeDrawable.modulateAlpha(paint.getAlpha(), mAlpha));
        if (paint.getAlpha() != 0) {
            final Rect r = getBounds();
            float cx = r.exactCenterX();
            float thick = mWidth * mThickness;
            canvas.drawRoundRect(cx - thick, r.top, cx + thick, r.bottom, thick, paint);
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
            if (state == R.attr.state_pressed || state == R.attr.state_focused) {
                toState = 1;
                break;
            }
        }
        changed |= selectTransition(toState) || selectDrawable(toState);

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
            transition = createUnpressedToPressedAnimator();
        } else {
            transition = createPressedToUnpressedAnimator();
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
            mThickness = 0.25f;
        } else {
            mThickness = 0.5f;
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

    private Animator createUnpressedToPressedAnimator() {
        ValueAnimator anim = new ValueAnimator();
        anim.setDuration(133);
        anim.setInterpolator(TimeInterpolator.FAST_OUT_SLOW_IN);
        anim.addUpdateListener(animation -> {
            mThickness = MathUtil.lerp(0.5f, 0.25f, animation.getAnimatedFraction());
            invalidateSelf();
        });
        return anim;
    }

    private Animator createPressedToUnpressedAnimator() {
        ValueAnimator anim = new ValueAnimator();
        anim.setDuration(133);
        anim.setInterpolator(TimeInterpolator.FAST_OUT_SLOW_IN);
        anim.addUpdateListener(animation -> {
            mThickness = MathUtil.lerp(0.25f, 0.5f, animation.getAnimatedFraction());
            invalidateSelf();
        });
        return anim;
    }

    //TODO returns optical insets 6dp

    @Override
    public int getIntrinsicWidth() {
        return mWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mHeight;
    }
}
