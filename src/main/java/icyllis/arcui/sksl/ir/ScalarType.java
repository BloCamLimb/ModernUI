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

public final class ScalarType extends Type {

    private final byte mScalarKind;
    private final int mPriority;
    private final int mBitWidth;

    ScalarType(String name, String abbrev, byte scalarKind, int priority, int bitWidth) {
        super(name, abbrev, TypeKind_Scalar);
        mScalarKind = scalarKind;
        mPriority = priority;
        mBitWidth = bitWidth;
    }

    @Override
    public byte scalarKind() {
        return mScalarKind;
    }

    @Override
    public int priority() {
        return mPriority;
    }

    @Override
    public int bitWidth() {
        return mBitWidth;
    }

    @Override
    public int columns() {
        return 1;
    }

    @Override
    public int rows() {
        return 1;
    }

    @Override
    public boolean isScalar() {
        return true;
    }

    @Override
    public int slotCount() {
        return 1;
    }
}
