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

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.graphics.font.CharArrayIterator;
import icyllis.modernui.graphics.font.FontMetricsInt;
import icyllis.modernui.graphics.font.FontPaint;
import icyllis.modernui.graphics.font.GraphemeBreak;
import icyllis.modernui.util.Pools;

import javax.annotation.Nonnull;
import java.util.Locale;

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

    private static final Pools.Pool<TextPaint> sPool = Pools.newSynchronizedPool(4);

    private Typeface mTypeface;
    private int mColor;

    // Special value 0 means no background paint
    @ColorInt
    public int bgColor;
    public int baselineShift;
    @ColorInt
    public int linkColor;
    public float density = 1.0f;
    /**
     * Special value 0 means no custom underline
     */
    @ColorInt
    public int underlineColor = 0;

    /**
     * Creates the new TextPaint.
     */
    public TextPaint() {
        super();
        mTypeface = ModernUI.getSelectedTypeface();
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
     * Returns the next cursor position in the run.
     * <p>
     * This avoids placing the cursor between surrogates, between characters that form conjuncts,
     * between base characters and combining marks, or within a reordering cluster.
     *
     * <p>
     * ContextStart and offset are relative to the start of text.
     * The context is the shaping context for cursor movement, generally the bounds of the metric
     * span enclosing the cursor in the direction of movement.
     *
     * <p>
     * If op is {@link GraphemeBreak#AT} and the offset is not a valid cursor position, this
     * returns -1.  Otherwise, this will never return a value before contextStart or after
     * contextStart + contextLength.
     *
     * @param text          the text
     * @param locale        the text's locale
     * @param contextStart  the start of the context
     * @param contextLength the length of the context
     * @param offset        the cursor position to move from
     * @param op            how to move the cursor
     * @return the offset of the next position or -1
     */
    public static int getTextRunCursor(@Nonnull char[] text, @Nonnull Locale locale, int contextStart,
                                       int contextLength, int offset, int op) {
        int contextEnd = contextStart + contextLength;
        if (((contextStart | contextEnd | offset | (contextEnd - contextStart)
                | (offset - contextStart) | (contextEnd - offset)
                | (text.length - contextEnd) | op) < 0)
                || op > GraphemeBreak.AT) {
            throw new IndexOutOfBoundsException();
        }
        return GraphemeBreak.sUseICU ? GraphemeBreak.getTextRunCursorICU(new CharArrayIterator(text, contextStart,
                contextEnd), locale, offset, op)
                : GraphemeBreak.getTextRunCursorImpl(null, text, contextStart, contextLength, offset, op);
    }

    /**
     * Returns the next cursor position in the run.
     * <p>
     * This avoids placing the cursor between surrogates, between characters that form conjuncts,
     * between base characters and combining marks, or within a reordering cluster.
     *
     * <p>
     * ContextStart, contextEnd, and offset are relative to the start of
     * text.  The context is the shaping context for cursor movement, generally
     * the bounds of the metric span enclosing the cursor in the direction of
     * movement.
     *
     * <p>
     * If op is {@link GraphemeBreak#AT} and the offset is not a valid cursor position, this
     * returns -1.  Otherwise, this will never return a value before contextStart or after
     * contextEnd.
     *
     * @param text         the text
     * @param locale       the text's locale
     * @param contextStart the start of the context
     * @param contextEnd   the end of the context
     * @param offset       the cursor position to move from
     * @param op           how to move the cursor
     * @return the offset of the next position, or -1
     */
    public static int getTextRunCursor(@Nonnull CharSequence text, @Nonnull Locale locale, int contextStart,
                                       int contextEnd, int offset, int op) {
        if (text instanceof String || text instanceof SpannedString ||
                text instanceof SpannableString) {
            return GraphemeBreak.getTextRunCursor(text.toString(), locale, contextStart, contextEnd,
                    offset, op);
        }
        final int contextLen = contextEnd - contextStart;
        final char[] buf = new char[contextLen];
        TextUtils.getChars(text, contextStart, contextEnd, buf, 0);
        offset = getTextRunCursor(buf, locale, 0, contextLen, offset - contextStart, op);
        return offset == -1 ? -1 : offset + contextStart;
    }

    /**
     * Copy the data from paint into this TextPaint
     */
    public void set(@Nonnull TextPaint paint) {
        super.set(paint);
        mTypeface = paint.mTypeface;
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
     * Set the font collection object to draw the text.
     *
     * @param typeface the font collection
     */
    public void setTypeface(@Nonnull Typeface typeface) {
        mTypeface = typeface;
        mFontCollection = typeface.mFontCollection;
    }

    @Nonnull
    public Typeface getTypeface() {
        return mTypeface;
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

    int getFlags() {
        return mFlags;
    }

    void setFlags(int flags) {
        mFlags = flags;
    }

    /**
     * Create a copy of this paint as the base class paint for internal
     * layout engine. Subclasses must ensure that be immutable.
     *
     * @return an internal paint
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
