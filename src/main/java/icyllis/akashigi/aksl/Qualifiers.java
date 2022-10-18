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
 * A set of qualifier keywords (in, out, uniform, etc.) appearing before a declaration.
 *
 * @param layout layout qualifiers
 * @param flags  other qualifiers
 */
public record Qualifiers(Layout layout, int flags) {

    private static final Qualifiers EMPTY = new Qualifiers(Layout.empty(), 0);

    /**
     * OpenGL 4.2 or ARB_shading_language_420pack removes the ordering restriction in most cases.
     */
    // GLSL interpolation qualifiers, only one qualifier can be used
    public static final int
            FLAT_FLAG = 1,
            NO_PERSPECTIVE_FLAG = 1 << 1;
    // GLSL storage qualifiers, only one qualifier can be used
    // GLSL parameter qualifiers, one of const, in, out, inout
    public static final int
            CONST_FLAG = 1 << 2,
            UNIFORM_FLAG = 1 << 3,
            IN_FLAG = 1 << 4,
            OUT_FLAG = 1 << 5;
    // GLSL memory qualifiers
    public static final int
            READ_ONLY_FLAG = 1 << 6,
            WRITE_ONLY_FLAG = 1 << 7;
    // Other GLSL storage qualifiers
    public static final int
            BUFFER_FLAG = 1 << 8,
            WORKGROUP_FLAG = 1 << 9;  // GLSL 'shared'
    // Extensions, not present in GLSL
    public static final int
            PURE_FLAG = 1 << 10,
            INLINE_FLAG = 1 << 11,
            NO_INLINE_FLAG = 1 << 12;

    @Nonnull
    public static Qualifiers empty() {
        return EMPTY;
    }

    @Nonnull
    @Override
    public String toString() {
        return layout.toString() + describeFlags(flags) + " ";
    }

    public static String describeFlags(int flags) {
        StringJoiner joiner = new StringJoiner(" ");

        // Extensions
        if ((flags & PURE_FLAG) != 0) {
            joiner.add("pure");
        }
        if ((flags & INLINE_FLAG) != 0) {
            joiner.add("inline");
        }
        if ((flags & NO_INLINE_FLAG) != 0) {
            joiner.add("noinline");
        }

        // Real GLSL qualifiers
        if ((flags & FLAT_FLAG) != 0) {
            joiner.add("flat");
        }
        if ((flags & NO_PERSPECTIVE_FLAG) != 0) {
            joiner.add("noperspective");
        }
        if ((flags & CONST_FLAG) != 0) {
            joiner.add("const");
        }
        if ((flags & UNIFORM_FLAG) != 0) {
            joiner.add("uniform");
        }
        if ((flags & IN_FLAG) != 0 && (flags & OUT_FLAG) != 0) {
            joiner.add("inout");
        } else if ((flags & IN_FLAG) != 0) {
            joiner.add("in");
        } else if ((flags & OUT_FLAG) != 0) {
            joiner.add("out");
        }
        if ((flags & READ_ONLY_FLAG) != 0) {
            joiner.add("readonly");
        }
        if ((flags & WRITE_ONLY_FLAG) != 0) {
            joiner.add("writeonly");
        }
        if ((flags & BUFFER_FLAG) != 0) {
            joiner.add("buffer");
        }

        // We're using a non-GLSL name for this one; the GLSL equivalent is "shared"
        if ((flags & WORKGROUP_FLAG) != 0) {
            joiner.add("workgroup");
        }

        return joiner.toString();
    }
}
