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
import java.util.List;

public final class AliasType extends Type {

    private final Type mTargetType;

    AliasType(String name, Type targetType) {
        super(name, targetType.getAbbreviation(), targetType.getTypeKind());
        mTargetType = targetType;
    }

    @Nonnull
    @Override
    public Type getResolvedType() {
        return mTargetType;
    }

    @Nonnull
    @Override
    public Type getComponentType() {
        return mTargetType.getComponentType();
    }

    @Override
    public byte getScalarKind() {
        return mTargetType.getScalarKind();
    }

    @Override
    public int getPriority() {
        return mTargetType.getPriority();
    }

    @Override
    public int getColumns() {
        return mTargetType.getColumns();
    }

    @Override
    public int getRows() {
        return mTargetType.getRows();
    }

    @Override
    public int getBitWidth() {
        return mTargetType.getBitWidth();
    }

    @Override
    public boolean isPrivate() {
        return mTargetType.isPrivate();
    }

    @Override
    public int getSlots() {
        return mTargetType.getSlots();
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
    public boolean isInterfaceBlock() {
        return mTargetType.isInterfaceBlock();
    }

    @Nonnull
    @Override
    public List<Type> getCoercibleTypes() {
        return mTargetType.getCoercibleTypes();
    }
}
