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

import icyllis.arc3d.compiler.Context;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.OptionalDouble;

/**
 * A constant value. These can contain ints, floats, or booleans.
 */
public final class Literal extends Expression {

    private final double mValue;

    private Literal(int position, double value, Type type) {
        super(position, type);
        mValue = value;
    }

    @NonNull
    public static Literal makeFloat(@NonNull Context context, int position, float value) {
        return new Literal(position, value, context.getTypes().mFloat);
    }

    @NonNull
    public static Literal makeFloat(int position, float value, Type type) {
        if (type.isFloat() && Float.isFinite(value)) {
            return new Literal(position, value, type);
        }
        throw new IllegalArgumentException();
    }

    @NonNull
    public static Literal makeInteger(@NonNull Context context, int position, long value, boolean signed) {
        return new Literal(position, value, signed ? context.getTypes().mInt : context.getTypes().mUInt);
    }

    @NonNull
    public static Literal makeInteger(int position, long value, Type type) {
        if (type.isInteger() && value >= type.getMinValue() && value <= type.getMaxValue()) {
            return new Literal(position, value, type);
        }
        throw new IllegalArgumentException();
    }

    @NonNull
    public static Literal makeBoolean(@NonNull Context context, int position, boolean value) {
        return new Literal(position, value ? 1 : 0, context.getTypes().mBool);
    }

    @NonNull
    public static Literal makeBoolean(int position, boolean value, Type type) {
        if (type.isBoolean()) {
            return new Literal(position, value ? 1 : 0, type);
        }
        throw new IllegalArgumentException();
    }

    @NonNull
    public static Literal make(int position, double value, Type type) {
        if (type.isFloat()) {
            return makeFloat(position, (float) value, type);
        }
        if (type.isInteger()) {
            return makeInteger(position, (long) value, type);
        }
        if (type.isBoolean()) {
            return makeBoolean(position, value != 0, type);
        }
        throw new IllegalArgumentException();
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.LITERAL;
    }

    @Override
    public boolean accept(@NonNull TreeVisitor visitor) {
        return visitor.visitLiteral(this);
    }

    @Override
    public boolean isLiteral() {
        return true;
    }

    public float getFloatValue() {
        assert (getType().isFloat());
        return (float) mValue;
    }

    public long getIntegerValue() {
        assert (getType().isInteger());
        return (long) mValue;
    }

    public boolean getBooleanValue() {
        assert (getType().isBoolean());
        return mValue != 0;
    }

    public double getValue() {
        return mValue;
    }

    @Override
    public OptionalDouble getConstantValue(int i) {
        assert i == 0;
        return OptionalDouble.of(mValue);
    }

    @NonNull
    @Override
    public Expression clone(int position) {
        return new Literal(position, mValue, getType());
    }

    @NonNull
    @Override
    public String toString(int parentPrecedence) {
        if (getType().isFloat()) {
            return String.valueOf(getFloatValue());
        }
        if (getType().isInteger()) {
            return String.valueOf(getIntegerValue());
        }
        assert (getType().isBoolean());
        return String.valueOf(getBooleanValue());
    }
}
