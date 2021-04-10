/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.math;

public class MathUtil {

    private static final float[] SINE_TABLE;

    static {
        float[] v = new float[0x10000];
        for (int i = 0; i < 0x10000; i++)
            v[i] = (float) Math.sin(i * 9.587379924285257E-5);
        SINE_TABLE = v;
    }

    // error +- 0.000152, in radians
    public static float sin(float a) {
        return SINE_TABLE[Math.round(a * 10430.378f) & 0xffff];
    }

    // error +- 0.000152, in radians
    public static float cos(float a) {
        return SINE_TABLE[(Math.round(a * 10430.378f) + 16384) & 0xffff];
    }

    // exactly equal
    public static boolean exactEqual(float a, float b) {
        return Float.floatToIntBits(a) == Float.floatToIntBits(b);
    }

    // approximately equal
    public static boolean approxEqual(float a, float b, float epsilon) {
        return a == b || Math.abs(b - a) < epsilon;
    }

    public static boolean approxEqual(float a, float b) {
        return a == b || Math.abs(b - a) < 1.0e-6f;
    }

    public static boolean isZero(float a, float epsilon) {
        return a == 0.0f || Math.abs(a) < epsilon;
    }

    public static boolean isZero(float a) {
        return a == 0.0f || Math.abs(a) < 1.0e-6f;
    }

    public static float sqrt(float f) {
        return (float) Math.sqrt(f);
    }

    // fast inverse square root
    // see https://en.wikipedia.org/wiki/Fast_inverse_square_root
    public static float fastInvSqrt(float a) {
        float x2 = 0.5f * a;
        int i = Float.floatToIntBits(a);
        i = 0x5f3759df - (i >> 1);
        a = Float.intBitsToFloat(i);
        a *= 1.5f - x2 * a * a;
        return a;
    }

    public static double fastInvSqrt(double a) {
        double x2 = 0.5 * a;
        long i = Double.doubleToRawLongBits(a);
        i = 0x5fe6eb50c7b537a9L - (i >> 1);
        a = Double.longBitsToDouble(i);
        a *= 1.5 - x2 * a * a;
        return a;
    }
}
