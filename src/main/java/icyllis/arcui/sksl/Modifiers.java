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

/**
 * A set of qualifier keywords (in, out, uniform, etc.) appearing before a declaration.
 *
 * @param layout layout qualifiers
 * @param flags  other qualifiers
 */
public record Modifiers(Layout layout, int flags) {

    private static final Modifiers EMPTY = new Modifiers(Layout.empty(), 0);

    /**
     * OpenGL 4.2 or ARB_shading_language_420pack removes the ordering restriction in most cases.
     */
    // GLSL interpolation qualifiers, only one qualifier can be used
    public static final int
            kSmooth_Flag = 1,               // (default and can be explicitly declared)
            kFlat_Flag = 1 << 1,            // no interpolation
            kNoPerspective_Flag = 1 << 2;   // linear interpolation
    // GLSL storage qualifiers, only one qualifier can be used
    // GLSL parameter qualifiers, one of const, in, out, inout
    public static final int
            kConst_Flag = 1 << 3,
            kUniform_Flag = 1 << 4,
            kIn_Flag = 1 << 5,
            kOut_Flag = 1 << 6;
    // We use the Metal name for this one (corresponds to the GLSL 'shared' modifier)
    public static final int
            kThreadgroup_Flag = 1 << 7;  // for compute shaders
    // SkSL extensions, not present in GLSL
    public static final int
            kHasSideEffects_Flag = 1 << 8,
            kInline_Flag = 1 << 9,
            kNoInline_Flag = 1 << 10;

    @Nonnull
    public static Modifiers empty() {
        return EMPTY;
    }

    @Nonnull
    public String description() {
        final StringBuilder result = layout.descriptionBuilder();

        // SkSL extensions
        if ((flags & kHasSideEffects_Flag) != 0) {
            result.append("sk_has_side_effects ");
        }
        if ((flags & kInline_Flag) != 0) {
            result.append("inline ");
        }
        if ((flags & kNoInline_Flag) != 0) {
            result.append("noinline ");
        }

        // Real GLSL qualifiers (must be specified in order in GLSL 4.1 and below)
        if ((flags & kSmooth_Flag) != 0) {
            result.append("smooth ");
        }
        if ((flags & kFlat_Flag) != 0) {
            result.append("flat ");
        }
        if ((flags & kNoPerspective_Flag) != 0) {
            result.append("noperspective ");
        }
        if ((flags & kConst_Flag) != 0) {
            result.append("const ");
        }
        if ((flags & kUniform_Flag) != 0) {
            result.append("uniform ");
        }
        if ((flags & kIn_Flag) != 0 && (flags & kOut_Flag) != 0) {
            result.append("inout ");
        } else if ((flags & kIn_Flag) != 0) {
            result.append("in ");
        } else if ((flags & kOut_Flag) != 0) {
            result.append("out ");
        }

        // We're using a non-GLSL name for this one; the GLSL equivalent is "shared"
        if ((flags & kThreadgroup_Flag) != 0) {
            result.append("threadgroup ");
        }

        return result.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Modifiers modifiers = (Modifiers) o;
        return flags == modifiers.flags && layout.equals(modifiers.layout);
    }

    @Override
    public int hashCode() {
        return 31 * layout.hashCode() + flags;
    }
}
