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

import icyllis.akashigi.aksl.ThreadContext;
import icyllis.akashigi.aksl.Operator;

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
        super(position, KIND_BINARY, type);
        mLeft = left;
        mOperator = op;
        mRight = right;
        //TODO checkref
    }

    /**
     * Creates a potentially-simplified form of the expression. Determines the result type
     * programmatically.
     */
    public static Expression convert(ThreadContext context,
                                     int position,
                                     Expression left,
                                     Operator op,
                                     Expression right) {
        Type rawLeftType = (left.isIntLiteral() && right.type().isInteger())
                ? right.type()
                : left.type();
        Type rawRightType = (right.isIntLiteral() && left.type().isInteger())
                ? left.type()
                : right.type();

        boolean isAssignment = op.isAssignment();

        Type[] outTypes = new Type[3];
        if (!op.determineBinaryType(context, rawLeftType, rawRightType, outTypes)) {
            context.error(position, "type mismatch: '" + op.tightOperatorName() +
                    "' cannot operate on '" + left.type().displayName() + "', '" +
                    right.type().displayName() + "'");
            return null;
        }
        Type leftType = outTypes[0];
        Type rightType = outTypes[1];
        Type resultType = outTypes[2];

        if (isAssignment && leftType.componentType().isOpaque()) {
            context.error(position, "assignments to opaque type '" + left.type().displayName() +
                    "' are not permitted");
            return null;
        }

        return null; // TODO
    }

    public Expression getLeft() {
        return mLeft;
    }

    public Expression getRight() {
        return mRight;
    }

    public Operator getOperator() {
        return mOperator;
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        int operatorPrecedence = getOperator().getBinaryPrecedence();
        boolean needsParens = (operatorPrecedence >= parentPrecedence);
        return (needsParens ? "(" : "") +
                getLeft().toString(operatorPrecedence) +
                getOperator().operatorName() +
                getRight().toString(operatorPrecedence) +
                (needsParens ? ")" : "");
    }
}
