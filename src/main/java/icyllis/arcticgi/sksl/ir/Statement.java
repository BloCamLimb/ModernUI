/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.sksl.ir;

/**
 * Abstract supertype of all statements.
 */
public abstract class Statement extends IRNode {

    public static final int Kind_First = Symbol.Kind_Last + 1;
    public static final int
            Kind_Block = Kind_First,
            Kind_Break = Kind_First + 1,
            Kind_Continue = Kind_First + 2,
            Kind_Discard = Kind_First + 3,
            Kind_Do = Kind_First + 4,
            Kind_Expression = Kind_First + 5,
            Kind_For = Kind_First + 6,
            Kind_If = Kind_First + 7,
            Kind_Nop = Kind_First + 8,
            Kind_Return = Kind_First + 9,
            Kind_Switch = Kind_First + 10,
            Kind_SwitchCase = Kind_First + 11,
            Kind_VarDeclaration = Kind_First + 12;
    public static final int Kind_Last = Kind_VarDeclaration;

    protected Statement(int start, int end, int kind) {
        super(start, end, kind);
        assert (kind >= Kind_First && kind <= Kind_Last);
    }

    public final int kind() {
        return mKind;
    }

    public boolean isEmpty() {
        return false;
    }
}
