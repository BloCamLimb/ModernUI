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

import javax.annotation.concurrent.Immutable;

/**
 * This immutable object contains all information needed to build a pipeline.
 */
//TODO
@Immutable
public final class PipelineInfo {

    /**
     * Pipeline flags.
     */
    public static final int kNone_Flag = 0;
    /**
     * Cause every pixel to be rasterized that is touched by the triangle anywhere (not just at
     * pixel center). Additionally, if using MSAA, the sample mask will always have 100%
     * coverage.
     * NOTE: The primitive type must be a triangle type.
     */
    public static final int kConservativeRaster_Flag = 0x01;
    /**
     * Draws triangles as outlines.
     */
    public static final int kWireframe_Flag = 0x02;
    /**
     * Modifies the vertex shader so that vertices will be positioned at pixel centers.
     */
    public static final int kSnapToPixels_Flag = 0x04;
    /**
     * Scissor clip is applied.
     */
    public static final int kHasScissorClip_Flag = 0x08;
    /**
     * Stencil clip is applied.
     */
    public static final int kHasStencilClip_Flag = 0x10;
    /**
     * Render pass requires a barrier for advanced blending.
     */
    public static final int kRenderPassBlendBarrier_Flag = 0x20;

    private final BackendFormat mBackendFormat;
    private final int mSampleCount;
    private final int mOrigin;
    private final short mWriteSwizzle;
    private final GeometryProcessor mGeomProc;
    private final UserStencilSettings mUserStencilSettings;
    private final int mFlags;
    private boolean mNeedsStencil;
    private boolean mTargetHasVkResolveAttachmentWithInput;

    /**
     * Note that the fields of all input objects MUST be immutable after constructor is called.
     *
     * @param writeView           the main color render target to write, can NOT be null
     * @param geomProc            the geometry processor, can NOT be null
     * @param xferProc            the transfer processor, can NOT be null
     * @param colorFragProc       the paint's color fragment processors, can be null
     * @param coverageFragProc    the paint's coverage fragment processors, can be null
     * @param userStencilSettings the stencil settings for stencil clipping, can be null
     */
    public PipelineInfo(SurfaceProxyView writeView,
                        GeometryProcessor geomProc,
                        TransferProcessor xferProc,
                        FragmentProcessor colorFragProc,
                        FragmentProcessor coverageFragProc,
                        UserStencilSettings userStencilSettings,
                        int pipelineFlags) {
        assert (writeView != null);
        assert (geomProc != null);
        mBackendFormat = writeView.getProxy().getBackendFormat();
        mSampleCount = writeView.getProxy().getSampleCount();
        mOrigin = writeView.getOrigin();
        mWriteSwizzle = writeView.getSwizzle();
        mGeomProc = geomProc;
        mUserStencilSettings = userStencilSettings;
        mFlags = pipelineFlags;
    }

    public UserStencilSettings userStencilSettings() {
        return mUserStencilSettings;
    }

    public BackendFormat backendFormat() {
        return mBackendFormat;
    }

    /**
     * @see SurfaceOrigin
     */
    public int origin() {
        return mOrigin;
    }

    /**
     * @see Swizzle
     */
    public short writeSwizzle() {
        return mWriteSwizzle;
    }

    public int sampleCount() {
        return mSampleCount;
    }

    public GeometryProcessor geomProc() {
        return mGeomProc;
    }

    /**
     * @return see PrimitiveType
     */
    public byte primitiveType() {
        return mGeomProc.primitiveType();
    }

    public boolean hasScissorClip() {
        return (mFlags & kHasScissorClip_Flag) != 0;
    }

    public boolean hasStencilClip() {
        return (mFlags & kHasStencilClip_Flag) != 0;
    }

    public boolean isStencilEnabled() {
        return mUserStencilSettings != null || hasStencilClip();
    }

    public boolean needsBlendBarrier() {
        return (mFlags & kRenderPassBlendBarrier_Flag) != 0;
    }
}
