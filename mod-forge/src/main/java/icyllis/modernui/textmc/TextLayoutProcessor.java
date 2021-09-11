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

package icyllis.modernui.textmc;

import com.ibm.icu.text.Bidi;
import icyllis.modernui.text.FontRun;
import icyllis.modernui.text.GlyphManager;
import icyllis.modernui.text.TexturedGlyph;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.textmc.pipeline.DigitGlyphRender;
import icyllis.modernui.textmc.pipeline.GlyphRender;
import icyllis.modernui.textmc.pipeline.StandardGlyphRender;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * This is where the text layout is actually performed.
 */
public class TextLayoutProcessor {

    /**
     * Array to build char array.
     */
    private final CharArrayList mChars = new CharArrayList();

    /**
     * Array of temporary style carriers.
     */
    private final List<CharacterStyleCarrier> mCarriers = new ArrayList<>();

    /**
     * List of all processing glyphs
     */
    private final List<GlyphRender> mAllList = new ArrayList<>();

    /**
     * List of processing glyphs with same layout direction
     */
    private final List<GlyphRender> mBidiList = new ArrayList<>();

    private final List<GlyphRender> mStyleList = new ArrayList<>();

    /*
     * All color states
     */
    //public final List<ColorStateInfo> colors = new ArrayList<>();

    /**
     * Indicates current style index in {@link #mCarriers} for layout processing
     */
    private int mCarrierIndex;

    /**
     * The total advance (horizontal width) of the processing text
     */
    private float mAdvance;

    /**
     * Needed in RTL layout
     */
    private float mLayoutRight;

    /**
     * Mark whether this node should enable effect rendering
     */
    private boolean mHasEffect;

    private void finishBidiRun(float adjust) {
        if (adjust != 0) {
            mBidiList.forEach(e -> e.mOffsetX += adjust);
        }
        mAllList.addAll(mBidiList);
        mBidiList.clear();
    }

    private void finishStyleRun(float adjust) {
        if (adjust != 0) {
            mStyleList.forEach(e -> e.mOffsetX += adjust);
        }
        mBidiList.addAll(mStyleList);
        mStyleList.clear();
    }

    private void release() {
        mChars.clear();
        mAllList.clear();
        mCarriers.clear();
        mCarrierIndex = 0;
        mAdvance = 0;
        mLayoutRight = 0;
        mHasEffect = false;
    }

    @Nonnull
    public TextRenderNode performFullLayout(@Nonnull String text) {
        char[] chars = resolveFormattingCodes(text);
        TextRenderNode node = null;
        if (chars.length > 0) {
            performBidiAnalysis(chars);
            if (!mAllList.isEmpty()) {
                finish();
                node = new TextRenderNode(mAllList.toArray(new GlyphRender[0]), mAdvance, mHasEffect);
            }
        }
        if (node == null) {
            node = new TextRenderNode(new GlyphRender[0], 0, false);
        }
        release();
        return node;
    }

    /**
     * Formatting codes are not involved in rendering, so we should first extract formatting codes
     * from a formatted text into a stripped text. The color codes must be removed for a font's context
     * sensitive glyph substitution to work (like Arabic letter middle form) or Bidi analysis.
     *
     * @param text text with formatting codes to strip
     * @return a new char array with all formatting codes removed from the given string
     * @see net.minecraft.util.StringDecomposer
     */
    @Nonnull
    private char[] resolveFormattingCodes(@Nonnull String text) {
        int shift = 0;

        Style style = Style.EMPTY;
        mCarriers.add(new CharacterStyleCarrier(0, 0, style));

        // also fix surrogate pairs
        final int limit = text.length();
        for (int next = 0; next < limit; ++next) {
            char c1 = text.charAt(next);
            if (c1 == '\u00a7') {
                if (next + 1 >= limit) {
                    break;
                }

                ChatFormatting formatting = TextLayoutEngine.getFormattingByCode(text.charAt(next + 1));
                if (formatting != null) {
                    /* Classic formatting will set all FancyStyling (like BOLD, UNDERLINE) to false if it's a color
                    formatting */
                    final Style newStyle = formatting == ChatFormatting.RESET ? Style.EMPTY :
                            style.applyLegacyFormat(formatting);
                    if (!style.equals(newStyle)) {
                        style = newStyle; // transit to new style
                        mCarriers.add(new CharacterStyleCarrier(next, next - shift, style));
                    }
                }

                next++;
                shift += 2;
            } else if (Character.isHighSurrogate(c1)) {
                if (next + 1 >= limit) {
                    mChars.add('\uFFFD');
                    break;
                }

                char c2 = text.charAt(next + 1);
                if (Character.isLowSurrogate(c2)) {
                    mChars.add(c1);
                    mChars.add(c2);
                    ++next;
                } else if (Character.isSurrogate(c1)) {
                    mChars.add('\uFFFD');
                } else {
                    mChars.add(c1);
                }
            } else if (Character.isSurrogate(c1)) {
                mChars.add('\uFFFD');
            } else {
                mChars.add(c1);
            }
        }

        /*while ((next = string.indexOf('\u00a7', start)) != -1 && next + 1 < string.length()) {
            TextFormatting formatting = fromFormattingCode(string.charAt(next + 1));

            *//*
         * Remove the two char color code from text[] by shifting the remaining data in the array over on top of it.
         * The "start" and "next" variables all contain offsets into the original unmodified "str" string. The "shift"
         * variable keeps track of how many characters have been stripped so far, and it's used to compute offsets into
         * the text[] array based on the start/next offsets in the original string.
         *
         * If string only contains 1 formatting code (2 chars in total), this doesn't work
         *//*
            //System.arraycopy(text, next - shift + 2, text, next - shift, text.length - next - 2);

            if (formatting != null) {
                *//* forceFormatting will set all FancyStyling (like BOLD, UNDERLINE) to false if this is a color
                formatting *//*
                style = style.forceFormatting(formatting);


                data.codes.add(new FormattingStyle(next, next - shift, style));
            }

            start = next + 2;
            shift += 2;
        }*/

        return mChars.toCharArray();
    }

    /**
     * Split the full text into contiguous LTR or RTL sections by applying the Unicode Bidirectional Algorithm. Calls
     * performBidiAnalysis() for each contiguous run to perform further analysis.
     *
     * @param text the full plain text (without formatting codes) to analyze
     * @see #performStyleAnalysis(char[], int, int, boolean)
     */
    private void performBidiAnalysis(@Nonnull char[] text) {
        /* Avoid performing full bidirectional analysis if text has no "strong" right-to-left characters */
        if (Bidi.requiresBidi(text, 0, text.length)) {
            /* Note that while requiresBidi() uses start/limit the Bidi constructor uses start/length */
            Bidi bidi = new Bidi(text, 0, null, 0, text.length, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);

            /* If text is entirely right-to-left, then insert an EntryText node for the entire string */
            if (bidi.isRightToLeft()) {
                performStyleAnalysis(text, 0, text.length, true);
            }

            /* Otherwise text has a mixture of LTR and RLT, and it requires full bidirectional analysis */
            else {
                int runCount = bidi.getRunCount();
                byte[] runs = new byte[runCount];

                /* Reorder contiguous runs of text into their display order from left to right */
                for (int i = 0; i < runCount; i++) {
                    runs[i] = (byte) bidi.getRunLevel(i);
                }
                int[] indexMap = Bidi.reorderVisual(runs);

                /*
                 * Every GlyphVector must be created on a contiguous run of left-to-right or right-to-left text. Keep
                 *  track of
                 * the horizontal advance between each run of text, so that the glyphs in each run can be assigned a
                 * position relative
                 * to the start of the entire string and not just relative to that run.
                 */
                for (int visualIndex = 0; visualIndex < runCount; visualIndex++) {
                    int logicalIndex = indexMap[visualIndex];
                    /* An odd numbered level indicates right-to-left ordering */
                    performStyleAnalysis(text, bidi.getRunStart(logicalIndex), bidi.getRunLimit(logicalIndex),
                            (bidi.getRunLevel(logicalIndex) & 1) != 0);
                }
            }
        }

        /* If text is entirely left-to-right, then insert an node for the entire string */
        else {
            performStyleAnalysis(text, 0, text.length, false);
        }
    }

    /**
     * Analyze the best matching font and paragraph context, according to layout direction and generate glyph vector.
     * In some languages, the original Unicode code is mapped to another Unicode code for visual rendering.
     * They will finally be converted into glyph codes according to different Font.
     *
     * @param text  the plain text (without formatting codes) to analyze
     * @param start start index (inclusive) of the text
     * @param limit end index (exclusive) of the text
     * @param isRtl layout direction, either {@link Font#LAYOUT_LEFT_TO_RIGHT} or {@link Font#LAYOUT_RIGHT_TO_LEFT}
     */
    private void performStyleAnalysis(@Nonnull char[] text, int start, int limit, boolean isRtl) {
        float lastAdvance = mAdvance;
        if (isRtl) {
            mLayoutRight = lastAdvance;
        }
        final int carrierLimit = mCarriers.size() - 1;

        // Break up the string into segments, where each segment has the same font style in use
        while (start < limit) {
            int next = limit;

            CharacterStyleCarrier carrier = mCarriers.get(mCarrierIndex);

            // remove empty styles in case of multiple consecutive carriers with the same stripIndex,
            // select the last one which will have active font style
            while (mCarrierIndex < carrierLimit) {
                CharacterStyleCarrier c = mCarriers.get(mCarrierIndex + 1);
                if (carrier.mStringIndex == c.mStringIndex) {
                    carrier = c;
                    mCarrierIndex++;
                } else {
                    break;
                }
            }

            /*
             * Search for the next FormattingCode that uses a different layoutStyle than the current one. If found,
             * the stripIndex of that
             * new code is the split point where the string must be split into a separately styled segment.
             */
            while (mCarrierIndex < carrierLimit) {
                mCarrierIndex++;
                CharacterStyleCarrier c = mCarriers.get(mCarrierIndex);
                if (c.isLayoutAffecting(carrier)) {
                    next = c.mStripIndex;
                    break;
                }
            }

            /* Layout the string segment with the style currently selected by the last color code */
            performFontAnalysis(text, start, next, isRtl, carrier);
            start = next;
        }

        if (isRtl) {
            finishBidiRun(mAdvance - lastAdvance);
        } else {
            finishBidiRun(0);
        }
    }

    /**
     * Analyze the best matching font and paragraph context, according to layout direction and generate glyph vector.
     * In some languages, the original Unicode code is mapped to another Unicode code for visual rendering.
     * They will finally be converted into glyph codes according to different Font.
     *
     * @param text    the plain text (without formatting codes) to analyze
     * @param start   start index (inclusive) of the text
     * @param limit   end index (exclusive) of the text
     * @param isRtl   layout direction, either {@link Font#LAYOUT_LEFT_TO_RIGHT} or {@link Font#LAYOUT_RIGHT_TO_LEFT}
     * @param carrier the style to layout the text
     * @see Typeface#itemize(char[], int, int)
     */
    private void performFontAnalysis(@Nonnull char[] text, int start, int limit, boolean isRtl,
                                     @Nonnull CharacterStyleCarrier carrier) {
        /*
         * Convert all digits in the string to a '0' before layout to ensure that any glyphs replaced on the fly will
         * all have the same positions. Under Windows, Java's "SansSerif" logical font uses the "Arial" font for
         * digits, in which the "1" digit is slightly narrower than all other digits. Digits are not on SMP.
         */
        for (int i = start; i < limit; i++) {
            if (text[i] <= '9' && text[i] >= '0') {
                text[i] = '0';
            }
        }

        float lastAdvance = mAdvance;

        List<FontRun> items = Typeface.PREFERENCE.itemize(text, start, limit);
        for (int runIndex = isRtl ? items.size() - 1 : 0;
             isRtl ? runIndex >= 0 : runIndex < items.size(); ) {
            FontRun run = items.get(runIndex);
            performTextLayout(text, run.getStart(), run.getEnd(), isRtl, carrier, run.getFont());
            if (isRtl) {
                runIndex--;
            } else {
                runIndex++;
            }
        }

        if (isRtl) {
            float totalAdvance = mAdvance - lastAdvance;
            finishStyleRun(-totalAdvance);
            mLayoutRight -= totalAdvance;
        } else {
            finishStyleRun(0);
        }
    }

    /**
     * Finally, we got a piece of text with same layout direction, font style and whether to be obfuscated.
     *
     * @param text    the plain text (without formatting codes) to analyze
     * @param start   start index (inclusive) of the text
     * @param limit   end index (exclusive) of the text
     * @param isRtl   layout direction, either {@link Font#LAYOUT_LEFT_TO_RIGHT} or {@link Font#LAYOUT_RIGHT_TO_LEFT}
     * @param carrier the style to layout the text
     * @param font    the derived font with fontStyle and fontSize
     */
    @SuppressWarnings("MagicConstant")
    private void performTextLayout(@Nonnull char[] text, int start, int limit, boolean isRtl,
                                   @Nonnull CharacterStyleCarrier carrier, @Nonnull Font font) {
        // The glyphCode matched to the same codePoint is specified in the font, they are different
        // in different font, HarfBuzz is introduced in Java 11 or higher
        GlyphManager engine = GlyphManager.getInstance();
        TextLayoutEngine layoutEngine = TextLayoutEngine.getInstance();
        final float res = layoutEngine.getResolutionLevel();
        font = font.deriveFont(carrier.getFontStyle(), 8 * res);
        GlyphVector vector = engine.layoutGlyphVector(font, text, start, limit, isRtl);
        int num = vector.getNumGlyphs();

        final TexturedGlyph[] digits = layoutEngine.lookupDigits(font);

        float lastOffset = 0;
        for (int i = 0; i < num; i++) {
            /*
             * Back compatibility for Java 8, since LayoutGlyphVector should not have non-standard glyphs
             * HarfBuzz is introduced in Java 11 or higher
             */
                /*if (vector.getGlyphMetrics(i).getAdvanceX() == 0 &&
                        vector.getGlyphMetrics(i).getBounds2D().getWidth() == 0) {
                    continue;
                }*/

            int stripIndex = vector.getGlyphCharIndex(i) + start;
            Point2D point = vector.getGlyphPosition(i);

            float offset = (float) (point.getX() / res);
            float advance = offset - lastOffset;
            lastOffset = offset;

            if (isRtl) {
                offset += mLayoutRight;
            } else {
                offset += mAdvance;
            }

            int decoration = carrier.getDecoration();
            // Digits are not on SMP
            if (text[stripIndex] == '0') {
                mStyleList.add(new DigitGlyphRender(stripIndex, offset, advance, decoration, digits));
                mHasEffect |= decoration != 0;
                continue;
            }

            int glyphCode = vector.getGlyphCode(i);
            TexturedGlyph glyph = engine.lookupGlyph(font, glyphCode);

            mStyleList.add(new StandardGlyphRender(stripIndex, offset, advance, decoration, glyph));
            if (glyph != null) {
                mHasEffect |= decoration != 0;
            }
        }

        float totalAdvance = (float) (vector.getGlyphPosition(num).getX() / res);
        mAdvance += totalAdvance;
    }

    /**
     * Adjust strip index to string index and insert color transitions.
     */
    private void finish() {
        /* Sort by stripIndex, mixed with LTR and RTL layout */
        mAllList.sort(Comparator.comparingInt(g -> g.mStringIndex));

        /* Shift stripIndex to stringIndex */
        /* Skip the default code */
        int codeIndex = 1, shift = 0;
        for (GlyphRender glyph : mAllList) {
            /*
             * Adjust the string index for each glyph to point into the original string with un-stripped color codes.
             *  The while
             * loop is necessary to handle multiple consecutive color codes with no visible glyphs between them.
             * These new adjusted
             * stringIndex can now be compared against the color stringIndex during rendering. It also allows lookups
             *  of ASCII
             * digits in the original string for fast glyph replacement during rendering.
             */
            while (codeIndex < mCarriers.size() && glyph.mStringIndex + shift >= mCarriers.get(codeIndex).mStringIndex) {
                shift += 2;
                codeIndex++;
            }
            glyph.mStringIndex += shift;
        }

        codeIndex = 0;

        while (codeIndex < mCarriers.size() - 1 &&
                mCarriers.get(codeIndex).mStripIndex == mCarriers.get(codeIndex + 1).mStripIndex) {
            codeIndex++;
        }

        int color = mCarriers.get(codeIndex).getColor();
        /* The default is no color */
        GlyphRender glyph;
        if (color != CharacterStyleCarrier.USE_PARAM_COLOR) {
            glyph = mAllList.get(0);
            glyph.mFlags &= ~GlyphRender.COLOR_NO_CHANGE;
            glyph.mFlags |= color;
        }

        if (++codeIndex < mCarriers.size()) {
            for (int glyphIndex = 1; glyphIndex < mAllList.size(); glyphIndex++) {
                glyph = mAllList.get(glyphIndex);

                if (codeIndex < mCarriers.size() && glyph.mStringIndex > mCarriers.get(codeIndex).mStringIndex) {
                    /* In case of multiple consecutive color codes with the same stripIndex,
                    select the last one which will have active font style */
                    while (codeIndex < mCarriers.size() - 1 &&
                            mCarriers.get(codeIndex).mStripIndex == mCarriers.get(codeIndex + 1).mStripIndex) {
                        codeIndex++;
                    }

                    CharacterStyleCarrier s = mCarriers.get(codeIndex);
                    if (s.getColor() != color) {
                        color = s.getColor();
                        glyph.mFlags &= ~GlyphRender.COLOR_NO_CHANGE;
                        glyph.mFlags |= color;
                    }

                    codeIndex++;
                }
            }
        }
    }

    /*private void merge(@Nonnull List<float[]> list, int color, byte type) {
        if (list.isEmpty()) {
            return;
        }
        list.sort((o1, o2) -> Float.compare(o1[0], o2[0]));
        float[][] res = new float[list.size()][2];
        int i = -1;
        for (float[] interval : list) {
            if (i == -1 || interval[0] > res[i][1]) {
                res[++i] = interval;
            } else {
                res[i][1] = Math.max(res[i][1], interval[1]);
            }
        }
        res = Arrays.copyOf(res, i + 1);
        for (float[] in : res) {
            effects.add(new EffectRenderInfo(in[0], in[1], color, type));
        }
        list.clear();
    }*/

    /*@Nonnull
    public GlyphRender[] wrapGlyphs() {
        //return allList.stream().map(ProcessingGlyph::toGlyph).toArray(GlyphRenderInfo[]::new);
        return mAllList.toArray(new GlyphRender[0]);
    }*/

    /*@Nonnull
    public ColorStateInfo[] wrapColors() {
        if (colors.isEmpty()) {
            return ColorStateInfo.NO_COLOR_STATE;
        }
        return colors.toArray(new ColorStateInfo[0]);
    }*/
}
