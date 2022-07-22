/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.testforge;

import com.google.gson.*;
import com.ibm.icu.text.BreakIterator;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import icyllis.modernui.ModernUI;
import icyllis.modernui.audio.*;
import icyllis.modernui.core.Window;
import icyllis.modernui.core.*;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.font.*;
import icyllis.modernui.graphics.opengl.*;
import icyllis.modernui.math.*;
import icyllis.modernui.test.SpectrumGraph;
import icyllis.modernui.test.TestFragment;
import icyllis.modernui.text.*;
import icyllis.modernui.text.style.*;
import icyllis.modernui.textmc.CharSequenceBuilder;
import icyllis.modernui.view.Gravity;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.util.GsonHelper;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.system.Callback;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static icyllis.modernui.ModernUI.LOGGER;
import static icyllis.modernui.graphics.opengl.GLCore.*;
import static org.lwjgl.glfw.GLFW.*;

@SuppressWarnings({"unused"})
public class TestMain {

    public static final Marker MARKER = MarkerManager.getMarker("Test");

    private static List<Font> ALL_FONTS;

    private static BufferedImage IMAGE;
    private static Graphics2D GRAPHICS;

    public static boolean CREATE_WINDOW = false;

    private static MainWindow sWindow;

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
        GraphicsEnvironment.getLocalGraphicsEnvironment().preferLocaleFonts();
        ALL_FONTS = List.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts());
        IMAGE = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
        GRAPHICS = IMAGE.createGraphics();
        GRAPHICS.setColor(Color.BLACK);
        GRAPHICS.fillRect(0, 0, 1024, 1024);
        GRAPHICS.setColor(Color.WHITE);
        GRAPHICS.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        GRAPHICS.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        GRAPHICS.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (CREATE_WINDOW) {
            AudioManager.getInstance().initialize();
            try {
                sTrack = new Track(new OggDecoder(FileChannel.open(Path.of("F:/10.ogg"))));
                sGraph = new SpectrumGraph(sTrack, true, 300);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        //System.setProperty("java.awt.headless", Boolean.TRUE.toString());
       /* Matrix4 baseMat = Matrix4.identity();
        baseMat.preScale(2, 4, 5);
        Matrix4 baseMat2 = baseMat.copy();
        Matrix4 rot = Matrix4.identity();
        rot.preRotateX(FMath.PI_O_6);
        baseMat.postMul(rot);
        baseMat2.postRotateX(FMath.PI_O_6);
        LOGGER.info(baseMat.approxEqual(baseMat2));

        String name = Configuration.OPENGL_LIBRARY_NAME.get("null");
        LOGGER.info("{} {}", name, Paths.get(name).isAbsolute());
        */
        int[] codePoints = {0x1f469, 0x1f3fc, 0x200d, 0x2764, 0xfe0f, 0x200d, 0x1f48b, 0x200d, 0x1f469, 0x1f3fd};
        CharSequenceBuilder bufferBuilder = new CharSequenceBuilder();
        for (int cp : codePoints) {
            bufferBuilder.addCodePoint(cp);
        }
        for (int cp : codePoints) {
            LOGGER.info(MARKER, "0x{}: Emoji:{}, EmojiModifier:{}, EmojiModifierBase:{}, Combining:{}, " +
                            "VariationSelector:{}", Integer.toHexString(cp),
                    Emoji.isEmoji(cp), Emoji.isEmojiModifier(cp), Emoji.isEmojiModifierBase(cp),
                    FontCollection.isCombining(cp), FontCollection.isVariationSelector(cp));
        }
        String text = new String(codePoints, 0, codePoints.length);
        GraphemeBreak.forTextRun(text.toCharArray(), Locale.getDefault(), 0, text.length(), (s, e) -> {
            LOGGER.info(MARKER, "{}, {} to {}", text, s, e);
        });
        LOGGER.info(MARKER, "ZWSP Combining:{}", Emoji.isEmoji(0x1F918));
        LOGGER.info(MARKER, "HashCodeEquals{}", bufferBuilder.hashCode() == text.hashCode());

        breakGraphemes("\u2b1b\u200c");

        Pattern pattern = Pattern.compile("(\\:(\\w|\\+|\\-)+\\:)(?=|[\\!\\.\\?]|$)");
        String[] testStr = {
                ":any-non-whitespace:",
                ":kudos:",
                ":text1:sample2:",
                ":@(1@#\\$@SD: :s:",
                ":nospace::inbetween: because there are 2 colons in the middle",
                ":nospace:middle:nospace:`",
        };
        for (String test : testStr) {
            Matcher matcher = pattern.matcher(test);
            LOGGER.info("Test:{}", test);
            while (matcher.find()) {
                LOGGER.info("Index:{} to {}, Group:{}", matcher.start(), matcher.end(), matcher.group());
            }
        }

        Gson gson = new Gson();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream("E:/emoji_data.json"), StandardCharsets.UTF_8))){
            JsonObject object = GsonHelper.fromJson(gson, reader, JsonObject.class);
            for (var entry : object.entrySet()) {
                JsonArray shortcodes = entry.getValue().getAsJsonArray().get(3).getAsJsonArray();
                LOGGER.info("{} {}", entry.getKey(), shortcodes.get(0).getAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!CREATE_WINDOW) {
            System.LoggerFinder.getLoggerFinder().getLogger("ModernUI", TestMain.class.getModule())
                    .log(System.Logger.Level.INFO, "AABBCC");
            try (ModernUI modernUI = new ModernUI()) {
                modernUI.run(new TestFragment());
            }
            return;
        }
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

        /*ModernUI.LOGGER.info("Module: {}", TestMain.class.getModule());
        ModernUI.LOGGER.info("Main class loader: {}", TestMain.class.getClassLoader());
        ModernUI.LOGGER.info("System class loader: {}", ClassLoader.getSystemClassLoader());
        ModernUI.LOGGER.info("Bootstrap class loader: {}", Object.class.getClassLoader());

        ArrayList<Object> acc_r = new ArrayList<>();
        acc_r.add("c");
        List<String> acc_f = (List<String>) (Object) acc_r;

        ArrayList<String> acc_c = new ArrayList<>();
        acc_c.add("d");
        List<Object> acc_g = (List<Object>) (Object) acc_c;*/

        /*IMAGE = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
        GRAPHICS = IMAGE.createGraphics();

        GraphicsEnvironment.getLocalGraphicsEnvironment().preferLocaleFonts();
        ALL_FONTS = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts());
        GRAPHICS.setColor(Color.BLACK);
        GRAPHICS.fillRect(0, 0, 1024, 1024);
        GRAPHICS.setColor(Color.WHITE);
        GRAPHICS.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        GRAPHICS.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        GRAPHICS.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GRAPHICS.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        GRAPHICS.draw(new RoundRectangle2D.Float(20, 20, 600, 600, 20, 20));
        try {
            ImageIO.write(IMAGE, "png", new File("F:/trrr.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        /*try {
            DataSet dataSet = DataSet.inflate(new FileInputStream("F:/ftestdata.dat"));
            ModernUI.LOGGER.info(dataSet);
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        //(sec23°)²+(sec22°)²-2sec23°sec22°cos45°=a²
        //θ=arccos((a²+(sec22°)²-(sec23°)²)/(2asec22°))
        /*double sec22 = 1 / Math.cos(Math.toRadians(22));
        double sec23 = 1 / Math.cos(Math.toRadians(23));
        double sec22_2 = sec22 * sec22;
        double sec23_2 = sec23 * sec23;
        double a_2 = sec23_2 + sec22_2 - 2 * sec22 * sec23 * Math.cos(Math.toRadians(45));
        double a = Math.sqrt(a_2);
        double theta = Math.acos((a_2 + sec22_2 - sec23_2) / (2 * a * sec22));
        ModernUI.LOGGER.info(Math.toDegrees(theta));

        ModernUI.LOGGER.info(Integer.toHexString(ColorEvaluator.evaluate(0.5f, 0xF0AADCF0, 0xF0FFC3F7)).toUpperCase
        (Locale.ROOT));*/

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
        new ModernUI();
        ShaderManager.getInstance().addListener(mgr -> mgr.getShard(ModernUI.ID, "a.vert"));
        try {
            Thread.currentThread().setName("Main-Thread");
            Core.initialize();
            sWindow = MainWindow.initialize("Modern UI Layout Editor", 1600, 900);
            try (var c1 = ModernUI.getInstance().getResourceChannel(ModernUI.ID, "AppLogo16x.png");
                 var bitmap1 = NativeImage.decode(null, c1);
                 var c2 = ModernUI.getInstance().getResourceChannel(ModernUI.ID, "AppLogo32x.png");
                 var bitmap2 = NativeImage.decode(null, c2);
                 var c3 = ModernUI.getInstance().getResourceChannel(ModernUI.ID, "AppLogo48x.png");
                 var bitmap3 = NativeImage.decode(null, c3)) {
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
            /*new Thread(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    ByteBuffer buffer = stack.malloc(3);
                    String c = TinyFileDialogs.tinyfd_colorChooser((ByteBuffer) null, null, buffer, buffer);
                    if (c != null) {
                        ModernUI.LOGGER.info("Choose color {}", c);
                    }
                }
            }, "Choose-Color").start();*/

            Looper.prepare(sWindow);
            Looper.loop();

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
            LOGGER.info(MARKER, "Stopped");
        }
    }

    private static void runRenderThread() {
        final Window window = sWindow;
        window.makeCurrent();
        Core.initOpenGL();
        GLCore.showCapsErrorDialog();
        GLSurfaceCanvas canvas = GLSurfaceCanvas.initialize();
        ShaderManager.getInstance().reload();
        Matrix4 projection = new Matrix4();
        //projection = Matrix4.makePerspective(MathUtil.PI_DIV_2, window.getAspectRatio(), 0.01f, 1000);

        final Image image;
        try {
            GLTexture texture = TextureManager.getInstance().create(
                    FileChannel.open(Path.of("F:", "eromanga.png"), StandardOpenOption.READ), true);
            image = new Image(texture);
        } catch (IOException e) {
            throw new IllegalStateException();
        }

        //GlyphManager glyphManager = GlyphManager.getInstance();

        String text;
        text = "\t\t\u0639\u0646\u062f\u0645\u0627\u0020\u064a\u0631\u064a\u062f\u0020\u0627\u0644\u0639\u0627" +
                "\u0644\u0645\u0020\u0623\u0646\u0020\u202a\u064a\u062a\u0643\u0644\u0651\u0645\u0020\u202c\u0020" +
                "\u060c\u0020\u0641\u0647\u0648\u0020\u064a\u062a\u062d\u062f\u0651\u062b\u0020\u0628\u0644\u063a" +
                "\u0629\u0020\u064a\u0648\u0646\u064a\u0643\u0648\u062f\u002e\u0020\u062a\u0633\u062c\u0651\u0644" +
                "\u0020\u0627\u0644\u0622\u0646\u0020\u0644\u062d\u0636\u0648\u0631\u0020\u0627\u0644\u0645\u0624" +
                "\u062a\u0645\u0631\u0020\u0627\u0644\u062f\u0648\u0644\u064a\u0020\u0627\u0644\u0639\u0627\u0634" +
                "\u0631\u0020\u0644\u064a\u0648\u0646\u064a\u0643\u0648\u062f\u0020\u0028\u0055\u006e\u0069\u0063" +
                "\u006f\u0064\u0065\u0020\u0043\u006f\u006e\u0066\u0065\u0072\u0065\u006e\u0063\u0065\u0029\n";
        /*text = "\u0938\u092d\u0940\u0020\u092e\u0928\u0941\u0937\u094d\u092f\u094b\u0902\u0020\u0915" +
                "\u094b\u0020\u0917\u094c\u0930\u0935\u0020\u0914\u0930\u0020\u0905\u0927\u093f\u0915" +
                "\u093e\u0930\u094b\u0902\u0020\u0915\u0947\u0020\u092e\u093e\u092e\u0932\u0947\u0020" +
                "\u092e\u0947\u0902\u0020\u091c\u0928\u094d\u092e\u091c\u093e\u0924\u0020\u0938\u094d" +
                "\u0935\u0924\u0928\u094d\u0924\u094d\u0930\u0924\u093e\u0020\u0914\u0930\u0020\u0938" +
                "\u092e\u093e\u0928\u0924\u093e\u0020\u092a\u094d\u0930\u093e\u092a\u094d\u0924\u0020" +
                "\u0939\u0948\u0964\u0020\u0909\u0928\u094d\u0939\u0947\u0902\u0020\u092c\u0941\u0926" +
                "\u094d\u0927\u093f\u0020\u0914\u0930\u0020\u0905\u0928\u094d\u0924\u0930\u093e\u0924" +
                "\u094d\u092e\u093e\u0020\u0915\u0940\u0020\u0926\u0947\u0928\u0020\u092a\u094d\u0930" +
                "\u093e\u092a\u094d\u0924\u0020\u0939\u0948\u0020\u0914\u0930\u0020\u092a\u0930\u0938" +
                "\u094d\u092a\u0930\u0020\u0909\u0928\u094d\u0939\u0947\u0902\u0020\u092d\u093e\u0908" +
                "\u091a\u093e\u0930\u0947\u0020\u0915\u0947\u0020\u092d\u093e\u0935\u0020\u0938\u0947" +
                "\u0020\u092c\u0930\u094d\u0924\u093e\u0935\u0020\u0915\u0930\u0928\u093e\u0020\u091a" +
                "\u093e\u0939\u093f\u092f\u0947\u0964";*/
        text += "\t\tMy name is Van, I'm 30 years old, and I'm from Japan. I'm an artist, I'm a performance artist. " +
                "I'm hired for people to fulfill their fantasies, their deep dark fantasies.\n" +
                "\t\t\u4f60\u770b\u8fd9\u4e2a\u5f6c\u5f6c\u0020\u624d\u559d\u51e0\u7f50\u0020\u5c31\u9189" +
                "\u4e86\u002e\u002e\u002e\ua994\ua9ba\ua9b4\ua98f\ua9ba\ua9b4\u0020\u771f\u7684\u592a\u900a\u529b" +
                "\uff1b\u54e6\uff0c\u542c\u4f60" +
                "\u90a3\u4e48\u8bf4\u0020\u4f60\u5f88\u52c7" +
                "\u54e6\uff1b\u5f00\u73a9\u7b11\uff0c\u6211" +
                "\u8d85\u52c7\u7684\u597d\u4e0d\u597d\u0020\u6211\u8d85\u4f1a\u559d\u7684\u5566";
        text += "\n\u09b9\u09cd\u09af\u09be\u09b2\u09cb\u0020\u0645\u0631\u062d\u0628\u0627\u0020\ud808\udd99\ud808" +
                "\udd99";
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

        //breakLines(text, true);

        Editable editable = Editable.DEFAULT_FACTORY.newEditable(text);
        editable.setSpan(new ForegroundColorSpan(0xfff699b4), text.length() - 54, text.length(), 0);
        editable.setSpan(new AbsoluteSizeSpan(18), text.length() - 69, text.length() - 30, 0);
        editable.setSpan(new StyleSpan(FontPaint.BOLD), text.length() - 50, text.length() - 40, 0);
        editable.setSpan(new UnderlineSpan(), text.length() / 2, text.length(), 0);
        //TextLine textLine = new TextLine(spannable);
        DynamicLayout dynamicLayout = DynamicLayout.builder(editable, new TextPaint(), 600)
                .build();

        //editable.insert(0, "ABCD");
        editable.append("ABCDEF");

        //GLFW.glfwSwapInterval(1);

        long lastTime = Core.timeMillis();

        Rect screenRect = new Rect(0, 0, window.getWidth(), window.getHeight());

        //sTrack.play();

        GLCore.glEnable(GL_CULL_FACE);
        GLCore.glEnable(GL_BLEND);
        GLCore.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        GLCore.glEnable(GL_STENCIL_TEST);
        GLCore.glEnable(GL_MULTISAMPLE);
        GLCore.glDisable(GL_DEPTH_TEST);

        TextPaint tps = new TextPaint();
        tps.setColor(0xff40ddee);
        tps.setTypeface(Typeface.SANS_SERIF);

        GLFramebuffer framebuffer = new GLFramebuffer(4);
        framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT0, GL_RGBA8);
        framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT1, GL_RGBA8);
        framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT2, GL_RGBA8);
        framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT3, GL_RGBA8);
        framebuffer.addRenderbufferAttachment(GL_STENCIL_ATTACHMENT, GL_STENCIL_INDEX8);
        framebuffer.setDrawBuffer(GL_COLOR_ATTACHMENT0);

        glfwShowWindow(window.getHandle());

        boolean note = false;

        while (!window.shouldClose()) {
            long time = Core.timeMillis();
            long delta = time - lastTime;
            lastTime = time;
            GLCore.resetFrame(window);

            if (window.getWidth() > 0) {
                canvas.reset(window.getWidth(), window.getHeight());

                // UI thread
                Paint paint = Paint.get();

                paint.setRGB(160, 160, 160);
                canvas.drawImage(image, null, screenRect, paint);

                int stackAlpha = (int) (255 * ((time - 2500) / 300F));
                if (stackAlpha < 255) {
                    canvas.saveLayer(null, stackAlpha);
                    drawOsuScore(canvas);
                    canvas.restore();
                } else {
                    drawOsuScore(canvas);
                }

                paint.setStyle(Paint.STROKE);
                float sin = FMath.sin(time / 300f);
                paint.setRGBA(255, 255, 255, 255);
                canvas.drawRoundRect(120, 120, 200, 250 - 50 * sin, 25 + 15 * sin, paint);

                canvas.save();
                canvas.rotate(180 * sin, 230, 100);
                canvas.drawRect(190, 60, 270, 140, paint);
                canvas.restore();

                paint.setStrokeWidth(10);
                canvas.drawArc(200, 200, 30, 90 + 60 * sin, -90 + 120 * sin, paint);

                paint.setStrokeWidth(8);
                paint.setRGBA(120, 220, 240, 192);
                canvas.drawRoundLine(20, 20, 140, 60, paint);
                canvas.drawRoundLine(120, 30, 60, 80, paint);

                canvas.drawBezier(300, 100, 410, 210 + 100 * sin, 480, 170, paint);

                canvas.drawTriangle(170, 140, 120, 90, 70, 140, paint);

                canvas.save();
                canvas.scale(10, 10);
                canvas.drawRoundRect(0, 0, 20, 20, 5, paint);
                canvas.restore();

                //canvas.rotate(30);
            /*String tcc = "今日も一日頑張るぞい";
            canvas.drawTextRun(tcc, 0, tcc.length(), 730, 170, false, paint1);
            tcc = "আমি আজ সকালের নাস্তা খাব না";
            canvas.drawTextRun(tcc, 0, tcc.length(), 660, 240, false, paint1);*/
                //textLine.draw(canvas, 32, 400);
                canvas.translate(40, 360);
                paint.setRGBA(0, 0, 0, 128);
                paint.setStyle(Paint.FILL);
                paint.setStrokeWidth(8);
                canvas.drawRoundRect(-6, -10, 606, 310, 12, Gravity.LEFT, paint);
                dynamicLayout.draw(canvas);
                canvas.translate(-40, -600);

                float playTime = sTrack.getTime();

                sGraph.update(delta);
                sGraph.draw(canvas, 800, 450);

                String tcc = String.format("%d / %d", (int) playTime, (int) sTrack.getLength());
                canvas.drawText(tcc, 0, tcc.length(), 800, 456, Gravity.CENTER_HORIZONTAL, tps);

                tcc = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576 + " / " +
                        Runtime.getRuntime().maxMemory() / 1048576;
                canvas.drawText(tcc, 0, tcc.length(), 1000, 60, tps);
                //canvas.rotate(-30);

                //paint.setStyle(Paint.Style.FILL);
                //canvas.drawRoundRect(100, 840, 100 + playTime / graph.mSongLength * 1400000, 860, 10, paint);
                paint.setStyle(Paint.STROKE);
                paint.setStrokeWidth(8);
                paint.setRGBA(255, 255, 255, 192);
                canvas.drawArc(800, 450, 100, -90,
                        360 * (playTime / sTrack.getLength()), paint);

                // render thread, wait UI thread
                canvas.draw(framebuffer);

                if (!note) {
                    LOGGER.info(TextUtils.binaryCompact(canvas.getNativeMemoryUsage()));
                    note = true;
                }
            }

            glBlitNamedFramebuffer(framebuffer.get(), 0, 0, 0, window.getWidth(), window.getHeight(),
                    0, 0, window.getWidth(), window.getHeight(), GL_COLOR_BUFFER_BIT, GL_NEAREST);

            window.swapBuffers();
        }
    }

    private static void drawOsuScore(Canvas canvas) {
        Paint paint = Paint.get();
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
        canvas.drawRoundLine(40, 318, 1600 - 40, 318, paint);

        String s = "Hitorigoto -TV MIX-";
        TextPaint textPaint = new TextPaint();
        textPaint.setColor(0xff000000);
        textPaint.setFontSize(45);
        canvas.drawText(s, 0, s.length(), 408, 75, textPaint);
        textPaint.setColor(~0);
        textPaint.setFontSize(44);
        canvas.drawText(s, 0, s.length(), 414, 78, textPaint);

        s = "Info";
        textPaint.setFontSize(24);
        canvas.drawText(s, 0, s.length(), 1310, 56, textPaint);
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
            toEscapeChars(s.substring(prevOffset, offset), prevOffset, offset);
            GlyphVector vector = font.layoutGlyphVector(GRAPHICS.getFontRenderContext(),
                    s.toCharArray(), prevOffset, offset, Font.LAYOUT_RIGHT_TO_LEFT);
            for (int i = 0; i < vector.getNumGlyphs(); i++) {
                LOGGER.info(MARKER, "GlyphCode: {}, GlyphIndex: {}, CharIndex: {}",
                        vector.getGlyphCode(i), i, vector.getGlyphCharIndex(i));
            }
            prevOffset = offset;
        }
    }

    public static void breakSegments(IntList styles, int start, int limit, boolean isRtl) {
        if (isRtl) {
            int lastOffset = limit;
            int currOffset = limit - 1;
            int lastStyle = styles.getInt(currOffset);
            int currStyle;
            while (currOffset > start) {
                if ((currStyle = styles.getInt(currOffset - 1)) != lastStyle) {
                    LOGGER.info("Segment: {} - {}, {}", currOffset, lastOffset, lastStyle);
                    lastOffset = currOffset;
                    lastStyle = currStyle;
                }
                currOffset--;
            }
            assert currOffset == start;
            LOGGER.info("Segment: {} - {}, {}", currOffset, lastOffset, styles.getInt(currOffset));
        } else {
            int lastOffset = start;
            int currOffset = start;
            int lastStyle = styles.getInt(currOffset);
            int currStyle;
            while (currOffset < limit - 1) {
                currOffset++;
                if ((currStyle = styles.getInt(currOffset)) != lastStyle) {
                    LOGGER.info("Segment: {} - {}, {}", lastOffset, currOffset, lastStyle);
                    lastOffset = currOffset;
                    lastStyle = currStyle;
                }
            }
            assert currOffset == limit - 1;
            LOGGER.info("Segment: {} - {}, {}", lastOffset, currOffset + 1, styles.getInt(currOffset));
        }
    }

    public static void breakWords(String s, boolean unicode) {
        int count = 0;
        BreakIterator iterator = BreakIterator.getWordInstance(Locale.ROOT);
        iterator.setText(s);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            if (unicode)
                toEscapeChars(s.substring(start, end), start, end);
            else
                LOGGER.info(s.substring(start, end));
            count++;
        }
        LOGGER.info(MARKER, "Word break count: {}", count);
    }

    public static void toEscapeChars(String t, int a, int b) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            builder.append("\\u");
            builder.append(Integer.toString(((int) t.charAt(i)) | 0x10000, 16).substring(1));
        }
        LOGGER.info(MARKER, "{} {}: {}", a, b, builder.toString());
    }

    public static void breakSentences(String s) {
        int count = 0;
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.ROOT);
        iterator.setText(s);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            LOGGER.info(s.substring(start, end));
            count++;
        }
        LOGGER.info(MARKER, "Sentence break count: {}", count);
    }

    public static void breakLines(String s, boolean unicode) {
        int count = 0;
        BreakIterator iterator = BreakIterator.getLineInstance(Locale.ROOT);
        iterator.setText(s);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            if (unicode)
                toEscapeChars(s.substring(start, end), start, end);
            else
                LOGGER.info(s.substring(start, end));
            count++;
        }
        LOGGER.info(MARKER, "Sentence break count: {}", count);
    }

    public static void testGraphemeBreak() {
        GraphemeBreak.sUseICU = true;
        String bengaliHello = "\u09b9\u09cd\u09af\u09be\u09b2\u09cb"; // two graphemes, first four chars and last two
        // chars
        LOGGER.info(MARKER, GraphemeBreak.getTextRunCursor(bengaliHello, Locale.getDefault(),
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
        LOGGER.info(MARKER, "{}{}", sb, node);
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
