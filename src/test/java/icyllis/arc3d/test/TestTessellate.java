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
import java.util.Arrays;

public class TestTessellate {

    public static void main(String[] args) throws IOException {
        float tol = 0.25f;
        int numPoints = PathUtils.countQuadraticPoints(
                20, 120, 120, 140, 20, 320,
                tol
        );
        System.out.println("numPoints " + numPoints);

        float[] coords = new float[numPoints * 2];
        int actualNumCoords = PathUtils.generateQuadraticPoints(
                20, 120, 120, 140, 20, 320,
                tol * tol,
                coords, 0, coords.length
        );
        System.out.println("actual numPoints " + actualNumCoords / 2);

        System.out.println(Arrays.toString(coords));

        var bufImg = new BufferedImage(512, 512, BufferedImage.TYPE_BYTE_GRAY);
        var g2d = bufImg.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < actualNumCoords; i += 2) {
            Line2D.Float line;
            if (i == 0) {
                line = new Line2D.Float(20, 120, coords[i], coords[i + 1]);
            } else {
                line = new Line2D.Float(coords[i - 2], coords[i - 1], coords[i], coords[i + 1]);
            }
            g2d.draw(line);
        }
        ImageIO.write(bufImg, "png", new File("test_tessellate.png"));
    }
}
