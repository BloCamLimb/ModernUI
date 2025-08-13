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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.text.*;

public class BulletSpan implements LeadingMarginSpan {

    public static final int STYLE_DISC = 0;

    public static final int STYLE_CIRCLE = 1;

    public static final int STYLE_SQUARE = 2;

    private final int mBlockMargin;
    private final int mBulletWidth;
    private final int mColor;
    private final int mStyle;

    public BulletSpan(int blockMargin, int bulletWidth, int color, int style) {
        mBlockMargin = blockMargin;
        mBulletWidth = bulletWidth;
        mColor = color;
        mStyle = style;
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
        if (first && ((Spanned) text).getSpanStart(this) == start) {
            var oldStyle = p.getStyle();
            boolean restoreStrokeWidth = false;
            float oldStrokeWidth = 0;
            boolean restoreColor = mColor != 0;
            int oldColor = restoreColor ? p.getColor() : 0;

            int width = mBlockMargin;
            int height = bottom - top;
            int bulletWidth = Math.min(height, width) / 2;
            if (mBulletWidth != 0) {
                bulletWidth = Math.min(mBulletWidth, bulletWidth);
            } else {
                bulletWidth = bulletWidth / 2;
            }

            float l, r;
            if (dir > 0) {
                l = x + width / 2f;
                r = l + bulletWidth;
            } else {
                r = x - width / 2f;
                l = r - bulletWidth;
            }

            float cy = (top + bottom) / 2f;

            if (restoreColor) {
                p.setColor(mColor);
            }

            float radius = bulletWidth / 2f;
            if (mStyle == STYLE_DISC || mStyle == STYLE_CIRCLE) {
                if (mStyle == STYLE_DISC) {
                    p.setStyle(Paint.FILL);
                } else {
                    p.setStyle(Paint.STROKE);
                    restoreStrokeWidth = true;
                    oldStrokeWidth = p.getStrokeWidth();
                    float sw = Math.max(1, p.getTextSize() / 16f);
                    p.setStrokeWidth(sw);
                    radius -= sw / 2f;
                }
                c.drawCircle((l + r) / 2f, cy, radius, p);
            } else {
                p.setStyle(Paint.FILL);
                c.drawRect(l, cy - radius, r, cy + radius, p);
            }

            p.setStyle(oldStyle);
            if (restoreStrokeWidth) {
                p.setStrokeWidth(oldStrokeWidth);
            }
            if (restoreColor) {
                p.setColor(oldColor);
            }
        }
    }
}
