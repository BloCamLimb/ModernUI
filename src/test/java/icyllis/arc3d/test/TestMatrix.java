/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.MathUtil;
import icyllis.arc3d.core.Matrix;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class TestMatrix {

    public static void main(String[] args) {
        var pw = new PrintWriter(System.out, true, StandardCharsets.UTF_8);

        float epsilon = calcMachineEpsilon();

        log(pw, m -> m.set(1, 0, 0,
                0, 1, 0,
                Float.NaN, 0, 1));

        log(pw, m -> m.set(0.7f, 0, 0,
                0, 0.5f, 0,
                0, 0, 1));

        log(pw, m -> {
            m.setRotate(90 * MathUtil.DEG_TO_RAD);
            m.preRotate(90 * MathUtil.DEG_TO_RAD);
        });

        log(pw, m -> {
            m.set(2.5f, 0, 0,
                    0, 3.5f, 0,
                    20, 50, 1);
            boolean res = m.invert();
            pw.println("Invert: " + res);
        });

        log(pw, m -> {
            m.set(2.5f, 0, 0,
                    0, 3.5f, 1,
                    20, 50, 1);
        });

        Matrix m1 = new Matrix(2.5f, 0, 0,
                0, 3.5f, 1,
                20, 50, 1);
        Matrix m2 = new Matrix(m1);

        m1.preShear(0.2f, -0.25f);
        Matrix m3 = new Matrix();
        m3.setShear(0.2f, -0.25f);
        m2.preConcat(m3);

        pw.println(Matrix.equals(m1, m2));
    }

    public static void log(PrintWriter pw, Consumer<Matrix> c) {
        var m = new Matrix();
        c.accept(m);
        pw.println(m);
        pw.println("Type: " + Integer.toBinaryString(0b1_0000 | m.getType())
                .substring(1));
        pw.println("Similarity: " + m.isSimilarity());
        pw.println("AxisAligned: " + m.isAxisAligned());
        pw.println("PreservesRightAngles: " + m.preservesRightAngles());
        pw.println();
    }

    public static float calcMachineEpsilon() {
        float machEps = 1.0f;
        do {
            machEps /= 2.0f;
        } while (1.0f + (machEps / 2.0f) != 1.0f);
        return machEps;
    }
}
