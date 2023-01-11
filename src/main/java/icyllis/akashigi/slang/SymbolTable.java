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

package icyllis.akashigi.slang;

import icyllis.akashigi.slang.ir.*;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Maps identifiers to symbols.
 */
public final class SymbolTable {

    private final NavigableMap<String, Symbol> mTable = new TreeMap<>();

    private final SymbolTable mParent;
    private final boolean mBuiltin;

    /**
     * The root symbol table is always builtin.
     */
    SymbolTable() {
        this(null, true);
    }

    private SymbolTable(SymbolTable parent, boolean builtin) {
        mParent = parent;
        mBuiltin = builtin;
    }

    @Nonnull
    public static SymbolTable push(SymbolTable table) {
        Objects.requireNonNull(table);
        return new SymbolTable(table, table.mBuiltin);
    }

    @Nonnull
    public static SymbolTable push(SymbolTable table, boolean builtin) {
        Objects.requireNonNull(table);
        return new SymbolTable(table, builtin);
    }

    public SymbolTable getParent() {
        return mParent;
    }

    /**
     * @return true if this symbol table is at builtin level
     */
    public boolean isBuiltin() {
        return mBuiltin;
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
        if (mBuiltin) {
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
        if (mBuiltin) {
            return isType(name);
        }
        return mParent != null && mParent.isBuiltinType(name);
    }

    /**
     * Inserts a symbol into the symbol table, reports errors if there was a name collision.
     *
     * @return the given symbol if successful, or null if there was a name collision
     */
    @Nullable
    public <T extends Symbol> T insert(T symbol) {
        String key = symbol.getName();

        // If this is a function declaration, we need to keep the overload chain in sync.
        if (symbol instanceof Function) {
            // If we have a function with the same name...
            if (find(key) instanceof Function next) {
                ((Function) symbol).setNextOverload(next);
                mTable.put(key, symbol);
                return symbol;
            }
        }

        if (mParent == null || mParent.find(key) == null) {
            if (mTable.putIfAbsent(key, symbol) == null) {
                return symbol;
            }
        }

        ThreadContext.getInstance().error(symbol.mPosition,
                "symbol '" + key + "' is already defined");
        return null;
    }

    /**
     * Finds or creates an array type with the given element type and array size.
     */
    public Type computeArrayType(Type type, int size) {
        if (size == 0) {
            return type;
        }
        // If this is a builtin type, we add it as high as possible in the symbol table tree (at the
        // module boundary), to enable additional reuse of the array-type.
        if (mParent != null && type.isInBuiltinTypes()) {
            return mParent.computeArrayType(type, size);
        }
        // Reuse an existing array type with this name if one already exists in our symbol table.
        String name = type.getArrayName(size);
        Symbol symbol;
        if ((symbol = find(name)) != null) {
            return (Type) symbol;
        }
        Type result = Type.makeArrayType(name, type, size);
        return Objects.requireNonNull(insert(result));
    }
}
