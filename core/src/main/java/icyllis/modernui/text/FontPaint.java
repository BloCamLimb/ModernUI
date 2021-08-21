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
import java.awt.*;
import java.util.Locale;
import java.util.Objects;

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

    private static final int FONT_STYLE_MASK = REGULAR | BOLD | ITALIC;

    // shared pointer
    protected Typeface mTypeface;
    protected Locale mLocale;
    protected int mFontStyle = REGULAR;
    protected int mFontSize = 24;

    public FontPaint() {
        //TODO replace with current user preference
        mTypeface = Typeface.PREFERENCE;
        mLocale = ModernUI.get().getSelectedLocale();
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
        mFontStyle = paint.mFontStyle;
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
        mFontStyle = (fontStyle & ~FONT_STYLE_MASK) == 0 ? fontStyle : 0;
    }

    /**
     * Get the font's style.
     *
     * @return the style of the font
     */
    public int getFontStyle() {
        return mFontStyle;
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

        if (mFontStyle != that.mFontStyle) return false;
        if (mFontSize != that.mFontSize) return false;
        if (!Objects.equals(mTypeface, that.mTypeface))
            return false;
        return Objects.equals(mLocale, that.mLocale);
    }

    @Override
    public int hashCode() {
        int result = mTypeface != null ? mTypeface.hashCode() : 0;
        result = 31 * result + (mLocale != null ? mLocale.hashCode() : 0);
        result = 31 * result + mFontStyle;
        result = 31 * result + mFontSize;
        return result;
    }

    @Override
    public String toString() {
        return "FontPaint{" +
                "mTypeface=" + mTypeface +
                ", mLocale=" + mLocale +
                ", mFontStyle=" + mFontStyle +
                ", mFontSize=" + mFontSize +
                '}';
    }
}
