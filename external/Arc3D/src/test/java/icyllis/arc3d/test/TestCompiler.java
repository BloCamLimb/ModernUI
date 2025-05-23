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

import icyllis.arc3d.compiler.CompileOptions;
import icyllis.arc3d.compiler.GLSLVersion;
import icyllis.arc3d.compiler.ModuleLoader;
import icyllis.arc3d.compiler.SPIRVVersion;
import icyllis.arc3d.compiler.ShaderCaps;
import icyllis.arc3d.compiler.ShaderCompiler;
import icyllis.arc3d.compiler.ShaderKind;
import icyllis.arc3d.compiler.TargetApi;
import icyllis.arc3d.compiler.TranslationUnit;
import icyllis.arc3d.compiler.lex.Lexer;
import icyllis.arc3d.core.MathUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import static org.lwjgl.util.spvc.Spvc.*;

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
            layout(std140, binding = 0) uniform UniformBlock {
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
                float[] arr = {1,2,3,4};
                M4 v = {{2,2,3,1},vec4(1),vec4(2),vec4(3),};
                FragColor0 = vec4(shape(u_Buffer0.u_Color.xy),1.0);
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
        options.mMinifyCode = true;
        options.mMinifyNames = false;
        //options.mUsePrecisionQualifiers = true;

        /*String file = TinyFileDialogs.tinyfd_openFileDialog("Open shader source",
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
        }*/

        TranslationUnit translationUnit = compiler.parse(
                SOURCE,
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
        shaderCaps.mTargetApi = TargetApi.OPENGL_4_5;
        shaderCaps.mGLSLVersion = GLSLVersion.GLSL_320_ES;
        shaderCaps.mSPIRVVersion = SPIRVVersion.SPIRV_1_0;

        ByteBuffer glsl = compiler.generateGLSL(translationUnit, shaderCaps);
        System.out.println(compiler.getErrorMessage());

        if (glsl != null) {
            try (var channel = FileChannel.open(Path.of("test_shader1.glsl"),
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                while (glsl.hasRemaining()) {
                    channel.write(glsl);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // reparse to see if success
            TranslationUnit t2 = compiler.parse(
                    StandardCharsets.UTF_8.decode(glsl.rewind()),
                    ShaderKind.FRAGMENT,
                    options,
                    ModuleLoader.getInstance().loadCommonModule(compiler)
            );
            Objects.requireNonNull(t2, compiler::getErrorMessage);
        }

        ByteBuffer spirv = compiler.generateSPIRV(translationUnit, shaderCaps);
        System.out.print(compiler.getErrorMessage());

        if (spirv != null) {
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

            testCompileToGLSL(spirv.rewind());
        }
    }

    public static void testCompileToGLSL(ByteBuffer spirv) {
        try (var stack = MemoryStack.stackPush()) {
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
        } finally {
            Reference.reachabilityFence(spirv);
        }
    }
}
