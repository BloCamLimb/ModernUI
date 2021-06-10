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

public final class MathUtil {

    private static volatile float[] SINE_TABLE;

    public static final float PI = (float) Math.PI;

    public static final float PI_DIV_2 = (float) (Math.PI / 2);
    public static final float PI_DIV_3 = (float) (Math.PI / 3);
    public static final float PI_DIV_4 = (float) (Math.PI / 4);
    public static final float PI_DIV_6 = (float) (Math.PI / 6);
    public static final float TWO_PI = (float) (Math.PI * 2);
    public static final float THREE_PI_DIV_2 = (float) (Math.PI * 3 / 2);

    public static synchronized void initSineTable() {
        if (SINE_TABLE == null) {
            float[] v = new float[0x10000];
            for (int i = 0; i < 0x10000; i++)
                v[i] = (float) Math.sin(i * 9.587379924285257E-5);
            SINE_TABLE = v;
        }
    }

    // fast sin, error +- 0.000152, in radians
    public static float fsin(float a) {
        return SINE_TABLE[Math.round(a * 10430.378f) & 0xffff];
    }

    // fast cos, error +- 0.000152, in radians
    public static float fcos(float a) {
        return SINE_TABLE[(Math.round(a * 10430.378f) + 0x4000) & 0xffff];
    }

    // sin
    public static float sin(float a) {
        return (float) Math.sin(a);
    }

    // cos
    public static float cos(float a) {
        return (float) Math.cos(a);
    }

    // tan
    public static float tan(float a) {
        return (float) Math.tan(a);
    }

    // exactly equal
    public static boolean exactlyEqual(float a, float b) {
        return Float.floatToIntBits(a) == Float.floatToIntBits(b);
    }

    // approximately equal
    public static boolean approxEqual(float a, float b) {
        return a == b || Math.abs(b - a) < 1.0e-6f;
    }

    // approximately equal
    public static boolean approxEqual(float a, float b, float c, float z) {
        return approxEqual(a, z) && approxEqual(b, z) && approxEqual(c, z);
    }

    // approximately equal
    public static boolean approxEqual(float a, float b, float c, float d, float z) {
        return approxEqual(a, z) && approxEqual(b, z) && approxEqual(c, z) && approxEqual(d, z);
    }

    // approximately equal
    public static boolean isEqual(float a, float b, float eps) {
        return Math.abs(b - a) < eps;
    }

    // approximately equal
    public static boolean approxZero(float a) {
        return a == 0.0f || Math.abs(a) < 1.0e-6f;
    }

    // approximately equal
    public static boolean approxZero(float a, float b, float c) {
        return approxZero(a) && approxZero(b) && approxZero(c);
    }

    // approximately equal
    public static boolean isZero(float a, float eps) {
        return Math.abs(a) < eps;
    }

    // square root
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

    // fast inverse square root
    // see https://en.wikipedia.org/wiki/Fast_inverse_square_root
    public static double fastInvSqrt(double a) {
        double x2 = 0.5 * a;
        long i = Double.doubleToLongBits(a);
        i = 0x5fe6eb50c7b537a9L - (i >> 1);
        a = Double.longBitsToDouble(i);
        a *= 1.5 - x2 * a * a;
        return a;
    }

    // asin
    public static float asin(float a) {
        return (float) Math.asin(a);
    }

    // acos
    public static float acos(float a) {
        return (float) Math.acos(a);
    }

    // atan2 (b, a)
    public static float atan2(float a, float b) {
        return (float) Math.atan2(a, b);
    }

    // clamp 'a' in range [min,max]
    public static int clamp(int a, int min, int max) {
        return Math.max(Math.min(a, max), min);
    }

    // 't' must be positive
    // eg, a=74, t=10, return 80
    public static int roundUp(int a, int t) {
        if (a == 0) return t;
        if (a < 0) t = -t;
        int m = a % t;
        return m == 0 ? a : a + t - m;
    }

    public static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    public static int quickPow(int a, int x) {
        int i = 1;
        while (x != 0) {
            if ((x & 1) == 1)
                i *= a;
            a *= a;
            x >>= 1;
        }
        return i;
    }

    // a^x % mod
    public static int quickModPow(int a, int x, int mod) {
        int i = 1;
        while (x != 0) {
            if ((x & 1) == 1)
                i = quickModMul(i, a, mod);
            a = quickModMul(a, a, mod);
            x >>= 1;
        }
        return i;
    }

    // a * b % mod
    public static int quickModMul(int a, int b, int mod) {
        int i = 0;
        while (b != 0) {
            if ((b & 1) == 1)
                i = (i + a) % mod;
            a = (a << 1) % mod;
            b >>= 1;
        }
        return i;
    }

    // linear interpolation
    public static float lerp(float f, float st, float en) {
        return st + f * (en - st);
    }
}
