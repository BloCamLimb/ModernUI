/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.sksl;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Represents a layout block appearing before a variable declaration, as in:
 * <p>
 * layout (location = 0) int x;
 *
 * @param location             (UBO or SSBO): individual variable;
 *                             (in or out): individual variable, block (struct), block member (struct field).
 * @param offset               (UBO or SSBO) atomic counter, block member (struct field)
 * @param binding              (UBO or SSBO) individual variable with opaque types, block (struct)
 * @param index                (fragment out) individual variable
 * @param set                  (UBO or SSBO) Vulkan only, individual variable with opaque types, block (struct);
 *                             descriptor set.
 * @param builtin              SPIR-V, identifies which particular builtin value this object represents
 *                             (includes SpvBuiltIn and ArcUI-defined values).
 * @param inputAttachmentIndex SPIR-V, Vulkan only, connect a shader variable to the corresponding attachment on the
 *                             subpass in which the shader is being used.
 */
@Immutable
public record Layout(int flags, int location, int offset, int binding, int index, int set, int builtin,
                     int inputAttachmentIndex) {

    private static final String SEPARATOR = ", ";

    // GLSL layout qualifiers, order-independent.
    public static final int
            ORIGIN_UPPER_LEFT_FLAG = 1,
            PUSH_CONSTANT_FLAG = 1 << 1,
            BLEND_SUPPORT_ALL_EQUATIONS_FLAG = 1 << 2;

    // These flags indicate if the qualifier appeared, regardless of the accompanying value.
    public static final int
            LOCATION_FLAG = 1 << 4,
            OFFSET_FLAG = 1 << 5,
            BINDING_FLAG = 1 << 6,
            INDEX_FLAG = 1 << 7,
            SET_FLAG = 1 << 8,
            BUILTIN_FLAG = 1 << 9,
            INPUT_ATTACHMENT_INDEX_FLAG = 1 << 10;

    @Nonnull
    public static Layout builtin(int builtin) {
        return new Layout(0, -1, -1, -1, -1, -1, builtin, -1);
    }

    @Nonnull
    StringBuilder getDescriptionBuilder() {
        final StringBuilder result = new StringBuilder();
        boolean separator = false;
        if (location >= 0) {
            separator = true;
            result.append("location = ").append(location);
        }
        if (offset >= 0) {
            if (separator)
                result.append(SEPARATOR);
            else
                separator = true;
            result.append("offset = ").append(offset);
        }
        if (binding >= 0) {
            if (separator)
                result.append(SEPARATOR);
            else
                separator = true;
            result.append("binding = ").append(binding);
        }
        if (index >= 0) {
            if (separator)
                result.append(SEPARATOR);
            else
                separator = true;
            result.append("index = ").append(index);
        }
        if (set >= 0) {
            if (separator)
                result.append(SEPARATOR);
            else
                separator = true;
            result.append("set = ").append(set);
        }
        // no use in GLSL
        /*if (builtin >= 0) {
            if (separator)
                result.append(SEPARATOR);
            else
                separator = true;
            result.append("builtin = ").append(builtin);
        }*/
        if (inputAttachmentIndex >= 0) {
            if (separator)
                result.append(SEPARATOR);
            else
                separator = true;
            result.append("input_attachment_index = ").append(inputAttachmentIndex);
        }
        if ((flags & ORIGIN_UPPER_LEFT_FLAG) != 0) {
            if (separator)
                result.append(SEPARATOR);
            else
                separator = true;
            result.append("origin_upper_left");
        }
        if ((flags & BLEND_SUPPORT_ALL_EQUATIONS_FLAG) != 0) {
            if (separator)
                result.append(SEPARATOR);
            else
                separator = true;
            result.append("blend_support_all_equations");
        }
        if ((flags & PUSH_CONSTANT_FLAG) != 0) {
            if (separator)
                result.append(SEPARATOR);
            result.append("push_constant");
        }
        assert (separator && !result.isEmpty()) || (!separator && result.isEmpty());
        if (separator) {
            result.insert(0, "layout (");
            result.append(")");
        }
        return result;
    }

    @Nonnull
    public String getDescription() {
        return getDescriptionBuilder().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Layout layout = (Layout) o;
        if (flags != layout.flags) return false;
        if (location != layout.location) return false;
        if (offset != layout.offset) return false;
        if (binding != layout.binding) return false;
        if (index != layout.index) return false;
        if (set != layout.set) return false;
        if (builtin != layout.builtin) return false;
        return inputAttachmentIndex == layout.inputAttachmentIndex;
    }

    @Override
    public int hashCode() {
        int result = flags;
        result = 31 * result + location;
        result = 31 * result + offset;
        result = 31 * result + binding;
        result = 31 * result + index;
        result = 31 * result + set;
        result = 31 * result + builtin;
        result = 31 * result + inputAttachmentIndex;
        return result;
    }
}
