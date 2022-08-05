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

/**
 * Holds the compiler settings for a program.
 */
public class ProgramSettings {

    /**
     * SkSL supports several different program kinds.
     */
    public static final byte
            ProgramKind_Fragment = 0,
            ProgramKind_Vertex = 1,
            ProgramKind_Compute = 2;
    //TODO more ProgramKinds

    // If true, implicit conversions to lower precision numeric types are allowed (e.g., float to
    // half). These are always allowed when compiling Runtime Effects.
    public boolean mAllowNarrowingConversions = false;
}
