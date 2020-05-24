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
import icyllis.modernui.graphics.font.IFontRenderer;
import icyllis.modernui.graphics.font.ModernFontRenderer;
import icyllis.modernui.graphics.font.TrueTypeRenderer;
import icyllis.modernui.graphics.shader.ShaderTools;
import icyllis.modernui.gui.math.Color3i;
import icyllis.modernui.graphics.math.TextAlign;
import icyllis.modernui.gui.shader.*;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

/**
 * Use paint brush and drawing board to draw things in or especially for ModernUI's GUI:
 * likes rect, rounded rect, circle, ring, line, point
 * textured icon, etc.
 * This avoided RenderType being used in gui, for better performance
 * (reduces GL callings, because render states changed little)
 * <p>
 * The font renderer uses another system, which has two parts, one for ModernUI's GUI, and
 * the global one is using RenderType, make ModernUI's font renderer work everywhere,
 * because it's not always called in gui, likes non-ModernUI GUI, TileEntityRenderer
 * or in world renderer, that also need matrix transformation to be compatible with vanilla
 * <p>
 * {@link net.minecraft.client.renderer.RenderType}
 * {@link icyllis.modernui.graphics.renderer.ModernTextRenderType}
 * {@link icyllis.modernui.graphics.font.TrueTypeRenderer}
 */
@SuppressWarnings("unused")
//TODO use custom MVP matrix loaded to shader
// use int RGBA color rather than float
public class Canvas {

    /**
     * Instances
     */
    private final MainWindow mainWindow = Minecraft.getInstance().getMainWindow();

    private final Tessellator tessellator = Tessellator.getInstance();

    private final BufferBuilder bufferBuilder = tessellator.getBuffer();

    private final IFontRenderer fontRenderer = TrueTypeRenderer.INSTANCE;

    private final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();


    /**
     * Shaders instance
     */
    private final RingShader ring = RingShader.INSTANCE;

    private final RoundedRectShader roundedRect = RoundedRectShader.INSTANCE;

    private final RoundedRectFrameShader roundedRectFrame = RoundedRectFrameShader.INSTANCE;

    private final CircleShader circle = CircleShader.INSTANCE;

    private final FeatheredRectShader featheredRect = FeatheredRectShader.INSTANCE;


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


    public Canvas() {

    }

    /**
     * Set current paint color
     *
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
     *
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
     *
     * @param a alpha [0,1]
     */
    public void setAlpha(float a) {
        this.a = a;
    }

    public void setColor(@Nonnull Color3i color) {
        setRGB(color.getFloatRed(), color.getFloatGreen(), color.getFloatBlue());
    }

    public void setColor(@Nonnull Color3i color, float a) {
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
     *
     * @param aa anti-aliasing
     */
    public void setLineAntiAliasing(boolean aa) {
        setLineAA0(aa);
    }

    /**
     * Set line width for lines drawing
     *
     * @param width width, default is 1.0f (not affected by gui scale)
     */
    public void setLineWidth(float width) {
        RenderSystem.lineWidth(width);
    }

    protected static void setLineAA0(boolean aa) {
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

    /**
     * Set text align type
     *
     * @param align align
     */
    public void setTextAlign(TextAlign align) {
        this.textAlign = align;
    }

    /**
     * Draw text on screen, text formatting and bidi are supported
     *
     * @param text formatted string
     * @param x    x pos
     * @param y    y pos
     * @return text advance (text width)
     */
    public float drawText(String text, float x, float y) {
        return fontRenderer.drawString(text, x, y, r, g, b, a, textAlign);
    }

    /**
     * Draw a rectangle on screen with given rect area
     *
     * @param left   rect left
     * @param top    rect top
     * @param right  rect right
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
     * @param left      rect left
     * @param top       rect top
     * @param right     rect right
     * @param bottom    rect bottom
     * @param thickness thickness, must be integral multiple of 1.0
     */
    public void drawRectOutline(float left, float top, float right, float bottom, float thickness) {
        RenderSystem.disableTexture();

        /*ShaderTools.useShader(featheredRect);
        featheredRect.setThickness(0.25f);

        featheredRect.setInnerRect(left - thickness + 0.25f, top - thickness + 0.25f, right - 0.25f, top - 0.25f);*/

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(left - thickness, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, top - thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left - thickness, top - thickness, z).color(r, g, b, a).endVertex();
        tessellator.draw();

        //featheredRect.setInnerRect(right + 0.25f, top - thickness + 0.25f, right + thickness - 0.25f, bottom - 0.25f);

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(right, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right + thickness, top - thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, top - thickness, z).color(r, g, b, a).endVertex();
        tessellator.draw();

        //featheredRect.setInnerRect(left + 0.25f, bottom + 0.25f, right + thickness - 0.25f, bottom + thickness - 0.25f);

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(left, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right + thickness, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, bottom, z).color(r, g, b, a).endVertex();
        tessellator.draw();

        //featheredRect.setInnerRect(left - thickness + 0.25f, top + 0.25f, left - 0.25f, bottom + thickness - 0.25f);

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(left - thickness, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left - thickness, top, z).color(r, g, b, a).endVertex();
        tessellator.draw();

        //ShaderTools.releaseShader();
    }

    /**
     * Draw a rect frame with bevel angle
     *
     * @param left   rect left
     * @param top    rect top
     * @param right  rect right
     * @param bottom rect bottom
     * @param bevel  bevel length
     */
    public void drawOctagonRectFrame(float left, float top, float right, float bottom, float bevel) {
        RenderSystem.disableTexture();
        bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(left, bottom - bevel, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left + bevel, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right - bevel, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, bottom - bevel, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, top + bevel, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right - bevel, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left + bevel, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, top + bevel, z).color(r, g, b, a).endVertex();
        tessellator.draw();
    }

    /**
     * Draw four lines around a closed rect area, anti-aliasing is needed
     * Otherwise, there's a missing pixel
     *
     * @param left   rect left
     * @param top    rect top
     * @param right  rect right
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
     * <p>
     * Default feather radius: 1 px
     *
     * @param centerX     center x pos
     * @param centerY     center y pos
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
     * <p>
     * Default feather radius: 1 px
     *
     * @param centerX center x pos
     * @param centerY center y pos
     * @param radius  circle radius
     */
    public void drawCircle(float centerX, float centerY, float radius) {
        ShaderTools.useShader(circle);
        circle.setRadius(radius);
        circle.setCenterPos(centerX, centerY);
        drawRect(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        ShaderTools.releaseShader();
    }

    /**
     * Draw a line with given two pos
     *
     * @param startX x1
     * @param startY y1
     * @param stopX  x2
     * @param stopY  y2
     */
    public void drawLine(float startX, float startY, float stopX, float stopY) {
        RenderSystem.disableTexture();
        bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(startX, startY, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(stopX, stopY, z).color(r, g, b, a).endVertex();
        tessellator.draw();
    }

    /**
     * Draw rounded rectangle on screen with given rect area and rounded radius
     * <p>
     * Default feather radius: 1 px
     *
     * @param left   rect left
     * @param top    rect top
     * @param right  rect right
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
     * <p>
     * Default feather radius: 1 px
     * Default frame thickness: 1.5 px
     *
     * @param left   rect left
     * @param top    rect top
     * @param right  rect right
     * @param bottom rect bottom
     * @param radius rounded radius, must >= 1.5
     */
    public void drawRoundedRectFrame(float left, float top, float right, float bottom, float radius) {
        ShaderTools.useShader(roundedRectFrame);
        roundedRect.setRadius(radius - 1);
        roundedRect.setInnerRect(left + radius, top + radius, right - radius, bottom - radius);
        drawRect(left, top, right, bottom);
        ShaderTools.releaseShader();
    }

    /**
     * Draw feathered rectangle frame in a rounded rect on screen
     * with given rect area and feather thickness (not radius)
     * A replacement for rounded rect when radius is too small.
     *
     * @param left      rect left
     * @param top       rect top
     * @param right     rect right
     * @param bottom    rect bottom
     * @param thickness feather thickness (>= 0.5 is better)
     */
    public void drawFeatheredRect(float left, float top, float right, float bottom, float thickness) {
        ShaderTools.useShader(featheredRect);
        featheredRect.setThickness(thickness);
        featheredRect.setInnerRect(left + thickness, top + thickness, right - thickness, bottom - thickness);
        drawRect(left, top, right, bottom);
        ShaderTools.releaseShader();
    }

    /**
     * Draw icon on screen fitting to given rect area
     *
     * @param icon   icon
     * @param left   rect left
     * @param top    rect top
     * @param right  rect right
     * @param bottom rect bottom
     */
    public void drawIcon(@Nonnull Icon icon, float left, float top, float right, float bottom) {
        RenderSystem.enableTexture();
        icon.bindTexture();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
        bufferBuilder.pos(left, bottom, z).color(r, g, b, a).tex(icon.getLeft(), icon.getBottom()).endVertex();
        bufferBuilder.pos(right, bottom, z).color(r, g, b, a).tex(icon.getRight(), icon.getBottom()).endVertex();
        bufferBuilder.pos(right, top, z).color(r, g, b, a).tex(icon.getRight(), icon.getTop()).endVertex();
        bufferBuilder.pos(left, top, z).color(r, g, b, a).tex(icon.getLeft(), icon.getTop()).endVertex();
        tessellator.draw();
    }

    /**
     * Draw item default instance, without any NBT data
     * Size: 16 * 16 (* GuiScale)
     *
     * @param item item
     * @param x    x pos
     * @param y    y pos
     */
    public void drawItem(@Nonnull Item item, float x, float y) {
        itemRenderer.renderItemIntoGUI(item.getDefaultInstance(), (int) x, (int) y);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
    }

    /**
     * Draw item stack with NBT
     *
     * @param stack item stack
     * @param x     x pos
     * @param y     y pos
     */
    public void drawItemStack(@Nonnull ItemStack stack, float x, float y) {
        itemRenderer.renderItemAndEffectIntoGUI(stack, (int) x, (int) y);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
    }

    /**
     * Draw item stack with NBT and their damage, amount
     *
     * @param stack item stack
     * @param x     x pos
     * @param y     y pos
     */
    public void drawItemStackWithOverlays(@Nonnull ItemStack stack, float x, float y) {
        itemRenderer.renderItemAndEffectIntoGUI(stack, (int) x, (int) y);
        itemRenderer.renderItemOverlays(ModernFontRenderer.INSTANCE, stack, (int) x, (int) y);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
    }

    public void save() {
        RenderSystem.pushMatrix();
    }

    public void restore() {
        RenderSystem.popMatrix();
    }

    public void translate(float dx, float dy) {
        RenderSystem.translatef(dx, dy, 0.0f);
    }

    public void scale(float sx, float sy) {
        RenderSystem.scalef(sx, sy, 1.0f);
    }

    /**
     * Scale the canvas and translate to pos
     *
     * @param sx x scale
     * @param sy y scale
     * @param px pivot x pos
     * @param py pivot y pos
     */
    public void scale(float sx, float sy, float px, float py) {
        scale(sx, sy);
        float kx;
        float ky;
        if (sx < 1) {
            kx = 1.0f / sx - 1.0f;
        } else {
            kx = sx - 1.0f;
        }
        kx *= px;
        if (sy < 1) {
            ky = 1.0f / sy - 1.0f;
        } else {
            ky = sy - 1.0f;
        }
        ky *= py;
        translate(kx, ky);
    }

    public void clipStart(float x, float y, float width, float height) {
        double scale = mainWindow.getGuiScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * scale), (int) (mainWindow.getFramebufferHeight() - ((y + height) * scale)),
                (int) (width * scale), (int) (height * scale));
    }

    public void clipEnd() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
