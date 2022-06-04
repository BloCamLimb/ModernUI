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
import java.util.Objects;

/**
 * A set of qualifier keywords (in, out, uniform, etc.) appearing before a declaration.
 *
 * @param layout the layout qualifiers
 * @param flags  the other qualifiers
 */
@Immutable
public record Qualifiers(Layout layout, int flags) {

    /**
     * OpenGL 4.2 or ARB_shading_language_420pack removes the ordering restriction in most cases.
     * <p>
     * IN, OUT may be storage qualifier, or parameter qualifier
     */
    // GLSL interpolation qualifiers, only one qualifier can be used
    public static final int
            SMOOTH_FLAG = 0,                // (default) perspective correct interpolation
            FLAT_FLAG = 1,                  // no interpolation
            NO_PERSPECTIVE_FLAG = 1 << 1;   // linear interpolation

    // GLSL storage qualifiers, only one qualifier can be used
    // GLSL parameter qualifiers, one of const, in, out, inout
    public static final int
            UNIFORM_FLAG = 1 << 2,
            BUFFER_FLAG = 1 << 3,
            CONST_FLAG = 1 << 4,
            IN_FLAG = 1 << 5,
            OUT_FLAG = 1 << 6;

    // SkSL extensions, not present in GLSL
    public static final int
            HAS_SIDE_EFFECTS_FLAG = 1 << 10,
            INLINE_FLAG = 1 << 11,
            NO_INLINE_FLAG = 1 << 12;

    @Nonnull
    public String getDescription() {
        final StringBuilder result = layout.getDescriptionBuilder();
        result.append(" ");

        // SkSL extensions
        if ((flags & HAS_SIDE_EFFECTS_FLAG) != 0) {
            result.append("sk_has_side_effects ");
        }
        if ((flags & NO_INLINE_FLAG) != 0) {
            result.append("noinline ");
        }

        // Real GLSL qualifiers (must be specified in order in GLSL 4.1 and below)
        if ((flags & FLAT_FLAG) != 0) {
            result.append("flat ");
        }
        if ((flags & NO_PERSPECTIVE_FLAG) != 0) {
            result.append("noperspective ");
        }
        if ((flags & UNIFORM_FLAG) != 0) {
            result.append("uniform ");
        }
        if ((flags & BUFFER_FLAG) != 0) {
            result.append("buffer ");
        }
        if ((flags & CONST_FLAG) != 0) {
            result.append("const ");
        }
        if ((flags & IN_FLAG) != 0 && (flags & OUT_FLAG) != 0) {
            result.append("inout ");
        } else if ((flags & IN_FLAG) != 0) {
            result.append("in ");
        } else if ((flags & OUT_FLAG) != 0) {
            result.append("out ");
        }

        return result.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Qualifiers that = (Qualifiers) o;
        if (flags != that.flags) return false;
        return Objects.equals(layout, that.layout);
    }

    @Override
    public int hashCode() {
        int result = layout != null ? layout.hashCode() : 0;
        result = 31 * result + flags;
        return result;
    }
}
