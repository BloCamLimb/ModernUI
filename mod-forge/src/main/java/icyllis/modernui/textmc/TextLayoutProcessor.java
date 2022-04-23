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
import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.text.*;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.util.List;
import java.util.*;

/**
 * This is where the text layout is actually performed.
 */
@RenderThread
public class TextLayoutProcessor {

    /**
     * Config values.
     */
    public static volatile int sBaseFontSize = 8;

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
    private final List<BaseGlyphRender> mAllList = new ArrayList<>();

    /**
     * List of processing glyphs with same layout direction
     */
    private final List<BaseGlyphRender> mBidiList = new ArrayList<>();

    private final List<BaseGlyphRender> mTextList = new ArrayList<>();

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

    private int mShift;
    private int mNext;

    private Style mStyle;

    TextLayoutProcessor() {
    }

    private void finishBidiRun(float adjust) {
        if (adjust != 0) {
            mBidiList.forEach(e -> e.mOffsetX += adjust);
        }
        mAllList.addAll(mBidiList);
        mBidiList.clear();
    }

    private void finishTextRun(float adjust) {
        if (adjust != 0) {
            mTextList.forEach(e -> e.mOffsetX += adjust);
        }
        mBidiList.addAll(mTextList);
        mTextList.clear();
    }

    private void release() {
        mChars.clear();
        mAllList.clear();
        mCarriers.clear();
        mCarrierIndex = 0;
        mAdvance = 0;
        mLayoutRight = 0;
        mHasEffect = false;
        mShift = 0;
        mNext = 0;
        mStyle = null;
    }

    @Nonnull
    public TextRenderNode doLayout(@Nonnull String text, @Nonnull Style style) {
        char[] chars = resolveFormattingCodes(text, style);
        /*ModernUI.LOGGER.info("Layout: {}\n{}", new String(chars),
                mCarriers.stream().map(Object::toString).collect(Collectors.joining("\n")));*/
        TextRenderNode node = performFullLayout(chars, true);
        /*ModernUI.LOGGER.info("Glyphs: \n{}",
                mAllList.stream().map(Object::toString).collect(Collectors.joining("\n")));*/
        release();
        return node;
    }

    @Nonnull
    public TextRenderNode doLayout(@Nonnull FormattedText text, @Nonnull Style style) {
        text.visit(mContentBuilder, style);
        /*boolean show = !mChars.isEmpty() && mChars.getChar(0) == '[';
        if (show) {
            ModernUI.LOGGER.info("LayoutSequence: {}\n{}", new String(mChars.toCharArray()),
                    mCarriers.stream().map(Objects::toString).collect(Collectors.joining("\n")));
        }*/
        TextRenderNode node = performFullLayout(mChars.toCharArray(), false);
        /*if (show) {
            ModernUI.LOGGER.info(mAllList.stream().map(Objects::toString).collect(Collectors.joining("\n")));
        }*/
        release();
        return node;
    }

    @Nonnull
    public TextRenderNode doLayout(@Nonnull FormattedCharSequence sequence) {
        sequence.accept(mBuilderSink);
        TextRenderNode node = performFullLayout(mChars.toCharArray(), false);
        release();
        return node;
    }

    /**
     * Always LTR.
     *
     * @see FormattedTextWrapper#accept(FormattedCharSink)
     */
    private final FormattedCharSink mBuilderSink = new FormattedCharSink() {

        @Override
        public boolean accept(int index, @Nonnull Style style, int codePoint) {
            // note that index will be reset to 0 for composited char sequence
            // we should get the continuous string index
            if (style != mStyle) {
                mCarriers.add(new CharacterStyleCarrier(mChars.size(), mChars.size(), style, false));
                mStyle = style;
            }
            if (Character.isBmpCodePoint(codePoint)) {
                mChars.add((char) codePoint);
            } else {
                mChars.add(Character.highSurrogate(codePoint));
                mChars.add(Character.lowSurrogate(codePoint));
            }
            // continue
            return true;
        }
    };

    private final FormattedText.StyledContentConsumer<?> mContentBuilder = new FormattedText.StyledContentConsumer<>() {
        @Override
        public Optional<Object> accept(Style style, @Nonnull String text) {
            mCarriers.add(new CharacterStyleCarrier(mNext, mNext - mShift, style, false));
            final int limit = text.length();
            for (int i = 0; i < limit; ++i) {
                char c1 = text.charAt(i);
                if (c1 == '\u00a7') {
                    if (i + 1 >= limit) {
                        mNext++;
                        break;
                    }

                    ChatFormatting formatting = TextLayoutEngine.getFormattingByCode(text.charAt(i + 1));
                    if (formatting != null) {
                        /* Classic formatting will set all FancyStyling (like BOLD, UNDERLINE) to false if it's a color
                        formatting */
                        style = formatting == ChatFormatting.RESET ? Style.EMPTY : style.applyLegacyFormat(formatting);
                        mCarriers.add(new CharacterStyleCarrier(mNext, mNext - mShift, style, true));
                    }

                    i++;
                    mNext++;
                    mShift += 2;
                } else if (Character.isHighSurrogate(c1)) {
                    if (i + 1 >= limit) {
                        mChars.add('\uFFFD');
                        mNext++;
                        break;
                    }

                    char c2 = text.charAt(i + 1);
                    if (Character.isLowSurrogate(c2)) {
                        mChars.add(c1);
                        mChars.add(c2);
                        i++;
                        mNext++;
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
                mNext++;
            }
            // continue
            return Optional.empty();
        }
    };

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
    private char[] resolveFormattingCodes(@Nonnull String text, @Nonnull Style base) {
        int shift = 0;

        Style style = base;
        mCarriers.add(new CharacterStyleCarrier(0, 0, style, false));

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
                    style = formatting == ChatFormatting.RESET ? base : style.applyLegacyFormat(formatting);
                    mCarriers.add(new CharacterStyleCarrier(next, next - shift, style, true));
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

    @Nonnull
    private TextRenderNode performFullLayout(@Nonnull char[] text, boolean fastDigit) {
        if (text.length > 0) {
            performBidiAnalysis(text, fastDigit);
            if (!mAllList.isEmpty()) {
                adjustAndInsertColor();
                return new TextRenderNode(mAllList.toArray(new BaseGlyphRender[0]), mAdvance, mHasEffect);
            }
        }
        return new TextRenderNode(new BaseGlyphRender[0], 0, false);
    }

    /**
     * Split the full text into contiguous LTR or RTL sections by applying the Unicode Bidirectional Algorithm. Calls
     * performBidiAnalysis() for each contiguous run to perform further analysis.
     *
     * @param text the full plain text (without formatting codes) to analyze
     * @see #performStyleAnalysis(char[], int, int, boolean, boolean)
     */
    private void performBidiAnalysis(@Nonnull char[] text, boolean fastDigit) {
        /* Avoid performing full bidirectional analysis if text has no "strong" right-to-left characters */
        if (Bidi.requiresBidi(text, 0, text.length)) {
            /* Note that while requiresBidi() uses start/limit the Bidi constructor uses start/length */
            Bidi bidi = new Bidi(text, 0, null, 0, text.length, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);

            /* If text is entirely right-to-left, then insert an EntryText node for the entire string */
            if (bidi.isRightToLeft()) {
                performStyleAnalysis(text, 0, text.length, true, fastDigit);
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
                            (bidi.getRunLevel(logicalIndex) & 1) != 0, fastDigit);
                }
            }
        }

        /* If text is entirely left-to-right, then insert an node for the entire string */
        else {
            performStyleAnalysis(text, 0, text.length, false, fastDigit);
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
    private void performStyleAnalysis(@Nonnull char[] text, int start, int limit, boolean isRtl, boolean fastDigit) {
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
                if (carrier.mStripIndex == c.mStripIndex) {
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
            performFontAnalysis(text, start, next, isRtl, fastDigit, carrier);
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
    private void performFontAnalysis(@Nonnull char[] text, int start, int limit, boolean isRtl, boolean fastDigit,
                                     @Nonnull CharacterStyleCarrier carrier) {
        /*
         * Convert all digits in the string to a '0' before layout to ensure that any glyphs replaced on the fly will
         * all have the same positions. Under Windows, Java's "SansSerif" logical font uses the "Arial" font for
         * digits, in which the "1" digit is slightly narrower than all other digits. Digits are not on SMP.
         */
        if (fastDigit) {
            for (int i = start; i < limit; i++) {
                if (text[i] <= '9' && text[i] >= '0') {
                    text[i] = '0';
                }
            }
        }

        List<FontRun> items = ModernUI.get().getPreferredTypeface().itemize(text, start, limit);
        for (FontRun run : items) {
            performTextLayout(text, run.getStart(), run.getEnd(), isRtl, fastDigit, carrier, run.getFont());
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
    private void performTextLayout(@Nonnull char[] text, int start, int limit, boolean isRtl, boolean fastDigit,
                                   @Nonnull CharacterStyleCarrier carrier, @Nonnull Font font) {
        final TextLayoutEngine layoutEngine = TextLayoutEngine.getInstance();
        final int decoration = carrier.getEffect();
        final int fontStyle = carrier.getFontStyle();
        // Note max font size is 96, see FontPaint, font size will be (8 * res) in Minecraft
        final float res = layoutEngine.getResolutionLevel();
        // Convert to float form for calculation, but actually an integer
        final float scale = (int) Minecraft.getInstance().getWindow().getGuiScale();
        int style = Font.PLAIN;
        if ((fontStyle & CharacterStyleCarrier.BOLD) != 0) {
            style |= Font.BOLD;
        }
        if ((fontStyle & CharacterStyleCarrier.ITALIC) != 0) {
            style |= Font.ITALIC;
        }
        font = font.deriveFont(style, Math.min(sBaseFontSize * res, 96));
        if (carrier.isObfuscated()) {
            final var digits = layoutEngine.lookupDigits(font);
            final float advance = digits.getValue()[0];

            float offset;
            if (isRtl) {
                offset = mLayoutRight;
            } else {
                offset = mAdvance;
            }

            /* Process code point */
            for (int i = start; i < limit; i++) {
                float renderOffset = GlyphManager.sBitmapLike ? Math.round(offset * scale) / scale : offset;
                mTextList.add(new RandomGlyphRender(start + i, advance, renderOffset, decoration, digits));

                offset += advance;

                char c1 = text[i];
                if (i + 1 < limit && Character.isHighSurrogate(c1)) {
                    char c2 = text[i + 1];
                    if (Character.isLowSurrogate(c2)) {
                        ++i;
                    }
                }
                mHasEffect |= decoration != 0;
            }

            // get offset in this run
            if (isRtl) {
                offset -= mLayoutRight;
            } else {
                offset -= mAdvance;
            }

            mAdvance += offset;

            if (isRtl) {
                finishTextRun(-offset);
                mLayoutRight -= offset;
            } else {
                finishTextRun(0);
            }
        } else {
            // The glyphCode matched to the same codePoint is specified in the font, they are different
            // in different font, HarfBuzz is introduced in Java 11 or higher
            GlyphManager glyphManager = GlyphManager.getInstance();

            GlyphVector vector = glyphManager.layoutGlyphVector(font, text, start, limit, isRtl);
            final int num = vector.getNumGlyphs();

            final var digits = layoutEngine.lookupDigits(font);

            float offset = (float) vector.getGlyphPosition(0).getX();
            float nextOffset = 0;
            for (int i = 0; i < num; i++, offset = nextOffset) {
                /*
                 * Back compatibility for Java 8, since LayoutGlyphVector should not have non-standard glyphs
                 * HarfBuzz is introduced in Java 11 or higher
                 */
                /*if (vector.getGlyphMetrics(i).getAdvanceX() == 0 &&
                        vector.getGlyphMetrics(i).getBounds2D().getWidth() == 0) {
                    continue;
                }*/

                int stripIndex = vector.getGlyphCharIndex(i) + start;

                nextOffset = (float) (vector.getGlyphPosition(i + 1).getX() / res);
                float advance = nextOffset - offset;

                if (isRtl) {
                    offset += mLayoutRight;
                } else {
                    offset += mAdvance;
                }

                // Align with a full pixel
                float renderOffset = GlyphManager.sBitmapLike ? Math.round(offset * scale) / scale : offset;

                // Digits are not on SMP
                if (fastDigit && text[stripIndex] == '0') {
                    mTextList.add(new DigitGlyphRender(stripIndex, renderOffset, advance, decoration, digits));
                    mHasEffect |= decoration != 0;
                    continue;
                }

                int glyphCode = vector.getGlyphCode(i);
                TexturedGlyph glyph = glyphManager.lookupGlyph(font, glyphCode);

                mTextList.add(new StandardGlyphRender(stripIndex, renderOffset, advance, decoration, glyph));
                if (glyph != null) {
                    mHasEffect |= decoration != 0;
                }
            }

            mAdvance += nextOffset;

            if (isRtl) {
                finishTextRun(-nextOffset);
                mLayoutRight -= nextOffset;
            } else {
                finishTextRun(0);
            }
        }
    }

    /**
     * Adjust strip index to string index and insert color transitions.
     *
     * <p>
     * Example:<br>
     * 0123456§a78§r9
     * <p>
     * Carriers: stringIndex (stripIndex)<br>
     * 0 - grey (default style)<br>
     * 3 - blue (style)<br>
     * 7 (7) - orange (formatting code)<br>
     * 11 (9) - grey (formatting code)<br>
     * After:
     * <table border="1">
     *   <tr>
     *     <td>0</th>
     *     <td>1</th>
     *     <td>2</th>
     *     <td>3</th>
     *     <td>4</th>
     *     <td>5</th>
     *     <td>6</th>
     *     <td>7</th>
     *     <td>8</th>
     *     <td>9</th>
     *   </tr>
     *   <tr>
     *     <td>0</th>
     *     <td>1</th>
     *     <td>2</th>
     *     <td>3</th>
     *     <td>4</th>
     *     <td>5</th>
     *     <td>6</th>
     *     <td>9</th>
     *     <td>10</th>
     *     <td>13</th>
     *   </tr>
     *   <tr>
     *     <td>G</th>
     *     <td></th>
     *     <td></th>
     *     <td>B</th>
     *     <td></th>
     *     <td></th>
     *     <td></th>
     *     <td>O</th>
     *     <td></th>
     *     <td>G</th>
     *   </tr>
     * </table>
     */
    private void adjustAndInsertColor() {
        if (mAllList.isEmpty()) {
            throw new IllegalStateException();
        }
        // Sort by stripIndex, if mixed with LTR and RTL layout.
        // Logical order to visual order for line breaking, etc.
        mAllList.sort(Comparator.comparingInt(g -> g.mStringIndex));

        // Shift stripIndex to stringIndex
        int codeIndex = 0, shift = 0;
        for (BaseGlyphRender glyph : mAllList) {
            /*
             * Adjust the string index for each glyph to point into the original string with un-stripped color codes.
             *  The while
             * loop is necessary to handle multiple consecutive color codes with no visible glyphs between them.
             * These new adjusted
             * stringIndex can now be compared against the color stringIndex during rendering. It also allows lookups
             *  of ASCII
             * digits in the original string for fast glyph replacement during rendering.
             */
            CharacterStyleCarrier carrier;
            while (codeIndex < mCarriers.size() &&
                    glyph.mStringIndex + shift >= (carrier = mCarriers.get(codeIndex)).mStringIndex) {
                if (carrier.isFormattingCode()) {
                    shift += 2;
                }
                codeIndex++;
            }
            glyph.mStringIndex += shift;
        }


        // insert color flags
        codeIndex = 0;
        CharacterStyleCarrier carrier = mCarriers.get(codeIndex);
        // In case of multiple consecutive color codes with the same stripIndex,
        // select the last one which will have active carrier
        while (codeIndex < mCarriers.size() - 1) {
            CharacterStyleCarrier c = mCarriers.get(codeIndex + 1);
            if (carrier.mStripIndex == c.mStripIndex) {
                carrier = c;
                codeIndex++;
            } else {
                break;
            }
        }

        int color = carrier.getColor();
        BaseGlyphRender glyph;
        // If there's no input style at the beginning
        if (color != CharacterStyleCarrier.NO_COLOR_SPECIFIED) {
            glyph = mAllList.get(0);
            glyph.mFlags &= ~BaseGlyphRender.COLOR_NO_CHANGE;
            glyph.mFlags |= color;
        }

        if (++codeIndex >= mCarriers.size()) {
            return;
        }
        for (int glyphIndex = 1; glyphIndex < mAllList.size() && codeIndex < mCarriers.size(); glyphIndex++) {
            carrier = mCarriers.get(codeIndex);

            // In case of multiple consecutive color codes with the same stripIndex,
            // select the last one which will have active carrier,
            // don't compare indices with formatting codes
            while (codeIndex < mCarriers.size() - 1) {
                CharacterStyleCarrier c = mCarriers.get(codeIndex + 1);
                if (carrier.mStripIndex == c.mStripIndex) {
                    carrier = c;
                    codeIndex++;
                } else {
                    break;
                }
            }

            glyph = mAllList.get(glyphIndex);

            // Can be equal, not formatting code in this case
            if (glyph.mStringIndex >= carrier.mStringIndex) {
                if (carrier.getColor() != color) {
                    color = carrier.getColor();
                    glyph.mFlags &= ~BaseGlyphRender.COLOR_NO_CHANGE;
                    glyph.mFlags |= color;
                }

                codeIndex++;
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
