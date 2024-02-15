/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.test;

import icyllis.arc3d.compiler.*;
import icyllis.arc3d.compiler.tree.TranslationUnit;

public class TestCompiler {

    public static final String SOURCE = """
            const int blockSize = -4 + 6;
            layout(binding = 0) uniform UniformBlock {
                mat4 u_Projection;
                mat4 u_ModelView;
                vec4 u_Color;
            } u_Buffer0[blockSize];
            out SV_PerVertex {
              layout(position) float4 SV_Position;
            };
            layout(location = 0) smooth in vec2 f_Position;
            layout(location = 1) smooth in vec4 f_Color;
            layout(location = 0, index = 0) out vec4 FragColor0;
            float rr(float2 a, float2 b) {
                // this will compile into 1.0
                return float(a.x1.y.x);
            }
            float sa(float a) {
                return a;
            }
            half sa(half a) {
                return a;
            }
            float rand(float2 n) {
                const float[] a = float[](12.9898, n.x), b = float[](12.9898, n.x, n.y);
                return sa(sa(rr(n, float2(a[0],12.1414))) * 83758.5453);
            }
            """;

    public static void main(String[] args) {
        var compiler = new ShaderCompiler();

        TranslationUnit parsed = compiler.parse(
                ExecutionModel.VERTEX,
                new CompileOptions(),
                SOURCE,
                ModuleLoader.getInstance().getRootModule()
        );

        System.out.println(compiler.getLogMessage());
        if (parsed != null) {
            System.out.println(parsed);
            System.out.println(parsed.getUsage());
        }
    }
}
