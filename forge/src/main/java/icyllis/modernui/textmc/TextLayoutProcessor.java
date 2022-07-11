/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

import com.ibm.icu.text.*;
import com.ibm.icu.util.ULocale;
import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.graphics.font.*;
import icyllis.modernui.text.TextDirectionHeuristic;
import icyllis.modernui.text.TextDirectionHeuristics;
import icyllis.modernui.textmc.mixin.*;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.*;

/**
 * This is where the text layout is actually performed.
 *
 * @see GlyphManager
 * @see CharacterStyle
 * @see MixinBidiReorder
 * @see MixinClientLanguage
 * @see MixinLanguage
 * @see VanillaTextKey#update(String, Style)
 * @see MultilayerTextKey.Lookup#update(FormattedCharSequence)
 * @see FormattedText
 * @see FormattedCharSequence
 * @see FormattedTextWrapper
 * @see net.minecraft.client.gui.Font
 * @see net.minecraft.client.StringSplitter
 * @see net.minecraft.util.StringDecomposer
 * @see net.minecraft.network.chat.Component
 * @see net.minecraft.network.chat.SubStringSource
 * @see net.minecraft.client.resources.language.FormattedBidiReorder
 * @see icyllis.modernui.graphics.font.GraphemeBreak
 * @see icyllis.modernui.graphics.font.LineBreaker
 */
@RenderThread
public class TextLayoutProcessor {

    /**
     * Compile-time only.
     */
    public static final boolean DEBUG = false;
    //public static final char REPLACEMENT_CHAR = '\uFFFD';

    /**
     * Config values.
     */
    public static volatile int sBaseFontSize = 8;
    public static volatile boolean sAlignPixels = false;

    private final TextLayoutEngine mEngine;

    /**
     * Char array builder. Formatting codes will be stripped from this array.
     */
    private final CharSequenceBuilder mBuilder = new CharSequenceBuilder();
    /**
     * Style appearance flags in logical order. Same indexing with {@link #mBuilder}.
     */
    private final IntArrayList mStyles = new IntArrayList();

    /*
     * Array of temporary style carriers.
     */
    //private final List<CharacterStyle> mDStyles = new ArrayList<>();

    /**
     * All baked glyphs for rendering, empty glyphs have been removed from this array.
     * The order is visually left-to-right (i.e. in visual order).
     */
    private final List<GLBakedGlyph> mGlyphs = new ArrayList<>();
    /**
     * Glyphs to relative char indices of the strip string (without formatting codes).
     * For vanilla layout ({@link VanillaTextKey} and {@link TextLayoutEngine#lookupVanillaNode(String)}),
     * these will be adjusted to string index (with formatting codes).
     * Same indexing with {@link #mGlyphs}, in visual order.
     */
    private final IntArrayList mCharIndices = new IntArrayList();
    /**
     * Position x1 y1 x2 y2... relative to the same point, for rendering glyphs.
     * These values are not offset to glyph additional baseline but aligned.
     * Same indexing with {@link #mGlyphs}, align to left, in visual order.
     * <p>
     * Note the values are scaled to Minecraft GUI coordinates.
     */
    private final FloatArrayList mPositions = new FloatArrayList();
    /**
     * The length and order are relative to the raw string (with formatting codes).
     * Only grapheme cluster bounds have advances, others are zeros. For example:
     * [13.57, 0, 14.26, 0, 0]. {@link #mGlyphs}.length may less than grapheme cluster
     * count (invisible glyphs are removed). Logical order.
     * <p>
     * Note the values are scaled to Minecraft GUI coordinates.
     */
    private final FloatArrayList mAdvances = new FloatArrayList();
    /**
     * Glyph rendering flags. Same indexing with {@link #mGlyphs}, in visual order.
     */
    /*
     * lower 24 bits - RGB color, ignored when has USE_PARAM_COLOR bit
     * higher 8 bits
     * |--------|
     *         1  BOLD
     *        1   ITALIC
     *       1    UNDERLINE
     *      1     STRIKETHROUGH
     *     1      OBFUSCATED
     *    1       FORMATTING_CODE
     *   1        FAST_DIGIT_REPLACEMENT
     *  1         NO_COLOR_SPECIFIED
     * |--------|
     */
    private final IntArrayList mFlags = new IntArrayList();
    /**
     * Strip indices that are boundaries for Unicode line breaking, this list will be
     * sorted into logical order. 0 is not included.
     */
    private final IntArrayList mLineBoundaries = new IntArrayList();

    /*
     * List of all processing glyphs
     */
    //private final List<BaseGlyphRender> mAllList = new ArrayList<>();

    /*
     * List of processing glyphs with same layout direction
     */
    //private final List<BaseGlyphRender> mBidiList = new ArrayList<>();

    //private final List<BaseGlyphRender> mTextList = new ArrayList<>();

    /*
     * All color states
     */
    //public final List<ColorStateInfo> colors = new ArrayList<>();

    /*
     * Indicates current style index in {@link #mDStyles} for layout processing
     */
    //private int mStyleIndex;

    /**
     * The total advance (horizontal width) of the processing text
     */
    private float mAdvance;

    /*
     * Needed in RTL layout
     */
    //private float mLayoutRight;

    /**
     * Mark whether this node should enable effect rendering
     */
    private boolean mHasEffect;
    private boolean mHasFastDigit;

    /**
     * Always LTR.
     *
     * @see FormattedTextWrapper#accept(FormattedCharSink)
     */
    private final FormattedCharSink mSequenceBuilder = (index, style, codePoint) -> {
        int flags = CharacterStyle.getAppearanceFlags(style);
        mStyles.add(flags);
        if (mBuilder.addCodePoint(codePoint) > 1) {
            mStyles.add(flags);
        }
        return true;
    };

    /**
     * Transfer code points in logical order.
     */
    private final FormattedText.StyledContentConsumer<Unit> mContentBuilder = (style, text) ->
            StringDecomposer.iterateFormatted(text, style, mSequenceBuilder) ? Optional.empty()
                    : FormattedText.STOP_ITERATION;

    /*private class ContentBuilder implements FormattedText.StyledContentConsumer<Object> {

        private int mShift;
        private int mNext;

        @Nonnull
        @Override
        public Optional<Object> accept(@Nonnull Style base, @Nonnull String text) {
            Style style = base;
            mStyles.add(new CharacterStyle(mNext, mNext - mShift, style, false));

            // also fix surrogate pairs
            final int limit = text.length();
            for (int i = 0; i < limit; ++i) {
                char c1 = text.charAt(i);
                if (c1 == ChatFormatting.PREFIX_CODE) {
                    if (i + 1 >= limit) {
                        mNext++;
                        break;
                    }

                    ChatFormatting formatting = TextLayoutEngine.getFormattingByCode(text.charAt(i + 1));
                    if (formatting != null) {
                        *//* Classic formatting will set all FancyStyling (like BOLD, UNDERLINE) to false if it's a
                        color
                        formatting *//*
                        style = formatting == ChatFormatting.RESET ? base : style.applyLegacyFormat(formatting);
                        mStyles.add(new CharacterStyle(mNext, mNext - mShift, style, true));
                    }

                    i++;
                    mNext++;
                    mShift += 2;
                } else if (Character.isHighSurrogate(c1)) {
                    if (i + 1 >= limit) {
                        mBuilder.addChar(REPLACEMENT_CHAR);
                        mNext++;
                        break;
                    }

                    char c2 = text.charAt(i + 1);
                    if (Character.isLowSurrogate(c2)) {
                        mBuilder.addChar(c1);
                        mBuilder.addChar(c2);
                        i++;
                        mNext++;
                    } else if (Character.isSurrogate(c1)) {
                        mBuilder.addChar(REPLACEMENT_CHAR);
                    } else {
                        mBuilder.addChar(c1);
                    }
                } else if (Character.isSurrogate(c1)) {
                    mBuilder.addChar(REPLACEMENT_CHAR);
                } else {
                    mBuilder.addChar(c1);
                }
                mNext++;
            }
            // continue
            return Optional.empty();
        }

        private void reset() {
            mShift = 0;
            mNext = 0;
        }
    }*/

    /*private class SequenceBuilder implements FormattedCharSink {

        private Style mStyle;

        @Override
        public boolean accept(int index, @Nonnull Style style, int codePoint) {
            // note that index will be reset to 0 for composited char sequence
            // we should get the continuous string index
            if (mStyle == null || CharacterStyle.affectsAppearance(mStyle, style)) {
                mDStyles.add(new CharacterStyle(mCharBuilder.length(), mCharBuilder.length(), style, false));
                mStyle = style;
            } else {
                mStyles.add(mStyles.getInt(mStyles.size() - 1));
            }
            int flags = CharacterStyle.getAppearanceFlags(style);
            if (mBuilder.addCodePoint(codePoint) > 1) {
                mStyles.add(flags);
            }
            mStyles.add(flags);
            return true;
        }

        private void reset() {
            mStyle = null;
        }
    }*/

    public TextLayoutProcessor(@Nonnull TextLayoutEngine engine) {
        mEngine = engine;
    }

    /*private void finishBidiRun(float adjust) {
        if (adjust != 0) {
            mBidiList.forEach(e -> e.mOffsetX += adjust);
        }
        mAllList.addAll(mBidiList);
        mBidiList.clear();
    }

    private void finishFontRun(float adjust) {
        if (adjust != 0) {
            mTextList.forEach(e -> e.mOffsetX += adjust);
        }
        mBidiList.addAll(mTextList);
        mTextList.clear();
    }*/

    private void reset() {
        if (DEBUG) {
            if (mBuilder.length() != mStyles.size()) {
                throw new IllegalStateException();
            }
            if (mGlyphs.size() != mCharIndices.size()) {
                throw new IllegalStateException();
            }
            if (mGlyphs.size() * 2 != mPositions.size()) {
                throw new IllegalStateException();
            }
            if (mBuilder.length() != mAdvances.size()) {
                throw new IllegalStateException();
            }
            if (mGlyphs.size() != mFlags.size()) {
                throw new IllegalStateException();
            }
            if (mBuilder.length() != mLineBoundaries.getInt(mLineBoundaries.size() - 1)) {
                ModernUI.LOGGER.error("Last char cannot break line?");
            }
            if (Math.abs(mAdvances.doubleStream().sum() - mAdvance) > 1) {
                ModernUI.LOGGER.error("Advance error is too large?");
            }
        }
        mBuilder.clear();
        mStyles.clear();
        mGlyphs.clear();
        mCharIndices.clear();
        mPositions.clear();
        mAdvances.clear();
        mFlags.clear();
        mLineBoundaries.clear();
        mAdvance = 0;
        mHasEffect = false;
        mHasFastDigit = false;
    }

    @Nonnull
    public TextRenderNode performVanillaLayout(@Nonnull String text, @Nonnull Style style) {
        StringDecomposer.iterateFormatted(text, style, mSequenceBuilder);
        TextRenderNode node = performFullLayout(text);
        if (DEBUG) {
            ModernUI.LOGGER.info("Performed Vanilla Layout: {}, {}", mBuilder.toString(), node);
        }
        reset();
        return node;
    }

    @Nonnull
    public TextRenderNode performComplexLayout(@Nonnull FormattedText text, @Nonnull Style style) {
        text.visit(mContentBuilder, style);
        TextRenderNode node = performFullLayout(null);
        if (DEBUG) {
            ModernUI.LOGGER.info("Performed Complex Layout: {}, {}", mBuilder.toString(), node);
        }
        reset();
        return node;
    }

    @Nonnull
    public TextRenderNode performSequenceLayout(@Nonnull FormattedCharSequence sequence) {
        sequence.accept(mSequenceBuilder);
        TextRenderNode node = performFullLayout(null);
        if (DEBUG) {
            ModernUI.LOGGER.info("Performed Sequence Layout: {}, {}", mBuilder.toString(), node);
        }
        reset();
        return node;
    }

    /*
     * Formatting codes are not involved in rendering, so we should first extract formatting codes
     * from the raw string into a stripped text. The color codes must be removed for a font's
     * context-sensitive glyph substitution to work (like Arabic letter middle form) or Bidi analysis.
     * Results a new char array with all formatting codes removed from the given string.
     *
     * @param text      raw string with formatting codes to strip
     * @param baseStyle the base style if no formatting applied (initial or reset)
     * @see net.minecraft.util.StringDecomposer
     */
    /*private void buildVanilla(@Nonnull String text, @Nonnull final Style baseStyle) {
        int shift = 0;

        Style style = baseStyle;
        mDStyles.add(new CharacterStyle(0, 0, style, false));

        // also fix invalid surrogate pairs
        final int limit = text.length();
        for (int pos = 0; pos < limit; ++pos) {
            char c1 = text.charAt(pos);
            if (c1 == ChatFormatting.PREFIX_CODE) {
                if (pos + 1 >= limit) {
                    break;
                }

                ChatFormatting formatting = TextLayoutEngine.getFormattingByCode(text.charAt(pos + 1));
                if (formatting != null) {
                    *//* Classic formatting will set all FancyStyling (like BOLD, UNDERLINE) to false if it's a color
                    formatting *//*
                    style = formatting == ChatFormatting.RESET ? baseStyle : style.applyLegacyFormat(formatting);
                    mDStyles.add(new CharacterStyle(pos, pos - shift, style, true));
                }

                pos++;
                shift += 2;
            } else if (Character.isHighSurrogate(c1)) {
                if (pos + 1 >= limit) {
                    mBuilder.addChar(REPLACEMENT_CHAR);
                    break;
                }

                char c2 = text.charAt(pos + 1);
                if (Character.isLowSurrogate(c2)) {
                    mBuilder.addChar(c1);
                    mBuilder.addChar(c2);
                    ++pos;
                } else if (Character.isSurrogate(c1)) {
                    mBuilder.addChar(REPLACEMENT_CHAR);
                } else {
                    mBuilder.addChar(c1);
                }
            } else if (Character.isSurrogate(c1)) {
                mBuilder.addChar(REPLACEMENT_CHAR);
            } else {
                mBuilder.addChar(c1);
            }
        }

        *//*while ((next = string.indexOf('\u00a7', start)) != -1 && next + 1 < string.length()) {
            TextFormatting formatting = fromFormattingCode(string.charAt(next + 1));

            *//**//*
     * Remove the two char color code from text[] by shifting the remaining data in the array over on top of it.
     * The "start" and "next" variables all contain offsets into the original unmodified "str" string. The "shift"
     * variable keeps track of how many characters have been stripped so far, and it's used to compute offsets into
     * the text[] array based on the start/next offsets in the original string.
     *
     * If string only contains 1 formatting code (2 chars in total), this doesn't work
     *//**//*
            //System.arraycopy(text, next - shift + 2, text, next - shift, text.length - next - 2);

            if (formatting != null) {
                *//**//* forceFormatting will set all FancyStyling (like BOLD, UNDERLINE) to false if this is a color
                formatting *//**//*
                style = style.forceFormatting(formatting);


                data.codes.add(new FormattingStyle(next, next - shift, style));
            }

            start = next + 2;
            shift += 2;
        }*//*
    }*/

    /**
     * Perform text layout after building stripped characters.
     *
     * @return the full layout result
     */
    @Nonnull
    private TextRenderNode performFullLayout(@Nullable String raw) {
        if (!mBuilder.isEmpty()) {
            final boolean fastDigit = raw != null;
            final ULocale locale = ULocale.forLocale(ModernUI.getSelectedLocale());
            mAdvances.size(mBuilder.length());
            final char[] textBuf = mBuilder.toCharArray();
            performBidiAnalysis(textBuf, fastDigit, locale);
            if (mAdvance > 0) {
                if (fastDigit) {
                    adjustForFastDigit(raw);
                }
                mLineBoundaries.sort(IntComparators.NATURAL_COMPARATOR);
                return new TextRenderNode(textBuf, mGlyphs.toArray(new GLBakedGlyph[0]), mCharIndices.toIntArray(),
                        mPositions.toFloatArray(), mAdvances.toFloatArray(), mFlags.toIntArray(),
                        mLineBoundaries.toIntArray(), mAdvance, mHasEffect);
            }
        }
        return TextRenderNode.makeEmpty();
    }

    /**
     * Split the full text into contiguous LTR or RTL sections by applying the Unicode Bidirectional Algorithm. Calls
     * performBidiAnalysis() for each contiguous run to perform further analysis.
     *
     * @param text      the full plain text (without formatting codes) to analyze in logical order
     * @param fastDigit whether to use fast digit replacement
     * @param locale    the ICU locale for grapheme cluster break
     * @see #performBidiRun(char[], int, int, boolean, boolean, ULocale)
     */
    private void performBidiAnalysis(@Nonnull char[] text, boolean fastDigit, @Nonnull ULocale locale) {
        TextDirectionHeuristic dir = mEngine.getTextDirectionHeuristic();
        /* Avoid performing full bidirectional analysis if text has no "strong" right-to-left characters */
        if ((dir == TextDirectionHeuristics.LTR
                || dir == TextDirectionHeuristics.FIRSTSTRONG_LTR
                || dir == TextDirectionHeuristics.ANYRTL_LTR)
                && !Bidi.requiresBidi(text, 0, text.length)) {
            /* If text is entirely left-to-right, then insert a node for the entire string */
            if (DEBUG) {
                ModernUI.LOGGER.info("All LTR");
            }
            performBidiRun(text, 0, text.length, false, fastDigit, locale);
        } else {
            final byte paraLevel;
            if (dir == TextDirectionHeuristics.LTR) {
                paraLevel = Bidi.LTR;
            } else if (dir == TextDirectionHeuristics.RTL) {
                paraLevel = Bidi.RTL;
            } else if (dir == TextDirectionHeuristics.FIRSTSTRONG_LTR) {
                paraLevel = Bidi.LEVEL_DEFAULT_LTR;
            } else if (dir == TextDirectionHeuristics.FIRSTSTRONG_RTL) {
                paraLevel = Bidi.LEVEL_DEFAULT_RTL;
            } else {
                final boolean isRtl = dir.isRtl(text, 0, text.length);
                paraLevel = isRtl ? Bidi.RTL : Bidi.LTR;
            }
            Bidi bidi = new Bidi(text.length, 0);
            bidi.setPara(text, paraLevel, null);

            /* If text is entirely right-to-left, then insert a node for the entire string */
            if (bidi.isRightToLeft()) {
                if (DEBUG) {
                    ModernUI.LOGGER.info("All RTL (analysis)");
                }
                performBidiRun(text, 0, text.length, true, fastDigit, locale);
            }
            /* If text is entirely left-to-right, then insert a node for the entire string */
            else if (bidi.isLeftToRight()) {
                if (DEBUG) {
                    ModernUI.LOGGER.info("All LTR (analysis)");
                }
                performBidiRun(text, 0, text.length, false, fastDigit, locale);
            }
            /* Otherwise text has a mixture of LTR and RLT, and it requires full bidirectional analysis */
            else {
                int runCount = bidi.getRunCount();
                //byte[] runs = new byte[runCount];

                /* Reorder contiguous runs of text into their display order from left to right */
                /*for (int i = 0; i < runCount; i++) {
                    runs[i] = (byte) bidi.getRunLevel(i);
                }
                int[] indexMap = Bidi.reorderVisual(runs);*/

                /*
                 * Every GlyphVector must be created on a contiguous run of left-to-right or right-to-left text. Keep
                 * track of the horizontal advance between each run of text, so that the glyphs in each run can be
                 * assigned a position relative to the start of the entire string and not just relative to that run.
                 */
                for (int visualIndex = 0; visualIndex < runCount; visualIndex++) {
                    /*int logicalIndex = indexMap[visualIndex];
                    performBidiRun(text, bidi.getRunStart(logicalIndex), bidi.getRunLimit(logicalIndex),
                            (bidi.getRunLevel(logicalIndex) & 1) != 0, fastDigit);*/

                    /* An odd numbered level indicates right-to-left ordering */
                    BidiRun run = bidi.getVisualRun(visualIndex);
                    if (DEBUG) {
                        ModernUI.LOGGER.info("VisualRun {}, {}", visualIndex, run);
                    }
                    performBidiRun(text, run.getStart(), run.getLimit(), run.isOddRun(), fastDigit, locale);
                }
            }
        }
    }

    /**
     * Analyze the best matching font and paragraph context, according to layout direction and generate glyph vector.
     * In some languages, the original Unicode code is mapped to another Unicode code for visual rendering.
     * They will finally be converted into glyph codes according to different Font. This run is in visual order.
     *
     * @param text      the plain text (without formatting codes) in logical order
     * @param start     start index (inclusive) of the text
     * @param limit     end index (exclusive) of the text
     * @param isRtl     layout direction, either {@link Font#LAYOUT_LEFT_TO_RIGHT} or {@link Font#LAYOUT_RIGHT_TO_LEFT}
     * @param fastDigit whether to use fast digit replacement
     * @param locale    the ICU locale for grapheme cluster break
     * @see #performStyleRun(char[], int, int, boolean, boolean, int, ULocale)
     */
    private void performBidiRun(@Nonnull char[] text, int start, int limit, boolean isRtl, boolean fastDigit,
                                @Nonnull ULocale locale) {
        final IntArrayList styles = mStyles;
        int lastPos, currPos;
        int lastStyle, currStyle;
        // Style runs are in visual order
        if (isRtl) {
            lastPos = limit;
            currPos = limit - 1;
            lastStyle = styles.getInt(currPos);
            currStyle = lastStyle;
            while (currPos > start) {
                if ((currStyle = styles.getInt(currPos - 1)) != lastStyle) {
                    performStyleRun(text, currPos, lastPos, true, fastDigit, lastStyle, locale);
                    lastPos = currPos;
                    lastStyle = currStyle;
                }
                currPos--;
            }
            assert currPos == start;
            performStyleRun(text, currPos, lastPos, true, fastDigit, currStyle, locale);
        } else {
            lastPos = start;
            currPos = start;
            lastStyle = styles.getInt(currPos);
            currStyle = lastStyle;
            while (currPos + 1 < limit) {
                currPos++;
                if ((currStyle = styles.getInt(currPos)) != lastStyle) {
                    performStyleRun(text, lastPos, currPos, false, fastDigit, lastStyle, locale);
                    lastPos = currPos;
                    lastStyle = currStyle;
                }
            }
            assert currPos + 1 == limit;
            performStyleRun(text, lastPos, currPos + 1, false, fastDigit, currStyle, locale);
        }

        /*float lastAdvance = mAdvance;
        if (isRtl) {
            mLayoutRight = lastAdvance;
        }
        final int carrierLimit = mDStyles.size() - 1;

        // Break up the string into segments, where each segment has the same font style in use
        while (start < limit) {
            int next = limit;

            CharacterStyle carrier = mDStyles.get(mStyleIndex);

            // remove empty styles in case of multiple consecutive carriers with the same stripIndex,
            // select the last one which will have active font style
            while (mStyleIndex < carrierLimit) {
                CharacterStyle c = mDStyles.get(mStyleIndex + 1);
                if (carrier.mStripIndex == c.mStripIndex) {
                    carrier = c;
                    mStyleIndex++;
                } else {
                    break;
                }
            }*/

        /*
         * Search for the next FormattingCode that uses a different layoutStyle than the current one. If found,
         * the stripIndex of that
         * new code is the split point where the string must be split into a separately styled segment.
         */
            /*while (mStyleIndex < carrierLimit) {
                mStyleIndex++;
                CharacterStyle c = mDStyles.get(mStyleIndex);
                if (c.affectsLayout(carrier)) {
                    next = c.mStripIndex;
                    break;
                }
            }*/

        /* Layout the string segment with the style currently selected by the last color code */
            /*performStyleRun(text, start, next, isRtl, fastDigit, carrier, locale);
            start = next;
        }

        if (isRtl) {
            finishBidiRun(mAdvance - lastAdvance);
        } else {
            finishBidiRun(0);
        }*/
    }

    /**
     * Analyze the best matching font and paragraph context, according to layout direction and generate glyph vector.
     * In some languages, the original Unicode code is mapped to another Unicode code for visual rendering.
     * They will finally be converted into glyph codes according to different Font. This run is in visual order.
     *
     * @param text       the plain text (without formatting codes) to analyze in logical order
     * @param start      start index (inclusive) of the text
     * @param limit      end index (exclusive) of the text
     * @param isRtl      layout direction, either {@link Font#LAYOUT_LEFT_TO_RIGHT} or {@link Font#LAYOUT_RIGHT_TO_LEFT}
     * @param fastDigit  whether to use fast digit replacement
     * @param styleFlags the style to lay out the text
     * @param locale     the ICU locale for grapheme cluster break
     * @see FontCollection#itemize(char[], int, int)
     * @see #performFontRun(char[], int, int, boolean, boolean, int, Font, ULocale)
     */
    private void performStyleRun(@Nonnull char[] text, int start, int limit, boolean isRtl, boolean fastDigit,
                                 int styleFlags, @Nonnull ULocale locale) {
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

        int fontStyle = Font.PLAIN;
        if ((styleFlags & CharacterStyle.BOLD) != 0) {
            fontStyle |= Font.BOLD;
        }
        if ((styleFlags & CharacterStyle.ITALIC) != 0) {
            fontStyle |= Font.ITALIC;
        }
        // Note max font size is 96, see FontPaint, font size will be (8 * res) in Minecraft by default
        float fontSize = Math.min(sBaseFontSize * mEngine.getResolutionLevel(), 96);

        final var items = ModernUI.getSelectedTypeface().getFontCollection().itemize(text, start, limit);
        // Font runs are in visual order
        for (int runIndex = isRtl ? items.size() - 1 : 0; isRtl ? runIndex >= 0 : runIndex < items.size(); ) {
            var run = items.get(runIndex);

            Font derived = run.getFont().deriveFont(fontStyle, fontSize);
            performFontRun(text, run.getStart(), run.getEnd(), isRtl, fastDigit, styleFlags, derived, locale);

            if (isRtl) {
                runIndex--;
            } else {
                runIndex++;
            }
        }
    }

    /**
     * Finally, we got a piece of text with same layout direction, font style and whether to be obfuscated.
     * This run is in visual order.
     *
     * @param text       the plain text (without formatting codes) to analyze in logical order
     * @param start      start index (inclusive) of the text
     * @param limit      end index (exclusive) of the text
     * @param isRtl      layout direction, either {@link Font#LAYOUT_LEFT_TO_RIGHT} or {@link Font#LAYOUT_RIGHT_TO_LEFT}
     * @param fastDigit  whether to use fast digit replacement
     * @param styleFlags the style to lay out the text (underline, strikethrough, obfuscated, color)
     * @param font       the derived font with fontStyle and fontSize
     * @param locale     the ICU locale for grapheme cluster break
     */
    private void performFontRun(@Nonnull char[] text, int start, int limit, boolean isRtl, boolean fastDigit,
                                int styleFlags, @Nonnull Font font, @Nonnull ULocale locale) {
        final boolean hasEffect = (styleFlags & CharacterStyle.EFFECT_MASK) != 0;
        // Convert to float form for calculation, but actually an integer
        final float scale = mEngine.getCoordinateScale();

        if ((styleFlags & CharacterStyle.OBFUSCATED) != 0) {
            final TextLayoutEngine.FastCharSet fastChars = mEngine.lookupFastChars(font);
            final boolean alignPixels = sAlignPixels;
            final float advance = fastChars.offsets[0];

            float offset = mAdvance;
            // Process code point in visual order
            for (int i = start; i < limit; i++) {
                mAdvances.set(i, advance);

                float pos = offset;
                if (alignPixels) {
                    pos = Math.round(pos * scale) / scale;
                }

                mGlyphs.add(fastChars);
                mCharIndices.add(i + start);
                mPositions.add(pos);
                mPositions.add(0);
                mFlags.add(styleFlags);
                mHasEffect |= hasEffect;

                offset += advance;

                char c1 = text[i];
                if (i + 1 < limit && Character.isHighSurrogate(c1)) {
                    char c2 = text[i + 1];
                    if (Character.isLowSurrogate(c2)) {
                        ++i;
                    }
                }
            }

            mAdvance = offset;

            /*if (isRtl) {
                finishFontRun(-offset);
                mLayoutRight -= offset;
            } else {
                finishFontRun(0);
            }*/
        } else {
            final float level = mEngine.getResolutionLevel();
            // HarfBuzz is introduced in Java 11 or higher, perform measure and layout below
            final GlyphManager glyphManager = mEngine.getGlyphManager();

            // Measure grapheme cluster in logical order
            BreakIterator breaker = BreakIterator.getCharacterInstance(locale);
            CharArrayIterator charIterator = new CharArrayIterator(text, start, limit);
            breaker.setText(charIterator);
            int prevPos = start;
            int currPos;
            while ((currPos = breaker.following(prevPos)) != BreakIterator.DONE) {
                GlyphVector vector = glyphManager.layoutGlyphVector(font, text, prevPos, currPos, isRtl);
                // Don't forget to normalize it
                mAdvances.set(prevPos, (float) vector.getGlyphPosition(vector.getNumGlyphs()).getX() / level);

                prevPos = currPos;
            }
            // Compute line break boundaries, will be sorted into logical order.
            breaker = BreakIterator.getLineInstance(locale);
            charIterator.first();
            breaker.setText(charIterator);
            prevPos = start;
            while ((currPos = breaker.following(prevPos)) != BreakIterator.DONE) {
                mLineBoundaries.add(currPos);
                prevPos = currPos;
            }

            // Layout glyphs in visual order.
            // We need baked glyph, strip index, posX, posY and render flag.
            final GlyphVector vector = glyphManager.layoutGlyphVector(font, text, start, limit, isRtl);
            final int num = vector.getNumGlyphs();

            TextLayoutEngine.FastCharSet fastChars = null;
            final float offsetX = mAdvance;
            final boolean alignPixels = sAlignPixels;

            Point2D position = vector.getGlyphPosition(0);
            Point2D nextPosition = position;
            for (int i = 0; i < num; i++, position = nextPosition) {
                /*
                 * Back compatibility for Java 8, since LayoutGlyphVector should not have non-standard glyphs
                 * HarfBuzz is introduced in Java 11 or higher
                 */
                /*if (vector.getGlyphMetrics(i).getAdvanceX() == 0 &&
                        vector.getGlyphMetrics(i).getBounds2D().getWidth() == 0) {
                    continue;
                }*/

                int charIndex = vector.getGlyphCharIndex(i) + start;

                float posX = (float) position.getX() / level + offsetX;
                float posY = (float) position.getY() / level;
                // Align with a full pixel
                if (alignPixels) {
                    posX = Math.round(posX * scale) / scale;
                    posY = Math.round(posY * scale) / scale;
                }

                // ASCII digits are not on SMP
                if (fastDigit && text[charIndex] == '0') {
                    if (fastChars == null) {
                        fastChars = mEngine.lookupFastChars(font);
                    }
                    mGlyphs.add(fastChars);
                    mCharIndices.add(charIndex);
                    mPositions.add(posX);
                    mPositions.add(posY);
                    mFlags.add(styleFlags | CharacterStyle.FAST_DIGIT_REPLACEMENT);
                    mHasEffect |= hasEffect;
                    mHasFastDigit = true;
                } else {
                    int glyphCode = vector.getGlyphCode(i);
                    GLBakedGlyph glyph = glyphManager.lookupGlyph(font, glyphCode);
                    if (glyph != null) {
                        mGlyphs.add(glyph);
                        mCharIndices.add(charIndex);
                        mPositions.add(posX);
                        mPositions.add(posY);
                        mFlags.add(styleFlags);
                        mHasEffect |= hasEffect;
                    }
                }

                nextPosition = vector.getGlyphPosition(i + 1);
            }

            mAdvance += (float) nextPosition.getX() / level;

            /*if (isRtl) {
                finishFontRun(-nextOffset);
                mLayoutRight -= nextOffset;
            } else {
                finishFontRun(0);
            }*/
        }
    }

    /**
     * Adjust strip indices (w/o formatting codes) to string indices (w/ formatting codes).
     */
    private void adjustForFastDigit(@Nonnull String raw) {
        if (!mHasFastDigit) {
            return;
        }
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) == ChatFormatting.PREFIX_CODE) {
                for (int j = 0; j < mCharIndices.size(); j++) {
                    int value = mCharIndices.getInt(j);
                    if (value >= i) {
                        mCharIndices.set(j, value + 2);
                    }
                }
                i++;
            }
        }
        if (DEBUG) {
            for (int i = 0; i < mCharIndices.size(); i++) {
                if ((mFlags.getInt(i) & CharacterStyle.FAST_DIGIT_REPLACEMENT) != 0) {
                    char c = raw.charAt(mCharIndices.getInt(i));
                    if (c > '9' || c < '0') {
                        ModernUI.LOGGER.error("Fast Indexing Error: {} {} {} {}", i, c, mCharIndices, raw);
                    }
                }
            }
        }
    }

    /*
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
    //private void adjustAndInsertColor() {
        /*if (mAllList.isEmpty()) {
            throw new IllegalStateException();
        }
        // Sort by stripIndex, if mixed with LTR and RTL layout.
        // Logical order to visual order for line breaking, etc.
        mAllList.sort(Comparator.comparingInt(g -> g.mStringIndex));

        // Shift stripIndex to stringIndex
        int codeIndex = 0, shift = 0;
        for (BaseGlyphRender glyph : mAllList) {
            *//*
     * Adjust the string index for each glyph to point into the original string with un-stripped color codes.
     *  The while
     * loop is necessary to handle multiple consecutive color codes with no visible glyphs between them.
     * These new adjusted
     * stringIndex can now be compared against the color stringIndex during rendering. It also allows lookups
     *  of ASCII
     * digits in the original string for fast glyph replacement during rendering.
     *//*
            CharacterStyle carrier;
            while (codeIndex < mDStyles.size() &&
                    glyph.mStringIndex + shift >= (carrier = mDStyles.get(codeIndex)).mStringIndex) {
                if (carrier.isFormattingCode()) {
                    shift += 2;
                }
                codeIndex++;
            }
            glyph.mStringIndex += shift;
        }


        // insert color flags
        codeIndex = 0;
        CharacterStyle carrier = mDStyles.get(codeIndex);
        // In case of multiple consecutive color codes with the same stripIndex,
        // select the last one which will have active carrier
        while (codeIndex < mDStyles.size() - 1) {
            CharacterStyle c = mDStyles.get(codeIndex + 1);
            if (carrier.mStripIndex == c.mStripIndex) {
                carrier = c;
                codeIndex++;
            } else {
                break;
            }
        }

        int color = carrier.getFullColor();
        BaseGlyphRender glyph;
        // If there's no input style at the beginning
        if (color != CharacterStyle.NO_COLOR_SPECIFIED) {
            glyph = mAllList.get(0);
            glyph.mFlags &= ~BaseGlyphRender.COLOR_NO_CHANGE;
            glyph.mFlags |= color;
        }

        if (++codeIndex >= mDStyles.size()) {
            return;
        }
        for (int glyphIndex = 1; glyphIndex < mAllList.size() && codeIndex < mDStyles.size(); glyphIndex++) {
            carrier = mDStyles.get(codeIndex);

            // In case of multiple consecutive color codes with the same stripIndex,
            // select the last one which will have active carrier,
            // don't compare indices with formatting codes
            while (codeIndex < mDStyles.size() - 1) {
                CharacterStyle c = mDStyles.get(codeIndex + 1);
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
                if (carrier.getFullColor() != color) {
                    color = carrier.getFullColor();
                    glyph.mFlags &= ~BaseGlyphRender.COLOR_NO_CHANGE;
                    glyph.mFlags |= color;
                }

                codeIndex++;
            }
        }*/
    //}

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
