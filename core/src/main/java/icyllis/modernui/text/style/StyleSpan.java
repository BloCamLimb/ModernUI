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

package icyllis.modernui.text.style;

import icyllis.modernui.text.FontPaint;

import javax.annotation.Nonnull;

/**
 * Span that allows setting the style of the text it's attached to.
 * Possible styles are: {@link FontPaint#REGULAR}, {@link FontPaint#BOLD},
 * {@link FontPaint#ITALIC} and {@link FontPaint#BOLD_ITALIC}.
 * <p>
 * Note that styles are cumulative -- if both bold and italic are set in
 * separate spans, or if the base style is bold and a span calls for italic,
 * you get bold italic.  You can't turn off a style from the base style.
 */
public class StyleSpan extends MetricAffectingSpan {

    private final int mStyle;

    /**
     * Creates a {@link StyleSpan} from a style.
     *
     * @param style An integer constant describing the style for this span. Examples
     *              include bold, italic, and regular. Values are constants defined
     *              in {@link FontPaint}.
     */
    public StyleSpan(int style) {
        mStyle = (style & ~FontPaint.FONT_STYLE_MASK) == 0 ? style : FontPaint.REGULAR;
    }

    /**
     * Returns the style constant defined in {@link FontPaint}.
     */
    public int getStyle() {
        return mStyle;
    }

    @Override
    public void updateMeasureState(@Nonnull FontPaint paint) {
        paint.setFontStyle(paint.getFontStyle() | mStyle);
    }
}
