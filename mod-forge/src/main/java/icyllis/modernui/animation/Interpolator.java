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

import icyllis.modernui.math.MathUtil;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface Interpolator {

    /**
     * The linear interpolator.
     */
    @Nonnull
    Interpolator LINEAR = in -> in;

    /**
     * From slow to fast.
     */
    @Nonnull
    Interpolator ACCELERATE = in -> in * in;

    /**
     * From fast to slow.
     */
    @Nonnull
    Interpolator DECELERATE = in -> 1.0f - (1.0f - in) * (1.0f - in);

    /**
     * Slow to fast to slow.
     */
    @Nonnull
    Interpolator ACC_DEC = in -> MathUtil.cos((in + 1.0f) * MathUtil.PI) * 0.5f + 0.5f;

    /**
     * From fast to slow, simple harmonic motion.
     */
    @Nonnull
    Interpolator SINE = in -> MathUtil.sin(MathUtil.PI_DIV_2 * in);

    /**
     * Default used in scroller
     */
    @Nonnull
    Interpolator VISCOUS_FLUID = new ViscousFluidInterpolator();

    /**
     * Get interpolation value
     *
     * @param progress [0.0, 1.0] determined by timeline, 0.0 represents
     *                 the start and 1.0 represents the end
     * @return interpolated value
     */
    float getInterpolation(float progress);
}
