/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.api.global;

import java.util.function.Function;

public interface IAnimationBuilder {

    /**
     * Set the init value
     * @param init init
     * @return builder
     */
    IAnimationBuilder setInit(float init);

    /**
     * Set the init value
     * @param init init
     * @return builder
     */
    IAnimationBuilder setInit(Function<Integer, Float> init);

    /**
     * Set the target value
     * @param target target
     * @return builder
     */
    IAnimationBuilder setTarget(float target);

    /**
     * Set the target value
     * @param target target
     * @param isVertical same with init value
     * @return builder
     */
    IAnimationBuilder setTarget(Function<Integer, Float> target, boolean isVertical);

    /**
     * Delay animation start after being created
     * @param delay partial ticks (20.0 = 1s)
     * @return builder
     */
    IAnimationBuilder setDelay(float delay);

    /**
     * Set animation duration
     * @param timing partial ticks (20.0 = 1s)
     * @return builder
     */
    IAnimationBuilder setTiming(float timing);

    /**
     * Set motion type. Default is Uniform
     * @param type type
     * @return builder
     */
    IAnimationBuilder setMotion(MotionType type);
}
