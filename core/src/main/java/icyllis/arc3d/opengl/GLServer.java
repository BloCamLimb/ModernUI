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

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.Rect2i;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;
import icyllis.modernui.graphics.ImageInfo;
import icyllis.modernui.graphics.RefCnt;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

import static icyllis.arc3d.opengl.GLCore.*;
import static org.lwjgl.system.MemoryUtil.memPutInt;

/**
 * The OpenGL graphics engine/device/server.
 */
public final class GLServer extends Server {

    private final GLCaps mCaps;

    private final GLCommandBuffer mMainCmdBuffer;

    private final GLResourceProvider mResourceProvider;
    private final GLPipelineStateCache mPipelineStateCache;

    private final CpuBufferCache mCpuBufferCache;

    private final BufferAllocPool mVertexPool;
    private final BufferAllocPool mInstancePool;
    private final BufferAllocPool mIndexPool;

    // unique ptr
    private GLOpsRenderPass mCachedOpsRenderPass;

    private final ArrayDeque<FlushInfo.FinishedCallback> mFinishedCallbacks = new ArrayDeque<>();
    private final LongArrayFIFOQueue mFinishedFences = new LongArrayFIFOQueue();

    /**
     * Represents a certain resource ID is bound, but no {@link Resource} object is associated with.
     */
    // OpenGL 3 only.
    static final Resource.UniqueID INVALID_UNIQUE_ID = new Resource.UniqueID();

    //@formatter:off
    static final int BUFFER_TYPE_VERTEX         = 0;
    static final int BUFFER_TYPE_INDEX          = 1;
    static final int BUFFER_TYPE_XFER_SRC       = 2;
    static final int BUFFER_TYPE_XFER_DST       = 3;
    static final int BUFFER_TYPE_UNIFORM        = 4;
    static final int BUFFER_TYPE_DRAW_INDIRECT  = 5;

    static int bufferUsageToType(int usage) {
        // __builtin_ctz
        return Integer.numberOfTrailingZeros(usage);
    }

    static {
        assert BUFFER_TYPE_VERTEX           ==
                bufferUsageToType(BufferUsageFlags.kVertex);
        assert BUFFER_TYPE_INDEX            ==
                bufferUsageToType(BufferUsageFlags.kIndex);
        assert BUFFER_TYPE_XFER_SRC         ==
                bufferUsageToType(BufferUsageFlags.kTransferSrc);
        assert BUFFER_TYPE_XFER_DST         ==
                bufferUsageToType(BufferUsageFlags.kTransferDst);
        assert BUFFER_TYPE_UNIFORM          ==
                bufferUsageToType(BufferUsageFlags.kUniform);
        assert BUFFER_TYPE_DRAW_INDIRECT    ==
                bufferUsageToType(BufferUsageFlags.kDrawIndirect);
    }
    //@formatter:on

    static final class HWBufferState {
        final int mTarget;
        GLBuffer.UniqueID mBoundBufferUniqueID;

        HWBufferState(int target) {
            mTarget = target;
        }
    }

    // context's buffer binding state
    private final HWBufferState[] mHWBufferStates = new HWBufferState[6];

    //@formatter:off
    {
        mHWBufferStates[BUFFER_TYPE_VERTEX]         =
                new HWBufferState(GL_ARRAY_BUFFER);
        mHWBufferStates[BUFFER_TYPE_INDEX]          =
                new HWBufferState(GL_ELEMENT_ARRAY_BUFFER);
        mHWBufferStates[BUFFER_TYPE_XFER_SRC]       =
                new HWBufferState(GL_PIXEL_UNPACK_BUFFER);
        mHWBufferStates[BUFFER_TYPE_XFER_DST]       =
                new HWBufferState(GL_PIXEL_PACK_BUFFER);
        mHWBufferStates[BUFFER_TYPE_UNIFORM]        =
                new HWBufferState(GL_UNIFORM_BUFFER);
        mHWBufferStates[BUFFER_TYPE_DRAW_INDIRECT]  =
                new HWBufferState(GL_DRAW_INDIRECT_BUFFER);
    }
    //@formatter:on

    /**
     * We have four methods to allocate UBOs.
     * <ol>
     * <li>sub allocate persistently mapped ring buffer (OpenGL 4.4+).</li>
     * <li>one uniform buffer per block, multiple BufferSubData in one frame.</li>
     * <li>managed a pool of uniform buffers, use one per draw call, triple buffering.</li>
     * <li>use uniform arrays like push constants (e.g. vec4[10] uboData;).</li>
     * </ol>
     * <p>
     * This is used in case 2 and 3.
     * <p>
     * For case 2, any block <= 128 bytes using binding 0, others using binding 1.
     * Each program has only one block (update per draw call).
     */
    private final GLBuffer.UniqueID[] mBoundUniformBuffers = new GLBuffer.UniqueID[4];

    // Below OpenGL 4.5.
    private int mHWActiveTextureUnit;

    // target is Texture2D
    private final GLTexture.UniqueID[] mHWTextureStates;

    static final class HWSamplerState {
        // default to invalid, we use 0 because it's not a valid sampler state
        int mSamplerState = 0;
        @SharedPtr
        GLSampler mBoundSampler = null;
    }

    private final HWSamplerState[] mHWSamplerStates;

    private boolean mNeedsFlush;

    private GLServer(DirectContext context, GLCaps caps) {
        super(context, caps);
        mCaps = caps;
        mMainCmdBuffer = new GLCommandBuffer(this);
        mResourceProvider = new GLResourceProvider(this, context);
        mPipelineStateCache = new GLPipelineStateCache(this, 256);
        mCpuBufferCache = new CpuBufferCache(6);
        mVertexPool = BufferAllocPool.makeVertexPool(context);
        mInstancePool = BufferAllocPool.makeInstancePool(context);
        mIndexPool = BufferAllocPool.makeIndexPool(context);

        int maxTextureUnits = caps.shaderCaps().mMaxFragmentSamplers;
        mHWTextureStates = new GLTexture.UniqueID[maxTextureUnits];
        mHWSamplerStates = new HWSamplerState[maxTextureUnits];
        for (int i = 0; i < maxTextureUnits; i++) {
            mHWSamplerStates[i] = new HWSamplerState();
        }
    }

    /**
     * Create a {@link GLServer} with OpenGL context current in the current thread.
     *
     * @param context the owner context
     * @param options the context options
     * @return the engine or null if failed to create
     */
    @Nullable
    public static GLServer make(DirectContext context, ContextOptions options) {
        GLCapabilities capabilities;
        try {
            capabilities = Objects.requireNonNullElseGet(
                    GL.getCapabilities(),
                    GL::createCapabilities
            );
        } catch (Exception x) {
            try {
                capabilities = GL.createCapabilities();
            } catch (Exception e) {
                e.printStackTrace(context.getErrorWriter());
                return null;
            }
        }
        try {
            GLCaps caps = new GLCaps(options, capabilities);
            return new GLServer(context, caps);
        } catch (Exception e) {
            e.printStackTrace(context.getErrorWriter());
            return null;
        }
    }

    @Override
    public GLCaps getCaps() {
        return mCaps;
    }

    @Override
    public void disconnect(boolean cleanup) {
        super.disconnect(cleanup);
        mVertexPool.reset();
        mInstancePool.reset();
        mIndexPool.reset();
        mCpuBufferCache.releaseAll();

        mMainCmdBuffer.resetStates(~0);

        if (cleanup) {
            mPipelineStateCache.release();
            mResourceProvider.release();
        } else {
            mPipelineStateCache.discard();
            mResourceProvider.discard();
        }

        //callAllFinishedCallbacks(cleanup);
    }

    @Override
    protected void handleDirtyContext() {
        super.handleDirtyContext();
    }

    public GLCommandBuffer currentCommandBuffer() {
        return mMainCmdBuffer;
    }

    @Override
    public GLResourceProvider getResourceProvider() {
        return mResourceProvider;
    }

    @Override
    public GLPipelineStateCache getPipelineStateCache() {
        return mPipelineStateCache;
    }

    /**
     * As staging buffers.
     */
    public CpuBufferCache getCpuBufferCache() {
        return mCpuBufferCache;
    }

    @Override
    public BufferAllocPool getVertexPool() {
        return mVertexPool;
    }

    @Override
    public BufferAllocPool getInstancePool() {
        return mInstancePool;
    }

    @Override
    public BufferAllocPool getIndexPool() {
        return mIndexPool;
    }

    @Override
    protected void onResetContext(int resetBits) {
        currentCommandBuffer().resetStates(resetBits);

        // we assume these values
        if ((resetBits & GLBackendState.kPixelStore) != 0) {
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
            glPixelStorei(GL_PACK_ROW_LENGTH, 0);
        }

        if ((resetBits & GLBackendState.kPipeline) != 0) {
            mHWBufferStates[BUFFER_TYPE_VERTEX].mBoundBufferUniqueID = INVALID_UNIQUE_ID;
            mHWBufferStates[BUFFER_TYPE_INDEX].mBoundBufferUniqueID = INVALID_UNIQUE_ID;
        }

        if ((resetBits & GLBackendState.kTexture) != 0) {
            Arrays.fill(mHWTextureStates, INVALID_UNIQUE_ID);
            //TODO
            for (var ss : mHWSamplerStates) {
                ss.mSamplerState = 0;
                ss.mBoundSampler = RefCnt.move(ss.mBoundSampler);
            }
        }

        mHWActiveTextureUnit = -1; // invalid

        if ((resetBits & GLBackendState.kRaster) != 0) {
            glDisable(GL_LINE_SMOOTH);
            glDisable(GL_POLYGON_SMOOTH);

            glDisable(GL_DITHER);
            glEnable(GL_MULTISAMPLE);
        }

        if ((resetBits & GLBackendState.kBlend) != 0) {
            glDisable(GL_COLOR_LOGIC_OP);
        }

        if ((resetBits & GLBackendState.kMisc) != 0) {
            // we don't use the z-buffer at all
            glDisable(GL_DEPTH_TEST);
            glDepthMask(false);
            glDisable(GL_POLYGON_OFFSET_FILL);

            // We don't use face culling.
            glDisable(GL_CULL_FACE);
            // We do use separate stencil. Our algorithms don't care which face is front vs. back so
            // just set this to the default for self-consistency.
            glFrontFace(GL_CCW);

            // we only ever use lines in hairline mode
            glLineWidth(1);
            glPointSize(1);
            glDisable(GL_PROGRAM_POINT_SIZE);
        }
    }

    //FIXME this is temp, wait for beginRenderPass()
    public void forceResetContext(int state) {
        markContextDirty(state);
        handleDirtyContext();
    }

    @Nullable
    @Override
    protected Texture onCreateTexture(int width, int height,
                                      BackendFormat format,
                                      int mipLevelCount,
                                      int sampleCount,
                                      int surfaceFlags) {
        assert (mipLevelCount > 0 && sampleCount > 0);
        // We don't support protected textures in OpenGL.
        if ((surfaceFlags & Surface.FLAG_PROTECTED) != 0) {
            return null;
        }
        if (format.isExternal()) {
            return null;
        }
        int glFormat = format.getGLFormat();
        int texture = createTexture(width, height, glFormat, mipLevelCount);
        if (texture == 0) {
            return null;
        }
        Function<GLTexture, GLRenderTarget> target = null;
        if ((surfaceFlags & Surface.FLAG_RENDERABLE) != 0) {
            target = createRTObjects(
                    texture,
                    width, height,
                    glFormat,
                    sampleCount);
            if (target == null) {
                glDeleteTextures(texture);
                return null;
            }
        }
        final GLTextureInfo info = new GLTextureInfo();
        info.texture = texture;
        info.format = format.getGLFormat();
        info.levels = mipLevelCount;
        if (target == null) {
            return new GLTexture(this,
                    width, height,
                    info,
                    format,
                    (surfaceFlags & Surface.FLAG_BUDGETED) != 0,
                    true);
        } else {
            return new GLRenderTexture(this,
                    width, height,
                    info,
                    format,
                    (surfaceFlags & Surface.FLAG_BUDGETED) != 0,
                    target);
        }
    }

    @Nullable
    @Override
    protected Texture onWrapRenderableBackendTexture(BackendTexture texture, int sampleCount, boolean ownership) {
        return null;
    }

    @Nullable
    @Override
    public RenderSurface onWrapBackendRenderTarget(BackendRenderTarget backendRenderTarget) {
        GLFramebufferInfo info = new GLFramebufferInfo();
        if (!backendRenderTarget.getGLFramebufferInfo(info)) {
            return null;
        }
        if (backendRenderTarget.isProtected()) {
            return null;
        }
        if (!mCaps.isFormatRenderable(info.mFormat, backendRenderTarget.getSampleCount())) {
            return null;
        }
        int actualSamplerCount = mCaps.getRenderTargetSampleCount(backendRenderTarget.getSampleCount(), info.mFormat);
        @SharedPtr
        var rt = GLRenderTarget.makeWrapped(this,
                backendRenderTarget.getWidth(),
                backendRenderTarget.getHeight(),
                info.mFormat,
                actualSamplerCount,
                info.mFramebuffer,
                backendRenderTarget.getStencilBits(),
                false);
        return new RenderSurface(rt);
    }

    @Override
    protected boolean onWritePixels(Texture __,
                                    int x, int y,
                                    int width, int height,
                                    int dstColorType,
                                    int srcColorType,
                                    int rowBytes, long pixels) {
        var tex = (GLTexture) __;
        assert (!tex.isExternal());
        assert (!tex.getBackendFormat().isCompressed());
        int glFormat = tex.getFormat();
        assert (mCaps.isFormatTexturable(glFormat));

        GLTextureParameters parameters = tex.getParameters();
        if (parameters.baseMipmapLevel != 0) {
            glTextureParameteri(tex.getHandle(), GL_TEXTURE_BASE_LEVEL, 0);
            parameters.baseMipmapLevel = 0;
        }
        int maxLevel = tex.getMaxMipmapLevel();
        if (parameters.maxMipmapLevel != maxLevel) {
            glTextureParameteri(tex.getHandle(), GL_TEXTURE_MAX_LEVEL, maxLevel);
            parameters.maxMipmapLevel = maxLevel;
        }

        int srcFormat = mCaps.getPixelsExternalFormat(glFormat, dstColorType, srcColorType, /*write*/true);
        if (srcFormat == 0) {
            return false;
        }
        int srcType = mCaps.getPixelsExternalType(glFormat, dstColorType, srcColorType);
        if (srcType == 0) {
            return false;
        }

        assert (x >= 0 && y >= 0 && width > 0 && height > 0);
        assert (pixels != 0);
        int bpp = ImageInfo.bytesPerPixel(srcColorType);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        boolean restoreRowLength = false;

        int trimRowBytes = width * bpp;
        if (rowBytes != trimRowBytes) {
            int rowLength = rowBytes / bpp;
            glPixelStorei(GL_UNPACK_ROW_LENGTH, rowLength);
            restoreRowLength = true;
        }

        bindTextureForSetup(tex.getHandle());
        glTexSubImage2D(GL_TEXTURE_2D, 0,
                x, y, width, height, srcFormat, srcType, pixels);

        if (restoreRowLength) {
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        }

        return true;
    }

    @Override
    protected boolean onGenerateMipmaps(Texture texture) {
        GLTexture tex = (GLTexture) texture;
        if (mCaps.hasDSASupport()) {
            glGenerateTextureMipmap(tex.getHandle());
        } else {
            bindTextureForSetup(tex.getHandle());
            glGenerateMipmap(GL_TEXTURE_2D);
        }
        return true;
    }

    @Override
    protected OpsRenderPass onGetOpsRenderPass(SurfaceProxyView writeView,
                                               Rect2i contentBounds,
                                               byte colorOps,
                                               byte stencilOps,
                                               float[] clearColor,
                                               Set<TextureProxy> sampledTextures,
                                               int pipelineFlags) {
        mStats.incRenderPasses();
        if (mCachedOpsRenderPass == null) {
            mCachedOpsRenderPass = new GLOpsRenderPass(this);
        }
        return mCachedOpsRenderPass.set(writeView.getProxy().peekRenderTarget(),
                contentBounds,
                writeView.getOrigin(),
                colorOps,
                stencilOps,
                clearColor);
    }

    public GLCommandBuffer beginRenderPass(GLRenderTarget fs,
                                           byte colorOps,
                                           byte stencilOps,
                                           float[] clearColor) {
        handleDirtyContext();

        GLCommandBuffer cmdBuffer = currentCommandBuffer();

        boolean colorLoadClear = LoadStoreOps.loadOp(colorOps) == LoadOp.Clear;
        boolean stencilLoadClear = LoadStoreOps.loadOp(stencilOps) == LoadOp.Clear;
        if (colorLoadClear || stencilLoadClear) {
            int framebuffer = fs.getSampleFramebuffer();
            cmdBuffer.flushScissorTest(false);
            if (colorLoadClear) {
                cmdBuffer.flushColorWrite(true);
                glClearNamedFramebufferfv(framebuffer,
                        GL_COLOR,
                        0,
                        clearColor);
            }
            if (stencilLoadClear) {
                glStencilMask(0xFFFFFFFF); // stencil will be flushed later
                glClearNamedFramebufferfi(framebuffer,
                        GL_DEPTH_STENCIL,
                        0,
                        1.0f, 0);
            }
        }
        cmdBuffer.flushRenderTarget(fs);

        return cmdBuffer;
    }

    public void endRenderPass(GLRenderTarget fs,
                              byte colorOps,
                              byte stencilOps) {
        handleDirtyContext();

        boolean colorStoreDiscard = LoadStoreOps.storeOp(colorOps) == StoreOp.DontCare;
        boolean stencilStoreDiscard = LoadStoreOps.storeOp(stencilOps) == StoreOp.DontCare;
        if (colorStoreDiscard || stencilStoreDiscard) {
            int framebuffer = fs.getSampleFramebuffer();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                final long pAttachments = stack.nmalloc(4, 8);
                int numAttachments = 0;
                if (colorStoreDiscard) {
                    int attachment = fs.getSampleFramebuffer() == DEFAULT_FRAMEBUFFER
                            ? GL_COLOR
                            : GL_COLOR_ATTACHMENT0;
                    memPutInt(pAttachments, attachment);
                    numAttachments++;
                }
                if (stencilStoreDiscard) {
                    int attachment = fs.getSampleFramebuffer() == DEFAULT_FRAMEBUFFER
                            ? GL_STENCIL
                            : GL_STENCIL_ATTACHMENT;
                    memPutInt(pAttachments + (numAttachments << 2), attachment);
                    numAttachments++;
                }
                nglInvalidateNamedFramebufferData(framebuffer, numAttachments, pAttachments);
            }
        }
    }

    @Nullable
    @Override
    protected Buffer onCreateBuffer(int size, int flags) {
        handleDirtyContext();
        return GLBuffer.make(this, size, flags);
    }

    @Override
    protected void onResolveRenderTarget(RenderTarget renderTarget,
                                         int resolveLeft, int resolveTop,
                                         int resolveRight, int resolveBottom) {
        GLRenderTarget glRenderTarget = (GLRenderTarget) renderTarget;

        int framebuffer = glRenderTarget.getSampleFramebuffer();
        int resolveFramebuffer = glRenderTarget.getResolveFramebuffer();

        // We should always have something to resolve
        assert (framebuffer != 0 && framebuffer != resolveFramebuffer);

        // BlitFramebuffer respects the scissor, so disable it.
        currentCommandBuffer().flushScissorTest(false);
        glBlitNamedFramebuffer(framebuffer, resolveFramebuffer, // MSAA to single
                resolveLeft, resolveTop, resolveRight, resolveBottom, // src rect
                resolveLeft, resolveTop, resolveRight, resolveBottom, // dst rect
                GL_COLOR_BUFFER_BIT, GL_NEAREST);
    }

    @Override
    public long insertFence() {
        return 0;
    }

    @Override
    public boolean checkFence(long fence) {
        return false;
    }

    @Override
    public void deleteFence(long fence) {

    }

    @Override
    public void addFinishedCallback(FlushInfo.FinishedCallback callback) {

    }

    @Override
    public void checkFinishedCallbacks() {

    }

    @Override
    public void waitForQueue() {

    }

    // Binds a buffer to the GL target corresponding to 'type', updates internal state tracking, and
    // returns the GL target the buffer was bound to.
    // When 'type' is 'index', this function will also implicitly bind the default VAO.
    // If the caller wishes to bind an index buffer to a specific VAO, it can call glBind directly.
    public int bindBuffer(@Nonnull GLBuffer buffer) {
        assert !getCaps().hasDSASupport();

        handleDirtyContext();

        int type = bufferUsageToType(buffer.getUsage());
        if (type == BUFFER_TYPE_INDEX) {
            // Index buffer state is tied to the vertex array.
            currentCommandBuffer().bindVertexArray(null);
        }

        var bufferState = mHWBufferStates[type];
        if (bufferState.mBoundBufferUniqueID != buffer.getUniqueID()) {
            glBindBuffer(bufferState.mTarget, buffer.getHandle());
            bufferState.mBoundBufferUniqueID = buffer.getUniqueID();
        }

        return bufferState.mTarget;
    }

    /**
     * Bind raw buffer ID to context (below OpenGL 4.5).
     *
     * @param usage  {@link BufferUsageFlags}
     * @param buffer the nonzero texture ID
     */
    public int bindBufferForSetup(int usage, int buffer) {
        assert !getCaps().hasDSASupport();

        handleDirtyContext();

        int type = bufferUsageToType(usage);
        if (type == BUFFER_TYPE_INDEX) {
            // Index buffer state is tied to the vertex array.
            currentCommandBuffer().bindVertexArray(null);
        }

        var bufferState = mHWBufferStates[type];
        glBindBuffer(bufferState.mTarget, buffer);
        bufferState.mBoundBufferUniqueID = INVALID_UNIQUE_ID;

        return bufferState.mTarget;
    }

    public void bindIndexBufferInPipe(@Nonnull GLBuffer buffer) {
        assert !getCaps().hasDSASupport();

        handleDirtyContext();

        assert bufferUsageToType(buffer.getUsage()) == BUFFER_TYPE_INDEX;

        // force rebind
        var bufferState = mHWBufferStates[BUFFER_TYPE_INDEX];
        glBindBuffer(bufferState.mTarget, buffer.getHandle());
        bufferState.mBoundBufferUniqueID = buffer.getUniqueID();
    }

    public void bindTexture(int binding, GLTexture texture,
                            int samplerState, short readSwizzle) {
        boolean dsa = mCaps.hasDSASupport();
        if (mHWTextureStates[binding] != texture.getUniqueID()) {
            if (dsa) {
                glBindTextureUnit(binding, texture.getHandle());
            } else {
                setTextureUnit(binding);
                glBindTexture(GL_TEXTURE_2D, texture.getHandle());
            }
            mHWTextureStates[binding] = texture.getUniqueID();
        }
        var ss = mHWSamplerStates[binding];
        if (ss.mSamplerState != samplerState) {
            GLSampler sampler = samplerState != 0
                    ? mResourceProvider.findOrCreateCompatibleSampler(samplerState)
                    : null;
            glBindSampler(binding, sampler != null
                    ? sampler.getHandle()
                    : 0);
            ss.mBoundSampler = RefCnt.move(ss.mBoundSampler, sampler);
        }
        GLTextureParameters parameters = texture.getParameters();
        if (parameters.baseMipmapLevel != 0) {
            if (dsa) {
                glTextureParameteri(texture.getHandle(), GL_TEXTURE_BASE_LEVEL, 0);
            } else {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            }
            parameters.baseMipmapLevel = 0;
        }
        int maxLevel = texture.getMaxMipmapLevel();
        if (parameters.maxMipmapLevel != maxLevel) {
            if (dsa) {
                glTextureParameteri(texture.getHandle(), GL_TEXTURE_MAX_LEVEL, maxLevel);
            } else {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, maxLevel);
            }
            parameters.maxMipmapLevel = maxLevel;
        }
        // texture view is available since 4.3, but less used in OpenGL
        boolean swizzleChanged = false;
        for (int i = 0; i < 4; ++i) {
            int swiz = switch (readSwizzle & 0xF) {
                case 0 -> GL_RED;
                case 1 -> GL_GREEN;
                case 2 -> GL_BLUE;
                case 3 -> GL_ALPHA;
                case 4 -> GL_ZERO;
                case 5 -> GL_ONE;
                default -> throw new AssertionError(readSwizzle);
            };
            if (parameters.swizzle[i] != swiz) {
                parameters.swizzle[i] = swiz;
                swizzleChanged = true;
            }
            readSwizzle >>= 4;
        }
        if (swizzleChanged) {
            if (dsa) {
                glTextureParameteriv(texture.getHandle(), GL_TEXTURE_SWIZZLE_RGBA, parameters.swizzle);
            } else {
                glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_RGBA, parameters.swizzle);
            }
        }
    }

    /**
     * Binds texture unit in context. OpenGL 3 only.
     *
     * @param unit 0-based texture unit index
     */
    public void setTextureUnit(int unit) {
        assert (unit >= 0 && unit < mHWTextureStates.length);
        if (unit != mHWActiveTextureUnit) {
            glActiveTexture(GL_TEXTURE0 + unit);
            mHWActiveTextureUnit = unit;
        }
    }

    /**
     * Bind raw texture ID to a less used texture unit (below OpenGL 4.5).
     *
     * @param texture the nonzero texture ID
     */
    public void bindTextureForSetup(int texture) {
        int lastUnit = mHWTextureStates.length - 1;
        setTextureUnit(lastUnit);
        mHWTextureStates[lastUnit] = INVALID_UNIQUE_ID;
        glBindTexture(GL_TEXTURE_2D, texture);
    }

    private int createTexture(int width, int height, int format, int levels) {
        assert (glFormatIsSupported(format));
        assert (!glFormatIsCompressed(format));

        int internalFormat = mCaps.getTextureInternalFormat(format);
        if (internalFormat == 0) {
            return 0;
        }

        assert (mCaps.isFormatTexturable(format));
        int texture;
        if (mCaps.hasDSASupport()) {
            assert (mCaps.isTextureStorageCompatible(format));
            texture = glCreateTextures(GL_TEXTURE_2D);
            if (texture == 0) {
                return 0;
            }
            if (mCaps.skipErrorChecks()) {
                glTextureStorage2D(texture, levels, internalFormat, width, height);
            } else {
                glClearErrors();
                glTextureStorage2D(texture, levels, internalFormat, width, height);
                if (glGetError() != GL_NO_ERROR) {
                    glDeleteTextures(texture);
                    return 0;
                }
            }
        } else {
            texture = glGenTextures();
            if (texture == 0) {
                return 0;
            }
            bindTextureForSetup(texture);

            if (mCaps.isTextureStorageCompatible(format)) {
                if (mCaps.skipErrorChecks()) {
                    glTexStorage2D(GL_TEXTURE_2D, levels, internalFormat, width, height);
                } else {
                    glClearErrors();
                    glTexStorage2D(GL_TEXTURE_2D, levels, internalFormat, width, height);
                    if (glGetError() != GL_NO_ERROR) {
                        glDeleteTextures(texture);
                        return 0;
                    }
                }
            } else {
                final int externalFormat = mCaps.getFormatDefaultExternalFormat(format);
                final int externalType = mCaps.getFormatDefaultExternalType(format);
                final boolean checks = !mCaps.skipErrorChecks();
                int error = 0;
                if (checks) {
                    glClearErrors();
                }
                for (int level = 0; level < levels; level++) {
                    int currentWidth = Math.max(1, width >> level);
                    int currentHeight = Math.max(1, height >> level);
                    nglTexImage2D(GL_TEXTURE_2D, level, internalFormat,
                            currentWidth, currentHeight,
                            0, externalFormat, externalType, MemoryUtil.NULL);
                    if (checks) {
                        error |= glGetError();
                    }
                }
                if (error != 0) {
                    glDeleteTextures(texture);
                    return 0;
                }
            }
        }

        return texture;
    }

    @Nullable
    private Function<GLTexture, GLRenderTarget> createRTObjects(int texture,
                                                                int width, int height,
                                                                int format,
                                                                int samples) {
        assert texture != 0;
        assert glFormatIsSupported(format);
        assert !glFormatIsCompressed(format);

        // There's an NVIDIA driver bug that creating framebuffer via DSA with attachments of
        // different dimensions will report GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT.
        // The workaround is to use traditional glGen* and glBind* (validate).
        // see https://forums.developer.nvidia.com/t/framebuffer-incomplete-when-attaching-color-buffers-of-different-sizes-with-dsa/211550
        final int framebuffer = glGenFramebuffers();
        if (framebuffer == 0) {
            return null;
        }

        // If we are using multisampling we will create two FBOs. We render to one and then resolve to
        // the texture bound to the other.
        final int msaaFramebuffer;
        final GLAttachment msaaColorBuffer;
        if (samples <= 1) {
            msaaFramebuffer = 0;
            msaaColorBuffer = null;
        } else {
            msaaFramebuffer = glGenFramebuffers();
            if (msaaFramebuffer == 0) {
                glDeleteFramebuffers(framebuffer);
                return null;
            }

            msaaColorBuffer = GLAttachment.makeColor(this,
                    width, height,
                    samples,
                    format);
            if (msaaColorBuffer == null) {
                glDeleteFramebuffers(framebuffer);
                glDeleteFramebuffers(msaaFramebuffer);
                return null;
            }

            currentCommandBuffer().bindFramebuffer(msaaFramebuffer);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER,
                    GL_COLOR_ATTACHMENT0,
                    GL_RENDERBUFFER,
                    msaaColorBuffer.getRenderbufferID());
            if (!mCaps.skipErrorChecks()) {
                int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
                if (status != GL_FRAMEBUFFER_COMPLETE) {
                    glDeleteFramebuffers(framebuffer);
                    glDeleteFramebuffers(msaaFramebuffer);
                    msaaColorBuffer.unref();
                    return null;
                }
            }
        }

        currentCommandBuffer().bindFramebuffer(framebuffer);
        glFramebufferTexture(GL_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0,
                texture,
                0);
        if (!mCaps.skipErrorChecks()) {
            int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
            if (status != GL_FRAMEBUFFER_COMPLETE) {
                glDeleteFramebuffers(framebuffer);
                glDeleteFramebuffers(msaaFramebuffer);
                if (msaaColorBuffer != null) {
                    msaaColorBuffer.unref();
                }
                return null;
            }
        }

        return colorBuffer -> new GLRenderTarget(this,
                colorBuffer.getWidth(),
                colorBuffer.getHeight(),
                colorBuffer.getFormat(),
                samples,
                framebuffer,
                msaaFramebuffer,
                colorBuffer,
                msaaColorBuffer);
    }
}
