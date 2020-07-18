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

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.font.process.TextCacheProcessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.fonts.Font;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.Style;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.commons.lang3.mutable.MutableFloat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

/**
 * Replace vanilla renderer with Modern UI renderer
 * INTERNAL USE ONLY, developers can't use this for any reason
 */
public class ModernFontRenderer extends FontRenderer {

    /**
     * Render thread instance
     */
    private static ModernFontRenderer INSTANCE;

    /**
     * Config value
     */
    public static boolean sAllowFontShadow;


    private final TextCacheProcessor processor = TextCacheProcessor.getInstance();

    private ModernFontRenderer(Function<ResourceLocation, Font> fontLibrary) {
        super(fontLibrary);
    }

    /**
     * INTERNAL USE ONLY, developers can't use this for any reason
     *
     * @return instance
     */
    public static ModernFontRenderer getInstance() {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        if (INSTANCE == null) {
            INSTANCE = new ModernFontRenderer(
                    ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class,
                            Minecraft.getInstance().fontRenderer, "field_211127_e"));
        }
        return INSTANCE;
    }

    void hook() {
        try {
            ObfuscationReflectionHelper.findField(Minecraft.class, "field_71466_p")
                    .set(Minecraft.getInstance(), this);
            ObfuscationReflectionHelper.findField(EntityRendererManager.class, "field_78736_p")
                    .set(Minecraft.getInstance().getRenderManager(), this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int func_238411_a_(@Nonnull String text, float x, float y, int color, boolean dropShadow, Matrix4f matrix,
                              @Nonnull IRenderTypeBuffer buffer, boolean transparent, int colorBackground, int packedLight, boolean bidiFlag) {
        // it seems that transparent (seeThroughType) is only available in Minecraft Debug Mode
        // bidiFlag is useless, we have our layout system
        x += drawStringInternal(text, x, y, color, dropShadow, matrix, buffer, packedLight, bidiFlag, Style.EMPTY);
        return (int) x + (dropShadow ? 1 : 0);
    }

    @Override
    public int func_238416_a_(@Nonnull ITextProperties multiText, float x, float y, int color, boolean dropShadow, Matrix4f matrix,
                              @Nonnull IRenderTypeBuffer buffer, boolean transparent, int colorBackground, int packedLight) {
        MutableFloat m = new MutableFloat(x);
        multiText.func_230439_a_((style, text) -> {
            m.add(drawStringInternal(text, m.getAndAdd(0), y, color, dropShadow, matrix, buffer, packedLight, getBidiFlag(), style));
            return Optional.empty();
        }, Style.EMPTY);
        return m.getValue().intValue() + (dropShadow ? 1 : 0);
    }

    private float drawStringInternal(@Nonnull String text, float x, float y, int color, boolean dropShadow, Matrix4f matrix,
                                   @Nonnull IRenderTypeBuffer buffer, int packedLight, boolean bidiFlag, Style style) {
        if (text.isEmpty()) {
            return 0.0f;
        }
        if (bidiFlag) {
            text = bidiReorder(text);
        }
        // ensure alpha, color can be ARGB, or can be RGB
        // if alpha <= 1, make alpha = 255
        if ((color & 0xfe000000) == 0) {
            color |= 0xff000000;
        }

        int a = color >> 24 & 0xff;
        int r = color >> 16 & 0xff;
        int g = color >> 8 & 0xff;
        int b = color & 0xff;

        /*if (dropShadow && sAllowFontShadow) {
            fontRenderer.drawFromVanilla(matrix, buffer, text, x + 1, y + 1, r, g, b, a, packedLight);
        }*/

        return processor.lookupVanillaNode(text, style).drawText(matrix, buffer, text, x, y, r, g, b, a, packedLight);
    }

    @Override
    public int getStringWidth(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        if (getBidiFlag()) {
            text = bidiReorder(text);
        }
        return MathHelper.ceil(processor.lookupVanillaNode(text, Style.EMPTY).advance);
    }

    @Override
    public int func_238414_a_(@Nonnull ITextProperties multiText) {
        MutableFloat m = new MutableFloat(0);
        multiText.func_230439_a_((style, text) -> {
            if (getBidiFlag()) {
                text = bidiReorder(text);
            }
            m.add(processor.lookupVanillaNode(text, style).advance);
            return Optional.empty();
        }, Style.EMPTY);
        return MathHelper.ceil(m.getAndIncrement());
    }

    /*@Override
    public float getCharWidth(char character) {
        return fontRenderer.getStringWidth(String.valueOf(character));
    }

    @Nonnull
    @Override
    public String trimStringToWidth(@Nonnull String text, int width, boolean reverse) {
        return fontRenderer.trimStringToWidth(text, width, reverse);
    }

    @Override
    public void drawSplitString(@Nullable String text, int x, int y, int wrapWidth, int textColor) {
        if (text == null || text.isEmpty()) {
            return;
        }
        while (text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }
        List<String> list = listFormattedStringToWidth(text, wrapWidth);
        Matrix4f matrix4f = TransformationMatrix.identity().getMatrix();
        for (String s : list) {
            drawString(s, x, y, textColor, matrix4f, false);
            y += 9;
        }
    }

    @Override
    public int sizeStringToWidth(@Nullable String str, int wrapWidth) {
        return fontRenderer.sizeStringToWidth(str, wrapWidth);
    }

    @Deprecated
    @Override
    public void setGlyphProviders(@Nonnull List<IGlyphProvider> g) {
        // no font
    }

    @Deprecated
    @Override
    public void close() {
        // no stream
    }*/

    // we keep bidi enabled, so no need to convert text
    /*@Nonnull
    @Override
    public String bidiReorder(@Nonnull String text) {
        return text;
    }*/
}
