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

/**
 * Thread-safe class that loads shader modules.
 */
public class ModuleLoader {

    private static final ModuleLoader sInstance = new ModuleLoader();

    private final BuiltinTypes mBuiltinTypes = new BuiltinTypes();

    private final Module mRootModule;

    private volatile Module mCommonModule;

    private ModuleLoader() {
        mRootModule = new Module();

        SymbolTable symbols = new SymbolTable(/*builtin=*/true);
        BuiltinTypes types = mBuiltinTypes;

        symbols.insert(types.mVoid);

        symbols.insert(types.mBool);
        symbols.insert(types.mBool2);
        symbols.insert(types.mBool3);
        symbols.insert(types.mBool4);

        symbols.insert(types.mShort);
        symbols.insert(types.mShort2);
        symbols.insert(types.mShort3);
        symbols.insert(types.mShort4);

        symbols.insert(types.mUShort);
        symbols.insert(types.mUShort2);
        symbols.insert(types.mUShort3);
        symbols.insert(types.mUShort4);

        symbols.insert(types.mInt);
        symbols.insert(types.mInt2);
        symbols.insert(types.mInt3);
        symbols.insert(types.mInt4);

        symbols.insert(types.mUInt);
        symbols.insert(types.mUInt2);
        symbols.insert(types.mUInt3);
        symbols.insert(types.mUInt4);

        symbols.insert(types.mHalf);
        symbols.insert(types.mHalf2);
        symbols.insert(types.mHalf3);
        symbols.insert(types.mHalf4);

        symbols.insert(types.mFloat);
        symbols.insert(types.mFloat2);
        symbols.insert(types.mFloat3);
        symbols.insert(types.mFloat4);

        symbols.insert(types.mHalf2x2);
        symbols.insert(types.mHalf2x3);
        symbols.insert(types.mHalf2x4);
        symbols.insert(types.mHalf3x2);
        symbols.insert(types.mHalf3x3);
        symbols.insert(types.mHalf3x4);
        symbols.insert(types.mHalf4x2);
        symbols.insert(types.mHalf4x3);
        symbols.insert(types.mHalf4x4);

        symbols.insert(types.mFloat2x2);
        symbols.insert(types.mFloat2x3);
        symbols.insert(types.mFloat2x4);
        symbols.insert(types.mFloat3x2);
        symbols.insert(types.mFloat3x3);
        symbols.insert(types.mFloat3x4);
        symbols.insert(types.mFloat4x2);
        symbols.insert(types.mFloat4x3);
        symbols.insert(types.mFloat4x4);

        symbols.insert(types.mGenFType);
        symbols.insert(types.mGenIType);
        symbols.insert(types.mGenUType);
        symbols.insert(types.mGenHType);
        symbols.insert(types.mGenSType);
        symbols.insert(types.mGenUSType);
        symbols.insert(types.mGenBType);

        symbols.insert(types.mMat);
        symbols.insert(types.mHMat);

        symbols.insert(types.mVec);
        symbols.insert(types.mIVec);
        symbols.insert(types.mUVec);
        symbols.insert(types.mHVec);
        symbols.insert(types.mSVec);
        symbols.insert(types.mUSVec);
        symbols.insert(types.mBVec);

        symbols.insert(types.mImage1D);
        symbols.insert(types.mImage2D);
        symbols.insert(types.mImage3D);
        symbols.insert(types.mImageCube);
        symbols.insert(types.mImageBuffer);
        symbols.insert(types.mImage1DArray);
        symbols.insert(types.mImage2DArray);
        symbols.insert(types.mImageCubeArray);
        symbols.insert(types.mImage2DMS);
        symbols.insert(types.mImage2DMSArray);

        symbols.insert(types.mSubpassInput);
        symbols.insert(types.mSubpassInputMS);

        symbols.insert(types.mTexture1D);
        symbols.insert(types.mTexture2D);
        symbols.insert(types.mTexture3D);
        symbols.insert(types.mTextureCube);
        symbols.insert(types.mTextureBuffer);
        symbols.insert(types.mTexture1DArray);
        symbols.insert(types.mTexture2DArray);
        symbols.insert(types.mTextureCubeArray);
        symbols.insert(types.mTexture2DMS);
        symbols.insert(types.mTexture2DMSArray);

        symbols.insert(types.mSampler);
        symbols.insert(types.mSamplerShadow);

        symbols.insert(types.mSampler1D);
        symbols.insert(types.mSampler2D);
        symbols.insert(types.mSampler3D);
        symbols.insert(types.mSamplerCube);
        symbols.insert(types.mSamplerBuffer);
        symbols.insert(types.mSampler1DArray);
        symbols.insert(types.mSampler2DArray);
        symbols.insert(types.mSamplerCubeArray);
        symbols.insert(types.mSampler2DMS);
        symbols.insert(types.mSampler2DMSArray);

        symbols.insert(types.mSampler1DShadow);
        symbols.insert(types.mSampler2DShadow);
        symbols.insert(types.mSamplerCubeShadow);
        symbols.insert(types.mSampler1DArrayShadow);
        symbols.insert(types.mSampler2DArrayShadow);
        symbols.insert(types.mSamplerCubeArrayShadow);

        symbols.insert(types.mVec2);
        symbols.insert(types.mVec3);
        symbols.insert(types.mVec4);

        symbols.insert(types.mBVec2);
        symbols.insert(types.mBVec3);
        symbols.insert(types.mBVec4);

        symbols.insert(types.mIVec2);
        symbols.insert(types.mIVec3);
        symbols.insert(types.mIVec4);

        symbols.insert(types.mUVec2);
        symbols.insert(types.mUVec3);
        symbols.insert(types.mUVec4);

        symbols.insert(types.mMin16Int);
        symbols.insert(types.mMin16Int2);
        symbols.insert(types.mMin16Int3);
        symbols.insert(types.mMin16Int4);

        symbols.insert(types.mMin16UInt);
        symbols.insert(types.mMin16UInt2);
        symbols.insert(types.mMin16UInt3);
        symbols.insert(types.mMin16UInt4);

        symbols.insert(types.mMin16Float);
        symbols.insert(types.mMin16Float2);
        symbols.insert(types.mMin16Float3);
        symbols.insert(types.mMin16Float4);

        symbols.insert(types.mInt32);
        symbols.insert(types.mI32Vec2);
        symbols.insert(types.mI32Vec3);
        symbols.insert(types.mI32Vec4);

        symbols.insert(types.mUInt32);
        symbols.insert(types.mU32Vec2);
        symbols.insert(types.mU32Vec3);
        symbols.insert(types.mU32Vec4);

        symbols.insert(types.mFloat32);
        symbols.insert(types.mF32Vec2);
        symbols.insert(types.mF32Vec3);
        symbols.insert(types.mF32Vec4);

        symbols.insert(types.mMat2);
        symbols.insert(types.mMat3);
        symbols.insert(types.mMat4);

        symbols.insert(types.mMat2x2);
        symbols.insert(types.mMat2x3);
        symbols.insert(types.mMat2x4);
        symbols.insert(types.mMat3x2);
        symbols.insert(types.mMat3x3);
        symbols.insert(types.mMat3x4);
        symbols.insert(types.mMat4x2);
        symbols.insert(types.mMat4x3);
        symbols.insert(types.mMat4x4);

        symbols.insert(types.mF32Mat2);
        symbols.insert(types.mF32Mat3);
        symbols.insert(types.mF32Mat4);

        symbols.insert(types.mF32Mat2x2);
        symbols.insert(types.mF32Mat2x3);
        symbols.insert(types.mF32Mat2x4);
        symbols.insert(types.mF32Mat3x2);
        symbols.insert(types.mF32Mat3x3);
        symbols.insert(types.mF32Mat3x4);
        symbols.insert(types.mF32Mat4x2);
        symbols.insert(types.mF32Mat4x3);
        symbols.insert(types.mF32Mat4x4);

        mRootModule.mSymbols = symbols;
    }

    public static ModuleLoader getInstance() {
        return sInstance;
    }

    public BuiltinTypes getBuiltinTypes() {
        return mBuiltinTypes;
    }

    public Module getRootModule() {
        return mRootModule;
    }

    public Module loadCommonModule(Compiler compiler) {
        if (mCommonModule != null) {
            return mCommonModule;
        }
        synchronized (mRootModule) {
            if (mCommonModule == null) {
                mCommonModule = new Module();
                //TODO parse
            }
        }
        return mCommonModule;
    }
}
