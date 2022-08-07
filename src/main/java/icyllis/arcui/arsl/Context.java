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

package icyllis.arcui.arsl;

/**
 * Contains compiler-wide objects, which currently means the core types.
 */
public class Context {

    // The Context holds all of the built-in types.
    public final BuiltinTypes mTypes = new BuiltinTypes();

    // The Context holds a reference to our shader caps bits.
    public final ShaderCaps mCaps;

    // The Context holds a pointer to the configuration of the program being compiled.
    public byte mKind;
    public ProgramSettings mSettings;
    public boolean mIsBuiltinCode;

    // The Context holds a pointer to our error reporter.
    public ErrorReporter mErrors;

    // The Context holds a pointer to the shared name-mangler.
    public Mangler mMangler;

    public Context(ShaderCaps caps, ErrorReporter errors, Mangler mangler) {
        mCaps = caps;
        mErrors = errors;
        mMangler = mangler;
    }
}
