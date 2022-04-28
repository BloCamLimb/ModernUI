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
     * <p>
     * BASIC: Support to select the operator that combines src and dst terms.
     * <p>
     * ADVANCED: Additional fixed function support for specific SVG/PDF blend modes. Requires blend barriers.
     * <p>
     * ADVANCED_COHERENT: Advanced blend equation support that does not require blend barriers, and permits overlap.
     */
    public static final int
            BLEND_EQUATION_BASIC = 0,
            BLEND_EQUATION_ADVANCED = 1,
            BLEND_EQUATION_ADVANCED_COHERENT = 2;

    protected final ShaderCaps mShaderCaps = new ShaderCaps();

    // Stupid stuff
    protected final boolean mNPOTTextureTileSupport = true;
    protected final boolean mMipmapSupport = true;
    protected final boolean mReuseScratchTextures = true;
    protected final boolean mReuseScratchBuffers = true;
    protected final boolean mOversizeStencilSupport = true;
    protected final boolean mTextureBarrierSupport = true;
    protected final boolean mSampleLocationsSupport = true;
    protected final boolean mDrawInstancedSupport = true;
    protected final boolean mWireframeSupport = true;
    protected final boolean mMSAAResolvesAutomatically = false;
    protected final boolean mPreferDiscardableMSAAAttachment = false;
    protected final boolean mUsePrimitiveRestart = false;
    protected final boolean mPreferClientSideDynamicBuffers = false;
    protected final boolean mPreferFullscreenClears = false;
    protected final boolean mTwoSidedStencilRefsAndMasksMustMatch = false;
    protected final boolean mMustClearUploadedBufferData = false;
    protected final boolean mShouldInitializeTextures = false;
    protected final boolean mSupportsAHardwareBufferImages = false;
    protected final boolean mHalfFloatVertexAttributeSupport = true;
    protected final boolean mClampToBorderSupport = true;
    protected final boolean mPerformPartialClearsAsDraws = false;
    protected final boolean mPerformColorClearsAsDraws = false;
    protected final boolean mPerformStencilClearsAsDraws = false;
    protected final boolean mAvoidLargeIndexBufferDraws = false;
    protected final boolean mWritePixelsRowBytesSupport = true;
    protected final boolean mReadPixelsRowBytesSupport = true;
    protected final boolean mTransferFromBufferToTextureSupport = true;
    protected final boolean mTransferFromSurfaceToBufferSupport = true;
    protected final boolean mShouldCollapseSrcOverToSrcWhenAble = false;

    // Driver workaround
    protected final boolean mDriverDisableMSAAClipAtlas = false;
    protected final boolean mDisableTessellationPathRenderer = false;
    protected final boolean mAvoidStencilBuffers = false;
    protected final boolean mAvoidWritePixelsFastPath = false;
    protected final boolean mRequiresManualFBBarrierAfterTessellatedStencilDraw = false;
    protected final boolean mNativeDrawIndexedIndirectIsBroken = false;
    protected final boolean mAvoidReorderingRenderTasks = false;
    protected final boolean mAvoidDithering = false;

    // ANGLE performance workaround
    protected final boolean mPreferVRAMUseOverFlushes = true;

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

    protected int mBlendEquationSupport = BLEND_EQUATION_BASIC;

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

    /**
     * Can a texture be made with the BackendFormat, and then be bound and sampled in a shader.
     */
    public abstract boolean isTextureFormat(BackendFormat format);

    public boolean validateTextureParams(int width, int height, BackendFormat format, boolean mipmapped) {
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
