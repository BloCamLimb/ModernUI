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

import icyllis.modernui.font.pipeline.GlyphRender;
import icyllis.modernui.font.process.TextLayoutProcessor;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.CharacterManager;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextPropertiesManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Handle line breaks, get text width, etc.
 */
@OnlyIn(Dist.CLIENT)
public class ModernTextHandler extends CharacterManager {

    private final TextLayoutProcessor fontEngine = TextLayoutProcessor.getInstance();

    private final MutableFloat v = new MutableFloat();

    /**
     * Constructor
     *
     * @param widthRetriever retrieve char width with given codePoint and Style(BOLD)
     */
    //TODO remove width retriever as long as complex line wrapping finished
    ModernTextHandler(ICharWidthProvider widthRetriever) {
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
        return fontEngine.lookupVanillaNode(text, Style.EMPTY).advance;
    }

    /**
     * Get text width
     *
     * @param text text
     * @return total width
     */
    @Override
    public float func_238356_a_(@Nonnull ITextProperties text) {
        v.setValue(0);
        // iterate all siblings
        text.getComponentWithStyle((s, t) -> {
            if (!t.isEmpty()) {
                v.add(fontEngine.lookupVanillaNode(t, s).advance);
            }
            // continue
            return Optional.empty();
        }, Style.EMPTY);
        return v.floatValue();
    }

    /**
     * Get text width
     *
     * @param text text
     * @return total width
     */
    @Override
    public float func_243238_a(@Nonnull IReorderingProcessor text) {
        v.setValue(0);
        fontEngine.handleReorder(text, (t, s) -> {
            if (t.length() != 0) {
                v.add(fontEngine.lookupVanillaNode(t, s).advance);
            }
            return false;
        });
        return v.floatValue();
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
        return sizeToWidth0(text, width, style);
    }

    private int sizeToWidth0(@Nonnull CharSequence text, float width, @Nonnull Style style) {
        if (text.length() == 0) {
            return 0;
        }
        /* The glyph array for a string is sorted by the string's logical character position */
        GlyphRender[] glyphs = fontEngine.lookupVanillaNode(text, style).glyphs;

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
        return text.substring(0, sizeToWidth0(text, width, style));
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
        GlyphRender[] glyphs = fontEngine.lookupVanillaNode(text, style).glyphs;

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
     * Trim a text to find the last sibling text style to handle its click or hover event
     *
     * @param text  the text to trim
     * @param width the max width
     * @return the last sibling text style
     */
    @Nullable
    @Override
    public Style func_238357_a_(@Nonnull ITextProperties text, int width) {
        v.setValue(width);
        // iterate all siblings
        return text.getComponentWithStyle((s, t) -> {
            if (sizeToWidth0(t, v.floatValue(), s) < t.length()) {
                return Optional.of(s);
            }
            v.subtract(fontEngine.lookupVanillaNode(t, s).advance);
            // continue
            return Optional.empty();
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
    @Override
    public Style func_243239_a(@Nonnull IReorderingProcessor text, int width) {
        v.setValue(width);
        MutableObject<Style> sr = new MutableObject<>();
        // iterate all siblings
        if (!fontEngine.handleReorder(text, (t, s) -> {
            if (sizeToWidth0(t, v.floatValue(), s) < t.length()) {
                sr.setValue(s);
                // break with result
                return true;
            }
            v.subtract(fontEngine.lookupVanillaNode(t, s).advance);
            // continue
            return false;
        })) {
            return sr.getValue();
        }
        return null;
    }

    /**
     * Trim to width
     *
     * @param textIn  the text to trim
     * @param width   the max width
     * @param styleIn the default style of the text
     * @return the trimmed multi text
     */
    //TODO further optimization is possible
    @Nonnull
    @Override
    public ITextProperties func_238358_a_(@Nonnull ITextProperties textIn, int width, @Nonnull Style styleIn) {
        TextPropertiesManager collector = new TextPropertiesManager();
        v.setValue(width);
        // iterate all siblings
        return textIn.getComponentWithStyle((style, text) -> {
            int size;
            if ((size = sizeToWidth0(text, v.floatValue(), style)) < text.length()) {
                String sub = text.substring(0, size);
                if (!sub.isEmpty()) {
                    // add
                    collector.func_238155_a_(ITextProperties.func_240653_a_(sub, style));
                }
                // combine and break
                return Optional.of(collector.func_238156_b_());
            }
            if (!text.isEmpty()) {
                // add
                collector.func_238155_a_(ITextProperties.func_240653_a_(text, style));
            }
            v.subtract(fontEngine.lookupVanillaNode(text, style).advance);
            // continue
            return Optional.empty();
        }, styleIn).orElse(textIn); // full text
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
    //TODO handle complex line wrapping, including bidi analysis, style splitting
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
