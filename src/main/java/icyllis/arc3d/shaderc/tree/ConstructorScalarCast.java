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

import icyllis.arc3d.shaderc.ConstantFolder;
import icyllis.arc3d.shaderc.ThreadContext;

import javax.annotation.Nonnull;

/**
 * Represents the construction of a scalar cast, such as `float(intVariable)`.
 * <p>
 * These always contain exactly 1 scalar of a differing type, and are never constant.
 */
public final class ConstructorScalarCast extends ConstructorCall {

    private ConstructorScalarCast(int position, Type type, Expression... arguments) {
        super(position, type, arguments);
        assert arguments.length == 1;
    }

    // Casts a scalar expression. Casts that can be evaluated at compile-time will do so
    // (e.g. `int(4.1)` --> `Literal(int 4)`).
    public static Expression make(int position, Type type, Expression arg) {
        assert type.isScalar();
        assert arg.getType().isScalar();

        // No cast required when the types match.
        if (arg.getType().matches(type)) {
            return arg;
        }
        // Look up the value of constant variables. This allows constant-expressions like `int(zero)` to
        // be replaced with a literal zero.
        arg = ConstantFolder.makeConstantValueForVariable(position, arg);

        // We can cast scalar literals at compile-time when possible. (If the resulting literal would be
        // out of range for its type, we report an error and return zero to minimize error cascading.
        // This can occur when code is inlined, so we can't necessarily catch it during Convert. As
        // such, it's not safe to return null or assert.)
        if (arg instanceof Literal literal) {
            double value = literal.getValue();
            if (type.isNumeric() &&
                    (value < type.getMinValue() || value > type.getMaxValue())) {
                ThreadContext.getInstance().error(position,
                        String.format("value is out of range for type '%s': %.0f",
                                type.getName(), value));
                value = 0.0;
            }
            return Literal.make(position, value, type);
        }
        return new ConstructorScalarCast(position, type, arg);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.CONSTRUCTOR_SCALAR_CAST;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new ConstructorScalarCast(position, getType(), cloneArguments());
    }
}
