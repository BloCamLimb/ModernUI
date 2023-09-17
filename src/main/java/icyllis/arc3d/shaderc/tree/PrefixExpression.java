/*
 * Arc 3D.
 * Copyright (C) 2022-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.shaderc.tree;

import icyllis.arc3d.shaderc.Operator;
import icyllis.arc3d.shaderc.ThreadContext;
import icyllis.arc3d.shaderc.analysis.Analysis;
import icyllis.arc3d.shaderc.analysis.NodeVisitor;

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
        super(position, operand.getType());
        mOperator = op;
        mOperand = operand;
    }

    public static Expression convert(int position, Operator op, Expression base) {
        ThreadContext context = ThreadContext.getInstance();
        Type baseType = base.getType();
        switch (op) {
            case ADD:
            case SUB:
                if (baseType.isArray() || !baseType.getComponentType().isNumeric()) {
                    context.error(position,
                            "'" + op +
                                    "' cannot operate on '" + baseType.getName() + "'");
                    return null;
                }
                break;

            case INC:
            case DEC:
                if (!baseType.isNumeric()) {
                    context.error(position,
                            "'" + op +
                                    "' cannot operate on '" + baseType.getName() + "'");
                    return null;
                }
                if (!Analysis.updateVariableRefKind(base, VariableReference.kReadWrite_ReferenceKind)) {
                    return null;
                }
                break;

            case LOGICAL_NOT:
                if (!baseType.isBoolean()) {
                    context.error(position,
                            "'" + op +
                                    "' cannot operate on '" + baseType.getName() + "'");
                    return null;
                }
                break;

            case BITWISE_NOT:
                if (baseType.isArray() || !baseType.getComponentType().isInteger()) {
                    context.error(position,
                            "'" + op +
                                    "' cannot operate on '" + baseType.getName() + "'");
                    return null;
                }
                if (base.isLiteral()) {
                    // The expression `~123` is no longer a literal; coerce to the actual type.
                    base = baseType.coerceExpression(base);
                    if (base == null) {
                        return null;
                    }
                }
                break;

            default:
                throw new AssertionError(op);
        }

        Expression result = make(position, op, base);
        assert (result.mPosition == position);
        return result;
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
                throw new AssertionError(op);
        }

        return new PrefixExpression(position, op, base);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.PREFIX;
    }

    @Override
    public boolean accept(@Nonnull NodeVisitor visitor) {
        if (visitor.visitPrefix(this)) {
            return true;
        }
        return mOperand.accept(visitor);
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
                mOperator.toString() +
                mOperand.toString(Operator.PRECEDENCE_PREFIX) +
                (needsParens ? ")" : "");
    }
}
