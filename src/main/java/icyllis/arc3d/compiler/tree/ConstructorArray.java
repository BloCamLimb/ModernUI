/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.compiler.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents the construction of an array type, such as "float[5](x, y, z, w, 1)".
 */
public final class ConstructorArray extends ConstructorCall {

    private ConstructorArray(int position, Type type, Expression[] arguments) {
        super(position, type, arguments);
    }

    /**
     * Create array-constructor expressions.
     * <p>
     * Perform explicit check and report errors via ErrorHandler; returns null on error.
     */
    @Nullable
    public static Expression convert(@Nonnull Context context,
                                     int position, @Nonnull Type type, @Nonnull List<Expression> arguments) {
        assert type.isArray();

        // If there is a single argument containing an array of matching size and the types are
        // coercible, this is actually a cast. i.e., `half[10](myFloat10Array)`. This isn't a GLSL
        // feature, but the Pipeline stage code generator needs this functionality so that code which
        // was originally compiled with "allow narrowing conversions" enabled can be later recompiled
        // without narrowing conversions (we patch over these conversions with an explicit cast).
        if (arguments.size() == 1) {
            Expression arg = arguments.get(0);
            Type argType = arg.getType();

            if (argType.isArray() && argType.canCoerceTo(type, false)) {
                return ConstructorArrayCast.make(context, position, type, arg);
            }
        }

        if (type.isUnsizedArray()) {
            // implicitly sized array
            if (arguments.isEmpty()) {
                context.error(position, "implicitly sized array constructor must have at least one argument");
                return null;
            }
            type = context.getSymbolTable().getArrayType(
                    type.getElementType(), arguments.size());
        } else {
            // Check that the number of constructor arguments matches the array size.
            if (type.getArraySize() != arguments.size()) {
                context.error(position, String.format("invalid arguments to '%s' constructor " +
                                "(expected %d elements, but found %d)", type.getName(), type.getArraySize(),
                        arguments.size()));
                return null;
            }
        }

        // Convert each constructor argument to the array's element type.
        Type baseType = type.getElementType();
        Expression[] immutableArgs = new Expression[arguments.size()];
        for (int i = 0; i < arguments.size(); i++) {
            immutableArgs[i] = baseType.coerceExpression(context, arguments.get(i));
            if (immutableArgs[i] == null) {
                return null;
            }
        }

        return ConstructorArray.make(position, type, immutableArgs);
    }

    /**
     * Create array-constructor expressions.
     * <p>
     * No explicit check, assuming that the input array is immutable.
     */
    @Nonnull
    public static Expression make(int position, @Nonnull Type type, @Nonnull Expression[] arguments) {
        assert type.getArraySize() == arguments.length;
        for (Expression arg : arguments) {
            assert type.getElementType().matches(arg.getType());
        }
        return new ConstructorArray(position, type, arguments);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.CONSTRUCTOR_ARRAY;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new ConstructorArray(position, getType(), cloneArguments());
    }
}
