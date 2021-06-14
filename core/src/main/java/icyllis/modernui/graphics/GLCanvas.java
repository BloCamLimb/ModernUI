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

import icyllis.modernui.ModernUI;
import icyllis.modernui.graphics.shader.Shader;
import icyllis.modernui.graphics.shader.ShaderManager;
import icyllis.modernui.graphics.texture.Texture2D;
import icyllis.modernui.graphics.vertex.VertexAttrib;
import icyllis.modernui.graphics.vertex.VertexFormat;
import icyllis.modernui.math.MathUtil;
import icyllis.modernui.math.Matrix4;
import icyllis.modernui.math.Rect;
import icyllis.modernui.platform.RenderCore;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * Modern UI implementation to Canvas, handling multi-threaded rendering.
 */
public final class GLCanvas extends Canvas {

    private static GLCanvas INSTANCE;

    /**
     * Interface block binding points
     */
    public static final int MATRIX_BLOCK_BINDING = 0;
    public static final int ROUND_RECT_BINDING = 1;
    public static final int CIRCLE_BINDING = 2;
    public static final int ARC_BINDING = 3;

    /**
     * Vertex buffer binding points
     */
    public static final int GENERIC_BINDING = 0;
    public static final int INSTANCED_BINDING = 1;

    /**
     * Vertex attributes
     */
    public static final VertexAttrib POS;
    public static final VertexAttrib COLOR;
    public static final VertexAttrib UV;
    public static final VertexAttrib MODEL_VIEW;

    /**
     * Vertex formats
     */
    public static final VertexFormat POS_COLOR;
    public static final VertexFormat POS_COLOR_TEX;

    /**
     * Shader programs
     */
    public static final Shader COLOR_FILL = new Shader();
    public static final Shader COLOR_TEX = new Shader();
    public static final Shader ROUND_RECT_FILL = new Shader();
    public static final Shader ROUND_RECT_TEX = new Shader();
    public static final Shader ROUND_RECT_STROKE = new Shader();
    public static final Shader CIRCLE_FILL = new Shader();
    public static final Shader CIRCLE_STROKE = new Shader();
    public static final Shader ARC_FILL = new Shader();
    public static final Shader ARC_STROKE = new Shader();

    /**
     * Recording commands
     */
    public static final int DRAW_RECT = 1;
    public static final int DRAW_IMAGE = 2;
    public static final int DRAW_ROUND_RECT = 3;
    public static final int DRAW_ROUND_IMAGE = 4;
    public static final int DRAW_ROUND_RECT_OUTLINE = 5;
    public static final int DRAW_CIRCLE = 6;
    public static final int DRAW_CIRCLE_OUTLINE = 7;
    public static final int DRAW_ARC = 8;
    public static final int DRAW_ARC_OUTLINE = 9;

    /**
     * Uniform block sizes, use std140 layout
     */
    public static final int PROJECTION_UNIFORM_SIZE = 64;
    public static final int ROUND_RECT_UNIFORM_SIZE = 28;
    public static final int CIRCLE_UNIFORM_SIZE = 24;
    public static final int ARC_UNIFORM_SIZE = 32;

    static {
        POS = new VertexAttrib(GENERIC_BINDING, VertexAttrib.Src.FLOAT, VertexAttrib.Dst.VEC2, false);
        COLOR = new VertexAttrib(GENERIC_BINDING, VertexAttrib.Src.UBYTE, VertexAttrib.Dst.VEC4, true);
        UV = new VertexAttrib(GENERIC_BINDING, VertexAttrib.Src.FLOAT, VertexAttrib.Dst.VEC2, false);
        MODEL_VIEW = new VertexAttrib(INSTANCED_BINDING, VertexAttrib.Src.FLOAT, VertexAttrib.Dst.MAT4, false);
        POS_COLOR = new VertexFormat(POS, COLOR, MODEL_VIEW);
        POS_COLOR_TEX = new VertexFormat(POS, COLOR, UV, MODEL_VIEW);
    }

    private final Deque<Matrix4> mMatrixStack = new ArrayDeque<>();

    private final IntList mDrawStates = new IntArrayList();

    private int mPosColorVBO = INVALID_ID;
    private ByteBuffer mPosColorData = MemoryUtil.memAlloc(1024);

    private int mPosColorTexVBO = INVALID_ID;
    private ByteBuffer mPosColorTexData = MemoryUtil.memAlloc(1024);

    private int mModelViewVBO = INVALID_ID;
    private ByteBuffer mModelViewData = MemoryUtil.memAlloc(1024);

    private final int mProjectionUBO;

    private ByteBuffer mUniformData = MemoryUtil.memAlloc(1024);
    private final int mRoundRectUBO;
    private final int mCircleUBO;
    private final int mArcUBO;

    private final List<Texture2D> mTextures = new ArrayList<>();

    private GLCanvas() {
        mProjectionUBO = glCreateBuffers();
        glNamedBufferStorage(mProjectionUBO, PROJECTION_UNIFORM_SIZE, GL_DYNAMIC_STORAGE_BIT | GL_MAP_WRITE_BIT);

        mRoundRectUBO = glCreateBuffers();
        glNamedBufferStorage(mRoundRectUBO, ROUND_RECT_UNIFORM_SIZE, GL_DYNAMIC_STORAGE_BIT);

        mCircleUBO = glCreateBuffers();
        glNamedBufferStorage(mCircleUBO, CIRCLE_UNIFORM_SIZE, GL_DYNAMIC_STORAGE_BIT);

        mArcUBO = glCreateBuffers();
        glNamedBufferStorage(mArcUBO, ARC_UNIFORM_SIZE, GL_DYNAMIC_STORAGE_BIT);

        mMatrixStack.push(Matrix4.identity());

        ShaderManager.getInstance().addListener(this::onLoadShaders);
    }

    public static GLCanvas initialize() {
        RenderCore.checkRenderThread();
        if (INSTANCE == null) {
            INSTANCE = new GLCanvas();
            POS_COLOR.setBindingDivisor(INSTANCED_BINDING, 1);
            POS_COLOR_TEX.setBindingDivisor(INSTANCED_BINDING, 1);
        }
        return INSTANCE;
    }

    // internal use
    public static GLCanvas getInstance() {
        return INSTANCE;
    }

    private void onLoadShaders(@Nonnull ShaderManager manager) {
        int posColor = manager.getShard(ModernUI.get(), "pos_color.vert");
        int posColorTex = manager.getShard(ModernUI.get(), "pos_color_tex.vert");

        int fill = manager.getShard(ModernUI.get(), "color_fill.frag");
        int tex = manager.getShard(ModernUI.get(), "color_tex.frag");
        int roundRectFill = manager.getShard(ModernUI.get(), "round_rect_fill.frag");
        int roundRectTex = manager.getShard(ModernUI.get(), "round_rect_tex.frag");
        int roundRectStroke = manager.getShard(ModernUI.get(), "round_rect_stroke.frag");
        int circleFill = manager.getShard(ModernUI.get(), "circle_fill.frag");
        int circleStroke = manager.getShard(ModernUI.get(), "circle_stroke.frag");
        int arcFill = manager.getShard(ModernUI.get(), "arc_fill.frag");
        int arcStroke = manager.getShard(ModernUI.get(), "arc_stroke.frag");

        manager.create(COLOR_FILL, posColor, fill);
        manager.create(COLOR_TEX, posColorTex, tex);
        manager.create(ROUND_RECT_FILL, posColor, roundRectFill);
        manager.create(ROUND_RECT_TEX, posColorTex, roundRectTex);
        manager.create(ROUND_RECT_STROKE, posColor, roundRectStroke);
        manager.create(CIRCLE_FILL, posColor, circleFill);
        manager.create(CIRCLE_STROKE, posColor, circleStroke);
        manager.create(ARC_FILL, posColor, arcFill);
        manager.create(ARC_STROKE, posColor, arcStroke);

        ModernUI.LOGGER.info(MARKER, "Loaded shader programs");
    }

    /**
     * Set global projection matrix.
     *
     * @param projection the project matrix to replace current one
     */
    public void setProjection(Matrix4 projection) {
        RenderCore.checkRenderThread();
        ByteBuffer buffer = glMapNamedBuffer(mProjectionUBO, GL_WRITE_ONLY);
        if (buffer == null) {
            throw new IllegalStateException();
        }
        projection.get(buffer);
        glUnmapNamedBuffer(mProjectionUBO);
    }

    public void render() {
        RenderCore.checkRenderThread();
        if (getSaveCount() != 1) {
            throw new IllegalStateException("Unbalanced save()/restore() pair");
        }
        // upload textures
        RenderCore.flushRenderCalls();
        uploadBuffers();

        // uniform bindings are shared, we must re-bind before we use them
        glBindBufferBase(GL_UNIFORM_BUFFER, MATRIX_BLOCK_BINDING, mProjectionUBO);
        glBindBufferBase(GL_UNIFORM_BUFFER, ROUND_RECT_BINDING, mRoundRectUBO);
        glBindBufferBase(GL_UNIFORM_BUFFER, CIRCLE_BINDING, mCircleUBO);
        glBindBufferBase(GL_UNIFORM_BUFFER, ARC_BINDING, mArcUBO);

        long uniformDataPtr = MemoryUtil.memAddress(mUniformData.flip());

        final int posColorVAO = POS_COLOR.getVertexArray();
        final int posColorTexVAO = POS_COLOR_TEX.getVertexArray();

        final int colorShader = COLOR_FILL.get();
        final int colorImageShader = COLOR_TEX.get();
        final int roundRectShader = ROUND_RECT_FILL.get();
        final int roundImageShader = ROUND_RECT_TEX.get();
        final int roundRectStrokeShader = ROUND_RECT_STROKE.get();
        final int circleShader = CIRCLE_FILL.get();
        final int circleStrokeShader = CIRCLE_STROKE.get();
        final int arcShader = ARC_FILL.get();
        final int arcStrokeShader = ARC_STROKE.get();

        // draw states
        int vao = 0, program = 0;
        // base instance
        int instance = 0;
        // generic array index
        int posColorIndex = 0;
        int posColorTexIndex = 0;
        // textures
        int textureIndex = 0;

        for (int draw : mDrawStates) {
            switch (draw) {
                case DRAW_RECT:
                    if (vao != posColorVAO) {
                        glBindVertexArray(posColorVAO);
                        vao = posColorVAO;
                    }
                    if (program != colorShader) {
                        glUseProgram(colorShader);
                        program = colorShader;
                    }
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorIndex, 4, 1, instance);
                    posColorIndex += 4;
                    break;

                case DRAW_ROUND_RECT:
                    if (vao != posColorVAO) {
                        glBindVertexArray(posColorVAO);
                        vao = posColorVAO;
                    }
                    if (program != roundRectShader) {
                        glUseProgram(roundRectShader);
                        program = roundRectShader;
                    }
                    nglNamedBufferSubData(mRoundRectUBO, 0, ROUND_RECT_UNIFORM_SIZE, uniformDataPtr);
                    uniformDataPtr += ROUND_RECT_UNIFORM_SIZE;
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorIndex, 4, 1, instance);
                    posColorIndex += 4;
                    break;

                case DRAW_ROUND_RECT_OUTLINE:
                    if (vao != posColorVAO) {
                        glBindVertexArray(posColorVAO);
                        vao = posColorVAO;
                    }
                    if (program != roundRectStrokeShader) {
                        glUseProgram(roundRectStrokeShader);
                        program = roundRectStrokeShader;
                    }
                    nglNamedBufferSubData(mRoundRectUBO, 0, ROUND_RECT_UNIFORM_SIZE, uniformDataPtr);
                    uniformDataPtr += ROUND_RECT_UNIFORM_SIZE;
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorIndex, 4, 1, instance);
                    posColorIndex += 4;
                    break;

                case DRAW_ROUND_IMAGE:
                    if (vao != posColorTexVAO) {
                        glBindVertexArray(posColorTexVAO);
                        vao = posColorTexVAO;
                    }
                    if (program != roundImageShader) {
                        glUseProgram(roundImageShader);
                        program = roundImageShader;
                    }
                    nglNamedBufferSubData(mRoundRectUBO, 0, ROUND_RECT_UNIFORM_SIZE, uniformDataPtr);
                    uniformDataPtr += ROUND_RECT_UNIFORM_SIZE;
                    glBindTextureUnit(0, mTextures.get(textureIndex).get());
                    textureIndex++;
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorTexIndex, 4, 1, instance);
                    posColorTexIndex += 4;
                    break;

                case DRAW_IMAGE:
                    if (vao != posColorTexVAO) {
                        glBindVertexArray(posColorTexVAO);
                        vao = posColorTexVAO;
                    }
                    if (program != colorImageShader) {
                        glUseProgram(colorImageShader);
                        program = colorImageShader;
                    }
                    glBindTextureUnit(0, mTextures.get(textureIndex).get());
                    textureIndex++;
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorTexIndex, 4, 1, instance);
                    posColorTexIndex += 4;
                    break;

                case DRAW_CIRCLE:
                    if (vao != posColorVAO) {
                        glBindVertexArray(posColorVAO);
                        vao = posColorVAO;
                    }
                    if (program != circleShader) {
                        glUseProgram(circleShader);
                        program = circleShader;
                    }
                    nglNamedBufferSubData(mCircleUBO, 0, CIRCLE_UNIFORM_SIZE, uniformDataPtr);
                    uniformDataPtr += CIRCLE_UNIFORM_SIZE;
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorIndex, 4, 1, instance);
                    posColorIndex += 4;
                    break;

                case DRAW_CIRCLE_OUTLINE:
                    if (vao != posColorVAO) {
                        glBindVertexArray(posColorVAO);
                        vao = posColorVAO;
                    }
                    if (program != circleStrokeShader) {
                        glUseProgram(circleStrokeShader);
                        program = circleStrokeShader;
                    }
                    nglNamedBufferSubData(mCircleUBO, 0, CIRCLE_UNIFORM_SIZE, uniformDataPtr);
                    uniformDataPtr += CIRCLE_UNIFORM_SIZE;
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorIndex, 4, 1, instance);
                    posColorIndex += 4;
                    break;

                case DRAW_ARC:
                    if (vao != posColorVAO) {
                        glBindVertexArray(posColorVAO);
                        vao = posColorVAO;
                    }
                    if (program != arcShader) {
                        glUseProgram(arcShader);
                        program = arcShader;
                    }
                    nglNamedBufferSubData(mArcUBO, 0, ARC_UNIFORM_SIZE, uniformDataPtr);
                    uniformDataPtr += ARC_UNIFORM_SIZE;
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorIndex, 4, 1, instance);
                    posColorIndex += 4;
                    break;

                case DRAW_ARC_OUTLINE:
                    if (vao != posColorVAO) {
                        glBindVertexArray(posColorVAO);
                        vao = posColorVAO;
                    }
                    if (program != arcStrokeShader) {
                        glUseProgram(arcStrokeShader);
                        program = arcStrokeShader;
                    }
                    nglNamedBufferSubData(mArcUBO, 0, ARC_UNIFORM_SIZE, uniformDataPtr);
                    uniformDataPtr += ARC_UNIFORM_SIZE;
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorIndex, 4, 1, instance);
                    posColorIndex += 4;
                    break;
            }
            instance++;
        }

        mTextures.clear();
        mUniformData.clear();
        mDrawStates.clear();
    }

    private void uploadBuffers() {
        if (mPosColorVBO == INVALID_ID) {
            createPosColorVBO();
        } else {
            int size = glGetNamedBufferParameteri(mPosColorVBO, GL_BUFFER_SIZE);
            if (size < mPosColorData.capacity()) {
                createPosColorVBO();
            }
        }
        mPosColorData.flip();
        glNamedBufferSubData(mPosColorVBO, 0, mPosColorData);
        mPosColorData.clear();

        if (mPosColorTexVBO == INVALID_ID) {
            createPosColorTexVBO();
        } else {
            int size = glGetNamedBufferParameteri(mPosColorTexVBO, GL_BUFFER_SIZE);
            if (size < mPosColorTexData.capacity()) {
                createPosColorTexVBO();
            }
        }
        mPosColorTexData.flip();
        glNamedBufferSubData(mPosColorTexVBO, 0, mPosColorTexData);
        mPosColorTexData.clear();

        if (mModelViewVBO == INVALID_ID) {
            createModelViewVBO();
        } else {
            int size = glGetNamedBufferParameteri(mModelViewVBO, GL_BUFFER_SIZE);
            if (size < mModelViewData.capacity()) {
                createModelViewVBO();
            }
        }
        mModelViewData.flip();
        glNamedBufferSubData(mModelViewVBO, 0, mModelViewData);
        mModelViewData.clear();
    }

    private void createPosColorVBO() {
        mPosColorVBO = glCreateBuffers();
        glNamedBufferStorage(mPosColorVBO, mPosColorData.capacity(), GL_DYNAMIC_STORAGE_BIT);
        POS_COLOR.setVertexBuffer(GENERIC_BINDING, mPosColorVBO, 0);
    }

    private void createPosColorTexVBO() {
        mPosColorTexVBO = glCreateBuffers();
        glNamedBufferStorage(mPosColorTexVBO, mPosColorTexData.capacity(), GL_DYNAMIC_STORAGE_BIT);
        POS_COLOR_TEX.setVertexBuffer(GENERIC_BINDING, mPosColorTexVBO, 0);
    }

    private void createModelViewVBO() {
        mModelViewVBO = glCreateBuffers();
        glNamedBufferStorage(mModelViewVBO, mModelViewData.capacity(), GL_DYNAMIC_STORAGE_BIT);
        // configure
        POS_COLOR.setVertexBuffer(INSTANCED_BINDING, mModelViewVBO, 0);
        POS_COLOR_TEX.setVertexBuffer(INSTANCED_BINDING, mModelViewVBO, 0);
    }

    private ByteBuffer checkPosColorBuffer() {
        if (mPosColorData.remaining() < 256) {
            mPosColorData = MemoryUtil.memRealloc(mPosColorData, mPosColorData.capacity() << 1);
        }
        return mPosColorData;
    }

    private ByteBuffer checkPosColorTexBuffer() {
        if (mPosColorTexData.remaining() < 256) {
            mPosColorTexData = MemoryUtil.memRealloc(mPosColorTexData, mPosColorTexData.capacity() << 1);
        }
        return mPosColorTexData;
    }

    private ByteBuffer checkUniformBuffer() {
        if (mUniformData.remaining() < 256) {
            mUniformData = MemoryUtil.memRealloc(mUniformData, mUniformData.capacity() << 1);
        }
        return mUniformData;
    }

    private ByteBuffer checkModelViewBuffer() {
        if (mModelViewData.remaining() < 64) {
            mModelViewData = MemoryUtil.memRealloc(mModelViewData, mModelViewData.capacity() << 1);
        }
        return mModelViewData;
    }

    @Nonnull
    public Matrix4 getMatrix() {
        return mMatrixStack.getFirst();
    }

    @Override
    public int save() {
        int saveCount = getSaveCount();
        mMatrixStack.push(getMatrix().copy());
        return saveCount;
    }

    @Override
    public void restore() {
        mMatrixStack.pop();
        if (mMatrixStack.isEmpty()) {
            throw new IllegalStateException("Underflow in restore");
        }
    }

    @Override
    public int getSaveCount() {
        return mMatrixStack.size();
    }

    @Override
    public void restoreToCount(int saveCount) {
        if (saveCount < 1) {
            throw new IllegalArgumentException("Underflow in restoreToCount");
        }
        Deque<?> stack = mMatrixStack;
        while (stack.size() > saveCount) {
            stack.pop();
        }
    }

    @Override
    public void translate(float dx, float dy) {
        if (dx != 1.0f && dy != 1.0f)
            getMatrix().translate(dx, dy, 0);
    }

    @Override
    public void scale(float sx, float sy) {
        if (sx != 1.0f && sy != 1.0f)
            getMatrix().scale(sx, sy, 1);
    }

    @Override
    public void rotate(float degrees) {
        if (degrees != 0.0f)
            getMatrix().rotateZ(MathUtil.toRadians(degrees));
    }

    private void putPosColor(float left, float top, float right, float bottom, int color) {
        ByteBuffer buffer = checkPosColorBuffer();
        byte r = (byte) ((color >> 16) & 0xff);
        byte g = (byte) ((color >> 8) & 0xff);
        byte b = (byte) ((color) & 0xff);
        byte a = (byte) ((color >> 24) & 0xff);
        // CCW
        buffer.putFloat(left)
                .putFloat(bottom)
                .put(r).put(g).put(b).put(a);
        buffer.putFloat(right)
                .putFloat(bottom)
                .put(r).put(g).put(b).put(a);
        buffer.putFloat(left)
                .putFloat(top)
                .put(r).put(g).put(b).put(a);
        buffer.putFloat(right)
                .putFloat(top)
                .put(r).put(g).put(b).put(a);
    }

    private void putPosColorTex(float left, float top, float right, float bottom, int color,
                                float u0, float v0, float u1, float v1) {
        ByteBuffer buffer = checkPosColorTexBuffer();
        byte r = (byte) ((color >> 16) & 0xff);
        byte g = (byte) ((color >> 8) & 0xff);
        byte b = (byte) ((color) & 0xff);
        byte a = (byte) ((color >> 24) & 0xff);
        buffer.putFloat(left)
                .putFloat(bottom)
                .put(r).put(g).put(b).put(a)
                .putFloat(u0).putFloat(v1);
        buffer.putFloat(right)
                .putFloat(bottom)
                .put(r).put(g).put(b).put(a)
                .putFloat(u1).putFloat(v1);
        buffer.putFloat(left)
                .putFloat(top)
                .put(r).put(g).put(b).put(a)
                .putFloat(u0).putFloat(v0);
        buffer.putFloat(right)
                .putFloat(top)
                .put(r).put(g).put(b).put(a)
                .putFloat(u1).putFloat(v0);
    }

    @Override
    public void drawArc(float cx, float cy, float radius, float startAngle,
                        float sweepAngle, @Nonnull Paint paint) {
        if (sweepAngle == 0 || radius <= 0)
            return;
        if (sweepAngle >= 360) {
            drawCircle(cx, cy, radius, paint);
            return;
        }
        sweepAngle %= 360;
        float middle = (startAngle % 360) + sweepAngle * 0.5f;
        if (paint.getStyle() != Paint.Style.STROKE) {
            addArcFill(cx, cy, radius, middle, sweepAngle, paint);
        }
        if (paint.getStyle() != Paint.Style.FILL) {
            addArcStroke(cx, cy, radius, middle, sweepAngle, paint);
        }
    }

    private void addArcFill(float cx, float cy, float radius, float middle,
                            float sweepAngle, @Nonnull Paint paint) {
        putPosColor(cx - radius, cy - radius, cx + radius, cy + radius, paint.getColor());
        ByteBuffer buffer = checkUniformBuffer();
        buffer.putFloat(radius)
                .putFloat(Math.min(radius, paint.getSmoothRadius()));
        buffer.position(buffer.position() + 8);
        buffer.putFloat(cx)
                .putFloat(cy);
        buffer.putFloat(middle)
                .putFloat(sweepAngle);
        getMatrix().get(checkModelViewBuffer());
        mDrawStates.add(DRAW_ARC);
    }

    private void addArcStroke(float cx, float cy, float radius, float middle,
                              float sweepAngle, @Nonnull Paint paint) {
        float half = Math.min(paint.getStrokeWidth() * 0.5f, radius);
        float outer = radius + half;
        putPosColor(cx - outer, cy - outer, cx + outer, cy + outer, paint.getColor());
        ByteBuffer buffer = checkUniformBuffer();
        buffer.putFloat(radius)
                .putFloat(Math.min(half, paint.getSmoothRadius()))
                .putFloat(half);
        buffer.position(buffer.position() + 4);
        buffer.putFloat(cx)
                .putFloat(cy);
        buffer.putFloat(middle)
                .putFloat(sweepAngle);
        getMatrix().get(checkModelViewBuffer());
        mDrawStates.add(DRAW_ARC_OUTLINE);
    }

    @Override
    public void drawCircle(float cx, float cy, float radius, @Nonnull Paint paint) {
        if (radius <= 0)
            return;
        if (paint.getStyle() != Paint.Style.STROKE) {
            addCircleFill(cx, cy, radius, paint);
        }
        if (paint.getStyle() != Paint.Style.FILL) {
            addCircleStroke(cx, cy, radius, paint);
        }
    }

    private void addCircleFill(float cx, float cy, float radius, @Nonnull Paint paint) {
        putPosColor(cx - radius, cy - radius, cx + radius, cy + radius, paint.getColor());
        ByteBuffer buffer = checkUniformBuffer();
        // vec4
        buffer.putFloat(radius)
                .putFloat(Math.min(radius, paint.getSmoothRadius()));
        buffer.position(buffer.position() + 8); // padding
        // vec2
        buffer.putFloat(cx)
                .putFloat(cy);
        getMatrix().get(checkModelViewBuffer());
        mDrawStates.add(DRAW_CIRCLE);
    }

    private void addCircleStroke(float cx, float cy, float radius, @Nonnull Paint paint) {
        float half = Math.min(paint.getStrokeWidth() * 0.5f, radius);
        float outer = radius + half;
        putPosColor(cx - outer, cy - outer, cx + outer, cy + outer, paint.getColor());
        ByteBuffer buffer = checkUniformBuffer();
        buffer.putFloat(radius - half)
                .putFloat(outer)
                .putFloat(Math.min(half, paint.getSmoothRadius()));
        buffer.position(buffer.position() + 4); // padding
        buffer.putFloat(cx)
                .putFloat(cy);
        getMatrix().get(checkModelViewBuffer());
        mDrawStates.add(DRAW_CIRCLE_OUTLINE);
    }

    @Override
    public void drawRect(@Nonnull Rect r, @Nonnull Paint paint) {
        drawRect(r.left, r.top, r.right, r.bottom, paint);
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom, @Nonnull Paint paint) {
        if (right <= left || bottom <= top)
            return;
        putPosColor(left, top, right, bottom, paint.getColor());
        getMatrix().get(checkModelViewBuffer());
        mDrawStates.add(DRAW_RECT);
    }

    @Override
    public void drawImage(@Nonnull Image image, float left, float top, @Nonnull Paint paint) {
        Image.Source source = image.getSource();
        putPosColorTex(left, top, left + source.mWidth, top + source.mHeight, paint.getColor(),
                0, 0, 1, 1);
        mTextures.add(source.mTexture);
        getMatrix().get(checkModelViewBuffer());
        mDrawStates.add(DRAW_IMAGE);
    }

    @Override
    public void drawRoundRect(float left, float top, float right, float bottom, float radius, @Nonnull Paint paint) {
        if (right <= left || bottom <= top)
            return;
        if (radius < 0)
            radius = 0;
        if (paint.getStyle() != Paint.Style.STROKE) {
            addRoundRectFill(left, top, right, bottom, radius, paint);
        }
        if (paint.getStyle() != Paint.Style.FILL) {
            addRoundRectStroke(left, top, right, bottom, radius, paint);
        }
    }

    private void addRoundRectFill(float left, float top, float right, float bottom,
                                  float radius, @Nonnull Paint paint) {
        putPosColor(left, top, right, bottom, paint.getColor());
        ByteBuffer buffer = checkUniformBuffer();
        buffer.putFloat(left + radius)
                .putFloat(top + radius)
                .putFloat(right - radius)
                .putFloat(bottom - radius);
        buffer.putFloat(radius)
                .putFloat(Math.min(radius, paint.getSmoothRadius()));
        buffer.position(buffer.position() + 4);
        getMatrix().get(checkModelViewBuffer());
        mDrawStates.add(DRAW_ROUND_RECT);
    }

    private void addRoundRectStroke(float left, float top, float right, float bottom,
                                    float radius, @Nonnull Paint paint) {
        float half = Math.min(paint.getStrokeWidth() * 0.5f, radius);
        putPosColor(left - half, top - half, right + half, bottom + half, paint.getColor());
        ByteBuffer buffer = checkUniformBuffer();
        buffer.putFloat(left + radius)
                .putFloat(top + radius)
                .putFloat(right - radius)
                .putFloat(bottom - radius);
        buffer.putFloat(radius)
                .putFloat(Math.min(half, paint.getSmoothRadius()))
                .putFloat(half);
        getMatrix().get(checkModelViewBuffer());
        mDrawStates.add(DRAW_ROUND_RECT_OUTLINE);
    }

    @Override
    public void drawRoundImage(@Nonnull Image image, float left, float top, float radius, @Nonnull Paint paint) {
        Image.Source source = image.getSource();
        putPosColorTex(left, top, left + source.mWidth, top + source.mHeight, paint.getColor(),
                0, 0, 1, 1);
        if (radius < 0)
            radius = 0;
        ByteBuffer buffer = checkUniformBuffer();
        buffer.putFloat(left + radius)
                .putFloat(top + radius)
                .putFloat(left + source.mWidth - radius)
                .putFloat(top + source.mHeight - radius);
        buffer.putFloat(radius)
                .putFloat(Math.min(radius, paint.getSmoothRadius()));
        buffer.position(buffer.position() + 4);
        mTextures.add(source.mTexture);
        getMatrix().get(checkModelViewBuffer());
        mDrawStates.add(DRAW_ROUND_IMAGE);
    }
}
