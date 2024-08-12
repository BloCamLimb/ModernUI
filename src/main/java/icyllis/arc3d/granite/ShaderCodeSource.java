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

import icyllis.arc3d.core.PixelUtils;
import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.core.shaders.Shader;
import icyllis.arc3d.engine.SamplerDesc;
import icyllis.arc3d.granite.shading.UniformHandler;

import java.util.*;

import static icyllis.arc3d.granite.FragmentStage.*;

/**
 * Manage all the fragment shader code snippets, used by Granite renderer.
 */
public class ShaderCodeSource {

    // common uniform definitions
    private static final Uniform[] PAINT_COLOR_UNIFORMS = {
            new Uniform(SLDataType.kFloat4, UniformHandler.PAINT_COLOR_NAME)
    };
    private static final Uniform INV_IMAGE_SIZE =
            new Uniform(SLDataType.kFloat2, "u_InvImageSize");
    private static final Uniform SUBSET =
            new Uniform(SLDataType.kFloat4, "u_Subset");
    private static final Uniform TILE_MODE_X =
            new Uniform(SLDataType.kInt, "u_TileModeX");
    private static final Uniform TILE_MODE_Y =
            new Uniform(SLDataType.kInt, "u_TileModeY");
    private static final Uniform XFORM_FLAGS =
            new Uniform(SLDataType.kInt, "u_XformFlags");
    private static final Uniform XFORM_SRC_TF =
            new Uniform(SLDataType.kFloat4, "u_XformSrcTf", 2);
    private static final Uniform XFORM_GAMUT_TRANSFORM =
            new Uniform(SLDataType.kFloat3x3, "u_XformGamutTransform");
    private static final Uniform XFORM_DST_TF =
            new Uniform(SLDataType.kFloat4, "u_XformDstTf", 2);

    private static final Uniform GRAD_COLOR_SPACE =
            new Uniform(SLDataType.kInt, "u_ColorSpace");
    private static final Uniform GRAD_DO_UNPREMUL =
            new Uniform(SLDataType.kInt, "u_DoUnpremul");
    private static final Uniform GRAD_BIAS =
            new Uniform(SLDataType.kFloat, "u_Bias");
    private static final Uniform GRAD_SCALE =
            new Uniform(SLDataType.kFloat, "u_Scale");
    private static final Uniform GRAD_4_COLORS =
            new Uniform(SLDataType.kFloat4, "u_Colors", 4);
    private static final Uniform GRAD_4_OFFSETS =
            new Uniform(SLDataType.kFloat4, "u_Offsets");
    private static final Uniform GRAD_8_COLORS =
            new Uniform(SLDataType.kFloat4, "u_Colors", 8);
    private static final Uniform GRAD_8_OFFSETS =
            new Uniform(SLDataType.kFloat4, "u_Offsets", 2);

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
    // transfer function
    private static final String PRIV_TRANSFER_FUNCTION = """
            float TransferFunction(float x, vec4 tf[2]) {
                float G = tf[0][0], A = tf[0][1], B = tf[0][2], C = tf[0][3],
                      D = tf[1][0], E = tf[1][1], F = tf[1][2];
                float s = sign(x);
                x = abs(x);
                x = mix(pow(A * x + B, G) + E, (C * x) + F, x < D);
                return s * x;
            }
            """;
    private static final String PRIV_INV_TRANSFER_FUNCTION = """
            float InvTransferFunction(float x, vec4 tf[2]) {
                float G = tf[0][0], A = tf[0][1], B = tf[0][2], C = tf[0][3],
                      D = tf[1][0], E = tf[1][1], F = tf[1][2];
                float s = sign(x);
                x = abs(x);
                x = mix((pow(x - E, 1.0 / G) - B) / A, (x - F) / C, x < D * C);
                return s * x;
            }
            """;
    static {
        //noinspection ConstantValue
        assert PixelUtils.kColorSpaceXformFlagUnpremul == 0x1;
        //noinspection ConstantValue
        assert PixelUtils.kColorSpaceXformFlagLinearize == 0x2;
        //noinspection ConstantValue
        assert PixelUtils.kColorSpaceXformFlagGamutTransform == 0x4;
        //noinspection ConstantValue
        assert PixelUtils.kColorSpaceXformFlagEncode == 0x8;
        //noinspection ConstantValue
        assert PixelUtils.kColorSpaceXformFlagPremul == 0x10;
    }
    // We have 7 source coefficients and 7 destination coefficients. We pass them via two vec4 arrays;
    // In std140, this arrangement is much more efficient than a simple array of scalars, which
    // vec4 array and mat3 are always vec4 aligned
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
                    color.rgb /= max(color.a, 1e-7);
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
        assert Shader.TILE_MODE_REPEAT == 0;
        //noinspection ConstantValue
        assert Shader.TILE_MODE_MIRROR == 1;
        //noinspection ConstantValue
        assert Shader.TILE_MODE_CLAMP == 2;
        //noinspection ConstantValue
        assert Shader.TILE_MODE_DECAL == 3;
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
                    result = mix(colors[0], colors[1], (t.x        - offsets[0]) /
                                                       (offsets[1] - offsets[0]));
                } else if (t.x < offsets[2]) {
                    result = mix(colors[1], colors[2], (t.x        - offsets[1]) /
                                                       (offsets[2] - offsets[1]));
                } else if (t.x < offsets[3]) {
                    result = mix(colors[2], colors[3], (t.x        - offsets[2]) /
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
                                         (t.x           - offsets[0][0]) /
                                         (offsets[0][1] - offsets[0][0]));
                        } else {
                            result = mix(colors[1], colors[2],
                                         (t.x           - offsets[0][1]) /
                                         (offsets[0][2] - offsets[0][1]));
                        }
                    } else {
                        if (t.x < offsets[0][3]) {
                            result = mix(colors[2], colors[3],
                                         (t.x           - offsets[0][2]) /
                                         (offsets[0][3] - offsets[0][2]));
                        } else {
                            result = mix(colors[3], colors[4],
                                         (t.x           - offsets[0][3]) /
                                         (offsets[1][0] - offsets[0][3]));
                        }
                    }
                } else {
                    if (t.x < offsets[1][2]) {
                        if (t.x < offsets[1][1]) {
                            result = mix(colors[4], colors[5],
                                         (t.x           - offsets[1][0]) /
                                         (offsets[1][1] - offsets[1][0]));
                        } else {
                            result = mix(colors[5], colors[6],
                                         (t.x           - offsets[1][1]) /
                                         (offsets[1][2] - offsets[1][1]));
                        }
                    } else {
                        if (t.x < offsets[1][3]) {
                            result = mix(colors[6], colors[7],
                                         (t.x           - offsets[1][2]) /
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
    private static final String PRIV_TILE = """
            float Tile(int tileMode, float f, float low, float high) {
                const int kTileModeRepeat = 0;
                const int kTileModeMirror = 1;
                const int kTileModeClamp  = 2;
                const int kTileModeDecal  = 3;
                
                switch (tileMode) {
                    case kTileModeRepeat: {
                        float length = high - low;
                        f = mod(f - low, length) + low;
                        break;
                    }
                        
                    case kTileModeMirror: {
                        float length = high - low;
                        float t = mod(f - low, length * 2.0);
                        f = mix(t, length * 2.0 - t, step(length, t)) + low;
                        break;
                    }
                    
                    case kTileModeClamp:
                        f = clamp(f, low, high);
                        break;
                        
                    default: // kTileModeDecal
                        break;
                }
                return f;
            }
            """;
    static {
        //noinspection ConstantValue
        assert SamplerDesc.FILTER_NEAREST == 0;
        //noinspection ConstantValue
        assert SamplerDesc.FILTER_LINEAR  == 1;
    }
    // kLinearInset make sure we don't touch an outer row or column with a weight of 0 when linear filtering.
    private static final String PRIV_SAMPLE_IMAGE_SUBSET = """
            vec4 SampleImageSubset(vec2 pos,
                                   vec2 invImageSize,
                                   vec4 subset,
                                   int tileModeX,
                                   int tileModeY,
                                   int filterMode,
                                   vec2 linearFilterInset,
                                   sampler2D s) {
                const int kTileModeRepeat = 0;
                const int kTileModeMirror = 1;
                const int kTileModeClamp  = 2;
                const int kTileModeDecal  = 3;
                const int kFilterModeNearest = 0;
                const int kFilterModeLinear  = 1;
                const float kLinearInset = 0.5 + 0.00001;
                
                // Do hard-edge shader transitions to the border color for nearest-neighbor decal tiling at the
                // subset boundaries. Snap the input coordinates to nearest neighbor before comparing to the
                // subset rect, to avoid GPU interpolation errors.
                vec4 test = vec4(1.0);
                if (tileModeX == kTileModeDecal && filterMode == kFilterModeNearest) {
                    float snappedX = floor(pos.x) + 0.5;
                    test.xz = vec2(step(subset.x, snappedX), step(snappedX, subset.z));
                }
                if (tileModeY == kTileModeDecal && filterMode == kFilterModeNearest) {
                    float snappedY = floor(pos.y) + 0.5;
                    test.yw = vec2(step(subset.y, snappedY), step(snappedY, subset.w));
                }
                if (!all(bvec4(test))) {
                    return vec4(0);
                }
                
                pos.x = Tile(tileModeX, pos.x, subset.x, subset.z);
                pos.y = Tile(tileModeY, pos.y, subset.y, subset.w);
                
                // Clamp to an inset subset to prevent sampling neighboring texels when coords fall exactly at
                // texel boundaries.
                vec4 insetClamp;
                if (filterMode == kFilterModeNearest) {
                    insetClamp = vec4(floor(subset.xy) + kLinearInset, ceil(subset.zw) - kLinearInset);
                } else {
                    insetClamp = vec4(subset.xy + linearFilterInset.x, subset.zw - linearFilterInset.y);
                }
                vec2 clampedPos = clamp(pos, insetClamp.xy, insetClamp.zw);
                vec4 color = texture(s, clampedPos * invImageSize);
                
                if (filterMode == kFilterModeLinear) {
                    // Remember the amount the coord moved for clamping. This is used to implement shader-based
                    // filtering for repeat and decal tiling.
                    vec2 error = pos - clampedPos;
                    vec2 absError = abs(error);
                        
                    // Do 1 or 3 more texture reads depending on whether both x and y tiling modes are repeat
                    // and whether we're near a single subset edge or a corner. Then blend the multiple reads
                    // using the error values calculated above.
                    bvec2 sampleExtra = bvec2(tileModeX == kTileModeRepeat,
                                              tileModeY == kTileModeRepeat);
                    if (any(sampleExtra)) {
                        float extraCoordX;
                        float extraCoordY;
                        vec4 extraColorX;
                        vec4 extraColorY;
                        if (sampleExtra.x) {
                            extraCoordX = mix(insetClamp.z, insetClamp.x, error.x > 0.0);
                            extraColorX = texture(s, vec2(extraCoordX, clampedPos.y) * invImageSize);
                        }
                        if (sampleExtra.y) {
                            extraCoordY = mix(insetClamp.w, insetClamp.y, error.y > 0.0);
                            extraColorY = texture(s, vec2(clampedPos.x, extraCoordY) * invImageSize);
                        }
                        if (all(sampleExtra)) {
                            vec4 extraColorXY = texture(s, vec2(extraCoordX, extraCoordY) * invImageSize);
                            color = mix(mix(color, extraColorX, absError.x),
                                        mix(extraColorY, extraColorXY, absError.x),
                                        absError.y);
                        } else if (sampleExtra.x) {
                            color = mix(color, extraColorX, absError.x);
                        } else if (sampleExtra.y) {
                            color = mix(color, extraColorY, absError.y);
                        }
                    }
                        
                    // Do soft edge shader filtering for decal tiling and linear filtering using the error
                    // values calculated above.
                    color *= mix(1.0, max(1 - absError.x, 0), tileModeX == kTileModeDecal);
                    color *= mix(1.0, max(1 - absError.y, 0), tileModeY == kTileModeDecal);
                }
                        
                return color;
            }
            """;
    // simplified version from above, assuming filter is nearest and pos is clamped
    private static final String PRIV_SAMPLE_CUBIC_IMAGE_SUBSET = """
            vec4 SampleCubicImageSubset(vec2 pos,
                                        vec4 subset,
                                        int tileModeX,
                                        int tileModeY,
                                        sampler2D s) {
                const int kTileModeRepeat = 0;
                const int kTileModeMirror = 1;
                const int kTileModeClamp  = 2;
                const int kTileModeDecal  = 3;
                const float kLinearInset = 0.5 + 0.00001;
                
                vec4 test = vec4(1.0);
                if (tileModeX == kTileModeDecal) {
                    test.xz = vec2(step(subset.x, pos.x), step(pos.x, subset.z));
                }
                if (tileModeY == kTileModeDecal) {
                    test.yw = vec2(step(subset.y, pos.y), step(pos.y, subset.w));
                }
                if (!all(bvec4(test))) {
                    return vec4(0);
                }
                
                pos.x = Tile(tileModeX, pos.x, subset.x, subset.z);
                pos.y = Tile(tileModeY, pos.y, subset.y, subset.w);
                
                // Clamp to an inset subset to prevent sampling neighboring texels when coords fall exactly at
                // texel boundaries.
                vec4 insetClamp = vec4(floor(subset.xy) + kLinearInset, ceil(subset.zw) - kLinearInset);
                vec2 clampedPos = clamp(pos, insetClamp.xy, insetClamp.zw);
                vec4 color = texelFetch(s, ivec2(clampedPos), 0);
                        
                return color;
            }
            """;
    private static final String PRIV_CUBIC_FILTER_IMAGE = """
            vec4 CubicFilterImage(vec2 pos,
                                  vec4 subset,
                                  int tileModeX,
                                  int tileModeY,
                                  mat4 coeffs,
                                  int cubicClamp,
                                  sampler2D s) {
                const int kFilterModeNearest = 0;
                const int kFilterModeLinear  = 1;
                const int kCubicClampUnpremul = 0;
                const int kCubicClampPremul   = 1;
                const float kLinearInset = 0.5 + 0.00001;
                
                // Determine pos's fractional offset f between texel centers.
                vec2 f = fract(pos - 0.5);
                // Sample 16 points at 1-pixel intervals from [p - 1.5 ... p + 1.5].
                pos -= 1.5;
                // Snap to texel centers to prevent sampling neighboring texels.
                pos = floor(pos) + 0.5;
                        
                vec4 wx = coeffs * vec4(1.0, f.x, f.x * f.x, f.x * f.x * f.x);
                vec4 wy = coeffs * vec4(1.0, f.y, f.y * f.y, f.y * f.y * f.y);
                vec4 color = vec4(0);
                for (int y = 0; y < 4; ++y) {
                    vec4 rowColor = vec4(0);
                    for (int x = 0; x < 4; ++x) {
                        rowColor += wx[x] * SampleCubicImageSubset(pos + vec2(x, y), subset,
                                                                   tileModeX, tileModeY, s);
                    }
                    color += wy[y] * rowColor;
                }
                // Bicubic can send colors out of range, so clamp to get them back in gamut.
                if (cubicClamp == kCubicClampUnpremul) {
                    color = clamp(color, 0.0, 1.0);
                } else {
                    color.a = clamp(color.a, 0.0, 1.0);
                    color.rgb = clamp(color.rgb, 0.0, color.a);
                }
                return color;
            }
            """;
    public static final String ARC_IMAGE_SHADER = """
            vec4 arc_image_shader(vec2 coords,
                                  vec2 invImageSize,
                                  vec4 subset,
                                  int filterMode,
                                  int tileModeX,
                                  int tileModeY,
                                  sampler2D s) {
                const float kLinearInset = 0.5 + 0.00001;
                return SampleImageSubset(coords, invImageSize, subset, tileModeX, tileModeY,
                                         filterMode, vec2(kLinearInset), s);
            }
            """;
    public static final String ARC_CUBIC_IMAGE_SHADER = """
            vec4 arc_cubic_image_shader(vec2 coords,
                                        vec4 subset,
                                        mat4 cubicCoeffs,
                                        int cubicClamp,
                                        int tileModeX,
                                        int tileModeY,
                                        sampler2D s) {
                return CubicFilterImage(coords, subset, tileModeX, tileModeY,
                                        cubicCoeffs, cubicClamp, s);
            }
            """;
    public static final String ARC_HW_IMAGE_SHADER = """
            vec4 arc_hw_image_shader(vec2 coords,
                                     vec2 invImageSize,
                                     sampler2D s) {
                return texture(s, coords * invImageSize);
            }
            """;
    public static final String ARC_DITHER_SHADER = """
            vec4 arc_dither_shader(vec4 color,
                                   float range) {
                // Unrolled 8x8 Bayer matrix
                vec2 A = gl_FragCoord.xy;
                vec2 B = floor(A);
                float U = fract(B.x * 0.5 + B.y * B.y * 0.75);
                vec2 C = A * 0.5;
                vec2 D = floor(C);
                float V = fract(D.x * 0.5 + D.y * D.y * 0.75);
                vec2 E = C * 0.5;
                vec2 F = floor(E);
                float W = fract(F.x * 0.5 + F.y * F.y * 0.75);
                float dithering = ((W * 0.25 + V) * 0.25 + U) - (63.0 / 128.0);
                // For each color channel, add the random offset to the channel value and then clamp
                // between 0 and alpha to keep the color premultiplied.
                return vec4(clamp(color.rgb + dithering * range, 0.0, color.a), color.a);
            }
            """;
    /**
     * Public blend functions, these are pure functions.
     * <p>
     * Implementation is the same as raster pipeline, but is vectorized and eliminates branches.
     *
     * @see icyllis.arc3d.core.BlendMode
     */
    public static final String BLEND_CLEAR = """
            vec4 blend_clear(vec4 src, vec4 dst) {
                return vec4(0);
            }
            """;
    public static final String BLEND_SRC = """
            vec4 blend_src(vec4 src, vec4 dst) {
                return src;
            }
            """;
    public static final String BLEND_DST = """
            vec4 blend_dst(vec4 src, vec4 dst) {
                return dst;
            }
            """;
    public static final String BLEND_SRC_OVER = """
            vec4 blend_src_over(vec4 src, vec4 dst) {
                return src + dst * (1 - src.a);
            }
            """;
    public static final String BLEND_DST_OVER = """
            vec4 blend_dst_over(vec4 src, vec4 dst) {
                return src * (1 - dst.a) + dst;
            }
            """;
    public static final String BLEND_SRC_IN = """
            vec4 blend_src_in(vec4 src, vec4 dst) {
                return src * dst.a;
            }
            """;
    public static final String BLEND_DST_IN = """
            vec4 blend_dst_in(vec4 src, vec4 dst) {
                return dst * src.a;
            }
            """;
    public static final String BLEND_SRC_OUT = """
            vec4 blend_src_out(vec4 src, vec4 dst) {
                return src * (1 - dst.a);
            }
            """;
    public static final String BLEND_DST_OUT = """
            vec4 blend_dst_out(vec4 src, vec4 dst) {
                return dst * (1 - src.a);
            }
            """;
    public static final String BLEND_SRC_ATOP = """
            vec4 blend_src_atop(vec4 src, vec4 dst) {
                return src * dst.a + dst * (1 - src.a);
            }
            """;
    public static final String BLEND_DST_ATOP = """
            vec4 blend_dst_atop(vec4 src, vec4 dst) {
                return src * (1 - dst.a) + dst * src.a;
            }
            """;
    public static final String BLEND_XOR = """
            vec4 blend_xor(vec4 src, vec4 dst) {
                return src * (1 - dst.a) + dst * (1 - src.a);
            }
            """;
    public static final String BLEND_PLUS = """
            vec4 blend_plus(vec4 src, vec4 dst) {
                return src + dst;
            }
            """;
    public static final String BLEND_PLUS_CLAMPED = """
            vec4 blend_plus_clamped(vec4 src, vec4 dst) {
                return min(src + dst, 1);
            }
            """;
    public static final String BLEND_MINUS = """
            vec4 blend_minus(vec4 src, vec4 dst) {
                return dst - src;
            }
            """;
    public static final String BLEND_MINUS_CLAMPED = """
            vec4 blend_minus_clamped(vec4 src, vec4 dst) {
                return max(dst - src, 0);
            }
            """;
    public static final String BLEND_MODULATE = """
            vec4 blend_modulate(vec4 src, vec4 dst) {
                return src * dst;
            }
            """;
    public static final String BLEND_MULTIPLY = """
            vec4 blend_multiply(vec4 src, vec4 dst) {
                return src * dst + src * (1 - dst.a) + dst * (1 - src.a);
            }
            """;
    public static final String BLEND_SCREEN = """
            vec4 blend_screen(vec4 src, vec4 dst) {
                return src + dst - src * dst;
            }
            """;
    public static final String BLEND_OVERLAY = """
            vec4 blend_overlay(vec4 src, vec4 dst) {
                vec3 s = src.rgb,       d = dst.rgb;
                vec3 sa = vec3(src.a),  da = vec3(dst.a);
                return vec4(s * (1 - da) + d * (1 - sa) +
                                mix(sa * da - 2 * (sa - s) * (da - d),
                                    2 * s * d,
                                    lessThanEqual(2 * d, da)),
                            sa + da * (1 - sa));
            }
            """;
    public static final String BLEND_DARKEN = """
            vec4 blend_darken(vec4 src, vec4 dst) {
                return src + dst - max(src * dst.a, dst * src.a);
            }
            """;
    public static final String BLEND_LIGHTEN = """
            vec4 blend_lighten(vec4 src, vec4 dst) {
                return src + dst - min(src * dst.a, dst * src.a);
            }
            """;
    public static final String BLEND_COLOR_DODGE = """
            vec4 blend_color_dodge(vec4 src, vec4 dst) {
                vec3 s = src.rgb,       d = dst.rgb;
                vec3 sa = vec3(src.a),  da = vec3(dst.a);
                return vec4(mix(mix(sa * min(da, d * sa / (sa - s)) + s * (1 - da) + d * (1 - sa),
                                    sa * da + s * (1 - da) + d * (1 - sa),
                                    greaterThanEqual(s, sa)),
                                s * (1 - da),
                                lessThanEqual(d, vec3(0))),
                            sa + da * (1 - sa));
            }
            """;
    public static final String BLEND_COLOR_BURN = """
            vec4 blend_color_burn(vec4 src, vec4 dst) {
                vec3 s = src.rgb,       d = dst.rgb;
                vec3 sa = vec3(src.a),  da = vec3(dst.a);
                return vec4(mix(mix(sa * max(vec3(0), da - (da - d) * sa / s) + s * (1 - da) + d * (1 - sa),
                                    d * (1 - sa),
                                    lessThanEqual(s, vec3(0))),
                                sa * da + s * (1 - da) + d * (1 - sa),
                                greaterThanEqual(d, da)),
                            sa + da * (1 - sa));
            }
            """;
    public static final String BLEND_HARD_LIGHT = """
            vec4 blend_hard_light(vec4 src, vec4 dst) {
                vec3 s = src.rgb,       d = dst.rgb;
                vec3 sa = vec3(src.a),  da = vec3(dst.a);
                return vec4(s * (1 - da) + d * (1 - sa) +
                                mix(sa * da - 2 * (sa - s) * (da - d),
                                    2 * s * d,
                                    lessThanEqual(2 * s, sa)),
                            sa + da * (1 - sa));
            }
            """;
    public static final String BLEND_SOFT_LIGHT = """
            vec4 blend_soft_light(vec4 src, vec4 dst) {
                vec3 s = src.rgb,       d = dst.rgb;
                vec3 sa = vec3(src.a),  da = vec3(dst.a);
                vec3 dd = d * d,        dada = da * da;
                return vec4(mix(mix(d * (sa - 2 * s + 1) + s * (1 - da) - sqrt(d * da) * (sa - 2 * s),
                                    (dada * (s + d * (6 * s - 3 * sa + 1)) + 12 * da * dd * (sa - 2 * s) -
                                          16 * dd * d * (sa - 2 * s) - dada * da * s) / dada,
                                    lessThanEqual(4 * d, da)),
                                d * d * (sa - 2 * s) / da + s * (1 - da) + d * (2 * s + 1 - sa),
                                lessThanEqual(2 * s, sa)),
                            sa + da * (1 - sa));
            }
            """;
    public static final String BLEND_DIFFERENCE = """
            vec4 blend_difference(vec4 src, vec4 dst) {
                return vec4(src.rgb + dst.rgb - 2 * min(src.rgb * dst.a, dst.rgb * src.a),
                            src.a + dst.a * (1 - src.a));
            }
            """;
    public static final String BLEND_EXCLUSION = """
            vec4 blend_exclusion(vec4 src, vec4 dst) {
                return vec4(src.rgb + dst.rgb - 2 * (src.rgb * dst.rgb),
                            src.a + dst.a * (1 - src.a));
            }
            """;
    public static final String BLEND_SUBTRACT = """
            vec4 blend_subtract(vec4 src, vec4 dst) {
                return vec4(src.rgb * (1 - dst.a) + dst.rgb - min(src.rgb * dst.a, dst.rgb * src.a),
                            src.a + dst.a * (1 - src.a));
            }
            """;
    /**
     * This can produce undefined results from {@link icyllis.arc3d.core.BlendMode#blend_divide}
     * if values out of range.
     */
    public static final String BLEND_DIVIDE = """
            vec4 blend_divide(vec4 src, vec4 dst) {
                vec3 numer = dst.rgb * src.a;
                vec3 denom = src.rgb * dst.a;
                vec3 c = src.rgb * (1 - dst.a) + dst.rgb * (1 - src.a);
                return vec4(mix(clamp(numer / denom, 0, 1) * src.a * dst.a + c,
                                mix(src.a * dst.a + c,
                                    c,
                                    equal(numer, vec3(0))),
                                equal(denom, vec3(0))),
                            src.a + dst.a * (1 - src.a));
            }
            """;
    public static final String BLEND_LINEAR_DODGE = """
            vec4 blend_linear_dodge(vec4 src, vec4 dst) {
                return vec4(min(src.rgb + dst.rgb, src.a * dst.a + src.rgb * (1 - dst.a) + dst.rgb * (1 - src.a)),
                            src.a + dst.a * (1 - src.a));
            }
            """;
    public static final String BLEND_LINEAR_BURN = """
            vec4 blend_linear_burn(vec4 src, vec4 dst) {
                return vec4(max(src.rgb + dst.rgb - src.a * dst.a, src.rgb * (1 - dst.a) + dst.rgb * (1 - src.a)),
                            src.a + dst.a * (1 - src.a));
            }
            """;
    public static final String BLEND_VIVID_LIGHT = """
            vec4 blend_vivid_light(vec4 src, vec4 dst) {
                vec3 s = src.rgb,       d = dst.rgb;
                vec3 sa = vec3(src.a),  da = vec3(dst.a);
                return vec4(mix(mix(sa * min(da, d * sa / (2 * (sa - s))) + s * (1 - da) + d * (1 - sa),
                                    sa * da + s * (1 - da) + d * (1 - sa),
                                    greaterThanEqual(s, sa)),
                                mix(sa * max(vec3(0), da - (da - d) * sa / (2 * s)) + s * (1 - da) + d * (1 - sa),
                                    d * (1 - sa),
                                    lessThanEqual(s, vec3(0))),
                                lessThan(2 * s, sa)),
                            sa + da * (1 - sa));
            }
            """;
    public static final String BLEND_LINEAR_LIGHT = """
            vec4 blend_linear_light(vec4 src, vec4 dst) {
                vec3 s = src.rgb,       d = dst.rgb;
                vec3 sa = vec3(src.a),  da = vec3(dst.a);
                return vec4(clamp(2 * s * da + d * sa - sa * da, vec3(0), sa * da) +
                                  s * (1 - da) + d * (1 - sa),
                            sa + da * (1 - sa));
            }
            """;
    public static final String BLEND_PIN_LIGHT = """
            vec4 blend_pin_light(vec4 src, vec4 dst) {
                vec3 s = src.rgb,       d = dst.rgb;
                vec3 sa = vec3(src.a),  da = vec3(dst.a);
                vec3 x = 2 * s * da;
                vec3 y = x - sa * da;
                vec3 z = d * sa;
                return vec4(s * (1 - da) + d * (1 - sa) +
                            mix(min(x, z),
                                mix(y,
                                    vec3(0),
                                    lessThan(2 * s, sa)),
                                greaterThan(y, z)),
                            sa + da * (1 - sa));
            }
            """;
    public static final String BLEND_HARD_MIX = """
            vec4 blend_hard_mix(vec4 src, vec4 dst) {
                vec3 s = src.rgb,       d = dst.rgb;
                vec3 sa = vec3(src.a),  da = vec3(dst.a);
                vec3 b = s * da + d * sa;
                vec3 c = sa * da;
                return vec4(s + d - b + mix(c, vec3(0), lessThan(b, c)),
                            sa + da * (1 - sa));
            }
            """;
    private static final String PRIV_BLEND_GET_LUM = """
            float BlendGetLum(vec3 color) {
                return dot(vec3(0.299, 0.587, 0.114), color);
            }
            """;
    public static final String BLEND_DARKER_COLOR = """
            vec4 blend_darker_color(vec4 src, vec4 dst) {
                return mix(src * (1 - dst.a) + dst,
                           src + dst * (1 - src.a),
                           bvec4(BlendGetLum(src.rgb) <= BlendGetLum(dst.rgb)));
            }
            """;
    public static final String BLEND_LIGHTER_COLOR = """
            vec4 blend_lighter_color(vec4 src, vec4 dst) {
                return mix(src * (1 - dst.a) + dst,
                           src + dst * (1 - src.a),
                           bvec4(BlendGetLum(src.rgb) >= BlendGetLum(dst.rgb)));
            }
            """;
    private static final String PRIV_BLEND_SET_LUM = """
            vec3 BlendSetLum(vec3 cbase,
                             vec3 clum, float alum,
                             float alpha) {
                float ldiff = BlendGetLum(clum) * alum - BlendGetLum(cbase);
                cbase += ldiff;
                float lum = BlendGetLum(cbase);
                float mincol = min(min(cbase.r, cbase.g), cbase.b);
                float maxcol = max(max(cbase.r, cbase.g), cbase.b);
                if (mincol < 0 && lum != mincol) {
                    cbase = lum + ((cbase - lum) * lum) / (lum - mincol);
                }
                if (maxcol > alpha && maxcol != lum) {
                    cbase = lum + ((cbase - lum) * (alpha - lum)) / (maxcol - lum);
                }
                return cbase;
            }
            """;
    private static final String PRIV_BLEND_SET_LUM_SAT = """
            vec3 BlendSetLumSat(vec3 cbase,
                                vec3 csat, float asat,
                                vec3 clum, float alum,
                                float alpha) {
                float minbase = min(min(cbase.r, cbase.g), cbase.b);
                float sbase = max(max(cbase.r, cbase.g), cbase.b) - minbase;
                if (sbase > 0) {
                    float ssat = (max(max(csat.r, csat.g), csat.b) - min(min(csat.r, csat.g), csat.b)) * asat;
                    cbase = (cbase - minbase) * ssat / sbase;
                } else {
                    cbase = vec3(0);
                }
                return BlendSetLum(cbase, clum, alum, alpha);
            }
            """;
    public static final String BLEND_HUE = """
            vec4 blend_hue(vec4 src, vec4 dst) {
                float alpha = src.a * dst.a;
                vec3 c = src.rgb * dst.a;
                c = BlendSetLumSat(c, dst.rgb, src.a, dst.rgb, src.a, alpha);
                return vec4(c + src.rgb * (1 - dst.a) + dst.rgb * (1 - src.a),
                            src.a + dst.a - alpha);
            }
            """;
    public static final String BLEND_SATURATION = """
            vec4 blend_saturation(vec4 src, vec4 dst) {
                float alpha = src.a * dst.a;
                vec3 c = dst.rgb * src.a;
                c = BlendSetLumSat(c, src.rgb, dst.a, dst.rgb, src.a, alpha);
                return vec4(c + src.rgb * (1 - dst.a) + dst.rgb * (1 - src.a),
                            src.a + dst.a - alpha);
            }
            """;
    public static final String BLEND_COLOR = """
            vec4 blend_color(vec4 src, vec4 dst) {
                float alpha = src.a * dst.a;
                vec3 c = src.rgb * dst.a;
                c = BlendSetLum(c, dst.rgb, src.a, alpha);
                return vec4(c + src.rgb * (1 - dst.a) + dst.rgb * (1 - src.a),
                            src.a + dst.a - alpha);
            }
            """;
    public static final String BLEND_LUMINOSITY = """
            vec4 blend_luminosity(vec4 src, vec4 dst) {
                float alpha = src.a * dst.a;
                vec3 c = dst.rgb * src.a;
                c = BlendSetLum(c, src.rgb, dst.a, alpha);
                return vec4(c + src.rgb * (1 - dst.a) + dst.rgb * (1 - src.a),
                            src.a + dst.a - alpha);
            }
            """;
    /**
     * Apply one of 42 blend modes.
     */
    public static final String ARC_BLEND = """
            vec4 arc_blend(vec4 src, vec4 dst, int blendMode) {
                const int kClear        = 0;
                const int kSrc          = 1;
                const int kDst          = 2;
                const int kSrcOver      = 3;
                const int kDstOver      = 4;
                const int kSrcIn        = 5;
                const int kDstIn        = 6;
                const int kSrcOut       = 7;
                const int kDstOut       = 8;
                const int kSrcATop      = 9;
                const int kDstATop      = 10;
                const int kXor          = 11;
                const int kPlus         = 12;
                const int kPlusClamped  = 13;
                const int kMinus        = 14;
                const int kMinusClamped = 15;
                const int kModulate     = 16;
                const int kMultiply     = 17;
                const int kScreen       = 18;
                const int kOverlay      = 19;
                const int kDarken       = 20;
                const int kLighten      = 21;
                const int kColorDodge   = 22;
                const int kColorBurn    = 23;
                const int kHardLight    = 24;
                const int kSoftLight    = 25;
                const int kDifference   = 26;
                const int kExclusion    = 27;
                const int kSubtract     = 28;
                const int kDivide       = 29;
                const int kLinearDodge  = 30;
                const int kLinearBurn   = 31;
                const int kVividLight   = 32;
                const int kLinearLight  = 33;
                const int kPinLight     = 34;
                const int kHardMix      = 35;
                const int kDarkerColor  = 36;
                const int LighterColor  = 37;
                const int kHue          = 38;
                const int kSaturation   = 39;
                const int kColor        = 40;
                const int kLuminosity   = 41;
                
                switch (blendMode) {
                    case kClear        : return blend_clear          (src,dst);
                    case kSrc          : return blend_src            (src,dst);
                    case kDst          : return blend_dst            (src,dst);
                    case kSrcOver      : return blend_src_over       (src,dst);
                    case kDstOver      : return blend_dst_over       (src,dst);
                    case kSrcIn        : return blend_src_in         (src,dst);
                    case kDstIn        : return blend_dst_in         (src,dst);
                    case kSrcOut       : return blend_src_out        (src,dst);
                    case kDstOut       : return blend_dst_out        (src,dst);
                    case kSrcATop      : return blend_src_atop       (src,dst);
                    case kDstATop      : return blend_dst_atop       (src,dst);
                    case kXor          : return blend_xor            (src,dst);
                    case kPlus         : return blend_plus           (src,dst);
                    case kPlusClamped  : return blend_plus_clamped   (src,dst);
                    case kMinus        : return blend_minus          (src,dst);
                    case kMinusClamped : return blend_minus_clamped  (src,dst);
                    case kModulate     : return blend_modulate       (src,dst);
                    case kMultiply     : return blend_multiply       (src,dst);
                    case kScreen       : return blend_screen         (src,dst);
                    case kOverlay      : return blend_overlay        (src,dst);
                    case kDarken       : return blend_darken         (src,dst);
                    case kLighten      : return blend_lighten        (src,dst);
                    case kColorDodge   : return blend_color_dodge    (src,dst);
                    case kColorBurn    : return blend_color_burn     (src,dst);
                    case kHardLight    : return blend_hard_light     (src,dst);
                    case kSoftLight    : return blend_soft_light     (src,dst);
                    case kDifference   : return blend_difference     (src,dst);
                    case kExclusion    : return blend_exclusion      (src,dst);
                    case kSubtract     : return blend_subtract       (src,dst);
                    case kDivide       : return blend_divide         (src,dst);
                    case kLinearDodge  : return blend_linear_dodge   (src,dst);
                    case kLinearBurn   : return blend_linear_burn    (src,dst);
                    case kVividLight   : return blend_vivid_light    (src,dst);
                    case kLinearLight  : return blend_linear_light   (src,dst);
                    case kPinLight     : return blend_pin_light      (src,dst);
                    case kHardMix      : return blend_hard_mix       (src,dst);
                    case kDarkerColor  : return blend_darker_color   (src,dst);
                    case LighterColor  : return blend_lighter_color  (src,dst);
                    case kHue          : return blend_hue            (src,dst);
                    case kSaturation   : return blend_saturation     (src,dst);
                    case kColor        : return blend_color          (src,dst);
                    case kLuminosity   : return blend_luminosity     (src,dst);
                    default            : return vec4(0);
                }
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
        mBuiltinCodeSnippets[kLinearGradientShader4_BuiltinStageID] = new FragmentStage(
                "LinearGradient4",
                kLocalCoords_ReqFlag,
                "arc_linear_grad_4_shader",
                new String[]{PRIV_TILE_GRAD, PRIV_LINEAR_GRAD_LAYOUT, PRIV_COLORIZE_GRAD_4,
                        ARC_LINEAR_GRAD_4_SHADER},
                new Uniform[]{
                        GRAD_4_COLORS,
                        GRAD_4_OFFSETS,
                        TILE_MODE_X,
                        GRAD_COLOR_SPACE,
                        GRAD_DO_UNPREMUL
                },
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kLinearGradientShader8_BuiltinStageID] = new FragmentStage(
                "LinearGradient8",
                kLocalCoords_ReqFlag,
                "arc_linear_grad_8_shader",
                new String[]{PRIV_TILE_GRAD, PRIV_LINEAR_GRAD_LAYOUT, PRIV_COLORIZE_GRAD_8,
                        ARC_LINEAR_GRAD_8_SHADER},
                new Uniform[]{
                        GRAD_8_COLORS,
                        GRAD_8_OFFSETS,
                        TILE_MODE_X,
                        GRAD_COLOR_SPACE,
                        GRAD_DO_UNPREMUL
                },
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kRadialGradientShader4_BuiltinStageID] = new FragmentStage(
                "RadialGradient4",
                kLocalCoords_ReqFlag,
                "arc_radial_grad_4_shader",
                new String[]{PRIV_TILE_GRAD, PRIV_RADIAL_GRAD_LAYOUT, PRIV_COLORIZE_GRAD_4,
                        ARC_RADIAL_GRAD_4_SHADER},
                new Uniform[]{
                        GRAD_4_COLORS,
                        GRAD_4_OFFSETS,
                        TILE_MODE_X,
                        GRAD_COLOR_SPACE,
                        GRAD_DO_UNPREMUL
                },
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kRadialGradientShader8_BuiltinStageID] = new FragmentStage(
                "RadialGradient8",
                kLocalCoords_ReqFlag,
                "arc_radial_grad_8_shader",
                new String[]{PRIV_TILE_GRAD, PRIV_RADIAL_GRAD_LAYOUT, PRIV_COLORIZE_GRAD_8,
                        ARC_RADIAL_GRAD_8_SHADER},
                new Uniform[]{
                        GRAD_8_COLORS,
                        GRAD_8_OFFSETS,
                        TILE_MODE_X,
                        GRAD_COLOR_SPACE,
                        GRAD_DO_UNPREMUL
                },
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kAngularGradientShader4_BuiltinStageID] = new FragmentStage(
                "AngularGradient4",
                kLocalCoords_ReqFlag,
                "arc_angular_grad_4_shader",
                new String[]{PRIV_TILE_GRAD, PRIV_ANGULAR_GRAD_LAYOUT, PRIV_COLORIZE_GRAD_4,
                        ARC_ANGULAR_GRAD_4_SHADER},
                new Uniform[]{
                        GRAD_4_COLORS,
                        GRAD_4_OFFSETS,
                        GRAD_BIAS,
                        GRAD_SCALE,
                        TILE_MODE_X,
                        GRAD_COLOR_SPACE,
                        GRAD_DO_UNPREMUL
                },
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kAngularGradientShader8_BuiltinStageID] = new FragmentStage(
                "AngularGradient8",
                kLocalCoords_ReqFlag,
                "arc_angular_grad_8_shader",
                new String[]{PRIV_TILE_GRAD, PRIV_ANGULAR_GRAD_LAYOUT, PRIV_COLORIZE_GRAD_8,
                        ARC_ANGULAR_GRAD_8_SHADER},
                new Uniform[]{
                        GRAD_8_COLORS,
                        GRAD_8_OFFSETS,
                        GRAD_BIAS,
                        GRAD_SCALE,
                        TILE_MODE_X,
                        GRAD_COLOR_SPACE,
                        GRAD_DO_UNPREMUL
                },
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

                    String newPerspCoordsName = getMangledName("newPerspCoords", node.stageIndex());
                    String newLocalCoordsName = getMangledName("newLocalCoords", node.stageIndex());
                    String localMatrixName = getMangledName(node.stage().mUniforms[0].name(), node.stageIndex());
                    code.format("""
                                    {
                                    vec3 %1$s = %3$s * vec3(%4$s, 1);
                                    vec2 %2$s = %1$s.xy / %1$s.z;
                                    """,
                            newPerspCoordsName,
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
        mBuiltinCodeSnippets[kImageShader_BuiltinStageID] = new FragmentStage(
                "ImageShader",
                kLocalCoords_ReqFlag,
                "arc_image_shader",
                new String[]{
                        PRIV_TILE, PRIV_SAMPLE_IMAGE_SUBSET,
                        ARC_IMAGE_SHADER
                },
                new Uniform[]{
                        INV_IMAGE_SIZE, SUBSET,
                        new Uniform(SLDataType.kInt, "u_FilterMode"),
                        TILE_MODE_X, TILE_MODE_Y
                },
                new Sampler[]{
                        new Sampler(SLDataType.kSampler2D, "u_Sampler")
                },
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kCubicImageShader_BuiltinStageID] = new FragmentStage(
                "CubicImageShader",
                kLocalCoords_ReqFlag,
                "arc_cubic_image_shader",
                new String[]{
                        PRIV_TILE, PRIV_SAMPLE_CUBIC_IMAGE_SUBSET,
                        PRIV_CUBIC_FILTER_IMAGE, ARC_CUBIC_IMAGE_SHADER
                },
                new Uniform[]{
                        SUBSET,
                        new Uniform(SLDataType.kFloat4x4, "u_CubicCoeffs"),
                        new Uniform(SLDataType.kInt, "u_CubicClamp"),
                        TILE_MODE_X, TILE_MODE_Y
                },
                new Sampler[]{
                        new Sampler(SLDataType.kSampler2D, "u_Sampler")
                },
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kHWImageShader_BuiltinStageID] = new FragmentStage(
                "HardwareImageShader",
                kLocalCoords_ReqFlag,
                "arc_hw_image_shader",
                new String[]{
                        ARC_HW_IMAGE_SHADER
                },
                new Uniform[]{
                        INV_IMAGE_SIZE
                },
                new Sampler[]{
                        new Sampler(SLDataType.kSampler2D, "u_Sampler")
                },
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kDitherShader_BuiltinStageID] = new FragmentStage(
                "DitherShader",
                kPriorStageOutput_ReqFlag,
                "arc_dither_shader",
                new String[]{
                        ARC_DITHER_SHADER
                },
                new Uniform[]{
                        new Uniform(SLDataType.kFloat, "u_Range")
                },
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kColorSpaceXformColorFilter_BuiltinStageID] = new FragmentStage(
                "ColorSpaceTransform",
                kPriorStageOutput_ReqFlag,
                "arc_color_space_transform",
                new String[]{
                        PRIV_TRANSFER_FUNCTION, PRIV_INV_TRANSFER_FUNCTION,
                        ARC_COLOR_SPACE_TRANSFORM
                },
                new Uniform[]{
                        XFORM_FLAGS, XFORM_SRC_TF, XFORM_GAMUT_TRANSFORM, XFORM_DST_TF
                },
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kBlend_BuiltinStageID] = new FragmentStage(
                "Blend",
                kNone_ReqFlag,
                "Blend",
                NO_FUNCTIONS,
                NO_UNIFORMS,
                NO_SAMPLERS,
                ShaderCodeSource::generateComposeExpression,
                3
        );
        mBuiltinCodeSnippets[kBlendModeBlender_BuiltinStageID] = new FragmentStage(
                "BlendModeBlender",
                kPriorStageOutput_ReqFlag | kBlenderDstColor_ReqFlag,
                "arc_blend",
                new String[]{
                        PRIV_BLEND_GET_LUM, PRIV_BLEND_SET_LUM, PRIV_BLEND_SET_LUM_SAT,
                        BLEND_CLEAR, BLEND_SRC, BLEND_DST, BLEND_SRC_OVER, BLEND_DST_OVER,
                        BLEND_SRC_IN, BLEND_DST_IN, BLEND_SRC_OUT, BLEND_DST_OUT,
                        BLEND_SRC_ATOP, BLEND_DST_ATOP, BLEND_XOR, BLEND_PLUS, BLEND_PLUS_CLAMPED,
                        BLEND_MINUS, BLEND_MINUS_CLAMPED, BLEND_MODULATE, BLEND_MULTIPLY,
                        BLEND_SCREEN, BLEND_OVERLAY, BLEND_DARKEN, BLEND_LIGHTEN,
                        BLEND_COLOR_DODGE, BLEND_COLOR_BURN, BLEND_HARD_LIGHT, BLEND_SOFT_LIGHT,
                        BLEND_DIFFERENCE, BLEND_EXCLUSION, BLEND_SUBTRACT, BLEND_DIVIDE,
                        BLEND_LINEAR_DODGE, BLEND_LINEAR_BURN, BLEND_VIVID_LIGHT, BLEND_LINEAR_LIGHT,
                        BLEND_PIN_LIGHT, BLEND_HARD_MIX, BLEND_DARKER_COLOR, BLEND_LIGHTER_COLOR,
                        BLEND_HUE, BLEND_SATURATION, BLEND_COLOR, BLEND_LUMINOSITY,
                        ARC_BLEND
                },
                new Uniform[]{
                        new Uniform(SLDataType.kInt, "u_BlendMode")
                },
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kPrimitiveColor_BuiltinStageID] = new FragmentStage(
                "PrimitiveColor",
                kPrimitiveColor_ReqFlag,
                "arc_color_space_transform",
                new String[]{
                        PRIV_TRANSFER_FUNCTION, PRIV_INV_TRANSFER_FUNCTION,
                        ARC_COLOR_SPACE_TRANSFORM
                },
                new Uniform[]{
                        XFORM_FLAGS, XFORM_SRC_TF, XFORM_GAMUT_TRANSFORM, XFORM_DST_TF
                },
                NO_SAMPLERS,
                ShaderCodeSource::generateDefaultExpression,
                0
        );
        mBuiltinCodeSnippets[kCompose_BuiltinStageID] = new FragmentStage(
                "Compose",
                kNone_ReqFlag,
                "Compose",
                NO_FUNCTIONS,
                NO_UNIFORMS,
                NO_SAMPLERS,
                ShaderCodeSource::generateComposeExpression,
                2
        );
        mBuiltinCodeSnippets[kInlineSrcOverBlend_BuiltinStageID] = new FragmentStage(
                "SrcOver",
                kPriorStageOutput_ReqFlag | kBlenderDstColor_ReqFlag,
                "blend_src_over",
                NO_FUNCTIONS,
                NO_UNIFORMS,
                NO_SAMPLERS,
                (node, localCoords, priorStageOutput, blenderDstColor, output, code) -> {
                    assert node.codeID() == kInlineSrcOverBlend_BuiltinStageID;
                    code.format("""
                            %1$s = %2$s + %3$s * (1 - %2$s.a);
                            """, output, priorStageOutput, blenderDstColor);
                },
                0
        );
        mBuiltinCodeSnippets[kInlineSrcInBlend_BuiltinStageID] = new FragmentStage(
                "SrcIn",
                kPriorStageOutput_ReqFlag | kBlenderDstColor_ReqFlag,
                "blend_src_in",
                NO_FUNCTIONS,
                NO_UNIFORMS,
                NO_SAMPLERS,
                (node, localCoords, priorStageOutput, blenderDstColor, output, code) -> {
                    assert node.codeID() == kInlineSrcInBlend_BuiltinStageID;
                    code.format("""
                            %1$s = %2$s * %3$s.a;
                            """, output, priorStageOutput, blenderDstColor);
                },
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

    public static String invoke_node(FragmentNode node,
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

    public static String invoke_child(FragmentNode child,
                                      String localCoords,
                                      String priorStageOutput,
                                      String blenderDstColor,
                                      FragmentNode outputNode,
                                      String outputBaseName,
                                      byte outputType,
                                      Formatter code) {
        String output = getMangledName(outputBaseName, outputNode.stageIndex());
        code.format("""
                        // [%d] %s
                        %s %s;
                        """,
                child.stageIndex(), child.stage().mName,
                SLDataType.typeString(outputType), output);
        code.format("{\n");
        child.stage().mExpressionGenerator.generate(
                child,
                localCoords,
                priorStageOutput,
                blenderDstColor,
                output,
                code
        );
        code.format("}\n");
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
        if ((stage.mRequirementFlags & kLocalCoords_ReqFlag) != 0) {
            args.add(localCoords);
        }

        // Append prior-stage output color.
        if ((stage.mRequirementFlags & kPriorStageOutput_ReqFlag) != 0) {
            args.add(priorStageOutput);
        }

        // Append blender destination color.
        if ((stage.mRequirementFlags & kBlenderDstColor_ReqFlag) != 0) {
            args.add(blenderDstColor);
        }

        // Special variables and/or "global" scope variables that have to propagate
        // through the node tree.
        if ((stage.mRequirementFlags & kPrimitiveColor_ReqFlag) != 0) {
            args.add(PipelineBuilder.PRIMITIVE_COLOR_VAR_NAME);
        }

        // Append uniform names.
        for (var uniform : stage.mUniforms) {
            if (uniform.name().startsWith(UniformHandler.NO_MANGLE_PREFIX)) {
                args.add(uniform.name());
            } else {
                args.add(
                        getMangledName(uniform.name(), node.stageIndex())
                );
            }
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
                arguments.add(
                        invoke_child(child, localCoords, priorStageOutput, blenderDstColor, child,
                                "outColor",
                                SLDataType.kFloat4,
                                code)
                );
            }
        }
        code.format("""
                %s = %s(%s);
                """, output, node.stage().mStaticFunctionName, arguments);
    }

    private static void generateComposeExpression(FragmentNode node,
                                                  String localCoords,
                                                  String priorStageOutput,
                                                  String blenderDstColor,
                                                  String output,
                                                  Formatter code) {
        assert node.numChildren() >= 2;

        FragmentNode outer = node.childAt(node.numChildren() - 1);

        String outerLocalCoords = localCoords;
        String outerPriorStageOutput = priorStageOutput;
        String outerBlenderDstColor = blenderDstColor;

        code.format("{\n");

        int childIndex = 0;
        if ((outer.requirementFlags() & kLocalCoords_ReqFlag) != 0) {
            var child = node.childAt(childIndex++);
            outerLocalCoords = invoke_child(child, localCoords, priorStageOutput, blenderDstColor, node,
                    "outerLocalCoords",
                    SLDataType.kFloat2,
                    code);
        }
        if ((outer.requirementFlags() & kPriorStageOutput_ReqFlag) != 0) {
            var child = node.childAt(childIndex++);
            outerPriorStageOutput = invoke_child(child, localCoords, priorStageOutput, blenderDstColor, node,
                    "outerPriorStageOutput",
                    SLDataType.kFloat4,
                    code);
        }
        if ((outer.requirementFlags() & kBlenderDstColor_ReqFlag) != 0) {
            var child = node.childAt(childIndex++);
            outerBlenderDstColor = invoke_child(child, localCoords, priorStageOutput, blenderDstColor, node,
                    "outerBlenderDstColor",
                    SLDataType.kFloat4,
                    code);
        }
        assert childIndex + 1 == node.numChildren();

        code.format("// [%d] %s\n", outer.stageIndex(), outer.stage().mName);
        outer.stage().mExpressionGenerator.generate(
                outer,
                outerLocalCoords,
                outerPriorStageOutput,
                outerBlenderDstColor,
                output,
                code
        );

        code.format("}\n");
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
