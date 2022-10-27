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

package icyllis.akashigi.aksl.ir;

import javax.annotation.Nonnull;

/**
 * Represents a symbol table entry.
 */
public abstract class Symbol extends Node {

    public static final int KIND_FIRST = ProgramElement.KIND_LAST + 1;
    public static final int
            KIND_TYPE = KIND_FIRST,
            KIND_VARIABLE = KIND_FIRST + 1,
            KIND_FIELD = KIND_FIRST + 2,
            KIND_FUNCTION_DECLARATION = KIND_FIRST + 3;
    public static final int KIND_LAST = KIND_FUNCTION_DECLARATION;

    private final String mName;
    Type mType;

    protected Symbol(int position, int kind, String name, Type type) {
        super(position, kind);
        assert (kind >= KIND_FIRST && kind <= KIND_LAST);
        mName = name;
        mType = type;
    }

    public final int kind() {
        return mKind;
    }

    @Nonnull
    public final String name() {
        return mName;
    }

    @Nonnull
    public final Type type() {
        assert (mType != null);
        return mType;
    }
}
