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

package icyllis.modernui.graphics;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import icyllis.modernui.ModernUI;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.math.Icon;
import icyllis.modernui.graphics.math.TextAlign;
import icyllis.modernui.graphics.shader.ShaderProgram;
import icyllis.modernui.graphics.shader.program.CircleProgram;
import icyllis.modernui.graphics.shader.program.FeatheredRectProgram;
import icyllis.modernui.graphics.shader.program.RingProgram;
import icyllis.modernui.graphics.shader.program.RoundRectProgram;
import icyllis.modernui.graphics.textmc.TextLayoutProcessor;
import icyllis.modernui.graphics.textmc.pipeline.TextRenderNode;
import icyllis.modernui.view.UIManager;
import icyllis.modernui.view.View;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;

import javax.annotation.Nonnull;

/**
 * A canvas is used to draw things for View, likes rectangles,
 * rounded rectangles, circles, arcs, lines, points, images etc.
 * <p>
 * The canvas actually uses shaders (hardware-accelerated)
 * to render in real-time, so there's no need to control redrawing.
 * Also avoided RenderType being used in GUI, for better performance
 * (reduces GL callings, because render states changed little)
 * <p>
 * The font renderer uses another system, which has two parts, one for Modern UI, and
 * the global one is using RenderType, make Modern UI font renderer work everywhere,
 * because it's not always called in GUI, likes screens of other mods, TileEntityRenderer
 * or in world rendering, that also need matrix transformation to be compatible with vanilla
 *
 * @author BloCamLimb
 */
@SuppressWarnings("unused")
//TODO New render system (LOWEST PRIORITY)
public class Canvas {

    private static Canvas instance;

    /**
     * Instances
     */
    private final Window mWindow;
    private final ItemRenderer mItemRenderer;

    private final TextLayoutProcessor fontEngine = TextLayoutProcessor.getInstance();

    private final BufferBuilder mBufferBuilder = Tesselator.getInstance().getBuilder();


    /**
     * Shaders instance
     */
    private final RingProgram mRing = RingProgram.INSTANCE;
    private final CircleProgram mCircle = CircleProgram.INSTANCE;
    private final FeatheredRectProgram mFeatheredRect = FeatheredRectProgram.INSTANCE;


    /**
     * Paint colors, unsigned int
     */
    private int r = 255;
    private int g = 255;
    private int b = 255;
    private int a = 255;


    /**
     * Depth
     */
    @Deprecated
    private double z = 0.0D;

    /*
     * Drawing location offset, view or drawable
     */
    /*private int drawingX = 0;
    private int drawingY = 0;*/

    /**
     * Elapsed time from a gui open
     */
    @Deprecated
    private long drawingTime = 0;


    /**
     * Text align
     */
    @Deprecated
    private float alignFactor = TextAlign.LEFT.offsetFactor;


    /**
     * GL states
     */
    private static boolean lineAA = false;


    private Canvas(@Nonnull Minecraft minecraft) {
        mWindow = minecraft.getWindow();
        mItemRenderer = minecraft.getItemRenderer();
    }

    /**
     * This will start the render engine of Modern UI. Always do not call this
     * at the wrong time.
     *
     * @return the instance
     * @see UIManager#initialize()
     */
    public static Canvas getInstance() {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        if (instance == null) {
            instance = new Canvas(Minecraft.getInstance());
            ModernUI.LOGGER.debug(RenderCore.MARKER, "Canvas prepared");
        }
        return instance;
    }

    /**
     * Set current paint color with alpha.
     *
     * @param r red component [0, 255]
     * @param g green component [0, 255]
     * @param b blue component [0, 255]
     * @param a alpha component [0, 255]
     */
    public void setRGBA(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    /**
     * Set current paint color, keep previous alpha.
     *
     * @param r red component [0, 255]
     * @param g green component [0, 255]
     * @param b blue component [0, 255]
     */
    public void setRGB(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /**
     * Set current paint color in 0xAARRGGBB format.
     *
     * @param color the color to set
     */
    public void setColor(int color) {
        a = color >> 24 & 0xff;
        r = color >> 16 & 0xff;
        g = color >> 8 & 0xff;
        b = color & 0xff;
    }

    /**
     * Set current paint alpha in integer form
     *
     * @param a alpha [0,255]
     */
    public void setAlpha(int a) {
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
    @Deprecated
    public long getDrawingTime() {
        return drawingTime;
    }

    // inner use
    @Deprecated
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
     * Set current text alignment for next drawing
     *
     * @param align the align to set
     * @see #drawText(String, float, float)
     */
    @Deprecated
    public void setTextAlign(@Nonnull TextAlign align) {
        alignFactor = align.offsetFactor;
    }

    /**
     * Layout and draw a single line of text on screen, {@link ChatFormatting}
     * and bidirectional algorithm are supported, returns the text width.
     * <p>
     * It's recommended to use this when you draw a fast changing number,
     * all digits are laid-out with the same width as '0', because we don't know
     * whether it is static layout or dynamic layout, so we don't want to
     * re-layout when the numbers are changing too fast as it's performance hungry.
     * <p>
     * This method is convenient to use at any time but inflexible, such as,
     * you can't do cross line text layout, auto translatable text, formatting with
     * multiple arguments, hyperlinks, hover tooltips, or add various styles (such as
     * custom color, font size, etc) to different parts of the text. Because of
     * localization, the length of each part of the text is uncertain.
     * <p>
     * To achieve these things, use a {@link icyllis.modernui.widget.TextView}, we
     * also support markdown syntax using commonmark specification.
     *
     * @param text the text to draw
     * @param x    the x-coordinate of origin for where to draw the text
     * @param y    the y-coordinate of origin for where to draw the text
     * @return the total advance of the text (text line width)
     * @see #setTextAlign(TextAlign)
     */
    @Deprecated
    public float drawText(String text, float x, float y) {
        if (text == null || text.isEmpty())
            return 0;
        final TextRenderNode node = fontEngine.lookupVanillaNode(text, Style.EMPTY);
        if (alignFactor > 0)
            x -= node.advance * alignFactor;
        return node.drawText(mBufferBuilder, text, x, y, r, g, b, a);
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

        /*left += drawingX;
        top += drawingY;
        right += drawingX;
        bottom += drawingY;*/

        mBufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(left, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);
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

        /*left += drawingX;
        top += drawingY;
        right += drawingX;
        bottom += drawingY;*/

        /*ShaderTools.useShader(featheredRect);
        featheredRect.setThickness(0.25f);

        featheredRect.setInnerRect(left - thickness + 0.25f, top - thickness + 0.25f, right - 0.25f, top - 0.25f);*/

        final int r = this.r;
        final int g = this.g;
        final int b = this.b;
        final int a = this.a;
        final double z = this.z;

        mBufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(left - thickness, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, top - thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left - thickness, top - thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);

        //featheredRect.setInnerRect(right + 0.25f, top - thickness + 0.25f, right + thickness - 0.25f, bottom - 0.25f);

        mBufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(right, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right + thickness, top - thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, top - thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);

        //featheredRect.setInnerRect(left + 0.25f, bottom + 0.25f, right + thickness - 0.25f, bottom + thickness - 0.25f);

        mBufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(left, bottom + thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right + thickness, bottom + thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);

        //featheredRect.setInnerRect(left - thickness + 0.25f, top + 0.25f, left - 0.25f, bottom + thickness - 0.25f);

        mBufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(left - thickness, bottom + thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left, bottom + thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left - thickness, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);

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

        /*left += drawingX;
        top += drawingY;
        right += drawingX;
        bottom += drawingY;*/

        final int r = this.r;
        final int g = this.g;
        final int b = this.b;
        final int a = this.a;
        final double z = this.z;

        mBufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(left, bottom - bevel, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left + bevel, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right - bevel, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, bottom - bevel, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, top + bevel, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right - bevel, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left + bevel, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left, top + bevel, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);
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

        /*left += drawingX;
        top += drawingY;
        right += drawingX;
        bottom += drawingY;*/

        mBufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(left, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);
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
        mRing.use();
        mRing.setRadius(innerRadius, outerRadius);
        mRing.setCenter(centerX, centerY);
        drawRect(centerX - outerRadius, centerY - outerRadius, centerX + outerRadius, centerY + outerRadius);
        ShaderProgram.stop();
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
        mCircle.use();
        mCircle.setRadius(radius);
        mCircle.setCenter(centerX, centerY);
        drawRect(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        ShaderProgram.stop();
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

        /*startX += drawingX;
        stopX += drawingX;
        startY += drawingY;
        stopY += drawingY;*/

        mBufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(startX, startY, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(stopX, stopY, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);
    }

    /**
     * Draw a round rectangle with given rectangular bounds and round radius.
     *
     * @param left   the left of the rectangular bounds
     * @param top    the top of the rectangular bounds
     * @param right  the right of the rectangular bounds
     * @param bottom the bottom of the rectangular bounds
     * @param radius the round corner radius
     * @param paint  the paint used to draw the round rectangle
     */
    public void drawRoundRect(float left, float top, float right, float bottom,
                              float radius, @Nonnull Paint paint) {
        switch (paint.getStyle()) {
            case FILL:
                fillRoundRect(left, top, right, bottom, radius, paint);
                return;
            case STROKE:
                strokeRoundRect(left, top, right, bottom, radius, paint);
                return;
        }
        fillRoundRect(left, top, right, bottom, radius, paint);
        strokeRoundRect(left, top, right, bottom, radius, paint);
    }

    protected void fillRoundRect(float left, float top, float right, float bottom,
                                 float r, @Nonnull Paint paint) {
        final RoundRectProgram.Fill program = RoundRectProgram.fill();
        program.use();
        r = Math.max(0, r);
        program.setRadius(r, Math.min(r, paint.getFeatherRadius()));
        program.setInnerRect(left + r, top + r, right - r, bottom - r);
        upload(program, left, top, right, bottom, paint.getColor());
        ShaderProgram.stop();
    }

    protected void strokeRoundRect(float left, float top, float right, float bottom,
                                   float r, @Nonnull Paint paint) {
        final RoundRectProgram.Stroke program = RoundRectProgram.stroke();
        program.use();
        r = Math.max(0, r);
        float thickness = Math.min(paint.getStrokeWidth() * 0.5f, r);
        program.setRadius(r, Math.min(thickness, paint.getFeatherRadius()), thickness);
        program.setInnerRect(left + r, top + r, right - r, bottom - r);
        upload(program, left - r, top - r, right + r, bottom + r, paint.getColor());
        ShaderProgram.stop();
    }

    @Deprecated
    protected void upload(@Nonnull RoundRectProgram program, float left, float top,
                          float right, float bottom, int color) {
        final BufferBuilder builder = Tesselator.getInstance().getBuilder();

        final int a = color >>> 24;
        final int r = (color >> 16) & 0xff;
        final int g = (color >> 8) & 0xff;
        final int b = color & 0xff;

        builder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(left, bottom, 0).color(r, g, b, a).endVertex();
        builder.vertex(right, bottom, 0).color(r, g, b, a).endVertex();
        builder.vertex(right, top, 0).color(r, g, b, a).endVertex();
        builder.vertex(left, top, 0).color(r, g, b, a).endVertex();
        builder.end();
        BufferUploader.end(builder);
    }

    @Deprecated
    public void drawRoundedFrameT1(float left, float top, float right, float bottom, float radius) {
        RoundRectProgram.Stroke program = RoundRectProgram.stroke();
        program.use();
        program.setRadius(radius, 1.0f, 1.0f);
        program.setInnerRect(left + radius, top + radius, right - radius, bottom - radius);
        RenderSystem.disableTexture();
        mBufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(left, bottom, z).color(170, 220, 240, a).endVertex();
        mBufferBuilder.vertex(right, bottom, z).color(201, 200, 232, a).endVertex();
        mBufferBuilder.vertex(right, top, z).color(232, 180, 223, a).endVertex();
        mBufferBuilder.vertex(left, top, z).color(201, 200, 232, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);
        ShaderProgram.stop();
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
     * @param thickness feather thickness (&lt;= 0.5 is better)
     */
    public void drawFeatheredRect(float left, float top, float right, float bottom, float thickness) {
        mFeatheredRect.use();
        mFeatheredRect.setThickness(thickness);
        mFeatheredRect.setInnerRect(left + thickness, top + thickness, right - thickness, bottom - thickness);
        drawRect(left, top, right, bottom);
        ShaderProgram.stop();
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
    public void drawIcon(@Nonnull Icon icon, float left, float top, float right, float bottom, float radius) {
        RoundRectProgram.FillTex program = RoundRectProgram.fillTex();
        program.use();
        program.setRadius(radius, 1.0f);
        program.setInnerRect(left + radius, top + radius, right - radius, bottom - radius);
        RenderSystem.activeTexture(GL43.GL_TEXTURE0);
        icon.bindTexture();
        GL43.glUniform1i(2, 0);

        final BufferBuilder builder = Tesselator.getInstance().getBuilder();

        builder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        builder.vertex(left, bottom, z).color(r, g, b, a).uv(icon.getLeft(), icon.getBottom()).endVertex();
        builder.vertex(right, bottom, z).color(r, g, b, a).uv(icon.getRight(), icon.getBottom()).endVertex();
        builder.vertex(right, top, z).color(r, g, b, a).uv(icon.getRight(), icon.getTop()).endVertex();
        builder.vertex(left, top, z).color(r, g, b, a).uv(icon.getLeft(), icon.getTop()).endVertex();
        builder.end();
        BufferUploader.end(builder);

        ShaderProgram.stop();
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
        mItemRenderer.renderGuiItem(item.getDefaultInstance(), (int) (x), (int) (y));
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
        mItemRenderer.renderGuiItem(stack, (int) (x), (int) (y));
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
        mItemRenderer.renderGuiItem(stack, (int) (x), (int) (y));
        mItemRenderer.renderGuiItemDecorations(Minecraft.getInstance().font, stack, (int) (x), (int) (y));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * At most cases, you've to call this
     * in view's onDraw() method
     *
     * @param view view to move
     */
    @Deprecated
    public void moveTo(@Nonnull View view) {
        /*drawingX = view.getLeft();
        drawingY = view.getTop();*/
    }

    /**
     * At most cases, you've to call this
     * in drawable's draw() method
     *
     * @param drawable drawable to move
     */
    @Deprecated
    public void moveTo(@Nonnull Drawable drawable) {
        /*drawingX = drawable.getLeft();
        drawingY = drawable.getTop();*/
    }

    @Deprecated
    public void moveToZero() {
        /*drawingX = 0;
        drawingY = 0;*/
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
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(0, mWindow.getHeight() - view.getBottom(),
                mWindow.getWidth(), view.getHeight());
    }

    public void clipStart(float x, float y, float width, float height) {
        double scale = mWindow.getGuiScale();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * scale), (int) (mWindow.getHeight() - ((y + height) * scale)),
                (int) (width * scale), (int) (height * scale));
    }

    public void clipEnd() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
