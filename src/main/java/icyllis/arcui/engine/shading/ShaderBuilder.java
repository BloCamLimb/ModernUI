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

package icyllis.arcui.engine.shading;

import java.util.ArrayList;

/**
 * Base class for all shaders builders
 */
public abstract class ShaderBuilder {

    protected static final int
            EXTENSIONS = 0,
            DEFINITIONS = 1,
            PRECISION_QUALIFIER = 2,
            LAYOUT_QUALIFIERS = 3,
            UNIFORMS = 4,
            INPUTS = 5,
            OUTPUTS = 6,
            FUNCTIONS = 7,
            MAIN = 8,
            CODE = 9;
    protected static final int PRESERVE = CODE + 6; // 6 == Reasonable upper bound on number of processor stages

    protected final ProgramBuilder mProgramBuilder;
    protected final ArrayList<StringBuilder> mShaderStrings = new ArrayList<>(PRESERVE);
    protected String mCompilerString;

    public ShaderBuilder(ProgramBuilder programBuilder) {
        mProgramBuilder = programBuilder;
    }
}
