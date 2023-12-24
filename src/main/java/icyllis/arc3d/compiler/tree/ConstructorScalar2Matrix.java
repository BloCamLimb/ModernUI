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

import javax.annotation.Nonnull;
import java.util.OptionalDouble;

/**
 * Represents the construction of a diagonal matrix, such as {@literal `mat4x4(n)`}.
 * <p>
 * These always contain exactly 1 scalar.
 */
public final class ConstructorScalar2Matrix extends ConstructorCall {

    private ConstructorScalar2Matrix(int position, Type type, Expression... arguments) {
        super(position, type, arguments);
        assert arguments.length == 1;
    }

    @Nonnull
    public static Expression make(int position, Type type, Expression arg) {
        assert (type.isMatrix());
        assert (arg.getType().isScalar());
        assert (arg.getType().matches(type.getComponentType()));

        // Look up the value of constant variables. This allows constant-expressions like `mat4(five)`
        // to be replaced with `mat4(5.0)`.
        arg = ConstantFolder.makeConstantValueForVariable(position, arg);

        return new ConstructorScalar2Matrix(position, type, arg);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.CONSTRUCTOR_MATRIX_SCALAR;
    }

    @Override
    public OptionalDouble getConstantValue(int i) {
        int rows = getType().getRows();
        int row = i % rows;
        int col = i / rows;

        assert (col >= 0);
        assert (row >= 0);
        assert (col < getType().getCols());
        assert (row < getType().getRows());

        return (col == row) ? getArguments()[0].getConstantValue(0) : OptionalDouble.of(0.0);
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new ConstructorScalar2Matrix(position, getType(), cloneArguments());
    }
}
