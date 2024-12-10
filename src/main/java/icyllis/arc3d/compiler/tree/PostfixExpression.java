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

import icyllis.arc3d.compiler.Operator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import icyllis.arc3d.compiler.Context;

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
    public static Expression convert(@NonNull Context context,
                                     int position, Expression base, Operator op) {
        Type baseType = base.getType();
        if (!baseType.isNumeric()) {
            context.error(position, "'" + op +
                    "' cannot operate on '" + baseType.getName() + "'");
            return null;
        }
        return make(position, base, op);
    }

    @NonNull
    public static Expression make(int position, Expression base, Operator op) {
        assert base.getType().isNumeric();
        return new PostfixExpression(position, base, op);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.POSTFIX;
    }

    @Override
    public boolean accept(@NonNull TreeVisitor visitor) {
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

    @NonNull
    @Override
    public Expression clone(int position) {
        return new PostfixExpression(position, mOperand.clone(), mOperator);
    }

    @NonNull
    @Override
    public String toString(int parentPrecedence) {
        boolean needsParens = (Operator.PRECEDENCE_POSTFIX >= parentPrecedence);
        return (needsParens ? "(" : "") +
                mOperand.toString(Operator.PRECEDENCE_POSTFIX) +
                (mOperator.toString()) +
                (needsParens ? ")" : "");
    }
}
