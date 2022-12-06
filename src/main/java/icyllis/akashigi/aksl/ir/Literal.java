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

package icyllis.akashigi.aksl.ir;

import icyllis.akashigi.aksl.ThreadContext;

import javax.annotation.Nonnull;

/**
 * A constant value. These can contain ints, floats, or booleans.
 */
public final class Literal extends Expression {

    private final double mValue;

    private Literal(int position, double value, Type type) {
        super(position, KIND_CONSTANT, type);
        mValue = value;
    }

    @Nonnull
    public static Literal makeFloat(ThreadContext context, int position, float value) {
        return new Literal(position, value, context.getTypes().mFloat);
    }

    @Nonnull
    public static Literal makeFloat(int position, float value, Type type) {
        if (type.isFloat()) {
            return new Literal(position, value, type);
        }
        throw new IllegalArgumentException();
    }

    @Nonnull
    public static Literal makeInt(ThreadContext context, int position, long value) {
        return new Literal(position, value, context.getTypes().mInt);
    }

    @Nonnull
    public static Literal makeInt(int position, long value, Type type) {
        if (type.isInteger() && value >= type.getMinValue() && value <= type.getMaxValue()) {
            return new Literal(position, value, type);
        }
        throw new IllegalArgumentException();
    }

    @Nonnull
    public static Literal makeBoolean(ThreadContext context, int position, boolean value) {
        return new Literal(position, value ? 1 : 0, context.getTypes().mBool);
    }

    @Nonnull
    public static Literal makeBoolean(int position, boolean value, Type type) {
        if (type.isBoolean()) {
            return new Literal(position, value ? 1 : 0, type);
        }
        throw new IllegalArgumentException();
    }

    @Nonnull
    public static Literal make(int position, double value, Type type) {
        if (type.isFloat()) {
            return makeFloat(position, (float) value, type);
        }
        if (type.isInteger()) {
            return makeInt(position, (int) value, type);
        }
        if (type.isBoolean()) {
            return makeBoolean(position, value != 0, type);
        }
        throw new IllegalArgumentException();
    }

    public float floatValue() {
        assert (type().isFloat());
        return (float) mValue;
    }

    public int intValue() {
        assert (type().isInteger());
        return (int) mValue;
    }

    public boolean booleanValue() {
        assert (type().isBoolean());
        return mValue != 0;
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        if (type().isFloat()) {
            return Float.toString(floatValue());
        }
        if (type().isInteger()) {
            return Integer.toString(intValue());
        }
        assert (type().isBoolean());
        return Boolean.toString(booleanValue());
    }
}
