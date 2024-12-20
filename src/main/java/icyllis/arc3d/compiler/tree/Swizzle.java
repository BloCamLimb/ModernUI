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

import icyllis.arc3d.compiler.*;
import icyllis.arc3d.compiler.analysis.Analysis;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Represents a vector component selection (shuffling) such as 'float3(1, 2, 3).zyx'.
 */
public final class Swizzle extends Expression {

    /**
     * SwizzleComponents.
     */
    public static final byte
            X =  0,  Y =  1,  Z =  2,  W =  3, // X, Y, Z and W must be 0, 1, 2 and 3
            R =  4,  G =  5,  B =  6,  A =  7,
            S =  8,  T =  9,  P = 10,  Q = 11,
            ZERO = 16,  // custom swizzle
            ONE  = 17;  // custom swizzle

    private final Expression mBase;
    private final byte[] mComponents; // contains only X, Y, Z and W

    private Swizzle(int position, Type type, Expression base, byte[] components) {
        super(position, type);
        assert components.length >= 1 && components.length <= 4;
        mBase = base;
        mComponents = components;
    }

    private static boolean validateNameSet(byte[] components) {
        int set = -1;
        for (byte component : components) {
            int newSet;
            switch (component) {
                case X,Y,Z,W:
                    newSet = 0;
                    break;
                case R,G,B,A:
                    newSet = 1;
                    break;
                case S,T,P,Q:
                    newSet = 2;
                    break;
                case ZERO, ONE:
                    continue;
                default:
                    return false;
            }
            if (set == -1) {
                set = newSet;
            } else if (set != newSet) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static Expression optimizeSwizzle(@NonNull Context context,
                                              int pos,
                                              @NonNull ConstructorCompound base,
                                              byte[] components,
                                              int numComponents) {
        Expression[] baseArguments = base.getArguments();

        Type exprType = base.getType();
        Type componentType = exprType.getComponentType();

        // Swizzles can duplicate some elements and discard others, e.g.
        // `half4(1, 2, 3, 4).xxz` --> `half3(1, 1, 3)`. However, there are constraints:
        // - Expressions with side effects need to occur exactly once, even if they would otherwise be
        //   swizzle-eliminated
        // - Non-trivial expressions should not be repeated, but elimination is OK.
        //
        // Look up the argument for the constructor at each index. This is typically simple but for
        // weird cases like `half4(bar.yz, half2(foo))`, it can be harder than it seems. This example
        // would result in:
        //     argMap[0] = {.fArgIndex = 0, .fComponent = 0}   (bar.yz     .x)
        //     argMap[1] = {.fArgIndex = 0, .fComponent = 1}   (bar.yz     .y)
        //     argMap[2] = {.fArgIndex = 1, .fComponent = 0}   (half2(foo) .x)
        //     argMap[3] = {.fArgIndex = 1, .fComponent = 1}   (half2(foo) .y)

        int numConstructorArgs = exprType.getRows();

        // 0-8 arg_index
        // 8-16 component_index
        short[] argMap = new short[4];
        int writeIdx = 0;
        for (int argIdx = 0; argIdx < baseArguments.length; ++argIdx) {
            Expression arg = baseArguments[argIdx];
            Type argType = arg.getType();

            if (!argType.isScalar() && !argType.isVector()) {
                return null;
            }

            int argComps = argType.getComponents();
            for (int componentIdx = 0; componentIdx < argComps; ++componentIdx) {
                argMap[writeIdx] = (short) (argIdx | (componentIdx << 8));

                ++writeIdx;
            }
        }
        assert (writeIdx == numConstructorArgs);

        // Count up the number of times each constructor argument is used by the swizzle.
        //    `half4(bar.yz, half2(foo)).xwxy` -> { 3, 1 }
        // - bar.yz    is referenced 3 times, by `.x_xy`
        // - half(foo) is referenced 1 time,  by `._w__`
        byte[] exprUsed = new byte[4];
        for (byte c : components) {
            exprUsed[(byte) argMap[c]]++;
        }

        for (int index = 0; index < numConstructorArgs; ++index) {
            byte constructorArgIndex = (byte) argMap[index];
            Expression baseArg = baseArguments[constructorArgIndex];

            // Check that non-trivial expressions are not swizzled in more than once.
            if (exprUsed[constructorArgIndex] > 1 && !Analysis.isTrivialExpression(baseArg)) {
                return null;
            }
            // Check that side-effect-bearing expressions are swizzled in exactly once.
            if (exprUsed[constructorArgIndex] != 1 && Analysis.hasSideEffects(baseArg)) {
                return null;
            }
        }

        class ReorderedArgument {
            final byte mArgIndex;
            final byte[] mComponents = new byte[4];
            byte mNumComponents = 0;

            ReorderedArgument(byte argIndex) {
                mArgIndex = argIndex;
            }
        }
        List<ReorderedArgument> reorderedArgs = new ArrayList<>(4);
        for (byte c : components) {
            short argument = argMap[c];
            byte argumentIndex = (byte) argument;
            byte argumentComponent = (byte) (argument >> 8);
            Expression baseArg = baseArguments[argumentIndex];

            if (baseArg.getType().isScalar()) {
                // This argument is a scalar; add it to the list as-is.
                assert (argumentComponent == 0);
                reorderedArgs.add(new ReorderedArgument(argumentIndex));
            } else {
                // This argument is a component from a vector.
                assert (baseArg.getType().isVector());
                assert (argumentComponent < baseArg.getType().getRows());
                if (reorderedArgs.isEmpty() ||
                        reorderedArgs.get(reorderedArgs.size() - 1).mArgIndex != argumentIndex) {
                    // This can't be combined with the previous argument. Add a new one.
                    ReorderedArgument toAdd = new ReorderedArgument(argumentIndex);
                    toAdd.mComponents[toAdd.mNumComponents++] = argumentComponent;
                    reorderedArgs.add(toAdd);
                } else {
                    // Since we know this argument uses components, it should already have at least one
                    // component set.
                    ReorderedArgument last = reorderedArgs.get(reorderedArgs.size() - 1);
                    assert (last.mNumComponents != 0);
                    // Build up the current argument with one more component.
                    last.mComponents[last.mNumComponents++] = argumentComponent;
                }
            }
        }

        // Convert our reordered argument list to an actual array of expressions, with the new order and
        // any new inner swizzles that need to be applied.
        Expression[] newArgs = new Expression[numComponents];
        for (int i = 0; i < reorderedArgs.size(); i++) {
            ReorderedArgument reorderedArg = reorderedArgs.get(i);
            Expression newArg = baseArguments[reorderedArg.mArgIndex].copy();

            if (reorderedArg.mNumComponents == 0) {
                newArgs[i] = newArg;
            } else {
                newArgs[i] = Swizzle.make(
                        context,
                        pos,
                        newArg,
                        reorderedArg.mComponents,
                        reorderedArg.mNumComponents
                );
            }
        }

        // Wrap the new argument list in a compound constructor.
        return ConstructorCompound.make(
                context,
                pos,
                componentType.toVector(context, numComponents),
                newArgs);
    }

    /**
     * Create swizzle expressions. This method permits components containing ZERO or ONE,
     * does typechecking, reports errors via ErrorHandler, and returns an expression that
     * combines constructors and native swizzles (comprised solely of X/Y/W/Z).
     */
    @Nullable
    public static Expression convert(@NonNull Context context,
                                     int position, @NonNull Expression base,
                                     int maskPosition, @NonNull String maskString) {
        if (maskString.length() > 4) {
            context.error(maskPosition,
                    "too many components in swizzle mask");
            return null;
        }

        byte[] inComponents = new byte[maskString.length()];
        for (int i = 0; i < maskString.length(); ++i) {
            char field = maskString.charAt(i);
            byte c;
            switch (field) {
                case 'x' -> c = X;
                case 'r' -> c = R;
                case 's' -> c = S;
                case 'y' -> c = Y;
                case 'g' -> c = G;
                case 't' -> c = T;
                case 'z' -> c = Z;
                case 'b' -> c = B;
                case 'p' -> c = P;
                case 'w' -> c = W;
                case 'a' -> c = A;
                case 'q' -> c = Q;
                case '0' -> c = ZERO;
                case '1' -> c = ONE;
                default -> {
                    int offset = Position.getStartOffset(maskPosition) + i;
                    context.error(Position.range(offset, offset + 1),
                            String.format("invalid swizzle component '%c'", field));
                    return null;
                }
            }
            inComponents[i] = c;
        }

        if (!validateNameSet(inComponents)) {
            context.error(maskPosition, "swizzle components '" + maskString +
                    "' do not come from the same name set");
            return null;
        }

        Type baseType = base.getType();
        if (!baseType.isVector() && !baseType.isScalar()) {
            context.error(position, "cannot swizzle value of type '" +
                    baseType + "'");
            return null;
        }

        byte[] maskComponents = new byte[inComponents.length];
        byte numComponents = 0;
        boolean foundXYZW = false;
        for (int i = 0; i < inComponents.length; i++) {
            byte c = inComponents[i];
            switch (c) {
                case ZERO, ONE:
                    break;
                case X, R, S:
                    foundXYZW = true;
                    maskComponents[numComponents++] = X;
                    break;
                case Y, G, T:
                    foundXYZW = true;
                    if (baseType.getRows() >= 2) {
                        maskComponents[numComponents++] = Y;
                        break;
                    }
                    // fallthrough
                case Z, B, P:
                    foundXYZW = true;
                    if (baseType.getRows() >= 3) {
                        maskComponents[numComponents++] = Z;
                        break;
                    }
                    // fallthrough
                case W, A, Q:
                    foundXYZW = true;
                    if (baseType.getRows() >= 4) {
                        maskComponents[numComponents++] = W;
                        break;
                    }
                    // fallthrough
                default:
                    int offset = Position.getStartOffset(maskPosition) + i;
                    context.error(Position.range(offset, offset + 1),
                            String.format("swizzle component '%c' is out of range for type '%s'",
                                    maskString.charAt(i), baseType));
                    return null;
            }
        }

        if (!foundXYZW) {
            context.error(maskPosition, "swizzle must refer to base expression");
            return null;
        }

        // Coerce literals in expressions such as `(12345).xxx` to their actual type.
        base = baseType.coerceExpression(context, base);
        if (base == null) {
            return null;
        }

        // First, we need a vector expression that is the non-constant portion of the swizzle, packed:
        //   scalar.xxx  -> type3(scalar)
        //   scalar.x0x0 -> type2(scalar)
        //   vector.zyx  -> vector.zyx
        //   vector.x0y0 -> vector.xy
        Expression expr = make(context, position, base, maskComponents, numComponents);

        // If we have processed the entire swizzle, we're done.
        if (numComponents == inComponents.length) {
            return expr;
        }

        // Now we create a constructor that has the correct number of elements for the final swizzle,
        // with all fields at the start. It's not finished yet; constants we need will be added below.
        //   scalar.x0x0 -> type4(type2(x), ...)
        //   vector.y111 -> type4(vector.y, ...)
        //   vector.z10x -> type4(vector.zx, ...)
        //
        // The constructor will have at most three arguments: { base expr, constant 0, constant 1 }
        List<Expression> constructorArgs = new ArrayList<>(3);
        constructorArgs.add(expr);

        // Apply another swizzle to shuffle the constants into the correct place. Any constant values we
        // need are also tacked on to the end of the constructor.
        //   scalar.x0x0 -> type4(type2(x), 0).xyxy
        //   vector.y111 -> type2(vector.y, 1).xyyy
        //   vector.z10x -> type4(vector.zx, 1, 0).xzwy
        Type scalarType = baseType.getComponentType();
        byte maskFieldIdx = 0;
        byte constantFieldIdx = numComponents;
        byte constantZeroIdx = -1;
        byte constantOneIdx = -1;

        numComponents = 0;

        for (byte component : inComponents) {
            switch (component) {
                case ZERO:
                    if (constantZeroIdx == -1) {
                        // Synthesize a '0' argument at the end of the constructor.
                        constructorArgs.add(Literal.make(position, /*value=*/0, scalarType));
                        constantZeroIdx = constantFieldIdx++;
                    }
                    maskComponents[numComponents++] = (constantZeroIdx);
                    break;
                case ONE:
                    if (constantOneIdx == -1) {
                        // Synthesize a '1' argument at the end of the constructor.
                        constructorArgs.add(Literal.make(position, /*value=*/1, scalarType));
                        constantOneIdx = constantFieldIdx++;
                    }
                    maskComponents[numComponents++] = (constantOneIdx);
                    break;
                default:
                    // The non-constant fields are already in the expected order.
                    maskComponents[numComponents++] = maskFieldIdx++;
                    break;
            }
        }

        expr = ConstructorCompound.make(context, position,
                scalarType.toVector(context, constantFieldIdx),
                constructorArgs.toArray(new Expression[0]));

        // Create (and potentially optimize-away) the resulting swizzle-expression.
        return Swizzle.make(context, position, expr, maskComponents, numComponents);
    }

    // input array must be immutable
    @NonNull
    public static Expression make(@NonNull Context context,
                                  int position, @NonNull Expression base,
                                  byte[] components, int numComponents) {
        Type baseType = base.getType();
        assert baseType.isVector() || baseType.isScalar();
        // Confirm that the component array only contains X/Y/Z/W.
        // Once initial IR generation is complete, no swizzles should have zeros or ones in them.
        for (int i = 0; i < numComponents; i++) {
            byte component = components[i];
            assert component == X ||
                    component == Y ||
                    component == Z ||
                    component == W;
        }
        assert numComponents >= 1 && numComponents <= 4;

        // GLSL supports splatting a scalar via `scalar.xxxx`, but not all versions of GLSL allow this.
        // Replace swizzles with equivalent splat constructors (`scalar.xxx` --> `half3(value)`).
        //TODO
        if (baseType.isScalar()) {
            return ConstructorVectorSplat.make(position,
                    baseType.toVector(context, numComponents), base);
        }

        // Detect identity swizzles like `color.rgba` and optimize it away.
        if (numComponents == baseType.getRows()) {
            boolean identity = true;
            for (int i = 0; i < numComponents; ++i) {
                if (components[i] != i) {
                    identity = false;
                    break;
                }
            }
            if (identity) {
                base.mPosition = position;
                return base;
            }
        }

        // Optimize swizzles of swizzles, e.g. replace `foo.argb.rggg` with `foo.arrr`.
        if (base instanceof Swizzle b) {
            byte[] combined = new byte[numComponents];
            for (int i = 0; i < numComponents; ++i) {
                byte c = components[i];
                combined[i] = b.getComponents()[c];
            }

            // It may actually be possible to further simplify this swizzle. Go again.
            // (e.g. `color.abgr.abgr` --> `color.rgba` --> `color`.)
            return make(context, position, b.getBase(), combined, numComponents);
        }

        // If we are swizzling a constant expression, we can use its value instead here (so that
        // swizzles like `colorWhite.x` can be simplified to `1`).
        Expression value = ConstantFolder.getConstantValueForVariable(base);

        // `half4(scalar).zyy` can be optimized to `half3(scalar)`, and `half3(scalar).y` can be
        // optimized to just `scalar`. The swizzle components don't actually matter, as every field
        // in a splat constructor holds the same value.
        if (value instanceof ConstructorVectorSplat ctor) {
            Type ctorType = ctor.getComponentType().toVector(context, numComponents);
            return ConstructorVectorSplat.make(
                    position,
                    ctorType,
                    ctor.getArgument().copy());
        }

        // Swizzles on casts, like `half4(myFloat4).zyy`, can optimize to `half3(myFloat4.zyy)`.
        if (value instanceof ConstructorCompoundCast ctor) {
            Type ctorType = ctor.getComponentType().toVector(context, numComponents);
            Expression swizzled = make(context,
                    position, ctor.getArgument().copy(), components, numComponents);
            Objects.requireNonNull(swizzled);
            return (ctorType.getRows() > 1)
                    ? ConstructorCompoundCast.make(position, ctorType, swizzled)
                    : ConstructorScalarCast.make(context, position, ctorType, swizzled);
        }

        // Swizzles on compound constructors, like `half4(1, 2, 3, 4).yw`, can become `half2(2, 4)`.
        if (value.getKind() == ExpressionKind.CONSTRUCTOR_COMPOUND) {
            var ctor = (ConstructorCompound) value;
            var replacement = optimizeSwizzle(context, position, ctor, components, numComponents);
            if (replacement != null) {
                return replacement;
            }
        }

        // The swizzle could not be simplified, so apply the requested swizzle to the base expression.
        return new Swizzle(position,
                baseType.getComponentType().toVector(context, numComponents),
                base, Arrays.copyOf(components, numComponents));
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.SWIZZLE;
    }

    public Expression getBase() {
        return mBase;
    }

    // **immutable**
    public byte[] getComponents() {
        return mComponents;
    }

    @NonNull
    @Override
    public Expression copy(int position) {
        return new Swizzle(position, getType(), mBase.copy(), mComponents);
    }

    @NonNull
    @Override
    public String toString(int parentPrecedence) {
        StringBuilder result = new StringBuilder(
                mBase.toString(Operator.PRECEDENCE_POSTFIX));
        result.append('.');
        for (byte component : mComponents) {
            result.append(switch (component) {
                case X -> 'x';
                case Y -> 'y';
                case Z -> 'z';
                case W -> 'w';
                default -> throw new IllegalStateException();
            });
        }
        return result.toString();
    }
}
