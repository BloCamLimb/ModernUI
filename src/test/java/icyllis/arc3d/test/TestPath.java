/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.test;

import icyllis.arc3d.core.*;

public class TestPath {

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
        public void closePath() {
            System.out.println("path.close();");
        }

        @Override
        public void pathDone() {
            System.out.println("===== PATH DONE =====");
        }
    };

    public static void main(String[] args) {
        testMiterJoin();
    }

    public static void testMiterJoin() {
        Path src = new Path();
        src.moveTo(100, 120);
        src.lineTo(130, 160);
        src.lineTo(50, 120);
        src.forEach(PRINTER);

        Path dst = new Path();
        PathStroker stroker = new PathStroker();
        stroker.init(dst, 5, Paint.CAP_ROUND, Paint.JOIN_MITER, 4, 1);
        src.forEach(stroker);
        dst.forEach(PRINTER);
    }
}
