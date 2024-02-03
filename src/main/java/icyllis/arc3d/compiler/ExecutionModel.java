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
 * We support several execution models.
 */
public enum ExecutionModel {
    /**
     * For base modules.
     */
    BASE,
    /**
     * For vertex shaders.
     */
    VERTEX,
    /**
     * For fragment shaders.
     */
    FRAGMENT,
    /**
     * For compute shaders.
     */
    COMPUTE,
    /**
     * A substage of shader code, working as a function.
     */
    SUBROUTINE,
    // the following are all specialization of SUBROUTINE
    SUBROUTINE_SHADER,          // subroutine shader(float2 uv) -> float4
    SUBROUTINE_COLOR_FILTER,    // subroutine colorFilter(float4 col) -> float4
    SUBROUTINE_BLENDER,         // subroutine blender(float4 src, float4 dst) -> float4
    PRIVATE_SUBROUTINE_SHADER,  //TODO Do we really need private versions?
    PRIVATE_SUBROUTINE_COLOR_FILTER,
    PRIVATE_SUBROUTINE_BLENDER;

    public boolean isFragment() {
        return this == FRAGMENT;
    }

    public boolean isCompute() {
        return this == COMPUTE;
    }

    public boolean isAnySubroutine() {
        return this.compareTo(SUBROUTINE) >= 0;
    }
}
