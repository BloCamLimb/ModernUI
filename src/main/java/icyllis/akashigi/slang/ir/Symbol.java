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

import icyllis.akashigi.slang.analysis.NodeVisitor;

import javax.annotation.Nonnull;

/**
 * Represents a symbol table entry.
 */
public abstract class Symbol extends Node {

    private String mName;

    protected Symbol(int position, String name) {
        super(position);
        mName = name;
    }

    @Override
    public final boolean accept(@Nonnull NodeVisitor visitor) {
        // symbols will not be visited
        throw new AssertionError();
    }

    /**
     * @see Node.SymbolKind
     */
    @Nonnull
    public abstract SymbolKind getKind();

    @Nonnull
    public final String getName() {
        return mName;
    }

    /**
     * Changes the symbol's name.
     */
    public final void setName(@Nonnull String name) {
        mName = name;
    }

    @Nonnull
    public abstract Type getType();
}
