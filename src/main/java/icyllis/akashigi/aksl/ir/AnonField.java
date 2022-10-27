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
 * A symbol which should be interpreted as a field access. Fields are added to the symbolTable
 * whenever a bare reference to an identifier should refer to a struct field; in GLSL, this is the
 * result of declaring anonymous interface blocks.
 */
public final class AnonField extends Symbol {

    private final Variable mOwner;
    private final int mFieldIndex;

    public AnonField(int position, Variable owner, int fieldIndex) {
        super(position, KIND_FIELD, owner.type().fields()[fieldIndex].name(),
                owner.type().fields()[fieldIndex].type());
        mOwner = owner;
        mFieldIndex = fieldIndex;
    }

    public int fieldIndex() {
        return mFieldIndex;
    }

    public Variable owner() {
        return mOwner;
    }

    @Nonnull
    @Override
    public String toString() {
        return owner().toString() + "." + name();
    }
}
