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

package icyllis.modernui.gui.master;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

@SuppressWarnings("DuplicatedCode")
public class DrawTools {

    public static void fillRectWithColor(float left, float top, float right, float bottom, int RGBA) {
        int a = RGBA >> 24 & 255;
        int r = RGBA >> 16 & 255;
        int g = RGBA >> 8 & 255;
        int b = RGBA & 255;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left, bottom, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, bottom, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, top, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();
        RenderSystem.enableTexture();
    }

    public static void fillRectWithColor(float left, float top, float right, float bottom, int RGB, float alpha) {
        int a = (int) (alpha * 255F);
        int r = RGB >> 16 & 255;
        int g = RGB >> 8 & 255;
        int b = RGB & 255;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left, bottom, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, bottom, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, top, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();
        RenderSystem.enableTexture();
    }

    public static void fillRectWithColor(float left, float top, float right, float bottom, float r, float g, float b, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left, bottom, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, bottom, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, top, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();
        RenderSystem.enableTexture();
    }

    public static void fillRectWithFrame(float left, float top, float right, float bottom, float thickness, int innerRGB, float innerAlpha, int frameRGB, float frameAlpha) {
        int a = (int) (innerAlpha * 255F);
        int r = innerRGB >> 16 & 255;
        int g = innerRGB >> 8 & 255;
        int b = innerRGB & 255;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left, bottom, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, bottom, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, top, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();

        a = (int) (frameAlpha * 255F);
        r = frameRGB >> 16 & 255;
        g = frameRGB >> 8 & 255;
        b = frameRGB & 255;

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left - thickness, top, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top - thickness, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left - thickness, top - thickness, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(right, bottom, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right + thickness, bottom, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right + thickness, top - thickness, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top - thickness, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left, bottom + thickness, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right + thickness, bottom + thickness, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right + thickness, bottom, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, bottom, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left - thickness, bottom + thickness, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, bottom + thickness, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, top, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left - thickness, top, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();

        RenderSystem.enableTexture();
    }

    public static void fillFrameWithColor(float left, float top, float right, float bottom, float thickness, int frameRGB, float frameAlpha) {

        int a = (int) (frameAlpha * 255F);
        int r = frameRGB >> 16 & 255;
        int g = frameRGB >> 8 & 255;
        int b = frameRGB & 255;

        fillFrameWithColor(left, top, right, bottom, thickness, r, g, b, a);
    }

    public static void fillFrameWithWhite(float left, float top, float right, float bottom, float thickness, float brightness, float frameAlpha) {

        int a = (int) (frameAlpha * 255F);
        int r = (int) (brightness * 255F);

        fillFrameWithColor(left, top, right, bottom, thickness, r, r, r, a);
    }

    public static void fillFrameWithColor(float left, float top, float right, float bottom, float thickness, int r, int g, int b, int a) {

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left - thickness, top, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top - thickness, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left - thickness, top - thickness, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(right, bottom, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right + thickness, bottom, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right + thickness, top - thickness, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top - thickness, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left, bottom + thickness, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right + thickness, bottom + thickness, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right + thickness, bottom, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, bottom, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left - thickness, bottom + thickness, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, bottom + thickness, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, top, 0.0D).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left - thickness, top, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();

        RenderSystem.enableTexture();
    }

    /*public static void fillGradient(float x, float y, float width, float height, int startColor, int endColor, float zLevel) {
        float f = (float)(startColor >> 24 & 255) / 255.0F;
        float f1 = (float)(startColor >> 16 & 255) / 255.0F;
        float f2 = (float)(startColor >> 8 & 255) / 255.0F;
        float f3 = (float)(startColor & 255) / 255.0F;
        float f4 = (float)(endColor >> 24 & 255) / 255.0F;
        float f5 = (float)(endColor >> 16 & 255) / 255.0F;
        float f6 = (float)(endColor >> 8 & 255) / 255.0F;
        float f7 = (float)(endColor & 255) / 255.0F;
        GlStateManager.disableTexture();
        GlStateManager.enableBlend();
        GlStateManager.disableAlphaTest();
        GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(width, y, zLevel).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos(x, y, zLevel).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos(x, height, zLevel).color(f5, f6, f7, f4).endVertex();
        bufferbuilder.pos(width, height, zLevel).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture();
    }*/

    public static void blit(float x, float y, float u, float v, float width, float height) {
        blit(x, y, u, v, width, height, 256, 256);
    }

    public static void blit(float x, float y, float textureX, float textureY, float width, float height, float textureWidth, float textureHeight) {
        blitFinal(x, x + width, y, y + height, textureX / textureWidth, (textureX + width) / textureWidth, textureY / textureHeight, (textureY + height) / textureHeight);
    }

    public static void blitIcon(float x, float y, float width, float height) {
        blitFinal(x, x + width, y, y + height, 0, 1, 0, 1);
    }

    private static void blitFinal(double x1, double x2, double y1, double y2, float textureX1, float textureX2, float textureY1, float textureY2) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x1, y2, 0.0D).tex(textureX1, textureY2).endVertex();
        bufferbuilder.pos(x2, y2, 0.0D).tex(textureX2, textureY2).endVertex();
        bufferbuilder.pos(x2, y1, 0.0D).tex(textureX2, textureY1).endVertex();
        bufferbuilder.pos(x1, y1, 0.0D).tex(textureX1, textureY1).endVertex();
        tessellator.draw();
    }
}
