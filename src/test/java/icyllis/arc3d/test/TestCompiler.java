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

        ModuleUnit parsed = compiler.parseModule(
                ExecutionModel.BASE,
                """
                float rr(vec2 a, vec2 b) {
                    // this will compile into 1.0
                    return a.x1.y.x;
                }
                float sa(float a) {return a;}
                float rand(vec2 n) {
                    return sa(sa(rr(n, vec2(12.9898,12.1414))) * 83758.5453);
                }
                """,
                ModuleLoader.getInstance().getRootModule()
        );

        System.out.println(compiler.getLogMessage());
        System.out.println(parsed);
    }
}
