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
import icyllis.modernui.graphics.shader.program.ArcProgram;
import icyllis.modernui.graphics.shader.program.CircleProgram;
import icyllis.modernui.graphics.shader.program.RectProgram;
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
import org.lwjgl.opengl.GL43;

import javax.annotation.Nonnull;

/**
 * A canvas is used to draw contents for View, likes rectangles,
 * round rectangles, circles, arcs, lines, points, images etc.
 * <p>
 * The canvas is actually using shader programs (hardware-accelerated)
 * to render in real-time, so there's no need to control redrawing.
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
    private final Window mMainWindow;
    private final ItemRenderer mItemRenderer;

    private final TextLayoutProcessor mFontEngine = TextLayoutProcessor.getInstance();

    @Deprecated
    private final BufferBuilder mBufferBuilder = Tesselator.getInstance().getBuilder();


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
        mMainWindow = minecraft.getWindow();
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
    @Deprecated
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
    @Deprecated
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
                GL43.glEnable(GL43.GL_LINE_SMOOTH);
                GL43.glHint(GL43.GL_LINE_SMOOTH_HINT, GL43.GL_NICEST);
                lineAA = true;
            }
        } else if (lineAA) {
            GL43.glDisable(GL43.GL_LINE_SMOOTH);
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
        final TextRenderNode node = mFontEngine.lookupVanillaNode(text, Style.EMPTY);
        if (alignFactor > 0)
            x -= node.advance * alignFactor;
        return node.drawText(mBufferBuilder, text, x, y, r, g, b, a);
    }

    /**
     * <p>
     * Draw a circular arc.
     * </p>
     * <p>
     * If the start angle is negative or >= 360, the start angle is treated as start angle modulo
     * 360. If the sweep angle is >= 360, then the circle is drawn completely. If the sweep angle is
     * negative, the sweep angle is treated as sweep angle modulo 360 (e.g -30 to 330)
     * </p>
     * <p>
     * The arc is drawn clockwise. An angle of 0 degrees correspond to the geometric angle of 0
     * degrees (3 o'clock on a watch.)
     * </p>
     *
     * @param centerX    The x-coordinate of the center of the arc to be drawn
     * @param centerY    The y-coordinate of the center of the arc to be drawn
     * @param radius     The radius of the circular arc to be drawn
     * @param startAngle Starting angle (in degrees) where the arc begins
     * @param sweepAngle Sweep angle (in degrees) measured clockwise
     * @param paint      The paint used to draw the arc
     */
    public void drawArc(float centerX, float centerY, float radius, float startAngle,
                        float sweepAngle, @Nonnull Paint paint) {
        if (sweepAngle == 0)
            return;
        if (sweepAngle < 0)
            sweepAngle = (sweepAngle % 360) + 360;
        switch (paint.getStyle()) {
            case FILL:
                fillArc(centerX, centerY, radius, startAngle, sweepAngle, paint);
                return;
            case STROKE:
                strokeArc(centerX, centerY, radius, startAngle, sweepAngle, paint);
                return;
        }
        fillArc(centerX, centerY, radius, startAngle, sweepAngle, paint);
        strokeArc(centerX, centerY, radius, startAngle, sweepAngle, paint);
    }

    protected void fillArc(float cx, float cy, float radius, float startAngle,
                           float sweepAngle, @Nonnull Paint paint) {
        if (sweepAngle >= 360)
            fillCircle(cx, cy, radius, paint);
        else {
            final ArcProgram.Fill program = ArcProgram.fill();
            program.use();
            if (startAngle < 0 || startAngle >= 360) {
                startAngle %= 360;
                if (startAngle < 0)
                    startAngle += 360;
            }
            float middle = startAngle + sweepAngle * 0.5f;
            program.setCenter(cx, cy);
            program.setAngle(middle, sweepAngle);
            program.setRadius(radius, Math.min(radius, paint.getFeatherRadius()));
            upload(cx - radius, cy - radius, cx + radius, cy + radius, paint.getColor());
            ShaderProgram.stop();
        }
    }

    protected void strokeArc(float cx, float cy, float radius, float startAngle,
                             float sweepAngle, @Nonnull Paint paint) {
        if (sweepAngle >= 360)
            strokeCircle(cx, cy, radius, paint);
        else {
            final ArcProgram.Stroke program = ArcProgram.stroke();
            program.use();
            if (startAngle < 0 || startAngle >= 360) {
                startAngle %= 360;
                if (startAngle < 0)
                    startAngle += 360;
            }
            float middle = startAngle + sweepAngle * 0.5f;
            program.setCenter(cx, cy);
            program.setAngle(middle, sweepAngle);
            float thickness = Math.min(paint.getStrokeWidth() * 0.5f, radius);
            program.setRadius(radius, Math.min(thickness, paint.getFeatherRadius()), thickness);
            float outer = radius + thickness;
            upload(cx - outer, cy - outer, cx + outer, cy + outer, paint.getColor());
            ShaderProgram.stop();
        }
    }

    /**
     * Draw the specified Rect using the specified paint. The rectangle will be filled or framed
     * based on the Style in the paint.
     *
     * @param left   The left side of the rectangle to be drawn
     * @param top    The top side of the rectangle to be drawn
     * @param right  The right side of the rectangle to be drawn
     * @param bottom The bottom side of the rectangle to be drawn
     * @param paint  The paint used to draw the rect
     */
    public void drawRect(float left, float top, float right, float bottom, @Nonnull Paint paint) {
        switch (paint.getStyle()) {
            case FILL:
                fillRect(left, top, right, bottom, paint);
                return;
            case STROKE:
                return;
        }
        fillRect(left, top, right, bottom, paint);
    }

    protected void fillRect(float left, float top, float right, float bottom, @Nonnull Paint paint) {
        if (paint.getFeatherRadius() > 0) {
            final RectProgram.Feathered program = RectProgram.feathered();
            program.use();
            float t = Math.min(Math.min(right - left, right - bottom), paint.getFeatherRadius());
            program.setThickness(t);
            program.setInnerRect(left + t, top + t, right - t, bottom - t);
        } else {
            RectProgram.fill().use();
        }
        upload(left, top, right, bottom, paint.getColor());
        ShaderProgram.stop();
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

        mBufferBuilder.begin(GL43.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(left - thickness, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, top - thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left - thickness, top - thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);

        //featheredRect.setInnerRect(right + 0.25f, top - thickness + 0.25f, right + thickness - 0.25f, bottom - 0.25f);

        mBufferBuilder.begin(GL43.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(right, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right + thickness, top - thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, top - thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);

        //featheredRect.setInnerRect(left + 0.25f, bottom + 0.25f, right + thickness - 0.25f, bottom + thickness - 0.25f);

        mBufferBuilder.begin(GL43.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(left, bottom + thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right + thickness, bottom + thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);

        //featheredRect.setInnerRect(left - thickness + 0.25f, top + 0.25f, left - 0.25f, bottom + thickness - 0.25f);

        mBufferBuilder.begin(GL43.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
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

        mBufferBuilder.begin(GL43.GL_LINE_LOOP, DefaultVertexFormat.POSITION_COLOR);
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

        mBufferBuilder.begin(GL43.GL_LINE_LOOP, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(left, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);
    }

    /**
     * Draw the specified circle using the specified paint. If radius is <= 0, then nothing will be
     * drawn. The circle will be filled or framed based on the Style in the paint.
     *
     * @param centerX The x-coordinate of the center of the circle to be drawn
     * @param centerY The y-coordinate of the center of the circle to be drawn
     * @param radius  The radius of the circle to be drawn
     * @param paint   The paint used to draw the circle
     */
    public void drawCircle(float centerX, float centerY, float radius, @Nonnull Paint paint) {
        if (radius <= 0)
            return;
        switch (paint.getStyle()) {
            case FILL:
                fillCircle(centerX, centerY, radius, paint);
                return;
            case STROKE:
                strokeCircle(centerX, centerY, radius, paint);
                return;
        }
        fillCircle(centerX, centerY, radius, paint);
        strokeCircle(centerX, centerY, radius, paint);
    }

    protected void fillCircle(float cx, float cy, float r, @Nonnull Paint paint) {
        final CircleProgram.Fill program = CircleProgram.fill();
        program.use();
        program.setRadius(r, Math.min(r, paint.getFeatherRadius()));
        program.setCenter(cx, cy);
        upload(cx - r, cy - r, cx + r, cy + r, paint.getColor());
        ShaderProgram.stop();
    }

    protected void strokeCircle(float cx, float cy, float r, @Nonnull Paint paint) {
        final CircleProgram.Stroke program = CircleProgram.stroke();
        program.use();
        float thickness = Math.min(paint.getStrokeWidth() * 0.5f, r);
        float outer = r + thickness;
        program.setRadius(r - thickness, outer, Math.min(thickness, paint.getFeatherRadius()));
        program.setCenter(cx, cy);
        upload(cx - outer, cy - outer, cx + outer, cy + outer, paint.getColor());
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

        mBufferBuilder.begin(GL43.GL_LINES, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(startX, startY, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(stopX, stopY, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);
    }

    /**
     * Draw a rectangle with round corners within a rectangular bounds.
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
        radius = Math.max(0, radius);
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
        program.setRadius(r, Math.min(r, paint.getFeatherRadius()));
        program.setInnerRect(left + r, top + r, right - r, bottom - r);
        upload(left, top, right, bottom, paint.getColor());
        ShaderProgram.stop();
    }

    protected void strokeRoundRect(float left, float top, float right, float bottom,
                                   float r, @Nonnull Paint paint) {
        final RoundRectProgram.Stroke program = RoundRectProgram.stroke();
        program.use();
        float thickness = Math.min(paint.getStrokeWidth() * 0.5f, r);
        program.setRadius(r, Math.min(thickness, paint.getFeatherRadius()), thickness);
        program.setInnerRect(left + r, top + r, right - r, bottom - r);
        upload(left - r, top - r, right + r, bottom + r, paint.getColor());
        ShaderProgram.stop();
    }

    @Deprecated
    protected void upload(float left, float top, float right, float bottom, int color) {
        final BufferBuilder builder = Tesselator.getInstance().getBuilder();

        final int a = color >>> 24;
        final int r = (color >> 16) & 0xff;
        final int g = (color >> 8) & 0xff;
        final int b = color & 0xff;

        builder.begin(GL43.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
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
        mBufferBuilder.begin(GL43.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(left, bottom, z).color(170, 220, 240, a).endVertex();
        mBufferBuilder.vertex(right, bottom, z).color(201, 200, 232, a).endVertex();
        mBufferBuilder.vertex(right, top, z).color(232, 180, 223, a).endVertex();
        mBufferBuilder.vertex(left, top, z).color(201, 200, 232, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);
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
    public void drawRoundImage(@Nonnull Icon icon, float left, float top, float right, float bottom,
                               float radius, @Nonnull Paint paint) {
        RoundRectProgram.FillTex program = RoundRectProgram.fillTex();
        program.use();
        program.setRadius(radius, Math.min(radius, paint.getFeatherRadius()));
        program.setInnerRect(left + radius, top + radius, right - radius, bottom - radius);
        RenderSystem.activeTexture(GL43.GL_TEXTURE0);
        icon.bindTexture();
        GL43.glUniform1i(2, 0);

        final BufferBuilder builder = Tesselator.getInstance().getBuilder();

        final int color = paint.getColor();
        final int a = color >>> 24;
        final int r = (color >> 16) & 0xff;
        final int g = (color >> 8) & 0xff;
        final int b = color & 0xff;

        builder.begin(GL43.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        builder.vertex(left, bottom, 0).color(r, g, b, a).uv(icon.getLeft(), icon.getBottom()).endVertex();
        builder.vertex(right, bottom, 0).color(r, g, b, a).uv(icon.getRight(), icon.getBottom()).endVertex();
        builder.vertex(right, top, 0).color(r, g, b, a).uv(icon.getRight(), icon.getTop()).endVertex();
        builder.vertex(left, top, 0).color(r, g, b, a).uv(icon.getLeft(), icon.getTop()).endVertex();
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
        GL43.glEnable(GL43.GL_SCISSOR_TEST);
        GL43.glScissor(0, mMainWindow.getHeight() - view.getBottom(),
                mMainWindow.getWidth(), view.getHeight());
    }

    public void clipStart(float x, float y, float width, float height) {
        double scale = mMainWindow.getGuiScale();
        GL43.glEnable(GL43.GL_SCISSOR_TEST);
        GL43.glScissor((int) (x * scale), (int) (mMainWindow.getHeight() - ((y + height) * scale)),
                (int) (width * scale), (int) (height * scale));
    }

    public void clipEnd() {
        GL43.glDisable(GL43.GL_SCISSOR_TEST);
    }
}
