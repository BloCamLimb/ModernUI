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
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Base class representing a constructor call: type_name( args, ... ).
 *
 * @see FunctionCall
 */
public abstract class ConstructorCall extends Expression {

    @Unmodifiable
    private final Expression[] mArguments;

    protected ConstructorCall(int position, Type type,
                              Expression[] arguments) {
        super(position, type);
        assert (arguments.length != 0);
        mArguments = arguments;
    }

    @Nullable
    public static Expression convert(@NonNull Context context,
                                     int pos, @NonNull Type type, @NonNull List<Expression> args) {
        if (args.size() == 1 &&
                args.get(0).getType().matches(type) &&
                !type.getElementType().isOpaque()) {
            // Don't generate redundant casts; if the expression is already of the correct type, just
            // return it as-is.
            Expression expr = args.get(0);
            expr.mPosition = pos;
            return expr;
        }
        if (type.isScalar()) {
            return ConstructorScalarCast.convert(context, pos, type, args);
        }
        if (type.isVector() || type.isMatrix()) {
            return ConstructorCompound.convert(context, pos, type, args);
        }
        if (type.isArray()) {
            return ConstructorArray.convert(context, pos, type, args);
        }
        if (type.isStruct() && !type.getFields().isEmpty()) {
            return ConstructorStruct.convert(context, pos, type, args);
        }
        context.error(pos, "cannot construct '" + type + "'");
        return null;
    }

    @Override
    public boolean accept(@NonNull TreeVisitor visitor) {
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

    public final Expression getArgument() {
        assert mArguments.length == 1;
        return mArguments[0];
    }

    @Unmodifiable
    public final Expression[] getArguments() {
        return mArguments;
    }

    @Override
    public @NonNull OptionalDouble getConstantValue(int i) {
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

    @NonNull
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
