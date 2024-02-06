/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.compiler.tree;

import icyllis.arc3d.compiler.Context;
import icyllis.arc3d.compiler.analysis.NodeVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * <pre>
 * if (condition)
 *     true-statement
 * else
 *     false-statement
 * </pre>
 */
public final class IfStatement extends Statement {

    private Expression mCondition;
    private Statement mWhenTrue;
    private Statement mWhenFalse;

    public IfStatement(int position, Expression condition, Statement whenTrue, Statement whenFalse) {
        super(position);
        mCondition = condition;
        mWhenTrue = whenTrue;
        mWhenFalse = whenFalse;
    }

    @Nullable
    public static Statement convert(@Nonnull Context context,
                                    int position, Expression condition, Statement whenTrue, Statement whenFalse) {
        condition = context.getTypes().mBool.coerceExpression(context, condition);
        if (condition == null) {
            return null;
        }
        return make(position, condition, whenTrue, whenFalse);
    }

    public static Statement make(int position, Expression condition, Statement whenTrue, Statement whenFalse) {

        return new IfStatement(position, condition, whenTrue, whenFalse);
    }

    @Override
    public StatementKind getKind() {
        return StatementKind.IF;
    }

    @Override
    public boolean accept(@Nonnull NodeVisitor visitor) {
        if (visitor.visitIf(this)) {
            return true;
        }
        return (mCondition != null && mCondition.accept(visitor)) ||
                (mWhenTrue != null && mWhenTrue.accept(visitor)) ||
                (mWhenFalse != null && mWhenFalse.accept(visitor));
    }

    public Expression getCondition() {
        return mCondition;
    }

    public void setCondition(Expression condition) {
        mCondition = condition;
    }

    public Statement getWhenTrue() {
        return mWhenTrue;
    }

    public void setWhenTrue(Statement whenTrue) {
        mWhenTrue = whenTrue;
    }

    public Statement getWhenFalse() {
        return mWhenFalse;
    }

    public void setWhenFalse(Statement whenFalse) {
        mWhenFalse = whenFalse;
    }

    @Nonnull
    @Override
    public String toString() {
        String result = "if (" + mCondition.toString() + ") " + mWhenTrue.toString();
        if (mWhenFalse != null) {
            result += " else " + mWhenFalse;
        }
        return result;
    }
}
