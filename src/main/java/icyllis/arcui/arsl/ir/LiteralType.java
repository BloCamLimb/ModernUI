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

package icyllis.arcui.arsl.ir;

import javax.annotation.Nonnull;

public final class LiteralType extends Type {

    private final Type mScalarType;
    private final int mPriority;

    LiteralType(String name, Type scalarType, int priority) {
        super(name, "L", TypeKind_Literal);
        mScalarType = scalarType;
        mPriority = priority;
    }

    @Nonnull
    @Override
    public Type scalarTypeForLiteral() {
        return mScalarType;
    }

    @Override
    public int priority() {
        return mPriority;
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
    public byte scalarKind() {
        return mScalarType.scalarKind();
    }

    @Override
    public int bitWidth() {
        return mScalarType.bitWidth();
    }

    @Override
    public boolean isScalar() {
        return true;
    }

    @Override
    public boolean isLiteral() {
        return true;
    }

    @Override
    public boolean isPrivate() {
        return true;
    }

    @Override
    public int slotCount() {
        return 1;
    }
}
