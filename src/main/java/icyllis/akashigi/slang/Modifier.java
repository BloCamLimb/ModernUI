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

package icyllis.akashigi.slang;

import java.util.StringJoiner;

/**
 * A set of modifier keywords (in, out, uniform, etc.) appearing before a declaration.
 */
public final class Modifier {

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

    private Modifier() {
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
