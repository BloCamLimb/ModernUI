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
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import icyllis.modernui.ModernUI;
import icyllis.modernui.audio.AudioManager;
import icyllis.modernui.audio.OggDecoder;
import icyllis.modernui.audio.Track;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.*;
import icyllis.modernui.graphics.shader.ShaderManager;
import icyllis.modernui.graphics.texture.Texture2D;
import icyllis.modernui.math.Matrix4;
import icyllis.modernui.platform.Bitmap;
import icyllis.modernui.platform.RenderCore;
import icyllis.modernui.platform.Window;
import icyllis.modernui.text.*;
import icyllis.modernui.text.style.AbsoluteSizeSpan;
import icyllis.modernui.text.style.ForegroundColorSpan;
import icyllis.modernui.text.style.StyleSpan;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.system.Callback;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import static icyllis.modernui.graphics.GLWrapper.*;
import static org.lwjgl.glfw.GLFW.*;

@SuppressWarnings("unused")
public class TestMain {

    public static final Marker MARKER = MarkerManager.getMarker("Test");

    private static List<Font> ALL_FONTS;

    private static BufferedImage IMAGE;
    private static Graphics2D GRAPHICS;

    public static final boolean CREATE_WINDOW = true;

    private static double nextTime = 0;
    private static boolean needRedraw = true;

    private static icyllis.modernui.platform.Window sWindow;

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

    public static SpectrumGraph sGraph;
    public static Track sTrack;

    static {
        AudioManager.getInstance().initialize();
        try {
            sTrack = new Track(new OggDecoder(FileChannel.open(Path.of("F:", "10.ogg"))));
            sGraph = new SpectrumGraph(sTrack);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        ModernUI.initInternal();

        IMAGE = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
        GRAPHICS = IMAGE.createGraphics();

        GraphicsEnvironment.getLocalGraphicsEnvironment().preferLocaleFonts();
        ALL_FONTS = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts());
        GRAPHICS.setColor(Color.BLACK);
        GRAPHICS.fillRect(0, 0, 1024, 1024);
        GRAPHICS.setColor(Color.WHITE);
        GRAPHICS.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        GRAPHICS.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        GRAPHICS.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        /*float[] av = new float[]{1, 3, 2, 4.1f, 6, 0, 6, 0.5f, 5, 7, 11.3f, 9, 9.1f, 15, 8, 10};
        float[] bv = new float[]{9.1f, 2, 7, 5, 3.3f, 6.1f, 5.5f, 4, 0, 8, 3, 1, 2.7f, 3, 9, 2};
        int[] intervals = new int[]{0, 4, 9, 15, 17};*/

        /*float[] re = new float[256];
        float[] im = new float[256];

        for (int i = 0; i < 256; i++) {
            re[i] = MathUtil.cos(2 * MathUtil.PI * i / 256f);
            im[i] = 0;
        }

        ModernUI.LOGGER.info("Before \n{}\n{}", Arrays.toString(re), Arrays.toString(im));
        FFT.fft(re, im);

        float[] ff = new float[256];
        for (int i = 0; i < 256; i++) {
            ff[i] = MathUtil.sqrt(re[i] * re[i] + im[i] * im[i]);
        }

        ModernUI.LOGGER.info("After \n{}", Arrays.toString(ff));*/

        //Quaternion q = Quaternion.fromAxisAngle(0.40824829f, 0.81649658f, 0.40824829f, MathUtil.PI_DIV_3);
        /*Vector3 vec1 = new Vector3(5, 2, 2);
        Vector3 vec2 = vec1.copy();
        Vector3 vec3 = vec2.copy();
        Quaternion q = Quaternion.makeAxisAngle(1.0f, 0, 0, MathUtil.PI_DIV_4);
        vec1.transform(q.toMatrix4());
        vec2.transform(q);
        Matrix4 mat = Matrix4.identity();
        mat.rotateX(MathUtil.PI_DIV_4);
        vec3.transform(mat);
        ModernUI.LOGGER.info("\n{}\n{}\n{}\nEq: {}, {}", vec1, vec2, vec3, vec1.equivalent(vec2), vec2.equivalent
        (vec3));*/
        /*Matrix4 mat = Matrix4.identity();
        Vector3 pos = new Vector3(3, 0, 0);
        mat.translate(2, 0, 0);
        mat.rotateZ(MathUtil.PI_DIV_2);
        mat.translate(-2, 0, 0);
        pos.transform(mat);*/

        /*Bidi bidi = new Bidi(text.toCharArray(), 0, null, 0, text.length(), Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        int runCount = bidi.getRunCount();
        byte[] levels = new byte[runCount];
        Integer[] ranges = new Integer[runCount];

        *//* Reorder contiguous runs of text into their display order from left to right *//*
        for (int index = 0; index < runCount; index++) {
            levels[index] = (byte) bidi.getRunLevel(index);
            ranges[index] = index;
        }
        Bidi.reorderVisually(levels, 0, ranges, 0, runCount);
        ModernUI.LOGGER.info("Reorder: {}", Arrays.toString(Bidi.reorderVisual(levels)));


        for (int logicalIndex = 0; logicalIndex < runCount; logicalIndex++) {
            int visualIndex = ranges[logicalIndex];

            ModernUI.LOGGER.info("Bidi: {}, RTL {}, {} to {}", logicalIndex, (bidi.getRunLevel(visualIndex) & 1) != 0,
                    bidi.getRunStart(visualIndex), bidi.getRunLimit(visualIndex));
        }*/


        //ModernUI.LOGGER.info(MathUtil.atan2(-1, -1));
        //ModernUI.LOGGER.info(pos);
        /*try {
            new Runner(new OptionsBuilder().include(TestCompare.class.getSimpleName()).build()).run();
        } catch (RunnerException e) {
            e.printStackTrace();
        }*/
        //ModernUI.LOGGER.info(Gravity.TOP & Gravity.BOTTOM);
        if (!CREATE_WINDOW)
            return;
        try {
            Thread.currentThread().setName("Main-Thread");
            RenderCore.initBackend();
            sWindow = Window.create("Modern UI Layout Editor", Window.State.WINDOWED, 1600, 900);
            try (var c1 = ModernUI.get().getResource(Path.of("AppLogo16x.png"));
                 var bitmap1 = Bitmap.decode(null, c1);
                 var c2 = ModernUI.get().getResource(Path.of("AppLogo32x.png"));
                 var bitmap2 = Bitmap.decode(null, c2);
                 var c3 = ModernUI.get().getResource(Path.of("AppLogo48x.png"));
                 var bitmap3 = Bitmap.decode(null, c3)) {
                sWindow.setIcon(bitmap1, bitmap2, bitmap3);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Thread renderThread = new Thread(TestMain::runRenderThread, "Render-Thread");
            renderThread.start();

            /*new Thread(() -> {
                // convert to png format with alpha channel
                try (Bitmap b = Bitmap.openDialog(Bitmap.Format.RGBA)) {
                    if (b != null) {
                        b.saveDialog(Bitmap.SaveFormat.PNG, 0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "Open-File").start();*/

            while (sWindow == null || !sWindow.shouldClose()) {
                glfwWaitEventsTimeout(1 / 288D);
            }
            renderThread.interrupt();
        } finally {
            if (sWindow != null) {
                sWindow.close();
            }
            AudioManager.getInstance().close();
            Stream.of(glfwSetMonitorCallback(null),
                    glfwSetErrorCallback(null))
                    .filter(Objects::nonNull)
                    .forEach(Callback::free);
            glfwTerminate();
            ModernUI.LOGGER.info(MARKER, "Stopped");
        }
    }

    private static void runRenderThread() {
        final Window window = sWindow;
        window.makeCurrent();
        RenderCore.initialize();
        GLCanvas canvas = GLCanvas.initialize();
        ShaderManager.getInstance().reload();
        // OpenGL coordinates origin is "bottom" left, we flip it
        Matrix4 projection = Matrix4.makeOrthographic(window.getWidth(), -window.getHeight(), 0, 2000);
        //projection = Matrix4.makePerspective(MathUtil.PI_DIV_2, window.getAspectRatio(), 0.01f, 1000);
        canvas.setProjection(projection);

        Image image;
        try (ReadableByteChannel channel = ModernUI.get().getResource(Path.of("eromanga.png"))) {
            Bitmap bitmap = Bitmap.decode(null, channel);
            Texture2D texture2D = new Texture2D();
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            texture2D.initCore(GL_RGB8, width, height, 4);
            texture2D.upload(0, 0, 0, width, height, 0,
                    0, 0, 1, bitmap.getFormat().glFormat, GL_UNSIGNED_BYTE, bitmap.getPixels());
            texture2D.setFilter(true, true);
            texture2D.generateMipmap();
            image = new Image(new Image.Source(texture2D, width, height));
        } catch (IOException e) {
            throw new IllegalStateException();
        }

        //GlyphManager glyphManager = GlyphManager.getInstance();

        String text;/* = "\u0639\u0646\u062f\u0645\u0627\u0020\u064a\u0631\u064a\u062f\u0020\u0627\u0644\u0639\u0627" +
                "\u0644\u0645\u0020\u0623\u0646\u0020\u202a\u064a\u062a\u0643\u0644\u0651\u0645\u0020\u202c\u0020" +
                "\u060c\u0020\u0641\u0647\u0648\u0020\u064a\u062a\u062d\u062f\u0651\u062b\u0020\u0628\u0644\u063a" +
                "\u0629\u0020\u064a\u0648\u0646\u064a\u0643\u0648\u062f\u002e\u0020\u062a\u0633\u062c\u0651\u0644" +
                "\u0020\u0627\u0644\u0622\u0646\u0020\u0644\u062d\u0636\u0648\u0631\u0020\u0627\u0644\u0645\u0624" +
                "\u062a\u0645\u0631\u0020\u0627\u0644\u062f\u0648\u0644\u064a\u0020\u0627\u0644\u0639\u0627\u0634" +
                "\u0631\u0020\u0644\u064a\u0648\u0646\u064a\u0643\u0648\u062f\u0020\u0028\u0055\u006e\u0069\u0063" +
                "\u006f\u0064\u0065\u0020\u0043\u006f\u006e\u0066\u0065\u0072\u0065\u006e\u0063\u0065\u0029";*/
        text = "My name is Van, I'm 30 years old, and I'm from Japan. I'm an artist, I'm a performance artist. " +
                "I'm hired for people to fulfill their fantasies, their deep dark fantasies.";
                /*"I was gonna be a movie star, you know with modelling and uh, acting. " +
                "After a hundred or two audition and small parts, you know I decided, you know, I had enough, then I
                get into escort work.";*/
        //char[] textC = text.toCharArray();

                /*TextPaint tp = new TextPaint();
                var mt = MeasuredParagraph.buildForStaticLayout(tp, text, 0, text.length(), TextDirectionHeuristics
                .FIRSTSTRONG_LTR, null);
                var dirs = mt.getDirections(0, text.length());
                for (int i = 0; i < dirs.getRunCount(); i++) {
                    int st = dirs.getRunStart(i);
                    int runLimit = Math.min(st + dirs.getRunLength(i), text.length());
                    ModernUI.LOGGER.info("Measure: {}, RTL {}, {} to {}", i, dirs.isRunRtl(i), st, runLimit);
                    for (var run : Typeface.SERIF.itemize(textC, st, runLimit)) {
                        ModernUI.LOGGER.info("FontRun: {} to {}", run.getStart(), run.getEnd());
                        GlyphVector vector = run.getFont().layoutGlyphVector(GRAPHICS.getFontRenderContext(),
                                textC, run.getStart(), run.getEnd(), dirs.isRunRtl(i) ? Font.LAYOUT_RIGHT_TO_LEFT :
                                Font.LAYOUT_LEFT_TO_RIGHT);
                        for (int j = 0; j < vector.getNumGlyphs(); j++) {
                            ModernUI.LOGGER.info("GlyphIndex: {}, GlyphCode: {}, Pos: {}, CharIndex: {}", j,
                                    vector.getGlyphCode(j), vector.getGlyphPosition(j), vector.getGlyphCharIndex(j));
                            glyphManager.lookupGlyph(run.getFont().deriveFont(18.0f), vector.getGlyphCode(j));
                        }
                    }
                }
                glyphManager.export();*/

        Spannable spannable = SpannableString.valueOf(text);
        spannable.setSpan(new ForegroundColorSpan(0xfff699b4), 54, text.length(), 0);
        spannable.setSpan(new AbsoluteSizeSpan(18), 29, text.length() - 20, 0);
        spannable.setSpan(new StyleSpan(FontPaint.BOLD), text.length() - 20, text.length(), 0);
        TextLine textLine = new TextLine(spannable);

        long lastTime = RenderCore.timeMillis();

        sTrack.play();

        while (!window.shouldClose()) {
            long time = RenderCore.timeMillis();
            long delta = time - lastTime;
            if (delta > 16) {
                lastTime += 16;
                GLWrapper.resetFrame(window);
                GLWrapper.enableCull();
                GLWrapper.glEnable(GL_BLEND);
                GLWrapper.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                GLWrapper.glEnable(GL_STENCIL_TEST);
                GLWrapper.glEnable(GL_MULTISAMPLE);
                canvas.reset(window.getWidth(), window.getHeight());
                // UI thread

                Paint paint = Paint.take();

                canvas.save();
                canvas.scale(0.8334f, 0.8334f);
                paint.setRGB(192, 192, 192);
                canvas.drawImage(image, 0, 0, paint);
                canvas.restore();


                drawOsuScore(canvas);


                paint.setStyle(Paint.Style.STROKE);
                paint.setRGBA(255, 255, 255, 255);
                canvas.drawRoundRect(120, 50, 200, 250, 10, paint);

                canvas.save();
                canvas.rotate(45);
                canvas.drawRect(190, 60, 270, 140, paint);
                canvas.restore();

                paint.setStrokeWidth(10);
                canvas.drawArc(200, 200, 30, 90, -90, paint);

                paint.setStrokeWidth(8);
                paint.setRGBA(120, 220, 240, 192);
                canvas.drawLine(20, 20, 140, 60, paint);
                canvas.drawLine(120, 30, 60, 80, paint);

                TextPaint paint1 = new TextPaint();
                paint1.setColor(0xff40ddee);
                //canvas.rotate(30);
                /*String tcc = "今日も一日頑張るぞい";
                canvas.drawTextRun(tcc, 0, tcc.length(), 730, 170, false, paint1);
                tcc = "আমি আজ সকালের নাস্তা খাব না";
                canvas.drawTextRun(tcc, 0, tcc.length(), 660, 240, false, paint1);*/
                float playTime = sTrack.getTime();

                sGraph.update((long) (playTime * 1000L) + 16, delta);
                sGraph.draw(canvas, 800, 450);

                String tcc = String.format("%d / %d", (int) playTime, (int) sTrack.getLength());
                canvas.drawTextRun(tcc, 0, tcc.length(), 760, 456, false, paint1);
                //canvas.rotate(-30);

                textLine.draw(canvas, 32, 400);

                //paint.setStyle(Paint.Style.FILL);
                //canvas.drawRoundRect(100, 840, 100 + playTime / graph.mSongLength * 1400000, 860, 10, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(8);
                paint.setRGBA(255, 255, 255, 192);
                canvas.drawArc(800, 450, 100, -90,
                        360 * (playTime / sTrack.getLength()), paint);

                // render thread, wait UI thread
                canvas.draw();

                window.swapBuffers();
            }
            try {
                Thread.currentThread().join(6);
            } catch (InterruptedException ignored) {
                // waiting for interruption
            }
        }
    }

    private static void drawOsuScore(Canvas canvas) {
        Paint paint = Paint.take();
        paint.setRGBA(0, 0, 0, 64);

        // bottom
        canvas.drawRoundRect(24, 900 - 12 - 314, 1600 - 24, 900 - 12, 20, paint);
        canvas.drawRoundRect(800 - 224, 900 - 12 - 314, 800 + 224, 900 - 12 - 314 + 64, 20, paint);

        // top right
        canvas.drawRoundRect(1600 - 520, 28, 1600 - 16, 28 + 250, 18, paint);
        canvas.drawRoundRect(1600 - 520, 28, 1600 - 16, 28 + 44, 18, paint);

        paint.setAlpha(96);

        // middle
        canvas.drawRoundRect(24, 344, 1600 - 24, 344 + 200, 20, paint);
        canvas.drawRoundRect(24, 344 + 90, 1600 - 24, 344 + 200, 20, paint);

        // stars
        canvas.drawRoundRect(414, 208, 414 + 236, 208 + 70, 35, paint);

        // top left
        canvas.drawRoundRect(18, 24, 18 + 370, 24 + 254, 20, paint);

        paint.setStrokeWidth(4);
        paint.setRGBA(229, 188, 177, 255);
        canvas.drawLine(40, 318, 1600 - 40, 318, paint);

        String s = "Hitorigoto -TV MIX-";
        TextPaint textPaint = new TextPaint();
        textPaint.setColor(0xff000000);
        textPaint.setFontSize(45);
        canvas.drawTextRun(s, 0, s.length(), 408, 75, false, textPaint);
        textPaint.setColor(~0);
        textPaint.setFontSize(44);
        canvas.drawTextRun(s, 0, s.length(), 414, 78, false, textPaint);

        s = "Info";
        textPaint.setFontSize(24);
        canvas.drawTextRun(s, 0, s.length(), 1310, 56, false, textPaint);
    }

    private static int search(int[] a, int pos) {
        int low = 0;
        int high = a.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;

            if (a[mid + 1] <= pos)
                low = mid + 1;
            else if (a[mid] > pos)
                high = mid - 1;
            else
                return mid;
        }
        return -1;
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
        while ((offset = GraphemeBreak.getTextRunCursor(s, Locale.getDefault(), 0, s.length(), offset,
                GraphemeBreak.AFTER)) != prevOffset) {
            toEscapeChars(s.substring(prevOffset, offset));
            GlyphVector vector = font.layoutGlyphVector(GRAPHICS.getFontRenderContext(),
                    s.toCharArray(), prevOffset, offset, Font.LAYOUT_RIGHT_TO_LEFT);
            for (int i = 0; i < vector.getNumGlyphs(); i++) {
                ModernUI.LOGGER.info(MARKER, "GlyphCode: {}", vector.getGlyphCode(i));
            }
            prevOffset = offset;
        }
    }

    public static void breakWords(String s, boolean unicode) {
        int count = 0;
        BreakIterator iterator = BreakIterator.getWordInstance(Locale.ROOT);
        iterator.setText(s);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            if (unicode)
                toEscapeChars(s.substring(start, end));
            else
                ModernUI.LOGGER.info(s.substring(start, end));
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

    public static void breakSentences(String s) {
        int count = 0;
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.ROOT);
        iterator.setText(s);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            ModernUI.LOGGER.info(s.substring(start, end));
            count++;
        }
        ModernUI.LOGGER.info(MARKER, "Sentence break count: {}", count);
    }

    public static void testGraphemeBreak() {
        GraphemeBreak.sUseICU = true;
        String bengaliHello = "\u09b9\u09cd\u09af\u09be\u09b2\u09cb"; // two graphemes, first four chars and last two
        // chars
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
        sb.append("  ".repeat(Math.max(0, depth)));
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

    /*GL11.glMatrixMode(GL11.GL_PROJECTION);
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
                paint.setSmoothRadius(6);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(16);
                CanvasForge.getInstance().drawRoundRect(0, 20, 100, 70, 14, paint);
                paint.setColor(0xFFAADCF0);
                CanvasForge.getInstance().drawRoundRect(0, -110, 100, -60, 14, paint);
                GL11.glPopMatrix();

                GL11.glTranslatef(1.58f * window.getAspectRatio(), 1.0f, -4.8f);
                GL11.glScalef(1 / 90f, -1 / 90f, 1 / 90f);
                GL11.glRotatef(-90, 0, 1, 0);
                CanvasForge.getInstance().drawRoundRect(0, 20, 100, 70, 14, paint);
                CanvasForge.getInstance().drawRoundRect(-20, 190, 80, 240, 14, paint);
                CanvasForge.getInstance().setLineAntiAliasing(true);
                CanvasForge.getInstance().drawRect(-20, 0, 120, 90, paint);

                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);*/
}
