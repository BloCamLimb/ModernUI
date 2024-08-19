/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.granite.shading;

import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.engine.*;

import java.util.ArrayList;

import static icyllis.arc3d.engine.Engine.ShaderFlags;

/**
 * Builds shader stage inputs and outputs.
 */
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

    protected final ShaderCaps mShaderCaps;

    // the default interpolation qualifier is smooth (with perspective)
    private String mDefaultInterpolationModifier = "";

    public VaryingHandler(ShaderCaps shaderCaps) {
        mShaderCaps = shaderCaps;
    }

    /**
     * Notifies the varying handler that this shader will never emit geometry in perspective and
     * therefore does not require perspective-correct interpolation. When supported, this allows
     * varyings to use the "noperspective" keyword, which means the GPU can use cheaper math for
     * interpolation.
     */
    public final void setNoPerspective() {
        if (!mShaderCaps.mNoPerspectiveInterpolationSupport) {
            return;
        }
        mDefaultInterpolationModifier = "noperspective";
    }

    /**
     * Convenience for {@link #addVarying(String, byte, int)}
     * that uses smooth or noperspective interpolation.
     */
    public final void addVarying(String name,
                                 byte type) {
        addVarying(name, type, kSmooth_Interpolation);
    }

    /**
     * addVarying allows fine-grained control for setting up varyings between stages. Calling this
     * function will make sure all necessary decls are setup for the client. The client however is
     * responsible for setting up all shader code (e.g "vOut = vIn;") If you just need to take an
     * attribute and pass it through to an output value in a fragment shader, use
     * addPassThroughAttribute.
     */
    public final void addVarying(String name,
                                 byte type,
                                 int interpolation) {
        assert (type != SLDataType.kVoid);
        assert (SLDataType.isFloatType(type) || interpolation == kRequiredToBeFlat_Interpolation);
        var v = new VaryingInfo();

        v.mType = type;
        v.mIsFlat = useFlatInterpolation(interpolation, mShaderCaps);
        v.mVsOut = name;
        v.mVisibility = 0;
        v.mVisibility |= ShaderFlags.kVertex;
        v.mVisibility |= ShaderFlags.kFragment;
        mVaryings.add(v);
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
            if (mShaderCaps.mUseVaryingLocation) {
                // ARB_enhanced_layouts or GLSL 440
                layoutQualifier = "location = " + locationIndex;
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
        for (var var : mVertexOutputs) {
            var.appendDecl(outputDecls);
            outputDecls.append(";\n");
        }
    }

    // called after end
    public final void getFragDecls(StringBuilder inputDecls) {
        for (var var : mFragInputs) {
            var.appendDecl(inputDecls);
            inputDecls.append(";\n");
        }
    }
}
