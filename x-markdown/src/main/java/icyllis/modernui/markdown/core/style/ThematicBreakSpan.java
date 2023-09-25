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

package icyllis.modernui.markdown.core.style;

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.markdown.MarkdownTheme;
import icyllis.modernui.text.Layout;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.text.style.LeadingMarginSpan;

public class ThematicBreakSpan implements LeadingMarginSpan {

    private final MarkdownTheme mTheme;

    public ThematicBreakSpan(MarkdownTheme theme) {
        mTheme = theme;
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return 0;
    }

    @Override
    public void drawLeadingMargin(Canvas c, TextPaint p, int x, int dir, int top, int baseline, int bottom,
                                  CharSequence text, int start, int end, boolean first, Layout layout) {
    }

    @Override
    public void drawMargin(Canvas c, TextPaint p, int left, int right, int dir, int top, int baseline, int bottom,
                           CharSequence text, int start, int end, boolean first, Layout layout) {
        var style = p.getStyle();
        int color = p.getColor();

        p.setStyle(Paint.FILL);
        p.setColor(mTheme.getThematicBreakColor());
        float cy = (top + bottom) / 2f;
        float mid = p.getTextSize() / 6f;
        c.drawRect(left, cy - mid, right, cy + mid, p);

        p.setStyle(style);
        p.setColor(color);
    }
}
