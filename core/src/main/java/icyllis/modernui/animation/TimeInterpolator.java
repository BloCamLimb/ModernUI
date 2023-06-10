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

package icyllis.modernui.animation;

import icyllis.modernui.annotation.NonNull;

/**
 * An interpolator defines the rate of change of an animation. This allows animations
 * to have non-linear motion, such as acceleration and deceleration.
 */
@FunctionalInterface
public interface TimeInterpolator {

    /**
     * The linear interpolator.
     */
    @NonNull
    TimeInterpolator LINEAR = t -> t;

    /**
     * The constant accelerate interpolator whose factor is 1.0.
     *
     * @see #accelerate(float)
     */
    @NonNull
    TimeInterpolator ACCELERATE = t -> t * t;

    /**
     * The constant decelerate interpolator whose factor is 1.0.
     *
     * @see #decelerate(float)
     */
    @NonNull
    TimeInterpolator DECELERATE = t -> 1.0f - (1.0f - t) * (1.0f - t);

    /**
     * The constant decelerate interpolator whose factor is 1.5.
     *
     * @see #decelerate(float)
     */
    @NonNull
    TimeInterpolator DECELERATE_CUBIC = t -> {
        t -= 1.0f;
        return t * t * t + 1.0f;
    };

    /**
     * The constant decelerate interpolator whose factor is 2.5.
     *
     * @see #decelerate(float)
     */
    @NonNull
    TimeInterpolator DECELERATE_QUINTIC = t -> {
        t -= 1.0f;
        return t * t * t * t * t + 1.0f;
    };

    /**
     * The interpolator where the rate of change starts and ends slowly but
     * accelerates through the middle.
     */
    @NonNull
    TimeInterpolator ACCELERATE_DECELERATE = t -> (float) Math.cos((t + 1.0) * Math.PI) * 0.5f + 0.5f;

    /**
     * The constant cycle interpolator that indicates 1/4 cycle sine wave.
     *
     * @see #cycle(float)
     */
    @NonNull
    TimeInterpolator SINE = t -> (float) Math.sin(Math.PI / 2.0 * t);

    /**
     * The constant anticipate interpolator whose tension is 2.0.
     *
     * @see #anticipate(float)
     */
    @NonNull
    TimeInterpolator ANTICIPATE = t -> t * t * (3.0f * t - 2.0f);

    /**
     * The constant overshoot interpolator whose tension is 2.0.
     *
     * @see #overshoot(float)
     */
    @NonNull
    TimeInterpolator OVERSHOOT = t -> {
        t -= 1.0f;
        return t * t * (3.0f * t + 2.0f) + 1.0f;
    };

    /**
     * The constant anticipate/overshoot interpolator whose tension is 2.0.
     *
     * @see AnticipateOvershootInterpolator
     */
    @NonNull
    TimeInterpolator ANTICIPATE_OVERSHOOT = new AnticipateOvershootInterpolator();

    /**
     * The bounce interpolator where the change bounces at the end.
     */
    @NonNull
    TimeInterpolator BOUNCE = new BounceInterpolator();

    @NonNull
    TimeInterpolator VISCOUS_FLUID = new ViscousFluidInterpolator();

    /**
     * Get interpolation value. This interpolated value is then multiplied by the change in
     * value of an animation to derive the animated value at the current elapsed animation time.
     *
     * @param progress [0.0, 1.0] determined by timeline, 0.0 represents
     *                 the start and 1.0 represents the end
     * @return the interpolated value. this value can be more than 1.0 for those overshoot
     * their targets, or less than 0 for those undershoot their targets.
     */
    float getInterpolation(float progress);

    /**
     * Return a linear interpolator (identity).
     *
     * @return a linear interpolator
     */
    @NonNull
    static TimeInterpolator linear() {
        return LINEAR;
    }

    /**
     * Return an interpolator where the rate of change starts out slowly
     * and then accelerates.
     *
     * @return an accelerate interpolator
     */
    @NonNull
    static TimeInterpolator accelerate() {
        return ACCELERATE;
    }

    /**
     * Create an interpolator where the rate of change starts out slowly
     * and then accelerates.
     *
     * @param factor acceleration factor
     * @return an accelerate interpolator
     */
    @NonNull
    static TimeInterpolator accelerate(float factor) {
        if (factor == 1.0f)
            return ACCELERATE;
        final double f = factor * 2.0;
        return t -> (float) Math.pow(t, f);
    }

    /**
     * Return an interpolator where the rate of change starts out quickly
     * and then decelerates.
     *
     * @return a decelerate interpolator
     */
    @NonNull
    static TimeInterpolator decelerate() {
        return DECELERATE;
    }

    /**
     * Create an interpolator where the rate of change starts out quickly
     * and then decelerates.
     *
     * @param factor deceleration factor
     * @return a decelerate interpolator
     * @see #DECELERATE
     * @see #DECELERATE_CUBIC
     * @see #DECELERATE_QUINTIC
     */
    @NonNull
    static TimeInterpolator decelerate(float factor) {
        if (factor == 1.0f)
            return DECELERATE;
        else if (factor == 1.5f)
            return DECELERATE_CUBIC;
        else if (factor == 2.5f)
            return DECELERATE_QUINTIC;
        else {
            final double f = factor * 2.0;
            return t -> (float) (1.0 - Math.pow(1.0 - t, f));
        }
    }

    /**
     * Create a cycle interpolator which repeats the animation for a specified number of cycles. The
     * rate of change follows a sinusoidal pattern. If {@code cycle} is 0.25f, a constant object will
     * be returned.
     */
    @NonNull
    static TimeInterpolator cycle(float cycle) {
        if (cycle == 0.25f)
            return SINE;
        final double f = Math.PI * 2.0 * cycle;
        return t -> (float) Math.sin(f * t);
    }

    /**
     * Return an interpolator where the change starts backward then flings forward.
     *
     * @return an anticipate interpolator
     */
    @NonNull
    static TimeInterpolator anticipate() {
        return ANTICIPATE;
    }

    /**
     * Create an interpolator where the change starts backward then flings forward.
     *
     * @param tension anticipation tension
     * @return an anticipate interpolator
     */
    @NonNull
    static TimeInterpolator anticipate(float tension) {
        if (tension == 2.0f)
            return ANTICIPATE;
        return t -> t * t * ((tension + 1.0f) * t - tension);
    }

    /**
     * Return an interpolator where the change flings forward and overshoots the last value
     * then comes back.
     *
     * @return an overshoot interpolator
     */
    @NonNull
    static TimeInterpolator overshoot() {
        return OVERSHOOT;
    }

    /**
     * Create an interpolator where the change flings forward and overshoots the last value
     * then comes back.
     *
     * @param tension overshoot tension
     * @return an overshoot interpolator
     */
    @NonNull
    static TimeInterpolator overshoot(float tension) {
        if (tension == 2.0f)
            return OVERSHOOT;
        return t -> {
            t -= 1.0f;
            return t * t * ((tension + 1.0f) * t + tension) + 1.0f;
        };
    }

    /**
     * Return an interpolator where the change bounces at the end.
     *
     * @return a bounce interpolator
     */
    @NonNull
    static TimeInterpolator bounce() {
        return BOUNCE;
    }
}
