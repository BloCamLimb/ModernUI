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

    public static final String SOURCE = """
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
            }
            """;

    public static final char[] SOURCE_CHARS = SOURCE.toCharArray();
    public static final ModuleUnit COMMON_MODULE = ModuleLoader.getInstance().loadCommonModule(new ShaderCompiler());

    public static final ByteBuffer SOURCE_BUFFER = MemoryUtil.memUTF8(SOURCE, false);
    public static final ByteBuffer FILE_NAME_BUFFER = MemoryUtil.memUTF8("file");
    public static final ByteBuffer ENTRY_NAME_BUFFER = MemoryUtil.memUTF8("main");

    // the benchmark shows that our compiler is 20x to 32x faster than glslang
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
                new ShaderCaps(), new CompileOptions(), COMMON_MODULE);
        blackhole.consume(Objects.requireNonNull(spirv));
    }
}
