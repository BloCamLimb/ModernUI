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

import com.ibm.icu.text.NumberFormat;
import icyllis.arc3d.core.Matrix4;
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
import java.util.Arrays;
import java.util.Locale;

public class TestJ2D {

    public static void main(String[] args) throws IOException, FontFormatException {
        System.setProperty("java.awt.headless", "true");
        System.out.println(System.getProperty("sun.jnu.encoding"));
        Toolkit tk = Toolkit.getDefaultToolkit();
        System.out.println(tk);
        System.out.println(tk.getDesktopProperty("win.text.fontSmoothingOn"));
        System.out.println(tk.getDesktopProperty("win.text.fontSmoothingType"));
        System.out.println(tk.getDesktopProperty("win.text.fontSmoothingOrientation"));
        System.out.println(tk.getDesktopProperty("win.text.fontSmoothingContrast"));

        System.out.println(NumberFormat.getCurrencyInstance(new Locale("hi"))
                .format(5));

        float pivotX = 10.0f * (float) Math.random();
        float pivotY = 10.0f * (float) Math.random();
        float translationX = 10.0f * (float) Math.random();
        float translationY = 10.0f * (float) Math.random();
        float scaleX = 10.0f * (float) Math.random();
        float scaleY = 10.0f * (float) Math.random();
        float rotationX = 10.0f * (float) Math.random();
        float rotationY = 10.0f * (float) Math.random();
        float rotationZ = 10.0f * (float) Math.random();
        var mat1 = getOldMatrix(pivotX, pivotY, translationX, translationY, scaleX, scaleY, rotationX, rotationY,
                rotationZ);
        var mat2 = getNewMatrix(pivotX, pivotY, translationX, translationY, scaleX, scaleY, rotationX, rotationY,
                rotationZ);
        var mat3 = getNewMatrix2(pivotX, pivotY, translationX, translationY, scaleX, scaleY, rotationX, rotationY,
                rotationZ);
        var mat4 = new Matrix4();
        mat3.invert(mat4);
        float[] v1 = {4, 6, 0, 1};
        System.out.printf("%s\n%s\n%s\n%s", mat3.isApproxEqual(mat2), mat1, mat2, mat3);
        mapVec4(mat3, v1);
        System.out.println(Arrays.toString(v1));
        mapVec4(mat4, v1);
        System.out.println(Arrays.toString(v1));

        var bm = new BufferedImage(800, 450, BufferedImage.TYPE_USHORT_565_RGB);
        var g2d = bm.createGraphics();

        g2d.setColor(Color.white);
        g2d.setBackground(Color.black);
        g2d.clearRect(0, 0, 800, 450);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        Arc2D arc = new Arc2D.Float(100, 100, 200, 200, 0, -120, Arc2D.PIE);
        g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(arc);

        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        font = Font.createFonts(new File("E:\\Free Fonts\\biliw.otf"))[0];
        font = font.deriveFont(12f);
        //font = new Font("Nirmala Text", Font.PLAIN, 16);

        String s = "Microsoft ClearType antialiasing";
        //s = "হ্যালো";
        var gv0 = font.layoutGlyphVector(g2d.getFontRenderContext(),
                s.toCharArray(), 0, s.length(), Font.LAYOUT_LEFT_TO_RIGHT);
        g2d.drawGlyphVector(gv0, 2, 16);

        printGlyphVector(gv0);

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        var gv1 = font.layoutGlyphVector(g2d.getFontRenderContext(),
                s.toCharArray(), 0, s.length(), Font.LAYOUT_LEFT_TO_RIGHT);
        g2d.drawGlyphVector(gv1, 2, 34);

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        var gv2 = font.layoutGlyphVector(g2d.getFontRenderContext(),
                s.toCharArray(), 0, s.length(), Font.LAYOUT_LEFT_TO_RIGHT);
        g2d.drawGlyphVector(gv2, 2, 52);

        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        var gv8 = font.layoutGlyphVector(g2d.getFontRenderContext(),
                s.toCharArray(), 0, s.length(), Font.LAYOUT_LEFT_TO_RIGHT);
        g2d.drawGlyphVector(gv8, 2, 70);

        g2d.setStroke(new BasicStroke(1.0f));
        g2d.draw(gv2.getOutline(60, 260));
        g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(gv2.getOutline(60, 360));

        printGlyphVector(gv2);

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        //g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        var gv5 = font.layoutGlyphVector(g2d.getFontRenderContext(),
                s.toCharArray(), 0, s.length(), Font.LAYOUT_LEFT_TO_RIGHT);
        g2d.drawGlyphVector(gv5, 2, 88);

        printGlyphVector(gv5);

        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        //g2d.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, 120);
        var gv7 = font.layoutGlyphVector(g2d.getFontRenderContext(),
                s.toCharArray(), 0, s.length(), Font.LAYOUT_LEFT_TO_RIGHT);
        g2d.drawGlyphVector(gv7, 2, 106);


        font = new Font("SimSun", Font.PLAIN, 16);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        var gv3 = font.layoutGlyphVector(g2d.getFontRenderContext(),
                s.toCharArray(), 0, s.length(), Font.LAYOUT_LEFT_TO_RIGHT);
        g2d.drawGlyphVector(gv3, 2, 140);

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        var gv4 = font.layoutGlyphVector(g2d.getFontRenderContext(),
                s.toCharArray(), 0, s.length(), Font.LAYOUT_LEFT_TO_RIGHT);
        g2d.drawGlyphVector(gv4, 2, 170);

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

    public static Matrix4 getOldMatrix(float pivotX, float pivotY, float translationX, float translationY,
                                       float scaleX, float scaleY, float rotationX, float rotationY, float rotationZ) {
        Matrix4 matrix = new Matrix4();
        matrix.setTranslate(pivotX + translationX, pivotY + translationY, 0);
        matrix.preScale(scaleX, scaleY);
        matrix.preTranslate(-pivotX, -pivotY);
        Matrix4 matrix2 = Matrix4.identity();
        matrix2.m34 = 1 / 1920f;
        matrix2.preRotate(Math.toRadians(-rotationX),
                Math.toRadians(-rotationY),
                Math.toRadians(rotationZ));
        matrix2.preTranslate(-pivotX, -pivotY);
        matrix2.postTranslate(pivotX + translationX, pivotY + translationY);
        matrix.postConcat(matrix2);
        return matrix;
    }

    public static Matrix4 getNewMatrix(float pivotX, float pivotY, float translationX, float translationY,
                                       float scaleX, float scaleY, float rotationX, float rotationY, float rotationZ) {
        Matrix4 matrix = Matrix4.identity();
        matrix.preTranslate(pivotX, pivotY);
        matrix.preScale(scaleX, scaleY);
        matrix.preTranslate(-pivotX, -pivotY);
        Matrix4 matrix2 = Matrix4.identity();
        matrix2.m34 = 1 / 1920f;
        matrix2.preRotate(Math.toRadians(-rotationX),
                Math.toRadians(-rotationY),
                Math.toRadians(rotationZ));
        matrix2.preTranslate(-pivotX, -pivotY);
        matrix2.postTranslate(pivotX + translationX, pivotY + translationY);
        matrix.postConcat(matrix2);
        return matrix;
    }

    public static Matrix4 getNewMatrix2(float pivotX, float pivotY, float translationX, float translationY,
                                        float scaleX, float scaleY, float rotationX, float rotationY, float rotationZ) {
        Matrix4 matrix = Matrix4.identity();
        matrix.m34 = 1 / 1920f;
        matrix.preRotate(Math.toRadians(-rotationX),
                Math.toRadians(-rotationY),
                Math.toRadians(rotationZ));
        matrix.preScale(scaleX, scaleY);
        matrix.preTranslate(-pivotX, -pivotY);
        matrix.postTranslate(pivotX + translationX, pivotY + translationY);
        return matrix;
    }

    static void mapVec4(Matrix4 m, float[] vec) {
        final float x = m.m11 * vec[0] + m.m21 * vec[1] + m.m31 * vec[2] + m.m41 * vec[3];
        final float y = m.m12 * vec[0] + m.m22 * vec[1] + m.m32 * vec[2] + m.m42 * vec[3];
        final float z = m.m13 * vec[0] + m.m23 * vec[1] + m.m33 * vec[2] + m.m43 * vec[3];
        final float w = m.m14 * vec[0] + m.m24 * vec[1] + m.m34 * vec[2] + m.m44 * vec[3];
        vec[0] = x;
        vec[1] = y;
        vec[2] = z;
        vec[3] = w;
    }
}
