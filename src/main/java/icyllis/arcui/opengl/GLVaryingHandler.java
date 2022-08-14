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

package icyllis.arcui.opengl;

import icyllis.arcui.engine.shading.ProgramBuilder;
import icyllis.arcui.engine.shading.VaryingHandler;

public class GLVaryingHandler extends VaryingHandler {

    GLVaryingHandler(ProgramBuilder programBuilder) {
        super(programBuilder);
    }

    @Override
    protected void onFinish() {
        // OpenGL 3.3: explicit_attrib_location
        assignSequentialLocations(mVertexInputs);
        assignSequentialLocations(mVertexOutputs);
        assignSequentialLocations(mFragInputs);
        assignSequentialLocations(mFragOutputs);
    }
}
