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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.MathUtil;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

/**
 * The base paint used with layout engine at lower levels.
 */
public class FontPaint {

    /**
     * Bit flag used with fontStyle to request the normal/regular style
     */
    public static final int NORMAL = java.awt.Font.PLAIN;

    /**
     * Bit flag used with fontStyle to request the bold style
     */
    public static final int BOLD = java.awt.Font.BOLD;

    /**
     * Bit flag used with fontStyle to request the italic style
     */
    public static final int ITALIC = java.awt.Font.ITALIC;

    /**
     * Font style constant to request the bold and italic style
     */
    public static final int BOLD_ITALIC = BOLD | ITALIC;

    public static final int FONT_STYLE_MASK = NORMAL | BOLD | ITALIC;

    public static final float INV_FONT_SIZE_GRANULARITY = 3.0f;

    private static final int RENDER_FLAG_ANTI_ALIAS = 0x10;
    private static final int RENDER_FLAG_LINEAR_METRICS = 0x20;

    // shared pointer
    FontCollection mFont;
    Locale mLocale;
    int mFlags;
    private float mSize;

    @ApiStatus.Internal
    public FontPaint() {
    }

    @ApiStatus.Internal
    public FontPaint(@Nonnull FontPaint paint) {
        mFont = paint.mFont;
        mLocale = paint.mLocale;
        mFlags = paint.mFlags;
        mSize = paint.mSize;
    }

    /**
     * Copy the data from paint into this TextPaint
     */
    public void set(@Nonnull FontPaint paint) {
        mFont = paint.mFont;
        mLocale = paint.mLocale;
        mFlags = paint.mFlags;
        mSize = paint.mSize;
    }

    public void setFont(@Nonnull FontCollection font) {
        mFont = font;
    }

    public FontCollection getFont() {
        return mFont;
    }

    public void setLocale(@Nonnull Locale locale) {
        mLocale = locale;
    }

    public Locale getLocale() {
        return mLocale;
    }

    /**
     * Set font's style. Combination of REGULAR, BOLD and ITALIC.
     *
     * @param fontStyle the style of the font
     */
    public void setFontStyle(int fontStyle) {
        mFlags = (mFlags & ~FONT_STYLE_MASK) | (fontStyle & FONT_STYLE_MASK);
    }

    /**
     * Get the font's style.
     *
     * @return the style of the font
     */
    public int getFontStyle() {
        return mFlags & FONT_STYLE_MASK;
    }

    /**
     * Set the paint's text size in pixel units. For example, a text size
     * of 16 (1em) means the letter 'M' is 16 pixels high in device space.
     * Very large or small sizes will impact rendering performance, and the
     * rendering system might not render text at these sizes. For now, text
     * sizes will clamp to 1 and 2184.
     * <p>
     * Note: the point size is measured at 72 dpi, while Windows has 96 dpi.
     * This indicates that the font size 12 in MS Word is equal to the font size 16 here.
     *
     * @param fontSize set the paint's text size in pixel units.
     */
    public void setFontSize(float fontSize) {
        // our layout engine assumes 1..2184, do not edit
        mSize = MathUtil.pin(getCanonicalFontSize(fontSize), 1, 2184);
    }

    /**
     * Return the paint's text size, in points.
     *
     * @return the paint's text size in pixel units.
     */
    public float getFontSize() {
        return mSize;
    }

    public void setAntiAlias(boolean aa) {
        if (aa) {
            mFlags |= RENDER_FLAG_ANTI_ALIAS;
        } else {
            mFlags &= ~RENDER_FLAG_ANTI_ALIAS;
        }
    }

    public boolean isAntiAlias() {
        return (mFlags & RENDER_FLAG_ANTI_ALIAS) != 0;
    }

    public void setLinearMetrics(boolean linearMetrics) {
        if (linearMetrics) {
            mFlags |= RENDER_FLAG_LINEAR_METRICS;
        } else {
            mFlags &= ~RENDER_FLAG_LINEAR_METRICS;
        }
    }

    public boolean isLinearMetrics() {
        return (mFlags & RENDER_FLAG_LINEAR_METRICS) != 0;
    }

    /**
     * Returns true of the passed {@link FontPaint} will have the different effect on text measurement
     *
     * @param paint the paint to compare with
     * @return true if given {@link FontPaint} has the different effect on text measurement.
     */
    public boolean isMetricAffecting(@Nonnull FontPaint paint) {
        if (mSize != paint.mSize)
            return true;
        if (mFlags != paint.mFlags)
            return true;
        if (!mFont.equals(paint.mFont))
            return true;
        return !mLocale.equals(paint.mLocale);
    }

    /**
     * Return the font's interline spacing, given the Paint's settings for
     * typeface, textSize, etc. If metrics is not null, return the fontmetric
     * values in it. Note: all values have been converted to integers from
     * floats, in such a way has to make the answers useful for both spacing
     * and clipping. If you want more control over the rounding, call
     * getFontMetrics().
     *
     * <p>Note that these are the values for the main typeface, and actual text rendered may need a
     * larger set of values because fallback fonts may get used in rendering the text.
     *
     * @return the font's interline spacing.
     */
    public int getFontMetricsInt(@Nullable FontMetricsInt fm) {
        int height = 0;
        for (FontFamily family : getFont().getFamilies()) {
            var font = family.getClosestMatch(getFontStyle());
            height = Math.max(height, font.getMetrics(this, fm));
        }
        return height;
    }

    /**
     * Populates font attributes to native font object, excluding the typeface.
     *
     * @hidden
     */
    @ApiStatus.Internal
    public void getNativeFont(@NonNull icyllis.arc3d.core.Font nativeFont) {
        nativeFont.setSize(getFontSize());
        nativeFont.setEdging(isAntiAlias()
                ? icyllis.arc3d.core.Font.kAntiAlias_Edging
                : icyllis.arc3d.core.Font.kAlias_Edging);
        nativeFont.setLinearMetrics(isLinearMetrics());
    }

    public static float getCanonicalFontSize(float fontSize) {
        assert fontSize >= 0;
        return (int) (fontSize * INV_FONT_SIZE_GRANULARITY + 0.5f) / INV_FONT_SIZE_GRANULARITY;
    }

    @Override
    public int hashCode() {
        int h = mFont.hashCode();
        h = 31 * h + mLocale.hashCode();
        h = 31 * h + mFlags;
        h = 31 * h + Float.floatToIntBits(mSize);
        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FontPaint that = (FontPaint) o;

        if (mFlags != that.mFlags) return false;
        if (mSize != that.mSize) return false;
        if (!mFont.equals(that.mFont)) return false;
        return mLocale.equals(that.mLocale);
    }

    @Override
    public String toString() {
        return "FontPaint{" +
                "font=" + mFont +
                ", locale=" + mLocale +
                ", flags=0x" + Integer.toHexString(mFlags) +
                ", size=" + mSize +
                '}';
    }
}
