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
public class Caps {

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

    private boolean mNPOTTextureTileSupport;
    private boolean mMipmapSupport;
    private boolean mReuseScratchTextures;
    private boolean mReuseScratchBuffers;
    private boolean mGpuTracingSupport;
    private boolean mOversizedStencilSupport;
    private boolean mTextureBarrierSupport;
    private boolean mSampleLocationsSupport;
    private boolean mMultisampleDisableSupport;
    private boolean mDrawInstancedSupport;
    private boolean mNativeDrawIndirectSupport;
    private boolean mUseClientSideIndirectBuffers;
    private boolean mConservativeRasterSupport;
    private boolean mWireframeSupport;
    private boolean mMSAAResolvesAutomatically;
    private boolean mUsePrimitiveRestart;
    private boolean mPreferClientSideDynamicBuffers;
    private boolean mPreferFullscreenClears;
    private boolean mTwoSidedStencilRefsAndMasksMustMatch;
    private boolean mMustClearUploadedBufferData;
    private boolean mShouldInitializeTextures;
    private boolean mSupportsAHardwareBufferImages;
    private boolean mHalfFloatVertexAttributeSupport;
    private boolean mClampToBorderSupport;
    private boolean mPerformPartialClearsAsDraws;
    private boolean mPerformColorClearsAsDraws;
    private boolean mAvoidLargeIndexBufferDraws;
    private boolean mPerformStencilClearsAsDraws;
    private boolean mTransferFromBufferToTextureSupport;
    private boolean mTransferFromSurfaceToBufferSupport;
    private boolean mWritePixelsRowBytesSupport;
    private boolean mReadPixelsRowBytesSupport;
    private boolean mShouldCollapseSrcOverToSrcWhenAble;
    private boolean mMustSyncGpuDuringAbandon;

    // Driver workaround
    private boolean mDriverDisableMSAAClipAtlas;
    private boolean mDisableTessellationPathRenderer;
    private boolean mAvoidStencilBuffers;
    private boolean mAvoidWritePixelsFastPath;
    private boolean mRequiresManualFBBarrierAfterTessellatedStencilDraw;
    private boolean mNativeDrawIndexedIndirectIsBroken;
    private boolean mAvoidReorderingRenderTasks;

    // ANGLE performance workaround
    private boolean mPreferVRAMUseOverFlushes;

    private boolean mFenceSyncSupport;
    private boolean mSemaphoreSupport;

    // Requires fence sync support in GL.
    private boolean mCrossContextTextureSupport;

    // Not (yet) implemented in VK backend.
    private boolean mDynamicStateArrayGeometryProcessorTextureSupport;

    private int mBlendEquationSupport;
    private int mAdvBlendEqDisableFlags;

    private int mMapBufferFlags;
    private int mBufferMapThreshold;

    private int mMaxRenderTargetSize;
    private int mMaxPreferredRenderTargetSize;
    private int mMaxVertexAttributes;
    private int mMaxTextureSize;
    private int mMaxWindowRectangles;
    private int mInternalMultisampleCount;
    private int mMaxPushConstantsSize;


    public Caps(ContextOptions options) {
    }
}
