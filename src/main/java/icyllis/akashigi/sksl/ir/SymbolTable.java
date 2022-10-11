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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * Maps identifiers to symbols. Functions, in particular, are mapped to either FunctionDeclaration
 * or UnresolvedFunction depending on whether they are overloaded or not.
 */
public class SymbolTable {

    private final Object2ObjectOpenHashMap<String, Symbol> mSymbols
            = new Object2ObjectOpenHashMap<>();
    private boolean mBuiltIn;

    public SymbolTable mParent;

    public Symbol lookup(String name) {
        Symbol symbol = mSymbols.get(name);
        if (symbol != null) {
            return symbol;
        }
        // The symbol wasn't found; recurse into the parent symbol table.
        return mParent != null ? mParent.lookup(name) : null;
    }

    public boolean isType(String name) {
        Symbol symbol = lookup(name);
        return symbol != null && symbol.kind() == Symbol.Kind_Type;
    }

    public boolean isBuiltInType(String name) {
        if (mBuiltIn) {
            return isType(name);
        }
        return mParent != null && mParent.isBuiltInType(name);
    }
}
