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

import icyllis.arc3d.compiler.ThreadContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    public static Expression convert(int position, Type type, Expression[] arguments) {
        assert type.isArray() && type.getArraySize() > 0 : type.toString();

        // If there is a single argument containing an array of matching size and the types are
        // coercible, this is actually a cast. i.e., `half[10](myFloat10Array)`. This isn't a GLSL
        // feature, but the Pipeline stage code generator needs this functionality so that code which
        // was originally compiled with "allow narrowing conversions" enabled can be later recompiled
        // without narrowing conversions (we patch over these conversions with an explicit cast).
        if (arguments.length == 1) {
            Expression arg = arguments[0];
            Type argType = arg.getType();

            if (argType.isArray() && argType.canCoerceTo(type, false)) {
                return ConstructorArrayCast.make(position, type, arg);
            }
        }

        // Check that the number of constructor arguments matches the array size.
        if (type.getArraySize() != arguments.length) {
            ThreadContext.getInstance().error(position, String.format("invalid arguments to '%s' constructor " +
                            "(expected %d elements, but found %d)", type.getName(), type.getArraySize(),
                    arguments.length));
            return null;
        }

        // Convert each constructor argument to the array's element type.
        Type baseType = type.getElementType();
        Expression[] immutableArgs = new Expression[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            immutableArgs[i] = baseType.coerceExpression(arguments[i]);
            if (immutableArgs[i] == null) {
                return null;
            }
        }

        return make(position, type, immutableArgs);
    }

    /**
     * Create array-constructor expressions.
     * <p>
     * No explicit check, assuming that the input array is immutable.
     */
    @Nonnull
    public static Expression make(int position, Type type, Expression[] arguments) {
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
