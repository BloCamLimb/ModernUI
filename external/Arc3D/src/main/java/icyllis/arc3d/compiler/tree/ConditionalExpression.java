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
import icyllis.arc3d.compiler.Operator;
import icyllis.arc3d.compiler.Position;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A conditional expression (condition ? true-expression : false-expression).
 */
public final class ConditionalExpression extends Expression {

    private Expression mCondition;
    private Expression mWhenTrue;
    private Expression mWhenFalse;

    private ConditionalExpression(int position, Expression condition,
                                  Expression whenTrue, Expression whenFalse) {
        super(position, whenTrue.getType());
        mCondition = condition;
        mWhenTrue = whenTrue;
        mWhenFalse = whenFalse;
        assert whenTrue.getType().matches(whenFalse.getType());
    }

    // Creates a potentially-simplified form of the ternary. Typechecks and coerces input
    // expressions; reports errors via ErrorReporter.
    @Nullable
    public static Expression convert(@NonNull Context context,
                                     int position, Expression condition,
                                     Expression whenTrue, Expression whenFalse) {
        condition = context.getTypes().mBool.coerceExpression(context, condition);
        if (condition == null || whenTrue == null || whenFalse == null) {
            return null;
        }

        if (whenTrue.getType().getComponentType().isOpaque()) {
            context.error(position, "ternary expression of opaque type '" +
                    whenTrue.getType().getName() + "' not allowed");
            return null;
        }

        Type[] types = new Type[3];
        if (!Operator.EQ.determineBinaryType(context, whenTrue.getType(), whenFalse.getType(), types) ||
                !types[0].matches(types[1])) {
            context.error(Position.range(whenTrue.getStartOffset(), whenFalse.getEndOffset()),
                    "conditional operator result mismatch: '" + whenTrue.getType().getName() + "', '" +
                            whenFalse.getType().getName() + "'");
            return null;
        }
        whenTrue = types[0].coerceExpression(context, whenTrue);
        if (whenTrue == null) {
            return null;
        }
        whenFalse = types[1].coerceExpression(context, whenFalse);
        if (whenFalse == null) {
            return null;
        }

        return new ConditionalExpression(position, condition, whenTrue, whenFalse);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.CONDITIONAL;
    }

    public Expression getCondition() {
        return mCondition;
    }

    public void setCondition(Expression condition) {
        mCondition = condition;
    }

    public Expression getWhenTrue() {
        return mWhenTrue;
    }

    public void setWhenTrue(Expression whenTrue) {
        mWhenTrue = whenTrue;
    }

    public Expression getWhenFalse() {
        return mWhenFalse;
    }

    public void setWhenFalse(Expression whenFalse) {
        mWhenFalse = whenFalse;
    }

    @NonNull
    @Override
    public Expression copy(int position) {
        return new ConditionalExpression(position,
                mCondition.copy(),
                mWhenTrue.copy(),
                mWhenFalse.copy());
    }

    @NonNull
    @Override
    public String toString(int parentPrecedence) {
        boolean needsParens = (Operator.PRECEDENCE_CONDITIONAL >= parentPrecedence);
        return (needsParens ? "(" : "") +
                mCondition.toString(Operator.PRECEDENCE_CONDITIONAL) + " ? " +
                mWhenTrue.toString(Operator.PRECEDENCE_CONDITIONAL) + " : " +
                mWhenFalse.toString(Operator.PRECEDENCE_CONDITIONAL) +
                (needsParens ? ")" : "");
    }
}
