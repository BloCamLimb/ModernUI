/*
 * Arc 3D.
 * Copyright (C) 2022-2023 BloCamLimb. All rights reserved.
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.shaderc;

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
 * @param builtin              (SpvBuiltIn) identify which particular built-in value this object represents
 */
public record Layout(int flags, int location, int component, int index,
                     int binding, int offset, int align, int set,
                     int inputAttachmentIndex, int builtin) {

    private static final Layout EMPTY = new Layout(0, -1, -1, -1, -1, -1, -1, -1, -1, -1);

    // GLSL layout qualifiers, order-independent.
    public static final int
            kOriginUpperLeft_Flag = 1,
            kPixelCenterInteger_Flag = 1 << 1,
            kEarlyFragmentTests_Flag = 1 << 2,
            kBlendSupportAllEquations_Flag = 1 << 3,  // OpenGL only
            kPushConstant_Flag = 1 << 4;              // Vulkan only
    // These flags indicate if the qualifier appeared, regardless of the accompanying value.
    public static final int
            kLocation_Flag = 1 << 5,
            kComponent_Flag = 1 << 6,
            kIndex_Flag = 1 << 7,
            kBinding_Flag = 1 << 8,
            kOffset_Flag = 1 << 9,
            kAlign_Flag = 1 << 10,
            kSet_Flag = 1 << 11,
            kInputAttachmentIndex_Flag = 1 << 12,
            kBuiltin_Flag = 1 << 13;

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
        if (builtin >= 0) {
            joiner.add("builtin = " + builtin);
        }
        if ((flags & kOriginUpperLeft_Flag) != 0) {
            joiner.add("origin_upper_left");
        }
        if ((flags & kPixelCenterInteger_Flag) != 0) {
            joiner.add("pixel_center_integer");
        }
        if ((flags & kEarlyFragmentTests_Flag) != 0) {
            joiner.add("early_fragment_tests");
        }
        if ((flags & kBlendSupportAllEquations_Flag) != 0) {
            joiner.add("blend_support_all_equations");
        }
        if ((flags & kPushConstant_Flag) != 0) {
            joiner.add("push_constant");
        }
        return joiner.toString();
    }

    public static int flag(int flags, int mask, String name, int position) {
        if ((flags & mask) != 0) {
            ThreadContext.getInstance().error(position, "layout qualifier '" + name +
                    "' appears more than once");
        }
        return flags | mask;
    }
}
