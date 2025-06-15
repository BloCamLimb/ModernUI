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

package icyllis.modernui.animation;

import icyllis.modernui.annotation.NonNull;

/**
 * Utility class for animations containing motion system interpolators.
 *
 * @since 3.12
 */
public final class MotionEasingUtils {
    /**
     * i.e. motion_easing_legacy
     */
    @NonNull
    public static final
    TimeInterpolator FAST_OUT_SLOW_IN = new BezierInterpolator(0.4f, 0f, 0.2f, 1f);
    /**
     * i.e. motion_easing_legacy_accelerate
     */
    @NonNull
    public static final
    TimeInterpolator FAST_OUT_LINEAR_IN = new BezierInterpolator(0.4f, 0f, 1f, 1f);
    /**
     * i.e. motion_easing_legacy_decelerate
     */
    @NonNull
    public static final
    TimeInterpolator LINEAR_OUT_SLOW_IN = new BezierInterpolator(0f, 0f, 0.2f, 1f);

    @NonNull
    public static final
    TimeInterpolator MOTION_EASING_STANDARD = new BezierInterpolator(
            0.2f, 0f, 0f, 1f
    );
    @NonNull
    public static final
    TimeInterpolator MOTION_EASING_STANDARD_ACCELERATE = new BezierInterpolator(
            0.3f, 0f, 1f, 1f
    );
    @NonNull
    public static final
    TimeInterpolator MOTION_EASING_STANDARD_DECELERATE = new BezierInterpolator(
            0f, 0f, 0f, 1f
    );

    /**
     * i.e. fast_out_extra_slow_in
     */
    @NonNull
    public static final
    TimeInterpolator MOTION_EASING_EMPHASIZED = BezierInterpolator.createTwoCubic(
            0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f,
            0.208333f, 0.82f, 0.25f, 1f
    );
    @NonNull
    public static final
    TimeInterpolator MOTION_EASING_EMPHASIZED_ACCELERATE = new BezierInterpolator(
            0.3f, 0f, 0.8f, 0.15f
    );
    @NonNull
    public static final
    TimeInterpolator MOTION_EASING_EMPHASIZED_DECELERATE = new BezierInterpolator(
            0.05f, 0.7f, 0.1f, 1f
    );

    /**
     * Linear interpolation between {@code startValue} and {@code endValue} by {@code fraction}.
     */
    public static float lerp(float startValue, float endValue, float fraction) {
        return startValue + (fraction * (endValue - startValue));
    }

    /**
     * Linear interpolation between {@code startValue} and {@code endValue} by {@code fraction}.
     */
    public static int lerp(int startValue, int endValue, float fraction) {
        return startValue + Math.round(fraction * (endValue - startValue));
    }

    /**
     * Linear interpolation between {@code outputMin} and {@code outputMax} when {@code value} is
     * between {@code inputMin} and {@code inputMax}.
     *
     * <p>Note that {@code value} will be coerced into {@code inputMin} and {@code inputMax}.This
     * function can handle input and output ranges that span positive and negative numbers.
     */
    public static float lerp(
            float outputMin, float outputMax, float inputMin, float inputMax, float value) {
        if (value <= inputMin) {
            return outputMin;
        }
        if (value >= inputMax) {
            return outputMax;
        }

        return lerp(outputMin, outputMax, (value - inputMin) / (inputMax - inputMin));
    }
}
