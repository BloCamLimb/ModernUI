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
import icyllis.arc3d.shaderc.analysis.NodeVisitor;

import javax.annotation.Nonnull;
import java.util.StringJoiner;

/**
 * A function invocation: function_name( args, ... ).
 *
 * @see ConstructorCall
 */
public final class FunctionCall extends Expression {

    private final Function mFunction;
    private final Expression[] mArguments;

    private FunctionCall(int position, Type type, Function function,
                         Expression... arguments) {
        super(position, type);
        mFunction = function;
        mArguments = arguments;
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.FUNCTION_CALL;
    }

    @Override
    public boolean accept(@Nonnull NodeVisitor visitor) {
        if (visitor.visitFunctionCall(this)) {
            return true;
        }
        for (Expression arg : mArguments) {
            if (arg.accept(visitor)) {
                return true;
            }
        }
        return false;
    }

    public Function getFunction() {
        return mFunction;
    }

    public Expression[] getArguments() {
        return mArguments;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        Expression[] arguments = mArguments.clone();
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = arguments[i].clone();
        }
        return new FunctionCall(position, getType(), mFunction, arguments);
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Expression arg : mArguments) {
            joiner.add(arg.toString(Operator.PRECEDENCE_SEQUENCE));
        }
        return mFunction.getName() + "(" + joiner + ")";
    }
}
