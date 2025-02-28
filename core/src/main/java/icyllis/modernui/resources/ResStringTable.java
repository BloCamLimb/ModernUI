/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.resources;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.MathUtil;
import it.unimi.dsi.fastutil.HashCommon;

/**
 * Use an open-addressing and linear probing hash table to represent a set of strings,
 * for deduplication and mapping from string to index (not dense) and vice versa.
 * Although the index is not dense, the hash lookup is faster than binary search,
 * because string comparisons are not that fast.
 */
public class ResStringTable {

    private String[] key;
    private int mask;
    private int maxFill;
    private int size;

    // Fixed load factor
    private static final float LOAD_FACTOR = .75f;

    public ResStringTable() {
        this(16);
    }

    public ResStringTable(int expected) {
        int n = HashCommon.arraySize(expected, LOAD_FACTOR);
        mask = n - 1;
        maxFill = HashCommon.maxFill(n, LOAD_FACTOR);
        key = new String[n];
    }

    private ResStringTable(@NonNull String[] key, int size) {
        int n = key.length;
        mask = n - 1;
        maxFill = HashCommon.maxFill(n, LOAD_FACTOR);
        this.key = key;
        this.size = size;
    }

    @NonNull
    public static ResStringTable wrap(@NonNull String[] key, int size) {
        if (!MathUtil.isPow2(key.length)) {
            throw new IllegalArgumentException();
        }
        return new ResStringTable(key, size);
    }

    public String addOrGet(@NonNull String k) {
        int pos;
        String curr;
        final String[] key = this.key;
        // The starting point.
        if (!((curr = key[pos = (HashCommon.mix((k).hashCode())) & mask]) == null)) {
            if (((curr).equals(k))) return curr;
            while (!((curr = key[pos = (pos + 1) & mask]) == null)) if (((curr).equals(k))) return curr;
        }
        key[pos] = k;
        if (size++ >= maxFill) rehash(HashCommon.arraySize(size + 1, LOAD_FACTOR));
        return k;
    }

    public int indexOfString(@NonNull CharSequence k) {
        String curr;
        final String[] key = this.key;
        int pos;
        // The starting point.
        if (((curr = key[pos = (HashCommon.mix((k).hashCode())) & mask]) == null)) return -1;
        if (((k).equals(curr))) return pos;
        // There's always an unused entry.
        while (true) {
            if (((curr = key[pos = (pos + 1) & mask]) == null)) return -1;
            if (((k).equals(curr))) return pos;
        }
    }

    public String stringAt(int pos) {
        return key[pos];
    }

    private void rehash(int newN) {
        final String[] key = this.key;
        final int mask = newN - 1;
        final String[] newKey = new String[newN];
        int i = key.length, pos;
        for (int j = size; j-- != 0;) {
            while (((key[--i]) == null));
            if (!((newKey[pos = (HashCommon.mix((key[i]).hashCode())) & mask]) == null)) while (!((newKey[pos = (pos + 1) & mask]) == null));
            newKey[pos] = key[i];
        }
        this.mask = mask;
        maxFill = HashCommon.maxFill(newN, LOAD_FACTOR);
        this.key = newKey;
    }
}
