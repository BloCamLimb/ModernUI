/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.compiler.spirv;

import icyllis.arc3d.compiler.tree.Type;

import static org.lwjgl.util.spvc.Spv.*;

// addressable
interface LValue {

    int getPointer();

    int load(SPIRVCodeGenerator gen, Output output);

    void store(SPIRVCodeGenerator gen, int rvalue, Output output);

    boolean applySwizzle(byte[] components, Type newType);
}

class PointerLValue implements LValue {

    private final int mPointer;
    private final boolean mIsMemoryObject;
    private final int mType;
    private final boolean mRelaxedPrecision;
    private final int mStorageClass;

    PointerLValue(int pointer, boolean isMemoryObject,
                  int type, boolean relaxedPrecision, int storageClass) {
        mPointer = pointer;
        mIsMemoryObject = isMemoryObject;
        mType = type;
        mRelaxedPrecision = relaxedPrecision;
        mStorageClass = storageClass;
    }

    @Override
    public int getPointer() {
        return mPointer;
    }

    @Override
    public int load(SPIRVCodeGenerator gen, Output output) {
        return gen.writeOpLoad(mType, mRelaxedPrecision, mPointer, output);
    }

    @Override
    public void store(SPIRVCodeGenerator gen, int rvalue, Output output) {
        if (!mIsMemoryObject) {
            // We are going to write into an access chain; this could represent one component of a
            // vector, or one element of an array. This has the potential to invalidate other,
            // *unknown* elements of our store cache. (e.g. if the store cache holds `%50 = myVec4`,
            // and we store `%60 = myVec4.z`, this invalidates the cached value for %50.) To avoid
            // relying on stale data, reset the store cache entirely when this happens.
            gen.mStoreCache.clear();
        }

        gen.writeOpStore(mStorageClass, mPointer, rvalue, output);
    }

    @Override
    public boolean applySwizzle(byte[] components, Type newType) {
        return false;
    }
}

class SwizzleLValue implements LValue {

    private final int mBasePointer;
    // e.g. argb -> [3,0,1,2]
    private byte[] mComponents;
    private final Type mBaseType;
    private Type mResultType;
    private final int mStorageClass;

    SwizzleLValue(int basePointer, byte[] components,
                  Type baseType, Type resultType, int storageClass) {
        mBasePointer = basePointer;
        mComponents = components;
        mBaseType = baseType;
        mResultType = resultType;
        mStorageClass = storageClass;
    }

    @Override
    public int getPointer() {
        return SPIRVCodeGenerator.NONE_ID;
    }

    @Override
    public int load(SPIRVCodeGenerator gen, Output output) {
        int base = gen.getUniqueId(mBaseType);
        int baseType = gen.writeType(mBaseType);
        gen.writeInstruction(SpvOpLoad, baseType, base, mBasePointer, output);
        int result = gen.getUniqueId(mBaseType);
        gen.writeOpcode(SpvOpVectorShuffle, 5 + mComponents.length, output);
        int resultType = gen.writeType(mResultType);
        output.writeWord(resultType);
        output.writeWord(result);
        output.writeWord(base);
        output.writeWord(base);
        for (int component : mComponents) {
            output.writeWord(component);
        }
        return result;
    }

    @Override
    public void store(SPIRVCodeGenerator gen, int rvalue, Output output) {
        // use OpVectorShuffle to mix and match the vector components. We effectively create
        // a virtual vector out of the concatenation of the left and right vectors, and then
        // select components from this virtual vector to make the result vector. For
        // instance, given:
        // float3L = ...;
        // float3R = ...;
        // L.xz = R.xy;
        // we end up with the virtual vector (L.x, L.y, L.z, R.x, R.y, R.z). Then we want
        // our result vector to look like (R.x, L.y, R.y), so we need to select indices
        // (3, 1, 4).
        int base = gen.getUniqueId(mBaseType);
        int baseType = gen.writeType(mBaseType);
        gen.writeInstruction(SpvOpLoad, baseType, base, mBasePointer, output);
        int shuffle = gen.getUniqueId(mBaseType);
        gen.writeOpcode(SpvOpVectorShuffle, 5 + mBaseType.getRows(), output);
        output.writeWord(baseType);
        output.writeWord(shuffle);
        output.writeWord(base);
        output.writeWord(rvalue);
        for (int i = 0; i < mBaseType.getRows(); i++) {
            // current offset into the virtual vector, defaults to pulling the unmodified
            // value from the left side
            int offset = i;
            // check to see if we are writing this component
            for (int j = 0; j < mComponents.length; j++) {
                if (mComponents[j] == i) {
                    // we're writing to this component, so adjust the offset to pull from
                    // the correct component of the right side instead of preserving the
                    // value from the left
                    offset = (j + mBaseType.getRows());
                    break;
                }
            }
            output.writeWord(offset);
        }
        gen.writeOpStore(mStorageClass, mBasePointer, shuffle, output);
    }

    @Override
    public boolean applySwizzle(byte[] components, Type newType) {
        byte[] newSwizzle = new byte[components.length];
        for (int i = 0; i < components.length; i++) {
            byte component = components[i];
            if (component < 0 || component >= mComponents.length) {
                assert false;
                return false;
            }
            newSwizzle[i] = mComponents[component];
        }
        mComponents = newSwizzle;
        mResultType = newType;
        return true;
    }
}
