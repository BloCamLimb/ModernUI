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

public class OvershootInterpolator implements Interpolator {

    private final float mTension;

    private OvershootInterpolator(float tension) {
        this.mTension = tension;
    }

    /**
     * Create an overshoot interpolator, if {@code tension} is 2.0f,
     * a constant object will be returned.
     *
     * @param tension the overshoot tension
     * @return an overshoot interpolator
     */
    public static Interpolator create(float tension) {
        return tension == 2.0f ? Interpolator.OVERSHOOT : new OvershootInterpolator(tension);
    }

    @Override
    public float getInterpolation(float t) {
        t -= 1.0f;
        return t * t * ((mTension + 1) * t + mTension) + 1.0f;
    }
}
