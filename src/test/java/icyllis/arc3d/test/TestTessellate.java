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

import icyllis.arc3d.engine.PathUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TestTessellate {

    public static void main(String[] args) throws IOException {
        float x0 = 220;
        float y0 = 120;
        float x1 = 320;
        float y1 = 140;
        float x2 = 220;
        float y2 = 250;
        float x3 = 270;
        float y3 = 460;
        if (true) {
            x0 = (int) (Math.random() * 512);
            y0 = (int) (Math.random() * 512);
            x1 = (int) (Math.random() * 512);
            y1 = (int) (Math.random() * 512);
            x2 = (int) (Math.random() * 512);
            y2 = (int) (Math.random() * 512);
            x3 = (int) (Math.random() * 512);
            y3 = (int) (Math.random() * 512);
        }
        float tol = PathUtils.DEFAULT_TOLERANCE;
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

        var img = new BufferedImage(512, 512, BufferedImage.TYPE_BYTE_GRAY);
        var g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float lastX = x0;
        float lastY = y0;
        for (int i = 0; i < actualNumCoords; i += 2) {
            var line = new Line2D.Float(lastX, lastY, coords[i], coords[i | 1]);
            g2d.draw(line);
            System.out.printf("Line %d: (%f, %f) to (%f, %f)\n",
                    i >> 1, line.x1, line.y1, line.x2, line.y2);
            lastX = line.x2;
            lastY = line.y2;
        }
        ImageIO.write(img, "png", new File("test_tessellate.png"));
    }
}
