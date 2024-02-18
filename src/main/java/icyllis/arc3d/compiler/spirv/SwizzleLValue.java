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

package icyllis.arc3d.compiler.spirv;

import icyllis.arc3d.compiler.tree.Type;

import static org.lwjgl.util.spvc.Spv.*;

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
