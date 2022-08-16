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

import icyllis.arcui.core.SLType;
import icyllis.arcui.engine.ShaderVar;
import icyllis.arcui.sksl.Compiler;

/**
 * This class implements the various vertex builder interfaces.
 */
public class VertexShaderBuilder extends ShaderBuilderBase implements VertexGeoBuilder {

    public VertexShaderBuilder(ProgramBuilder programBuilder) {
        super(programBuilder);
    }

    @Override
    protected void onFinish() {
        mProgramBuilder.varyingHandler().getVertDecls(inputs(), outputs());
    }

    @Override
    public void emitNormalizedPosition(ShaderVar worldPos) {
        if (worldPos.getType() == SLType.Vec3) {
            codeAppendf("""
                    gl_Position = vec4(%1$s.xy * %2$s.xz + %1$s.zz * %2$s.yw, 0.0, %1$s.z);
                    """, worldPos.getName(), Compiler.ORTHOPROJ_NAME);
        } else {
            assert (worldPos.getType() == SLType.Vec2);
            codeAppendf("""
                    gl_Position = vec4(%1$s.xy * %2$s.xz + %2$s.yw, 0.0, 1.0);
                    """, worldPos.getName(), Compiler.ORTHOPROJ_NAME);
        }
    }
}
