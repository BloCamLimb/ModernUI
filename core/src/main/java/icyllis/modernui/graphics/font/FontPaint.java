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

package icyllis.modernui.graphics.font;

import icyllis.modernui.ModernUI;
import icyllis.modernui.graphics.MathUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.Locale;

/**
 * The base paint used with glyph layout engine at lower levels.
 * See the subclass for public use.
 */
public class FontPaint {

    /**
     * Bit flag used with fontStyle to request the plain/regular/normal style
     */
    public static final int PLAIN = Font.PLAIN;
    public static final int NORMAL = PLAIN;     // alias
    public static final int REGULAR = PLAIN;    // alias

    /**
     * Bit flag used with fontStyle to request the bold style
     */
    public static final int BOLD = Font.BOLD;

    /**
     * Bit flag used with fontStyle to request the italic style
     */
    public static final int ITALIC = Font.ITALIC;

    /**
     * Font style constant to request the bold and italic style
     */
    public static final int BOLD_ITALIC = BOLD | ITALIC;

    public static final int FONT_STYLE_MASK = PLAIN | BOLD | ITALIC;

    // shared pointer
    protected FontCollection mFontCollection;
    Locale mLocale;
    protected int mFlags;
    int mFontSize;

    public FontPaint() {
        mFontCollection = ModernUI.getSelectedTypeface().getFontCollection();
        mLocale = ModernUI.getSelectedLocale();
        mFlags = REGULAR;
        mFontSize = 12;
    }

    public FontPaint(@Nonnull FontPaint paint) {
        mFontCollection = paint.mFontCollection;
        mLocale = paint.mLocale;
        mFlags = paint.mFlags;
        mFontSize = paint.mFontSize;
    }

    /**
     * Copy the data from paint into this TextPaint
     */
    public void set(@Nonnull FontPaint paint) {
        mFontCollection = paint.mFontCollection;
        mLocale = paint.mLocale;
        mFlags = paint.mFlags;
        mFontSize = paint.mFontSize;
    }

    /**
     * Set the text locale.
     * <p>
     * A Locale may affect word break, line break, grapheme cluster break, etc.
     * The locale should match the language of the text to be drawn or user preference,
     * by default, the selected locale should be used {@link ModernUI#getSelectedLocale()}.
     *
     * @param locale the paint's locale value for drawing text, must not be null.
     */
    public void setTextLocale(@Nonnull Locale locale) {
        if (!locale.equals(mLocale)) {
            mLocale = locale;
        }
    }

    /**
     * Get the text's Locale.
     *
     * @return the paint's Locale used for measuring and drawing text, never null.
     */
    @Nonnull
    public Locale getTextLocale() {
        return mLocale;
    }

    /**
     * Set font's style. Combination of REGULAR, BOLD and ITALIC.
     *
     * @param fontStyle the style of the font
     */
    public void setFontStyle(int fontStyle) {
        if ((fontStyle & ~FONT_STYLE_MASK) == 0) {
            mFlags |= fontStyle;
        } else {
            mFlags &= ~FONT_STYLE_MASK;
        }
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
     * Return the paint's text size, in points.
     *
     * @return the paint's text size in pixel units.
     */
    public int getFontSize() {
        return mFontSize;
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
        mFontSize = MathUtil.clamp(fontSize, 8, 96);
    }

    /**
     * Returns true of the passed {@link FontPaint} will have the different effect on text measurement
     *
     * @param paint the paint to compare with
     * @return true if given {@link FontPaint} has the different effect on text measurement.
     */
    public boolean isMetricAffecting(@Nonnull FontPaint paint) {
        if (mFontSize != paint.mFontSize)
            return true;
        if ((mFlags & FONT_STYLE_MASK) != (paint.mFlags & FONT_STYLE_MASK))
            return true;
        if (!mFontCollection.equals(paint.mFontCollection))
            return true;
        return !mLocale.equals(paint.mLocale);
    }

    @Nonnull
    public FontMetricsInt getFontMetricsInt() {
        FontMetricsInt fm = new FontMetricsInt();
        getFontMetricsInt(fm);
        return fm;
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
        return GlyphManager.getInstance().getFontMetrics(this, fm);
    }

    /**
     * Create a copy of this paint as the base class paint for internal
     * layout engine. Subclasses must ensure that be immutable.
     *
     * @return a internal paint
     */
    @Nonnull
    public FontPaint toBase() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FontPaint that = (FontPaint) o;

        if (mFontSize != that.mFontSize) return false;
        if ((mFlags & FONT_STYLE_MASK) != (that.mFlags & FONT_STYLE_MASK)) return false;
        if (!mFontCollection.equals(that.mFontCollection)) return false;
        return mLocale.equals(that.mLocale);
    }

    @Override
    public int hashCode() {
        int h = mFontCollection.hashCode();
        h = 31 * h + mLocale.hashCode();
        h = 31 * h + (mFlags & FONT_STYLE_MASK);
        h = 31 * h + mFontSize;
        return h;
    }

    @Override
    public String toString() {
        return "FontPaint{" +
                "typeface=" + mFontCollection +
                ", locale=" + mLocale +
                ", flags=0x" + Integer.toHexString(mFlags) +
                ", fontSize=" + mFontSize +
                '}';
    }
}
