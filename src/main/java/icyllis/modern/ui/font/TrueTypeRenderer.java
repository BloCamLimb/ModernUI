/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package icyllis.modern.ui.font;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

public class TrueTypeRenderer {

    public static final TrueTypeRenderer DEFAULT_FONT_RENDERER;
    private static final FontRenderer VANILLA_FONT_RENDERER;
    static {
        StringCache cache1 = new StringCache();
        cache1.setDefaultFont(12.5f, true);
        DEFAULT_FONT_RENDERER = new TrueTypeRenderer(cache1);
        VANILLA_FONT_RENDERER = Minecraft.getInstance().fontRenderer;
    }

    /**
     * Vertical adjustment (in pixels * 2) to string position because Minecraft uses top of string instead of baseline
     */
    private static final int BASELINE_OFFSET = 7;

    /**
     * Offset from the string's baseline as which to draw the underline (in pixels)
     */
    private static final int UNDERLINE_OFFSET = 1;

    /**
     * Thickness of the underline (in pixels)
     */
    private static final int UNDERLINE_THICKNESS = 2;

    /**
     * Offset from the string's baseline as which to draw the strikethrough line (in pixels)
     */
    private static final int STRIKETHROUGH_OFFSET = -6;

    /**
     * Thickness of the strikethrough line (in pixels)
     */
    private static final int STRIKETHROUGH_THICKNESS = 2;

    private final StringCache cache;

    private TrueTypeRenderer(StringCache cache) {
        this.cache = cache;
    }

    public void init() {

    }

    /**
     * Render a single-line string to the screen using the current OpenGL color. The (x,y) coordinates are of the uppet-left
     * corner of the string's bounding box, rather than the baseline position as is typical with fonts. This function will also
     * add the string to the cache so the next renderString() call with the same string is faster.
     *
     * @param str          the string being rendered; it can contain color codes
     * @param startX       the x coordinate to draw at
     * @param startY       the y coordinate to draw at
     * @return the total advance (horizontal distance) of this string
     */
    //todo Add optional NumericShaper to replace ASCII digits with locale specific ones
    //todo Add support for the "k" code which randomly replaces letters on each render (used on
    //todo Pre-sort by texture to minimize binds; can store colors per glyph in string cache
    //todo Optimize the underline/strikethrough drawing to draw a single line for each run
    public float renderString(String str, float startX, float startY, int color) {
        /* Check for invalid arguments */
        if (str == null || str.isEmpty()) {
            return 0;
        }

        // Fix for what RenderLivingBase#setBrightness does
        GlStateManager.texEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);

        /* Make sure the entire string is cached before rendering and return its glyph representation */
        StringCache.Entry entry = cache.cacheString(str);

        /* Adjust the baseline of the string because the startY coordinate in Minecraft is for the top of the string */
        startY += BASELINE_OFFSET;

        /*
         * This color change will have no effect on the actual text (since colors are included in the Tessellator vertex
         * array), however GuiEditSign of all things depends on having the current color set to white when it renders its
         * "Edit sign message:" text. Otherwise, the sign which is rendered underneath would look too dark.
         */
        GlStateManager.color3f((color >> 16 & 0xff) / 255F, (color >> 8 & 0xff) / 255F, (color & 0xff) / 255F);

        /*
         * Enable GL_BLEND in case the font is drawn anti-aliased because Minecraft itself only enables blending for chat text
         * (so it can fade out), but not GUI text or signs. Minecraft uses multiple blend functions so it has to be specified here
         * as well for consistent blending. To reduce the overhead of OpenGL state changes and making native LWJGL calls, this
         * function doesn't try to save/restore the blending state. Hopefully everything else that depends on blending in Minecraft
         * will set its own state as needed.
         */

        if (cache.antiAliasEnabled) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        /* The currently active font syle is needed to select the proper ASCII digit style for fast replacement */
        int fontStyle = Font.PLAIN;

        for (int glyphIndex = 0, colorIndex = 0; glyphIndex < entry.glyphs.length; glyphIndex++) {
            /*
             * If the original string had a color code at this glyph's position, then change the current GL color that gets added
             * to the vertex array. Note that only the RGB component of the color is replaced by a color code; the alpha component
             * of the original color passed into this function will remain. The while loop handles multiple consecutive color codes,
             * in which case only the last such color code takes effect.
             */
            while (colorIndex < entry.colors.length && entry.glyphs[glyphIndex].stringIndex >= entry.colors[colorIndex].stringIndex) {
                fontStyle = entry.colors[colorIndex].fontStyle;
                colorIndex++;
            }

            /* Select the current glyph's texture information and horizontal layout position within this string */
            GlyphCache.Glyph glyph = entry.glyphs[glyphIndex];
            GlyphCache.Entry texture = glyph.texture;
            int glyphX = glyph.x;

            /*
             * Replace ASCII digits in the string with their respective glyphs; strings differing by digits are only cached once.
             * If the new replacement glyph has a different width than the original placeholder glyph (e.g. the '1' glyph is often
             * narrower than other digits), re-center the new glyph over the placeholder's position to minimize the visual impact
             * of the width mismatch.
             */
            char c = str.charAt(glyph.stringIndex);
            if (c >= '0' && c <= '9') {
                int oldWidth = texture.width;
                texture = cache.digitGlyphs[fontStyle][c - '0'].texture;
                int newWidth = texture.width;
                glyphX += (oldWidth - newWidth) >> 1;
            }

            /* The divide by 2.0F is needed to align with the scaled GUI coordinate system; startX/startY are already scaled */
            float x1 = startX + (glyphX) / 2.0F;
            float x2 = startX + (glyphX + texture.width) / 2.0F;
            float y1 = startY + (glyph.y) / 2.0F;
            float y2 = startY + (glyph.y + texture.height) / 2.0F;

            int a = color >> 24 & 0xff;
            int r = color >> 16 & 0xff;
            int g = color >> 8 & 0xff;
            int b = color & 0xff;

            buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            GlStateManager.bindTexture(texture.textureName);

            buffer.pos(x1, y1, 0).tex(texture.u1, texture.v1).color(r, g, b, a).endVertex();
            buffer.pos(x1, y2, 0).tex(texture.u1, texture.v2).color(r, g, b, a).endVertex();
            buffer.pos(x2, y2, 0).tex(texture.u2, texture.v2).color(r, g, b, a).endVertex();
            buffer.pos(x2, y1, 0).tex(texture.u2, texture.v1).color(r, g, b, a).endVertex();

            tessellator.draw();
        }

        /* Draw strikethrough and underlines if the string uses them anywhere */
        if (entry.specialRender) {
            int renderStyle = 0;

            /* Use initial color passed to renderString(); disable texturing to draw solid color lines */
            GlStateManager.disableTexture();
            buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);

            for (int glyphIndex = 0, colorIndex = 0; glyphIndex < entry.glyphs.length; glyphIndex++) {
                /*
                 * If the original string had a color code at this glyph's position, then change the current GL color that gets added
                 * to the vertex array. The while loop handles multiple consecutive color codes, in which case only the last such
                 * color code takes effect.
                 */
                while (colorIndex < entry.colors.length && entry.glyphs[glyphIndex].stringIndex >= entry.colors[colorIndex].stringIndex) {
                    renderStyle = entry.colors[colorIndex].renderStyle;
                    colorIndex++;
                }

                /* Select the current glyph within this string for its layout position */
                GlyphCache.Glyph glyph = entry.glyphs[glyphIndex];

                /* The strike/underlines are drawn beyond the glyph's width to include the extra space between glyphs */
                float glyphSpace = glyph.advance - glyph.texture.width;

                int a = color >> 24 & 0xff;
                int r = color >> 16 & 0xff;
                int g = color >> 8 & 0xff;
                int b = color & 0xff;

                /* Draw underline under glyph if the style is enabled */
                if ((renderStyle & StringCache.ColorCode.UNDERLINE) != 0) {
                    /* The divide by 2.0F is needed to align with the scaled GUI coordinate system; startX/startY are already scaled */
                    float x1 = startX + (glyph.x - glyphSpace) / 2.0F;
                    float x2 = startX + (glyph.x + glyph.advance) / 2.0F;
                    float y1 = startY + (UNDERLINE_OFFSET) / 2.0F;
                    float y2 = startY + (UNDERLINE_OFFSET + UNDERLINE_THICKNESS) / 2.0F;

                    buffer.pos(x1, y1, 0).color(r, g, b, a).endVertex();
                    buffer.pos(x1, y2, 0).color(r, g, b, a).endVertex();
                    buffer.pos(x2, y2, 0).color(r, g, b, a).endVertex();
                    buffer.pos(x2, y1, 0).color(r, g, b, a).endVertex();
                }

                /* Draw strikethrough in the middle of glyph if the style is enabled */
                if ((renderStyle & StringCache.ColorCode.STRIKETHROUGH) != 0) {
                    /* The divide by 2.0F is needed to align with the scaled GUI coordinate system; startX/startY are already scaled */
                    float x1 = startX + (glyph.x - glyphSpace) / 2.0F;
                    float x2 = startX + (glyph.x + glyph.advance) / 2.0F;
                    float y1 = startY + (STRIKETHROUGH_OFFSET) / 2.0F;
                    float y2 = startY + (STRIKETHROUGH_OFFSET + STRIKETHROUGH_THICKNESS) / 2.0F;

                    buffer.pos(x1, y1, 0).color(r, g, b, a).endVertex();
                    buffer.pos(x1, y2, 0).color(r, g, b, a).endVertex();
                    buffer.pos(x2, y2, 0).color(r, g, b, a).endVertex();
                    buffer.pos(x2, y1, 0).color(r, g, b, a).endVertex();
                }
            }

            /* Finish drawing the last strikethrough/underline segments */
            tessellator.draw();
            GlStateManager.enableTexture();
        }


        /* Return total horizontal advance (slightly wider than the bounding box, but close enough for centering strings) */
        return entry.advance / 2;
    }

    /**
     * Return the width of a string in pixels. Used for centering strings inside GUI buttons.
     *
     * @param str compute the width of this string
     * @return the width in pixels (divided by 2; this matches the scaled coordinate system used by GUIs in Minecraft)
     */
    @SuppressWarnings("unused")
    public float getStringWidth(String str) {
        /* Check for invalid arguments */
        if (str == null || str.isEmpty()) {
            return 0;
        }

        /* Make sure the entire string is cached and rendered since it will probably be used again in a renderString() call */
        StringCache.Entry entry = cache.cacheString(str);

        /* Return total horizontal advance (slightly wider than the bounding box, but close enough for centering strings) */
        return entry.advance / 2;
    }

    /**
     * Return the number of characters in a string that will completly fit inside the specified width when rendered, with
     * or without prefering to break the line at whitespace instead of breaking in the middle of a word. This private provides
     * the real implementation of both sizeStringToWidth() and trimStringToWidth().
     *
     * @param str           the String to analyze
     * @param width         the desired string width (in GUI coordinate system)
     * @param breakAtSpaces set to prefer breaking line at spaces than in the middle of a word
     * @return the number of characters from str that will fit inside width
     */
    private int sizeString(String str, float width, boolean breakAtSpaces) {
        /* Check for invalid arguments */
        if (str == null || str.isEmpty()) {
            return 0;
        }

        /* Convert the width from GUI coordinate system to pixels */
        width += width;

        /* The glyph array for a string is sorted by the string's logical character position */
        GlyphCache.Glyph[] glyphs = cache.cacheString(str).glyphs;

        /* Index of the last whitespace found in the string; used if breakAtSpaces is true */
        int wsIndex = -1;

        /* Add up the individual advance of each glyph until it exceeds the specified width */
        float advance = 0;
        int index = 0;
        while (index < glyphs.length && advance <= width) {
            /* Keep track of spaces if breakAtSpaces it set */
            if (breakAtSpaces) {
                char c = str.charAt(glyphs[index].stringIndex);
                if (c == ' ') {
                    wsIndex = index;
                } else if (c == '\n') {
                    wsIndex = index;
                    break;
                }
            }

            float nextAdvance = advance + glyphs[index].advance;
            if (nextAdvance <= width) {
                advance = nextAdvance;
                index++;
            } else {
                break;
            }
        }

        /* Avoid splitting individual words if breakAtSpaces set; same test condition as in Minecraft's FontRenderer */
        if (index < glyphs.length && wsIndex != -1 && wsIndex < index) {
            index = wsIndex;
        }

        /* The string index of the last glyph that wouldn't fit gives the total desired length of the string in characters */
        return index < glyphs.length ? glyphs[index].stringIndex : str.length();
    }

    /**
     * Return the number of characters in a string that will completly fit inside the specified width when rendered.
     *
     * @param str   the String to analyze
     * @param width the desired string width (in GUI coordinate system)
     * @return the number of characters from str that will fit inside width
     */
    @SuppressWarnings("unused")
    public int sizeStringToWidth(String str, int width) {
        return sizeString(str, width, true);
    }

    /**
     * Trim a string so that it fits in the specified width when rendered, optionally reversing the string
     *
     * @param str     the String to trim
     * @param width   the desired string width (in GUI coordinate system)
     * @param reverse if true, the returned string will also be reversed
     * @return the trimmed and optionally reversed string
     */
    @SuppressWarnings("unused")
    public String trimStringToWidth(String str, float width, boolean reverse) {
        if (reverse)
            str = new StringBuilder(str).reverse().toString();

        int length = sizeString(str, width, true);
        str = str.substring(0, length);

        if (reverse) {
            str = (new StringBuilder(str)).reverse().toString();
        }

        return str;
    }
}
