/*
 * ModernUI.
 * Copyright (C) 2025-2026 BloCamLimb. All rights reserved.
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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;

/**
 * Provides linear search for sorted needle array (attributes we're looking for)
 * in the sorted haystack array (the attributes we're searching through).
 */
public class AttributeFinder {

    private String[] keys;
    private String haystack;

    private int start;
    private int end;
    private int current;

    public AttributeFinder() {
    }

    public void reset(String[] keys) {
        reset(keys, 0, keys != null ? keys.length : 0);
    }

    /**
     * Called before the search starts.
     */
    public void reset(String[] keys, int start, int end) {
        assert start >= 0 && end >= 0 && start <= end;
        this.keys = keys;
        this.start = start;
        this.end = end;
        current = start;
        if (start < end) {
            haystack = keys[current];
        }
    }

    /**
     * Called after the search ends.
     */
    public void clear() {
        haystack = null;
        keys = null;
        end = 0;
    }

    // Returns the index or '-1' if not found
    public int find(@NonNull String needle) {
        if (start >= end) {
            return -1;
        }

        assert haystack != null;

        while (current < end) {
            int compare = haystack.compareTo(needle);
            if (compare > 0) {
                // The attribute we are looking was not found.
                break;
            }

            ++current;
            if (current < end) {
                haystack = keys[current];
            }

            if (compare == 0) {
                // We found the attribute we were looking for.
                return current - 1;
            }
        }

        return -1;
    }
}
