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

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.text.FontMetricsInt;
import icyllis.modernui.graphics.text.FontPaint;
import icyllis.modernui.text.TextPaint;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ReplacementSpan extends MetricAffectingSpan {

    /**
     * This method does nothing, since ReplacementSpans are measured
     * explicitly instead of affecting Paint properties.
     */
    @Override
    public final void updateMeasureState(@Nonnull TextPaint paint) {
    }

    /**
     * Returns the width of the span. Extending classes can set the height of the span by updating
     * attributes of {@link FontMetricsInt}. If the span covers the whole
     * text, and the height is not set,
     * {@link #draw(Canvas, CharSequence, int, int, float, int, int, int, TextPaint)} will not be
     * called for the span.
     *
     * @param paint Paint instance.
     * @param text  Current text.
     * @param start Start character index for span.
     * @param end   End character index for span.
     * @param fm    Font metrics, can be null.
     * @return Width of the span.
     */
    public abstract int getSize(@Nonnull FontPaint paint, CharSequence text,
                                int start, int end, @Nullable FontMetricsInt fm);

    /**
     * Draws the span into the canvas.
     *
     * @param canvas Canvas into which the span should be rendered.
     * @param text   Current text.
     * @param start  Start character index for span.
     * @param end    End character index for span.
     * @param x      Edge of the replacement closest to the leading margin.
     * @param top    Top of the line.
     * @param y      Baseline.
     * @param bottom Bottom of the line.
     * @param paint  Paint instance.
     */
    public abstract void draw(@Nonnull Canvas canvas, CharSequence text,
                              int start, int end, float x, int top, int y, int bottom,
                              @Nonnull TextPaint paint);
}
