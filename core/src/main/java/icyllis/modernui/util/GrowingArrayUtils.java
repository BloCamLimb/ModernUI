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

package icyllis.modernui.util;

import javax.annotation.Nonnull;
import java.lang.reflect.Array;

/**
 * A helper class that aims to provide comparable growth performance to ArrayList, but on primitive
 * arrays. Common array operations are implemented for efficient use in dynamic containers.
 * <p>
 * All methods in this class assume that the length of an array is equivalent to its capacity and
 * NOT the number of elements in the array. The current size of the array is always passed in as a
 * parameter.
 */
public final class GrowingArrayUtils {

    private GrowingArrayUtils() {
    }

    /**
     * Given the current size of an array, returns an ideal size to which the array should grow.
     */
    public static int growSize(int currentSize) {
        return currentSize <= 1 ? 2 : currentSize + (currentSize >> 1);
    }

    /**
     * Appends an element to the end of the array, growing the array if there is no more room.
     *
     * @param array       The array to which to append the element. This must NOT be null.
     * @param currentSize The number of elements in the array. Must be less than or equal to
     *                    array.length.
     * @param element     The element to append.
     * @return the array to which the element was appended. This may be different than the given
     * array.
     */
    @Nonnull
    public static <T> T[] append(@Nonnull T[] array, int currentSize, T element) {
        assert currentSize <= array.length;

        if (currentSize + 1 > array.length) {
            Class<? extends Object[]> newType = array.getClass();
            int newLength = GrowingArrayUtils.growSize(currentSize);
            @SuppressWarnings("unchecked")
            T[] newArray = newType == Object[].class
                    ? (T[]) new Object[newLength]
                    : (T[]) Array.newInstance(newType.getComponentType(), newLength);
            System.arraycopy(array, 0, newArray, 0, currentSize);
            array = newArray;
        }
        array[currentSize] = element;
        return array;
    }

    /**
     * Primitive int version of {@link #append(Object[], int, Object)}.
     */
    @Nonnull
    public static int[] append(@Nonnull int[] array, int currentSize, int element) {
        assert currentSize <= array.length;

        if (currentSize + 1 > array.length) {
            int[] newArray = new int[growSize(currentSize)];
            System.arraycopy(array, 0, newArray, 0, currentSize);
            array = newArray;
        }
        array[currentSize] = element;
        return array;
    }

    /**
     * Inserts an element into the array at the specified index, growing the array if there is no
     * more room.
     *
     * @param array       The array to which to append the element. Must NOT be null.
     * @param currentSize The number of elements in the array. Must be less than or equal to
     *                    array.length.
     * @param element     The element to insert.
     * @return the array to which the element was appended. This may be different than the given
     * array.
     */
    @Nonnull
    public static <T> T[] insert(@Nonnull T[] array, int currentSize, int index, T element) {
        assert currentSize <= array.length;

        if (currentSize + 1 <= array.length) {
            System.arraycopy(array, index, array, index + 1, currentSize - index);
            array[index] = element;
            return array;
        }

        Class<? extends Object[]> newType = array.getClass();
        int newLength = GrowingArrayUtils.growSize(currentSize);
        @SuppressWarnings("unchecked")
        T[] newArray = newType == Object[].class
                ? (T[]) new Object[newLength]
                : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(array, 0, newArray, 0, index);
        newArray[index] = element;
        System.arraycopy(array, index, newArray, index + 1, array.length - index);
        return newArray;
    }

    /**
     * Primitive int version of {@link #insert(Object[], int, int, Object)}.
     */
    @Nonnull
    public static int[] insert(@Nonnull int[] array, int currentSize, int index, int element) {
        assert currentSize <= array.length;

        if (currentSize + 1 <= array.length) {
            System.arraycopy(array, index, array, index + 1, currentSize - index);
            array[index] = element;
            return array;
        }

        int[] newArray = new int[growSize(currentSize)];
        System.arraycopy(array, 0, newArray, 0, index);
        newArray[index] = element;
        System.arraycopy(array, index, newArray, index + 1, array.length - index);
        return newArray;
    }
}
