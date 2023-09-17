/*
 * Arc 3D.
 * Copyright (C) 2022-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.shaderc.codegen;

import icyllis.arc3d.shaderc.tree.Program;
import icyllis.arc3d.engine.Context;

/**
 * Abstract superclass of all code generators, which take a Program as input and produce code as
 * output.
 */
public abstract class CodeGenerator {

    public final Context mContext;
    public final Program mProgram;
    public StringBuilder mOut;

    public CodeGenerator(Context context, Program program, StringBuilder out) {
        mContext = context;
        mProgram = program;
        mOut = out;
    }
}
