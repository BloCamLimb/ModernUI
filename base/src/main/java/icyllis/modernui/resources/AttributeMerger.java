/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.resources;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Incrementally merges a sorted "parent" key array with a stream of sorted "child" keys,
 * optionally carrying a fixed number of {@code int} payload columns per key.
 *
 * <p>Usage:
 * <ol>
 *   <li>Call one of the {@code reset} methods, declaring how many child keys ({@code n}) will be appended.</li>
 *   <li>Call {@code append} exactly {@code n} times, with keys in non-decreasing order.</li>
 *   <li>Call {@link #getResultKeys()} and, if payloads are used, {@link #getResultValues()}.</li>
 * </ol>
 *
 * <p>Merge semantics:
 * <ul>
 *   <li>The parent range must be sorted ascending and free of duplicates, as is the case for any
 *       result produced by this class. This precondition is not verified.</li>
 *   <li>The result is sorted ascending by {@link String#compareTo(String)} and free of duplicates.</li>
 *   <li>If a child key equals a parent key, the child entry replaces the parent entry.</li>
 *   <li>If the same child key is appended more than once, the last appended payload wins.</li>
 * </ul>
 *
 * <p>Payloads are stored in a flattened row-major array: the payload of the entry at index
 * {@code r} occupies {@code [r * valueColumns, (r + 1) * valueColumns)}.
 *
 * <p>The merger is reusable through {@code reset} but is not thread-safe. The arrays returned
 * by the getters are the internal arrays and are not copied.
 */
public class AttributeMerger {

    /** Parent keys, sorted ascending. */
    private String[] keys;

    /** Exclusive end index of the parent range within {@link #keys}. */
    private int end;

    /** Flattened parent payloads, aligned with {@link #keys}, or {@code null} if unused. */
    private int[] values;

    /** Number of {@code int} payload columns per key; {@code 0} means no payload. */
    private int valueColumns;

    /** Expected number of {@code append} calls. */
    private int n;

    /** Merged keys; trimmed to the exact size when the merge is finalized. */
    private String[] resultKeys;

    /** Flattened merged payloads, or {@code null} if no payload is used. */
    private int[] resultValues;

    /** Index of the next unconsumed parent entry. */
    private int i;

    /** Number of entries written to the result so far. */
    private int k;

    /** Number of {@code append} calls made so far. */
    private int appended;

    /** Whether the merge has been finalized and the result is ready. */
    private boolean done;

    /**
     * Creates an empty merger. {@code reset} must be called before any other method.
     */
    public AttributeMerger() {
    }

    /**
     * Starts a merge over the whole parent array, without payloads.
     *
     * @param keys parent keys, sorted ascending without duplicates
     * @param n    number of child keys that will be appended
     * @throws IllegalArgumentException if {@code n} is negative
     */
    public void reset(@NonNull String @Nullable[] keys, int n) {
        reset(keys, 0, keys != null ? keys.length : 0, null, 0, n);
    }

    /**
     * Starts a merge over a range of the parent array, without payloads.
     *
     * @param keys  parent keys, sorted ascending without duplicates
     * @param start inclusive start index of the parent range
     * @param end   exclusive end index of the parent range
     * @param n     number of child keys that will be appended
     * @throws IllegalArgumentException if {@code n} is negative or the range is invalid
     */
    public void reset(@NonNull String @Nullable[] keys, int start, int end,
                      int n) {
        reset(keys, start, end, null, 0, n);
    }

    /**
     * Starts a merge over a range of the parent array, with payloads.
     *
     * @param keys         parent keys, sorted ascending without duplicates
     * @param start        inclusive start index of the parent range
     * @param end          exclusive end index of the parent range
     * @param values       flattened parent payloads aligned with {@code keys}
     *                     (entry {@code r} at {@code [r * valueColumns, (r + 1) * valueColumns)}),
     *                     or {@code null} if {@code valueColumns} is {@code 0}
     * @param valueColumns number of {@code int} payload columns per key, or {@code 0} for none
     * @param n            number of child keys that will be appended
     * @throws IllegalArgumentException if {@code n} or {@code valueColumns} is negative,
     *                                  the range is invalid, or {@code values} is missing or too short
     */
    public void reset(@NonNull String @Nullable[] keys, int start, int end,
                      int @Nullable[] values, int valueColumns, int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Count n must be non-negative: n=" + n);
        }
        if (valueColumns < 0) {
            throw new IllegalArgumentException(
                    "Value columns must be non-negative: valueColumns=" + valueColumns);
        }
        if (start < 0 || end < start || end > (keys != null ? keys.length : 0)) {
            throw new IllegalArgumentException("Invalid parent range: start=" + start
                    + ", end=" + end + ", keys.length=" + (keys != null ? keys.length : 0));
        }
        if (valueColumns > 0 && end > 0
                && (values == null || (long) values.length < (long) end * valueColumns)) {
            throw new IllegalArgumentException(
                    "Values array is missing or too short: valueColumns=" + valueColumns);
        }
        this.keys = keys;
        this.end = end;
        this.values = values;
        this.valueColumns = valueColumns;
        this.n = n;

        // Allocates the upper bound: every parent entry plus every child entry.
        int total = Math.addExact(end - start, n);
        resultKeys = new String[total];
        if (valueColumns > 0) {
            resultValues = new int[Math.multiplyExact(total, valueColumns)];
        } else {
            resultValues = null;
        }

        i = start;
        k = 0;
        appended = 0;
        done = false;
    }

    /**
     * Appends a child key without payload. Only valid when {@code valueColumns} is {@code 0}.
     *
     * @param key the child key; must not be smaller than the previously appended key
     * @return index of the key in the result array
     * @throws IllegalStateException    if {@code reset} was not called or more than {@code n} appends were made
     * @throws IllegalArgumentException if payloads are in use or the key is out of order
     */
    public int append(@NonNull String key) {
        return append(key, (int[]) null);
    }

    /**
     * Appends a child key with its payload.
     *
     * <p>All parent entries smaller than {@code key} are first moved to the result, and a parent
     * entry equal to {@code key} is dropped. If the previously appended key equals {@code key},
     * its payload is replaced instead of adding a new entry.
     *
     * @param key  the child key; must not be smaller than the previously appended key
     * @param vals payload of exactly {@code valueColumns} values; ignored when {@code valueColumns} is {@code 0}
     * @return index of the key in the result array; this index stays valid after later appends
     * @throws IllegalStateException    if {@code reset} was not called or more than {@code n} appends were made
     * @throws IllegalArgumentException if the key is out of order
     */
    public int append(@NonNull String key, int... vals) {
        if (appended >= n) {
            throw new IllegalStateException("Too many append calls: expected n=" + n);
        }

        while (i < end) {
            int cmp = keys[i].compareTo(key);
            if (cmp < 0) {
                resultKeys[k] = keys[i];
                if (valueColumns > 0) {
                    System.arraycopy(values, i * valueColumns,
                            resultValues, k * valueColumns, valueColumns);
                }
                i++;
                k++;
            } else if (cmp == 0) {
                // Drops the parent entry with the same key, since the child entry replaces it.
                i++;
            } else {
                break;
            }
        }
        if (k > 0) {
            int cmp = resultKeys[k - 1].compareTo(key);
            if (cmp > 0) {
                throw new IllegalArgumentException(
                        "Keys must be appended in non-decreasing order: previous='"
                                + resultKeys[k - 1] + "', current='" + key + "'");
            } else if (cmp == 0) {
                // Replaces the entry created by the previous append with the same key.
                if (valueColumns > 0) {
                    System.arraycopy(vals, 0, resultValues, (k - 1) * valueColumns, valueColumns);
                }
                appended++;
                return k - 1;
            }
        }
        // Adds a new entry for a key that is not in the result yet.
        resultKeys[k] = key;
        if (valueColumns > 0) {
            System.arraycopy(vals, 0, resultValues, k * valueColumns, valueColumns);
        }
        appended++;
        return k++;
    }

    private void flush() {
        if (done) return;

        if (appended < n) {
            throw new IllegalStateException("Too few append calls: expected n=" + n
                    + " but found " + appended);
        }

        int remaining = end - i;
        if (remaining > 0) {
            System.arraycopy(keys, i, resultKeys, k, remaining);
            if (valueColumns > 0) {
                System.arraycopy(values, i * valueColumns,
                        resultValues, k * valueColumns,
                        remaining * valueColumns);
            }
            k += remaining;
        }

        if (k < resultKeys.length) {
            // Trims the result arrays to the number of entries actually produced.
            resultKeys = Arrays.copyOf(resultKeys, k);
            if (valueColumns > 0) {
                resultValues = Arrays.copyOf(resultValues, k * valueColumns);
            }
        }

        done = true;
    }

    /**
     * Returns the merged keys in ascending order. Finalizes the merge on first call.
     *
     * @return the internal result array, not a copy
     * @throws IllegalStateException if {@code reset} was not called or fewer than {@code n} appends were made
     */
    public @NonNull String @NonNull[] getResultKeys() {
        flush();
        return resultKeys;
    }

    /**
     * Returns the merged payloads, flattened row-major and aligned with {@link #getResultKeys()}.
     * Finalizes the merge on first call.
     *
     * @return the internal result array, not a copy; {@code null} if no payload is used
     * @throws IllegalStateException if {@code reset} was not called or fewer than {@code n} appends were made
     */
    public int[] getResultValues() {
        flush();
        return resultValues;
    }
}
