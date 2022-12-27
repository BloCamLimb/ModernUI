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
import java.util.OptionalDouble;

/**
 * Abstract superclass of all expressions.
 */
public abstract class Expression extends Node {

    private final Type mType;

    protected Expression(int position, int kind, Type type) {
        super(position, kind);
        assert (kind >= ExpressionKind.kFirst && kind <= ExpressionKind.kLast);
        mType = type;
    }

    /**
     * @see Node.ExpressionKind
     */
    public final int kind() {
        return mKind;
    }

    @Nonnull
    public Type getType() {
        assert (mType != null);
        return mType;
    }

    public boolean isAnyConstructor() {
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

    public final boolean isBoolLiteral() {
        return isLiteral() && getType().isBoolean();
    }

    /**
     * @see Type.CoercionCost
     */
    public int getCoercionCost(Type other) {
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
    public final boolean isIncomplete() {
        return switch (kind()) {
            case ExpressionKind.kFunctionReference -> {
                int pos = getEndOffset();
                pos = makeRange(pos, pos + 1);
                ThreadContext.getInstance().error(pos, "expected '(' to begin function call");
                yield true;
            }
            case ExpressionKind.kTypeReference -> {
                int pos = getEndOffset();
                pos = makeRange(pos, pos + 1);
                ThreadContext.getInstance().error(pos, "expected '(' to begin constructor invocation");
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
     * Returns a clone at the same position.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Nonnull
    @Override
    public final Expression clone() {
        return clone(mPosition);
    }

    @Nonnull
    public abstract Expression clone(int position);

    /**
     * Returns a description of the expression.
     */
    @Nonnull
    @Override
    public final String toString() {
        return toString(Operator.PRECEDENCE_TOP_LEVEL);
    }

    @Nonnull
    public abstract String toString(int parentPrecedence);
}
