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

public class ProgramInfo {

    private int mNumSamples;
    private boolean mNeedsStencil;
    private final BackendFormat mBackendFormat;
    private final int mOrigin;
    private boolean mTargetHasVkResolveAttachmentWithInput;
    private int mTargetsNumSamples;
    private final Pipeline mPipeline;
    private final UserStencilSettings mUserStencilSettings;
    private final GeometryProcessor mGeomProc;
    private final byte mPrimitiveType;
    private final int mRenderPassXferBarriers;
    private final int mColorLoadOp;

    /**
     * @param primitiveType          see PrimitiveType
     * @param renderPassXferBarriers see XferBarrierFlags
     * @param colorLoadOp            see LoadOp
     */
    public ProgramInfo(Caps caps,
                       SurfaceProxyView targetView,
                       boolean usesMSAASurface,
                       Pipeline pipeline,
                       UserStencilSettings userStencilSettings,
                       GeometryProcessor geomProc,
                       byte primitiveType,
                       int renderPassXferBarriers,
                       int colorLoadOp) {
        assert (caps != null);
        assert (targetView != null);
        //assert (pipeline != null);
        //assert (userStencilSettings != null);
        assert (geomProc != null);
        assert (primitiveType >= 0 && primitiveType <= EngineTypes.PrimitiveType_Last);
        assert (colorLoadOp >= 0 && colorLoadOp <= EngineTypes.LoadOp_Last);
        mBackendFormat = targetView.getProxy().getBackendFormat();
        mOrigin = targetView.getOrigin();
        mPipeline = pipeline;
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

    public Pipeline pipeline() {
        return mPipeline;
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
