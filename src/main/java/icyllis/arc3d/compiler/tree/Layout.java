/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.compiler.tree;

import icyllis.arc3d.compiler.Context;
import org.jspecify.annotations.NonNull;

import java.util.StringJoiner;

/**
 * Represents a layout block appearing before a variable declaration, as in:
 * <p>
 * layout (location = 0) int x;
 */
public final class Layout {

    // GLSL layout qualifiers, order-independent.
    public static final int
            kOriginUpperLeft_LayoutFlag = 1,
            kPixelCenterInteger_LayoutFlag = 1 << 1,
            kEarlyFragmentTests_LayoutFlag = 1 << 2,
            kBlendSupportAllEquations_LayoutFlag = 1 << 3,  // OpenGL only
            kPushConstant_LayoutFlag = 1 << 4,              // Vulkan only
            kStd140_LayoutFlag = 1 << 5,
            kStd430_LayoutFlag = 1 << 6;
    // These flags indicate if the qualifier appeared, regardless of the accompanying value.
    public static final int
            kLocation_LayoutFlag = 1 << 7,
            kComponent_LayoutFlag = 1 << 8,
            kIndex_LayoutFlag = 1 << 9,
            kBinding_LayoutFlag = 1 << 10,
            kOffset_LayoutFlag = 1 << 11,
            kSet_LayoutFlag = 1 << 12,
            kInputAttachmentIndex_LayoutFlag = 1 << 13,
            kBuiltin_LayoutFlag = 1 << 14;
    public static final int kCount_LayoutFlag = 15;

    public static String describeLayoutFlag(int flag) {
        assert Integer.bitCount(flag) == 1;
        return switch (Integer.numberOfTrailingZeros(flag)) {
            case 0 -> "origin_upper_left";
            case 1 -> "pixel_center_integer";
            case 2 -> "early_fragment_tests";
            case 3 -> "blend_support_all_equations";
            case 4 -> "push_constant";
            case 5 -> "std140";
            case 6 -> "std430";
            case 7 -> "location";
            case 8 -> "component";
            case 9 -> "index";
            case 10 -> "binding";
            case 11 -> "offset";
            case 12 -> "set";
            case 13 -> "input_attachment_index";
            case 14 -> "builtin";
            default -> "";
        };
    }

    /**
     * Layout keyword flags.
     */
    private int mLayoutFlags = 0;
    /**
     * (in / out, expect for compute) individual variable, interface block, block member.
     */
    public int mLocation = -1;
    /**
     * (in / out, expect for compute) individual variable, block member.
     */
    public int mComponent = -1;
    /**
     * (fragment out) individual variable.
     */
    public int mIndex = -1;
    /**
     * (UBO / SSBO) individual variable (opaque types only), interface block.
     */
    public int mBinding = -1;
    /**
     * (UBO / SSBO) individual variable (atomic counters only), block member.
     */
    public int mOffset = -1;
    /**
     * (UBO / SSBO, Vulkan only) individual variable (opaque types only), interface block.
     */
    public int mSet = -1;
    /**
     * (UBO, Vulkan only) individual variable (subpass types only), connect a shader variable
     * to the corresponding attachment on the subpass in which the shader is being used.
     */
    public int mInputAttachmentIndex = -1;
    /**
     * (SpvBuiltIn) identify which particular built-in value this object represents.
     */
    public int mBuiltin = -1;

    public Layout() {
    }

    public int layoutFlags() {
        return mLayoutFlags;
    }

    public void setLayoutFlag(@NonNull Context context,
                              int mask, String name, int pos) {
        if ((mLayoutFlags & mask) != 0) {
            context.error(pos, "layout qualifier '" + name +
                    "' appears more than once");
        }
        mLayoutFlags |= mask;
    }

    public void clearLayoutFlag(int mask) {
        mLayoutFlags &= ~mask;
    }

    public boolean checkLayoutFlags(@NonNull Context context,
                                    int pos, int permittedLayoutFlags) {
        boolean success = true;

        for (int i = 0; i < kCount_LayoutFlag; i++) {
            int flag = 1 << i;
            if ((mLayoutFlags & flag) != 0 && (permittedLayoutFlags & flag) == 0) {
                context.error(pos, "layout qualifier '" +
                        describeLayoutFlag(flag) + "' is not permitted here");
                success = false;
            }
        }

        return success;
    }

    @Override
    public int hashCode() {
        int result = mLayoutFlags;
        result = 31 * result + mLocation;
        result = 31 * result + mComponent;
        result = 31 * result + mIndex;
        result = 31 * result + mBinding;
        result = 31 * result + mOffset;
        result = 31 * result + mSet;
        result = 31 * result + mInputAttachmentIndex;
        result = 31 * result + mBuiltin;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Layout layout = (Layout) o;

        if (mLayoutFlags != layout.mLayoutFlags) return false;
        if (mLocation != layout.mLocation) return false;
        if (mComponent != layout.mComponent) return false;
        if (mIndex != layout.mIndex) return false;
        if (mBinding != layout.mBinding) return false;
        if (mOffset != layout.mOffset) return false;
        if (mSet != layout.mSet) return false;
        if (mInputAttachmentIndex != layout.mInputAttachmentIndex) return false;
        return mBuiltin == layout.mBuiltin;
    }

    @NonNull
    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "layout(", ") ");
        if (mLocation >= 0) {
            joiner.add("location = " + mLocation);
        }
        if (mComponent >= 0) {
            joiner.add("component = " + mComponent);
        }
        if (mIndex >= 0) {
            joiner.add("index = " + mIndex);
        }
        if (mBinding >= 0) {
            joiner.add("binding = " + mBinding);
        }
        if (mOffset >= 0) {
            joiner.add("offset = " + mOffset);
        }
        if (mSet >= 0) {
            joiner.add("set = " + mSet);
        }
        if (mInputAttachmentIndex >= 0) {
            joiner.add("input_attachment_index = " + mInputAttachmentIndex);
        }
        if (mBuiltin >= 0) {
            joiner.add("builtin = " + mBuiltin);
        }
        if ((mLayoutFlags & kOriginUpperLeft_LayoutFlag) != 0) {
            joiner.add("origin_upper_left");
        }
        if ((mLayoutFlags & kPixelCenterInteger_LayoutFlag) != 0) {
            joiner.add("pixel_center_integer");
        }
        if ((mLayoutFlags & kEarlyFragmentTests_LayoutFlag) != 0) {
            joiner.add("early_fragment_tests");
        }
        if ((mLayoutFlags & kBlendSupportAllEquations_LayoutFlag) != 0) {
            joiner.add("blend_support_all_equations");
        }
        if ((mLayoutFlags & kPushConstant_LayoutFlag) != 0) {
            joiner.add("push_constant");
        }
        if ((mLayoutFlags & kStd140_LayoutFlag) != 0) {
            joiner.add("std140");
        }
        if ((mLayoutFlags & kStd430_LayoutFlag) != 0) {
            joiner.add("std430");
        }
        return joiner.toString();
    }
}
