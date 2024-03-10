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

import javax.annotation.Nullable;

/**
 * Abstract gl* function access between OpenGL 4.6 Core Profile and OpenGL ES 3.2.
 * Depending on GLCapabilities or GLESCapabilities.
 * <p>
 * No javadoc here, please refer to LWJGL javadoc and OpenGL specification
 *
 * @see GLCaps
 */
public interface GLInterface {

    @Nullable
    @NativeType("GLubyte const *")
    String GetString(@NativeType("GLenum") int name);

    @NativeType("void")
    int GetInteger(@NativeType("GLenum") int pname);

    void TextureBarrier();
}
