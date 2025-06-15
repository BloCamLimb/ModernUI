/*
 * Modern UI.
 * Copyright (C) 2024-2025 BloCamLimb. All rights reserved.
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

import icyllis.modernui.animation.AnimationUtils;
import icyllis.modernui.animation.Animator;
import icyllis.modernui.animation.AnimatorListener;
import icyllis.modernui.animation.MotionEasingUtils;
import icyllis.modernui.animation.ObjectAnimator;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.util.FloatProperty;

import java.util.ArrayList;

/**
 * Draws a ripple foreground.
 */
// Modified from Android
class RippleForeground extends RippleComponent {

    // Time it takes for the ripple to expand
    private static final int RIPPLE_ENTER_DURATION = 450;
    // Time it takes for the ripple to slide from the touch to the center point
    private static final int RIPPLE_ORIGIN_DURATION = 450;

    private static final int OPACITY_ENTER_DURATION = 150;
    private static final int OPACITY_EXIT_DURATION = 375;
    private static final int OPACITY_HOLD_DURATION = OPACITY_ENTER_DURATION + 300;

    private static final float RIPPLE_SMOOTHNESS = 0.5f;

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

    /**
     * Whether this ripple has finished its exit animation.
     */
    private boolean mHasFinishedExit;

    private long mEnterStartedAtMillis;

    private final ArrayList<Animator> mRunningAnimators = new ArrayList<>();

    private final float mStartRadius;

    RippleForeground(RippleDrawable owner, Rect bounds, float startingX, float startingY) {
        super(owner, bounds);

        mStartingX = startingX;
        mStartingY = startingY;

        // Take 40% of the maximum of the width and height, then divided half to get the radius.
        mStartRadius = 0;
        clampStartingPosition();
    }

    @Override
    protected void onTargetRadiusChanged(float targetRadius) {
        clampStartingPosition();
        invalidateSelf();
    }

    private void pruneFinished() {
        if (!mRunningAnimators.isEmpty()) {
            for (int i = mRunningAnimators.size() - 1; i >= 0; i--) {
                if (!mRunningAnimators.get(i).isRunning()) {
                    mRunningAnimators.remove(i);
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
    public final void enter(boolean hasMask) {
        // use longer animation when there's mask
        mEnterStartedAtMillis = AnimationUtils.currentAnimationTimeMillis();

        for (int i = 0; i < mRunningAnimators.size(); i++) {
            mRunningAnimators.get(i).cancel();
        }
        mRunningAnimators.clear();

        final ObjectAnimator tweenRadius = ObjectAnimator.ofFloat(this, TWEEN_RADIUS, 1);
        tweenRadius.setDuration(hasMask ? RIPPLE_ENTER_DURATION : RIPPLE_ENTER_DURATION / 2);
        tweenRadius.setInterpolator(MotionEasingUtils.FAST_OUT_SLOW_IN);
        tweenRadius.start();
        mRunningAnimators.add(tweenRadius);

        final ObjectAnimator tweenOrigin = ObjectAnimator.ofFloat(this, TWEEN_ORIGIN, 1);
        tweenOrigin.setDuration(hasMask ? RIPPLE_ORIGIN_DURATION : RIPPLE_ORIGIN_DURATION / 2);
        tweenOrigin.setInterpolator(MotionEasingUtils.FAST_OUT_SLOW_IN);
        tweenOrigin.start();
        mRunningAnimators.add(tweenOrigin);

        final ObjectAnimator opacity = ObjectAnimator.ofFloat(this, OPACITY, 1);
        opacity.setDuration(hasMask ? OPACITY_ENTER_DURATION : OPACITY_ENTER_DURATION / 2);
        opacity.setInterpolator(TimeInterpolator.LINEAR);
        opacity.start();
        mRunningAnimators.add(opacity);
    }

    /**
     * Starts a ripple exit animation.
     */
    public final void exit(boolean hasMask) {
        // use longer animation when there's mask
        final ObjectAnimator opacity = ObjectAnimator.ofFloat(this, OPACITY, 0);
        opacity.setDuration(hasMask ? OPACITY_EXIT_DURATION : 150);
        opacity.setInterpolator(TimeInterpolator.LINEAR);
        opacity.addListener(mAnimationListener);
        opacity.setStartDelay(computeFadeOutDelay());
        opacity.start();
        mRunningAnimators.add(opacity);
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
    public void draw(@NonNull Canvas c, float cx, float cy, @NonNull Paint p,
                     @Nullable Rect maskRect, @Nullable float[] maskRadii) {
        pruneFinished();

        final float origAlpha = p.getAlphaF();
        final float alpha = origAlpha * mOpacity;
        final float radius = getCurrentRadius();
        if (alpha > 0 && radius > 0) {
            final float x = getCurrentX();
            final float y = getCurrentY();
            p.setAlphaF(alpha);
            if (mTweenRadius < 1 && maskRect != null) {
                // make soft ripple if not fully expanded, and there's mask
                float maxRadius = radius / (1 - RIPPLE_SMOOTHNESS);
                var rr = new icyllis.arc3d.sketch.RRect();
                rr.setRectXY(cx + x - maxRadius - 0.001f, cy + y - maxRadius - 0.001f,
                        cx + x + maxRadius + 0.001f, cy + y + maxRadius + 0.001f,
                        maxRadius, maxRadius);
                p.getNativePaint().setShader(
                        icyllis.arc3d.sketch.shaders.RRectShader.make(
                                rr, maxRadius * RIPPLE_SMOOTHNESS, false, null
                        )
                );
            }
            // mask is used in bounded case, where targetRadius should cover the full bounds,
            // then we draw the mask and ignore the circle with targetRadius
            if (maskRect != null) {
                if (maskRadii != null) {
                    // matching behavior in ShapeDrawable
                    if (maskRadii[0] == maskRadii[1] &&
                            maskRadii[0] == maskRadii[2] &&
                            maskRadii[0] == maskRadii[3]) {
                        float rad = Math.min(maskRadii[0],
                                Math.min(maskRect.width(), maskRect.height()) * 0.5f);
                        c.drawRoundRect(maskRect.left, maskRect.top, maskRect.right, maskRect.bottom,
                                rad, p);
                    } else {
                        c.drawRoundRect(maskRect.left, maskRect.top, maskRect.right, maskRect.bottom,
                                maskRadii[0], maskRadii[1], maskRadii[2], maskRadii[3], p);
                    }
                } else {
                    c.drawRect(maskRect, p);
                }
            } else {
                c.drawCircle(cx + x, cy + y, radius, p);
            }
            p.setAlphaF(origAlpha);
            p.setShader(null);
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
        for (int i = 0; i < mRunningAnimators.size(); i++) {
            mRunningAnimators.get(i).end();
        }
        mRunningAnimators.clear();
    }

    private void onAnimationPropertyChanged() {
        invalidateSelf();
    }

    private final AnimatorListener mAnimationListener = new AnimatorListener() {
        @Override
        public void onAnimationEnd(@NonNull Animator animator) {
            mHasFinishedExit = true;
            pruneFinished();
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
