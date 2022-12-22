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

package icyllis.akashigi.slang.ir;

import javax.annotation.Nonnull;

public final class VectorType extends Type {

    private final ScalarType mComponentType;
    private final byte mLength;

    VectorType(String name, String desc, Type componentType, int length) {
        super(name, desc, TYPE_KIND_VECTOR);
        assert (length >= 2 && length <= 4);
        assert (desc.equals(componentType.getDescriptor() + length));
        assert (name.equals(componentType.getName() + length));
        mComponentType = (ScalarType) componentType;
        mLength = (byte) length;
    }

    @Override
    public boolean isVector() {
        return true;
    }

    @Nonnull
    @Override
    public ScalarType getComponentType() {
        return mComponentType;
    }

    @Override
    public int cols() {
        return 1;
    }

    @Override
    public int rows() {
        return mLength;
    }

    @Override
    public int length() {
        return mLength;
    }

    @Override
    public int getBitWidth() {
        return mComponentType.getBitWidth();
    }
}
