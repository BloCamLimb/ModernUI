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

package icyllis.modernui.akashi;

import icyllis.modernui.annotation.SharedPtr;
import icyllis.modernui.graphics.*;
import icyllis.modernui.akashi.shaderc.Compiler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Set;

/**
 * Represents the client connection to the backend 3D API, holding a reference to
 * {@link DirectContext}. It is responsible for creating / deleting 3D API objects,
 * controlling binding status, uploading and downloading data, transferring
 * 3D API commands, etc. Most methods are expected on render thread.
 */
public abstract class Server {

    // this server is managed by this context
    protected final DirectContext mContext;
    protected final Caps mCaps;
    protected final Compiler mCompiler;

    protected final Stats mStats = new Stats();

    private final ArrayList<FlushInfo.SubmittedCallback> mSubmittedCallbacks = new ArrayList<>();
    private int mResetBits = ~0;

    protected Server(DirectContext context, Caps caps) {
        assert context != null && caps != null;
        mContext = context;
        mCaps = caps;
        mCompiler = new Compiler();
    }

    public final DirectContext getContext() {
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
    public final Compiler getShaderCompiler() {
        return mCompiler;
    }

    public abstract ThreadSafePipelineBuilder getPipelineBuilder();

    /**
     * Called by context when the underlying backend context is already or will be destroyed
     * before {@link DirectContext}.
     * <p>
     * If cleanup is true, free allocated resources (other than {@link ResourceCache}) before
     * returning and ensure no backend 3D API calls will be made after this method returns.
     * Otherwise, no cleanup should be attempted, immediately cease making backend API calls.
     */
    public void disconnect(boolean cleanup) {
    }

    public final Stats getStats() {
        return mStats;
    }

    /**
     * The server object normally assumes that no outsider is setting state
     * within the underlying 3D API's context/device/whatever. This call informs
     * the server that the state was modified, and it shouldn't make assumptions
     * about the state.
     */
    public final void markContextDirty(int state) {
        mResetBits |= state;
    }

    protected final void handleDirtyContext() {
        if (mResetBits != 0) {
            onResetContext(mResetBits);
            mResetBits = 0;
        }
    }

    /**
     * Called when the 3D context state is unknown. Subclass should emit any
     * assumed 3D context state and dirty any state cache.
     */
    protected void onResetContext(int resetBits) {
    }

    public abstract BufferAllocPool getVertexPool();

    public abstract BufferAllocPool getInstancePool();

    /**
     * Creates a texture object and allocates its server memory. In other words, the
     * image data is dirty and needs to be uploaded later. If mipmapped, also allocates
     * <code>(31 - CLZ(max(width,height)))</code> mipmaps in addition to the base level.
     * NPoT (non-power-of-two) dimensions are always supported. Compressed format are
     * supported.
     *
     * @param width  the width of the texture to be created
     * @param height the height of the texture to be created
     * @param format the backend format for the texture
     * @return the texture object if successful, otherwise nullptr
     * @see Surface#FLAG_BUDGETED
     * @see Surface#FLAG_MIPMAPPED
     * @see Surface#FLAG_RENDERABLE
     * @see Surface#FLAG_PROTECTED
     */
    @Nullable
    @SharedPtr
    public final Texture createTexture(int width, int height,
                                       BackendFormat format,
                                       int sampleCount,
                                       int surfaceFlags,
                                       String label) {
        if (format.isCompressed()) {
            // use createCompressedTexture
            return null;
        }
        if (!mCaps.validateSurfaceParams(width, height, format,
                sampleCount, surfaceFlags)) {
            return null;
        }
        int maxLevel = (surfaceFlags & Surface.FLAG_MIPMAPPED) != 0
                ? MathUtil.floorLog2(Math.max(width, height))
                : 0;
        int levelCount = maxLevel + 1; // +1 base level 0
        if ((surfaceFlags & Surface.FLAG_RENDERABLE) != 0) {
            sampleCount = mCaps.getRenderTargetSampleCount(sampleCount, format);
        }
        assert (sampleCount > 0 && sampleCount <= 64);
        handleDirtyContext();
        final Texture texture = onCreateTexture(width, height, format,
                levelCount, sampleCount, surfaceFlags);
        if (texture != null) {
            // we don't copy the backend format object, use identity rather than equals()
            assert texture.getBackendFormat() == format;
            assert (surfaceFlags & Surface.FLAG_RENDERABLE) == 0 || texture.getRenderTarget() != null;
            if (label != null) {
                texture.setLabel(label);
            }
            mStats.incTextureCreates();
        }
        return texture;
    }

    /**
     * Overridden by backend-specific derived class to create objects.
     * <p>
     * Texture size and format support will have already been validated in base class
     * before onCreateTexture is called.
     */
    @Nullable
    @SharedPtr
    protected abstract Texture onCreateTexture(int width, int height,
                                               BackendFormat format,
                                               int levelCount,
                                               int sampleCount,
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
    public Texture wrapRenderableBackendTexture(BackendTexture texture,
                                                int sampleCount,
                                                boolean ownership) {
        handleDirtyContext();
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
    protected abstract Texture onWrapRenderableBackendTexture(BackendTexture texture,
                                                              int sampleCount,
                                                              boolean ownership);

    /**
     * Updates the pixels in a rectangle of a texture. No sRGB/linear conversions are performed.
     * The write operation can fail because of the surface doesn't support writing (e.g. read only),
     * the color type is not allowed for the format of the texture or if the rectangle written
     * is not contained in the texture.
     *
     * @param texture      the texture to write to
     * @param dstColorType the color type for this use of the texture
     * @param srcColorType the color type of the source buffer
     * @param rowBytes     the row bytes, must be a multiple of srcColorType's bytes-per-pixel.
     * @param pixels       the pointer to the texel data for base level image
     * @return true if succeeded, false if not
     */
    public boolean writePixels(Texture texture,
                               int x, int y,
                               int width, int height,
                               int dstColorType,
                               int srcColorType,
                               int rowBytes, long pixels) {
        assert (texture != null);
        if (x < 0 || y < 0 || width <= 0 || height <= 0) {
            return false;
        }
        if (texture.isReadOnly()) {
            return false;
        }
        assert (texture.getWidth() > 0 && texture.getHeight() > 0);
        if (x + width > texture.getWidth() || y + height > texture.getHeight()) {
            return false;
        }
        int bpp = ImageInfo.bytesPerPixel(srcColorType);
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
        handleDirtyContext();
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
    protected abstract boolean onWritePixels(Texture texture,
                                             int x, int y,
                                             int width, int height,
                                             int dstColorType,
                                             int srcColorType,
                                             int rowBytes,
                                             long pixels);

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
    public final OpsRenderPass getOpsRenderPass(SurfaceProxyView writeView,
                                                Rect contentBounds,
                                                byte colorOps,
                                                byte stencilOps,
                                                float[] clearColor,
                                                Set<TextureProxy> sampledTextures,
                                                int pipelineFlags) {
        mStats.incRenderPasses();
        return onGetOpsRenderPass(writeView, contentBounds,
                colorOps, stencilOps, clearColor,
                sampledTextures, pipelineFlags);
    }

    protected abstract OpsRenderPass onGetOpsRenderPass(SurfaceProxyView writeView,
                                                        Rect contentBounds,
                                                        byte colorOps,
                                                        byte stencilOps,
                                                        float[] clearColor,
                                                        Set<TextureProxy> sampledTextures,
                                                        int pipelineFlags);

    /**
     * Resolves MSAA. The resolve rectangle must already be in the native destination space.
     */
    public void resolveRenderTarget(RenderTarget renderTarget,
                                    int resolveLeft, int resolveTop,
                                    int resolveRight, int resolveBottom) {
        assert (renderTarget != null);
        handleDirtyContext();
        onResolveRenderTarget(renderTarget, resolveLeft, resolveTop, resolveRight, resolveBottom);
    }

    // overridden by backend-specific derived class to perform the resolve
    protected abstract void onResolveRenderTarget(RenderTarget renderTarget,
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

    public static final class Stats {

        private int mTextureCreates = 0;
        private int mTextureUploads = 0;
        private int mTransfersToTexture = 0;
        private int mTransfersFromSurface = 0;
        private int mStencilAttachmentCreates = 0;
        private int mMSAAAttachmentCreates = 0;
        private int mNumDraws = 0;
        private int mNumFailedDraws = 0;
        private int mNumSubmitToGpus = 0;
        private int mNumScratchTexturesReused = 0;
        private int mNumScratchMSAAAttachmentsReused = 0;
        private int mRenderPasses = 0;
        private int mNumReorderedDAGsOverBudget = 0;

        public Stats() {
        }

        public void reset() {
            mTextureCreates = 0;
            mTextureUploads = 0;
            mTransfersToTexture = 0;
            mTransfersFromSurface = 0;
            mStencilAttachmentCreates = 0;
            mMSAAAttachmentCreates = 0;
            mNumDraws = 0;
            mNumFailedDraws = 0;
            mNumSubmitToGpus = 0;
            mNumScratchTexturesReused = 0;
            mNumScratchMSAAAttachmentsReused = 0;
            mRenderPasses = 0;
            mNumReorderedDAGsOverBudget = 0;
        }

        public int numTextureCreates() {
            return mTextureCreates;
        }

        public void incTextureCreates() {
            mTextureCreates++;
        }

        public int numTextureUploads() {
            return mTextureUploads;
        }

        public void incTextureUploads() {
            mTextureUploads++;
        }

        public int numTransfersToTexture() {
            return mTransfersToTexture;
        }

        public void incTransfersToTexture() {
            mTransfersToTexture++;
        }

        public int numTransfersFromSurface() {
            return mTransfersFromSurface;
        }

        public void incTransfersFromSurface() {
            mTransfersFromSurface++;
        }

        public int numStencilAttachmentCreates() {
            return mStencilAttachmentCreates;
        }

        public void incStencilAttachmentCreates() {
            mStencilAttachmentCreates++;
        }

        public int msaaAttachmentCreates() {
            return mMSAAAttachmentCreates;
        }

        public void incMSAAAttachmentCreates() {
            mMSAAAttachmentCreates++;
        }

        public int numDraws() {
            return mNumDraws;
        }

        public void incNumDraws() {
            mNumDraws++;
        }

        public int numFailedDraws() {
            return mNumFailedDraws;
        }

        public void incNumFailedDraws() {
            mNumFailedDraws++;
        }

        public int numSubmitToGpus() {
            return mNumSubmitToGpus;
        }

        public void incNumSubmitToGpus() {
            mNumSubmitToGpus++;
        }

        public int numScratchTexturesReused() {
            return mNumScratchTexturesReused;
        }

        public void incNumScratchTexturesReused() {
            mNumScratchTexturesReused++;
        }

        public int numScratchMSAAAttachmentsReused() {
            return mNumScratchMSAAAttachmentsReused;
        }

        public void incNumScratchMSAAAttachmentsReused() {
            mNumScratchMSAAAttachmentsReused++;
        }

        public int numRenderPasses() {
            return mRenderPasses;
        }

        public void incRenderPasses() {
            mRenderPasses++;
        }

        public int numReorderedDAGsOverBudget() {
            return mNumReorderedDAGsOverBudget;
        }

        public void incNumReorderedDAGsOverBudget() {
            mNumReorderedDAGsOverBudget++;
        }
    }
}
