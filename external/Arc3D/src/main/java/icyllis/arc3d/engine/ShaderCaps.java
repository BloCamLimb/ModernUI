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

package icyllis.arc3d.engine;

/**
 * Shader capabilities for our pipeline builder.
 * <p>
 * The default values assume GLSL 4.50 is supported and all equivalent extensions
 * that promoted to GLSL 4.50 core.
 */
public class ShaderCaps extends icyllis.arc3d.compiler.ShaderCaps {

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
    // Use a reduced set of rendering algorithms or less optimal effects in order to reduce the
    // number of unique shaders generated.
    public boolean mReducedShaderMode = false;

    /**
     * True if either 'textureQueryLod' (GLSL 4.00) or 'textureQueryLOD' (ARB/EXT) is supported.
     */
    public boolean mTextureQueryLod = true;
    /**
     * Non-null if {@link #mTextureQueryLod} is supported from an extension (ARB/EXT).
     */
    public String mTextureQueryLodExtension = null;

    /**
     * True if either OpenGL 4.2 or 'ARB_shading_language_420pack' is supported.
     */
    public boolean mShadingLanguage420Pack = true;
    /**
     * Non-null if {@link #mShadingLanguage420Pack} is supported from an extension.
     */
    public String mShadingLanguage420PackExtensionName = null;

    /**
     * True if either OpenGL 4.4 or 'ARB_enhanced_layouts' is supported.
     */
    public boolean mEnhancedLayouts = true;
    /**
     * Non-null if {@link #mEnhancedLayouts} is supported from an extension.
     */
    public String mEnhancedLayoutsExtensionName = null;

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
    public String mSecondaryOutputExtension = null;

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

    @Override
    public String toString() {
        return "ShaderCaps{" +
                "mDualSourceBlendingSupport=" + mDualSourceBlendingSupport +
                ", mPreferFlatInterpolation=" + mPreferFlatInterpolation +
                ", mVertexIDSupport=" + mVertexIDSupport +
                ", mInfinitySupport=" + mInfinitySupport +
                ", mNonConstantArrayIndexSupport=" + mNonConstantArrayIndexSupport +
                ", mBitManipulationSupport=" + mBitManipulationSupport +
                ", mReducedShaderMode=" + mReducedShaderMode +
                ", mTextureQueryLod=" + mTextureQueryLod +
                ", mTextureQueryLodExtension='" + mTextureQueryLodExtension + '\'' +
                ", mEnhancedLayouts=" + mEnhancedLayouts +
                ", mEnhancedLayoutsExtensionName='" + mEnhancedLayoutsExtensionName + '\'' +
                ", mRequiresLocalOutputColorForFBFetch=" + mRequiresLocalOutputColorForFBFetch +
                ", mMustObfuscateUniformColor=" + mMustObfuscateUniformColor +
                ", mMustWriteToFragColor=" + mMustWriteToFragColor +
                ", mColorSpaceMathNeedsFloat=" + mColorSpaceMathNeedsFloat +
                ", mAvoidDfDxForGradientsWhenPossible=" + mAvoidDfDxForGradientsWhenPossible +
                ", mSecondaryOutputExtension='" + mSecondaryOutputExtension + '\'' +
                ", mAdvBlendEqInteraction=" + mAdvBlendEqInteraction +
                ", mMaxFragmentSamplers=" + mMaxFragmentSamplers +
                ", mTargetApi=" + mTargetApi +
                ", mGLSLVersion=" + mGLSLVersion +
                ", mSPIRVVersion=" + mSPIRVVersion +
                '}';
    }
}
