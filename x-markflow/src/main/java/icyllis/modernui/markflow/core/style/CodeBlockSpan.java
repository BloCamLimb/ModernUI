/*
 * Modern UI.
 * Copyright (C) 2023-2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.markflow.core.style;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.markflow.MarkflowTheme;
import icyllis.modernui.text.Layout;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.text.style.*;

public class CodeBlockSpan extends MetricAffectingSpan
        implements LeadingMarginSpan, TrailingMarginSpan {

    private final MarkflowTheme mTheme;

    public CodeBlockSpan(MarkflowTheme theme) {
        mTheme = theme;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint paint) {
        super.updateDrawState(paint);
        int color = mTheme.getCodeBlockTextColor();
        if (color != 0) {
            paint.setColor(color);
        }
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint paint) {
        paint.setTypeface(mTheme.getCodeBlockTypeface());
        int textSize = mTheme.getCodeBlockTextSize();
        if (textSize > 0) {
            paint.setTextSize(textSize);
        } else {
            paint.setTextSize(paint.getTextSize() * 0.875F);
        }
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return mTheme.getCodeBlockMargin();
    }

    @Override
    public int getTrailingMargin() {
        return mTheme.getCodeBlockMargin();
    }

    @Override
    public void drawLeadingMargin(Canvas c, TextPaint p, int x, int dir, int top, int baseline, int bottom,
                                  CharSequence text, int start, int end, boolean first, Layout layout) {
    }

    @Override
    public void drawMargin(Canvas c, TextPaint p, int left, int right, int dir, int top, int baseline, int bottom,
                           CharSequence text, int start, int end, boolean first, Layout layout) {
        final int color = p.getColor();
        p.setColor(mTheme.getCodeBlockBackgroundColor());
        c.drawRect(left, top, right, bottom, p);
        p.setColor(color);
    }
}
