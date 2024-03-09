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
import icyllis.arc3d.compiler.SPIRVVersion;
import icyllis.arc3d.compiler.lex.*;
import icyllis.arc3d.compiler.TranslationUnit;
import icyllis.arc3d.core.MathUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class TestCompiler {

    public static final String SOURCE = """
            #version 450    core
                # pragma deep dark # line 2
                      \s
                      
                # extension GL_ARB_enhanced_layouts: enable /*****/ //#  line 2
            # include  <fog>  /*  */
                      using M4          \s
                      = mat4;
            const int blockSize = -4 + 6;
            layout(std140, binding = 0, set = 0) uniform UniformBlock {
                M4 u_Projection;
                M4 u_ModelView;
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

    public static void main(String[] args) {
        var compiler = new ShaderCompiler();

        System.out.println("Source length: " + SOURCE.length());

        {
            long bytes = 0;
            bytes += 16 + MathUtil.align8(Lexer.MAPPINGS.length);
            bytes += 16 + MathUtil.align8(Lexer.ACCEPTS.length);
            bytes += 16 + MathUtil.align8(Lexer.INDICES.length * 2);
            bytes += 16 + MathUtil.align8(Lexer.FULL.length * 4);
            for (short[] elem : Lexer.FULL) {
                bytes += 16 + MathUtil.align8(elem.length * 2);
            }
            bytes += 16 + MathUtil.align8(Lexer.PACKED.length * 4);
            for (Lexer.PackedEntry elem : Lexer.PACKED) {
                bytes += 16 + 4 + 4 + 16 + MathUtil.align8(elem.data().length);
            }
            System.out.println("Lexer bytes: " + bytes);
        }

        var options = new CompileOptions();

        TranslationUnit translationUnit = compiler.parse(
                SOURCE,
                ShaderKind.FRAGMENT,
                options,
                ModuleLoader.getInstance().getRootModule()
        );

        System.out.print(compiler.getErrorMessage());
        if (translationUnit == null) {
            return;
        }

        System.out.println(translationUnit);
        System.out.println(translationUnit.getUsage());
        System.out.println(translationUnit.getExtensions());

        ShaderCaps shaderCaps = new ShaderCaps();
        shaderCaps.mSPIRVVersion = SPIRVVersion.SPIRV_1_5;

        ByteBuffer spirv = compiler.generateSPIRV(translationUnit, shaderCaps);
        System.out.print(compiler.getErrorMessage());

        if (spirv == null) {
            return;
        }

        System.out.println(spirv);
        System.out.println(spirv.order());
        try (var channel = FileChannel.open(Path.of("test_shader.spv"),
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            while (spirv.hasRemaining()) {
                channel.write(spirv);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
