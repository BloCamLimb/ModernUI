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

package icyllis.modernui.graphics.text;

import com.ibm.icu.text.Bidi;
import com.ibm.icu.text.BidiRun;
import icyllis.modernui.text.TextShaper;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrays;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * Text shaping result object for single style text, a sequence of positioned glyphs.
 * You can get text shaping result by {@link TextShaper#shapeTextRun}.
 * <p>
 * This object is immutable, internal buffers may be shared between threads.
 *
 * @see MeasuredText
 * @see LayoutCache
 */
@Immutable
public class ShapedText {

    /**
     * BiDi flags that indicating base direction is left-to-right.
     */
    public static final int BIDI_LTR = 0b000;
    /**
     * BiDi flags that indicating base direction is right-to-left.
     */
    public static final int BIDI_RTL = 0b001;
    /**
     * BiDi flags that indicating that the base direction depends on the first strong
     * directional character in the text according to the Unicode Bidirectional
     * Algorithm. If no strong directional character is present, the base
     * direction is left-to-right.
     */
    public static final int BIDI_DEFAULT_LTR = 0b010;
    /**
     * BiDi flags that indicating that the base direction depends on the first strong
     * directional character in the text according to the Unicode Bidirectional
     * Algorithm. If no strong directional character is present, the base
     * direction is right-to-left.
     */
    public static final int BIDI_DEFAULT_RTL = 0b011;
    /**
     * BiDi flags that indicating the whole text direction is known to be left-to-right,
     * no BiDi analysis will be performed.
     */
    public static final int BIDI_OVERRIDE_LTR = 0b100;
    /**
     * BiDi flags that indicating the whole text direction is known to be right-to-left,
     * no BiDi analysis will be performed.
     */
    public static final int BIDI_OVERRIDE_RTL = 0b101;

    /**
     * Returns the number of glyphs.
     */
    public int getGlyphCount() {
        return mGlyphs.length;
    }

    /**
     * The array is about all laid-out glyph codes for in order visually from left to right.
     *
     * @return glyphs
     */
    public int[] getGlyphs() {
        return mGlyphs;
    }

    /**
     * This array holds the repeat of x offset, y offset of glyph positions.
     * The length is twice as long as the glyph array.
     *
     * @return glyph positions
     */
    public float[] getPositions() {
        return mPositions;
    }

    /**
     * This array holds which font should be used for the i-th glyph.
     *
     * @return glyph fonts
     */
    public FontFamily[] getFonts() {
        return mFonts;
    }

    /**
     * Returns the number of characters (constructor <code>limit - start</code> in code units).
     */
    public int getCharCount() {
        return mAdvances.length;
    }

    /**
     * The array of all chars advance, the length and order are relative to the text buffer.
     * Only grapheme cluster bounds have advances, others are zeros. For example:
     * [13.57, 0, 14.26, 0, 0], meaning c[0] and c[1] become a cluster; c[2], c[3] and c[4]
     * become a cluster. The length is constructor <code>limit - start</code> in code units.
     *
     * @return advances
     * @see GraphemeBreak
     */
    public float[] getAdvances() {
        return mAdvances;
    }

    /**
     * Effective ascent value of this layout.
     * <p>
     * If two or more fonts are used in this series of glyphs, the effective ascent will be
     * the minimum ascent value across the all fonts.
     *
     * @return effective ascent value
     */
    public int getAscent() {
        return mAscent;
    }

    /**
     * Effective descent value of this layout.
     * <p>
     * If two or more fonts are used in this series of glyphs, the effective descent will be
     * the maximum descent value across the all fonts.
     *
     * @return effective descent value
     */
    public int getDescent() {
        return mDescent;
    }

    /**
     * Returns the total amount of advance consumed by this layout.
     * <p>
     * The advance is an amount of width consumed by the glyph. The total amount of advance is
     * a total amount of advance consumed by this series of glyphs. In other words, if another
     * glyph is placed next to this series of glyphs, it's X offset should be shifted this amount
     * of width.
     *
     * @return total amount of advance
     */
    public float getAdvance() {
        return mAdvance;
    }

    @Override
    public String toString() {
        return "ShapedText{" +
                "mGlyphs=" + Arrays.toString(mGlyphs) +
                ", mPositions=" + Arrays.toString(mPositions) +
                ", mFonts=" + Arrays.toString(mFonts) +
                ", mAdvances=" + Arrays.toString(mAdvances) +
                ", mAscent=" + mAscent +
                ", mDescent=" + mDescent +
                ", mAdvance=" + mAdvance +
                '}';
    }

    // all laid-out glyphs, the order is visually left-to-right
    private final int[] mGlyphs;

    // x0 y0 x1 y1... for positioning glyphs
    private final float[] mPositions;

    private final FontFamily[] mFonts;

    private final float[] mAdvances;

    // total font metrics
    private final int mAscent;
    private final int mDescent;

    // total advance
    private final float mAdvance;

    /**
     * Generate the shaped text layout. The layout object will not be associated with the
     * text array and the paint after construction, thus the buffer may be a shared object.
     * The context range will affect BiDi analysis and shaping results, it can be larger
     * than the layout range.
     *
     * @param text         text buffer, cannot be null
     * @param contextStart the context start index of text array
     * @param contextLimit the context end index of text array
     * @param start        the start index of the layout
     * @param limit        the end index of the layout
     * @param bidiFlags    one of BiDi flags listed above
     * @param paint        layout params
     */
    public ShapedText(@Nonnull char[] text, int contextStart, int contextLimit,
                      int start, int limit, int bidiFlags, FontPaint paint) {
        Objects.checkFromToIndex(contextStart, contextLimit, text.length);
        if (contextStart > start || contextLimit < limit) {
            throw new IndexOutOfBoundsException();
        }
        if (bidiFlags < 0 || bidiFlags > BIDI_OVERRIDE_RTL) {
            throw new IllegalArgumentException();
        }
        int count = limit - start;
        if (count == 0) {
            mAdvances = FloatArrays.EMPTY_ARRAY;
            mGlyphs = IntArrays.EMPTY_ARRAY;
            mPositions = FloatArrays.EMPTY_ARRAY;
            mFonts = (FontFamily[]) ObjectArrays.EMPTY_ARRAY;
            mAscent = 0;
            mDescent = 0;
            mAdvance = 0;
            return;
        }
        mAdvances = new float[count];
        final FontMetricsInt extent = new FontMetricsInt();

        // preserve memory
        final var glyphs = new IntArrayList(count);
        final var positions = new FloatArrayList(count * 2);
        final var fonts = new ArrayList<FontFamily>(count);

        float advance = 0;

        final boolean isOverride = (bidiFlags & 0b100) != 0;
        if (isOverride) {
            final boolean isRtl = (bidiFlags & 0b001) != 0;
            advance += doLayoutRun(text, contextStart, contextLimit,
                    start, limit, isRtl, paint, start,
                    mAdvances, advance, glyphs, positions, fonts, extent);
        } else {
            final byte paraLevel = switch (bidiFlags) {
                case BIDI_LTR -> Bidi.LTR;
                case BIDI_RTL -> Bidi.RTL;
                case BIDI_DEFAULT_LTR -> Bidi.LEVEL_DEFAULT_LTR;
                case BIDI_DEFAULT_RTL -> Bidi.LEVEL_DEFAULT_RTL;
                default -> throw new AssertionError();
            };
            // preserve memory
            Bidi bidi = new Bidi(contextLimit - contextStart, 0);
            bidi.setPara(text, paraLevel, null);
            // entirely right-to-left
            if (bidi.isRightToLeft()) {
                advance += doLayoutRun(text, contextStart, contextLimit,
                        start, limit, true, paint, start,
                        mAdvances, advance, glyphs, positions, fonts, extent);
            }
            // entirely left-to-right
            else if (bidi.isLeftToRight()) {
                advance += doLayoutRun(text, contextStart, contextLimit,
                        start, limit, false, paint, start,
                        mAdvances, advance, glyphs, positions, fonts, extent);
            }
            // full bidirectional analysis
            else {
                int runCount = bidi.getRunCount();
                for (int visualIndex = 0; visualIndex < runCount; visualIndex++) {
                    BidiRun run = bidi.getVisualRun(visualIndex);
                    int runStart = Math.max(run.getStart(), start);
                    int runEnd = Math.min(run.getLimit(), limit);
                    advance += doLayoutRun(text, contextStart, contextLimit,
                            runStart, runEnd, run.isOddRun(), paint, start,
                            mAdvances, advance, glyphs, positions, fonts, extent);
                }
            }
        }
        mAdvance = advance;

        mGlyphs = glyphs.toIntArray();
        mPositions = positions.toFloatArray();
        mFonts = fonts.toArray(new FontFamily[0]);

        mAscent = extent.ascent;
        mDescent = extent.descent;
    }

    // BiDi run, visual order
    private static float doLayoutRun(char[] text, int contextStart, int contextLimit,
                                     int start, int limit, boolean isRtl, FontPaint paint,
                                     int layoutStart, float[] advances, float parAdvance,
                                     IntArrayList glyphs, FloatArrayList positions,
                                     ArrayList<FontFamily> fonts, FontMetricsInt extent) {
        float advance = 0;

        //@formatter:off
        if (isRtl) {
            int pos = limit;
            for (;;) {
                int itContextStart = LayoutUtils.getPrevWordBreakForCache(
                        text, contextStart, contextLimit,
                        pos);
                int itContextEnd = LayoutUtils.getNextWordBreakForCache(
                        text, contextStart, contextLimit,
                        pos == 0 ? 0 : pos - 1);
                int itPieceStart = Math.max(itContextStart, start);
                int itPieceEnd = pos;
                if (itPieceStart == itPieceEnd) {
                    break;
                }
                advance += doLayoutWord(text, itContextStart, itContextEnd,
                        itPieceStart, itPieceEnd, true, paint,
                        itPieceStart - layoutStart, advances, parAdvance + advance,
                        glyphs, positions,
                        fonts, extent);
                pos = itPieceStart;
            }
        } else {
            int pos = start;
            for (;;) {
                int itContextStart = LayoutUtils.getPrevWordBreakForCache(
                        text, contextStart, contextLimit,
                        pos == limit ? pos : pos + 1);
                int itContextEnd = LayoutUtils.getNextWordBreakForCache(
                        text, contextStart, contextLimit,
                        pos);
                int itPieceStart = pos;
                int itPieceEnd = Math.min(itContextEnd, limit);
                if (itPieceStart == itPieceEnd) {
                    break;
                }
                advance += doLayoutWord(text, itContextStart, itContextEnd,
                        itPieceStart, itPieceEnd, false, paint,
                        itPieceStart - layoutStart, advances, parAdvance + advance,
                        glyphs, positions,
                        fonts, extent);
                pos = itPieceEnd;
            }
        }
        //@formatter:on
        return advance;
    }

    // visual order
    private static float doLayoutWord(char[] buf, int contextStart, int contextEnd,
                                      int start, int end, boolean isRtl, FontPaint paint,
                                      int advanceOffset, float[] advances, float runAdvance,
                                      IntArrayList glyphs, FloatArrayList positions,
                                      ArrayList<FontFamily> fonts, FontMetricsInt extent) {
        LayoutPiece src = LayoutCache.getOrCreate(
                buf, contextStart, contextEnd, start, end, isRtl, paint);

        for (int i = 0; i < src.getGlyphCount(); i++) {
            fonts.add(src.getFont(i));
        }
        glyphs.addElements(glyphs.size(), src.getGlyphs());
        int posStart = positions.size();
        positions.addElements(posStart, src.getPositions());
        for (int posIndex = posStart,
             posEnd = positions.size();
             posIndex < posEnd;
             posIndex += 2) {
            positions.elements()[posIndex] += runAdvance;
        }

        float[] srcAdvances = src.getAdvances();
        System.arraycopy(srcAdvances, 0,
                advances, advanceOffset, srcAdvances.length);
        extent.extendBy(src.getAscent(), src.getDescent());

        return src.getAdvance();
    }
}
