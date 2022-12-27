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

import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Maps identifiers to symbols.
 */
public class SymbolTable {

    private final NavigableMap<String, Symbol> mTable = new TreeMap<>();

    public SymbolTable mParent;
    private boolean mBuiltin;

    public SymbolTable(boolean builtin) {
        mBuiltin = builtin;
    }

    public SymbolTable(SymbolTable parent, boolean builtin) {
        mBuiltin = builtin;
        mParent = parent;
    }

    public static SymbolTable push(SymbolTable table) {
        return push(table, table.isBuiltin());
    }

    public static SymbolTable push(SymbolTable table, boolean isBuiltin) {
        return new SymbolTable(table, isBuiltin);
    }

    public static SymbolTable pop(SymbolTable table) {
        return table.mParent;
    }

    public static SymbolTable pushIfBuiltin(SymbolTable table) {
        if (!table.isBuiltin()) {
            return table;
        }
        return push(table, false);
    }

    public boolean isBuiltin() {
        return mBuiltin;
    }

    /**
     * Looks up the requested symbol and returns a const pointer.
     */
    public Symbol find(String name) {
        Symbol symbol = mTable.get(name);
        if (symbol != null) {
            return symbol;
        }
        return mParent != null ? mParent.find(name) : null;
    }

    /**
     * Looks up the requested symbol, only searching the built-in symbol tables. Always const.
     */
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
        Symbol symbol = find(name);
        return symbol != null && symbol.kind() == Node.SymbolKind.kType;
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
     */
    public void insert(Symbol symbol) {
        String key = symbol.getName();

        // If this is a function declaration, we need to keep the overload chain in sync.
        if (symbol.kind() == Node.SymbolKind.kFunctionDeclaration) {
            // If we have a function with the same name...
            Symbol existingSymbol = find(key);
            if (existingSymbol != null && existingSymbol.kind() == Node.SymbolKind.kFunctionDeclaration) {
                ((FunctionDeclaration) symbol).setNextOverload((FunctionDeclaration) existingSymbol);
                mTable.put(key, symbol);
                return;
            }
        }

        if (mParent == null || mParent.find(key) == null) {
            if (mTable.putIfAbsent(key, symbol) == null) {
                return;
            }
        }

        ThreadContext.getInstance().error(symbol.mPosition,
                "symbol '" + key + "' is already defined");
    }

    public Type findOrInsertArrayType(Type type, int size) {
        if (size == 0) {
            return type;
        }
        // If this is a builtin type, we add it as high as possible in the symbol table tree (at the
        // module boundary), to enable additional reuse of the array-type.
        if (mParent != null && type.isInBuiltinTypes()) {
            return mParent.findOrInsertArrayType(type, size);
        }
        // Reuse an existing array type with this name if one already exists in our symbol table.
        String arrayName = type.getArrayName(size);
        Symbol existingSymbol = find(arrayName);
        if (existingSymbol != null) {
            return (Type) existingSymbol;
        }
        Type arrayType = Type.makeArrayType(arrayName, type, size);
        insert(arrayType);
        return arrayType;
    }
}
