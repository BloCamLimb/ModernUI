/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.core;

import javax.annotation.Nonnull;

/**
 * Types of shader-language-specific boxed variables we can create. Shared constants.
 */
public final class SLType {

    public static final byte
            Void            = 0,
            Bool            = 1,
            BVec2           = 2,
            BVec3           = 3,
            BVec4           = 4,
            Int             = 5,
            IVec2           = 6,
            IVec3           = 7,
            IVec4           = 8,
            UInt            = 9,
            UVec2           = 10,
            UVec3           = 11,
            UVec4           = 12,
            Float           = 13,
            Vec2            = 14,
            Vec3            = 15,
            Vec4            = 16,
            Mat2            = 17,
            Mat3            = 18,
            Mat4            = 19,
            Sampler2D       = 20,
            Texture2D       = 21,
            Sampler         = 22,
            SubpassInput    = 23;
    public static final byte Last = SubpassInput;

    // Debug tool.
    public static boolean checkSLType(byte type) {
        return type >= 0 && type <= Last;
    }

    /**
     * Is the shading language type float (including vectors/matrices)?
     */
    public static boolean isFloatType(byte type) {
        switch (type) {
            case Float:
            case Vec2:
            case Vec3:
            case Vec4:
            case Mat2:
            case Mat3:
            case Mat4:
                return true;

            case Void:
            case Sampler2D:
            case Bool:
            case BVec2:
            case BVec3:
            case BVec4:
            case Int:
            case IVec2:
            case IVec3:
            case IVec4:
            case UInt:
            case UVec2:
            case UVec3:
            case UVec4:
            case Texture2D:
            case Sampler:
            case SubpassInput:
                return false;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * Is the shading language type integral (including vectors)?
     */
    public static boolean isIntegralType(byte type) {
        switch (type) {
            case Int:
            case IVec2:
            case IVec3:
            case IVec4:
            case UInt:
            case UVec2:
            case UVec3:
            case UVec4:
                return true;

            case Float:
            case Vec2:
            case Vec3:
            case Vec4:
            case Mat2:
            case Mat3:
            case Mat4:
            case Void:
            case Sampler2D:
            case Bool:
            case BVec2:
            case BVec3:
            case BVec4:
            case Texture2D:
            case Sampler:
            case SubpassInput:
                return false;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * Is the shading language type boolean (including vectors)?
     */
    public static boolean isBooleanType(byte type) {
        switch (type) {
            case Bool:
            case BVec2:
            case BVec3:
            case BVec4:
                return true;

            case Float:
            case Vec2:
            case Vec3:
            case Vec4:
            case Mat2:
            case Mat3:
            case Mat4:
            case Void:
            case Sampler2D:
            case Int:
            case IVec2:
            case IVec3:
            case IVec4:
            case UInt:
            case UVec2:
            case UVec3:
            case UVec4:
            case Texture2D:
            case Sampler:
            case SubpassInput:
                return false;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * Is the shading language type supported as a uniform block member.
     */
    public static boolean canBeUniformValue(byte type) {
        return isFloatType(type) || isIntegralType(type) || isBooleanType(type);
    }

    /**
     * If the type represents a single value or vector return the number of components, else -1.
     */
    public static int vectorDim(byte type) {
        switch (type) {
            case Bool:
            case Int:
            case UInt:
            case Float:
                return 1;

            case BVec2:
            case IVec2:
            case UVec2:
            case Vec2:
                return 2;

            case BVec3:
            case IVec3:
            case UVec3:
            case Vec3:
                return 3;

            case BVec4:
            case IVec4:
            case UVec4:
            case Vec4:
                return 4;

            case Mat2:
            case Mat3:
            case Mat4:
            case Void:
            case Sampler2D:
            case Texture2D:
            case Sampler:
            case SubpassInput:
                return -1;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * If the type represents a square matrix, return its order; otherwise, -1.
     */
    public static int matrixOrder(byte type) {
        switch (type) {
            case Mat2:
                return 2;

            case Mat3:
                return 3;

            case Mat4:
                return 4;

            case Void:
            case Bool:
            case BVec2:
            case BVec3:
            case BVec4:
            case Int:
            case IVec2:
            case IVec3:
            case IVec4:
            case UInt:
            case UVec2:
            case UVec3:
            case UVec4:
            case Float:
            case Vec2:
            case Vec3:
            case Vec4:
            case Sampler2D:
            case Texture2D:
            case Sampler:
            case SubpassInput:
                return -1;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    public static boolean isCombinedSamplerType(byte type) {
        switch (type) {
            case Sampler2D:
                return true;

            case Void:
            case Float:
            case Vec2:
            case Vec3:
            case Vec4:
            case Mat2:
            case Mat3:
            case Mat4:
            case Int:
            case IVec2:
            case IVec3:
            case IVec4:
            case UInt:
            case UVec2:
            case UVec3:
            case UVec4:
            case Bool:
            case BVec2:
            case BVec3:
            case BVec4:
            case Texture2D:
            case Sampler:
            case SubpassInput:
                return false;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    public static boolean isMatrixType(byte type) {
        switch (type) {
            case Mat2:
            case Mat3:
            case Mat4:
                return true;

            case Void:
            case Bool:
            case BVec2:
            case BVec3:
            case BVec4:
            case Float:
            case Vec2:
            case Vec3:
            case Vec4:
            case Int:
            case IVec2:
            case IVec3:
            case IVec4:
            case UInt:
            case UVec2:
            case UVec3:
            case UVec4:
            case Sampler2D:
            case Texture2D:
            case Sampler:
            case SubpassInput:
                return false;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * Returns the number of locations take up by a given SLType. We assume that all
     * scalar values are 32 bits.
     */
    public static int locationSize(byte type) {
        switch (type) {
            case Bool:
            case BVec2:
            case BVec3:
            case BVec4:
            case Int:
            case IVec2:
            case IVec3:
            case IVec4:
            case UInt:
            case UVec2:
            case UVec3:
            case UVec4:
            case Float:
            case Vec2:
            case Vec3:
            case Vec4:
                return 1;
            case Mat2:
                return 2;
            case Mat3:
                return 3;
            case Mat4:
                return 4;
            case Void:
            case Sampler2D:
            case Texture2D:
            case Sampler:
            case SubpassInput:
                return 0;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    @Nonnull
    public static String typeString(byte type) {
        switch (type) {
            case Void:          return "void";
            case Bool:          return "bool";
            case BVec2:         return "bvec2";
            case BVec3:         return "bvec3";
            case BVec4:         return "bvec4";
            case Int:           return "int";
            case IVec2:         return "ivec2";
            case IVec3:         return "ivec3";
            case IVec4:         return "ivec4";
            case UInt:          return "uint";
            case UVec2:         return "uvec2";
            case UVec3:         return "uvec3";
            case UVec4:         return "uvec4";
            case Float:         return "float";
            case Vec2:          return "vec2";
            case Vec3:          return "vec3";
            case Vec4:          return "vec4";
            case Mat2:          return "mat2";
            case Mat3:          return "mat3";
            case Mat4:          return "mat4";
            case Sampler2D:     return "sampler2D";
            case Texture2D:     return "texture2D";
            case Sampler:       return "sampler";
            case SubpassInput:  return "subpassInput";
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }
}
