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

public class CycleInterpolator implements Interpolator {

    private final float mCycle;

    private CycleInterpolator(float cycle) {
        this.mCycle = cycle;
    }

    /**
     * Create a cycle interpolator, if {@code cycle} is 0.25f,
     * a constant object will be returned.
     *
     * @param cycle the cycle
     * @return a cycle interpolator
     */
    public static Interpolator create(float cycle) {
        return cycle == 0.25f ? Interpolator.SINE : new CycleInterpolator(cycle);
    }

    @Override
    public float getInterpolation(float progress) {
        return MathUtil.sin(MathUtil.TWO_PI * mCycle * progress);
    }
}
