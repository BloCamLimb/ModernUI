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
import icyllis.arc3d.compiler.lex.Lexer;
import icyllis.arc3d.core.MathUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TestCompiler {

    public static final String SOURCE = """
            #version 450    core
                # pragma deep dark # line 2
                      \s
                      
                # extension GL_ARB_enhanced_layouts: enable /*****/ //#  line 2
            # import  <fog>  /*  */
                      using M4          \s
                      = mat4;
            const int blockSize = -4 + 6;
            layout(std140, binding = 0, set = 0) uniform UniformBlock {
                M4 u_Projection;
                M4 u_ModelView;
                vec4 u_Color;
            } u_Buffer0;
            uniform float iTime;
            vec3 shape( in vec2 uv )
            {
            	float time = iTime*0.05  + 47.0;
               \s
            	vec2 z = -1.0 + 2.0*uv;
            	z *= 1.5;
               \s
                vec3 col = vec3(1.0);
            	for( int j=0; j<48; j++ )
            	{
                    float s = float(j)/16.0;
                    float f = 0.2*(0.5 + 1.0*fract(sin(s*20.0)));
            
            		vec2 c = 0.5*vec2( cos(f*time+17.0*s),sin(f*time+19.0*s) );
            		z -= c;
            		float zr = length( z );
            	    float ar = atan( z.y, z.x ) + zr*0.6;
            	    z  = vec2( cos(ar), sin(ar) )/zr;
            		z += c;
            
                    // color		
                    col -= 0.5*exp( -10.0*dot(z,z) )* (0.25+0.4*sin( 5.5 + 1.5*s + vec3(1.6,0.8,0.5) ));
            	}
                   \s
                return col;
            }
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

        //System.out.println("Source length: " + SOURCE.length());

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
            //System.out.println("Lexer bytes: " + bytes);
        }

        var options = new CompileOptions();

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

        TranslationUnit translationUnit = compiler.parse(
                source,
                ShaderKind.FRAGMENT,
                options,
                ModuleLoader.getInstance().loadCommonModule(compiler)
        );

        System.out.print(compiler.getErrorMessage());
        if (translationUnit == null) {
            return;
        }

        /*System.out.println(translationUnit);
        System.out.println(translationUnit.getUsage());
        System.out.println(translationUnit.getExtensions());*/

        ShaderCaps shaderCaps = new ShaderCaps();
        shaderCaps.mSPIRVVersion = SPIRVVersion.SPIRV_1_5;

        ByteBuffer spirv = compiler.generateSPIRV(translationUnit, shaderCaps);
        System.out.print(compiler.getErrorMessage());

        if (spirv == null) {
            return;
        }

        /*System.out.println(spirv);
        System.out.println(spirv.order());*/
        try (var channel = FileChannel.open(Path.of("test_shader1.spv"),
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
