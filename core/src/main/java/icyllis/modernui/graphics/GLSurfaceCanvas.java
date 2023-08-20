/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.geom.DefaultGeoProc;
import icyllis.arc3d.engine.shading.UniformHandler;
import icyllis.arc3d.opengl.*;
import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.*;
import icyllis.modernui.core.Core;
import icyllis.modernui.graphics.font.BakedGlyph;
import icyllis.modernui.graphics.font.GlyphManager;
import icyllis.modernui.graphics.text.Font;
import icyllis.modernui.graphics.text.StandardFont;
import icyllis.modernui.view.Gravity;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.nio.*;
import java.util.*;

import static icyllis.arc3d.opengl.GLCore.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Modern OpenGL implementation to Canvas, handling multithreaded rendering.
 * This requires OpenGL 4.5 core profile.
 * <p>
 * Modern UI Canvas is designed for high-performance real-time rendering of
 * vector graphics with infinite precision. Thus, you can't draw other things
 * except those defined in Canvas easily. All geometry instances will be
 * redrawn/re-rendered every frame.
 * <p>
 * Modern UI doesn't make use of tessellation for non-primitives. Instead, we use
 * analytic geometry algorithm in Fragment Shaders to get ideal solution with
 * infinite precision. However, for stoke shaders, there will be a lot of discarded
 * fragments that can not be avoided on the CPU side. And they're recomputed each
 * frame. Especially the quadratic Bézier curve, the algorithm is very complex.
 * And this can't draw cubic Bézier curves, the only way is through tessellation.
 * <p>
 * This class is used to build OpenGL buffers from one thread, using multiple
 * vertex arrays, uniform buffers and vertex buffers. All drawing methods are
 * recording commands and must be called from one thread. Later call
 * {@link #executeDrawOps(GLFramebufferCompat)} for calling OpenGL functions on the render thread.
 * The color buffer drawn to must be at index 0, and stencil buffer must be 8-bit.
 * <p>
 * For multiple off-screen rendering targets, Modern UI allocates up to four
 * color buffers as attachments to the target framebuffer. This handles global
 * alpha transformation.
 * <p>
 * For clipping, Modern UI uses stencil test as well as pre-clipping on CPU side.
 * <p>
 * GLSL shader sources are defined in assets.
 *
 * @author BloCamLimb
 */
@NotThreadSafe
public final class GLSurfaceCanvas extends GLCanvas {

    private static volatile GLSurfaceCanvas sInstance;

    /**
     * Uniform block binding points (sequential)
     */
    public static final int MATRIX_BLOCK_BINDING = 0;

    /*
     * Vertex attributes
     */
    /*public static final GLVertexAttrib POS;
    public static final GLVertexAttrib COLOR;
    public static final GLVertexAttrib UV;*/

    /*
     * Vertex formats
     */
    /*public static final GLVertexFormat POS_COLOR;
    public static final GLVertexFormat POS_COLOR_TEX;
    public static final GLVertexFormat POS_TEX;*/

    /**
     * Recording draw operations (sequential)
     */
    public static final byte DRAW_PRIM = 0;
    public static final byte DRAW_RECT = 1;
    public static final byte DRAW_IMAGE = 2;
    public static final byte DRAW_ROUND_RECT_FILL = 3;
    public static final byte DRAW_ROUND_IMAGE = 4;
    public static final byte DRAW_ROUND_RECT_STROKE = 5;
    public static final byte DRAW_CIRCLE_FILL = 6;
    public static final byte DRAW_CIRCLE_STROKE = 7;
    public static final byte DRAW_ARC_FILL = 8;
    public static final byte DRAW_ARC_STROKE = 9;
    public static final byte DRAW_BEZIER = 10;
    public static final byte DRAW_TEXT = 11;
    public static final byte DRAW_IMAGE_LAYER = 12;
    public static final byte DRAW_CLIP_PUSH = 13;
    public static final byte DRAW_CLIP_POP = 14;
    public static final byte DRAW_MATRIX = 15;
    public static final byte DRAW_SMOOTH = 16;
    public static final byte DRAW_LAYER_PUSH = 17;
    public static final byte DRAW_LAYER_POP = 18;
    public static final byte DRAW_CUSTOM = 19;
    public static final byte DRAW_GLOW_WAVE = 20;
    public static final byte DRAW_PIE_FILL = 21;
    public static final byte DRAW_PIE_STROKE = 22;
    public static final byte DRAW_ROUND_LINE_FILL = 23;
    public static final byte DRAW_ROUND_LINE_STROKE = 24;

    /**
     * Uniform block sizes (maximum), use std140 layout
     *
     * @see UniformHandler
     */
    public static final int MATRIX_UNIFORM_SIZE = 144;
    public static final int SMOOTH_UNIFORM_SIZE = 4;
    public static final int ARC_UNIFORM_SIZE = 24;
    public static final int BEZIER_UNIFORM_SIZE = 28;
    public static final int CIRCLE_UNIFORM_SIZE = 16;
    public static final int ROUND_RECT_UNIFORM_SIZE = 24;

    static {
        //POS = new GLVertexAttrib(GENERIC_BINDING, GLVertexAttrib.Src.FLOAT, GLVertexAttrib.Dst.VEC2, false);
        //COLOR = new GLVertexAttrib(GENERIC_BINDING, GLVertexAttrib.Src.UBYTE, GLVertexAttrib.Dst.VEC4, true);
        //UV = new GLVertexAttrib(GENERIC_BINDING, GLVertexAttrib.Src.FLOAT, GLVertexAttrib.Dst.VEC2, false);
        //MODEL_VIEW = new VertexAttrib(INSTANCED_BINDING, VertexAttrib.Src.FLOAT, VertexAttrib.Dst.MAT4, false);
        //POS_COLOR = new GLVertexFormat(POS, COLOR);
        //POS_COLOR_TEX = new GLVertexFormat(POS, COLOR, UV);
        //POS_TEX = new GLVertexFormat(POS, UV);
    }

    /**
     * Pipelines.
     */
    private GLProgram COLOR_FILL;
    private GLProgram COLOR_TEX;
    private GLProgram ROUND_RECT_FILL;
    private GLProgram ROUND_RECT_TEX;
    private GLProgram ROUND_RECT_STROKE;
    private GLProgram CIRCLE_FILL;
    private GLProgram CIRCLE_STROKE;
    private GLProgram ARC_FILL;
    private GLProgram ARC_STROKE;
    private GLProgram BEZIER_CURVE;
    private GLProgram ALPHA_TEX;
    private GLProgram COLOR_TEX_PRE;
    private GLProgram GLOW_WAVE;
    private GLProgram PIE_FILL;
    private GLProgram PIE_STROKE;
    private GLProgram ROUND_LINE_FILL;
    private GLProgram ROUND_LINE_STROKE;

    private GLVertexArray POS_COLOR;
    private GLVertexArray POS_COLOR_TEX;
    private GLVertexArray POS_TEX;

    /**
     * pos vec2 + color ubyte4 + uv vec2
     */
    public static final int TEXTURE_RECT_VERTEX_SIZE = 20;

    // recorded operations
    private final ByteArrayList mDrawOps = new ByteArrayList();
    private final IntArrayList mDrawPrims = new IntArrayList();

    // vertex buffer objects
    private GLBuffer mColorMeshVertexBuffer;
    private ByteBuffer mColorMeshStagingBuffer = memAlloc(16384);
    private boolean mColorMeshBufferResized = true;

    private GLBuffer mTextureMeshVertexBuffer;
    private ByteBuffer mTextureMeshStagingBuffer = memAlloc(4096);
    private boolean mTextureMeshBufferResized = true;

    public static final int MAX_GLYPH_INDEX_COUNT = 3072;

    // dynamic update on render thread
    private GLBuffer mGlyphVertexBuffer;
    private ByteBuffer mGlyphStagingBuffer = memAlloc(MAX_GLYPH_INDEX_COUNT / 6 * 4 * 16); // 32KB
    private boolean mGlyphBufferResized = true;

    private final GLBuffer mGlyphIndexBuffer;

    /*private int mModelViewVBO = INVALID_ID;
    private ByteBuffer mModelViewData = memAlloc(1024);
    private boolean mRecreateModelView = true;*/

    // the client buffer used for updating the uniform blocks
    private ByteBuffer mUniformRingBuffer = memAlloc(8192);

    // immutable uniform buffer objects
    private final GLUniformBufferCompat mMatrixUBO = new GLUniformBufferCompat();
    private final GLUniformBufferCompat mSmoothUBO = new GLUniformBufferCompat();
    private final GLUniformBufferCompat mArcUBO = new GLUniformBufferCompat();
    private final GLUniformBufferCompat mBezierUBO = new GLUniformBufferCompat();
    private final GLUniformBufferCompat mCircleUBO = new GLUniformBufferCompat();
    private final GLUniformBufferCompat mRoundRectUBO = new GLUniformBufferCompat();

    // mag filter = linear
    @SharedPtr
    private final GLSampler mLinearSampler;

    private final ByteBuffer mLayerImageMemory = memAlloc(TEXTURE_RECT_VERTEX_SIZE * 4);

    // used in rendering, local states
    private int mCurrTexture;
    private GLSampler mCurrSampler;

    // absolute value presents the reference value, and sign represents whether to
    // update the stencil buffer (positive = update, or just change stencil func)
    private final IntList mClipRefs = new IntArrayList();
    private final IntList mLayerAlphas = new IntArrayList();
    private final IntStack mLayerStack = new IntArrayList(3);

    // using textures of draw states, in the order of calling
    private final Queue<Object> mTextures = new ArrayDeque<>();
    private final List<DrawTextOp> mDrawTexts = new ArrayList<>();
    private final Queue<CustomDrawable.DrawHandler> mCustoms = new ArrayDeque<>();

    private final List<SurfaceProxy> mTexturesToClean = new ArrayList<>();

    private final Matrix4 mProjection = new Matrix4();
    private final FloatBuffer mProjectionUpload = memAllocFloat(16);

    private final GLServer mServer;

    private boolean mNeedsTexBinding;

    @RenderThread
    public GLSurfaceCanvas(GLServer server) {
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

        mServer = server;

        mMatrixUBO.allocate(MATRIX_UNIFORM_SIZE);
        mSmoothUBO.allocate(SMOOTH_UNIFORM_SIZE);
        mArcUBO.allocate(ARC_UNIFORM_SIZE);
        mBezierUBO.allocate(BEZIER_UNIFORM_SIZE);
        mCircleUBO.allocate(CIRCLE_UNIFORM_SIZE);
        mRoundRectUBO.allocate(ROUND_RECT_UNIFORM_SIZE);

        mLinearSampler = Objects.requireNonNull(
                server.getResourceProvider().findOrCreateCompatibleSampler(SamplerState.DEFAULT),
                "Failed to create font sampler");

        {
            ShortBuffer indices = MemoryUtil.memAllocShort(MAX_GLYPH_INDEX_COUNT);
            int baseIndex = 0;
            for (int i = 0; i < MAX_GLYPH_INDEX_COUNT / 6; i++) {
                // CCW, bottom left (0), bottom right (1), top left (2), top right (3)
                indices.put((short) (baseIndex));
                indices.put((short) (baseIndex + 1));
                indices.put((short) (baseIndex + 2));
                indices.put((short) (baseIndex + 2));
                indices.put((short) (baseIndex + 1));
                indices.put((short) (baseIndex + 3));
                baseIndex += 4;
            }
            indices.flip();
            mGlyphIndexBuffer = Objects.requireNonNull(
                    GLBuffer.make(server, indices.capacity(),
                            Engine.BufferUsageFlags.kStatic |
                                    Engine.BufferUsageFlags.kIndex),
                    "Failed to create index buffer for glyph mesh");
            mGlyphIndexBuffer.updateData(0, indices.capacity(), MemoryUtil.memAddress(indices));
            MemoryUtil.memFree(indices);
        }

        ModernUI.LOGGER.debug(MARKER,
                "Created glyph index buffer: {}, size: {}",
                mGlyphIndexBuffer.getHandle(), mGlyphIndexBuffer.getSize());

        mSaves.push(new Save());

        loadPipelines();
    }

    @RenderThread
    public static GLSurfaceCanvas initialize() {
        Core.checkRenderThread();
        if (sInstance == null) {
            sInstance = new GLSurfaceCanvas((GLServer) Core.requireDirectContext().getServer());
            /*POS_COLOR.setBindingDivisor(INSTANCED_BINDING, 1);
            POS_COLOR_TEX.setBindingDivisor(INSTANCED_BINDING, 1);*/
        }
        return sInstance;
    }

    /**
     * Exposed for internal use, be aware of the thread-safety and client-controlled GL states.
     *
     * @return the global instance
     */
    public static GLSurfaceCanvas getInstance() {
        return sInstance;
    }

    //@formatter:off
    private void loadPipelines() {

        GLVertexArray aPosColor = GLVertexArray.make(mServer,
                new DefaultGeoProc(DefaultGeoProc.FLAG_COLOR_ATTRIBUTE));
        GLVertexArray aPosColorUV;
        {
            var gp = new DefaultGeoProc(DefaultGeoProc.FLAG_COLOR_ATTRIBUTE |
                    DefaultGeoProc.FLAG_TEX_COORD_ATTRIBUTE);
            assert gp.vertexStride() == TEXTURE_RECT_VERTEX_SIZE;
            aPosColorUV = GLVertexArray.make(mServer, gp);
        }
        GLVertexArray aPosUV = GLVertexArray.make(mServer,
                new DefaultGeoProc(DefaultGeoProc.FLAG_TEX_COORD_ATTRIBUTE));
        Objects.requireNonNull(aPosColor);
        Objects.requireNonNull(aPosColorUV);
        Objects.requireNonNull(aPosUV);

        int posColor;
        int posColorTex;
        int posTex;

        int colorFill;
        int colorTex;
        int roundRectFill;
        int roundRectTex;
        int roundRectStroke;
        int circleFill;
        int circleStroke;
        int arcFill;
        int arcStroke;
        int quadBezier;
        int alphaTex;
        int colorTexPre;
        int glowWave;
        int pieFill;
        int pieStroke;
        int roundLineFill;
        int roundLineStroke;

        boolean compat = !mServer.getCaps().hasDSASupport();

        posColor    = createStage( "pos_color.vert", compat);
        posColorTex = createStage( "pos_color_tex.vert", compat);
        posTex      = createStage( "pos_tex.vert", compat);

        colorFill       = createStage( "color_fill.frag", compat);
        colorTex        = createStage( "color_tex.frag", compat);
        roundRectFill   = createStage( "round_rect_fill.frag", compat);
        roundRectTex    = createStage( "round_rect_tex.frag", compat);
        roundRectStroke = createStage( "round_rect_stroke.frag", compat);
        circleFill      = createStage( "circle_fill.frag", compat);
        circleStroke    = createStage( "circle_stroke.frag", compat);
        arcFill         = createStage( "arc_fill.frag", compat);
        arcStroke       = createStage( "arc_stroke.frag", compat);
        quadBezier      = createStage( "quadratic_bezier.frag", compat);
        alphaTex        = createStage( "alpha_tex.frag", compat);
        colorTexPre     = createStage( "color_tex_pre.frag", compat);
        glowWave        = createStage( "glow_wave.frag", compat);
        pieFill         = createStage( "pie_fill.frag", compat);
        pieStroke       = createStage( "pie_stroke.frag", compat);
        roundLineFill   = createStage( "round_line_fill.frag", compat);
        roundLineStroke = createStage( "round_line_stroke.frag", compat);

        int pColorFill          = createProgram(posColor,    colorFill);
        int pColorTex           = createProgram(posColorTex, colorTex);
        int pRoundRectFill      = createProgram(posColor,    roundRectFill);
        int pRoundRectTex       = createProgram(posColorTex, roundRectTex);
        int pRoundRectStroke    = createProgram(posColor,    roundRectStroke);
        int pCircleFill         = createProgram(posColor,    circleFill);
        int pCircleStroke       = createProgram(posColor,    circleStroke);
        int pArcFill            = createProgram(posColor,    arcFill);
        int pArcStroke          = createProgram(posColor,    arcStroke);
        int pBezierCurve        = createProgram(posColor,    quadBezier);
        int pAlphaTex           = createProgram(posTex,      alphaTex);
        int pColorTexPre        = createProgram(posColorTex, colorTexPre);
        int pGlowWave           = createProgram(posColor,    glowWave);
        int pPieFill            = createProgram(posColor,    pieFill);
        int pPieStroke          = createProgram(posColor,    pieStroke);
        int pRoundLineFill      = createProgram(posColor,    roundLineFill);
        int pRoundLineStroke    = createProgram(posColor,    roundLineStroke);

        boolean success = pColorFill != 0 &&
        pColorTex != 0 &&
                pRoundRectFill != 0 &&
        pRoundRectTex != 0 &&
                pRoundRectStroke != 0 &&
        pCircleFill != 0 &&
                pCircleStroke != 0 &&
        pArcFill != 0 &&
                pArcStroke != 0 &&
        pBezierCurve != 0 &&
                pAlphaTex != 0 &&
        pColorTexPre != 0 &&
                pGlowWave != 0 &&
        pPieFill != 0 &&
                pPieStroke != 0 &&
        pRoundLineFill != 0 &&
                pRoundLineStroke != 0;

        if (!success) {
            throw new RuntimeException("Failed to link shader programs");
        }

        // manually bind
        if (compat) {
            bindProgramMatrixBlock(pColorFill);
            bindProgramFragLocation(pColorFill);

            bindProgramMatrixBlock( pColorTex);
            bindProgramFragLocation(pColorTex);

            bindProgramMatrixBlock( pColorTexPre);
            bindProgramFragLocation(pColorTexPre);

            bindProgramMatrixBlock( pAlphaTex);
            bindProgramFragLocation(pAlphaTex);

            bindProgramMatrixBlock( pGlowWave);
            bindProgramFragLocation(pGlowWave);

            bindProgramMatrixBlock( pArcFill);
            bindProgramFragLocation(pArcFill);
            bindProgramArcBlock(    pArcFill);
            bindProgramSmoothBlock( pArcFill);

            bindProgramMatrixBlock( pArcStroke);
            bindProgramFragLocation(pArcStroke);
            bindProgramArcBlock(    pArcStroke);
            bindProgramSmoothBlock( pArcStroke);

            bindProgramMatrixBlock( pPieFill);
            bindProgramFragLocation(pPieFill);
            bindProgramArcBlock(    pPieFill);
            bindProgramSmoothBlock( pPieFill);

            bindProgramMatrixBlock( pPieStroke);
            bindProgramFragLocation(pPieStroke);
            bindProgramArcBlock(    pPieStroke);
            bindProgramSmoothBlock( pPieStroke);

            bindProgramMatrixBlock( pBezierCurve);
            bindProgramFragLocation(pBezierCurve);
            int c3 = glGetUniformBlockIndex(pBezierCurve, "PaintBlock");
            glUniformBlockBinding(pBezierCurve, c3, 3);
            bindProgramSmoothBlock(pBezierCurve);

            bindProgramMatrixBlock( pCircleFill);
            bindProgramFragLocation(pCircleFill);
            bindProgramCircleBlock( pCircleFill);
            bindProgramSmoothBlock( pCircleFill);

            bindProgramMatrixBlock( pCircleStroke);
            bindProgramFragLocation(pCircleStroke);
            bindProgramCircleBlock( pCircleStroke);
            bindProgramSmoothBlock( pCircleStroke);

            bindProgramMatrixBlock(   pRoundLineFill);
            bindProgramFragLocation(  pRoundLineFill);
            bindProgramRoundRectBlock(pRoundLineFill);
            bindProgramSmoothBlock(   pRoundLineFill);

            bindProgramMatrixBlock(   pRoundLineStroke);
            bindProgramFragLocation(  pRoundLineStroke);
            bindProgramRoundRectBlock(pRoundLineStroke);
            bindProgramSmoothBlock(   pRoundLineStroke);

            bindProgramMatrixBlock(   pRoundRectFill);
            bindProgramFragLocation(  pRoundRectFill);
            bindProgramRoundRectBlock(pRoundRectFill);
            bindProgramSmoothBlock(   pRoundRectFill);

            bindProgramMatrixBlock(   pRoundRectStroke);
            bindProgramFragLocation(  pRoundRectStroke);
            bindProgramRoundRectBlock(pRoundRectStroke);
            bindProgramSmoothBlock(   pRoundRectStroke);

            bindProgramMatrixBlock(   pRoundRectTex);
            bindProgramFragLocation(  pRoundRectTex);
            bindProgramRoundRectBlock(pRoundRectTex);
            bindProgramSmoothBlock(   pRoundRectTex);

            mNeedsTexBinding = true;
        }

        COLOR_FILL = new GLProgram(mServer, pColorFill );
        COLOR_TEX  = new GLProgram(mServer, pColorTex  );
        ROUND_RECT_FILL   = new GLProgram(mServer, pRoundRectFill);
        ROUND_RECT_TEX    = new GLProgram(mServer, pRoundRectTex);
        ROUND_RECT_STROKE = new GLProgram(mServer, pRoundRectStroke);
        CIRCLE_FILL   = new GLProgram(mServer, pCircleFill);
        CIRCLE_STROKE = new GLProgram(mServer, pCircleStroke);
        ARC_FILL   = new GLProgram(mServer, pArcFill);
        ARC_STROKE = new GLProgram(mServer, pArcStroke);
        BEZIER_CURVE = new GLProgram(mServer, pBezierCurve);
        ALPHA_TEX = new GLProgram(mServer, pAlphaTex);
        COLOR_TEX_PRE = new GLProgram(mServer, pColorTexPre);
        GLOW_WAVE = new GLProgram(mServer, pGlowWave);
        PIE_FILL = new GLProgram(mServer, pPieFill);
        PIE_STROKE = new GLProgram(mServer, pPieStroke);
        ROUND_LINE_FILL = new GLProgram(mServer, pRoundLineFill);
        ROUND_LINE_STROKE = new GLProgram(mServer, pRoundLineStroke);

        POS_COLOR = aPosColor;
        POS_COLOR_TEX = aPosColorUV;
        POS_TEX = aPosUV;

        ModernUI.LOGGER.info("Loaded OpenGL canvas shaders, compatibility mode: " + compat);
    }
    //@formatter:on

    private int createStage(String entry, boolean compat) {
        int sp = entry.length() - 5;
        String path = "shaders/" + (compat
                ? entry.substring(0, sp) + "_330" + entry.substring(sp)
                : entry);
        int type;
        if (entry.endsWith(".vert")) {
            type = GLCore.GL_VERTEX_SHADER;
        } else if (entry.endsWith(".frag")) {
            type = GLCore.GL_FRAGMENT_SHADER;
        } else if (entry.endsWith(".geom")) {
            type = GL_GEOMETRY_SHADER;
        } else {
            throw new RuntimeException();
        }
        ByteBuffer source = null;
        try (var stream = ModernUI.getInstance().getResourceStream(ModernUI.ID, path)) {
            source = Core.readIntoNativeBuffer(stream).flip();
            return GLCore.glCompileShader(type, source,
                    mServer.getPipelineStateCache().getStats(),
                    mServer.getContext().getErrorWriter());
        } catch (IOException e) {
            ModernUI.LOGGER.error(GLCore.MARKER, "Failed to get shader source {}:{}\n", ModernUI.ID, path, e);
        } finally {
            MemoryUtil.memFree(source);
        }
        return 0;
    }

    public int createProgram(int... stages) {
        int program = GLCore.glCreateProgram();
        if (program == 0) {
            return 0;
        }
        for (int s : stages) {
            GLCore.glAttachShader(program, s);
        }
        GLCore.glLinkProgram(program);
        if (GLCore.glGetProgrami(program, GLCore.GL_LINK_STATUS) == GL_FALSE) {
            String log = GLCore.glGetProgramInfoLog(program, 8192);
            ModernUI.LOGGER.error(GLCore.MARKER, "Failed to link shader program\n{}", log);
            // also detaches all shaders
            GLCore.glDeleteProgram(program);
            return 0;
        }
        for (int s : stages) {
            GLCore.glDetachShader(program, s);
        }
        return program;
    }

    private void bindProgramMatrixBlock(int program) {
        int c0 = glGetUniformBlockIndex(program, "MatrixBlock");
        glUniformBlockBinding(program, c0, 0);
    }

    private void bindProgramSmoothBlock(int program) {
        int c1 = glGetUniformBlockIndex(program, "SmoothBlock");
        glUniformBlockBinding(program, c1, 1);
    }

    private void bindProgramArcBlock(int program) {
        int c2 = glGetUniformBlockIndex(program, "PaintBlock");
        glUniformBlockBinding(program, c2, 2);
    }

    private void bindProgramCircleBlock(int program) {
        int c4 = glGetUniformBlockIndex(program, "PaintBlock");
        glUniformBlockBinding(program, c4, 4);
    }

    private void bindProgramRoundRectBlock(int program) {
        int c5 = glGetUniformBlockIndex(program, "PaintBlock");
        glUniformBlockBinding(program, c5, 5);
    }

    private void bindProgramFragLocation(int program) {
        // we use draw buffer 0
        glBindFragDataLocation(program, 0, "fragColor");
    }

    private void bindProgramTexBinding(int program) {
        glUseProgram(program);
        int u0 = glGetUniformLocation(program, "u_Sampler");
        glUniform1i(u0, 0); // <- the texture unit is 0
    }

    private ImageInfo mInfo;

    @Override
    public void reset(int width, int height) {
        super.reset(width, height);
        mDrawOps.clear();
        mClipRefs.clear();
        mLayerAlphas.clear();
        mDrawTexts.clear();
        mColorMeshStagingBuffer.clear();
        mTextureMeshStagingBuffer.clear();
        mUniformRingBuffer.clear();
        mInfo = ImageInfo.make(width, height, ImageInfo.CT_RGBA_8888, ImageInfo.AT_PREMUL, null);
    }

    public void destroy() {
        COLOR_FILL.unref();
        COLOR_TEX.unref();
        ROUND_RECT_FILL.unref();
        ROUND_RECT_TEX.unref();
        ROUND_RECT_STROKE.unref();
        CIRCLE_FILL.unref();
        CIRCLE_STROKE.unref();
        ARC_FILL.unref();
        ARC_STROKE.unref();
        BEZIER_CURVE.unref();
        ALPHA_TEX.unref();
        COLOR_TEX_PRE.unref();
        GLOW_WAVE.unref();
        PIE_FILL.unref();
        PIE_STROKE.unref();
        ROUND_LINE_FILL.unref();
        ROUND_LINE_STROKE.unref();

        POS_COLOR.unref();
        POS_COLOR_TEX.unref();
        POS_TEX.unref();

        mLinearSampler.unref();
        mTextures.forEach(o -> {
            if (o instanceof SurfaceProxyView v) {
                if (v.getProxy() != null) {
                    v.getProxy().unref();
                }
            }
        });
        mTextures.clear();
        mTexturesToClean.forEach(SurfaceProxy::unref);
        mTexturesToClean.clear();
    }

    @RenderThread
    public void setProjection(@NonNull Matrix4 projection) {
        projection.store(mProjectionUpload.clear());
    }

    @NonNull
    @RenderThread
    public FloatBuffer getProjection() {
        return mProjectionUpload.rewind();
    }

    private GLVertexArray bindPipeline(GLProgram program, GLVertexArray vertexArray) {
        var cmdBuffer = mServer.currentCommandBuffer();
        cmdBuffer.bindPipeline(program, vertexArray);
        return vertexArray;
    }

    @RenderThread
    public void bindSampler(GLSampler sampler) {
        if (mCurrSampler != sampler) {
            if (sampler != null) {
                glBindSampler(0, sampler.getHandle());
            } else {
                glBindSampler(0, 0);
            }
            mCurrSampler = sampler;
        }
    }

    @RenderThread
    public void bindTexture(int texture) {
        if (mCurrTexture != texture) {
            glBindTexture(GL_TEXTURE_2D, texture);
            mCurrTexture = texture;
        }
    }

    private void bindNextTexture() {
        var tex = mTextures.remove();
        if (tex instanceof GLTextureCompat compat) {
            bindSampler(null);
            bindTexture(compat.get());
        } else {
            var view = (SurfaceProxyView) tex;
            var proxy = view.getProxy();
            boolean success = true;
            if (!proxy.isInstantiated()) {
                var resourceProvider = mServer.getResourceProvider();
                success = proxy.doLazyInstantiation(resourceProvider);
            }
            if (success) {
                var glTex = (GLTexture) Objects.requireNonNull(proxy.peekTexture());
                bindSampler(mLinearSampler);
                bindTexture(glTex.getHandle());

                mServer.generateMipmaps(glTex);

                var swizzle = view.getSwizzle();
                var parameters = glTex.getParameters();
                var swizzleChanged = false;
                for (int i = 0; i < 4; ++i) {
                    int swiz = switch (swizzle & 0xF) {
                        case 0 -> GL_RED;
                        case 1 -> GL_GREEN;
                        case 2 -> GL_BLUE;
                        case 3 -> GL_ALPHA;
                        case 4 -> GL_ZERO;
                        case 5 -> GL_ONE;
                        default -> throw new AssertionError(swizzle);
                    };
                    if (parameters.swizzle[i] != swiz) {
                        parameters.swizzle[i] = swiz;
                        swizzleChanged = true;
                    }
                    swizzle >>= 4;
                }
                if (swizzleChanged) {
                    glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_RGBA, parameters.swizzle);
                }
            }
            mTexturesToClean.add(proxy);
        }
    }

    @RenderThread
    public boolean executeDrawOps(@Nullable GLFramebufferCompat framebuffer) {
        Core.checkRenderThread();
        Core.flushRenderCalls();
        if (framebuffer != null) {
            framebuffer.bindDraw();
            framebuffer.makeBuffers(mWidth, mHeight, false);
            framebuffer.clearColorBuffer();
            framebuffer.clearDepthStencilBuffer();
        }
        if (mDrawOps.isEmpty()) {
            return false;
        }
        if (getSaveCount() != 1) {
            throw new IllegalStateException("Unbalanced save-restore pair: " + getSaveCount());
        }
        mServer.forceResetContext(Engine.GLBackendState.kPipeline);

        // upload projection matrix
        mMatrixUBO.upload(0, 64, memAddress(mProjectionUpload.flip()));

        uploadVertexBuffers();

        if (mNeedsTexBinding) {
            bindProgramTexBinding(ALPHA_TEX.getProgram());
            bindProgramTexBinding(COLOR_TEX.getProgram());
            bindProgramTexBinding(COLOR_TEX_PRE.getProgram());
            bindProgramTexBinding(ROUND_RECT_TEX.getProgram());
            mNeedsTexBinding = false;
        }

        // uniform bindings are globally shared, we must re-bind before we use them
        //nglBindBuffersBase(GL_UNIFORM_BUFFER, MATRIX_BLOCK_BINDING, 6, mUniformBuffers);
        mMatrixUBO.bindBase(GL_UNIFORM_BUFFER, 0);
        mSmoothUBO.bindBase(GL_UNIFORM_BUFFER, 1);
        mArcUBO.bindBase(GL_UNIFORM_BUFFER, 2);
        mBezierUBO.bindBase(GL_UNIFORM_BUFFER, 3);
        mCircleUBO.bindBase(GL_UNIFORM_BUFFER, 4);
        mRoundRectUBO.bindBase(GL_UNIFORM_BUFFER, 5);

        glStencilFunc(GL_EQUAL, 0, 0xff);
        glStencilMask(0xff);
        glActiveTexture(GL_TEXTURE0);
        glBindSampler(0, 0);
        glBindVertexArray(0);
        glUseProgram(0);

        mCurrSampler = null;
        mCurrTexture = 0;

        long uniformDataPtr = memAddress(mUniformRingBuffer.flip());

        // generic array index
        int posColorIndex = 0;
        // preserve two triangles
        int posColorTexIndex = 4;
        int primIndex = 0;
        int clipIndex = 0;
        int textIndex = 0;
        int textBaseVertex = 0;
        // layer alphas
        int alphaIndex = 0;
        // draw buffers
        int colorBuffer = GL_COLOR_ATTACHMENT0;

        final var stats = mServer.getStats();
        int nDraws = 0;

        for (int op : mDrawOps) {
            switch (op) {
                case DRAW_PRIM -> {
                    bindPipeline(COLOR_FILL, POS_COLOR)
                            .bindVertexBuffer(mColorMeshVertexBuffer, 0);
                    int prim = mDrawPrims.getInt(primIndex++);
                    int n = prim & 0xFFFF;
                    glDrawArrays(prim >> 16, posColorIndex, n);
                    nDraws++;
                    posColorIndex += n;
                }
                case DRAW_RECT -> {
                    bindPipeline(COLOR_FILL, POS_COLOR).
                            bindVertexBuffer(mColorMeshVertexBuffer, 0);
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    nDraws++;
                    posColorIndex += 4;
                }
                case DRAW_ROUND_RECT_FILL -> {
                    bindPipeline(ROUND_RECT_FILL, POS_COLOR)
                            .bindVertexBuffer(mColorMeshVertexBuffer, 0);
                    mRoundRectUBO.upload(0, 20, uniformDataPtr);
                    uniformDataPtr += 20;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    nDraws++;
                    posColorIndex += 4;
                }
                case DRAW_ROUND_RECT_STROKE -> {
                    bindPipeline(ROUND_RECT_STROKE, POS_COLOR)
                            .bindVertexBuffer(mColorMeshVertexBuffer, 0);
                    mRoundRectUBO.upload(0, 24, uniformDataPtr);
                    uniformDataPtr += 24;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    nDraws++;
                    posColorIndex += 4;
                }
                case DRAW_ROUND_IMAGE -> {
                    bindPipeline(ROUND_RECT_TEX, POS_COLOR_TEX)
                            .bindVertexBuffer(mTextureMeshVertexBuffer, 0);
                    bindNextTexture();
                    mRoundRectUBO.upload(0, 20, uniformDataPtr);
                    uniformDataPtr += 20;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorTexIndex, 4);
                    nDraws++;
                    posColorTexIndex += 4;
                }
                case DRAW_ROUND_LINE_FILL -> {
                    bindPipeline(ROUND_LINE_FILL, POS_COLOR)
                            .bindVertexBuffer(mColorMeshVertexBuffer, 0);
                    mRoundRectUBO.upload(0, 20, uniformDataPtr);
                    uniformDataPtr += 20;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    nDraws++;
                    posColorIndex += 4;
                }
                case DRAW_ROUND_LINE_STROKE -> {
                    bindPipeline(ROUND_LINE_STROKE, POS_COLOR)
                            .bindVertexBuffer(mColorMeshVertexBuffer, 0);
                    mRoundRectUBO.upload(0, 24, uniformDataPtr);
                    uniformDataPtr += 24;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    nDraws++;
                    posColorIndex += 4;
                }
                case DRAW_IMAGE -> {
                    bindPipeline(COLOR_TEX, POS_COLOR_TEX)
                            .bindVertexBuffer(mTextureMeshVertexBuffer, 0);
                    bindNextTexture();
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorTexIndex, 4);
                    nDraws++;
                    posColorTexIndex += 4;
                }
                case DRAW_IMAGE_LAYER -> {
                    bindPipeline(COLOR_TEX_PRE, POS_COLOR_TEX)
                            .bindVertexBuffer(mTextureMeshVertexBuffer, 0);
                    bindSampler(null);
                    bindTexture(((GLTextureCompat) mTextures.remove()).get());
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorTexIndex, 4);
                    nDraws++;
                    posColorTexIndex += 4;
                }
                case DRAW_CIRCLE_FILL -> {
                    bindPipeline(CIRCLE_FILL, POS_COLOR)
                            .bindVertexBuffer(mColorMeshVertexBuffer, 0);
                    mCircleUBO.upload(0, 12, uniformDataPtr);
                    uniformDataPtr += 12;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    nDraws++;
                    posColorIndex += 4;
                }
                case DRAW_CIRCLE_STROKE -> {
                    bindPipeline(CIRCLE_STROKE, POS_COLOR)
                            .bindVertexBuffer(mColorMeshVertexBuffer, 0);
                    mCircleUBO.upload(0, 16, uniformDataPtr);
                    uniformDataPtr += 16;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    nDraws++;
                    posColorIndex += 4;
                }
                case DRAW_ARC_FILL -> {
                    bindPipeline(ARC_FILL, POS_COLOR)
                            .bindVertexBuffer(mColorMeshVertexBuffer, 0);
                    mArcUBO.upload(0, 20, uniformDataPtr);
                    uniformDataPtr += 20;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    nDraws++;
                    posColorIndex += 4;
                }
                case DRAW_ARC_STROKE -> {
                    bindPipeline(ARC_STROKE, POS_COLOR)
                            .bindVertexBuffer(mColorMeshVertexBuffer, 0);
                    mArcUBO.upload(0, 24, uniformDataPtr);
                    uniformDataPtr += 24;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    nDraws++;
                    posColorIndex += 4;
                }
                case DRAW_BEZIER -> {
                    bindPipeline(BEZIER_CURVE, POS_COLOR)
                            .bindVertexBuffer(mColorMeshVertexBuffer, 0);
                    mBezierUBO.upload(0, 28, uniformDataPtr);
                    uniformDataPtr += 28;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    nDraws++;
                    posColorIndex += 4;
                }
                case DRAW_PIE_FILL -> {
                    bindPipeline(PIE_FILL, POS_COLOR)
                            .bindVertexBuffer(mColorMeshVertexBuffer, 0);
                    mArcUBO.upload(0, 20, uniformDataPtr);
                    uniformDataPtr += 20;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    nDraws++;
                    posColorIndex += 4;
                }
                case DRAW_PIE_STROKE -> {
                    bindPipeline(PIE_STROKE, POS_COLOR)
                            .bindVertexBuffer(mColorMeshVertexBuffer, 0);
                    mArcUBO.upload(0, 24, uniformDataPtr);
                    uniformDataPtr += 24;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    nDraws++;
                    posColorIndex += 4;
                }
                case DRAW_CLIP_PUSH -> {
                    int clipRef = mClipRefs.getInt(clipIndex);

                    if (clipRef >= 0) {
                        glStencilOp(GL_KEEP, GL_KEEP, GL_INCR);
                        glColorMask(false, false, false, false);

                        bindPipeline(COLOR_FILL, POS_COLOR)
                                .bindVertexBuffer(mColorMeshVertexBuffer, 0);
                        glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                        nDraws++;
                        posColorIndex += 4;

                        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
                        glColorMask(true, true, true, true);
                    }

                    glStencilFunc(GL_EQUAL, Math.abs(clipRef), 0xff);
                    clipIndex++;
                }
                case DRAW_CLIP_POP -> {
                    int clipRef = mClipRefs.getInt(clipIndex);

                    if (clipRef >= 0) {
                        glStencilFunc(GL_LESS, clipRef, 0xff);
                        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
                        glColorMask(false, false, false, false);

                        bindPipeline(COLOR_FILL, POS_COLOR)
                                .bindVertexBuffer(mColorMeshVertexBuffer, 0);
                        glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                        nDraws++;
                        posColorIndex += 4;

                        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
                        glColorMask(true, true, true, true);
                    }

                    glStencilFunc(GL_EQUAL, Math.abs(clipRef), 0xff);
                    clipIndex++;
                }
                case DRAW_TEXT -> {
                    mMatrixUBO.upload(128, 16, uniformDataPtr);
                    uniformDataPtr += 16;

                    var textOp = mDrawTexts.get(textIndex++);
                    int limit = textOp.mVisibleGlyphCount;
                    if (limit == 0) {
                        // due to deferred plotting, this can be empty
                        continue;
                    }

                    bindPipeline(ALPHA_TEX, POS_TEX);
                    POS_TEX.bindIndexBuffer(mGlyphIndexBuffer);
                    POS_TEX.bindVertexBuffer(mGlyphVertexBuffer, 0);
                    bindTexture(textOp.mTexture);
                    bindSampler(mLinearSampler);
                    for (int lastPrim = 0;
                         lastPrim < limit;
                    ) {
                        int primCount = Math.min(limit - lastPrim, MAX_GLYPH_INDEX_COUNT / 6);
                        nglDrawElementsBaseVertex(GL_TRIANGLES, /*indexCount*/ primCount * 6,
                                GL_UNSIGNED_SHORT, /*baseIndex*/ 0, textBaseVertex);
                        nDraws++;
                        lastPrim += primCount;
                        textBaseVertex += primCount << 2;
                    }
                }
                case DRAW_MATRIX -> {
                    mMatrixUBO.upload(64, 64, uniformDataPtr);
                    uniformDataPtr += 64;
                }
                case DRAW_SMOOTH -> {
                    mSmoothUBO.upload(0, 4, uniformDataPtr);
                    uniformDataPtr += 4;
                }
                case DRAW_LAYER_PUSH -> {
                    assert framebuffer != null;
                    mLayerStack.push(mLayerAlphas.getInt(alphaIndex));
                    framebuffer.setDrawBuffer(++colorBuffer);
                    framebuffer.clearColorBuffer();
                    alphaIndex++;
                }
                case DRAW_LAYER_POP -> {
                    assert framebuffer != null;
                    GLFramebufferCompat resolve = GLFramebufferCompat.resolve(framebuffer,
                            colorBuffer, mWidth, mHeight);
                    framebuffer.setReadBuffer(GL_COLOR_ATTACHMENT0);
                    framebuffer.bindDraw();
                    GLTextureCompat layer = resolve.getAttachedTexture(GL_COLOR_ATTACHMENT0);

                    float alpha = mLayerStack.popInt() / 255f;
                    putRectColorUV(mLayerImageMemory, 0, 0, mWidth, mHeight,
                            1, 1, 1, alpha,
                            0, (float) mHeight / layer.getHeight(),
                            (float) mWidth / layer.getWidth(), 0);
                    mLayerImageMemory.flip();
                    mTextureMeshVertexBuffer.updateData(0, mLayerImageMemory.remaining(),
                            MemoryUtil.memAddress(mLayerImageMemory)
                    );
                    mLayerImageMemory.clear();

                    bindPipeline(COLOR_TEX_PRE, POS_COLOR_TEX)
                            .bindVertexBuffer(mTextureMeshVertexBuffer, 0);
                    bindSampler(null);
                    bindTexture(layer.get());
                    framebuffer.setDrawBuffer(--colorBuffer);
                    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
                    nDraws++;
                }
                case DRAW_CUSTOM -> {
                    var drawable = mCustoms.remove();
                    drawable.draw(mServer.getContext(), null);
                    drawable.close();
                    mServer.forceResetContext(Engine.GLBackendState.kPipeline);
                    glBindSampler(0, 0);
                    mCurrSampler = null;
                    mCurrTexture = 0;
                }
                case DRAW_GLOW_WAVE -> {
                    bindPipeline(GLOW_WAVE, POS_COLOR)
                            .bindVertexBuffer(mColorMeshVertexBuffer, 0);
                    mMatrixUBO.upload(128, 4, uniformDataPtr);
                    uniformDataPtr += 4;
                    glDrawArrays(GL_TRIANGLE_STRIP, posColorIndex, 4);
                    nDraws++;
                    posColorIndex += 4;
                }
                default -> throw new IllegalStateException("Unexpected draw op " + op);
            }
        }
        assert mLayerStack.isEmpty();
        assert mTextures.isEmpty();
        assert mCustoms.isEmpty();

        bindSampler(null);
        glStencilFunc(GL_ALWAYS, 0, 0xff);

        mDrawOps.clear();
        mDrawPrims.clear();
        mClipRefs.clear();
        mLayerAlphas.clear();
        mDrawTexts.clear();
        mUniformRingBuffer.clear();

        for (int i = 0; i < mTexturesToClean.size(); i++) {
            mTexturesToClean.get(i).unref();
        }
        mTexturesToClean.clear();

        stats.incNumDraws(nDraws);
        stats.incRenderPasses();

        return true;
    }

    @RenderThread
    private void uploadVertexBuffers() {
        if (mColorMeshBufferResized) {
            GLBuffer newBuffer = GLBuffer.make(mServer,
                    mColorMeshStagingBuffer.capacity(),
                    Engine.BufferUsageFlags.kVertex |
                            Engine.BufferUsageFlags.kDynamic);
            if (newBuffer == null) {
                throw new IllegalStateException("Failed to create color mesh buffer");
            }
            newBuffer.setLabel("ColorRectMesh");
            mColorMeshVertexBuffer = GLBuffer.move(mColorMeshVertexBuffer, newBuffer);
            ModernUI.LOGGER.info("Created new color mesh buffer: {}, size: {}",
                    newBuffer.getHandle(), newBuffer.getSize());
            mColorMeshBufferResized = false;
        }
        if (mColorMeshStagingBuffer.position() > 0) {
            mColorMeshStagingBuffer.flip();
            mColorMeshVertexBuffer.updateData(0, mColorMeshStagingBuffer.remaining(),
                    MemoryUtil.memAddress(mColorMeshStagingBuffer)
            );
            mColorMeshStagingBuffer.clear();
        }

        int preserveForLayer = TEXTURE_RECT_VERTEX_SIZE * 4;

        if (mTextureMeshBufferResized) {
            GLBuffer newBuffer = GLBuffer.make(mServer,
                    mTextureMeshStagingBuffer.capacity() + preserveForLayer,
                    Engine.BufferUsageFlags.kVertex |
                            Engine.BufferUsageFlags.kDynamic);
            if (newBuffer == null) {
                throw new IllegalStateException("Failed to create texture mesh buffer");
            }
            newBuffer.setLabel("TextureRectMesh");
            mTextureMeshVertexBuffer = GLBuffer.move(mTextureMeshVertexBuffer, newBuffer);
            ModernUI.LOGGER.info("Created new texture mesh buffer: {}, size: {}",
                    newBuffer.getHandle(), newBuffer.getSize());
            mTextureMeshBufferResized = false;
        }
        if (mTextureMeshStagingBuffer.position() > 0) {
            // preserve memory for layer rendering
            mTextureMeshStagingBuffer.flip();
            mTextureMeshVertexBuffer.updateData(preserveForLayer, mTextureMeshStagingBuffer.remaining(),
                    MemoryUtil.memAddress(mTextureMeshStagingBuffer)
            );
            mTextureMeshStagingBuffer.clear();
        }

        if (!mDrawTexts.isEmpty()) {
            for (DrawTextOp textOp : mDrawTexts) {
                textOp.writeMeshData(this);
            }
            if (mGlyphBufferResized) {
                GLBuffer newBuffer = GLBuffer.make(mServer,
                        mGlyphStagingBuffer.capacity(),
                        Engine.BufferUsageFlags.kVertex |
                                Engine.BufferUsageFlags.kDynamic);
                if (newBuffer == null) {
                    throw new IllegalStateException("Failed to create buffer for glyph mesh");
                }
                newBuffer.setLabel("GlyphMesh");
                mGlyphVertexBuffer = GLBuffer.move(mGlyphVertexBuffer, newBuffer);
                ModernUI.LOGGER.info("Created new glyph mesh buffer: {}, size: {}",
                        newBuffer.getHandle(), newBuffer.getSize());
                mGlyphBufferResized = false;
            }
            if (mGlyphStagingBuffer.position() > 0) {
                mGlyphStagingBuffer.flip();
                mGlyphVertexBuffer.updateData(0, mGlyphStagingBuffer.remaining(),
                        MemoryUtil.memAddress(mGlyphStagingBuffer)
                );
                mGlyphStagingBuffer.clear();
            }
        }

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

    private ByteBuffer checkColorMeshStagingBuffer() {
        if (mColorMeshStagingBuffer.remaining() < 48) {
            int newCap = grow(mColorMeshStagingBuffer.capacity());
            mColorMeshStagingBuffer = memRealloc(mColorMeshStagingBuffer, newCap);
            mColorMeshBufferResized = true;
            ModernUI.LOGGER.debug(MARKER, "Grow pos color buffer to {} bytes", newCap);
        }
        return mColorMeshStagingBuffer;
    }

    private ByteBuffer checkTextureMeshStagingBuffer() {
        if (mTextureMeshStagingBuffer.remaining() < TEXTURE_RECT_VERTEX_SIZE * 4) {
            int newCap = grow(mTextureMeshStagingBuffer.capacity());
            mTextureMeshStagingBuffer = memRealloc(mTextureMeshStagingBuffer, newCap);
            mTextureMeshBufferResized = true;
            ModernUI.LOGGER.debug(MARKER, "Grow pos color tex buffer to {} bytes", newCap);
        }
        return mTextureMeshStagingBuffer;
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
    private ByteBuffer checkGlyphStagingBuffer() {
        if (mGlyphStagingBuffer.remaining() < 64) {
            int newCap = grow(mGlyphStagingBuffer.capacity());
            mGlyphStagingBuffer = memRealloc(mGlyphStagingBuffer, newCap);
            mGlyphBufferResized = true;
            ModernUI.LOGGER.debug(MARKER, "Grow pos tex buffer to {} bytes", newCap);
        }
        return mGlyphStagingBuffer;
    }

    private ByteBuffer checkUniformStagingBuffer() {
        if (mUniformRingBuffer.remaining() < 64) {
            int newCap = grow(mUniformRingBuffer.capacity());
            mUniformRingBuffer = memRealloc(mUniformRingBuffer, newCap);
            ModernUI.LOGGER.debug(MARKER, "Grow general uniform buffer to {} bytes", newCap);
        }
        return mUniformRingBuffer;
    }

    public int getNativeMemoryUsage() {
        return mColorMeshStagingBuffer.capacity() + mTextureMeshStagingBuffer.capacity() + mGlyphStagingBuffer.capacity() + mUniformRingBuffer.capacity();
    }

    private static int grow(int cap) {
        return cap + (cap >> 1);
    }

    /**
     * "Draw" matrix. Update the model view matrix on render thread.
     */
    private void drawMatrix() {
        drawMatrix(getMatrix());
    }

    /**
     * "Draw" matrix. Update the model view matrix on render thread.
     *
     * @param matrix specified matrix
     */
    private void drawMatrix(@NonNull Matrix4 matrix) {
        if (!matrix.isApproxEqual(mLastMatrix)) {
            mLastMatrix.set(matrix);
            ByteBuffer buf = checkUniformStagingBuffer();
            matrix.store(buf);
            buf.position(buf.position() + 64);
            mDrawOps.add(DRAW_MATRIX);
        }
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

        Save s = sSavePool.acquire();
        if (s == null) {
            s = getSave().copy();
        } else {
            s.set(getSave());
        }
        mSaves.push(s);

        return saveCount;
    }

    @Override
    public int saveLayer(float left, float top, float right, float bottom, int alpha) {
        int saveCount = getSaveCount();

        Save s = sSavePool.acquire();
        if (s == null) {
            s = getSave().copy();
        } else {
            s.set(getSave());
        }
        mSaves.push(s);

        if (alpha <= 0) {
            // will be quick rejected
            s.mClip.setEmpty();
        } else if (alpha < 255 && s.mColorBuf < 3) {
            s.mColorBuf++;
            mLayerAlphas.add(alpha);
            mDrawOps.add(DRAW_LAYER_PUSH);
        }

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
        if (mSaves.size() < 1) {
            throw new IllegalStateException("Underflow in restore");
        }
        Save save = mSaves.poll();
        if (save.mClipRef != getSave().mClipRef) {
            restoreClipBatch(save.mClip);
        }
        if (save.mColorBuf != getSave().mColorBuf) {
            restoreLayer();
        }
        sSavePool.release(save);
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
        Deque<Save> stack = mSaves;

        Save lastPop = stack.pop();
        final int topRef = lastPop.mClipRef;
        final int topBuf = lastPop.mColorBuf;
        sSavePool.release(lastPop);

        while (stack.size() > saveCount) {
            lastPop = stack.pop();
            sSavePool.release(lastPop);
        }

        if (topRef != getSave().mClipRef) {
            restoreClipBatch(lastPop.mClip);
        }

        int bufDiff = topBuf - getSave().mColorBuf;
        while (bufDiff-- > 0) {
            restoreLayer();
        }
    }

    /**
     * Batch operation.
     *
     * @param b bounds
     * @see #clipRect(float, float, float, float)
     */
    private void restoreClipBatch(@NonNull Rect2i b) {
        /*int pointer = mDrawOps.size() - 1;
        int op;
        boolean skip = true;
        while ((op = mDrawOps.getInt(pointer)) != DRAW_CLIP_PUSH) {
            if (op < DRAW_CLIP_PUSH) {
                skip = false;
                break;
            }
            pointer--;
        }
        if (skip) {
            mClipRefs.removeInt(mClipRefs.size() - 1);
            for (int i = mDrawOps.size() - 1; i >= pointer; i--) {
                mDrawOps.removeInt(i);
            }
            return;
        }*/
        if (b.isEmpty()) {
            // taking the opposite number means that the clip rect is not to drawn
            mClipRefs.add(-getSave().mClipRef);
        } else {
            drawMatrix(RESET_MATRIX);
            // must have a color
            putRectColor(b.mLeft, b.mTop, b.mRight, b.mBottom, 1, 1, 1, 1);
            mClipRefs.add(getSave().mClipRef);
        }
        mDrawOps.add(DRAW_CLIP_POP);
    }

    private void restoreLayer() {
        drawMatrix(RESET_MATRIX);
        mDrawOps.add(DRAW_LAYER_POP);
        drawMatrix();
    }


    @Override
    public boolean clipRect(float left, float top, float right, float bottom) {
        final Save save = getSave();
        // empty rect, ignore it
        if (right <= left || bottom <= top) {
            return !save.mClip.isEmpty();
        }
        // already empty, return false
        if (save.mClip.isEmpty()) {
            return false;
        }
        var temp = mTmpRectF;
        temp.set(left, top, right, bottom);
        temp.inset(-1, -1);
        getMatrix().mapRect(temp);

        var test = mTmpRectI;
        temp.roundOut(test);

        // not empty and not changed, return true
        if (test.contains(save.mClip)) {
            return true;
        }

        boolean intersects = save.mClip.intersect(test);
        save.mClipRef++;
        if (intersects) {
            drawMatrix();
            // updating stencil must have a color
            putRectColor(left - 1, top - 1, right + 1, bottom + 1, 1.0f, 1.0f, 1.0f, 1.0f);
            mClipRefs.add(save.mClipRef);
        } else {
            // empty
            mClipRefs.add(-save.mClipRef);
            save.mClip.setEmpty();
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
        final var clip = getSave().mClip;
        // already empty, reject
        if (clip.isEmpty()) {
            return true;
        }

        var temp = mTmpRectF;
        temp.set(left, top, right, bottom);
        getMatrix().mapRect(temp);

        var test = mTmpRectI;
        temp.roundOut(test);
        return !Rect2i.intersects(clip, test);
    }

    private void putRectColor(float left, float top, float right, float bottom, @NonNull Paint paint) {
        putRectColor(left, top, right, bottom, paint.r(), paint.g(), paint.b(), paint.a());
    }

    private void putRectColorGrad(float left, float top, float right, float bottom,
                                  int colorUL, int colorUR, int colorLR, int colorLL) {
        final ByteBuffer buffer = checkColorMeshStagingBuffer();

        // CCW
        int color = colorLL;
        float alpha = (color >>> 24);
        float red = ((color >> 16) & 0xff) / 255.0f;
        float green = ((color >> 8) & 0xff) / 255.0f;
        float blue = (color & 0xff) / 255.0f;
        byte r = (byte) (red * alpha + 0.5f);
        byte g = (byte) (green * alpha + 0.5f);
        byte b = (byte) (blue * alpha + 0.5f);
        byte a = (byte) (alpha + 0.5f);
        buffer.putFloat(left)
                .putFloat(bottom)
                .put(r).put(g).put(b).put(a);

        color = colorLR;
        alpha = (color >>> 24);
        red = ((color >> 16) & 0xff) / 255.0f;
        green = ((color >> 8) & 0xff) / 255.0f;
        blue = (color & 0xff) / 255.0f;
        r = (byte) (red * alpha + 0.5f);
        g = (byte) (green * alpha + 0.5f);
        b = (byte) (blue * alpha + 0.5f);
        a = (byte) (alpha + 0.5f);
        buffer.putFloat(right)
                .putFloat(bottom)
                .put(r).put(g).put(b).put(a);

        color = colorUL;
        alpha = (color >>> 24);
        red = ((color >> 16) & 0xff) / 255.0f;
        green = ((color >> 8) & 0xff) / 255.0f;
        blue = (color & 0xff) / 255.0f;
        r = (byte) (red * alpha + 0.5f);
        g = (byte) (green * alpha + 0.5f);
        b = (byte) (blue * alpha + 0.5f);
        a = (byte) (alpha + 0.5f);
        buffer.putFloat(left)
                .putFloat(top)
                .put(r).put(g).put(b).put(a);

        color = colorUR;
        alpha = (color >>> 24);
        red = ((color >> 16) & 0xff) / 255.0f;
        green = ((color >> 8) & 0xff) / 255.0f;
        blue = (color & 0xff) / 255.0f;
        r = (byte) (red * alpha + 0.5f);
        g = (byte) (green * alpha + 0.5f);
        b = (byte) (blue * alpha + 0.5f);
        a = (byte) (alpha + 0.5f);
        buffer.putFloat(right)
                .putFloat(top)
                .put(r).put(g).put(b).put(a);
    }

    private void putRectColor(float left, float top, float right, float bottom,
                              float red, float green, float blue, float alpha) {
        ByteBuffer buffer = checkColorMeshStagingBuffer();
        float factor = alpha * 255.0f;
        byte r = (byte) (red * factor + 0.5f);
        byte g = (byte) (green * factor + 0.5f);
        byte b = (byte) (blue * factor + 0.5f);
        byte a = (byte) (factor + 0.5f);
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

    private void putRectColorUV(float left, float top, float right, float bottom, @Nullable Paint paint,
                                float u1, float v1, float u2, float v2) {
        final ByteBuffer buffer = checkTextureMeshStagingBuffer();
        if (paint == null) {
            putRectColorUV(buffer, left, top, right, bottom, 1, 1, 1, 1, u1, v1, u2, v2);
        } else if (false) {
            //TODO add four color gradient for image
            final int[] colors = new int[4];

            // CCW
            int color = colors[3];
            float alpha = (color >>> 24);
            float red = ((color >> 16) & 0xff) / 255.0f;
            float green = ((color >> 8) & 0xff) / 255.0f;
            float blue = (color & 0xff) / 255.0f;
            byte r = (byte) (red * alpha + 0.5f);
            byte g = (byte) (green * alpha + 0.5f);
            byte b = (byte) (blue * alpha + 0.5f);
            byte a = (byte) (alpha + 0.5f);
            buffer.putFloat(left)
                    .putFloat(bottom)
                    .put(r).put(g).put(b).put(a)
                    .putFloat(u1).putFloat(v2);

            color = colors[2];
            alpha = (color >>> 24);
            red = ((color >> 16) & 0xff) / 255.0f;
            green = ((color >> 8) & 0xff) / 255.0f;
            blue = (color & 0xff) / 255.0f;
            r = (byte) (red * alpha + 0.5f);
            g = (byte) (green * alpha + 0.5f);
            b = (byte) (blue * alpha + 0.5f);
            a = (byte) (alpha + 0.5f);
            buffer.putFloat(right)
                    .putFloat(bottom)
                    .put(r).put(g).put(b).put(a)
                    .putFloat(u2).putFloat(v2);

            color = colors[0];
            alpha = (color >>> 24);
            red = ((color >> 16) & 0xff) / 255.0f;
            green = ((color >> 8) & 0xff) / 255.0f;
            blue = (color & 0xff) / 255.0f;
            r = (byte) (red * alpha + 0.5f);
            g = (byte) (green * alpha + 0.5f);
            b = (byte) (blue * alpha + 0.5f);
            a = (byte) (alpha + 0.5f);
            buffer.putFloat(left)
                    .putFloat(top)
                    .put(r).put(g).put(b).put(a)
                    .putFloat(u1).putFloat(v1);

            color = colors[1];
            alpha = (color >>> 24);
            red = ((color >> 16) & 0xff) / 255.0f;
            green = ((color >> 8) & 0xff) / 255.0f;
            blue = (color & 0xff) / 255.0f;
            r = (byte) (red * alpha + 0.5f);
            g = (byte) (green * alpha + 0.5f);
            b = (byte) (blue * alpha + 0.5f);
            a = (byte) (alpha + 0.5f);
            buffer.putFloat(right)
                    .putFloat(top)
                    .put(r).put(g).put(b).put(a)
                    .putFloat(u2).putFloat(v1);
        } else {
            putRectColorUV(buffer, left, top, right, bottom, paint.r(), paint.g(), paint.b(), paint.a(),
                    u1, v1, u2, v2);
        }
    }

    private void putRectColorUV(@NonNull ByteBuffer buffer,
                                float left, float top, float right, float bottom,
                                float red, float green, float blue, float alpha,
                                float u1, float v1, float u2, float v2) {
        float factor = alpha * 255.0f;
        byte r = (byte) (red * factor + 0.5f);
        byte g = (byte) (green * factor + 0.5f);
        byte b = (byte) (blue * factor + 0.5f);
        byte a = (byte) (factor + 0.5f);
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
    private void putGlyph(@NonNull BakedGlyph glyph, float left, float top) {
        ByteBuffer buffer = checkGlyphStagingBuffer();
        left += glyph.x;
        top += glyph.y;
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

    /**
     * Record an operation to update smooth radius later for geometries that use smooth radius.
     *
     * @param smooth current smooth
     */
    private void drawSmooth(float smooth) {
        if (smooth != mLastSmooth) {
            mLastSmooth = smooth;
            checkUniformStagingBuffer()
                    .putFloat(smooth);
            mDrawOps.add(DRAW_SMOOTH);
        }
    }

    @Override
    public void drawMesh(@NonNull VertexMode mode,
                         @NonNull FloatBuffer pos,
                         @Nullable IntBuffer color,
                         @Nullable FloatBuffer tex,
                         @Nullable ShortBuffer indices,
                         @Nullable Blender blender,
                         @NonNull Paint paint) {
        int numVertices = pos.remaining() / 2;
        if (numVertices > 65535) {
            throw new IllegalArgumentException("Number of vertices is too big: " + numVertices);
        }
        if (color != null && color.remaining() < numVertices) {
            throw new BufferUnderflowException();
        }
        if (tex != null && tex.remaining() < numVertices * 2) {
            throw new BufferUnderflowException();
        }
        //TODO add support
        if (tex != null || indices != null || blender != null) {
            throw new UnsupportedOperationException();
        }

        int prim;
        switch (mode) {
            case POINTS -> {
                if (numVertices < 1) return;
                prim = GLCore.GL_POINTS;
            }
            case LINES, LINE_STRIP -> {
                if (numVertices < 2) return;
                if (mode == VertexMode.LINES) {
                    numVertices -= numVertices % 2;
                }
                prim = (mode == VertexMode.LINES
                        ? GLCore.GL_LINES
                        : GLCore.GL_LINE_STRIP);
            }
            default -> {
                if (numVertices < 3) return;
                if (mode == VertexMode.TRIANGLES) {
                    numVertices -= numVertices % 3;
                }
                prim = (mode == VertexMode.TRIANGLES
                        ? GLCore.GL_TRIANGLES
                        : GLCore.GL_TRIANGLE_STRIP);
            }
        }

        drawMatrix();
        mDrawOps.add(DRAW_PRIM);
        mDrawPrims.add(numVertices | (prim << 16));

        ByteBuffer buffer = checkColorMeshStagingBuffer();
        if (color != null) {
            int pb = pos.position(), cb = color.position();
            for (int i = 0; i < numVertices; i++) {
                int col = color.get(cb++);
                byte a = (byte) (col >>> 24);
                float factor = (a & 0xFF) / 255.0f;
                byte r = (byte) (((col >> 16) & 0xff) * factor + 0.5f);
                byte g = (byte) (((col >> 8) & 0xff) * factor + 0.5f);
                byte b = (byte) ((col & 0xff) * factor + 0.5f);
                buffer.putFloat(pos.get(pb++))
                        .putFloat(pos.get(pb++))
                        .put(r).put(g).put(b).put(a);
            }
        } else {
            float factor = paint.a();
            byte a = (byte) (factor * 255.0f + 0.5f);
            factor *= 255.0f;
            byte r = (byte) (paint.r() * factor + 0.5f);
            byte g = (byte) (paint.g() * factor + 0.5f);
            byte b = (byte) (paint.b() * factor + 0.5f);
            int pb = pos.position();
            for (int i = 0; i < numVertices; i++) {
                buffer.putFloat(pos.get(pb++))
                        .putFloat(pos.get(pb++))
                        .put(r).put(g).put(b).put(a);
            }
        }
    }

    @Override
    public void drawArc(float cx, float cy, float radius, float startAngle,
                        float sweepAngle, @NonNull Paint paint) {
        if (MathUtil.isApproxZero(sweepAngle) || radius < 0.0001f) {
            return;
        }
        if (sweepAngle >= 360) {
            drawCircle(cx, cy, radius, paint);
            return;
        }
        sweepAngle %= 360;
        final float middleAngle = (startAngle % 360) + sweepAngle * 0.5f;
        if (paint.getStyle() == Paint.FILL) {
            drawArcFill(cx, cy, radius, middleAngle, sweepAngle, paint);
        } else {
            drawArcStroke(cx, cy, radius, middleAngle, sweepAngle, paint);
        }
    }

    private void drawArcFill(float cx, float cy, float radius, float middleAngle,
                             float sweepAngle, @NonNull Paint paint) {
        if (quickReject(cx - radius, cy - radius, cx + radius, cy + radius)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(radius, paint.getSmoothWidth() / 2));
        putRectColor(cx - radius, cy - radius, cx + radius, cy + radius, paint);
        checkUniformStagingBuffer()
                .putFloat(cx)
                .putFloat(cy)
                .putFloat(middleAngle)
                .putFloat(sweepAngle)
                .putFloat(radius);
        mDrawOps.add(DRAW_ARC_FILL);
    }

    private void drawArcStroke(float cx, float cy, float radius, float middleAngle,
                               float sweepAngle, @NonNull Paint paint) {
        float strokeRadius = Math.min(radius, paint.getStrokeWidth() * 0.5f);
        if (strokeRadius < 0.0001f) {
            return;
        }
        float maxRadius = radius + strokeRadius;
        if (quickReject(cx - maxRadius, cy - maxRadius, cx + maxRadius, cy + maxRadius)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(strokeRadius, paint.getSmoothWidth() / 2));
        putRectColor(cx - maxRadius, cy - maxRadius, cx + maxRadius, cy + maxRadius, paint);
        checkUniformStagingBuffer()
                .putFloat(cx)
                .putFloat(cy)
                .putFloat(middleAngle)
                .putFloat(sweepAngle)
                .putFloat(radius)
                .putFloat(strokeRadius);
        mDrawOps.add(DRAW_ARC_STROKE);
    }

    @Override
    public void drawPie(float cx, float cy, float radius, float startAngle,
                        float sweepAngle, @NonNull Paint paint) {
        if (MathUtil.isApproxZero(sweepAngle) || radius < 0.0001f) {
            return;
        }
        if (sweepAngle >= 360) {
            drawCircle(cx, cy, radius, paint);
            return;
        }
        sweepAngle %= 360;
        final float middleAngle = (startAngle % 360) + sweepAngle * 0.5f;
        if (paint.getStyle() == Paint.FILL) {
            drawPieFill(cx, cy, radius, middleAngle, sweepAngle, paint);
        } else {
            drawPieStroke(cx, cy, radius, middleAngle, sweepAngle, paint);
        }
    }

    private void drawPieFill(float cx, float cy, float radius, float middleAngle,
                             float sweepAngle, @NonNull Paint paint) {
        if (quickReject(cx - radius, cy - radius, cx + radius, cy + radius)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(radius, paint.getSmoothWidth() / 2));
        putRectColor(cx - radius, cy - radius, cx + radius, cy + radius, paint);
        checkUniformStagingBuffer()
                .putFloat(cx)
                .putFloat(cy)
                .putFloat(middleAngle)
                .putFloat(sweepAngle)
                .putFloat(radius);
        mDrawOps.add(DRAW_PIE_FILL);
    }

    private void drawPieStroke(float cx, float cy, float radius, float middleAngle,
                               float sweepAngle, @NonNull Paint paint) {
        float strokeRadius = Math.min(radius, paint.getStrokeWidth() * 0.5f);
        if (strokeRadius < 0.0001f) {
            return;
        }
        float maxRadius = radius + strokeRadius;
        if (quickReject(cx - maxRadius, cy - maxRadius, cx + maxRadius, cy + maxRadius)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(strokeRadius, paint.getSmoothWidth() / 2));
        putRectColor(cx - maxRadius, cy - maxRadius, cx + maxRadius, cy + maxRadius, paint);
        checkUniformStagingBuffer()
                .putFloat(cx)
                .putFloat(cy)
                .putFloat(middleAngle)
                .putFloat(sweepAngle)
                .putFloat(radius)
                .putFloat(strokeRadius);
        mDrawOps.add(DRAW_PIE_STROKE);
    }

    @Override
    public void drawBezier(float x0, float y0, float x1, float y1, float x2, float y2, @NonNull Paint paint) {
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
        drawSmooth(Math.min(strokeRadius, paint.getSmoothWidth() / 2));
        putRectColor(left, top, right, bottom, paint);
        checkUniformStagingBuffer()
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
    public void drawCircle(float cx, float cy, float radius, @NonNull Paint paint) {
        if (radius < 0.0001f) {
            return;
        }
        if (paint.getStyle() == Paint.FILL) {
            drawCircleFill(cx, cy, radius, paint);
        } else {
            drawCircleStroke(cx, cy, radius, paint);
        }
    }

    private void drawCircleFill(float cx, float cy, float radius, @NonNull Paint paint) {
        if (quickReject(cx - radius, cy - radius, cx + radius, cy + radius)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(radius, paint.getSmoothWidth() / 2));
        putRectColor(cx - radius - 1, cy - radius - 1, cx + radius + 1, cy + radius + 1, paint);
        checkUniformStagingBuffer()
                .putFloat(cx)
                .putFloat(cy)
                .putFloat(radius);
        mDrawOps.add(DRAW_CIRCLE_FILL);
    }

    private void drawCircleStroke(float cx, float cy, float radius, @NonNull Paint paint) {
        float strokeRadius = Math.min(radius, paint.getStrokeWidth() * 0.5f);
        if (strokeRadius < 0.0001f) {
            return;
        }
        float maxRadius = radius + strokeRadius;
        if (quickReject(cx - maxRadius, cy - maxRadius, cx + maxRadius, cy + maxRadius)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(strokeRadius, paint.getSmoothWidth() / 2));
        putRectColor(cx - maxRadius - 1, cy - maxRadius - 1, cx + maxRadius + 1, cy + maxRadius + 1, paint);
        checkUniformStagingBuffer()
                .putFloat(cx)
                .putFloat(cy)
                .putFloat(radius - strokeRadius) // inner radius
                .putFloat(maxRadius);
        mDrawOps.add(DRAW_CIRCLE_STROKE);
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom, @NonNull Paint paint) {
        if (quickReject(left, top, right, bottom)) {
            return;
        }
        drawMatrix();
        putRectColor(left, top, right, bottom, paint);
        mDrawOps.add(DRAW_RECT);
    }

    @Override
    public void drawRectGradient(float left, float top, float right, float bottom,
                                 int colorUL, int colorUR,
                                 int colorLR, int colorLL, Paint paint) {
        if (quickReject(left, top, right, bottom)) {
            return;
        }
        drawMatrix();
        putRectColorGrad(left, top, right, bottom, colorUL, colorUR, colorLR, colorLL);
        mDrawOps.add(DRAW_RECT);
    }

    // test stuff :p
    public void drawGlowWave(float left, float top, float right, float bottom) {
        if (quickReject(left, top, right, bottom)) {
            return;
        }
        save();
        float aspect = (right - left) / (bottom - top);
        scale((right - left) * 0.5f, (right - left) * 0.5f,
                -1 - left / ((right - left) * 0.5f),
                (-1 - top / ((bottom - top) * 0.5f)) / aspect);
        drawMatrix();
        putRectColor(-1, -1 / aspect, 1, 1 / aspect, 1, 1, 1, 1);
        ByteBuffer buffer = checkUniformStagingBuffer();
        buffer.putFloat((float) GLFW.glfwGetTime());
        restore();
        mDrawOps.add(DRAW_GLOW_WAVE);
    }

    @Override
    public void drawImage(@NonNull Image image, float left, float top, @Nullable Paint paint) {
        var view = image.asTextureView();
        if (view == null) {
            return;
        }
        float right = left + view.getWidth();
        float bottom = top + view.getHeight();
        if (quickReject(left, top, right, bottom)) {
            return;
        }
        drawMatrix();
        putRectColorUV(left, top, right, bottom, paint, 0, 0, 1, 1);
        view.refProxy();
        mTextures.add(view);
        mDrawOps.add(DRAW_IMAGE);
    }

    // this is only used for offscreen
    public void drawLayer(@NonNull GLTextureCompat texture, float w, float h, float alpha, boolean flipY) {
        int target = texture.getTarget();
        if (target == GL_TEXTURE_2D) {
            drawMatrix();
            putRectColorUV(checkTextureMeshStagingBuffer(), 0, 0, w, h, 1, 1, 1, alpha,
                    0, flipY ? h / texture.getHeight() : 0,
                    w / texture.getWidth(), flipY ? 0 : h / texture.getHeight());
            mTextures.add(texture);
            mDrawOps.add(DRAW_IMAGE_LAYER);
        } else {
            ModernUI.LOGGER.warn(MARKER, "Cannot draw texture target {}", target);
        }
    }

    @Override
    public void drawImage(@NonNull Image image, float srcLeft, float srcTop, float srcRight, float srcBottom,
                          float dstLeft, float dstTop, float dstRight, float dstBottom, @Nullable Paint paint) {
        if (quickReject(dstLeft, dstTop, dstRight, dstBottom)) {
            return;
        }
        var view = image.asTextureView();
        if (view == null) {
            return;
        }
        srcLeft = Math.max(0, srcLeft);
        srcTop = Math.max(0, srcTop);
        int w = view.getWidth();
        int h = view.getHeight();
        srcRight = Math.min(srcRight, w);
        srcBottom = Math.min(srcBottom, h);
        if (srcRight <= srcLeft || srcBottom <= srcTop) {
            return;
        }
        drawMatrix();
        putRectColorUV(dstLeft, dstTop, dstRight, dstBottom, paint,
                srcLeft / w, srcTop / h, srcRight / w, srcBottom / h);
        view.refProxy();
        mTextures.add(view);
        mDrawOps.add(DRAW_IMAGE);
    }

    public void drawTexture(@NonNull GLTextureCompat texture, float srcLeft, float srcTop, float srcRight,
                            float srcBottom,
                            float dstLeft, float dstTop, float dstRight, float dstBottom) {
        if (quickReject(dstLeft, dstTop, dstRight, dstBottom)) {
            return;
        }
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
        putRectColorUV(dstLeft, dstTop, dstRight, dstBottom, null,
                srcLeft / w, srcTop / h, srcRight / w, srcBottom / h);
        mTextures.add(texture);
        mDrawOps.add(DRAW_IMAGE);
    }

    @Override
    public void drawLine(float startX, float startY, float stopX, float stopY,
                         float thickness, @NonNull Paint paint) {
        float t = thickness * 0.5f;
        if (t <= 0) {
            // hairline
            drawMatrix();
            mDrawOps.add(DRAW_PRIM);
            mDrawPrims.add(2 | (GLCore.GL_LINES << 16));

            ByteBuffer buffer = checkColorMeshStagingBuffer();
            float factor = paint.a();
            byte a = (byte) (factor * 255.0f + 0.5f);
            factor *= 255.0f;
            byte r = (byte) (paint.r() * factor + 0.5f);
            byte g = (byte) (paint.g() * factor + 0.5f);
            byte b = (byte) (paint.b() * factor + 0.5f);
            buffer.putFloat(startX)
                    .putFloat(startY)
                    .put(r).put(g).put(b).put(a);
            buffer.putFloat(stopX)
                    .putFloat(stopY)
                    .put(r).put(g).put(b).put(a);
            return;
        }
        float left = Math.min(startX, stopX) - t;
        float top = Math.min(startY, stopY) - t;
        float right = Math.max(startX, stopX) + t;
        float bottom = Math.max(startY, stopY) + t;
        if (paint.getStyle() == Paint.FILL) {
            drawLineFill(startX, startY, stopX, stopY, t, paint, left, top, right, bottom);
        } else {
            drawLineStroke(startX, startY, stopX, stopY, t, paint, left, top, right, bottom);
        }
    }

    private void drawLineFill(float startX, float startY, float stopX, float stopY,
                              float radius, @NonNull Paint paint,
                              float left, float top, float right, float bottom) {
        if (quickReject(left, top, right, bottom)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(radius, paint.getSmoothWidth() / 2));
        putRectColor(left - 1, top - 1, right + 1, bottom + 1, paint);
        ByteBuffer buffer = checkUniformStagingBuffer();
        buffer.putFloat(startX)
                .putFloat(startY)
                .putFloat(stopX)
                .putFloat(stopY);
        buffer.putFloat(radius);
        mDrawOps.add(DRAW_ROUND_LINE_FILL);
    }

    private void drawLineStroke(float startX, float startY, float stopX, float stopY,
                                float radius, @NonNull Paint paint,
                                float left, float top, float right, float bottom) {
        float strokeRadius = Math.min(radius, paint.getStrokeWidth() * 0.5f);
        if (strokeRadius < 0.0001f) {
            return;
        }
        if (quickReject(left - strokeRadius, top - strokeRadius, right + strokeRadius, bottom + strokeRadius)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(strokeRadius, paint.getSmoothWidth() / 2));
        putRectColor(left - strokeRadius - 1, top - strokeRadius - 1, right + strokeRadius + 1,
                bottom + strokeRadius + 1, paint);
        ByteBuffer buffer = checkUniformStagingBuffer();
        buffer.putFloat(startX)
                .putFloat(startY)
                .putFloat(stopX)
                .putFloat(stopY);
        buffer.putFloat(radius)
                .putFloat(strokeRadius);
        mDrawOps.add(DRAW_ROUND_LINE_STROKE);
    }

    public void drawRoundLine(float startX, float startY, float stopX, float stopY, @NonNull Paint paint) {
        float t = paint.getStrokeWidth() * 0.5f;
        if (t < 0.0001f) {
            return;
        }
        if (MathUtil.isApproxEqual(startX, stopX)) {
            if (MathUtil.isApproxEqual(startY, stopY)) {
                drawCircleFill(startX, startY, t, paint);
            } else {
                // vertical
                float top = Math.min(startY, stopY);
                float bottom = Math.max(startY, stopY);
                drawRoundRectFill(startX - t, top - t, startX + t, bottom + t, 0, 0, 0, 0, false, t, 0, paint);
            }
        } else if (MathUtil.isApproxEqual(startY, stopY)) {
            // horizontal
            float left = Math.min(startX, stopX);
            float right = Math.max(startX, stopX);
            drawRoundRectFill(left - t, startY - t, right + t, startY + t, 0, 0, 0, 0, false, t, 0, paint);
        } else {
            float cx = (stopX + startX) * 0.5f;
            float cy = (stopY + startY) * 0.5f;
            float ang = MathUtil.atan2(stopY - startY, stopX - startX);
            save();
            Matrix4 mat = getMatrix();
            // rotate the round rect
            mat.preTranslate(cx, cy, 0);
            mat.preRotateZ(ang);
            mat.preTranslate(-cx, -cy, 0);
            // rotate positions to horizontal
            float sin = MathUtil.sin(-ang);
            float cos = MathUtil.cos(-ang);
            float left = (startX - cx) * cos - (startY - cy) * sin + cx;
            float right = (stopX - cx) * cos - (stopY - cy) * sin + cx;
            drawRoundRectFill(left - t, cy - t, right + t, cy + t, 0, 0, 0, 0, false, t, 0, paint);
            restore();
        }
    }

    @Override
    public void drawRoundRect(float left, float top, float right, float bottom, float radius,
                              int sides, @NonNull Paint paint) {
        radius = Math.min(radius, Math.min(right - left, bottom - top) * 0.5f);
        if (radius < 0) {
            radius = 0;
        }
        if (paint.getStyle() == Paint.FILL) {
            drawRoundRectFill(left, top, right, bottom, 0, 0, 0, 0, false, radius, sides, paint);
        } else {
            drawRoundRectStroke(left, top, right, bottom, 0, 0, 0, 0, false, radius, sides, paint);
        }
    }

    @Override
    public void drawRoundRectGradient(float left, float top, float right, float bottom,
                                      int colorUL, int colorUR,
                                      int colorLR, int colorLL, float radius, Paint paint) {
        radius = Math.min(radius, Math.min(right - left, bottom - top) * 0.5f);
        if (radius < 0) {
            radius = 0;
        }
        if (paint.getStyle() == Paint.FILL) {
            drawRoundRectFill(left, top, right, bottom, colorUL, colorUR, colorLR, colorLL, true, radius, 0, paint);
        } else {
            drawRoundRectStroke(left, top, right, bottom, colorUL, colorUR, colorLR, colorLL, true, radius, 0, paint);
        }
    }

    private void drawRoundRectFill(float left, float top, float right, float bottom,
                                   int colorUL, int colorUR,
                                   int colorLR, int colorLL, boolean useGrad,
                                   float radius, int sides, @NonNull Paint paint) {
        if (quickReject(left, top, right, bottom)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(radius, paint.getSmoothWidth() / 2));
        if (useGrad) {
            putRectColorGrad(left - 1, top - 1, right + 1, bottom + 1,
                    colorUL, colorUR, colorLR, colorLL);
        } else {
            putRectColor(left - 1, top - 1, right + 1, bottom + 1, paint);
        }
        ByteBuffer buffer = checkUniformStagingBuffer();
        if ((sides & Gravity.RIGHT) == Gravity.RIGHT) {
            buffer.putFloat(left);
        } else {
            buffer.putFloat(left + radius);
        }
        if ((sides & Gravity.BOTTOM) == Gravity.BOTTOM) {
            buffer.putFloat(top);
        } else {
            buffer.putFloat(top + radius);
        }
        if ((sides & Gravity.LEFT) == Gravity.LEFT) {
            buffer.putFloat(right);
        } else {
            buffer.putFloat(right - radius);
        }
        if ((sides & Gravity.TOP) == Gravity.TOP) {
            buffer.putFloat(bottom);
        } else {
            buffer.putFloat(bottom - radius);
        }
        buffer.putFloat(radius);
        mDrawOps.add(DRAW_ROUND_RECT_FILL);
    }

    private void drawRoundRectStroke(float left, float top, float right, float bottom,
                                     int colorUL, int colorUR,
                                     int colorLR, int colorLL, boolean useGrad,
                                     float radius, int sides, @NonNull Paint paint) {
        float strokeRadius = Math.min(radius, paint.getStrokeWidth() * 0.5f);
        if (strokeRadius < 0.0001f) {
            return;
        }
        if (quickReject(left - strokeRadius, top - strokeRadius, right + strokeRadius, bottom + strokeRadius)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(strokeRadius, paint.getSmoothWidth() / 2));
        if (useGrad) {
            putRectColorGrad(left - strokeRadius - 1, top - strokeRadius - 1, right + strokeRadius + 1,
                    bottom + strokeRadius + 1, colorUL, colorUR, colorLR, colorLL);
        } else {
            putRectColor(left - strokeRadius - 1, top - strokeRadius - 1, right + strokeRadius + 1,
                    bottom + strokeRadius + 1, paint);
        }
        ByteBuffer buffer = checkUniformStagingBuffer();
        if ((sides & Gravity.RIGHT) == Gravity.RIGHT) {
            buffer.putFloat(left);
        } else {
            buffer.putFloat(left + radius);
        }
        if ((sides & Gravity.BOTTOM) == Gravity.BOTTOM) {
            buffer.putFloat(top);
        } else {
            buffer.putFloat(top + radius);
        }
        if ((sides & Gravity.LEFT) == Gravity.LEFT) {
            buffer.putFloat(right);
        } else {
            buffer.putFloat(right - radius);
        }
        if ((sides & Gravity.TOP) == Gravity.TOP) {
            buffer.putFloat(bottom);
        } else {
            buffer.putFloat(bottom - radius);
        }
        buffer.putFloat(radius)
                .putFloat(strokeRadius);
        mDrawOps.add(DRAW_ROUND_RECT_STROKE);
    }

    @Override
    public void drawRoundImage(@NonNull Image image, float left, float top, float radius, @NonNull Paint paint) {
        if (radius < 0) {
            radius = 0;
        }
        var view = image.asTextureView();
        if (view == null) {
            return;
        }
        float right = left + view.getWidth();
        float bottom = top + view.getHeight();
        if (quickReject(left, top, right, bottom)) {
            return;
        }
        drawMatrix();
        drawSmooth(Math.min(radius, paint.getSmoothWidth() / 2));
        putRectColorUV(left, top, right, bottom, paint,
                0, 0, 1, 1);
        checkUniformStagingBuffer()
                .putFloat(left + radius - 1)
                .putFloat(top + radius - 1)
                .putFloat(right - radius + 1)
                .putFloat(bottom - radius + 1)
                .putFloat(radius);
        view.refProxy();
        mTextures.add(view);
        mDrawOps.add(DRAW_ROUND_IMAGE);
    }

    @Override
    public void drawGlyphs(@NonNull int[] glyphs, int glyphOffset,
                           @NonNull float[] positions, int positionOffset,
                           int glyphCount, @NonNull Font font,
                           float x, float y,
                           @NonNull Paint paint) {
        if (font instanceof StandardFont ff) {
            drawMatrix();
            int color = paint.getColor();
            float alpha = (color >>> 24) / 255.0f;
            float red = ((color >> 16) & 0xff) / 255.0f;
            float green = ((color >> 8) & 0xff) / 255.0f;
            float blue = (color & 0xff) / 255.0f;
            mDrawTexts.add(new DrawTextOp(glyphs, glyphOffset,
                    positions, positionOffset, glyphCount,
                    x, y, ff.chooseFont(paint.getFontSize())));
            checkUniformStagingBuffer()
                    .putFloat(red * alpha)
                    .putFloat(green * alpha)
                    .putFloat(blue * alpha)
                    .putFloat(alpha);
            mDrawOps.add(DRAW_TEXT);
        }
    }

    private static class DrawTextOp {

        private final int[] mGlyphs;
        private final int mGlyphOffset;
        private final float[] mPositions;
        private final int mPositionOffset;
        private final int mGlyphCount;
        private final float mOffsetX;
        private final float mOffsetY;
        private final java.awt.Font mFont;

        private int mTexture;
        private int mVisibleGlyphCount;

        public DrawTextOp(int[] glyphs, int glyphOffset, float[] positions, int positionOffset, int glyphCount,
                          float offsetX, float offsetY, java.awt.Font font) {
            mGlyphs = glyphs;
            mGlyphOffset = glyphOffset;
            mPositions = positions;
            mPositionOffset = positionOffset;
            mGlyphCount = glyphCount;
            mOffsetX = offsetX;
            mOffsetY = offsetY;
            mFont = font;
        }

        private void writeMeshData(@NonNull GLSurfaceCanvas canvas) {
            GlyphManager glyphManager = GlyphManager.getInstance();
            final int[] glyphs = mGlyphs;
            int glyphOffset = mGlyphOffset;
            final float[] positions = mPositions;
            int positionOffset = mPositionOffset;
            int visibleGlyphCount = 0;
            for (int i = 0; i < mGlyphCount; i++) {
                BakedGlyph bakedGlyph = glyphManager.lookupGlyph(mFont, glyphs[glyphOffset++]);
                if (bakedGlyph != null) {
                    canvas.putGlyph(bakedGlyph,
                            mOffsetX + positions[positionOffset++],
                            mOffsetY + positions[positionOffset++]);
                    visibleGlyphCount++;
                } else {
                    positionOffset += 2;
                }
            }
            mTexture = glyphManager.getCurrentTexture(Engine.MASK_FORMAT_A8);
            mVisibleGlyphCount = visibleGlyphCount;
        }
    }

    /**
     * Draw something custom. Do not break any state of current OpenGL context or GLCanvas in the future.
     *
     * @param drawable the custom drawable
     */
    public void drawCustomDrawable(@NonNull CustomDrawable drawable, @Nullable Matrix4 matrix) {
        var viewMatrix = getMatrix().clone();
        if (matrix != null) {
            viewMatrix.preConcat(matrix);
        }
        var draw = drawable.snapDrawHandler(Engine.BackendApi.kOpenGL,
                viewMatrix, getSave().mClip, mInfo);
        if (draw != null) {
            mCustoms.add(draw);
            mDrawOps.add(DRAW_CUSTOM);
        }
    }
}
