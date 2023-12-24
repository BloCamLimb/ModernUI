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
import javax.annotation.Nullable;
import java.util.List;
import java.util.StringJoiner;

/**
 * Represents a function declaration (function symbol). If the function is overloaded,
 * it will serve as a singly linked list.
 */
public final class Function extends Symbol {

    private final Modifiers mModifiers;
    private final List<Variable> mParameters;
    private final Type mReturnType;

    private boolean mBuiltin;
    private boolean mEntryPoint;
    private int mIntrinsicKind;

    private Function mNextOverload;
    private FunctionDefinition mDefinition;

    public Function(int position, Modifiers modifiers, String name, List<Variable> parameters, Type returnType) {
        super(position, name);
        mModifiers = modifiers;
        mParameters = parameters;
        mReturnType = returnType;
    }

    @Nonnull
    @Override
    public SymbolKind getKind() {
        return SymbolKind.FUNCTION;
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

    /**
     * Sets the previously defined function symbol with the same function name.
     */
    public void setNextOverload(Function overload) {
        assert (overload == null || overload.getName().equals(getName()));
        mNextOverload = overload;
    }

    public Modifiers getModifiers() {
        return mModifiers;
    }

    @Nonnull
    @Override
    public String toString() {
        String result =
                (mModifiers.flags() != 0 ? Modifiers.describeFlags(mModifiers.flags()) + " " : "") +
                        mReturnType.getName() + " " + getName() + "(";
        StringJoiner joiner = new StringJoiner(", ");
        for (Variable p : mParameters) {
            String s = "";
            if (p.getModifiers().flags() != 0) {
                s += Modifiers.describeFlags(p.getModifiers().flags()) + " ";
            }
            s += p.getType().getName();
            s += " ";
            s += p.getName();
            joiner.add(s);
        }
        return result + joiner + ")";
    }
}
