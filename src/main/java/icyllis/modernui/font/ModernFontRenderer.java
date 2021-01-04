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
import icyllis.modernui.font.pipeline.TextRenderNode;
import icyllis.modernui.font.process.TextLayoutProcessor;
import icyllis.modernui.graphics.RenderCore;
import icyllis.modernui.system.mixin.AccessFontRenderer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.fonts.Font;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.CharacterManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Function;

/**
 * Replace vanilla renderer with Modern UI renderer
 *
 * @author BloCamLimb
 */
@OnlyIn(Dist.CLIENT)
public class ModernFontRenderer extends FontRenderer {

    /**
     * Render thread instance
     */
    private static ModernFontRenderer instance;

    /**
     * Config values
     *
     * @see icyllis.modernui.system.Config.Client
     */
    private boolean allowShadow = true;
    private boolean globalRenderer = false;

    private final TextLayoutProcessor fontEngine = TextLayoutProcessor.getInstance();

    // temporary float value
    private final MutableFloat v = new MutableFloat();

    private ModernTextHandler textHandler;
    private CharacterManager stringSplitter; // vanilla

    private ModernFontRenderer(Function<ResourceLocation, Font> fonts) {
        super(fonts);
    }

    public static FontRenderer create(Function<ResourceLocation, Font> fonts) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        if (instance == null) {
            instance = new ModernFontRenderer(fonts);
            CharacterManager o = ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class,
                    instance, "field_238402_e_");
            @Deprecated
            CharacterManager.ICharWidthProvider c = ObfuscationReflectionHelper.getPrivateValue(CharacterManager.class,
                    o, "field_238347_a_");
            instance.textHandler = new ModernTextHandler(c);
            instance.stringSplitter = o;
            return instance;
        } else {
            throw new IllegalStateException("Already created");
        }
    }

    public static void change(boolean global) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        if (RenderCore.isRenderEngineStarted()) {
            if (instance.globalRenderer != global) {
                if (global) {
                    ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class,
                            instance, instance.textHandler, "field_238402_e_");
                } else {
                    ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class,
                            instance, instance.stringSplitter, "field_238402_e_");
                }
            }
            instance.globalRenderer = global;
        }
    }

    public static boolean isGlobalRenderer() {
        return instance.globalRenderer;
    }

    public static void setAllowShadow(boolean allow) {
        instance.allowShadow = allow;
    }

    /*static void hook(boolean doHook) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        if (instance == null) {
            instance = new ModernFontRenderer();
            Minecraft minecraft = Minecraft.getInstance();

            Function<ResourceLocation, Font> r = ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class,
                    minecraft.fontRenderer, "field_211127_e");

            ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class,
                    instance, r, "field_211127_e");
        }
        instance.hook0(doHook);
    }

    private void hook0(boolean doHook) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.fontRenderer == instance == doHook) {
            return;
        }
        if (doHook) {
            vanillaRenderer = minecraft.fontRenderer;
            try {
                ObfuscationReflectionHelper.findField(Minecraft.class, "field_71466_p")
                        .set(minecraft, this);
                ObfuscationReflectionHelper.findField(EntityRendererManager.class, "field_78736_p")
                        .set(minecraft.getRenderManager(), this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
            try {
                ObfuscationReflectionHelper.findField(Minecraft.class, "field_71466_p")
                        .set(minecraft, vanillaRenderer);
                ObfuscationReflectionHelper.findField(EntityRendererManager.class, "field_78736_p")
                        .set(minecraft.getRenderManager(), vanillaRenderer);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }*/

    @Override
    public int func_238411_a_(@Nonnull String string, float x, float y, int color, boolean dropShadow, @NotNull Matrix4f matrix,
                              @Nonnull IRenderTypeBuffer buffer, boolean seeThrough, int colorBackground, int packedLight, boolean bidiFlag) {
        if (globalRenderer) {
            // bidiFlag is useless, we have our layout system
            x += drawLayer0(string, x, y, color, dropShadow, matrix, buffer, seeThrough, colorBackground, packedLight, Style.EMPTY);
            return (int) x + (dropShadow ? 1 : 0);
        }
        return super.func_238411_a_(string, x, y, color, dropShadow, matrix, buffer, seeThrough, colorBackground, packedLight, bidiFlag);
    }

    @Override
    public int func_243247_a(@Nonnull ITextComponent text, float x, float y, int color, boolean dropShadow, @Nonnull Matrix4f matrix,
                             @Nonnull IRenderTypeBuffer buffer, boolean seeThrough, int colorBackground, int packedLight) {
        if (globalRenderer) {
            v.setValue(x);
            // iterate all siblings
            text.getComponentWithStyle((style, string) -> {
                v.add(drawLayer0(string, v.floatValue(), y, color, dropShadow, matrix,
                        buffer, seeThrough, colorBackground, packedLight, style));
                // continue
                return Optional.empty();
            }, Style.EMPTY);
            return v.intValue() + (dropShadow ? 1 : 0);
        }
        return super.func_243247_a(text, x, y, color, dropShadow, matrix, buffer, seeThrough, colorBackground, packedLight);
    }

    @Override
    public int func_238416_a_(@Nonnull IReorderingProcessor text, float x, float y, int color, boolean dropShadow, @Nonnull Matrix4f matrix,
                              @Nonnull IRenderTypeBuffer buffer, boolean seeThrough, int colorBackground, int packedLight) {
        if (globalRenderer) {
            v.setValue(x);
            fontEngine.handleSequence(text,
                    (string, style) -> {
                        v.add(drawLayer0(string, v.floatValue(), y, color, dropShadow, matrix,
                                buffer, seeThrough, colorBackground, packedLight, style));
                        // continue, equals to Optional.empty()
                        return false;
                    }
            );
            return v.intValue() + (dropShadow ? 1 : 0);
        }
        return super.func_238416_a_(text, x, y, color, dropShadow, matrix, buffer, seeThrough, colorBackground, packedLight);
    }

    private float drawLayer0(@Nonnull CharSequence string, float x, float y, int color, boolean dropShadow, Matrix4f matrix,
                             @Nonnull IRenderTypeBuffer buffer, boolean seeThrough, int colorBackground, int packedLight, Style style) {
        if (string.length() == 0)
            return 0;

        // ensure alpha, color can be ARGB, or can be RGB
        // we check if alpha <= 1, then make alpha = 255 (fully opaque)
        if ((color & 0xfe000000) == 0)
            color |= 0xff000000;

        int a = color >> 24 & 0xff;
        int r = color >> 16 & 0xff;
        int g = color >> 8 & 0xff;
        int b = color & 0xff;

        TextRenderNode node = fontEngine.lookupVanillaNode(string, style);
        if (dropShadow && allowShadow) {
            node.drawText(matrix, buffer, string, x + 0.8f, y + 0.8f, r >> 2, g >> 2, b >> 2, a, true,
                    seeThrough, colorBackground, packedLight);
            matrix = matrix.copy(); // if not drop shadow, we don't need to copy the matrix
            matrix.translate(AccessFontRenderer.shadowLifting());
        }

        return node.drawText(matrix, buffer, string, x, y, r, g, b, a, false, seeThrough, colorBackground, packedLight);
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
        if (globalRenderer) {
            return false;
        }
        return super.getBidiFlag();
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
        if (globalRenderer) {
            return text;
        }
        return super.bidiReorder(text);
    }
}
