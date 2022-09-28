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

package icyllis.akashigi.sksl.ir;

import icyllis.akashigi.sksl.Modifiers;

import javax.annotation.Nonnull;

/**
 * Represents a variable, whether local, global, or a function parameter. This represents the
 * variable itself (the storage location), which is shared between all VariableReferences which
 * read or write that storage location.
 */
public final class Variable extends Symbol {

    public static final byte
            VariableStorage_Global = 0,
            VariableStorage_InterfaceBlock = 1,
            VariableStorage_Local = 2,
            VariableStorage_Parameter = 3;

    public final int mModifiersStart;
    public final int mModifiersEnd;
    private Modifiers mModifiers;
    private final byte mStorage;
    private final boolean mBuiltin;

    public Variable(int start, int end, int modifiersStart, int modifiersEnd, Modifiers modifiers,
                    String name, Type type, boolean builtin, byte storage) {
        super(start, end, Kind_Variable, name, type);
        mModifiersStart = modifiersStart;
        mModifiersEnd = modifiersEnd;
        mModifiers = modifiers;
        mStorage = storage;
        mBuiltin = builtin;
    }

    public Modifiers modifiers() {
        return mModifiers;
    }

    public void setModifiers(Modifiers modifiers) {
        mModifiers = modifiers;
    }

    public boolean isBuiltin() {
        return mBuiltin;
    }

    public byte getStorage() {
        return mStorage;
    }

    @Nonnull
    @Override
    public String description() {
        return modifiers().description() + type().displayName() + " " + name();
    }
}
