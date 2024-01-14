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

import icyllis.arc3d.core.*;
import org.lwjgl.stb.*;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

public class TestRectanglePacker {

    public static final int ALGORITHM_STB_SKYLINE = 10005;
    public static final int ALGORITHM_STB_SKYLINE_BEST = 10006;

    public static final int WIDTH = 1024;
    public static final int HEIGHT = 1024;

    public static void main(String[] args) throws Exception {
        var packer1 = new Packer(RectanglePacker.ALGORITHM_SKYLINE);
        var packer2 = new Packer(RectanglePacker.ALGORITHM_HORIZON);
        var packer4 = new Packer(RectanglePacker.ALGORITHM_BINARY_TREE);
        var packer5 = new Packer(RectanglePacker.ALGORITHM_POWER2_LINE);
        var packer6 = new Packer(ALGORITHM_STB_SKYLINE);
        var packer7 = new Packer(ALGORITHM_STB_SKYLINE_BEST);
        var packer8 = new Packer(RectanglePacker.ALGORITHM_SKYLINE_NEW);

        var random = new Random();

        boolean shouldContinue;
        do {
            int w = MathUtil.clamp(20 + (int) (6 * random.nextGaussian()), 8, 90);
            int h = MathUtil.clamp(24 + (int) (6 * random.nextGaussian()), 12, 90);
            var col = new Color(random.nextInt(0x1000000));
            shouldContinue =
                    packer1.add(w, h, col) |
                            packer2.add(w, h, col) |
                            packer4.add(w, h, col) |
                            packer5.add(w, h, col) |
                            packer6.add(w, h, col) |
                            packer7.add(w, h, col) |
                            packer8.add(w, h, col);
        } while (shouldContinue);

        System.out.println("Algorithm Skyline:");
        packer1.print();

        System.out.println("-".repeat(20));

        System.out.println("Algorithm Skyline (New):");
        packer8.print();

        System.out.println("-".repeat(20));

        System.out.println("Algorithm STB Skyline:");
        packer6.print();

        System.out.println("-".repeat(20));

        System.out.println("Algorithm STB Skyline (Best):");
        packer7.print();

        System.out.println("-".repeat(20));

        System.out.println("Algorithm Horizon:");
        packer2.print();

        System.out.println("-".repeat(20));

        System.out.println("Algorithm Binary Tree:");
        packer4.print();

        System.out.println("-".repeat(20));

        System.out.println("Algorithm Power Of Two:");
        packer5.print();

        ImageIO.write(packer1.bm, "png", new File("rect_packer1.png"));
        ImageIO.write(packer2.bm, "png", new File("rect_packer2.png"));
        ImageIO.write(packer4.bm, "png", new File("rect_packer4.png"));
        ImageIO.write(packer5.bm, "png", new File("rect_packer5.png"));
        ImageIO.write(packer6.bm, "png", new File("rect_packer6.png"));
        ImageIO.write(packer7.bm, "png", new File("rect_packer7.png"));
        ImageIO.write(packer8.bm, "png", new File("rect_packer8.png"));

        packer6.free();
        packer7.free();
    }

    public static class Packer {

        final RectanglePacker packer;
        final BufferedImage bm;
        final Graphics2D g2d;

        long time;
        int n;
        int fails;

        final Rect2i rect = new Rect2i();

        public Packer(int algorithm) {
            packer = switch (algorithm) {
                case ALGORITHM_STB_SKYLINE -> new STBSkyline(WIDTH, HEIGHT,
                        STBRectPack.STBRP_HEURISTIC_Skyline_BL_sortHeight);
                case ALGORITHM_STB_SKYLINE_BEST -> new STBSkyline(WIDTH, HEIGHT,
                        STBRectPack.STBRP_HEURISTIC_Skyline_BF_sortHeight);
                default -> RectanglePacker.make(WIDTH, HEIGHT, algorithm);
            };
            bm = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            g2d = bm.createGraphics();
        }

        public boolean add(int w, int h, Color color) {
            if (fails >= 50) return false;
            var rect = this.rect;
            rect.set(0, 0, w, h);
            long start = System.nanoTime();
            boolean result = packer.addRect(rect);
            time += System.nanoTime() - start;
            if (result) {
                g2d.setColor(color);
                g2d.fillRect(rect.x(), rect.y(), w, h);
                ++n;
            } else {
                ++fails;
            }
            return true;
        }

        // rough benchmark
        public void print() {
            System.out.println(n + " rectangles");
            System.out.println("Coverage: " + packer.getCoverage() + " (higher is better)");
            System.out.println((time / n) + " nanoseconds per rectangle (lower is better)");
        }

        public void free() {
            packer.free();
        }
    }

    public static final class STBSkyline extends RectanglePacker implements AutoCloseable {

        private final int mHeuristic;

        private final STBRPContext mContext;
        private final STBRPNode.Buffer mNodes;
        private final STBRPRect.Buffer mRects;

        public STBSkyline(int width, int height, int heuristic) {
            super(width, height);
            mHeuristic = heuristic;
            mContext = STBRPContext.malloc();
            mNodes = STBRPNode.malloc(width + 16);
            mRects = STBRPRect.malloc(1);
            clear();
        }

        @Override
        public void clear() {
            mArea = 0;
            STBRectPack.stbrp_init_target(mContext, mWidth, mHeight, mNodes);
            STBRectPack.stbrp_setup_heuristic(mContext, mHeuristic);
        }

        @Override
        public boolean addRect(Rect2i rect) {
            final int width = rect.width();
            final int height = rect.height();
            if (width <= 0 || height <= 0) {
                rect.offsetTo(0, 0);
                return true;
            }
            if (width > mWidth || height > mHeight) {
                return false;
            }
            var rects = mRects.w(width).h(height);
            if (STBRectPack.stbrp_pack_rects(mContext, rects) != 0) {
                rect.offsetTo(rects.x(), rects.y());
                mArea += width * height;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void free() {
            mRects.free();
            mNodes.free();
            mContext.free();
        }

        @Override
        public void close() {
            free();
        }
    }
}
