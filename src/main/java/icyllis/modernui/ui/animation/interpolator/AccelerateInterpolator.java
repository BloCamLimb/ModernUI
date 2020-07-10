/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.ui.animation.interpolator;

import icyllis.modernui.ui.animation.ITimeInterpolator;

public class AccelerateInterpolator implements ITimeInterpolator {

    private final float factor;

    public AccelerateInterpolator(float factor) {
        this.factor = factor;
    }

    @Override
    public float getInterpolation(float progress) {
        if (factor == 1.0f) {
            return progress * progress;
        }
        return (float) Math.pow(progress, factor * 2);
    }
}
