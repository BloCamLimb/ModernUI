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

import javax.annotation.Nonnull;

public class DecelerateInterpolator implements Interpolator {

    private final float mFactor;

    private DecelerateInterpolator(float factor) {
        this.mFactor = factor;
    }

    /**
     * Create a decelerate interpolator, if {@code factor} is 1.0f,
     * a constant object will be returned.
     *
     * @param factor the decelerate factor
     * @return a decelerate interpolator
     */
    @Nonnull
    public static Interpolator create(float factor) {
        return factor == 1.0f ? Interpolator.DECELERATE : new DecelerateInterpolator(factor);
    }

    @Override
    public float getInterpolation(float progress) {
        return (float) (1.0f - Math.pow(1.0f - progress, mFactor * 2.0));
    }
}
