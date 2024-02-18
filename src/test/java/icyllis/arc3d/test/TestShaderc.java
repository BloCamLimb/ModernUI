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
        shaderc_compile_options_set_target_env(options, shaderc_target_env_vulkan, shaderc_env_version_vulkan_1_1);
        shaderc_compile_options_set_target_spirv(options, shaderc_spirv_version_1_3);
        shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_zero);

        long result = shaderc_compile_into_spv_assembly(
                compiler,
                """
                     #version 450 core
                     layout(std140, binding = 0) buffer UniformBlock {
                         layout(offset=0) float[4] u_Projection;
                         layout(offset=64, row_major) mat2 u_Color;
                     };
                     layout(location = 0) smooth in vec2 f_Position;
                     layout(location = 1) smooth in vec4 f_Color;
                     layout(location = 0, index = 0) out vec4 FragColor0;
                     layout(location = 0, index = 1) out vec4 FragColor1;
                     void main(void) {
                        mat2 m = u_Color;
                         FragColor0 = vec4(m[0][0]);
                         FragColor1 = vec4(m[0][1]);
                     }
                     """,
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
