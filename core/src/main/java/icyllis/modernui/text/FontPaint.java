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

package icyllis.modernui.text;

import icyllis.modernui.ModernUI;
import icyllis.modernui.math.MathUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.Locale;

/**
 * The base paint used with text layout engine at lower levels.
 */
public class FontPaint {

    /**
     * Bit flag used with fontStyle to request the plain/regular/normal style
     */
    public static final int REGULAR = Font.PLAIN;

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

    public static final int FONT_STYLE_MASK = REGULAR | BOLD | ITALIC;

    // shared pointer
    Typeface mTypeface;
    Locale mLocale;
    int mFlags;
    int mFontSize;

    public FontPaint() {
        mTypeface = ModernUI.get().getSelectedTypeface();
        mLocale = ModernUI.get().getSelectedLocale();
        mFlags = REGULAR;
        mFontSize = 24;
    }

    public FontPaint(@Nonnull FontPaint paint) {
        set(paint);
    }

    /**
     * Copy the data from paint into this TextPaint
     */
    public void set(@Nonnull FontPaint paint) {
        mTypeface = paint.mTypeface;
        mLocale = paint.mLocale;
        mFlags = paint.mFlags;
        mFontSize = paint.mFontSize;
    }

    /**
     * Set the font collection object to draw the text.
     *
     * @param typeface the font collection
     */
    public void setTypeface(@Nonnull Typeface typeface) {
        mTypeface = typeface;
    }

    @Nonnull
    public Typeface getTypeface() {
        return mTypeface;
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
     * Return the paint's text size.
     *
     * @return the paint's text size in pixel units.
     */
    public int getFontSize() {
        return mFontSize;
    }

    /**
     * Set the paint's text size. This value clamps to 8 and 96.
     *
     * @param fontSize set the paint's text size in pixel units.
     */
    public void setFontSize(int fontSize) {
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
        if (!mTypeface.equals(paint.mTypeface))
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
        if (!mTypeface.equals(that.mTypeface)) return false;
        return mLocale.equals(that.mLocale);
    }

    @Override
    public int hashCode() {
        int h = mTypeface.hashCode();
        h = 31 * h + mLocale.hashCode();
        h = 31 * h + (mFlags & FONT_STYLE_MASK);
        h = 31 * h + mFontSize;
        return h;
    }

    @Override
    public String toString() {
        return "FontPaint{" +
                "typeface=" + mTypeface +
                ", locale=" + mLocale +
                ", flags=0x" + Integer.toHexString(mFlags) +
                ", fontSize=" + mFontSize +
                '}';
    }
}
