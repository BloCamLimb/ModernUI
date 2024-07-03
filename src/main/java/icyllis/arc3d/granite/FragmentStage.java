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

import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.shading.UniformHandler;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Formatter;

/**
 * Represents a substage of a fragment shader.
 */
//TODO
@Immutable
public class FragmentStage extends Processor {

    /**
     * Builtin Code Snippet ID
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
            kHWImageShader_BuiltinStageID = 17,
            kColorSpaceXformColorFilter_BuiltinStageID = 18,
            kCompose_BuiltinStageID = 19;

    public static final int
            kLast_BuiltinStageID = kCompose_BuiltinStageID;
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
        super(99);
        mName = name;
        mRequirementFlags = requirementFlags;
        mStaticFunctionName = staticFunctionName;
        mRequiredFunctions = requiredFunctions;
        mUniforms = uniforms;
        mSamplers = samplers;
        mExpressionGenerator = expressionGenerator;
        mNumChildren = numChildren;
    }

    @Nonnull
    @Override
    public String name() {
        return mName;
    }

    public boolean needsLocalCoords() {
        return (mRequirementFlags & kLocalCoords_ReqFlag) != 0;
    }

    public boolean needsPriorStageOutput() {
        return (mRequirementFlags & kPriorStageOutput_ReqFlag) != 0;
    }

    public boolean needsBlenderDstColor() {
        return (mRequirementFlags & kBlenderDstColor_ReqFlag) != 0;
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
