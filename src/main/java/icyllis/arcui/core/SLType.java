/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.core;

import javax.annotation.Nonnull;

/**
 * Types of shader-language-specific boxed variables we can create. Shared constants.
 */
public final class SLType {

    public static final byte
            VOID                    = 0,
            BOOL                    = 1,
            BVEC2                   = 2,
            BVEC3                   = 3,
            BVEC4                   = 4,
            INT                     = 5,
            IVEC2                   = 6,
            IVEC3                   = 7,
            IVEC4                   = 8,
            UINT                    = 9,
            UVEC2                   = 10,
            UVEC3                   = 11,
            UVEC4                   = 12,
            FLOAT                   = 13,
            VEC2                    = 14,
            VEC3                    = 15,
            VEC4                    = 16,
            MAT2                    = 17,
            MAT3                    = 18,
            MAT4                    = 19,
            SAMPLER2D               = 20,
            TEXTURE2D               = 21,
            SAMPLER                 = 22,
            SUBPASSINPUT            = 23;
    public static final byte LAST = SUBPASSINPUT;

    /**
     * Is the shading language type float (including vectors/matrices)?
     */
    public static boolean isFloatType(byte type) {
        switch (type) {
            case FLOAT:
            case VEC2:
            case VEC3:
            case VEC4:
            case MAT2:
            case MAT3:
            case MAT4:
                return true;

            case VOID:
            case SAMPLER2D:
            case BOOL:
            case BVEC2:
            case BVEC3:
            case BVEC4:
            case INT:
            case IVEC2:
            case IVEC3:
            case IVEC4:
            case UINT:
            case UVEC2:
            case UVEC3:
            case UVEC4:
            case TEXTURE2D:
            case SAMPLER:
            case SUBPASSINPUT:
                return false;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * Is the shading language type integral (including vectors)?
     */
    public static boolean isIntegralType(byte type) {
        switch (type) {
            case INT:
            case IVEC2:
            case IVEC3:
            case IVEC4:
            case UINT:
            case UVEC2:
            case UVEC3:
            case UVEC4:
                return true;

            case FLOAT:
            case VEC2:
            case VEC3:
            case VEC4:
            case MAT2:
            case MAT3:
            case MAT4:
            case VOID:
            case SAMPLER2D:
            case BOOL:
            case BVEC2:
            case BVEC3:
            case BVEC4:
            case TEXTURE2D:
            case SAMPLER:
            case SUBPASSINPUT:
                return false;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * Is the shading language type supported as a uniform (ie, does it have a corresponding set
     * function on GrGLSLProgramDataManager)?
     */
    public static boolean canBeUniformValue(byte type) {
        return isFloatType(type) || isIntegralType(type);
    }

    /**
     * If the type represents a single value or vector return the vector length, else -1.
     */
    public static int vecLength(byte type) {
        switch (type) {
            case BOOL:
            case INT:
            case UINT:
            case FLOAT:
                return 1;

            case BVEC2:
            case IVEC2:
            case UVEC2:
            case VEC2:
                return 2;

            case BVEC3:
            case IVEC3:
            case UVEC3:
            case VEC3:
                return 3;

            case BVEC4:
            case IVEC4:
            case UVEC4:
            case VEC4:
                return 4;

            case MAT2:
            case MAT3:
            case MAT4:
            case VOID:
            case SAMPLER2D:
            case TEXTURE2D:
            case SAMPLER:
            case SUBPASSINPUT:
                return -1;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    public static boolean isCombinedSamplerType(byte type) {
        switch (type) {
            case SAMPLER2D:
                return true;

            case VOID:
            case FLOAT:
            case VEC2:
            case VEC3:
            case VEC4:
            case MAT2:
            case MAT3:
            case MAT4:
            case INT:
            case IVEC2:
            case IVEC3:
            case IVEC4:
            case UINT:
            case UVEC2:
            case UVEC3:
            case UVEC4:
            case BOOL:
            case BVEC2:
            case BVEC3:
            case BVEC4:
            case TEXTURE2D:
            case SAMPLER:
            case SUBPASSINPUT:
                return false;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    public static boolean isMatrixType(byte type) {
        switch (type) {
            case MAT2:
            case MAT3:
            case MAT4:
                return true;

            case VOID:
            case BOOL:
            case BVEC2:
            case BVEC3:
            case BVEC4:
            case FLOAT:
            case VEC2:
            case VEC3:
            case VEC4:
            case INT:
            case IVEC2:
            case IVEC3:
            case IVEC4:
            case UINT:
            case UVEC2:
            case UVEC3:
            case UVEC4:
            case SAMPLER2D:
            case TEXTURE2D:
            case SAMPLER:
            case SUBPASSINPUT:
                return false;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    @Nonnull
    public static String typeString(byte type) {
        switch (type) {
            case VOID:          return "void";
            case BOOL:          return "bool";
            case BVEC2:         return "bvec2";
            case BVEC3:         return "bvec3";
            case BVEC4:         return "bvec4";
            case INT:           return "int";
            case IVEC2:         return "ivec2";
            case IVEC3:         return "ivec3";
            case IVEC4:         return "ivec4";
            case UINT:          return "uint";
            case UVEC2:         return "uvec2";
            case UVEC3:         return "uvec3";
            case UVEC4:         return "uvec4";
            case FLOAT:         return "float";
            case VEC2:          return "vec2";
            case VEC3:          return "vec3";
            case VEC4:          return "vec4";
            case MAT2:          return "mat2";
            case MAT3:          return "mat3";
            case MAT4:          return "mat4";
            case SAMPLER2D:     return "sampler2D";
            case TEXTURE2D:     return "texture2D";
            case SAMPLER:       return "sampler";
            case SUBPASSINPUT:  return "subpassInput";
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }
}
