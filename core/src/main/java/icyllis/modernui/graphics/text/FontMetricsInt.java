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

/**
 * Also used as Font Extent (just ascent &amp; descent).
 */
public class FontMetricsInt {

    /**
     * The recommended distance above the baseline for singled spaced text.
     * This is always a negative integer.
     */
    public int ascent;

    /**
     * The recommended distance below the baseline for singled spaced text.
     * This is always a positive integer.
     */
    public int descent;

    /**
     * The recommended additional space to add between lines of text.
     */
    public int leading;

    public FontMetricsInt() {
    }

    public void reset() {
        ascent = descent = leading = 0;
    }

    public void extendBy(@Nonnull FontMetricsInt fm) {
        extendBy(fm.ascent, fm.descent, fm.leading);
    }

    public void extendBy(int ascent, int descent) {
        this.ascent = Math.min(this.ascent, ascent); // negative
        this.descent = Math.max(this.descent, descent); // positive
    }

    public void extendBy(int ascent, int descent, int leading) {
        extendBy(ascent, descent);
        this.leading = Math.max(this.leading, leading); // positive
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FontMetricsInt that = (FontMetricsInt) o;

        if (ascent != that.ascent) return false;
        if (descent != that.descent) return false;
        return leading == that.leading;
    }

    @Override
    public int hashCode() {
        int result = ascent;
        result = 31 * result + descent;
        result = 31 * result + leading;
        return result;
    }

    @Override
    public String toString() {
        return "FontMetricsInt{" +
                "ascent=" + ascent +
                ", descent=" + descent +
                ", leading=" + leading +
                '}';
    }
}
