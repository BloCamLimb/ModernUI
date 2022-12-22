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
import java.util.List;
import java.util.StringJoiner;

/**
 * A function declaration.
 */
public final class Function extends Symbol {

    private final int mModifiers;
    private final List<Variable> mParameters;
    private final Type mReturnType;
    private final String mMangledName;

    public Function(int position, int modifiers, String name, List<Variable> parameters, Type returnType) {
        super(position, SymbolKind.kFunctionDeclaration, name);
        mModifiers = modifiers;
        mParameters = parameters;
        mReturnType = returnType;
        StringBuilder mangledName = new StringBuilder(name);
        mangledName.append('(');
        for (Variable p : parameters) {
            mangledName.append(p.getType().getDescriptor()).append(';');
        }
        mMangledName = mangledName.toString();
    }

    @Nonnull
    @Override
    public Type getType() {
        return mReturnType;
    }

    @Nonnull
    @Override
    public String getMangledName() {
        return mMangledName;
    }

    @Nonnull
    @Override
    public String toString() {
        String result =
                (mModifiers != 0 ? Modifier.describeFlags(mModifiers) + " " : "") +
                        mReturnType.displayName() + " " + getName() + "(";
        StringJoiner joiner = new StringJoiner(", ");
        for (Variable p : mParameters) {
            String s = "";
            if (p.getModifiers() != 0) {
                s += Modifier.describeFlags(p.getModifiers()) + " ";
            }
            s += p.getType().displayName();
            s += " ";
            s += p.getName();
            joiner.add(s);
        }
        return result + joiner + ")";
    }
}
