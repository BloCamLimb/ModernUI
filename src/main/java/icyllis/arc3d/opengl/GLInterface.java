/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

import org.lwjgl.system.NativeType;

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

    void glDrawArraysInstancedBaseInstance(@NativeType("GLenum") int mode, @NativeType("GLint") int first,
                                           @NativeType("GLsizei") int count, @NativeType("GLsizei") int instancecount,
                                           @NativeType("GLuint") int baseinstance);

    void glDrawElementsInstancedBaseVertexBaseInstance(@NativeType("GLenum") int mode, @NativeType("GLsizei") int count,
                                                       @NativeType("GLenum") int type,
                                                       @NativeType("void const *") long indices,
                                                       @NativeType("GLsizei") int instancecount,
                                                       @NativeType("GLint") int basevertex,
                                                       @NativeType("GLuint") int baseinstance);

    void glInvalidateBufferSubData(@NativeType("GLuint") int buffer, @NativeType("GLintptr") long offset,
                                   @NativeType("GLsizeiptr") long length);

    void glObjectLabel(@NativeType("GLenum") int identifier, @NativeType("GLuint") int name,
                       @NativeType("GLsizei") int length, @NativeType("GLchar const *") long label);

    void glObjectLabel(@NativeType("GLenum") int identifier, @NativeType("GLuint") int name,
                       @NativeType("GLchar const *") CharSequence label);

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
}
