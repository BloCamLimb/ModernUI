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

package icyllis.arc3d.compiler;

public enum GLSLVersion {
    /**
     * GLSL version 3.30 core for OpenGL 3.3.
     */
    GLSL_330("#version 330 core\n"),
    /**
     * GLSL version 3.00 es for OpenGL ES 3.0.
     */
    GLSL_300_ES("#version 300 es\n"),
    /**
     * GLSL version 4.30 core for OpenGL 4.3.
     * This version includes all the GLSL ES 3.00 features.
     */
    GLSL_430("#version 430 core\n"),
    /**
     * GLSL version 3.10 es for OpenGL ES 3.1.
     */
    GLSL_310_ES("#version 310 es\n"),
    /**
     * GLSL version 4.50 core for OpenGL 4.5 and Vulkan 1.0 or above.
     * This version includes all the GLSL ES 3.10 features.
     */
    GLSL_450("#version 450 core\n");

    public final String mVersionDeclString;

    GLSLVersion(String versionDeclString) {
        mVersionDeclString = versionDeclString;
    }

    public boolean isAtLeast(GLSLVersion other) {
        return compareTo(other) >= 0;
    }
}
