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

    public static final float SQRT2 = 1.4142135623730951f;
    public static final float SQRT1_2 = 0.7071067811865476f;

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
     * Returns {@code true} if {@code x} is a power of 2. Asserts {@code x > 0}.
     */
    public static boolean isPow2(int x) {
        assert x > 0;
        return (x & x - 1) == 0;
    }

    /**
     * Returns the greatest common divisor of {@code a, b}. Asserts {@code a >= 0 && b >= 0}.
     *
     * @see <a href="https://github.com/google/guava/blob/master/guava/src/com/google/common/math/IntMath.java">
     * IntMath</a>
     */
    public static int gcd(int a, int b) {
        assert a >= 0 && b >= 0;
        if (a == 0) return b;
        if (b == 0) return a;
        int aTwos = Integer.numberOfTrailingZeros(a);
        a >>= aTwos;
        int bTwos = Integer.numberOfTrailingZeros(b);
        b >>= bTwos;
        while (a != b) {
            int delta = a - b;
            int minDeltaOrZero = delta & (delta >> (Integer.SIZE - 1));
            a = delta - minDeltaOrZero - minDeltaOrZero;
            b += minDeltaOrZero;
            a >>= Integer.numberOfTrailingZeros(a);
        }
        return a << Math.min(aTwos, bTwos);
    }

    public static long gcd(long a, long b) {
        assert a >= 0 && b >= 0;
        if (a == 0) return b;
        if (b == 0) return a;
        int aTwos = Long.numberOfTrailingZeros(a);
        a >>= aTwos;
        int bTwos = Long.numberOfTrailingZeros(b);
        b >>= bTwos;
        while (a != b) {
            long delta = a - b;
            long minDeltaOrZero = delta & (delta >> (Long.SIZE - 1));
            a = delta - minDeltaOrZero - minDeltaOrZero;
            b += minDeltaOrZero;
            a >>= Long.numberOfTrailingZeros(a);
        }
        return a << Math.min(aTwos, bTwos);
    }

    /**
     * Returns {@code a^b}.
     */
    public static int quickPow(int a, int b) {
        int res = 1;
        for (; b != 0; b >>= 1, a *= a)
            if ((b & 1) != 0) res *= a;
        return res;
    }

    /**
     * Returns {@code a^b mod m}.
     */
    public static long quickModPow(long a, long b, int m) {
        assert a > 0 && a <= Integer.MAX_VALUE;
        assert b > 0 && b <= Integer.MAX_VALUE;
        long res = 1;
        for (; b != 0; b >>= 1, a = a * a % m)
            if ((b & 1) != 0) res = res * a % m;
        return res;
    }

    public static long quickModMul(long a, long b, int m) {
        assert a > 0 && a <= Integer.MAX_VALUE;
        assert b > 0 && b <= Integer.MAX_VALUE;
        return a * b % m;
    }

    public static int upperBound(int[] a, int key) {
        return upperBound(a, 0, a.length, key);
    }

    public static int upperBound(int[] a, int start, int end, int key) {
        assert (start | end - start | a.length - end) >= 0;
        int low = start, high = end - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (a[mid] > key) high = mid - 1;
            else low = mid + 1;
        }
        return low;
    }

    public static int upperBound(long[] a, long key) {
        return upperBound(a, 0, a.length, key);
    }

    public static int upperBound(long[] a, int start, int end, long key) {
        assert (start | end - start | a.length - end) >= 0;
        int low = start, high = end - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (a[mid] > key) high = mid - 1;
            else low = mid + 1;
        }
        return low;
    }

    public static int lowerBound(int[] a, int key) {
        return lowerBound(a, 0, a.length, key);
    }

    public static int lowerBound(int[] a, int start, int end, int key) {
        assert (start | end - start | a.length - end) >= 0;
        int low = start, high = end - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (a[mid] < key) low = mid + 1;
            else high = mid - 1;
        }
        return low;
    }

    public static int lowerBound(long[] a, long key) {
        return lowerBound(a, 0, a.length, key);
    }

    public static int lowerBound(long[] a, int start, int end, long key) {
        assert (start | end - start | a.length - end) >= 0;
        int low = start, high = end - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (a[mid] < key) low = mid + 1;
            else high = mid - 1;
        }
        return low;
    }

    /**
     * Returns the length of the longest increasing subsequence (non-strictly,
     * also known as longest non-decreasing subsequence).
     * This algorithm has a time complexity of O(n log(n)).
     */
    public static int lengthOfLIS(int[] a, int n) {
        assert n <= a.length;
        if (n <= 1) return n;
        int[] tail = new int[n];
        int length = 1;
        tail[0] = a[0];
        for (int i = 1; i < n; i++) {
            int v = a[i], idx = upperBound(tail, 0, length, v);
            if (idx == length) tail[length++] = v;
            else tail[idx] = v;
        }
        return length;
    }

    /**
     * Returns the length of the longest increasing subsequence.
     * This algorithm has a time complexity of O(n log(n)).
     *
     * @param strict strictly increasing or not
     */
    public static int lengthOfLIS(int[] a, int n, boolean strict) {
        if (!strict) return lengthOfLIS(a, n);
        // strict version only changes 'upperBound' to 'lowerBound'
        assert n <= a.length;
        if (n <= 1) return n;
        int[] tail = new int[n];
        int length = 1;
        tail[0] = a[0];
        for (int i = 1; i < n; i++) {
            int v = a[i], idx = lowerBound(tail, 0, length, v);
            if (idx == length) tail[length++] = v;
            else tail[idx] = v;
        }
        return length;
    }

    private MathUtil() {
    }
}
