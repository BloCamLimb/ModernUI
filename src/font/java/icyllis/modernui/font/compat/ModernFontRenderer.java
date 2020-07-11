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

package icyllis.modernui.font.compat;

import icyllis.modernui.font.TrueTypeRenderer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.fonts.providers.IGlyphProvider;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.TransformationMatrix;
import net.minecraft.util.math.vector.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Replace vanilla renderer with Modern UI renderer
 */
public final class ModernFontRenderer extends FontRenderer {

    private static final ModernFontRenderer INSTANCE = new ModernFontRenderer(TrueTypeRenderer.getInstance());

    public static boolean sAllowFontShadow;

    private final TrueTypeRenderer fontRenderer;

    ModernFontRenderer(TrueTypeRenderer fontRenderer) {
        super(null);
        this.fontRenderer = fontRenderer;
    }

    /**
     * For compatibility, developers should not use this
     *
     * @return instance
     */
    public static ModernFontRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public int drawString(@Nullable String text, float x, float y, int color) {
        return drawString(text, x, y, color, TransformationMatrix.identity().getMatrix(), false);
    }

    @Override
    public int drawStringWithShadow(@Nullable String text, float x, float y, int color) {
        return drawString(text, x, y, color, TransformationMatrix.identity().getMatrix(), true);
    }

    private int drawString(@Nullable String text, float x, float y, int color, Matrix4f matrix, boolean dropShadow) {
        if (text == null) {
            return 0;
        } else {
            IRenderTypeBuffer.Impl impl = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
            int newX = renderString(text, x, y, color, dropShadow, matrix, impl, false, 0, 0x00f000f0);
            impl.finish();
            return newX;
        }
    }

    @Override
    public int renderString(@Nullable String text, float x, float y, int color, boolean dropShadow, Matrix4f matrix, @Nonnull IRenderTypeBuffer buffer, boolean transparentIn, int colorBackgroundIn, int packedLight) {
        return drawStringInternal(text, x, y, color, dropShadow, buffer, matrix, packedLight);
    }

    public int drawStringInternal(@Nullable String text, float x, float y, int color, boolean dropShadow, @Nonnull IRenderTypeBuffer buffer, Matrix4f matrix, int packedLight) {
        if (text == null) {
            return 0;
        }
        if (text.isEmpty()) {
            return (int) (x + (dropShadow ? 1.0f : 0.0f));
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

        if (dropShadow && sAllowFontShadow) {
            fontRenderer.drawStringGlobal(text, x + 1, y + 1, r, g, b, a, true, matrix, buffer, packedLight);
        }

        Matrix4f matrix4f = matrix.copy();
        matrix4f.translate(new Vector3f(0.0F, 0.0F, 0.001f));
        x += fontRenderer.drawStringGlobal(text, x, y, r, g, b, a, false, matrix4f, buffer, packedLight);
        return (int) (x + (dropShadow ? 1.0f : 0.0f));
    }

    @Override
    public int getStringWidth(@Nullable String text) {
        return MathHelper.ceil(fontRenderer.getStringWidth(text));
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
        // no font
    }

    @Deprecated
    @Override
    public void close() {
        // no stream
    }

    // we keep bidi enabled, so no need to convert text
    @Nonnull
    @Override
    public String bidiReorder(@Nonnull String text) {
        return text;
    }
}
