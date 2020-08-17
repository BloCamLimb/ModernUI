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
import icyllis.modernui.font.node.TextRenderNode;
import icyllis.modernui.font.process.TextCacheProcessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.fonts.Font;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.CharacterManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.commons.lang3.mutable.MutableFloat;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Function;

/**
 * Replace vanilla renderer with Modern UI renderer
 */
@OnlyIn(Dist.CLIENT)
public class ModernFontRenderer extends FontRenderer {

    /**
     * Render thread instance
     */
    private static ModernFontRenderer instance;

    public static final Vector3f SHADOW_LIFTING = new Vector3f(0.0f, 0.0f, 0.03f);

    /**
     * Config value
     */
    public static boolean sAllowFontShadow;


    private final TextCacheProcessor processor = TextCacheProcessor.getInstance();

    private final MutableFloat mutableFloat = new MutableFloat();

    private FontRenderer originalRenderer;

    private ModernFontRenderer() {
        super($ -> null);
    }

    /**
     * INTERNAL USE ONLY, developers can't use this for any reason
     *
     * @return instance
     * @see TrueTypeRenderer#getInstance()
     */
    public static ModernFontRenderer getInstance() {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        if (instance == null) {
            instance = new ModernFontRenderer();
            CharacterManager o = ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class,
                    Minecraft.getInstance().fontRenderer, "field_238402_e_");
            Function<ResourceLocation, Font> r = ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class,
                    Minecraft.getInstance().fontRenderer, "field_211127_e");
            CharacterManager.ICharWidthProvider c = ObfuscationReflectionHelper.getPrivateValue(CharacterManager.class,
                    o, "field_238347_a_");
            ModernTextHandler t = new ModernTextHandler(c);
            ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class,
                    instance, t, "field_238402_e_");
            ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class,
                    instance, r, "field_211127_e");
        }
        return instance;
    }

    void hook(boolean doHook) {
        boolean working = Minecraft.getInstance().fontRenderer instanceof ModernFontRenderer;
        if (working == doHook) {
            return;
        }
        if (doHook) {
            originalRenderer = Minecraft.getInstance().fontRenderer;
            try {
                ObfuscationReflectionHelper.findField(Minecraft.class, "field_71466_p")
                        .set(Minecraft.getInstance(), this);
                ObfuscationReflectionHelper.findField(EntityRendererManager.class, "field_78736_p")
                        .set(Minecraft.getInstance().getRenderManager(), this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
            try {
                ObfuscationReflectionHelper.findField(Minecraft.class, "field_71466_p")
                        .set(Minecraft.getInstance(), originalRenderer);
                ObfuscationReflectionHelper.findField(EntityRendererManager.class, "field_78736_p")
                        .set(Minecraft.getInstance().getRenderManager(), originalRenderer);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int func_238411_a_(@Nonnull String string, float x, float y, int color, boolean dropShadow, Matrix4f matrix,
                              @Nonnull IRenderTypeBuffer buffer, boolean seeThrough, int colorBackground, int packedLight, boolean bidiFlag) {
        // bidiFlag is useless, we have our layout system
        x += drawLayer0(string, x, y, color, dropShadow, matrix, buffer, seeThrough, colorBackground, packedLight, Style.EMPTY);
        return (int) x + (dropShadow ? 1 : 0);
    }

    @Override
    public int func_243247_a(@Nonnull ITextComponent text, float x, float y, int color, boolean dropShadow, @Nonnull Matrix4f matrix,
                             @Nonnull IRenderTypeBuffer buffer, boolean seeThrough, int colorBackground, int packedLight) {
        mutableFloat.setValue(x);
        // iterate all siblings
        text.func_230439_a_((style, string) -> {
            mutableFloat.add(drawLayer0(string, mutableFloat.floatValue(), y, color, dropShadow, matrix,
                    buffer, seeThrough, colorBackground, packedLight, style));
            // continue
            return Optional.empty();
        }, Style.EMPTY);
        return mutableFloat.intValue() + (dropShadow ? 1 : 0);
    }

    @Override
    public int func_238416_a_(@Nonnull IReorderingProcessor text, float x, float y, int color, boolean dropShadow, @Nonnull Matrix4f matrix,
                              @Nonnull IRenderTypeBuffer buffer, boolean seeThrough, int colorBackground, int packedLight) {
        mutableFloat.setValue(x);
        processor.copier.copyAndConsume(text, (string, style) -> {
                    mutableFloat.add(drawLayer0(string, mutableFloat.floatValue(), y, color, dropShadow, matrix,
                            buffer, seeThrough, colorBackground, packedLight, style));
                    // continue, equals to Optional.empty()
                    return false;
                }
        );
        return mutableFloat.intValue() + (dropShadow ? 1 : 0);
    }

    private float drawLayer0(@Nonnull CharSequence string, float x, float y, int color, boolean dropShadow, Matrix4f matrix,
                             @Nonnull IRenderTypeBuffer buffer, boolean transparent, int colorBackground, int packedLight, Style style) {
        if (string.length() == 0) {
            return 0;
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

        TextRenderNode node = processor.lookupVanillaNode(string, style);
        if (dropShadow && sAllowFontShadow) {
            node.drawText(matrix, buffer, string, x + 1, y + 1, r >> 2, g >> 2, b >> 2, a, true,
                    transparent, colorBackground, packedLight);
            matrix = matrix.copy();
            matrix.translate(SHADOW_LIFTING);
        }

        return node.drawText(matrix, buffer, string, x, y, r, g, b, a, false, transparent, colorBackground, packedLight);
    }

    /*@Override
    public int getStringWidth(@Nullable String string) {
        if (string == null || string.isEmpty()) {
            return 0;
        }
        return MathHelper.ceil(processor.lookupVanillaNode(string, Style.EMPTY).advance);
    }

    @Override
    public int func_238414_a_(@Nonnull ITextProperties text) {
        MutableFloat m = new MutableFloat(0);
        // iterate the multi text
        text.func_230439_a_((style, string) -> {
            if (!string.isEmpty()) {
                m.add(processor.lookupVanillaNode(string, style).advance);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return MathHelper.ceil(m.floatValue());
    }

    @Override
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

    }

    @Deprecated
    @Override
    public void close() {

    }*/

    @Deprecated
    @Override
    public boolean getBidiFlag() {
        return false;
    }

    /**
     * Bidi always works no matter what language is in.
     * So we should analyze the original string without reordering.
     *
     * @param text text
     * @return text
     */
    @Deprecated
    @Nonnull
    @Override
    public String bidiReorder(@Nonnull String text) {
        return text;
    }
}
