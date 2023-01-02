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

import icyllis.akashigi.slang.Modifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.StringJoiner;

/**
 * A function declaration (function symbol).
 */
public final class Function extends Symbol {

    private final int mModifiers;
    private final List<Variable> mParameters;
    private final Type mReturnType;

    private boolean mBuiltin;
    private boolean mEntryPoint;
    private int mIntrinsicKind;

    private Function mNextOverload;
    private FunctionDefinition mDefinition;

    public Function(int position, int modifiers, String name, List<Variable> parameters, Type returnType) {
        super(position, SymbolKind.kFunctionDeclaration, name);
        mModifiers = modifiers;
        mParameters = parameters;
        mReturnType = returnType;
    }

    @Nonnull
    @Override
    public Type getType() {
        return mReturnType;
    }

    @Nonnull
    public String getMangledName() {
        StringBuilder mangledName = new StringBuilder(getName());
        mangledName.append('(');
        for (Variable p : mParameters) {
            mangledName.append(p.getType().getDesc()).append(';');
        }
        return  mangledName.toString();
    }

    @Nullable
    public Function getNextOverload() {
        return mNextOverload;
    }

    public void setNextOverload(Function overload) {
        assert (overload == null || overload.getName().equals(getName()));
        mNextOverload = overload;
    }

    @Nonnull
    @Override
    public String toString() {
        String result =
                (mModifiers != 0 ? Modifier.describeFlags(mModifiers) + " " : "") +
                        mReturnType.getName() + " " + getName() + "(";
        StringJoiner joiner = new StringJoiner(", ");
        for (Variable p : mParameters) {
            String s = "";
            if (p.getModifiers() != 0) {
                s += Modifier.describeFlags(p.getModifiers()) + " ";
            }
            s += p.getType().getName();
            s += " ";
            s += p.getName();
            joiner.add(s);
        }
        return result + joiner + ")";
    }
}
