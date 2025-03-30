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

import icyllis.arc3d.sketch.Matrix;
import icyllis.arc3d.sketch.Path;
import icyllis.arc3d.sketch.PathConsumer;
import icyllis.arc3d.sketch.PathMeasure;

public class TestPathMeasure {

    public static final PathConsumer PRINTER = new PathConsumer() {
        @Override
        public void moveTo(float x, float y) {
            System.out.printf("path.moveTo(%f, %f);\n", x, y);
        }

        @Override
        public void lineTo(float x, float y) {
            System.out.printf("path.lineTo(%f, %f);\n", x, y);
        }

        @Override
        public void quadTo(float x1, float y1, float x2, float y2) {
            System.out.printf("path.quadTo(%f, %f, %f, %f);\n", x1, y1, x2, y2);
        }

        @Override
        public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
            System.out.printf("path.cubicTo(%f, %f, %f, %f, %f, %f);\n", x1, y1, x2, y2, x3, y3);
        }

        @Override
        public void close() {
            System.out.println("path.close();");
        }

        @Override
        public void done() {
            System.out.println("===== PATH DONE =====");
        }
    };

    public static void main(String[] args) {
        var path = new Path();
        path.moveTo(100, 120);
        path.lineTo(130, 160);
        path.cubicTo(160, 130, 120, 100, 190, 60);

        var measure = new PathMeasure(path, false);
        float length = measure.getContourLength();
        System.out.println("Path length " + length);

        var dst = new Path();
        boolean res = measure.getSegment(0, length * 0.7f, dst, true);
        if (res) {
            dst.forEach(PRINTER);
        } else {
            System.out.println("Failed");
        }

        var matrix = new Matrix();
        res = measure.getMatrix(length * 0.5f, matrix, PathMeasure.MATRIX_FLAG_GET_POS_AND_TAN);
        if (res) {
            System.out.println(matrix);
        } else {
            System.out.println("Failed");
        }

        if (measure.nextContour()) {
            System.out.println("Next contour");
        }

        measure.reset();
        path.reset();
    }
}
