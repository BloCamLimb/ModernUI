/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.engine;

import javax.annotation.concurrent.Immutable;

/**
 * This immutable object contains all information needed to build a pipeline.
 */
//TODO
@Immutable
public class ProgramInfo {

    // Pipeline options that the caller may enable.
    public static final byte InputFlag_None = 0;
    /**
     * Cause every pixel to be rasterized that is touched by the triangle anywhere (not just at
     * pixel center). Additionally, if using MSAA, the sample mask will always have 100%
     * coverage.
     * NOTE: The primitive type must be a triangle type.
     */
    public static final byte InputFlag_ConservativeRaster = 0x01;
    /**
     * Draws triangles as outlines.
     */
    public static final byte InputFlag_Wireframe = 0x02;
    /**
     * Modifies the vertex shader so that vertices will be positioned at pixel centers.
     */
    public static final byte InputFlag_SnapVerticesToPixelCenters = 0x04;
    /**
     * Stencil clip is applied.
     */
    public static final byte InputFlag_HasStencilClip = 0x08;

    private int mNumSamples;
    private boolean mNeedsStencil;
    private final BackendFormat mBackendFormat;
    private final int mOrigin;
    private boolean mTargetHasVkResolveAttachmentWithInput;
    private int mTargetsNumSamples;
    private final UserStencilSettings mUserStencilSettings;
    private final GeometryProcessor mGeomProc;
    private final byte mPrimitiveType;
    private final int mRenderPassXferBarriers;
    private final int mColorLoadOp;
    private final short mWriteSwizzle;

    /**
     * Note that the fields of all input objects MUST be immutable after constructor is called.
     *
     * @param writeView              the main color render target to write, can NOT be null
     * @param geomProc               the geometry processor, can NOT be null
     * @param xferProc               the transfer processor, can NOT be null
     * @param colorFP                the paint's color fragment processors, can be null
     * @param coverageFP             the paint's coverage fragment processors, can be null
     * @param userStencilSettings    the stencil settings for stencil clipping, can be null
     * @param primitiveType          see PrimitiveType, triangles at most cases
     * @param renderPassXferBarriers see XferBarrierFlags
     * @param colorLoadOp            see LoadOp
     * @param inputFlags             additional flags, see this class
     */
    public ProgramInfo(SurfaceProxyView writeView,
                       GeometryProcessor geomProc,
                       TransferProcessor xferProc,
                       FragmentProcessor colorFP,
                       FragmentProcessor coverageFP,
                       UserStencilSettings userStencilSettings,
                       byte primitiveType,
                       int renderPassXferBarriers,
                       int colorLoadOp,
                       byte inputFlags) {
        assert (writeView != null);
        assert (geomProc != null);
        assert (primitiveType >= 0 && primitiveType <= Engine.PrimitiveType_Last);
        assert (colorLoadOp >= 0 && colorLoadOp <= Engine.LoadOp_Last);
        mBackendFormat = writeView.getProxy().getBackendFormat();
        mOrigin = writeView.getOrigin();
        mWriteSwizzle = writeView.getSwizzle();
        mUserStencilSettings = userStencilSettings;
        mGeomProc = geomProc;
        mPrimitiveType = primitiveType;
        mRenderPassXferBarriers = renderPassXferBarriers;
        mColorLoadOp = colorLoadOp;
    }

    public UserStencilSettings userStencilSettings() {
        return mUserStencilSettings;
    }

    public BackendFormat backendFormat() {
        return mBackendFormat;
    }

    /**
     * @return see SurfaceOrigin
     */
    public int origin() {
        return mOrigin;
    }

    public GeometryProcessor geomProc() {
        return mGeomProc;
    }

    /**
     * @return see PrimitiveType
     */
    public byte primitiveType() {
        return mPrimitiveType;
    }

    /**
     * @return XferBarrierFlags
     */
    public int renderPassBarriers() {
        return mRenderPassXferBarriers;
    }

    /**
     * @return see LoadOp
     */
    public int colorLoadOp() {
        return mColorLoadOp;
    }
}
