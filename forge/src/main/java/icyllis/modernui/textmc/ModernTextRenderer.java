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

import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.fml.loading.FMLEnvironment;

import javax.annotation.Nonnull;

/**
 * Modern Text Engine to Minecraft font renderer.
 *
 * @author BloCamLimb
 */
public final class ModernTextRenderer {

    static {
        if (FMLEnvironment.dist.isDedicatedServer()) {
            throw new RuntimeException();
        }
    }

    public static final Vector3f SHADOW_OFFSET = new Vector3f(0.0F, 0.0F, 0.03F);
    public static final Vector3f OUTLINE_OFFSET = new Vector3f(0.0F, 0.0F, 0.0001F);

    /*
     * Render thread instance
     */
    //private static volatile ModernFontRenderer instance;

    /**
     * Config values
     */
    public static volatile boolean sAllowShadow = true;
    public static volatile float sShadowOffset = 0.8f;
    public static volatile float sOutlineOffset = 0.5f;
    //private boolean mGlobalRenderer = false;

    //private final TextLayoutEngine mFontEngine = TextLayoutEngine.getInstance();

    // temporary float value used in lambdas
    //private final MutableFloat v = new MutableFloat();

    //private ModernStringSplitter mModernSplitter;

    /*private ModernFontRenderer(Function<ResourceLocation, FontSet> fonts) {
        super(fonts);
    }

    private static Font create(Function<ResourceLocation, FontSet> fonts) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        if (instance == null) {
            ModernFontRenderer i = new ModernFontRenderer(fonts);
            i.mModernSplitter = new ModernStringSplitter(null);
            return instance = i;
        } else {
            throw new IllegalStateException("Already created");
        }
    }*/

    public static float drawText(@Nonnull String text, float x, float y, int color, boolean dropShadow,
                                 @Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, boolean seeThrough,
                                 int colorBackground, int packedLight) {
        if (text.isEmpty()) {
            return x;
        }

        // ensure alpha, color can be ARGB, or can be RGB
        // we check if alpha <= 1, then make alpha = 255 (fully opaque)
        /*if ((color & 0xfe000000) == 0) {
            color |= 0xff000000;
        }*/

        int a = color >>> 24;
        if (a <= 1) a = 255;
        int r = color >> 16 & 0xff;
        int g = color >> 8 & 0xff;
        int b = color & 0xff;

        TextLayoutEngine layoutEngine = TextLayoutEngine.getInstance();
        TextRenderNode node = layoutEngine.lookupVanillaNode(text);
        float scale = layoutEngine.getCoordinateScale();
        float level = layoutEngine.getResolutionLevel();
        if (node.hasColorEmoji() && source instanceof MultiBufferSource.BufferSource) {
            // performance impact
            ((MultiBufferSource.BufferSource) source).endBatch(Sheets.signSheet());
        }
        if (dropShadow && sAllowShadow) {
            float offset = sShadowOffset;
            node.drawText(matrix, source, text, x + offset, y + offset, r >> 2, g >> 2, b >> 2, a, true,
                    seeThrough, colorBackground, packedLight, scale, level);
            matrix = matrix.copy(); // if not drop shadow, we don't need to copy the matrix
            matrix.translate(SHADOW_OFFSET);
        }

        x += node.drawText(matrix, source, text, x, y, r, g, b, a, false,
                seeThrough, colorBackground, packedLight, scale, level);
        return x;
    }

    public static float drawText(@Nonnull FormattedText text, float x, float y, int color, boolean dropShadow,
                                 @Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, boolean seeThrough,
                                 int colorBackground, int packedLight) {
        if (text == CommonComponents.EMPTY || text == FormattedText.EMPTY) {
            return x;
        }

        // ensure alpha, color can be ARGB, or can be RGB
        // we check if alpha <= 1, then make alpha = 255 (fully opaque)
        /*if ((color & 0xfe000000) == 0) {
            color |= 0xff000000;
        }*/

        int a = color >>> 24;
        if (a <= 1) a = 255;
        int r = color >> 16 & 0xff;
        int g = color >> 8 & 0xff;
        int b = color & 0xff;

        TextLayoutEngine layoutEngine = TextLayoutEngine.getInstance();
        TextRenderNode node = layoutEngine.lookupComplexNode(text);
        float scale = layoutEngine.getCoordinateScale();
        float level = layoutEngine.getResolutionLevel();
        if (node.hasColorEmoji() && source instanceof MultiBufferSource.BufferSource) {
            // performance impact
            ((MultiBufferSource.BufferSource) source).endBatch(Sheets.signSheet());
        }
        if (dropShadow && sAllowShadow) {
            float offset = sShadowOffset;
            node.drawText(matrix, source, null, x + offset, y + offset, r >> 2, g >> 2, b >> 2, a, true,
                    seeThrough, colorBackground, packedLight, scale, level);
            matrix = matrix.copy(); // if not drop shadow, we don't need to copy the matrix
            matrix.translate(SHADOW_OFFSET);
        }

        x += node.drawText(matrix, source, null, x, y, r, g, b, a, false,
                seeThrough, colorBackground, packedLight, scale, level);
        return x;
    }

    public static float drawText(@Nonnull FormattedCharSequence text, float x, float y, int color, boolean dropShadow,
                                 @Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, boolean seeThrough,
                                 int colorBackground, int packedLight) {
        if (text == FormattedCharSequence.EMPTY) {
            return x;
        }

        // ensure alpha, color can be ARGB, or can be RGB
        // we check if alpha <= 1, then make alpha = 255 (fully opaque)
        /*if ((color & 0xfe000000) == 0) {
            color |= 0xff000000;
        }*/

        int a = color >>> 24;
        if (a <= 1) a = 255;
        int r = color >> 16 & 0xff;
        int g = color >> 8 & 0xff;
        int b = color & 0xff;

        TextLayoutEngine layoutEngine = TextLayoutEngine.getInstance();
        TextRenderNode node = layoutEngine.lookupSequenceNode(text);
        float scale = layoutEngine.getCoordinateScale();
        float level = layoutEngine.getResolutionLevel();
        if (node.hasColorEmoji() && source instanceof MultiBufferSource.BufferSource) {
            // performance impact
            ((MultiBufferSource.BufferSource) source).endBatch(Sheets.signSheet());
        }
        if (dropShadow && sAllowShadow) {
            float offset = sShadowOffset;
            node.drawText(matrix, source, null, x + offset, y + offset, r >> 2, g >> 2, b >> 2, a, true,
                    seeThrough, colorBackground, packedLight, scale, level);
            matrix = matrix.copy(); // if not drop shadow, we don't need to copy the matrix
            matrix.translate(SHADOW_OFFSET);
        }

        x += node.drawText(matrix, source, null, x, y, r, g, b, a, false,
                seeThrough, colorBackground, packedLight, scale, level);
        return x;
    }

    public static void drawText8xOutline(@Nonnull FormattedCharSequence text, float x, float y,
                                         int color, int outlineColor, @Nonnull Matrix4f matrix,
                                         @Nonnull MultiBufferSource source, int packedLight) {
        if (text == FormattedCharSequence.EMPTY) {
            return;
        }

        int a = color >>> 24;
        if (a <= 1) a = 255;
        int r = color >> 16 & 0xff;
        int g = color >> 8 & 0xff;
        int b = color & 0xff;

        int oa = outlineColor >>> 24;
        if (oa <= 1) oa = 255;
        int or = outlineColor >> 16 & 0xff;
        int og = outlineColor >> 8 & 0xff;
        int ob = outlineColor & 0xff;

        TextLayoutEngine layoutEngine = TextLayoutEngine.getInstance();
        TextRenderNode node = layoutEngine.lookupSequenceNode(text);
        float scale = layoutEngine.getCoordinateScale();
        float level = layoutEngine.getResolutionLevel();
        if (node.hasColorEmoji() && source instanceof MultiBufferSource.BufferSource) {
            // performance impact
            ((MultiBufferSource.BufferSource) source).endBatch(Sheets.signSheet());
        }

        final float offset = sOutlineOffset;
        matrix = matrix.copy();
        //TODO use one-pass shader without transparency?
        for (int ox = -1; ox <= 1; ++ox) {
            for (int oy = -1; oy <= 1; ++oy) {
                if (ox == 0 && oy == 0) {
                    continue;
                }
                node.drawTextOutline(matrix, source, x + ox * offset, y + oy * offset,
                        or, og, ob, oa, packedLight, scale, level);
                matrix.translate(OUTLINE_OFFSET);
            }
        }

        node.drawText(matrix, source, null, x, y, r, g, b, a, false,
                false, 0, packedLight, scale, level);
    }

    /*public static void change(boolean global, boolean shadow) {
        RenderCore.checkRenderThread();
        if (RenderCore.isInitialized()) {
            instance.mGlobalRenderer = global;
            instance.mAllowShadow = shadow;
        }
    }*/

    /*public static boolean isGlobalRenderer() {
        return false;
    }*/

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

    /*@Override
    public int drawInBatch(@Nonnull String text, float x, float y, int color, boolean dropShadow,
                           @NotNull Matrix4f matrix,
                           @Nonnull MultiBufferSource buffer, boolean seeThrough, int colorBackground,
                           int packedLight, boolean bidiFlag) {
        if (mGlobalRenderer) {
            // bidiFlag is useless, we have our layout system
            x += drawLayer(text, x, y, color, dropShadow, matrix, buffer, seeThrough, colorBackground, packedLight,
                    Style.EMPTY);
            return (int) x + (dropShadow ? 1 : 0);
        }
        return super.drawInBatch(text, x, y, color, dropShadow, matrix, buffer, seeThrough, colorBackground,
                packedLight, bidiFlag);
    }

    @Override
    public int drawInBatch(@Nonnull Component text, float x, float y, int color, boolean dropShadow,
                           @Nonnull Matrix4f matrix,
                           @Nonnull MultiBufferSource buffer, boolean seeThrough, int colorBackground,
                           int packedLight) {
        if (mGlobalRenderer) {
            v.setValue(x);
            // iterate all siblings
            text.visit((style, t) -> {
                v.add(drawLayer(t, v.floatValue(), y, color, dropShadow, matrix,
                        buffer, seeThrough, colorBackground, packedLight, style));
                // continue
                return Optional.empty();
            }, Style.EMPTY);
            return v.intValue() + (dropShadow ? 1 : 0);
        }
        return super.drawInBatch(text, x, y, color, dropShadow, matrix, buffer, seeThrough, colorBackground,
                packedLight);
    }

    // compatibility layer
    public void drawText(@Nonnull FormattedText text, float x, float y, int color, boolean dropShadow,
                         @Nonnull Matrix4f matrix,
                         @Nonnull MultiBufferSource buffer, boolean seeThrough, int colorBackground, int packedLight) {
        if (mGlobalRenderer) {
            v.setValue(x);
            // iterate all siblings
            text.visit((style, t) -> {
                v.add(drawLayer(t, v.floatValue(), y, color, dropShadow, matrix,
                        buffer, seeThrough, colorBackground, packedLight, style));
                // continue
                return Optional.empty();
            }, Style.EMPTY);
        } else {
            super.drawInBatch(Language.getInstance().getVisualOrder(text), x, y, color, dropShadow, matrix, buffer,
                    seeThrough, colorBackground, packedLight);
        }
    }

    @Override
    public int drawInBatch(@Nonnull FormattedCharSequence text, float x, float y, int color, boolean dropShadow,
                           @Nonnull Matrix4f matrix,
                           @Nonnull MultiBufferSource buffer, boolean seeThrough, int colorBackground,
                           int packedLight) {
        if (mGlobalRenderer && text.accept((index, style, codePoint) -> !style.getFont().equals(Minecraft.ALT_FONT))) {
            v.setValue(x);
            mFontEngine.handleSequence(text,
                    (t, style) -> {
                        v.add(drawLayer(t, v.floatValue(), y, color, dropShadow, matrix,
                                buffer, seeThrough, colorBackground, packedLight, style));
                        // continue, equals to Optional.empty()
                        return false;
                    }
            );
            return v.intValue() + (dropShadow ? 1 : 0);
        }
        return super.drawInBatch(text, x, y, color, dropShadow, matrix, buffer, seeThrough, colorBackground,
                packedLight);
    }*/

    /*public float drawLayer(@Nonnull CharSequence text, float x, float y, int color, boolean dropShadow, Matrix4f
    matrix,
                           @Nonnull MultiBufferSource buffer, boolean seeThrough, int colorBackground,
                           int packedLight, Style style) {
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

        TextRenderNode node = mFontEngine.lookupVanillaNode(text, style);
        if (dropShadow && mAllowShadow) {
            node.drawText(matrix, buffer, text, x + 0.8f, y + 0.8f, r >> 2, g >> 2, b >> 2, a, true,
                    seeThrough, colorBackground, packedLight, );
            matrix = matrix.copy(); // if not drop shadow, we don't need to copy the matrix
            matrix.translate(AccessFontRenderer.shadowLifting());
        }

        return node.drawText(matrix, buffer, text, x, y, r, g, b, a, false, seeThrough, colorBackground, packedLight,
         );
        return 0;
    }*/

    /*@Override
    public int width(String string) {
        if (mGlobalRenderer) {
            return Mth.ceil(mModernSplitter.measure(string));
        }
        return super.width(string);
    }

    @Override
    public int width(FormattedText text) {
        if (mGlobalRenderer) {
            return Mth.ceil(mModernSplitter.measure(text));
        }
        return super.width(text);
    }

    @Override
    public int width(FormattedCharSequence text) {
        if (mGlobalRenderer) {
            return Mth.ceil(mModernSplitter.stringWidth(text));
        }
        return super.width(text);
    }*/

    /*@Override
    public String plainSubstrByWidth(String text, int width, boolean reverse) {
        if (mGlobalRenderer) {
            return reverse ? mModernSplitter.trimReverse(text, width, Style.EMPTY) :
                    mModernSplitter.plainHeadByWidth(text, width, Style.EMPTY);
        }
        return super.plainSubstrByWidth(text, width, reverse);
    }

    @Override
    public String plainSubstrByWidth(String text, int width) {
        if (mGlobalRenderer) {
            return mModernSplitter.plainHeadByWidth(text, width, Style.EMPTY);
        }
        return super.plainSubstrByWidth(text, width);
    }

    @Override
    public FormattedText substrByWidth(FormattedText text, int width) {
        if (mGlobalRenderer)
            return mModernSplitter.trimText(text, width, Style.EMPTY);
        return super.substrByWidth(text, width);
    }*/

    /*@Override
    public int wordWrapHeight(String text, int width) {
        if (mGlobalRenderer)
            return lineHeight * mModernSplitter.splitLines(text, width, Style.EMPTY).size();
        return super.wordWrapHeight(text, width);
    }

    @Override
    public List<FormattedCharSequence> split(FormattedText text, int width) {
        if (mGlobalRenderer)
            return Language.getInstance().getVisualOrder(mModernSplitter.splitLines(text, width, Style.EMPTY));
        return super.split(text, width);
    }

    @Override
    public StringSplitter getSplitter() {
        return mGlobalRenderer ? mModernSplitter : super.getSplitter();
    }*/

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

    /*
     * Bidi and shaping always works no matter what language is in.
     * So we should analyze the original string without reordering.
     *
     * @param text text
     * @return text
     * @see MixinClientLanguage#getVisualOrder(FormattedText)
     */
    /*@Deprecated
    @Nonnull
    @Override
    public String bidirectionalShaping(@Nonnull String text) {
        if (mGlobalRenderer)
            return text;
        return super.bidirectionalShaping(text);
    }*/
}
