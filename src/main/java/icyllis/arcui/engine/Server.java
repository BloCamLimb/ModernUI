/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.engine;

import icyllis.arcui.sksl.ShaderCompiler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the application-controlled 3D API server, holding a reference
 * to {@link DirectContext}. It is responsible for creating / deleting 3D API objects,
 * controlling binding status, uploading and downloading data, transferring
 * 3D API commands, etc.
 */
public abstract class Server {

    // this server is managed by this context
    protected final DirectContext mContext;
    protected final Caps mCaps;
    protected final ShaderCompiler mCompiler;

    protected final Stats mStats = new Stats();

    private final List<FlushInfo.SubmittedCallback> mSubmittedCallbacks = new ArrayList<>();
    private int mDirtyFlags = ~0;

    protected Server(DirectContext context, Caps caps) {
        assert context != null && caps != null;
        mContext = context;
        mCaps = caps;
        mCompiler = new ShaderCompiler(caps.mShaderCaps);
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
    public final ShaderCompiler getShaderCompiler() {
        return mCompiler;
    }

    public abstract ThreadSafePipelineBuilder getPipelineBuilder();

    public final Stats getStats() {
        return mStats;
    }

    /**
     * The server object normally assumes that no outsider is setting state
     * within the underlying 3D API's context/device/whatever. This call informs
     * the server that the state was modified, and it shouldn't make assumptions
     * about the state.
     */
    public final void markDirty(int dirtyFlags) {
        mDirtyFlags |= dirtyFlags;
    }

    protected final void handleDirty() {
        if (mDirtyFlags != 0) {
            onDirty(mDirtyFlags);
            mDirtyFlags = 0;
        }
    }

    /**
     * Called when the 3D context state is unknown. Subclass should emit any
     * assumed 3D context state and dirty any state cache.
     */
    protected void onDirty(int dirtyFlags) {
    }

    /**
     * Creates a texture object and allocates its server memory. In other words, the
     * image data is dirty and needs to be uploaded later. If mipmapped, also allocates
     * <code>(31 - CLZ(max(width,height)))</code> mipmaps in addition to the base level.
     * NPoT (non-power-of-two) dimensions are always supported. Compressed format are
     * supported.
     *
     * @param width       the width of the texture to be created
     * @param height      the height of the texture to be created
     * @param format      the backend format for the texture
     * @param mipmapped   should the texture be allocated with mipmaps
     * @param budgeted    should the texture count against the resource cache budget
     * @param isProtected should the texture be created as protected
     * @return the referenced texture object if successful, otherwise nullptr
     */
    @Nullable
    public final Texture createTexture(int width, int height,
                                       BackendFormat format,
                                       boolean mipmapped,
                                       boolean budgeted,
                                       boolean isProtected) {
        if (format.isCompressed()) {
            return null;
        }
        if (!mCaps.validateTextureParams(width, height, format)) {
            return null;
        }
        return onCreateTexture(width, height, format,
                mipmapped ? Integer.SIZE - Integer.numberOfLeadingZeros(Math.max(width, height)) : 1,
                budgeted, isProtected);
    }

    /**
     * Overridden by backend-specific derived class to create objects.
     * <p>
     * Texture size and format support will have already been validated in base class
     * before onCreateTexture is called.
     */
    @Nullable
    protected abstract Texture onCreateTexture(int width, int height,
                                               BackendFormat format,
                                               int mipLevels,
                                               boolean budgeted,
                                               boolean isProtected);

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
    @SmartPtr
    public RenderTarget wrapRenderableBackendTexture(BackendTexture texture,
                                                     int sampleCount,
                                                     boolean ownership) {
        handleDirty();
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
    @SmartPtr
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
