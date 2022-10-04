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

package icyllis.akashigi.core;

/**
 * Utility class that provides auxiliary operations.
 */
public final class MathUtil {

    public static final float PI = (float) Math.PI;
    public static final float PI_O_2 = (float) (Math.PI / 2);
    public static final float PI_O_3 = (float) (Math.PI / 3);
    public static final float PI_O_4 = (float) (Math.PI / 4);
    public static final float PI_O_6 = (float) (Math.PI / 6);
    public static final float PI2 = (float) (Math.PI * 2);
    public static final float PI3 = (float) (Math.PI * 3);
    public static final float PI4 = (float) (Math.PI * 4);
    public static final float PI3_O_2 = (float) (Math.PI * 3 / 2);
    public static final float HALF_PI = PI_O_2;
    public static final float QUARTER_PI = PI_O_4;
    public static final float TWO_PI = PI2;
    public static final float THREE_PI = PI3;
    public static final float FOUR_PI = PI4;

    public static final float EPS = 1.0e-6f;
    public static final float INV_EPS = 1.0e6f;
    public static final float DEG_TO_RAD = 0.01745329251994329576923690768489f;
    public static final float RAD_TO_DEG = 57.295779513082320876798154814105f;

    public static final float SQRT_OF_TWO = 1.4142135623730950488016887242097f;

    /**
     * @return true if <code>a</code> is approximately equal to zero
     */
    public static boolean approxZero(float a) {
        return Math.abs(a) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to zero
     */
    public static boolean approxZero(float a, float b) {
        return Math.abs(a) < EPS && Math.abs(b) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to zero
     */
    public static boolean approxZero(float a, float b, float c) {
        return Math.abs(a) < EPS && Math.abs(b) < EPS && Math.abs(c) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to zero
     */
    public static boolean approxZero(float a, float b, float c, float d) {
        return Math.abs(a) < EPS && Math.abs(b) < EPS && Math.abs(c) < EPS && Math.abs(d) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code>
     */
    public static boolean approxEqual(float a, float b) {
        return Math.abs(b - a) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code>
     */
    public static boolean approxEqual(float a, float b, float c) {
        return Math.abs(b - a) < EPS && Math.abs(c - a) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code>
     */
    public static boolean approxEqual(float a, float b, float c, float d) {
        return Math.abs(b - a) < EPS && Math.abs(c - a) < EPS && Math.abs(d - a) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code>
     */
    public static boolean approxEqual(float a, float b, float c, float d, float e) {
        return Math.abs(b - a) < EPS && Math.abs(c - a) < EPS && Math.abs(d - a) < EPS && Math.abs(e - a) < EPS;
    }

    /**
     * If v compares less than lo, returns lo; otherwise if hi compares less than v,
     * returns hi; otherwise returns v.
     *
     * @return v clamped between lo and hi, inclusively.
     */
    public static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(v, hi));
    }

    /**
     * If v compares less than lo, returns lo; otherwise if hi compares less than v,
     * returns hi; otherwise returns v.
     *
     * @return v clamped between lo and hi, inclusively.
     */
    public static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(v, hi));
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static float min(float a, float b, float c, float d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static float max(float a, float b, float c, float d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    /**
     * @return linear interpolation
     */
    public static float lerp(float f, float st, float en) {
        return st + f * (en - st);
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

    /**
     * Aligns {@code x} up to 2 (half-word).
     */
    public static long align2(long x) {
        assert x >= 0 && x <= Long.MAX_VALUE - 16;
        return (x + 1) & -2;
    }

    /**
     * Aligns {@code x} up to 4 (word).
     */
    public static long align4(long x) {
        assert x >= 0 && x <= Long.MAX_VALUE - 16;
        return (x + 3) & -4;
    }

    /**
     * Aligns {@code x} up to 8 (double word).
     */
    public static long align8(long x) {
        assert x >= 0 && x <= Long.MAX_VALUE - 16;
        return (x + 7) & -8;
    }

    public static boolean isAlign2(int x) {
        assert x >= 0;
        return (x & 1) == 0;
    }

    public static boolean isAlign4(int x) {
        assert x >= 0;
        return (x & 3) == 0;
    }

    public static boolean isAlign8(int x) {
        assert x >= 0;
        return (x & 7) == 0;
    }

    public static boolean isAlign2(long x) {
        assert x >= 0;
        return (x & 1) == 0;
    }

    public static boolean isAlign4(long x) {
        assert x >= 0;
        return (x & 3) == 0;
    }

    public static boolean isAlign8(long x) {
        assert x >= 0;
        return (x & 7) == 0;
    }

    /**
     * Aligns {@code x} up to a power of two.
     */
    public static int alignTo(int x, int alignment) {
        assert x >= 0 && alignment > 0 && (alignment & (alignment - 1)) == 0;
        return (x + alignment - 1) & -alignment;
    }

    public static int alignUp(int x, int alignment) {
        assert x >= 0 && alignment > 0;
        int n = x % alignment;
        return n == 0 ? x : x + alignment - n;
    }

    public static int alignUpPad(int x, int alignment) {
        assert x >= 0 && alignment > 0;
        return (alignment - x % alignment) % alignment;
    }

    public static int alignDown(int x, int alignment) {
        assert x >= 0 && alignment > 0;
        return (x / alignment) * alignment;
    }

    /**
     * Returns the smallest power of two greater than or equal to {@code x}.
     */
    public static int nextPow2(int x) {
        assert x > 0 && x <= (1 << (Integer.SIZE - 2));
        return 1 << -Integer.numberOfLeadingZeros(x - 1);
    }

    /**
     * Returns the largest power of two less than or equal to {@code x}.
     */
    public static int prevPow2(int x) {
        assert x > 0;
        return Integer.highestOneBit(x);
    }

    /**
     * Returns {@code true} if {@code x} represents a power of two.
     */
    public static boolean isPow2(int x) {
        assert x > 0;
        return (x & (x - 1)) == 0;
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

    private MathUtil() {
    }
}
