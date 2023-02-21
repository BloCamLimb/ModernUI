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

package icyllis.modernui.graphics.engine;

public class ShaderCaps {

    /**
     * Indicates how GLSL must interact with advanced blend equations. The KHR extension requires
     * special layout qualifiers in the fragment shader.
     */
    public static final int
            NotSupported_AdvBlendEqInteraction = 0,     // No _blend_equation_advanced extension
            Automatic_AdvBlendEqInteraction = 1,        // No interaction required
            GeneralEnable_AdvBlendEqInteraction = 2;    // layout(blend_support_all_equations) out

    public boolean mDualSourceBlendingSupport = false;
    public boolean mPreferFlatInterpolation = false;
    public boolean mVertexIDSupport = false;
    // isinf() is defined, and floating point infinities are handled according to IEEE standards.
    public boolean mInfinitySupport = false;
    // Returns true if `expr` in `myArray[expr]` can be any integer expression. If false, `expr`
    // must be a constant-index-expression as defined in the OpenGL ES2 specification, Appendix A.5.
    public boolean mNonConstantArrayIndexSupport = false;
    // frexp(), ldexp(), findMSB(), findLSB().
    public boolean mBitManipulationSupport = false;
    public boolean mHalfIs32Bits = false;
    public boolean mHasLowFragmentPrecision = false;
    // Use a reduced set of rendering algorithms or less optimal effects in order to reduce the
    // number of unique shaders generated.
    public boolean mReducedShaderMode = false;

    // Used for specific driver bug workarounds
    public boolean mRequiresLocalOutputColorForFBFetch = false;
    // Workaround for Mali GPU opacity bug with uniform colors.
    public boolean mMustObfuscateUniformColor = false;
    // On Nexus 6, the GL context can get lost if a shader does not write a value to gl_FragColor.
    public boolean mMustWriteToFragColor = false;
    public boolean mColorSpaceMathNeedsFloat = false;
    // When we have the option of using either dFdx or dfDy in a shader, this returns whether we
    // should avoid using dFdx. We have found some drivers have bugs or lower precision when using
    // dFdx.
    public boolean mAvoidDfDxForGradientsWhenPossible = false;

    // This contains the name of an extension that must be enabled in the shader, if such a thing is
    // required in order to use a secondary output in the shader. This returns a nullptr if no such
    // extension is required. However, the return value of this function does not say whether dual
    // source blending is supported.
    public String mSecondaryOutputExtensionString = null;

    public int mAdvBlendEqInteraction = NotSupported_AdvBlendEqInteraction;

    public int mMaxFragmentSamplers = 0;

    public ShaderCaps() {
    }

    public void applyOptionsOverrides(ContextOptions options) {
        if (options.mReducedShaderVariations) {
            mReducedShaderMode = true;
        }
    }

    public final boolean mustEnableAdvBlendEqs() {
        return mAdvBlendEqInteraction >= GeneralEnable_AdvBlendEqInteraction;
    }
}
