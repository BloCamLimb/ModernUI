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

package icyllis.arc3d.test;

import icyllis.arc3d.compiler.*;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.ByteBuffer;
import java.util.Objects;

import static org.lwjgl.util.shaderc.Shaderc.*;

@Fork(2)
@Threads(2)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@State(Scope.Thread)
public class CompilerBenchmark {

    public static final String SOURCE_AKSL = """
            #version 450 core
            layout(std140, binding = 1) uniform FragmentUniforms {
            layout(offset=0) mat3 u_LocalMatrix_1;
            layout(offset=48) vec4 u_Colors_3[4];
            layout(offset=112) vec4 u_Offsets_3;
            layout(offset=128) int u_TileModeX_3;
            layout(offset=132) int u_ColorSpace_3;
            layout(offset=136) int u_DoUnpremul_3;
            layout(offset=140) int u_XformFlags_4;
            layout(offset=144) vec4 u_XformSrcTf_4[2];
            layout(offset=176) mat3 u_XformGamutTransform_4;
            layout(offset=224) vec4 u_XformDstTf_4[2];
            layout(offset=256) float u_Range_5;
            };
            layout(builtin=frag_coord) in vec4 gl_FragCoord;
            layout(location = 0) in vec2 f_ArcEdge;
            layout(location = 1) flat in vec2 f_Span;
            layout(location = 2) flat in vec3 f_Radii;
            layout(location = 3) in vec2 f_LocalCoords;
            layout(location=0) out vec4 FragColor0;
            vec2 _tile_grad(int tileMode, vec2 t) {
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
            vec2 _linear_grad_layout(vec2 pos) {
                return vec2(pos.x + 0.00001, 1);
            }
            vec4 _colorize_grad_4(vec4 colors[4], vec4 offsets, vec2 t) {
                vec4 result;
                if (t.y < 0.0) {
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
            vec3 _css_lab_to_xyz(vec3 lab) {
                const float B = 841.0 / 108.0;
                const float C = 4.0 / 29.0;
                const float D = 6.0 / 29.0;
            
                vec3 f;
                f[1] = (lab[0] + 16.0) / 116.0;
                f[0] = f[1] + (lab[1] * 0.002);
                f[2] = f[1] - (lab[2] * 0.005);
            
                vec3 xyz = mix((1.0 / B) * (f - C), pow(f, vec3(3)), greaterThan(f, vec3(D)));
            
                const vec3 D50 = vec3(0.964212, 1.0, 0.825188);
                return xyz * D50;
            }
            vec3 _css_hcl_to_lab(vec3 hcl) {
                return vec3(
                    hcl[2],
                    hcl[1] * cos(radians(hcl[0])),
                    hcl[1] * sin(radians(hcl[0]))
                );
            }
            vec3 _css_oklab_to_linear_srgb(vec3 oklab) {
                float l_ = oklab.x + 0.3963377774 * oklab.y + 0.2158037573 * oklab.z,
                      m_ = oklab.x - 0.1055613458 * oklab.y - 0.0638541728 * oklab.z,
                      s_ = oklab.x - 0.0894841775 * oklab.y - 1.2914855480 * oklab.z;
            
                float l = l_*l_*l_,
                      m = m_*m_*m_,
                      s = s_*s_*s_;
            
                return vec3(
                    +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
                    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
                    -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
                );
            }
            vec3 _css_oklab_gamut_map_to_linear_srgb(vec3 oklab) {
                // Constants for the normal vector of the plane formed by white, black, and
                // the specified vertex of the gamut.
                const vec2 normal_R = vec2(0.409702, -0.912219);
                const vec2 normal_M = vec2(-0.397919, -0.917421);
                const vec2 normal_B = vec2(-0.906800, 0.421562);
                const vec2 normal_C = vec2(-0.171122, 0.985250);
                const vec2 normal_G = vec2(0.460276, 0.887776);
                const vec2 normal_Y = vec2(0.947925, 0.318495);
            
                // For the triangles formed by white (W) or black (K) with the vertices
                // of Yellow and Red (YR), Red and Magenta (RM), etc, the constants to be
                // used to compute the intersection of a line of constant hue and luminance
                // with that plane.
                const float c0_YR = 0.091132;
                const vec2 cW_YR = vec2(0.070370, 0.034139);
                const vec2 cK_YR = vec2(0.018170, 0.378550);
                const float c0_RM = 0.113902;
                const vec2 cW_RM = vec2(0.090836, 0.036251);
                const vec2 cK_RM = vec2(0.226781, 0.018764);
                const float c0_MB = 0.161739;
                const vec2 cW_MB = vec2(-0.008202, -0.264819);
                const vec2 cK_MB = vec2( 0.187156, -0.284304);
                const float c0_BC = 0.102047;
                const vec2 cW_BC = vec2(-0.014804, -0.162608);
                const vec2 cK_BC = vec2(-0.276786,  0.004193);
                const float c0_CG = 0.092029;
                const vec2 cW_CG = vec2(-0.038533, -0.001650);
                const vec2 cK_CG = vec2(-0.232572, -0.094331);
                const float c0_GY = 0.081709;
                const vec2 cW_GY = vec2(-0.034601, -0.002215);
                const vec2 cK_GY = vec2( 0.012185,  0.338031);
            
                vec2 ab = oklab.yz;
            
                // Find the planes to intersect with and set the constants based on those
                // planes.
                float c0;
                vec2 cW;
                vec2 cK;
                if (dot(ab, normal_R) < 0.0) {
                    if (dot(ab, normal_G) < 0.0) {
                        if (dot(ab, normal_C) < 0.0) {
                            c0 = c0_BC; cW = cW_BC; cK = cK_BC;
                        } else {
                            c0 = c0_CG; cW = cW_CG; cK = cK_CG;
                        }
                    } else {
                        if (dot(ab, normal_Y) < 0.0) {
                            c0 = c0_GY; cW = cW_GY; cK = cK_GY;
                        } else {
                            c0 = c0_YR; cW = cW_YR; cK = cK_YR;
                        }
                    }
                } else {
                    if (dot(ab, normal_B) < 0.0) {
                        if (dot(ab, normal_M) < 0.0) {
                            c0 = c0_RM; cW = cW_RM; cK = cK_RM;
                        } else {
                            c0 = c0_MB; cW = cW_MB; cK = cK_MB;
                        }
                    } else {
                        c0 = c0_BC; cW = cW_BC; cK = cK_BC;
                    }
                }
            
                // Perform the intersection.
                float alpha = 1.0;
            
                // Intersect with the plane with white.
                float w_denom = dot(cW, ab);
                if (w_denom > 0.0) {
                    float one_minus_L = 1.0 - oklab.r;
                    float w_num = c0*one_minus_L;
                    if (w_num < w_denom) {
                        alpha = min(alpha, w_num / w_denom);
                    }
                }
            
                // Intersect with the plane with black.
                float k_denom = dot(cK, ab);
                if (k_denom > 0.0) {
                    float L = oklab.r;
                    float k_num = c0*L;
                    if (k_num < k_denom) {
                        alpha = min(alpha,  k_num / k_denom);
                    }
                }
            
                // Attenuate the ab coordinate by alpha.
                oklab.yz *= alpha;
            
                return _css_oklab_to_linear_srgb(oklab);
            }
            vec3 _css_hsl_to_srgb(vec3 hsl) {
                hsl.x = mod(hsl.x, 360.0);
                if (hsl.x < 0.0) {
                    hsl.x += 360.0;
                }
            
                hsl.yz /= 100.0;
            
                vec3 k = mod(vec3(0, 8, 4) + hsl.x/30.0, 12.0);
                float a = hsl.y * min(hsl.z, 1.0 - hsl.z);
                return hsl.z - a * clamp(min(k - 3.0, 9.0 - k), -1.0, 1.0);
            }
            vec3 _css_hwb_to_srgb(vec3 hwb) {
                vec3 rgb;
                hwb.yz /= 100.0;
                if (hwb.y + hwb.z >= 1.0) {
                    // grayscale
                    rgb = vec3(hwb.y / (hwb.y + hwb.z));
                } else {
                    rgb = _css_hsl_to_srgb(vec3(hwb.x, 100, 50));
                    rgb *= (1.0 - hwb.y - hwb.z);
                    rgb += hwb.y;
                }
                return rgb;
            }
            vec4 _interpolated_to_rgb_unpremul(vec4 color, int colorSpace, int doUnpremul) {
                const int kDestination   = 0;
                const int kSRGB          = 1;
                const int kSRGBLinear    = 2;
                const int kLab           = 3;
                const int kOKLab         = 4;
                const int kOKLabGamutMap = 5;
                const int kHSL           = 6;
                const int kHWB           = 7;
                const int kLCH           = 8;
                const int kOKLCH         = 9;
                const int kOKLCHGamutMap = 10;
            
                if (bool(doUnpremul)) {
                    switch (colorSpace) {
                        case kLab:
                        case kOKLab:
                        case kOKLabGamutMap: color.rgb /= max(color.a, 1e-7); break;
                        case kHSL:
                        case kHWB:
                        case kLCH:
                        case kOKLCH:
                        case kOKLCHGamutMap: color.gb /= max(color.a, 1e-7); break;
                    }
                }
                switch (colorSpace) {
                    case kLab:
                        color.rgb = _css_lab_to_xyz(color.rgb);
                        break;
                    case kOKLab:
                        color.rgb = _css_oklab_to_linear_srgb(color.rgb);
                        break;
                    case kOKLabGamutMap:
                        color.rgb = _css_oklab_gamut_map_to_linear_srgb(color.rgb);
                        break;
                    case kHSL:
                        color.rgb = _css_hsl_to_srgb(color.rgb);
                        break;
                    case kHWB:
                        color.rgb = _css_hwb_to_srgb(color.rgb);
                        break;
                    case kLCH:
                        color.rgb = _css_lab_to_xyz(_css_hcl_to_lab(color.rgb));
                        break;
                    case kOKLCH:
                        color.rgb = _css_oklab_to_linear_srgb(_css_hcl_to_lab(color.rgb));
                        break;
                    case kOKLCHGamutMap:
                        color.rgb = _css_oklab_gamut_map_to_linear_srgb(_css_hcl_to_lab(color.rgb));
                        break;
                }
                return color;
            }
            vec4 arc_linear_grad_4_shader(vec2 coords,
                                          vec4 colors[4],
                                          vec4 offsets,
                                          int tileMode,
                                          int colorSpace,
                                          int doUnpremul) {
                vec2 t = _linear_grad_layout(coords);
                t = _tile_grad(tileMode, t);
                vec4 color = _colorize_grad_4(colors, offsets, t);
                return _interpolated_to_rgb_unpremul(color, colorSpace, doUnpremul);
            }
            float _transfer_function(float x, vec4 tf[2]) {
                float G = tf[0][0], A = tf[0][1], B = tf[0][2], C = tf[0][3],
                      D = tf[1][0], E = tf[1][1], F = tf[1][2];
                float s = sign(x);
                x = abs(x);
                x = mix(pow(A * x + B, G) + E, (C * x) + F, x < D);
                return s * x;
            }
            float _inv_transfer_function(float x, vec4 tf[2]) {
                float G = tf[0][0], A = tf[0][1], B = tf[0][2], C = tf[0][3],
                      D = tf[1][0], E = tf[1][1], F = tf[1][2];
                float s = sign(x);
                x = abs(x);
                x = mix((pow(x - E, 1.0 / G) - B) / A, (x - F) / C, x < D * C);
                return s * x;
            }
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
                    color.r = _transfer_function(color.r, srcTf);
                    color.g = _transfer_function(color.g, srcTf);
                    color.b = _transfer_function(color.b, srcTf);
                }
                if (bool(flags & kColorSpaceXformFlagGamutTransform)) {
                    color.rgb = gamutTransform * color.rgb;
                }
                if (bool(flags & kColorSpaceXformFlagEncode)) {
                    color.r = _inv_transfer_function(color.r, dstTf);
                    color.g = _inv_transfer_function(color.g, dstTf);
                    color.b = _inv_transfer_function(color.b, dstTf);
                }
            
                if (bool(flags & kColorSpaceXformFlagPremul)) {
                    color.rgb *= color.a;
                }
                return color;
            }
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
            void main() {
            // [0] Compose
            vec4 outColor_0;
            {
            // [1] LocalMatrixShader
            vec4 outerPriorStageOutput_0;
            {
            {
            vec3 newPerspCoords_1 = u_LocalMatrix_1 * vec3(f_LocalCoords, 1);
            vec2 newLocalCoords_1 = newPerspCoords_1.xy / newPerspCoords_1.z;
            {
            // [3] LinearGradient4
            vec4 outerPriorStageOutput_2;
            {
            outerPriorStageOutput_2 = arc_linear_grad_4_shader(newLocalCoords_1,u_Colors_3,u_Offsets_3,u_TileModeX_3,u_ColorSpace_3,u_DoUnpremul_3);
            }
            // [4] ColorSpaceTransform
            outerPriorStageOutput_0 = arc_color_space_transform(outerPriorStageOutput_2,u_XformFlags_4,u_XformSrcTf_4,u_XformGamutTransform_4,u_XformDstTf_4);
            }
            }
            }
            // [5] DitherShader
            outColor_0 = arc_dither_shader(outerPriorStageOutput_0,u_Range_5);
            }
            vec4 outputCoverage;
            vec3 radii = f_Radii.xyz;
            vec2 q = f_ArcEdge;
            vec2 cs = f_Span;
            q.x = abs(q.x);
            float l = length(q) - radii.x;
            float m = length(q - cs.yx * clamp(dot(q,cs.yx), 0.0, radii.x));
            float dis = max(l, m * sign(cs.x*q.x - cs.y*q.y));
            dis = mix(dis, abs(dis - radii.z) - radii.y, radii.y >= 0.0);
            float afwidth = fwidth(dis);
            float edgeAlpha = 1.0 - clamp(dis/afwidth+0.5, 0.0, 1.0);
            outputCoverage = vec4(edgeAlpha);
            FragColor0 = outColor_0 * outputCoverage;
            }
            """;
    public static final String SOURCE_GLSL = """
            #version 450 core
            layout(std140, binding = 1) uniform FragmentUniforms {
            layout(offset=0) mat3 u_LocalMatrix_1;
            layout(offset=48) vec4 u_Colors_3[4];
            layout(offset=112) vec4 u_Offsets_3;
            layout(offset=128) int u_TileModeX_3;
            layout(offset=132) int u_ColorSpace_3;
            layout(offset=136) int u_DoUnpremul_3;
            layout(offset=140) int u_XformFlags_4;
            layout(offset=144) vec4 u_XformSrcTf_4[2];
            layout(offset=176) mat3 u_XformGamutTransform_4;
            layout(offset=224) vec4 u_XformDstTf_4[2];
            layout(offset=256) float u_Range_5;
            };
            layout(location = 0) in vec2 f_ArcEdge;
            layout(location = 1) flat in vec2 f_Span;
            layout(location = 2) flat in vec3 f_Radii;
            layout(location = 3) in vec2 f_LocalCoords;
            layout(location=0) out vec4 FragColor0;
            vec2 _tile_grad(int tileMode, vec2 t) {
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
            vec2 _linear_grad_layout(vec2 pos) {
                return vec2(pos.x + 0.00001, 1);
            }
            vec4 _colorize_grad_4(vec4 colors[4], vec4 offsets, vec2 t) {
                vec4 result;
                if (t.y < 0.0) {
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
            vec3 _css_lab_to_xyz(vec3 lab) {
                const float B = 841.0 / 108.0;
                const float C = 4.0 / 29.0;
                const float D = 6.0 / 29.0;
            
                vec3 f;
                f[1] = (lab[0] + 16.0) / 116.0;
                f[0] = f[1] + (lab[1] * 0.002);
                f[2] = f[1] - (lab[2] * 0.005);
            
                vec3 xyz = mix((1.0 / B) * (f - C), pow(f, vec3(3)), greaterThan(f, vec3(D)));
            
                const vec3 D50 = vec3(0.964212, 1.0, 0.825188);
                return xyz * D50;
            }
            vec3 _css_hcl_to_lab(vec3 hcl) {
                return vec3(
                    hcl[2],
                    hcl[1] * cos(radians(hcl[0])),
                    hcl[1] * sin(radians(hcl[0]))
                );
            }
            vec3 _css_oklab_to_linear_srgb(vec3 oklab) {
                float l_ = oklab.x + 0.3963377774 * oklab.y + 0.2158037573 * oklab.z,
                      m_ = oklab.x - 0.1055613458 * oklab.y - 0.0638541728 * oklab.z,
                      s_ = oklab.x - 0.0894841775 * oklab.y - 1.2914855480 * oklab.z;
            
                float l = l_*l_*l_,
                      m = m_*m_*m_,
                      s = s_*s_*s_;
            
                return vec3(
                    +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
                    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
                    -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
                );
            }
            vec3 _css_oklab_gamut_map_to_linear_srgb(vec3 oklab) {
                // Constants for the normal vector of the plane formed by white, black, and
                // the specified vertex of the gamut.
                const vec2 normal_R = vec2(0.409702, -0.912219);
                const vec2 normal_M = vec2(-0.397919, -0.917421);
                const vec2 normal_B = vec2(-0.906800, 0.421562);
                const vec2 normal_C = vec2(-0.171122, 0.985250);
                const vec2 normal_G = vec2(0.460276, 0.887776);
                const vec2 normal_Y = vec2(0.947925, 0.318495);
            
                // For the triangles formed by white (W) or black (K) with the vertices
                // of Yellow and Red (YR), Red and Magenta (RM), etc, the constants to be
                // used to compute the intersection of a line of constant hue and luminance
                // with that plane.
                const float c0_YR = 0.091132;
                const vec2 cW_YR = vec2(0.070370, 0.034139);
                const vec2 cK_YR = vec2(0.018170, 0.378550);
                const float c0_RM = 0.113902;
                const vec2 cW_RM = vec2(0.090836, 0.036251);
                const vec2 cK_RM = vec2(0.226781, 0.018764);
                const float c0_MB = 0.161739;
                const vec2 cW_MB = vec2(-0.008202, -0.264819);
                const vec2 cK_MB = vec2( 0.187156, -0.284304);
                const float c0_BC = 0.102047;
                const vec2 cW_BC = vec2(-0.014804, -0.162608);
                const vec2 cK_BC = vec2(-0.276786,  0.004193);
                const float c0_CG = 0.092029;
                const vec2 cW_CG = vec2(-0.038533, -0.001650);
                const vec2 cK_CG = vec2(-0.232572, -0.094331);
                const float c0_GY = 0.081709;
                const vec2 cW_GY = vec2(-0.034601, -0.002215);
                const vec2 cK_GY = vec2( 0.012185,  0.338031);
            
                vec2 ab = oklab.yz;
            
                // Find the planes to intersect with and set the constants based on those
                // planes.
                float c0;
                vec2 cW;
                vec2 cK;
                if (dot(ab, normal_R) < 0.0) {
                    if (dot(ab, normal_G) < 0.0) {
                        if (dot(ab, normal_C) < 0.0) {
                            c0 = c0_BC; cW = cW_BC; cK = cK_BC;
                        } else {
                            c0 = c0_CG; cW = cW_CG; cK = cK_CG;
                        }
                    } else {
                        if (dot(ab, normal_Y) < 0.0) {
                            c0 = c0_GY; cW = cW_GY; cK = cK_GY;
                        } else {
                            c0 = c0_YR; cW = cW_YR; cK = cK_YR;
                        }
                    }
                } else {
                    if (dot(ab, normal_B) < 0.0) {
                        if (dot(ab, normal_M) < 0.0) {
                            c0 = c0_RM; cW = cW_RM; cK = cK_RM;
                        } else {
                            c0 = c0_MB; cW = cW_MB; cK = cK_MB;
                        }
                    } else {
                        c0 = c0_BC; cW = cW_BC; cK = cK_BC;
                    }
                }
            
                // Perform the intersection.
                float alpha = 1.0;
            
                // Intersect with the plane with white.
                float w_denom = dot(cW, ab);
                if (w_denom > 0.0) {
                    float one_minus_L = 1.0 - oklab.r;
                    float w_num = c0*one_minus_L;
                    if (w_num < w_denom) {
                        alpha = min(alpha, w_num / w_denom);
                    }
                }
            
                // Intersect with the plane with black.
                float k_denom = dot(cK, ab);
                if (k_denom > 0.0) {
                    float L = oklab.r;
                    float k_num = c0*L;
                    if (k_num < k_denom) {
                        alpha = min(alpha,  k_num / k_denom);
                    }
                }
            
                // Attenuate the ab coordinate by alpha.
                oklab.yz *= alpha;
            
                return _css_oklab_to_linear_srgb(oklab);
            }
            vec3 _css_hsl_to_srgb(vec3 hsl) {
                hsl.x = mod(hsl.x, 360.0);
                if (hsl.x < 0.0) {
                    hsl.x += 360.0;
                }
            
                hsl.yz /= 100.0;
            
                vec3 k = mod(vec3(0, 8, 4) + hsl.x/30.0, 12.0);
                float a = hsl.y * min(hsl.z, 1.0 - hsl.z);
                return hsl.z - a * clamp(min(k - 3.0, 9.0 - k), -1.0, 1.0);
            }
            vec3 _css_hwb_to_srgb(vec3 hwb) {
                vec3 rgb;
                hwb.yz /= 100.0;
                if (hwb.y + hwb.z >= 1.0) {
                    // grayscale
                    rgb = vec3(hwb.y / (hwb.y + hwb.z));
                } else {
                    rgb = _css_hsl_to_srgb(vec3(hwb.x, 100, 50));
                    rgb *= (1.0 - hwb.y - hwb.z);
                    rgb += hwb.y;
                }
                return rgb;
            }
            vec4 _interpolated_to_rgb_unpremul(vec4 color, int colorSpace, int doUnpremul) {
                const int kDestination   = 0;
                const int kSRGB          = 1;
                const int kSRGBLinear    = 2;
                const int kLab           = 3;
                const int kOKLab         = 4;
                const int kOKLabGamutMap = 5;
                const int kHSL           = 6;
                const int kHWB           = 7;
                const int kLCH           = 8;
                const int kOKLCH         = 9;
                const int kOKLCHGamutMap = 10;
            
                if (bool(doUnpremul)) {
                    switch (colorSpace) {
                        case kLab:
                        case kOKLab:
                        case kOKLabGamutMap: color.rgb /= max(color.a, 1e-7); break;
                        case kHSL:
                        case kHWB:
                        case kLCH:
                        case kOKLCH:
                        case kOKLCHGamutMap: color.gb /= max(color.a, 1e-7); break;
                    }
                }
                switch (colorSpace) {
                    case kLab:
                        color.rgb = _css_lab_to_xyz(color.rgb);
                        break;
                    case kOKLab:
                        color.rgb = _css_oklab_to_linear_srgb(color.rgb);
                        break;
                    case kOKLabGamutMap:
                        color.rgb = _css_oklab_gamut_map_to_linear_srgb(color.rgb);
                        break;
                    case kHSL:
                        color.rgb = _css_hsl_to_srgb(color.rgb);
                        break;
                    case kHWB:
                        color.rgb = _css_hwb_to_srgb(color.rgb);
                        break;
                    case kLCH:
                        color.rgb = _css_lab_to_xyz(_css_hcl_to_lab(color.rgb));
                        break;
                    case kOKLCH:
                        color.rgb = _css_oklab_to_linear_srgb(_css_hcl_to_lab(color.rgb));
                        break;
                    case kOKLCHGamutMap:
                        color.rgb = _css_oklab_gamut_map_to_linear_srgb(_css_hcl_to_lab(color.rgb));
                        break;
                }
                return color;
            }
            vec4 arc_linear_grad_4_shader(vec2 coords,
                                          vec4 colors[4],
                                          vec4 offsets,
                                          int tileMode,
                                          int colorSpace,
                                          int doUnpremul) {
                vec2 t = _linear_grad_layout(coords);
                t = _tile_grad(tileMode, t);
                vec4 color = _colorize_grad_4(colors, offsets, t);
                return _interpolated_to_rgb_unpremul(color, colorSpace, doUnpremul);
            }
            float _transfer_function(float x, vec4 tf[2]) {
                float G = tf[0][0], A = tf[0][1], B = tf[0][2], C = tf[0][3],
                      D = tf[1][0], E = tf[1][1], F = tf[1][2];
                float s = sign(x);
                x = abs(x);
                x = mix(pow(A * x + B, G) + E, (C * x) + F, x < D);
                return s * x;
            }
            float _inv_transfer_function(float x, vec4 tf[2]) {
                float G = tf[0][0], A = tf[0][1], B = tf[0][2], C = tf[0][3],
                      D = tf[1][0], E = tf[1][1], F = tf[1][2];
                float s = sign(x);
                x = abs(x);
                x = mix((pow(x - E, 1.0 / G) - B) / A, (x - F) / C, x < D * C);
                return s * x;
            }
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
                    color.r = _transfer_function(color.r, srcTf);
                    color.g = _transfer_function(color.g, srcTf);
                    color.b = _transfer_function(color.b, srcTf);
                }
                if (bool(flags & kColorSpaceXformFlagGamutTransform)) {
                    color.rgb = gamutTransform * color.rgb;
                }
                if (bool(flags & kColorSpaceXformFlagEncode)) {
                    color.r = _inv_transfer_function(color.r, dstTf);
                    color.g = _inv_transfer_function(color.g, dstTf);
                    color.b = _inv_transfer_function(color.b, dstTf);
                }
            
                if (bool(flags & kColorSpaceXformFlagPremul)) {
                    color.rgb *= color.a;
                }
                return color;
            }
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
            void main() {
            // [0] Compose
            vec4 outColor_0;
            {
            // [1] LocalMatrixShader
            vec4 outerPriorStageOutput_0;
            {
            {
            vec3 newPerspCoords_1 = u_LocalMatrix_1 * vec3(f_LocalCoords, 1);
            vec2 newLocalCoords_1 = newPerspCoords_1.xy / newPerspCoords_1.z;
            {
            // [3] LinearGradient4
            vec4 outerPriorStageOutput_2;
            {
            outerPriorStageOutput_2 = arc_linear_grad_4_shader(newLocalCoords_1,u_Colors_3,u_Offsets_3,u_TileModeX_3,u_ColorSpace_3,u_DoUnpremul_3);
            }
            // [4] ColorSpaceTransform
            outerPriorStageOutput_0 = arc_color_space_transform(outerPriorStageOutput_2,u_XformFlags_4,u_XformSrcTf_4,u_XformGamutTransform_4,u_XformDstTf_4);
            }
            }
            }
            // [5] DitherShader
            outColor_0 = arc_dither_shader(outerPriorStageOutput_0,u_Range_5);
            }
            vec4 outputCoverage;
            vec3 radii = f_Radii.xyz;
            vec2 q = f_ArcEdge;
            vec2 cs = f_Span;
            q.x = abs(q.x);
            float l = length(q) - radii.x;
            float m = length(q - cs.yx * clamp(dot(q,cs.yx), 0.0, radii.x));
            float dis = max(l, m * sign(cs.x*q.x - cs.y*q.y));
            dis = mix(dis, abs(dis - radii.z) - radii.y, radii.y >= 0.0);
            float afwidth = fwidth(dis);
            float edgeAlpha = 1.0 - clamp(dis/afwidth+0.5, 0.0, 1.0);
            outputCoverage = vec4(edgeAlpha);
            FragColor0 = outColor_0 * outputCoverage;
            }
            """;

    public static final ModuleUnit COMMON_MODULE = ModuleLoader.getInstance().loadCommonModule(new ShaderCompiler());

    public static final ByteBuffer SOURCE_BUFFER = MemoryUtil.memUTF8(SOURCE_GLSL, false);
    public static final ByteBuffer FILE_NAME_BUFFER = MemoryUtil.memUTF8("file");
    public static final ByteBuffer ENTRY_NAME_BUFFER = MemoryUtil.memUTF8("main");

    // the benchmark shows that our compiler is 6x to 32x faster than glslang
    public static void main(String[] args) throws RunnerException {
        new Runner(new OptionsBuilder()
                .include(CompilerBenchmark.class.getSimpleName())
                .jvmArgs("-XX:+UseZGC", "-XX:+ZGenerational")
                .shouldFailOnError(true).shouldDoGC(true)
                .build())
                .run();
    }

    @Benchmark
    public static void shaderc(Blackhole blackhole) {
        long compiler = Shaderc.shaderc_compiler_initialize();
        long options = Shaderc.shaderc_compile_options_initialize();
        shaderc_compile_options_set_target_env(options, shaderc_target_env_opengl, shaderc_env_version_opengl_4_5);
        shaderc_compile_options_set_target_spirv(options, shaderc_spirv_version_1_0);
        shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_zero);
        long result = Shaderc.shaderc_compile_into_spv(compiler, SOURCE_BUFFER, Shaderc.shaderc_fragment_shader,
                FILE_NAME_BUFFER, ENTRY_NAME_BUFFER, options);
        ByteBuffer spirv = Shaderc.shaderc_result_get_bytes(result);
        blackhole.consume(Objects.requireNonNull(spirv));
        Shaderc.shaderc_result_release(result);
        Shaderc.shaderc_compile_options_release(options);
        Shaderc.shaderc_compiler_release(compiler);
    }

    @Benchmark
    public static void arc3d(Blackhole blackhole) {
        ShaderCompiler compiler = new ShaderCompiler();
        ByteBuffer spirv = compiler.compileIntoSPIRV(SOURCE_AKSL, ShaderKind.FRAGMENT,
                new ShaderCaps(), new CompileOptions(), COMMON_MODULE);
        blackhole.consume(Objects.requireNonNull(spirv));
    }
}
