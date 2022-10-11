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

/**
 * Represents a top-level element (e.g. function or global variable) in a program.
 */
public abstract class ProgramElement extends IRNode {

    public static final int KIND_FIRST = 0;
    public static final int
            KIND_EXTENSION = KIND_FIRST,
            KIND_FUNCTION = KIND_FIRST + 1,
            KIND_FUNCTION_PROTOTYPE = KIND_FIRST + 2,
            KIND_GLOBAL_VAR = KIND_FIRST + 3,
            KIND_INTERFACE_BLOCK = KIND_FIRST + 4,
            KIND_MODIFIERS = KIND_FIRST + 5,
            KIND_STRUCT_DEFINITION = KIND_FIRST + 6;
    public static final int Kind_Last = KIND_STRUCT_DEFINITION;

    protected ProgramElement(int start, int end, int kind) {
        super(start, end, kind);
        assert (kind >= KIND_FIRST && kind <= Kind_Last);
    }

    public final int kind() {
        return mKind;
    }
}
