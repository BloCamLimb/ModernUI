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

import icyllis.arc3d.core.*;

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
            m.setRotate(MathUtil.DEG_TO_RAD * 45);
        });

        Matrix m1 = new Matrix(1.0f, 0, 0,
                0, 1.0f, 1,
                20, 50, 1);

        m1.preShear(2.0f, 0);

        log(pw, m -> m.set(m1));

        print5x4Mul(pw, "lhs", "rhs");

        Matrix4 m4 = new Matrix4();
        m4.setPerspective(MathUtil.PI_O_2, 1, 0.01f, 2000f);
        Vector3 v4 = new Vector3();
        v4.x = 2;
        v4.y = 2;
        v4.z = 10000;
        m4.preTransform(v4);
        pw.println(v4);
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

    public static void print5x4Mul(PrintWriter pw, String lhs, String rhs) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                pw.print("float f");
                pw.print(i + 1);
                pw.print(j + 1);
                pw.print("=");
                for (int k = 0; k < 4; k++) {
                    pw.print(lhs);
                    pw.print('[');
                    pw.print(i * 4 + k);
                    pw.print(']');
                    pw.print('*');
                    pw.print(rhs);
                    pw.print('[');
                    pw.print(k * 4 + j);
                    pw.print(']');
                    if (k < 3) {
                        pw.print('+');
                    }
                }
                pw.println(';');
            }
        }
        for (int j = 0; j < 4; j++) {
            pw.print("float f");
            pw.print(5);
            pw.print(j + 1);
            pw.print("=");
            for (int k = 0; k < 5; k++) {
                if (k < 4) {
                    pw.print(lhs);
                    pw.print('[');
                    pw.print(16 + k);
                    pw.print(']');
                    pw.print('*');
                }
                pw.print(rhs);
                pw.print('[');
                pw.print(k * 4 + j);
                pw.print(']');
                if (k < 4) {
                    pw.print('+');
                }
            }
            pw.println(';');
        }
    }
}
