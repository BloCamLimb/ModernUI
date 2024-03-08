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
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
public class CompilerBenchmark {

    public static final String SOURCE = """
            #version 450    core
                # pragma deep dark # line 2
                      \s
                      
                # extension GL_ARB_enhanced_layouts: enable /*****/ //#  line 2
            const int blockSize = -4 + 6;
            layout(binding = 0, set = 0) uniform UniformBlock {
                mat4 u_Projection;
                mat4 u_ModelView;
                vec4 u_Color;
            } u_Buffer0;
            layout(location = 0) smooth in vec2 f_Position;
            layout(location = 1) smooth in vec4 f_Color;
            layout(location = 0, index = 0) out vec4 FragColor0;
            layout(location = 0, index = 1) out vec4 FragColor1;
            void main(void) {
                // M4 m = "what?";
                FragColor0 = u_Buffer0.u_Color;
            }
            """;

    public static final char[] SOURCE_CHARS = SOURCE.toCharArray();

    public static final ByteBuffer SOURCE_BUFFER = MemoryUtil.memUTF8(SOURCE, false);
    public static final ByteBuffer FILE_NAME_BUFFER = MemoryUtil.memUTF8("file");
    public static final ByteBuffer ENTRY_NAME_BUFFER = MemoryUtil.memUTF8("main");

    // the benchmark shows that our compiler is 100x faster than glslang
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
        ByteBuffer spirv = compiler.compileIntoSPIRV(SOURCE_CHARS, 0, SOURCE_CHARS.length, ShaderKind.FRAGMENT,
                new ShaderCaps(), new CompileOptions(), ModuleLoader.getInstance().getRootModule());
        blackhole.consume(Objects.requireNonNull(spirv));
    }
}
