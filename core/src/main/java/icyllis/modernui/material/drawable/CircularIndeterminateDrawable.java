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

import icyllis.modernui.animation.Animator;
import icyllis.modernui.animation.AnimatorSet;
import icyllis.modernui.animation.BezierInterpolator;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.animation.ValueAnimator;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.resources.TypedValue;
import org.jetbrains.annotations.ApiStatus;

/**
 * An indeterminate progress drawable for ProgressBar, similar to Material Design,
 * but not so rich animation effects.
 */
@ApiStatus.Internal
public class CircularIndeterminateDrawable extends BaseIndeterminateDrawable {

    private static final TimeInterpolator TRIM_START_INTERPOLATOR =
            BezierInterpolator.createTwoCubic(0.5f, 0, 0.5f, 0, 0.5f, 0, 0.7f, 0, 0.6f, 1);
    private static final TimeInterpolator TRIM_END_INTERPOLATOR =
            BezierInterpolator.createTwoCubic(0.2f, 0, 0.1f, 1, 0.5f, 1, 1, 1, 1, 1);

    private final int mTotalSize;
    private final float mRadius;
    private final float mThickness;

    private float mTrimPathStart;
    private float mTrimPathEnd;
    private float mTrimPathOffset;
    private float mRotation;

    public CircularIndeterminateDrawable(Resources res, int size, int inset, float thickness) {
        int totalSize = size + inset * 2;
        mTotalSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DP, totalSize,
                res.getDisplayMetrics()));
        float scale = (float) mTotalSize / totalSize;
        mRadius = (size - inset) * 0.5f * scale;
        mThickness = thickness * scale;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Paint paint = Paint.obtain();
        if (mAlpha != 0) {
            final Rect r = getBounds();
            // default stroke cap is ROUND
            paint.setStyle(Paint.STROKE);
            paint.setStrokeWidth(mThickness);
            // circular indeterminate progress bar does not have a track, only draw the indicator
            paint.setColor(mIndicatorColor);
            paint.setAlpha(ShapeDrawable.modulateAlpha(paint.getAlpha(), mAlpha));
            if (ValueAnimator.areAnimatorsEnabled()) {
                canvas.save();
                canvas.translate(r.exactCenterX(), r.exactCenterY());
                canvas.rotate(mRotation);

                canvas.drawArc(0, 0,
                        mRadius,
                        (mTrimPathStart + mTrimPathOffset) * 360,
                        (mTrimPathEnd - mTrimPathStart) * 360,
                        paint);
                canvas.restore();
            } else {
                // draw a full circle if animations are disabled
                canvas.drawCircle(r.exactCenterX(), r.exactCenterY(),
                        mRadius, paint);
            }
        }
        paint.recycle();
    }

    @Override
    public int getIntrinsicWidth() {
        return mTotalSize;
    }

    @Override
    public int getIntrinsicHeight() {
        return mTotalSize;
    }

    @NonNull
    protected Animator createAnimator() {
        ValueAnimator anim1 = new ValueAnimator();
        anim1.setDuration(1333);
        anim1.setRepeatCount(ValueAnimator.INFINITE);
        anim1.setInterpolator(null);
        anim1.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            mTrimPathStart = MathUtil.lerp(0, 0.75f, TRIM_START_INTERPOLATOR.getInterpolation(fraction));
            mTrimPathEnd = MathUtil.lerp(0, 0.75f, TRIM_END_INTERPOLATOR.getInterpolation(fraction));
            mTrimPathOffset = MathUtil.lerp(0, 0.25f, fraction);
        });

        ValueAnimator anim2 = new ValueAnimator();
        anim2.setDuration(4444);
        anim2.setRepeatCount(ValueAnimator.INFINITE);
        anim2.setInterpolator(null);
        anim2.addUpdateListener(animation -> {
            mRotation = MathUtil.lerp(0, 720, animation.getAnimatedFraction());
            invalidateSelf();
        });

        AnimatorSet set = new AnimatorSet();
        set.playTogether(anim1, anim2);
        return set;
    }
}
