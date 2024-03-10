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

/**
 * Limited set of GLSL versions we generate shaders for. Caller should round
 * down the GLSL version to one of these enums.
 * <p>
 * Note: Do not rely on enum's ordinal.
 */
public enum GLSLVersion {
    /**
     * GLSL version 3.00 es for OpenGL ES 3.0.
     */
    GLSL_300_ES("#version 300 es\n"),
    /**
     * GLSL version 3.10 es for OpenGL ES 3.1.
     */
    GLSL_310_ES("#version 310 es\n"),
    /**
     * GLSL version 3.20 es for OpenGL ES 3.2.
     */
    GLSL_320_ES("#version 320 es\n"),
    /**
     * GLSL version 3.30 core for OpenGL 3.3.
     */
    GLSL_330("#version 330 core\n"),
    /**
     * GLSL version 4.00 core for OpenGL 4.0.
     */
    GLSL_400("#version 400 core\n"),
    /**
     * GLSL version 4.20 core for OpenGL 4.2.
     */
    GLSL_420("#version 420 core\n"),
    /**
     * GLSL version 4.30 core for OpenGL 4.3.
     * This version includes all the GLSL ES 3.00 features.
     */
    GLSL_430("#version 430 core\n"),
    /**
     * GLSL version 4.40 core for OpenGL 4.4.
     */
    GLSL_440("#version 440 core\n"),
    /**
     * GLSL version 4.50 core for OpenGL 4.5 and Vulkan 1.0 or above.
     * This version includes all the GLSL ES 3.10 features.
     */
    GLSL_450("#version 450 core\n");

    public final String mVersionDecl;

    GLSLVersion(String versionDecl) {
        mVersionDecl = versionDecl;
    }

    public boolean isAtLeast(GLSLVersion other) {
        return compareTo(other) >= 0;
    }

    public boolean isCoreProfile() {
        return compareTo(GLSL_330) >= 0;
    }

    public boolean isEsProfile() {
        return compareTo(GLSL_310_ES) <= 0;
    }
}
