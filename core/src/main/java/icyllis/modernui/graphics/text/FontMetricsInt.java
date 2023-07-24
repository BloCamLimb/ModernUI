/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.text;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * Also known as Font Extent.
 */
public class FontMetricsInt {

    /**
     * The recommended distance above the baseline for singled spaced text.
     * This is always a positive integer.
     */
    public int ascent;

    /**
     * The recommended distance below the baseline for singled spaced text.
     * This is always a positive integer.
     */
    public int descent;

    public FontMetricsInt() {
    }

    public void reset() {
        ascent = descent = 0;
    }

    public int getAscent() {
        return ascent;
    }

    public int getDescent() {
        return descent;
    }

    public void extendBy(@Nonnull FontMetrics fm) {
        extendBy(fm.getAscent(), fm.getDescent());
    }

    public void extendBy(@Nonnull FontMetricsInt fm) {
        extendBy(fm.ascent, fm.descent);
    }

    public void extendBy(int ascent, int descent) {
        this.ascent = Math.max(this.ascent, ascent); // positive
        this.descent = Math.max(this.descent, descent); // positive
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FontMetricsInt that = (FontMetricsInt) o;

        if (ascent != that.ascent) return false;
        return descent == that.descent;
    }

    @Override
    public int hashCode() {
        int result = ascent;
        result = 31 * result + descent;
        return result;
    }

    @Override
    public String toString() {
        return "FontMetricsInt: " +
                "ascent=" + ascent +
                ", descent=" + descent;
    }
}
