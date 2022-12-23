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

import javax.annotation.Nonnull;

/**
 * A binary operation.
 */
public final class BinaryExpression extends Expression {

    private final Expression mLeft;
    private final Operator mOperator;
    private final Expression mRight;

    private BinaryExpression(int position, Expression left, Operator op,
                             Expression right, Type type) {
        super(position, ExpressionKind.kBinary, type);
        mLeft = left;
        mOperator = op;
        mRight = right;
        //TODO checkref
    }

    /**
     * Creates a potentially-simplified form of the expression. Determines the result type
     * programmatically.
     */
    public static Expression convert(int position,
                                     Expression left,
                                     Operator op,
                                     Expression right) {
        ThreadContext context = ThreadContext.getInstance();
        Type rawLeftType = (left.isIntLiteral() && right.getType().isInteger())
                ? right.getType()
                : left.getType();
        Type rawRightType = (right.isIntLiteral() && left.getType().isInteger())
                ? left.getType()
                : right.getType();

        boolean isAssignment = op.isAssignment();

        Type[] outTypes = new Type[3];
        if (!op.determineBinaryType(context, rawLeftType, rawRightType, outTypes)) {
            context.error(position, "type mismatch: '" + op.tightOperatorName() +
                    "' cannot operate on '" + left.getType().getName() + "', '" +
                    right.getType().getName() + "'");
            return null;
        }
        Type leftType = outTypes[0];
        Type rightType = outTypes[1];
        Type resultType = outTypes[2];

        if (isAssignment && leftType.getComponentType().isOpaque()) {
            context.error(position, "assignments to opaque type '" + left.getType().getName() +
                    "' are not permitted");
            return null;
        }

        left = leftType.coerceExpression(left);
        right = rightType.coerceExpression(right);

        return new BinaryExpression(position, left, op, right, resultType);
    }

    public Expression getLeft() {
        return mLeft;
    }

    public Operator getOperator() {
        return mOperator;
    }

    public Expression getRight() {
        return mRight;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new BinaryExpression(position,
                mLeft.clone(),
                mOperator,
                mRight.clone(),
                getType());
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        int operatorPrecedence = mOperator.getBinaryPrecedence();
        boolean needsParens = (operatorPrecedence >= parentPrecedence);
        return (needsParens ? "(" : "") +
                mLeft.toString(operatorPrecedence) +
                mOperator.operatorName() +
                mRight.toString(operatorPrecedence) +
                (needsParens ? ")" : "");
    }
}
