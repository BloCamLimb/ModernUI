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

public interface Keyframes {

    /**
     * Sets the TypeEvaluator to be used when calculating animated values. This object
     * is required only for Keyframes that are not either IntKeyframes or FloatKeyframes,
     * both of which assume their own evaluator to speed up calculations with those primitive
     * types.
     *
     * @param evaluator The TypeEvaluator to be used to calculate animated values.
     */
    void setEvaluator(TypeEvaluator<Object> evaluator);

    /**
     * Gets the animated value, given the elapsed fraction of the animation (interpolated by the
     * animation's interpolator) and the evaluator used to calculate in-between values. This
     * function maps the input fraction to the appropriate keyframe interval and a fraction
     * between them and returns the interpolated value. Note that the input fraction may fall
     * outside the [0-1] bounds, if the animation's interpolator made that happen (e.g., a
     * spring interpolation that might send the fraction past 1.0). We handle this situation by
     * just using the two keyframes at the appropriate end when the value is outside those bounds.
     *
     * @param fraction The elapsed fraction of the animation
     * @return The animated value.
     */
    Object getValue(float fraction);

    /**
     * @return The backing array of all Keyframes contained by this. This may return null if this is
     * not made up of Keyframes.
     */
    Keyframe[] getKeyframes();

    interface IntKeyframes extends Keyframes {

        /**
         * Works like {@link #getValue(float)}, but returning a primitive.
         *
         * @param fraction The elapsed fraction of the animation
         * @return The animated value.
         */
        int getIntValue(float fraction);

        @Override
        default Object getValue(float fraction) {
            return getIntValue(fraction);
        }
    }

    interface FloatKeyframes extends Keyframes {

        /**
         * Works like {@link #getValue(float)}, but returning a primitive.
         *
         * @param fraction The elapsed fraction of the animation
         * @return The animated value.
         */
        float getFloatValue(float fraction);

        @Override
        default Object getValue(float fraction) {
            return getFloatValue(fraction);
        }
    }
}
