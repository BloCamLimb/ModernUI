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

package icyllis.akashigi.slang;

import icyllis.akashigi.slang.ir.Type;
import org.lwjgl.util.spvc.Spv;

/**
 * Contains the built-in, core SL types.
 */
public final class BuiltinTypes {

    public final Type mVoid;

    public final Type mBool;
    public final Type mBool2;
    public final Type mBool3;
    public final Type mBool4;

    public final Type mShort;
    public final Type mShort2;
    public final Type mShort3;
    public final Type mShort4;

    public final Type mUShort;
    public final Type mUShort2;
    public final Type mUShort3;
    public final Type mUShort4;

    public final Type mInt;
    public final Type mInt2;
    public final Type mInt3;
    public final Type mInt4;

    public final Type mUInt;
    public final Type mUInt2;
    public final Type mUInt3;
    public final Type mUInt4;

    public final Type mHalf;
    public final Type mHalf2;
    public final Type mHalf3;
    public final Type mHalf4;

    public final Type mFloat;
    public final Type mFloat2;
    public final Type mFloat3;
    public final Type mFloat4;

    public final Type mDouble;
    public final Type mDouble2;
    public final Type mDouble3;
    public final Type mDouble4;

    public final Type mHalf2x2;
    public final Type mHalf2x3;
    public final Type mHalf2x4;

    public final Type mHalf3x2;
    public final Type mHalf3x3;
    public final Type mHalf3x4;

    public final Type mHalf4x2;
    public final Type mHalf4x3;
    public final Type mHalf4x4;

    public final Type mFloat2x2;
    public final Type mFloat2x3;
    public final Type mFloat2x4;

    public final Type mFloat3x2;
    public final Type mFloat3x3;
    public final Type mFloat3x4;

    public final Type mFloat4x2;
    public final Type mFloat4x3;
    public final Type mFloat4x4;

    public final Type mDouble2x2;
    public final Type mDouble2x3;
    public final Type mDouble2x4;

    public final Type mDouble3x2;
    public final Type mDouble3x3;
    public final Type mDouble3x4;

    public final Type mDouble4x2;
    public final Type mDouble4x3;
    public final Type mDouble4x4;

    public final Type mVec2;
    public final Type mVec3;
    public final Type mVec4;

    public final Type mDVec2;
    public final Type mDVec3;
    public final Type mDVec4;

    public final Type mBVec2;
    public final Type mBVec3;
    public final Type mBVec4;

    public final Type mIVec2;
    public final Type mIVec3;
    public final Type mIVec4;

    public final Type mUVec2;
    public final Type mUVec3;
    public final Type mUVec4;

    public final Type mMin16Int;
    public final Type mMin16Int2;
    public final Type mMin16Int3;
    public final Type mMin16Int4;

    public final Type mMin16UInt;
    public final Type mMin16UInt2;
    public final Type mMin16UInt3;
    public final Type mMin16UInt4;

    public final Type mMin16Float;
    public final Type mMin16Float2;
    public final Type mMin16Float3;
    public final Type mMin16Float4;

    public final Type mInt32;
    public final Type mI32Vec2;
    public final Type mI32Vec3;
    public final Type mI32Vec4;

    public final Type mUInt32;
    public final Type mU32Vec2;
    public final Type mU32Vec3;
    public final Type mU32Vec4;

    public final Type mFloat32;
    public final Type mF32Vec2;
    public final Type mF32Vec3;
    public final Type mF32Vec4;

    public final Type mFloat64;
    public final Type mF64Vec2;
    public final Type mF64Vec3;
    public final Type mF64Vec4;

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

    public final Type mDMat2;
    public final Type mDMat3;
    public final Type mDMat4;

    public final Type mDMat2x2;
    public final Type mDMat2x3;
    public final Type mDMat2x4;

    public final Type mDMat3x2;
    public final Type mDMat3x3;
    public final Type mDMat3x4;

    public final Type mDMat4x2;
    public final Type mDMat4x3;
    public final Type mDMat4x4;

    public final Type mF32Mat2;
    public final Type mF32Mat3;
    public final Type mF32Mat4;

    public final Type mF32Mat2x2;
    public final Type mF32Mat2x3;
    public final Type mF32Mat2x4;

    public final Type mF32Mat3x2;
    public final Type mF32Mat3x3;
    public final Type mF32Mat3x4;

    public final Type mF32Mat4x2;
    public final Type mF32Mat4x3;
    public final Type mF32Mat4x4;

    public final Type mF64Mat2;
    public final Type mF64Mat3;
    public final Type mF64Mat4;

    public final Type mF64Mat2x2;
    public final Type mF64Mat2x3;
    public final Type mF64Mat2x4;

    public final Type mF64Mat3x2;
    public final Type mF64Mat3x3;
    public final Type mF64Mat3x4;

    public final Type mF64Mat4x2;
    public final Type mF64Mat4x3;
    public final Type mF64Mat4x4;

    public final Type mImage1D;
    public final Type mImage2D;
    public final Type mImage3D;
    public final Type mImageCube;
    public final Type mImageBuffer;
    public final Type mImage1DArray;
    public final Type mImage2DArray;
    public final Type mImageCubeArray;
    public final Type mImage2DMS;
    public final Type mImage2DMSArray;

    public final Type mSubpassInput;
    public final Type mSubpassInputMS;

    public final Type mTexture1D;
    public final Type mTexture2D;
    public final Type mTexture3D;
    public final Type mTextureCube;
    public final Type mTextureBuffer;
    public final Type mTexture1DArray;
    public final Type mTexture2DArray;
    public final Type mTextureCubeArray;
    public final Type mTexture2DMS;
    public final Type mTexture2DMSArray;

    public final Type mSampler;
    public final Type mSamplerShadow;

    public final Type mSampler1D;
    public final Type mSampler2D;
    public final Type mSampler3D;
    public final Type mSamplerCube;
    public final Type mSamplerBuffer;
    public final Type mSampler1DArray;
    public final Type mSampler2DArray;
    public final Type mSamplerCubeArray;
    public final Type mSampler2DMS;
    public final Type mSampler2DMSArray;

    public final Type mSampler1DShadow;
    public final Type mSampler2DShadow;
    public final Type mSamplerCubeShadow;
    public final Type mSampler1DArrayShadow;
    public final Type mSampler2DArrayShadow;
    public final Type mSamplerCubeArrayShadow;

    public final Type mInvalid;

    public final Type mGenFType;
    public final Type mGenIType;

    public final Type mPoison;

    /**
     * Initializes the core SL types.
     */
    public BuiltinTypes() {
        mVoid = Type.makeSpecialType("void", "v", Type.TYPE_KIND_VOID);

        // scalar and vector types

        mBool = Type.makeScalarType(
                "bool", "b", Type.SCALAR_KIND_BOOLEAN, /*priority=*/0, /*bitWidth=*/1);
        mBool2 = Type.makeVectorType("bool2", "b2", mBool, /*vectorSize*/2);
        mBool3 = Type.makeVectorType("bool3", "b3", mBool, /*vectorSize*/3);
        mBool4 = Type.makeVectorType("bool4", "b4", mBool, /*vectorSize*/4);

        mShort = Type.makeScalarType(
                "short", "s", Type.SCALAR_KIND_SIGNED, /*priority=*/3, /*bitWidth=*/16);
        mShort2 = Type.makeVectorType("short2", "s2", mShort, /*vectorSize*/2);
        mShort3 = Type.makeVectorType("short3", "s3", mShort, /*vectorSize*/3);
        mShort4 = Type.makeVectorType("short4", "s4", mShort, /*vectorSize*/4);

        mUShort = Type.makeScalarType(
                "ushort", "S", Type.SCALAR_KIND_UNSIGNED, /*priority=*/4, /*bitWidth=*/16);
        mUShort2 = Type.makeVectorType("ushort2", "S2", mUShort, /*vectorSize*/2);
        mUShort3 = Type.makeVectorType("ushort3", "S3", mUShort, /*vectorSize*/3);
        mUShort4 = Type.makeVectorType("ushort4", "S4", mUShort, /*vectorSize*/4);

        mInt = Type.makeScalarType(
                "int", "i", Type.SCALAR_KIND_SIGNED, /*priority=*/6, /*bitWidth=*/32);
        mInt2 = Type.makeVectorType("int2", "i2", mInt, /*vectorSize*/2);
        mInt3 = Type.makeVectorType("int3", "i3", mInt, /*vectorSize*/3);
        mInt4 = Type.makeVectorType("int4", "i4", mInt, /*vectorSize*/4);

        mUInt = Type.makeScalarType(
                "uint", "I", Type.SCALAR_KIND_UNSIGNED, /*priority=*/7, /*bitWidth=*/32);
        mUInt2 = Type.makeVectorType("uint2", "I2", mUInt, /*vectorSize*/2);
        mUInt3 = Type.makeVectorType("uint3", "I3", mUInt, /*vectorSize*/3);
        mUInt4 = Type.makeVectorType("uint4", "I4", mUInt, /*vectorSize*/4);

        mHalf = Type.makeScalarType(
                "half", "h", Type.SCALAR_KIND_FLOAT, /*priority=*/12, /*bitWidth=*/16);
        mHalf2 = Type.makeVectorType("half2", "h2", mHalf, /*vectorSize*/2);
        mHalf3 = Type.makeVectorType("half3", "h3", mHalf, /*vectorSize*/3);
        mHalf4 = Type.makeVectorType("half4", "h4", mHalf, /*vectorSize*/4);

        mFloat = Type.makeScalarType(
                "float", "f", Type.SCALAR_KIND_FLOAT, /*priority=*/13, /*bitWidth=*/32);
        mFloat2 = Type.makeVectorType("float2", "f2", mFloat, /*vectorSize*/2);
        mFloat3 = Type.makeVectorType("float3", "f3", mFloat, /*vectorSize*/3);
        mFloat4 = Type.makeVectorType("float4", "f4", mFloat, /*vectorSize*/4);

        mDouble = Type.makeScalarType(
                "double", "d", Type.SCALAR_KIND_FLOAT, /*priority=*/15, /*bitWidth=*/64);
        mDouble2 = Type.makeVectorType("double2", "d2", mDouble, /*vectorSize*/2);
        mDouble3 = Type.makeVectorType("double3", "d3", mDouble, /*vectorSize*/3);
        mDouble4 = Type.makeVectorType("double4", "d4", mDouble, /*vectorSize*/4);

        // matrix types

        mHalf2x2 = Type.makeMatrixType("half2x2", "h22", mHalf, /*columns=*/2, /*rows=*/2);
        mHalf2x3 = Type.makeMatrixType("half2x3", "h23", mHalf, /*columns=*/2, /*rows=*/3);
        mHalf2x4 = Type.makeMatrixType("half2x4", "h24", mHalf, /*columns=*/2, /*rows=*/4);

        mHalf3x2 = Type.makeMatrixType("half3x2", "h32", mHalf, /*columns=*/3, /*rows=*/2);
        mHalf3x3 = Type.makeMatrixType("half3x3", "h33", mHalf, /*columns=*/3, /*rows=*/3);
        mHalf3x4 = Type.makeMatrixType("half3x4", "h34", mHalf, /*columns=*/3, /*rows=*/4);

        mHalf4x2 = Type.makeMatrixType("half4x2", "h42", mHalf, /*columns=*/4, /*rows=*/2);
        mHalf4x3 = Type.makeMatrixType("half4x3", "h43", mHalf, /*columns=*/4, /*rows=*/3);
        mHalf4x4 = Type.makeMatrixType("half4x4", "h44", mHalf, /*columns=*/4, /*rows=*/4);

        mFloat2x2 = Type.makeMatrixType("float2x2", "f22", mFloat, /*columns=*/2, /*rows=*/2);
        mFloat2x3 = Type.makeMatrixType("float2x3", "f23", mFloat, /*columns=*/2, /*rows=*/3);
        mFloat2x4 = Type.makeMatrixType("float2x4", "f24", mFloat, /*columns=*/2, /*rows=*/4);

        mFloat3x2 = Type.makeMatrixType("float3x2", "f32", mFloat, /*columns=*/3, /*rows=*/2);
        mFloat3x3 = Type.makeMatrixType("float3x3", "f33", mFloat, /*columns=*/3, /*rows=*/3);
        mFloat3x4 = Type.makeMatrixType("float3x4", "f34", mFloat, /*columns=*/3, /*rows=*/4);

        mFloat4x2 = Type.makeMatrixType("float4x2", "f42", mFloat, /*columns=*/4, /*rows=*/2);
        mFloat4x3 = Type.makeMatrixType("float4x3", "f43", mFloat, /*columns=*/4, /*rows=*/3);
        mFloat4x4 = Type.makeMatrixType("float4x4", "f44", mFloat, /*columns=*/4, /*rows=*/4);

        mDouble2x2 = Type.makeMatrixType("double2x2", "d22", mDouble, /*columns=*/2, /*rows=*/2);
        mDouble2x3 = Type.makeMatrixType("double2x3", "d23", mDouble, /*columns=*/2, /*rows=*/3);
        mDouble2x4 = Type.makeMatrixType("double2x4", "d24", mDouble, /*columns=*/2, /*rows=*/4);

        mDouble3x2 = Type.makeMatrixType("double3x2", "d32", mDouble, /*columns=*/3, /*rows=*/2);
        mDouble3x3 = Type.makeMatrixType("double3x3", "d33", mDouble, /*columns=*/3, /*rows=*/3);
        mDouble3x4 = Type.makeMatrixType("double3x4", "d34", mDouble, /*columns=*/3, /*rows=*/4);

        mDouble4x2 = Type.makeMatrixType("double4x2", "d42", mDouble, /*columns=*/4, /*rows=*/2);
        mDouble4x3 = Type.makeMatrixType("double4x3", "d43", mDouble, /*columns=*/4, /*rows=*/3);
        mDouble4x4 = Type.makeMatrixType("double4x4", "d44", mDouble, /*columns=*/4, /*rows=*/4);

        // GLSL vector aliases

        mVec2 = Type.makeAliasType("vec2", mFloat2);
        mVec3 = Type.makeAliasType("vec3", mFloat3);
        mVec4 = Type.makeAliasType("vec4", mFloat4);

        mDVec2 = Type.makeAliasType("dvec2", mDouble2);
        mDVec3 = Type.makeAliasType("dvec3", mDouble3);
        mDVec4 = Type.makeAliasType("dvec4", mDouble4);

        mBVec2 = Type.makeAliasType("bvec2", mBool2);
        mBVec3 = Type.makeAliasType("bvec3", mBool3);
        mBVec4 = Type.makeAliasType("bvec4", mBool4);

        mIVec2 = Type.makeAliasType("ivec2", mInt2);
        mIVec3 = Type.makeAliasType("ivec3", mInt3);
        mIVec4 = Type.makeAliasType("ivec4", mInt4);

        mUVec2 = Type.makeAliasType("uvec2", mUInt2);
        mUVec3 = Type.makeAliasType("uvec3", mUInt3);
        mUVec4 = Type.makeAliasType("uvec4", mUInt4);

        // HLSL minimum aliases

        mMin16Int = Type.makeAliasType("min16int", mShort);
        mMin16Int2 = Type.makeAliasType("min16int2", mShort2);
        mMin16Int3 = Type.makeAliasType("min16int3", mShort3);
        mMin16Int4 = Type.makeAliasType("min16int4", mShort4);

        mMin16UInt = Type.makeAliasType("min16uint", mUShort);
        mMin16UInt2 = Type.makeAliasType("min16uint2", mUShort2);
        mMin16UInt3 = Type.makeAliasType("min16uint3", mUShort3);
        mMin16UInt4 = Type.makeAliasType("min16uint4", mUShort4);

        mMin16Float = Type.makeAliasType("min16float", mHalf);
        mMin16Float2 = Type.makeAliasType("min16float2", mHalf2);
        mMin16Float3 = Type.makeAliasType("min16float3", mHalf3);
        mMin16Float4 = Type.makeAliasType("min16float4", mHalf4);

        // GLSL extension scalar and vector aliases

        mInt32 = Type.makeAliasType("int32_t", mInt);
        mI32Vec2 = Type.makeAliasType("i32vec2", mInt2);
        mI32Vec3 = Type.makeAliasType("i32vec3", mInt3);
        mI32Vec4 = Type.makeAliasType("i32vec4", mInt4);

        mUInt32 = Type.makeAliasType("uint32_t", mUInt);
        mU32Vec2 = Type.makeAliasType("u32vec2", mUInt2);
        mU32Vec3 = Type.makeAliasType("u32vec3", mUInt3);
        mU32Vec4 = Type.makeAliasType("u32vec4", mUInt4);

        mFloat32 = Type.makeAliasType("float32_t", mFloat);
        mF32Vec2 = Type.makeAliasType("f32vec2", mFloat2);
        mF32Vec3 = Type.makeAliasType("f32vec3", mFloat3);
        mF32Vec4 = Type.makeAliasType("f32vec4", mFloat4);

        mFloat64 = Type.makeAliasType("float64_t", mDouble);
        mF64Vec2 = Type.makeAliasType("f64vec2", mDouble2);
        mF64Vec3 = Type.makeAliasType("f64vec3", mDouble3);
        mF64Vec4 = Type.makeAliasType("f64vec4", mDouble4);

        // GLSL matrix aliases

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

        mDMat2 = Type.makeAliasType("dmat2", mDouble2x2);
        mDMat3 = Type.makeAliasType("dmat3", mDouble3x3);
        mDMat4 = Type.makeAliasType("dmat4", mDouble4x4);

        mDMat2x2 = Type.makeAliasType("dmat2x2", mDouble2x2);
        mDMat2x3 = Type.makeAliasType("dmat2x3", mDouble2x3);
        mDMat2x4 = Type.makeAliasType("dmat2x4", mDouble2x4);

        mDMat3x2 = Type.makeAliasType("dmat3x2", mDouble3x2);
        mDMat3x3 = Type.makeAliasType("dmat3x3", mDouble3x3);
        mDMat3x4 = Type.makeAliasType("dmat3x4", mDouble3x4);

        mDMat4x2 = Type.makeAliasType("dmat4x2", mDouble4x2);
        mDMat4x3 = Type.makeAliasType("dmat4x3", mDouble4x3);
        mDMat4x4 = Type.makeAliasType("dmat4x4", mDouble4x4);

        // GLSL extension matrix aliases

        mF32Mat2 = Type.makeAliasType("f32mat2", mFloat2x2);
        mF32Mat3 = Type.makeAliasType("f32mat3", mFloat3x3);
        mF32Mat4 = Type.makeAliasType("f32mat4", mFloat4x4);

        mF32Mat2x2 = Type.makeAliasType("f32mat2x2", mFloat2x2);
        mF32Mat2x3 = Type.makeAliasType("f32mat2x3", mFloat2x3);
        mF32Mat2x4 = Type.makeAliasType("f32mat2x4", mFloat2x4);

        mF32Mat3x2 = Type.makeAliasType("f32mat3x2", mFloat3x2);
        mF32Mat3x3 = Type.makeAliasType("f32mat3x3", mFloat3x3);
        mF32Mat3x4 = Type.makeAliasType("f32mat3x4", mFloat3x4);

        mF32Mat4x2 = Type.makeAliasType("f32mat4x2", mFloat4x2);
        mF32Mat4x3 = Type.makeAliasType("f32mat4x3", mFloat4x3);
        mF32Mat4x4 = Type.makeAliasType("f32mat4x4", mFloat4x4);

        mF64Mat2 = Type.makeAliasType("f64mat2", mDouble2x2);
        mF64Mat3 = Type.makeAliasType("f64mat3", mDouble3x3);
        mF64Mat4 = Type.makeAliasType("f64mat4", mDouble4x4);

        mF64Mat2x2 = Type.makeAliasType("f64mat2x2", mDouble2x2);
        mF64Mat2x3 = Type.makeAliasType("f64mat2x3", mDouble2x3);
        mF64Mat2x4 = Type.makeAliasType("f64mat2x4", mDouble2x4);

        mF64Mat3x2 = Type.makeAliasType("f64mat3x2", mDouble3x2);
        mF64Mat3x3 = Type.makeAliasType("f64mat3x3", mDouble3x3);
        mF64Mat3x4 = Type.makeAliasType("f64mat3x4", mDouble3x4);

        mF64Mat4x2 = Type.makeAliasType("f64mat4x2", mDouble4x2);
        mF64Mat4x3 = Type.makeAliasType("f64mat4x3", mDouble4x3);
        mF64Mat4x4 = Type.makeAliasType("f64mat4x4", mDouble4x4);

        // opaque types (image, texture, sampler, combined sampler)

        mImage1D = Type.makeImageType("image1D", "M1", mFloat,
                Spv.SpvDim1D, /*isArrayed=*/false, /*isMultisampled=*/false);
        mImage2D = Type.makeImageType("image2D", "M2", mFloat,
                Spv.SpvDim2D, /*isArrayed=*/false, /*isMultisampled=*/false);
        mImage3D = Type.makeImageType("image3D", "M3", mFloat,
                Spv.SpvDim3D, /*isArrayed=*/false, /*isMultisampled=*/false);
        mImageCube = Type.makeImageType("imageCube", "MC", mFloat,
                Spv.SpvDimCube, /*isArrayed=*/false, /*isMultisampled=*/false);
        mImageBuffer = Type.makeImageType("imageBuffer", "MB", mFloat,
                Spv.SpvDimBuffer, /*isArrayed=*/false, /*isMultisampled=*/false);
        mImage1DArray = Type.makeImageType("image1DArray", "M1A", mFloat,
                Spv.SpvDim1D, /*isArrayed=*/true, /*isMultisampled=*/false);
        mImage2DArray = Type.makeImageType("image2DArray", "M2A", mFloat,
                Spv.SpvDim2D, /*isArrayed=*/true, /*isMultisampled=*/false);
        mImageCubeArray = Type.makeImageType("imageCubeArray", "MCA", mFloat,
                Spv.SpvDimCube, /*isArrayed=*/true, /*isMultisampled=*/false);
        mImage2DMS = Type.makeImageType("image2DMS", "MM", mFloat,
                Spv.SpvDim2D, /*isArrayed=*/false, /*isMultisampled=*/true);
        mImage2DMSArray = Type.makeImageType("image2DMSArray", "MMA", mFloat,
                Spv.SpvDim2D, /*isArrayed=*/true, /*isMultisampled=*/true);

        mSubpassInput = Type.makeImageType("subpassInput", "MP", mFloat,
                Spv.SpvDimSubpassData, /*isArrayed=*/false, /*isMultisampled=*/false);
        mSubpassInputMS = Type.makeImageType("subpassInputMS", "MPM", mFloat,
                Spv.SpvDimSubpassData, /*isArrayed=*/false, /*isMultisampled=*/true);

        mTexture1D = Type.makeTextureType("texture1D", "T1", mFloat,
                Spv.SpvDim1D, /*isArrayed=*/false, /*isMultisampled=*/false);
        mTexture2D = Type.makeTextureType("texture2D", "T2", mFloat,
                Spv.SpvDim2D, /*isArrayed=*/false, /*isMultisampled=*/false);
        mTexture3D = Type.makeTextureType("texture3D", "T3", mFloat,
                Spv.SpvDim3D, /*isArrayed=*/false, /*isMultisampled=*/false);
        mTextureCube = Type.makeTextureType("textureCube", "TC", mFloat,
                Spv.SpvDimCube, /*isArrayed=*/false, /*isMultisampled=*/false);
        mTextureBuffer = Type.makeTextureType("textureBuffer", "TB", mFloat,
                Spv.SpvDimBuffer, /*isArrayed=*/false, /*isMultisampled=*/false);
        mTexture1DArray = Type.makeTextureType("texture1DArray", "T1A", mFloat,
                Spv.SpvDim1D, /*isArrayed=*/true, /*isMultisampled=*/false);
        mTexture2DArray = Type.makeTextureType("texture2DArray", "T2A", mFloat,
                Spv.SpvDim2D, /*isArrayed=*/true, /*isMultisampled=*/false);
        mTextureCubeArray = Type.makeTextureType("textureCubeArray", "TCA", mFloat,
                Spv.SpvDimCube, /*isArrayed=*/true, /*isMultisampled=*/false);
        mTexture2DMS = Type.makeTextureType("texture2DMS", "TM", mFloat,
                Spv.SpvDim2D, /*isArrayed=*/false, /*isMultisampled=*/true);
        mTexture2DMSArray = Type.makeTextureType("texture2DMSArray", "TMA", mFloat,
                Spv.SpvDim2D, /*isArrayed=*/true, /*isMultisampled=*/true);

        mSampler = Type.makeSamplerType("sampler", "ss", mVoid, /*isShadow*/false);
        mSamplerShadow = Type.makeSamplerType("samplerShadow", "sss", mVoid, /*isShadow*/true);

        mSampler1D = Type.makeCombinedType("sampler1D", "Z1", mFloat,
                Spv.SpvDim1D, /*isShadow*/false, /*isArrayed=*/false, /*isMultisampled=*/false);
        mSampler2D = Type.makeCombinedType("sampler2D", "Z2", mFloat,
                Spv.SpvDim2D, /*isShadow*/false, /*isArrayed=*/false, /*isMultisampled=*/false);
        mSampler3D = Type.makeCombinedType("sampler3D", "Z3", mFloat,
                Spv.SpvDim3D, /*isShadow*/false, /*isArrayed=*/false, /*isMultisampled=*/false);
        mSamplerCube = Type.makeCombinedType("samplerCube", "ZC", mFloat,
                Spv.SpvDimCube, /*isShadow*/false, /*isArrayed=*/false, /*isMultisampled=*/false);
        mSamplerBuffer = Type.makeCombinedType("samplerBuffer", "ZB", mFloat,
                Spv.SpvDimBuffer, /*isShadow*/false, /*isArrayed=*/false, /*isMultisampled=*/false);
        mSampler1DArray = Type.makeCombinedType("sampler1DArray", "Z1A", mFloat,
                Spv.SpvDim1D, /*isShadow*/false, /*isArrayed=*/true, /*isMultisampled=*/false);
        mSampler2DArray = Type.makeCombinedType("sampler2DArray", "Z2A", mFloat,
                Spv.SpvDim2D, /*isShadow*/false, /*isArrayed=*/true, /*isMultisampled=*/false);
        mSamplerCubeArray = Type.makeCombinedType("samplerCubeArray", "ZCA", mFloat,
                Spv.SpvDimCube, /*isShadow*/false, /*isArrayed=*/true, /*isMultisampled=*/false);
        mSampler2DMS = Type.makeCombinedType("sampler2DMS", "ZM", mFloat,
                Spv.SpvDim2D, /*isShadow*/false, /*isArrayed=*/false, /*isMultisampled=*/true);
        mSampler2DMSArray = Type.makeCombinedType("sampler2DMSArray", "ZMA", mFloat,
                Spv.SpvDim2D, /*isShadow*/false, /*isArrayed=*/true, /*isMultisampled=*/true);

        mSampler1DShadow = Type.makeCombinedType("sampler1DShadow", "Z4", mFloat,
                Spv.SpvDim1D, /*isShadow*/true, /*isArrayed=*/false, /*isMultisampled=*/false);
        mSampler2DShadow = Type.makeCombinedType("sampler2DShadow", "Z5", mFloat,
                Spv.SpvDim2D, /*isShadow*/true, /*isArrayed=*/false, /*isMultisampled=*/false);
        mSamplerCubeShadow = Type.makeCombinedType("samplerCubeShadow", "ZX", mFloat,
                Spv.SpvDimCube, /*isShadow*/true, /*isArrayed=*/false, /*isMultisampled=*/false);
        mSampler1DArrayShadow = Type.makeCombinedType("sampler1DArrayShadow", "Z4A", mFloat,
                Spv.SpvDim1D, /*isShadow*/true, /*isArrayed=*/true, /*isMultisampled=*/false);
        mSampler2DArrayShadow = Type.makeCombinedType("sampler2DArrayShadow", "Z5A", mFloat,
                Spv.SpvDim2D, /*isShadow*/true, /*isArrayed=*/true, /*isMultisampled=*/false);
        mSamplerCubeArrayShadow = Type.makeCombinedType("samplerCubeArrayShadow", "ZXA", mFloat,
                Spv.SpvDimCube, /*isShadow*/true, /*isArrayed=*/true, /*isMultisampled=*/false);

        mInvalid = Type.makeSpecialType(Compiler.INVALID_TAG, "O", Type.TYPE_KIND_OTHER);

        mGenFType = Type.makeGenericType("__genFType", mFloat, mFloat2, mFloat3, mFloat4);
        mGenIType = Type.makeGenericType("__genIType", mInt, mInt2, mInt3, mInt4);

        mPoison = Type.makeSpecialType(Compiler.POISON_TAG, "P", Type.TYPE_KIND_OTHER);
    }
}
