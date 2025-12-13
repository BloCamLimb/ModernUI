/*
 * Modern UI.
 * Copyright (C) 2023-2025 BloCamLimb. All rights reserved.
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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.util.Comparator;

/**
 * This class provides utilities based on binary search for custom containers.
 * None of the methods perform explicit bounds checking.
 *
 * <pre>
 * lower   : &lt; (C++ lower_bound - 1)
 * floor   : &le; (C++ upper_bound - 1)
 * ceiling : &ge; (C++ lower_bound)
 * higher  : &gt; (C++ upper_bound)
 * </pre>
 */
@ApiStatus.Experimental
public class ContainerHelpers {

    /**
     * Same as {@link java.util.Arrays#binarySearch(int[], int, int, int)}.
     *
     * @param a     the array to be searched
     * @param first the index of the first element (inclusive) to be searched
     * @param last  the index of the last element (exclusive) to be searched
     * @param value the value to be searched for
     */
    public static int binarySearch(@NonNull int[] a, int first, int last, int value) {
        assert (first | last - first | a.length - last) >= 0;
        int low = first, high = last - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = a[mid];
            if (midVal < value) low = mid + 1;
            else if (midVal > value) high = mid - 1;
            else return mid;
        }
        return ~low;
    }

    /**
     * Same as {@link java.util.Arrays#binarySearch(long[], int, int, long)}.
     *
     * @param a     the array to be searched
     * @param first the index of the first element (inclusive) to be searched
     * @param last  the index of the last element (exclusive) to be searched
     * @param value the value to be searched for
     */
    public static int binarySearch(@NonNull long[] a, int first, int last, long value) {
        assert (first | last - first | a.length - last) >= 0;
        int low = first, high = last - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = a[mid];
            if (midVal < value) low = mid + 1;
            else if (midVal > value) high = mid - 1;
            else return mid;
        }
        return ~low;
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
    public static int ceiling(@NonNull int[] a, int value) {
        return ceiling(a, 0, a.length, value);
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
    public static int ceiling(@NonNull int[] a, int first, int last, int value) {
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
    public static int ceiling(@NonNull long[] a, long value) {
        return ceiling(a, 0, a.length, value);
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
    public static int ceiling(@NonNull long[] a, int first, int last, long value) {
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
    public static <T> int ceiling(@NonNull T[] a, T value, @Nullable Comparator<? super T> c) {
        return ceiling(a, 0, a.length, value, c);
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
    public static <T> int ceiling(@NonNull T[] a, int first, int last, T value,
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
            int v = a[i], pos = ceiling(tail, 0, length, v);
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
            int pos = ceiling(tail, 0, length, v);
            if (pos == length) tail[length++] = v;
            else tail[pos] = v;
        }
        return length;
    }

    protected ContainerHelpers() {
        throw new UnsupportedOperationException();
    }
}
