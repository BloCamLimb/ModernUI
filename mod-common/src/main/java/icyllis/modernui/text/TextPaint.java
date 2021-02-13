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

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * This class holds data used during text measuring and drawing.
 */
public class TextPaint {

    private Locale mLocale;

    public TextPaint() {
    }

    /**
     * Copy the data from paint into this TextPaint
     */
    public void set(TextPaint paint) {

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
}
