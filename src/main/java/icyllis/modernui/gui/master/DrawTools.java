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
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

@SuppressWarnings("DuplicatedCode")
public class DrawTools {

    public static void fillRectWithColor(float left, float top, float right, float bottom, int color) {
        int a = color >> 24 & 255;
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
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
        RenderSystem.disableBlend();
    }

    public static void fillRectWithColor(float left, float top, float right, float bottom, int color, float opacity) {
        int a = (int) (opacity * 255F);
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
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
        RenderSystem.disableBlend();
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
        RenderSystem.disableBlend();
    }

    public static void fillRectWithFrame(float left, float top, float right, float bottom, float thickness, int innerColor, float innerOpacity, int frameColor, float frameOpacity) {
        int a = (int) (innerOpacity * 255F);
        int r = innerColor >> 16 & 255;
        int g = innerColor >> 8 & 255;
        int b = innerColor & 255;

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

        a = (int) (frameOpacity * 255F);
        r = frameColor >> 16 & 255;
        g = frameColor >> 8 & 255;
        b = frameColor & 255;

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
        RenderSystem.disableBlend();
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
        blitToScale(x, y, 0, u, v, width, height);
    }

    public static void blitWithZ(float x, float y, float z, float u, float v, float width, float height) {
        blitToScale(x, y, z, u, v, width, height);
    }

    private static void blitToScale(float x, float y, float z, float textureX, float textureY, float width, float height) {
        blitRender(x, x + width, y, y + height, z, textureX / 256.0f, (textureX + width) / 256.0f, textureY / 256.0f, (textureY + height) / 256.0f);
    }

    private static void blitRender(double x1, double x2, double y1, double y2, double z, float textureX1, float textureX2, float textureY1, float textureY2) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x1, y2, z).tex(textureX1, textureY2).endVertex();
        bufferbuilder.pos(x2, y2, z).tex(textureX2, textureY2).endVertex();
        bufferbuilder.pos(x2, y1, z).tex(textureX2, textureY1).endVertex();
        bufferbuilder.pos(x1, y1, z).tex(textureX1, textureY1).endVertex();
        tessellator.draw();
    }
}
