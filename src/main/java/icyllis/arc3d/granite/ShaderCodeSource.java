/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.granite.shading.UniformHandler;

import java.util.Formatter;
import java.util.StringJoiner;

import static icyllis.arc3d.granite.FragmentStage.*;

/**
 * Manage all the fragment shader code snippets, used by Granite renderer.
 */
public class ShaderCodeSource {

    private static final Uniform[] PAINT_COLOR_UNIFORMS = {
            new Uniform(SLDataType.kFloat4, UniformHandler.PAINT_COLOR_NAME)
    };

    private final FragmentStage[] mBuiltinCodeSnippets =
            new FragmentStage[kBuiltinStageIDCount];

    {
        mBuiltinCodeSnippets[kError_BuiltinStageID] = new FragmentStage(
                "Error",
                kLocalCoords_ReqFlag,
                "arc_error",
                """
                        vec4 arc_error(vec2 localCoords) {
                            return mix(vec4(1.0), vec4(0.0,1.0,0.0,1.0),
                                   bool((int(localCoords.x) >> 3 ^ int(localCoords.y) >> 3) & 1));
                        }""", // 8x8 lime and white checkerboard
                NO_UNIFORMS,
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kPassthrough_BuiltinStageID] = new FragmentStage(
                "Passthrough",
                kPriorStageOutput_ReqFlag,
                "arc_passthrough",
                """
                        vec4 arc_passthrough(vec4 priorStageOutput) {
                            return priorStageOutput;
                        }
                        """,
                NO_UNIFORMS,
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kSolidColorShader_BuiltinStageID] = new FragmentStage(
                "SolidColor",
                kNone_ReqFlag,
                "arc_solid_color",
                """
                        vec4 arc_solid_color(vec4 color) {
                            return color;
                        }
                        """,
                new Uniform[]{
                        new Uniform(
                                SLDataType.kFloat4,
                                "u_Color"
                        )
                },
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kRGBOpaquePaintColor_BuiltinStageID] = new FragmentStage(
                "RGBOpaquePaintColor",
                kNone_ReqFlag,
                "arc_rgb_opaque",
                """
                        vec4 arc_rgb_opaque(vec4 paintColor) {
                            return vec4(paintColor.rgb, 1.0);
                        }
                        """,
                PAINT_COLOR_UNIFORMS,
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kAlphaOnlyPaintColor_BuiltinStageID] = new FragmentStage(
                "AlphaOnlyPaintColor",
                kNone_ReqFlag,
                "arc_alpha_only",
                """
                        vec4 arc_alpha_only(vec4 paintColor) {
                            return vec4(0.0, 0.0, 0.0, paintColor.a);
                        }
                        """,
                PAINT_COLOR_UNIFORMS,
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kLocalMatrixShader_BuiltinStageID] = new FragmentStage(
                "LocalMatrixShader",
                kLocalCoords_ReqFlag | kPriorStageOutput_ReqFlag,
                "LocalMatrix",
                "",
                new Uniform[]{
                        new Uniform(
                                SLDataType.kFloat4x4,
                                "u_LocalMatrix"
                        )
                },
                NO_SAMPLERS,
                (node, localCoords, priorStageOutput, blenderDstColor, output, code) -> {
                    assert node.codeID() == kLocalMatrixShader_BuiltinStageID;
                    assert node.numChildren() == 1;

                    String newLocalCoordsName = getMangledName("newLocalCoords", node.stageIndex());
                    String localMatrixName = getMangledName(node.stage().mUniforms[0].name(), node.stageIndex());
                    code.format("""
                                    {
                                        vec2 %s = (%s * vec4(%s, 0, 1)).xy;
                                    """,
                            newLocalCoordsName,
                            localMatrixName,
                            localCoords);
                    var childNode = node.childAt(0);
                    childNode.stage().mExpressionGenerator.generate(
                            childNode,
                            newLocalCoordsName,
                            priorStageOutput,
                            "vec4(1)", // unused blender dst color
                            output,
                            code
                    );
                    code.format("\n}");
                },
                1
        );
        mBuiltinCodeSnippets[kHWImageShader_BuiltinStageID] = new FragmentStage(
                "HardwareImageShader",
                kLocalCoords_ReqFlag,
                "arc_hw_image_shader",
                """
                        vec4 arc_hw_image_shader(vec2 localCoords,
                                                 vec2 invImageSize,
                                                 sampler2D s) {
                            vec4 samp = texture(s, localCoords * invImageSize);
                            samp.rgb *= samp.a;
                            return samp;
                        }
                        """,
                new Uniform[]{
                        new Uniform(
                                SLDataType.kFloat2,
                                "u_InvImageSize")
                },
                new Sampler[]{
                        new Sampler(
                                SLDataType.kSampler2D,
                                "u_Sampler")
                },
                ShaderCodeSource::generateDefaultExpression,
                0
        );
    }

    public FragmentStage findStage(int stageId) {
        if (stageId < 0) {
            return null;
        }

        if (stageId < kBuiltinStageIDCount) {
            return mBuiltinCodeSnippets[stageId];
        }

        return null;
    }

    public static String getMangledName(String baseName, int manglingSuffix) {
        if (manglingSuffix >= 0) {
            return baseName + '_' + manglingSuffix;
        } else {
            return baseName;
        }
    }

    public static String emitGlueCode(FragmentNode node,
                                      String localCoords,
                                      String priorStageOutput,
                                      String blenderDstColor,
                                      Formatter code) {
        String output = getMangledName("outColor", node.stageIndex());
        code.format("""
                // [%d] %s
                vec4 %s;
                """, node.stageIndex(), node.stage().mName, output);
        node.stage().mExpressionGenerator.generate(
                node,
                localCoords,
                priorStageOutput,
                blenderDstColor,
                output,
                code
        );
        return output;
    }

    /**
     * Collect arguments for {@link FragmentStage}'s static function call
     */
    private static void appendStageArguments(FragmentNode node,
                                             String localCoords,
                                             String priorStageOutput,
                                             String blenderDstColor,
                                             StringJoiner args) {
        FragmentStage stage = node.stage();

        // Append local coordinates.
        if (stage.needsLocalCoords()) {
            args.add(localCoords);
        }

        // Append prior-stage output color.
        if (stage.needsPriorStageOutput()) {
            args.add(priorStageOutput);
        }

        // Append blender destination color.
        if (stage.needsBlenderDstColor()) {
            args.add(blenderDstColor);
        }

        // Append uniform names.
        for (var uniform : stage.mUniforms) {
            args.add(
                    getMangledName(uniform.name(), node.stageIndex())
            );
        }

        // Append samplers.
        for (var sampler : stage.mSamplers) {
            args.add(
                    getMangledName(sampler.name(), node.stageIndex())
            );
        }
    }

    private static void generateDefaultExpression(FragmentNode node,
                                                  String localCoords,
                                                  String priorStageOutput,
                                                  String blenderDstColor,
                                                  String output,
                                                  Formatter code) {
        var arguments = new StringJoiner(",");
        appendStageArguments(node, localCoords, priorStageOutput, blenderDstColor, arguments);
        if (node.numChildren() > 0) {
            for (var child : node.children()) {
                code.format("{\n");
                arguments.add(
                        emitGlueCode(child,
                                localCoords,
                                priorStageOutput,
                                blenderDstColor,
                                code)
                );
                code.format("\n}\n");
            }
        }
        code.format("""
                %s = %s(%s);
                """, output, node.stage().mStaticFunctionName, arguments);
    }

    public static void emitDefinitions(FragmentNode[] nodes,
                                       Formatter code) {
        for (FragmentNode node : nodes) {

            if (node.numChildren() > 0) {
                emitDefinitions(node.children(), code);
            }

            code.format(node.stage().mStaticFunctionBody);
        }
    }
}
