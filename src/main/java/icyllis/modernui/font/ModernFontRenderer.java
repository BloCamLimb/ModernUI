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
import com.mojang.datafixers.FunctionType;
import icyllis.modernui.gui.math.Align3H;
import icyllis.modernui.gui.math.Color3f;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.fonts.providers.IGlyphProvider;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.tileentity.SignTileEntityRenderer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Use modern ui font renderer to replace vanilla's renderer
 */
//TODO this has bugs in SignTileEntityRenderer, EditBookScreen(OpColor) etc
public class ModernFontRenderer extends FontRenderer {

    private final TrueTypeRenderer fontRenderer;

    public ModernFontRenderer(TrueTypeRenderer fontRenderer) {
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
        return drawStringInternal(text, x, y, color, dropShadow, matrix, packedLight);
    }

    private int drawStringInternal(@Nullable String text, float x, float y, int color, boolean dropShadow, Matrix4f matrix, int packedLight) {
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

        x += fontRenderer.drawString(text, x, y, r, g, b, a, Align3H.LEFT, matrix, packedLight);
        return text == null ? 0 : (int) (x + (dropShadow ? 1.0f : 0.0f));
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
