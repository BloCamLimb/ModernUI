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

package icyllis.akashigi.slang.ir;

import icyllis.akashigi.slang.Operator;
import icyllis.akashigi.slang.ThreadContext;
import icyllis.akashigi.slang.analysis.NodeVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An expression modified by a unary operator appearing after it.
 * <p>
 * Possible examples: 'i++', 'i--'
 *
 * @see PrefixExpression
 */
public final class PostfixExpression extends Expression {

    private final Expression mOperand;
    private final Operator mOperator;

    private PostfixExpression(int position, Expression operand, Operator op) {
        super(position, operand.getType());
        mOperand = operand;
        mOperator = op;
    }

    @Nullable
    public static Expression convert(int position, Expression base, Operator op) {
        ThreadContext context = ThreadContext.getInstance();
        Type baseType = base.getType();
        if (!baseType.isNumeric()) {
            context.error(position, "'" + op.tightOperatorName() +
                    "' cannot operate on '" + baseType.getName() + "'");
            return null;
        }
        return make(position, base, op);
    }

    @Nonnull
    public static Expression make(int position, Expression base, Operator op) {
        assert base.getType().isNumeric();
        return new PostfixExpression(position, base, op);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.POSTFIX;
    }

    @Override
    public boolean accept(@Nonnull NodeVisitor visitor) {
        if (visitor.visitPostfix(this)) {
            return true;
        }
        return mOperand.accept(visitor);
    }

    public Expression getOperand() {
        return mOperand;
    }

    public Operator getOperator() {
        return mOperator;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new PostfixExpression(position, mOperand.clone(), mOperator);
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        boolean needsParens = (Operator.PRECEDENCE_POSTFIX >= parentPrecedence);
        return (needsParens ? "(" : "") +
                mOperand.toString(Operator.PRECEDENCE_POSTFIX) +
                (mOperator.tightOperatorName()) +
                (needsParens ? ")" : "");
    }
}
