/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.test;

import icyllis.arc3d.core.Matrix4;

public class TestAssembly {

    //-XX:+UnlockDiagnosticVMOptions
    //-XX:+UnlockExperimentalVMOptions
    //-XX:CompileCommand="print *Matrix4::*"
    // compile hsdis
    public static void main(String[] args) {
        for (int i = 0; i < 1000000; i++) {
            Matrix4 a = new Matrix4(2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
            Matrix4 b = new Matrix4(2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
            a.preConcat(b);
        }
        Matrix4 a = new Matrix4(2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
        Matrix4 b = new Matrix4(2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
        long time = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            a.preConcat(b);
        }
        time = System.nanoTime() - time;
        System.out.println(a);
        System.out.println(time);
        // the deep dark fantasies
    }
}
