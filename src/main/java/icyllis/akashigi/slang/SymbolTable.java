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

import icyllis.akashigi.slang.ir.Node;
import icyllis.akashigi.slang.ir.Symbol;

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

    public static SymbolTable push(SymbolTable table, boolean isBuiltin) {
        return new SymbolTable(table, isBuiltin);
    }

    public static SymbolTable pop(SymbolTable table) {
        return table.mParent;
    }

    public Symbol lookup(String name) {
        Symbol symbol = mTable.get(name);
        if (symbol != null) {
            return symbol;
        }
        // The symbol wasn't found; recurse into the parent symbol table.
        return mParent != null ? mParent.lookup(name) : null;
    }

    public boolean isType(String name) {
        Symbol symbol = lookup(name);
        return symbol != null && symbol.kind() == Node.SymbolKind.kType;
    }

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
        if (symbol.kind() == Node.SymbolKind.kFunctionDeclaration || !hasFunctionName(symbol.getName())) {
            String insertName = symbol.getMangledName();
            if (symbol.kind() == Node.SymbolKind.kFunctionDeclaration) {
                // make sure there isn't a variable of this name
                if (!mTable.containsKey(symbol.getName())) {
                    mTable.put(insertName, symbol);
                    return;
                }
            } else {
                if (mTable.putIfAbsent(insertName, symbol) == null) {
                    return;
                }
            }
        }
        ThreadContext.getInstance().error(symbol.mPosition,
                "symbol '" + symbol.getName() + "' is already defined");
    }

    public boolean hasFunctionName(String name) {
        String candidate = mTable.ceilingKey(name);
        if (candidate != null) {
            int parenAt = candidate.indexOf('(');
            return parenAt != -1 && candidate.substring(0, parenAt).equals(name);
        }
        return false;
    }
}
