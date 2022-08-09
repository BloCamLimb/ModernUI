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

package icyllis.arcui.sksl;

import icyllis.arcui.sksl.ir.Type;
import org.lwjgl.util.spvc.Spv;

/**
 * Contains the built-in, core types for SkSL.
 */
public class BuiltinTypes {

    public final Type mFloat;
    public final Type mFloat2;
    public final Type mFloat3;
    public final Type mFloat4;

    public final Type mHalf;
    public final Type mHalf2;
    public final Type mHalf3;
    public final Type mHalf4;

    public final Type mInt;
    public final Type mInt2;
    public final Type mInt3;
    public final Type mInt4;

    public final Type mUInt;
    public final Type mUInt2;
    public final Type mUInt3;
    public final Type mUInt4;

    public final Type mShort;
    public final Type mShort2;
    public final Type mShort3;
    public final Type mShort4;

    public final Type mUShort;
    public final Type mUShort2;
    public final Type mUShort3;
    public final Type mUShort4;

    public final Type mBool;
    public final Type mBool2;
    public final Type mBool3;
    public final Type mBool4;

    public final Type mInvalid;
    public final Type mPoison;
    public final Type mVoid;
    public final Type mFloatLiteral;
    public final Type mIntLiteral;

    public final Type mFloat2x2;
    public final Type mFloat2x3;
    public final Type mFloat2x4;
    public final Type mFloat3x2;
    public final Type mFloat3x3;
    public final Type mFloat3x4;
    public final Type mFloat4x2;
    public final Type mFloat4x3;
    public final Type mFloat4x4;

    public final Type mHalf2x2;
    public final Type mHalf2x3;
    public final Type mHalf2x4;
    public final Type mHalf3x2;
    public final Type mHalf3x3;
    public final Type mHalf3x4;
    public final Type mHalf4x2;
    public final Type mHalf4x3;
    public final Type mHalf4x4;

    public final Type mVec2;
    public final Type mVec3;
    public final Type mVec4;

    public final Type mIVec2;
    public final Type mIVec3;
    public final Type mIVec4;

    public final Type mBVec2;
    public final Type mBVec3;
    public final Type mBVec4;

    public final Type mMat2;
    public final Type mMat3;
    public final Type mMat4;

    public final Type mMat2x2;
    public final Type mMat2x3;
    public final Type mMat2x4;
    public final Type mMat3x2;
    public final Type mMat3x3;
    public final Type mMat3x4;
    public final Type mMat4x2;
    public final Type mMat4x3;
    public final Type mMat4x4;

    public final Type mTexture1D;
    public final Type mTexture2D;
    public final Type mTexture3D;

    /**
     * Initializes the core SkSL types.
     */
    public BuiltinTypes() {
        mFloat = Type.makeScalarType(
                "float", "f", Type.ScalarKind_Float, /*priority=*/10, /*bitWidth=*/32);
        mFloat2 = Type.makeVectorType("float2", "f2", mFloat, /*columns=*/2);
        mFloat3 = Type.makeVectorType("float3", "f3", mFloat, /*columns=*/3);
        mFloat4 = Type.makeVectorType("float4", "f4", mFloat, /*columns=*/4);
        mHalf = Type.makeScalarType(
                "half", "h", Type.ScalarKind_Float, /*priority=*/9, /*bitWidth=*/16);
        mHalf2 = Type.makeVectorType("half2", "h2", mHalf, /*columns=*/2);
        mHalf3 = Type.makeVectorType("half3", "h3", mHalf, /*columns=*/3);
        mHalf4 = Type.makeVectorType("half4", "h4", mHalf, /*columns=*/4);
        mInt = Type.makeScalarType(
                "int", "i", Type.ScalarKind_Signed, /*priority=*/7, /*bitWidth=*/32);
        mInt2 = Type.makeVectorType("int2", "i2", mInt, /*columns=*/2);
        mInt3 = Type.makeVectorType("int3", "i3", mInt, /*columns=*/3);
        mInt4 = Type.makeVectorType("int4", "i4", mInt, /*columns=*/4);
        mUInt = Type.makeScalarType(
                "uint", "I", Type.ScalarKind_Unsigned, /*priority=*/6, /*bitWidth=*/32);
        mUInt2 = Type.makeVectorType("uint2", "I2", mUInt, /*columns=*/2);
        mUInt3 = Type.makeVectorType("uint3", "I3", mUInt, /*columns=*/3);
        mUInt4 = Type.makeVectorType("uint4", "I4", mUInt, /*columns=*/4);
        mShort = Type.makeScalarType(
                "short", "s", Type.ScalarKind_Signed, /*priority=*/4, /*bitWidth=*/16);
        mShort2 = Type.makeVectorType("short2", "s2", mShort, /*columns=*/2);
        mShort3 = Type.makeVectorType("short3", "s3", mShort, /*columns=*/3);
        mShort4 = Type.makeVectorType("short4", "s4", mShort, /*columns=*/4);
        mUShort = Type.makeScalarType(
                "ushort", "S", Type.ScalarKind_Unsigned, /*priority=*/3, /*bitWidth=*/16);
        mUShort2 = Type.makeVectorType("ushort2", "S2", mUShort, /*columns=*/2);
        mUShort3 = Type.makeVectorType("ushort3", "S3", mUShort, /*columns=*/3);
        mUShort4 = Type.makeVectorType("ushort4", "S4", mUShort, /*columns=*/4);
        mBool = Type.makeScalarType(
                "bool", "b", Type.ScalarKind_Boolean, /*priority=*/0, /*bitWidth=*/1);
        mBool2 = Type.makeVectorType("bool2", "b2", mBool, /*columns=*/2);
        mBool3 = Type.makeVectorType("bool3", "b3", mBool, /*columns=*/3);
        mBool4 = Type.makeVectorType("bool4", "b4", mBool, /*columns=*/4);
        mInvalid = Type.makeSpecialType(Compiler.INVALID_TAG, "O", Type.TypeKind_Other);
        mPoison = Type.makeSpecialType(Compiler.POISON_TAG, "P", Type.TypeKind_Other);
        mVoid = Type.makeSpecialType("void", "v", Type.TypeKind_Void);
        mFloatLiteral = Type.makeLiteralType("$floatLiteral", mFloat, /*priority=*/8);
        mIntLiteral = Type.makeLiteralType("$intLiteral", mInt, /*priority=*/5);
        mFloat2x2 = Type.makeMatrixType("float2x2", "f22", mFloat, /*columns=*/2, /*rows=*/2);
        mFloat2x3 = Type.makeMatrixType("float2x3", "f23", mFloat, /*columns=*/2, /*rows=*/3);
        mFloat2x4 = Type.makeMatrixType("float2x4", "f24", mFloat, /*columns=*/2, /*rows=*/4);
        mFloat3x2 = Type.makeMatrixType("float3x2", "f32", mFloat, /*columns=*/3, /*rows=*/2);
        mFloat3x3 = Type.makeMatrixType("float3x3", "f33", mFloat, /*columns=*/3, /*rows=*/3);
        mFloat3x4 = Type.makeMatrixType("float3x4", "f34", mFloat, /*columns=*/3, /*rows=*/4);
        mFloat4x2 = Type.makeMatrixType("float4x2", "f42", mFloat, /*columns=*/4, /*rows=*/2);
        mFloat4x3 = Type.makeMatrixType("float4x3", "f43", mFloat, /*columns=*/4, /*rows=*/3);
        mFloat4x4 = Type.makeMatrixType("float4x4", "f44", mFloat, /*columns=*/4, /*rows=*/4);
        mHalf2x2 = Type.makeMatrixType("half2x2", "h22", mHalf, /*columns=*/2, /*rows=*/2);
        mHalf2x3 = Type.makeMatrixType("half2x3", "h23", mHalf, /*columns=*/2, /*rows=*/3);
        mHalf2x4 = Type.makeMatrixType("half2x4", "h24", mHalf, /*columns=*/2, /*rows=*/4);
        mHalf3x2 = Type.makeMatrixType("half3x2", "h32", mHalf, /*columns=*/3, /*rows=*/2);
        mHalf3x3 = Type.makeMatrixType("half3x3", "h33", mHalf, /*columns=*/3, /*rows=*/3);
        mHalf3x4 = Type.makeMatrixType("half3x4", "h34", mHalf, /*columns=*/3, /*rows=*/4);
        mHalf4x2 = Type.makeMatrixType("half4x2", "h42", mHalf, /*columns=*/4, /*rows=*/2);
        mHalf4x3 = Type.makeMatrixType("half4x3", "h43", mHalf, /*columns=*/4, /*rows=*/3);
        mHalf4x4 = Type.makeMatrixType("half4x4", "h44", mHalf, /*columns=*/4, /*rows=*/4);
        mVec2 = Type.makeAliasType("vec2", mFloat2);
        mVec3 = Type.makeAliasType("vec3", mFloat3);
        mVec4 = Type.makeAliasType("vec4", mFloat4);
        mIVec2 = Type.makeAliasType("ivec2", mInt2);
        mIVec3 = Type.makeAliasType("ivec3", mInt3);
        mIVec4 = Type.makeAliasType("ivec4", mInt4);
        mBVec2 = Type.makeAliasType("bvec2", mBool2);
        mBVec3 = Type.makeAliasType("bvec3", mBool3);
        mBVec4 = Type.makeAliasType("bvec4", mBool4);
        mMat2 = Type.makeAliasType("mat2", mFloat2x2);
        mMat3 = Type.makeAliasType("mat3", mFloat3x3);
        mMat4 = Type.makeAliasType("mat4", mFloat4x4);
        mMat2x2 = Type.makeAliasType("mat2x2", mFloat2x2);
        mMat2x3 = Type.makeAliasType("mat2x3", mFloat2x3);
        mMat2x4 = Type.makeAliasType("mat2x4", mFloat2x4);
        mMat3x2 = Type.makeAliasType("mat3x2", mFloat3x2);
        mMat3x3 = Type.makeAliasType("mat3x3", mFloat3x3);
        mMat3x4 = Type.makeAliasType("mat3x4", mFloat3x4);
        mMat4x2 = Type.makeAliasType("mat4x2", mFloat4x2);
        mMat4x3 = Type.makeAliasType("mat4x3", mFloat4x3);
        mMat4x4 = Type.makeAliasType("mat4x4", mFloat4x4);
        mTexture1D = Type.makeTextureType("texture1D",
                Spv.SpvDim1D,
                /*isDepth=*/false,
                /*isLayered=*/false,
                /*isMultisampled=*/false,
                /*isSampled=*/true);
        mTexture2D = Type.makeTextureType("texture2D",
                Spv.SpvDim2D,
                /*isDepth=*/false,
                /*isLayered=*/false,
                /*isMultisampled=*/false,
                /*isSampled=*/true);
        mTexture3D = Type.makeTextureType("texture3D",
                Spv.SpvDim3D,
                /*isDepth=*/false,
                /*isLayered=*/false,
                /*isMultisampled=*/false,
                /*isSampled=*/true);
    }
}
