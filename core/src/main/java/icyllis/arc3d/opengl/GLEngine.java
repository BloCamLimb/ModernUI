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

import icyllis.arc3d.engine.*;
import icyllis.modernui.graphics.ImageInfo;
import icyllis.modernui.graphics.Rect;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

import static icyllis.arc3d.opengl.GLCore.*;

/**
 * The OpenGL graphics engine.
 */
public final class GLEngine extends Engine {

    private final GLCaps mCaps;

    private final GLCommandBuffer mMainCmdBuffer;

    private final GLPipelineStateCache mProgramCache;
    private final GLResourceProvider mResourceProvider;

    private final CpuBufferCache mCpuBufferCache;

    private final BufferAllocPool mVertexPool;
    private final BufferAllocPool mInstancePool;

    // unique ptr
    //private GLOpsRenderPass mCachedOpsRenderPass;

    private final ArrayDeque<FlushInfo.FinishedCallback> mFinishedCallbacks = new ArrayDeque<>();
    private final LongArrayFIFOQueue mFinishedFences = new LongArrayFIFOQueue();

    static final int BUFFER_TYPE_VERTEX = 0;
    static final int BUFFER_TYPE_INDEX = 1;
    static final int BUFFER_TYPE_XFER_SRC = 2;
    static final int BUFFER_TYPE_XFER_DST = 3;

    static final class HWBufferState {
        int mTarget;
        GLBuffer.UniqueID mBoundBufferUniqueID;
    }

    // context's buffer binding state
    private final HWBufferState[] mHWBufferStates = new HWBufferState[4];

    {
        for (int i = 0; i < mHWBufferStates.length; i++) {
            mHWBufferStates[i] = new HWBufferState();
        }
        mHWBufferStates[BUFFER_TYPE_VERTEX].mTarget = GL_ARRAY_BUFFER;
        mHWBufferStates[BUFFER_TYPE_INDEX].mTarget = GL_ELEMENT_ARRAY_BUFFER;
        mHWBufferStates[BUFFER_TYPE_XFER_SRC].mTarget = GL_PIXEL_UNPACK_BUFFER;
        mHWBufferStates[BUFFER_TYPE_XFER_DST].mTarget = GL_PIXEL_PACK_BUFFER;
    }

    private boolean mNeedsFlush;

    private GLEngine(DirectContext context, GLCaps caps) {
        super(context, caps);
        mCaps = caps;
        mMainCmdBuffer = new GLCommandBuffer(this);
        mProgramCache = new GLPipelineStateCache(this, 256);
        mResourceProvider = new GLResourceProvider(this);
        mCpuBufferCache = new CpuBufferCache(6);
        mVertexPool = BufferAllocPool.makeVertexPool(this);
        mInstancePool = BufferAllocPool.makeInstancePool(this);
    }

    /**
     * Create a {@link GLEngine} with OpenGL context current in the current thread.
     *
     * @param context the owner context
     * @param options the context options
     * @return the engine or null if failed to create
     */
    @Nullable
    public static GLEngine make(DirectContext context, ContextOptions options) {
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
                e.printStackTrace();
                return null;
            }
        }
        try {
            GLCaps caps = new GLCaps(options, capabilities);
            return new GLEngine(context, caps);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public GLCaps getCaps() {
        return mCaps;
    }

    @Override
    public GLPipelineStateCache getPipelineBuilder() {
        return mProgramCache;
    }

    @Override
    public void disconnect(boolean cleanup) {
        super.disconnect(cleanup);
        mVertexPool.reset();
        mInstancePool.reset();
        mCpuBufferCache.releaseAll();

        mMainCmdBuffer.resetStates(~0);

        if (cleanup) {
            mProgramCache.release();
            mResourceProvider.release();
        } else {
            mProgramCache.discard();
            mResourceProvider.discard();
        }

        //callAllFinishedCallbacks(cleanup);
    }

    public GLCommandBuffer currentCommandBuffer() {
        return mMainCmdBuffer;
    }

    public GLResourceProvider getResourceProvider() {
        return mResourceProvider;
    }

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
    protected void onResetContext(int resetBits) {
        currentCommandBuffer().resetStates(resetBits);

        // we assume these values
        if ((resetBits & GLBackendState.kPixelStore) != 0) {
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
            glPixelStorei(GL_PACK_ROW_LENGTH, 0);
        }

        if ((resetBits & GLBackendState.kPipeline) != 0) {
            mHWBufferStates[BUFFER_TYPE_VERTEX].mBoundBufferUniqueID = null;
            mHWBufferStates[BUFFER_TYPE_INDEX].mBoundBufferUniqueID = null;
        }

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
        Function<GLTexture, GLSurfaceManager> target = null;
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

    @Override
    protected boolean onWritePixels(Texture texture,
                                    int x, int y,
                                    int width, int height,
                                    int dstColorType,
                                    int srcColorType,
                                    int rowBytes, long pixels) {
        assert (!texture.isExternal());
        assert (!texture.getBackendFormat().isCompressed());
        GLTexture tex = (GLTexture) texture;
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

        currentCommandBuffer().bindTextureForSetup(tex.getHandle());
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
            currentCommandBuffer().bindTextureForSetup(tex.getHandle());
            glGenerateMipmap(GL_TEXTURE_2D);
        }
        return true;
    }

    @Override
    protected OpsRenderPass onGetOpsRenderPass(SurfaceProxyView writeView, Rect contentBounds, byte colorOps,
                                               byte stencilOps, float[] clearColor, Set<TextureProxy> sampledTextures
            , int pipelineFlags) {
        return null;
    }

    @Override
    protected void onResolveRenderTarget(SurfaceManager surfaceManager, int resolveLeft, int resolveTop,
                                         int resolveRight
            , int resolveBottom) {

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

        int type = buffer.getTypeEnum();
        if (type == BUFFER_TYPE_INDEX) {
            // Index buffer state is tied to the vertex array.
            currentCommandBuffer().bindVertexArray(DEFAULT_VERTEX_ARRAY);
        }

        var bufferState = mHWBufferStates[type];
        if (bufferState.mBoundBufferUniqueID != buffer.getUniqueID()) {
            glBindBuffer(bufferState.mTarget, buffer.getBufferID());
            bufferState.mBoundBufferUniqueID = buffer.getUniqueID();
        }

        return bufferState.mTarget;
    }

    public int maxTextureUnits() {
        return mCaps.shaderCaps().mMaxFragmentSamplers;
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
            currentCommandBuffer().bindTextureForSetup(texture);

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
    private Function<GLTexture, GLSurfaceManager> createRTObjects(int texture,
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

        return colorBuffer -> new GLSurfaceManager(this,
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
