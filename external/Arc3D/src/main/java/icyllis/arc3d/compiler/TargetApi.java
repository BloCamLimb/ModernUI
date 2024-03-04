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
 * Specify a backend API for validation.
 */
public enum TargetApi {
    /**
     * Targeting OpenGL 3.3.
     */
    OPENGL_3_3,
    /**
     * Targeting OpenGL ES 3.0.
     */
    OPENGL_ES_3_0,
    /**
     * Targeting OpenGL 4.3.
     */
    OPENGL_4_3,
    /**
     * Targeting OpenGL ES 3.1.
     */
    OPENGL_ES_3_1,
    /**
     * Targeting OpenGL 4.5.
     */
    OPENGL_4_5,
    /**
     * Targeting Vulkan 1.0 or above.
     */
    VULKAN_1_0;

    public boolean isOpenGL() {
        return this == OPENGL_3_3 || this == OPENGL_4_3 || this == OPENGL_4_5;
    }

    public boolean isOpenGLES() {
        return this == OPENGL_ES_3_0 || this == OPENGL_ES_3_1;
    }

    public boolean isVulkan() {
        return compareTo(VULKAN_1_0) >= 0;
    }
}
