/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.compiler.tree;

import icyllis.arc3d.compiler.Modifiers;

import javax.annotation.Nonnull;

/**
 * Represents a variable symbol, whether local, global, or a function parameter.
 * This represents the variable itself (the storage location), which is shared
 * between all VariableReferences which read or write that storage location.
 */
public final class Variable extends Symbol {

    public static final byte
            VariableStorage_Global = 0,
            VariableStorage_InterfaceBlock = 1,
            VariableStorage_Local = 2,
            VariableStorage_Parameter = 3;

    public final int mModifiersPos;
    private final Modifiers mModifiers;
    private final Type mType;
    private final byte mStorage;
    private final boolean mBuiltin;

    public Variable(int position, int modifiersPos, Modifiers modifiers,
                    String name, Type type, boolean builtin, byte storage) {
        super(position, name);
        mModifiersPos = modifiersPos;
        mModifiers = modifiers;
        mType = type;
        mStorage = storage;
        mBuiltin = builtin;
    }

    @Nonnull
    @Override
    public SymbolKind getKind() {
        return SymbolKind.VARIABLE;
    }

    @Nonnull
    @Override
    public Type getType() {
        return mType;
    }

    public Modifiers getModifiers() {
        return mModifiers;
    }

    public boolean isBuiltin() {
        return mBuiltin;
    }

    public byte getStorage() {
        return mStorage;
    }

    public Expression initialValue() {
        return null;
    }

    @Nonnull
    @Override
    public String toString() {
        return mModifiers.toString() + " " + getType().getName() + " " + getName();
    }
}
