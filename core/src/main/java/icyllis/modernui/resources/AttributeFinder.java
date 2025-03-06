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
import icyllis.modernui.annotation.Nullable;

/**
 * Provides linear search for sorted needle array (attributes we're looking for)
 * in the sorted haystack array (the attributes we're searching through).
 */
public abstract class AttributeFinder {

    protected String haystackNamespace;
    protected String haystackAttribute;

    private int start;
    private int end;
    private int current;

    public AttributeFinder() {
    }

    /**
     * Override to update {@link #haystackNamespace} and {@link #haystackAttribute}.
     */
    protected abstract void onGetAttribute(int index);

    /**
     * Called before the search starts.
     */
    public void reset(int start, int end) {
        this.start = start;
        this.end = end;
        current = start;
        if (start < end) {
            onGetAttribute(start);
        }
    }

    /**
     * Called after the search ends.
     */
    public void clear() {
        haystackNamespace = null;
        haystackAttribute = null;
    }

    // Returns the index or '-1' if not found
    public int find(@NonNull String namespace, @NonNull String attribute) {
        if (start >= end) {
            return -1;
        }

        assert haystackNamespace != null;
        assert haystackAttribute != null;

        while (current != end) {
            int compare = ResourceId.comparePair(haystackNamespace, haystackAttribute,
                    namespace, attribute);
            if (compare > 0) {
                // The attribute we are looking was not found.
                break;
            }

            ++current;
            if (current != end) {
                onGetAttribute(current);
            }

            if (compare == 0) {
                // We found the attribute we were looking for.
                return current - 1;
            }
        }

        return -1;
    }
}

class BagAttributeFinder extends AttributeFinder {

    private String[] keys;

    public BagAttributeFinder() {
    }

    public void reset(@Nullable AssetManager.ResolvedBag bag) {
        this.keys = bag != null ? bag.keys : null;
        super.reset(0, bag != null ? bag.getEntryCount() : 0);
    }

    public void clear() {
        super.clear();
        keys = null;
    }

    @Override
    protected void onGetAttribute(int index) {
        int ii = index<<1;
        haystackNamespace = keys[ii];
        haystackAttribute = keys[ii+1];
    }
}
