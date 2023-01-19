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

package icyllis.akashigi.slang.tree;

import icyllis.akashigi.slang.analysis.NodeVisitor;

import javax.annotation.Nonnull;

/**
 * A reference to a variable, through which it can be read or written.
 */
public final class VariableReference extends Expression {

    /**
     * ReferenceKinds.
     */
    public static final int
            kRead_ReferenceKind = 0,        // init once, read at least once
            kWrite_ReferenceKind = 1,       // init once, written at least once
            kReadWrite_ReferenceKind = 2,   // init once, read and written at least once
            kPointer_ReferenceKind = 3;     // no init, written or read at least once

    private Variable mVariable;
    private int mReferenceKind;

    private VariableReference(int position, Variable variable, int referenceKind) {
        super(position, variable.getType());
        mVariable = variable;
        mReferenceKind = referenceKind;
    }

    @Nonnull
    public static Expression make(int position, Variable variable, int referenceKind) {
        return new VariableReference(position, variable, referenceKind);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.VARIABLE_REFERENCE;
    }

    @Override
    public boolean accept(@Nonnull NodeVisitor visitor) {
        return visitor.visitVariableReference(this);
    }

    public Variable getVariable() {
        return mVariable;
    }

    public void setVariable(Variable variable) {
        mVariable = variable;
    }

    public int getReferenceKind() {
        return mReferenceKind;
    }

    public void setReferenceKind(int referenceKind) {
        mReferenceKind = referenceKind;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new VariableReference(position, mVariable, mReferenceKind);
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        return mVariable.getName();
    }
}
