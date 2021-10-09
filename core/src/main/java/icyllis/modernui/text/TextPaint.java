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
 * For the base class {@link FontPaint}, changing any attributes will require a
 * reflow and re-layout, not just re-drawing.
 */
public class TextPaint extends FontPaint {

    /**
     * Paint flag that applies an underline decoration to drawn text.
     */
    public static final int UNDERLINE_FLAG = 0x08;

    /**
     * Paint flag that applies a strike-through decoration to drawn text.
     */
    public static final int STRIKETHROUGH_FLAG = 0x10;

    private static final Pool<TextPaint> sPool = Pools.concurrent(4);

    private int mColor;

    // 0 means no background
    public int bgColor;

    /**
     * Creates the new TextPaint.
     */
    public TextPaint() {
        mColor = ~0;
    }

    /**
     * Returns a TextPaint from the shared pool, a {@link #set(TextPaint)} is
     * expected before use and a {@link #recycle()} after use.
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
        mColor = paint.mColor;
        bgColor = paint.bgColor;
    }

    /**
     * Return the paint's color in sRGB. Note that the color is a 32bit value
     * containing alpha as well as r,g,b. This 32bit value is not premultiplied,
     * meaning that its alpha can be any value, regardless of the values of
     * r,g,b. See the Color class for more details.
     *
     * @return the paint's color (and alpha).
     */
    public int getColor() {
        return mColor;
    }

    /**
     * Set the paint's color. Note that the color is an int containing alpha
     * as well as r,g,b. This 32bit value is not premultiplied, meaning that
     * its alpha can be any value, regardless of the values of r,g,b.
     * See the Color class for more details.
     *
     * @param color The new color (including alpha) to set in the paint.
     */
    public void setColor(int color) {
        mColor = color;
    }

    /**
     * Helper for getFlags(), returning true if UNDERLINE_TEXT_FLAG bit is set
     *
     * @return true if the underlineText bit is set in the paint's flags.
     * @see #setUnderline(boolean)
     */
    public final boolean isUnderline() {
        return (mFlags & UNDERLINE_FLAG) != 0;
    }

    /**
     * Helper for setFlags(), setting or clearing the UNDERLINE_TEXT_FLAG bit
     *
     * @param underline true to set the underline bit in the paint's
     *                  flags, false to clear it.
     * @see #isUnderline()
     */
    public void setUnderline(boolean underline) {
        if (underline) {
            mFlags |= UNDERLINE_FLAG;
        } else {
            mFlags &= ~UNDERLINE_FLAG;
        }
    }

    /**
     * Returns the distance from top of the underline to the baseline in pixels.
     * <p>
     * The result is positive for positions that are below the baseline.
     * This method returns where the underline should be drawn independent of if the {@link
     * #UNDERLINE_FLAG} bit is set.
     *
     * @return the position of the underline in pixels
     * @see #isUnderline()
     * @see #getUnderlineThickness(FontMetricsInt)
     * @see #setUnderline(boolean)
     */
    public float getUnderlineOffset(@Nonnull FontMetricsInt fm) {
        return fm.descent / 3f;
    }

    /**
     * Returns the thickness of the underline in pixels.
     *
     * @return the thickness of the underline in pixels
     * @see #isUnderline()
     * @see #getUnderlineOffset(FontMetricsInt)
     * @see #setUnderline(boolean)
     */
    public float getUnderlineThickness(@Nonnull FontMetricsInt fm) {
        return fm.ascent / 12f;
    }

    /**
     * Helper for getFlags(), returning true if STRIKE_THRU_TEXT_FLAG bit is set
     *
     * @return true if the {@link #STRIKETHROUGH_FLAG} bit is set in the paint's flags.
     * @see #setStrikethrough(boolean)
     */
    public final boolean isStrikethrough() {
        return (mFlags & STRIKETHROUGH_FLAG) != 0;
    }

    /**
     * Helper for setFlags(), setting or clearing the STRIKE_THRU_TEXT_FLAG bit
     *
     * @param strikethrough true to set the strikethrough bit in the paint's
     *                      flags, false to clear it.
     * @see #isStrikethrough()
     */
    public void setStrikethrough(boolean strikethrough) {
        if (strikethrough) {
            mFlags |= STRIKETHROUGH_FLAG;
        } else {
            mFlags &= ~STRIKETHROUGH_FLAG;
        }
    }

    /**
     * Distance from top of the strike-through line to the baseline in pixels.
     * <p>
     * The result is negative for positions that are above the baseline.
     * This method returns where the strike-through line should be drawn independent of if the
     * {@link #STRIKETHROUGH_FLAG} bit is set.
     *
     * @return the position of the strike-through line in pixels
     * @see #getStrikethroughThickness(FontMetricsInt)
     */
    public float getStrikethroughOffset(@Nonnull FontMetricsInt fm) {
        return -fm.ascent / 2f;
    }

    /**
     * Returns the thickness of the strike-through line in pixels.
     *
     * @return the position of the strike-through line in pixels
     * @see #getStrikethroughOffset(FontMetricsInt)
     */
    public float getStrikethroughThickness(@Nonnull FontMetricsInt fm) {
        return fm.ascent / 12f;
    }

    /**
     * Create a copy of this paint as the base class paint for internal
     * layout engine. Subclasses must ensure that be immutable.
     *
     * @return a internal paint
     */
    @Nonnull
    @Override
    public final FontPaint toBase() {
        return new FontPaint(this);
    }

    /**
     * Recycle this text paint, this object cannot be used anymore after recycling.
     */
    public void recycle() {
        sPool.release(this);
    }
}
