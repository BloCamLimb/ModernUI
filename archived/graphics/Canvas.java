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
import icyllis.modernui.graphics.math.Icon;
import icyllis.modernui.graphics.math.TextAlign;
import icyllis.modernui.graphics.shader.program.ArcProgram;
import icyllis.modernui.graphics.shader.program.CircleProgram;
import icyllis.modernui.graphics.shader.program.RectProgram;
import icyllis.modernui.graphics.shader.program.RoundRectProgram;
import icyllis.modernui.graphics.textmc.TextLayoutProcessor;
import icyllis.modernui.graphics.textmc.pipeline.TextRenderNode;
import icyllis.modernui.view.View;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL43;

import javax.annotation.Nullable;

/**
 * The font renderer uses another system, which has two parts, one for Modern UI, and
 * the global one is using RenderType, make Modern UI font renderer work everywhere,
 * because it's not always called in GUI, likes screens of other mods, TileEntityRenderer
 * or in world rendering, that also need matrix transformation to be compatible with vanilla
 */
@Deprecated
public class Canvas {

    /**
     * Set current paint color with alpha.
     *
     * @param r red component [0, 255]
     * @param g green component [0, 255]
     * @param b blue component [0, 255]
     * @param a alpha component [0, 255]
     */
    @Deprecated
    public void setRGBA(int r, int g, int b, int a) {
        throw new UnsupportedOperationException();
        /*this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;*/
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
        throw new UnsupportedOperationException();
        /*this.r = r;
        this.g = g;
        this.b = b;*/
    }

    /**
     * Set current paint color in 0xAARRGGBB format.
     *
     * @param color the color to set
     */
    @Deprecated
    public void setColor(int color) {
        throw new UnsupportedOperationException();
        /*a = color >> 24 & 0xff;
        r = color >> 16 & 0xff;
        g = color >> 8 & 0xff;
        b = color & 0xff;*/
    }

    /**
     * Set current paint alpha in integer form
     *
     * @param a alpha [0,255]
     */
    @Deprecated
    public void setAlpha(int a) {
        throw new UnsupportedOperationException();
        //this.a = a;
    }

    /**
     * Reset color to white color and completely opaque.
     */
    @Deprecated
    public void resetColor() {
        throw new UnsupportedOperationException();
        /*r = 255;
        g = 255;
        b = 255;
        a = 255;*/
    }

    /**
     * Get elapsed time in UI window, update every frame
     *
     * @return drawing time in milliseconds
     */
    @Deprecated
    public long getDrawingTime() {
        throw new UnsupportedOperationException();
        //return drawingTime;
    }

    // inner use
    @Deprecated
    public void setDrawingTime(long drawingTime) {
        throw new UnsupportedOperationException();
        //this.drawingTime = drawingTime;
    }

    /**
     * Enable or disable anti aliasing for lines
     *
     * @param aa anti-aliasing
     */
    @Deprecated
    public void setLineAntiAliasing(boolean aa) {
        throw new UnsupportedOperationException();
        /*if (aa) {
            if (!lineAA) {
                GL43.glEnable(GL43.GL_LINE_SMOOTH);
                GL43.glHint(GL43.GL_LINE_SMOOTH_HINT, GL43.GL_NICEST);
                lineAA = true;
            }
        } else if (lineAA) {
            GL43.glDisable(GL43.GL_LINE_SMOOTH);
            lineAA = false;
        }*/
    }

    /**
     * Set line width for lines drawing
     *
     * @param width width, default is 1.0f (not affected by gui scale)
     */
    @Deprecated
    public void setLineWidth(float width) {
        throw new UnsupportedOperationException();
        //RenderSystem.lineWidth(width);
    }

    /**
     * Set z pos / level, determines the depth, higher value will draw at the top
     * Minimum value and default value are 0
     *
     * @param z target z
     */
    @Deprecated
    public void setZ(double z) {
        throw new UnsupportedOperationException();
        //this.z = z;
    }

    /**
     * Set current text alignment for next drawing
     *
     * @param align the align to set
     * @see #drawText(String, float, float)
     */
    @Deprecated
    public void setTextAlign(@Nonnull TextAlign align) {
        throw new UnsupportedOperationException();
        //alignFactor = align.offsetFactor;
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
        final TextRenderNode node = mTextEngine.lookupVanillaNode(text, Style.EMPTY);
        /*if (alignFactor > 0)
            x -= node.advance * alignFactor;*/
        return node.drawText(Tesselator.getInstance().getBuilder(), text, x, y, 255, 255, 255, 255);
    }

    @Deprecated
    public void drawArc(float centerX, float centerY, float radius, float startAngle,
                        float sweepAngle, @Nonnull Paint paint) {
        if (sweepAngle == 0 || radius <= 0)
            return;
        if (sweepAngle < 0)
            sweepAngle = (sweepAngle % 360) + 360;
        switch (paint.getStyle()) {
            case FILL:
                fillArc(centerX, centerY, radius, startAngle, sweepAngle, paint);
                return;
            case FILL_AND_STROKE:
                fillArc(centerX, centerY, radius, startAngle, sweepAngle, paint);
            case STROKE:
                strokeArc(centerX, centerY, radius, startAngle, sweepAngle, paint);
        }
        throw new UnsupportedOperationException();
    }

    @Deprecated
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
            program.setRadius(radius, Math.min(radius, paint.getSmoothRadius()));
            upload(cx - radius, cy - radius, cx + radius, cy + radius, paint.getColor());
            GLWrapper.stopProgram();
        }
        throw new UnsupportedOperationException();
    }

    @Deprecated
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
            program.setRadius(radius, Math.min(thickness, paint.getSmoothRadius()), thickness);
            float outer = radius + thickness;
            upload(cx - outer, cy - outer, cx + outer, cy + outer, paint.getColor());
            GLWrapper.stopProgram();
        }
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public void drawRect(float left, float top, float right, float bottom, @Nonnull Paint paint) {
        if (left >= right || bottom <= top)
            return;
        switch (paint.getStyle()) {
            case FILL:
                fillRect(left, top, right, bottom, paint);
                return;
            case STROKE:
                return;
        }
        fillRect(left, top, right, bottom, paint);
        throw new UnsupportedOperationException();
    }

    @Deprecated
    protected void fillRect(float left, float top, float right, float bottom, @Nonnull Paint paint) {
        if (paint.getSmoothRadius() > 0) {
            final RectProgram.Feathered program = RectProgram.feathered();
            program.use();
            float t = Math.min(Math.min(right - left, right - bottom), paint.getSmoothRadius());
            program.setThickness(t);
            program.setInnerRect(left + t, top + t, right - t, bottom - t);
        } else {
            RectProgram.fill().use();
        }
        upload(left, top, right, bottom, paint.getColor());
        GLWrapper.stopProgram();
        throw new UnsupportedOperationException();
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
    @Deprecated
    public void drawRectOutline(float left, float top, float right, float bottom, float thickness) {
        throw new UnsupportedOperationException();
        //RenderSystem.disableTexture();

        /*left += drawingX;
        top += drawingY;
        right += drawingX;
        bottom += drawingY;*/

        /*ShaderTools.useShader(featheredRect);
        featheredRect.setThickness(0.25f);

        featheredRect.setInnerRect(left - thickness + 0.25f, top - thickness + 0.25f, right - 0.25f, top - 0.25f);*/

        /*final int r = this.r;
        final int g = this.g;
        final int b = this.b;
        final int a = this.a;
        final double z = this.z;*/

        /*mBufferBuilder.begin(GL43.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(left - thickness, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, top - thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left - thickness, top - thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);*/

        //featheredRect.setInnerRect(right + 0.25f, top - thickness + 0.25f, right + thickness - 0.25f, bottom - 0.25f);

        /*mBufferBuilder.begin(GL43.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(right, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right + thickness, top - thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, top - thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);*/

        //featheredRect.setInnerRect(left + 0.25f, bottom + 0.25f, right + thickness - 0.25f, bottom + thickness - 0.25f);

        /*mBufferBuilder.begin(GL43.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(left, bottom + thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right + thickness, bottom + thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);*/

        //featheredRect.setInnerRect(left - thickness + 0.25f, top + 0.25f, left - 0.25f, bottom + thickness - 0.25f);

        /*mBufferBuilder.begin(GL43.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(left - thickness, bottom + thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left, bottom + thickness, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left - thickness, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);*/

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
    @Deprecated
    public void drawOctagonRectFrame(float left, float top, float right, float bottom, float bevel) {
        throw new UnsupportedOperationException();
        //RenderSystem.disableTexture();

        /*left += drawingX;
        top += drawingY;
        right += drawingX;
        bottom += drawingY;*/

        /*final int r = this.r;
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
        BufferUploader.end(mBufferBuilder);*/
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
    @Deprecated
    public void drawRectLines(float left, float top, float right, float bottom) {
        throw new UnsupportedOperationException();
        //RenderSystem.disableTexture();

        /*left += drawingX;
        top += drawingY;
        right += drawingX;
        bottom += drawingY;*/

        /*mBufferBuilder.begin(GL43.GL_LINE_LOOP, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(left, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, bottom, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(right, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(left, top, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);*/
    }

    @Deprecated
    public void drawCircle(float centerX, float centerY, float radius, @Nonnull Paint paint) {
        if (radius <= 0)
            return;
        switch (paint.getStyle()) {
            case FILL:
                fillCircle(centerX, centerY, radius, paint);
                return;
            case FILL_AND_STROKE:
                fillCircle(centerX, centerY, radius, paint);
            case STROKE:
                strokeCircle(centerX, centerY, radius, paint);
        }
        throw new UnsupportedOperationException();
    }

    @Deprecated
    protected void fillCircle(float cx, float cy, float r, @Nonnull Paint paint) {
        final CircleProgram.Fill program = CircleProgram.fill();
        program.use();
        program.setRadius(r, Math.min(r, paint.getSmoothRadius()));
        program.setCenter(cx, cy);
        upload(cx - r, cy - r, cx + r, cy + r, paint.getColor());
        GLWrapper.stopProgram();
        throw new UnsupportedOperationException();
    }

    @Deprecated
    protected void strokeCircle(float cx, float cy, float r, @Nonnull Paint paint) {
        final CircleProgram.Stroke program = CircleProgram.stroke();
        program.use();
        float thickness = Math.min(paint.getStrokeWidth() * 0.5f, r);
        float outer = r + thickness;
        program.setRadius(r - thickness, outer, Math.min(thickness, paint.getSmoothRadius()));
        program.setCenter(cx, cy);
        upload(cx - outer, cy - outer, cx + outer, cy + outer, paint.getColor());
        GLWrapper.stopProgram();
        throw new UnsupportedOperationException();
    }

    /**
     * Draw a line with given two pos
     *
     * @param startX x1
     * @param startY y1
     * @param stopX  x2
     * @param stopY  y2
     */
    @Deprecated
    public void drawLine(float startX, float startY, float stopX, float stopY) {
        throw new UnsupportedOperationException();
        /*RenderSystem.disableTexture();*/

        /*startX += drawingX;
        stopX += drawingX;
        startY += drawingY;
        stopY += drawingY;*/

        /*mBufferBuilder.begin(GL43.GL_LINES, DefaultVertexFormat.POSITION_COLOR);
        mBufferBuilder.vertex(startX, startY, z).color(r, g, b, a).endVertex();
        mBufferBuilder.vertex(stopX, stopY, z).color(r, g, b, a).endVertex();
        mBufferBuilder.end();
        BufferUploader.end(mBufferBuilder);*/
    }

    @Deprecated
    public void drawRoundRect(float left, float top, float right, float bottom,
                              float radius, @Nonnull Paint paint) {
        radius = Math.max(0, radius);
        switch (paint.getStyle()) {
            case FILL:
                fillRoundRect(left, top, right, bottom, radius, paint);
                return;
            case FILL_AND_STROKE:
                fillRoundRect(left, top, right, bottom, radius, paint);
            case STROKE:
                strokeRoundRect(left, top, right, bottom, radius, paint);
        }
        throw new UnsupportedOperationException();
    }

    @Deprecated
    protected void fillRoundRect(float left, float top, float right, float bottom,
                                 float r, @Nonnull Paint paint) {
        final RoundRectProgram.Fill program = RoundRectProgram.fill();
        program.use();
        program.setRadius(r, Math.min(r, paint.getSmoothRadius()));
        program.setInnerRect(left + r, top + r, right - r, bottom - r);
        upload(left, top, right, bottom, paint.getColor());
        GLWrapper.stopProgram();
        throw new UnsupportedOperationException();
    }

    @Deprecated
    protected void strokeRoundRect(float left, float top, float right, float bottom,
                                   float r, @Nonnull Paint paint) {
        final RoundRectProgram.Stroke program = RoundRectProgram.stroke();
        program.use();
        float thickness = Math.min(paint.getStrokeWidth() * 0.5f, r);
        program.setRadius(r, Math.min(thickness, paint.getSmoothRadius()), thickness);
        program.setInnerRect(left + r, top + r, right - r, bottom - r);
        upload(left - r, top - r, right + r, bottom + r, paint.getColor());
        GLWrapper.stopProgram();
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public void drawRoundedFrameT1(float left, float top, float right, float bottom, float radius) {
        throw new UnsupportedOperationException();
        /*RoundRectProgram.Stroke program = RoundRectProgram.stroke();
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
        GLWrapper.stopProgram();*/
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
    @Deprecated
    public void drawRoundImage(@Nonnull Icon icon, float left, float top, float right, float bottom,
                               float radius, @Nonnull Paint paint) {
        RoundRectProgram.FillTex program = RoundRectProgram.fillTex();
        program.use();
        program.setRadius(radius, Math.min(radius, paint.getSmoothRadius()));
        program.setInnerRect(left + radius, top + radius, right - radius, bottom - radius);
        RenderSystem.activeTexture(GL43.GL_TEXTURE0);
        icon.bindTexture();

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

        GLWrapper.stopProgram();
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public void clipVertical(@Nonnull View view) {
        throw new UnsupportedOperationException();
        /*GL43.glEnable(GL43.GL_SCISSOR_TEST);
        GL43.glScissor(0, window.getHeight() - view.getBottom(),
                window.getWidth(), view.getHeight());*/
    }

    @Deprecated
    public void clipStart(float x, float y, float width, float height) {
        throw new UnsupportedOperationException();
        /*double scale = window.getGuiScale();
        GL43.glEnable(GL43.GL_SCISSOR_TEST);
        GL43.glScissor((int) (x * scale), (int) (window.getHeight() - ((y + height) * scale)),
                (int) (width * scale), (int) (height * scale));*/
    }

    @Deprecated
    public void clipEnd() {
        throw new UnsupportedOperationException();
        /*GL43.glDisable(GL43.GL_SCISSOR_TEST);*/
    }

    /*@Deprecated
    private final BufferBuilder mBufferBuilder = Tesselator.getInstance().getBuilder();*/


    /*@Deprecated
    private int r = 255;
    @Deprecated
    private int g = 255;
    @Deprecated
    private int b = 255;
    @Deprecated
    private int a = 255;


    @Deprecated
    private double z = 0.0D;*/

    /*
     * Drawing location offset, view or drawable
     */
    /*private int drawingX = 0;
    private int drawingY = 0;*/

    /*@Deprecated
    private long drawingTime = 0;


    @Deprecated
    private float alignFactor = TextAlign.LEFT.offsetFactor;

    @Deprecated
    private static boolean lineAA = false;*/
}
