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

package icyllis.arcui.hgi;

/**
 * Represents the capabilities of a Context.
 */
public abstract class Caps {

    /**
     * Indicates the capabilities of the fixed function blend unit.
     */
    public enum BlendEquationSupport {
        /**
         * Support to select the operator that combines src and dst terms.
         */
        BASIC,
        /**
         * Additional fixed function support for specific SVG/PDF blend modes. Requires blend barriers.
         */
        ADVANCED,
        /**
         * Advanced blend equation support that does not require blend barriers, and permits overlap.
         */
        ADVANCED_COHERENT
    }

    protected final ShaderCaps mShaderCaps = new ShaderCaps();

    // Stupid stuff
    protected final boolean mDrawInstancedSupport = true;
    protected final boolean mMustClearUploadedBufferData = false;
    protected final boolean mShouldInitializeTextures = false;
    protected final boolean mSupportsAHardwareBufferImages = false;
    protected final boolean mClampToBorderSupport = true;
    protected final boolean mPerformPartialClearsAsDraws = false;
    protected final boolean mPerformColorClearsAsDraws = false;
    protected final boolean mPerformStencilClearsAsDraws = false;
    protected final boolean mAvoidLargeIndexBufferDraws = false;
    protected final boolean mWritePixelsRowBytesSupport = true;
    protected final boolean mReadPixelsRowBytesSupport = true;
    protected final boolean mTransferFromBufferToTextureSupport = true;
    protected final boolean mTransferFromSurfaceToBufferSupport = true;

    // Driver workaround
    protected final boolean mDriverDisableMSAAClipAtlas = false;
    protected final boolean mDisableTessellationPathRenderer = false;
    protected final boolean mAvoidReorderingRenderTasks = false;
    protected final boolean mAvoidDithering = false;

    protected final boolean mFenceSyncSupport = true;
    protected final boolean mSemaphoreSupport = true;

    // Requires fence sync support in GL.
    protected final boolean mCrossContextTextureSupport = true;

    protected boolean mAnisoSupport = false;
    protected boolean mGpuTracingSupport = false;
    protected boolean mNativeDrawIndirectSupport = false;
    protected boolean mUseClientSideIndirectBuffers = false;
    protected boolean mConservativeRasterSupport = false;
    protected boolean mTransferPixelsToRowBytesSupport = false;
    protected boolean mMustSyncGpuDuringDiscard = true;

    // Not (yet) implemented in VK backend.
    protected boolean mDynamicStateArrayGeometryProcessorTextureSupport = false;

    protected BlendEquationSupport mBlendEquationSupport = BlendEquationSupport.BASIC;

    protected int mMapBufferFlags;

    protected int mMaxRenderTargetSize = 1;
    protected int mMaxPreferredRenderTargetSize = 1;
    protected int mMaxVertexAttributes = 0;
    protected int mMaxTextureSize = 1;
    protected int mMaxWindowRectangles = 0;
    protected int mInternalMultisampleCount = 0;
    protected int mMinPathVerbsForHwTessellation = 25;
    protected int mMinStrokeVerbsForHwTessellation = 50;
    protected int mMaxPushConstantsSize = 0;

    public Caps(ContextOptions options) {
    }

    public final ShaderCaps getShaderCaps() {
        return mShaderCaps;
    }

    /**
     * Non-power-of-two texture tile.
     */
    public final boolean getNPOTTextureTileSupport() {
        return true;
    }

    /**
     * To avoid as-yet-unnecessary complexity we don't allow any partial support of MIP Maps (e.g.
     * only for POT textures)
     */
    public final boolean getMipmapSupport() {
        return true;
    }

    /**
     * Anisotropic filtering (AF).
     */
    public final boolean getAnisoSupport() {
        return mAnisoSupport;
    }

    public final boolean getGpuTracingSupport() {
        return mGpuTracingSupport;
    }

    /**
     * Allows mixed size FBO attachments.
     */
    public final boolean getOversizeStencilSupport() {
        return true;
    }

    public final boolean getTextureBarrierSupport() {
        return true;
    }

    public final boolean getSampleLocationsSupport() {
        return true;
    }

    public final boolean getDrawInstancedSupport() {
        return true;
    }

    /**
     * Is there hardware support for indirect draws? (Ganesh always supports indirect draws as long
     * as it can polyfill them with instanced calls, but this cap tells us if they are supported
     * natively.)
     */
    public final boolean getNativeDrawIndirectSupport() {
        return mNativeDrawIndirectSupport;
    }

    public final boolean getUseClientSideIndirectBuffers() {
        return mUseClientSideIndirectBuffers;
    }

    public final boolean getConservativeRasterSupport() {
        return mConservativeRasterSupport;
    }

    public final boolean getWireframeSupport() {
        return true;
    }

    /**
     * This flag indicates that we never have to resolve MSAA. In practice, it means that we have
     * an MSAA-render-to-texture extension: Any render target we create internally will use the
     * extension, and any wrapped render target is the client's responsibility.
     */
    public final boolean getMSAAResolvesAutomatically() {
        return false;
    }

    /**
     * If true then when doing MSAA draws, we will prefer to discard the msaa attachment on load
     * and stores. The use of this feature for specific draws depends on the render target having a
     * resolve attachment, and if we need to load previous data the resolve attachment must be
     * usable as an input attachment/texture. Otherwise, we will just write out and store the msaa
     * attachment like normal.
     * <p>
     * This flag is similar to enabling gl render to texture for msaa rendering.
     */
    public final boolean getPreferDiscardableMSAAAttachment() {
        return false;
    }

    public final boolean getHalfFloatVertexAttributeSupport() {
        return true;
    }

    /**
     * Primitive restart functionality is core in ES 3.0, but using it will cause slowdowns on some
     * systems. This cap is only set if primitive restart will improve performance.
     */
    public final boolean getUsePrimitiveRestart() {
        return false;
    }

    public final boolean getPreferClientSideDynamicBuffers() {
        return false;
    }

    /**
     * On tilers, an initial fullscreen clear is an OPTIMIZATION. It allows the hardware to
     * initialize each tile with a constant value rather than loading each pixel from memory.
     */
    public final boolean getPreferFullscreenClears() {
        return false;
    }

    /**
     * Should we discard stencil values after a render pass? (Tilers get better performance if we
     * always load stencil buffers with a "clear" op, and then discard the content when finished.)
     */
    public final boolean getDiscardStencilValuesAfterRenderPass() {
        //TODO review
        return getPreferFullscreenClears();
    }

    /**
     * D3D does not allow the refs or masks to differ on a two-sided stencil draw.
     */
    public final boolean getTwoSidedStencilRefsAndMasksMustMatch() {
        return false;
    }

    public final boolean getPreferVRAMUseOverFlushes() {
        return true;
    }

    public final boolean getAvoidStencilBuffers() {
        return false;
    }

    public final boolean getAvoidWritePixelsFastPath() {
        return false;
    }

    public final boolean getRequiresManualFBBarrierAfterTessellatedStencilDraw() {
        return false;
    }

    public final boolean getNativeDrawIndexedIndirectIsBroken() {
        return false;
    }

    public final BlendEquationSupport getBlendEquationSupport() {
        return mBlendEquationSupport;
    }

    public final boolean getAdvancedBlendEquationSupport() {
        return mBlendEquationSupport != BlendEquationSupport.BASIC;
    }

    public final boolean getAdvancedCoherentBlendEquationSupport() {
        return mBlendEquationSupport == BlendEquationSupport.ADVANCED_COHERENT;
    }

    /**
     * On some GPUs it is a performance win to disable blending instead of doing src-over with a src
     * alpha equal to 1. To disable blending we collapse src-over to src and the backends will
     * handle the disabling of blending.
     */
    public final boolean getShouldCollapseSrcOverToSrcWhenAble() {
        return false;
    }

    /**
     * When discarding the DirectContext do we need to sync the GPU before we start discarding
     * resources.
     */
    public final boolean getMustSyncGpuDuringDiscard() {
        return mMustSyncGpuDuringDiscard;
    }

    public final boolean getReducedShaderMode() {
        return mShaderCaps.mReducedShaderMode;
    }

    /**
     * Scratch textures not being reused means that those scratch textures
     * that we upload to (i.e., don't have a render target) will not be
     * recycled in the texture cache. This is to prevent ghosting by drivers
     * (in particular for deferred architectures).
     */
    public final boolean getReuseScratchTextures() {
        return true;
    }

    public final boolean getReuseScratchBuffers() {
        return true;
    }

    /**
     * Maximum number of attribute values per vertex
     */
    public final int getMaxVertexAttributes() {
        return mMaxVertexAttributes;
    }

    public final int getMaxRenderTargetSize() {
        return mMaxRenderTargetSize;
    }

    /**
     * This is the largest render target size that can be used without incurring extra performance
     * cost. It is usually the max RT size, unless larger render targets are known to be slower.
     */
    public final int getMaxPreferredRenderTargetSize() {
        return mMaxPreferredRenderTargetSize;
    }

    public final int getMaxTextureSize() {
        return mMaxTextureSize;
    }

    public final int getMaxWindowRectangles() {
        return mMaxWindowRectangles;
    }

    /**
     * Hardware tessellation seems to have a fixed upfront cost. If there is a somewhat small number
     * of verbs, we seem to be faster emulating tessellation with instanced draws instead.
     */
    public final int getMinPathVerbsForHwTessellation() {
        return mMinPathVerbsForHwTessellation;
    }

    public final int getMinStrokeVerbsForHwTessellation() {
        return mMinStrokeVerbsForHwTessellation;
    }

    public final int getMaxPushConstantsSize() {
        return mMaxPushConstantsSize;
    }

    public final int getTransferBufferAlignment() {
        return 1;
    }

    /**
     * Can a texture be made with the BackendFormat, and then be bound and sampled in a shader.
     * It must be a color format, you cannot pass a stencil format here.
     * <p>
     * For OpenGL: Formats that deprecated in core profile are not supported; Compressed formats
     * from extensions are uncertain; Others are always supported.
     */
    public abstract boolean isColorFormat(BackendFormat format);

    /**
     * Returns the maximum supported sample count for a format. 0 means the format is not renderable
     * 1 means the format is renderable but doesn't support MSAA.
     */
    public abstract int getMaxRenderTargetSampleCount(BackendFormat format);

    /**
     * Returns the number of samples to use when performing draws to the given config with internal
     * MSAA. If 0, we should not attempt to use internal multisampling.
     */
    public final int getInternalMultisampleCount(BackendFormat format) {
        return Math.min(mInternalMultisampleCount, getMaxRenderTargetSampleCount(format));
    }

    public abstract boolean isRenderFormat(BackendFormat format, int sampleCount, int colorType);

    public abstract boolean isRenderFormat(BackendFormat format, int sampleCount);

    /**
     * Find a sample count greater than or equal to the requested count which is supported for a
     * render target of the given format or 0 if no such sample count is supported. If the requested
     * sample count is 1 then 1 will be returned if non-MSAA rendering is supported, otherwise 0.
     * For historical reasons requestedCount==0 is handled identically to requestedCount==1.
     */
    public abstract int getRenderTargetSampleCount(BackendFormat format, int sampleCount);

    /**
     * If a texture can be created with these params.
     */
    public final boolean validateTextureParams(int width, int height, BackendFormat format) {
        if (width < 1 || height < 1) {
            return false;
        }
        final int maxSize = getMaxTextureSize();
        if (width > maxSize || height > maxSize) {
            return false;
        }
        if (format.getTextureType() != Types.TEXTURE_TYPE_NONE) {
            return isColorFormat(format);
        }
        return true;
    }

    protected final void finishInitialization(ContextOptions options) {
        if (!mNativeDrawIndirectSupport) {
            // We will implement indirect draws with a polyfill, so the commands need to reside in CPU
            // memory.
            mUseClientSideIndirectBuffers = true;
        }

        mShaderCaps.applyOptionsOverrides(options);
        onApplyOptionsOverrides(options);

        mMaxWindowRectangles = Math.min(8, mMaxWindowRectangles);
        mInternalMultisampleCount = options.mInternalMultisampleCount;

        // Our render targets are always created with textures as the color attachment, hence this min:
        mMaxRenderTargetSize = Math.min(mMaxRenderTargetSize, mMaxTextureSize);
        mMaxPreferredRenderTargetSize = Math.min(mMaxPreferredRenderTargetSize, mMaxRenderTargetSize);
    }

    protected void onApplyOptionsOverrides(ContextOptions options) {
    }
}
