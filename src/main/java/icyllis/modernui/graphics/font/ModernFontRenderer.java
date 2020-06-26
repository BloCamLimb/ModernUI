/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or 3.0 any later version.
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

package icyllis.modernui.graphics.font;

import icyllis.modernui.graphics.math.Color3i;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.fonts.providers.IGlyphProvider;
import net.minecraft.client.renderer.*;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Use modern ui font renderer to replace vanilla's renderer
 */
public class ModernFontRenderer extends FontRenderer {

    /* This instance shouldn't be called manually */
    public static ModernFontRenderer INSTANCE;

    private final TrueTypeRenderer fontRenderer;

    public static boolean sAllowFontShadow = true;

    protected ModernFontRenderer(TrueTypeRenderer fontRenderer) {
        super(null, null);
        this.fontRenderer = fontRenderer;
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
            int i = renderString(text, x, y, color, dropShadow, matrix, impl, false, 0, 0x00f000f0);
            impl.finish();
            return i;
        }
    }

    @Override
    public int renderString(@Nullable String text, float x, float y, int color, boolean dropShadow, Matrix4f matrix, @Nonnull IRenderTypeBuffer buffer, boolean transparentIn, int colorBackgroundIn, int packedLight) {
        return drawStringInternal(text, x, y, color, dropShadow, buffer, matrix, packedLight);
    }

    private int drawStringInternal(@Nullable String text, float x, float y, int color, boolean dropShadow, @Nonnull IRenderTypeBuffer buffer, Matrix4f matrix, int packedLight) {
        // alpha test

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
        matrix4f.translate(new Vector3f(0.0F, 0.0F, 0.001F));
        x += fontRenderer.drawStringGlobal(text, x, y, r, g, b, a, false, matrix4f, buffer, packedLight);
        return text == null ? 0 : (int) (x + (dropShadow ? 1.0f : 0.0f));
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

    }

    @Deprecated
    @Override
    public void close() {

    }

    @Deprecated
    @Nonnull
    @Override
    public String bidiReorder(@Nonnull String text) {
        throw new RuntimeException();
    }
}
