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
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

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

        String file = TinyFileDialogs.tinyfd_openFileDialog("Open shader source",
                null, null, null, false);
        if (file == null) {
            return;
        }
        CharBuffer source;
        try (FileChannel fc = FileChannel.open(Path.of(file), StandardOpenOption.READ)) {
            MappedByteBuffer mb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            source = StandardCharsets.UTF_8.decode(mb);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        long result = shaderc_compile_into_spv_assembly(
                compiler,
                source,
                shaderc_vertex_shader,
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
                //testCompileToGLSL(bytes);
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
