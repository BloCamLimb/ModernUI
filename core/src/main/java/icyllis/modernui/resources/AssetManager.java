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

public class AssetManager {

    public static class ResolvedBag {
        public static final int ENTRY_COLUMNS = 3;

        public int mTypeSpecFlags;
        /*
        struct Entry {
            uint32_t key;
            uint32_t dataType;
            uint32_t data;
        }
         */
        // sorted by key
        public int[] mEntries;

        public int getEntryCount() {
            return mEntries != null ? mEntries.length / ENTRY_COLUMNS : 0;
        }
    }
}
