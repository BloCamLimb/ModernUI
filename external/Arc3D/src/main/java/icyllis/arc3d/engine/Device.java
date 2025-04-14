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

package icyllis.arc3d.engine;

import icyllis.arc3d.compiler.ShaderCompiler;
import icyllis.arc3d.core.Rect2i;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.granite.RendererProvider;
import icyllis.arc3d.granite.ShaderCodeSource;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@link Device} represents a logical GPU device and provides shared context info
 * of the backend 3D API. A {@link Device} is created with an {@link ImmediateContext}.
 */
public abstract class Device implements Engine {

    // @formatter:off
    static {
        assert (LoadStoreOps.Load_Store         == LoadStoreOps.make(LoadOp.kLoad,       StoreOp.kStore));
        assert (LoadStoreOps.Clear_Store        == LoadStoreOps.make(LoadOp.kClear,      StoreOp.kStore));
        assert (LoadStoreOps.DontLoad_Store     == LoadStoreOps.make(LoadOp.kDiscard,   StoreOp.kStore));
        assert (LoadStoreOps.Load_DontStore     == LoadStoreOps.make(LoadOp.kLoad,       StoreOp.kDiscard));
        assert (LoadStoreOps.Clear_DontStore    == LoadStoreOps.make(LoadOp.kClear,      StoreOp.kDiscard));
        assert (LoadStoreOps.DontLoad_DontStore == LoadStoreOps.make(LoadOp.kDiscard,   StoreOp.kDiscard));
        //noinspection ConstantValue
        assert ( LoadOp.kCount  <= (1 << LoadStoreOps.StoreOpShift)) &&
                (StoreOp.kCount <= (1 << LoadStoreOps.StoreOpShift));
    }
    // @formatter:on

    private static final AtomicInteger sNextID = new AtomicInteger(1);

    private static int createUniqueID() {
        for (; ; ) {
            final int value = sNextID.get();
            final int newValue = value == -1 ? 1 : value + 1; // 0 is reserved
            if (sNextID.weakCompareAndSetVolatile(value, newValue)) {
                return value;
            }
        }
    }

    private final int mBackend;
    private final ContextOptions mOptions;
    private final int mContextID;

    private ThreadSafeCache mThreadSafeCache;
    private GlobalResourceCache mGlobalResourceCache;
    private ShaderCodeSource mShaderCodeSource;
    RendererProvider mRendererProvider;

    private final AtomicBoolean mDiscarded = new AtomicBoolean(false);

    // this device is managed by this context
    //protected final ImmediateContext mContext;
    protected final Caps mCaps;
    protected final ShaderCompiler mCompiler;

    protected final Stats mStats = new Stats();

    protected volatile boolean mOutOfMemoryEncountered = false;
    protected volatile boolean mDeviceIsLost = false;

    private final ArrayList<FlushInfo.SubmittedCallback> mSubmittedCallbacks = new ArrayList<>();
    private int mResetBits = ~0;

    private final Thread mExecutingThread;

    protected Device(int backend, ContextOptions options, Caps caps) {
        assert caps != null;
        mExecutingThread = Thread.currentThread();
        mBackend = backend;
        mOptions = options;
        mContextID = createUniqueID();
        //mContext = context;
        mCaps = caps;
        mCompiler = new ShaderCompiler();
        mGlobalResourceCache = new GlobalResourceCache();
        mShaderCodeSource = new ShaderCodeSource();
    }

    public final Logger getLogger() {
        return Objects.requireNonNullElse(getOptions().mLogger, NOPLogger.NOP_LOGGER);
    }

    /**
     * @return the command-executing thread
     */
    public final Thread getExecutingThread() {
        return mExecutingThread;
    }

    /**
     * @return true if calling from the command-executing thread
     */
    public final boolean isOnExecutingThread() {
        return mExecutingThread == Thread.currentThread();
    }

    /**
     * Retrieve the default {@link BackendFormat} for a given {@code ColorType} and renderability.
     * It is guaranteed that this backend format will be the one used by the following
     * {@code ColorType} and {@link SurfaceCharacterization#createBackendFormat(int, BackendFormat)}.
     * <p>
     * The caller should check that the returned format is valid (nullability).
     *
     * @param colorType  see {@link ImageDesc}
     * @param renderable true if the format will be used as color attachments
     */
    @Nullable
    public BackendFormat getDefaultBackendFormat(int colorType, boolean renderable) {
        assert (mCaps != null);

        colorType = Engine.colorTypeToPublic(colorType);
        BackendFormat format = mCaps.getDefaultBackendFormat(colorType, renderable);
        if (format == null) {
            return null;
        }
        assert (!renderable ||
                mCaps.isFormatRenderable(colorType, format, 1));
        return format;
    }

    /**
     * Retrieve the {@link BackendFormat} for a given {@code CompressionType}. This is
     * guaranteed to match the backend format used by the following
     * createCompressedBackendTexture methods that take a {@code CompressionType}.
     * <p>
     * The caller should check that the returned format is valid (nullability).
     *
     * @param compressionType see {@link ImageDesc}
     */
    @Nullable
    public BackendFormat getCompressedBackendFormat(int compressionType) {
        assert (mCaps != null);

        BackendFormat format = mCaps.getCompressedBackendFormat(compressionType);
        assert (format == null) ||
                (!format.isExternal() && mCaps.isFormatTexturable(format));
        return format;
    }

    /**
     * Gets the maximum supported sample count for a color type. 1 is returned if only non-MSAA
     * rendering is supported for the color type. 0 is returned if rendering to this color type
     * is not supported at all.
     *
     * @param colorType see {@link ImageDesc}
     */
    public int getMaxSurfaceSampleCount(int colorType) {
        assert (mCaps != null);

        colorType = Engine.colorTypeToPublic(colorType);
        BackendFormat format = mCaps.getDefaultBackendFormat(colorType, true);
        if (format == null) {
            return 0;
        }
        return mCaps.getMaxRenderTargetSampleCount(format);
    }

    /**
     * @return initialized or not, if {@link ImmediateContext} is created, it must be true
     */
    public boolean isValid() {
        return mCaps != null;
    }

    @ApiStatus.Internal
    public int getBackend() {
        return mBackend;
    }

    @ApiStatus.Internal
    public ContextOptions getOptions() {
        return mOptions;
    }

    @ApiStatus.Internal
    public int getContextID() {
        return mContextID;
    }

    boolean discard() {
        return !mDiscarded.compareAndExchange(false, true);
    }

    public boolean isDiscarded() {
        return mDiscarded.get();
    }

    @Override
    public int hashCode() {
        return mContextID;
    }

    // use reference equality

    /**
     * Gets the capabilities of the context.
     */
    public Caps getCaps() {
        return mCaps;
    }

    /**
     * Gets the compiler used for compiling AkSL into backend shader code.
     */
    public final ShaderCompiler getShaderCompiler() {
        return mCompiler;
    }

    public abstract ResourceProvider makeResourceProvider(Context context, long maxResourceBudget);

    public final GlobalResourceCache getGlobalResourceCache() {
        return mGlobalResourceCache;
    }

    public final ShaderCodeSource getShaderCodeSource() {
        return mShaderCodeSource;
    }

    public final RendererProvider getRendererProvider() {
        return mRendererProvider;
    }

    /**
     * Called by context when the underlying backend context is already or will be destroyed
     * before {@link ImmediateContext}.
     * <p>
     * If cleanup is true, free allocated resources (other than {@link ResourceCache}) before
     * returning and ensure no backend 3D API calls will be made after this method returns.
     * Otherwise, no cleanup should be attempted, immediately cease making backend API calls.
     */
    public void disconnect(boolean cleanup) {
        mGlobalResourceCache.release();
    }

    /**
     * Returns true if GPU is gone.
     */
    public boolean isDeviceLost() {
        return mDeviceIsLost;
    }

    public final Stats getStats() {
        return mStats;
    }

    /**
     * The engine object normally assumes that no outsider is setting state
     * within the underlying 3D API's context/device/whatever. This call informs
     * the engine that the state was modified, and it shouldn't make assumptions
     * about the state.
     */
    public final void markContextDirty(int state) {
        mResetBits |= state;
    }

    protected void handleDirtyContext(int state) {
        int dirtyBits = (mResetBits & state);
        if (dirtyBits != 0) {
            onResetContext(dirtyBits);
            mResetBits &= ~dirtyBits;
        }
    }

    /**
     * Called when the 3D context state is unknown. Subclass should emit any
     * assumed 3D context state and dirty any state cache.
     */
    protected void onResetContext(int resetBits) {
    }

    @Deprecated
    public GpuBufferPool getVertexPool() {
        return null;
    }

    @Deprecated
    public GpuBufferPool getInstancePool() {
        return null;
    }

    @Deprecated
    public GpuBufferPool getIndexPool() {
        return null;
    }

    // Overridden by backend to free additional resources used by command buffers,
    // like framebuffers, this is called by ImmediateContext on the executing thread
    protected void freeGpuResources() {
    }

    // Overridden by backend to free additional resources used by command buffers,
    // like framebuffers, this is called by ImmediateContext on the executing thread
    protected void purgeResourcesNotUsedSince(long timeMillis) {
    }

    /* *//**
     * Creates a new GPU image object and allocates its GPU memory. In other words, the
     * image data is dirty and needs to be uploaded later. If mipmapped, also allocates
     * <code>(31 - CLZ(max(width,height)))</code> mipmaps in addition to the base level.
     * NPoT (non-power-of-two) dimensions are always supported. Compressed format are
     * supported.
     *
     * @param width  the width of the image to be created
     * @param height the height of the image to be created
     * @param format the backend format for the image
     * @return the image object if successful, otherwise nullptr
     * @see ISurface#FLAG_BUDGETED
     * @see ISurface#FLAG_MIPMAPPED
     * @see ISurface#FLAG_RENDERABLE
     * @see ISurface#FLAG_PROTECTED
     *//*
    @Nullable
    @SharedPtr
    public GpuImage createImage(int width, int height,
                                int depth, int arraySize,
                                ImageInfo info,
                                boolean budgeted,
                                @Nullable String label) {
        if (format.isCompressed()) {
            return null;
        }
        if (!mCaps.validateSurfaceParams(width, height, format,
                sampleCount, surfaceFlags)) {
            return null;
        }
        int mipLevelCount = (surfaceFlags & ISurface.FLAG_MIPMAPPED) != 0
                ? MathUtil.floorLog2(Math.max(width, height)) + 1 // +1 base level 0
                : 1; // only base level
        if ((surfaceFlags & ISurface.FLAG_RENDERABLE) != 0) {
            sampleCount = mCaps.getRenderTargetSampleCount(sampleCount, format);
        }
        if (sampleCount > 1 && mipLevelCount > 1) {
            getContext().getLogger().error(
                    "Failed to GpuDevice::createImage: mipmapped images cannot be multisampled, " +
                            "width {} height {} format {} sampleCount {} mipLevelCount {}",
                    width, height, format, sampleCount, mipLevelCount);
            return null;
        }
        assert (sampleCount > 0 && sampleCount <= 64);
        final GpuImage image = onCreateImage(width, height, format,
                mipLevelCount, sampleCount, surfaceFlags);
        if (image != null) {
            // we don't copy the backend format object, use identity rather than equals()
            assert image.getBackendFormat() == format;
            if (label != null) {
                image.setLabel(label);
            }
            mStats.incImageCreates();
            if (image.isSampledImage()) {
                mStats.incTextureCreates();
            }
        }
        return image;
    }

    */

    /**
     * Overridden by backend-specific derived class to create objects.
     * <p>
     * Image size and format support will have already been validated in base class
     * before onCreateImage is called.
     *//*
    @ApiStatus.OverrideOnly
    @Nullable
    @SharedPtr
    protected abstract GpuImage onCreateImage(int width, int height,
                                              BackendFormat format,
                                              int mipLevelCount,
                                              int sampleCount,
                                              int surfaceFlags);*/
    @Deprecated
    @Nullable
    public final @SharedPtr GpuRenderTarget createRenderTarget(int numColorTargets,
                                                               Image @Nullable [] colorTargets,
                                                               Image @Nullable [] resolveTargets,
                                                               int @Nullable [] mipLevels,
                                                               @Nullable Image depthStencilTarget,
                                                               int surfaceFlags) {
        if (numColorTargets < 0 || numColorTargets > mCaps.maxColorAttachments()) {
            return null;
        }
        int usedColorTargets = 0;
        if (colorTargets != null) {
            for (int i = 0; i < numColorTargets; i++) {
                usedColorTargets += colorTargets[i] != null ? 1 : 0;
            }
        }
        if (usedColorTargets == 0 && depthStencilTarget == null) {
            return null;
        }

        // find logical dimension
        int sampleCount = -1;
        int width = Integer.MAX_VALUE;
        int height = Integer.MAX_VALUE;
        if (colorTargets != null) {
            for (int i = 0; i < numColorTargets; i++) {
                Image colorTarget = colorTargets[i];
                if (colorTarget == null) continue;
                if (!colorTarget.isRenderable()) {
                    return null;
                }
                int samples = colorTarget.getSampleCount();
                if (sampleCount == -1) {
                    sampleCount = samples;
                } else if (sampleCount != samples) {
                    return null;
                }
                width = Math.min(width, colorTarget.getWidth());
                height = Math.min(height, colorTarget.getHeight());
            }
        }
        if (depthStencilTarget != null) {
            if (!depthStencilTarget.isRenderable()) {
                return null;
            }
            int samples = depthStencilTarget.getSampleCount();
            if (sampleCount == -1) {
                sampleCount = samples;
            } else if (sampleCount != samples) {
                return null;
            }
            width = Math.min(width, depthStencilTarget.getWidth());
            height = Math.min(height, depthStencilTarget.getHeight());
        }
        if (sampleCount == -1) {
            return null;
        }
        assert width < Integer.MAX_VALUE;
        assert height < Integer.MAX_VALUE;
        if (resolveTargets != null) {
            for (int i = 0; i < numColorTargets; i++) {
                Image resolveTarget = resolveTargets[i];
                if (resolveTarget == null) continue;
                if (colorTargets == null || colorTargets[i] == null) {
                    return null;
                }
                if (sampleCount == 1) {
                    return null;
                }
                if (resolveTarget.getSampleCount() != 1) {
                    return null;
                }
                if (resolveTarget.getWidth() < width ||
                        resolveTarget.getHeight() < height) {
                    return null;
                }
            }
        }

        return onCreateRenderTarget(width, height,
                sampleCount,
                numColorTargets,
                colorTargets,
                resolveTargets,
                mipLevels,
                depthStencilTarget,
                surfaceFlags);
    }

    @Deprecated
    @ApiStatus.OverrideOnly
    @Nullable
    @SharedPtr
    protected abstract GpuRenderTarget onCreateRenderTarget(int width, int height,
                                                            int sampleCount,
                                                            int numColorTargets,
                                                            Image @Nullable [] colorTargets,
                                                            Image @Nullable [] resolveTargets,
                                                            int @Nullable [] mipLevels,
                                                            @Nullable Image depthStencilTarget,
                                                            int surfaceFlags);

    /**
     * This makes the backend texture be renderable. If <code>sampleCount</code> is > 1 and
     * the underlying API uses separate MSAA render buffers then a MSAA render buffer is created
     * that resolves to the texture.
     * <p>
     * Ownership specifies rules for external GPU resources imported into Engine. If false,
     * Engine will assume the client will keep the resource alive and Engine will not free it.
     * If true, Engine will assume ownership of the resource and free it.
     *
     * @param texture the backend texture must be single sample
     * @return a non-cacheable render target, or null if failed
     */
    @Deprecated
    @Nullable
    @SharedPtr
    public GpuRenderTarget wrapRenderableBackendTexture(BackendImage texture,
                                                        int sampleCount,
                                                        boolean ownership) {
        if (sampleCount < 1) {
            return null;
        }

        final Caps caps = mCaps;

        if (!caps.isFormatTexturable(texture.getBackendFormat()) ||
                !caps.isFormatRenderable(texture.getBackendFormat(), sampleCount)) {
            return null;
        }

        if (texture.getWidth() > caps.maxRenderTargetSize() ||
                texture.getHeight() > caps.maxRenderTargetSize()) {
            return null;
        }
        return onWrapRenderableBackendTexture(texture, sampleCount, ownership);
    }

    @Deprecated
    @Nullable
    @SharedPtr
    protected abstract GpuRenderTarget onWrapRenderableBackendTexture(BackendImage texture,
                                                                      int sampleCount,
                                                                      boolean ownership);

    @Deprecated
    @Nullable
    @SharedPtr
    public final GpuRenderTarget wrapGLDefaultFramebuffer(int width, int height,
                                                          int sampleCount,
                                                          int depthBits,
                                                          int stencilBits,
                                                          BackendFormat format) {
        if (!getCaps().isFormatRenderable(format,
                sampleCount)) {
            return null;
        }
        return onWrapGLDefaultFramebuffer(width, height, sampleCount, depthBits, stencilBits, format);
    }

    @Deprecated
    @ApiStatus.OverrideOnly
    @Nullable
    @SharedPtr
    protected GpuRenderTarget onWrapGLDefaultFramebuffer(int width, int height,
                                                         int sampleCount,
                                                         int depthBits,
                                                         int stencilBits,
                                                         BackendFormat format) {
        return null;
    }

    @Deprecated
    @Nullable
    @SharedPtr
    public GpuRenderTarget wrapBackendRenderTarget(BackendRenderTarget backendRenderTarget) {
        if (!getCaps().isFormatRenderable(backendRenderTarget.getBackendFormat(),
                backendRenderTarget.getSampleCount())) {
            return null;
        }
        return onWrapBackendRenderTarget(backendRenderTarget);
    }

    @Deprecated
    @Nullable
    @SharedPtr
    public abstract GpuRenderTarget onWrapBackendRenderTarget(BackendRenderTarget backendRenderTarget);

    /**
     * Returns a {@link OpsRenderPass} which {@link OpsTask OpsTasks} record draw commands to.
     *
     * @param writeView       the render target to be rendered to
     * @param contentBounds   the clipped content bounds of the render pass
     * @param colorOps        the color load/store ops
     * @param stencilOps      the stencil load/store ops
     * @param clearColor      the color used to clear the color buffer
     * @param sampledTextures list of all textures to be sampled in the render pass (no refs)
     * @param pipelineFlags   combination of flags of all pipelines to be used in the render pass
     * @return a render pass used to record draw commands, or null if failed
     */
    @Deprecated
    @Nullable
    public final OpsRenderPass getOpsRenderPass(ImageProxyView writeView,
                                                Rect2i contentBounds,
                                                byte colorOps,
                                                byte stencilOps,
                                                float[] clearColor,
                                                Set<SurfaceProxy> sampledTextures,
                                                int pipelineFlags) {
        mStats.incRenderPasses();
        return onGetOpsRenderPass(writeView, contentBounds,
                colorOps, stencilOps, clearColor,
                sampledTextures, pipelineFlags);
    }

    @Deprecated
    protected abstract OpsRenderPass onGetOpsRenderPass(ImageProxyView writeView,
                                                        Rect2i contentBounds,
                                                        byte colorOps,
                                                        byte stencilOps,
                                                        float[] clearColor,
                                                        Set<SurfaceProxy> sampledTextures,
                                                        int pipelineFlags);

    /**
     * Resolves MSAA. The resolve rectangle must already be in the native destination space.
     */
    @Deprecated
    public void resolveRenderTarget(GpuRenderTarget renderTarget,
                                    int resolveLeft, int resolveTop,
                                    int resolveRight, int resolveBottom) {
        assert (renderTarget != null);
        onResolveRenderTarget(renderTarget, resolveLeft, resolveTop, resolveRight, resolveBottom);
    }

    // overridden by backend-specific derived class to perform the resolve
    @Deprecated
    protected abstract void onResolveRenderTarget(GpuRenderTarget renderTarget,
                                                  int resolveLeft, int resolveTop,
                                                  int resolveRight, int resolveBottom);

    /**
     * Creates a new fence and inserts it into the graphics queue.
     * Calls {@link #deleteFence(long)} if the fence is no longer used.
     *
     * @return the handle to the fence, or null if failed
     */
    public abstract long insertFence();

    /**
     * Checks a fence on client side to see if signalled. This method returns immediately.
     *
     * @param fence the handle to the fence
     * @return true if signalled, false otherwise
     */
    public abstract boolean checkFence(long fence);

    /**
     * Deletes an existing fence that previously returned by {@link #insertFence()}.
     *
     * @param fence the handle to the fence, cannot be null
     */
    public abstract void deleteFence(long fence);

    public abstract void addFinishedCallback(FlushInfo.FinishedCallback callback);

    public abstract void checkFinishedCallbacks();

    /**
     * Blocks the current thread and waits for GPU to finish outstanding works.
     */
    public abstract void waitForQueue();

    /**
     * Checks if we detected an OOM from the underlying 3D API and if so returns true and resets
     * the internal OOM state to false. Otherwise, returns false.
     */
    public final boolean checkOutOfMemory() {
        if (mOutOfMemoryEncountered) {
            mOutOfMemoryEncountered = false;
            return true;
        }
        return false;
    }

    public static final class Stats {

        private long mImageCreates = 0;
        private long mTextureCreates = 0;
        private long mTextureUploads = 0;
        private long mTransfersToTexture = 0;
        private long mTransfersFromSurface = 0;
        private long mNumDraws = 0;
        private long mNumFailedDraws = 0;
        private long mNumSubmitToGpus = 0;
        private long mNumScratchTexturesReused = 0;
        private long mNumScratchRenderTargetsReused = 0;
        private long mNumScratchMSAAAttachmentsReused = 0;
        private long mRenderPasses = 0;

        public Stats() {
        }

        public void reset() {
            mImageCreates = 0;
            mTextureCreates = 0;
            mTextureUploads = 0;
            mTransfersToTexture = 0;
            mTransfersFromSurface = 0;
            mNumDraws = 0;
            mNumFailedDraws = 0;
            mNumSubmitToGpus = 0;
            mNumScratchTexturesReused = 0;
            mNumScratchRenderTargetsReused = 0;
            mNumScratchMSAAAttachmentsReused = 0;
            mRenderPasses = 0;
        }

        public long numImageCreates() {
            return mImageCreates;
        }

        public void incImageCreates() {
            mImageCreates++;
        }

        public long numTextureCreates() {
            return mTextureCreates;
        }

        public void incTextureCreates() {
            mTextureCreates++;
        }

        public long numTextureUploads() {
            return mTextureUploads;
        }

        public void incTextureUploads() {
            mTextureUploads++;
        }

        public long numTransfersToTexture() {
            return mTransfersToTexture;
        }

        public void incTransfersToTexture() {
            mTransfersToTexture++;
        }

        public long numTransfersFromSurface() {
            return mTransfersFromSurface;
        }

        public void incTransfersFromSurface() {
            mTransfersFromSurface++;
        }

        public long numDraws() {
            return mNumDraws;
        }

        public void incNumDraws() {
            mNumDraws++;
        }

        public void incNumDraws(int increment) {
            mNumDraws += increment;
        }

        public long numFailedDraws() {
            return mNumFailedDraws;
        }

        public void incNumFailedDraws() {
            mNumFailedDraws++;
        }

        public long numSubmitToGpus() {
            return mNumSubmitToGpus;
        }

        public void incNumSubmitToGpus() {
            mNumSubmitToGpus++;
        }

        public long numScratchTexturesReused() {
            return mNumScratchTexturesReused;
        }

        public void incNumScratchTexturesReused() {
            mNumScratchTexturesReused++;
        }

        public long numScratchRenderTargetsReused() {
            return mNumScratchRenderTargetsReused;
        }

        public void incNumScratchRenderTargetsReused() {
            mNumScratchRenderTargetsReused++;
        }

        public long numScratchMSAAAttachmentsReused() {
            return mNumScratchMSAAAttachmentsReused;
        }

        public void incNumScratchMSAAAttachmentsReused() {
            mNumScratchMSAAAttachmentsReused++;
        }

        public long numRenderPasses() {
            return mRenderPasses;
        }

        public void incRenderPasses() {
            mRenderPasses++;
        }

        @Override
        public String toString() {
            return "GpuDevice.Stats{" +
                    "mImageCreates=" + mImageCreates +
                    ", mTextureCreates=" + mTextureCreates +
                    ", mTextureUploads=" + mTextureUploads +
                    ", mTransfersToTexture=" + mTransfersToTexture +
                    ", mTransfersFromSurface=" + mTransfersFromSurface +
                    ", mNumDraws=" + mNumDraws +
                    ", mNumFailedDraws=" + mNumFailedDraws +
                    ", mNumSubmitToGpus=" + mNumSubmitToGpus +
                    ", mNumScratchTexturesReused=" + mNumScratchTexturesReused +
                    ", mNumScratchRenderTargetsReused=" + mNumScratchRenderTargetsReused +
                    ", mNumScratchMSAAAttachmentsReused=" + mNumScratchMSAAAttachmentsReused +
                    ", mRenderPasses=" + mRenderPasses +
                    '}';
        }
    }
}
