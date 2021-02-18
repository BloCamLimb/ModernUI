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
import icyllis.modernui.graphics.font.FontCollection;
import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Locale;

/**
 * This class holds data used during text measuring and drawing.
 */
public class TextPaint {

    /**
     * Bit flag used with fontStyle to request the plain/regular/normal style
     */
    public static final int REGULAR = Font.PLAIN;

    /**
     * Bit flag used with fontStyle to request the bold style
     */
    public static final byte BOLD = Font.BOLD;

    /**
     * Bit flag used with fontStyle to request the italic style
     */
    public static final byte ITALIC = Font.ITALIC;

    private static final byte TEXT_STYLE_MASK = REGULAR | BOLD | ITALIC;

    private FontCollection mFontCollection;
    private Locale mLocale;
    private int mFontStyle;

    public TextPaint() {
        //TODO replace with current user preference
        mFontCollection = FontCollection.SANS_SERIF;
        mLocale = ModernUI.get().getSelectedLocale();
    }

    private TextPaint(TextPaint t) {
        set(t);
    }

    /**
     * Copy the data from paint into this TextPaint
     */
    public void set(@Nonnull TextPaint paint) {
        mFontCollection = paint.mFontCollection;
        mLocale = paint.mLocale;
        mFontStyle = paint.mFontStyle;
    }

    /**
     * Creates a copy of current instance. (A shallow clone)
     */
    public TextPaint copy() {
        return new TextPaint(this);
    }

    /**
     * Set the font collection object to draw the text.
     *
     * @param fontCollection the font collection
     */
    public void setFontCollection(@Nonnull FontCollection fontCollection) {
        mFontCollection = fontCollection;
    }

    @Nonnull
    public FontCollection getFontCollection() {
        return mFontCollection;
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
    public void setFontStyle(@MagicConstant(flags = {REGULAR, BOLD, ITALIC}) int fontStyle) {
        mFontStyle = (fontStyle & ~TEXT_STYLE_MASK) == 0 ? fontStyle : 0;
    }

    /**
     * Get the font's style.
     *
     * @return the style of the font
     */
    public int getFontStyle() {
        return mFontStyle;
    }
}
