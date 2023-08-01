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

import icyllis.arc3d.core.MathUtil;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.util.Locale;

/**
 * The base paint used with layout engine at lower levels.
 */
public class FontPaint {

    /**
     * Bit flag used with fontStyle to request the plain/normal style
     */
    public static final int PLAIN = java.awt.Font.PLAIN;
    public static final int NORMAL = PLAIN;     // alias

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

    public static final int FONT_STYLE_MASK = PLAIN | BOLD | ITALIC;

    public static final int RENDER_FLAG_ANTI_ALIAS = 0x1;
    public static final int RENDER_FLAG_LINEAR_METRICS = 0x2;

    public static final int RENDER_FLAG_MASK = 0x3;
    public static final int RENDER_FLAG_SHIFT = 4;

    // shared pointer
    FontCollection mFont;
    Locale mLocale;
    int mFlags;
    int mSize;

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
     * Set the paint's text size, in points. This value clamps to 8 and 96.
     * You can have even larger glyphs through matrix transformation, and our engine
     * will attempt to use SDF text rendering.
     * <p>
     * Note: the point size is measured at 72 dpi, while Windows has 96 dpi.
     * This indicates that the font size 12 in MS Word is equal to the font size 16 here.
     *
     * @param fontSize set the paint's text size in pixel units.
     */
    public void setFontSize(int fontSize) {
        // our engine assumes 8..96, do not edit
        //TODO Remove this restriction, once our rendering engine updates
        mSize = MathUtil.clamp(fontSize, 8, 96);
    }

    /**
     * Return the paint's text size, in points.
     *
     * @return the paint's text size in pixel units.
     */
    public int getFontSize() {
        return mSize;
    }

    public void setRenderFlags(int flags) {
        mFlags = (mFlags & ~(RENDER_FLAG_MASK << RENDER_FLAG_SHIFT)) |
                ((flags & RENDER_FLAG_MASK) << RENDER_FLAG_SHIFT);
    }

    public int getRenderFlags() {
        return (mFlags >> RENDER_FLAG_SHIFT) & RENDER_FLAG_MASK;
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
        int ascent = 0, descent = 0, leading = 0;
        for (FontFamily family : getFont().getFamilies()) {
            java.awt.Font font = family.chooseFont(getFontStyle(), getFontSize());
            FontMetrics metrics = LayoutPiece.sGraphics[getRenderFlags()].getFontMetrics(font);
            ascent = Math.max(ascent, metrics.getAscent()); // positive
            descent = Math.max(descent, metrics.getDescent()); // positive
            leading = Math.max(leading, metrics.getLeading()); // positive
        }
        if (fm != null) {
            fm.ascent = -ascent;
            fm.descent = descent;
            fm.leading = leading;
        }
        return ascent + descent + leading;
    }

    @ApiStatus.Internal
    public static FontRenderContext getFontRenderContext(int renderFlags) {
        return LayoutPiece.sGraphics[renderFlags].getFontRenderContext();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FontPaint that = (FontPaint) o;

        if (mSize != that.mSize) return false;
        if (mFlags != that.mFlags) return false;
        if (!mFont.equals(that.mFont)) return false;
        return mLocale.equals(that.mLocale);
    }

    @Override
    public int hashCode() {
        int h = mFont.hashCode();
        h = 31 * h + mLocale.hashCode();
        h = 31 * h + mFlags;
        h = 31 * h + mSize;
        return h;
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
