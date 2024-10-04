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

import javax.annotation.Nonnull;
import java.util.OptionalDouble;

/**
 * Represents the construction of a matrix resize operation, such as `mat4x4(myMat2x2)`.
 * <p>
 * These always contain exactly 1 matrix of non-matching size. Cells that aren't present in the
 * input matrix are populated with the identity matrix.
 */
public final class ConstructorMatrixResize extends ConstructorCall {

    private ConstructorMatrixResize(int position, Type type, Expression... arguments) {
        super(position, type, arguments);
        assert arguments.length == 1;
    }

    @Nonnull
    public static Expression make(int position, @Nonnull Type type, @Nonnull Expression arg) {
        assert (type.isMatrix());
        assert (arg.getType().getComponentType().matches(type.getComponentType()));

        // If the matrix isn't actually changing size, return it as-is.
        if (type.getRows() == arg.getType().getRows() && type.getCols() == arg.getType().getCols()) {
            return arg;
        }

        return new ConstructorMatrixResize(position, type, arg);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.CONSTRUCTOR_MATRIX_RESIZE;
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

        // GLSL resize matrices are of the form:
        //  |m m 0|
        //  |m m 0|
        //  |0 0 1|
        // Where `m` is the matrix being wrapped, and other cells contain the identity matrix.

        // Forward `getConstantValue` to the wrapped matrix if the position is in its bounds.
        Type argType = getArgument().getType();
        if (col < argType.getCols() && row < argType.getRows()) {
            // Recalculate `i` in terms of the inner matrix's dimensions.
            i = row + (col * argType.getRows());
            return getArgument().getConstantValue(i);
        }

        // Synthesize an identity matrix for out-of-bounds positions.
        return OptionalDouble.of((col == row) ? 1.0 : 0.0);
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new ConstructorMatrixResize(position, getType(), cloneArguments());
    }
}
