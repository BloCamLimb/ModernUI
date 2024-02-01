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

import icyllis.arc3d.compiler.tree.Type;
import org.lwjgl.util.spvc.Spv;

/**
 * Contains the built-in, core SL types.
 */
public final class BuiltinTypes {

    /**
     * For functions that do not return a value.
     */
    public final Type mVoid;

    /**
     * A conditional type, taking on values of true or false.
     */
    public final Type mBool;
    public final Type mBool2;
    public final Type mBool3;
    public final Type mBool4;

    /**
     * A minimum 16-bit signed integer.
     */
    public final Type mShort;
    public final Type mShort2;
    public final Type mShort3;
    public final Type mShort4;

    /**
     * A minimum 16-bit unsigned integer.
     */
    public final Type mUShort;
    public final Type mUShort2;
    public final Type mUShort3;
    public final Type mUShort4;

    /**
     * A 32-bit signed integer.
     */
    public final Type mInt;
    public final Type mInt2;
    public final Type mInt3;
    public final Type mInt4;

    /**
     * A 32-bit unsigned integer.
     */
    public final Type mUInt;
    public final Type mUInt2;
    public final Type mUInt3;
    public final Type mUInt4;

    /**
     * A minimum 16-bit floating point value.
     */
    public final Type mHalf;
    public final Type mHalf2;
    public final Type mHalf3;
    public final Type mHalf4;

    /**
     * A 32-bit floating point value.
     */
    public final Type mFloat;
    public final Type mFloat2;
    public final Type mFloat3;
    public final Type mFloat4;

    /**
     * A minimum 16-bit floating point matrix.
     */
    public final Type mHalf2x2;
    public final Type mHalf2x3;
    public final Type mHalf2x4;
    public final Type mHalf3x2;
    public final Type mHalf3x3;
    public final Type mHalf3x4;
    public final Type mHalf4x2;
    public final Type mHalf4x3;
    public final Type mHalf4x4;

    /**
     * A 32-bit floating point matrix.
     */
    public final Type mFloat2x2;
    public final Type mFloat2x3;
    public final Type mFloat2x4;
    public final Type mFloat3x2;
    public final Type mFloat3x3;
    public final Type mFloat3x4;
    public final Type mFloat4x2;
    public final Type mFloat4x3;
    public final Type mFloat4x4;

    /**
     * GLSL aliases.
     */
    public final Type mVec2;
    public final Type mVec3;
    public final Type mVec4;

    /**
     * GLSL aliases.
     */
    public final Type mBVec2;
    public final Type mBVec3;
    public final Type mBVec4;

    /**
     * GLSL aliases.
     */
    public final Type mIVec2;
    public final Type mIVec3;
    public final Type mIVec4;

    /**
     * GLSL aliases.
     */
    public final Type mUVec2;
    public final Type mUVec3;
    public final Type mUVec4;

    /**
     * HLSL aliases.
     */
    public final Type mMin16Int;
    public final Type mMin16Int2;
    public final Type mMin16Int3;
    public final Type mMin16Int4;

    /**
     * HLSL aliases.
     */
    public final Type mMin16UInt;
    public final Type mMin16UInt2;
    public final Type mMin16UInt3;
    public final Type mMin16UInt4;

    /**
     * HLSL aliases.
     */
    public final Type mMin16Float;
    public final Type mMin16Float2;
    public final Type mMin16Float3;
    public final Type mMin16Float4;

    /**
     * GLSL aliases.
     */
    public final Type mInt32;
    public final Type mI32Vec2;
    public final Type mI32Vec3;
    public final Type mI32Vec4;

    /**
     * GLSL aliases.
     */
    public final Type mUInt32;
    public final Type mU32Vec2;
    public final Type mU32Vec3;
    public final Type mU32Vec4;

    /**
     * GLSL aliases.
     */
    public final Type mFloat32;
    public final Type mF32Vec2;
    public final Type mF32Vec3;
    public final Type mF32Vec4;

    /**
     * GLSL aliases.
     */
    public final Type mMat2;
    public final Type mMat3;
    public final Type mMat4;

    /**
     * GLSL aliases.
     */
    public final Type mMat2x2;
    public final Type mMat2x3;
    public final Type mMat2x4;
    public final Type mMat3x2;
    public final Type mMat3x3;
    public final Type mMat3x4;
    public final Type mMat4x2;
    public final Type mMat4x3;
    public final Type mMat4x4;

    /**
     * GLSL aliases.
     */
    public final Type mF32Mat2;
    public final Type mF32Mat3;
    public final Type mF32Mat4;

    /**
     * GLSL aliases.
     */
    public final Type mF32Mat2x2;
    public final Type mF32Mat2x3;
    public final Type mF32Mat2x4;
    public final Type mF32Mat3x2;
    public final Type mF32Mat3x3;
    public final Type mF32Mat3x4;
    public final Type mF32Mat4x2;
    public final Type mF32Mat4x3;
    public final Type mF32Mat4x4;

    /**
     * Image types.
     */
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

    /**
     * Subpass types.
     */
    public final Type mSubpassInput;
    public final Type mSubpassInputMS;

    /**
     * Texture types.
     */
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

    /**
     * Sampler types.
     */
    public final Type mSampler;
    public final Type mSamplerShadow;

    /**
     * Texture sampler types.
     */
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

    /**
     * Depth texture sampler types.
     */
    public final Type mSampler1DShadow;
    public final Type mSampler2DShadow;
    public final Type mSamplerCubeShadow;
    public final Type mSampler1DArrayShadow;
    public final Type mSampler2DArrayShadow;
    public final Type mSamplerCubeArrayShadow;

    public final Type mInvalid;

    public final Type mGenFType;
    public final Type mGenIType;
    public final Type mGenUType;
    public final Type mGenHType;
    public final Type mGenSType;
    public final Type mGenUSType;
    public final Type mGenBType;

    public final Type mMat;
    public final Type mHMat;

    public final Type mVec;
    public final Type mIVec;
    public final Type mUVec;
    public final Type mHVec;
    public final Type mSVec;
    public final Type mUSVec;
    public final Type mBVec;

    /**
     * A bad value or there's an error.
     */
    public final Type mPoison;

    /**
     * Initializes the core SL types.
     */
    public BuiltinTypes() {
        mVoid = Type.makeSpecialType("void", "v", Type.kVoid_TypeKind);

        // scalar and vector types

        mBool = Type.makeScalarType(
                "bool", "b", Type.kBoolean_ScalarKind, /*rank=*/0, /*width=*/1);
        mBool2 = Type.makeVectorType("bool2", "b2", mBool, /*rows*/2);
        mBool3 = Type.makeVectorType("bool3", "b3", mBool, /*rows*/3);
        mBool4 = Type.makeVectorType("bool4", "b4", mBool, /*rows*/4);

        mShort = Type.makeScalarType(
                "short", "s", Type.kSigned_ScalarKind, /*rank=*/3, /*width=*/16);
        mShort2 = Type.makeVectorType("short2", "s2", mShort, /*rows*/2);
        mShort3 = Type.makeVectorType("short3", "s3", mShort, /*rows*/3);
        mShort4 = Type.makeVectorType("short4", "s4", mShort, /*rows*/4);

        mUShort = Type.makeScalarType(
                "ushort", "S", Type.kUnsigned_ScalarKind, /*rank=*/4, /*width=*/16);
        mUShort2 = Type.makeVectorType("ushort2", "S2", mUShort, /*rows*/2);
        mUShort3 = Type.makeVectorType("ushort3", "S3", mUShort, /*rows*/3);
        mUShort4 = Type.makeVectorType("ushort4", "S4", mUShort, /*rows*/4);

        mInt = Type.makeScalarType(
                "int", "i", Type.kSigned_ScalarKind, /*rank=*/6, /*width=*/32);
        mInt2 = Type.makeVectorType("int2", "i2", mInt, /*rows*/2);
        mInt3 = Type.makeVectorType("int3", "i3", mInt, /*rows*/3);
        mInt4 = Type.makeVectorType("int4", "i4", mInt, /*rows*/4);

        mUInt = Type.makeScalarType(
                "uint", "I", Type.kUnsigned_ScalarKind, /*rank=*/7, /*width=*/32);
        mUInt2 = Type.makeVectorType("uint2", "I2", mUInt, /*rows*/2);
        mUInt3 = Type.makeVectorType("uint3", "I3", mUInt, /*rows*/3);
        mUInt4 = Type.makeVectorType("uint4", "I4", mUInt, /*rows*/4);

        mHalf = Type.makeScalarType(
                "half", "h", Type.kFloat_ScalarKind, /*rank=*/9, /*width=*/16);
        mHalf2 = Type.makeVectorType("half2", "h2", mHalf, /*rows*/2);
        mHalf3 = Type.makeVectorType("half3", "h3", mHalf, /*rows*/3);
        mHalf4 = Type.makeVectorType("half4", "h4", mHalf, /*rows*/4);

        mFloat = Type.makeScalarType(
                "float", "f", Type.kFloat_ScalarKind, /*rank=*/10, /*width=*/32);
        mFloat2 = Type.makeVectorType("float2", "f2", mFloat, /*rows*/2);
        mFloat3 = Type.makeVectorType("float3", "f3", mFloat, /*rows*/3);
        mFloat4 = Type.makeVectorType("float4", "f4", mFloat, /*rows*/4);

        // matrix types

        mHalf2x2 = Type.makeMatrixType("half2x2", "h22", mHalf2, /*cols=*/2);
        mHalf2x3 = Type.makeMatrixType("half2x3", "h23", mHalf3, /*cols=*/2);
        mHalf2x4 = Type.makeMatrixType("half2x4", "h24", mHalf4, /*cols=*/2);
        mHalf3x2 = Type.makeMatrixType("half3x2", "h32", mHalf2, /*cols=*/3);
        mHalf3x3 = Type.makeMatrixType("half3x3", "h33", mHalf3, /*cols=*/3);
        mHalf3x4 = Type.makeMatrixType("half3x4", "h34", mHalf4, /*cols=*/3);
        mHalf4x2 = Type.makeMatrixType("half4x2", "h42", mHalf2, /*cols=*/4);
        mHalf4x3 = Type.makeMatrixType("half4x3", "h43", mHalf3, /*cols=*/4);
        mHalf4x4 = Type.makeMatrixType("half4x4", "h44", mHalf4, /*cols=*/4);

        mFloat2x2 = Type.makeMatrixType("float2x2", "f22", mFloat2, /*cols=*/2);
        mFloat2x3 = Type.makeMatrixType("float2x3", "f23", mFloat3, /*cols=*/2);
        mFloat2x4 = Type.makeMatrixType("float2x4", "f24", mFloat4, /*cols=*/2);
        mFloat3x2 = Type.makeMatrixType("float3x2", "f32", mFloat2, /*cols=*/3);
        mFloat3x3 = Type.makeMatrixType("float3x3", "f33", mFloat3, /*cols=*/3);
        mFloat3x4 = Type.makeMatrixType("float3x4", "f34", mFloat4, /*cols=*/3);
        mFloat4x2 = Type.makeMatrixType("float4x2", "f42", mFloat2, /*cols=*/4);
        mFloat4x3 = Type.makeMatrixType("float4x3", "f43", mFloat3, /*cols=*/4);
        mFloat4x4 = Type.makeMatrixType("float4x4", "f44", mFloat4, /*cols=*/4);

        // GLSL vector aliases

        mVec2 = Type.makeAliasType("vec2", mFloat2);
        mVec3 = Type.makeAliasType("vec3", mFloat3);
        mVec4 = Type.makeAliasType("vec4", mFloat4);

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

        // opaque types (image, texture, sampler, combined sampler)

        mImage1D = Type.makeImageType("image1D", "M1", mHalf,
                Spv.SpvDim1D, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mImage2D = Type.makeImageType("image2D", "M2", mHalf,
                Spv.SpvDim2D, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mImage3D = Type.makeImageType("image3D", "M3", mHalf,
                Spv.SpvDim3D, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mImageCube = Type.makeImageType("imageCube", "MC", mHalf,
                Spv.SpvDimCube, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mImageBuffer = Type.makeImageType("imageBuffer", "MB", mHalf,
                Spv.SpvDimBuffer, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mImage1DArray = Type.makeImageType("image1DArray", "M1A", mHalf,
                Spv.SpvDim1D, /*isArrayed=*/true, /*isMultiSampled=*/false);
        mImage2DArray = Type.makeImageType("image2DArray", "M2A", mHalf,
                Spv.SpvDim2D, /*isArrayed=*/true, /*isMultiSampled=*/false);
        mImageCubeArray = Type.makeImageType("imageCubeArray", "MCA", mHalf,
                Spv.SpvDimCube, /*isArrayed=*/true, /*isMultiSampled=*/false);
        mImage2DMS = Type.makeImageType("image2DMS", "MM", mHalf,
                Spv.SpvDim2D, /*isArrayed=*/false, /*isMultiSampled=*/true);
        mImage2DMSArray = Type.makeImageType("image2DMSArray", "MMA", mHalf,
                Spv.SpvDim2D, /*isArrayed=*/true, /*isMultiSampled=*/true);

        mSubpassInput = Type.makeImageType("subpassInput", "MP", mHalf,
                Spv.SpvDimSubpassData, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mSubpassInputMS = Type.makeImageType("subpassInputMS", "MPM", mHalf,
                Spv.SpvDimSubpassData, /*isArrayed=*/false, /*isMultiSampled=*/true);

        mTexture1D = Type.makeTextureType("texture1D", "T1", mHalf,
                Spv.SpvDim1D, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mTexture2D = Type.makeTextureType("texture2D", "T2", mHalf,
                Spv.SpvDim2D, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mTexture3D = Type.makeTextureType("texture3D", "T3", mHalf,
                Spv.SpvDim3D, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mTextureCube = Type.makeTextureType("textureCube", "TC", mHalf,
                Spv.SpvDimCube, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mTextureBuffer = Type.makeTextureType("textureBuffer", "TB", mHalf,
                Spv.SpvDimBuffer, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mTexture1DArray = Type.makeTextureType("texture1DArray", "T1A", mHalf,
                Spv.SpvDim1D, /*isArrayed=*/true, /*isMultiSampled=*/false);
        mTexture2DArray = Type.makeTextureType("texture2DArray", "T2A", mHalf,
                Spv.SpvDim2D, /*isArrayed=*/true, /*isMultiSampled=*/false);
        mTextureCubeArray = Type.makeTextureType("textureCubeArray", "TCA", mHalf,
                Spv.SpvDimCube, /*isArrayed=*/true, /*isMultiSampled=*/false);
        mTexture2DMS = Type.makeTextureType("texture2DMS", "TM", mHalf,
                Spv.SpvDim2D, /*isArrayed=*/false, /*isMultiSampled=*/true);
        mTexture2DMSArray = Type.makeTextureType("texture2DMSArray", "TMA", mHalf,
                Spv.SpvDim2D, /*isArrayed=*/true, /*isMultiSampled=*/true);

        mSampler = Type.makeSeparateType("sampler", "ss", mVoid, /*isShadow*/false);
        mSamplerShadow = Type.makeSeparateType("samplerShadow", "sss", mVoid, /*isShadow*/true);

        mSampler1D = Type.makeCombinedType("sampler1D", "Z1", mHalf,
                Spv.SpvDim1D, /*isShadow*/false, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mSampler2D = Type.makeCombinedType("sampler2D", "Z2", mHalf,
                Spv.SpvDim2D, /*isShadow*/false, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mSampler3D = Type.makeCombinedType("sampler3D", "Z3", mHalf,
                Spv.SpvDim3D, /*isShadow*/false, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mSamplerCube = Type.makeCombinedType("samplerCube", "ZC", mHalf,
                Spv.SpvDimCube, /*isShadow*/false, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mSamplerBuffer = Type.makeCombinedType("samplerBuffer", "ZB", mHalf,
                Spv.SpvDimBuffer, /*isShadow*/false, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mSampler1DArray = Type.makeCombinedType("sampler1DArray", "Z1A", mHalf,
                Spv.SpvDim1D, /*isShadow*/false, /*isArrayed=*/true, /*isMultiSampled=*/false);
        mSampler2DArray = Type.makeCombinedType("sampler2DArray", "Z2A", mHalf,
                Spv.SpvDim2D, /*isShadow*/false, /*isArrayed=*/true, /*isMultiSampled=*/false);
        mSamplerCubeArray = Type.makeCombinedType("samplerCubeArray", "ZCA", mHalf,
                Spv.SpvDimCube, /*isShadow*/false, /*isArrayed=*/true, /*isMultiSampled=*/false);
        mSampler2DMS = Type.makeCombinedType("sampler2DMS", "ZM", mHalf,
                Spv.SpvDim2D, /*isShadow*/false, /*isArrayed=*/false, /*isMultiSampled=*/true);
        mSampler2DMSArray = Type.makeCombinedType("sampler2DMSArray", "ZMA", mHalf,
                Spv.SpvDim2D, /*isShadow*/false, /*isArrayed=*/true, /*isMultiSampled=*/true);

        mSampler1DShadow = Type.makeCombinedType("sampler1DShadow", "Z4", mHalf,
                Spv.SpvDim1D, /*isShadow*/true, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mSampler2DShadow = Type.makeCombinedType("sampler2DShadow", "Z5", mHalf,
                Spv.SpvDim2D, /*isShadow*/true, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mSamplerCubeShadow = Type.makeCombinedType("samplerCubeShadow", "ZX", mHalf,
                Spv.SpvDimCube, /*isShadow*/true, /*isArrayed=*/false, /*isMultiSampled=*/false);
        mSampler1DArrayShadow = Type.makeCombinedType("sampler1DArrayShadow", "Z4A", mHalf,
                Spv.SpvDim1D, /*isShadow*/true, /*isArrayed=*/true, /*isMultiSampled=*/false);
        mSampler2DArrayShadow = Type.makeCombinedType("sampler2DArrayShadow", "Z5A", mHalf,
                Spv.SpvDim2D, /*isShadow*/true, /*isArrayed=*/true, /*isMultiSampled=*/false);
        mSamplerCubeArrayShadow = Type.makeCombinedType("samplerCubeArrayShadow", "ZXA", mHalf,
                Spv.SpvDimCube, /*isShadow*/true, /*isArrayed=*/true, /*isMultiSampled=*/false);

        mInvalid = Type.makeSpecialType(ShaderCompiler.INVALID_TAG, "O", Type.kOther_TypeKind);

        mGenFType = Type.makeGenericType("__genFType", mFloat, mFloat2, mFloat3, mFloat4);
        mGenIType = Type.makeGenericType("__genIType", mInt, mInt2, mInt3, mInt4);
        mGenUType = Type.makeGenericType("__genUType", mUInt, mUInt2, mUInt3, mUInt4);
        mGenHType = Type.makeGenericType("__genHType", mHalf, mHalf2, mHalf3, mHalf4);
        mGenSType = Type.makeGenericType("__genSType", mShort, mShort2, mShort3, mShort4);
        mGenUSType = Type.makeGenericType("__genUSType", mUShort, mUShort2, mUShort3, mUShort4);
        mGenBType = Type.makeGenericType("__genBType", mBool, mBool2, mBool3, mBool4);

        mMat = Type.makeGenericType("__mat", mFloat2x2, mFloat2x3, mFloat2x4,
                mFloat3x2, mFloat3x3, mFloat3x4,
                mFloat4x2, mFloat4x3, mFloat4x4);
        mHMat = Type.makeGenericType("__hmat", mHalf2x2, mHalf2x3, mHalf2x4,
                mHalf3x2, mHalf3x3, mHalf3x4,
                mHalf4x2, mHalf4x3, mHalf4x4);

        mVec = Type.makeGenericType("__vec", mInvalid, mFloat2, mFloat3, mFloat4);
        mIVec = Type.makeGenericType("__ivec", mInvalid, mInt2, mInt3, mInt4);
        mUVec = Type.makeGenericType("__uvec", mInvalid, mUInt2, mUInt3, mUInt4);
        mHVec = Type.makeGenericType("__hvec", mInvalid, mHalf2, mHalf3, mHalf4);
        mSVec = Type.makeGenericType("__svec", mInvalid, mShort2, mShort3, mShort4);
        mUSVec = Type.makeGenericType("__usvec", mInvalid, mUShort2, mUShort3, mUShort4);
        mBVec = Type.makeGenericType("__bvec", mInvalid, mBool2, mBool3, mBool4);

        mPoison = Type.makeSpecialType(ShaderCompiler.POISON_TAG, "P", Type.kOther_TypeKind);
    }
}
