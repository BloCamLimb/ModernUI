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

package icyllis.akashigi.aksl.ir;

import javax.annotation.Nonnull;

public final class ArrayType extends Type {

    private final Type mComponentType;
    private final int mArraySize;

    ArrayType(Type componentType, int arraySize) {
        super(convertNameOrDesc(componentType.name(), arraySize),
                convertNameOrDesc(componentType.desc(), arraySize),
                TYPE_KIND_ARRAY);
        assert (arraySize == UNSIZED_ARRAY_SIZE || arraySize > 0);
        // Disallow multi-dimensional arrays (Vulkan).
        if (componentType instanceof ArrayType) {
            throw new IllegalArgumentException("Multi-dimensional arrays are not supported");
        }
        mComponentType = componentType;
        mArraySize = arraySize;
    }

    @Nonnull
    public static String convertNameOrDesc(String nameOrDesc, int arraySize) {
        if (arraySize == UNSIZED_ARRAY_SIZE) {
            return nameOrDesc + "[]";
        }
        assert (arraySize > 0);
        return nameOrDesc + "[" + arraySize + "]";
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public boolean isUnsizedArray() {
        return mArraySize == UNSIZED_ARRAY_SIZE;
    }

    @Nonnull
    @Override
    public Type getComponentType() {
        return mComponentType;
    }

    @Override
    public int columns() {
        return mArraySize;
    }

    @Override
    public int bitWidth() {
        return mComponentType.bitWidth();
    }

    @Override
    public int slotCount() {
        assert (mArraySize != UNSIZED_ARRAY_SIZE && mArraySize > 0);
        return mArraySize * mComponentType.slotCount();
    }
}
