/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.compiler.tree;

import icyllis.arc3d.compiler.Context;
import icyllis.arc3d.compiler.analysis.TreeVisitor;

import javax.annotation.Nonnull;
import java.util.Objects;
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
public final class Modifiers extends Node {

    // GLSL interpolation qualifiers, only one qualifier can be used
    public static final int
            kSmooth_Flag = 1,
            kFlat_Flag = 1 << 1,
            kNoPerspective_Flag = 1 << 2;
    // GLSL storage qualifiers, only one qualifier can be used
    // GLSL parameter qualifiers, one of const, in, out, inout
    public static final int
            kConst_Flag = 1 << 3,
            kUniform_Flag = 1 << 4,
            kIn_Flag = 1 << 5,
            kOut_Flag = 1 << 6;
    // GLSL memory qualifiers
    public static final int
            kCoherent_Flag = 1 << 7,
            kVolatile_Flag = 1 << 8,
            kRestrict_Flag = 1 << 9,
            kReadOnly_Flag = 1 << 10,
            kWriteOnly_Flag = 1 << 11;
    // Other GLSL storage qualifiers
    public static final int
            kBuffer_Flag = 1 << 12,
            kWorkgroup_Flag = 1 << 13;  // GLSL 'shared'
    // Extensions, not present in GLSL
    public static final int
            kSubroutine_Flag = 1 << 14,
            kPure_Flag = 1 << 15,
            kInline_Flag = 1 << 16,
            kNoInline_Flag = 1 << 17;
    public static final int kCount_Flag = 18;

    public static final int kInterpolation_Flags =
            kSmooth_Flag | kFlat_Flag | kNoPerspective_Flag;
    public static final int kMemory_Flags =
            kCoherent_Flag | kVolatile_Flag | kRestrict_Flag | kReadOnly_Flag | kWriteOnly_Flag;
    public static final int kStorage_Flags =
            kIn_Flag | kOut_Flag | kUniform_Flag | kBuffer_Flag | kWorkgroup_Flag;

    public static String describeFlag(int flag) {
        assert Integer.bitCount(flag) == 1;
        return switch (Integer.numberOfTrailingZeros(flag)) {
            case 0 -> "smooth";
            case 1 -> "flat";
            case 2 -> "noperspective";
            case 3 -> "const";
            case 4 -> "uniform";
            case 5 -> "in";
            case 6 -> "out";
            case 7 -> "coherent";
            case 8 -> "volatile";
            case 9 -> "restrict";
            case 10 -> "readonly";
            case 11 -> "writeonly";
            case 12 -> "buffer";
            case 13 -> "workgroup";
            case 14 -> "subroutine";
            case 15 -> "__pure";
            case 16 -> "inline";
            case 17 -> "noinline";
            default -> "";
        };
    }

    /**
     * Layout qualifiers.
     */
    private Layout mLayout = null;
    /**
     * Non-layout keyword flags.
     */
    private int mFlags = 0;

    public Modifiers(int position) {
        super(position);
    }

    // check layout flags first, or null
    public Layout layout() {
        return mLayout;
    }

    public int layoutFlags() {
        return mLayout != null ? mLayout.layoutFlags() : 0;
    }

    public void setLayoutFlag(@Nonnull Context context,
                              int mask, String name, int pos) {
        Layout layout = mLayout;
        if (layout == null) {
            layout = mLayout = new Layout();
        }
        layout.setLayoutFlag(context, mask, name, pos);
    }

    public void clearLayoutFlag(int mask) {
        if (mLayout != null) {
            mLayout.clearLayoutFlag(mask);
        }
    }

    public boolean checkLayoutFlags(@Nonnull Context context,
                                    int permittedLayoutFlags) {
        if (mLayout != null) {
            return mLayout.checkLayoutFlags(context, mPosition, permittedLayoutFlags);
        }
        return true;
    }

    public int flags() {
        return mFlags;
    }

    public void setFlag(@Nonnull Context context,
                        int mask, int pos) {
        if ((mFlags & mask) != 0) {
            context.error(pos, "qualifier '" + describeFlags(mFlags & mask) +
                    "' appears more than once");
        }
        mFlags |= mask;
    }

    public void clearFlag(int mask) {
        mFlags &= ~mask;
    }

    public boolean checkFlags(@Nonnull Context context,
                              int permittedFlags) {
        boolean success = true;

        for (int i = 0; i < kCount_Flag; i++) {
            int flag = 1 << i;
            if ((mFlags & flag) != 0 && (permittedFlags & flag) == 0) {
                context.error(mPosition, "qualifier '" +
                        describeFlag(flag) + "' is not permitted here");
                success = false;
            }
        }

        if (Integer.bitCount(mFlags & kInterpolation_Flags) > 1) {
            context.error(mPosition, "at most one interpolation qualifier can be used");
            success = false;
        }

        return success;
    }

    @Override
    public boolean accept(@Nonnull TreeVisitor visitor) {
        return false;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(mLayout);
        result = 31 * result + mFlags;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Modifiers modifiers = (Modifiers) o;

        return mFlags == modifiers.mFlags &&
                Objects.equals(mLayout, modifiers.mLayout);
    }

    @Nonnull
    @Override
    public String toString() {
        if (mLayout == null) {
            return describeFlags(mFlags, true);
        }
        return mLayout + describeFlags(mFlags, true);
    }

    public static String describeFlags(int flags) {
        return describeFlags(flags, false);
    }

    public static String describeFlags(int flags, boolean padded) {
        if (flags == 0) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(" ", "", padded ? " " : "");

        // Extensions
        if ((flags & kSubroutine_Flag) != 0) {
            joiner.add("subroutine");
        }
        if ((flags & kPure_Flag) != 0) {
            joiner.add("__pure");
        }
        if ((flags & kInline_Flag) != 0) {
            joiner.add("inline");
        }
        if ((flags & kNoInline_Flag) != 0) {
            joiner.add("noinline");
        }

        // Real GLSL qualifiers
        if ((flags & kSmooth_Flag) != 0) {
            joiner.add("smooth");
        }
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
        if ((flags & kCoherent_Flag) != 0) {
            joiner.add("coherent");
        }
        if ((flags & kVolatile_Flag) != 0) {
            joiner.add("volatile");
        }
        if ((flags & kRestrict_Flag) != 0) {
            joiner.add("restrict");
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
