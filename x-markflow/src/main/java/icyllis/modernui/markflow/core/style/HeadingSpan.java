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
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.text.FontMetricsInt;
import icyllis.modernui.markflow.MarkflowTheme;
import icyllis.modernui.text.Layout;
import icyllis.modernui.text.Spanned;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.text.style.LeadingMarginSpan;
import icyllis.modernui.text.style.LineHeightSpan;
import icyllis.modernui.text.style.MetricAffectingSpan;

public class HeadingSpan extends MetricAffectingSpan
        implements LeadingMarginSpan, LineHeightSpan {

    private final MarkflowTheme mTheme;
    private final int mLevel;

    public HeadingSpan(@NonNull MarkflowTheme theme, int level) {
        mTheme = theme;
        mLevel = level;
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint paint) {
        Typeface typeface = mTheme.getHeadingTypeface();
        if (typeface != null) {
            paint.setTypeface(typeface);
        }
        int size = mTheme.getHeadingTextSize(mLevel);
        if (size > 0) {
            paint.setTextSize(size);
        } else {
            float multiplier = mTheme.getHeadingTextSizeMultiplier(mLevel);
            paint.setTextSize(paint.getTextSize() * multiplier);
        }
        paint.setTextStyle(paint.getTextStyle() | mTheme.getHeadingTextStyle());
    }

    @Override
    public void drawMargin(@NonNull Canvas c, @NonNull TextPaint p,
                           int left, int right, int dir,
                           int top, int baseline, int bottom,
                           @NonNull Spanned text, int start, int end,
                           boolean first, @NonNull Layout layout) {
        int color = mTheme.getHeadingBreakColor();
        if (color != 0 && (mLevel == 1 || mLevel == 2) && text.getSpanEnd(this) == end) {
            var oldStyle = p.getStyle();
            int oldColor = p.getColor();

            p.setStyle(Paint.FILL);
            p.setColor(color);
            int height = mTheme.getHeadingBreakHeight();
            float mid;
            if (height > 0) {
                mid = height * 0.5f;
            } else {
                mid = p.getTextSize() / 32f;
            }
            float cy = bottom - mid;
            c.drawRect(left, cy - mid, right, cy + mid, p);

            p.setStyle(oldStyle);
            p.setColor(oldColor);
        }
    }

    @Override
    public void chooseHeight(CharSequence text, int start, int end,
                             int spanstartv, int lineHeight,
                             FontMetricsInt fm, TextPaint paint) {
        fm.descent += (int) (fm.descent * 0.5f);
    }
}
