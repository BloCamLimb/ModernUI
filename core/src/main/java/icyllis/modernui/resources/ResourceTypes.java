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

public class ResourceTypes {

    public static class Res_value {
        // uint16_t size;
        // uint8_t res0;
        // uint8_t dataType;
        // uint32_t data;

        public static final int // The 'data' is either 0 or 1, specifying this resource is either
                // undefined or empty, respectively.
                TYPE_NULL = 0x00,
        // The 'data' holds a ResTable_ref, a reference to another resource
        // table entry.
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
        // The 'data' holds a dynamic ResTable_ref, which needs to be
        // resolved before it can be used like a TYPE_REFERENCE.
        TYPE_DYNAMIC_REFERENCE = 0x07,
        // The 'data' holds an attribute resource identifier, which needs to be resolved
        // before it can be used like a TYPE_ATTRIBUTE.
        TYPE_DYNAMIC_ATTRIBUTE = 0x08,

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
    }

    public static class ResTable_map {
        // uint32_t name;
        // Res_value value;
    }

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
}
