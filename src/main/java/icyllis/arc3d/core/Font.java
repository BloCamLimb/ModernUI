/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2025 BloCamLimb <pocamelards@gmail.com>
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

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Font controls options applied when drawing text.
 */
public class Font {

    /**
     * Text antialiasing mode.
     */
    public static final int
            kAlias_Edging = 0,
            kAntiAlias_Edging = 1;

    // private flags
    private static final int
            kSubpixelPositioning_Flag = 0x2,
            kLinearMetrics_Flag = 0x4;

    private Typeface mTypeface;
    private float mSize;
    private byte mFlags;
    private byte mEdging;

    public Font() {
        mSize = 12;
    }

    public Font(@NonNull Font other) {
        set(other);
    }

    public void set(@NonNull Font other) {
        mTypeface = other.mTypeface;
        mSize = other.mSize;
        mFlags = other.mFlags;
        mEdging = other.mEdging;
    }

    /**
     * Sets SkTypeface to typeface, decreasing SkRefCnt of the previous SkTypeface.
     * Pass nullptr to clear SkTypeface and use an empty typeface (which draws nothing).
     * Increments tf SkRefCnt by one.
     *
     * @param typeface font and style used to draw text
     */
    public void setTypeface(Typeface typeface) {
        mTypeface = typeface;
    }

    /**
     * Returns a raw pointer to Typeface.
     *
     * @return non-null typeface
     */
    public Typeface getTypeface() {
        return mTypeface;
    }

    /**
     * Returns text size in points.
     *
     * @return typographic height of text
     */
    public float getSize() {
        return mSize;
    }

    /**
     * Sets text size in points.
     * Has no effect if size is not greater than or equal to zero.
     *
     * @param size typographic height of text
     */
    public void setSize(float size) {
        if (size >= 0) {
            mSize = size;
        }
    }

    /**
     * Whether edge pixels draw opaque or with partial transparency.
     */
    @MagicConstant(intValues = {kAlias_Edging, kAntiAlias_Edging})
    public int getEdging() {
        return mEdging;
    }

    /**
     * Requests, but does not require, that edge pixels draw opaque or with
     * partial transparency.
     */
    public void setEdging(@MagicConstant(intValues = {kAlias_Edging, kAntiAlias_Edging}) int edging) {
        mEdging = (byte) edging;
    }

    /**
     * Returns true if glyphs may be drawn at sub-pixel offsets.
     *
     * @return true if glyphs may be drawn at sub-pixel offsets.
     */
    public boolean isSubpixel() {
        return (mFlags & kSubpixelPositioning_Flag) != 0;
    }

    /**
     * Returns true if font and glyph metrics are requested to be linearly scalable.
     *
     * @return true if font and glyph metrics are requested to be linearly scalable.
     */
    public boolean isLinearMetrics() {
        return (mFlags & kLinearMetrics_Flag) != 0;
    }

    /**
     * Requests, but does not require, that glyphs respect sub-pixel positioning.
     *
     * @param subpixel setting for sub-pixel positioning
     */
    public void setSubpixel(boolean subpixel) {
        if (subpixel) {
            mFlags |= kSubpixelPositioning_Flag;
        } else {
            mFlags &= ~kSubpixelPositioning_Flag;
        }
    }

    /**
     * Requests, but does not require, linearly scalable font and glyph metrics.
     * <p>
     * For outline fonts 'true' means font and glyph metrics should ignore hinting and rounding.
     * Note that some bitmap formats may not be able to scale linearly and will ignore this flag.
     *
     * @param linearMetrics setting for linearly scalable font and glyph metrics.
     */
    public void setLinearMetrics(boolean linearMetrics) {
        if (linearMetrics) {
            mFlags |= kLinearMetrics_Flag;
        } else {
            mFlags &= ~kLinearMetrics_Flag;
        }
    }

    /**
     * Return the approximate largest dimension of typical text when transformed by the matrix.
     *
     * @param matrix  used to transform size
     * @param centerX location of the text prior to matrix transformation. Used if the
     *                matrix has perspective.
     * @return typical largest dimension
     */
    @ApiStatus.Internal
    public float approximateTransformedFontSize(Matrixc matrix,
                                                float centerX, float centerY) {
        if (matrix.hasPerspective()) {
            float scaleSq = matrix.differentialAreaScale(centerX, centerY);
            if (scaleSq <= MathUtil.EPS || !Float.isFinite(scaleSq)) {
                return -mSize;
            } else {
                return (float) (mSize * Math.sqrt(scaleSq));
            }
        } else {
            return mSize * matrix.getMaxScale();
        }
    }

    @ApiStatus.Internal
    public static final int kCanonicalTextSizeForPaths = 64;

    @ApiStatus.Internal
    public float setupForPaths(Paint paint) {
        setLinearMetrics(true);
        if (paint != null) {
            paint.setStyle(Paint.FILL);
            paint.setPathEffect(null);
        }
        float textSize = mSize;
        setSize(kCanonicalTextSizeForPaths);
        return textSize / kCanonicalTextSizeForPaths;
    }

    @Override
    public int hashCode() {
        int result = mTypeface != null ? mTypeface.hashCode() : 0;
        result = 31 * result + Float.floatToIntBits(mSize);
        result = 31 * result + (int) mFlags;
        result = 31 * result + (int) mEdging;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Font font = (Font) o;

        if (mSize != font.mSize) return false;
        if (mFlags != font.mFlags) return false;
        if (mEdging != font.mEdging) return false;
        return Objects.equals(mTypeface, font.mTypeface);
    }
}
