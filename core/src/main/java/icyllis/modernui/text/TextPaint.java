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
import icyllis.modernui.annotation.*;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.text.*;
import icyllis.modernui.util.Pools;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * This class holds data used during text measuring and drawing at higher levels.
 * For the base class {@link FontPaint}, changing any attributes will require a
 * reflow and re-layout, not just re-drawing.
 */
public class TextPaint extends Paint {

    /**
     * Paint flag that applies an underline decoration to drawn text.
     */
    public static final int UNDERLINE_FLAG = 0x400;

    /**
     * Paint flag that applies a strike-through decoration to drawn text.
     */
    public static final int STRIKETHROUGH_FLAG = 0x800;

    private static final Pools.Pool<TextPaint> sPool = Pools.newSynchronizedPool(4);

    private final FontPaint mInternalPaint = new FontPaint();

    private Typeface mTypeface;
    private Locale mLocale;

    // Special value 0 means no background paint
    @ColorInt
    public int bgColor;
    public int baselineShift;
    @ColorInt
    public int linkColor = 0xff539bf5;
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
        mLocale = ModernUI.getSelectedLocale();
    }

    @ApiStatus.Internal
    public TextPaint(@NonNull TextPaint paint) {
        set(paint);
        mTypeface = paint.mTypeface;
        mLocale = paint.mLocale;
    }

    /**
     * Returns a TextPaint from the shared pool, a {@link #set(TextPaint)} is
     * expected before use and a {@link #recycle()} after use.
     *
     * @return a pooled object, states are undefined
     */
    @NonNull
    public static TextPaint obtain() {
        TextPaint paint = sPool.acquire();
        if (paint == null) {
            return new TextPaint();
        }
        return paint;
    }

    /**
     * Recycle this text paint, this object cannot be used anymore after recycling.
     */
    @Override
    public void recycle() {
        sPool.release(this);
    }

    /**
     * Copy the data from paint into this TextPaint
     */
    public void set(@NonNull TextPaint paint) {
        super.set(paint);
        mTypeface = paint.mTypeface;
        mLocale = paint.mLocale;
        bgColor = paint.bgColor;
        baselineShift = paint.baselineShift;
    }

    /**
     * Set the font collection object to draw the text.
     *
     * @param typeface the font collection
     */
    public void setTypeface(@NonNull Typeface typeface) {
        mTypeface = typeface;
    }

    @NonNull
    public Typeface getTypeface() {
        return mTypeface;
    }

    /**
     * Set the text locale.
     * <p>
     * A Locale may affect word break, line break, grapheme cluster break, etc.
     * The locale should match the language of the text to be drawn or user preference,
     * by default, the selected locale should be used {@link ModernUI#getSelectedLocale()}.
     *
     * @param locale the paint's locale value for drawing text, must not be null.
     */
    public void setTextLocale(@NonNull Locale locale) {
        if (!locale.equals(mLocale)) {
            mLocale = locale;
        }
    }

    /**
     * Get the text's Locale.
     *
     * @return the paint's Locale used for measuring and drawing text, never null.
     */
    @NonNull
    public Locale getTextLocale() {
        return mLocale;
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
    public float getUnderlineOffset(@NonNull FontMetricsInt fm) {
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
    public float getUnderlineThickness(@NonNull FontMetricsInt fm) {
        return -fm.ascent / 12f;
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
    public float getStrikethroughOffset(@NonNull FontMetricsInt fm) {
        return fm.ascent / 2f;
    }

    /**
     * Returns the thickness of the strike-through line in pixels.
     *
     * @return the position of the strike-through line in pixels
     * @see #getStrikethroughOffset(FontMetricsInt)
     */
    public float getStrikethroughThickness(@NonNull FontMetricsInt fm) {
        return -fm.ascent / 12f;
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
     * @param contextStart  the start of the context
     * @param contextLength the length of the context
     * @param offset        the cursor position to move from
     * @param op            how to move the cursor
     * @return the offset of the next position or -1
     */
    public int getTextRunCursor(@NonNull char[] text, int contextStart,
                                int contextLength, int offset, int op) {
        int contextEnd = contextStart + contextLength;
        if (((contextStart | contextEnd | offset | (contextEnd - contextStart)
                | (offset - contextStart) | (contextEnd - offset)
                | (text.length - contextEnd) | op) < 0)
                || op > GraphemeBreak.AT) {
            throw new IndexOutOfBoundsException();
        }
        return GraphemeBreak.sUseICU ? GraphemeBreak.getTextRunCursorICU(new CharArrayIterator(text, contextStart,
                contextEnd), mLocale, offset, op)
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
     * @param contextStart the start of the context
     * @param contextEnd   the end of the context
     * @param offset       the cursor position to move from
     * @param op           how to move the cursor
     * @return the offset of the next position, or -1
     */
    public int getTextRunCursor(@NonNull CharSequence text, int contextStart,
                                int contextEnd, int offset, int op) {
        if (text instanceof String || text instanceof SpannedString ||
                text instanceof SpannableString) {
            return GraphemeBreak.getTextRunCursor(text.toString(), mLocale, contextStart, contextEnd,
                    offset, op);
        }
        final int contextLen = contextEnd - contextStart;
        final char[] buf = new char[contextLen];
        TextUtils.getChars(text, contextStart, contextEnd, buf, 0);
        offset = getTextRunCursor(buf, 0, contextLen, offset - contextStart, op);
        return offset == -1 ? -1 : offset + contextStart;
    }

    int getFontFlags() {
        return mFlags;
    }

    void setFontFlags(int flags) {
        mFlags = flags;
    }

    /**
     * Returns true of the passed {@link TextPaint} will have the different effect on text measurement
     *
     * @param paint the paint to compare with
     * @return true if given {@link TextPaint} has the different effect on text measurement.
     */
    public boolean equalsForTextMeasurement(@Nonnull TextPaint paint) {
        return !getInternalPaint().isMetricAffecting(paint.getInternalPaint());
    }

    @NonNull
    public FontMetricsInt getFontMetricsInt() {
        FontMetricsInt fm = new FontMetricsInt();
        getFontMetricsInt(fm);
        return fm;
    }

    /**
     * Return the font's interline spacing, given the Paint's settings for
     * typeface, textSize, etc. If metrics is not null, return the fontmetric
     * values in it. Note: all values have been converted to integers from
     * floats, in such a way has to make the answers useful for both spacing
     * and clipping. If you want more control over the rounding, call
     * getFontMetrics().
     *
     * <p>Note that these are the values for the main typeface, and actual text rendered may need a
     * larger set of values because fallback fonts may get used in rendering the text.
     *
     * @return the font's interline spacing.
     */
    public int getFontMetricsInt(@Nullable FontMetricsInt fm) {
        return getInternalPaint().getFontMetricsInt(fm);
    }

    /**
     * Populates layout attributes to a temporary internal paint and returns.
     * See {@link #createInternalPaint()} to create a new paint.
     *
     * @return a shared internal paint
     */
    @NonNull
    public final FontPaint getInternalPaint() {
        FontPaint p = mInternalPaint;
        p.setFont(mTypeface);
        p.setLocale(mLocale);
        p.setFontSize(getFontSize());
        p.setFontStyle(getFontStyle());
        p.setRenderFlags(
               FontPaint.computeRenderFlags(this)
        );
        return p;
    }

    /**
     * Create a copy of this paint as the base class paint for internal
     * layout engine.
     *
     * @return an internal paint
     */
    @NonNull
    public final FontPaint createInternalPaint() {
        return new FontPaint(getInternalPaint());
    }
}
