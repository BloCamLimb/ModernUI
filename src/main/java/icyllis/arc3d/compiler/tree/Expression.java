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
import org.jspecify.annotations.NullMarked;

import java.util.OptionalDouble;

/**
 * Abstract superclass of all expressions.
 */
@NullMarked
public abstract class Expression extends Node {

    private final Type mType;

    protected Expression(int position, Type type) {
        super(position);
        mType = type;
    }

    /**
     * @see Node.ExpressionKind
     */
    public abstract ExpressionKind getKind();

    public final Type getType() {
        return mType;
    }

    public boolean isConstructorCall() {
        return false;
    }

    public boolean isLiteral() {
        return false;
    }

    public final boolean isIntLiteral() {
        return isLiteral() && getType().isInteger();
    }

    public final boolean isFloatLiteral() {
        return isLiteral() && getType().isFloat();
    }

    public final boolean isBooleanLiteral() {
        return isLiteral() && getType().isBoolean();
    }

    /**
     * @see Type.CoercionCost
     */
    public final long getCoercionCost(Type other) {
        if (isIntLiteral() && other.isNumeric()) {
            return Type.CoercionCost.free();
        }
        return getType().getCoercionCost(other);
    }

    /**
     * Returns true if this expression is incomplete. Specifically, dangling function/method-call
     * references that were never invoked, or type references that were never constructed, are
     * considered incomplete expressions and should result in an error.
     */
    public final boolean isIncomplete(Context context) {
        return switch (getKind()) {
            case FUNCTION_REFERENCE -> {
                int pos = getEndOffset();
                pos = Position.range(pos, pos + 1);
                context.error(pos, "expected '(' to begin function invocation");
                yield true;
            }
            case TYPE_REFERENCE -> {
                int pos = getEndOffset();
                pos = Position.range(pos, pos + 1);
                context.error(pos, "expected '(' to begin constructor invocation");
                yield true;
            }
            default -> false;
        };
    }

    /**
     * Returns the i'th compile-time constant value within a literal or constructor.
     * Indices which do not contain compile-time constant values will return empty.
     * `vec4(1, vec2(2), 3)` contains four compile-time constants: (1, 2, 2, 3)
     * `mat2(f)` contains four slots, and two are constant: (empty, 0, 0, empty)
     */
    public OptionalDouble getConstantValue(int i) {
        return OptionalDouble.empty();
    }

    /**
     * Returns a deep copy at the same position.
     */
    public final Expression copy() {
        return copy(mPosition);
    }

    public abstract Expression copy(int position);

    /**
     * Returns a description of the expression.
     */
    @Override
    public final String toString() {
        return toString(Operator.PRECEDENCE_EXPRESSION);
    }

    public abstract String toString(int parentPrecedence);
}
