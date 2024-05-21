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

package icyllis.arc3d.opengl;

import org.lwjgl.opengl.GL45C;

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


}
