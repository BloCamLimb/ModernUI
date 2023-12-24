/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.compiler.ConstantFolder;
import icyllis.arc3d.compiler.analysis.Analysis;

import javax.annotation.Nonnull;

/**
 * Represents the typecasting of an array.
 * <p>
 * These always contain exactly 1 array of matching size, and are never constant.
 */
public final class ConstructorArrayCast extends ConstructorCall {

    private ConstructorArrayCast(int position, Type type, Expression... arguments) {
        super(position, type, arguments);
        assert arguments.length == 1;
    }

    @Nonnull
    public static Expression make(int position, Type type, Expression arg) {
        // Only arrays of the same size are allowed.
        assert (type.isArray());
        assert (arg.getType().isArray());
        assert (type.getArraySize() == arg.getType().getArraySize());

        // If this is a no-op cast, return the expression as-is.
        if (type.matches(arg.getType())) {
            arg.mPosition = position;
            return arg;
        }

        // Look up the value of constant variables. This allows constant-expressions like `myArray` to
        // be replaced with the compile-time constant `int[2](0, 1)`.
        arg = ConstantFolder.makeConstantValueForVariable(position, arg);

        // We can cast a vector of compile-time constants at compile-time.
        if (Analysis.isCompileTimeConstant(arg)) {
            Type scalarType = type.getComponentType();

            // Create a ConstructorArray(...) which typecasts each argument inside.
            Expression[] inputArgs = ((ConstructorArray) arg).getArguments();
            Expression[] typecastArgs = new Expression[inputArgs.length];
            for (int i = 0; i < inputArgs.length; i++) {
                Expression inputArg = inputArgs[i];
                if (inputArg.getType().isScalar()) {
                    typecastArgs[i] = ConstructorScalarCast.make(inputArg.mPosition, scalarType, inputArg);
                } else {
                    typecastArgs[i] = ConstructorCompoundCast.make(inputArg.mPosition, scalarType, inputArg);
                }
            }

            return ConstructorArray.make(position, type, typecastArgs);
        }
        return new ConstructorArrayCast(position, type, arg);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.CONSTRUCTOR_ARRAY_CAST;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new ConstructorArrayCast(position, getType(), cloneArguments());
    }
}
