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

package icyllis.arcticgi.sksl.ir;

import javax.annotation.Nonnull;

public final class AliasType extends Type {

    private final Type mTargetType;

    AliasType(String name, Type targetType) {
        super(name, targetType.abbreviatedName(), targetType.typeKind());
        mTargetType = targetType;
    }

    @Nonnull
    @Override
    public Type resolve() {
        return mTargetType;
    }

    @Nonnull
    @Override
    public Type componentType() {
        return mTargetType.componentType();
    }

    @Override
    public byte scalarKind() {
        return mTargetType.scalarKind();
    }

    @Override
    public int priority() {
        return mTargetType.priority();
    }

    @Override
    public int columns() {
        return mTargetType.columns();
    }

    @Override
    public int rows() {
        return mTargetType.rows();
    }

    @Override
    public int bitWidth() {
        return mTargetType.bitWidth();
    }

    @Override
    public boolean isPrivate() {
        return mTargetType.isPrivate();
    }

    @Override
    public int slotCount() {
        return mTargetType.slotCount();
    }

    @Override
    public boolean isDepth() {
        return mTargetType.isDepth();
    }

    @Override
    public boolean isLayered() {
        return mTargetType.isLayered();
    }

    @Override
    public boolean isScalar() {
        return mTargetType.isScalar();
    }

    @Override
    public boolean isLiteral() {
        return mTargetType.isLiteral();
    }

    @Override
    public boolean isVector() {
        return mTargetType.isVector();
    }

    @Override
    public boolean isMatrix() {
        return mTargetType.isMatrix();
    }

    @Override
    public boolean isArray() {
        return mTargetType.isArray();
    }

    @Override
    public boolean isStruct() {
        return mTargetType.isStruct();
    }

    @Override
    public boolean isInterfaceBlock() {
        return mTargetType.isInterfaceBlock();
    }

    @Nonnull
    @Override
    public Type[] coercibleTypes() {
        return mTargetType.coercibleTypes();
    }
}
