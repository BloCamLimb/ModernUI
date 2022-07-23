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
            BOOL2                   = 2,
            BOOL3                   = 3,
            BOOL4                   = 4,
            SHORT                   = 5,
            SHORT2                  = 6,
            SHORT3                  = 7,
            SHORT4                  = 8,
            USHORT                  = 9,
            USHORT2                 = 10,
            USHORT3                 = 11,
            USHORT4                 = 12,
            FLOAT                   = 13,
            FLOAT2                  = 14,
            FLOAT3                  = 15,
            FLOAT4                  = 16,
            FLOAT2X2                = 17,
            FLOAT3X3                = 18,
            FLOAT4X4                = 19,
            HALF                    = 20,
            HALF2                   = 21,
            HALF3                   = 22,
            HALF4                   = 23,
            HALF2X2                 = 24,
            HALF3X3                 = 25,
            HALF4X4                 = 26,
            INT                     = 27,
            INT2                    = 28,
            INT3                    = 29,
            INT4                    = 30,
            UINT                    = 31,
            UINT2                   = 32,
            UINT3                   = 33,
            UINT4                   = 34,
            TEXTURE_2D_SAMPLER      = 35,
            TEXTURE_2D_RECT_SAMPLER = 36,
            TEXTURE_2D              = 37,
            SAMPLER                 = 38,
            SUBPASS_INPUT           = 39;
    public static final byte LAST = SUBPASS_INPUT;

    /**
     * Is the shading language type float (including vectors/matrices)?
     */
    public static boolean isFloatType(byte type) {
        switch (type) {
            case FLOAT:
            case FLOAT2:
            case FLOAT3:
            case FLOAT4:
            case FLOAT2X2:
            case FLOAT3X3:
            case FLOAT4X4:
            case HALF:
            case HALF2:
            case HALF3:
            case HALF4:
            case HALF2X2:
            case HALF3X3:
            case HALF4X4:
                return true;

            case VOID:
            case TEXTURE_2D_SAMPLER:
            case TEXTURE_2D_RECT_SAMPLER:
            case BOOL:
            case BOOL2:
            case BOOL3:
            case BOOL4:
            case SHORT:
            case SHORT2:
            case SHORT3:
            case SHORT4:
            case USHORT:
            case USHORT2:
            case USHORT3:
            case USHORT4:
            case INT:
            case INT2:
            case INT3:
            case INT4:
            case UINT:
            case UINT2:
            case UINT3:
            case UINT4:
            case TEXTURE_2D:
            case SAMPLER:
            case SUBPASS_INPUT:
                return false;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * Is the shading language type integral (including vectors)?
     */
    public static boolean isIntegralType(byte type) {
        switch (type) {
            case SHORT:
            case SHORT2:
            case SHORT3:
            case SHORT4:
            case USHORT:
            case USHORT2:
            case USHORT3:
            case USHORT4:
            case INT:
            case INT2:
            case INT3:
            case INT4:
            case UINT:
            case UINT2:
            case UINT3:
            case UINT4:
                return true;

            case FLOAT:
            case FLOAT2:
            case FLOAT3:
            case FLOAT4:
            case FLOAT2X2:
            case FLOAT3X3:
            case FLOAT4X4:
            case HALF:
            case HALF2:
            case HALF3:
            case HALF4:
            case HALF2X2:
            case HALF3X3:
            case HALF4X4:
            case VOID:
            case TEXTURE_2D_SAMPLER:
            case TEXTURE_2D_RECT_SAMPLER:
            case BOOL:
            case BOOL2:
            case BOOL3:
            case BOOL4:
            case TEXTURE_2D:
            case SAMPLER:
            case SUBPASS_INPUT:
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
            case FLOAT:
            case HALF:
            case BOOL:
            case SHORT:
            case USHORT:
            case INT:
            case UINT:
                return 1;

            case FLOAT2:
            case HALF2:
            case BOOL2:
            case SHORT2:
            case USHORT2:
            case INT2:
            case UINT2:
                return 2;

            case FLOAT3:
            case HALF3:
            case BOOL3:
            case SHORT3:
            case USHORT3:
            case INT3:
            case UINT3:
                return 3;

            case FLOAT4:
            case HALF4:
            case BOOL4:
            case SHORT4:
            case USHORT4:
            case INT4:
            case UINT4:
                return 4;

            case FLOAT2X2:
            case FLOAT3X3:
            case FLOAT4X4:
            case HALF2X2:
            case HALF3X3:
            case HALF4X4:
            case VOID:
            case TEXTURE_2D_SAMPLER:
            case TEXTURE_2D_RECT_SAMPLER:
            case TEXTURE_2D:
            case SAMPLER:
            case SUBPASS_INPUT:
                return -1;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    public static boolean isCombinedSamplerType(byte type) {
        switch (type) {
            case TEXTURE_2D_SAMPLER:
            case TEXTURE_2D_RECT_SAMPLER:
                return true;

            case VOID:
            case FLOAT:
            case FLOAT2:
            case FLOAT3:
            case FLOAT4:
            case FLOAT2X2:
            case FLOAT3X3:
            case FLOAT4X4:
            case HALF:
            case HALF2:
            case HALF3:
            case HALF4:
            case HALF2X2:
            case HALF3X3:
            case HALF4X4:
            case INT:
            case INT2:
            case INT3:
            case INT4:
            case UINT:
            case UINT2:
            case UINT3:
            case UINT4:
            case BOOL:
            case BOOL2:
            case BOOL3:
            case BOOL4:
            case SHORT:
            case SHORT2:
            case SHORT3:
            case SHORT4:
            case USHORT:
            case USHORT2:
            case USHORT3:
            case USHORT4:
            case TEXTURE_2D:
            case SAMPLER:
            case SUBPASS_INPUT:
                return false;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    public static boolean isMatrixType(byte type) {
        switch (type) {
            case FLOAT2X2:
            case FLOAT3X3:
            case FLOAT4X4:
            case HALF2X2:
            case HALF3X3:
            case HALF4X4:
                return true;

            case VOID:
            case BOOL:
            case BOOL2:
            case BOOL3:
            case BOOL4:
            case SHORT:
            case SHORT2:
            case SHORT3:
            case SHORT4:
            case USHORT:
            case USHORT2:
            case USHORT3:
            case USHORT4:
            case FLOAT:
            case FLOAT2:
            case FLOAT3:
            case FLOAT4:
            case HALF:
            case HALF2:
            case HALF3:
            case HALF4:
            case INT:
            case INT2:
            case INT3:
            case INT4:
            case UINT:
            case UINT2:
            case UINT3:
            case UINT4:
            case TEXTURE_2D_SAMPLER:
            case TEXTURE_2D_RECT_SAMPLER:
            case TEXTURE_2D:
            case SAMPLER:
            case SUBPASS_INPUT:
                return false;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    @Nonnull
    public static String typeString(byte type) {
        switch (type) {
            case VOID:                    return "void";
            case BOOL:                    return "bool";
            case BOOL2:                   return "bool2";
            case BOOL3:                   return "bool3";
            case BOOL4:                   return "bool4";
            case SHORT:                   return "short";
            case SHORT2:                  return "short2";
            case SHORT3:                  return "short3";
            case SHORT4:                  return "short4";
            case USHORT:                  return "ushort";
            case USHORT2:                 return "ushort2";
            case USHORT3:                 return "ushort3";
            case USHORT4:                 return "ushort4";
            case FLOAT:                   return "float";
            case FLOAT2:                  return "float2";
            case FLOAT3:                  return "float3";
            case FLOAT4:                  return "float4";
            case FLOAT2X2:                return "float2x2";
            case FLOAT3X3:                return "float3x3";
            case FLOAT4X4:                return "float4x4";
            case HALF:                    return "half";
            case HALF2:                   return "half2";
            case HALF3:                   return "half3";
            case HALF4:                   return "half4";
            case HALF2X2:                 return "half2x2";
            case HALF3X3:                 return "half3x3";
            case HALF4X4:                 return "half4x4";
            case INT:                     return "int";
            case INT2:                    return "int2";
            case INT3:                    return "int3";
            case INT4:                    return "int4";
            case UINT:                    return "uint";
            case UINT2:                   return "uint2";
            case UINT3:                   return "uint3";
            case UINT4:                   return "uint4";
            case TEXTURE_2D_SAMPLER:      return "sampler2D";
            case TEXTURE_2D_RECT_SAMPLER: return "sampler2DRect";
            case TEXTURE_2D:              return "texture2D";
            case SAMPLER:                 return "sampler";
            case SUBPASS_INPUT:           return "subpassInput";
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }
}
