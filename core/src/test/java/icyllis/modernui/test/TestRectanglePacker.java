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

package icyllis.modernui.test;

import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.RectanglePacker;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.lwjgl.glfw.GLFW;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class TestRectanglePacker {

    public static final int WIDTH =  1024;
    public static final int HEIGHT = 1024;

    public static void main(String[] args) throws IOException {
        Configurator.setRootLevel(Level.INFO);

        var bm1 = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        var g2d1 = bm1.createGraphics();

        var bm2 = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        var g2d2 = bm2.createGraphics();

        var bm3 = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        var g2d3 = bm3.createGraphics();

        var bm4 = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        var g2d4 = bm4.createGraphics();

        var bm5 = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        var g2d5 = bm5.createGraphics();

        var packer1 = RectanglePacker.make(WIDTH, HEIGHT, RectanglePacker.ALGORITHM_SKYLINE);
        var packer2 = RectanglePacker.make(WIDTH, HEIGHT, RectanglePacker.ALGORITHM_HORIZON);
        var packer3 = RectanglePacker.make(WIDTH, HEIGHT, RectanglePacker.ALGORITHM_HORIZON_OLD);
        var packer4 = RectanglePacker.make(WIDTH, HEIGHT, RectanglePacker.ALGORITHM_BINARY_TREE);
        var packer5 = RectanglePacker.make(WIDTH, HEIGHT, RectanglePacker.ALGORITHM_POWER2_LINE);
        var random = new Random();

        double time1 = 0;
        double time2 = 0;
        double time3 = 0;
        double time4 = 0;
        double time5 = 0;

        GLFW.glfwInit();

        int n1 = 0, n2 = 0, n3 = 0, n4 = 0, n5 = 0, fails = 0;
        while (fails < 3000) {
            int w = random.nextInt(8, 33);
            int h = random.nextInt(12, 37);
            var col = new Color(random.nextInt(0x1000000));
            {
                var rect = new Rect(0, 0, w, h);
                double start = GLFW.glfwGetTime();
                if (packer1.addRect(rect)) {
                    g2d1.setColor(col);
                    g2d1.drawRect(rect.left, rect.top, rect.width() - 1, rect.height() - 1);
                    ++n1;
                } else {
                    ++fails;
                }
                time1 += GLFW.glfwGetTime() - start;
            }
            {
                var rect = new Rect(0, 0, w, h);
                double start = GLFW.glfwGetTime();
                if (packer2.addRect(rect)) {
                    g2d2.setColor(col);
                    g2d2.drawRect(rect.left, rect.top, rect.width() - 1, rect.height() - 1);
                    ++n2;
                } else {
                    ++fails;
                }
                time2 += GLFW.glfwGetTime() - start;
            }
            {
                var rect = new Rect(0, 0, w, h);
                double start = GLFW.glfwGetTime();
                if (packer3.addRect(rect)) {
                    g2d3.setColor(col);
                    g2d3.drawRect(rect.left, rect.top, rect.width() - 1, rect.height() - 1);
                    ++n3;
                } else {
                    ++fails;
                }
                time3 += GLFW.glfwGetTime() - start;
            }
            {
                var rect = new Rect(0, 0, w, h);
                double start = GLFW.glfwGetTime();
                if (packer4.addRect(rect)) {
                    g2d4.setColor(col);
                    g2d4.drawRect(rect.left, rect.top, rect.width() - 1, rect.height() - 1);
                    ++n4;
                } else {
                    ++fails;
                }
                time4 += GLFW.glfwGetTime() - start;
            }
            {
                var rect = new Rect(0, 0, w, h);
                double start = GLFW.glfwGetTime();
                if (packer5.addRect(rect)) {
                    g2d5.setColor(col);
                    g2d5.drawRect(rect.left, rect.top, rect.width() - 1, rect.height() - 1);
                    ++n5;
                } else {
                    ++fails;
                }
                time5 += GLFW.glfwGetTime() - start;
            }
        }
        System.out.println("Algorithm Skyline:");
        System.out.println(n1 + " rectangles");
        System.out.println("Coverage: " + packer1.getCoverage() + " (↑)");
        System.out.printf("Took %d microseconds (↓)\n", (int) (time1 * 1000000));

        System.out.println("-".repeat(20));

        System.out.println("Algorithm Horizontal Line:");
        System.out.println(n2 + " rectangles");
        System.out.println("Coverage: " + packer2.getCoverage() + " (↑)");
        System.out.printf("Took %d microseconds (↓)\n", (int) (time2 * 1000000));

        System.out.println("-".repeat(20));

        System.out.println("Algorithm Horizontal Line Memoryless:");
        System.out.println(n3 + " rectangles");
        System.out.println("Coverage: " + packer3.getCoverage() + " (↑)");
        System.out.printf("Took %d microseconds (↓)\n", (int) (time3 * 1000000));

        System.out.println("-".repeat(20));

        System.out.println("Algorithm Binary Tree:");
        System.out.println(n4 + " rectangles");
        System.out.println("Coverage: " + packer4.getCoverage() + " (↑)");
        System.out.printf("Took %d microseconds (↓)\n", (int) (time4 * 1000000));

        System.out.println("-".repeat(20));

        System.out.println("Algorithm Power 2 Line:");
        System.out.println(n5 + " rectangles");
        System.out.println("Coverage: " + packer5.getCoverage() + " (↑)");
        System.out.printf("Took %d microseconds (↓)\n", (int) (time5 * 1000000));

        ImageIO.write(bm1, "png", new File("rect_packer1.png"));
        ImageIO.write(bm2, "png", new File("rect_packer2.png"));
        ImageIO.write(bm3, "png", new File("rect_packer3.png"));
        ImageIO.write(bm4, "png", new File("rect_packer4.png"));
        ImageIO.write(bm5, "png", new File("rect_packer5.png"));

        GLFW.glfwTerminate();
    }
}
