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

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.util.spvc.Spvc.*;

/**
 * Test glslang compile result.
 */
public class TestShaderc {

    public static void main(String[] args) {
        long compiler = shaderc_compiler_initialize();
        if (compiler == 0) {
            throw new RuntimeException("No compiler");
        }
        long options = shaderc_compile_options_initialize();
        shaderc_compile_options_set_target_env(options, shaderc_target_env_opengl, shaderc_env_version_opengl_4_5);
        shaderc_compile_options_set_target_spirv(options, shaderc_spirv_version_1_0);
        // SPIRV-Tools optimization LOWER the performance on NVIDIA GPU
        shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_zero);

        long result = shaderc_compile_into_spv(
                compiler,
                """
                        #version 450 core
                        
                          layout(std140, binding = 1) uniform SmoothBlock {
                              float u_SmoothRadius;
                          };
                          layout(std140, binding = 2) uniform PaintBlock {
                              vec2 u_CenterPos;
                              float u_MiddleAngle;
                              float u_SweepAngle;
                              float u_Radius;
                              float u_StrokeRadius;
                          };
                        
                          layout(location = 0) smooth in vec2 f_Position;
                          layout(location = 1) smooth in vec4 f_Color;
                        
                          layout(location = 0, index = 0) out vec4 fragColor;
                        
                          void main() {
                              vec2 v = f_Position - u_CenterPos;
                        
                              // smoothing normal direction
                              float d1 = abs(length(v) - u_Radius) - u_StrokeRadius;
                              float a1 = smoothstep(-u_SmoothRadius, 0.0, d1);
                        
                              // sweep angle (0,360) in degrees
                              float c = cos(u_SweepAngle * 0.00872664626);
                        
                              float f = u_MiddleAngle * 0.01745329252;
                              // normalized vector from the center to the middle of the arc
                              vec2 up = vec2(cos(f), sin(f));
                        
                              // smoothing tangent direction
                              float d2 = dot(up, normalize(v)) - c;
                        
                              // proportional to how much `d2` changes between pixels
                              float w = u_SmoothRadius * fwidth(d2);
                              float a2 = smoothstep(w * -0.5, w * 0.5, d2);
                        
                              // mix alpha value
                              float a = (1.0 - a1) * a2;
                        
                              fragColor = f_Color * a;
                          }""",
                shaderc_fragment_shader,
                "test_shader",
                "main",
                options
        );
        if (result == 0) {
            throw new RuntimeException("No result");
        }
        long num_errors = shaderc_result_get_num_errors(result);
        long num_warnings = shaderc_result_get_num_warnings(result);
        System.out.println(num_errors + " errors, " + num_warnings + " warnings");
        String error_message = shaderc_result_get_error_message(result);
        System.out.println("msg: " + error_message);
        int status = shaderc_result_get_compilation_status(result);
        if (status == shaderc_compilation_status_success) {
            long len = shaderc_result_get_length(result);
            System.out.println("Bytes: " + len);
            ByteBuffer bytes = shaderc_result_get_bytes(result);
            if (bytes != null) {
                /*String s = MemoryUtil.memASCII(bytes);
                System.out.println(s);*/
                testCompileToGLSL(bytes);
            } else {
                System.out.println("No bytes");
            }
        } else {
            System.out.println("Failed " + status);
        }
        shaderc_result_release(result);
        shaderc_compile_options_release(options);
        shaderc_compiler_release(compiler);
    }

    public static void testCompileToGLSL(ByteBuffer spirv) {
        try(var stack = MemoryStack.stackPush()) {
            PointerBuffer pointer = stack.mallocPointer(1);

            spvc_context_create(pointer);
            long context = pointer.get(0);

            // the spirv is in host endianness, just reinterpret
            spvc_context_parse_spirv(context, spirv.asIntBuffer(), spirv.remaining() / 4, pointer);
            long parsed_ir = pointer.get(0);

            spvc_context_create_compiler(context, SPVC_BACKEND_GLSL, parsed_ir, SPVC_CAPTURE_MODE_TAKE_OWNERSHIP,
                    pointer);
            long compiler_glsl = pointer.get(0);

            spvc_compiler_create_compiler_options(compiler_glsl, pointer);
            long options = pointer.get(0);
            spvc_compiler_options_set_uint(options, SPVC_COMPILER_OPTION_GLSL_VERSION, 450);
            spvc_compiler_options_set_bool(options, SPVC_COMPILER_OPTION_GLSL_ES, false);
            spvc_compiler_install_compiler_options(compiler_glsl, options);

            spvc_compiler_compile(compiler_glsl, pointer);
            System.out.print("Cross-compiled source: ");
            System.out.println(MemoryUtil.memUTF8(pointer.get(0)));

            spvc_context_destroy(context);
        }
    }
}
