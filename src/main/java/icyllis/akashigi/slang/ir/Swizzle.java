/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.slang.ir;

import icyllis.akashigi.slang.ConstantFolder;
import icyllis.akashigi.slang.Operator;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Represents a vector component selection such as 'float3(1, 2, 3).zyx'.
 */
public final class Swizzle extends Expression {

    /**
     * SwizzleComponents.
     * <p>
     * Pack format: 0xAABBGGRR; terminated with 0xFF if less than 4
     * (e.g. 0x00FF0102 means myVector.zy; 0x01010301 means myVector.ywyy).
     */
    public static final byte
            X =  0,  Y =  1,  Z =  2,  W =  3, // X, Y, Z and W must be 0, 1, 2 and 3
            R =  4,  G =  5,  B =  6,  A =  7,
            S =  8,  T =  9,  P = 10,  Q = 11,
            ZERO = 16,
            ONE  = 17;

    private final Expression mBase;
    private final int mComponents; // contains only X, Y, Z and W

    private Swizzle(int position, Type type, Expression base, int components) {
        super(position, ExpressionKind.kSwizzle, type);
        mBase = base;
        mComponents = components;
    }

    public static int at(int components, int i) {
        return (components >> (i << 3)) & 0xFF;
    }

    @Nonnull
    public static Expression make(int position, Expression base, int components) {
        Type baseType = base.getType();
        assert baseType.isVector() || baseType.isScalar() :
                "cannot swizzle type " + baseType;
        // Confirm that the component array only contains X/Y/Z/W.
        // Once initial IR generation is complete, no swizzles should have zeros or ones in them.
        int n = 0;
        for (; n < 4 && at(components, n) != 0xFF; ++n) {
            switch (at(components, n)) {
                case X, Y, Z, W -> {
                }
                default -> throw new AssertionError();
            }
        }
        assert n >= 1 && n <= 4;

        // GLSL supports splatting a scalar via `scalar.xxxx`, but not all versions of GLSL allow this.
        // Replace swizzles with equivalent splat constructors (`scalar.xxx` --> `half3(value)`).
        if (baseType.isScalar()) {
            return ConstructorVectorScalar.make(position,
                    baseType.toCompound(/*cols=*/1, n), base);
        }

        // Detect identity swizzles like `color.rgba` and optimize it away.
        if (n == baseType.getRows()) {
            boolean identity = true;
            for (int i = 0; i < n; ++i) {
                if (at(components, i) != i) {
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
        if (base.kind() == ExpressionKind.kSwizzle) {
            var b = (Swizzle) base;
            int combined = 0;
            for (int i = 0; i < n; ++i) {
                int c = at(components, i);
                int t = at(b.getComponents(), c);
                combined |= (t << (i << 3));
            }

            // It may actually be possible to further simplify this swizzle. Go again.
            // (e.g. `color.abgr.abgr` --> `color.rgba` --> `color`.)
            return make(position, b.getBase(), combined);
        }

        // If we are swizzling a constant expression, we can use its value instead here (so that
        // swizzles like `colorWhite.x` can be simplified to `1`).
        Expression value = ConstantFolder.getConstantValueForVariable(base);

        // `half4(scalar).zyy` can be optimized to `half3(scalar)`, and `half3(scalar).y` can be
        // optimized to just `scalar`. The swizzle components don't actually matter, as every field
        // in a splat constructor holds the same value.
        if (value.kind() == ExpressionKind.kConstructorVectorScalar) {
            var ctor = (ConstructorVectorScalar) value;
            Type ctorType = ctor.getComponentType().toCompound(/*cols=*/1, n);
            return ConstructorVectorScalar.make(
                    position,
                    ctorType,
                    ctor.getArguments()[0].clone());
        }

        // Swizzles on casts, like `half4(myFloat4).zyy`, can optimize to `half3(myFloat4.zyy)`.
        if (value.kind() == ExpressionKind.kConstructorCompoundCast) {
            var ctor = (ConstructorCompoundCast) value;
            Type ctorType = ctor.getComponentType().toCompound(/*cols=*/1, n);
            Expression swizzled = make(position, ctor.getArguments()[0].clone(), components);
            Objects.requireNonNull(swizzled);
            return (ctorType.getRows() > 1)
                    ? ConstructorCompoundCast.make(position, ctorType, swizzled)
                    : ConstructorScalarCast.make(position, ctorType, swizzled);
        }

        // Swizzles on compound constructors, like `half4(1, 2, 3, 4).yw`, can become `half2(2, 4)`.
        /*if (value.kind() == ExpressionKind.kConstructorCompound) {
            var ctor = (ConstructorCompound) value;
            if (var replacement = optimize_constructor_swizzle(context, pos, ctor, components)) {
                return replacement;
            }
        }*/

        // The swizzle could not be simplified, so apply the requested swizzle to the base expression.
        return new Swizzle(position,
                baseType.getComponentType().toCompound(/*cols=*/1, n),
                base, components);
    }

    public Expression getBase() {
        return mBase;
    }

    public int getComponents() {
        return mComponents;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new Swizzle(position, getType(), mBase.clone(), mComponents);
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        StringBuilder result = new StringBuilder(
                mBase.toString(Operator.PRECEDENCE_POSTFIX));
        result.append('.');
        for (int i = 0; i < 4 && at(mComponents, i) != 0xFF; ++i) {
            result.append((char) switch (at(mComponents, i)) {
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
