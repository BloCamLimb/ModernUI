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

import icyllis.modernui.text.TextPaint;

import javax.annotation.Nonnull;

/**
 * Changes the color of the text to which the span is attached.
 */
public class ForegroundColorSpan extends CharacterStyle implements UpdateAppearance {

    private final int mColor;

    /**
     * Creates a {@link ForegroundColorSpan} from a color integer.
     *
     * @param color color integer that defines the text color
     */
    public ForegroundColorSpan(int color) {
        mColor = color;
    }

    /**
     * @return the foreground color of this span.
     * @see ForegroundColorSpan#ForegroundColorSpan(int)
     */
    public int getForegroundColor() {
        return mColor;
    }

    @Override
    public void updateDrawState(@Nonnull TextPaint paint) {
        paint.setColor(mColor);
    }
}
