/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine.shading;

import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.engine.*;

import java.util.ArrayList;

import static icyllis.arc3d.engine.Engine.ShaderFlags;

public class VaryingHandler {

    public static final int
            kSmooth_Interpolation = 0,
            kCanBeFlat_Interpolation = 1,  // Use "flat" if it will be faster.
            kRequiredToBeFlat_Interpolation = 2; // Use "flat" even if it is known to be slow.

    protected static class VaryingInfo {

        public byte mType;
        public boolean mIsFlat;
        public String mVsOut;
        public int mVisibility;

        public VaryingInfo() {
        }
    }

    protected final ArrayList<VaryingInfo> mVaryings = new ArrayList<>();

    protected final ArrayList<ShaderVar> mVertexOutputs = new ArrayList<>();
    protected final ArrayList<ShaderVar> mFragInputs = new ArrayList<>();

    protected final PipelineBuilder mPipelineBuilder;

    // the default interpolation qualifier is smooth (with perspective)
    private String mDefaultInterpolationModifier = "";

    public VaryingHandler(PipelineBuilder pipelineBuilder) {
        mPipelineBuilder = pipelineBuilder;
    }

    /**
     * Notifies the varying handler that this shader will never emit geometry in perspective and
     * therefore does not require perspective-correct interpolation. When supported, this allows
     * varyings to use the "noperspective" keyword, which means the GPU can use cheaper math for
     * interpolation.
     */
    public final void setNoPerspective() {
        mDefaultInterpolationModifier = "noperspective";
    }

    /**
     * Convenience for {@link #addVarying(String, Varying, int)}
     * that uses smooth or noperspective interpolation.
     */
    public final void addVarying(String name,
                                 Varying varying) {
        addVarying(name, varying, kSmooth_Interpolation);
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
        assert (varying.mType != SLDataType.kVoid);
        assert (SLDataType.isFloatType(varying.mType) || interpolation == kRequiredToBeFlat_Interpolation);
        var v = new VaryingInfo();

        v.mType = varying.mType;
        v.mIsFlat = useFlatInterpolation(interpolation, mPipelineBuilder.shaderCaps());
        v.mVsOut = mPipelineBuilder.nameVariable('f', name);
        v.mVisibility = 0;
        if (varying.isInVertexShader()) {
            varying.mVsOut = v.mVsOut;
            v.mVisibility |= ShaderFlags.kVertex;
        }
        if (varying.isInFragmentShader()) {
            varying.mFsIn = v.mVsOut;
            v.mVisibility |= ShaderFlags.kFragment;
        }
        mVaryings.add(v);
    }

    /**
     * Convenience for {@link #addPassThroughAttribute(GeometryProcessor.Attribute, String, int)}
     * that uses smooth or noperspective interpolation.
     */
    public final void addPassThroughAttribute(GeometryProcessor.Attribute attr,
                                              String output) {
        addPassThroughAttribute(attr, output, kSmooth_Interpolation);
    }

    /**
     * The GP can use these calls to pass a vertex shader variable directly to 'output' in the
     * fragment shader. Though this adds code to vertex and fragment stages, 'output' is expected to
     * be defined in the fragment shader before the call is made.
     */
    public final void addPassThroughAttribute(GeometryProcessor.Attribute attr,
                                              String output,
                                              int interpolation) {
        assert (attr.dstType() != SLDataType.kVoid);
        assert (!output.isEmpty());
        Varying v = new Varying(attr.dstType());
        addVarying(attr.name(), v, interpolation);
        mPipelineBuilder.mVS.codeAppendf("%s = %s;\n", v.vsOut(), attr.name());
        mPipelineBuilder.mFS.codeAppendf("%s = %s;\n", output, v.fsIn());
    }

    private static boolean useFlatInterpolation(int interpolation, ShaderCaps shaderCaps) {
        return switch (interpolation) {
            case kSmooth_Interpolation -> false;
            case kCanBeFlat_Interpolation -> shaderCaps.mPreferFlatInterpolation;
            case kRequiredToBeFlat_Interpolation -> true;
            default -> throw new AssertionError(interpolation);
        };
    }

    // This should be called once all attributes and varyings have been added to the
    // VaryingHandler and before getting/adding any of the declarations to the shaders.
    public final void finish() {
        int locationIndex = 0;
        for (var v : mVaryings) {
            String layoutQualifier;
            if (mPipelineBuilder.shaderCaps().mEnhancedLayouts) {
                // ARB_enhanced_layouts or GLSL 440
                layoutQualifier = "location = " + locationIndex;
                String extensionName = mPipelineBuilder.shaderCaps().mEnhancedLayoutsExtensionName;
                mPipelineBuilder.mVS.addExtension(extensionName);
                mPipelineBuilder.mFS.addExtension(extensionName);
            } else {
                layoutQualifier = "";
            }
            String modifier = v.mIsFlat ? "flat" : mDefaultInterpolationModifier;
            if ((v.mVisibility & ShaderFlags.kVertex) != 0) {
                mVertexOutputs.add(new ShaderVar(v.mVsOut, v.mType, ShaderVar.kOut_TypeModifier,
                        ShaderVar.kNonArray, layoutQualifier, modifier));
            }
            if ((v.mVisibility & ShaderFlags.kFragment) != 0) {
                String fsIn = v.mVsOut;
                mFragInputs.add(new ShaderVar(fsIn, v.mType, ShaderVar.kIn_TypeModifier,
                        ShaderVar.kNonArray, layoutQualifier, modifier));
            }
            int locations = SLDataType.locations(v.mType);
            assert (locations > 0);
            locationIndex += locations;
        }
        onFinish();
    }

    protected void onFinish() {
    }

    // called after end
    public final void getVertDecls(StringBuilder outputDecls) {
        mPipelineBuilder.appendDecls(mVertexOutputs, outputDecls);
    }

    // called after end
    public final void getFragDecls(StringBuilder inputDecls) {
        mPipelineBuilder.appendDecls(mFragInputs, inputDecls);
    }
}
