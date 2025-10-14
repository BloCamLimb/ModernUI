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
import icyllis.arc3d.sketch.TextBlob;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.MathUtil;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.function.Function;

/**
 * Text shaping result object for single style text, a sequence of positioned glyphs.
 * You can get text shaping result by {@link icyllis.modernui.text.TextShaper#shapeText}, or directly
 * calling the constructor if no text direction heuristic algorithm is needed.
 * <p>
 * Text shaping is the process of translating a string of character codes (such as
 * Unicode codepoints) into a properly arranged sequence of glyphs that can be rendered
 * onto a screen or into final output form for inclusion in a document. See
 * <a href="https://harfbuzz.github.io/what-is-harfbuzz.html">HarfBuzz</a> for more.
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
     * BiDi flags that indicating the whole text direction is determined to be left-to-right,
     * no BiDi analysis will be performed.
     */
    public static final int BIDI_OVERRIDE_LTR = 0b100;
    /**
     * BiDi flags that indicating the whole text direction is determined to be right-to-left,
     * no BiDi analysis will be performed.
     */
    public static final int BIDI_OVERRIDE_RTL = 0b101;

    /**
     * Request to create per-cluster advance information.
     *
     * @see LayoutCache#COMPUTE_CLUSTER_ADVANCES
     */
    public static final int CREATE_CLUSTER_ADVANCES = 0x01;
    /**
     * Request to create positioned glyph vector (for rendering).
     */
    public static final int CREATE_POSITIONED_GLYPHS = 0x02;

    /**
     * Returns the number of glyphs.
     * <p>
     * This is available when {@link #CREATE_POSITIONED_GLYPHS} is set.
     */
    public int getGlyphCount() {
        return mGlyphs.length;
    }

    /**
     * The array is about all laid-out glyph codes for in order visually from left to right.
     * The length is {@link #getGlyphCount()}.
     * <p>
     * This is the underlying array that may be shared across threads,
     * it must NOT be modified.
     * <p>
     * This is available when {@link #CREATE_POSITIONED_GLYPHS} is set.
     *
     * @return glyphs
     */
    @Unmodifiable
    public int[] getGlyphs() {
        return mGlyphs;
    }

    /**
     * Copy an array of glyphs for the given range.
     * The array is about all laid-out glyph codes for in order visually from left to right.
     */
    public void getGlyphs(int glyphIndex, int glyphCount, @NonNull int[] dstGlyphs, int dstOffset) {
        System.arraycopy(mGlyphs, glyphIndex, dstGlyphs, dstOffset, glyphCount);
    }

    /**
     * Helper of {@link #getGlyphs}.
     */
    public int getGlyph(int i) {
        return mGlyphs[i];
    }

    /**
     * This array holds the repeat of x offset, y offset of glyph positions.
     * The length is twice as long as the glyph array.
     * <p>
     * This is the underlying array that may be shared across threads,
     * it must NOT be modified.
     * <p>
     * This is available when {@link #CREATE_POSITIONED_GLYPHS} is set.
     *
     * @return glyph positions
     */
    @Unmodifiable
    public float[] getPositions() {
        return mPositions;
    }

    /**
     * Copy an array of glyph positions for the given range.
     * This array holds the repeat of x offset, y offset of glyph positions.
     */
    public void getPositions(int glyphIndex, int glyphCount, @NonNull float[] dstPositions, int dstOffset) {
        System.arraycopy(mPositions, glyphIndex << 1, dstPositions, dstOffset, glyphCount << 1);
    }

    /**
     * Helper of {@link #getPositions}.
     */
    public float getX(int i) {
        return mPositions[i << 1];
    }

    /**
     * Helper of {@link #getPositions}.
     */
    public float getY(int i) {
        return mPositions[i << 1 | 1];
    }

    /**
     * Returns which font should be used for the i-th glyph.
     * <p>
     * A layout can contain multiple font runs.
     * <p>
     * This is available when {@link #CREATE_POSITIONED_GLYPHS} is set
     * and glyph count is nonzero.
     *
     * @param i the index
     * @return the font
     */
    public Font getFont(int i) {
        if (mFontIndices != null) {
            return mFonts[mFontIndices[i] & 0xFF];
        }
        return mFonts[0];
    }

    /**
     * Returns the number of characters (i.e. constructor <code>limit - start</code> in code units).
     * This is available when {@link #CREATE_CLUSTER_ADVANCES} is set.
     */
    public int getCharCount() {
        return mAdvances.length;
    }

    /**
     * The array of all chars advance, the length and order are relative to the text buffer.
     * This is available when {@link #CREATE_CLUSTER_ADVANCES} is set.
     *
     * @return advances, or null
     */
    @Unmodifiable
    public float[] getAdvances() {
        return mAdvances;
    }

    /**
     * Helper of {@link #getAdvances()}.
     */
    public float getAdvance(int i) {
        if (i == mAdvances.length) {
            return mAdvance;
        }
        return mAdvances[i];
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

    /**
     * Returns the base paragraph direction (1 if left-to-right, -1 if right-to-left).
     * <p>
     * A layout may contain only one BiDi run, or multiple BiDi runs.
     *
     * @return base dir, positive if LTR, negative if RTL
     */
    public int getParaDir() {
        return mParaDir;
    }

    /**
     * Returns the FontPaint that used to create this layout,
     * or null of this layout contains no glyphs (i.e. getGlyphCount() == 0).
     * <p>
     * This is an immutable copy of the constructor argument, it must
     * NOT be modified.
     * <p>
     * This is available when {@link #CREATE_POSITIONED_GLYPHS} is set
     * and glyph count is nonzero.
     *
     * @return the creating layout params
     */
    @Unmodifiable
    public FontPaint getPaint() {
        return mPaint;
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    @Nullable
    public TextBlob getTextBlob() {
        if (!mTextBlobValid) {
            synchronized (this) {
                if (!mTextBlobValid) {
                    var nativeFont = new icyllis.arc3d.sketch.Font();
                    mPaint.getNativeFont(nativeFont);
                    mTextBlob = computeTextBlob(nativeFont);
                    mTextBlobValid = true; // order is important
                }
            }
        }
        return mTextBlob;
    }

    @ApiStatus.Internal
    public int getMemoryUsage() {
        int m = 48;
        m += 16 + MathUtil.align8(mGlyphs.length << 2);
        m += 16 + MathUtil.align8(mPositions.length << 2);
        if (mFontIndices != null) {
            m += 16 + MathUtil.align8(mFontIndices.length);
        }
        m += 16 + MathUtil.align8(mFonts.length << 2);
        if (mAdvances != null) {
            m += 16 + MathUtil.align8(mAdvances.length << 2);
        }
        return m;
    }

    @Override
    public String toString() {
        return "ShapedText{" +
                "mGlyphs=" + Arrays.toString(mGlyphs) +
                ", mPositions=" + Arrays.toString(mPositions) +
                ", mFonts=" + Arrays.toString(mFonts) +
                ", mFontIndices=" + Arrays.toString(mFontIndices) +
                ", mAdvances=" + Arrays.toString(mAdvances) +
                ", mAscent=" + mAscent +
                ", mDescent=" + mDescent +
                ", mAdvance=" + mAdvance +
                ", mParaDir" + mParaDir +
                ", mTextBlob=" + mTextBlob +
                '}';
    }

    // all laid-out glyphs, the order is visually left-to-right
    private final int[] mGlyphs;

    // x0 y0 x1 y1... for positioning glyphs
    private final float[] mPositions;

    private final byte[] mFontIndices;
    private final Font[] mFonts;

    private final float[] mAdvances;

    // total font metrics
    private final int mAscent;
    private final int mDescent;

    // total advance
    private final float mAdvance;
    // base paragraph direction
    private final int mParaDir;

    private final FontPaint mPaint;
    private volatile boolean mTextBlobValid;
    private TextBlob mTextBlob;

    // we don't compute/store per-cluster advances for the first constructor
    private static final boolean COMPUTE_ADVANCES = false;

    /**
     * Generate the shaped text layout. The layout object will not be associated with the
     * text array and the paint after construction.
     * <p>
     * If <var>bidiFlags</var> are not OVERRIDE, the text array is the entire context, the
     * caller is responsible for creating a copy of the context. Otherwise, the text array
     * can be larger than context range, which is specified by <var>contextStart</var> and
     * <var>contextLimit</var>.
     * <p>
     * The context range will affect BiDi analysis and shaping results, it can be slightly
     * larger than the layout range.
     * <p>
     * This method only computes positioned glyphs but does not compute per-cluster advances,
     * use {@link #ShapedText(char[], int, int, int, int, int, FontPaint, int)} or
     * {@link #ShapedText(Bidi, int, int, FontPaint, int)} for fine control.
     *
     * @param text         text buffer, cannot be null
     * @param contextStart the context start index of text array
     * @param contextLimit the context end index of text array
     * @param start        the start index of the layout
     * @param limit        the end index of the layout
     * @param bidiFlags    one of BiDi flags listed above
     * @param paint        layout params
     */
    public ShapedText(@NonNull char[] text, int contextStart, int contextLimit,
                      int start, int limit,
                      @MagicConstant(flags = {
                              BIDI_LTR, BIDI_RTL, BIDI_DEFAULT_LTR, BIDI_DEFAULT_RTL,
                              BIDI_OVERRIDE_LTR, BIDI_OVERRIDE_RTL
                      }) int bidiFlags,
                      @NonNull FontPaint paint) {
        this(text, contextStart, contextLimit, start, limit, bidiFlags, paint,
                CREATE_POSITIONED_GLYPHS | (COMPUTE_ADVANCES ? CREATE_CLUSTER_ADVANCES : 0));
    }

    /**
     * Generate the shaped text layout. The layout object will not be associated with the
     * text array and the paint after construction.
     * <p>
     * If <var>bidiFlags</var> are not OVERRIDE, the text array is the entire context, the
     * caller is responsible for creating a copy of the context. Otherwise, the text array
     * can be larger than context range, which is specified by <var>contextStart</var> and
     * <var>contextLimit</var>.
     * <p>
     * The context range will affect BiDi analysis and shaping results, it can be slightly
     * larger than the layout range.
     * <p>
     * If you have already got the Bidi object, use {@link #ShapedText(Bidi, int, int, FontPaint, int)}.
     *
     * @param text         text buffer, cannot be null
     * @param contextStart the context start index of text array
     * @param contextLimit the context end index of text array
     * @param start        the start index of the layout
     * @param limit        the end index of the layout
     * @param bidiFlags    one of BiDi flags listed above
     * @param paint        layout params
     * @param createFlags  one of create flags listed above
     */
    public ShapedText(@NonNull char[] text, int contextStart, int contextLimit,
                      int start, int limit,
                      @MagicConstant(flags = {
                              BIDI_LTR, BIDI_RTL, BIDI_DEFAULT_LTR, BIDI_DEFAULT_RTL,
                              BIDI_OVERRIDE_LTR, BIDI_OVERRIDE_RTL
                      }) int bidiFlags,
                      @NonNull FontPaint paint,
                      @MagicConstant(flags = {
                              CREATE_CLUSTER_ADVANCES,
                              CREATE_POSITIONED_GLYPHS
                      }) int createFlags) {
        int length = text.length;
        Objects.checkFromToIndex(contextStart, contextLimit, length);
        if (contextStart > start || contextLimit < limit) {
            throw new IndexOutOfBoundsException(
                    String.format("context range [%d,%d) must be no smaller than layout range [%d,%d)",
                            contextStart, contextLimit, start, limit)
            );
        }
        if (bidiFlags < 0 || bidiFlags > 0b111) {
            throw new IllegalArgumentException();
        }
        final boolean isOverride = (bidiFlags & 0b100) != 0;
        if (!isOverride && (contextStart != 0 || contextLimit != length)) {
            throw new IllegalArgumentException(
                    String.format("text array [0,%d) must be context range [%d,%d) for non-override bidi flags 0x%X",
                            length, contextStart, contextLimit, bidiFlags)
            );
        }
        int count = limit - start;
        // we allow for an empty range
        if (count == 0) {
            mAdvances = (createFlags & CREATE_CLUSTER_ADVANCES) != 0 ? FloatArrays.EMPTY_ARRAY : null;
            mGlyphs = (createFlags & CREATE_POSITIONED_GLYPHS) != 0 ? IntArrays.EMPTY_ARRAY : null;
            mPositions = (createFlags & CREATE_POSITIONED_GLYPHS) != 0 ? FloatArrays.EMPTY_ARRAY : null;
            // these two arrays are internal so can be null
            mFontIndices = null;
            mFonts = null;
            mAscent = 0;
            mDescent = 0;
            mAdvance = 0;
            mParaDir = 1;
            mPaint = null;
            mTextBlob = null;
            mTextBlobValid = true;
            return;
        }
        if ((createFlags & CREATE_CLUSTER_ADVANCES) != 0) {
            mAdvances = new float[count];
        } else {
            mAdvances = null;
        }
        final FontMetricsInt extent = new FontMetricsInt();

        float advance = 0;

        GlyphsBuilder builder = (createFlags & CREATE_POSITIONED_GLYPHS) != 0 ? new GlyphsBuilder(count) : null;

        if (isOverride) {
            final boolean isRtl = (bidiFlags & 0b001) != 0;
            advance += doLayoutRun(text, contextStart, contextLimit,
                    start, limit, isRtl, paint, start,
                    mAdvances, advance, extent,
                    builder);
            mParaDir = isRtl ? -1 : 1;
        } else {
            // here, text array is the entire context
            final byte paraLevel = switch (bidiFlags) {
                case BIDI_LTR -> Bidi.LTR;
                case BIDI_RTL -> Bidi.RTL;
                case BIDI_DEFAULT_LTR -> Bidi.LEVEL_DEFAULT_LTR;
                case BIDI_DEFAULT_RTL -> Bidi.LEVEL_DEFAULT_RTL;
                default -> throw new AssertionError();
            };
            // reserve memory
            Bidi bidi = new Bidi(length, 0);
            bidi.setPara(text, paraLevel, null);
            // entirely right-to-left
            if (bidi.isRightToLeft()) {
                advance += doLayoutRun(text, 0, length,
                        start, limit, true, paint, start,
                        mAdvances, advance, extent,
                        builder);
            }
            // entirely left-to-right
            else if (bidi.isLeftToRight()) {
                advance += doLayoutRun(text, 0, length,
                        start, limit, false, paint, start,
                        mAdvances, advance, extent,
                        builder);
            }
            // full bidirectional analysis
            else {
                int runCount = bidi.getRunCount();
                for (int visualIndex = 0; visualIndex < runCount; visualIndex++) {
                    BidiRun run = bidi.getVisualRun(visualIndex);
                    int runStart = Math.max(run.getStart(), start);
                    int runEnd = Math.min(run.getLimit(), limit);
                    advance += doLayoutRun(text, 0, length,
                            runStart, runEnd, run.isOddRun(), paint, start,
                            mAdvances, advance, extent,
                            builder);
                }
            }
            mParaDir = (bidi.getParaLevel() & 1) == 0 ? 1 : -1;
        }
        mAdvance = advance;

        if (builder != null) {
            mGlyphs = builder.glyphs.toIntArray();
            mPositions = builder.positions.toFloatArray();
            if (builder.fontVec.size() > 1) {
                mFontIndices = builder.fontIndices.toByteArray();
            } else {
                mFontIndices = null;
            }
            mFonts = builder.fontVec.toArray(new Font[0]);

            assert mGlyphs.length * 2 == mPositions.length;
            assert mFontIndices == null || mFontIndices.length == mGlyphs.length;
        } else {
            mGlyphs = null;
            mPositions = null;
            mFontIndices = null;
            mFonts = null;
        }

        mAscent = extent.ascent;
        mDescent = extent.descent;

        if (mGlyphs == null || mGlyphs.length == 0) {
            mPaint = null;
            mTextBlob = null;
            mTextBlobValid = true;
        } else {
            // save a paint for computeTextBlob()
            mPaint = new FontPaint(paint);
            mTextBlobValid = false;
        }
    }

    /**
     * Generate the shaped text layout. The layout object will not be associated with the
     * bidi object and the paint after construction.
     * <p>
     * The <var>bidi</var> text is the entire context, the caller can create text layout
     * for a subrange.
     *
     * @param bidi        bidi text, cannot be null
     * @param start       the start index of the layout
     * @param limit       the end index of the layout
     * @param paint       layout params
     * @param createFlags one of create flags listed above
     * @since 3.12.1
     */
    public ShapedText(@NonNull Bidi bidi, int start, int limit,
                      @NonNull FontPaint paint,
                      @MagicConstant(flags = {
                              CREATE_CLUSTER_ADVANCES,
                              CREATE_POSITIONED_GLYPHS
                      }) int createFlags) {
        char[] text = bidi.getText();
        int length = bidi.getLength();
        int count = limit - start;
        // we allow for an empty range
        if (count == 0) {
            mAdvances = (createFlags & CREATE_CLUSTER_ADVANCES) != 0 ? FloatArrays.EMPTY_ARRAY : null;
            mGlyphs = (createFlags & CREATE_POSITIONED_GLYPHS) != 0 ? IntArrays.EMPTY_ARRAY : null;
            mPositions = (createFlags & CREATE_POSITIONED_GLYPHS) != 0 ? FloatArrays.EMPTY_ARRAY : null;
            // these two arrays are internal so can be null
            mFontIndices = null;
            mFonts = null;
            mAscent = 0;
            mDescent = 0;
            mAdvance = 0;
            mParaDir = 1;
            mPaint = null;
            mTextBlob = null;
            mTextBlobValid = true;
            return;
        }
        if ((createFlags & CREATE_CLUSTER_ADVANCES) != 0) {
            mAdvances = new float[count];
        } else {
            mAdvances = null;
        }
        final FontMetricsInt extent = new FontMetricsInt();

        float advance = 0;

        GlyphsBuilder builder = (createFlags & CREATE_POSITIONED_GLYPHS) != 0 ? new GlyphsBuilder(count) : null;

        // entirely right-to-left
        if (bidi.isRightToLeft()) {
            advance += doLayoutRun(text, 0, length,
                    start, limit, true, paint, start,
                    mAdvances, advance, extent,
                    builder);
        }
        // entirely left-to-right
        else if (bidi.isLeftToRight()) {
            advance += doLayoutRun(text, 0, length,
                    start, limit, false, paint, start,
                    mAdvances, advance, extent,
                    builder);
        }
        // full bidirectional analysis
        else {
            int runCount = bidi.getRunCount();
            for (int visualIndex = 0; visualIndex < runCount; visualIndex++) {
                BidiRun run = bidi.getVisualRun(visualIndex);
                int runStart = Math.max(run.getStart(), start);
                int runEnd = Math.min(run.getLimit(), limit);
                advance += doLayoutRun(text, 0, length,
                        runStart, runEnd, run.isOddRun(), paint, start,
                        mAdvances, advance, extent,
                        builder);
            }
        }
        mParaDir = (bidi.getParaLevel() & 1) == 0 ? 1 : -1;
        mAdvance = advance;

        if (builder != null) {
            mGlyphs = builder.glyphs.toIntArray();
            mPositions = builder.positions.toFloatArray();
            if (builder.fontVec.size() > 1) {
                mFontIndices = builder.fontIndices.toByteArray();
            } else {
                mFontIndices = null;
            }
            mFonts = builder.fontVec.toArray(new Font[0]);

            assert mGlyphs.length * 2 == mPositions.length;
            assert mFontIndices == null || mFontIndices.length == mGlyphs.length;
        } else {
            mGlyphs = null;
            mPositions = null;
            mFontIndices = null;
            mFonts = null;
        }

        mAscent = extent.ascent;
        mDescent = extent.descent;

        if (mGlyphs == null || mGlyphs.length == 0) {
            mPaint = null;
            mTextBlob = null;
            mTextBlobValid = true;
        } else {
            // save a paint for computeTextBlob()
            mPaint = new FontPaint(paint);
            mTextBlobValid = false;
        }
    }

    @Nullable
    private TextBlob computeTextBlob(@NonNull icyllis.arc3d.sketch.Font nativeFont) {
        if (mFontIndices == null) {
            // optimize for single font
            nativeFont.setTypeface(mFonts[0].getNativeTypeface());
            if (nativeFont.getTypeface() != null) {
                return TextBlob.makeNoCopy(mGlyphs, mPositions,
                        nativeFont, null);
            } else {
                return null;
            }
        } else {
            // theoretically, we don't need to copy the array in this case, but...
            final TextBlob.Builder builder = new TextBlob.Builder();
            var lastFont = getFont(0);
            int lastPos = 0;
            int currPos = 1;
            int nGlyphs = mGlyphs.length;
            for (; currPos <= nGlyphs; currPos++) {
                var currFont = currPos == nGlyphs ? null : getFont(currPos);
                if (lastFont != currFont) {
                    nativeFont.setTypeface(lastFont.getNativeTypeface());
                    if (nativeFont.getTypeface() != null) {
                        int runCount = currPos - lastPos;
                        var runBuffer = builder.allocRunPos(
                                nativeFont, runCount, null
                        );
                        runBuffer.addGlyphs(mGlyphs, lastPos, runCount);
                        runBuffer.addPositions(mPositions, lastPos << 1, runCount);
                    }
                    lastFont = currFont;
                    lastPos = currPos;
                }
            }
            return builder.build();
        }
    }

    // build positioned glyphs
    private static class GlyphsBuilder implements RunConsumer {
        final ByteArrayList fontIndices;
        final IntArrayList glyphs;
        final FloatArrayList positions;

        final ArrayList<Font> fontVec;
        final HashMap<Font, Byte> fontMap;
        final Function<Font, Byte> nextID;

        GlyphsBuilder(int count) {
            // reserve memory, glyph count is <= char count
            fontIndices = new ByteArrayList(count);
            glyphs = new IntArrayList(count);
            positions = new FloatArrayList(count * 2);

            fontVec = new ArrayList<>();
            fontMap = new HashMap<>();
            nextID = font -> {
                fontVec.add(font);
                return (byte) fontMap.size();
            };
        }

        @Override
        public void accept(LayoutPiece src, int start, int end, boolean isRtl, FontPaint paint, float offsetX) {
            for (int i = 0; i < src.getGlyphCount(); i++) {
                byte id = fontMap.computeIfAbsent(src.getFont(i), nextID);
                fontIndices.add(id);
            }
            glyphs.addElements(glyphs.size(), src.getGlyphs());
            int posIndex = positions.size();
            positions.addElements(posIndex, src.getPositions());
            for (int posEnd = positions.size();
                 posIndex < posEnd;
                 posIndex += 2) {
                positions.elements()[posIndex] += offsetX;
            }
        }
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    @FunctionalInterface
    public interface RunConsumer {
        // piece layout start, piece layout end
        void accept(LayoutPiece piece, int start, int end, boolean isRtl, FontPaint paint, float offsetX);
    }

    /**
     * @hidden
     */
    // BiDi run, visual order, append layout pieces
    @ApiStatus.Internal
    public static float doLayoutRun(char[] text, int contextStart, int contextLimit,
                                    int start, int limit, boolean isRtl, FontPaint paint,
                                    FontMetricsInt extent, RunConsumer consumer) {
        return doLayoutRun(text, contextStart, contextLimit,
                start, limit, isRtl, paint, start,
                null, 0.0F, extent,
                consumer);
    }

    /**
     * @hidden
     */
    // BiDi run, visual order
    @ApiStatus.Internal
    public static float doLayoutRun(char[] text, int contextStart, int contextLimit,
                                    int start, int limit, boolean isRtl, FontPaint paint,
                                    int layoutStart, float[] advances, float startX,
                                    FontMetricsInt extent,
                                    RunConsumer consumer) {
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
                advance += doLayoutWord(text,
                        itContextStart, itContextEnd,
                        itPieceStart, itPieceEnd,
                        true,
                        paint,
                        itPieceStart - layoutStart,
                        advances,
                        startX + advance,
                        extent,
                        consumer);
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
                advance += doLayoutWord(text,
                        itContextStart, itContextEnd,
                        itPieceStart, itPieceEnd,
                        false,
                        paint,
                        itPieceStart - layoutStart,
                        advances,
                        startX + advance,
                        extent,
                        consumer);
                pos = itPieceEnd;
            }
        }
        //@formatter:on
        return advance;
    }

    // visual order
    private static float doLayoutWord(char[] buf, int contextStart, int contextEnd,
                                      int start, int end, boolean isRtl, FontPaint paint,
                                      int advanceOffset, float[] advances, float offsetX,
                                      FontMetricsInt extent, RunConsumer consumer) {
        LayoutPiece src = LayoutCache.getOrCreate(
                buf, contextStart, contextEnd, start, end, isRtl, paint,
                advances != null ? LayoutCache.COMPUTE_CLUSTER_ADVANCES : 0);

        if (consumer != null) {
            consumer.accept(src, start, end, isRtl, paint, offsetX);
        }
        if (advances != null) {
            System.arraycopy(src.getAdvances(), 0,
                    advances, advanceOffset, src.getCharCount());
        }
        if (extent != null) {
            extent.extendBy(src.getAscent(), src.getDescent());
        }

        return src.getAdvance();
    }
}
