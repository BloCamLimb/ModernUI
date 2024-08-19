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
 * Represents the construction of a vector/matrix typecast, such as `half3(myInt3)` or
 * `float4x4(myHalf4x4)`. Matrix resizes are done in ConstructorMatrixMatrix, not here.
 * <p>
 * These always contain exactly 1 vector or matrix of matching size, and are never constant.
 */
public final class ConstructorCompoundCast extends ConstructorCall {

    private ConstructorCompoundCast(int position, Type type, Expression... arguments) {
        super(position, type, arguments);
        assert arguments.length == 1;
    }

    @Nonnull
    public static Expression make(int position, @Nonnull Type type, @Nonnull Expression arg) {
        // Only vectors or matrices of the same dimensions are allowed.
        assert (type.isVector() || type.isMatrix());
        assert (arg.getType().isVector() == type.isVector());
        assert (arg.getType().isMatrix() == type.isMatrix());
        assert (type.getCols() == arg.getType().getCols());
        assert (type.getRows() == arg.getType().getRows());

        // If this is a no-op cast, return the expression as-is.
        if (type.matches(arg.getType())) {
            return arg;
        }
        // Look up the value of constant variables. This allows constant-expressions like
        // `int4(colorGreen)` to be replaced with the compile-time constant `int4(0, 1, 0, 1)`.
        arg = ConstantFolder.makeConstantValueForVariable(position, arg);

        //TODO optimize for constexpr
        return new ConstructorCompoundCast(position, type, arg);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.CONSTRUCTOR_COMPOUND_CAST;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new ConstructorCompoundCast(position, getType(), cloneArguments());
    }
}
