/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.Device;
import icyllis.arc3d.engine.Image;
import icyllis.arc3d.engine.*;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import static icyllis.arc3d.opengl.GLCore.*;

/**
 * The OpenGL device.
 */
public final class GLDevice extends Device {

    private final GLCaps mCaps;
    private final GLInterface mGLInterface;

    /*private final GpuBufferPool mVertexPool;
    private final GpuBufferPool mInstancePool;
    private final GpuBufferPool mIndexPool;*/

    // unique ptr
    private GLOpsRenderPass mCachedOpsRenderPass;

    private final ArrayDeque<FlushInfo.FinishedCallback> mFinishedCallbacks = new ArrayDeque<>();
    private final LongArrayFIFOQueue mFinishedFences = new LongArrayFIFOQueue();

    /*
     * Represents a certain resource ID is bound, but no {@link Resource} object is associated with.
     */
    // OpenGL 3 only.
    //static final UniqueID INVALID_UNIQUE_ID = new UniqueID();

    //@formatter:off
    /*static final int BUFFER_TYPE_VERTEX         = 0;
    static final int BUFFER_TYPE_INDEX          = 1;
    static final int BUFFER_TYPE_XFER_SRC       = 2;
    static final int BUFFER_TYPE_XFER_DST       = 3;
    static final int BUFFER_TYPE_UNIFORM        = 4;
    static final int BUFFER_TYPE_DRAW_INDIRECT  = 5;

    static int bufferUsageToType(int usage) {
        // __builtin_ctz
        return Integer.numberOfTrailingZeros(usage);
    }*/

    /*static {
        assert BUFFER_TYPE_VERTEX           ==
                bufferUsageToType(BufferUsageFlags.kVertex);
        assert BUFFER_TYPE_INDEX            ==
                bufferUsageToType(BufferUsageFlags.kIndex);
        assert BUFFER_TYPE_XFER_SRC         ==
                bufferUsageToType(BufferUsageFlags.kUpload);
        assert BUFFER_TYPE_XFER_DST         ==
                bufferUsageToType(BufferUsageFlags.kReadback);
        assert BUFFER_TYPE_UNIFORM          ==
                bufferUsageToType(BufferUsageFlags.kUniform);
        assert BUFFER_TYPE_DRAW_INDIRECT    ==
                bufferUsageToType(BufferUsageFlags.kDrawIndirect);
    }*/
    //@formatter:on

    /*static final class HWBufferState {
        final int mTarget;
        UniqueID mBoundBufferUniqueID;

        HWBufferState(int target) {
            mTarget = target;
        }
    }*/

    // context's buffer binding state
    //private final HWBufferState[] mHWBufferStates = new HWBufferState[6];

    //@formatter:off
    /*{
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
    }*/
    //@formatter:on

    /*
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
    //private final UniqueID[] mBoundUniformBuffers = new UniqueID[4];

    // Below OpenGL 4.5.
    //private int mHWActiveTextureUnit;

    // target is Texture2D
    //private final UniqueID[] mHWTextureStates;

    /*static final class HWSamplerState {
        // default to invalid, we use 0 because it's not a valid sampler state
        int mSamplerState = 0;
        @SharedPtr
        GLSampler mBoundSampler = null;
    }*/

    //private final HWSamplerState[] mHWSamplerStates;

    /**
     * Framebuffer used for pixel transfer operations, compatibility only.
     * Lazily init.
     */
    private int mCopySrcFramebuffer = 0;
    private int mCopyDstFramebuffer = 0;

    private boolean mNeedsFlush;

    private final ConcurrentLinkedQueue<Consumer<GLDevice>> mRenderCalls =
            new ConcurrentLinkedQueue<>();

    // executing thread only
    private final FramebufferCache mFramebufferCache =
            new FramebufferCache();

    private GLDevice(ContextOptions options, GLCaps caps, GLInterface glInterface) {
        super(BackendApi.kOpenGL, options, caps);
        mCaps = caps;
        mGLInterface = glInterface;

        /*int maxTextureUnits = caps.shaderCaps().mMaxFragmentSamplers;
        mHWTextureStates = new UniqueID[maxTextureUnits];
        mHWSamplerStates = new HWSamplerState[maxTextureUnits];
        for (int i = 0; i < maxTextureUnits; i++) {
            mHWSamplerStates[i] = new HWSamplerState();
        }*/
    }

    /**
     * Create a {@link GLDevice} with OpenGL context current in the current thread.
     *
     * @param options the context options
     * @return the engine or null if failed to create
     */
    @Nullable
    public static GLDevice make(ContextOptions options, Object capabilities) {
        try {
            final GLCaps caps;
            final GLInterface glInterface;
            switch (capabilities.getClass().getName()) {
                case "org.lwjgl.opengl.GLCapabilities" -> {
                    var impl = new GLCaps_GL(options, capabilities);
                    caps = impl;
                    glInterface = impl;
                }
                case "org.lwjgl.opengles.GLESCapabilities" -> {
                    var impl = new GLCaps_GLES(options, capabilities);
                    caps = impl;
                    glInterface = impl;
                }
                default -> {
                    options.mLogger.error("Failed to create GLDevice: invalid capabilities");
                    return null;
                }
            }
            return new GLDevice(options, caps, glInterface);
        } catch (Exception e) {
            options.mLogger.error("Failed to create GLDevice", e);
            return null;
        }
    }

    /**
     * OpenGL only method. Execute the GL command as soon as possible.
     */
    public void executeRenderCall(Consumer<GLDevice> renderCall) {
        if (isOnExecutingThread()) {
            renderCall.accept(this);
        } else {
            recordRenderCall(renderCall);
        }
    }

    public void recordRenderCall(Consumer<GLDevice> renderCall) {
        mRenderCalls.add(renderCall);
    }

    public void flushRenderCalls() {
        //noinspection UnnecessaryLocalVariable
        final var queue = mRenderCalls;
        Consumer<GLDevice> r;
        while ((r = queue.poll()) != null) r.accept(this);
    }

    public FramebufferCache getFramebufferCache() {
        return mFramebufferCache;
    }

    @Override
    public GLCaps getCaps() {
        return mCaps;
    }

    public GLInterface getGL() {
        return mGLInterface;
    }

    @Override
    public void disconnect(boolean cleanup) {
        super.disconnect(cleanup);
        /*mVertexPool.reset();
        mInstancePool.reset();
        mIndexPool.reset();*/


        if (cleanup) {
            //mPipelineCache.release();
            //mResourceProvider.release();
        } else {
            //mPipelineCache.discard();
            //mResourceProvider.discard();
        }

        callAllFinishedCallbacks(cleanup);

        flushRenderCalls();

        mFramebufferCache.close();
    }

    @Override
    public GLResourceProvider makeResourceProvider(Context context) {
        return new GLResourceProvider(this, context);
    }

    @Override
    protected void onResetContext(int resetBits) {
        //currentCommandBuffer().resetStates(resetBits);

        // we assume these values
        if ((resetBits & GLBackendState.kPixelStore) != 0) {
            glPixelStorei(GL_PACK_ROW_LENGTH, 0);
        }

        /*if ((resetBits & GLBackendState.kPipeline) != 0) {
            mHWBufferStates[BUFFER_TYPE_VERTEX].mBoundBufferUniqueID = INVALID_UNIQUE_ID;
            mHWBufferStates[BUFFER_TYPE_INDEX].mBoundBufferUniqueID = INVALID_UNIQUE_ID;
        }*/

        /*if ((resetBits & GLBackendState.kTexture) != 0) {
            Arrays.fill(mHWTextureStates, INVALID_UNIQUE_ID);
            for (var ss : mHWSamplerStates) {
                ss.mSamplerState = 0;
                ss.mBoundSampler = RefCnt.move(ss.mBoundSampler);
            }
        }*/

        //mHWActiveTextureUnit = -1; // invalid

        /*if ((resetBits & GLBackendState.kRaster) != 0) {
            getGL().glDisable(GL_LINE_SMOOTH);
            getGL().glDisable(GL_POLYGON_SMOOTH);

            getGL().glDisable(GL_DITHER);
            getGL().glEnable(GL_MULTISAMPLE);
        }

        if ((resetBits & GLBackendState.kBlend) != 0) {
            getGL().glDisable(GL_COLOR_LOGIC_OP);
        }

        if ((resetBits & GLBackendState.kMisc) != 0) {
            // we don't use the z-buffer at all
            getGL().glDisable(GL_DEPTH_TEST);
            glDepthMask(false);
            getGL().glDisable(GL_POLYGON_OFFSET_FILL);

            // We don't use face culling.
            getGL().glDisable(GL_CULL_FACE);
            // We do use separate stencil. Our algorithms don't care which face is front vs. back so
            // just set this to the default for self-consistency.
            glFrontFace(GL_CCW);

            // we only ever use lines in hairline mode
            glLineWidth(1);
            glPointSize(1);
            getGL().glDisable(GL_PROGRAM_POINT_SIZE);
        }*/
    }

    /**
     * Call {@link #getError()} until there are no errors.
     */
    public void clearErrors() {
        //noinspection StatementWithEmptyBody
        while (getError() != GL_NO_ERROR)
            ;
    }

    /**
     * Polls an error code and sets the OOM and context lost state.
     */
    public int getError() {
        int error = getGL().glGetError();
        if (error == GL_OUT_OF_MEMORY) {
            mOutOfMemoryEncountered = true;
        } else if (error == GL_CONTEXT_LOST) {
            mDeviceIsLost = true;
        }
        return error;
    }

    /*@Nullable
    @Override
    protected GLImage onCreateImage(int width, int height,
                                    BackendFormat format,
                                    int mipLevelCount,
                                    int sampleCount,
                                    int surfaceFlags) {
        assert (mipLevelCount > 0 && sampleCount > 0);
        // We don't support protected textures in OpenGL.
        if ((surfaceFlags & ISurface.FLAG_PROTECTED) != 0) {
            return null;
        }
        // There's no memoryless attachments in OpenGL.
        if ((surfaceFlags & ISurface.FLAG_MEMORYLESS) != 0) {
            return null;
        }
        if (format.isExternal()) {
            return null;
        }
        int glFormat = format.getGLFormat();
        final int handle;
        final int target;
        if (sampleCount > 1 && (surfaceFlags & ISurface.FLAG_SAMPLED_IMAGE) == 0) {
            int internalFormat = glFormat;
            if (GLUtil.glFormatStencilBits(glFormat) == 0) {
                internalFormat = getCaps().getRenderbufferInternalFormat(glFormat);
            }
            handle = createRenderbuffer(width, height, sampleCount, internalFormat);
            target = GL_RENDERBUFFER;
        } else {
            handleDirtyContext(GLBackendState.kTexture);
            handle = createTexture(width, height, glFormat, mipLevelCount);
            target = GL_TEXTURE_2D;
        }
        if (handle == 0) {
            return null;
        }
        *//*Function<GLTexture, GLRenderTarget> target = null;
        if ((surfaceFlags & ISurface.FLAG_RENDERABLE) != 0) {
            target = createRTObjects(
                    texture,
                    width, height,
                    glFormat,
                    sampleCount);
            if (target == null) {
                glDeleteTextures(texture);
                return null;
            }
        }*//*
        final GLImageInfo info = new GLImageInfo();
        info.mTarget = target;
        info.handle = handle;
        info.mFormat = glFormat;
        info.levels = mipLevelCount;
        info.samples = sampleCount;
        //if (target == null) {
        return new GLImage(this,
                width, height,
                info,
                format,
                surfaceFlags);
        *//*} else {
            return new GLRenderTexture(this,
                    width, height,
                    info,
                    format,
                    (surfaceFlags & ISurface.FLAG_BUDGETED) != 0,
                    target);
        }*//*
    }*/

    @Nullable
    @Override
    protected GpuRenderTarget onCreateRenderTarget(int width, int height,
                                                   int sampleCount,
                                                   int numColorTargets,
                                                   @Nullable Image[] colorTargets,
                                                   @Nullable Image[] resolveTargets,
                                                   @Nullable int[] mipLevels,
                                                   @Nullable Image depthStencilTarget,
                                                   int surfaceFlags) {
        return null;
    }

    @Nullable
    @Override
    protected GLRenderTarget onWrapRenderableBackendTexture(BackendImage texture,
                                                            int sampleCount,
                                                            boolean ownership) {
        if (texture.isProtected()) {
            // Not supported in GL backend at this time.
            return null;
        }
        /*if (!(texture instanceof GLBackendImage)) {
            return null;
        }
        final GLImageInfo info = new GLImageInfo();
        ((GLBackendImage) texture).getGLImageInfo(info);
        if (info.handle == 0 || info.mFormat == 0) {
            return null;
        }
        if (info.mTarget != GL_TEXTURE_2D) {
            return null;
        }
        int format = info.mFormat;
        if (!GLUtil.glFormatIsSupported(format)) {
            return null;
        }
        handleDirtyContext(GLBackendState.kTexture);
        assert mCaps.isFormatRenderable(format, sampleCount);
        assert mCaps.isFormatTexturable(format);

        sampleCount = mCaps.getRenderTargetSampleCount(sampleCount, format);
        assert sampleCount > 0;

        *//*var objects = createRTObjects(info.handle,
                texture.getWidth(), texture.getHeight(),
                texture.getBackendFormat(),
                sampleCount);*//*

        var colorTarget = createImage(
                texture.getWidth(), texture.getHeight(),
                texture.getBackendFormat(),
                sampleCount,
                ISurface.FLAG_BUDGETED | ISurface.FLAG_RENDERABLE,
                ""
        );*/

        //TODO create wrapped texture
        /*if (objects != null) {


            return new GLRenderTarget(this, texture.getWidth(), texture.getHeight(),
                format, sampleCount, framebuffer, msaaFramebuffer,
                texture, msaaColorBuffer, ownership);
        }*/

        return null;
    }

    @Override
    protected GpuRenderTarget onWrapGLDefaultFramebuffer(int width, int height,
                                                         int sampleCount,
                                                         int depthBits,
                                                         int stencilBits,
                                                         BackendFormat format) {
        int actualSamplerCount = mCaps.getRenderTargetSampleCount(sampleCount, format.getGLFormat());
        return GLRenderTarget.makeWrapped(null,
                width,
                height,
                format,
                actualSamplerCount,
                0,
                depthBits,
                stencilBits,
                false);
    }

    @Nullable
    @Override
    public GLRenderTarget onWrapBackendRenderTarget(BackendRenderTarget backendRenderTarget) {
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
        return GLRenderTarget.makeWrapped(null,
                backendRenderTarget.getWidth(),
                backendRenderTarget.getHeight(),
                backendRenderTarget.getBackendFormat(),
                actualSamplerCount,
                info.mFramebuffer,
                0,
                backendRenderTarget.getStencilBits(),
                false);
    }

    /**
     * Updates the pixels in a rectangle of an image. No sRGB/linear conversions are performed.
     * The write operation can fail because of the surface doesn't support writing (e.g. read only),
     * the color type is not allowed for the format of the texture or if the rectangle written
     * is not contained in the texture.
     *
     * @param texture      the image to write to
     * @param dstColorType the color type for this use of the surface
     * @param srcColorType the color type of the source data
     * @param rowBytes     the row bytes, must be a multiple of srcColorType's bytes-per-pixel.
     * @param pixels       the pointer to the texel data for base level image
     * @return true if succeeded, false if not
     */
    public boolean writePixels(@RawPtr Image texture,
                               int x, int y,
                               int width, int height,
                               int dstColorType,
                               int srcColorType,
                               int rowBytes, long pixels) {
        assert (texture != null);
        if (x < 0 || y < 0 || width <= 0 || height <= 0) {
            return false;
        }
        if (texture.isReadOnly() || texture.getSampleCount() > 1) {
            return false;
        }
        if ((texture.getSurfaceFlags() & ISurface.FLAG_SAMPLED_IMAGE) == 0) {
            return false;
        }
        assert (texture.getWidth() > 0 && texture.getHeight() > 0);
        if (x + width > texture.getWidth() || y + height > texture.getHeight()) {
            return false;
        }
        int bpp = ColorInfo.bytesPerPixel(srcColorType);
        int minRowBytes = width * bpp;
        if (rowBytes < minRowBytes) {
            return false;
        }
        if (rowBytes % bpp != 0) {
            return false;
        }
        if (pixels == 0) {
            return true;
        }
        if (!onWritePixels(texture,
                x, y, width, height,
                dstColorType,
                srcColorType,
                rowBytes, pixels)) {
            return false;
        }
        if (texture.isMipmapped()) {
            texture.setMipmapsDirty(true);
        }
        mStats.incTextureUploads();
        return true;
    }

    private boolean onWritePixels(@RawPtr Image image,
                                  int x, int y,
                                  int width, int height,
                                  int dstColorType,
                                  int srcColorType,
                                  int rowBytes, long pixels) {
        //assert (!image.getBackendFormat().isCompressed());
        if (!image.isSampledImage() && !image.isStorageImage()) {
            return false;
        }
        GLTexture glTexture = (GLTexture) image;
        int glFormat = glTexture.getFormat();
        assert (mCaps.isFormatTexturable(glFormat));

        int target = glTexture.getTarget();
        if (target == GL_RENDERBUFFER) {
            return false;
        }

        int srcFormat = mCaps.getPixelsExternalFormat(
                glFormat, dstColorType, srcColorType, /*write*/true
        );
        if (srcFormat == 0) {
            return false;
        }
        int srcType = mCaps.getPixelsExternalType(
                glFormat, dstColorType, srcColorType
        );
        if (srcType == 0) {
            return false;
        }
        //handleDirtyContext(GLBackendState.kTexture | GLBackendState.kPixelStore);

        boolean dsa = mCaps.hasDSASupport();
        int texName = glTexture.getHandle();
        int boundTexture = 0;
        //TODO not only 2D
        if (!dsa) {
            boundTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
            if (texName != boundTexture) {
                glBindTexture(target, texName);
            }
        }

        GLTextureMutableState mutableState = glTexture.getGLMutableState();
        if (mutableState.mBaseMipmapLevel != 0) {
            if (dsa) {
                glTextureParameteri(texName, GL_TEXTURE_BASE_LEVEL, 0);
            } else {
                glTexParameteri(target, GL_TEXTURE_BASE_LEVEL, 0);
            }
            mutableState.mBaseMipmapLevel = 0;
        }
        int maxLevel = glTexture.getMipLevelCount() - 1; // minus base level
        if (mutableState.mMaxMipmapLevel != maxLevel) {
            if (dsa) {
                glTextureParameteri(texName, GL_TEXTURE_MAX_LEVEL, maxLevel);
            } else {
                glTexParameteri(target, GL_TEXTURE_MAX_LEVEL, maxLevel);
            }
            // Bug fixed by Arc3D
            mutableState.mMaxMipmapLevel = maxLevel;
        }

        assert (x >= 0 && y >= 0 && width > 0 && height > 0);
        assert (pixels != 0);
        int bpp = ColorInfo.bytesPerPixel(srcColorType);

        int trimRowBytes = width * bpp;
        if (rowBytes != trimRowBytes) {
            int rowLength = rowBytes / bpp;
            glPixelStorei(GL_UNPACK_ROW_LENGTH, rowLength);
        } else {
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        }

        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        if (dsa) {
            glTextureSubImage2D(texName, 0,
                    x, y, width, height, srcFormat, srcType, pixels);
        } else {
            glTexSubImage2D(target, 0,
                    x, y, width, height, srcFormat, srcType, pixels);
        }

        if (!dsa) {
            if (texName != boundTexture) {
                glBindTexture(target, boundTexture);
            }
        }

        return true;
    }

    /**
     * Uses the base level of the image to compute the contents of the other mipmap levels.
     *
     * @return success or not
     */
    public boolean generateMipmaps(@RawPtr Image image) {
        assert image != null;
        assert image.isMipmapped();
        if (!image.isMipmapsDirty()) {
            return true;
        }
        if (image.isReadOnly()) {
            return false;
        }
        if (onGenerateMipmaps(image)) {
            image.setMipmapsDirty(false);
            return true;
        }
        return false;
    }

    private boolean onGenerateMipmaps(@RawPtr Image image) {
        var glImage = (GLTexture) image;
        if (mCaps.hasDSASupport()) {
            glGenerateTextureMipmap(glImage.getHandle());
        } else {
            var texName = glImage.getHandle();
            var boundTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
            if (texName != boundTexture) {
                glBindTexture(GL_TEXTURE_2D, texName);
            }
            glGenerateMipmap(GL_TEXTURE_2D);
            if (texName != boundTexture) {
                glBindTexture(GL_TEXTURE_2D, boundTexture);
            }
        }
        return true;
    }

    /**
     * Perform an image-to-image copy, with the specified regions. Scaling is
     * not allowed.
     * <p>
     * If their dimensions are same and formats are compatible, then this method will
     * attempt to perform copy. Otherwise, this method will attempt to perform blit,
     * which may include format conversion.
     * <p>
     * Only mipmap level <var>level</var> of 2D images will be copied, without any
     * multisampled buffer and depth/stencil buffer.
     *
     * @return success or not
     */
    public boolean copyImage(@RawPtr GLImage src,
                             int srcL, int srcT, int srcR, int srcB,
                             @RawPtr GLImage dst,
                             int dstX, int dstY,
                             int level) {
        int srcWidth = srcR - srcL;
        int srcHeight = srcB - srcT;

        // no scaling
        if (mCaps.hasCopyImageSupport() &&
                mCaps.canCopyImage(
                        src.getFormat(), 1,
                        dst.getFormat(), 1
                )) {
            //TODO checks
            glCopyImageSubData(
                    src.getHandle(),
                    src.getTarget(),
                    level,
                    srcL, srcT, 0,
                    dst.getHandle(),
                    dst.getTarget(),
                    level,
                    dstX, dstY, 0,
                    srcWidth, srcHeight, 1
            );
            return true;
        }

        if (src.getTarget() == GL_TEXTURE_2D &&
                dst.getTarget() == GL_TEXTURE_2D &&
                mCaps.canCopyTexSubImage(
                        src.getFormat(),
                        dst.getFormat()
                )) {

            int framebuffer = mCopySrcFramebuffer;
            if (framebuffer == 0) {
                mCopySrcFramebuffer = framebuffer = getGL().glGenFramebuffers();
            }
            if (framebuffer == 0) {
                return false;
            }

            int dstTexName = dst.getHandle();
            int boundTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
            if (dstTexName != boundTexture) {
                getGL().glBindTexture(GL_TEXTURE_2D, dstTexName);
            }

            int boundFramebuffer = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
            getGL().glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer);
            glFramebufferTexture(
                    GL_READ_FRAMEBUFFER,
                    GL_COLOR_ATTACHMENT0,
                    src.getHandle(),
                    level
            );

            glCopyTexSubImage2D(
                    GL_TEXTURE_2D,
                    level,
                    dstX, dstY,
                    srcL, srcT,
                    srcWidth, srcHeight
            );

            glFramebufferTexture(
                    GL_READ_FRAMEBUFFER,
                    GL_COLOR_ATTACHMENT0,
                    0,
                    0);
            getGL().glBindFramebuffer(GL_READ_FRAMEBUFFER, boundFramebuffer);

            if (dstTexName != boundTexture) {
                getGL().glBindTexture(GL_TEXTURE_2D, boundTexture);
            }

            return true;
        }

        //TODO

        return false;
    }

    /**
     * Special case of {@link #copyImage} that has same dimensions.
     */
    public boolean copyImage(Image src, int srcX, int srcY,
                             Image dst, int dstX, int dstY,
                             int width, int height) {
        return copyImage(
                src,
                srcX, srcY, srcX + width, srcY + height,
                dst,
                dstX, dstY, dstX + width, dstY + height,
                SamplerDesc.FILTER_NEAREST
        );
    }

    /**
     * Perform a surface-to-surface copy, with the specified regions.
     * <p>
     * If their dimensions are same and formats are compatible, then this method will
     * attempt to perform copy. Otherwise, this method will attempt to perform blit,
     * which may include resampling and format conversion. <var>filter</var> can be one
     * of {@link SamplerDesc#FILTER_NEAREST} and {@link SamplerDesc#FILTER_LINEAR}.
     * <p>
     * Only mipmap level 0 of 2D images will be copied, without any multisampled buffer
     * and depth/stencil buffer.
     *
     * @return success or not
     */
    public boolean copyImage(Image src,
                             int srcL, int srcT, int srcR, int srcB,
                             Image dst,
                             int dstL, int dstT, int dstR, int dstB,
                             int filter) {
        if ((dst.getSurfaceFlags() & ISurface.FLAG_READ_ONLY) != 0) {
            return false;
        }
        return onCopyImage(
                src,
                srcL, srcT, srcR, srcB,
                dst,
                dstL, dstT, dstR, dstB,
                filter
        );
    }

    private boolean onCopyImage(Image src,
                                int srcL, int srcT, int srcR, int srcB,
                                Image dst,
                                int dstL, int dstT, int dstR, int dstB,
                                int filter) {
        int srcWidth = srcR - srcL;
        int srcHeight = srcB - srcT;
        int dstWidth = dstR - dstL;
        int dstHeight = dstB - dstT;

        // we restore the context, no need to handle
        // handleDirtyContext();

        if (srcWidth == dstWidth && srcHeight == dstHeight) {
            // no scaling
            if (mCaps.hasCopyImageSupport() &&
                    src instanceof GLTexture srcImage &&
                    dst instanceof GLTexture dstImage &&
                    mCaps.canCopyImage(
                            srcImage.getFormat(), 1,
                            dstImage.getFormat(), 1
                    )) {
                //TODO checks
                glCopyImageSubData(
                        srcImage.getHandle(),
                        srcImage.getTarget(),
                        0,
                        srcL, srcT, 0,
                        dstImage.getHandle(),
                        dstImage.getTarget(),
                        0,
                        dstL, dstT, 0,
                        srcWidth, srcHeight, 1
                );
                return true;
            }

            if (src instanceof GLTexture srcTex &&
                    dst instanceof GLTexture dstTex &&
                    mCaps.canCopyTexSubImage(
                            srcTex.getFormat(),
                            dstTex.getFormat()
                    )) {

                int dstTexName = dstTex.getHandle();
                int boundTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
                if (dstTexName != boundTexture) {
                    getGL().glBindTexture(GL_TEXTURE_2D, dstTexName);
                }

                int framebuffer = mCopySrcFramebuffer;
                if (framebuffer == 0) {
                    mCopySrcFramebuffer = framebuffer = getGL().glGenFramebuffers();
                }
                int boundFramebuffer = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
                getGL().glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer);
                glFramebufferTexture(
                        GL_READ_FRAMEBUFFER,
                        GL_COLOR_ATTACHMENT0,
                        srcTex.getHandle(),
                        0
                );

                glCopyTexSubImage2D(
                        GL_TEXTURE_2D,
                        0,
                        dstL, dstT,
                        srcL, srcT,
                        srcWidth, srcHeight
                );

                glFramebufferTexture(
                        GL_READ_FRAMEBUFFER,
                        GL_COLOR_ATTACHMENT0,
                        0,
                        0);
                getGL().glBindFramebuffer(GL_READ_FRAMEBUFFER, boundFramebuffer);

                if (dstTexName != boundTexture) {
                    getGL().glBindTexture(GL_TEXTURE_2D, boundTexture);
                }

                return true;
            }
        }

        //TODO

        return false;
    }

    @Override
    protected OpsRenderPass onGetOpsRenderPass(ImageProxyView writeView,
                                               Rect2i contentBounds,
                                               byte colorOps,
                                               byte stencilOps,
                                               float[] clearColor,
                                               Set<SurfaceProxy> sampledTextures,
                                               int pipelineFlags) {
        mStats.incRenderPasses();
        if (mCachedOpsRenderPass == null) {
            mCachedOpsRenderPass = new GLOpsRenderPass(this);
        }
        //TODO
        /*return mCachedOpsRenderPass.set(writeView.getProxy().getGpuRenderTarget(),
                contentBounds,
                writeView.getOrigin(),
                colorOps,
                stencilOps,
                clearColor);*/
        return null;
    }

    @Override
    protected void onResolveRenderTarget(GpuRenderTarget renderTarget,
                                         int resolveLeft, int resolveTop,
                                         int resolveRight, int resolveBottom) {
        /*GLRenderTarget glRenderTarget = (GLRenderTarget) renderTarget;
        //TODO handle non-DSA case
        //handleDirtyContext();

        int renderFramebuffer = glRenderTarget.getRenderFramebuffer();
        int resolveFramebuffer = glRenderTarget.getResolveFramebuffer();

        // We should always have something to resolve
        assert (renderFramebuffer != 0 && renderFramebuffer != resolveFramebuffer);

        // BlitFramebuffer respects the scissor, so disable it.
        currentCommandBuffer().flushScissorTest(false);
        glBlitNamedFramebuffer(renderFramebuffer, resolveFramebuffer, // MSAA to single
                resolveLeft, resolveTop, resolveRight, resolveBottom, // src rect
                resolveLeft, resolveTop, resolveRight, resolveBottom, // dst rect
                GL_COLOR_BUFFER_BIT, GL_NEAREST);*/
    }

    private void flush(boolean forceFlush) {
        if (mNeedsFlush || forceFlush) {
            glFlush();
            mNeedsFlush = false;
        }
    }

    @Override
    public long insertFence() {
        long fence = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        mNeedsFlush = true;
        return fence;
    }

    @Override
    public boolean checkFence(long fence) {
        int result = glClientWaitSync(fence, 0, 0L);
        return (result == GL_CONDITION_SATISFIED || result == GL_ALREADY_SIGNALED);
    }

    @Override
    public void deleteFence(long fence) {
        glDeleteSync(fence);
    }

    @Override
    public void addFinishedCallback(FlushInfo.FinishedCallback callback) {
        mFinishedCallbacks.addLast(callback);
        mFinishedFences.enqueue(insertFence());
        assert (mFinishedCallbacks.size() == mFinishedFences.size());
    }

    @Override
    public void checkFinishedCallbacks() {
        // Bail after the first unfinished sync since we expect they signal in the order inserted.
        while (!mFinishedCallbacks.isEmpty() && checkFence(mFinishedFences.firstLong())) {
            // While we are processing a proc we need to make sure to remove it from the callback list
            // before calling it. This is because the client could trigger a call (e.g. calling
            // flushAndSubmit(/*sync=*/true)) that has us process the finished callbacks. We also must
            // process deleting the fence before a client may abandon the context.
            deleteFence(mFinishedFences.dequeueLong());
            mFinishedCallbacks.removeFirst().onFinished(true);
        }
        assert (mFinishedCallbacks.size() == mFinishedFences.size());
    }

    private void callAllFinishedCallbacks(boolean cleanup) {
        while (!mFinishedCallbacks.isEmpty()) {
            // While we are processing a proc we need to make sure to remove it from the callback list
            // before calling it. This is because the client could trigger a call (e.g. calling
            // flushAndSubmit(/*sync=*/true)) that has us process the finished callbacks. We also must
            // process deleting the fence before a client may abandon the context.
            if (cleanup) {
                deleteFence(mFinishedFences.dequeueLong());
            }
            mFinishedCallbacks.removeFirst().onFinished(true);
        }
        if (!cleanup) {
            mFinishedFences.clear();
        } else {
            assert (mFinishedFences.isEmpty());
        }
    }

    @Override
    public void waitForQueue() {
        glFinish();
    }

    // Binds a buffer to the GL target corresponding to 'type', updates internal state tracking, and
    // returns the GL target the buffer was bound to.
    // When 'type' is 'index', this function will also implicitly bind the default VAO.
    // If the caller wishes to bind an index buffer to a specific VAO, it can call glBind directly.
    /*public int bindBuffer(@Nonnull @RawPtr GLBuffer buffer) {
        assert !getCaps().hasDSASupport();

        handleDirtyContext(GLBackendState.kPipeline);

        int type = bufferUsageToType(buffer.getUsage());
        if (type == BUFFER_TYPE_INDEX) {
            // Index buffer state is tied to the vertex array.
            //currentCommandBuffer().bindVertexArray(null);
        }

        var bufferState = mHWBufferStates[type];
        if (bufferState.mBoundBufferUniqueID != buffer.getUniqueID()) {
            getGL().glBindBuffer(bufferState.mTarget, buffer.getHandle());
            bufferState.mBoundBufferUniqueID = buffer.getUniqueID();
        }

        return bufferState.mTarget;
    }*/

    /*public void bindIndexBufferInPipe(@Nonnull @RawPtr GLBuffer buffer) {
        // pipeline is already handled
        //handleDirtyContext(GLBackendState.kPipeline);

        assert bufferUsageToType(buffer.getUsage()) == BUFFER_TYPE_INDEX;

        // force rebind
        var bufferState = mHWBufferStates[BUFFER_TYPE_INDEX];
        assert bufferState.mTarget == GL_ELEMENT_ARRAY_BUFFER;

        getGL().glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer.getHandle());
        bufferState.mBoundBufferUniqueID = buffer.getUniqueID();
    }*/

    /*
     * Bind raw buffer ID to context (below OpenGL 4.5).
     *
     * @param usage  {@link BufferUsageFlags}
     * @param buffer the nonzero texture ID
     */
    /*public int bindBufferForSetup(int usage, int buffer) {
        assert !getCaps().hasDSASupport();

        handleDirtyContext(GLBackendState.kPipeline);

        int type = bufferUsageToType(usage);
        if (type == BUFFER_TYPE_INDEX) {
            // Index buffer state is tied to the vertex array.
            //currentCommandBuffer().bindVertexArray(null);
        }

        var bufferState = mHWBufferStates[type];
        getGL().glBindBuffer(bufferState.mTarget, buffer);
        bufferState.mBoundBufferUniqueID = INVALID_UNIQUE_ID;

        return bufferState.mTarget;
    }*/

    /*public void bindTextureSampler(int bindingUnit, GLTexture texture,
                                   int samplerState, short readSwizzle) {
        boolean dsa = mCaps.hasDSASupport();
        if (mHWTextureStates[bindingUnit] != texture.getUniqueID()) {
            if (dsa) {
                glBindTextureUnit(bindingUnit, texture.getHandle());
            } else {
                setTextureUnit(bindingUnit);
                getGL().glBindTexture(GL_TEXTURE_2D, texture.getHandle());
            }
            mHWTextureStates[bindingUnit] = texture.getUniqueID();
        }
        var hwSamplerState = mHWSamplerStates[bindingUnit];
        if (hwSamplerState.mSamplerState != samplerState) {
            GLSampler sampler = null;*//*= samplerState != 0
                    ? mResourceProvider.findOrCreateCompatibleSampler(samplerState)
                    : null;*//*
            //TODO
            getGL().glBindSampler(bindingUnit, sampler != null
                    ? sampler.getHandle()
                    : 0);
            hwSamplerState.mBoundSampler = RefCnt.move(hwSamplerState.mBoundSampler, sampler);
        }
        GLTextureMutableState mutableState = texture.getGLMutableState();
        if (mutableState.baseMipmapLevel != 0) {
            if (dsa) {
                glTextureParameteri(texture.getHandle(), GL_TEXTURE_BASE_LEVEL, 0);
            } else {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            }
            mutableState.baseMipmapLevel = 0;
        }
        int maxLevel = texture.getMipLevelCount() - 1; // minus base level
        if (mutableState.maxMipmapLevel != maxLevel) {
            if (dsa) {
                glTextureParameteri(texture.getHandle(), GL_TEXTURE_MAX_LEVEL, maxLevel);
            } else {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, maxLevel);
            }
            mutableState.maxMipmapLevel = maxLevel;
        }
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
            if (mutableState.getSwizzle(i) != swiz) {
                mutableState.setSwizzle(i, swiz);
                // swizzle enums are sequential
                int channel = GL_TEXTURE_SWIZZLE_R + i;
                if (dsa) {
                    glTextureParameteri(texture.getHandle(), channel, swiz);
                } else {
                    glTexParameteri(GL_TEXTURE_2D, channel, swiz);
                }
            }
            readSwizzle >>= 4;
        }
    }*/

    /*
     * Binds texture unit in context. OpenGL 3 only.
     *
     * @param unit 0-based texture unit index
     */
    /*public void setTextureUnit(int unit) {
        assert (unit >= 0 && unit < mHWTextureStates.length);
        if (unit != mHWActiveTextureUnit) {
            glActiveTexture(GL_TEXTURE0 + unit);
            mHWActiveTextureUnit = unit;
        }
    }*/
}
