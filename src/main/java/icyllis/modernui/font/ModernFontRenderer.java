/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.font;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.math.Color3f;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.fonts.providers.IGlyphProvider;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.TransformationMatrix;
import net.minecraft.client.renderer.Vector4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Use modern ui font renderer to replace vanilla's renderer
 */
public class ModernFontRenderer extends FontRenderer {

    private final IFontRenderer fontRenderer;

    public ModernFontRenderer(IFontRenderer fontRenderer) {
        super(null, null);
        this.fontRenderer = fontRenderer;
    }

    @Override
    public int drawString(@Nullable String text, float x, float y, int color) {
        return drawStringInternal(text, x, y, color, false, TransformationMatrix.identity().getMatrix());
    }

    @Override
    public int drawStringWithShadow(@Nullable String text, float x, float y, int color) {
        return drawStringInternal(text, x, y, color, true, TransformationMatrix.identity().getMatrix());
    }

    @Override
    //TODO last 4 params is not supported
    public int renderString(@Nullable String text, float x, float y, int color, boolean dropShadow, Matrix4f matrix, @Nonnull IRenderTypeBuffer buffer, boolean transparentIn, int colorBackgroundIn, int packedLight) {
        return drawStringInternal(text, x, y, color, dropShadow, matrix);
    }

    private int drawStringInternal(@Nullable String text, float x, float y, int color, boolean dropShadow, Matrix4f matrix) {
        // alpha test

        if ((color & 0xfe000000) == 0) {
            color |= 0xff000000;
        }

        float a = Color3f.getAlphaFrom(color);
        float r = Color3f.getRedFrom(color);
        float g = Color3f.getGreenFrom(color);
        float b = Color3f.getBlueFrom(color);

        /*Vector4f vector4f = new Vector4f(x, y, 0, 1.0f);
        vector4f.transform(matrix);
        if (dropShadow) {
            //this.renderStringAtPos(text, x, y, color, true, matrix, buffer, transparentIn, colorBackgroundIn, packedLight);
            fontRenderer.drawString(text, vector4f.getX() + 1, vector4f.getY() + 1, r * 0.25f, g * 0.25f, b * 0.25f, a);
        }

        Matrix4f matrix4f = matrix.copy();
        matrix4f.translate(new Vector3f(0.0F, 0.0F, 0.001F));*/

        Vector4f vector4f = new Vector4f(x, y, 0.001f, 1.0f);
        vector4f.transform(matrix);
        RenderSystem.pushMatrix();
        RenderSystem.translatef(0, 0, vector4f.getZ());
        float w = fontRenderer.drawString(text, vector4f.getX(), vector4f.getY(), r, g, b, a);
        RenderSystem.popMatrix();
        return text == null ? 0 : (int) (x + w + (dropShadow ? 1.0f : 0.0f));
    }

    @Override
    public int getStringWidth(@Nullable String text) {
        return (int) fontRenderer.getStringWidth(text);
    }

    @Override
    public float getCharWidth(char character) {
        return (int) fontRenderer.getStringWidth(String.valueOf(character));
    }

    @Nonnull
    @Override
    public String trimStringToWidth(@Nonnull String text, int width, boolean reverse) {
        return fontRenderer.trimStringToWidth(text, width, reverse);
    }

    @Override
    public void drawSplitString(@Nullable String text, int x, int y, int wrapWidth, int textColor) {
        if (text == null) {
            return;
        }
        while (text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }
        List<String> list = listFormattedStringToWidth(text, wrapWidth);
        Matrix4f matrix4f = TransformationMatrix.identity().getMatrix();
        for (String s : list) {
            drawStringInternal(s, x, y, textColor, false, matrix4f);
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

    @Deprecated
    @Override
    public void setBidiFlag(boolean bidiFlagIn) {

    }

    @Deprecated
    @Override
    public boolean getBidiFlag() {
        return false;
    }
}
