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
import java.util.OptionalDouble;
import java.util.StringJoiner;

/**
 * Base class representing a constructor call: type_name( args, ... ).
 *
 * @see FunctionCall
 */
public abstract class ConstructorCall extends Expression {

    private final Expression[] mArguments;

    protected ConstructorCall(int position, Type type,
                              Expression[] arguments) {
        super(position, type);
        assert (arguments.length != 0);
        mArguments = arguments;
    }

    @Override
    public boolean accept(@Nonnull NodeVisitor visitor) {
        if (visitor.visitConstructorCall(this)) {
            return true;
        }
        for (Expression arg : mArguments) {
            if (arg.accept(visitor)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final boolean isConstructorCall() {
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
        StringJoiner joiner = new StringJoiner(", ");
        for (Expression arg : mArguments) {
            joiner.add(arg.toString(Operator.PRECEDENCE_SEQUENCE));
        }
        return getType().getName() + "(" + joiner + ")";
    }

    final Expression[] cloneArguments() {
        Expression[] result = mArguments.clone();
        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].clone();
        }
        return result;
    }
}
