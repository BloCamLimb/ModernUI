/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.text.style;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.text.Layout;
import icyllis.modernui.text.TextPaint;

public class QuoteSpan implements LeadingMarginSpan {

    private final int mBlockMargin;

    private final int mStripeWidth;

    private final int mColor;

    public QuoteSpan(int blockMargin, int stripeWidth, int color) {
        mBlockMargin = blockMargin;
        mStripeWidth = stripeWidth;
        mColor = color;
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return mBlockMargin;
    }

    @Override
    public void drawLeadingMargin(@NonNull Canvas c, @NonNull TextPaint p,
                                  int x, int dir,
                                  int top, int baseline, int bottom,
                                  @NonNull CharSequence text, int start, int end,
                                  boolean first, @NonNull Layout layout) {
        var style = p.getStyle();
        var color = p.getColorLong();

        p.setStyle(Paint.FILL);
        p.setColor(mColor);

        c.drawRect(x, top, x + dir * mStripeWidth, bottom, p);

        p.setStyle(style);
        p.setColor(color);
    }
}
