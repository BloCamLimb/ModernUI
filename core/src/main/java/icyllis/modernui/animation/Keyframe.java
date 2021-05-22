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

public abstract class Keyframe {

    private Interpolator mInterpolator;
    private float mFraction;

    /**
     * Gets the value for this Keyframe.
     *
     * @return The value for this Keyframe.
     */
    public abstract Object getValue();

    /**
     * Sets the value for this Keyframe.
     *
     * @param value value for this Keyframe.
     */
    public abstract void setValue(Object value);

    /**
     * Gets the time for this keyframe, as a fraction of the overall animation duration.
     *
     * @return The time associated with this keyframe, as a fraction of the overall animation
     * duration. This should be a value between 0 and 1.
     */
    public float getFraction() {
        return mFraction;
    }

    /**
     * Sets the time for this keyframe, as a fraction of the overall animation duration.
     *
     * @param fraction time associated with this keyframe, as a fraction of the overall animation
     * duration. This should be a value between 0 and 1.
     */
    public void setFraction(float fraction) {
        mFraction = fraction;
    }

    /**
     * Gets the optional interpolator for this Keyframe. A value of <code>null</code> indicates
     * that there is no interpolation, which is the same as linear interpolation.
     *
     * @return The optional interpolator for this Keyframe.
     */
    public Interpolator getInterpolator() {
        return mInterpolator;
    }
}
