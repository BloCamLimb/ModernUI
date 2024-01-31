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

public class TestCompiler {

    public static void main(String[] args) {
        var compiler = new ShaderCompiler();

        SharedLibrary lib = compiler.parseLibrary(
                ExecutionModel.BASE,
                """
                float rand(vec2 n) {
                    return fract(sin(dot(n, vec2(12.9898,12.1414))) * 83758.5453);
                }
                void main() {
                    vec2 pos = f_Position;
                    float dist = abs(pos.y-sin(pos.x*10.0-u_Color.x*5.0)*0.1-cos(pos.x*5.0)*0.05);
                    dist = pow(0.1/dist,0.8);
                    vec4 col = vec4(mix(vec3(0.2,0.85,0.95),vec3(0.85,0.5,0.75),pos.x*0.5+0.5),1.0);
                    col *= (dist+rand(pos.yx)*0.05);
                    col = 1.0 - exp(-col*0.5);
                    FragColor0 = col;
                }
                """,
                LibraryLoader.getInstance().getRootLibrary()
        );

        System.out.println(compiler.getLogMessage());
        System.out.println(lib);
    }
}
