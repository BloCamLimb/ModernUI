/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.compiler.tree;

import icyllis.arc3d.compiler.Context;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * <pre>
 * for (init; condition; step)
 *     loop-statement
 * </pre>
 */
public final class ForLoop extends Statement {

    private Statement mInit;
    private Expression mCondition;
    private Expression mStep;
    private Statement mStatement;

    public ForLoop(int position, Statement init, Expression condition, Expression step, Statement statement) {
        super(position);
        mInit = init;
        mCondition = condition;
        mStep = step;
        mStatement = statement;
    }

    @Nullable
    public static Statement convert(@NonNull Context context,
                                    int pos,
                                    Statement init,
                                    Expression cond,
                                    Expression step,
                                    Statement statement) {
        if (cond != null) {
            cond = context.getTypes().mBool.coerceExpression(context, cond);
            if (cond == null) {
                return null;
            }
        }

        if (step != null && step.isIncomplete(context)) {
            return null;
        }

        return make(pos, init, cond, step, statement);
    }

    public static Statement make(int pos,
                                 Statement init,
                                 Expression cond,
                                 Expression step,
                                 Statement statement) {
        return new ForLoop(pos, init, cond, step, statement);
    }

    @Override
    public StatementKind getKind() {
        return StatementKind.FOR_LOOP;
    }

    @Override
    public boolean accept(@NonNull TreeVisitor visitor) {
        if (visitor.visitForLoop(this)) {
            return true;
        }
        return (mInit != null && mInit.accept(visitor)) ||
                (mCondition != null && mCondition.accept(visitor)) ||
                (mStep != null && mStep.accept(visitor)) ||
                mStatement.accept(visitor);
    }

    public Statement getInit() {
        return mInit;
    }

    public void setInit(Statement init) {
        mInit = init;
    }

    public Expression getCondition() {
        return mCondition;
    }

    public void setCondition(Expression condition) {
        mCondition = condition;
    }

    public Expression getStep() {
        return mStep;
    }

    public void setStep(Expression step) {
        mStep = step;
    }

    public Statement getStatement() {
        return mStatement;
    }

    public void setStatement(Statement statement) {
        mStatement = statement;
    }

    @NonNull
    @Override
    public String toString() {
        String result = "for (";
        if (mInit != null) {
            result += mInit.toString();
        } else {
            result += ";";
        }
        result += " ";
        if (mCondition != null) {
            result += mCondition.toString();
        }
        result += "; ";
        if (mStep != null) {
            result += mStep.toString();
        }
        result += ") " + mStatement.toString();
        return result;
    }
}
