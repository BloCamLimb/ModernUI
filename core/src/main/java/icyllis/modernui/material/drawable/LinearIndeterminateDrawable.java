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
public class LinearIndeterminateDrawable extends BaseIndeterminateDrawable {

    // Constants for animation timing.
    private static final int TOTAL_DURATION_IN_MS = 1800;
    private static final int[] DURATION_TO_MOVE_SEGMENT_ENDS = {533, 567, 850, 750};
    private static final int[] DELAY_TO_MOVE_SEGMENT_ENDS = {1267, 1000, 333, 0};

    private static final TimeInterpolator[] INTERPOLATOR_ARRAY = {
            new BezierInterpolator(0.2f, 0.0f, 0.8f, 1.0f),
            new BezierInterpolator(0.4f, 0.0f, 1.0f, 1.0f),
            new BezierInterpolator(0.0f, 0.0f, 0.65f, 1.0f),
            new BezierInterpolator(0.1f, 0.0f, 0.45f, 1.0f),
    };

    private static final int SHORT_SIZE = 10;
    private static final int LONG_SIZE = 360;
    private final int mShortSize;
    private final int mLongSize;
    private final boolean mVertical;

    private float mStartFraction1;
    private float mEndFraction1;
    private float mStartFraction2;
    private float mEndFraction2;

    public LinearIndeterminateDrawable(Resources res, boolean vertical) {
        mShortSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DP, SHORT_SIZE,
                res.getDisplayMetrics()));
        mLongSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DP, LONG_SIZE,
                res.getDisplayMetrics()));
        mVertical = vertical;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Paint paint = Paint.obtain();
        if (mAlpha != 0) {
            final Rect r = getBounds();
            float thickness = 4 * mLongSize * (1.0f / LONG_SIZE);
            // default stroke cap is ROUND
            paint.setStyle(Paint.STROKE);
            paint.setStrokeWidth(thickness);
            // draw the track
            paint.setColor(mTrackColor);
            paint.setAlpha(ShapeDrawable.modulateAlpha(paint.getAlpha(), mAlpha));
            float start, end;
            if (mVertical) {
                start = r.bottom - thickness * 0.5f;
                end = r.top + thickness * 0.5f;
                canvas.drawLine(r.exactCenterX(), start,
                        r.exactCenterX(), end,
                        paint);
            } else {
                start = r.left + thickness * 0.5f;
                end = r.right - thickness * 0.5f;
                canvas.drawLine(start, r.exactCenterY(),
                        end, r.exactCenterY(),
                        paint);
            }
            // draw the indicators
            paint.setColor(mIndicatorColor);
            paint.setAlpha(ShapeDrawable.modulateAlpha(paint.getAlpha(), mAlpha));
            if (mVertical) {
                if (mStartFraction1 != mEndFraction1)
                    canvas.drawLine(r.exactCenterX(), MathUtil.lerp(start, end, mStartFraction1),
                            r.exactCenterX(), MathUtil.lerp(start, end, mEndFraction1),
                            paint);
                if (mStartFraction2 != mEndFraction2)
                    canvas.drawLine(r.exactCenterX(), MathUtil.lerp(start, end, mStartFraction2),
                            r.exactCenterX(), MathUtil.lerp(start, end, mEndFraction2),
                            paint);
            } else {
                if (mStartFraction1 != mEndFraction1)
                    canvas.drawLine(MathUtil.lerp(start, end, mStartFraction1), r.exactCenterY(),
                            MathUtil.lerp(start, end, mEndFraction1), r.exactCenterY(),
                            paint);
                if (mStartFraction2 != mEndFraction2)
                    canvas.drawLine(MathUtil.lerp(start, end, mStartFraction2), r.exactCenterY(),
                            MathUtil.lerp(start, end, mEndFraction2), r.exactCenterY(),
                            paint);
            }
        }
        paint.recycle();
    }

    @Override
    public int getIntrinsicWidth() {
        return mVertical ? mShortSize : mLongSize;
    }

    @Override
    public int getIntrinsicHeight() {
        return mVertical ? mLongSize : mShortSize;
    }

    @NonNull
    protected Animator createAnimator() {
        ValueAnimator anim = new ValueAnimator();
        anim.setDuration(TOTAL_DURATION_IN_MS);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(null);
        anim.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            int playtime = (int) (fraction * TOTAL_DURATION_IN_MS + 0.5f);
            updateSegmentPositions(playtime);
            invalidateSelf();
        });

        return anim;
    }

    private void updateSegmentPositions(int playtime) {
        float fraction =
                getFractionInRange(
                        playtime, DELAY_TO_MOVE_SEGMENT_ENDS[0], DURATION_TO_MOVE_SEGMENT_ENDS[0]);
        mStartFraction1 = MathUtil.clamp(INTERPOLATOR_ARRAY[0].getInterpolation(fraction), 0f, 1f);
        fraction =
                getFractionInRange(
                        playtime,
                        DELAY_TO_MOVE_SEGMENT_ENDS[1],
                        DURATION_TO_MOVE_SEGMENT_ENDS[1]);
        mEndFraction1 =
                MathUtil.clamp(INTERPOLATOR_ARRAY[1].getInterpolation(fraction), 0f, 1f);
        fraction =
                getFractionInRange(
                        playtime, DELAY_TO_MOVE_SEGMENT_ENDS[2], DURATION_TO_MOVE_SEGMENT_ENDS[2]);
        mStartFraction2 = MathUtil.clamp(INTERPOLATOR_ARRAY[2].getInterpolation(fraction), 0f, 1f);
        fraction =
                getFractionInRange(
                        playtime,
                        DELAY_TO_MOVE_SEGMENT_ENDS[3],
                        DURATION_TO_MOVE_SEGMENT_ENDS[3]);
        mEndFraction2 =
                MathUtil.clamp(INTERPOLATOR_ARRAY[3].getInterpolation(fraction), 0f, 1f);
    }

    protected static float getFractionInRange(int playtime, int start, int duration) {
        return (float) (playtime - start) / duration;
    }
}
