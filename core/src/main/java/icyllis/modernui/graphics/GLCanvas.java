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
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.graphics.shader.Shader;
import icyllis.modernui.graphics.shader.ShaderManager;
import icyllis.modernui.graphics.texture.Texture;
import icyllis.modernui.graphics.vertex.VertexAttrib;
import icyllis.modernui.graphics.vertex.VertexFormat;
import icyllis.modernui.math.MathUtil;
import icyllis.modernui.math.Matrix4;
import icyllis.modernui.math.Rect;
import icyllis.modernui.math.RectF;
import icyllis.modernui.platform.RenderCore;
import icyllis.modernui.text.*;
import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * Modern OpenGL implementation to Canvas, handling multi-threaded rendering.
 * <p>
 * GLCanvas is highly integrated, you can't draw other things except those
 * defined in Canvas easily. This helps to build OpenGL buffers from UI thread,
 * using multiple vertex arrays, uniform buffers and vertex buffers. Later
 * calls OpenGL functions on the render thread.
 * <p>
 * Here, drawing means recording on UI thread (or synchronized), and rendering
 * means calling OpenGL draw functions on the render thread.
 * <p>
 * The color buffer drawn to must be index 0, and stencil buffer must be 8-bit.
 */
@NotThreadSafe
public final class GLCanvas extends Canvas {

    private static GLCanvas INSTANCE;

    // we have only one instance called on UI thread only
    private static final Pool<Matrix4> sMatrixPool = Pools.simple(20);
    private static final Pool<Clip> sClipPool = Pools.simple(20);

    // a client side identity matrix
    private static final Matrix4 IDENTITY_MAT = Matrix4.identity();

    /**
     * Uniform block binding points
     */
    public static final int MATRIX_BLOCK_BINDING = 0;
    public static final int ROUND_RECT_BINDING = 1;
    public static final int CIRCLE_BINDING = 2;
    public static final int ARC_BINDING = 3;
    public static final int GLYPH_BINDING = 4;

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
    public static final VertexFormat POS_TEX_NO_MAT;

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
    public static final Shader GLYPH_BATCH = new Shader();
    public static final Shader COLOR_TEX_MS = new Shader();

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
    public static final int DRAW_CLIP_PUSH = 10;
    public static final int DRAW_CLIP_POP = 11;
    public static final int DRAW_TEXT = 12;
    public static final int DRAW_IMAGE_MS = 13;

    /**
     * Uniform block sizes, use std140 layout
     */
    public static final int PROJECTION_UNIFORM_SIZE = 64;
    public static final int ROUND_RECT_UNIFORM_SIZE = 28;
    public static final int CIRCLE_UNIFORM_SIZE = 24;
    public static final int ARC_UNIFORM_SIZE = 32;
    public static final int GLYPH_UNIFORM_SIZE = 80;

    static {
        POS = new VertexAttrib(GENERIC_BINDING, VertexAttrib.Src.FLOAT, VertexAttrib.Dst.VEC2, false);
        COLOR = new VertexAttrib(GENERIC_BINDING, VertexAttrib.Src.UBYTE, VertexAttrib.Dst.VEC4, true);
        UV = new VertexAttrib(GENERIC_BINDING, VertexAttrib.Src.FLOAT, VertexAttrib.Dst.VEC2, false);
        MODEL_VIEW = new VertexAttrib(INSTANCED_BINDING, VertexAttrib.Src.FLOAT, VertexAttrib.Dst.MAT4, false);
        POS_COLOR = new VertexFormat(POS, COLOR, MODEL_VIEW);
        POS_COLOR_TEX = new VertexFormat(POS, COLOR, UV, MODEL_VIEW);
        POS_TEX_NO_MAT = new VertexFormat(POS, UV);
    }


    // private stacks
    private final Deque<Matrix4> mMatrixStack = new ArrayDeque<>();
    private final Deque<Clip> mClipStack = new ArrayDeque<>();

    // recorded commands
    private final IntList mDrawStates = new IntArrayList();


    // 3 vertex buffer objects
    private int mPosColorVBO = INVALID_ID;
    private ByteBuffer mPosColorData = MemoryUtil.memAlloc(1024);
    private boolean mRecreatePosColor = true;

    private int mPosColorTexVBO = INVALID_ID;
    private ByteBuffer mPosColorTexData = MemoryUtil.memAlloc(1024);
    private boolean mRecreatePosColorTex = true;

    private int mModelViewVBO = INVALID_ID;
    private ByteBuffer mModelViewData = MemoryUtil.memAlloc(1024);
    private boolean mRecreateModelView = true;

    // dynamic updated VBO
    private int mGlyphVBO = INVALID_ID;
    private ByteBuffer mGlyphData = MemoryUtil.memAlloc(1024);
    private boolean mRecreateGlyph = true;


    // universal uniform blocks
    private final int mProjectionUBO;

    // the data used for updating the uniform blocks
    private ByteBuffer mUniformData = MemoryUtil.memAlloc(1024);
    private final int mRoundRectUBO;
    private final int mCircleUBO;
    private final int mArcUBO;
    private final int mGlyphUBO;

    // used in rendering, local states
    private int mCurrVertexArray;
    private int mCurrProgram;
    private int mCurrTexture;

    // using textures of draw states, in the order of calling
    private final List<Texture> mTextures = new ArrayList<>();

    // absolute value presents the reference value, and sign represents whether to
    // update the stencil buffer (positive = update, or just change stencil func)
    private final IntList mClipDepths = new IntArrayList();

    private final List<TextInfo> mTextInfos = new ArrayList<>();

    private final Rect mTmpRect = new Rect();
    private final RectF mTmpRectF = new RectF();


    // constructor on render thread
    private GLCanvas() {
        mProjectionUBO = glCreateBuffers();
        glNamedBufferStorage(mProjectionUBO, PROJECTION_UNIFORM_SIZE, GL_DYNAMIC_STORAGE_BIT | GL_MAP_WRITE_BIT);

        mGlyphUBO = glCreateBuffers();
        glNamedBufferStorage(mGlyphUBO, GLYPH_UNIFORM_SIZE, GL_DYNAMIC_STORAGE_BIT);

        mRoundRectUBO = glCreateBuffers();
        glNamedBufferStorage(mRoundRectUBO, ROUND_RECT_UNIFORM_SIZE, GL_DYNAMIC_STORAGE_BIT);

        mCircleUBO = glCreateBuffers();
        glNamedBufferStorage(mCircleUBO, CIRCLE_UNIFORM_SIZE, GL_DYNAMIC_STORAGE_BIT);

        mArcUBO = glCreateBuffers();
        glNamedBufferStorage(mArcUBO, ARC_UNIFORM_SIZE, GL_DYNAMIC_STORAGE_BIT);

        mMatrixStack.push(Matrix4.identity());
        mClipStack.push(new Clip());

        //ModernUI.LOGGER.info(MARKER, "Created GLCanvas: {}", getSaveCount());

        ShaderManager.getInstance().addListener(this::onLoadShaders);
    }

    @RenderThread
    public static GLCanvas initialize() {
        RenderCore.checkRenderThread();
        if (INSTANCE == null) {
            INSTANCE = new GLCanvas();
            // for instanced-rendering
            POS_COLOR.setBindingDivisor(INSTANCED_BINDING, 1);
            POS_COLOR_TEX.setBindingDivisor(INSTANCED_BINDING, 1);
        }
        return INSTANCE;
    }

    // exposed for internal use, be aware of the thread-safety
    public static GLCanvas getInstance() {
        return INSTANCE;
    }

    private void onLoadShaders(@Nonnull ShaderManager manager) {
        int posColor = manager.getShard(ModernUI.get(), "pos_color.vert");
        int posColorTex = manager.getShard(ModernUI.get(), "pos_color_tex.vert");
        int glyphBatch = manager.getShard(ModernUI.get(), "glyph_batch.vert");

        int colorFill = manager.getShard(ModernUI.get(), "color_fill.frag");
        int colorTex = manager.getShard(ModernUI.get(), "color_tex.frag");
        int roundRectFill = manager.getShard(ModernUI.get(), "round_rect_fill.frag");
        int roundRectTex = manager.getShard(ModernUI.get(), "round_rect_tex.frag");
        int roundRectStroke = manager.getShard(ModernUI.get(), "round_rect_stroke.frag");
        int circleFill = manager.getShard(ModernUI.get(), "circle_fill.frag");
        int circleStroke = manager.getShard(ModernUI.get(), "circle_stroke.frag");
        int arcFill = manager.getShard(ModernUI.get(), "arc_fill.frag");
        int arcStroke = manager.getShard(ModernUI.get(), "arc_stroke.frag");
        int glyphTex = manager.getShard(ModernUI.get(), "glyph_tex.frag");
        int colorTexMs = manager.getShard(ModernUI.get(), "color_tex_4x.frag");

        manager.create(COLOR_FILL, posColor, colorFill);
        manager.create(COLOR_TEX, posColorTex, colorTex);
        manager.create(ROUND_RECT_FILL, posColor, roundRectFill);
        manager.create(ROUND_RECT_TEX, posColorTex, roundRectTex);
        manager.create(ROUND_RECT_STROKE, posColor, roundRectStroke);
        manager.create(CIRCLE_FILL, posColor, circleFill);
        manager.create(CIRCLE_STROKE, posColor, circleStroke);
        manager.create(ARC_FILL, posColor, arcFill);
        manager.create(ARC_STROKE, posColor, arcStroke);
        manager.create(GLYPH_BATCH, glyphBatch, glyphTex);
        manager.create(COLOR_TEX_MS, posColorTex, colorTexMs);

        ModernUI.LOGGER.info(MARKER, "Loaded shader programs");
    }

    /**
     * Set global projection matrix. This is required when window size changed and
     * called before rendering. It should not change in a frame.
     *
     * @param projection the project matrix to replace current one
     */
    @RenderThread
    public void setProjection(@Nonnull Matrix4 projection) {
        RenderCore.checkRenderThread();
        ByteBuffer buffer = glMapNamedBuffer(mProjectionUBO, GL_WRITE_ONLY);
        if (buffer == null) {
            throw new IllegalStateException("You don't have GL_MAP_WRITE_BIT bit flag");
        }
        projection.get(buffer);
        glUnmapNamedBuffer(mProjectionUBO);
    }

    /**
     * Resets the clip bounds and matrix. This is required before drawing each frame.
     *
     * @param width  the width in pixels
     * @param height the height in pixels
     */
    public void reset(int width, int height) {
        getMatrix().setIdentity();
        Clip clip = getClip();
        clip.mBounds.set(0, 0, width, height);
        clip.mDepth = 0;
    }

    private void bindVertexArray(@Nonnull VertexFormat format) {
        int t = format.getVertexArray();
        if (mCurrVertexArray != t) {
            glBindVertexArray(t);
            mCurrVertexArray = t;
        }
    }

    private void useProgram(@Nonnull Shader shader) {
        int t = shader.get();
        if (mCurrProgram != t) {
            glUseProgram(t);
            mCurrProgram = t;
        }
    }

    private void bindTexture(int tex) {
        if (mCurrTexture != tex) {
            glBindTextureUnit(0, tex);
            mCurrTexture = tex;
        }
    }

    @RenderThread
    public void render() {
        RenderCore.checkRenderThread();
        RenderCore.flushRenderCalls();
        if (mDrawStates.isEmpty()) {
            return;
        }
        if (getSaveCount() != 1) {
            throw new IllegalStateException("Unbalanced save()/restore() pair");
        }
        uploadBuffers();

        // uniform bindings are globally shared, we must re-bind before we use them
        glBindBufferBase(GL_UNIFORM_BUFFER, MATRIX_BLOCK_BINDING, mProjectionUBO);
        glBindBufferBase(GL_UNIFORM_BUFFER, ROUND_RECT_BINDING, mRoundRectUBO);
        glBindBufferBase(GL_UNIFORM_BUFFER, CIRCLE_BINDING, mCircleUBO);
        glBindBufferBase(GL_UNIFORM_BUFFER, ARC_BINDING, mArcUBO);
        glBindBufferBase(GL_UNIFORM_BUFFER, GLYPH_BINDING, mGlyphUBO);

        glStencilFuncSeparate(GL_FRONT, GL_EQUAL, 0, 0xff);
        glStencilMaskSeparate(GL_FRONT, 0xff);

        mCurrVertexArray = 0;
        mCurrProgram = 0;
        mCurrTexture = 0;

        mUniformData.flip();

        long uniformDataPtr = MemoryUtil.memAddress(mUniformData);

        // base instance
        int instance = 0;
        // generic array index
        int posColorIndex = 0;
        int posColorTexIndex = 0;
        // textures
        int textureIndex = 0;
        int clipIndex = 0;
        int clipDepth;
        int textIndex = 0;

        for (int draw : mDrawStates) {
            switch (draw) {
                case DRAW_RECT:
                    bindVertexArray(POS_COLOR);
                    useProgram(COLOR_FILL);
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorIndex, 4, 1, instance);
                    posColorIndex += 4;
                    break;

                case DRAW_ROUND_RECT:
                    bindVertexArray(POS_COLOR);
                    useProgram(ROUND_RECT_FILL);
                    nglNamedBufferSubData(mRoundRectUBO, 0, ROUND_RECT_UNIFORM_SIZE, uniformDataPtr);
                    uniformDataPtr += ROUND_RECT_UNIFORM_SIZE;
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorIndex, 4, 1, instance);
                    posColorIndex += 4;
                    break;

                case DRAW_ROUND_RECT_OUTLINE:
                    bindVertexArray(POS_COLOR);
                    useProgram(ROUND_RECT_STROKE);
                    nglNamedBufferSubData(mRoundRectUBO, 0, ROUND_RECT_UNIFORM_SIZE, uniformDataPtr);
                    uniformDataPtr += ROUND_RECT_UNIFORM_SIZE;
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorIndex, 4, 1, instance);
                    posColorIndex += 4;
                    break;

                case DRAW_ROUND_IMAGE:
                    bindVertexArray(POS_COLOR_TEX);
                    useProgram(ROUND_RECT_TEX);
                    bindTexture(mTextures.get(textureIndex).get());
                    textureIndex++;
                    nglNamedBufferSubData(mRoundRectUBO, 0, ROUND_RECT_UNIFORM_SIZE, uniformDataPtr);
                    uniformDataPtr += ROUND_RECT_UNIFORM_SIZE;
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorTexIndex, 4, 1, instance);
                    posColorTexIndex += 4;
                    break;

                case DRAW_IMAGE:
                    bindVertexArray(POS_COLOR_TEX);
                    useProgram(COLOR_TEX);
                    bindTexture(mTextures.get(textureIndex).get());
                    textureIndex++;
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorTexIndex, 4, 1, instance);
                    posColorTexIndex += 4;
                    break;

                case DRAW_IMAGE_MS:
                    bindVertexArray(POS_COLOR_TEX);
                    useProgram(COLOR_TEX_MS);
                    bindTexture(mTextures.get(textureIndex).get());
                    textureIndex++;
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorTexIndex, 4, 1, instance);
                    posColorTexIndex += 4;
                    break;

                case DRAW_CIRCLE:
                    bindVertexArray(POS_COLOR);
                    useProgram(CIRCLE_FILL);
                    nglNamedBufferSubData(mCircleUBO, 0, CIRCLE_UNIFORM_SIZE, uniformDataPtr);
                    uniformDataPtr += CIRCLE_UNIFORM_SIZE;
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorIndex, 4, 1, instance);
                    posColorIndex += 4;
                    break;

                case DRAW_CIRCLE_OUTLINE:
                    bindVertexArray(POS_COLOR);
                    useProgram(CIRCLE_STROKE);
                    nglNamedBufferSubData(mCircleUBO, 0, CIRCLE_UNIFORM_SIZE, uniformDataPtr);
                    uniformDataPtr += CIRCLE_UNIFORM_SIZE;
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorIndex, 4, 1, instance);
                    posColorIndex += 4;
                    break;

                case DRAW_ARC:
                    bindVertexArray(POS_COLOR);
                    useProgram(ARC_FILL);
                    nglNamedBufferSubData(mArcUBO, 0, ARC_UNIFORM_SIZE, uniformDataPtr);
                    uniformDataPtr += ARC_UNIFORM_SIZE;
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorIndex, 4, 1, instance);
                    posColorIndex += 4;
                    break;

                case DRAW_ARC_OUTLINE:
                    bindVertexArray(POS_COLOR);
                    useProgram(ARC_STROKE);
                    nglNamedBufferSubData(mArcUBO, 0, ARC_UNIFORM_SIZE, uniformDataPtr);
                    uniformDataPtr += ARC_UNIFORM_SIZE;
                    glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorIndex, 4, 1, instance);
                    posColorIndex += 4;
                    break;

                case DRAW_CLIP_PUSH:
                    clipDepth = mClipDepths.getInt(clipIndex);

                    if (clipDepth >= 0) {
                        glStencilOpSeparate(GL_FRONT, GL_KEEP, GL_KEEP, GL_INCR);
                        glColorMaski(0, false, false, false, false);

                        bindVertexArray(POS_COLOR);
                        useProgram(COLOR_FILL);
                        glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorIndex, 4, 1, instance);
                        posColorIndex += 4;

                        glStencilOpSeparate(GL_FRONT, GL_KEEP, GL_KEEP, GL_KEEP);
                        glColorMaski(0, true, true, true, true);
                    }

                    glStencilFuncSeparate(GL_FRONT, GL_EQUAL, Math.abs(clipDepth), 0xff);
                    clipIndex++;
                    break;

                case DRAW_CLIP_POP:
                    clipDepth = mClipDepths.getInt(clipIndex);

                    if (clipDepth >= 0) {
                        glStencilFuncSeparate(GL_FRONT, GL_LESS, clipDepth, 0xff);
                        glStencilOpSeparate(GL_FRONT, GL_KEEP, GL_KEEP, GL_REPLACE);
                        glColorMaski(0, false, false, false, false);

                        bindVertexArray(POS_COLOR);
                        useProgram(COLOR_FILL);
                        glDrawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, posColorIndex, 4, 1, instance);
                        posColorIndex += 4;

                        glStencilOpSeparate(GL_FRONT, GL_KEEP, GL_KEEP, GL_KEEP);
                        glColorMaski(0, true, true, true, true);
                    }

                    glStencilFuncSeparate(GL_FRONT, GL_EQUAL, Math.abs(clipDepth), 0xff);
                    clipIndex++;
                    break;

                case DRAW_TEXT: {
                    bindVertexArray(POS_TEX_NO_MAT);
                    useProgram(GLYPH_BATCH);
                    nglNamedBufferSubData(mGlyphUBO, 0, GLYPH_UNIFORM_SIZE, uniformDataPtr);
                    uniformDataPtr += GLYPH_UNIFORM_SIZE;

                    TextInfo text = mTextInfos.get(textIndex++);
                    final TexturedGlyph[] glyphs = text.mLayoutPiece.getGlyphs();
                    final float[] positions = text.mLayoutPiece.getPositions();
                    for (int i = 0, e = glyphs.length; i < e; i++) {
                        putGlyph(glyphs[i], text.mX + positions[i * 2], text.mY + positions[i * 2 + 1]);
                    }

                    checkGlyphVBO();
                    mGlyphData.flip();
                    glNamedBufferSubData(mGlyphVBO, 0, mGlyphData);
                    mGlyphData.clear();

                    for (int i = 0, e = glyphs.length; i < e; i++) {
                        bindTexture(glyphs[i].texture);
                        glDrawArrays(GL_TRIANGLE_STRIP, i << 2, 4);
                    }
                    break;
                }

                default:
                    throw new IllegalStateException("Unexpected draw state " + draw);
            }

            if (draw != DRAW_TEXT) {
                instance++;
            }
        }

        mTextures.clear();
        mClipDepths.clear();
        mUniformData.clear();
        mTextInfos.clear();
        mDrawStates.clear();
    }

    @RenderThread
    private void uploadBuffers() {
        checkPosColorVBO();
        mPosColorData.flip();
        glNamedBufferSubData(mPosColorVBO, 0, mPosColorData);
        mPosColorData.clear();

        checkPosColorTexVBO();
        mPosColorTexData.flip();
        glNamedBufferSubData(mPosColorTexVBO, 0, mPosColorTexData);
        mPosColorTexData.clear();

        checkModelViewVBO();
        mModelViewData.flip();
        glNamedBufferSubData(mModelViewVBO, 0, mModelViewData);
        mModelViewData.clear();
    }

    @RenderThread
    private void checkPosColorVBO() {
        if (!mRecreatePosColor)
            return;
        mPosColorVBO = glCreateBuffers();
        glNamedBufferStorage(mPosColorVBO, mPosColorData.capacity(), GL_DYNAMIC_STORAGE_BIT);
        POS_COLOR.setVertexBuffer(GENERIC_BINDING, mPosColorVBO, 0);
        mRecreatePosColor = false;
    }

    @RenderThread
    private void checkPosColorTexVBO() {
        if (!mRecreatePosColorTex)
            return;
        mPosColorTexVBO = glCreateBuffers();
        glNamedBufferStorage(mPosColorTexVBO, mPosColorTexData.capacity(), GL_DYNAMIC_STORAGE_BIT);
        POS_COLOR_TEX.setVertexBuffer(GENERIC_BINDING, mPosColorTexVBO, 0);
        mRecreatePosColorTex = false;
    }

    @RenderThread
    private void checkModelViewVBO() {
        if (!mRecreateModelView)
            return;
        mModelViewVBO = glCreateBuffers();
        glNamedBufferStorage(mModelViewVBO, mModelViewData.capacity(), GL_DYNAMIC_STORAGE_BIT);
        // configure
        POS_COLOR.setVertexBuffer(INSTANCED_BINDING, mModelViewVBO, 0);
        POS_COLOR_TEX.setVertexBuffer(INSTANCED_BINDING, mModelViewVBO, 0);
        mRecreateModelView = false;
    }

    @RenderThread
    private void checkGlyphVBO() {
        if (!mRecreateGlyph)
            return;
        mGlyphVBO = glCreateBuffers();
        glNamedBufferStorage(mGlyphVBO, mGlyphData.capacity(), GL_DYNAMIC_STORAGE_BIT);
        POS_TEX_NO_MAT.setVertexBuffer(GENERIC_BINDING, mGlyphVBO, 0);
        mRecreateGlyph = false;
    }

    private ByteBuffer getPosColorBuffer() {
        if (mPosColorData.remaining() < 256) {
            mPosColorData = MemoryUtil.memRealloc(mPosColorData, mPosColorData.capacity() << 1);
            mRecreatePosColor = true;
            ModernUI.LOGGER.debug("Resize position color buffer to {} bytes", mPosColorData.capacity());
        }
        return mPosColorData;
    }

    private ByteBuffer getPosColorTexBuffer() {
        if (mPosColorTexData.remaining() < 256) {
            mPosColorTexData = MemoryUtil.memRealloc(mPosColorTexData, mPosColorTexData.capacity() << 1);
            mRecreatePosColorTex = true;
            ModernUI.LOGGER.debug("Resize position color tex buffer to {} bytes", mPosColorTexData.capacity());
        }
        return mPosColorTexData;
    }

    private ByteBuffer getModelViewBuffer() {
        if (mModelViewData.remaining() < 64) {
            mModelViewData = MemoryUtil.memRealloc(mModelViewData, mModelViewData.capacity() << 1);
            mRecreateModelView = true;
            ModernUI.LOGGER.debug("Resize model view buffer to {} bytes", mModelViewData.capacity());
        }
        return mModelViewData;
    }

    private ByteBuffer getUniformBuffer() {
        if (mUniformData.remaining() < 256) {
            mUniformData = MemoryUtil.memRealloc(mUniformData, mUniformData.capacity() << 1);
            ModernUI.LOGGER.debug("Resize universal uniform buffer to {} bytes", mUniformData.capacity());
        }
        return mUniformData;
    }

    @RenderThread
    private ByteBuffer getGlyphBuffer() {
        if (mGlyphData.remaining() < 64) {
            mGlyphData = MemoryUtil.memRealloc(mGlyphData, mGlyphData.capacity() << 1);
            mRecreateGlyph = true;
            ModernUI.LOGGER.debug("Resize glyph buffer to {} bytes", mGlyphData.capacity());
        }
        return mGlyphData;
    }

    @Nonnull
    public Matrix4 getMatrix() {
        return mMatrixStack.getFirst();
    }

    @Nonnull
    private Clip getClip() {
        return mClipStack.getFirst();
    }

    @Override
    public int save() {
        int saveCount = getSaveCount();

        Matrix4 m = sMatrixPool.acquire();
        if (m == null) {
            m = getMatrix().copy();
        } else {
            m.set(getMatrix());
        }
        mMatrixStack.push(m);

        Clip c = sClipPool.acquire();
        if (c == null) {
            c = getClip().copy();
        } else {
            c.set(getClip());
        }
        mClipStack.push(c);

        return saveCount;
    }

    @Override
    public void restore() {
        sMatrixPool.release(mMatrixStack.pop());
        if (mMatrixStack.isEmpty()) {
            throw new IllegalStateException("Underflow in restore");
        }
        final Clip l = mClipStack.pop();
        if (l.mDepth != getClip().mDepth) {
            restoreClip(l.mBounds);
        }
        sClipPool.release(l);
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
        Deque<Matrix4> stack = mMatrixStack;
        int top = -1;
        Clip l = null;
        while (stack.size() > saveCount) {
            sMatrixPool.release(stack.pop());
            l = mClipStack.pop();
            if (top == -1) {
                top = l.mDepth;
            }
            sClipPool.release(l);
        }

        if (l != null && top != getClip().mDepth) {
            restoreClip(l.mBounds);
        }
    }

    // see also clipRect
    private void restoreClip(@Nonnull Rect b) {
        if (b.isEmpty()) {
            mClipDepths.add(-getClip().mDepth);
        } else {
            putRectColor(b.left, b.top, b.right, b.bottom, ~0);
            IDENTITY_MAT.get(getModelViewBuffer());
            mClipDepths.add(getClip().mDepth);
        }
        mDrawStates.add(DRAW_CLIP_POP);
    }

    @Override
    public void translate(float dx, float dy) {
        if (dx != 0.0f || dy != 0.0f)
            getMatrix().translate(dx, dy, 0);
    }

    @Override
    public void scale(float sx, float sy) {
        if (sx != 1.0f || sy != 1.0f)
            getMatrix().scale(sx, sy, 1);
    }

    @Override
    public void rotate(float degrees) {
        if (degrees != 0.0f)
            getMatrix().rotateZ(MathUtil.toRadians(degrees));
    }

    @Override
    public void multiply(@Nonnull Matrix4 matrix) {
        getMatrix().multiply(matrix);
    }

    private static final class Clip {

        // this is only the maximum bounds transformed by model view matrix
        private final Rect mBounds = new Rect();
        private int mDepth;

        public void set(@Nonnull Clip c) {
            mBounds.set(c.mBounds);
            mDepth = c.mDepth;
        }

        // deep copy
        @Nonnull
        public Clip copy() {
            Clip c = new Clip();
            c.set(this);
            return c;
        }
    }

    @Override
    public boolean clipRect(float left, float top, float right, float bottom) {
        Clip clip = getClip();
        // empty rect, ignore it
        if (right <= left || bottom <= top) {
            return !clip.mBounds.isEmpty();
        }
        // already empty, return false
        if (clip.mBounds.isEmpty()) {
            return false;
        }
        Matrix4 matrix = getMatrix();
        RectF temp = mTmpRectF;
        temp.set(left, top, right, bottom);
        matrix.transform(temp);

        Rect test = mTmpRect;
        temp.roundOut(test);

        // not empty and not changed, return true
        if (test.contains(clip.mBounds)) {
            return true;
        }

        boolean intersects = clip.mBounds.intersect(test);
        int depth = ++clip.mDepth;
        if (!intersects) {
            // empty
            mClipDepths.add(-depth);
            clip.mBounds.setEmpty();
        } else {
            // updating stencil must have a color
            putRectColor(left, top, right, bottom, ~0);
            matrix.get(getModelViewBuffer());
            mClipDepths.add(depth);
        }
        mDrawStates.add(DRAW_CLIP_PUSH);
        return intersects;
    }

    @Override
    public boolean quickReject(float left, float top, float right, float bottom) {
        // empty rect, always reject
        if (right <= left || bottom <= top) {
            return true;
        }
        Rect clip = getClip().mBounds;
        if (clip.isEmpty()) {
            return true;
        }
        Rect test = mTmpRect;
        RectF temp = mTmpRectF;
        temp.set(left, top, right, bottom);
        getMatrix().transform(temp);
        temp.roundOut(test);
        return !Rect.intersects(clip, test);
    }

    private void putRectColor(float left, float top, float right, float bottom, @Nonnull Paint paint) {
        if (paint.isMultiColor()) {
            ByteBuffer buffer = getPosColorBuffer();
            int[] colors = paint.getColors();

            // CCW
            int color = colors[3];
            byte r = (byte) ((color >> 16) & 0xff);
            byte g = (byte) ((color >> 8) & 0xff);
            byte b = (byte) (color & 0xff);
            byte a = (byte) (color >>> 24);
            buffer.putFloat(left)
                    .putFloat(bottom)
                    .put(r).put(g).put(b).put(a);

            color = colors[2];
            r = (byte) ((color >> 16) & 0xff);
            g = (byte) ((color >> 8) & 0xff);
            b = (byte) (color & 0xff);
            a = (byte) (color >>> 24);
            buffer.putFloat(right)
                    .putFloat(bottom)
                    .put(r).put(g).put(b).put(a);

            color = colors[0];
            r = (byte) ((color >> 16) & 0xff);
            g = (byte) ((color >> 8) & 0xff);
            b = (byte) (color & 0xff);
            a = (byte) (color >>> 24);
            buffer.putFloat(left)
                    .putFloat(top)
                    .put(r).put(g).put(b).put(a);

            color = colors[1];
            r = (byte) ((color >> 16) & 0xff);
            g = (byte) ((color >> 8) & 0xff);
            b = (byte) (color & 0xff);
            a = (byte) (color >>> 24);
            buffer.putFloat(right)
                    .putFloat(top)
                    .put(r).put(g).put(b).put(a);
        } else {
            putRectColor(left, top, right, bottom, paint.getColor());
        }
    }

    private void putRectColor(float left, float top, float right, float bottom, int color) {
        ByteBuffer buffer = getPosColorBuffer();
        byte r = (byte) ((color >> 16) & 0xff);
        byte g = (byte) ((color >> 8) & 0xff);
        byte b = (byte) (color & 0xff);
        byte a = (byte) (color >>> 24);
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

    private void putRectColorUV(float left, float top, float right, float bottom, int color,
                                float u0, float v0, float u1, float v1) {
        ByteBuffer buffer = getPosColorTexBuffer();
        byte r = (byte) ((color >> 16) & 0xff);
        byte g = (byte) ((color >> 8) & 0xff);
        byte b = (byte) (color & 0xff);
        byte a = (byte) (color >>> 24);
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

    @RenderThread
    private void putGlyph(@Nonnull TexturedGlyph glyph, float left, float top) {
        ByteBuffer buffer = getGlyphBuffer();
        left += glyph.offsetX;
        top += glyph.offsetY;
        float right = left + glyph.width;
        float bottom = top + glyph.height;
        buffer.putFloat(left)
                .putFloat(bottom)
                .putFloat(glyph.u1).putFloat(glyph.v2);
        buffer.putFloat(right)
                .putFloat(bottom)
                .putFloat(glyph.u2).putFloat(glyph.v2);
        buffer.putFloat(left)
                .putFloat(top)
                .putFloat(glyph.u1).putFloat(glyph.v1);
        buffer.putFloat(right)
                .putFloat(top)
                .putFloat(glyph.u2).putFloat(glyph.v1);
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
        if (quickReject(cx - radius, cy - radius, cx + radius, cy + radius)) {
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
        putRectColor(cx - radius, cy - radius, cx + radius, cy + radius, paint);
        ByteBuffer buffer = getUniformBuffer();
        buffer.putFloat(radius)
                .putFloat(Math.min(radius, paint.getSmoothRadius()));
        buffer.position(buffer.position() + 8);
        buffer.putFloat(cx)
                .putFloat(cy);
        buffer.putFloat(middle)
                .putFloat(sweepAngle);
        getMatrix().get(getModelViewBuffer());
        mDrawStates.add(DRAW_ARC);
    }

    private void addArcStroke(float cx, float cy, float radius, float middle,
                              float sweepAngle, @Nonnull Paint paint) {
        float half = Math.min(paint.getStrokeWidth() * 0.5f, radius);
        float outer = radius + half;
        putRectColor(cx - outer, cy - outer, cx + outer, cy + outer, paint);
        ByteBuffer buffer = getUniformBuffer();
        buffer.putFloat(radius)
                .putFloat(Math.min(half, paint.getSmoothRadius()))
                .putFloat(half);
        buffer.position(buffer.position() + 4);
        buffer.putFloat(cx)
                .putFloat(cy);
        buffer.putFloat(middle)
                .putFloat(sweepAngle);
        getMatrix().get(getModelViewBuffer());
        mDrawStates.add(DRAW_ARC_OUTLINE);
    }

    @Override
    public void drawCircle(float cx, float cy, float radius, @Nonnull Paint paint) {
        if (radius <= 0)
            return;
        if (quickReject(cx - radius, cy - radius, cx + radius, cy + radius)) {
            return;
        }
        if (paint.getStyle() != Paint.Style.STROKE) {
            addCircleFill(cx, cy, radius, paint);
        }
        if (paint.getStyle() != Paint.Style.FILL) {
            addCircleStroke(cx, cy, radius, paint);
        }
    }

    private void addCircleFill(float cx, float cy, float radius, @Nonnull Paint paint) {
        putRectColor(cx - radius, cy - radius, cx + radius, cy + radius, paint);
        ByteBuffer buffer = getUniformBuffer();
        // vec4
        buffer.putFloat(radius)
                .putFloat(Math.min(radius, paint.getSmoothRadius()));
        buffer.position(buffer.position() + 8); // padding
        // vec2
        buffer.putFloat(cx)
                .putFloat(cy);
        getMatrix().get(getModelViewBuffer());
        mDrawStates.add(DRAW_CIRCLE);
    }

    private void addCircleStroke(float cx, float cy, float radius, @Nonnull Paint paint) {
        float half = Math.min(paint.getStrokeWidth() * 0.5f, radius);
        float outer = radius + half;
        putRectColor(cx - outer, cy - outer, cx + outer, cy + outer, paint);
        ByteBuffer buffer = getUniformBuffer();
        buffer.putFloat(radius - half)
                .putFloat(outer)
                .putFloat(Math.min(half, paint.getSmoothRadius()));
        buffer.position(buffer.position() + 4); // padding
        buffer.putFloat(cx)
                .putFloat(cy);
        getMatrix().get(getModelViewBuffer());
        mDrawStates.add(DRAW_CIRCLE_OUTLINE);
    }

    @Override
    public void drawLine(float startX, float startY, float stopX, float stopY, @Nonnull Paint paint) {
        float t = paint.getStrokeWidth() * 0.5f;
        if (MathUtil.approxEqual(startX, stopX)) {
            if (MathUtil.approxEqual(startY, stopY)) {
                if (quickReject(startX - t, startY - t, startX + t, startY + t)) {
                    return;
                }
                addCircleFill(startX, startY, t, paint);
            } else {
                // vertical
                float top = Math.min(startY, stopY);
                float bottom = Math.max(startY, stopY);
                if (quickReject(startX - t, top - t, startX + t, bottom + t)) {
                    return;
                }
                addRoundRectFill(startX - t, top - t, startX + t, bottom + t, t, 0, paint);
            }
        } else if (MathUtil.approxEqual(startY, stopY)) {
            // horizontal
            float left = Math.min(startX, stopX);
            float right = Math.max(startX, stopX);
            if (quickReject(left - t, startY - t, right + t, startY + t)) {
                return;
            }
            addRoundRectFill(left - t, startY - t, right + t, startY + t, t, 0, paint);
        } else {
            float cx = (stopX + startX) * 0.5f;
            float cy = (stopY + startY) * 0.5f;
            float ang = MathUtil.atan2(stopY - startY, stopX - startX);
            save();
            Matrix4 mat = getMatrix();
            // rotate the round rect
            mat.translate(cx, cy, 0);
            mat.rotateZ(ang);
            mat.translate(-cx, -cy, 0);
            // rotate positions to horizontal
            float sin = MathUtil.sin(-ang);
            float cos = MathUtil.cos(-ang);
            float left = (startX - cx) * cos - (startY - cy) * sin + cx;
            float right = (stopX - cx) * cos - (stopY - cy) * sin + cx;
            if (!quickReject(left - t, cy - t, right + t, cy + t)) {
                addRoundRectFill(left - t, cy - t, right + t, cy + t, t, 0, paint);
            }
            restore();
        }
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom, @Nonnull Paint paint) {
        if (quickReject(left, top, right, bottom)) {
            return;
        }
        putRectColor(left, top, right, bottom, paint);
        getMatrix().get(getModelViewBuffer());
        mDrawStates.add(DRAW_RECT);
    }

    @Override
    public void drawImage(@Nonnull Image image, float left, float top, @Nonnull Paint paint) {
        Image.Source source = image.getSource();
        putRectColorUV(left, top, left + source.width, top + source.height, paint.getColor(),
                0, 0, 1, 1);
        mTextures.add(source.texture);
        getMatrix().get(getModelViewBuffer());
        mDrawStates.add(DRAW_IMAGE);
    }

    public void drawTextureMSAA(@Nonnull Texture texture, float l, float t, float r, float b, int color,
                                boolean flipY) {
        // flip vertical
        putRectColorUV(l, t, r, b, color,
                0, flipY ? 1 : 0, 1, flipY ? 0 : 1);
        mTextures.add(texture);
        getMatrix().get(getModelViewBuffer());
        mDrawStates.add(DRAW_IMAGE_MS);
    }

    @Override
    public void drawRoundRect(float left, float top, float right, float bottom, float radius,
                              int side, @Nonnull Paint paint) {
        if (quickReject(left, top, right, bottom)) {
            return;
        }
        if (radius < 0)
            radius = 0;
        if (paint.getStyle() != Paint.Style.STROKE) {
            addRoundRectFill(left, top, right, bottom, radius, side, paint);
        }
        if (paint.getStyle() != Paint.Style.FILL) {
            addRoundRectStroke(left, top, right, bottom, radius, side, paint);
        }
    }

    private void addRoundRectFill(float left, float top, float right, float bottom,
                                  float radius, int side, @Nonnull Paint paint) {
        float sm = Math.min(radius, paint.getSmoothRadius());
        putRectColor(left, top, right, bottom, paint);
        ByteBuffer buffer = getUniformBuffer();
        if ((side & RIGHT) == RIGHT) {
            buffer.putFloat(left);
        } else {
            buffer.putFloat(left + radius);
        }
        if ((side & BOTTOM) == BOTTOM) {
            buffer.putFloat(top);
        } else {
            buffer.putFloat(top + radius);
        }
        if ((side & LEFT) == LEFT) {
            buffer.putFloat(right);
        } else {
            buffer.putFloat(right - radius);
        }
        if ((side & TOP) == TOP) {
            buffer.putFloat(bottom);
        } else {
            buffer.putFloat(bottom - radius);
        }
        buffer.putFloat(radius)
                .putFloat(sm);
        buffer.position(buffer.position() + 4);
        getMatrix().get(getModelViewBuffer());
        mDrawStates.add(DRAW_ROUND_RECT);
    }

    private void addRoundRectStroke(float left, float top, float right, float bottom,
                                    float radius, int side, @Nonnull Paint paint) {
        float half = Math.min(paint.getStrokeWidth() * 0.5f, radius);
        float sm = Math.min(half, paint.getSmoothRadius());
        putRectColor(left - half, top - half, right + half, bottom + half, paint);
        ByteBuffer buffer = getUniformBuffer();
        if ((side & RIGHT) == RIGHT) {
            buffer.putFloat(left);
        } else {
            buffer.putFloat(left + radius);
        }
        if ((side & BOTTOM) == BOTTOM) {
            buffer.putFloat(top);
        } else {
            buffer.putFloat(top + radius);
        }
        if ((side & LEFT) == LEFT) {
            buffer.putFloat(right);
        } else {
            buffer.putFloat(right - radius);
        }
        if ((side & TOP) == TOP) {
            buffer.putFloat(bottom);
        } else {
            buffer.putFloat(bottom - radius);
        }
        buffer.putFloat(radius)
                .putFloat(sm)
                .putFloat(half);
        getMatrix().get(getModelViewBuffer());
        mDrawStates.add(DRAW_ROUND_RECT_OUTLINE);
    }

    @Override
    public void drawRoundImage(@Nonnull Image image, float left, float top, float radius, @Nonnull Paint paint) {
        Image.Source source = image.getSource();
        putRectColorUV(left, top, left + source.width, top + source.height, paint.getColor(),
                0, 0, 1, 1);
        if (radius < 0)
            radius = 0;
        ByteBuffer buffer = getUniformBuffer();
        buffer.putFloat(left + radius)
                .putFloat(top + radius)
                .putFloat(left + source.width - radius)
                .putFloat(top + source.height - radius);
        buffer.putFloat(radius)
                .putFloat(Math.min(radius, paint.getSmoothRadius()));
        buffer.position(buffer.position() + 4);
        mTextures.add(source.texture);
        getMatrix().get(getModelViewBuffer());
        mDrawStates.add(DRAW_ROUND_IMAGE);
    }

    @Deprecated
    @Override
    public void drawTextRun(@Nonnull CharSequence text, int start, int end, float x, float y,
                            boolean isRtl, @Nonnull TextPaint paint) {
        if ((start | end | end - start | text.length() - end) < 0) {
            throw new IndexOutOfBoundsException();
        }
        char[] chars = new char[end - start];
        TextUtils.getChars(text, start, end, chars, 0);
        LayoutPiece piece = LayoutCache.getOrCreate(chars, 0, end - start, isRtl, paint);
        addTextRun(piece, x, y, paint.getColor());
    }

    @Override
    public void drawTextRun(@Nonnull MeasuredText text, int start, int end, float x, float y,
                            @Nonnull TextPaint paint) {
        if ((start | end | end - start) < 0) {
            throw new IndexOutOfBoundsException();
        }
        LayoutPiece piece = text.getLayoutPiece(start, end);
        if (piece != null && piece.getAdvances().length != 0) {
            if (!quickReject(x, y - piece.getAscent(),
                    x + piece.getAdvance(), y + piece.getDescent())) {
                addTextRun(piece, x, y, paint.getColor());
            }
        }
    }

    private void addTextRun(@Nonnull LayoutPiece piece, float x, float y, int color) {
        mTextInfos.add(new TextInfo(piece, x, y));
        ByteBuffer buffer = getUniformBuffer();
        getMatrix().get(buffer);
        buffer.putFloat(((color >> 16) & 0xff) / 255f)
                .putFloat(((color >> 8) & 0xff) / 255f)
                .putFloat((color & 0xff) / 255f)
                .putFloat((color >>> 24) / 255f);
        mDrawStates.add(DRAW_TEXT);
    }

    private static class TextInfo {

        private final LayoutPiece mLayoutPiece;
        private final float mX;
        private final float mY;

        public TextInfo(LayoutPiece layoutPiece, float x, float y) {
            mLayoutPiece = layoutPiece;
            mX = x;
            mY = y;
        }
    }
}
