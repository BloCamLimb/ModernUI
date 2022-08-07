/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.arsl.ir;

import javax.annotation.Nonnull;

/**
 * Represents a symbol table entry.
 */
public abstract class Symbol extends IRNode {

    public static final int Kind_First = ProgramElement.Kind_Last + 1;
    public static final int
            Kind_External = Kind_First,
            Kind_Field = Kind_First + 1,
            Kind_FunctionDeclaration = Kind_First + 2,
            Kind_Type = Kind_First + 3,
            Kind_UnresolvedFunction = Kind_First + 4,
            Kind_Variable = Kind_First + 5;
    public static final int Kind_Last = Kind_Variable;

    private final String mName;
    private final Type mType;

    protected Symbol(int start, int end, int kind, String name, Type type) {
        super(start, end, kind);
        assert (kind >= Kind_First && kind <= Kind_Last);
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
    public Type type() {
        assert (mType != null);
        return mType;
    }
}
