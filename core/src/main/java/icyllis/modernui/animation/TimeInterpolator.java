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

import icyllis.modernui.math.FMath;

import javax.annotation.Nonnull;

/**
 * An interpolator defines the rate of change of an animation. This allows animations
 * to have non-linear motion, such as acceleration and deceleration.
 */
@FunctionalInterface
public interface TimeInterpolator {

    /**
     * The linear interpolator.
     */
    @Nonnull
    TimeInterpolator LINEAR = in -> in;

    /**
     * The constant accelerate interpolator whose factor is 1.0.
     *
     * @see #accelerate(float)
     */
    @Nonnull
    TimeInterpolator ACCELERATE = in -> in * in;

    /**
     * The constant decelerate interpolator whose factor is 1.0.
     *
     * @see #decelerate(float)
     */
    @Nonnull
    TimeInterpolator DECELERATE = in -> 1.0f - (1.0f - in) * (1.0f - in);

    /**
     * The constant decelerate interpolator whose factor is 1.5.
     *
     * @see #decelerate(float)
     */
    @Nonnull
    TimeInterpolator DECELERATE_CUBIC = in -> 1.0f - (1.0f - in) * (1.0f - in) * (1.0f - in);

    /**
     * The interpolator where the rate of change starts and ends slowly but
     * accelerates through the middle.
     */
    @Nonnull
    TimeInterpolator ACCELERATE_DECELERATE = in -> FMath.cos((in + 1.0f) * FMath.PI) * 0.5f + 0.5f;

    /**
     * The constant cycle interpolator that indicates 1/4 cycle sine wave.
     *
     * @see #cycle(float)
     */
    @Nonnull
    TimeInterpolator SINE = in -> FMath.sin(FMath.PI_O_2 * in);

    /**
     * The constant anticipate interpolator whose tension is 2.0.
     *
     * @see #anticipate(float)
     */
    @Nonnull
    TimeInterpolator ANTICIPATE = in -> in * in * (3.0f * in - 2.0f);

    /**
     * The constant overshoot interpolator whose tension is 2.0.
     *
     * @see #overshoot(float)
     */
    @Nonnull
    TimeInterpolator OVERSHOOT = in -> (in - 1.0f) * (in - 1.0f) * (3.0f * (in - 1.0f) + 2.0f) + 1.0f;

    /**
     * The constant anticipate/overshoot interpolator whose tension is 2.0.
     *
     * @see #anticipateOvershoot(float)
     */
    @Nonnull
    TimeInterpolator ANTICIPATE_OVERSHOOT = new AnticipateOvershootInterpolator();

    /**
     * The bounce interpolator where the change bounces at the end.
     */
    @Nonnull
    TimeInterpolator BOUNCE = new BounceInterpolator();

    @Nonnull
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
     * Create an interpolator where the rate of change starts out slowly
     * and then accelerates. If {@code factor} is 1.0f, a constant object will be returned.
     *
     * @param factor acceleration factor
     * @return an accelerate interpolator
     */
    @Nonnull
    static TimeInterpolator accelerate(float factor) {
        if (factor == 1.0f)
            return ACCELERATE;
        return t -> (float) Math.pow(t, factor * 2.0);
    }

    /**
     * Create an interpolator where the rate of change starts out quickly
     * and then decelerates. If {@code factor} is 1.0f, a constant object will be returned.
     *
     * @param factor deceleration factor
     * @return a decelerate interpolator
     */
    @Nonnull
    static TimeInterpolator decelerate(float factor) {
        if (factor == 1.0f)
            return DECELERATE;
        else if (factor == 1.5f)
            return DECELERATE_CUBIC;
        else
            return t -> (float) (1.0f - Math.pow(1.0f - t, factor * 2.0));
    }

    /**
     * Create a cycle interpolator which repeats the animation for a specified number of cycles. The
     * rate of change follows a sinusoidal pattern. If {@code cycle} is 0.25f, a constant object will
     * be returned.
     */
    @Nonnull
    static TimeInterpolator cycle(float cycle) {
        if (cycle == 0.25f)
            return SINE;
        return t -> FMath.sin(FMath.PI2 * cycle * t);
    }

    /**
     * Create an interpolator where the change starts backward then flings forward.
     * If {@code tension} is 2.0f, a constant object will be returned.
     *
     * @param tension anticipation tension
     * @return an anticipate interpolator
     */
    @Nonnull
    static TimeInterpolator anticipate(float tension) {
        if (tension == 2.0f)
            return ANTICIPATE;
        return t -> t * t * ((tension + 1.0f) * t - tension);
    }

    /**
     * Create an interpolator where the change flings forward and overshoots the last value
     * then comes back. If {@code tension} is 2.0f, a constant object will be returned.
     *
     * @param tension overshoot tension
     * @return an overshoot interpolator
     */
    @Nonnull
    static TimeInterpolator overshoot(float tension) {
        if (tension == 2.0f)
            return OVERSHOOT;
        return t -> {
            t -= 1.0f;
            return t * t * ((tension + 1.0f) * t + tension) + 1.0f;
        };
    }

    /**
     * Create an anticipate/overshoot interpolator where the change starts backward then flings forward
     * and overshoots the target value and finally goes back to the final value. If {@code tension} is
     * 2.0f, a constant object will be returned.
     *
     * @param tension Amount of anticipation/overshoot. When tension equals 0.0f,
     *                there is no anticipation/overshoot and the interpolator becomes
     *                a simple acceleration/deceleration interpolator.
     * @return an anticipate/overshoot interpolator
     */
    @Nonnull
    static TimeInterpolator anticipateOvershoot(float tension) {
        if (tension == 2.0f) {
            return ANTICIPATE_OVERSHOOT;
        }
        return new AnticipateOvershootInterpolator(tension);
    }

    /**
     * Create an anticipate/overshoot interpolator where the change starts backward then flings forward
     * and overshoots the target value and finally goes back to the final value.
     *
     * @param tension      Amount of anticipation/overshoot. When tension equals 0.0f,
     *                     there is no anticipation/overshoot and the interpolator becomes
     *                     a simple acceleration/deceleration interpolator.
     * @param extraTension Amount by which to multiply the tension. For instance,
     *                     to get the same overshoot as an OvershootInterpolator with
     *                     a tension of 2.0f, you would use an extraTension of 1.5f.
     * @return an anticipate/overshoot interpolator
     */
    @Nonnull
    static TimeInterpolator anticipateOvershoot(float tension, float extraTension) {
        return new AnticipateOvershootInterpolator(tension, extraTension);
    }
}
