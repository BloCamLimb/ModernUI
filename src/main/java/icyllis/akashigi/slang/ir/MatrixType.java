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

public final class MatrixType extends Type {

    private final ScalarType mComponentType;
    private final byte mCols;
    private final byte mRows;

    MatrixType(String name, String desc, Type componentType, int cols, int rows) {
        super(name, desc, TYPE_KIND_MATRIX);
        assert (rows >= 2 && rows <= 4);
        assert (cols >= 2 && cols <= 4);
        assert (desc.equals(componentType.getDescriptor() + cols + "" + rows));
        assert (name.equals(componentType.getName() + cols + "x" + rows));
        mComponentType = (ScalarType) componentType;
        mCols = (byte) cols;
        mRows = (byte) rows;
    }

    @Override
    public boolean isMatrix() {
        return true;
    }

    @Nonnull
    @Override
    public ScalarType getComponentType() {
        return mComponentType;
    }

    @Override
    public int cols() {
        return mCols;
    }

    @Override
    public int rows() {
        return mRows;
    }

    @Override
    public int getBitWidth() {
        return mComponentType.getBitWidth();
    }
}
