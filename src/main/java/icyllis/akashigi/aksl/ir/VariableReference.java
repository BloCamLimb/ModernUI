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

public final class VariableReference extends Expression {

    public static final int
            REF_KIND_READ = 0,          // init once, read at least once
            REF_KIND_WRITE = 1,         // init once, written at least once
            REF_KIND_READ_WRITE = 2,    // init once, read and written at least once
            REF_KIND_POINTER = 3;       // no init, written or read at least once

    private final Variable mVariable;
    private int mRefKind;

    public VariableReference(int position, Variable variable, int refKind) {
        super(position, KIND_VARIABLE_REFERENCE, variable.type());
        mVariable = variable;
        mRefKind = refKind;
    }

    public Variable getVariable() {
        return mVariable;
    }

    public int getRefKind() {
        return mRefKind;
    }

    public void setRefKind(int refKind) {
        mRefKind = refKind;
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        return mVariable.toString();
    }
}
