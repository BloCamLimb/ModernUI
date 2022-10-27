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

/**
 * Abstract supertype of all statements.
 */
public abstract class Statement extends Node {

    public static final int KIND_FIRST = Symbol.KIND_LAST + 1;
    public static final int
            KIND_BLOCK = KIND_FIRST,
            KIND_BREAK = KIND_FIRST + 1,
            KIND_CONTINUE = KIND_FIRST + 2,
            KIND_DISCARD = KIND_FIRST + 3,
            KIND_DO = KIND_FIRST + 4,
            KIND_EXPRESSION = KIND_FIRST + 5,
            KIND_FOR = KIND_FIRST + 6,
            KIND_IF = KIND_FIRST + 7,
            KIND_NOP = KIND_FIRST + 8,
            KIND_RETURN = KIND_FIRST + 9,
            KIND_SWITCH = KIND_FIRST + 10,
            KIND_SWITCH_CASE = KIND_FIRST + 11,
            KIND_VAR_DECLARATION = KIND_FIRST + 12;
    public static final int KIND_LAST = KIND_VAR_DECLARATION;

    protected Statement(int position, int kind) {
        super(position, kind);
        assert (kind >= KIND_FIRST && kind <= KIND_LAST);
    }

    public final int kind() {
        return mKind;
    }

    public boolean isEmpty() {
        return false;
    }
}
