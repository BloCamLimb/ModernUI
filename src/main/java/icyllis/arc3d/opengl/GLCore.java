/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.opengl;

import icyllis.arc3d.engine.PipelineStateCache;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.*;

import java.io.PrintWriter;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Locale;

/**
 * Provides native interfaces of OpenGL 4.5 core and user-defined utilities.
 */
public final class GLCore extends GL45C {

    /**
     * Represents an invalid/unassigned OpenGL object compared to {@link #GL_NONE}.
     */
    public static final int INVALID_ID = 0xFFFFFFFF;

    /**
     * The reserved framebuffer that used for swapping buffers with window.
     */
    public static final int DEFAULT_FRAMEBUFFER = 0;

    /**
     * The default vertex array compared to custom vertex array objects.
     */
    public static final int DEFAULT_VERTEX_ARRAY = 0;

    public static final int DEFAULT_TEXTURE = 0;

    private GLCore() {
        throw new UnsupportedOperationException();
    }

    public static void glClearErrors() {
        //noinspection StatementWithEmptyBody
        while (glGetError() != GL_NO_ERROR)
            ;
    }

    @NativeType("GLuint")
    public static int glCompileShader(@NativeType("GLenum") int shaderType,
                                      @NativeType("GLchar const *") ByteBuffer source,
                                      PipelineStateCache.Stats stats,
                                      PrintWriter pw) {
        int shader = glCreateShader(shaderType);
        if (shader == 0) {
            return 0;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer string = stack.pointers(source);
            IntBuffer length = stack.ints(source.remaining());
            glShaderSource(shader, string, length);
        } finally {
            Reference.reachabilityFence(source);
        }

        glCompileShader(shader);
        stats.incShaderCompilations();

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            handleCompileError(pw, MemoryUtil.memUTF8(source), log);
            return 0;
        }

        return shader;
    }

    public static int glCompileAndAttachShader(int program,
                                               int shaderType,
                                               String source,
                                               PipelineStateCache.Stats stats,
                                               PrintWriter pw) {
        int shader = glCreateShader(shaderType);
        if (shader == 0) {
            return 0;
        }
        glShaderSource(shader, source);

        glCompileShader(shader);
        stats.incShaderCompilations();

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            handleCompileError(pw, source, log);
            return 0;
        }

        // Attach the shader, but defer deletion until after we have linked the program.
        glAttachShader(program, shader);
        return shader;
    }

    public static int glCompileAndAttachShader(int program,
                                               int shaderType,
                                               String[] source,
                                               PipelineStateCache.Stats stats,
                                               PrintWriter pw) {
        int shader = glCreateShader(shaderType);
        if (shader == 0) {
            return 0;
        }
        glShaderSource(shader, source);

        glCompileShader(shader);
        stats.incShaderCompilations();

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            pw.println(log);
            return 0;
        }

        // Attach the shader, but defer deletion until after we have linked the program.
        glAttachShader(program, shader);
        return shader;
    }

    public static int glSpecializeAndAttachShader(int program,
                                                  int shaderType,
                                                  ByteBuffer spirv,
                                                  PipelineStateCache.Stats stats,
                                                  PrintWriter pw) {
        int shader = glCreateShader(shaderType);
        if (shader == 0) {
            return 0;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var shaders = stack.ints(shader);
            glShaderBinary(shaders, GL46C.GL_SHADER_BINARY_FORMAT_SPIR_V, spirv);
        }

        GL46C.glSpecializeShader(shader, "main", (IntBuffer) null, null);
        stats.incShaderCompilations();

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            pw.println("Shader specialization error");
            pw.println("Errors:");
            pw.println(log);
            return 0;
        }

        // Attach the shader, but defer deletion until after we have linked the program.
        glAttachShader(program, shader);
        return shader;
    }

    public static void handleCompileError(PrintWriter pw,
                                          String source,
                                          String errors) {
        pw.println("Shader compilation error");
        pw.println("------------------------");
        String[] lines = source.split("\n");
        for (int i = 0; i < lines.length; ++i) {
            pw.printf(Locale.ROOT, "%4s\t%s", i + 1, lines[i]);
            pw.println();
        }
        pw.println("Errors:");
        pw.println(errors);
    }

    public static void handleLinkError(PrintWriter pw,
                                       String[] headers,
                                       String[] sources,
                                       String errors) {
        pw.println("Program linking error");
        pw.println("------------------------");
        for (int i = 0; i < headers.length; i++) {
            pw.println(headers[i]);
            String[] lines = sources[i].split("\n");
            for (int j = 0; j < lines.length; ++j) {
                pw.printf(Locale.ROOT, "%4s\t%s", j + 1, lines[j]);
                pw.println();
            }
        }
        pw.println("Errors:");
        pw.println(errors);
    }
}
