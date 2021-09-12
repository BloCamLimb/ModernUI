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

import net.minecraft.client.ComponentCollector;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Handle line breaks, get text width, etc. For Vanilla Only.
 */
@OnlyIn(Dist.CLIENT)
public class ModernStringSplitter extends StringSplitter {

    private final TextLayoutEngine mFontEngine = TextLayoutEngine.getInstance();

    private final MutableFloat v = new MutableFloat();

    /**
     * Constructor
     *
     * @param vanillaWidths retrieve char width with given codePoint and Style(BOLD)
     */
    public ModernStringSplitter(WidthProvider vanillaWidths) {
        super(vanillaWidths);
    }

    /**
     * Get text width
     *
     * @param text text
     * @return text width
     */
    public static float measure(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return TextLayoutEngine.getInstance().lookupVanillaNode(text).mAdvance;
    }

    /**
     * Get text width
     *
     * @param text text
     * @return total width
     */
    public static float measure(@Nonnull FormattedText text) {
        /*v.setValue(0);
        // iterate all siblings
        text.visit((s, t) -> {
            if (!t.isEmpty()) {
                if (s.getFont() != Minecraft.ALT_FONT)
                    v.add(mFontEngine.lookupVanillaNode(t, s).mAdvance);
                else {
                    v.setValue(-1);
                    return FormattedText.STOP_ITERATION;
                }
            }
            // continue
            return Optional.empty();
        }, Style.EMPTY);
        return v.floatValue() >= 0 ? v.floatValue() : super.stringWidth(text);*/
        return TextLayoutEngine.getInstance().lookupMultilayerNode(text).mAdvance;
    }

    /**
     * Get text width
     *
     * @param text text
     * @return total width
     */
    public static float measure(@Nonnull FormattedCharSequence text) {
        /*v.setValue(0);
        mFontEngine.handleSequence(text, (t, s) -> {
            if (t.length() != 0) {
                v.add(mFontEngine.lookupVanillaNode(t, s).mAdvance);
            }
            return false;
        });
        return v.floatValue();*/
        return TextLayoutEngine.getInstance().lookupMultilayerNode(text).mAdvance;
    }

    /**
     * Get trimmed length / size to width.
     * <p>
     * Return the number of characters in a text that will completely fit inside
     * the specified width when rendered.
     *
     * @param text  the text to trim
     * @param width the max width
     * @param style the style of the text
     * @return the length of the text when it is trimmed to be at most /
     * the number of characters from text that will fit inside width
     */
    public static int getTrimSize(@Nonnull String text, int width, @Nonnull Style style) {
        if (text.isEmpty()) {
            return 0;
        }

        TextRenderNode node = TextLayoutEngine.getInstance().lookupVanillaNode(text, style);
        if (width >= node.mAdvance) {
            return text.length();
        }
        // the glyph array for a string is sorted by the visual order
        int r = getTrimSize(node.mGlyphs, width);
        return r == -1 ? text.length() : r;
    }

    private static int getTrimSize(@Nonnull BaseGlyphRender[] glyphs, int width) {
        // Add up the individual advance of each glyph until it exceeds the specified width
        float advance = 0;
        int glyphIndex = 0;
        while (glyphIndex < glyphs.length) {
            // always incr, see below
            advance += glyphs[glyphIndex++].getAdvance();
            if (advance > width) {
                break;
            }
        }

        // The string index of the last glyph that wouldn't fit gives the total desired length of the string in
        // characters
        // Note we should return the length of chars, so it's (index + 1)
        // But we shouldn't break a glyph, so while loop find the next glyph
        // So the index is the size to next glyph - 1
        return glyphIndex < glyphs.length ? glyphs[glyphIndex].mStringIndex : -1;
    }

    /**
     * Trim a text so that it fits in the specified width when rendered.
     *
     * @param text  the text to trim
     * @param width the max width
     * @param style the style of the text
     * @return the trimmed text
     */
    @Nonnull
    public static String trimText(@Nonnull String text, int width, @Nonnull Style style) {
        if (text.isEmpty()) {
            return text;
        }
        return text.substring(0, getTrimSize(text, width, style));
    }

    /**
     * Trim a text backwards so that it fits in the specified width when rendered.
     *
     * @param text  the text to trim
     * @param width the max width
     * @param style the style of the text
     * @return the trimmed text
     */
    @Nonnull
    public static String trimReverse(@Nonnull String text, int width, @Nonnull Style style) {
        if (text.isEmpty()) {
            return text;
        }
        TextRenderNode node = TextLayoutEngine.getInstance().lookupVanillaNode(text, style);
        if (width >= node.mAdvance) {
            return text;
        }
        // The glyph array for a string is sorted by the string's logical character position
        BaseGlyphRender[] glyphs = node.mGlyphs;

        // Add up the individual advance of each glyph until it exceeds the specified width
        float advance = 0;
        int glyphIndex = glyphs.length - 1;
        while (glyphIndex >= 0) {
            advance += glyphs[glyphIndex].getAdvance();
            if (advance <= width) {
                glyphIndex--;
            } else {
                break;
            }
        }

        // The string index of the last glyph that wouldn't fit gives the total desired length of the string in
        // characters
        int l = glyphIndex >= 0 ? glyphs[glyphIndex].mStringIndex : 0;
        return text.substring(l);
    }

    /**
     * Trim a text to find the last sibling text style to handle its click or hover event
     *
     * @param text  the text to trim
     * @param width the max width
     * @return the last sibling text style
     */
    @Nullable
    public static Style styleAtWidth(@Nonnull FormattedText text, int width) {
        /*v.setValue(width);
        // iterate all siblings
        return text.visit((s, t) -> {
            if (sizeToWidth0(t, v.floatValue(), s) < t.length()) {
                return Optional.of(s);
            }
            v.subtract(mFontEngine.lookupVanillaNode(t, s).mAdvance);
            // continue
            return Optional.empty();
        }, Style.EMPTY).orElse(null);*/
        if (width < 0) {
            return null;
        }
        TextRenderNode node = TextLayoutEngine.getInstance().lookupMultilayerNode(text);
        if (width >= node.mAdvance) {
            return null;
        }
        final int r = getTrimSize(node.mGlyphs, width);
        if (r == -1) {
            return null;
        }
        return text.visit(new FormattedText.StyledContentConsumer<Style>() {
            private int mNext;

            @Override
            public Optional<Style> accept(Style s, String t) {
                if (r >= mNext && r < mNext + t.length()) {
                    return Optional.of(s);
                } else {
                    mNext += t.length();
                    return Optional.empty();
                }
            }
        }, Style.EMPTY).orElse(null);
    }

    /**
     * Trim a text to find the last sibling text style to handle its click or hover event
     *
     * @param text  the text to trim
     * @param width the max width
     * @return the last sibling text style
     */
    @Nullable
    public static Style styleAtWidth(@Nonnull FormattedCharSequence text, int width) {
        /*v.setValue(width);
        MutableObject<Style> sr = new MutableObject<>();
        // iterate all siblings
        if (!mFontEngine.handleSequence(text, (t, s) -> {
            if (sizeToWidth0(t, v.floatValue(), s) < t.length()) {
                sr.setValue(s);
                // break with result
                return true;
            }
            v.subtract(mFontEngine.lookupVanillaNode(t, s).mAdvance);
            // continue
            return false;
        })) {
            return sr.getValue();
        }
        return null;*/
        if (width < 0) {
            return null;
        }
        TextRenderNode node = TextLayoutEngine.getInstance().lookupMultilayerNode(text);
        if (width >= node.mAdvance) {
            return null;
        }
        final int r = getTrimSize(node.mGlyphs, width);
        if (r == -1) {
            return null;
        }
        final MutableObject<Style> value = new MutableObject<>();
        text.accept((i, s, c) -> {
            if (i >= r) {
                value.setValue(s);
                return false;
            } else {
                return true;
            }
        });
        return value.getValue();
    }

    /**
     * Trim to width
     *
     * @param text  the text to trim
     * @param width the max width
     * @param style the default style of the text
     * @return the trimmed multi text
     */
    @Nonnull
    public static FormattedText trimText(@Nonnull FormattedText text, int width, @Nonnull Style style) {
        if (width < 0) {
            return FormattedText.EMPTY;
        }
        TextRenderNode node = TextLayoutEngine.getInstance().lookupMultilayerNode(text, style);
        if (width >= node.mAdvance) {
            return text;
        }
        final int r = getTrimSize(node.mGlyphs, width);
        if (r == -1) {
            return text;
        }
        return text.visit(new FormattedText.StyledContentConsumer<FormattedText>() {
            private final ComponentCollector mCollector = new ComponentCollector();
            private int mNext;

            @Override
            public Optional<FormattedText> accept(Style s, String t) {
                if (mNext + t.length() >= r) {
                    String sub = t.substring(0, r - mNext);
                    if (!sub.isEmpty()) {
                        // add
                        mCollector.append(FormattedText.of(sub, s));
                    }
                    // combine and break
                    return Optional.of(mCollector.getResultOrEmpty());
                }
                if (!t.isEmpty()) {
                    // add
                    mCollector.append(FormattedText.of(t, s));
                }
                mNext += t.length();
                // continue
                return Optional.empty();
            }
        }, style).orElse(text); // full text
    }

    /**
     * Wrap lines
     *
     * @param text      text to handle
     * @param wrapWidth max width of each line
     * @param style     style for the text
     * @param retainEnd retain the last word on each line
     * @param consumer  accept each line result, params{current style, start index (inclusive), end index (exclusive)}
     */
    //TODO handle complex line wrapping, including bidi analysis, style splitting
    @Override
    public void splitLines(String text, int wrapWidth, @Nonnull Style style, boolean retainEnd,
                           @Nonnull StringSplitter.LinePosConsumer consumer) {
        super.splitLines(text, wrapWidth, style, retainEnd, consumer);
    }

    /**
     * Wrap lines
     *
     * @param text      text to handle
     * @param wrapWidth max width of each line
     * @param style     style for the text
     * @return a list of text for each line
     */
    @Nonnull
    @Override
    public List<FormattedText> splitLines(String text, int wrapWidth, @Nonnull Style style) {
        return super.splitLines(text, wrapWidth, style);
    }
}
