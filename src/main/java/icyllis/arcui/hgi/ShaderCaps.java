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

public class ShaderCaps {

    /**
     * Indicates how GLSL must interact with advanced blend equations. The KHR extension requires
     * special layout qualifiers in the fragment shader.
     */
    public static final int
            ADV_BLEND_EQ_INTERACTION_NOT_SUPPORTED = 0,     // No _blend_equation_advanced extension
            ADV_BLEND_EQ_INTERACTION_AUTOMATIC = 1,         // No interaction required
            ADV_BLEND_EQ_INTERACTION_GENERAL_ENABLE = 2;    // layout(blend_support_all_equations) out

    // Stupid stuff
    public final boolean mShaderDerivativeSupport = true;
    public final boolean mIntegerSupport = true;
    public final boolean mNonsquareMatrixSupport = true;
    public final boolean mInverseHyperbolicSupport = true;
    public final boolean mFlatInterpolationSupport = true;
    public final boolean mNoPerspectiveInterpolationSupport = true;
    public final boolean mSampleMaskSupport = true;

    public final boolean mBuiltinFMASupport = true;
    public final boolean mBuiltinDeterminantSupport = true;

    // Used for specific driver bug workarounds
    public final boolean mCanUseAnyFunctionInShader = true;
    public final boolean mCanUseMinAndAbsTogether = true;
    public final boolean mCanUseFractForNegativeValues = true;
    public final boolean mMustForceNegatedAtanParamToFloat = false;
    public final boolean mMustForceNegatedLdexpParamToMultiply = false;
    public final boolean mAtan2ImplementedAsAtanYOverX = false;
    public final boolean mMustDoOpBetweenFloorAndAbs = false;
    public final boolean mMustGuardDivisionEvenAfterExplicitZeroCheck = false;
    public final boolean mCanUseFragCoord = true;
    public final boolean mIncompleteShortIntPrecision = false;
    public final boolean mAddAndTrueToLoopCondition = false;
    public final boolean mUnfoldShortCircuitAsTernary = false;
    public final boolean mEmulateAbsIntFunction = false;
    public final boolean mRewriteDoWhileLoops = false;
    public final boolean mRewriteSwitchStatements = false;
    public final boolean mRemovePowWithConstantExponent = false;
    public final boolean mNoDefaultPrecisionForExternalSamplers = false;
    public final boolean mRewriteMatrixVectorMultiply = false;
    public final boolean mRewriteMatrixComparisons = false;

    public final boolean mPreferFlatInterpolation = true;
    public final boolean mVertexIDSupport = true;
    public final boolean mInfinitySupport = true;
    public final boolean mNonconstantArrayIndexSupport = true;
    public final boolean mBitManipulationSupport = true;
    public final boolean mHasLowFragmentPrecision = false;

    // Used for specific driver bug workarounds
    public final boolean mRequiresLocalOutputColorForFBFetch = false;
    public final boolean mMustObfuscateUniformColor = false;
    public final boolean mMustWriteToFragColor = false;
    public final boolean mColorSpaceMathNeedsFloat = false;
    public final boolean mCanUseFastMath = false;
    public final boolean mAvoidDfDxForGradientsWhenPossible = false;

    public String mVersionDeclString = "";

    public boolean mFBFetchSupport = false;
    public boolean mFBFetchNeedsCustomOutput = false;
    public String mFBFetchColorName = null;
    public String mFBFetchExtensionString = null;

    public int mAdvBlendEqInteraction = ADV_BLEND_EQ_INTERACTION_NOT_SUPPORTED;

    public boolean mFloatIs32Bits = true;
    public boolean mHalfIs32Bits = false;
    public boolean mReducedShaderMode = false;

    public boolean mDstReadInShaderSupport = false;
    public boolean mDualSourceBlendingSupport = false;

    public int mMaxFragmentSamplers = 0;
    public int mMaxTessellationSegments = 0;

    public void applyOptionsOverrides(ContextOptions options) {
        if (options.mReducedShaderVariations) {
            mReducedShaderMode = true;
        }
    }
}
