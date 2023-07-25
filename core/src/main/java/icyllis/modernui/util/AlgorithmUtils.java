/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.util;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.Comparator;

public final class AlgorithmUtils {

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

    // lower   : <  (C++ lower_bound - 1)
    // floor   : <= (C++ upper_bound - 1)
    // ceil    : >= (C++ lower_bound)
    // higher  : >  (C++ upper_bound)

    /**
     * Returns an index of the last element in the range {@code [0, a.length)}
     * such that {@code a[index] < value}, or {@code -1} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code -1}
     * @see java.util.Arrays#sort(int[])
     */
    public static int lower(@NonNull int[] a, int value) {
        return lower(a, 0, a.length, value);
    }

    /**
     * Returns an index of the last element in the range {@code [0, a.length)}
     * such that {@code a[index] < value}, or {@code first - 1} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param first the index of the first element (inclusive) to be searched
     * @param last  the index of the last element (exclusive) to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code first - 1}
     * @see java.util.Arrays#sort(int[], int, int)
     */
    @Contract(pure = true)
    public static int lower(@NonNull int[] a, int first, int last, int value) {
        assert (first | last - first | a.length - last) >= 0;
        int low = first, high = last - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (a[mid] < value) low = mid + 1;
            else high = mid - 1;
        }
        return high;
    }

    /**
     * Returns an index of the last element in the range {@code [0, a.length)}
     * such that {@code a[index] < value}, or {@code -1} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code -1}
     * @see java.util.Arrays#sort(long[])
     */
    @Contract(pure = true)
    public static int lower(@NonNull long[] a, long value) {
        return lower(a, 0, a.length, value);
    }

    /**
     * Returns an index of the last element in the range {@code [0, a.length)}
     * such that {@code a[index] < value}, or {@code first - 1} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param first the index of the first element (inclusive) to be searched
     * @param last  the index of the last element (exclusive) to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code first - 1}
     * @see java.util.Arrays#sort(long[], int, int)
     */
    @Contract(pure = true)
    public static int lower(@NonNull long[] a, int first, int last, long value) {
        assert (first | last - first | a.length - last) >= 0;
        int low = first, high = last - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (a[mid] < value) low = mid + 1;
            else high = mid - 1;
        }
        return high;
    }

    /**
     * Returns an index of the last element in the range {@code [0, a.length)}
     * such that {@code a[index] < value}, or {@code -1} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @param c     the comparator by which the array is ordered.  A
     *              {@code null} value indicates that the elements'
     *              {@linkplain Comparable natural ordering} should be used.
     * @return index of the search value, or {@code -1}
     * @see java.util.Arrays#sort(Object[], Comparator)
     */
    public static <T> int lower(@NonNull T[] a, T value, @Nullable Comparator<? super T> c) {
        return lower(a, 0, a.length, value, c);
    }

    /**
     * Returns an index of the last element in the range {@code [0, a.length)}
     * such that {@code a[index] < value}, or {@code first - 1} if no such element is
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
     * @return index of the search value, or {@code first - 1}
     * @see java.util.Arrays#sort(Object[], int, int, Comparator)
     */
    @SuppressWarnings("unchecked")
    public static <T> int lower(@NonNull T[] a, int first, int last, T value,
                                @Nullable Comparator<? super T> c) {
        assert (first | last - first | a.length - last) >= 0;
        if (c == null) c = (Comparator<? super T>) Comparator.naturalOrder();
        int low = first, high = last - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (c.compare(a[mid], value) < 0) high = mid - 1;
            else low = mid + 1;
        }
        return high;
    }

    /**
     * Returns an index of the last element in the range {@code [0, a.length)}
     * such that {@code a[index] <= value}, or {@code -1} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code -1}
     * @see java.util.Arrays#sort(int[])
     */
    public static int floor(@NonNull int[] a, int value) {
        return floor(a, 0, a.length, value);
    }

    /**
     * Returns an index of the last element in the range {@code [first, last)}
     * such that {@code a[index] <= value}, or {@code first - 1} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param first the index of the first element (inclusive) to be searched
     * @param last  the index of the last element (exclusive) to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code first - 1}
     * @see java.util.Arrays#sort(int[], int, int)
     */
    @Contract(pure = true)
    public static int floor(@NonNull int[] a, int first, int last, int value) {
        assert (first | last - first | a.length - last) >= 0;
        int low = first, high = last - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (a[mid] > value) high = mid - 1;
            else low = mid + 1;
        }
        return high;
    }

    /**
     * Returns an index of the last element in the range {@code [0, a.length)}
     * such that {@code a[index] <= value}, or {@code -1} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code -1}
     * @see java.util.Arrays#sort(long[])
     */
    @Contract(pure = true)
    public static int floor(@NonNull long[] a, long value) {
        return floor(a, 0, a.length, value);
    }

    /**
     * Returns an index of the last element in the range {@code [first, last)}
     * such that {@code a[index] <= value}, or {@code first - 1} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param first the index of the first element (inclusive) to be searched
     * @param last  the index of the last element (exclusive) to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code first - 1}
     * @see java.util.Arrays#sort(long[], int, int)
     */
    @Contract(pure = true)
    public static int floor(@NonNull long[] a, int first, int last, long value) {
        assert (first | last - first | a.length - last) >= 0;
        int low = first, high = last - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (a[mid] > value) high = mid - 1;
            else low = mid + 1;
        }
        return high;
    }

    /**
     * Returns an index of the last element in the range {@code [0, a.length)}
     * such that {@code a[index] <= value}, or {@code -1} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @param c     the comparator by which the array is ordered.  A
     *              {@code null} value indicates that the elements'
     *              {@linkplain Comparable natural ordering} should be used.
     * @return index of the search value, or {@code -1}
     * @see java.util.Arrays#sort(Object[], Comparator)
     */
    public static <T> int floor(@NonNull T[] a, T value, @Nullable Comparator<? super T> c) {
        return floor(a, 0, a.length, value, c);
    }

    /**
     * Returns an index of the last element in the range {@code [first, last)}
     * such that {@code a[index] <= value}, or {@code first - 1} if no such element is
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
     * @return index of the search value, or {@code first - 1}
     * @see java.util.Arrays#sort(Object[], int, int, Comparator)
     */
    @SuppressWarnings("unchecked")
    public static <T> int floor(@NonNull T[] a, int first, int last, T value,
                                @Nullable Comparator<? super T> c) {
        assert (first | last - first | a.length - last) >= 0;
        if (c == null) c = (Comparator<? super T>) Comparator.naturalOrder();
        int low = first, high = last - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (c.compare(a[mid], value) > 0) high = mid - 1;
            else low = mid + 1;
        }
        return high;
    }

    /**
     * Returns an index of the first element in the range {@code [0, a.length)}
     * such that {@code a[index] >= value}, or {@code a.length} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     * <p>
     * Equivalent to C++ {@code lower_bound}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code a.length}
     * @see java.util.Arrays#sort(int[])
     */
    public static int ceil(@NonNull int[] a, int value) {
        return ceil(a, 0, a.length, value);
    }

    /**
     * Returns an index of the first element in the range {@code [first, last)}
     * such that {@code a[index] >= value}, or {@code last} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     * <p>
     * Equivalent to C++ {@code lower_bound}.
     *
     * @param a     the array to be searched
     * @param first the index of the first element (inclusive) to be searched
     * @param last  the index of the last element (exclusive) to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code last}
     * @see java.util.Arrays#sort(int[], int, int)
     */
    @Contract(pure = true)
    public static int ceil(@NonNull int[] a, int first, int last, int value) {
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
     * such that {@code a[index] >= value}, or {@code a.length} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     * <p>
     * Equivalent to C++ {@code lower_bound}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code a.length}
     * @see java.util.Arrays#sort(long[])
     */
    @Contract(pure = true)
    public static int ceil(@NonNull long[] a, long value) {
        return ceil(a, 0, a.length, value);
    }

    /**
     * Returns an index of the first element in the range {@code [first, last)}
     * such that {@code a[index] >= value}, or {@code last} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     * <p>
     * Equivalent to C++ {@code lower_bound}.
     *
     * @param a     the array to be searched
     * @param first the index of the first element (inclusive) to be searched
     * @param last  the index of the last element (exclusive) to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code last}
     * @see java.util.Arrays#sort(long[], int, int)
     */
    @Contract(pure = true)
    public static int ceil(@NonNull long[] a, int first, int last, long value) {
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
     * such that {@code a[index] >= value}, or {@code a.length} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     * <p>
     * Equivalent to C++ {@code lower_bound}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @param c     the comparator by which the array is ordered.  A
     *              {@code null} value indicates that the elements'
     *              {@linkplain Comparable natural ordering} should be used.
     * @return index of the search value, or {@code a.length}
     * @see java.util.Arrays#sort(Object[], Comparator)
     */
    public static <T> int ceil(@NonNull T[] a, T value, @Nullable Comparator<? super T> c) {
        return ceil(a, 0, a.length, value, c);
    }

    /**
     * Returns an index of the first element in the range {@code [first, last)}
     * such that {@code a[index] >= value}, or {@code last} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     * <p>
     * Equivalent to C++ {@code lower_bound}.
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
    public static <T> int ceil(@NonNull T[] a, int first, int last, T value,
                               @Nullable Comparator<? super T> c) {
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
     * Returns an index of the first element in the range {@code [0, a.length)}
     * such that {@code a[index] > value}, or {@code a.length} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     * <p>
     * Equivalent to C++ {@code upper_bound}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code a.length}
     * @see java.util.Arrays#sort(int[])
     */
    public static int higher(@NonNull int[] a, int value) {
        return higher(a, 0, a.length, value);
    }

    /**
     * Returns an index of the first element in the range {@code [first, last)}
     * such that {@code a[index] > value}, or {@code last} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     * <p>
     * Equivalent to C++ {@code upper_bound}.
     *
     * @param a     the array to be searched
     * @param first the index of the first element (inclusive) to be searched
     * @param last  the index of the last element (exclusive) to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code last}
     * @see java.util.Arrays#sort(int[], int, int)
     */
    @Contract(pure = true)
    public static int higher(@NonNull int[] a, int first, int last, int value) {
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
     * <p>
     * Equivalent to C++ {@code upper_bound}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code a.length}
     * @see java.util.Arrays#sort(long[])
     */
    public static int higher(@NonNull long[] a, long value) {
        return higher(a, 0, a.length, value);
    }

    /**
     * Returns an index of the first element in the range {@code [first, last)}
     * such that {@code a[index] > value}, or {@code last} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     * <p>
     * Equivalent to C++ {@code upper_bound}.
     *
     * @param a     the array to be searched
     * @param first the index of the first element (inclusive) to be searched
     * @param last  the index of the last element (exclusive) to be searched
     * @param value the value to be searched for
     * @return index of the search value, or {@code last}
     * @see java.util.Arrays#sort(long[], int, int)
     */
    public static int higher(@NonNull long[] a, int first, int last, long value) {
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
     * <p>
     * Equivalent to C++ {@code upper_bound}.
     *
     * @param a     the array to be searched
     * @param value the value to be searched for
     * @param c     the comparator by which the array is ordered.  A
     *              {@code null} value indicates that the elements'
     *              {@linkplain Comparable natural ordering} should be used.
     * @return index of the search value, or {@code a.length}
     * @see java.util.Arrays#sort(Object[], Comparator)
     */
    public static <T> int higher(@NonNull T[] a, T value, @Nullable Comparator<? super T> c) {
        return higher(a, 0, a.length, value, c);
    }

    /**
     * Returns an index of the first element in the range {@code [first, last)}
     * such that {@code a[index] > value}, or {@code last} if no such element is
     * found. The elements in the range shall already be sorted or at least
     * partitioned with respect to {@code value}.
     * <p>
     * Equivalent to C++ {@code upper_bound}.
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
    public static <T> int higher(@NonNull T[] a, int first, int last, T value,
                                 @Nullable Comparator<? super T> c) {
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
     * Returns the length of the longest increasing subsequence (non-strictly).
     * This algorithm has a time complexity of O(n log(n)).
     */
    @Contract(pure = true)
    public static int lengthOfLIS(@NonNull int[] a, int n) {
        assert n <= a.length;
        if (n <= 1) return n;
        int[] tail = new int[n];
        int length = 1;
        tail[0] = a[0];
        for (int i = 1; i < n; i++) {
            int v = a[i], pos = higher(tail, 0, length, v);
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
    @Contract(pure = true)
    public static int lengthOfLIS(@NonNull int[] a, int n, boolean strict) {
        if (!strict) return lengthOfLIS(a, n);
        // strict version only changes '>' to '>='
        assert n <= a.length;
        if (n <= 1) return n;
        int[] tail = new int[n];
        int length = 1;
        tail[0] = a[0];
        for (int i = 1; i < n; i++) {
            int v = a[i], pos = ceil(tail, 0, length, v);
            if (pos == length) tail[length++] = v;
            else tail[pos] = v;
        }
        return length;
    }

    /**
     * Returns the length of the longest increasing subsequence (non-strictly).
     * This algorithm has a time complexity of O(n log(n)).
     */
    @Contract(pure = true)
    public static int lengthOfLIS(@NonNull long[] a, int n) {
        assert n <= a.length;
        if (n <= 1) return n;
        long[] tail = new long[n];
        int length = 1;
        tail[0] = a[0];
        for (int i = 1; i < n; i++) {
            long v = a[i];
            int pos = higher(tail, 0, length, v);
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
    @Contract(pure = true)
    public static int lengthOfLIS(@NonNull long[] a, int n, boolean strict) {
        if (!strict) return lengthOfLIS(a, n);
        // strict version only changes '>' to '>='
        assert n <= a.length;
        if (n <= 1) return n;
        long[] tail = new long[n];
        int length = 1;
        tail[0] = a[0];
        for (int i = 1; i < n; i++) {
            long v = a[i];
            int pos = ceil(tail, 0, length, v);
            if (pos == length) tail[length++] = v;
            else tail[pos] = v;
        }
        return length;
    }

    /**
     * Calculate arithmetic mean without intermediate overflow or underflow.
     */
    public static double averageStable(@NonNull double[] a) {
        double r = 0;
        for (int i = 0, e = a.length; i < e; )
            r += (a[i] - r) / ++i;
        return r;
    }

    /**
     * Calculate arithmetic mean without intermediate overflow or underflow.
     */
    public static double averageStable(@NonNull double[] a, int start, int limit) {
        double r = 0, t = 0;
        for (int i = start; i < limit; )
            r += (a[i++] - r) / ++t;
        return r;
    }

    private AlgorithmUtils() {
    }
}
