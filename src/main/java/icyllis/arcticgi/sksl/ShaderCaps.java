/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.sksl;

public class ShaderCaps {

    /**
     * Indicates how GLSL must interact with advanced blend equations. The KHR extension requires
     * special layout qualifiers in the fragment shader.
     */
    public static final int
            NotSupported_AdvBlendEqInteraction = 0,     // No _blend_equation_advanced extension
            Automatic_AdvBlendEqInteraction = 1,        // No interaction required
            GeneralEnable_AdvBlendEqInteraction = 2;    // layout(blend_support_all_equations) out

    public boolean mShaderDerivativeSupport = true;
    /**
     * Indicates true 32-bit integer support, with unsigned types and bitwise operations
     */
    public boolean mIntegerSupport = true;
    public boolean mNonSquareMatrixSupport = true;
    /**
     * asinh(), acosh(), atanh()
     */
    public boolean mInverseHyperbolicSupport = true;
    public boolean mFBFetchSupport = false;
    public boolean mFBFetchNeedsCustomOutput = false;
    public boolean mUsesPrecisionModifiers = false;
    public boolean mFlatInterpolationSupport = true;
    public boolean mNoPerspectiveInterpolationSupport = true;
    public boolean mSampleMaskSupport = true;
    public boolean mExternalTextureSupport = false;
    public boolean mFloatIs32Bits = true;

    // Used by SkSL to know when to generate polyfills.
    public boolean mBuiltinFMASupport = true;
    public boolean mBuiltinDeterminantSupport = true;

    // Used for specific driver bug workarounds
    public boolean mCanUseMinAndAbsTogether = true;
    public boolean mCanUseFractForNegativeValues = true;
    public boolean mMustForceNegatedAtanParamToFloat = false;
    public boolean mMustForceNegatedLdexpParamToMultiply = false;
    // Returns whether a device incorrectly implements atan(y,x) as atan(y/x)
    public boolean mAtan2ImplementedAsAtanYOverX = false;
    // If this returns true some operation (could be a no op) must be called between floor and abs
    // to make sure the driver compiler doesn't inline them together which can cause a driver bug in
    // the shader.
    public boolean mMustDoOpBetweenFloorAndAbs = false;
    // The D3D shader compiler, when targeting PS 3.0 (ie within ANGLE) fails to compile certain
    // constructs. See detailed comments in GrGLCaps.cpp.
    public boolean mMustGuardDivisionEvenAfterExplicitZeroCheck = false;
    // If false, SkSL uses a workaround so that sk_FragCoord doesn't actually query gl_FragCoord
    public boolean mCanUseFragCoord = true;
    // If true, short ints can't represent every integer in the 16-bit two's complement range as
    // required by the spec. SkSL will always emit full ints.
    public boolean mIncompleteShortIntPrecision = false;
    // If true, then conditions in for loops need "&& true" to work around driver bugs.
    public boolean mAddAndTrueToLoopCondition = false;
    // If true, then expressions such as "x && y" or "x || y" are rewritten as ternary to work
    // around driver bugs.
    public boolean mUnfoldShortCircuitAsTernary = false;
    public boolean mEmulateAbsIntFunction = false;
    public boolean mRewriteDoWhileLoops = false;
    public boolean mRewriteSwitchStatements = false;
    public boolean mRemovePowWithConstantExponent = false;
    // The Android emulator claims samplerExternalOES is an unknown type if a default precision
    // statement is made for the type.
    public boolean mNoDefaultPrecisionForExternalSamplers = false;
    // ARM GPUs calculate `matrix * vector` in SPIR-V at full precision, even when the inputs are
    // RelaxedPrecision. Rewriting the multiply as a sum of vector*scalar fixes this.
    public boolean mRewriteMatrixVectorMultiply = false;
    // Rewrites matrix equality comparisons to avoid an Adreno driver bug.
    public boolean mRewriteMatrixComparisons = false;

    // This controls behavior of the SkSL compiler, not the code we generate. By default, SkSL pools
    // IR nodes per-program. To debug memory corruption, it can be helpful to disable that feature.
    public boolean mUseNodePools = true;

    public String mVersionDeclString = "";

    public String mShaderDerivativeExtensionString  = null;
    public String mExternalTextureExtensionString = null;
    public String mSecondExternalTextureExtensionString = null;
    public String mFBFetchColorName = null;

    public int mAdvBlendEqInteraction = NotSupported_AdvBlendEqInteraction;

    public ShaderCaps() {
    }

    public final boolean mustEnableAdvBlendEqs() {
        return mAdvBlendEqInteraction >= GeneralEnable_AdvBlendEqInteraction;
    }

    // XXX: we are in core profile
    public final boolean mustDeclareFragmentShaderOutput() {
        return true;
    }

    // Returns the string of an extension that must be enabled in the shader to support
    // derivatives. If nullptr is returned then no extension needs to be enabled. Before calling
    // this function, the caller should check that shaderDerivativeSupport exists.
    public final String shaderDerivativeExtensionString() {
        assert mShaderDerivativeSupport;
        return mShaderDerivativeExtensionString;
    }

    // This returns the name of an extension that must be enabled in the shader to support external
    // textures. In some cases, two extensions must be enabled - the second extension is returned
    // by secondExternalTextureExtensionString(). If that function returns nullptr, then only one
    // extension is required.
    public final String externalTextureExtensionString() {
        assert mExternalTextureSupport;
        return mExternalTextureExtensionString;
    }

    public final String secondExternalTextureExtensionString() {
        assert mExternalTextureSupport;
        return mSecondExternalTextureExtensionString;
    }

    public final boolean supportsDistanceFieldText() {
        return mShaderDerivativeSupport;
    }
}
