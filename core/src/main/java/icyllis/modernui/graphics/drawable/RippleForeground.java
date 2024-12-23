/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.drawable;

import icyllis.modernui.animation.*;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.*;
import icyllis.modernui.util.FloatProperty;

import java.util.ArrayList;

/**
 * Draws a ripple foreground.
 */
// Modified from Android
class RippleForeground extends RippleComponent {

    // Time it takes for the ripple to expand
    private static final int RIPPLE_ENTER_DURATION = 225;
    // Time it takes for the ripple to slide from the touch to the center point
    private static final int RIPPLE_ORIGIN_DURATION = 225;

    private static final int OPACITY_ENTER_DURATION = 75;
    private static final int OPACITY_EXIT_DURATION = 150;
    private static final int OPACITY_HOLD_DURATION = OPACITY_ENTER_DURATION + 150;

    // Parent-relative values for starting position.
    private float mStartingX;
    private float mStartingY;
    private float mClampedStartingX;
    private float mClampedStartingY;

    // Target values for tween animations.
    private float mTargetX = 0;
    private float mTargetY = 0;

    // Software rendering properties.
    private float mOpacity = 0;

    // Values used to tween between the start and end positions.
    private float mTweenRadius = 0;
    private float mTweenX = 0;
    private float mTweenY = 0;

    /** Whether this ripple has finished its exit animation. */
    private boolean mHasFinishedExit;

    private long mEnterStartedAtMillis;

    private final ArrayList<Animator> mRunningVtAnimators = new ArrayList<>();

    private final float mStartRadius;

    RippleForeground(RippleDrawable owner, Rect bounds, float startingX, float startingY) {
        super(owner, bounds);

        mStartingX = startingX;
        mStartingY = startingY;

        // Take 40% of the maximum of the width and height, then divided half to get the radius.
        mStartRadius = Math.max(bounds.width(), bounds.height()) * 0.2f;
        clampStartingPosition();
    }

    @Override
    protected void onTargetRadiusChanged(float targetRadius) {
        clampStartingPosition();
        invalidateSelf();
    }

    private void pruneVtFinished() {
        if (!mRunningVtAnimators.isEmpty()) {
            for (int i = mRunningVtAnimators.size() - 1; i >= 0; i--) {
                if (!mRunningVtAnimators.get(i).isRunning()) {
                    mRunningVtAnimators.remove(i);
                }
            }
        }
    }

    /**
     * Returns the maximum bounds of the ripple relative to the ripple center.
     */
    @Override
    public void getBounds(Rect bounds) {
        final int outerX = (int) mTargetX;
        final int outerY = (int) mTargetY;
        final int r = (int) mTargetRadius + 1;
        bounds.set(outerX - r, outerY - r, outerX + r, outerY + r);
    }

    /**
     * Specifies the starting position relative to the drawable bounds. No-op if
     * the ripple has already entered.
     */
    public void move(float x, float y) {
        mStartingX = x;
        mStartingY = y;

        clampStartingPosition();
    }

    /**
     * @return {@code true} if this ripple has finished its exit animation
     */
    public boolean hasFinishedExit() {
        return mHasFinishedExit;
    }

    private long computeFadeOutDelay() {
        long timeSinceEnter = AnimationUtils.currentAnimationTimeMillis() - mEnterStartedAtMillis;
        if (timeSinceEnter > 0 && timeSinceEnter < OPACITY_HOLD_DURATION) {
            return OPACITY_HOLD_DURATION - timeSinceEnter;
        }
        return 0;
    }

    /**
     * Starts a ripple enter animation.
     */
    public final void enter() {
        mEnterStartedAtMillis = AnimationUtils.currentAnimationTimeMillis();

        for (int i = 0; i < mRunningVtAnimators.size(); i++) {
            mRunningVtAnimators.get(i).cancel();
        }
        mRunningVtAnimators.clear();

        final ObjectAnimator tweenRadius = ObjectAnimator.ofFloat(this, TWEEN_RADIUS, 1);
        tweenRadius.setDuration(RIPPLE_ENTER_DURATION);
        tweenRadius.setInterpolator(TimeInterpolator.FAST_OUT_SLOW_IN);
        tweenRadius.start();
        mRunningVtAnimators.add(tweenRadius);

        final ObjectAnimator tweenOrigin = ObjectAnimator.ofFloat(this, TWEEN_ORIGIN, 1);
        tweenOrigin.setDuration(RIPPLE_ORIGIN_DURATION);
        tweenOrigin.setInterpolator(TimeInterpolator.FAST_OUT_SLOW_IN);
        tweenOrigin.start();
        mRunningVtAnimators.add(tweenOrigin);

        final ObjectAnimator opacity = ObjectAnimator.ofFloat(this, OPACITY, 1);
        opacity.setDuration(OPACITY_ENTER_DURATION);
        opacity.setInterpolator(TimeInterpolator.LINEAR);
        opacity.start();
        mRunningVtAnimators.add(opacity);
    }

    /**
     * Starts a ripple exit animation.
     */
    public final void exit() {
        final ObjectAnimator opacity = ObjectAnimator.ofFloat(this, OPACITY, 0);
        opacity.setDuration(OPACITY_EXIT_DURATION);
        opacity.setInterpolator(TimeInterpolator.LINEAR);
        opacity.addListener(mAnimationListener);
        opacity.setStartDelay(computeFadeOutDelay());
        opacity.start();
        mRunningVtAnimators.add(opacity);
    }

    private float getCurrentX() {
        return MathUtil.lerp(mClampedStartingX - mBounds.exactCenterX(), mTargetX, mTweenX);
    }

    private float getCurrentY() {
        return MathUtil.lerp(mClampedStartingY - mBounds.exactCenterY(), mTargetY, mTweenY);
    }

    private float getCurrentRadius() {
        return MathUtil.lerp(mStartRadius, mTargetRadius, mTweenRadius);
    }

    /**
     * Draws the ripple to the canvas, inheriting the paint's color and alpha
     * properties.
     *
     * @param c the canvas to which the ripple should be drawn
     * @param p the paint used to draw the ripple
     */
    public void draw(Canvas c, Paint p) {
        pruneVtFinished();

        final int origAlpha = p.getAlpha();
        final int alpha = (int) (origAlpha * mOpacity + 0.5f);
        final float radius = getCurrentRadius();
        if (alpha > 0 && radius > 0) {
            final float x = getCurrentX();
            final float y = getCurrentY();
            p.setAlpha(alpha);
            c.drawCircle(x, y, radius, p);
            p.setAlpha(origAlpha);
        }
    }

    /**
     * Clamps the starting position to fit within the ripple bounds.
     */
    private void clampStartingPosition() {
        final float cX = mBounds.exactCenterX();
        final float cY = mBounds.exactCenterY();
        final float dX = mStartingX - cX;
        final float dY = mStartingY - cY;
        final float r = mTargetRadius - mStartRadius;
        if (dX * dX + dY * dY > r * r) {
            // Point is outside the circle, clamp to the perimeter.
            final double angle = Math.atan2(dY, dX);
            mClampedStartingX = cX + (float) (Math.cos(angle) * r);
            mClampedStartingY = cY + (float) (Math.sin(angle) * r);
        } else {
            mClampedStartingX = mStartingX;
            mClampedStartingY = mStartingY;
        }
    }

    /**
     * Ends all animations, jumping values to the end state.
     */
    public void end() {
        for (int i = 0; i < mRunningVtAnimators.size(); i++) {
            mRunningVtAnimators.get(i).end();
        }
        mRunningVtAnimators.clear();
    }

    private void onAnimationPropertyChanged() {
        invalidateSelf();
    }

    private final AnimatorListener mAnimationListener = new AnimatorListener() {
        @Override
        public void onAnimationEnd(@NonNull Animator animator) {
            mHasFinishedExit = true;
            pruneVtFinished();
        }
    };

    /**
     * Property for animating radius between its initial and target values.
     */
    private static final FloatProperty<RippleForeground> TWEEN_RADIUS = new FloatProperty<>("tweenRadius") {
        @Override
        public void setValue(RippleForeground object, float value) {
            object.mTweenRadius = value;
            object.onAnimationPropertyChanged();
        }

        @Override
        public Float get(RippleForeground object) {
            return object.mTweenRadius;
        }
    };

    /**
     * Property for animating origin between its initial and target values.
     */
    private static final FloatProperty<RippleForeground> TWEEN_ORIGIN = new FloatProperty<>("tweenOrigin") {
        @Override
        public void setValue(RippleForeground object, float value) {
            object.mTweenX = value;
            object.mTweenY = value;
            object.onAnimationPropertyChanged();
        }

        @Override
        public Float get(RippleForeground object) {
            return object.mTweenX;
        }
    };

    /**
     * Property for animating opacity between 0 and its target value.
     */
    private static final FloatProperty<RippleForeground> OPACITY = new FloatProperty<>("opacity") {
        @Override
        public void setValue(RippleForeground object, float value) {
            object.mOpacity = value;
            object.onAnimationPropertyChanged();
        }

        @Override
        public Float get(RippleForeground object) {
            return object.mOpacity;
        }
    };
}
