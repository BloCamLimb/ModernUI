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

import javax.annotation.Nonnull;

/**
 * An expression modified by a unary operator appearing before it.
 * <p>
 * Possible examples: '+a', '-a', '++i', '--i', '!flag', '~flag'
 *
 * @see PostfixExpression
 */
public final class PrefixExpression extends Expression {

    private final Operator mOperator;
    private final Expression mOperand;

    private PrefixExpression(int position, Operator op, Expression operand) {
        super(position, ExpressionKind.kPrefix, operand.getType());
        mOperator = op;
        mOperand = operand;
    }

    @Nonnull
    public static Expression make(int position, Operator op, Expression base) {
        switch (op) {
            case ADD: // unary plus
                assert (!base.getType().isArray());
                assert (base.getType().getComponentType().isNumeric());
                base.mPosition = position;
                return base;

            case SUB: // unary minus
                assert (!base.getType().isArray());
                assert (base.getType().getComponentType().isNumeric());
                //return negate_operand(context, pos, std::move (base));

            case LOGICAL_NOT:
                assert (base.getType().isBoolean());
                //return logical_not_operand(context, pos, std::move (base));

            case INC:
            case DEC:
                assert (base.getType().isNumeric());
                break;

            case BITWISE_NOT:
                assert (!base.getType().isArray());
                assert (base.getType().getComponentType().isInteger());
                assert (!base.isLiteral());
                break;

            default:
                throw new AssertionError("unsupported prefix operator: " + op.operatorName());
        }

        return new PrefixExpression(position, op, base);
    }

    public Operator getOperator() {
        return mOperator;
    }

    public Expression getOperand() {
        return mOperand;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new PrefixExpression(position, mOperator, mOperand.clone());
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        boolean needsParens = (Operator.PRECEDENCE_PREFIX >= parentPrecedence);
        return (needsParens ? "(" : "") +
                mOperator.tightOperatorName() +
                mOperand.toString(Operator.PRECEDENCE_PREFIX) +
                (needsParens ? ")" : "");
    }
}
