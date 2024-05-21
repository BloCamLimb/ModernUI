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

import icyllis.arc3d.core.Paint;
import icyllis.arc3d.core.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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

    public static class J2DPathConverter implements PathConsumer {

        private GeneralPath mDst;

        public Path2D convert(Path src) {
            mDst = new GeneralPath();
            src.forEach(this);
            Path2D ret = mDst;
            mDst = null;
            return ret;
        }

        @Override
        public void moveTo(float x, float y) {
            mDst.moveTo(x, y);
        }

        @Override
        public void lineTo(float x, float y) {
            mDst.lineTo(x, y);
        }

        @Override
        public void quadTo(float x1, float y1, float x2, float y2) {
            mDst.quadTo(x1, y1, x2, y2);
        }

        @Override
        public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
            mDst.curveTo(x1, y1, x2, y2, x3, y3);
        }

        @Override
        public void closePath() {
            mDst.closePath();
        }

        @Override
        public void pathDone() {

        }
    }

    private static final J2DPathConverter J2D_PATH_CONVERTER = new J2DPathConverter();

    public static void main(String[] args) {
        Path result;
        PathStroker stroker = new PathStroker();
        result = testMiterJoin(stroker);
        result = testRoundJoin(stroker);
        //writePath(result, false, "test_path_stroke_round_join.png");
        System.out.println("Empty path bytes: " + new Path().estimatedByteSize());
    }

    public static Path testMiterJoin(PathStroker stroker) {
        Path src = new Path();
        src.moveTo(100, 120);
        src.lineTo(130, 160);
        src.lineTo(50, 120);
        src.forEach(PRINTER);

        Path dst = new Path();
        stroker.init(dst, 5, Paint.CAP_ROUND, Paint.JOIN_MITER, 4, 1);
        src.forEach(stroker);
        dst.forEach(PRINTER);

        System.out.println("Src bounds: " + src.getBounds() + ", bytes: " + src.estimatedByteSize());
        for (int i = 0; i < 2; i++) {
            System.out.println("Dst bounds: " + dst.getBounds() + ", bytes: " + dst.estimatedByteSize());
            dst.trimToSize();
        }

        return dst;
    }

    public static Path testRoundJoin(PathStroker stroker) {
        Path src = new Path();
        src.moveTo(100, 120);
        src.lineTo(130, 160);
        //src.lineTo(100, 200);
        //src.lineTo(180, 140);
        //src.lineTo(170, 130);
        //src.lineTo(170, 120);
        src.cubicTo(160, 130, 120, 100, 190, 60);
        src.forEach(PRINTER);

        Path dst = new Path();
        stroker.init(dst, 5, Paint.CAP_ROUND, Paint.JOIN_ROUND, 4, 1);
        src.forEach(stroker);
        dst.forEach(PRINTER);

        System.out.println("Src bounds: " + src.getBounds() + ", bytes: " + src.estimatedByteSize());
        for (int i = 0; i < 2; i++) {
            System.out.println("Dst bounds: " + dst.getBounds() + ", bytes: " + dst.estimatedByteSize());
            dst.trimToSize();
        }

        return dst;
    }

    public static void writePath(Path src, boolean stroke, String outName) {
        var path = J2D_PATH_CONVERTER.convert(src);
        var image = new BufferedImage(256, 256, BufferedImage.TYPE_BYTE_GRAY);
        var graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (stroke) {
            graphics.draw(path);
        } else {
            graphics.fill(path);
        }
        try {
            ImageIO.write(image, "png", new File(outName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
