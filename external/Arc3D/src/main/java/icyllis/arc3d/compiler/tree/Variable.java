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

import icyllis.arc3d.compiler.Context;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * Represents a variable symbol, whether local, global, or a function parameter.
 * This represents the variable itself (the storage location), which is shared
 * between all VariableReferences which read or write that storage location.
 */
public final class Variable extends Symbol {

    public static final byte
            kLocal_Storage = 0,
            kGlobal_Storage = 1,
            kParameter_Storage = 2;

    private final Modifiers mModifiers;
    private final Type mType;
    private final byte mStorage;
    private final boolean mBuiltin;

    private Node mDecl;
    private WeakReference<InterfaceBlock> mInterfaceBlock;

    public Variable(int position, Modifiers modifiers,
                    Type type, String name, byte storage, boolean builtin) {
        super(position, name);
        mModifiers = modifiers;
        mType = type;
        mStorage = storage;
        mBuiltin = builtin;
    }

    @NonNull
    public static Variable convert(@NonNull Context context,
                                   int pos,
                                   @NonNull Modifiers modifiers,
                                   @NonNull Type type,
                                   @NonNull String name,
                                   byte storage) {
        if (context.getKind().isCompute() && (modifiers.layoutFlags() & Layout.kBuiltin_LayoutFlag) == 0) {
            if (storage == Variable.kGlobal_Storage) {
                if ((modifiers.flags() & Modifiers.kIn_Flag) != 0) {
                    context.error(pos, "pipeline inputs not permitted in compute shaders");
                } else if ((modifiers.flags() & Modifiers.kOut_Flag) != 0) {
                    context.error(pos, "pipeline outputs not permitted in compute shaders");
                }
            }
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

    @NonNull
    public static Variable make(int pos,
                                @NonNull Modifiers modifiers,
                                @NonNull Type type,
                                @NonNull String name,
                                byte storage,
                                boolean builtin) {
        return new Variable(pos, modifiers, type, name, storage, builtin);
    }

    @NonNull
    @Override
    public SymbolKind getKind() {
        return SymbolKind.VARIABLE;
    }

    @NonNull
    @Override
    public Type getType() {
        return mType;
    }

    public Modifiers getModifiers() {
        return mModifiers;
    }

    @NonNull
    public Type getBaseType() {
        return mType.isArray() ? mType.getElementType() : mType;
    }

    public int getArraySize() {
        return mType.isArray() ? mType.getArraySize() : 0;
    }

    public boolean isBuiltin() {
        return mBuiltin;
    }

    public byte getStorage() {
        return mStorage;
    }

    @Nullable
    public Expression initialValue() {
        VariableDeclaration decl = getDeclaration();
        return decl != null ? decl.getInit() : null;
    }

    @Nullable
    public VariableDeclaration getDeclaration() {
        if (mDecl instanceof VariableDeclaration) {
            return (VariableDeclaration) mDecl;
        }
        if (mDecl instanceof GlobalVariable) {
            return ((GlobalVariable) mDecl).getDeclaration();
        }
        return null;
    }

    @Nullable
    public GlobalVariable getGlobalVariable() {
        if (mDecl instanceof GlobalVariable) {
            return (GlobalVariable) mDecl;
        }
        return null;
    }

    public void setDeclaration(VariableDeclaration decl) {
        if (mDecl != null && decl.getVariable() != this) {
            throw new AssertionError();
        }
        if (mDecl == null) {
            mDecl = decl;
        }
    }

    public void setGlobalVariable(GlobalVariable global) {
        if (mDecl != null && global.getDeclaration().getVariable() != this) {
            throw new AssertionError();
        }
        mDecl = global;
    }

    @Nullable
    public InterfaceBlock getInterfaceBlock() {
        return mInterfaceBlock != null ? mInterfaceBlock.get() : null;
    }

    public void setInterfaceBlock(InterfaceBlock interfaceBlock) {
        mInterfaceBlock = new WeakReference<>(interfaceBlock);
    }

    @NonNull
    @Override
    public String toString() {
        return mModifiers.toString() + mType.getName() + " " + getName();
    }
}
