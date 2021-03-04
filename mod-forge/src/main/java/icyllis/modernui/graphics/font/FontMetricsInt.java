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

package icyllis.modernui.graphics.font;

import javax.annotation.Nonnull;
import java.awt.*;

public class FontMetricsInt {

    /**
     * The recommended distance above the baseline for singled spaced text.
     * This is always a positive integer.
     */
    public int mAscent;

    /**
     * The recommended distance below the baseline for singled spaced text.
     * This is always a positive integer.
     */
    public int mDescent;

    public void reset() {
        mAscent = mDescent = 0;
    }

    public void extendBy(@Nonnull FontMetrics fm) {
        extendBy(fm.getAscent(), fm.getDescent());
    }

    public void extendBy(@Nonnull FontMetricsInt fm) {
        extendBy(fm.mAscent, fm.mDescent);
    }

    public void extendBy(int ascent, int descent) {
        mAscent = Math.max(mAscent, ascent); // positive
        mDescent = Math.max(mDescent, descent); // positive
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FontMetricsInt that = (FontMetricsInt) o;

        if (mAscent != that.mAscent) return false;
        return mDescent == that.mDescent;
    }

    @Override
    public int hashCode() {
        int result = mAscent;
        result = 31 * result + mDescent;
        return result;
    }
}
