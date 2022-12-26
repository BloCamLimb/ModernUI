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
import java.util.OptionalDouble;
import java.util.StringJoiner;

/**
 * Base class representing a constructor.
 */
public abstract class AnyConstructor extends Expression {

    private final Expression[] mArguments;

    protected AnyConstructor(int position, int kind, Type type,
                             Expression[] arguments) {
        super(position, kind, type);
        assert (kind >= ExpressionKind.kConstructorArray && kind <= ExpressionKind.kConstructorVectorScalar);
        mArguments = arguments;
    }

    @Override
    public final boolean isAnyConstructor() {
        return true;
    }

    public final Type getComponentType() {
        return getType().getComponentType();
    }

    // **immutable**
    public final Expression[] getArguments() {
        return mArguments;
    }

    @Override
    public OptionalDouble getConstantValue(int i) {
        assert (i >= 0 && i < getType().getComponents());
        for (Expression arg : mArguments) {
            int components = arg.getType().getComponents();
            if (i < components) {
                return arg.getConstantValue(i);
            }
            i -= components;
        }
        throw new AssertionError(i);
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        String result = getType() + "(";
        StringJoiner joiner = new StringJoiner(", ");
        for (Expression arg : mArguments) {
            joiner.add(arg.toString(Operator.PRECEDENCE_SEQUENCE));
        }
        return result + joiner + ")";
    }

    final Expression[] cloneArguments() {
        Expression[] result = mArguments.clone();
        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].clone();
        }
        return result;
    }
}
