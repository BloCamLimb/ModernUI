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
import icyllis.modernui.graphics.shader.GLProgram;
import icyllis.modernui.graphics.shader.ShaderManager;
import icyllis.modernui.graphics.texture.GLTexture;
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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static icyllis.modernui.graphics.GLWrapper.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Modern OpenGL implementation to Canvas, handling multi-threaded rendering.
 * <p>
 * GLCanvas is highly integrated, you can't draw other things except those
 * defined in Canvas easily. This helps to build OpenGL buffers from UI thread,
 * using multiple vertex arrays, uniform buffers and vertex buffers. Later
 * calls OpenGL functions on the render thread.
 * <p>
 * All drawing methods are recording commands on UI thread (or synchronized),
 * and {@link #draw()} means calling OpenGL draw functions on the render thread.
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
     * Uniform block binding points (sequential)
     */
    public static final int MATRIX_BLOCK_BINDING = 0;

    /**
     * Vertex buffer binding points
     */
    public static final int GENERIC_BINDING = 0;

    /**
     * Vertex attributes
     */
    public static final VertexAttrib POS;
    public static final VertexAttrib COLOR;
    public static final VertexAttrib UV;

    /**
     * Vertex formats
     */
    public static final VertexFormat POS_COLOR;
    public static final VertexFormat POS_COLOR_TEX;
    public static final VertexFormat POS_TEX;

    /**
     * Shader programs
     */
    public static final GLProgram COLOR_FILL = new GLProgram();
    public static final GLProgram COLOR_TEX = new GLProgram();
    public static final GLProgram ROUND_RECT_FILL = new GLProgram();
    public static final GLProgram ROUND_RECT_TEX = new GLProgram();
    public static final GLProgram ROUND_RECT_STROKE = new GLProgram();
    public static final GLProgram CIRCLE_FILL = new GLProgram();
    public static final GLProgram CIRCLE_STROKE = new GLProgram();
    public static final GLProgram ARC_FILL = new GLProgram();
    public static final GLProgram ARC_STROKE = new GLProgram();
    public static final GLProgram BEZIER_CURVE = new GLProgram();
    public static final GLProgram ALPHA_TEX = new GLProgram();
    public static final GLProgram COLOR_TEX_MS = new GLProgram();

    /**
     * Recording draw operations (sequential)
     */
    public static final int DRAW_RECT = 1;
    public static final int DRAW_IMAGE = 2;
    public static final int DRAW_ROUND_RECT_FILL = 3;
    public static final int DRAW_ROUND_IMAGE = 4;
    public static final int DRAW_ROUND_RECT_STROKE = 5;
    public static final int DRAW_CIRCLE_FILL = 6;
    public static final int DRAW_CIRCLE_STROKE = 7;
    public static final int DRAW_ARC_FILL = 8;
    public static final int DRAW_ARC_STROKE = 9;
    public static final int DRAW_CLIP_PUSH = 10;
    public static final int DRAW_CLIP_POP = 11;
    public static final int DRAW_TEXT = 12;
    public static final int DRAW_MATRIX = 13;
    public static final int DRAW_SMOOTH = 14;
    public static final int DRAW_BEZIER = 15;
    public static final int DRAW_IMAGE_MS = 16;

    /**
     * Uniform block sizes (maximum), use std140 layout
     */
    public static final int MATRIX_UNIFORM_SIZE = 144;
    public static final int SMOOTH_UNIFORM_SIZE = 4;
    public static final int ARC_UNIFORM_SIZE = 24;
    public static final int BEZIER_UNIFORM_SIZE = 28;
    public static final int CIRCLE_UNIFORM_SIZE = 16;
    public static final int ROUND_RECT_UNIFORM_SIZE = 24;

    static {
        POS = new VertexAttrib(GENERIC_BINDING, VertexAttrib.Src.FLOAT, VertexAttrib.Dst.VEC2, false);
        COLOR = new VertexAttrib(GENERIC_BINDING, VertexAttrib.Src.UBYTE, VertexAttrib.Dst.VEC4, true);
        UV = new VertexAttrib(GENERIC_BINDING, VertexAttrib.Src.FLOAT, VertexAttrib.Dst.VEC2, false);
        //MODEL_VIEW = new VertexAttrib(INSTANCED_BINDING, VertexAttrib.Src.FLOAT, VertexAttrib.Dst.MAT4, false);
        POS_COLOR = new VertexFormat(POS, COLOR);
        POS_COLOR_TEX = new VertexFormat(POS, COLOR, UV);
        POS_TEX = new VertexFormat(POS, UV);
    }

    // private stacks
    private final Deque<Matrix4> mMatrixStack = new ArrayDeque<>();
    private final Deque<Clip> mClipStack = new ArrayDeque<>();

    // recorded operations
    private final IntList mDrawOps = new IntArrayList();

    // vertex buffer objects
    private final GLBuffer mPosColorVBO = new GLBuffer();
    private ByteBuffer mPosColorMemory = memAlloc(1024);
    private boolean mPosColorResized = true;

    private final GLBuffer mPosColorTexVBO = new GLBuffer();
    private ByteBuffer mPosColorTexMemory = memAlloc(1024);
    private boolean mPosColorTexResized = true;

    // dynamic update on render thread
    private final GLBuffer mPosTexVBO = new GLBuffer();
    private ByteBuffer mPosTexMemory = memAlloc(1024);
    private boolean mPosTexResized = true;

    /*private int mModelViewVBO = INVALID_ID;
    private ByteBuffer mModelViewData = memAlloc(1024);
    private boolean mRecreateModelView = true;*/

    // the client buffer used for updating the uniform blocks
    private ByteBuffer mUniformMemory = memAlloc(1024);

    // immutable uniform buffer objects
    private final GLBuffer mMatrixUBO = new GLBuffer();
    private final GLBuffer mSmoothUBO = new GLBuffer();
    private final GLBuffer mArcUBO = new GLBuffer();
    private final GLBuffer mBezierUBO = new GLBuffer();
    private final GLBuffer mCircleUBO = new GLBuffer();
    private final GLBuffer mRoundRectUBO = new GLBuffer();

    private final IntBuffer mUniformBuffers = memAllocInt(6);

    // used in rendering, local states
    private int mCurrTexture;
    private GLProgram mCurrProgram;
    private VertexFormat mCurrVertexFormat;

    private final Matrix4 mLastMatrix = new Matrix4();
    private float mLastSmoothRadius;

    // absolute value presents the reference value, and sign represents whether to
    // update the stencil buffer (positive = update, or just change stencil func)
    private final IntList mClipRefs = new IntArrayList();

    // using textures of draw states, in the order of calling
    private final List<GLTexture> mTextures = new ArrayList<>();
    private final List<DrawText> mDrawTexts = new ArrayList<>();

    private final Rect mTmpRect = new Rect();
    private final RectF mTmpRectF = new RectF();

    private final ByteBuffer mProjectionUpload = memAlloc(64);

    @RenderThread
    private GLCanvas() {
        /*mProjectionUBO = glCreateBuffers();
        glNamedBufferStorage(mProjectionUBO, PROJECTION_UNIFORM_SIZE, GL_DYNAMIC_STORAGE_BIT);

        mGlyphUBO = glCreateBuffers();
        glNamedBufferStorage(mGlyphUBO, GLYPH_UNIFORM_SIZE, GL_DYNAMIC_STORAGE_BIT);

        mRoundRectUBO = glCreateBuffers();
        glNamedBufferStorage(mRoundRectUBO, ROUND_RECT_UNIFORM_SIZE, GL_DYNAMIC_STORAGE_BIT);

        mCircleUBO = glCreateBuffers();
        glNamedBufferStorage(mCircleUBO, CIRCLE_UNIFORM_SIZE, GL_DYNAMIC_STORAGE_BIT);

        mArcUBO = glCreateBuffers();
        glNamedBufferStorage(mArcUBO, ARC_UNIFORM_SIZE, GL_DYNAMIC_STORAGE_BIT);*/

        mMatrixUBO.allocate(MATRIX_UNIFORM_SIZE, NULL, GL_DYNAMIC_STORAGE_BIT);
        mSmoothUBO.allocate(SMOOTH_UNIFORM_SIZE, NULL, GL_DYNAMIC_STORAGE_BIT);
        mArcUBO.allocate(ARC_UNIFORM_SIZE, NULL, GL_DYNAMIC_STORAGE_BIT);
        mBezierUBO.allocate(BEZIER_UNIFORM_SIZE, NULL, GL_DYNAMIC_STORAGE_BIT);
        mCircleUBO.allocate(CIRCLE_UNIFORM_SIZE, NULL, GL_DYNAMIC_STORAGE_BIT);
        mRoundRectUBO.allocate(ROUND_RECT_UNIFORM_SIZE, NULL, GL_DYNAMIC_STORAGE_BIT);

        mUniformBuffers.put(mMatrixUBO.get())
                .put(mSmoothUBO.get())
                .put(mArcUBO.get())
                .put(mBezierUBO.get())
                .put(mCircleUBO.get())
                .put(mRoundRectUBO.get())
                .flip();
        ModernUI.LOGGER.debug(MARKER,
                "Uniform buffers: Matrix {}, Smooth {}, Arc {}, Bezier {}, Circle {}, RoundRect {}",
                mUniformBuffers.get(0), mUniformBuffers.get(1), mUniformBuffers.get(2),
                mUniformBuffers.get(3), mUniformBuffers.get(4), mUniformBuffers.get(5));

        POS_COLOR.setVertexBuffer(GENERIC_BINDING, mPosColorVBO, 0);
        POS_COLOR_TEX.setVertexBuffer(GENERIC_BINDING, mPosColorTexVBO, 0);
        POS_TEX.setVertexBuffer(GENERIC_BINDING, mPosTexVBO, 0);

        ModernUI.LOGGER.debug(MARKER,
                "Vertex buffers: PosColor {}, PosColorTex {}, PosTex(Glyph) {}",
                mPosColorVBO.get(), mPosColorTexVBO.get(), mPosTexVBO.get());

        mMatrixStack.push(Matrix4.identity());
        mClipStack.push(new Clip());

        ShaderManager.getInstance().addListener(this::onLoadShaders);
    }

    @RenderThread
    public static GLCanvas initialize() {
        RenderCore.checkRenderThread();
        if (INSTANCE == null) {
            INSTANCE = new GLCanvas();
            /*POS_COLOR.setBindingDivisor(INSTANCED_BINDING, 1);
            POS_COLOR_TEX.setBindingDivisor(INSTANCED_BINDING, 1);*/
            ModernUI.LOGGER.info(MARKER, "GLCanvas initialized");
        }
        return INSTANCE;
    }

    /**
     * Exposed for internal use, be aware of the thread-safety and client-controlled GL states.
     *
     * @return the global instance
     */
    public static GLCanvas getInstance() {
        return INSTANCE;
    }

    private void onLoadShaders(@Nonnull ShaderManager manager) {
        int posColor = manager.getShard(ModernUI.ID, "pos_color.vert");
        int posColorTex = manager.getShard(ModernUI.ID, "pos_color_tex.vert");
        int posTex = manager.getShard(ModernUI.ID, "pos_tex.vert");

        int colorFill = manager.getShard(ModernUI.ID, "color_fill.frag");
        int colorTex = manager.getShard(ModernUI.ID, "color_tex.frag");
        int roundRectFill = manager.getShard(ModernUI.ID, "round_rect_fill.frag");
        int roundRectTex = manager.getShard(ModernUI.ID, "round_rect_tex.frag");
        int roundRectStroke = manager.getShard(ModernUI.ID, "round_rect_stroke.frag");
        int circleFill = manager.getShard(ModernUI.ID, "circle_fill.frag");
        int circleStroke = manager.getShard(ModernUI.ID, "circle_stroke.frag");
        int arcFill = manager.getShard(ModernUI.ID, "arc_fill.frag");
        int arcStroke = manager.getShard(ModernUI.ID, "arc_stroke.frag");
        int bezierCurve = manager.getShard(ModernUI.ID, "bezier_curve.frag");
        int alphaTex = manager.getShard(ModernUI.ID, "alpha_tex.frag");
        int colorTexMs = manager.getShard(ModernUI.ID, "color_tex_4x.frag");

        manager.create(COLOR_FILL, posColor, colorFill);
        manager.create(COLOR_TEX, posColorTex, colorTex);
        manager.create(ROUND_RECT_FILL, posColor, roundRectFill);
        manager.create(ROUND_RECT_TEX, posColorTex, roundRectTex);
        manager.create(ROUND_RECT_STROKE, posColor, roundRectStroke);
        manager.create(CIRCLE_FILL, posColor, circleFill);
        manager.create(CIRCLE_STROKE, posColor, circleStroke);
        manager.create(ARC_FILL, posColor, arcFill);
        manager.create(ARC_STROKE, posColor, arcStroke);
        manager.create(BEZIER_CURVE, posColor, bezierCurve);
        manager.create(ALPHA_TEX, posTex, alphaTex);
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
        projection.get(mProjectionUpload);
        mMatrixUBO.upload(0, mProjectionUpload.flip());
    }

    /**
     * Resets the clip bounds and matrix. This is required before drawing.
     *
     * @param width  the width in pixels
     * @param height the height in pixels
     */
    public void reset(int width, int height) {
        getMatrix().setIdentity();
        Clip clip = getClip();
        clip.mBounds.set(0, 0, width, height);
        clip.mRefVal = 0;
        mLastMatrix.setZero();
        mLastSmoothRadius = -1;
    }

    private void bindVertexArray(@Nonnull VertexFormat format) {
        if (mCurrVertexFormat != format) {
            glBindVertexArray(format.getVertexArray());
            mCurrVertexFormat = format;
        }
    }

    private void useProgram(@Nonnull GLProgram program) {
        if (mCurrProgram != program) {
            glUseProgram(program.get());
            mCurrProgram = program;
        }
    }

    private void bindTexture(int texture) {
        if (mCurrTexture != texture) {
            glBindTextureUnit(0, texture);
            mCurrTexture = texture;
        }
    }

    @RenderThread
    public void draw() {
        RenderCore.checkRenderThread();
        RenderCore.flushRenderCalls();
        if (mDrawOps.isEmpty()) {
            return;
        }
        if (getSaveCount() != 1) {
            throw new IllegalStateException("Unbalanced save-restore pair " + getSaveCount());
        }
        uploadBuffers();

        // uniform bindings are globally shared, we must re-bind before we use them
        glBindBuffersBase(GL_UNIFORM_BUFFER, MATRIX_BLOCK_BINDING, mUniformBuffers);

        glStencilFuncSeparate(GL_FRONT, GL_EQUAL, 0, 0xff);
        glStencilMaskSeparate(GL_FRONT, 0xff);

        mCurrVertexFormat = null;
        mCurrProgram = null;
        mCurrTexture = 0;

        long uniformDataPtr = memAddress(mUniformMemory.flip());

        // generic array index
        int posColorIndex = 0;
        int posColorTexIndex = 0;
        // textures
        int textureIndex = 0;
        int clipIndex = 0;
        int textIndex = 0;

        for (int op : mDrawOps) {
            switch (op) {
                case DRAW_RECT:
                    bindVertexArray(POS_COLOR);
                    useProgram(COLOR_FILL);
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    posColorIndex += 4;
                    break;

                case DRAW_ROUND_RECT_FILL:
                    bindVertexArray(POS_COLOR);
                    useProgram(ROUND_RECT_FILL);
                    mRoundRectUBO.upload(0, 20, uniformDataPtr);
                    uniformDataPtr += 20;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    posColorIndex += 4;
                    break;

                case DRAW_ROUND_RECT_STROKE:
                    bindVertexArray(POS_COLOR);
                    useProgram(ROUND_RECT_STROKE);
                    mRoundRectUBO.upload(0, 24, uniformDataPtr);
                    uniformDataPtr += 24;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    posColorIndex += 4;
                    break;

                case DRAW_ROUND_IMAGE:
                    bindVertexArray(POS_COLOR_TEX);
                    useProgram(ROUND_RECT_TEX);
                    bindTexture(mTextures.get(textureIndex++).get());
                    mRoundRectUBO.upload(0, 20, uniformDataPtr);
                    uniformDataPtr += 20;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorTexIndex, 4);
                    posColorTexIndex += 4;
                    break;

                case DRAW_IMAGE:
                    bindVertexArray(POS_COLOR_TEX);
                    useProgram(COLOR_TEX);
                    bindTexture(mTextures.get(textureIndex++).get());
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorTexIndex, 4);
                    posColorTexIndex += 4;
                    break;

                case DRAW_IMAGE_MS:
                    bindVertexArray(POS_COLOR_TEX);
                    useProgram(COLOR_TEX_MS);
                    bindTexture(mTextures.get(textureIndex++).get());
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorTexIndex, 4);
                    posColorTexIndex += 4;
                    break;

                case DRAW_CIRCLE_FILL:
                    bindVertexArray(POS_COLOR);
                    useProgram(CIRCLE_FILL);
                    mCircleUBO.upload(0, 12, uniformDataPtr);
                    uniformDataPtr += 12;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    posColorIndex += 4;
                    break;

                case DRAW_CIRCLE_STROKE:
                    bindVertexArray(POS_COLOR);
                    useProgram(CIRCLE_STROKE);
                    mCircleUBO.upload(0, 16, uniformDataPtr);
                    uniformDataPtr += 16;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    posColorIndex += 4;
                    break;

                case DRAW_ARC_FILL:
                    bindVertexArray(POS_COLOR);
                    useProgram(ARC_FILL);
                    mArcUBO.upload(0, 20, uniformDataPtr);
                    uniformDataPtr += 20;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    posColorIndex += 4;
                    break;

                case DRAW_ARC_STROKE:
                    bindVertexArray(POS_COLOR);
                    useProgram(ARC_STROKE);
                    mArcUBO.upload(0, 24, uniformDataPtr);
                    uniformDataPtr += 24;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    posColorIndex += 4;
                    break;

                case DRAW_BEZIER:
                    bindVertexArray(POS_COLOR);
                    useProgram(BEZIER_CURVE);
                    mBezierUBO.upload(0, 28, uniformDataPtr);
                    uniformDataPtr += 28;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    posColorIndex += 4;
                    break;

                case DRAW_CLIP_PUSH: {
                    int clipRef = mClipRefs.getInt(clipIndex);

                    if (clipRef >= 0) {
                        glStencilOpSeparate(GL_FRONT, GL_KEEP, GL_KEEP, GL_INCR);
                        glColorMaski(0, false, false, false, false);

                        bindVertexArray(POS_COLOR);
                        useProgram(COLOR_FILL);
                        glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                        posColorIndex += 4;

                        glStencilOpSeparate(GL_FRONT, GL_KEEP, GL_KEEP, GL_KEEP);
                        glColorMaski(0, true, true, true, true);
                    }

                    glStencilFuncSeparate(GL_FRONT, GL_EQUAL, Math.abs(clipRef), 0xff);
                    clipIndex++;
                    break;
                }

                case DRAW_CLIP_POP: {
                    int clipRef = mClipRefs.getInt(clipIndex);

                    if (clipRef >= 0) {
                        glStencilFuncSeparate(GL_FRONT, GL_LESS, clipRef, 0xff);
                        glStencilOpSeparate(GL_FRONT, GL_KEEP, GL_KEEP, GL_REPLACE);
                        glColorMaski(0, false, false, false, false);

                        bindVertexArray(POS_COLOR);
                        useProgram(COLOR_FILL);
                        glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                        posColorIndex += 4;

                        glStencilOpSeparate(GL_FRONT, GL_KEEP, GL_KEEP, GL_KEEP);
                        glColorMaski(0, true, true, true, true);
                    }

                    glStencilFuncSeparate(GL_FRONT, GL_EQUAL, Math.abs(clipRef), 0xff);
                    clipIndex++;
                    break;
                }

                case DRAW_TEXT: {
                    bindVertexArray(POS_TEX);
                    useProgram(ALPHA_TEX);
                    mMatrixUBO.upload(128, 16, uniformDataPtr);
                    uniformDataPtr += 16;

                    final TexturedGlyph[] glyphs = mDrawTexts.get(textIndex++).buildBuffer(this);

                    if (mPosTexResized) {
                        mPosTexVBO.allocateM(mPosTexMemory.capacity(), NULL, GL_DYNAMIC_DRAW);
                        mPosTexResized = false;
                    }
                    mPosTexVBO.upload(0, mPosTexMemory.flip());
                    mPosTexMemory.clear();

                    for (int i = 0, e = glyphs.length; i < e; i++) {
                        bindTexture(glyphs[i].texture);
                        glDrawArrays(GL_TRIANGLE_STRIP, i << 2, 4);
                    }
                    break;
                }

                case DRAW_MATRIX:
                    mMatrixUBO.upload(64, 64, uniformDataPtr);
                    uniformDataPtr += 64;
                    break;

                case DRAW_SMOOTH:
                    mSmoothUBO.upload(0, 4, uniformDataPtr);
                    uniformDataPtr += 4;
                    break;

                default:
                    throw new IllegalStateException("Unexpected draw state " + op);
            }
        }

        mDrawOps.clear();
        mClipRefs.clear();
        mTextures.clear();
        mDrawTexts.clear();
        mUniformMemory.clear();
    }

    @RenderThread
    private void uploadBuffers() {
        if (mPosColorResized) {
            mPosColorVBO.allocateM(mPosColorMemory.capacity(), NULL, GL_DYNAMIC_DRAW);
            mPosColorResized = false;
        }
        mPosColorVBO.upload(0, mPosColorMemory.flip());
        mPosColorMemory.clear();

        if (mPosColorTexResized) {
            mPosColorTexVBO.allocateM(mPosColorTexMemory.capacity(), NULL, GL_DYNAMIC_DRAW);
            mPosColorTexResized = false;
        }
        mPosColorTexVBO.upload(0, mPosColorTexMemory.flip());
        mPosColorTexMemory.clear();

        /*checkModelViewVBO();
        mModelViewData.flip();
        glNamedBufferSubData(mModelViewVBO, 0, mModelViewData);
        mModelViewData.clear();*/
    }

    /*@RenderThread
    private void checkModelViewVBO() {
        if (!mRecreateModelView)
            return;
        mModelViewVBO = glCreateBuffers();
        glNamedBufferStorage(mModelViewVBO, mModelViewData.capacity(), GL_DYNAMIC_STORAGE_BIT);
        // configure
        POS_COLOR.setVertexBuffer(INSTANCED_BINDING, mModelViewVBO, 0);
        POS_COLOR_TEX.setVertexBuffer(INSTANCED_BINDING, mModelViewVBO, 0);
        mRecreateModelView = false;
    }*/

    private ByteBuffer checkPosColorMemory() {
        if (mPosColorMemory.remaining() < 48) {
            int cap = mPosColorMemory.capacity();
            cap = cap + (cap >> 1);
            mPosColorMemory = memRealloc(mPosColorMemory, cap);
            mPosColorResized = true;
            ModernUI.LOGGER.debug(MARKER, "Grow pos color buffer to {} bytes", cap);
        }
        return mPosColorMemory;
    }

    private ByteBuffer checkPosColorTexMemory() {
        if (mPosColorTexMemory.remaining() < 80) {
            int cap = mPosColorTexMemory.capacity();
            cap = cap + (cap >> 1);
            mPosColorTexMemory = memRealloc(mPosColorTexMemory, cap);
            mPosColorTexResized = true;
            ModernUI.LOGGER.debug(MARKER, "Grow pos color tex buffer to {} bytes", cap);
        }
        return mPosColorTexMemory;
    }

    /*private ByteBuffer getModelViewBuffer() {
        if (mModelViewData.remaining() < 64) {
            mModelViewData = memRealloc(mModelViewData, mModelViewData.capacity() << 1);
            mRecreateModelView = true;
            ModernUI.LOGGER.debug(MARKER, "Resize model view buffer to {} bytes", mModelViewData.capacity());
        }
        return mModelViewData;
    }*/

    @RenderThread
    private ByteBuffer checkPosTexMemory() {
        if (mPosTexMemory.remaining() < 64) {
            int cap = mPosTexMemory.capacity();
            cap = cap + (cap >> 1);
            mPosTexMemory = memRealloc(mPosTexMemory, cap);
            mPosTexResized = true;
            ModernUI.LOGGER.debug(MARKER, "Grow pos tex buffer to {} bytes", cap);
        }
        return mPosTexMemory;
    }

    private ByteBuffer checkUniformMemory() {
        if (mUniformMemory.remaining() < 64) {
            int cap = mUniformMemory.capacity();
            cap = cap + (cap >> 1);
            mUniformMemory = memRealloc(mUniformMemory, cap);
            ModernUI.LOGGER.debug(MARKER, "Grow general uniform buffer to {} bytes", cap);
        }
        return mUniformMemory;
    }

    /**
     * Draw matrix. Update the model view matrix on render thread.
     */
    private void drawMatrix() {
        drawMatrix(getMatrix());
    }

    /**
     * Draw matrix. Update the model view matrix on render thread.
     */
    private void drawMatrix(@Nonnull Matrix4 matrix) {
        if (!matrix.equivalent(mLastMatrix)) {
            mLastMatrix.set(matrix);
            matrix.get(checkUniformMemory());
            mDrawOps.add(DRAW_MATRIX);
        }
    }

    /**
     * Gets the matrix for modification purposes.
     *
     * @return current model view matrix
     */
    @Nonnull
    @Override
    public Matrix4 getMatrix() {
        return mMatrixStack.getFirst();
    }

    @Nonnull
    private Clip getClip() {
        return mClipStack.getFirst();
    }

    /**
     * Saves the current matrix and clip onto a private stack.
     * <p>
     * Subsequent calls to translate,scale,rotate,skew,concat or clipRect,
     * clipPath will all operate as usual, but when the balancing call to
     * restore() is made, those calls will be forgotten, and the settings that
     * existed before the save() will be reinstated.
     *
     * @return The value to pass to restoreToCount() to balance this save()
     */
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

    /**
     * This call balances a previous call to save(), and is used to remove all
     * modifications to the matrix/clip state since the last save call. It is
     * an error to call restore() more or less times than save() was called in
     * the final state.
     * <p>
     * If current clip doesn't change, it won't generate overhead for modifying
     * the stencil buffer.
     */
    @Override
    public void restore() {
        if (mMatrixStack.size() < 1) {
            throw new IllegalStateException("Underflow in restore");
        }
        sMatrixPool.release(mMatrixStack.pop());
        Clip clip = mClipStack.pop();
        if (clip.mRefVal != getClip().mRefVal) {
            restoreClip(clip.mBounds);
        }
        sClipPool.release(clip);
    }

    /**
     * Returns the number of matrix/clip states on the Canvas' private stack.
     * This will equal # save() calls - # restore() calls. The initial and
     * minimum value are 1.
     */
    @Override
    public int getSaveCount() {
        return mMatrixStack.size();
    }

    /**
     * Efficient way to pop any calls to save() that happened after the save
     * count reached saveCount. It is an error for saveCount to be less than 1.
     * <p>
     * Example:
     * <pre>
     * int count = canvas.save();
     * ... // more calls potentially to save()
     * canvas.restoreToCount(count);
     * // now the canvas is back in the same state it
     * // was before the initial call to save().
     * </pre>
     *
     * @param saveCount The save level to restore to.
     */
    @Override
    public void restoreToCount(int saveCount) {
        if (saveCount < 1) {
            throw new IllegalArgumentException("Underflow in restoreToCount");
        }
        Deque<Matrix4> stack = mMatrixStack;

        sMatrixPool.release(stack.pop());
        Clip clip = mClipStack.pop();
        final int topRef = clip.mRefVal;
        sClipPool.release(clip);

        while (stack.size() > saveCount) {
            sMatrixPool.release(stack.pop());
            clip = mClipStack.pop();
            sClipPool.release(clip);
        }

        if (topRef != getClip().mRefVal) {
            restoreClip(clip.mBounds);
        }
    }

    /**
     * @param b bounds
     * @see #clipRect(float, float, float, float)
     */
    private void restoreClip(@Nonnull Rect b) {
        if (b.isEmpty()) {
            // taking the opposite number means that the clip rect is not to drawn
            mClipRefs.add(-getClip().mRefVal);
        } else {
            drawMatrix(IDENTITY_MAT);
            // must have a color
            putRectColor(b.left, b.top, b.right, b.bottom, ~0);
            mClipRefs.add(getClip().mRefVal);
        }
        mDrawOps.add(DRAW_CLIP_POP);
    }

    private static final class Clip {

        // the maximum bounds transformed by model view matrix
        private final Rect mBounds = new Rect();

        // stencil reference
        private int mRefVal;

        private void set(@Nonnull Clip c) {
            mBounds.set(c.mBounds);
            mRefVal = c.mRefVal;
        }

        // deep copy
        @Nonnull
        private Clip copy() {
            Clip c = new Clip();
            c.set(this);
            return c;
        }
    }

    @Override
    public boolean clipRect(float left, float top, float right, float bottom) {
        final Clip clip = getClip();
        // empty rect, ignore it
        if (right <= left || bottom <= top) {
            return !clip.mBounds.isEmpty();
        }
        // already empty, return false
        if (clip.mBounds.isEmpty()) {
            return false;
        }
        RectF temp = mTmpRectF;
        temp.set(left, top, right, bottom);
        getMatrix().transform(temp);

        Rect test = mTmpRect;
        temp.roundOut(test);

        // not empty and not changed, return true
        if (test.contains(clip.mBounds)) {
            return true;
        }

        boolean intersects = clip.mBounds.intersect(test);
        clip.mRefVal++;
        if (intersects) {
            drawMatrix();
            // updating stencil must have a color
            putRectColor(left, top, right, bottom, ~0);
            mClipRefs.add(clip.mRefVal);
        } else {
            // empty
            mClipRefs.add(-clip.mRefVal);
            clip.mBounds.setEmpty();
        }
        mDrawOps.add(DRAW_CLIP_PUSH);
        return intersects;
    }

    @Override
    public boolean quickReject(float left, float top, float right, float bottom) {
        // empty rect, always reject
        if (right <= left || bottom <= top) {
            return true;
        }
        final Rect clip = getClip().mBounds;
        // already empty, reject
        if (clip.isEmpty()) {
            return true;
        }

        RectF temp = mTmpRectF;
        temp.set(left, top, right, bottom);
        getMatrix().transform(temp);

        Rect test = mTmpRect;
        temp.roundOut(test);
        return !Rect.intersects(clip, test);
    }

    private void putRectColor(float left, float top, float right, float bottom, @Nonnull Paint paint) {
        if (paint.isMultiColor()) {
            final ByteBuffer buffer = checkPosColorMemory();
            final int[] colors = paint.getColors();

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
        ByteBuffer buffer = checkPosColorMemory();
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

    private void putRectColorUV(float left, float top, float right, float bottom, @Nonnull Paint paint,
                                float u1, float v1, float u2, float v2) {
        if (paint.isMultiColor()) {
            final ByteBuffer buffer = checkPosColorTexMemory();
            final int[] colors = paint.getColors();

            // CCW
            int color = colors[3];
            byte r = (byte) ((color >> 16) & 0xff);
            byte g = (byte) ((color >> 8) & 0xff);
            byte b = (byte) (color & 0xff);
            byte a = (byte) (color >>> 24);
            buffer.putFloat(left)
                    .putFloat(bottom)
                    .put(r).put(g).put(b).put(a)
                    .putFloat(u1).putFloat(v2);

            color = colors[2];
            r = (byte) ((color >> 16) & 0xff);
            g = (byte) ((color >> 8) & 0xff);
            b = (byte) (color & 0xff);
            a = (byte) (color >>> 24);
            buffer.putFloat(right)
                    .putFloat(bottom)
                    .put(r).put(g).put(b).put(a)
                    .putFloat(u2).putFloat(v2);

            color = colors[0];
            r = (byte) ((color >> 16) & 0xff);
            g = (byte) ((color >> 8) & 0xff);
            b = (byte) (color & 0xff);
            a = (byte) (color >>> 24);
            buffer.putFloat(left)
                    .putFloat(top)
                    .put(r).put(g).put(b).put(a)
                    .putFloat(u1).putFloat(v1);

            color = colors[1];
            r = (byte) ((color >> 16) & 0xff);
            g = (byte) ((color >> 8) & 0xff);
            b = (byte) (color & 0xff);
            a = (byte) (color >>> 24);
            buffer.putFloat(right)
                    .putFloat(top)
                    .put(r).put(g).put(b).put(a)
                    .putFloat(u2).putFloat(v1);
        } else {
            putRectColorUV(left, top, right, bottom, paint.getColor(), u1, v1, u2, v2);
        }
    }

    private void putRectColorUV(float left, float top, float right, float bottom, int color,
                                float u1, float v1, float u2, float v2) {
        ByteBuffer buffer = checkPosColorTexMemory();
        byte r = (byte) ((color >> 16) & 0xff);
        byte g = (byte) ((color >> 8) & 0xff);
        byte b = (byte) (color & 0xff);
        byte a = (byte) (color >>> 24);
        buffer.putFloat(left)
                .putFloat(bottom)
                .put(r).put(g).put(b).put(a)
                .putFloat(u1).putFloat(v2);
        buffer.putFloat(right)
                .putFloat(bottom)
                .put(r).put(g).put(b).put(a)
                .putFloat(u2).putFloat(v2);
        buffer.putFloat(left)
                .putFloat(top)
                .put(r).put(g).put(b).put(a)
                .putFloat(u1).putFloat(v1);
        buffer.putFloat(right)
                .putFloat(top)
                .put(r).put(g).put(b).put(a)
                .putFloat(u2).putFloat(v1);
    }

    @RenderThread
    private void putGlyph(@Nonnull TexturedGlyph glyph, float left, float top) {
        ByteBuffer buffer = checkPosTexMemory();
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

    private void drawSmooth(float smooth) {
        if (smooth != mLastSmoothRadius) {
            mLastSmoothRadius = smooth;
            checkUniformMemory()
                    .putFloat(smooth);
            mDrawOps.add(DRAW_SMOOTH);
        }
    }

    @Override
    public void drawArc(float cx, float cy, float radius, float startAngle,
                        float sweepAngle, @Nonnull Paint paint) {
        if (MathUtil.approxZero(sweepAngle) || radius < 0.0001f) {
            return;
        }
        if (sweepAngle >= 360) {
            drawCircle(cx, cy, radius, paint);
            return;
        }
        sweepAngle %= 360;
        final float middleAngle = (startAngle % 360) + sweepAngle * 0.5f;
        if (paint.getStyle() == Paint.Style.FILL) {
            drawArcFill(cx, cy, radius, middleAngle, sweepAngle, paint);
        } else {
            drawArcStroke(cx, cy, radius, middleAngle, sweepAngle, paint);
        }
    }

    private void drawArcFill(float cx, float cy, float radius, float middleAngle,
                             float sweepAngle, @Nonnull Paint paint) {
        if (quickReject(cx - radius, cy - radius, cx + radius, cy + radius)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(radius, paint.getSmoothRadius()));
        putRectColor(cx - radius, cy - radius, cx + radius, cy + radius, paint);
        checkUniformMemory()
                .putFloat(cx)
                .putFloat(cy)
                .putFloat(middleAngle)
                .putFloat(sweepAngle)
                .putFloat(radius);
        mDrawOps.add(DRAW_ARC_FILL);
    }

    private void drawArcStroke(float cx, float cy, float radius, float middleAngle,
                               float sweepAngle, @Nonnull Paint paint) {
        float strokeRadius = Math.min(radius, paint.getStrokeWidth() * 0.5f);
        if (strokeRadius < 0.0001f) {
            return;
        }
        float maxRadius = radius + strokeRadius;
        if (quickReject(cx - maxRadius, cy - maxRadius, cx + maxRadius, cy + maxRadius)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(strokeRadius, paint.getSmoothRadius()));
        putRectColor(cx - maxRadius, cy - maxRadius, cx + maxRadius, cy + maxRadius, paint);
        checkUniformMemory()
                .putFloat(cx)
                .putFloat(cy)
                .putFloat(middleAngle)
                .putFloat(sweepAngle)
                .putFloat(radius)
                .putFloat(strokeRadius);
        mDrawOps.add(DRAW_ARC_STROKE);
    }

    @Override
    public void drawBezier(float x0, float y0, float x1, float y1, float x2, float y2, @Nonnull Paint paint) {
        float strokeRadius = paint.getStrokeWidth() * 0.5f;
        if (strokeRadius < 0.0001f) {
            return;
        }
        float left = Math.min(Math.min(x0, x1), x2) - strokeRadius;
        float top = Math.min(Math.min(y0, y1), y2) - strokeRadius;
        float right = Math.max(Math.max(x0, x1), x2) + strokeRadius;
        float bottom = Math.max(Math.max(y0, y1), y2) + strokeRadius;
        if (quickReject(left, top, right, bottom)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(strokeRadius, paint.getSmoothRadius()));
        putRectColor(left, top, right, bottom, paint);
        checkUniformMemory()
                .putFloat(x0)
                .putFloat(y0)
                .putFloat(x1)
                .putFloat(y1)
                .putFloat(x2)
                .putFloat(y2)
                .putFloat(strokeRadius);
        mDrawOps.add(DRAW_BEZIER);
    }

    @Override
    public void drawCircle(float cx, float cy, float radius, @Nonnull Paint paint) {
        if (radius < 0.0001f) {
            return;
        }
        if (paint.getStyle() == Paint.Style.FILL) {
            drawCircleFill(cx, cy, radius, paint);
        } else {
            drawCircleStroke(cx, cy, radius, paint);
        }
    }

    private void drawCircleFill(float cx, float cy, float radius, @Nonnull Paint paint) {
        if (quickReject(cx - radius, cy - radius, cx + radius, cy + radius)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(radius, paint.getSmoothRadius()));
        putRectColor(cx - radius, cy - radius, cx + radius, cy + radius, paint);
        checkUniformMemory()
                .putFloat(cx)
                .putFloat(cy)
                .putFloat(radius);
        mDrawOps.add(DRAW_CIRCLE_FILL);
    }

    private void drawCircleStroke(float cx, float cy, float radius, @Nonnull Paint paint) {
        float strokeRadius = Math.min(radius, paint.getStrokeWidth() * 0.5f);
        if (strokeRadius < 0.0001f) {
            return;
        }
        float maxRadius = radius + strokeRadius;
        if (quickReject(cx - maxRadius, cy - maxRadius, cx + maxRadius, cy + maxRadius)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(strokeRadius, paint.getSmoothRadius()));
        putRectColor(cx - maxRadius, cy - maxRadius, cx + maxRadius, cy + maxRadius, paint);
        checkUniformMemory()
                .putFloat(cx)
                .putFloat(cy)
                .putFloat(radius - strokeRadius) // inner radius
                .putFloat(maxRadius);
        mDrawOps.add(DRAW_CIRCLE_STROKE);
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom, @Nonnull Paint paint) {
        if (quickReject(left, top, right, bottom)) {
            return;
        }
        drawMatrix();
        putRectColor(left, top, right, bottom, paint);
        mDrawOps.add(DRAW_RECT);
    }

    @Override
    public void drawImage(@Nonnull Image image, float left, float top, @Nonnull Paint paint) {
        GLTexture texture = image.getTexture();
        float right = left + texture.getWidth();
        float bottom = top + texture.getHeight();
        if (quickReject(left, top, right, bottom)) {
            return;
        }
        drawMatrix();
        putRectColorUV(left, top, right, bottom, paint, 0, 0, 1, 1);
        mTextures.add(texture);
        mDrawOps.add(DRAW_IMAGE);
    }

    @Override
    public void drawImage(@Nonnull Image image, float srcLeft, float srcTop, float srcRight, float srcBottom,
                          float dstLeft, float dstTop, float dstRight, float dstBottom, @Nonnull Paint paint) {
        if (quickReject(dstLeft, dstTop, dstRight, dstBottom)) {
            return;
        }
        GLTexture texture = image.getTexture();
        srcLeft = Math.max(0, srcLeft);
        srcTop = Math.max(0, srcTop);
        int w = texture.getWidth();
        int h = texture.getHeight();
        srcRight = Math.min(srcRight, w);
        srcBottom = Math.min(srcBottom, h);
        if (srcRight <= srcLeft || srcBottom <= srcTop) {
            return;
        }
        drawMatrix();
        putRectColorUV(dstLeft, dstTop, dstRight, dstBottom, paint,
                srcLeft / w, srcTop / h, srcRight / w, srcBottom / h);
        mTextures.add(texture);
        mDrawOps.add(DRAW_IMAGE);
    }

    //FIXME Going to use Framebuffer blit
    public void drawTextureMSAA(@Nonnull GLTexture texture, float l, float t, float r, float b, int color,
                                boolean flipY) {
        drawMatrix();
        putRectColorUV(l, t, r, b, color,
                0, flipY ? 1 : 0, 1, flipY ? 0 : 1);
        mTextures.add(texture);
        mDrawOps.add(DRAW_IMAGE_MS);
    }

    @Override
    public void drawRoundLine(float startX, float startY, float stopX, float stopY, @Nonnull Paint paint) {
        float t = paint.getStrokeWidth() * 0.5f;
        if (t < 0.0001f) {
            return;
        }
        if (MathUtil.approxEqual(startX, stopX)) {
            if (MathUtil.approxEqual(startY, stopY)) {
                drawCircleFill(startX, startY, t, paint);
            } else {
                // vertical
                float top = Math.min(startY, stopY);
                float bottom = Math.max(startY, stopY);
                drawRoundRectFill(startX - t, top - t, startX + t, bottom + t, t, 0, paint);
            }
        } else if (MathUtil.approxEqual(startY, stopY)) {
            // horizontal
            float left = Math.min(startX, stopX);
            float right = Math.max(startX, stopX);
            drawRoundRectFill(left - t, startY - t, right + t, startY + t, t, 0, paint);
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
            drawRoundRectFill(left - t, cy - t, right + t, cy + t, t, 0, paint);
            restore();
        }
    }

    @Override
    public void drawRoundRect(float left, float top, float right, float bottom, float radius,
                              int sides, @Nonnull Paint paint) {
        if (radius < 0) {
            radius = 0;
        }
        if (paint.getStyle() == Paint.Style.FILL) {
            drawRoundRectFill(left, top, right, bottom, radius, sides, paint);
        } else {
            drawRoundRectStroke(left, top, right, bottom, radius, sides, paint);
        }
    }

    private void drawRoundRectFill(float left, float top, float right, float bottom,
                                   float radius, int sides, @Nonnull Paint paint) {
        if (quickReject(left, top, right, bottom)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(radius, paint.getSmoothRadius()));
        putRectColor(left, top, right, bottom, paint);
        ByteBuffer buffer = checkUniformMemory();
        if ((sides & RIGHT) == RIGHT) {
            buffer.putFloat(left);
        } else {
            buffer.putFloat(left + radius);
        }
        if ((sides & BOTTOM) == BOTTOM) {
            buffer.putFloat(top);
        } else {
            buffer.putFloat(top + radius);
        }
        if ((sides & LEFT) == LEFT) {
            buffer.putFloat(right);
        } else {
            buffer.putFloat(right - radius);
        }
        if ((sides & TOP) == TOP) {
            buffer.putFloat(bottom);
        } else {
            buffer.putFloat(bottom - radius);
        }
        buffer.putFloat(radius);
        mDrawOps.add(DRAW_ROUND_RECT_FILL);
    }

    private void drawRoundRectStroke(float left, float top, float right, float bottom,
                                     float radius, int sides, @Nonnull Paint paint) {
        float strokeRadius = Math.min(radius, paint.getStrokeWidth() * 0.5f);
        if (strokeRadius < 0.0001f) {
            return;
        }
        if (quickReject(left - strokeRadius, top - strokeRadius, right + strokeRadius, bottom + strokeRadius)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(strokeRadius, paint.getSmoothRadius()));
        putRectColor(left - strokeRadius, top - strokeRadius, right + strokeRadius, bottom + strokeRadius, paint);
        ByteBuffer buffer = checkUniformMemory();
        if ((sides & RIGHT) == RIGHT) {
            buffer.putFloat(left);
        } else {
            buffer.putFloat(left + radius);
        }
        if ((sides & BOTTOM) == BOTTOM) {
            buffer.putFloat(top);
        } else {
            buffer.putFloat(top + radius);
        }
        if ((sides & LEFT) == LEFT) {
            buffer.putFloat(right);
        } else {
            buffer.putFloat(right - radius);
        }
        if ((sides & TOP) == TOP) {
            buffer.putFloat(bottom);
        } else {
            buffer.putFloat(bottom - radius);
        }
        buffer.putFloat(radius)
                .putFloat(strokeRadius);
        mDrawOps.add(DRAW_ROUND_RECT_STROKE);
    }

    @Override
    public void drawRoundImage(@Nonnull Image image, float left, float top, float radius, @Nonnull Paint paint) {
        if (radius < 0) {
            radius = 0;
        }
        GLTexture texture = image.getTexture();
        float right = left + texture.getWidth();
        float bottom = top + texture.getHeight();
        if (quickReject(left, top, right, bottom)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(radius, paint.getSmoothRadius()));
        putRectColorUV(left, top, right, bottom, paint,
                0, 0, 1, 1);
        checkUniformMemory()
                .putFloat(left + radius)
                .putFloat(top + radius)
                .putFloat(right - radius)
                .putFloat(bottom - radius)
                .putFloat(radius);
        mTextures.add(texture);
        mDrawOps.add(DRAW_ROUND_IMAGE);
    }

    @Override
    public void drawTextRun(@Nonnull CharSequence text, int start, int end, float x, float y,
                            boolean isRtl, @Nonnull TextPaint paint) {
        if ((start | end | end - start | text.length() - end) < 0) {
            throw new IndexOutOfBoundsException();
        }
        char[] chars = new char[end - start];
        TextUtils.getChars(text, start, end, chars, 0);
        LayoutPiece piece = LayoutCache.getOrCreate(chars, 0, chars.length, isRtl, paint);
        drawTextRun(piece, x, y, paint.getColor());
    }

    @Override
    public void drawTextRun(@Nonnull MeasuredText text, int start, int end, float x, float y,
                            @Nonnull TextPaint paint) {
        if ((start | end | end - start) < 0) {
            throw new IndexOutOfBoundsException();
        }
        LayoutPiece piece = text.getLayoutPiece(start, end);
        if (piece != null) {
            drawTextRun(piece, x, y, paint.getColor());
        }
    }

    private void drawTextRun(@Nonnull LayoutPiece piece, float x, float y, int color) {
        if (piece.getAdvances().length == 0 ||
                quickReject(x, y - piece.getAscent(),
                        x + piece.getAdvance(), y + piece.getDescent())) {
            return;
        }
        mDrawTexts.add(new DrawText(piece, x, y));
        drawMatrix();
        checkUniformMemory()
                .putFloat(((color >> 16) & 0xff) / 255f)
                .putFloat(((color >> 8) & 0xff) / 255f)
                .putFloat((color & 0xff) / 255f)
                .putFloat((color >>> 24) / 255f);
        mDrawOps.add(DRAW_TEXT);
    }

    private static class DrawText {

        @Nonnull
        private final LayoutPiece piece;
        private final float x;
        private final float y;

        private DrawText(@Nonnull LayoutPiece piece, float x, float y) {
            this.piece = piece;
            this.x = x;
            this.y = y;
        }

        @Nonnull
        private TexturedGlyph[] buildBuffer(@Nonnull GLCanvas canvas) {
            final TexturedGlyph[] glyphs = piece.getGlyphs();
            final float[] positions = piece.getPositions();
            for (int i = 0, e = glyphs.length; i < e; i++) {
                canvas.putGlyph(glyphs[i], x + positions[i * 2], y + positions[i * 2 + 1]);
            }
            return glyphs;
        }
    }
}
