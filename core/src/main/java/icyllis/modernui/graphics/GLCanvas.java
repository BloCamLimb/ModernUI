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
import icyllis.modernui.graphics.vertex.VertexAttrib;
import icyllis.modernui.graphics.vertex.VertexFormat;
import icyllis.modernui.math.MathUtil;
import icyllis.modernui.math.Matrix4;
import icyllis.modernui.platform.RenderCore;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * Modern UI implementation to Canvas.
 */
public final class GLCanvas extends Canvas {

    private static GLCanvas INSTANCE;

    public static final int MATRIX_BLOCK_BINDING = 0;

    public static final int GENERIC_BINDING = 0;
    public static final int INSTANCED_BINDING = 1;

    public static final VertexAttrib POS;
    public static final VertexAttrib COLOR;
    public static final VertexAttrib MODEL_VIEW;

    public static final VertexFormat POS_COLOR;

    public static final Shader POS_COLOR_FILL = new Shader();

    public static final int DRAW_RECT = 1;

    static {
        POS = new VertexAttrib(GENERIC_BINDING, VertexAttrib.Src.FLOAT, VertexAttrib.Dst.VEC2, false);
        COLOR = new VertexAttrib(GENERIC_BINDING, VertexAttrib.Src.UBYTE, VertexAttrib.Dst.VEC4, true);
        MODEL_VIEW = new VertexAttrib(INSTANCED_BINDING, VertexAttrib.Src.FLOAT, VertexAttrib.Dst.MAT4, false);
        POS_COLOR = new VertexFormat(POS, COLOR, MODEL_VIEW);
    }

    private final Deque<Matrix4> mMatrixStack = new ArrayDeque<>();

    private int mPosColorVBO = INVALID_ID;
    private int mPosColorSize;
    private ByteBuffer mPosColorData = MemoryUtil.memAlloc(1024);

    private int mMatrixVBO = INVALID_ID;
    private int mMatrixSize;
    private ByteBuffer mMatrixData = MemoryUtil.memAlloc(1024);

    private final IntList mDrawStates = new IntArrayList();

    private final int mMatrixUBO;

    private GLCanvas() {
        mMatrixUBO = glCreateBuffers();
        glNamedBufferStorage(mMatrixUBO, 64, GL_DYNAMIC_STORAGE_BIT | GL_MAP_WRITE_BIT);
        glBindBufferBase(GL_UNIFORM_BUFFER, MATRIX_BLOCK_BINDING, mMatrixUBO);

        mMatrixStack.push(Matrix4.identity());

        ShaderManager.getInstance().addListener(this::onLoadShaders);
    }

    public static void initialize() {
        RenderCore.checkRenderThread();
        if (INSTANCE == null) {
            INSTANCE = new GLCanvas();
            POS_COLOR.setBindingDivisor(INSTANCED_BINDING, 1);
        }
    }

    public static GLCanvas getInstance() {
        return INSTANCE;
    }

    private void onLoadShaders(@Nonnull ShaderManager manager) {
        int pos_color_vert = manager.getShard(ModernUI.get(), "pos_color.vert");
        int color_frag = manager.getShard(ModernUI.get(), "color.frag");
        manager.create(POS_COLOR_FILL, pos_color_vert, color_frag);
        ModernUI.LOGGER.info("Loaded shader programs");
    }

    public void setProjection(Matrix4 mat) {
        ByteBuffer buffer = glMapNamedBuffer(mMatrixUBO, GL_WRITE_ONLY);
        if (buffer == null) {
            throw new IllegalStateException();
        }
        mat.get(buffer);
        glUnmapNamedBuffer(mMatrixUBO);
    }

    public void render() {
        uploadBuffers();
        int baseInstance = 0;
        int posColorIndex = 0;
        for (int draw : mDrawStates) {
            if (draw == DRAW_RECT) {
                glBindVertexArray(POS_COLOR.getVertexArray());
                POS_COLOR_FILL.use();
                glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorIndex, 4, 1, baseInstance);
                posColorIndex += 4;
            }
            baseInstance++;
        }
        mDrawStates.clear();
    }

    private void uploadBuffers() {
        if (mPosColorSize < mPosColorData.capacity()) {
            if (mPosColorVBO != INVALID_ID) {
                glDeleteBuffers(mPosColorVBO);
            }
            mPosColorVBO = glCreateBuffers();
            glNamedBufferStorage(mPosColorVBO, mPosColorData.capacity(), GL_DYNAMIC_STORAGE_BIT);
            mPosColorSize = mPosColorData.capacity();

            ModernUI.LOGGER.info("Create PosColor VBO");
            POS_COLOR.setVertexBuffer(GENERIC_BINDING, mPosColorVBO, 0);
        }
        mPosColorData.flip();
        glNamedBufferSubData(mPosColorVBO, 0, mPosColorData);
        ModernUI.LOGGER.info("Update PosColor Data {}", mPosColorData);
        mPosColorData.clear();

        if (mMatrixSize < mMatrixData.capacity()) {
            if (mMatrixVBO != INVALID_ID) {
                glDeleteBuffers(mMatrixVBO);
            }
            mMatrixVBO = glCreateBuffers();
            glNamedBufferStorage(mMatrixVBO, mMatrixData.capacity(), GL_DYNAMIC_STORAGE_BIT);
            mMatrixSize = mMatrixData.capacity();

            ModernUI.LOGGER.info("Create Matrix VBO");
            POS_COLOR.setVertexBuffer(INSTANCED_BINDING, mMatrixVBO, 0);
            // other
        }
        mMatrixData.flip();
        glNamedBufferSubData(mMatrixVBO, 0, mMatrixData);
        ModernUI.LOGGER.info("Update Matrix Data {}", mMatrixData);
        mMatrixData.clear();
    }

    private void checkPosColor() {
        if (mPosColorData.remaining() < 256) {
            mPosColorData = MemoryUtil.memRealloc(mPosColorData, mPosColorData.capacity() << 1);
        }
        if (mMatrixData.remaining() < 64) {
            mMatrixData = MemoryUtil.memRealloc(mMatrixData, mMatrixData.capacity() << 1);
        }
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

    @Override
    public void drawRect(float left, float top, float right, float bottom, @Nonnull Paint paint) {
        checkPosColor();
        ByteBuffer buffer = mPosColorData;
        byte r = (byte) ((paint.getColor() >> 16) & 0xff);
        byte g = (byte) ((paint.getColor() >> 8) & 0xff);
        byte b = (byte) ((paint.getColor()) & 0xff);
        byte a = (byte) ((paint.getColor() >> 24) & 0xff);
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
        getMatrix().get(mMatrixData);
        mDrawStates.add(DRAW_RECT);
    }
}
