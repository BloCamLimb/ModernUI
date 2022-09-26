/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.engine;

import icyllis.arcticgi.core.Rect2i;
import icyllis.arcticgi.core.SharedPtr;
import icyllis.arcticgi.engine.ops.OpsTask;
import icyllis.arcticgi.sksl.Compiler;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static icyllis.arcticgi.engine.Engine.SurfaceFlag_Mipmapped;

/**
 * Represents the client connection to the backend 3D API, holding a reference to
 * {@link DirectContext}. It is responsible for creating / deleting 3D API objects,
 * controlling binding status, uploading and downloading data, transferring
 * 3D API commands, etc. Most methods are expected on render thread.
 */
public abstract class Server {

    // 1, 2, 4, 8, 16, ...
    protected static final int MAX_NUM_SAMPLES_COUNT = 8;

    // this server is managed by this context
    protected final DirectContext mContext;
    protected final Caps mCaps;
    protected final Compiler mCompiler;

    protected final Stats mStats = new Stats();

    private final List<FlushInfo.SubmittedCallback> mSubmittedCallbacks = new ArrayList<>();
    private int mResetBits = ~0;

    protected Server(DirectContext context, Caps caps) {
        assert context != null && caps != null;
        mContext = context;
        mCaps = caps;
        mCompiler = new Compiler(caps.mShaderCaps);
    }

    public final DirectContext getContext() {
        return mContext;
    }

    /**
     * Gets the capabilities of the context.
     */
    public final Caps getCaps() {
        return mCaps;
    }

    /**
     * Gets the compiler used for compiling SkSL into backend shader code.
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
     * @see Engine#SurfaceFlag_Mipmapped
     * @see Engine#SurfaceFlag_Budgeted
     * @see Engine#SurfaceFlag_Protected
     */
    @Nullable
    @SharedPtr
    public final Texture createTexture(int width, int height,
                                       BackendFormat format,
                                       int surfaceFlags) {
        if (format.isCompressed()) {
            return null;
        }
        if (!mCaps.validateSurfaceParams(width, height, format, 1, surfaceFlags)) {
            return null;
        }
        int levelCount = (surfaceFlags & SurfaceFlag_Mipmapped) != 0
                ? 32 - Integer.numberOfLeadingZeros(Math.max(width, height))
                : 1;
        handleDirtyContext();
        final Texture texture = onCreateTexture(width, height, format,
                levelCount,surfaceFlags);
        if (texture != null) {
            // we don't copy the backend format object, use identity rather than equals()
            assert texture.getBackendFormat() == format;
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
                                               int surfaceFlags);

    /**
     * Finds or creates a render target that promotes the texture to be renderable with the
     * sample count. If <code>sampleCount</code> is > 1 and the underlying API uses separate
     * MSAA render buffers then a MSAA render buffer is created that resolves to the texture.
     * If failed, the texture smart pointer will be destroyed, otherwise transfers to the
     * render target. If succeeded, the texture cannot be obtained from the resource cache
     * until render target is recycled.
     *
     * @param texture     the single sample color buffer
     * @param sampleCount the desired sample count of the render target
     * @return a managed, recyclable render target, or null if failed
     */
    @Nullable
    @SharedPtr
    @VisibleForTesting
    public final RenderTarget findOrCreateRenderTarget(@SharedPtr Texture texture,
                                                       int sampleCount) {
        assert sampleCount > 0;
        assert mCaps.validateSurfaceParams(
                texture.mWidth, texture.mHeight,
                texture.getBackendFormat(),
                sampleCount, texture.getFlags());
        sampleCount = mCaps.getRenderTargetSampleCount(sampleCount, texture.getBackendFormat());
        assert sampleCount > 0;
        handleDirtyContext();
        final RenderTarget renderTarget = onFindOrCreateRenderTarget(texture, sampleCount);
        if (renderTarget != null) {
            // we don't copy the backend format object, use identity rather than equals()
            assert renderTarget.getBackendFormat() == texture.getBackendFormat();
            return renderTarget;
        }
        texture.unref();
        return null;
    }

    /**
     * Overridden by backend-specific derived class to create objects.
     * <p>
     * Render target size and format support will have already been validated in base class
     * before onFindOrCreateRenderTarget is called.
     */
    @Nullable
    @SharedPtr
    protected abstract RenderTarget onFindOrCreateRenderTarget(@SharedPtr Texture texture,
                                                               int sampleCount);

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
    public RenderTarget wrapRenderableBackendTexture(BackendTexture texture,
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
    protected abstract RenderTarget onWrapRenderableBackendTexture(BackendTexture texture,
                                                                   int sampleCount,
                                                                   boolean ownership);

    public boolean writePixels(Texture texture,
                               int x, int y,
                               int width, int height,
                               int rowLength,
                               int alignment,
                               int dstColorType,
                               int srcColorType,
                               long pixels) {
        //TODO
        return false;
    }

    /**
     * Returns a {@link OpsRenderPass} which {@link OpsTask OpsTasks} send draw commands to instead of directly
     * to the {@link Server} object. The <code>bounds</code> rect is the content rect of the <code>renderTarget</code>.
     * If a 'stencil' is provided it will be the one bound to 'renderTarget'. If one is not
     * provided but 'renderTarget' has a stencil buffer then that is a signal that the
     * render target's stencil buffer should be ignored.
     *
     * @param renderTarget
     * @param useStencil
     * @param origin
     * @param bounds
     * @return
     */
    public abstract OpsRenderPass getOpsRenderPass(RenderTarget renderTarget,
                                                   boolean useStencil,
                                                   int origin,
                                                   Rect2i bounds,
                                                   int colorLoadOp, int colorStoreOp,
                                                   int stencilLoadOp, int stencilStoreOp,
                                                   float[] clearColor);

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

        public int textureCreates() {
            return mTextureCreates;
        }

        public void incTextureCreates() {
            mTextureCreates++;
        }

        public int textureUploads() {
            return mTextureUploads;
        }

        public void incTextureUploads() {
            mTextureUploads++;
        }

        public int transfersToTexture() {
            return mTransfersToTexture;
        }

        public void incTransfersToTexture() {
            mTransfersToTexture++;
        }

        public int transfersFromSurface() {
            return mTransfersFromSurface;
        }

        public void incTransfersFromSurface() {
            mTransfersFromSurface++;
        }

        public int stencilAttachmentCreates() {
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

        public int renderPasses() {
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
