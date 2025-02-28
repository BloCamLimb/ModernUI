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

import icyllis.modernui.resources.ResourceTypes.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.lwjgl.system.MemoryUtil;

import java.util.List;

public class LoadedPackage {

    public static class TypeSpec {
        public static class TypeEntry {
            // Pointer to a ResTable_type struct.
            public final long type;

            // Type configurations are accessed frequently when setting up an AssetManager and querying
            // resources. Access this cached configuration to minimize page faults.
            public final ResTable_config config;

            public TypeEntry(long type) {
                this.config = new ResTable_config(ResTable_type.pConfig(type));
                this.type = type;
            }
        }

        // Pointer to a ResTable_typeSpec struct.
        public final long typeSpec;

        public final TypeEntry[] typeEntries;

        public TypeSpec(long typeSpec, List<TypeEntry> typeEntries) {
            this.typeEntries = typeEntries.toArray(new TypeEntry[0]);
            this.typeSpec = typeSpec;
        }
    }

    public ResStringTable getTypeStringTable() {
        return typeStringTable;
    }

    public ResStringTable getKeyStringTable() {
        return keyStringTable;
    }

    public TypeSpec getTypeSpecByTypeIndex(int typeIndex) {
        return typeSpecs.get(typeIndex + 1);
    }

    ResStringTable typeStringTable;
    ResStringTable keyStringTable;

    Int2ObjectOpenHashMap<TypeSpec> typeSpecs;

    // Returns byte offset to entriesStart, or ResTable_type.NO_ENTRY if not found.
    public int getEntryOffset(long typeChunk, int entryIndex) {
        int entryCount = ResTable_type.entryCount(typeChunk);
        long offsets = typeChunk + ResTable_type.headerSize(typeChunk);

        if ((ResTable_type.flags(typeChunk) & ResTable_type.FLAG_SPARSE) != 0) {
            // This is encoded as a sparse map, so perform a binary search.
            int low = 0, high = entryCount - 1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                long entry = offsets + (long) mid * ResTable_sparseTypeEntry.SIZEOF;
                if (ResTable_sparseTypeEntry.index(entry) < entryIndex) low = mid + 1;
                else high = mid - 1;
            }
            if (low == entryCount) {
                // No entry found.
                return ResTable_type.NO_ENTRY;
            }

            long entry = offsets + (long) low * ResTable_sparseTypeEntry.SIZEOF;
            if (ResTable_sparseTypeEntry.index(entry) != entryIndex) {
                return ResTable_type.NO_ENTRY;
            }

            return ResTable_sparseTypeEntry.offset(entry);
        }

        // This type is encoded as a dense array.
        if (entryIndex >= entryCount) {
            return ResTable_type.NO_ENTRY;
        }

        //TODO validation of unsafe op

        return MemoryUtil.memGetInt(offsets + (long) entryIndex * 4);
    }

    // Returns pointer to ResTable_entry struct.
    public long getEntryFromOffset(long typeChunk, int entryOffset) {
        //TODO validation of unsafe op
        entryOffset += ResTable_type.entriesStart(typeChunk);
        long entry = typeChunk + entryOffset;
        return entry;
    }
}
