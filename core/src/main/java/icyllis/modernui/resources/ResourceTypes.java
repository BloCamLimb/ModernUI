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

import org.jetbrains.annotations.ApiStatus;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * This class defines some structs to interpret MRSC file.
 */
@ApiStatus.Internal
public class ResourceTypes {

    public static class ResChunk_header {
        public static final int SIZEOF = 2 + 2 + 4;

        /*
            uint16_t type;
            uint16_t headerSize;
            uint32_t size;
         */

        // Type identifier for this chunk.  The meaning of this value depends
        // on the containing chunk.
        public static int type(long struct) {
            return memGetShort(struct) & 0xffff;
        }

        // Size of the chunk header (in bytes).  Adding this value to
        // the address of the chunk allows you to find its associated data
        // (if any).
        public static int headerSize(long struct) {
            return memGetShort(struct + 2) & 0xffff;
        }

        // Total size of this chunk (in bytes).  This is the chunkSize plus
        // the size of any data associated with the chunk.  Adding this value
        // to the chunk allows you to completely skip its contents (including
        // any child chunks).  If this value is the same as chunkSize, there is
        // no data associated with the chunk.
        public static int size(long struct) {
            return memGetInt(struct + 4);
        }
    }

    public static class ResTable_config {

        public ResTable_config(long ptr) {

        }
    }

    /*
        struct ResTable_ref {
            // higher 8 bits hold the 1-based index of namespace (from namespace table)
            // lower 24 bits hold the 0-based index of entry name (from key string table)
            // the type of the resource can be deduced, 0 means invalid id
            uint32_t id;
        }
     */

    /*
     * Representation of a value in a resource, supplying type
     * information.
     */
    public static class Res_value {
        public static final int SIZEOF = 2 + 2 + 4;

        /*
            uint16_t size;      // Number of bytes in this structure.
            uint16_t type;      //  0-7 bits: Type of the data value.
                                // 8-15 bits: Type identifier of the resource referenced by this item.
            uint32_t data;      // The data for this item, as interpreted according to dataType.
         */

        public static final int
        // The 'data' is either 0 or 1, specifying this resource is either
        // undefined or empty, respectively.
        TYPE_NULL = 0x00,
        // The 'data' holds a ResTable_ref, a reference to another resource
        // table entry. The high 8 bits of 'type' hold the type identifier of the reference.
        TYPE_REFERENCE = 0x01,
        // The 'data' holds an attribute resource identifier.
        TYPE_ATTRIBUTE = 0x02,
        // The 'data' holds an index into the containing resource table's
        // global value string pool.
        TYPE_STRING = 0x03,
        // The 'data' holds a single-precision floating point number.
        TYPE_FLOAT = 0x04,
        // The 'data' holds a complex number encoding a dimension value,
        // such as "100in".
        TYPE_DIMENSION = 0x05,
        // The 'data' holds a complex number encoding a fraction of a
        // container.
        TYPE_FRACTION = 0x06,

        // Beginning of integer flavors...
        TYPE_FIRST_INT = 0x10,

        // The 'data' is a raw integer value of the form n..n.
        TYPE_INT_DEC = 0x10,
        // The 'data' is a raw integer value of the form 0xn..n.
        TYPE_INT_HEX = 0x11,
        // The 'data' is either 0 or 1, for input "false" or "true" respectively.
        TYPE_INT_BOOLEAN = 0x12,

        // Beginning of color integer flavors...
        TYPE_FIRST_COLOR_INT = 0x1c,

        // The 'data' is a raw integer value of the form #aarrggbb.
        TYPE_INT_COLOR_ARGB8 = 0x1c,
        // The 'data' is a raw integer value of the form #rrggbb.
        TYPE_INT_COLOR_RGB8 = 0x1d,
        // The 'data' is a raw integer value of the form #argb.
        TYPE_INT_COLOR_ARGB4 = 0x1e,
        // The 'data' is a raw integer value of the form #rgb.
        TYPE_INT_COLOR_RGB4 = 0x1f,

        // ...end of integer flavors.
        TYPE_LAST_COLOR_INT = 0x1f,

        // ...end of integer flavors.
        TYPE_LAST_INT = 0x1f;

        public static final int DATA_NULL_UNDEFINED = 0;
        public static final int DATA_NULL_EMPTY = 1;

        public static int type(long struct) {
            return memGetShort(struct + 2) & 0xffff;
        }

        public static int data(long struct) {
            return memGetInt(struct + 4);
        }
    }

    public static class ResTable_type extends ResChunk_header {

        public static final int
                NO_ENTRY = 0xFFFFFFFF;

        public static final int
                FLAG_SPARSE = 0x01;

        // The type identifier this chunk is holding.  Type IDs start
        // at 1 (corresponding to the value of the type bits in a
        // resource identifier).  0 is invalid.
        public static int id(long struct) {
            return memGetByte(struct) & 0xff;
        }

        public static int flags(long struct) {
            return memGetByte(struct + 1) & 0xff;
        }

        // Number of uint32_t entry indices that follow.
        public static int entryCount(long struct) {
            return memGetInt(struct + 4);
        }

        // Offset from header where ResTable_entry data starts.
        public static int entriesStart(long struct) {
            return memGetInt(struct + 8);
        }

        // Configuration this collection of entries is designed for. This must always be last.
        public static long pConfig(long struct) {
            return struct + 12;
        }
    }

    /*
     * An entry in a ResTable_type with the flag `FLAG_SPARSE` set.
     */
    public static class ResTable_sparseTypeEntry {
        public static final int SIZEOF = 4 + 4;

        // The index of the entry.
        public static int index(long struct) {
            return memGetInt(struct);
        }

        // The offset from ResTable_type::entriesStart, in bytes.
        public static int offset(long struct) {
            return memGetInt(struct + 4);
        }
    }

    /*
     * This is the beginning of information about an entry in the resource
     * table.  It holds the reference to the name of this entry, and is
     * immediately followed by one of:
     *   * A Res_value structure, if FLAG_COMPLEX is -not- set.
     *   * An array of ResTable_map structures, if FLAG_COMPLEX is set.
     *     These supply a set of name/value mappings of data.
     */
    public static class ResTable_entry {
        public static final int SIZEOF = 2 + 2 + 4;

        // If set, this is a complex entry, holding a set of name/value
        // mappings.  It is followed by an array of ResTable_map structures.
        public static final int FLAG_COMPLEX = 0x0001;

        /*
            uint16_t size;  // Number of bytes in this structure.
            uint16_t flags;
            uint32_t key;   // Reference into key string table
         */

        public static int size(long struct) {
            return memGetShort(struct) & 0xffff;
        }

        public static int flags(long struct) {
            return memGetShort(struct + 2) & 0xffff;
        }

        public static int key(long struct) {
            return memGetInt(struct + 4);
        }
    }

    /*
     * Extended form of a ResTable_entry for map entries, defining a parent map
     * resource from which to inherit values.
     */
    public static class ResTable_map_entry extends ResTable_entry {
        public static final int SIZEOF = 4 + 4 + ResTable_entry.SIZEOF;

        /*
            ResTable_ref parent;    // Resource identifier of the parent mapping, or 0 if there is none.
            uint32_t count;         // Number of name/value pairs that follow for FLAG_COMPLEX.
         */

        // ResTable_ref
        public static int parent(long struct) {
            return memGetInt(struct + 8);
        }

        public static int count(long struct) {
            return memGetInt(struct + 12);
        }
    }

    public static class ResTable_map {
        public static final int SIZEOF = 4 + Res_value.SIZEOF;

        /*
            ResTable_ref name;  // The resource identifier defining this mapping's name.
            Res_value value;    // This mapping's value.
         */

        public static final int
        // No type has been defined for this attribute, use generic
        // type handling.  The low 16 bits are for types that can be
        // handled generically; the upper 16 require additional information
        // in the bag so can not be handled generically for TYPE_ANY.
        TYPE_ANY = 0x0000FFFF,

        // Attribute holds a references to another resource.
        TYPE_REFERENCE = 1,

        // Attribute holds a generic string.
        TYPE_STRING = 1 << 1,

        // Attribute holds an integer value.  ATTR_MIN and ATTR_MIN can
        // optionally specify a constrained range of possible integer values.
        TYPE_INTEGER = 1 << 2,

        // Attribute holds a boolean integer.
        TYPE_BOOLEAN = 1 << 3,

        // Attribute holds a color value.
        TYPE_COLOR = 1 << 4,

        // Attribute holds a floating point value.
        TYPE_FLOAT = 1 << 5,

        // Attribute holds a dimension value, such as "20px".
        TYPE_DIMENSION = 1 << 6,

        // Attribute holds a fraction value, such as "20%".
        TYPE_FRACTION = 1 << 7,

        // Attribute holds an enumeration.  The enumeration values are
        // supplied as additional entries in the map.
        TYPE_ENUM = 1 << 16,

        // Attribute holds a bitmaks of flags.  The flag bit values are
        // supplied as additional entries in the map.
        TYPE_FLAGS = 1 << 17;

        // ResTable_ref
        public static int name(long struct) {
            return memGetInt(struct);
        }

        // Res_value*
        public static long pValue(long struct) {
            return struct + 4;
        }
    }
}
