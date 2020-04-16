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
import icyllis.modernui.font.IFontRenderer;
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.font.TrueTypeRenderer;
import icyllis.modernui.graphics.shader.ShaderTools;
import icyllis.modernui.gui.math.Color3f;
import icyllis.modernui.gui.shader.CircleShader;
import icyllis.modernui.gui.shader.RingShader;
import icyllis.modernui.gui.shader.RoundedRectFrameShader;
import icyllis.modernui.gui.shader.RoundedRectShader;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

/**
 * Use paint brush and drawing board to draw everything!
 */
public class DrawTools {

    /**
     * Internal use
     */
    //protected static final DrawTools INSTANCE = new DrawTools();

    /**
     * Instances
     */
    private final Tessellator tessellator = Tessellator.getInstance();

    private final BufferBuilder bufferBuilder = tessellator.getBuffer();

    private final IFontRenderer fontRenderer = TrueTypeRenderer.INSTANCE;


    /**
     * Shaders instance
     */
    private final RingShader ring = RingShader.INSTANCE;

    private final RoundedRectShader roundedRect = RoundedRectShader.INSTANCE;

    private final RoundedRectFrameShader roundedRectFrame = RoundedRectFrameShader.INSTANCE;

    private final CircleShader circle = CircleShader.INSTANCE;


    /**
     * Paint colors
     */
    private float r = 1.0f;

    private float g = 1.0f;

    private float b = 1.0f;

    private float a = 1.0f;


    private double z = 0.0D;


    /**
     * Text align
     */
    private TextAlign textAlign = TextAlign.LEFT;


    /**
     * GL states
     */
    private static boolean lineAA = false;


    public DrawTools() {

    }

    /**
     * Set current paint color
     * @param r red [0,1]
     * @param g green [0,1]
     * @param b blue [0,1]
     * @param a alpha [0,1]
     */
    public void setRGBA(float r, float g, float b, float a) {
        setRGB(r, g, b);
        setAlpha(a);
    }

    /**
     * Set current paint color
     * @param r red [0,1]
     * @param g green [0,1]
     * @param b blue [0,1]
     */
    public void setRGB(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /**
     * Set current paint alpha
     * @param a alpha [0,1]
     */
    public void setAlpha(float a) {
        this.a = a;
    }

    public void setColor(@Nonnull Color3f color) {
        setRGB(color.getFloatRed(), color.getFloatGreen(), color.getFloatBlue());
    }

    public void setColor(@Nonnull Color3f color, float a) {
        setColor(color);
        setAlpha(a);
    }

    /**
     * Reset color to default color
     */
    public void resetColor() {
        setRGBA(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Enable or disable anti aliasing for lines
     * @param aa anti-aliasing
     */
    public void setLineAntiAliasing(boolean aa) {
        setLineAA(aa);
    }

    protected static void setLineAA(boolean aa) {
        if (aa && !lineAA) {
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            lineAA = true;
        } else if (!aa && lineAA) {
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            lineAA = false;
        }
    }

    /**
     * Set z pos / level, determines the depth, higher value will draw at the top
     * Minimum value and default value are 0
     *
     * @param z target z
     */
    public void setZ(double z) {
        this.z = z;
    }

    public void setTextAlign(TextAlign align) {
        this.textAlign = align;
    }

    /**
     * Draw text on screen, text formatting and bidi are supported
     * {@link net.minecraft.util.text.TextFormatting}
     *
     * @param text formatted string
     * @param x x pos
     * @param y y pos
     */
    public void drawText(String text, float x, float y) {
        fontRenderer.drawString(text, x, y, r, g, b, a, textAlign);
    }

    /**
     * Draw a rectangle on screen with given rect area
     *
     * @param left rect left
     * @param top rect top
     * @param right rect right
     * @param bottom rect bottom
     */
    public void drawRect(float left, float top, float right, float bottom) {
        RenderSystem.disableTexture();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(left, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, top, z).color(r, g, b, a).endVertex();
        tessellator.draw();
    }

    /**
     * Draw four rectangles outside the given rect with thickness
     *
     * @param left rect left
     * @param top rect top
     * @param right rect right
     * @param bottom rect bottom
     * @param thickness thickness
     */
    public void drawRectOutline(float left, float top, float right, float bottom, float thickness) {
        RenderSystem.disableTexture();

        bufferBuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(left - thickness, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, top - thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left - thickness, top - thickness, z).color(r, g, b, a).endVertex();
        tessellator.draw();

        bufferBuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(right, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right + thickness, top - thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, top - thickness, z).color(r, g, b, a).endVertex();
        tessellator.draw();

        bufferBuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(left, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right + thickness, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, bottom, z).color(r, g, b, a).endVertex();
        tessellator.draw();

        bufferBuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(left - thickness, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left - thickness, top, z).color(r, g, b, a).endVertex();
        tessellator.draw();
    }

    /**
     * Draw four lines around a rect area
     *
     * @param left rect left
     * @param top rect top
     * @param right rect right
     * @param bottom rect bottom
     */
    public void drawRectFrame(float left, float top, float right, float bottom) {
        RenderSystem.disableTexture();
        bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(left, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, bottom, z).color(r, g, b, a).endVertex();
        tessellator.draw();
    }

    /**
     * Draw four lines around a closed rect area, anti-aliasing is needed
     * Otherwise, there's a missing pixel
     *
     * @param left rect left
     * @param top rect top
     * @param right rect right
     * @param bottom rect bottom
     */
    public void drawRectLines(float left, float top, float right, float bottom) {
        RenderSystem.disableTexture();
        bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(left, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, top, z).color(r, g, b, a).endVertex();
        tessellator.draw();
    }

    /**
     * Draw ring / annulus on screen with given center pos and radius
     *
     * Default feather radius: 1 px
     *
     * @param centerX center x pos
     * @param centerY center y pos
     * @param innerRadius inner circle radius
     * @param outerRadius outer circle radius
     */
    public void drawRing(float centerX, float centerY, float innerRadius, float outerRadius) {
        ShaderTools.useShader(ring);
        ring.setRadius(innerRadius, outerRadius);
        ring.setCenterPos(centerX, centerY);
        drawRect(centerX - outerRadius, centerY - outerRadius, centerX + outerRadius, centerY + outerRadius);
        ShaderTools.releaseShader();
    }

    /**
     * Draw circle on screen with given center pos and radius
     *
     * Default feather radius: 1 px
     *
     * @param centerX center x pos
     * @param centerY center y pos
     * @param radius circle radius
     */
    public void drawCircle(float centerX, float centerY, float radius) {
        ShaderTools.useShader(circle);
        circle.setRadius(radius);
        circle.setCenterPos(centerX, centerY);
        drawRect(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        ShaderTools.releaseShader();
    }

    public void drawLine(float startX, float startY, float stopX, float stopY) {
        RenderSystem.disableTexture();
        bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(startX, startY, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(stopX, stopY, z).color(r, g, b, a).endVertex();
        tessellator.draw();
    }

    /**
     * Draw rounded rectangle on screen with given rect area and rounded radius
     *
     * Default feather radius: 1 px
     *
     * @param left rect left
     * @param top rect top
     * @param right rect right
     * @param bottom rect bottom
     * @param radius rounded radius, actually must >= 2
     */
    public void drawRoundedRect(float left, float top, float right, float bottom, float radius) {
        ShaderTools.useShader(roundedRect);
        roundedRect.setRadius(radius - 1); // we have feather radius 1px
        roundedRect.setInnerRect(left + radius, top + radius, right - radius, bottom - radius);
        drawRect(left, top, right, bottom);
        ShaderTools.releaseShader();
    }

    /**
     * Draw rounded rectangle frame in a rounded rect on screen
     * with given rect area and rounded radius
     *
     * Default feather radius: 1 px
     * Default frame thickness: 1 px
     *
     * @param left rect left
     * @param top rect top
     * @param right rect right
     * @param bottom rect bottom
     * @param radius rounded radius, actually must >= 2
     */
    public void drawRoundedRectFrame(float left, float top, float right, float bottom, float radius) {
        ShaderTools.useShader(roundedRectFrame);
        roundedRect.setRadius(radius - 1);
        roundedRect.setInnerRect(left + radius, top + radius, right - radius, bottom - radius);
        drawRect(left, top, right, bottom);
        ShaderTools.releaseShader();
    }

    /**
     * Draw icon on screen fitting to given rect area
     *
     * @param icon icon
     * @param left rect left
     * @param top rect top
     * @param right rect right
     * @param bottom rect bottom
     */
    public void drawIcon(@Nonnull Icon icon, float left, float top, float right, float bottom) {
        RenderSystem.enableTexture();
        icon.loadTexture();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
        bufferBuilder.pos(left, bottom, z).color(r, g, b, a).tex(icon.getLeft(), icon.getBottom()).endVertex();
        bufferBuilder.pos(right, bottom, z).color(r, g, b, a).tex(icon.getRight(), icon.getBottom()).endVertex();
        bufferBuilder.pos(right, top, z).color(r, g, b, a).tex(icon.getRight(), icon.getTop()).endVertex();
        bufferBuilder.pos(left, top, z).color(r, g, b, a).tex(icon.getLeft(), icon.getTop()).endVertex();
        tessellator.draw();
    }






    @Deprecated
    public static void blit(float x, float y, float u, float v, float width, float height) {
        blit(x, y, width, height, u, v, 256, 256);
    }

    public static void blit(float x, float y, float width, float height, float textureX, float textureY, float textureWidth, float textureHeight) {
        blitFinal(x, x + width, y, y + height, textureX / textureWidth, (textureX + width) / textureWidth, textureY / textureHeight, (textureY + height) / textureHeight);
    }

    private static void blitFinal(double x1, double x2, double y1, double y2, float textureX1, float textureX2, float textureY1, float textureY2) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        RenderSystem.enableTexture();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x1, y2, 0.0D).tex(textureX1, textureY2).endVertex();
        bufferbuilder.pos(x2, y2, 0.0D).tex(textureX2, textureY2).endVertex();
        bufferbuilder.pos(x2, y1, 0.0D).tex(textureX2, textureY1).endVertex();
        bufferbuilder.pos(x1, y1, 0.0D).tex(textureX1, textureY1).endVertex();
        tessellator.draw();
    }

    @Deprecated
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
        bufferbuilder.pos(left, bottom, 0).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, bottom, 0).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top, 0).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, top, 0).color(r, g, b, a).endVertex();
        tessellator.draw();
        RenderSystem.enableTexture();

    }

    /*@Deprecated
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
        bufferbuilder.pos(left, bottom, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, bottom, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, top, z).color(r, g, b, a).endVertex();
        tessellator.draw();
        RenderSystem.enableTexture();
    }

    @Deprecated
    public static void fillRectWithColor(float left, float top, float right, float bottom, float r, float g, float b, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left, bottom, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, bottom, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, top, z).color(r, g, b, a).endVertex();
        tessellator.draw();
        RenderSystem.enableTexture();
    }

    @Deprecated
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
        bufferbuilder.pos(left, bottom, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, bottom, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, top, z).color(r, g, b, a).endVertex();
        tessellator.draw();

        a = (int) (frameAlpha * 255F);
        r = frameRGB >> 16 & 255;
        g = frameRGB >> 8 & 255;
        b = frameRGB & 255;

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left - thickness, top, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top - thickness, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left - thickness, top - thickness, z).color(r, g, b, a).endVertex();
        tessellator.draw();

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(right, bottom, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right + thickness, top - thickness, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top - thickness, z).color(r, g, b, a).endVertex();
        tessellator.draw();

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right + thickness, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, bottom, z).color(r, g, b, a).endVertex();
        tessellator.draw();

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left - thickness, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, top, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left - thickness, top, z).color(r, g, b, a).endVertex();
        tessellator.draw();

        RenderSystem.enableTexture();
    }

    @Deprecated
    public static void fillFrameWithColor(float left, float top, float right, float bottom, float thickness, int frameRGB, float frameAlpha) {

        int a = (int) (frameAlpha * 255F);
        int r = frameRGB >> 16 & 255;
        int g = frameRGB >> 8 & 255;
        int b = frameRGB & 255;

        fillFrameWithColor(left, top, right, bottom, thickness, r, g, b, a);
    }

    @Deprecated
    public static void fillFrameWithWhite(float left, float top, float right, float bottom, float thickness, float brightness, float frameAlpha) {

        int a = (int) (frameAlpha * 255F);
        int r = (int) (brightness * 255F);

        fillFrameWithColor(left, top, right, bottom, thickness, r, r, r, a);
    }

    @Deprecated
    public static void fillFrameWithColor(float left, float top, float right, float bottom, float thickness, int r, int g, int b, int a) {

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left - thickness, top, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top - thickness, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left - thickness, top - thickness, z).color(r, g, b, a).endVertex();
        tessellator.draw();

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(right, bottom, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right + thickness, top - thickness, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right, top - thickness, z).color(r, g, b, a).endVertex();
        tessellator.draw();

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right + thickness, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, bottom, z).color(r, g, b, a).endVertex();
        tessellator.draw();

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left - thickness, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left, top, z).color(r, g, b, a).endVertex();
        bufferbuilder.pos(left - thickness, top, z).color(r, g, b, a).endVertex();
        tessellator.draw();

        RenderSystem.enableTexture();
    }

    public static void fillGradient(float x, float y, float width, float height, int startColor, int endColor, float zLevel) {
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
}
