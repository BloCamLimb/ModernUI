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

public final class VectorType extends Type {

    private final ScalarType mComponentType;
    private final int mColumns;

    VectorType(String name, String abbrev, Type componentType, int columns) {
        super(name, abbrev, KIND_VECTOR);
        assert columns >= 2 && columns <= 4;
        mComponentType = (ScalarType) componentType;
        mColumns = columns;
    }

    @Nonnull
    @Override
    public ScalarType getComponentType() {
        return mComponentType;
    }

    @Override
    public int getColumns() {
        return mColumns;
    }

    @Override
    public int getRows() {
        return 1;
    }

    @Override
    public int getBitWidth() {
        return mComponentType.getBitWidth();
    }

    @Override
    public boolean isVector() {
        return true;
    }

    @Override
    public int getSlots() {
        return mColumns;
    }
}
