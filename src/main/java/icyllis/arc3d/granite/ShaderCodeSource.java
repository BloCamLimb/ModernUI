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
import icyllis.arc3d.core.shaders.ImageShader;
import icyllis.arc3d.granite.shading.UniformHandler;

import java.util.*;

import static icyllis.arc3d.granite.FragmentStage.*;

/**
 * Manage all the fragment shader code snippets, used by Granite renderer.
 */
public class ShaderCodeSource {

    private static final Uniform[] PAINT_COLOR_UNIFORMS = {
            new Uniform(SLDataType.kFloat4, UniformHandler.PAINT_COLOR_NAME)
    };
    private static final Uniform XFORM_FLAGS =
            new Uniform(SLDataType.kInt, "u_XformFlags");
    private static final Uniform XFORM_SRC_TF =
            new Uniform(SLDataType.kFloat4, "u_XformSrcTf", 2);
    private static final Uniform XFORM_GAMUT_TRANSFORM =
            new Uniform(SLDataType.kFloat3x3, "u_XformGamutTransform");
    private static final Uniform XFORM_DST_TF =
            new Uniform(SLDataType.kFloat4, "u_XformDstTf", 2);

    // 8x8 lime and white checkerboard
    public static final String ARC_ERROR = """
            vec4 arc_error(vec2 coords) {
                return mix(vec4(1.0), vec4(0.0,1.0,0.0,1.0),
                       bool((int(coords.x) >> 3 ^ int(coords.y) >> 3) & 1));
            }
            """;
    public static final String ARC_PASSTHROUGH = """
            vec4 arc_passthrough(vec4 inColor) {
                return inColor;
            }
            """;
    public static final String ARC_SOLID_COLOR = """
            vec4 arc_solid_color(vec4 color) {
                return color;
            }
            """;
    public static final String ARC_RGB_OPAQUE = """
            vec4 arc_rgb_opaque(vec4 paintColor) {
                return vec4(paintColor.rgb, 1.0);
            }
            """;
    public static final String ARC_ALPHA_ONLY = """
            vec4 arc_alpha_only(vec4 paintColor) {
                return vec4(0.0, 0.0, 0.0, paintColor.a);
            }
            """;
    // sRGB-like transfer function
    private static final String PRIV_TRANSFER_FUNCTION = """
            float TransferFunction(float x, vec4 tf[2]) {
                float G = tf[0][0], A = tf[0][1], B = tf[0][2], C = tf[0][3],
                      D = tf[1][0], E = tf[1][1], F = tf[1][2];
                float s = sign(x);
                x = abs(x);
                x = (x < D) ? (C * x) + F
                            : pow(A * x + B, G) + E;
                return s * x;
            }
            """;
    private static final String PRIV_INV_TRANSFER_FUNCTION = """
            float InvTransferFunction(float x, vec4 tf[2]) {
                float G = tf[0][0], A = tf[0][1], B = tf[0][2], C = tf[0][3],
                      D = tf[1][0], E = tf[1][1], F = tf[1][2];
                float s = sign(x);
                x = abs(x);
                x = (x < D * C) ? (x - F) / C
                                : (pow(x - E, 1.0 / G) - B) / A;
                return s * x;
            }
            """;
    // We have 7 source coefficients and 7 destination coefficients. We pass them via two vec4 arrays;
    // In std140, this arrangement is much more efficient than a simple array of scalars, which
    // vec4 array and mat3 are always vec4 aligned
    //TODO TBD: can we move 'flags' to the last value of 'srcTf'?
    public static final String ARC_COLOR_SPACE_TRANSFORM = """
            vec4 arc_color_space_transform(vec4 color,
                                           int flags,
                                           vec4 srcTf[2],
                                           mat3 gamutTransform,
                                           vec4 dstTf[2]) {
                const int kColorSpaceXformFlagUnpremul = 0x1;
                const int kColorSpaceXformFlagLinearize = 0x2;
                const int kColorSpaceXformFlagGamutTransform = 0x4;
                const int kColorSpaceXformFlagEncode = 0x8;
                const int kColorSpaceXformFlagPremul = 0x10;
                
                if (bool(flags & kColorSpaceXformFlagUnpremul)) {
                    color.rgb /= max(color.a, 0.00001);
                }
                        
                if (bool(flags & kColorSpaceXformFlagLinearize)) {
                    color.r = TransferFunction(color.r, srcTf);
                    color.g = TransferFunction(color.g, srcTf);
                    color.b = TransferFunction(color.b, srcTf);
                }
                if (bool(flags & kColorSpaceXformFlagGamutTransform)) {
                    color.rgb = gamutTransform * color.rgb;
                }
                if (bool(flags & kColorSpaceXformFlagEncode)) {
                    color.r = InvTransferFunction(color.r, dstTf);
                    color.g = InvTransferFunction(color.g, dstTf);
                    color.b = InvTransferFunction(color.b, dstTf);
                }
                        
                if (bool(flags & kColorSpaceXformFlagPremul)) {
                    color.rgb *= color.a;
                }
                return color;
            }
            """;

    static {
        //noinspection ConstantValue
        assert ImageShader.TILE_MODE_REPEAT == 0;
        //noinspection ConstantValue
        assert ImageShader.TILE_MODE_MIRROR == 1;
        //noinspection ConstantValue
        assert ImageShader.TILE_MODE_CLAMP == 2;
        //noinspection ConstantValue
        assert ImageShader.TILE_MODE_DECAL == 3;
    }

    // t.y < 0 means out of bounds, then color will be (0,0,0,0)
    // if any component is out of bounds, then that component is 0,
    // and (s.x * s.y) is 0, this eliminates branch
    private static final String PRIV_TILE_GRAD = """
            vec2 TileGrad(int tileMode, vec2 t) {
                const int kTileModeRepeat = 0;
                const int kTileModeMirror = 1;
                const int kTileModeClamp  = 2;
                const int kTileModeDecal  = 3;
                
                switch (tileMode) {
                    case kTileModeRepeat:
                        t.x = fract(t.x);
                        break;
                        
                    case kTileModeMirror: {
                        float s = t.x - 1.0;
                        s = s - 2.0 * floor(s * 0.5) - 1.0;
                        t.x = abs(s);
                        break;
                    }
                    
                    case kTileModeClamp:
                        t.x = clamp(t.x, 0.0, 1.0);
                        break;
                        
                    case kTileModeDecal: {
                        vec2 s = vec2(step(0.0, t.x), step(t.x, 1.0));
                        t.y = s.x * s.y - 0.5;
                        break;
                    }
                }
                return t;
            }
            """;
    private static final String PRIV_COLORIZE_GRAD_4 = """
            vec4 ColorizeGrad4(vec4 colors[4], vec4 offsets, vec2 t) {
                vec4 result;
                if (t.y < 0) {
                    result = vec4(0);
                } else if (t.x <= offsets[0]) {
                    result = colors[0];
                } else if (t.x < offsets[1]) {
                    result = mix(colors[0], colors[1], (t.x        - offsets[0])
                                                       (offsets[1] - offsets[0]));
                } else if (t.x < offsets[2]) {
                    result = mix(colors[1], colors[2], (t.x        - offsets[1])
                                                       (offsets[2] - offsets[1]));
                } else if (t.x < offsets[3]) {
                    result = mix(colors[2], colors[3], (t.x        - offsets[2])
                                                       (offsets[3] - offsets[2]));
                } else {
                    result = colors[3];
                }
                return result;
            }
            """;
    // Unrolled binary search through intervals
    // ( .. 0), (0 .. 1), (1 .. 2), (2 .. 3), (3 .. 4), (4 .. 5), (5 .. 6), (6 .. 7), (7 .. ).
    private static final String PRIV_COLORIZE_GRAD_8 = """
            vec4 ColorizeGrad8(vec4 colors[8], vec4 offsets[2], vec2 t) {
                vec4 result;
                if (t.y < 0) {
                    result = vec4(0);
                } else if (t.x < offsets[1][0]) {
                    if (t.x < offsets[0][2]) {
                        if (t.x <= offsets[0][0]) {
                            result = colors[0];
                        } else if (t.x < offsets[0][1]) {
                            result = mix(colors[0], colors[1],
                                         (t.x           - offsets[0][0])
                                         (offsets[0][1] - offsets[0][0]));
                        } else {
                            result = mix(colors[1], colors[2],
                                         (t.x           - offsets[0][1])
                                         (offsets[0][2] - offsets[0][1]));
                        }
                    } else {
                        if (t.x < offsets[0][3]) {
                            result = mix(colors[2], colors[3],
                                         (t.x           - offsets[0][2])
                                         (offsets[0][3] - offsets[0][2]));
                        } else {
                            result = mix(colors[3], colors[4],
                                         (t.x           - offsets[0][3])
                                         (offsets[1][0] - offsets[0][3]));
                        }
                    }
                } else {
                    if (t.x < offsets[1][2]) {
                        if (t.x < offsets[1][1]) {
                            result = mix(colors[4], colors[5],
                                         (t.x           - offsets[1][0])
                                         (offsets[1][1] - offsets[1][0]));
                        } else {
                            result = mix(colors[5], colors[6],
                                         (t.x           - offsets[1][1])
                                         (offsets[1][2] - offsets[1][1]));
                        }
                    } else {
                        if (t.x < offsets[1][3]) {
                            result = mix(colors[6], colors[7],
                                         (t.x           - offsets[1][2])
                                         (offsets[1][3] - offsets[1][2]));
                        } else {
                            result = colors[7];
                        }
                    }
                }
                return result;
            }
            """;
    // Add small epsilon since when the gradient is horizontally or vertically aligned,
    // pixels along the same column or row can have slightly different interpolated t values
    // causing pixels to choose the wrong offset when colorizing. This helps ensure pixels
    // along the same column or row choose the same gradient offsets.
    private static final String PRIV_LINEAR_GRAD_LAYOUT = """
            vec2 LinearGradLayout(vec2 pos) {
                return vec2(pos.x + 0.00001, 1);
            }
            """;
    private static final String PRIV_RADIAL_GRAD_LAYOUT = """
            vec2 RadialGradLayout(vec2 pos) {
                float t = length(pos);
                return vec2(t, 1);
            }
            """;
    // Hardcode pi/2 for the angle when x == 0, to avoid undefined behavior.
    // 0.1591549430918953 is 1/(2*pi), used since atan returns values [-pi, pi]
    private static final String PRIV_ANGULAR_GRAD_LAYOUT = """
            vec2 AngularGradLayout(vec2 pos, float bias, float scale) {
                float angle = mix(sign(pos.y) * -1.5707963267948966, atan(-pos.y, -pos.x), pos.x != 0.0);
                float t = (angle * 0.1591549430918953 + 0.5 + bias) * scale;
                return vec2(t, 1);
            }
            """;
    public static final String ARC_LINEAR_GRAD_4_SHADER = """
            vec4 arc_linear_grad_4_shader(vec2 coords,
                                          vec4 colors[4],
                                          vec4 offsets,
                                          int tileMode,
                                          int colorSpace,
                                          int doUnpremul) {
                vec2 t = LinearGradLayout(coords);
                t = TileGrad(tileMode, t);
                vec4 color = ColorizeGrad4(colors, offsets, t);
                return color;
            }
            """;
    public static final String ARC_LINEAR_GRAD_8_SHADER = """
            vec4 arc_linear_grad_8_shader(vec2 coords,
                                          vec4 colors[8],
                                          vec4 offsets[2],
                                          int tileMode,
                                          int colorSpace,
                                          int doUnpremul) {
                vec2 t = LinearGradLayout(coords);
                t = TileGrad(tileMode, t);
                vec4 color = ColorizeGrad8(colors, offsets, t);
                return color;
            }
            """;
    public static final String ARC_RADIAL_GRAD_4_SHADER = """
            vec4 arc_radial_grad_4_shader(vec2 coords,
                                          vec4 colors[4],
                                          vec4 offsets,
                                          int tileMode,
                                          int colorSpace,
                                          int doUnpremul) {
                vec2 t = RadialGradLayout(coords);
                t = TileGrad(tileMode, t);
                vec4 color = ColorizeGrad4(colors, offsets, t);
                return color;
            }
            """;
    public static final String ARC_RADIAL_GRAD_8_SHADER = """
            vec4 arc_radial_grad_8_shader(vec2 coords,
                                          vec4 colors[8],
                                          vec4 offsets[2],
                                          int tileMode,
                                          int colorSpace,
                                          int doUnpremul) {
                vec2 t = RadialGradLayout(coords);
                t = TileGrad(tileMode, t);
                vec4 color = ColorizeGrad8(colors, offsets, t);
                return color;
            }
            """;
    public static final String ARC_ANGULAR_GRAD_4_SHADER = """
            vec4 arc_angular_grad_4_shader(vec2 coords,
                                          vec4 colors[4],
                                          vec4 offsets,
                                          float bias,
                                          float scale,
                                          int tileMode,
                                          int colorSpace,
                                          int doUnpremul) {
                vec2 t = AngularGradLayout(coords, bias, scale);
                t = TileGrad(tileMode, t);
                vec4 color = ColorizeGrad4(colors, offsets, t);
                return color;
            }
            """;
    public static final String ARC_ANGULAR_GRAD_8_SHADER = """
            vec4 arc_angular_grad_8_shader(vec2 coords,
                                          vec4 colors[8],
                                          vec4 offsets[2],
                                          float bias,
                                          float scale,
                                          int tileMode,
                                          int colorSpace,
                                          int doUnpremul) {
                vec2 t = AngularGradLayout(coords, bias, scale);
                t = TileGrad(tileMode, t);
                vec4 color = ColorizeGrad8(colors, offsets, t);
                return color;
            }
            """;
    public static final String ARC_HW_IMAGE_SHADER = """
            vec4 arc_hw_image_shader(vec2 coords,
                                     vec2 invImageSize,
                                     int xformFlags,
                                     vec4 xformSrcTf[2],
                                     mat3 xformGamutTransform,
                                     vec4 xformDstTf[2],
                                     sampler2D s) {
                vec4 samp = texture(s, coords * invImageSize);
                return arc_color_space_transform(samp, xformFlags, xformSrcTf,
                                                 xformGamutTransform, xformDstTf);
            }
            """;

    private final FragmentStage[] mBuiltinCodeSnippets =
            new FragmentStage[kBuiltinStageIDCount];

    {
        mBuiltinCodeSnippets[kError_BuiltinStageID] = new FragmentStage(
                "Error",
                kLocalCoords_ReqFlag,
                "arc_error",
                new String[]{ARC_ERROR},
                NO_UNIFORMS,
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kPassthrough_BuiltinStageID] = new FragmentStage(
                "Passthrough",
                kPriorStageOutput_ReqFlag,
                "arc_passthrough",
                new String[]{ARC_PASSTHROUGH},
                NO_UNIFORMS,
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kSolidColorShader_BuiltinStageID] = new FragmentStage(
                "SolidColor",
                kNone_ReqFlag,
                "arc_solid_color",
                new String[]{ARC_SOLID_COLOR},
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
                new String[]{ARC_RGB_OPAQUE},
                PAINT_COLOR_UNIFORMS,
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kAlphaOnlyPaintColor_BuiltinStageID] = new FragmentStage(
                "AlphaOnlyPaintColor",
                kNone_ReqFlag,
                "arc_alpha_only",
                new String[]{ARC_ALPHA_ONLY},
                PAINT_COLOR_UNIFORMS,
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kLocalMatrixShader_BuiltinStageID] = new FragmentStage(
                "LocalMatrixShader",
                kLocalCoords_ReqFlag | kPriorStageOutput_ReqFlag,
                "LocalMatrix",
                NO_FUNCTIONS,
                new Uniform[]{
                        new Uniform(
                                SLDataType.kFloat3x3,
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
                                    vec2 %s = (%s * vec3(%s, 1)).xy;
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
                    code.format("}\n");
                },
                1
        );
        //TODO TBD can we store InvImageSize in local matrix?
        mBuiltinCodeSnippets[kHWImageShader_BuiltinStageID] = new FragmentStage(
                "HardwareImageShader",
                kLocalCoords_ReqFlag,
                "arc_hw_image_shader",
                new String[]{PRIV_TRANSFER_FUNCTION, PRIV_INV_TRANSFER_FUNCTION,
                        ARC_COLOR_SPACE_TRANSFORM, ARC_HW_IMAGE_SHADER},
                new Uniform[]{
                        new Uniform(SLDataType.kFloat2, "u_InvImageSize"),
                        XFORM_FLAGS, XFORM_SRC_TF, XFORM_GAMUT_TRANSFORM, XFORM_DST_TF
                },
                new Sampler[]{
                        new Sampler(SLDataType.kSampler2D, "u_Sampler")
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
                code.format("}\n");
            }
        }
        code.format("""
                %s = %s(%s);
                """, output, node.stage().mStaticFunctionName, arguments);
    }

    public static void emitDefinitions(FragmentNode[] nodes,
                                       IdentityHashMap<String, String> added,
                                       Formatter code) {
        for (FragmentNode node : nodes) {

            if (node.numChildren() > 0) {
                emitDefinitions(node.children(), added, code);
            }

            for (String function : node.stage().mRequiredFunctions) {
                if (added.put(function, function) == null) {
                    code.format(function);
                }
            }
        }
    }
}
