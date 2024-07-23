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

public class TestPath {

    public static void main(String[] args) {
        Path result;
        result = testMiterJoin();
        result = testRoundJoin();
        TestPathUtils.writePath(result, false, "test_path_stroke_round_join.png");
        System.out.println("Empty path bytes: " + new Path().estimatedByteSize());
    }

    public static Path testMiterJoin() {
        Path src = new Path();
        src.moveTo(100, 120);
        src.lineTo(130, 160);
        src.lineTo(50, 120);
        System.out.println("Src path:");
        src.forEach(TestPathUtils.PRINTER);

        Path dst = new Path();
        Stroke stroke = new Stroke();
        stroke.setWidth(10);
        stroke.setStrokeParams(Paint.CAP_ROUND, Paint.JOIN_MITER, 4);
        stroke.applyToPath(src, dst);
        System.out.println("Dst path:");
        dst.forEach(TestPathUtils.PRINTER);

        System.out.println("Src bounds: " + src.getBounds() + ", bytes: " + src.estimatedByteSize());
        for (int i = 0; i < 2; i++) {
            System.out.println("Dst bounds: " + dst.getBounds() + ", bytes: " + dst.estimatedByteSize());
            dst.trimToSize();
        }

        return dst;
    }

    public static Path testRoundJoin() {
        Path src = new Path();
        src.moveTo(100, 120);
        src.lineTo(130, 160);
        //src.lineTo(100, 200);
        //src.lineTo(180, 140);
        //src.lineTo(170, 130);
        //src.lineTo(170, 120);
        src.cubicTo(160, 130, 120, 100, 190, 60);
        System.out.println("Src path:");
        src.forEach(TestPathUtils.PRINTER);

        Path dst = new Path();
        Stroke stroke = new Stroke();
        stroke.setWidth(10);
        stroke.setStrokeParams(Paint.CAP_ROUND, Paint.JOIN_ROUND, 4);
        stroke.applyToPath(src, dst);
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
