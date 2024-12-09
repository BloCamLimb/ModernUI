/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.compiler.tree;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A symbol which should be interpreted as a field access. Fields are added to the SymbolTable
 * whenever a bare reference to an identifier should refer to a struct field; in GLSL, this is the
 * result of declaring anonymous interface blocks.
 */
public final class AnonymousField extends Symbol {

    private final Variable mContainer;
    private final int mFieldIndex;

    public AnonymousField(int position, @NonNull Variable container, int fieldIndex) {
        super(position, container.getType().getFields().get(fieldIndex).name());
        mContainer = container;
        mFieldIndex = fieldIndex;
    }

    @NonNull
    @Override
    public SymbolKind getKind() {
        return SymbolKind.ANONYMOUS_FIELD;
    }

    @NonNull
    @Override
    public Type getType() {
        return mContainer.getType().getFields().get(mFieldIndex).type();
    }

    public int getFieldIndex() {
        return mFieldIndex;
    }

    public @NonNull Variable getContainer() {
        return mContainer;
    }

    @NonNull
    @Override
    public String toString() {
        return mContainer.toString() + "." + getName();
    }
}
