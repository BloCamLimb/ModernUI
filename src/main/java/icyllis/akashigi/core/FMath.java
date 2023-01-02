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

import java.util.Comparator;

/**
 * Utility class that provides auxiliary operations.
 */
public final class FMath {

    public static final float PI = (float) Math.PI;
    public static final float PI2 = (float) (Math.PI * 2.0);
    public static final float PI3 = (float) (Math.PI * 3.0);
    public static final float PI4 = (float) (Math.PI * 4.0);
    public static final float PI_O_2 = (float) (Math.PI / 2.0);
    public static final float PI_O_3 = (float) (Math.PI / 3.0);
    public static final float PI_O_4 = (float) (Math.PI / 4.0);
    public static final float PI_O_6 = (float) (Math.PI / 6.0);

    public static final float EPS = 1.0e-5f;
    public static final float INV_EPS = 1.0e5f;

    // DEG_TO_RAD == 1.0 / RAD_TO_DEG
    public static final float DEG_TO_RAD = 0.01745329251994329576923690768489f;
    public static final float RAD_TO_DEG = 57.295779513082320876798154814105f;

    // SQRT2 == INV_SQRT2 * 2.0
    public static final float SQRT2 = 1.4142135623730951f;
    public static final float INV_SQRT2 = 0.7071067811865476f;

    /**
     * @return true if <code>a</code> is approximately equal to zero
     */
    public static boolean isNearlyZero(float a) {
        return Math.abs(a) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to zero
     */
    public static boolean isNearlyZero(float a, float b) {
        return Math.abs(a) < EPS && Math.abs(b) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to zero
     */
    public static boolean isNearlyZero(float a, float b, float c) {
        return Math.abs(a) < EPS && Math.abs(b) < EPS && Math.abs(c) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to zero
     */
    public static boolean isNearlyZero(float a, float b, float c, float d) {
        return Math.abs(a) < EPS && Math.abs(b) < EPS && Math.abs(c) < EPS && Math.abs(d) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code>
     */
    public static boolean isNearlyEqual(float a, float b) {
        return Math.abs(b - a) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code>
     */
    public static boolean isNearlyEqual(float a, float b, float c) {
        return Math.abs(b - a) < EPS && Math.abs(c - a) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code>
     */
    public static boolean isNearlyEqual(float a, float b, float c, float d) {
        return Math.abs(b - a) < EPS && Math.abs(c - a) < EPS && Math.abs(d - a) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code>
     */
    public static boolean isNearlyEqual(float a, float b, float c, float d, float e) {
        return Math.abs(b - a) < EPS && Math.abs(c - a) < EPS && Math.abs(d - a) < EPS && Math.abs(e - a) < EPS;
    }

    /**
     * Converts an angle in degrees to an angle in radians.
     */
    public static float toRadians(float degrees) {
        return degrees * DEG_TO_RAD;
    }

    /**
     * Converts an angle in radians to an angle in degrees.
     */
    public static float toDegrees(float radians) {
        return radians * RAD_TO_DEG;
    }

    /**
     * If x compares less than min, returns min; otherwise if max compares less than x,
     * returns max; otherwise returns x.
     *
     * @return x clamped between min and max, inclusively.
     */
    public static int clamp(int x, int min, int max) {
        return Math.max(min, Math.min(x, max));
    }

    /**
     * If x compares less than min, returns min; otherwise if max compares less than x,
     * returns max; otherwise returns x.
     *
     * @return x clamped between min and max, inclusively.
     */
    public static long clamp(long x, long min, long max) {
        return Math.max(min, Math.min(x, max));
    }

    /**
     * If x compares less than min, returns min; otherwise if max compares less than x,
     * returns max; otherwise returns x.
     *
     * @return x clamped between min and max, inclusively.
     */
    public static float clamp(float x, float min, float max) {
        return Math.max(min, Math.min(x, max));
    }

    /**
     * If x compares less than min, returns min; otherwise if max compares less than x,
     * returns max; otherwise returns x.
     *
     * @return x clamped between min and max, inclusively.
     */
    public static double clamp(double x, double min, double max) {
        return Math.max(min, Math.min(x, max));
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static float min(float a, float b, float c) {
        return Math.min(Math.min(a, b), c);
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static double min(double a, double b, double c) {
        return Math.min(Math.min(a, b), c);
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static float min(float a, float b, float c, float d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static double min(double a, double b, double c, double d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static float max(float a, float b, float c) {
        return Math.max(Math.max(a, b), c);
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static double max(double a, double b, double c) {
        return Math.max(Math.max(a, b), c);
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static float max(float a, float b, float c, float d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static double max(double a, double b, double c, double d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    /**
     * Linear interpolation between two values.
     */
    public static float lerp(float a, float b, float t) {
        return (b - a) * t + a;
    }

    /**
     * Linear interpolation between two values.
     */
    public static double lerp(double a, double b, double t) {
        return (b - a) * t + a;
    }

    /**
     * Linear interpolation between two values, matches GLSL {@code mix} intrinsic function.
     * Slower than {@link #lerp(float, float, float)} but without intermediate overflow or underflow.
     */
    public static float mix(float a, float b, float t) {
        return a * (1 - t) + b * t;
    }

    /**
     * Linear interpolation between two values, matches GLSL {@code mix} intrinsic function.
     * Slower than {@link #lerp(double, double, double)} but without intermediate overflow or underflow.
     */
    public static double mix(double a, double b, double t) {
        return a * (1 - t) + b * t;
    }

    /**
     * 2D bilinear interpolation between four values (a quad).
     */
    public static float biLerp(float q00, float q10, float q01, float q11, float tx, float ty) {
        return lerp(lerp(q00, q10, tx), lerp(q01, q11, tx), ty);
    }

    /**
     * 2D bilinear interpolation between four values (a quad).
     */
    public static double biLerp(double q00, double q10, double q01, double q11, double tx, double ty) {
        return lerp(lerp(q00, q10, tx), lerp(q01, q11, tx), ty);
    }

    /**
     * 3D trilinear interpolation between eight values (a cube).
     */
    public static float triLerp(float c000, float c100, float c010, float c110,
                                float c001, float c101, float c011, float c111,
                                float tx, float ty, float tz) {
        return lerp(lerp(lerp(c000, c100, tx), lerp(c010, c110, tx), ty),
                lerp(lerp(c001, c101, tx), lerp(c011, c111, tx), ty), tz);
    }

    /**
     * 3D trilinear interpolation between eight values (a cube).
     */
    public static double triLerp(double c000, double c100, double c010, double c110,
                                 double c001, double c101, double c011, double c111,
                                 double tx, double ty, double tz) {
        return lerp(lerp(lerp(c000, c100, tx), lerp(c010, c110, tx), ty),
                lerp(lerp(c001, c101, tx), lerp(c011, c111, tx), ty), tz);
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

    /**
     * Returns {@code true} if {@code x} is a multiple of 2. Asserts {@code x >= 0}.
     */
    public static boolean isAlign2(int x) {
        assert x >= 0;
        return (x & 1) == 0;
    }

    /**
     * Returns {@code true} if {@code x} is a multiple of 4. Asserts {@code x >= 0}.
     */
    public static boolean isAlign4(int x) {
        assert x >= 0;
        return (x & 3) == 0;
    }

    /**
     * Returns {@code true} if {@code x} is a multiple of 8. Asserts {@code x >= 0}.
     */
    public static boolean isAlign8(int x) {
        assert x >= 0;
        return (x & 7) == 0;
    }

    /**
     * Returns {@code true} if {@code x} is a multiple of 2. Asserts {@code x >= 0}.
     */
    public static boolean isAlign2(long x) {
        assert x >= 0;
        return (x & 1) == 0;
    }

    /**
     * Returns {@code true} if {@code x} is a multiple of 4. Asserts {@code x >= 0}.
     */
    public static boolean isAlign4(long x) {
        assert x >= 0;
        return (x & 3) == 0;
    }

    /**
     * Returns {@code true} if {@code x} is a multiple of 8. Asserts {@code x >= 0}.
     */
    public static boolean isAlign8(long x) {
        assert x >= 0;
        return (x & 7) == 0;
    }

    /**
     * Aligns {@code x} up to a power of two.
     */
    public static int alignTo(int x, int alignment) {
        assert x >= 0 && alignment > 0 &&
                (alignment & (alignment - 1)) == 0;
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
     * Returns {@code true} if {@code x} is a power of 2. Asserts {@code x > 0}.
     */
    public static boolean isPow2(int x) {
        assert x > 0;
        return (x & x - 1) == 0;
    }

    /**
     * Returns {@code true} if {@code x} is a power of 2. Asserts {@code x > 0}.
     */
    public static boolean isPow2(long x) {
        assert x > 0;
        return (x & x - 1) == 0;
    }

    /**
     * Returns the smallest power of two greater than or equal to {@code x}.
     * Asserts {@code x > 0 && x <= 2^30}.
     */
    public static int ceilPow2(int x) {
        assert x > 0 && x <= (1 << (Integer.SIZE - 2));
        return 1 << -Integer.numberOfLeadingZeros(x - 1);
    }

    /**
     * Returns the smallest power of two greater than or equal to {@code x}.
     * Asserts {@code x > 0 && x <= 2^62}.
     */
    public static long ceilPow2(long x) {
        assert x > 0 && x <= (1L << (Long.SIZE - 2));
        return 1L << -Long.numberOfLeadingZeros(x - 1);
    }

    /**
     * Returns the largest power of two less than or equal to {@code x}.
     * Asserts {@code x > 0}.
     */
    public static int floorPow2(int x) {
        assert x > 0;
        return Integer.highestOneBit(x);
    }

    /**
     * Returns the largest power of two less than or equal to {@code x}.
     * Asserts {@code x > 0}.
     */
    public static long floorPow2(long x) {
        assert x > 0;
        return Long.highestOneBit(x);
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

    /**
     * Returns the greatest common divisor of {@code a, b}. Asserts {@code a >= 0 && b >= 0}.
     *
     * @see <a href="https://github.com/google/guava/blob/master/guava/src/com/google/common/math/LongMath.java">
     * LongMath</a>
     */
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
     * Returns {@code a^b}, no overflow checks. Asserts {@code b >= 0}.
     *
     * @param a the base
     * @param b the exponent
     */
    public static int quickPow(int a, int b) {
        assert b >= 0;
        int res = 1;
        for (; b != 0; b >>= 1, a *= a)
            if ((b & 1) != 0) res *= a;
        return res;
    }

    /**
     * Returns {@code a^b}, no overflow checks. Asserts {@code b >= 0}.
     *
     * @param a the base
     * @param b the exponent
     */
    public static long quickPow(long a, long b) {
        assert b >= 0;
        long res = 1;
        for (; b != 0; b >>= 1, a *= a)
            if ((b & 1) != 0) res *= a;
        return res;
    }

    /**
     * Returns {@code a^b mod m}, no overflow checks. Asserts {@code b >= 0 && m > 0}.
     */
    public static int quickModPow(int a, int b, int m) {
        assert b >= 0 && m > 0;
        int res = 1;
        for (; b != 0; b >>= 1, a = a * a % m)
            if ((b & 1) != 0) res = res * a % m;
        return res;
    }

    /**
     * Returns {@code a^b mod m}, no overflow checks. Asserts {@code b >= 0 && m > 0}.
     */
    public static long quickModPow(long a, long b, int m) {
        assert b >= 0 && m > 0;
        long res = 1;
        for (; b != 0; b >>= 1, a = a * a % m)
            if ((b & 1) != 0) res = res * a % m;
        return res;
    }

    /**
     * Returns an index of the first element in the range {@code [0, a.length)}
     * such that {@code a[index] > value}, or {@code a.length} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code a.length}
     * @see java.util.Arrays#sort(int[])
     */
    public static int upperBound(int[] a, int value) {
        return upperBound(a, 0, a.length, value);
    }

    /**
     * Returns an index of the first element in the range {@code [first, last)}
     * such that {@code a[index] > value}, or {@code last} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param first the index of the first element (inclusive) to be searched
     * @param last  the index of the last element (exclusive) to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code last}
     * @see java.util.Arrays#sort(int[], int, int)
     */
    public static int upperBound(int[] a, int first, int last, int value) {
        assert (first | last - first | a.length - last) >= 0;
        int low = first, high = last - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (a[mid] > value) high = mid - 1;
            else low = mid + 1;
        }
        return low;
    }

    /**
     * Returns an index of the first element in the range {@code [0, a.length)}
     * such that {@code a[index] > value}, or {@code a.length} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code a.length}
     * @see java.util.Arrays#sort(long[])
     */
    public static int upperBound(long[] a, long value) {
        return upperBound(a, 0, a.length, value);
    }

    /**
     * Returns an index of the first element in the range {@code [first, last)}
     * such that {@code a[index] > value}, or {@code last} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param first the index of the first element (inclusive) to be searched
     * @param last  the index of the last element (exclusive) to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code last}
     * @see java.util.Arrays#sort(long[], int, int)
     */
    public static int upperBound(long[] a, int first, int last, long value) {
        assert (first | last - first | a.length - last) >= 0;
        int low = first, high = last - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (a[mid] > value) high = mid - 1;
            else low = mid + 1;
        }
        return low;
    }

    /**
     * Returns an index of the first element in the range {@code [0, a.length)}
     * such that {@code a[index] > value}, or {@code a.length} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @param c     the comparator by which the array is ordered.  A
     *              {@code null} value indicates that the elements'
     *              {@linkplain Comparable natural ordering} should be used.
     * @return index of the search value, or {@code a.length}
     * @see java.util.Arrays#sort(Object[], Comparator)
     */
    public static <T> int upperBound(T[] a, T value, Comparator<? super T> c) {
        return upperBound(a, 0, a.length, value, c);
    }

    /**
     * Returns an index of the first element in the range {@code [first, last)}
     * such that {@code a[index] > value}, or {@code last} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param first the index of the first element (inclusive) to be searched
     * @param last  the index of the last element (exclusive) to be searched
     * @param value the value to be searched for
     * @param c     the comparator by which the array is ordered.  A
     *              {@code null} value indicates that the elements'
     *              {@linkplain Comparable natural ordering} should be used.
     * @return index of the search value, or {@code last}
     * @see java.util.Arrays#sort(Object[], int, int, Comparator)
     */
    @SuppressWarnings("unchecked")
    public static <T> int upperBound(T[] a, int first, int last, T value, Comparator<? super T> c) {
        assert (first | last - first | a.length - last) >= 0;
        if (c == null) c = (Comparator<? super T>) Comparator.naturalOrder();
        int low = first, high = last - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (c.compare(a[mid], value) > 0) high = mid - 1;
            else low = mid + 1;
        }
        return low;
    }

    /**
     * Returns an index of the first element in the range {@code [0, a.length)}
     * such that {@code a[index] < value}, or {@code a.length} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code a.length}
     * @see java.util.Arrays#sort(int[])
     */
    public static int lowerBound(int[] a, int value) {
        return lowerBound(a, 0, a.length, value);
    }

    /**
     * Returns an index of the first element in the range {@code [first, last)}
     * such that {@code a[index] < value}, or {@code last} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param first the index of the first element (inclusive) to be searched
     * @param last  the index of the last element (exclusive) to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code last}
     * @see java.util.Arrays#sort(int[], int, int)
     */
    public static int lowerBound(int[] a, int first, int last, int value) {
        assert (first | last - first | a.length - last) >= 0;
        int low = first, high = last - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (a[mid] < value) low = mid + 1;
            else high = mid - 1;
        }
        return low;
    }

    /**
     * Returns an index of the first element in the range {@code [0, a.length)}
     * such that {@code a[index] < value}, or {@code a.length} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code a.length}
     * @see java.util.Arrays#sort(long[])
     */
    public static int lowerBound(long[] a, long value) {
        return lowerBound(a, 0, a.length, value);
    }

    /**
     * Returns an index of the first element in the range {@code [first, last)}
     * such that {@code a[index] < value}, or {@code last} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param first the index of the first element (inclusive) to be searched
     * @param last  the index of the last element (exclusive) to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code last}
     * @see java.util.Arrays#sort(long[], int, int)
     */
    public static int lowerBound(long[] a, int first, int last, long value) {
        assert (first | last - first | a.length - last) >= 0;
        int low = first, high = last - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (a[mid] < value) low = mid + 1;
            else high = mid - 1;
        }
        return low;
    }

    /**
     * Returns an index of the first element in the range {@code [0, a.length)}
     * such that {@code a[index] < value}, or {@code a.length} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @param c     the comparator by which the array is ordered.  A
     *              {@code null} value indicates that the elements'
     *              {@linkplain Comparable natural ordering} should be used.
     * @return index of the search value, or {@code a.length}
     * @see java.util.Arrays#sort(Object[], Comparator)
     */
    public static <T> int lowerBound(T[] a, T value, Comparator<? super T> c) {
        return lowerBound(a, 0, a.length, value, c);
    }

    /**
     * Returns an index of the first element in the range {@code [first, last)}
     * such that {@code a[index] < value}, or {@code last} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param first the index of the first element (inclusive) to be searched
     * @param last  the index of the last element (exclusive) to be searched
     * @param value the value to be searched for
     * @param c     the comparator by which the array is ordered.  A
     *              {@code null} value indicates that the elements'
     *              {@linkplain Comparable natural ordering} should be used.
     * @return index of the search value, or {@code last}
     * @see java.util.Arrays#sort(Object[], int, int, Comparator)
     */
    @SuppressWarnings("unchecked")
    public static <T> int lowerBound(T[] a, int first, int last, T value, Comparator<? super T> c) {
        assert (first | last - first | a.length - last) >= 0;
        if (c == null) c = (Comparator<? super T>) Comparator.naturalOrder();
        int low = first, high = last - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (c.compare(a[mid], value) < 0) high = mid - 1;
            else low = mid + 1;
        }
        return low;
    }

    /**
     * Returns the length of the longest increasing subsequence (non-strictly).
     * This algorithm has a time complexity of O(n log(n)).
     */
    public static int lengthOfLIS(int[] a, int n) {
        assert n <= a.length;
        if (n <= 1) return n;
        int[] tail = new int[n];
        int length = 1;
        tail[0] = a[0];
        for (int i = 1; i < n; i++) {
            int v = a[i], pos = upperBound(tail, 0, length, v);
            if (pos == length) tail[length++] = v;
            else tail[pos] = v;
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
            int v = a[i], pos = lowerBound(tail, 0, length, v);
            if (pos == length) tail[length++] = v;
            else tail[pos] = v;
        }
        return length;
    }

    /**
     * Returns the length of the longest increasing subsequence (non-strictly).
     * This algorithm has a time complexity of O(n log(n)).
     */
    public static int lengthOfLIS(long[] a, int n) {
        assert n <= a.length;
        if (n <= 1) return n;
        long[] tail = new long[n];
        int length = 1;
        tail[0] = a[0];
        for (int i = 1; i < n; i++) {
            long v = a[i];
            int pos = upperBound(tail, 0, length, v);
            if (pos == length) tail[length++] = v;
            else tail[pos] = v;
        }
        return length;
    }

    /**
     * Returns the length of the longest increasing subsequence.
     * This algorithm has a time complexity of O(n log(n)).
     *
     * @param strict strictly increasing or not
     */
    public static int lengthOfLIS(long[] a, int n, boolean strict) {
        if (!strict) return lengthOfLIS(a, n);
        // strict version only changes 'upperBound' to 'lowerBound'
        assert n <= a.length;
        if (n <= 1) return n;
        long[] tail = new long[n];
        int length = 1;
        tail[0] = a[0];
        for (int i = 1; i < n; i++) {
            long v = a[i];
            int pos = lowerBound(tail, 0, length, v);
            if (pos == length) tail[length++] = v;
            else tail[pos] = v;
        }
        return length;
    }

    private FMath() {
    }
}
