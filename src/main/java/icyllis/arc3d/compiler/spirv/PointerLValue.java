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
