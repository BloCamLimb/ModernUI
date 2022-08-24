/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arctic.sksl.ir;

import javax.annotation.Nonnull;

public final class MatrixType extends Type {

    private final ScalarType mComponentType;
    private final int mColumns;
    private final int mRows;

    MatrixType(String name, String abbrev, Type componentType, int columns, int rows) {
        super(name, abbrev, TypeKind_Matrix);
        assert columns >= 2 && columns <= 4;
        assert rows >= 2 && rows <= 4;
        mComponentType = (ScalarType) componentType;
        mColumns = columns;
        mRows = rows;
    }

    @Nonnull
    @Override
    public ScalarType componentType() {
        return mComponentType;
    }

    @Override
    public int columns() {
        return mColumns;
    }

    @Override
    public int rows() {
        return mRows;
    }

    @Override
    public int bitWidth() {
        return mComponentType.bitWidth();
    }

    @Override
    public boolean isMatrix() {
        return true;
    }

    @Override
    public int slotCount() {
        return mColumns * mRows;
    }
}
