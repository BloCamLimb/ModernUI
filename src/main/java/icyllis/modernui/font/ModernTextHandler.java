/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.font;

import icyllis.modernui.font.node.GlyphRenderInfo;
import icyllis.modernui.font.process.TextCacheProcessor;
import net.minecraft.util.text.CharacterManager;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextPropertiesManager;
import org.apache.commons.lang3.mutable.MutableFloat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Handle line breaks, get text width, etc.
 */
public class ModernTextHandler extends CharacterManager {

    private final TextCacheProcessor processor = TextCacheProcessor.getInstance();

    /**
     * Constructor
     *
     * @param widthRetriever retrieve char width with given codePoint
     */
    public ModernTextHandler(ICharWidthProvider widthRetriever) {
        super(widthRetriever);
    }

    /**
     * Get text width
     *
     * @param text text
     * @return text width
     */
    @Override
    public float func_238350_a_(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return processor.lookupVanillaNode(text, Style.EMPTY).advance;
    }

    /**
     * Get text width
     *
     * @param text text
     * @return total width
     */
    @Override
    public float func_238356_a_(@Nonnull ITextProperties text) {
        MutableFloat m = new MutableFloat(0);
        // iterate all siblings
        text.func_230439_a_((s, t) -> {
            if (!t.isEmpty()) {
                m.add(processor.lookupVanillaNode(t, s).advance);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return m.floatValue();
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
    @Override
    public int func_238352_a_(@Nonnull String text, int width, @Nonnull Style style) {
        return sizeToWidth(text, width, style);
    }

    private int sizeToWidth(@Nonnull String text, float width, @Nonnull Style style) {
        if (text.isEmpty()) {
            return 0;
        }
        /* The glyph array for a string is sorted by the string's logical character position */
        GlyphRenderInfo[] glyphs = processor.lookupVanillaNode(text, style).glyphs;

        /* Add up the individual advance of each glyph until it exceeds the specified width */
        float advance = 0;
        int glyphIndex = 0;
        while (glyphIndex < glyphs.length) {

            advance += glyphs[glyphIndex].getAdvance();
            if (advance <= width) {
                glyphIndex++;
            } else {
                break;
            }
        }

        /* The string index of the last glyph that wouldn't fit gives the total desired length of the string in characters */
        return glyphIndex < glyphs.length ? glyphs[glyphIndex].stringIndex : text.length();
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
    @Override
    public String func_238361_b_(@Nonnull String text, int width, @Nonnull Style style) {
        return text.substring(0, sizeToWidth(text, width, style));
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
    @Override
    public String func_238364_c_(@Nonnull String text, int width, @Nonnull Style style) {
        if (text.isEmpty()) {
            return text;
        }
        /* The glyph array for a string is sorted by the string's logical character position */
        GlyphRenderInfo[] glyphs = processor.lookupVanillaNode(text, style).glyphs;

        /* Add up the individual advance of each glyph until it exceeds the specified width */
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

        /* The string index of the last glyph that wouldn't fit gives the total desired length of the string in characters */
        int l = glyphIndex >= 0 ? glyphs[glyphIndex].stringIndex : 0;
        return text.substring(l);
    }

    /**
     * Trim a text to obtain last sibling text style to handle click or hover event
     *
     * @param text  the text to trim
     * @param width the max width
     * @return the last sibling text style
     */
    @Nullable
    @Override
    public Style func_238357_a_(@Nonnull ITextProperties text, int width) {
        MutableFloat mw = new MutableFloat(width);
        // iterate all siblings
        return text.func_230439_a_((s, t) -> {
            if (sizeToWidth(t, mw.floatValue(), s) < t.length()) {
                return Optional.of(s);
            }
            mw.subtract(processor.lookupVanillaNode(t, s).advance);
            return Optional.empty();
        }, Style.EMPTY).orElse(null);
    }

    /**
     * Trim to width
     *
     * @param text  the text to trim
     * @param width the max width
     * @param style the style of the text
     * @return the trimmed multi text
     */
    @Nonnull
    @Override
    public ITextProperties func_238358_a_(@Nonnull ITextProperties text, int width, @Nonnull Style style) {
        TextPropertiesManager collector = new TextPropertiesManager();
        MutableFloat mw = new MutableFloat(width);
        // iterate all siblings
        return text.func_230439_a_((sty, string) -> {
            int size;
            if ((size = sizeToWidth(string, mw.floatValue(), sty)) < string.length()) {
                String s2 = string.substring(0, size);
                if (!s2.isEmpty()) {
                    // add
                    collector.func_238155_a_(ITextProperties.func_240653_a_(s2, sty));
                }
                // combine and break
                return Optional.of(collector.func_238156_b_());
            }
            if (!string.isEmpty()) {
                // add
                collector.func_238155_a_(ITextProperties.func_240653_a_(string, sty));
            }
            mw.subtract(processor.lookupVanillaNode(string, sty).advance);
            // continue
            return Optional.empty();
        }, style).orElse(text); // full text
    }

    /**
     * Wrap lines
     *
     * @param text      text to handle
     * @param wrapWidth max width of each line
     * @param style     style for the text
     * @param retainEnd retain the last word on each line
     * @param acceptor  accept each line result, params{current style, start index (inclusive), end index (exclusive)}
     */
    //TODO
    @Override
    public void func_238353_a_(String text, int wrapWidth, @Nonnull Style style, boolean retainEnd, @Nonnull ISliceAcceptor acceptor) {
        super.func_238353_a_(text, wrapWidth, style, retainEnd, acceptor);
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
    public List<ITextProperties> func_238365_g_(String text, int wrapWidth, @Nonnull Style style) {
        return super.func_238365_g_(text, wrapWidth, style);
    }
}
