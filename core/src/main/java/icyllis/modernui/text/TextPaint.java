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

import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;

import javax.annotation.Nonnull;

/**
 * This class holds data used during text measuring and drawing at higher levels.
 */
public class TextPaint extends FontPaint {

    private static final Pool<TextPaint> sPool = Pools.concurrent(4);

    // the glyph/text/chars/foreground color
    public int color;

    // 0 means no background
    public int bgColor;

    /**
     * Creates the new TextPaint.
     */
    public TextPaint() {
    }

    /**
     * Returns a TextPaint from the shared pool, a {@link #recycle()} is
     * expected after use.
     *
     * @return a pooled object, states are undefined
     */
    @Nonnull
    public static TextPaint obtain() {
        TextPaint paint = sPool.acquire();
        if (paint == null) {
            return new TextPaint();
        }
        return paint;
    }

    /**
     * Copy the data from paint into this TextPaint
     */
    public void set(@Nonnull TextPaint paint) {
        super.set(paint);
        color = paint.color;
        bgColor = paint.bgColor;
    }

    @Nonnull
    @Override
    public FontPaint toBase() {
        return new FontPaint(this);
    }

    /**
     * Calculate font metrics in pixels
     *
     * @param fm a FontMetrics to store the result
     */
    public void getFontMetrics(FontMetricsInt fm) {
        GlyphManager.getInstance().getFontMetrics(mTypeface, this, fm);
    }

    /**
     * Recycle this text paint, this object cannot be used anymore after recycling.
     */
    public void recycle() {
        sPool.release(this);
    }
}
