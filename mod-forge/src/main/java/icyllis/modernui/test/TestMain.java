/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

import com.ibm.icu.text.BreakIterator;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import icyllis.modernui.ModernUI;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.GLWrapper;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.shader.ShaderProgram;
import icyllis.modernui.graphics.shader.program.ArcProgram;
import icyllis.modernui.graphics.shader.program.CircleProgram;
import icyllis.modernui.graphics.shader.program.RectProgram;
import icyllis.modernui.graphics.shader.program.RoundRectProgram;
import icyllis.modernui.math.MathUtil;
import icyllis.modernui.math.Matrix4;
import icyllis.modernui.math.Quaternion;
import icyllis.modernui.math.Vector3;
import icyllis.modernui.platform.RenderCore;
import icyllis.modernui.platform.Window;
import icyllis.modernui.platform.WindowState;
import icyllis.modernui.text.GraphemeBreak;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.stream.Stream;

import static org.lwjgl.glfw.GLFW.*;

public class TestMain {

    public static final Marker MARKER = MarkerManager.getMarker("Test");

    private static final List<Font> ALL_FONTS;

    private static final BufferedImage IMAGE = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
    private static final Graphics2D GRAPHICS = IMAGE.createGraphics();

    public static final boolean CREATE_WINDOW = true;

    private static double nextTime = 0;
    private static boolean needRedraw = true;

    private static Window sWindow;

    /*
        Heading font size (In Minecraft: GUI scale 2)
        level 1: 32
        level 2: 24
        level 3: 19
        level 4: 16 (default size for vanilla)
        level 5: 13 (default size for paragraph)
        level 6: 11

        8 9
        10 11
        12 13
        14 15 1024x
        16 17 18 19
        20 21 22 23
        24 25 26 27
        28 29 30 31 2048x
        32 33 34 35 36 37 38 39
        40 41 42 43 44 45 46 47
        48 49 50 51 52 53 54 55
        56 57 58 59 60 61 62 63
        64 65 66 67 68 69 70 71
        72 73 74 75 76 77 78 79
        80 81 82 83 84 85 86 87
        88 89 90 91 92 93 94 95 96 4096x
     */

    static {
        GraphicsEnvironment.getLocalGraphicsEnvironment().preferLocaleFonts();
        ALL_FONTS = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts());
        GRAPHICS.setColor(Color.BLACK);
        GRAPHICS.fillRect(0, 0, 1024, 1024);
        GRAPHICS.setColor(Color.WHITE);
        GRAPHICS.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        GRAPHICS.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        GRAPHICS.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    public static void main(String[] args) throws InterruptedException {
        /*String s = "\u0641\u0647\u0648\u064a\u062a\u062d\u062f\u0651\u062b\u0020\u0628\u0644\u063a\u0629\u0020";
        Font font = ALL_FONTS.stream().filter(f -> f.canDisplayUpTo("\u0641\u0647\u0648") == -1).findFirst().get();
        GlyphVector vector = font.layoutGlyphVector(GRAPHICS.getFontRenderContext(),
                s.toCharArray(), 0, s.length(), Font.LAYOUT_RIGHT_TO_LEFT);
        ModernUI.LOGGER.info(MARKER, "GlyphNum: {}", vector.getNumGlyphs());
        for (int i = 0; i < vector.getNumGlyphs(); i++) {
            ModernUI.LOGGER.info(MARKER, "GlyphCode: {}", vector.getGlyphCode(i));
        }
        breakWords(s);
        breakGraphemes(s);
        ModernUI.LOGGER.info(Integer.toHexString(MuiHooks.C.calcGuiScales(3840, 2160)));*/

        /*System.setProperty("org.lwjgl.librarypath", nativesDir);*/

        float[] av = new float[]{1, 3, 2, 4.1f, 6, 0, 6, 0.5f, 5, 7, 11.3f, 9, 9.1f, 15, 8, 10};
        float[] bv = new float[]{9.1f, 2, 7, 5, 3.3f, 6.1f, 5.5f, 4, 0, 8, 3, 1, 2.7f, 3, 9, 2};
        //Quaternion q = Quaternion.fromAxisAngle(0.40824829f, 0.81649658f, 0.40824829f, MathUtil.PI_DIV_3);
        Vector3 vec1 = new Vector3(5, 2, 2);
        Vector3 vec2 = vec1.copy();
        Vector3 vec3 = vec2.copy();
        Quaternion q = Quaternion.makeAxisAngle(1.0f, 0, 0, MathUtil.PI_DIV_4);
        vec1.transform(q.toMatrix4());
        vec2.transform(q);
        Matrix4 mat = Matrix4.identity();
        mat.rotateX(MathUtil.PI_DIV_4);
        vec3.transform(mat);
        ModernUI.LOGGER.info("\n{}\n{}\n{}\nEq: {}, {}", vec1, vec2, vec3, vec1.equivalent(vec2), vec2.equivalent(vec3));
        /*try {
            new Runner(new OptionsBuilder().include(TestCompare.class.getSimpleName()).build()).run();
        } catch (RunnerException e) {
            e.printStackTrace();
        }*/

        if (!CREATE_WINDOW)
            return;
        try {
            Thread.currentThread().setName("Main-Thread");
            RenderCore.initBackend();
            sWindow = new Window("Modern UI Layout Editor", WindowState.WINDOWED, 1280, 720);
            Thread t = new Thread(() -> {
                final Window window = sWindow;
                window.makeCurrent();
                RenderCore.initialize();
                RenderSystem.initRenderThread();
                ArcProgram.createPrograms();
                CircleProgram.createPrograms();
                RectProgram.createPrograms();
                RoundRectProgram.createPrograms();
                ShaderProgram.linkAll(null);
                final float[] projection;
                Matrix4.makePerspective(MathUtil.PI_DIV_2, window.getAspectRatio(), 0.001f, 1000)
                        .put(projection = new float[16]);
                while (window.exists()) {
                    if (window.needsRefresh()) {
                        GLWrapper.reset(window);
                        GLWrapper.enableCull();
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        RenderSystem.disableDepthTest();
                        GlStateManager._colorMask(true, true, true, true);
                        GlStateManager._depthMask(false);
                        GL11.glMatrixMode(GL11.GL_PROJECTION);
                        GL43.glPushMatrix();
                        GL11.glLoadIdentity();
                        //RenderSystem.ortho(0.0D, window.getWidth(), window.getHeight(), 0.0D, 1000.0D, 3000.0D);
                        //a.multiply(b);
                        GL11.glMultMatrixf(projection);
                        GL11.glMatrixMode(GL11.GL_MODELVIEW);
                        GL43.glPushMatrix();
                        GL11.glLoadIdentity();
                        //GlStateManager._translatef(0.0F, 0.0F, -2000.0F);
                        Paint paint = Paint.take();
                        GL11.glPushMatrix();
                        GL11.glTranslatef(-1.58f * window.getAspectRatio(), -1.0f, -3.8f);
                        GL11.glScalef(1 / 90f, -1 / 90f, 1 / 90f);
                        GL11.glRotatef(90, 0, 1, 0);

                        paint.reset();
                        paint.setFeatherRadius(6);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(16);
                        Canvas.getInstance().drawRoundRect(0, 20, 100, 70, 14, paint);
                        paint.setColor(0xFFAADCF0);
                        Canvas.getInstance().drawRoundRect(0, -110, 100, -60, 14, paint);
                        GL11.glPopMatrix();

                        GL11.glTranslatef(1.58f * window.getAspectRatio(), 1.0f, -4.8f);
                        GL11.glScalef(1 / 90f, -1 / 90f, 1 / 90f);
                        GL11.glRotatef(-90, 0, 1, 0);
                        Canvas.getInstance().drawRoundRect(0, 20, 100, 70, 14, paint);
                        Canvas.getInstance().drawRoundRect(-20, 190, 80, 240, 14, paint);

                        GL11.glPopMatrix();
                        GL11.glMatrixMode(GL11.GL_PROJECTION);
                        GL11.glPopMatrix();
                        GL11.glMatrixMode(GL11.GL_MODELVIEW);

                        window.swapBuffers();
                    }
                    try {
                        Thread.currentThread().join(1000);
                    } catch (InterruptedException ignored) {
                        // waiting for interruption
                    }
                }
            }, "Render-Thread");
            t.start();

            /*new Thread(() -> {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    PointerBuffer filters = stack.mallocPointer(1);
                    stack.nUTF8Safe("*.png", true);
                    filters.put(stack.getPointerAddress());
                    filters.rewind();
                    Optional.ofNullable(TinyFileDialogs.tinyfd_openFileDialog(null, null,
                            filters, "PNG files (*.png)", true))
                            .map(s -> s.split("\\|"))
                            .ifPresent(files -> ModernUI.LOGGER.info("Selects:\n{}", String.join("\n", files)));
                }
            }, "Open-File").start();*/

            while (sWindow == null || sWindow.exists()) {
                glfwWaitEventsTimeout(1 / 60D);
            }
            t.interrupt();
        } finally {
            if (sWindow != null) {
                sWindow.destroy();
            }
            Stream.of(glfwSetMonitorCallback(null),
                    glfwSetErrorCallback(null))
                    .filter(Objects::nonNull)
                    .forEach(Callback::free);
            glfwTerminate();
            ModernUI.LOGGER.info(MARKER, "Stopped");
        }
    }

    public static float calcMachineEpsilon() {
        float machEps = 1.0f;
        do {
            machEps /= 2.0f;
        } while (1.0f + (machEps / 2.0f) != 1.0f);
        return machEps;
    }

    public static void breakGraphemes(String s) {
        GraphemeBreak.sUseICU = true;
        int offset = 0;
        int prevOffset = 0;
        Font font = ALL_FONTS.stream().filter(f -> f.canDisplayUpTo("\u0641\u0647\u0648") == -1).findFirst().get();
        while ((offset = GraphemeBreak.getTextRunCursor(s, Locale.getDefault(), 0, s.length(), offset, GraphemeBreak.AFTER)) != prevOffset) {
            toEscapeChars(s.substring(prevOffset, offset));
            GlyphVector vector = font.layoutGlyphVector(GRAPHICS.getFontRenderContext(),
                    s.toCharArray(), prevOffset, offset, Font.LAYOUT_RIGHT_TO_LEFT);
            for (int i = 0; i < vector.getNumGlyphs(); i++) {
                ModernUI.LOGGER.info(MARKER, "GlyphCode: {}", vector.getGlyphCode(i));
            }
            prevOffset = offset;
        }
    }

    public static void breakWords(String s) {
        int count = 0;
        BreakIterator iterator = BreakIterator.getWordInstance(Locale.ROOT);
        iterator.setText(s);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            toEscapeChars(s.substring(start, end));
            count++;
        }
        ModernUI.LOGGER.info(MARKER, "Word break count: {}", count);
    }

    public static void toEscapeChars(String t) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            builder.append("\\u");
            builder.append(Integer.toString(((int) t.charAt(i)) | 0x10000, 16).substring(1));
        }
        ModernUI.LOGGER.info(MARKER, builder.toString());
    }

    public static void testGraphemeBreak() {
        GraphemeBreak.sUseICU = true;
        String bengaliHello = "\u09b9\u09cd\u09af\u09be\u09b2\u09cb"; // two graphemes, first four chars and last two chars
        ModernUI.LOGGER.info(MARKER, GraphemeBreak.getTextRunCursor(bengaliHello, Locale.getDefault(),
                3, bengaliHello.length(), bengaliHello.length(), GraphemeBreak.BEFORE)); // output 4, correct
    }

    public static void testMarkdownParsing() {
        Parser parser = Parser.builder().build();
        Document document = parser.parse("Advanced Page\r\n---\r\nMy **One** Line\r\n> My Two");
        iterateNode(document, 0);
    }

    private static void iterateNode(@Nonnull Node node, int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
        depth++;
        ModernUI.LOGGER.info(MARKER, "{}{}", sb, node);
        Node child = Node.AST_ADAPTER.getFirstChild(node);
        while (child != null) {
            iterateNode(child, depth);
            child = Node.AST_ADAPTER.getNext(child);
        }
    }

    private static void drawText() {
        /*BufferedImage image = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = image.createGraphics();
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 32);
        graphics2D.setFont(font);
        graphics2D.setColor(Color.BLACK);
        graphics2D.fillRect(0, 0, 1024, 1024);
        graphics2D.setColor(Color.WHITE);
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        String s = "\u0e01\u0e25\u0e31\u0e1a\u0e40\u0e02\u0e49\u0e32\u0e2a\u0e39\u0e48\u0e40\u0e01\u0e21";
        s = "\u0e23\u0e32\u0e22\u0e07\u0e32\u0e19\u0e1a\u0e31\u0e4a\u0e01";
        //s = "\u090f\u0915\u0932\u0916\u093f\u0932\u093e\u0921\u093c\u0940";
        GlyphVector vector = font.layoutGlyphVector(graphics2D.getFontRenderContext(), s.toCharArray(),
                0, s.length(), Font.LAYOUT_LEFT_TO_RIGHT);
        graphics2D.drawGlyphVector(vector, 20, 50);
        for (int i = 0; i < s.length(); i++) {
            System.out.println(vector.getGlyphMetrics(i).isCombining());
        }
        System.out.println();
        for (int i = 0; i < s.length(); i++) {
            System.out.println(vector.getGlyphMetrics(i).isLigature());
        }
        System.out.println();
        for (int i = 0; i < s.length(); i++) {
            System.out.println(vector.getGlyphMetrics(i).isStandard());
        }
        System.out.println();
        for (int i = 0; i < s.length(); i++) {
            System.out.println(vector.getGlyphMetrics(i).isComponent());
        }
        System.out.println();
        for (int i = 0; i < s.length(); i++) {
            System.out.println(vector.getGlyphMetrics(i).isWhitespace());
        }
        System.out.println();
        for (int i = 0; i < s.length(); i++) {
            System.out.println(vector.getGlyphMetrics(i).getAdvanceX());
        }
        System.out.println();
        for (int i = 0; i < s.length(); i++) {
            System.out.println(vector.getGlyphMetrics(i).getBounds2D());
        }


        ImageIO.write(image, "png", new File("F:/a.png"));*/
    }
}
