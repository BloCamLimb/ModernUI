/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class FMath {

    // compile-time
    private static final boolean USE_SIN_TABLE = false;

    // 256 kB
    private static final float[] SIN_TABLE;

    public static final float PI = (float) Math.PI;
    public static final float PI_O_2 = (float) (Math.PI / 2);
    public static final float PI_O_3 = (float) (Math.PI / 3);
    public static final float PI_O_4 = (float) (Math.PI / 4);
    public static final float PI_O_6 = (float) (Math.PI / 6);
    public static final float PI2 = (float) (Math.PI * 2);
    public static final float PI3 = (float) (Math.PI * 3);
    public static final float PI4 = (float) (Math.PI * 4);
    public static final float PI3_O_2 = (float) (Math.PI * 3 / 2);

    public static final float EPS = 1.0e-6f;

    private static final float DEG_TO_RAD = 0.017453292519943295f;

    static {
        if (USE_SIN_TABLE) {
            float[] v = new float[0x10000];
            for (int i = 0; i < 0x10000; i++)
                v[i] = (float) Math.sin(i * 9.587379924285257E-5);
            SIN_TABLE = v;
        } else {
            SIN_TABLE = null;
        }
    }

    // fast sin, error +- 0.000152, in radians
    public static float fsin(float a) {
        return SIN_TABLE[Math.round(a * 10430.378f) & 0xffff];
    }

    // fast cos, error +- 0.000152, in radians
    public static float fcos(float a) {
        return SIN_TABLE[(Math.round(a * 10430.378f) + 0x4000) & 0xffff];
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

    // approximately equal
    public static boolean eq(float a, float b) {
        return Math.abs(b - a) < EPS;
    }

    // approximately equal
    public static boolean eq(float a, float b, float c) {
        return eq(a, c) && eq(b, c);
    }

    // approximately equal
    public static boolean eq(float a, float b, float c, float d) {
        return eq(a, d) && eq(b, d) && eq(c, d);
    }

    // approximately equal
    public static boolean eq(float a, float b, float c, float d, float e) {
        return eq(a, e) && eq(b, e) && eq(c, e) && eq(d, e);
    }

    // approximately equal
    public static boolean zero(float a) {
        return Math.abs(a) < EPS;
    }

    // approximately equal
    public static boolean zero(float a, float b) {
        return zero(a) && zero(b);
    }

    // approximately equal
    public static boolean zero(float a, float b, float c) {
        return zero(a) && zero(b) && zero(c);
    }

    // approximately equal
    public static boolean zero(float a, float b, float c, float d) {
        return zero(a) && zero(b) && zero(c) && zero(d);
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

    // hypot
    public static float hypot(float x, float y) {
        return (float) Math.hypot(x, y);
    }

    // clamp 'a' in range [min,max]
    public static int clamp(int a, int min, int max) {
        return Math.max(Math.min(a, max), min);
    }

    // clamp 'a' in range [min,max]
    public static float clamp(float a, float min, float max) {
        return Math.max(Math.min(a, max), min);
    }

    // min component of vec4
    public static float min(float a, float b, float c, float d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    // max component of vec4
    public static float max(float a, float b, float c, float d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    public static float toRadians(float degrees) {
        return degrees * DEG_TO_RAD;
    }

    // 'to' must be positive
    // eg, a=74, to=10, return 80
    public static int roundUp(int a, int to) {
        if (a == 0) return to;
        if (a < 0) to = -to;
        int m = a % to;
        return m == 0 ? a : a + to - m;
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

    // n - positive
    public static int numOnes(int n) {
        if (n == 0)
            return 0;
        int r = 1;
        while ((n &= n - 1) != 0)
            r++;
        return r;
    }

    /**
     * Aligns {@code x} up to 2 (half-word).
     */
    public static int align2(int x) {
        assert x >= 0 && x <= Integer.MAX_VALUE - 8;
        return (x + 1) & -2;
    }

    /**
     * Aligns {@code x} up to 4 (word).
     */
    public static int align4(int x) {
        assert x >= 0 && x <= Integer.MAX_VALUE - 8;
        return (x + 3) & -4;
    }

    /**
     * Aligns {@code x} up to 8 (double word).
     */
    public static int align8(int x) {
        assert x >= 0 && x <= Integer.MAX_VALUE - 8;
        return (x + 7) & -8;
    }
}
