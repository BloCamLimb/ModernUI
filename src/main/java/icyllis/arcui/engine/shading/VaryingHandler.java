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

package icyllis.arcui.engine.shading;

import icyllis.arcui.core.SLType;
import icyllis.arcui.engine.*;

import java.util.ArrayList;

public class VaryingHandler {

    public static final int
            Interpolation_Interpolated = 0,
            Interpolation_CanBeFlat = 1,  // Use "flat" if it will be faster.
            Interpolation_MustBeFlat = 2; // Use "flat" even if it is known to be slow.

    protected static class VaryingInfo {

        /**
         * @see icyllis.arcui.core.SLType
         */
        public byte mType;
        public boolean mIsFlat;
        public String mVsOut;
        /**
         * @see EngineTypes#ShaderFlag_Vertex
         */
        public int mVisibility;
    }

    protected final ArrayList<VaryingInfo> mVaryings = new ArrayList<>();

    protected final ArrayList<ShaderVar> mVertexInputs = new ArrayList<>();
    protected final ArrayList<ShaderVar> mVertexOutputs = new ArrayList<>();
    protected final ArrayList<ShaderVar> mFragInputs = new ArrayList<>();
    protected final ArrayList<ShaderVar> mFragOutputs = new ArrayList<>();

    protected final ProgramBuilder mProgramBuilder;

    String mDefaultInterpolationModifier;

    public VaryingHandler(ProgramBuilder programBuilder) {
        mProgramBuilder = programBuilder;
    }

    /**
     * Notifies the varying handler that this shader will never emit geometry in perspective and
     * therefore does not require perspective-correct interpolation. When supported, this allows
     * varyings to use the "noperspective" keyword, which means the GPU can use cheaper math for
     * interpolation.
     */
    public final void setNoPerspective() {
        final ShaderCaps caps = mProgramBuilder.shaderCaps();
        if (!caps.mNoPerspectiveInterpolationSupport) {
            return;
        }
    }

    public final void addVarying(String name, Varying varying) {
        addVarying(name, varying, Interpolation_Interpolated);
    }

    /**
     * addVarying allows fine-grained control for setting up varyings between stages. Calling this
     * function will make sure all necessary decls are setup for the client. The client however is
     * responsible for setting up all shader code (e.g "vOut = vIn;") If you just need to take an
     * attribute and pass it through to an output value in a fragment shader, use
     * addPassThroughAttribute.
     */
    public final void addVarying(String name, Varying varying, int interpolation) {
        assert SLType.isFloatType(varying.type()) || interpolation == Interpolation_MustBeFlat;
        VaryingInfo v = new VaryingInfo();

        assert varying.mType != SLType.VOID;
        v.mType = varying.mType;
        v.mIsFlat = useFlatInterpolation(interpolation, mProgramBuilder.shaderCaps());
        // since our target is fragment shader, so prefix it by 'f'
        v.mVsOut = mProgramBuilder.nameVariable('f', name);
        v.mVisibility = 0;
        if (varying.isInVertexShader()) {
            varying.mVsOut = v.mVsOut;
            v.mVisibility |= EngineTypes.ShaderFlag_Vertex;
        }
        if (varying.isInFragmentShader()) {
            varying.mFsIn = v.mVsOut;
            v.mVisibility |= EngineTypes.ShaderFlag_Fragment;
        }
    }

    public final void addPassThroughAttribute(ShaderVar vsVar, String output) {
        addPassThroughAttribute(vsVar, output, Interpolation_Interpolated);
    }

    /**
     * The GP can use these calls to pass a vertex shader variable directly to 'output' in the
     * fragment shader. Though this adds code to vertex and fragment stages, 'output' is expected to
     * be defined in the fragment shader before the call is made.
     */
    //TODO it might be nicer behavior to have a flag to declare output inside these calls
    public final void addPassThroughAttribute(ShaderVar vsVar, String output, int interpolation) {

    }

    private static boolean useFlatInterpolation(int interpolation, ShaderCaps shaderCaps) {
        switch (interpolation) {
            case Interpolation_Interpolated:
                return false;
            case Interpolation_CanBeFlat:
                assert !shaderCaps.mPreferFlatInterpolation || shaderCaps.mFlatInterpolationSupport;
                return shaderCaps.mPreferFlatInterpolation;
            case Interpolation_MustBeFlat:
                assert shaderCaps.mFlatInterpolationSupport;
                return true;
        }
        throw new IllegalArgumentException(String.valueOf(interpolation));
    }
}
