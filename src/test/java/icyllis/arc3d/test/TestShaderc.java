/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.test;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.util.shaderc.Shaderc.*;

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
        shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_zero);

        long result = shaderc_compile_into_spv_assembly(
                compiler,
                """
                        #version 450 core
                        layout(binding = 0, set = 0) uniform UniformBlock {
                            mat4 u_Projection;
                            mat4 u_ModelView;
                            vec4 u_Color;
                        } u_Buffer0;
                        layout(location = 0) smooth in vec2 f_Position;
                        layout(location = 1) smooth in vec4 f_Color;
                        layout(location = 0, index = 0) out vec4 FragColor0;
                        layout(location = 0, index = 1) out vec4 FragColor1;
                        float rr(vec2 a, vec2 b) {
                            return float(vec2(a.x,1).y.x);
                        }
                        float sa(float a) {
                            return a;
                        }
                        float rand(vec2 n) {
                            const float[] a = float[](12.9898, n.x), b = float[](12.9898, n.x, n.y);
                            return sa(sa(rr(n, vec2(a[0],12.1414))) * 83758.5453);
                        }
                        void main(void) {
                            FragColor0 = u_Buffer0.u_Color;
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
                String s = MemoryUtil.memASCII(bytes);
                System.out.println(s);
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
}
