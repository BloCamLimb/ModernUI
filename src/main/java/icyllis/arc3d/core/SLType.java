/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.core;

import javax.annotation.Nonnull;

/**
 * Types of shader-language-specific boxed variables we can create, shared constants.
 */
public final class SLType {

    public static final byte
            kVoid = 0,
            kBool = 1,
            kBool2 = 2,
            kBool3 = 3,
            kBool4 = 4,
            kShort = 5,
            kShort2 = 6,
            kShort3 = 7,
            kShort4 = 8,
            kUShort = 9,
            kUShort2 = 10,
            kUShort3 = 11,
            kUShort4 = 12,
            kFloat = 13,
            kFloat2 = 14,
            kFloat3 = 15,
            kFloat4 = 16,
            kFloat2x2 = 17,
            kFloat3x3 = 18,
            kFloat4x4 = 19,
            kHalf = 20,
            kHalf2 = 21,
            kHalf3 = 22,
            kHalf4 = 23,
            kHalf2x2 = 24,
            kHalf3x3 = 25,
            kHalf4x4 = 26,
            kInt = 27,
            kInt2 = 28,
            kInt3 = 29,
            kInt4 = 30,
            kUInt = 31,
            kUInt2 = 32,
            kUInt3 = 33,
            kUInt4 = 34,
            kSampler2D = 35,
            kTexture2D = 36,
            kSampler = 37,
            kSubpassInput = 38;
    public static final byte kLast = kSubpassInput;

    // Debug tool.
    public static boolean checkSLType(byte type) {
        return type >= 0 && type <= kLast;
    }

    //TODO update all methods to new version

    /**
     * Is the shading language type float (including vectors/matrices)?
     */
    public static boolean isFloatType(byte type) {
        switch (type) {
            case kFloat:
            case kFloat2:
            case kFloat3:
            case kFloat4:
            case kFloat2x2:
            case kFloat3x3:
            case kFloat4x4:
                return true;

            case kVoid:
            case kSampler2D:
            case kBool:
            case kBool2:
            case kBool3:
            case kBool4:
            case kInt:
            case kInt2:
            case kInt3:
            case kInt4:
            case kUInt:
            case kUInt2:
            case kUInt3:
            case kUInt4:
            case kTexture2D:
            case kSampler:
            case kSubpassInput:
                return false;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * Is the shading language type integral (including vectors)?
     */
    public static boolean isIntegralType(byte type) {
        switch (type) {
            case kInt:
            case kInt2:
            case kInt3:
            case kInt4:
            case kUInt:
            case kUInt2:
            case kUInt3:
            case kUInt4:
                return true;

            case kFloat:
            case kFloat2:
            case kFloat3:
            case kFloat4:
            case kFloat2x2:
            case kFloat3x3:
            case kFloat4x4:
            case kVoid:
            case kSampler2D:
            case kBool:
            case kBool2:
            case kBool3:
            case kBool4:
            case kTexture2D:
            case kSampler:
            case kSubpassInput:
                return false;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * Is the shading language type boolean (including vectors)?
     */
    public static boolean isBooleanType(byte type) {
        switch (type) {
            case kBool:
            case kBool2:
            case kBool3:
            case kBool4:
                return true;

            case kFloat:
            case kFloat2:
            case kFloat3:
            case kFloat4:
            case kFloat2x2:
            case kFloat3x3:
            case kFloat4x4:
            case kVoid:
            case kSampler2D:
            case kInt:
            case kInt2:
            case kInt3:
            case kInt4:
            case kUInt:
            case kUInt2:
            case kUInt3:
            case kUInt4:
            case kTexture2D:
            case kSampler:
            case kSubpassInput:
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
            case kBool:
            case kInt:
            case kUInt:
            case kFloat:
                return 1;

            case kBool2:
            case kInt2:
            case kUInt2:
            case kFloat2:
                return 2;

            case kBool3:
            case kInt3:
            case kUInt3:
            case kFloat3:
                return 3;

            case kBool4:
            case kInt4:
            case kUInt4:
            case kFloat4:
                return 4;

            case kFloat2x2:
            case kFloat3x3:
            case kFloat4x4:
            case kVoid:
            case kSampler2D:
            case kTexture2D:
            case kSampler:
            case kSubpassInput:
                return -1;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * If the type represents a square matrix, return its order; otherwise, -1.
     */
    public static int matrixOrder(byte type) {
        switch (type) {
            case kFloat2x2:
                return 2;

            case kFloat3x3:
                return 3;

            case kFloat4x4:
                return 4;

            case kVoid:
            case kBool:
            case kBool2:
            case kBool3:
            case kBool4:
            case kInt:
            case kInt2:
            case kInt3:
            case kInt4:
            case kUInt:
            case kUInt2:
            case kUInt3:
            case kUInt4:
            case kFloat:
            case kFloat2:
            case kFloat3:
            case kFloat4:
            case kSampler2D:
            case kTexture2D:
            case kSampler:
            case kSubpassInput:
                return -1;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    public static boolean isCombinedSamplerType(byte type) {
        switch (type) {
            case kSampler2D:
                return true;

            case kVoid:
            case kFloat:
            case kFloat2:
            case kFloat3:
            case kFloat4:
            case kFloat2x2:
            case kFloat3x3:
            case kFloat4x4:
            case kInt:
            case kInt2:
            case kInt3:
            case kInt4:
            case kUInt:
            case kUInt2:
            case kUInt3:
            case kUInt4:
            case kBool:
            case kBool2:
            case kBool3:
            case kBool4:
            case kTexture2D:
            case kSampler:
            case kSubpassInput:
                return false;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    public static boolean isMatrixType(byte type) {
        switch (type) {
            case kFloat2x2:
            case kFloat3x3:
            case kFloat4x4:
                return true;

            case kVoid:
            case kBool:
            case kBool2:
            case kBool3:
            case kBool4:
            case kFloat:
            case kFloat2:
            case kFloat3:
            case kFloat4:
            case kInt:
            case kInt2:
            case kInt3:
            case kInt4:
            case kUInt:
            case kUInt2:
            case kUInt3:
            case kUInt4:
            case kSampler2D:
            case kTexture2D:
            case kSampler:
            case kSubpassInput:
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
            case kBool:
            case kBool2:
            case kBool3:
            case kBool4:
            case kInt:
            case kInt2:
            case kInt3:
            case kInt4:
            case kUInt:
            case kUInt2:
            case kUInt3:
            case kUInt4:
            case kFloat:
            case kFloat2:
            case kFloat3:
            case kFloat4:
                return 1;
            case kFloat2x2:
                return 2;
            case kFloat3x3:
                return 3;
            case kFloat4x4:
                return 4;
            case kVoid:
            case kSampler2D:
            case kTexture2D:
            case kSampler:
            case kSubpassInput:
                return 0;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    @Nonnull
    public static String typeString(byte type) {
        switch (type) {
            case kVoid:
                return "void";
            case kBool:
                return "bool";
            case kBool2:
                return "bvec2";
            case kBool3:
                return "bvec3";
            case kBool4:
                return "bvec4";
            case kInt:
                return "int";
            case kInt2:
                return "ivec2";
            case kInt3:
                return "ivec3";
            case kInt4:
                return "ivec4";
            case kUInt:
                return "uint";
            case kUInt2:
                return "uvec2";
            case kUInt3:
                return "uvec3";
            case kUInt4:
                return "uvec4";
            case kFloat:
                return "float";
            case kFloat2:
                return "vec2";
            case kFloat3:
                return "vec3";
            case kFloat4:
                return "vec4";
            case kFloat2x2:
                return "mat2";
            case kFloat3x3:
                return "mat3";
            case kFloat4x4:
                return "mat4";
            case kSampler2D:
                return "sampler2D";
            case kTexture2D:
                return "texture2D";
            case kSampler:
                return "sampler";
            case kSubpassInput:
                return "subpassInput";
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }
}
