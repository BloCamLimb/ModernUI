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

import icyllis.akashigi.slang.*;
import icyllis.akashigi.slang.analysis.NodeVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A conditional expression (condition ? trueExpr : falseExpr).
 */
public final class ConditionalExpression extends Expression {

    private Expression mCondition;
    private Expression mTrueExpr;
    private Expression mFalseExpr;

    private ConditionalExpression(int position, Expression condition,
                                  Expression trueExpr, Expression falseExpr) {
        super(position, trueExpr.getType());
        mCondition = condition;
        mTrueExpr = trueExpr;
        mFalseExpr = falseExpr;
        assert trueExpr.getType().matches(falseExpr.getType());
    }

    // Creates a potentially-simplified form of the ternary. Typechecks and coerces input
    // expressions; reports errors via ErrorReporter.
    @Nullable
    public static Expression convert(int position, Expression condition,
                                     Expression trueExpr, Expression falseExpr) {
        ThreadContext context = ThreadContext.getInstance();
        condition = context.getTypes().mBool.coerceExpression(condition);
        if (condition == null || trueExpr == null || falseExpr == null) {
            return null;
        }

        if (trueExpr.getType().getComponentType().isOpaque()) {
            context.error(position, "ternary expression of opaque type '" +
                    trueExpr.getType().getName() + "' not allowed");
            return null;
        }

        Type[] types = new Type[3];
        if (!Operator.EQ.determineBinaryType(trueExpr.getType(), falseExpr.getType(), types) ||
                !types[0].matches(types[1])) {
            context.error(Position.range(trueExpr.getStartOffset(), falseExpr.getEndOffset()),
                    "conditional operator result mismatch: '" + trueExpr.getType().getName() + "', '" +
                            falseExpr.getType().getName() + "'");
            return null;
        }
        trueExpr = types[0].coerceExpression(trueExpr);
        if (trueExpr == null) {
            return null;
        }
        falseExpr = types[1].coerceExpression(falseExpr);
        if (falseExpr == null) {
            return null;
        }

        return new ConditionalExpression(position, condition, trueExpr, falseExpr);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.CONDITIONAL;
    }

    @Override
    public boolean accept(@Nonnull NodeVisitor visitor) {
        if (visitor.visitConditional(this)) {
            return true;
        }
        return mCondition.accept(visitor) ||
                (mTrueExpr != null && mTrueExpr.accept(visitor)) ||
                (mFalseExpr != null && mFalseExpr.accept(visitor));
    }

    public Expression getCondition() {
        return mCondition;
    }

    public Expression getTrueExpression() {
        return mTrueExpr;
    }

    public Expression getFalseExpression() {
        return mFalseExpr;
    }

    public void setCondition(Expression condition) {
        mCondition = condition;
    }

    public void setTrueExpression(Expression trueExpr) {
        mTrueExpr = trueExpr;
    }

    public void setFalseExpression(Expression falseExpr) {
        mFalseExpr = falseExpr;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new ConditionalExpression(position,
                mCondition.clone(),
                mTrueExpr.clone(),
                mFalseExpr.clone());
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        boolean needsParens = (Operator.PRECEDENCE_CONDITIONAL >= parentPrecedence);
        return (needsParens ? "(" : "") +
                mCondition.toString(Operator.PRECEDENCE_CONDITIONAL) + " ? " +
                mTrueExpr.toString(Operator.PRECEDENCE_CONDITIONAL) + " : " +
                mFalseExpr.toString(Operator.PRECEDENCE_CONDITIONAL) +
                (needsParens ? ")" : "");
    }
}
