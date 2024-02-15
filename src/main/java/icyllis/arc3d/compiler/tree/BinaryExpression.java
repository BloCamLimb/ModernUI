/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.compiler.*;
import icyllis.arc3d.compiler.analysis.TreeVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A binary operation.
 */
public final class BinaryExpression extends Expression {

    private Expression mLeft;
    private final Operator mOperator;
    private Expression mRight;

    private BinaryExpression(int position, Expression left, Operator op,
                             Expression right, Type type) {
        super(position, type);
        mLeft = left;
        mOperator = op;
        mRight = right;
        //TODO checkref
    }

    /**
     * Creates a potentially-simplified form of the expression. Determines the result type
     * programmatically.
     */
    @Nullable
    public static Expression convert(@Nonnull Context context,
                                     int position,
                                     Expression left,
                                     Operator op,
                                     Expression right) {
        Type rawLeftType = (left.isIntLiteral() && right.getType().isInteger())
                ? right.getType()
                : left.getType();
        Type rawRightType = (right.isIntLiteral() && left.getType().isInteger())
                ? left.getType()
                : right.getType();

        boolean isAssignment = op.isAssignment();

        Type[] types = new Type[3];
        if (!op.determineBinaryType(context, rawLeftType, rawRightType, types)) {
            context.error(position, "type mismatch: '" + op +
                    "' cannot operate on '" + left.getType().getName() + "', '" +
                    right.getType().getName() + "'");
            return null;
        }
        Type leftType = types[0];
        Type rightType = types[1];
        Type resultType = types[2];

        if (isAssignment && leftType.getComponentType().isOpaque()) {
            context.error(position, "assignments to opaque type '" + left.getType().getName() +
                    "' are not permitted");
            return null;
        }

        left = leftType.coerceExpression(context, left);
        if (left == null) {
            return null;
        }
        right = rightType.coerceExpression(context, right);
        if (right == null) {
            return null;
        }

        return BinaryExpression.make(context, position, left, op, right, resultType);
    }

    @Nonnull
    public static Expression make(@Nonnull Context context,
                                  int pos,
                                  Expression left,
                                  Operator op,
                                  Expression right,
                                  Type resultType) {
        // Perform constant-folding on the expression.
        Expression folded = ConstantFolder.fold(context, pos,
                left, op, right, resultType);
        if (folded != null) {
            return folded;
        }

        return new BinaryExpression(pos, left, op, right, resultType);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.BINARY;
    }

    @Override
    public boolean accept(@Nonnull TreeVisitor visitor) {
        if (visitor.visitBinary(this)) {
            return true;
        }
        return (mLeft != null && mLeft.accept(visitor)) ||
                (mRight != null && mRight.accept(visitor));
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

    public void setLeft(Expression left) {
        mLeft = left;
    }

    public void setRight(Expression right) {
        mRight = right;
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
                mOperator.getPrettyName() +
                mRight.toString(operatorPrecedence) +
                (needsParens ? ")" : "");
    }
}
