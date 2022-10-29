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

public final class ScalarType extends Type {

    private final byte mScalarKind;
    private final byte mPriority;
    private final byte mBitWidth;

    ScalarType(String name, String desc, byte scalarKind, int priority, int bitWidth) {
        super(name, desc, TYPE_KIND_SCALAR);
        assert (desc.length() == 1);
        mScalarKind = scalarKind;
        mPriority = (byte) priority;
        mBitWidth = (byte) bitWidth;
    }

    @Override
    public boolean isScalar() {
        return true;
    }

    @Override
    public byte scalarKind() {
        return mScalarKind;
    }

    @Override
    public int priority() {
        return mPriority;
    }

    @Override
    public int bitWidth() {
        return mBitWidth;
    }

    @Override
    public int columns() {
        return 1;
    }

    @Override
    public int rows() {
        return 1;
    }

    @Override
    public int slotCount() {
        return 1;
    }

    @Override
    public double minimumValue() {
        return switch (mScalarKind) {
            case SCALAR_KIND_SIGNED -> highPrecision() ? Integer.MIN_VALUE
                    : Short.MIN_VALUE;
            case SCALAR_KIND_UNSIGNED -> 0;
            default -> doublePrecision() ? -Double.MAX_VALUE
                    : -Float.MAX_VALUE;
        };
    }

    @Override
    public double maximumValue() {
        return switch (mScalarKind) {
            case SCALAR_KIND_SIGNED -> highPrecision() ? Integer.MAX_VALUE
                    : Short.MAX_VALUE;
            case SCALAR_KIND_UNSIGNED -> highPrecision() ? 0xFFFFFFFF
                    : 0xFFFF;
            default -> doublePrecision() ? Double.MAX_VALUE
                    : Float.MAX_VALUE;
        };
    }
}
