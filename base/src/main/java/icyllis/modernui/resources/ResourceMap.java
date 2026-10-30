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

import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.resources.ResourceTypes.Res_value;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;

/**
 * Represents a loaded resources file.
 *
 * @hide
 * @hidden
 */
@ApiStatus.Internal
public class ResourceMap {

    ResStringPool globalStringPool;

    ResStringPool typeStringPool;
    ResStringPool keyStringPool;

    RuntimeFactory[] factoryPool;

    NamespaceSpec[] namespaceSpecs;

    final String[] namespaces = new String[256];//TODO use dynamic size

    public NamespaceSpec[] getNamespaceSpecs() {
        return namespaceSpecs;
    }

    /**
     * Returns a namespace that an integer id in this Table represents.
     * Or null if missing.
     *
     * @param id the res id to lookup
     * @return the namespace the id represents
     */
    @Nullable
    public String lookupNamespace(int id) {
        int packageId = id >>> Res_value.PACKAGE_ID_SHIFT;

        return namespaces[packageId];
    }

    @Nullable
    public ResourceId lookupParentId(ResourceId source, int id) {
        String namespace = lookupNamespace(id);
        if (namespace == null) {
            return null;
        }
        String entry = keyStringPool.getStringAt(id & Res_value.KEY_INDEX_MASK);
        if (entry == null) {
            return null;
        }
        return new ResourceId(namespace, source.type(), entry);
    }

    /**
     * Converts packed integer id to string-based resource id.
     * The integer id must come from this table.
     *
     * @param typeId type id if the res id is a reference, or 0 if they have the same type
     * @param id     the res id to lookup
     * @return the resource id the id represents
     */
    @Nullable
    public ResourceId lookupReferenceId(int typeId, int id) {
        String namespace = lookupNamespace(id);
        if (namespace == null) {
            return null;
        }
        String type = typeStringPool.getStringAt(typeId - 1);
        if (type == null) {
            return null;
        }
        String entry = keyStringPool.getStringAt(id & Res_value.KEY_INDEX_MASK);
        if (entry == null) {
            return null;
        }
        return new ResourceId(namespace, type, entry);
    }

    public ResStringPool getKeyStringPool() {
        return keyStringPool;
    }

    public ResStringPool getGlobalStringPool() {
        return globalStringPool;
    }

    public RuntimeFactory getFactory(int idx) {
        if (idx < 0 || idx >= factoryPool.length) {
            return null;
        }
        return factoryPool[idx];
    }

    public static class NamespaceSpec {

        public String namespace;
        public HashMap<String, TypeSpec> typeSpecs;

        public NamespaceSpec(String namespace) {
            this.namespace = namespace;
        }

        public String getNamespace() {
            return namespace;
        }

        public TypeSpec getTypeSpecByTypeName(String typeName) {
            return typeSpecs.get(typeName);
        }
    }

    public static class TypeSpec {

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
            return flags.getInt(entryIndex << 2);
        }
    }

    public static class TypeEntry {

        public byte id;
        public byte flags;
        public int entryCount;
        public int entriesStart;
        public ResConfig config;

        // remaining data (without header) of the type chunk
        public ByteBuffer data;

        public TypeEntry() {
        }
    }

    // Returns byte offset to entriesStart, or ResTable_type.NO_ENTRY if not found.
    public static int getEntryOffset(TypeEntry typeChunk, int entryIndex) {
        int entryCount = typeChunk.entryCount;

        if ((typeChunk.flags & (ResourceTypes.ResTable_type.FLAG_SPARSE | ResourceTypes.ResTable_type.FLAG_OFFSET16)) != 0) {
            // not implemented yet
            return ResourceTypes.ResTable_type.NO_ENTRY;
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
            return ResourceTypes.ResTable_type.NO_ENTRY;
        }

        return typeChunk.data.getInt(entryIndex * 4);
    }

    /**
     * Returns memory view to ResTable_entry struct.
     * <p>
     * If ResTable_entry is not complex, returns simple TypedValue.
     * If complex, returns a ByteBuffer slice. If invalid, returns null.
     */
    @Nullable
    public static Object getEntryFromOffset(@NonNull TypeEntry typeChunk, int entryOffset,
                                     @Nullable TypedValue outValue) {

        if (!MathUtil.isAlign4(entryOffset)) {
            return null;
        }

        entryOffset += typeChunk.entriesStart - ResourceTypes.ResTable_type.SIZEOF;
        if (entryOffset < 0 || entryOffset > typeChunk.data.limit() - ResourceTypes.ResTable_entry.SIZEOF) {
            return null;
        }

        int entryFlags = typeChunk.data.getShort(entryOffset + ResourceTypes.ResTable_entry.flags) & 0xFFFF;

        if ((entryFlags & ResourceTypes.ResTable_entry.FLAG_COMPLEX) == 0) {
            if (outValue == null) {
                outValue = new TypedValue();
            }
            outValue.type = typeChunk.data.getShort(entryOffset + ResourceTypes.ResTable_entry.dataType) & 0xFFFF;
            outValue.data = typeChunk.data.getInt(entryOffset + ResourceTypes.ResTable_entry.data);
            return outValue;
        }

        int entrySize = typeChunk.data.getShort(entryOffset + ResourceTypes.ResTable_entry.size) & 0xFFFF;

        if (entrySize < ResourceTypes.ResTable_entry.SIZEOF || entryOffset > typeChunk.data.limit() - entrySize) {
            return null;
        }

        if (entrySize < ResourceTypes.ResTable_entry.SIZEOF_EXT) {
            return null;
        }

        int mapEntryCount = typeChunk.data.getInt(entryOffset + ResourceTypes.ResTable_entry.count);
        int mapEntriesStart = entryOffset + entrySize;
        if (!MathUtil.isAlign4(mapEntriesStart)) {
            return null;
        }

        if (mapEntryCount > (typeChunk.data.limit() - mapEntriesStart) / ResourceTypes.ResTable_map.SIZEOF) {
            return null;
        }

        return typeChunk.data.slice(entryOffset, entrySize + mapEntryCount * ResourceTypes.ResTable_map.SIZEOF)
                .order(ByteOrder.nativeOrder());
    }
}
