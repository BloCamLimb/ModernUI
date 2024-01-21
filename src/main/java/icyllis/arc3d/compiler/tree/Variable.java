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

import icyllis.arc3d.compiler.ThreadContext;

import javax.annotation.Nonnull;

/**
 * Represents a variable symbol, whether local, global, or a function parameter.
 * This represents the variable itself (the storage location), which is shared
 * between all VariableReferences which read or write that storage location.
 */
public final class Variable extends Symbol {

    public static final byte
            kGlobal_Storage = 0,
            kInterfaceBlock_Storage = 1,
            kLocal_Storage = 2,
            kParameter_Storage = 3;

    private final Modifiers mModifiers;
    private final Type mType;
    private final byte mStorage;
    private final boolean mBuiltin;

    public Variable(int position, Modifiers modifiers,
                    String name, Type type, boolean builtin, byte storage) {
        super(position, name);
        mModifiers = modifiers;
        mType = type;
        mStorage = storage;
        mBuiltin = builtin;
    }

    public static Variable convert(int pos,
                                   Modifiers modifiers,
                                   Type type,
                                   String name,
                                   byte storage) {
        var context = ThreadContext.getInstance();
        if (type.isUnsizedArray() && storage != kInterfaceBlock_Storage) {
            context.error(pos, "unsized arrays are not permitted here");
        }
        if (storage == kParameter_Storage) {
            // The `in` modifier on function parameters is implicit, so we can replace `in float x` with
            // `float x`. This prevents any ambiguity when matching a function by its param types.
            if ((modifiers.flags() & (Modifiers.kOut_Flag | Modifiers.kIn_Flag)) == Modifiers.kIn_Flag) {
                modifiers.clearFlag(Modifiers.kOut_Flag | Modifiers.kIn_Flag);
            }
        }
        return make(pos, modifiers, type, name, storage, context.isBuiltin());
    }

    public static Variable make(int pos,
                                Modifiers modifiers,
                                Type type,
                                String name,
                                byte storage,
                                boolean builtin) {
        return new Variable(pos, modifiers, name, type, builtin, storage);
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
