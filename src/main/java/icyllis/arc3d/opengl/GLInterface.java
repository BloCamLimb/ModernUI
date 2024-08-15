/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.opengl;

import org.lwjgl.system.NativeType;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Interface for gl* function access between OpenGL 4.6 Core and OpenGL ES 3.2,
 * depending on GLCapabilities or GLESCapabilities.
 * <p>
 * No javadoc here, please refer to LWJGL javadoc and OpenGL specification.
 *
 * @see GLCaps
 */
public interface GLInterface extends GLInterfaceCommon {

    void glDrawElementsBaseVertex(@NativeType("GLenum") int mode, @NativeType("GLsizei") int count,
                                  @NativeType("GLenum") int type, @NativeType("void const *") long indices,
                                  @NativeType("GLint") int basevertex);

    void glDrawElementsInstancedBaseVertex(@NativeType("GLenum") int mode, @NativeType("GLsizei") int count,
                                           @NativeType("GLenum") int type, @NativeType("void const *") long indices,
                                           @NativeType("GLsizei") int instancecount,
                                           @NativeType("GLint") int basevertex);

    void glShaderBinary(@NativeType("GLuint const *") IntBuffer shaders, @NativeType("GLenum") int binaryformat,
                        @NativeType("void const *") ByteBuffer binary);

    void glDrawArraysInstancedBaseInstance(@NativeType("GLenum") int mode, @NativeType("GLint") int first,
                                           @NativeType("GLsizei") int count, @NativeType("GLsizei") int instancecount,
                                           @NativeType("GLuint") int baseinstance);

    void glDrawElementsInstancedBaseVertexBaseInstance(@NativeType("GLenum") int mode, @NativeType("GLsizei") int count,
                                                       @NativeType("GLenum") int type,
                                                       @NativeType("void const *") long indices,
                                                       @NativeType("GLsizei") int instancecount,
                                                       @NativeType("GLint") int basevertex,
                                                       @NativeType("GLuint") int baseinstance);

    void glTexStorage2D(@NativeType("GLenum") int target, @NativeType("GLsizei") int levels,
                        @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width,
                        @NativeType("GLsizei") int height);

    void glInvalidateBufferSubData(@NativeType("GLuint") int buffer, @NativeType("GLintptr") long offset,
                                   @NativeType("GLsizeiptr") long length);

    void glObjectLabel(@NativeType("GLenum") int identifier, @NativeType("GLuint") int name,
                       @NativeType("GLsizei") int length, @NativeType("GLchar const *") long label);

    void glObjectLabel(@NativeType("GLenum") int identifier, @NativeType("GLuint") int name,
                       @NativeType("GLchar const *") CharSequence label);

    void glBindVertexBuffer(@NativeType("GLuint") int bindingindex, @NativeType("GLuint") int buffer,
                            @NativeType("GLintptr") long offset, @NativeType("GLsizei") int stride);

    void glVertexAttribFormat(@NativeType("GLuint") int attribindex, @NativeType("GLint") int size,
                              @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized,
                              @NativeType("GLuint") int relativeoffset);

    void glVertexAttribIFormat(@NativeType("GLuint") int attribindex, @NativeType("GLint") int size,
                               @NativeType("GLenum") int type, @NativeType("GLuint") int relativeoffset);

    void glVertexAttribBinding(@NativeType("GLuint") int attribindex, @NativeType("GLuint") int bindingindex);

    void glVertexBindingDivisor(@NativeType("GLuint") int bindingindex, @NativeType("GLuint") int divisor);

    void glBufferStorage(@NativeType("GLenum") int target, @NativeType("GLsizeiptr") long size,
                         @NativeType("void const *") long data, @NativeType("GLbitfield") int flags);

    void glTextureBarrier();

    @NativeType("void")
    int glCreateBuffers();

    void glNamedBufferData(@NativeType("GLuint") int buffer, @NativeType("GLsizeiptr") long size,
                           @NativeType("void const *") long data, @NativeType("GLenum") int usage);

    void glNamedBufferSubData(@NativeType("GLuint") int buffer, @NativeType("GLintptr") long offset,
                              @NativeType("GLsizeiptr") long size, @NativeType("void const *") long data);

    @NativeType("void *")
    long glMapNamedBufferRange(@NativeType("GLuint") int buffer, @NativeType("GLintptr") long offset,
                               @NativeType("GLsizeiptr") long length, @NativeType("GLbitfield") int access);

    @NativeType("GLboolean")
    boolean glUnmapNamedBuffer(@NativeType("GLuint") int buffer);

    void glNamedBufferStorage(@NativeType("GLuint") int buffer, @NativeType("GLsizeiptr") long size,
                              @NativeType("void const *") long data, @NativeType("GLbitfield") int flags);

    void glCopyNamedBufferSubData(@NativeType("GLuint") int readBuffer, @NativeType("GLuint") int writeBuffer,
                                  @NativeType("GLintptr") long readOffset, @NativeType("GLintptr") long writeOffset,
                                  @NativeType("GLsizeiptr") long size);

    @NativeType("void")
    int glCreateTextures(@NativeType("GLenum") int target);

    void glTextureParameteri(@NativeType("GLuint") int texture, @NativeType("GLenum") int pname,
                             @NativeType("GLint") int param);

    void glTextureStorage2D(@NativeType("GLuint") int texture, @NativeType("GLsizei") int levels,
                            @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width,
                            @NativeType("GLsizei") int height);

    @NativeType("void")
    int glCreateVertexArrays();

    void glEnableVertexArrayAttrib(@NativeType("GLuint") int vaobj, @NativeType("GLuint") int index);

    void glVertexArrayAttribFormat(@NativeType("GLuint") int vaobj, @NativeType("GLuint") int attribindex,
                                   @NativeType("GLint") int size, @NativeType("GLenum") int type,
                                   @NativeType("GLboolean") boolean normalized,
                                   @NativeType("GLuint") int relativeoffset);

    void glVertexArrayAttribIFormat(@NativeType("GLuint") int vaobj, @NativeType("GLuint") int attribindex,
                                    @NativeType("GLint") int size, @NativeType("GLenum") int type,
                                    @NativeType("GLuint") int relativeoffset);

    void glVertexArrayAttribBinding(@NativeType("GLuint") int vaobj, @NativeType("GLuint") int attribindex,
                                    @NativeType("GLuint") int bindingindex);

    void glVertexArrayBindingDivisor(@NativeType("GLuint") int vaobj, @NativeType("GLuint") int bindingindex,
                                     @NativeType("GLuint") int divisor);

    void glBindTextureUnit(@NativeType("GLuint") int unit, @NativeType("GLuint") int texture);

    void glSpecializeShader(@NativeType("GLuint") int shader, @NativeType("GLchar const *") CharSequence pEntryPoint,
                            @NativeType("GLuint const *") IntBuffer pConstantIndex,
                            @NativeType("GLuint const *") IntBuffer pConstantValue);
}
