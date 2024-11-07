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

import icyllis.arc3d.compiler.ConstantFolder;

import javax.annotation.Nonnull;

/**
 * Represents the construction of a vector splat (broadcast), such as `float3(n)`.
 * <p>
 * These always contain exactly 1 scalar.
 */
public final class ConstructorVectorSplat extends ConstructorCall {

    private ConstructorVectorSplat(int position, Type type, Expression... arguments) {
        super(position, type, arguments);
        assert arguments.length == 1;
    }

    // The input argument must be scalar. A "splat" to a scalar type will be optimized into a no-op.
    @Nonnull
    public static Expression make(int position, @Nonnull Type type, @Nonnull Expression arg) {
        assert (type.isScalar() || type.isVector());
        assert (arg.getType().matches(type.getComponentType()));
        assert (arg.getType().isScalar());

        // A "splat" to a scalar type is a no-op and can be eliminated.
        if (type.isScalar()) {
            arg.mPosition = position;
            return arg;
        }

        // Replace constant variables with their corresponding values, so `float3(five)` can compile
        // down to `float3(5.0)` (the latter is a compile-time constant).
        arg = ConstantFolder.makeConstantValueForVariable(position, arg);

        assert (type.isVector());
        return new ConstructorVectorSplat(position, type, arg);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.CONSTRUCTOR_VECTOR_SPLAT;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new ConstructorVectorSplat(position, getType(), cloneArguments());
    }
}
