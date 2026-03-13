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

import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.resources.ResourceTypes.*;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;

public class LoadedPackage {

    public static class TypeSpec {

        // Pointer to a ResTable_typeSpec struct.
        public int typeSpec;

        public byte id;
        public short typeCount;
        public int entryCount;
        public int entriesStart;

        // Map key string index into entry index
        public Object2IntOpenHashMap<String> entryIndexTable;

        public ByteBuffer flags;

        public final TypeEntry[] typeEntries;

        public TypeSpec(long typeSpec, List<TypeEntry> typeEntries) {
            this.typeEntries = typeEntries.toArray(new TypeEntry[0]);
        }

        public int getEntryIndexByName(String entryName) {
            return entryIndexTable.getInt(entryName);
        }

        public int getFlagsForEntryIndex(int entryIndex) {
            if (entryIndex < 0 || entryIndex >= entryCount) {
                return 0;
            }
            return flags.getInt(entryIndex);
        }
    }

    public static class TypeEntry {

        // Pointer to a ResTable_type struct.
        public int type;

        public byte id;
        public byte flags;
        public int entryCount;
        public int entriesStart;

        // remaining data (without header) of the type chunk
        public ByteBuffer data;

        public TypeEntry() {
        }
    }

    public String packageName;

    public LoadedPackage(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    public TypeSpec getTypeSpecByName(String typeName) {
        return typeSpecs.get(typeName);
    }

    HashMap<String, TypeSpec> typeSpecs;

    // Returns byte offset to entriesStart, or ResTable_type.NO_ENTRY if not found.
    public int getEntryOffset(TypeEntry typeChunk, int entryIndex) {
        int entryCount = typeChunk.entryCount;

        if ((typeChunk.flags & (ResTable_type.FLAG_SPARSE | ResTable_type.FLAG_OFFSET16)) != 0) {
            // not implemented yet
            return ResTable_type.NO_ENTRY;
        }

        /*if ((ResTable_type.flags(typeChunk) & ResTable_type.FLAG_SPARSE) != 0) {
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
        }*/

        // This type is encoded as a dense array.
        if (entryIndex < 0 || entryIndex >= entryCount) {
            return ResTable_type.NO_ENTRY;
        }

        return typeChunk.data.getInt(entryIndex * 4);
    }

    // Returns memory view to ResTable_entry struct.
    public ByteBuffer getEntryFromOffset(TypeEntry typeChunk, int entryOffset) {

        if (!MathUtil.isAlign4(entryOffset)) {
            return null;
        }

        entryOffset += typeChunk.entriesStart - ResTable_type.SIZEOF;
        if (entryOffset < 0 || entryOffset > typeChunk.data.limit() - ResTable_entry.SIZEOF) {
            return null;
        }

        int entrySize = typeChunk.data.getShort(entryOffset + ResTable_entry.size) & 0xFFFF;

        if (entrySize < ResTable_entry.SIZEOF || entryOffset > typeChunk.data.limit() - entrySize) {
            return null;
        }

        int entryFlags = typeChunk.data.getShort(entryOffset + ResTable_entry.flags) & 0xFFFF;
        if ((entryFlags & ResTable_entry.FLAG_COMPLEX) == 0) {
            return typeChunk.data.slice(entryOffset, entrySize)
                    .order(ByteOrder.nativeOrder());
        }

        if (entrySize < ResTable_entry.SIZEOF_EXT) {
            return null;
        }

        int mapEntryCount = typeChunk.data.getInt(entryOffset + ResTable_entry.count);
        int mapEntriesStart = entryOffset + entrySize;
        if (!MathUtil.isAlign4(mapEntriesStart)) {
            return null;
        }

        if (mapEntryCount > (typeChunk.data.limit() - mapEntriesStart) / ResTable_map.SIZEOF) {
            return null;
        }

        return typeChunk.data.slice(entryOffset, entrySize + mapEntryCount * ResTable_map.SIZEOF)
                .order(ByteOrder.nativeOrder());
    }
}
