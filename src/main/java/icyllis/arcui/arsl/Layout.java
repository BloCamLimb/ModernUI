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

package icyllis.arcui.arsl;

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

    private static final Layout EMPTY = builtin(-1);

    // GLSL layout qualifiers, order-independent.
    public static final int
            kOriginUpperLeft_Flag = 1,
            kPushConstant_Flag = 1 << 1,
            kBlendSupportAllEquations_Flag = 1 << 2,
            kColor_Flag = 1 << 3;
    // These flags indicate if the qualifier appeared, regardless of the accompanying value.
    public static final int
            kLocation_Flag = 1 << 4,
            kOffset_Flag = 1 << 5,
            kBinding_Flag = 1 << 6,
            kIndex_Flag = 1 << 7,
            kSet_Flag = 1 << 8,
            kBuiltin_Flag = 1 << 9,
            kInputAttachmentIndex_Flag = 1 << 10;

    @Nonnull
    public static Layout empty() {
        return EMPTY;
    }

    @Nonnull
    public static Layout builtin(int builtin) {
        return new Layout(0, -1, -1, -1, -1, -1, builtin, -1);
    }

    @Nonnull
    public String description() {
        return descriptionBuilder().toString();
    }

    @Nonnull
    StringBuilder descriptionBuilder() {
        final StringBuilder result = new StringBuilder();
        boolean firstSeparator = true;
        if (location >= 0) {
            firstSeparator = false;
            result.append("location = ").append(location);
        }
        if (offset >= 0) {
            if (firstSeparator)
                firstSeparator = false;
            else
                result.append(", ");
            result.append("offset = ").append(offset);
        }
        if (binding >= 0) {
            if (firstSeparator)
                firstSeparator = false;
            else
                result.append(", ");
            result.append("binding = ").append(binding);
        }
        if (index >= 0) {
            if (firstSeparator)
                firstSeparator = false;
            else
                result.append(", ");
            result.append("index = ").append(index);
        }
        if (set >= 0) {
            if (firstSeparator)
                firstSeparator = false;
            else
                result.append(", ");
            result.append("set = ").append(set);
        }
        if (builtin >= 0) {
            if (firstSeparator)
                firstSeparator = false;
            else
                result.append(", ");
            result.append("builtin = ").append(builtin);
        }
        if (inputAttachmentIndex >= 0) {
            if (firstSeparator)
                firstSeparator = false;
            else
                result.append(", ");
            result.append("input_attachment_index = ").append(inputAttachmentIndex);
        }
        if ((flags & kOriginUpperLeft_Flag) != 0) {
            if (firstSeparator)
                firstSeparator = false;
            else
                result.append(", ");
            result.append("origin_upper_left");
        }
        if ((flags & kBlendSupportAllEquations_Flag) != 0) {
            if (firstSeparator)
                firstSeparator = false;
            else
                result.append(", ");
            result.append("blend_support_all_equations");
        }
        if ((flags & kPushConstant_Flag) != 0) {
            if (firstSeparator)
                firstSeparator = false;
            else
                result.append(", ");
            result.append("push_constant");
        }
        if ((flags & kColor_Flag) != 0) {
            if (firstSeparator)
                firstSeparator = false;
            else
                result.append(", ");
            result.append("color");
        }
        assert (!firstSeparator && !result.isEmpty()) || (firstSeparator && result.isEmpty());
        if (!firstSeparator) {
            result.insert(0, "layout (");
            result.append(")");
        }
        return result;
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
