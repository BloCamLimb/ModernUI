/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

public class Font {

    /**
     * Text antialiasing mode.
     */
    public static final int
            kAlias_Edging = 0,
            kAntiAlias_Edging = 1;

    static final int
            kLinearMetrics_Flag = 0x4;

    private Typeface mTypeface;
    private float mSize;
    private byte mFlags;
    private byte mEdging;

    public void setTypeface(Typeface typeface) {
        mTypeface = typeface;
    }

    public Typeface getTypeface() {
        return mTypeface;
    }

    public void setSize(float size) {
        mSize = Math.max(0, size);
    }

    public float getSize() {
        return mSize;
    }

    public void setEdging(byte edging) {
        mEdging = edging;
    }

    public byte getEdging() {
        return mEdging;
    }

    /**
     * Returns true if font and glyph metrics are requested to be linearly scalable.
     *
     * @return true if font and glyph metrics are requested to be linearly scalable.
     */
    public boolean isLinearMetrics() {
        return (mFlags & kLinearMetrics_Flag) != 0;
    }
}
