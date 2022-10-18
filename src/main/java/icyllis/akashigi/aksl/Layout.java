/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.aksl;

import javax.annotation.Nonnull;
import java.util.StringJoiner;

/**
 * Represents a layout block appearing before a variable declaration, as in:
 * <p>
 * layout (location = 0) int x;
 *
 * @param location             (in / out, expect for compute) individual variable, interface block, block member
 * @param component            (in / out, expect for compute) individual variable, block member
 * @param index                (fragment out) individual variable
 * @param binding              (UBO / SSBO) individual variable (opaque types only), interface block
 * @param offset               (UBO / SSBO) individual variable (atomic counters only), block member
 * @param align                (UBO / SSBO) interface block, block member
 * @param set                  (UBO / SSBO, Vulkan only) individual variable (opaque types only), interface block
 * @param inputAttachmentIndex (UBO, Vulkan only) individual variable (subpass types only), connect a shader variable
 *                             to the corresponding attachment on the subpass in which the shader is being used
 * @param builtIn              (SpvBuiltIn) identify which particular built-in value this object represents
 */
public record Layout(int flags, int location, int component, int index,
                     int binding, int offset, int align, int set,
                     int inputAttachmentIndex, int builtIn) {

    private static final Layout EMPTY = new Layout(0, -1, -1, -1, -1, -1, -1, -1, -1, -1);

    // GLSL layout qualifiers, order-independent.
    public static final int
            ORIGIN_UPPER_LEFT_FLAG = 1,
            PIXEL_CENTER_INTEGER_FLAG = 1 << 1,
            EARLY_FRAGMENT_TESTS_FLAG = 1 << 2,
            BLEND_SUPPORT_ALL_EQUATIONS_FLAG = 1 << 3,  // OpenGL only
            PUSH_CONSTANT_FLAG = 1 << 4;                // Vulkan only
    // These flags indicate if the qualifier appeared, regardless of the accompanying value.
    public static final int
            LOCATION_FLAG = 1 << 5,
            COMPONENT_FLAG = 1 << 6,
            INDEX_FLAG = 1 << 7,
            BINDING_FLAG = 1 << 8,
            OFFSET_FLAG = 1 << 9,
            ALIGN_FLAG = 1 << 10,
            SET_FLAG = 1 << 11,
            INPUT_ATTACHMENT_INDEX_FLAG = 1 << 12,
            BUILT_IN_FLAG = 1 << 13;

    @Nonnull
    public static Layout empty() {
        return EMPTY;
    }

    @Nonnull
    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "layout (", ") ");
        if (location >= 0) {
            joiner.add("location = " + location);
        }
        if (component >= 0) {
            joiner.add("component = " + component);
        }
        if (index >= 0) {
            joiner.add("index = " + index);
        }
        if (binding >= 0) {
            joiner.add("binding = " + binding);
        }
        if (offset >= 0) {
            joiner.add("offset = " + offset);
        }
        if (align >= 0) {
            joiner.add("align = " + align);
        }
        if (set >= 0) {
            joiner.add("set = " + set);
        }
        if (inputAttachmentIndex >= 0) {
            joiner.add("input_attachment_index = " + inputAttachmentIndex);
        }
        if (builtIn >= 0) {
            joiner.add("built_in = " + builtIn);
        }
        if ((flags & ORIGIN_UPPER_LEFT_FLAG) != 0) {
            joiner.add("origin_upper_left");
        }
        if ((flags & PIXEL_CENTER_INTEGER_FLAG) != 0) {
            joiner.add("pixel_center_integer");
        }
        if ((flags & EARLY_FRAGMENT_TESTS_FLAG) != 0) {
            joiner.add("early_fragment_tests");
        }
        if ((flags & BLEND_SUPPORT_ALL_EQUATIONS_FLAG) != 0) {
            joiner.add("blend_support_all_equations");
        }
        if ((flags & PUSH_CONSTANT_FLAG) != 0) {
            joiner.add("push_constant");
        }
        return joiner.toString();
    }
}
