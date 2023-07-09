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

import icyllis.modernui.core.Core;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TestJ2D {

    public static void main(String[] args) throws IOException, FontFormatException {
        System.setProperty("java.awt.headless", "true");
        System.out.println(System.getProperty("sun.jnu.encoding"));

        var bm = new BufferedImage(800, 450, BufferedImage.TYPE_INT_RGB);
        var g2d = bm.createGraphics();

        g2d.setColor(Color.black);
        g2d.setBackground(Color.white);
        g2d.clearRect(0, 0, 800, 450);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        Arc2D arc = new Arc2D.Float(100, 100, 200, 200, 0, -120, Arc2D.PIE);
        g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(arc);

        Font font = new Font("Microsoft YaHei UI", Font.PLAIN, 32);
        //font = Font.createFonts(new File("F:\\NotoColorEmoji.ttf"))[0];
        //font = font.deriveFont(16f);
        //font = new Font("Nirmala Text", Font.PLAIN, 16);

        String s = "你说的对，但是《原神》是由米哈游自主研发的一款全新开放世界冒险游戏。";
        //s = "হ্যালো";
        var gv0 = font.layoutGlyphVector(g2d.getFontRenderContext(),
                s.toCharArray(), 0, s.length(), Font.LAYOUT_LEFT_TO_RIGHT);
        g2d.drawGlyphVector(gv0, 2, 16);

        printGlyphVector(gv0);

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        var gv1 = font.layoutGlyphVector(g2d.getFontRenderContext(),
                s.toCharArray(), 0, s.length(), Font.LAYOUT_LEFT_TO_RIGHT);
        g2d.drawGlyphVector(gv1, 2, 34);

        var gv2 = font.layoutGlyphVector(g2d.getFontRenderContext(),
                s.toCharArray(), 0, s.length(), Font.LAYOUT_LEFT_TO_RIGHT);
        g2d.drawGlyphVector(gv2, 2, 52);

        g2d.setStroke(new BasicStroke(1.0f));
        g2d.draw(gv2.getOutline(60, 260));
        g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(gv2.getOutline(60, 360));

        printGlyphVector(gv2);

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        //g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        var gv5 = font.layoutGlyphVector(g2d.getFontRenderContext(),
                s.toCharArray(), 0, s.length(), Font.LAYOUT_LEFT_TO_RIGHT);
        g2d.drawGlyphVector(gv5, 2, 70);

        printGlyphVector(gv5);

        font = new Font("SimSun", Font.PLAIN, 16);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        var gv3 = font.layoutGlyphVector(g2d.getFontRenderContext(),
                s.toCharArray(), 0, s.length(), Font.LAYOUT_LEFT_TO_RIGHT);
        g2d.drawGlyphVector(gv3, 2, 110);

        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        var gv4 = font.layoutGlyphVector(g2d.getFontRenderContext(),
                s.toCharArray(), 0, s.length(), Font.LAYOUT_LEFT_TO_RIGHT);
        g2d.drawGlyphVector(gv4, 2, 150);

        ByteBuffer fontBuffer = null;
        try (FileChannel channel = FileChannel.open(Path.of("C:\\Windows\\Fonts\\msyh.ttc"), StandardOpenOption.READ)) {
            fontBuffer = Core.readIntoNativeBuffer(channel);
            fontBuffer.flip();
            STBTTFontinfo fontinfo = STBTTFontinfo.malloc();
            boolean suc1 = STBTruetype.stbtt_InitFont(fontinfo, fontBuffer,
                    STBTruetype.stbtt_GetFontOffsetForIndex(fontBuffer, 0));
            System.out.println("Init Font success: " + suc1);
            float scale = STBTruetype.stbtt_ScaleForMappingEmToPixels(fontinfo, 16);
            int[] w = new int[1], h = new int[1], xoff = new int[1], yoff = new int[1];
            System.out.printf("scale %f \n", scale);
            ByteBuffer bitmap = STBTruetype.stbtt_GetCodepointBitmap(fontinfo, scale, scale, '冒', w, h, xoff, yoff);
            System.out.printf("w: %d, h: %d,xoff: %d, yoff: %d\n", w[0], h[0], xoff[0], yoff[0]);
            if (bitmap != null) {
                System.out.println("Writing Bitmap");
                boolean success = STBImageWrite.stbi_write_png("F:\\am.png", w[0], h[0], 1, bitmap, 0);
                System.out.println("Write bitmap success: " + success);
                STBTruetype.stbtt_FreeBitmap(bitmap);
            }
            fontinfo.free();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            MemoryUtil.memFree(fontBuffer);
        }

        ImageIO.write(bm, "png", new File("F:/testj2d.png"));
    }

    public static void printGlyphVector(GlyphVector gv) {
        int nGlyphs = gv.getNumGlyphs();
        for (int i = 0; i < nGlyphs; i++) {
            System.out.printf("Glyph index: %d, point: %s, charIndex: %d, advance: %f\n", i, gv.getGlyphPosition(i),
                    gv.getGlyphCharIndex(i), gv.getGlyphMetrics(i).getAdvance());
        }
    }
}
