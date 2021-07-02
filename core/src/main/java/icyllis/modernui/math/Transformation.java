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

package icyllis.modernui.math;

import javax.annotation.Nonnull;

/**
 * Transformation is used for setting each transformation property individually
 * and composing them into a transform matrix when necessary.
 */
public class Transformation {

    private final Matrix4 mMatrix = Matrix4.identity();

    private boolean mDirty = false;

    private final Vector3 mTranslation = new Vector3();

    /**
     * Sets the translation value for the transformation on the X axis.
     *
     * @param translationX The X axis translation value of the transformation
     * @return true if the value changed, otherwise false
     * @see #getTranslationX()
     */
    public boolean setTranslationX(float translationX) {
        if (MathUtil.approxEqual(translationX, mTranslation.x)) {
            return false;
        }
        mTranslation.x = translationX;
        mDirty = true;
        return true;
    }

    /**
     * Returns the translation value for the transformation on the X axis.
     *
     * @see #setTranslationX(float)
     */
    public float getTranslationX() {
        return mTranslation.x;
    }

    private void compute() {

    }

    /**
     * Returns the matrix representing the transformation, recomputes the matrix
     * if necessary.
     *
     * @return the transform matrix for this transformation
     */
    @Nonnull
    public Matrix4 getMatrix() {
        if (mDirty) {
            compute();
            mDirty = false;
        }
        return mMatrix;
    }
}
