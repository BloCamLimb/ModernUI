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

import icyllis.arc3d.sketch.Paint;
import icyllis.arc3d.sketch.Path;
import icyllis.arc3d.sketch.StrokeRec;
import icyllis.arc3d.sketch.j2d.J2DUtils;

import java.awt.BasicStroke;

public class TestPath {

    public static void main(String[] args) {
        Path src = new Path();
        src.moveTo(100, 120);
        src.lineTo(130, 160);
        src.lineTo(100, 200);
        src.lineTo(180, 140);
        src.lineTo(170, 130);
        src.lineTo(170, 120);
        src.cubicTo(160, 130, 120, 100, 190, 60);
        System.out.println("Src path:");
        src.forEach(TestPathUtils.PRINTER);

        Path result;
        //result = testMiterJoin();
        result = testRoundJoin(src);
        TestPathUtils.writePath(result, false, "test_path_stroke_round_join.png");
        result = testRoundJoin2(src);
        TestPathUtils.writePath(result, false, "test_path_stroke_round_join2.png");
        System.out.println("Empty path bytes: " + new Path().estimatedByteSize());
    }

    /*public static Path testMiterJoin() {
        Path src = new Path();
        src.moveTo(100, 120);
        src.lineTo(130, 160);
        src.lineTo(50, 120);
        System.out.println("Src path:");
        src.forEach(TestPathUtils.PRINTER);

        Path dst = new Path();
        StrokeRec strokeRec = new StrokeRec();
        strokeRec.setWidth(10);
        strokeRec.setStrokeParams(Paint.CAP_ROUND, Paint.JOIN_MITER, Paint.ALIGN_CENTER, 4);
        strokeRec.applyToPath(src, dst);
        System.out.println("Dst path:");
        dst.forEach(TestPathUtils.PRINTER);

        System.out.println("Src bounds: " + src.getBounds() + ", bytes: " + src.estimatedByteSize());
        for (int i = 0; i < 2; i++) {
            System.out.println("Dst bounds: " + dst.getBounds() + ", bytes: " + dst.estimatedByteSize());
            dst.trimToSize();
        }

        return dst;
    }*/

    public static Path testRoundJoin(Path src) {
        Path dst = new Path();
        StrokeRec strokeRec = new StrokeRec();
        strokeRec.setWidth(10);
        strokeRec.setStrokeParams(Paint.CAP_ROUND, Paint.JOIN_ROUND, Paint.ALIGN_CENTER, 4);
        strokeRec.applyToPath(src, dst);
        System.out.println("Dst path:");
        dst.forEach(TestPathUtils.PRINTER);

        System.out.println("Src bounds: " + src.getBounds() + ", bytes: " + src.estimatedByteSize());
        for (int i = 0; i < 2; i++) {
            System.out.println("Dst bounds: " + dst.getBounds() + ", bytes: " + dst.estimatedByteSize());
            dst.trimToSize();
        }

        return dst;
    }

    public static Path testRoundJoin2(Path src) {
        Path dst = J2DUtils.toPath(
                new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 4)
                .createStrokedShape(src)
                .getPathIterator(null),
                null
        );
        System.out.println("Dst path:");
        dst.forEach(TestPathUtils.PRINTER);

        System.out.println("Src bounds: " + src.getBounds() + ", bytes: " + src.estimatedByteSize());
        for (int i = 0; i < 2; i++) {
            System.out.println("Dst bounds: " + dst.getBounds() + ", bytes: " + dst.estimatedByteSize());
            dst.trimToSize();
        }

        return dst;
    }
}
