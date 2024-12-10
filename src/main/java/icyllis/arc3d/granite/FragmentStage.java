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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.BlendMode;
import icyllis.arc3d.engine.Engine;
import icyllis.arc3d.engine.ShaderVar;
import icyllis.arc3d.granite.shading.UniformHandler;
import org.jspecify.annotations.NonNull;

import javax.annotation.concurrent.Immutable;
import java.util.Formatter;

/**
 * Represents a substage of a fragment shader, providing custom shader code to the
 * Arc3D shading pipeline. Managed by {@link ShaderCodeSource}.
 */
//TODO
@Immutable
public class FragmentStage {

    /**
     * Builtin Code Snippet ID, most are from Skia Graphite.
     * Do not use these ID outside Granite Renderer.
     */
    public static final int
            kError_BuiltinStageID = 0,
            kPassthrough_BuiltinStageID = 1,
            kSolidColorShader_BuiltinStageID = 2,
            kRGBOpaquePaintColor_BuiltinStageID = 3,
            kAlphaOnlyPaintColor_BuiltinStageID = 4,
            kLinearGradientShader4_BuiltinStageID = 5,
            kLinearGradientShader8_BuiltinStageID = 6,
            kRadialGradientShader4_BuiltinStageID = 7,
            kRadialGradientShader8_BuiltinStageID = 8,
            kAngularGradientShader4_BuiltinStageID = 9,
            kAngularGradientShader8_BuiltinStageID = 10,
            kLocalMatrixShader_BuiltinStageID = 16,
            kImageShader_BuiltinStageID = 17,
            kCubicImageShader_BuiltinStageID = 18,
            kHWImageShader_BuiltinStageID = 19,
            kDitherShader_BuiltinStageID = 20,
            kColorSpaceXformColorFilter_BuiltinStageID = 21,
            kBlend_BuiltinStageID = 22,
            kBlendModeBlender_BuiltinStageID = 23,
            kPorterDuffBlender_BuiltinStageID = 24,
            kPrimitiveColor_BuiltinStageID = 25,
            kCompose_BuiltinStageID = 26;
    // Fixed blend modes hard code a specific blend function into the shader tree. This can be
    // valuable when an internal effect is known to always do a certain blend and we want to
    // benefit from inlining constants. It is also important for being able to convert the final
    // blend of the SkPaint into fixed function HW blending, where each HW blend is part of the
    // pipeline key, so using a known blend mode ID ensures the PaintParamsKey are also different.
    //
    // Lastly, for advanced blend modes that require complex shader calculations, we assume they
    // are used rarely and with intent (i.e. unlikely to share a common shader tree with another
    // advanced blend if we were to add branching). This keeps the amount of reachable code that
    // must be compiled for a given pipeline with advanced blends to a minimum.
    //
    // NOTE: Pipeline code generation depends on the fixed-function code IDs being contiguous and be
    // defined last in the enum. They are ordered to match BlendMode such that:
    //     (id - kFirstFixedBlend) == BlendMode).
    public static final int
            kFirstFixedBlend_BuiltinStageID = 27;
    // this is not compile-time constant
    public static final int
            kLastFixedBlend_BuiltinStageID = kFirstFixedBlend_BuiltinStageID + BlendMode.COUNT - 1;

    public static final int
            kLast_BuiltinStageID = kLastFixedBlend_BuiltinStageID;
    public static final int
            kBuiltinStageIDCount = kLast_BuiltinStageID + 1;

    /**
     * Requirement flags.
     */
    public static final int
            kNone_ReqFlag = 0x0,
            kLocalCoords_ReqFlag = 0x1,     // Geometry local coordinates
            kPriorStageOutput_ReqFlag = 0x2, // AKA the "input" color, or the "src" argument for a blender
            kBlenderDstColor_ReqFlag = 0x4; // The "dst" argument for a blender
    public static final int
            kPrimitiveColor_ReqFlag = 0x10;

    public record Uniform(byte type, String name, short arraySize) {

        public Uniform(byte type, String name) {
            this(type, name, (short) ShaderVar.kNonArray);
        }

        public Uniform(byte type, String name, int arraySize) {
            this(type, name, (short) arraySize);
        }
    }

    public record Sampler(byte type, String name) {
    }

    public static final String[] NO_FUNCTIONS = new String[0];
    public static final Uniform[] NO_UNIFORMS = new Uniform[0];
    public static final Sampler[] NO_SAMPLERS = new Sampler[0];

    /**
     * Emit assignment expression statement.
     */
    @FunctionalInterface
    public interface GenerateExpression {

        void generate(FragmentNode node,
                      String localCoords,
                      String priorStageOutput,
                      String blenderDstColor,
                      String output,
                      Formatter code);
    }

    public final String mName;
    public final int mRequirementFlags;
    public final String mStaticFunctionName;
    // will use String's reference identity
    public final String[] mRequiredFunctions;
    public final Uniform[] mUniforms;
    public final Sampler[] mSamplers;
    public final GenerateExpression mExpressionGenerator;
    public final int mNumChildren;

    public FragmentStage(String name, int requirementFlags,
                         String staticFunctionName,
                         String[] requiredFunctions, // will use String's reference identity
                         Uniform[] uniforms, Sampler[] samplers,
                         GenerateExpression expressionGenerator,
                         int numChildren) {
        mName = name;
        mRequirementFlags = requirementFlags;
        mStaticFunctionName = staticFunctionName;
        mRequiredFunctions = requiredFunctions;
        mUniforms = uniforms;
        mSamplers = samplers;
        mExpressionGenerator = expressionGenerator;
        mNumChildren = numChildren;
    }

    @NonNull
    public String name() {
        return mName;
    }

    public void generateUniforms(UniformHandler uniformHandler, int stageIndex) {
        for (var uniform : mUniforms) {
            uniformHandler.addUniformArray(
                    Engine.ShaderFlags.kFragment,
                    uniform.type,
                    uniform.name,
                    uniform.arraySize,
                    stageIndex
            );
        }
        for (var sampler : mSamplers) {
            uniformHandler.addSampler(
                    sampler.type,
                    sampler.name,
                    stageIndex
            );
        }
    }
}
