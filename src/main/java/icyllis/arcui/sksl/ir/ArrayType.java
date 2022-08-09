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

package icyllis.arcui.sksl.ir;

import javax.annotation.Nonnull;

public final class ArrayType extends Type {

    private final Type mComponentType;
    private final int mColumns;

    ArrayType(String name, Type componentType, int columns) {
        super(name, componentType.abbreviatedName(), TypeKind_Array);
        // Only allow explicitly-sized arrays.
        assert columns > 0;
        // Disallow multi-dimensional arrays.
        assert !(componentType instanceof ArrayType);
        mComponentType = componentType;
        mColumns = columns;
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Nonnull
    @Override
    public Type componentType() {
        return mComponentType;
    }

    @Override
    public int columns() {
        return mColumns;
    }

    @Override
    public int bitWidth() {
        return mComponentType.bitWidth();
    }

    @Override
    public boolean isPrivate() {
        return mComponentType.isPrivate();
    }

    @Override
    public int slotCount() {
        return mColumns * mComponentType.slotCount();
    }
}
