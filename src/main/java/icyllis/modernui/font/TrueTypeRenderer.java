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

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.font.process.TextProcessRegister;
import icyllis.modernui.graphics.math.Color3i;
import icyllis.modernui.font.node.TextRenderNode;
import icyllis.modernui.font.process.TextCacheProcessor;
import icyllis.modernui.font.style.TextAlign;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class TrueTypeRenderer implements IFontRenderer {

    /**
     * Render thread instance
     */
    private static TrueTypeRenderer INSTANCE;

    /**
     * Config values
     *
     * @see icyllis.modernui.system.Config.Client
     */
    public static boolean sGlobalRenderer;

    /**
     * Vertical adjustment (in pixels * 2) to string position because Minecraft uses top of string instead of baseline
     */
    private static final int BASELINE_OFFSET = 7;

    /**
     * Cache and pre-process string for much better performance
     */
    private final TextCacheProcessor processor = new TextCacheProcessor();

    /**
     * Note: When Minecraft load completed, MainMenuScreen will be open and post GuiOpenEvent
     * UIManager will listen the event and create new Canvas instance, Canvas will call
     * {@link #getInstance()} and init to perform this constructor
     */
    private TrueTypeRenderer() {

        // init constructor and hook
        if (sGlobalRenderer) {
            try {
                ModernFontRenderer.INSTANCE = new ModernFontRenderer(this,
                        ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class,
                                Minecraft.getInstance().fontRenderer, "field_211127_e"));
                ObfuscationReflectionHelper.findField(Minecraft.class, "field_71466_p")
                        .set(Minecraft.getInstance(), ModernFontRenderer.INSTANCE);
                ObfuscationReflectionHelper.findField(EntityRendererManager.class, "field_78736_p")
                        .set(Minecraft.getInstance().getRenderManager(), ModernFontRenderer.INSTANCE);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static TrueTypeRenderer getInstance() {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        if (INSTANCE == null) {
            INSTANCE = new TrueTypeRenderer();
        }
        return INSTANCE;
    }

    public float drawFromCanvas(@Nullable String str, float x, float y, int r, int g, int b, int a, TextAlign align) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        if (LanguageMap.getInstance().func_230505_b_()) {
            try {
                Bidi bidi = new Bidi(new ArabicShaping(ArabicShaping.LETTERS_SHAPE).shape(str),
                        Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
                bidi.setReorderingMode(Bidi.REORDER_DEFAULT);
                str = bidi.writeReordered(Bidi.DO_MIRRORING);
            } catch (ArabicShapingException ignored) {

            }
        }
        TextRenderNode node = processor.lookupVanillaNode(str, Style.EMPTY);

        x -= node.advance * align.offsetFactor;
        return node.drawText(Tessellator.getInstance().getBuffer(), str, x, y, r, g, b, a);
    }

    public float drawFromVanilla(Matrix4f matrix, IRenderTypeBuffer buffer, @Nonnull String str, float x, float y, int r, int g, int b, int a, int packedLight) {
        if (str.isEmpty()) {
            return 0;
        }
        if (LanguageMap.getInstance().func_230505_b_()) {
            try {
                Bidi bidi = new Bidi(new ArabicShaping(ArabicShaping.LETTERS_SHAPE).shape(str),
                        Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
                bidi.setReorderingMode(Bidi.REORDER_DEFAULT);
                str = bidi.writeReordered(Bidi.DO_MIRRORING);
            } catch (ArabicShapingException ignored) {

            }
        }

        return processor.lookupVanillaNode(str, Style.EMPTY).drawText(matrix, buffer, str, x, y, r, g, b, a, packedLight);
    }

    @Override
    public float drawString(@Nullable String str, float startX, float startY, int r, int g, int b, int a, TextAlign align) {
        /* Check for invalid arguments */
        if (str == null || str.isEmpty()) {
            return 0;
        }

        // Fix for what RenderLivingBase#setBrightness does
        //GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);

        RenderSystem.enableTexture();

        /* Make sure the entire string is cached before rendering and return its glyph representation */
        //TextCacheProcessor.Entry entry = cache.getOrCacheString(str);

        /* Adjust the baseline of the string because the startY coordinate in Minecraft is for the top of the string */
        startY += BASELINE_OFFSET;

        /*
         * This color change will have no effect on the actual text (since colors are included in the Tessellator vertex
         * array), however GuiEditSign of all things depends on having the current color set to white when it renders its
         * "Edit sign message:" text. Otherwise, the sign which is rendered underneath would look too dark.
         *
         * It seems no use any more
         */
        //RenderSystem.color3f(r, g, b);

        /* formatting color will replace parameter color */
        Color3i formattedColor = null;

        /*
         * Enable GL_BLEND in case the font is drawn anti-aliased because Minecraft itself only enables blending for chat text
         * (so it can fade out), but not GUI text or signs. Minecraft uses multiple blend functions so it has to be specified here
         * as well for consistent blending. To reduce the overhead of OpenGL state changes and making native LWJGL calls, this
         * function doesn't try to save/restore the blending state. Hopefully everything else that depends on blending in Minecraft
         * will set its own state as needed.
         */
        /*if (cache.antiAliasEnabled) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }*/
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        //startX = startX - entry.advance * align.offsetFactor;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        /* The currently active font style is needed to select the proper ASCII digit style for fast replacement */
        int fontStyle = TextProcessRegister.PLAIN;

        //for (int glyphIndex = 0, colorIndex = 0; glyphIndex < entry.glyphs.length; glyphIndex++) {
            /*
             * If the original string had a color code at this glyph's position, then change the current GL color that gets added
             * to the vertex array. Note that only the RGB component of the color is replaced by a color code; the alpha component
             * of the original color passed into this function will remain. The while loop handles multiple consecutive color codes,
             * in which case only the last such color code takes effect.
             */
            /*while (colorIndex < entry.codes.length && entry.glyphs[glyphIndex].stringIndex >= entry.codes[colorIndex].stringIndex) {
                formattedColor = entry.codes[colorIndex].color;
                fontStyle = entry.codes[colorIndex].fontStyle;
                colorIndex++;
            }*/

            /* Select the current glyph's texture information and horizontal layout position within this string */
            /*Glyph glyph = entry.glyphs[glyphIndex];
            TexturedGlyph texture = glyph.texture;
            int glyphX = glyph.x;*/

            /*
             * Replace ASCII digits in the string with their respective glyphs; strings differing by digits are only cached once.
             * If the new replacement glyph has a different width than the original placeholder glyph (e.g. the '1' glyph is often
             * narrower than other digits), re-center the new glyph over the placeholder's position to minimize the visual impact
             * of the width mismatch.
             */
            /*char c = str.charAt(glyph.stringIndex);
            if (c >= '0' && c <= '9') {
                int oldWidth = texture.advance;
                texture = cache.digitGlyphs[fontStyle][c - '0'].texture;
                int newWidth = texture.advance;
                glyphX += (oldWidth - newWidth) >> 1;
            }*/

            /* The divide by 2.0F is needed to align with the scaled GUI coordinate system; startX/startY are already scaled */
            /*float x1 = startX + (glyphX) / 2.0F;
            float x2 = startX + (glyphX + texture.advance) / 2.0F;
            float y1 = startY + (glyph.y) / 2.0F;
            float y2 = startY + (glyph.y + texture.height) / 2.0F;

            bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
            GlStateManager.bindTexture(texture.textureName);

            if (formattedColor != null) {
                int red = formattedColor.getRed();
                int green = formattedColor.getGreen();
                int blue = formattedColor.getBlue();
                bufferBuilder.pos(x1, y1, 0).color(red, green, blue, a).tex(texture.u1, texture.v1).endVertex();
                bufferBuilder.pos(x1, y2, 0).color(red, green, blue, a).tex(texture.u1, texture.v2).endVertex();
                bufferBuilder.pos(x2, y2, 0).color(red, green, blue, a).tex(texture.u2, texture.v2).endVertex();
                bufferBuilder.pos(x2, y1, 0).color(red, green, blue, a).tex(texture.u2, texture.v1).endVertex();
            } else {
                bufferBuilder.pos(x1, y1, 0).color(r, g, b, a).tex(texture.u1, texture.v1).endVertex();
                bufferBuilder.pos(x1, y2, 0).color(r, g, b, a).tex(texture.u1, texture.v2).endVertex();
                bufferBuilder.pos(x2, y2, 0).color(r, g, b, a).tex(texture.u2, texture.v2).endVertex();
                bufferBuilder.pos(x2, y1, 0).color(r, g, b, a).tex(texture.u2, texture.v1).endVertex();
            }

            tessellator.draw();*/
        //}

        /* Draw strikethrough and underlines if the string uses them anywhere */
        //if (entry.needExtraRender) {
            //int renderStyle = 0;

            /* Use initial color passed to renderString(); disable texturing to draw solid color lines */
            //GlStateManager.disableTexture();
            //bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

            //for (int glyphIndex = 0, colorIndex = 0; glyphIndex < entry.glyphs.length; glyphIndex++) {
                /*
                 * If the original string had a color code at this glyph's position, then change the current GL color that gets added
                 * to the vertex array. The while loop handles multiple consecutive color codes, in which case only the last such
                 * color code takes effect.
                 */
                /*while (colorIndex < entry.codes.length && entry.glyphs[glyphIndex].stringIndex >= entry.codes[colorIndex].stringIndex) {
                    formattedColor = entry.codes[colorIndex].color;
                    renderStyle = entry.codes[colorIndex].renderEffect;
                    colorIndex++;
                }*/

                /* Select the current glyph within this string for its layout position */
                //Glyph glyph = entry.glyphs[glyphIndex];

                /* The strike/underlines are drawn beyond the glyph's width to include the extra space between glyphs */
                float glyphSpace = 0;//glyph.advance - glyph.texture.advance;

                /* Draw underline under glyph if the style is enabled */
                //if ((renderStyle & TextCacheProcessor.FormattingCode.UNDERLINE) != 0) {
                    /* The divide by 2.0F is needed to align with the scaled GUI coordinate system; startX/startY are already scaled */
                    /*if (formattedColor != null) {
                        int red = formattedColor.getRed();
                        int green = formattedColor.getGreen();
                        int blue = formattedColor.getBlue();
                        drawI1(startX, startY, bufferBuilder, glyph, glyphSpace, a, red, green, blue, UNDERLINE_OFFSET, UNDERLINE_THICKNESS);
                    } else {
                        drawI1(startX, startY, bufferBuilder, glyph, glyphSpace, a, r, g, b, UNDERLINE_OFFSET, UNDERLINE_THICKNESS);
                    }*/
                //}

                /* Draw strikethrough in the middle of glyph if the style is enabled */
                /*if ((renderStyle & TextCacheProcessor.FormattingCode.STRIKETHROUGH) != 0) {
                    *//* The divide by 2.0F is needed to align with the scaled GUI coordinate system; startX/startY are already scaled *//*
                    if (formattedColor != null) {
                        int red = formattedColor.getRed();
                        int green = formattedColor.getGreen();
                        int blue = formattedColor.getBlue();
                        drawI1(startX, startY, bufferBuilder, glyph, glyphSpace, a, red, green, blue, UNDERLINE_OFFSET, UNDERLINE_THICKNESS);
                    } else {
                        drawI1(startX, startY, bufferBuilder, glyph, glyphSpace, a, r, g, b, STRIKETHROUGH_OFFSET, STRIKETHROUGH_THICKNESS);
                    }
                }*/
            //}

            /* Finish drawing the last strikethrough/underline segments */
            //tessellator.draw();
            //GlStateManager.enableTexture();
        //}


        /* Return total horizontal advance (slightly wider than the bounding box, but close enough for centering strings) */
        return 0;//entry.advance / 2;
    }

    /*private void drawI1(float startX, float startY, @Nonnull BufferBuilder buffer, @Nonnull Glyph glyph, float glyphSpace, int a, int r, int g, int b, int underlineOffset, int underlineThickness) {
        float x1 = startX + (glyph.x - glyphSpace) / 2.0F;
        float x2 = startX + (glyph.x + glyph.advance) / 2.0F;
        float y1 = startY + (underlineOffset) / 2.0F;
        float y2 = startY + (underlineOffset + underlineThickness) / 2.0F;

        buffer.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, 0).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, 0).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, 0).color(r, g, b, a).endVertex();
    }*/

    /**
     * Render a single-line string to the screen using the current OpenGL color. The (x,y) coordinates are of the upper-left
     * corner of the string's bounding box, rather than the baseline position as is typical with fonts. This function will also
     * add the string to the cache so the next drawString() call with the same string is faster.
     *
     * @param str    the string being rendered; it can contain formatting codes
     * @param startX the x coordinate to draw at
     * @param startY the y coordinate to draw at
     * @return the total advance (horizontal distance) of this string
     */
    float drawStringGlobal(@Nullable String str, float startX, float startY, int red, int green, int blue, int alpha, boolean isShadow, Matrix4f matrix, IRenderTypeBuffer typeBuffer, int packedLight) {
        /* Check for invalid arguments */
        if (str == null || str.isEmpty()) {
            return 0;
        }

        // Fix for what RenderLivingBase#setBrightness does
        //GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);

        //RenderSystem.enableTexture();

        if (typeBuffer instanceof IRenderTypeBuffer.Impl) {
            ((IRenderTypeBuffer.Impl) typeBuffer).finish();
        }

        /* Make sure the entire string is cached before rendering and return its glyph representation */
        //TextCacheProcessor.Entry entry = processor.getOrCacheString(str);

        /* Adjust the baseline of the string because the startY coordinate in Minecraft is for the top of the string */
        startY += BASELINE_OFFSET;

        /*
         * This color change will have no effect on the actual text (since colors are included in the Tessellator vertex
         * array), however GuiEditSign of all things depends on having the current color set to white when it renders its
         * "Edit sign message:" text. Otherwise, the sign which is rendered underneath would look too dark.
         */
        //RenderSystem.color3f(r, g, b);
        if (isShadow) {
            red = red >> 2;
            green = green >> 2;
            blue = blue >> 2;
        }

        /* formatting color will replace parameter color */
        Color3i formattedColor = null;

        /*
         * Enable GL_BLEND in case the font is drawn anti-aliased because Minecraft itself only enables blending for chat text
         * (so it can fade out), but not GUI text or signs. Minecraft uses multiple blend functions so it has to be specified here
         * as well for consistent blending. To reduce the overhead of OpenGL state changes and making native LWJGL calls, this
         * function doesn't try to save/restore the blending state. Hopefully everything else that depends on blending in Minecraft
         * will set its own state as needed.
         */
        /*if (cache.antiAliasEnabled) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }*/
        /*RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();*/

        /* The currently active font style is needed to select the proper ASCII digit style for fast replacement */
        int fontStyle = TextProcessRegister.PLAIN;

        //for (int glyphIndex = 0, colorIndex = 0; glyphIndex < entry.glyphs.length; glyphIndex++) {
            /*
             * If the original string had a color code at this glyph's position, then change the current GL color that gets added
             * to the vertex array. Note that only the RGB component of the color is replaced by a color code; the alpha component
             * of the original color passed into this function will remain. The while loop handles multiple consecutive color codes,
             * in which case only the last such color code takes effect.
             */
            /*while (colorIndex < entry.codes.length && entry.glyphs[glyphIndex].stringIndex >= entry.codes[colorIndex].stringIndex) {
                formattedColor = entry.codes[colorIndex].color;
                fontStyle = entry.codes[colorIndex].fontStyle;
                colorIndex++;
            }*/

            /* Select the current glyph's texture information and horizontal layout position within this string */
            /*Glyph glyph = entry.glyphs[glyphIndex];
            TexturedGlyph texture = glyph.texture;
            int glyphX = glyph.x;*/

            /*
             * Replace ASCII digits in the string with their respective glyphs; strings differing by digits are only cached once.
             * If the new replacement glyph has a different width than the original placeholder glyph (e.g. the '1' glyph is often
             * narrower than other digits), re-center the new glyph over the placeholder's position to minimize the visual impact
             * of the width mismatch.
             */
            /*char c = str.charAt(glyph.stringIndex);
            if (c >= '0' && c <= '9') {
                int oldWidth = texture.advance;
                texture = processor.digitGlyphs[fontStyle][c - '0'].texture;
                int newWidth = texture.advance;
                glyphX += (oldWidth - newWidth) >> 1;
            }*/

            /* The divide by 2.0F is needed to align with the scaled GUI coordinate system; startX/startY are already scaled */
            /*float x1 = startX + (glyphX) / 2.0F;
            float x2 = startX + (glyphX + texture.advance) / 2.0F;
            float y1 = startY + (glyph.y) / 2.0F;
            float y2 = startY + (glyph.y + texture.height) / 2.0F;*/

            //GlStateManager.bindTexture(texture.textureName);

            //IVertexBuilder builder = typeBuffer.getBuffer(texture.renderType);

            /*if (formattedColor != null) {
                int r = formattedColor.getRed();
                int g = formattedColor.getGreen();
                int b = formattedColor.getBlue();
                if (isShadow) {
                    r = r >> 2;
                    g = g >> 2;
                    b = b >> 2;
                }
                builder.pos(matrix, x1, y1, 0).color(r, g, b, alpha).tex(texture.u1, texture.v1).lightmap(packedLight).endVertex();
                builder.pos(matrix, x1, y2, 0).color(r, g, b, alpha).tex(texture.u1, texture.v2).lightmap(packedLight).endVertex();
                builder.pos(matrix, x2, y2, 0).color(r, g, b, alpha).tex(texture.u2, texture.v2).lightmap(packedLight).endVertex();
                builder.pos(matrix, x2, y1, 0).color(r, g, b, alpha).tex(texture.u2, texture.v1).lightmap(packedLight).endVertex();
            } else {
                builder.pos(matrix, x1, y1, 0).color(red, green, blue, alpha).tex(texture.u1, texture.v1).lightmap(packedLight).endVertex();
                builder.pos(matrix, x1, y2, 0).color(red, green, blue, alpha).tex(texture.u1, texture.v2).lightmap(packedLight).endVertex();
                builder.pos(matrix, x2, y2, 0).color(red, green, blue, alpha).tex(texture.u2, texture.v2).lightmap(packedLight).endVertex();
                builder.pos(matrix, x2, y1, 0).color(red, green, blue, alpha).tex(texture.u2, texture.v1).lightmap(packedLight).endVertex();
            }*/

            //tessellator.draw();
        //}

        /* Draw strikethrough and underlines if the string uses them anywhere */
        //if (entry.needExtraRender) {

            // we use default buffer builder now, so we must finish the render type buffer manually
            if (typeBuffer instanceof IRenderTypeBuffer.Impl) {
                ((IRenderTypeBuffer.Impl) typeBuffer).finish();
            }


            int renderStyle = 0;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.getBuffer();

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            RenderSystem.enableCull();

            /* Use initial color passed to renderString(); disable texturing to draw solid color lines */
            GlStateManager.disableTexture();
            bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

            //for (int glyphIndex = 0, colorIndex = 0; glyphIndex < entry.glyphs.length; glyphIndex++) {
                /*
                 * If the original string had a color code at this glyph's position, then change the current GL color that gets added
                 * to the vertex array. The while loop handles multiple consecutive color codes, in which case only the last such
                 * color code takes effect.
                 */
                /*while (colorIndex < entry.codes.length && entry.glyphs[glyphIndex].stringIndex >= entry.codes[colorIndex].stringIndex) {
                    formattedColor = entry.codes[colorIndex].color;
                    renderStyle = entry.codes[colorIndex].renderEffect;
                    colorIndex++;
                }*/

                /* Select the current glyph within this string for its layout position */
                //Glyph glyph = entry.glyphs[glyphIndex];

                /* The strike/underlines are drawn beyond the glyph's width to include the extra space between glyphs */
                //float glyphSpace = glyph.advance - glyph.texture.advance;

                /* Draw underline under glyph if the style is enabled */
                //if ((renderStyle & TextCacheProcessor.FormattingCode.UNDERLINE) != 0) {
                    /* The divide by 2.0F is needed to align with the scaled GUI coordinate system; startX/startY are already scaled */
                    /*if (formattedColor != null) {
                        int r = formattedColor.getRed();
                        int g = formattedColor.getGreen();
                        int b = formattedColor.getBlue();
                        if (isShadow) {
                            r = r >> 2;
                            g = g >> 2;
                            b = b >> 2;
                        }
                        drawI1(matrix, startX, startY, bufferBuilder, glyph, glyphSpace, alpha, r, g, b, UNDERLINE_OFFSET, UNDERLINE_THICKNESS);
                    } else {
                        drawI1(matrix, startX, startY, bufferBuilder, glyph, glyphSpace, alpha, red, green, blue, UNDERLINE_OFFSET, UNDERLINE_THICKNESS);
                    }*/
                //}

                /* Draw strikethrough in the middle of glyph if the style is enabled */
                //if ((renderStyle & TextCacheProcessor.FormattingCode.STRIKETHROUGH) != 0) {
                    /* The divide by 2.0F is needed to align with the scaled GUI coordinate system; startX/startY are already scaled */
                    /*if (formattedColor != null) {
                        int r = formattedColor.getRed();
                        int g = formattedColor.getGreen();
                        int b = formattedColor.getBlue();
                        if (isShadow) {
                            r = r >> 2;
                            g = g >> 2;
                            b = b >> 2;
                        }
                        drawI1(matrix, startX, startY, bufferBuilder, glyph, glyphSpace, alpha, r, g, b, UNDERLINE_OFFSET, UNDERLINE_THICKNESS);
                    } else {
                        drawI1(matrix, startX, startY, bufferBuilder, glyph, glyphSpace, alpha, red, green, blue, STRIKETHROUGH_OFFSET, STRIKETHROUGH_THICKNESS);
                    }*/
                //}
            //}

            /* Finish drawing the last strikethrough/underline segments */
            /*tessellator.draw();
            GlStateManager.enableTexture();
            RenderSystem.disableCull();*/
        //}


        /* Return total horizontal advance (slightly wider than the bounding box, but close enough for centering strings) */
        return 0;
    }

    /*private void drawI1(Matrix4f matrix, float startX, float startY, BufferBuilder buffer, Glyph glyph, float glyphSpace, int a, int r, int g, int b, int underlineOffset, int underlineThickness) {
        float x1 = startX + (glyph.x - glyphSpace) / 2.0F;
        float x2 = startX + (glyph.x + glyph.advance) / 2.0F;
        float y1 = startY + (underlineOffset) / 2.0F;
        float y2 = startY + (underlineOffset + underlineThickness) / 2.0F;

        buffer.pos(matrix, x1, y1, 0).color(r, g, b, a).endVertex();
        buffer.pos(matrix, x1, y2, 0).color(r, g, b, a).endVertex();
        buffer.pos(matrix, x2, y2, 0).color(r, g, b, a).endVertex();
        buffer.pos(matrix, x2, y1, 0).color(r, g, b, a).endVertex();
    }*/

    /**
     * Return the width of a string in pixels. Used for centering strings inside GUI buttons.
     *
     * @param str compute the width of this string
     * @return the width in pixels (divided by 2; this matches the scaled coordinate system used by GUIs in Minecraft)
     */
    @SuppressWarnings("unused")
    @Override
    public float getStringWidth(@Nullable String str) {
        /* Check for invalid arguments */
        if (str == null || str.isEmpty()) {
            return 0;
        }

        /* Make sure the entire string is cached and rendered since it will probably be used again in a renderString() call */
        //TextCacheProcessor.Entry entry = processor.getOrCacheString(str);

        /* Return total horizontal advance (slightly wider than the bounding box, but close enough for centering strings) */
        return processor.lookupVanillaNode(str, Style.EMPTY).advance;
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
    //FIXME
    @SuppressWarnings("SameParameterValue")
    private int sizeString(@Nullable String str, float width, final boolean breakAtSpaces) {
        /* Check for invalid arguments */
        if (str == null || str.isEmpty()) {
            return 0;
        }

        /* Convert the width from GUI coordinate system to pixels */
        width += width;

        /* The glyph array for a string is sorted by the string's logical character position */
        //Glyph[] glyphs = processor.getOrCacheString(str).glyphs;

        /* Index of the last whitespace found in the string; used if breakAtSpaces is true */
        int wsIndex = -1;

        /* Add up the individual advance of each glyph until it exceeds the specified width */
        float advance = 0;
        int index = 0;
        /*while (index < glyphs.length && advance <= width) {
            // Keep track of spaces if breakAtSpaces it set
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
        }*/

        /* Avoid splitting individual words if breakAtSpaces set; same test condition as in Minecraft's FontRenderer */
        /*if (index < glyphs.length && wsIndex != -1 && wsIndex < index) {
            index = wsIndex;
        }*/

        /* The string index of the last glyph that wouldn't fit gives the total desired length of the string in characters */
        return 0;//index < glyphs.length ? glyphs[index].stringIndex : str.length();
    }

    /**
     * Return the number of characters in a string that will completly fit inside the specified width when rendered.
     *
     * @param str   the String to analyze
     * @param width the desired string width (in GUI coordinate system)
     * @return the number of characters from str that will fit inside width
     */
    @SuppressWarnings("unused")
    @Override
    public int sizeStringToWidth(String str, float width) {
        return sizeString(str, width, false);
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
    @Override
    public String trimStringToWidth(String str, float width, boolean reverse) {
        if (reverse)
            str = new StringBuilder(str).reverse().toString();

        int length = sizeString(str, width, false);
        str = str.substring(0, length);

        if (reverse) {
            str = (new StringBuilder(str)).reverse().toString();
        }

        return str;
    }
}
