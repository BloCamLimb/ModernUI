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

import icyllis.arc3d.compiler.ConstantFolder;
import icyllis.arc3d.compiler.ThreadContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a vector or matrix that is composed of other expressions, such as
 * `float3(pos.xy, 1)` or `float3x3(a.xyz, b.xyz, 0, 0, 1)`
 * <p>
 * These can contain a mix of scalars and aggregates. The total number of scalar values inside the
 * constructor must always match the type's scalar count. (e.g. `pos.xy` consumes two scalars.)
 * The inner values must have the same component type as the vector/matrix.
 */
public final class ConstructorCompound extends ConstructorCall {

    private ConstructorCompound(int position, Type type, Expression[] arguments) {
        super(position, type, arguments);
    }

    @Nullable
    public static Expression convert(int pos,
                                     @Nonnull Type type,
                                     @Nonnull List<Expression> args) {
        assert (type.isVector() || type.isMatrix());

        // The meaning of a compound constructor containing a single argument varies significantly in
        // GLSL/SkSL, depending on the argument type.
        if (args.size() == 1) {
            Expression argument = args.get(0);
            if (type.isVector() && argument.getType().isVector() &&
                    argument.getType().getComponentType().matches(type.getComponentType()) &&
                    argument.getType().getComponents() > type.getComponents()) {
                // Casting a vector-type into a smaller matching vector-type is a slice in GLSL.
                // We don't allow those casts in SkSL; recommend a swizzle instead.
                // Only `.xy` and `.xyz` are valid recommendations here, because `.x` would imply a
                // scalar(vector) cast, and nothing has more slots than `.xyzw`.
                String swizzleHint = switch (type.getComponents()) {
                    case 2 -> "; use '.xy' instead";
                    case 3 -> "; use '.xyz' instead";
                    default -> "";
                };

                ThreadContext.getInstance().error(pos, "'" + argument.getType() +
                        "' is not a valid parameter to '" + type + "' constructor" +
                        swizzleHint);
                return null;
            }

            if (argument.getType().isScalar()) {
                // A constructor containing a single scalar is a splat (for vectors) or diagonal matrix
                // (for matrices). It's legal regardless of the scalar's type, so synthesize an explicit
                // conversion to the proper type. (This cast is a no-op if it's unnecessary; it can fail
                // if we're casting a literal that exceeds the limits of the type.)
                Expression typecast = ConstructorScalarCast.convert(
                        pos, type.getComponentType(), args);
                if (typecast == null) {
                    return null;
                }

                // Matrix-from-scalar creates a diagonal matrix; vector-from-scalar creates a splat.
                return type.isMatrix()
                        ? ConstructorScalar2Matrix.make(pos, type, typecast)
                        : ConstructorScalar2Vector.make(pos, type, typecast);
            } else if (argument.getType().isVector()) {
                // A vector constructor containing a single vector with the same number of columns is a
                // cast (e.g. float3 -> int3).
                if (type.isVector() && argument.getType().getRows() == type.getRows()) {
                    return ConstructorCompoundCast.make(pos, type, argument);
                }
            } else if (argument.getType().isMatrix()) {
                // A matrix constructor containing a single matrix can be a resize, typecast, or both.
                // GLSL lumps these into one category, but internally SkSL keeps them distinct.
                if (type.isMatrix()) {
                    // First, handle type conversion. If the component types differ, synthesize the
                    // destination type with the argument's rows/columns. (This will be a no-op if it's
                    // already the right type.)
                    Type typecastType = type.getComponentType().toCompound(
                            argument.getType().getCols(),
                            argument.getType().getRows());
                    argument = ConstructorCompoundCast.make(pos, typecastType,
                            argument);

                    // Casting a matrix type into another matrix type is a resize.
                    return ConstructorMatrix2Matrix.make(pos, type,
                            argument);
                }

                // A vector constructor containing a single matrix can be compound construction if the
                // matrix is 2x2 and the vector is 4-slot.
                if (type.isVector() && type.getRows() == 4 && argument.getType().getComponents() == 4) {
                    // Casting a 2x2 matrix to a vector is a form of compound construction.
                    // First, reshape the matrix into a 4-slot vector of the same type.
                    Type vectorType = argument.getType().getComponentType().toVector(/*rows*/4);
                    Expression vecCtor =
                            ConstructorCompound.make(pos, vectorType, args.toArray(new Expression[0]));

                    // Then, add a typecast to the result expression to ensure the types match.
                    // This will be a no-op if no typecasting is needed.
                    return ConstructorCompoundCast.make(pos, type, vecCtor);
                }
            }
        }

        // For more complex cases, we walk the argument list and fix up the arguments as needed.
        int expected = type.getRows() * type.getCols();
        int actual = 0;
        for (var it = args.listIterator(); it.hasNext(); ) {
            var arg = it.next();
            if (!arg.getType().isScalar() && !arg.getType().isVector()) {
                ThreadContext.getInstance().error(pos, "'" + arg.getType() +
                        "' is not a valid parameter to '" + type + "' constructor");
                return null;
            }

            // Rely on ConstructorCall.convert() to force this subexpression to the proper type. If it's a
            // literal, this will make sure it's the right type of literal. If an expression of matching
            // type, the expression will be returned as-is. If it's an expression of mismatched type,
            // this adds a cast.
            Type ctorType = type.getComponentType().toVector(arg.getType().getRows());
            List<Expression> ctorArg = new ArrayList<>(1);
            ctorArg.add(arg);
            arg = ConstructorCall.convert(pos, ctorType, ctorArg);
            if (arg == null) {
                return null;
            }
            it.set(arg);
            actual += ctorType.getRows();
        }

        if (actual != expected) {
            ThreadContext.getInstance().error(pos, "invalid arguments to '" + type +
                    "' constructor (expected " + expected +
                    " scalars, but found " + actual + ")");
            return null;
        }

        return ConstructorCompound.make(pos, type, args.toArray(new Expression[0]));
    }

    @Nonnull
    public static Expression make(int position, Type type, Expression[] arguments) {
        int n = 0;
        // All the arguments must have matching component type.
        for (Expression arg : arguments) {
            Type argType = arg.getType();
            assert (argType.isScalar() || argType.isVector() || argType.isMatrix()) &&
                    (argType.getComponentType().matches(type.getComponentType()));
            n += argType.getComponents();
        }
        // The scalar count of the combined argument list must match the composite type's scalar count.
        assert type.getComponents() == n;

        // No-op compound constructors (containing a single argument of the same type) are eliminated.
        // (Even though this is a "compound constructor," we let scalars pass through here; it's
        // harmless to allow and simplifies call sites which need to narrow a vector and may sometimes
        // end up with a scalar.)
        if (arguments.length == 1) {
            Expression arg = arguments[0];
            if (type.isScalar()) {
                // A scalar "compound type" with a single scalar argument is a no-op and can be eliminated.
                // (Pedantically, this isn't a compound at all, but it's harmless to allow and simplifies
                // call sites which need to narrow a vector and may sometimes end up with a scalar.)
                assert (arg.getType().matches(type));
                arg.mPosition = position;
                return arg;
            }
            if (type.isVector() && arg.getType().matches(type)) {
                // A vector compound constructor containing a single argument of matching type can trivially
                // be eliminated.
                arg.mPosition = position;
                return arg;
            }
            // This is a meaningful single-argument compound constructor (e.g. vector-from-matrix,
            // matrix-from-vector).
        }
        // Beyond this point, the type must be a vector or matrix.
        assert (type.isVector() || type.isMatrix());

        // Replace constant variables with their corresponding values, so `float2(one, two)` can
        // compile down to `float2(1.0, 2.0)` (the latter is a compile-time constant).
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = ConstantFolder.makeConstantValueForVariable(position, arguments[i]);
        }

        return new ConstructorCompound(position, type, arguments);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.CONSTRUCTOR_COMPOUND;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new ConstructorCompound(position, getType(), cloneArguments());
    }
}
