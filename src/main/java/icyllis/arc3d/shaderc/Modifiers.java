/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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

import java.util.StringJoiner;

/**
 * Represents a layout block and a set of modifier keywords (in, out, uniform, etc.)
 * appearing before a variable or interface block declaration, as in:
 * <pre>{@literal
 * layout(location = 2) smooth in float2 v_TexCoord;
 * layout(binding = 1, set = 0) uniform UniformBlock {
 *     float u_Radius;
 * };
 * }</pre>
 */
public final class Modifiers {

    // GLSL layout qualifiers, order-independent.
    public static final int
            kOriginUpperLeft_LayoutFlag = 1,
            kPixelCenterInteger_LayoutFlag = 1 << 1,
            kEarlyFragmentTests_LayoutFlag = 1 << 2,
            kBlendSupportAllEquations_LayoutFlag = 1 << 3,  // OpenGL only
            kPushConstant_LayoutFlag = 1 << 4;              // Vulkan only
    // These flags indicate if the qualifier appeared, regardless of the accompanying value.
    public static final int
            kLocation_LayoutFlag = 1 << 5,
            kComponent_LayoutFlag = 1 << 6,
            kIndex_LayoutFlag = 1 << 7,
            kBinding_LayoutFlag = 1 << 8,
            kOffset_LayoutFlag = 1 << 9,
            kAlign_LayoutFlag = 1 << 10,
            kSet_LayoutFlag = 1 << 11,
            kInputAttachmentIndex_LayoutFlag = 1 << 12,
            kBuiltin_LayoutFlag = 1 << 13;

    // GLSL interpolation qualifiers, only one qualifier can be used
    public static final int
            kFlat_Flag = 1,
            kNoPerspective_Flag = 1 << 1;
    // GLSL storage qualifiers, only one qualifier can be used
    // GLSL parameter qualifiers, one of const, in, out, inout
    public static final int
            kConst_Flag = 1 << 2,
            kUniform_Flag = 1 << 3,
            kIn_Flag = 1 << 4,
            kOut_Flag = 1 << 5;
    // GLSL memory qualifiers
    public static final int
            kReadOnly_Flag = 1 << 6,
            kWriteOnly_Flag = 1 << 7;
    // Other GLSL storage qualifiers
    public static final int
            kBuffer_Flag = 1 << 8,
            kWorkgroup_Flag = 1 << 9;  // GLSL 'shared'
    // Extensions, not present in GLSL
    public static final int
            kExport_Flag = 1 << 10,
            kPure_Flag = 1 << 11,
            kInline_Flag = 1 << 12,
            kNoInline_Flag = 1 << 13;

    /**
     * Layout keyword flags.
     */
    private int mLayoutFlags;
    /**
     * (in / out, expect for compute) individual variable, interface block, block member.
     */
    public int mLocation;
    /**
     * (in / out, expect for compute) individual variable, block member.
     */
    public int mComponent;
    /**
     * (fragment out) individual variable.
     */
    public int mIndex;
    /**
     * (UBO / SSBO) individual variable (opaque types only), interface block.
     */
    public int mBinding;
    /**
     * (UBO / SSBO) individual variable (atomic counters only), block member.
     */
    public int mOffset;
    /**
     * (UBO / SSBO) interface block, block member.
     */
    public int mAlign;
    /**
     * (UBO / SSBO, Vulkan only) individual variable (opaque types only), interface block.
     */
    public int mSet;
    /**
     * (UBO, Vulkan only) individual variable (subpass types only), connect a shader variable
     * to the corresponding attachment on the subpass in which the shader is being used.
     */
    public int mInputAttachmentIndex;
    /**
     * (SpvBuiltIn) identify which particular built-in value this object represents.
     */
    public int mBuiltin;
    /**
     * Non-layout keyword flags.
     */
    private int mFlags;

    public void setLayoutFlag(int mask, String name, int pos) {
        if ((mLayoutFlags & mask) != 0) {
            ThreadContext.getInstance().error(pos, "layout qualifier '" + name +
                    "' appears more than once");
        }
        mLayoutFlags |= mask;
    }

    public int layoutFlags() {
        return mLayoutFlags;
    }

    public void setFlag(int mask, int pos) {
        if ((mFlags & mask) != 0) {
            ThreadContext.getInstance().error(pos, "qualifier '" + describeFlags(mask) +
                    "' appears more than once");
        }
        mFlags |= mask;
    }

    public int flags() {
        return mFlags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Modifiers modifiers = (Modifiers) o;

        if (mLayoutFlags != modifiers.mLayoutFlags) return false;
        if (mLocation != modifiers.mLocation) return false;
        if (mComponent != modifiers.mComponent) return false;
        if (mIndex != modifiers.mIndex) return false;
        if (mBinding != modifiers.mBinding) return false;
        if (mOffset != modifiers.mOffset) return false;
        if (mAlign != modifiers.mAlign) return false;
        if (mSet != modifiers.mSet) return false;
        if (mInputAttachmentIndex != modifiers.mInputAttachmentIndex) return false;
        if (mBuiltin != modifiers.mBuiltin) return false;
        return mFlags == modifiers.mFlags;
    }

    @Override
    public int hashCode() {
        int result = mLayoutFlags;
        result = 31 * result + mLocation;
        result = 31 * result + mComponent;
        result = 31 * result + mIndex;
        result = 31 * result + mBinding;
        result = 31 * result + mOffset;
        result = 31 * result + mAlign;
        result = 31 * result + mSet;
        result = 31 * result + mInputAttachmentIndex;
        result = 31 * result + mBuiltin;
        result = 31 * result + mFlags;
        return result;
    }

    @Override
    public String toString() {
        StringJoiner layout = new StringJoiner(", ", "layout (", ") ");
        if (mLocation >= 0) {
            layout.add("location = " + mLocation);
        }
        if (mComponent >= 0) {
            layout.add("component = " + mComponent);
        }
        if (mIndex >= 0) {
            layout.add("index = " + mIndex);
        }
        if (mBinding >= 0) {
            layout.add("binding = " + mBinding);
        }
        if (mOffset >= 0) {
            layout.add("offset = " + mOffset);
        }
        if (mAlign >= 0) {
            layout.add("align = " + mAlign);
        }
        if (mSet >= 0) {
            layout.add("set = " + mSet);
        }
        if (mInputAttachmentIndex >= 0) {
            layout.add("input_attachment_index = " + mInputAttachmentIndex);
        }
        if (mBuiltin >= 0) {
            layout.add("builtin = " + mBuiltin);
        }
        if ((mLayoutFlags & kOriginUpperLeft_LayoutFlag) != 0) {
            layout.add("origin_upper_left");
        }
        if ((mLayoutFlags & kPixelCenterInteger_LayoutFlag) != 0) {
            layout.add("pixel_center_integer");
        }
        if ((mLayoutFlags & kEarlyFragmentTests_LayoutFlag) != 0) {
            layout.add("early_fragment_tests");
        }
        if ((mLayoutFlags & kBlendSupportAllEquations_LayoutFlag) != 0) {
            layout.add("blend_support_all_equations");
        }
        if ((mLayoutFlags & kPushConstant_LayoutFlag) != 0) {
            layout.add("push_constant");
        }
        return layout + describeFlags(mFlags);
    }

    public static String describeFlags(int flags) {
        StringJoiner joiner = new StringJoiner(" ");

        // Extensions
        if ((flags & kExport_Flag) != 0) {
            joiner.add("export");
        }
        if ((flags & kPure_Flag) != 0) {
            joiner.add("pure");
        }
        if ((flags & kInline_Flag) != 0) {
            joiner.add("inline");
        }
        if ((flags & kNoInline_Flag) != 0) {
            joiner.add("noinline");
        }

        // Real GLSL qualifiers
        if ((flags & kFlat_Flag) != 0) {
            joiner.add("flat");
        }
        if ((flags & kNoPerspective_Flag) != 0) {
            joiner.add("noperspective");
        }
        if ((flags & kConst_Flag) != 0) {
            joiner.add("const");
        }
        if ((flags & kUniform_Flag) != 0) {
            joiner.add("uniform");
        }
        if ((flags & kIn_Flag) != 0 && (flags & kOut_Flag) != 0) {
            joiner.add("inout");
        } else if ((flags & kIn_Flag) != 0) {
            joiner.add("in");
        } else if ((flags & kOut_Flag) != 0) {
            joiner.add("out");
        }
        if ((flags & kReadOnly_Flag) != 0) {
            joiner.add("readonly");
        }
        if ((flags & kWriteOnly_Flag) != 0) {
            joiner.add("writeonly");
        }
        if ((flags & kBuffer_Flag) != 0) {
            joiner.add("buffer");
        }

        // We're using a non-GLSL name for this one; the GLSL equivalent is "shared"
        if ((flags & kWorkgroup_Flag) != 0) {
            joiner.add("workgroup");
        }

        return joiner.toString();
    }
}
