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
 * Abstract superclass of all expressions.
 */
public abstract class Expression extends Node {

    private final Type mType;

    protected Expression(int position, int kind, Type type) {
        super(position, kind);
        assert (kind >= ExpressionKind.kFirst && kind <= ExpressionKind.kLast);
        mType = type;
    }

    public final int kind() {
        return mKind;
    }

    @Nonnull
    public Type getType() {
        assert (mType != null);
        return mType;
    }

    public final boolean isAnyConstructor() {
        return kind() >= ExpressionKind.kConstructorArray && kind() <= ExpressionKind.kConstructorStruct;
    }

    public final boolean isIntLiteral() {
        return kind() == ExpressionKind.kLiteral && getType().isInteger();
    }

    public final boolean isFloatLiteral() {
        return kind() == ExpressionKind.kLiteral && getType().isFloat();
    }

    public final boolean isBooleanLiteral() {
        return kind() == ExpressionKind.kLiteral && getType().isBoolean();
    }

    /**
     * @see Type.CoercionCost
     */
    public int coercionCost(Type target) {
        return getType().coercionCost(target);
    }

    /**
     * Returns true if this expression is incomplete. Specifically, dangling function/method-call
     * references that were never invoked, or type references that were never constructed, are
     * considered incomplete expressions and should result in an error.
     */
    public boolean isIncomplete() {
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

    @Nonnull
    @Override
    public final String toString() {
        return toString(Operator.PRECEDENCE_TOP_LEVEL);
    }

    @Nonnull
    public abstract String toString(int parentPrecedence);
}
