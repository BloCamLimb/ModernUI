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

package icyllis.arc3d.compiler.spirv;

/**
 * Specify a backend API for validation.
 */
public enum SPIRVTarget {
    /**
     * Targeting OpenGL 4.5.
     */
    OPENGL_4_5,
    /**
     * Targeting Vulkan 1.0.
     */
    VULKAN_1_0;

    public boolean isOpenGL() {
        return this == OPENGL_4_5;
    }

    public boolean isVulkan() {
        return compareTo(VULKAN_1_0) >= 0;
    }
}
