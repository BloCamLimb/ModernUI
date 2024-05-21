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
import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.ops.OpsTask;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Set;

/**
 * A {@link Device} represents a logical GPU device and provides shared context info
 * of the backend 3D API.
 * <p>
 * It is responsible for
 * creating/deleting 3D API objects, transferring data, submitting 3D API commands, etc.
 * Most methods are only permitted on render thread.
 */
public abstract class Device implements Engine {

    // @formatter:off
    static {
        assert (LoadStoreOps.Load_Store         == LoadStoreOps.make(LoadOp.Load,       StoreOp.Store));
        assert (LoadStoreOps.Clear_Store        == LoadStoreOps.make(LoadOp.Clear,      StoreOp.Store));
        assert (LoadStoreOps.DontLoad_Store     == LoadStoreOps.make(LoadOp.DontCare,   StoreOp.Store));
        assert (LoadStoreOps.Load_DontStore     == LoadStoreOps.make(LoadOp.Load,       StoreOp.DontCare));
        assert (LoadStoreOps.Clear_DontStore    == LoadStoreOps.make(LoadOp.Clear,      StoreOp.DontCare));
        assert (LoadStoreOps.DontLoad_DontStore == LoadStoreOps.make(LoadOp.DontCare,   StoreOp.DontCare));
        //noinspection ConstantValue
        assert ( LoadOp.Count  <= (1 << LoadStoreOps.StoreOpShift)) &&
                (StoreOp.Count <= (1 << LoadStoreOps.StoreOpShift));
    }
    // @formatter:on

    // this device is managed by this context
    protected final ImmediateContext mContext;
    protected final Caps mCaps;
    protected final ShaderCompiler mCompiler;

    protected final Stats mStats = new Stats();

    protected boolean mOutOfMemoryEncountered = false;
    protected boolean mDeviceIsLost = false;

    private final ArrayList<FlushInfo.SubmittedCallback> mSubmittedCallbacks = new ArrayList<>();
    private int mResetBits = ~0;

    protected Device(ImmediateContext context, Caps caps) {
        assert context != null && caps != null;
        mContext = context;
        mCaps = caps;
        mCompiler = new ShaderCompiler();
    }

    public final ImmediateContext getContext() {
        return mContext;
    }

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

    public abstract ResourceProvider getResourceProvider();

    public abstract PipelineCache getPipelineCache();

    /**
     * Called by context when the underlying backend context is already or will be destroyed
     * before {@link ImmediateContext}.
     * <p>
     * If cleanup is true, free allocated resources (other than {@link ResourceCache}) before
     * returning and ensure no backend 3D API calls will be made after this method returns.
     * Otherwise, no cleanup should be attempted, immediately cease making backend API calls.
     */
    public void disconnect(boolean cleanup) {
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

    public abstract GpuBufferPool getVertexPool();

    public abstract GpuBufferPool getInstancePool();

    public abstract GpuBufferPool getIndexPool();

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

    *//**
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

    @Nullable
    public final @SharedPtr GpuRenderTarget createRenderTarget(int numColorTargets,
                                                    @Nullable Image[] colorTargets,
                                                    @Nullable Image[] resolveTargets,
                                                    @Nullable int[] mipLevels,
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

    @ApiStatus.OverrideOnly
    @Nullable
    @SharedPtr
    protected abstract GpuRenderTarget onCreateRenderTarget(int width, int height,
                                                            int sampleCount,
                                                            int numColorTargets,
                                                            @Nullable Image[] colorTargets,
                                                            @Nullable Image[] resolveTargets,
                                                            @Nullable int[] mipLevels,
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

    @Nullable
    @SharedPtr
    protected abstract GpuRenderTarget onWrapRenderableBackendTexture(BackendImage texture,
                                                                      int sampleCount,
                                                                      boolean ownership);

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

    @Nullable
    @SharedPtr
    public GpuRenderTarget wrapBackendRenderTarget(BackendRenderTarget backendRenderTarget) {
        if (!getCaps().isFormatRenderable(backendRenderTarget.getBackendFormat(),
                backendRenderTarget.getSampleCount())) {
            return null;
        }
        return onWrapBackendRenderTarget(backendRenderTarget);
    }

    @Nullable
    @SharedPtr
    public abstract GpuRenderTarget onWrapBackendRenderTarget(BackendRenderTarget backendRenderTarget);

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
    public boolean writePixels(Image texture,
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

    // overridden by backend-specific derived class to perform the surface write
    protected abstract boolean onWritePixels(Image texture,
                                             int x, int y,
                                             int width, int height,
                                             int dstColorType,
                                             int srcColorType,
                                             int rowBytes,
                                             long pixels);

    /**
     * Uses the base level of the image to compute the contents of the other mipmap levels.
     *
     * @return success or not
     */
    public final boolean generateMipmaps(Image image) {
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

    protected abstract boolean onGenerateMipmaps(Image image);

    /**
     * Special case of {@link #copySurface} that has same dimensions.
     */
    public final boolean copySurface(GpuSurface src, int srcX, int srcY,
                                     GpuSurface dst, int dstX, int dstY,
                                     int width, int height) {
        return copySurface(
                src,
                srcX, srcY, srcX + width, srcY + height,
                dst,
                dstX, dstY, dstX + width, dstY + height,
                SamplerState.FILTER_NEAREST
        );
    }

    /**
     * Perform a surface-to-surface copy, with the specified regions.
     * <p>
     * If their dimensions are same and formats are compatible, then this method will
     * attempt to perform copy. Otherwise, this method will attempt to perform blit,
     * which may include resampling and format conversion. <var>filter</var> can be one
     * of {@link SamplerState#FILTER_NEAREST} and {@link SamplerState#FILTER_LINEAR}.
     * <p>
     * Only mipmap level 0 of 2D images will be copied, without any multisampled buffer
     * and depth/stencil buffer.
     *
     * @return success or not
     */
    public final boolean copySurface(GpuSurface src,
                                     int srcL, int srcT, int srcR, int srcB,
                                     GpuSurface dst,
                                     int dstL, int dstT, int dstR, int dstB,
                                     int filter) {
        if ((dst.getSurfaceFlags() & ISurface.FLAG_READ_ONLY) != 0) {
            return false;
        }
        return onCopySurface(
                src,
                srcL, srcT, srcR, srcB,
                dst,
                dstL, dstT, dstR, dstB,
                filter
        );
    }

    protected abstract boolean onCopySurface(GpuSurface src,
                                             int srcL, int srcT, int srcR, int srcB,
                                             GpuSurface dst,
                                             int dstL, int dstT, int dstR, int dstB,
                                             int filter);

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
    public void resolveRenderTarget(GpuRenderTarget renderTarget,
                                    int resolveLeft, int resolveTop,
                                    int resolveRight, int resolveBottom) {
        assert (renderTarget != null);
        onResolveRenderTarget(renderTarget, resolveLeft, resolveTop, resolveRight, resolveBottom);
    }

    // overridden by backend-specific derived class to perform the resolve
    protected abstract void onResolveRenderTarget(GpuRenderTarget renderTarget,
                                                  int resolveLeft, int resolveTop,
                                                  int resolveRight, int resolveBottom);

    @Nullable
    @SharedPtr
    public final Buffer createBuffer(long size, int flags) {
        if (size <= 0) {
            getContext().getLogger().error(
                    "Failed to create buffer: invalid size {}",
                    size);
            return null;
        }
        if ((flags & (BufferUsageFlags.kTransferSrc | BufferUsageFlags.kTransferDst)) != 0 &&
                (flags & BufferUsageFlags.kDeviceLocal) != 0) {
            return null;
        }
        return onCreateBuffer(size, flags);
    }

    @Nullable
    @SharedPtr
    protected abstract Buffer onCreateBuffer(long size, int flags);

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
