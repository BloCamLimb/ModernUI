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

package icyllis.modernui.text.style;

import icyllis.modernui.graphics.text.FontMetricsInt;
import icyllis.modernui.text.TextPaint;

/**
 * The classes that affect the line height of paragraph should implement this interface.
 */
public interface LineHeightSpan extends ParagraphStyle, WrapTogetherSpan {

    /**
     * Classes that implement this should define how the height is being calculated.
     *
     * @param text       the text
     * @param start      the start of the line
     * @param end        the end of the line
     * @param spanstartv the start of the span
     * @param lineHeight the line height
     * @param fm         font metrics of the paint, in integers
     * @param paint      the paint
     */
    void chooseHeight(CharSequence text, int start, int end,
                      int spanstartv, int lineHeight,
                      FontMetricsInt fm, TextPaint paint);
}
