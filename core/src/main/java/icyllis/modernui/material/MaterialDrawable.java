/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.material;

import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.util.ColorStateList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Base drawable that used for blending with white vectors.
 */
public abstract class MaterialDrawable extends Drawable {

    protected ColorStateList mTint;
    protected int mColor = ~0;
    protected int mAlpha = 255;

    public static int modulateAlpha(int paintAlpha, int alpha) {
        return paintAlpha * (alpha & 0xFF) >>> 8;
    }

    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        if (mTint != tint) {
            mTint = tint;
            if (tint != null) {
                mColor = tint.getColorForState(getState(), ~0);
            } else {
                mColor = ~0;
            }
            invalidateSelf();
        }
    }

    @Override
    protected boolean onStateChange(@Nonnull int[] stateSet) {
        if (mTint != null) {
            mColor = mTint.getColorForState(stateSet, ~0);
            return true;
        }
        return false;
    }

    @Override
    public boolean isStateful() {
        return super.isStateful() || (mTint != null && mTint.isStateful());
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return mTint != null && mTint.hasFocusStateSpecified();
    }

    @Override
    public void setAlpha(int alpha) {
        if (mAlpha != alpha) {
            mAlpha = alpha;
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return mAlpha;
    }
}
