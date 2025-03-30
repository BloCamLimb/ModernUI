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

import icyllis.arc3d.granite.PathUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TestTessellate {

    public static void main(String[] args) throws IOException {
        float x0 = 163;
        float y0 = 119;
        float x1 = 61;
        float y1 = 5;
        float x2 = 108;
        float y2 = 194;
        float x3 = 42;
        float y3 = 94;
        /*if (true) {
            x0 = (int) (Math.random() * 256);
            y0 = (int) (Math.random() * 256);
            x1 = (int) (Math.random() * 256);
            y1 = (int) (Math.random() * 256);
            x2 = (int) (Math.random() * 256);
            y2 = (int) (Math.random() * 256);
            x3 = (int) (Math.random() * 256);
            y3 = (int) (Math.random() * 256);
        }*/
        System.out.printf("%.0f %.0f %.0f %.0f %.0f %.0f %.0f %.0f\n",
                x0, y0, x1, y1, x2, y2, x3, y3);
        float tol = 0.05f;
        int numPoints = PathUtils.countCubicPoints(
                x0, y0, x1, y1, x2, y2, x3, y3,
                tol
        );
        System.out.println("Number of Points from Wang's Formula " + numPoints);

        float[] coords = new float[numPoints << 1];
        int actualNumCoords = PathUtils.generateCubicPoints(
                x0, y0, x1, y1, x2, y2, x3, y3,
                tol * tol,
                coords, 0, coords.length
        );
        System.out.println("Number of Points from Tessellation " + (actualNumCoords >> 1));

        var img1 = new BufferedImage(256, 256, BufferedImage.TYPE_BYTE_GRAY);
        var img2 = new BufferedImage(256, 256, BufferedImage.TYPE_BYTE_GRAY);
        var g2d1 = img1.createGraphics();
        g2d1.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        var g2d2 = img2.createGraphics();
        g2d2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float lastX = x0;
        float lastY = y0;
        var path = new Path2D.Float();
        path.moveTo(x0, y0);
        for (int i = 0; i < actualNumCoords; i += 2) {
            var line = new Line2D.Float(lastX, lastY, coords[i], coords[i | 1]);
            path.lineTo(line.x2, line.y2);
            System.out.printf("Line %d: (%f, %f) to (%f, %f)\n",
                    i >> 1, line.x1, line.y1, line.x2, line.y2);
            lastX = line.x2;
            lastY = line.y2;
        }
        g2d1.draw(path);
        path.reset();
        path.moveTo(x0, y0);
        path.curveTo(x1, y1, x2, y2, x3, y3);
        g2d2.draw(path);
        ImageIO.write(img1, "png", new File("test_tessellate1.png"));
        ImageIO.write(img2, "png", new File("test_tessellate2.png"));
    }
}
