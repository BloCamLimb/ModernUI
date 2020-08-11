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

package icyllis.modernui.graphics.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.font.ModernFontRenderer;
import icyllis.modernui.font.TrueTypeRenderer;
import icyllis.modernui.font.node.TextRenderType;
import icyllis.modernui.font.text.TextAlign;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.math.Color3i;
import icyllis.modernui.graphics.shader.program.*;
import icyllis.modernui.ui.master.View;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

/**
 * Use paint to draw things in or especially for Modern UI's UI:
 * likes rect, rounded rect, circle, ring, line, point
 * textured icon, etc.
 * This avoided RenderType being used in gui, for better performance
 * (reduces GL callings, because render states changed little)
 * <p>
 * The font renderer uses another system, which has two parts, one for ModernUI's UI, and
 * the global one is using RenderType, make ModernUI's font renderer work everywhere,
 * because it's not always called in gui, likes non-ModernUI GUI, TileEntityRenderer
 * or in world renderer, that also need matrix transformation to be compatible with vanilla
 * <p>
 * You shouldn't create instances, the canvas will be given in draw method
 *
 * @see net.minecraft.client.renderer.RenderType
 * @see TextRenderType
 * @see TrueTypeRenderer
 */
@SuppressWarnings("unused")
//TODO New render system
public class Canvas {

    private static Canvas instance;

    /**
     * Instances
     */
    private final MainWindow   mainWindow;
    private final ItemRenderer itemRenderer;

    private final TrueTypeRenderer fontRenderer = TrueTypeRenderer.getInstance();

    private final BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();


    /**
     * Shaders instance
     */
    private final RingShader          ring          = RingShader.INSTANCE;
    private final RoundedRectShader   roundedRect   = RoundedRectShader.INSTANCE;
    private final RoundedFrameShader  roundedFrame  = RoundedFrameShader.INSTANCE;
    private final CircleShader        circle        = CircleShader.INSTANCE;
    private final FeatheredRectShader featheredRect = FeatheredRectShader.INSTANCE;


    /**
     * Paint colors
     */
    private int r = 255;
    private int g = 255;
    private int b = 255;
    private int a = 255;


    /**
     * Depth
     */
    private double z = 0.0D;

    /**
     * Drawing location offset, view or drawable
     */
    private int drawingX = 0;
    private int drawingY = 0;

    /**
     * Elapsed time from a gui open
     */
    private long drawingTime = 0;


    /**
     * Text align
     */
    private TextAlign textAlign = TextAlign.LEFT;


    /**
     * GL states
     */
    private static boolean lineAA = false;


    private Canvas(@Nonnull Minecraft minecraft) {
        mainWindow = minecraft.getMainWindow();
        itemRenderer = minecraft.getItemRenderer();
    }

    public static Canvas getInstance() {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        if (instance == null) {
            RenderTools.checkCapabilities();
            instance = new Canvas(Minecraft.getInstance());
        }
        return instance;
    }

    /**
     * Set current paint color with alpha
     *
     * @param r red [0,255]
     * @param g green [0,255]
     * @param b blue [0,255]
     * @param a alpha [0,255]
     */
    public void setColor(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    /**
     * Set current paint color, keep previous alpha
     *
     * @param r red [0,1]
     * @param g green [0,1]
     * @param b blue [0,1]
     */
    public void setColor(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /**
     * Set current paint color with alpha
     *
     * @param argb like 0x80404040 (=R64,G64,B64,A128)
     */
    public void setARGB(int argb) {
        a = argb >> 24 & 0xff;
        r = argb >> 16 & 0xff;
        g = argb >> 8 & 0xff;
        b = argb & 0xff;
    }

    /**
     * Set current paint color, keep previous alpha
     *
     * @param rgb like 0x404040 (=R64,G64,B64)
     */
    public void setRGB(int rgb) {
        r = rgb >> 16 & 0xff;
        g = rgb >> 8 & 0xff;
        b = rgb & 0xff;
    }

    /**
     * Set current paint alpha in float form
     *
     * @param a alpha [0,1]
     */
    public void setAlpha(float a) {
        this.a = (int) (a * 255.0f);
    }

    /**
     * Set current paint alpha in integer form
     *
     * @param a alpha [0,255]
     */
    public void setAlpha(int a) {
        this.a = a;
    }

    @Deprecated
    public void setColor(@Nonnull Color3i color) {
        r = color.getRed();
        g = color.getGreen();
        b = color.getBlue();
    }

    @Deprecated
    public void setColor(@Nonnull Color3i color, int a) {
        r = color.getRed();
        g = color.getGreen();
        b = color.getBlue();
        this.a = a;
    }

    /**
     * Reset color to white color and completely opaque.
     */
    public void resetColor() {
        r = 255;
        g = 255;
        b = 255;
        a = 255;
    }

    /**
     * Get elapsed time in UI window, update every frame
     *
     * @return drawing time in milliseconds
     */
    public long getDrawingTime() {
        return drawingTime;
    }

    // inner use
    public void setDrawingTime(long drawingTime) {
        this.drawingTime = drawingTime;
    }

    /**
     * Enable or disable anti aliasing for lines
     *
     * @param aa anti-aliasing
     */
    public void setLineAntiAliasing(boolean aa) {
        if (aa) {
            if (!lineAA) {
                GL11.glEnable(GL11.GL_LINE_SMOOTH);
                GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
                lineAA = true;
            }
        } else if (lineAA) {
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            lineAA = false;
        }
    }

    /**
     * Set line width for lines drawing
     *
     * @param width width, default is 1.0f (not affected by gui scale)
     */
    public void setLineWidth(float width) {
        RenderSystem.lineWidth(width);
    }

    /**
     * Set z pos / level, determines the depth, higher value will draw at the top
     * Minimum value and default value are 0
     *
     * @param z target z
     */
    @Deprecated
    public void setZ(double z) {
        this.z = z;
    }

    /**
     * Set current text align, left, center or right
     *
     * @param align align
     */
    public void setTextAlign(TextAlign align) {
        textAlign = align;
    }

    /**
     * Draw text on screen, text formatting and bidi are supported.
     * This method returns the text width, or you can get width by
     * {@link icyllis.modernui.ui.master.UITools#getTextWidth(String)}
     *
     * @param text formatted string
     * @param x    x pos
     * @param y    y pos
     * @return text advance (text width)
     */
    public float drawText(String text, float x, float y) {
        return fontRenderer.drawFromCanvas(text, x + drawingX, y + drawingY, r, g, b, a, textAlign);
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

        left += drawingX;
        top += drawingY;
        right += drawingX;
        bottom += drawingY;

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(left, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferBuilder);
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

        left += drawingX;
        top += drawingY;
        right += drawingX;
        bottom += drawingY;

        /*ShaderTools.useShader(featheredRect);
        featheredRect.setThickness(0.25f);

        featheredRect.setInnerRect(left - thickness + 0.25f, top - thickness + 0.25f, right - 0.25f, top - 0.25f);*/

        final int r = this.r;
        final int g = this.g;
        final int b = this.b;
        final int a = this.a;
        final double z = this.z;

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(left - thickness, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, top - thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left - thickness, top - thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferBuilder);

        //featheredRect.setInnerRect(right + 0.25f, top - thickness + 0.25f, right + thickness - 0.25f, bottom - 0.25f);

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(right, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right + thickness, top - thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, top - thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferBuilder);

        //featheredRect.setInnerRect(left + 0.25f, bottom + 0.25f, right + thickness - 0.25f, bottom + thickness - 0.25f);

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(left, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right + thickness, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferBuilder);

        //featheredRect.setInnerRect(left - thickness + 0.25f, top + 0.25f, left - 0.25f, bottom + thickness - 0.25f);

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(left - thickness, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left - thickness, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferBuilder);

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

        left += drawingX;
        top += drawingY;
        right += drawingX;
        bottom += drawingY;

        final int r = this.r;
        final int g = this.g;
        final int b = this.b;
        final int a = this.a;
        final double z = this.z;

        bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(left, bottom - bevel, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left + bevel, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right - bevel, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, bottom - bevel, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, top + bevel, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right - bevel, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left + bevel, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, top + bevel, z).color(r, g, b, a).endVertex();
        bufferBuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferBuilder);
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

        left += drawingX;
        top += drawingY;
        right += drawingX;
        bottom += drawingY;

        bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(left, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(right, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(left, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferBuilder);
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
        RenderTools.useShader(ring);
        ring.setRadius(innerRadius, outerRadius);
        ring.setCenterPos(centerX, centerY);
        drawRect(centerX - outerRadius, centerY - outerRadius, centerX + outerRadius, centerY + outerRadius);
        RenderTools.releaseShader();
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
        RenderTools.useShader(circle);
        circle.setRadius(radius);
        circle.setCenterPos(centerX + drawingX, centerY + drawingY);
        drawRect(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        RenderTools.releaseShader();
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

        startX += drawingX;
        stopX += drawingX;
        startY += drawingY;
        stopY += drawingY;

        bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(startX, startY, z).color(r, g, b, a).endVertex();
        bufferBuilder.pos(stopX, stopY, z).color(r, g, b, a).endVertex();
        bufferBuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferBuilder);
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
        RenderTools.useShader(roundedRect);
        roundedRect.setRadius(radius - 1); // we have feather radius 1px
        roundedRect.setInnerRect(left + radius + drawingX, top + radius + drawingY, right - radius + drawingX, bottom - radius + drawingY);
        drawRect(left, top, right, bottom);
        RenderTools.releaseShader();
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
    public void drawRoundedFrame(float left, float top, float right, float bottom, float radius) {
        RenderTools.useShader(roundedFrame);
        roundedRect.setRadius(radius - 1);
        roundedRect.setInnerRect(left + radius, top + radius, right - radius, bottom - radius);
        drawRect(left, top, right, bottom);
        RenderTools.releaseShader();
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
        RenderTools.useShader(featheredRect);
        featheredRect.setThickness(thickness);
        featheredRect.setInnerRect(left + thickness, top + thickness, right - thickness, bottom - thickness);
        drawRect(left, top, right, bottom);
        RenderTools.releaseShader();
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

        left += drawingX;
        top += drawingY;
        right += drawingX;
        bottom += drawingY;

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
        bufferBuilder.pos(left, bottom, z).color(r, g, b, a).tex(icon.getLeft(), icon.getBottom()).endVertex();
        bufferBuilder.pos(right, bottom, z).color(r, g, b, a).tex(icon.getRight(), icon.getBottom()).endVertex();
        bufferBuilder.pos(right, top, z).color(r, g, b, a).tex(icon.getRight(), icon.getTop()).endVertex();
        bufferBuilder.pos(left, top, z).color(r, g, b, a).tex(icon.getLeft(), icon.getTop()).endVertex();
        bufferBuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferBuilder);
    }

    /**
     * Draw item default instance, without any NBT data
     * Size on screen: 16 * 16 * GuiScale
     *
     * @param item item
     * @param x    x pos
     * @param y    y pos
     */
    public void drawItem(@Nonnull Item item, float x, float y) {
        itemRenderer.renderItemIntoGUI(item.getDefaultInstance(), (int) (x + drawingX), (int) (y + drawingY));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * Draw item stack with NBT
     *
     * @param stack item stack to draw
     * @param x     x pos
     * @param y     y pos
     */
    public void drawItemStack(@Nonnull ItemStack stack, float x, float y) {
        itemRenderer.renderItemAndEffectIntoGUI(stack, (int) (x + drawingX), (int) (y + drawingY));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * Draw item stack with NBT and their damage bar, amount etc
     *
     * @param stack item stack to draw
     * @param x     x pos
     * @param y     y pos
     */
    public void drawItemStackWithOverlays(@Nonnull ItemStack stack, float x, float y) {
        itemRenderer.renderItemAndEffectIntoGUI(stack, (int) (x + drawingX), (int) (y + drawingY));
        // force to use ModernUI font renderer
        itemRenderer.renderItemOverlays(ModernFontRenderer.getInstance(), stack, (int) (x + drawingX), (int) (y + drawingY));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * At most cases, you've to call this
     * in view's onDraw() method
     *
     * @param view view to move
     */
    public void moveTo(@Nonnull View view) {
        drawingX = view.getLeft();
        drawingY = view.getTop();
    }

    /**
     * At most cases, you've to call this
     * in drawable's draw() method
     *
     * @param drawable drawable to move
     */
    public void moveTo(@Nonnull Drawable drawable) {
        drawingX = drawable.getLeft();
        drawingY = drawable.getTop();
    }

    public void moveToZero() {
        drawingX = 0;
        drawingY = 0;
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
        RenderSystem.scalef(sx, sy, 1.0f);
        float dx;
        float dy;
        if (sx < 1) {
            dx = 1.0f / sx - 1.0f;
        } else {
            dx = sx - 1.0f;
        }
        dx *= px;
        if (sy < 1) {
            dy = 1.0f / sy - 1.0f;
        } else {
            dy = sy - 1.0f;
        }
        dy *= py;
        RenderSystem.translatef(dx, dy, 0.0f);
    }

    public void clipVertical(@Nonnull View view) {
        int scale = (int) mainWindow.getGuiScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(0, mainWindow.getFramebufferHeight() - (view.getBottom() * scale),
                mainWindow.getFramebufferWidth(), view.getHeight() * scale);
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
