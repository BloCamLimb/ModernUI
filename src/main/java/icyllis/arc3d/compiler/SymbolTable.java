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

package icyllis.arc3d.compiler;

import icyllis.arc3d.compiler.tree.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

/**
 * Maps identifiers to symbols, each instance represents a scope level or a module boundary.
 */
public final class SymbolTable {

    private final Map<String, Symbol> mTable = new HashMap<>();

    private final SymbolTable mParent;
    /**
     * True if at built-in level.
     */
    private final boolean mIsBuiltin;
    /**
     * True if at global level.
     */
    private final boolean mIsModuleLevel;

    /**
     * Constructor for the root symbol table.
     */
    SymbolTable() {
        this(null, true, false);
    }

    private SymbolTable(SymbolTable parent, boolean isBuiltin, boolean isModuleLevel) {
        mParent = parent;
        mIsBuiltin = isBuiltin;
        mIsModuleLevel = isModuleLevel;
    }

    /**
     * Enters a scope level.
     */
    @NonNull
    SymbolTable enterScope() {
        return new SymbolTable(this, mIsBuiltin, /*isModuleLevel*/false);
    }

    /**
     * Enters a module level.
     */
    @NonNull
    SymbolTable enterModule(boolean isBuiltin) {
        if ((isBuiltin && !mIsBuiltin) ||
                (!mIsModuleLevel && mParent != null)) {
            throw new AssertionError();
        }
        return new SymbolTable(this, isBuiltin, /*isModuleLevel*/true);
    }

    SymbolTable leaveScope() {
        if (mIsModuleLevel || mParent == null) {
            throw new AssertionError();
        }
        return mParent;
    }

    public SymbolTable getParent() {
        return mParent;
    }

    /**
     * @return true if this symbol table is at built-in level
     */
    public boolean isBuiltin() {
        return mIsBuiltin;
    }

    /**
     * Looks up the requested symbol and returns a const pointer.
     */
    @Nullable
    public Symbol find(String name) {
        Symbol symbol;
        if ((symbol = mTable.get(name)) != null) {
            return symbol;
        }
        return mParent != null ? mParent.find(name) : null;
    }

    /**
     * Looks up the requested symbol, only searching the built-in symbol tables. Always const.
     */
    @Nullable
    public Symbol findBuiltinSymbol(String name) {
        if (mIsBuiltin) {
            return find(name);
        }
        return mParent != null ? mParent.findBuiltinSymbol(name) : null;
    }

    /**
     * Returns true if the name refers to a type (user or built-in) in the current symbol table.
     */
    public boolean isType(String name) {
        return find(name) instanceof Type;
    }

    /**
     * Returns true if the name refers to a builtin type.
     */
    public boolean isBuiltinType(String name) {
        if (mIsBuiltin) {
            return isType(name);
        }
        return mParent != null && mParent.isBuiltinType(name);
    }

    <T extends Symbol> T insert(@NonNull T symbol) {
        return Objects.requireNonNull(insert(null, symbol));
    }

    /**
     * Inserts a symbol into the symbol table, reports errors if there was a name collision.
     *
     * @return the given symbol if successful, or null if there was a name collision
     */
    @Nullable
    public <T extends Symbol> T insert(Context context, @NonNull T symbol) {
        String key = symbol.getName();
        if (key.isEmpty()) {
            // We have legitimate use cases of nameless symbols, such as anonymous function parameters.
            // If we find one here, we don't need to add its name to the symbol table.
            return symbol;
        }
        if (key.length() > 1024) {
            context.error(symbol.mPosition,
                    "symbol name '" + key + "' is too long, " + key.length() + " > 1024 chars");
            return null;
        }

        // If this is a function declaration, we need to keep the overload chain in sync.
        if (symbol instanceof FunctionDecl) {
            // If we have a function with the same name...
            if (find(key) instanceof FunctionDecl next) {
                // The new definition is at the top
                ((FunctionDecl) symbol).setNextOverload(next);
                mTable.put(key, symbol);
                return symbol;
            }
        }

        // the symbol cannot be defined at parent's global scope
        if (!mIsModuleLevel || mParent == null || mParent.find(key) == null) {
            if (mTable.putIfAbsent(key, symbol) == null) {
                return symbol;
            }
        }

        context.error(symbol.mPosition,
                "symbol '" + key + "' is already defined");
        return null;
    }

    /**
     * Finds or creates an array type with the given element type and array size.
     */
    public Type getArrayType(Type type, int size) {
        if (size == 0) {
            return type;
        }
        // If this is a builtin type, we add it as high as possible in the symbol table tree
        // (at the module level), to enable additional reuse of the array-type.
        if (!mIsModuleLevel && mParent != null && type.isInBuiltinTypes()) {
            return mParent.getArrayType(type, size);
        }
        // Reuse an existing array type with this name if one already exists in our symbol table.
        String name = type.getArrayName(size);
        Symbol symbol;
        if ((symbol = find(name)) != null) {
            return (Type) symbol;
        }
        Type result = Type.makeArrayType(name, type, size);
        return insert(result);
    }
}
