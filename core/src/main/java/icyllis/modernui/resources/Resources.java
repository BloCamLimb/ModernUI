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

package icyllis.modernui.resources;

import icyllis.modernui.ModernUI;
import icyllis.modernui.util.DisplayMetrics;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;

@ApiStatus.Experimental
public class Resources {

    public static final Marker MARKER = MarkerManager.getMarker("Resources");

    private final DisplayMetrics mMetrics = new DisplayMetrics();

    public Resources() {
        mMetrics.setToDefaults();
    }

    @ApiStatus.Internal
    public void updateMetrics(DisplayMetrics metrics) {
        if (metrics != null) {
            mMetrics.setTo(metrics);
        }
    }

    public final Theme newTheme() {
        return new Theme();
    }

    public DisplayMetrics getDisplayMetrics() {
        return mMetrics;
    }

    @SuppressWarnings("ConstantValue")
    public class Theme {

        String[] mKeyStrings;
        Object[] mGlobalObjects;

        String[] mStyleKeys;
        int[] mStyleOffsets;

        static final int MAP_ENTRY_HEADER_COLUMNS = 2;
        static final int MAP_COLUMNS = 3;

        static final int MAP_ENTRY_PARENT = 0;
        static final int MAP_ENTRY_COUNT = 1;
        static final int MAP_ENTRY_ENTRIES = 2;

        static final int MAP_NAME = 0;
        static final int MAP_DATA_TYPE = 1;
        static final int MAP_DATA = 2;

        /*
            this is defined as:
        struct map_entry {
            int32_t parent; // index of keyStrings
            int32_t count;
            struct map {
                uint32_t name;
                uint32_t dataType;
                uint32_t data;
            } entries[count];
        } data[0];
         */
        int[] mData;

        Object2ObjectOpenHashMap<String, AssetManager.ResolvedBag> mCachedBags = new Object2ObjectOpenHashMap<>();

        AssetManager.ResolvedBag getBag(String style) {
            var cached = mCachedBags.get(style);
            if (cached != null) {
                return cached;
            }

            int entryIndex = Arrays.binarySearch(mStyleKeys, style);
            if (entryIndex < 0) {
                return null;
            }

            int offset = mStyleOffsets[entryIndex];
            int[] data = mData;

            int parentId = data[offset + MAP_ENTRY_PARENT];
            int entryCount = data[offset + MAP_ENTRY_COUNT];
            offset += MAP_ENTRY_HEADER_COLUMNS;
            if (parentId == -1) {
                // no parent
                AssetManager.ResolvedBag bag = new AssetManager.ResolvedBag();
                assert MAP_COLUMNS == AssetManager.ResolvedBag.ENTRY_COLUMNS;
                if (entryCount > 0) {
                    // TODO assert entries already sorted, assert data layouts are same
                    bag.mEntries = Arrays.copyOfRange(data, offset, offset + entryCount * MAP_COLUMNS);
                }
                mCachedBags.put(style, bag);
                return bag;
            }

            var parentBag = getBag(mKeyStrings[parentId]);
            if (parentBag == null) {
                ModernUI.LOGGER.error(MARKER, "Failed to find parent '{}' of bag '{}'",
                        mKeyStrings[parentId], style);
                return null;
            }

            AssetManager.ResolvedBag newBag = new AssetManager.ResolvedBag();
            int parentCount = parentBag.getEntryCount();
            int maxCount = parentCount + entryCount;
            if (maxCount > 0) {
                int[] newEntries = new int[maxCount * AssetManager.ResolvedBag.ENTRY_COLUMNS];
                int newOffset = 0;
                int[] parentData = parentBag.mEntries;
                int childIndex = 0;
                int childOffset = offset;
                int parentIndex = 0;
                int parentOffset = 0;

                while (childIndex < entryCount && parentIndex < parentCount) {
                    int childKey = data[childOffset + MAP_NAME];
                    int parentKey = parentData[parentOffset + MAP_NAME];

                    if (childKey <= parentKey) {
                        // Use the child key if it comes before the parent
                        // or is equal to the parent (overrides).
                        newEntries[newOffset + MAP_NAME] = childKey;
                        newEntries[newOffset + MAP_DATA_TYPE] = data[childOffset + MAP_DATA_TYPE];
                        newEntries[newOffset + MAP_DATA] = data[childOffset + MAP_DATA];
                        childIndex++;
                        childOffset += MAP_COLUMNS;
                    } else {
                        newEntries[newOffset + MAP_NAME] = parentKey;
                        newEntries[newOffset + MAP_DATA_TYPE] = parentData[parentOffset + MAP_DATA_TYPE];
                        newEntries[newOffset + MAP_DATA] = parentData[parentOffset + MAP_DATA];
                    }

                    assert newOffset == 0 || newEntries[newOffset + MAP_NAME] >= newEntries[newOffset - MAP_COLUMNS + MAP_NAME];

                    if (childKey >= parentKey) {
                        parentIndex++;
                        parentOffset += MAP_COLUMNS;
                    }
                    newOffset += MAP_COLUMNS;
                }

                while (childIndex < entryCount) {
                    int childKey = data[childOffset + MAP_NAME];
                    newEntries[newOffset + MAP_NAME] = childKey;
                    newEntries[newOffset + MAP_DATA_TYPE] = data[childOffset + MAP_DATA_TYPE];
                    newEntries[newOffset + MAP_DATA] = data[childOffset + MAP_DATA];
                    childIndex++;
                    childOffset += MAP_COLUMNS;
                    newOffset += MAP_COLUMNS;
                }

                if (parentIndex < parentCount) {
                    int numColumnsToCopy = (parentCount - parentIndex) * MAP_COLUMNS;
                    System.arraycopy(parentData, parentOffset, newEntries, newOffset, numColumnsToCopy);
                    newOffset += numColumnsToCopy;
                }

                assert newOffset <= newEntries.length;
                if (newOffset < newEntries.length) {
                    newEntries = Arrays.copyOf(newEntries, newOffset);
                }

                newBag.mEntries = newEntries;
            }
            mCachedBags.put(style, newBag);
            return newBag;
        }
    }
}
