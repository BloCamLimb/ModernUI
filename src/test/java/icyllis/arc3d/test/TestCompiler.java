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
import icyllis.arc3d.compiler.spirv.SPIRVTarget;
import icyllis.arc3d.compiler.spirv.SPIRVVersion;
import icyllis.arc3d.compiler.tree.TranslationUnit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TestCompiler {

    public static final String SOURCE = """
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
                FragColor0 = u_Buffer0.u_Color;
            }
            """;

    public static void main(String[] args) {
        var compiler = new ShaderCompiler();

        System.out.println(SOURCE.length());

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

        ByteBuffer spirv = compiler.toSPIRV(translationUnit,
                SPIRVTarget.OPENGL_4_5,
                SPIRVVersion.SPIRV_1_5);
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
