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

import static icyllis.arcui.engine.EngineTypes.*;

public abstract class VaryingHandler {

    public static final int
            Interpolation_Interpolated = 0,
            Interpolation_CanBeFlat = 1,  // Use "flat" if it will be faster.
            Interpolation_MustBeFlat = 2; // Use "flat" even if it is known to be slow.

    protected static class VaryingInfo {

        public byte mType;
        public boolean mIsFlat;
        public String mVsOut;
        public int mVisibility;

        public VaryingInfo() {
        }
    }

    protected final ArrayList<VaryingInfo> mVaryings = new ArrayList<>();

    protected final ArrayList<ShaderVar> mVertexInputs = new ArrayList<>();
    protected final ArrayList<ShaderVar> mVertexOutputs = new ArrayList<>();
    protected final ArrayList<ShaderVar> mFragInputs = new ArrayList<>();
    protected final ArrayList<ShaderVar> mFragOutputs = new ArrayList<>();

    protected final ProgramBuilder mProgramBuilder;

    private String mDefaultInterpolationModifier = "smooth";

    protected VaryingHandler(ProgramBuilder programBuilder) {
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
        mDefaultInterpolationModifier = "noperspective";
    }

    public final void addVarying(String name,
                                 Varying varying) {
        addVarying(name, varying, Interpolation_Interpolated);
    }

    /**
     * addVarying allows fine-grained control for setting up varyings between stages. Calling this
     * function will make sure all necessary decls are setup for the client. The client however is
     * responsible for setting up all shader code (e.g "vOut = vIn;") If you just need to take an
     * attribute and pass it through to an output value in a fragment shader, use
     * addPassThroughAttribute.
     */
    public final void addVarying(String name,
                                 Varying varying,
                                 int interpolation) {
        assert (varying.mType != SLType.Void);
        assert (SLType.isFloatType(varying.mType) || interpolation == Interpolation_MustBeFlat);
        var v = new VaryingInfo();

        v.mType = varying.mType;
        v.mIsFlat = useFlatInterpolation(interpolation, mProgramBuilder.shaderCaps());
        v.mVsOut = mProgramBuilder.nameVariable('f', name);
        v.mVisibility = 0;
        if (varying.isInVertexShader()) {
            varying.mVsOut = v.mVsOut;
            v.mVisibility |= Vertex_ShaderFlag;
        }
        if (varying.isInFragmentShader()) {
            varying.mFsIn = v.mVsOut;
            v.mVisibility |= Fragment_ShaderFlag;
        }
        mVaryings.add(v);
    }

    public final void addPassThroughAttribute(ShaderVar vsVar,
                                              String output) {
        addPassThroughAttribute(vsVar, output, Interpolation_Interpolated);
    }

    /**
     * The GP can use these calls to pass a vertex shader variable directly to 'output' in the
     * fragment shader. Though this adds code to vertex and fragment stages, 'output' is expected to
     * be defined in the fragment shader before the call is made.
     */
    public final void addPassThroughAttribute(ShaderVar vsVar,
                                              String output,
                                              int interpolation) {
        assert (vsVar.getType() != SLType.Void);
        assert (!output.isEmpty());
        Varying v = new Varying(vsVar.getType());
        addVarying(vsVar.getName(), v, interpolation);
        mProgramBuilder.mVS.codeAppendf("%s = %s;\n", v.vsOut(), vsVar.getName());
        mProgramBuilder.mFS.codeAppendf("%s = %s;\n", output, v.fsIn());
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

    public final void emitAttributes(GeometryProcessor gp) {
        for (var attr : gp.vertexAttributes()) {
            addAttribute(attr.asShaderVar());
        }
        for (var attr : gp.instanceAttributes()) {
            addAttribute(attr.asShaderVar());
        }
    }

    private void addAttribute(ShaderVar var) {
        assert (var.getTypeModifier() == ShaderVar.TypeModifier_In);
        for (var attr : mVertexInputs) {
            // if attribute already added, don't add it again
            if (attr.getName().equals(var.getName())) {
                return;
            }
        }
        mVertexInputs.add(var);
    }

    // This should be called once all attributes and varyings have been added to the
    // VaryingHandler and before getting/adding any of the declarations to the shaders.
    public final void end() {
        for (var v : mVaryings) {
            String modifier = v.mIsFlat ? "flat" : mDefaultInterpolationModifier;
            if ((v.mVisibility & Vertex_ShaderFlag) != 0) {
                mVertexOutputs.add(new ShaderVar(v.mVsOut, v.mType, ShaderVar.TypeModifier_Out,
                        ShaderVar.NonArray, "", modifier));
            }
            if ((v.mVisibility & Fragment_ShaderFlag) != 0) {
                String fsIn = v.mVsOut;
                mFragInputs.add(new ShaderVar(fsIn, v.mType, ShaderVar.TypeModifier_In,
                        ShaderVar.NonArray, "", modifier));
            }
        }
        onEnd();
    }

    protected abstract void onEnd();

    protected static void assignLocations(ArrayList<ShaderVar> vars) {
        int locationIndex = 0;
        for (var var : vars) {
            String location = "location = " + locationIndex;
            var.addLayoutQualifier(location);

            int elementSize = SLType.locationSize(var.getType());
            assert (elementSize > 0);
            int numElements = var.isArray() ? var.getArrayCount() : 1;
            assert (numElements > 0);
            locationIndex += elementSize * numElements;
        }

        //TODO check max location capability
    }

    // called after end
    public final void getVertDecls(StringBuilder inputDecls, StringBuilder outputDecls) {
        appendDecls(mVertexInputs, inputDecls);
        appendDecls(mVertexOutputs, outputDecls);
    }

    // called after end
    public final void getFragDecls(StringBuilder inputDecls, StringBuilder outputDecls) {
        appendDecls(mFragInputs, inputDecls);
        appendDecls(mFragOutputs, outputDecls);
    }

    // helper function for get*Decls
    private static void appendDecls(ArrayList<ShaderVar> vars, StringBuilder out) {
        for (var var : vars) {
            var.appendDecl(out);
            out.append(";\n");
        }
    }
}
