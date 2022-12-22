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

import javax.annotation.Nonnull;

/**
 * Represents a symbol table entry.
 */
public abstract class Symbol extends Node {

    private String mName;

    protected Symbol(int position, int kind, String name) {
        super(position, kind);
        assert (kind >= SymbolKind.kFirst && kind <= SymbolKind.kLast);
        mName = name;
    }

    /**
     * @see Node.SymbolKind
     */
    public final int kind() {
        return mKind;
    }

    @Nonnull
    public final String getName() {
        return mName;
    }

    /**
     * Changes the symbol's name.
     */
    public final void setName(String name) {
        mName = name;
    }

    @Nonnull
    public String getMangledName() {
        return mName;
    }

    @Nonnull
    public abstract Type getType();
}
