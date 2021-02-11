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

package icyllis.modernui.graphics.text;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix4f;
import icyllis.modernui.graphics.RenderCore;
import icyllis.modernui.graphics.font.pipeline.TextRenderNode;
import icyllis.modernui.mcimpl.mixin.AccessFontRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
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
@Environment(EnvType.CLIENT)
public class ModernFontRenderer extends Font {

    /**
     * Render thread instance
     */
    private static ModernFontRenderer instance;

    /**
     * Config values
     */
    private boolean allowShadow = true;
    private boolean globalRenderer = false;

    private final TextLayoutProcessor fontEngine = TextLayoutProcessor.getInstance();

    // temporary float value used in lambdas
    private final MutableFloat v = new MutableFloat();

    private ModernStringSplitter modernStringSplitter;
    private StringSplitter vanillaStringSplitter;

    private ModernFontRenderer(Function<ResourceLocation, FontSet> fonts) {
        super(fonts);
    }

    public static Font create(Function<ResourceLocation, FontSet> fonts) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        if (instance == null) {
            ModernFontRenderer i = new ModernFontRenderer(fonts);
            StringSplitter o = i.getSplitter();
            @Deprecated
            StringSplitter.WidthProvider c = (codePoint, style) ->
                    i.fontEngine.lookupVanillaNode(new String(new int[]{codePoint}, 0, 1), style).advance;
            i.modernStringSplitter = new ModernStringSplitter(c);
            i.vanillaStringSplitter = o;
            return instance = i;
        } else {
            throw new IllegalStateException("Already created");
        }
    }

    public static void change(boolean global, boolean shadow) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        if (RenderCore.isRenderEngineStarted()) {
            if (instance.globalRenderer != global) {
                ((AccessFontRenderer) instance).setSplitter(global ? instance.modernStringSplitter : instance.vanillaStringSplitter);
                instance.globalRenderer = global;
            }
            instance.allowShadow = shadow;
        }
    }

    public static boolean isGlobalRenderer() {
        return instance.globalRenderer;
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
    public int drawInBatch(@Nonnull String text, float x, float y, int color, boolean dropShadow, @NotNull Matrix4f matrix,
                           @Nonnull MultiBufferSource buffer, boolean seeThrough, int colorBackground, int packedLight, boolean bidiFlag) {
        if (globalRenderer) {
            // bidiFlag is useless, we have our layout system
            x += drawLayer0(text, x, y, color, dropShadow, matrix, buffer, seeThrough, colorBackground, packedLight, Style.EMPTY);
            return (int) x + (dropShadow ? 1 : 0);
        }
        return super.drawInBatch(text, x, y, color, dropShadow, matrix, buffer, seeThrough, colorBackground, packedLight, bidiFlag);
    }

    @Override
    public int drawInBatch(@Nonnull Component text, float x, float y, int color, boolean dropShadow, @Nonnull Matrix4f matrix,
                           @Nonnull MultiBufferSource buffer, boolean seeThrough, int colorBackground, int packedLight) {
        if (globalRenderer) {
            v.setValue(x);
            // iterate all siblings
            text.visit((style, t) -> {
                v.add(drawLayer0(t, v.floatValue(), y, color, dropShadow, matrix,
                        buffer, seeThrough, colorBackground, packedLight, style));
                // continue
                return Optional.empty();
            }, Style.EMPTY);
            return v.intValue() + (dropShadow ? 1 : 0);
        }
        return super.drawInBatch(text, x, y, color, dropShadow, matrix, buffer, seeThrough, colorBackground, packedLight);
    }

    // compatibility layer
    public void drawText(@Nonnull FormattedText text, float x, float y, int color, boolean dropShadow, @Nonnull Matrix4f matrix,
                         @Nonnull MultiBufferSource buffer, boolean seeThrough, int colorBackground, int packedLight) {
        if (globalRenderer) {
            v.setValue(x);
            // iterate all siblings
            text.visit((style, t) -> {
                v.add(drawLayer0(t, v.floatValue(), y, color, dropShadow, matrix,
                        buffer, seeThrough, colorBackground, packedLight, style));
                // continue
                return Optional.empty();
            }, Style.EMPTY);
        } else {
            super.drawInBatch(Language.getInstance().getVisualOrder(text), x, y, color, dropShadow, matrix, buffer, seeThrough, colorBackground, packedLight);
        }
    }

    @Override
    public int drawInBatch(@Nonnull FormattedCharSequence text, float x, float y, int color, boolean dropShadow, @Nonnull Matrix4f matrix,
                           @Nonnull MultiBufferSource buffer, boolean seeThrough, int colorBackground, int packedLight) {
        if (globalRenderer) {
            v.setValue(x);
            fontEngine.handleSequence(text,
                    (t, style) -> {
                        v.add(drawLayer0(t, v.floatValue(), y, color, dropShadow, matrix,
                                buffer, seeThrough, colorBackground, packedLight, style));
                        // continue, equals to Optional.empty()
                        return false;
                    }
            );
            return v.intValue() + (dropShadow ? 1 : 0);
        }
        return super.drawInBatch(text, x, y, color, dropShadow, matrix, buffer, seeThrough, colorBackground, packedLight);
    }

    private float drawLayer0(@Nonnull CharSequence text, float x, float y, int color, boolean dropShadow, Matrix4f matrix,
                             @Nonnull MultiBufferSource buffer, boolean seeThrough, int colorBackground, int packedLight, Style style) {
        if (text.length() == 0)
            return 0;

        // ensure alpha, color can be ARGB, or can be RGB
        // we check if alpha <= 1, then make alpha = 255 (fully opaque)
        if ((color & 0xfe000000) == 0)
            color |= 0xff000000;

        int a = color >> 24 & 0xff;
        int r = color >> 16 & 0xff;
        int g = color >> 8 & 0xff;
        int b = color & 0xff;

        TextRenderNode node = fontEngine.lookupVanillaNode(text, style);
        if (dropShadow && allowShadow) {
            node.drawText(matrix, buffer, text, x + 0.8f, y + 0.8f, r >> 2, g >> 2, b >> 2, a, true,
                    seeThrough, colorBackground, packedLight);
            matrix = matrix.copy(); // if not drop shadow, we don't need to copy the matrix
            matrix.translate(AccessFontRenderer.shadowLifting());
        }

        return node.drawText(matrix, buffer, text, x, y, r, g, b, a, false, seeThrough, colorBackground, packedLight);
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

    /**
     * Bidi and shaping always works no matter what language is in.
     * So we should analyze the original string without reordering.
     *
     * @param text text
     * @return text
     * @see icyllis.modernui.mcimpl.mixin.MixinClientLanguage#getVisualOrder(FormattedText)
     */
    @Deprecated
    @Nonnull
    @Override
    public String bidirectionalShaping(@Nonnull String text) {
        if (globalRenderer)
            return text;
        return super.bidirectionalShaping(text);
    }
}
