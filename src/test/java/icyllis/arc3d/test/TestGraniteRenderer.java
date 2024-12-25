/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.Image;
import icyllis.arc3d.core.*;
import icyllis.arc3d.core.effects.BlendModeColorFilter;
import icyllis.arc3d.core.effects.ColorFilter;
import icyllis.arc3d.core.j2d.Typeface_JDK;
import icyllis.arc3d.core.shaders.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.*;
import icyllis.arc3d.opengl.GLUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengles.*;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.font.FontRenderContext;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.*;

import static org.lwjgl.glfw.GLFW.*;

public class TestGraniteRenderer {

    public static final Logger LOGGER = LoggerFactory.getLogger("Arc3D");

    public static int CANVAS_WIDTH = 1760;
    public static int CANVAS_HEIGHT = 990;

    public static final int MAX_RECT_WIDTH = 370;
    public static final int MIN_RECT_WIDTH = 20;
    public static final int MAX_RECT_HEIGHT = 270;
    public static final int MIN_RECT_HEIGHT = 20;
    public static final int MAX_CORNER_RADIUS = 50;

    public static final int WINDOW_WIDTH = 1760;
    public static final int WINDOW_HEIGHT = 990;

    public static final boolean TEST_OPENGL_ES = false;

    public static final int TEST_SCENE = 1;
    public static final boolean POST_PROCESS = false;

    public static final ExecutorService RECORDING_THREAD = Executors.newSingleThreadExecutor();

    public static float Bayer2(float x, float y) {
        x = (float) Math.floor(x);
        y = (float) Math.floor(y);
        return (x * .5f + y * y * .75f) % 1.0f;
    }

    public static float Bayer4(float x, float y) {
        return Bayer2(.5f * (x), 0.5f * y) * .25f + Bayer2(x, y);
    }

    public static float Bayer8(float x, float y) {
        return Bayer4(.5f * (x), 0.5f * y) * .25f + Bayer2(x, y);
    }

    //-Dorg.slf4j.simpleLogger.logFile=System.out
    //-Dorg.slf4j.simpleLogger.defaultLogLevel=debug
    //-XX:+UseZGC
    //-XX:+ZGenerational
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GLFW.glfwInit();
        LOGGER.info(Long.toString(ProcessHandle.current().pid()));
        TinyFileDialogs.tinyfd_messageBox(
                "Arc3D Test",
                "Arc3D starting with pid: " + ProcessHandle.current().pid(),
                "ok",
                "info",
                true
        );
        Objects.requireNonNull(GL.getFunctionProvider());
        GLFW.glfwDefaultWindowHints();
        if (TEST_OPENGL_ES) {
            GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_ES_API);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 0);
        } else {
            GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 5);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
        }
        glfwWindowHint(GLFW_DEPTH_BITS, 0);
        glfwWindowHint(GLFW_STENCIL_BITS, 0);
        //GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Test Window", 0, 0);
        if (window == 0) {
            throw new RuntimeException("0x" + Integer.toHexString(GLFW.nglfwGetError(MemoryUtil.NULL)));
        }
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);

        ContextOptions contextOptions = new ContextOptions();
        contextOptions.mLogger = LOGGER;
        contextOptions.mSkipGLErrorChecks = Boolean.TRUE;
        ImmediateContext immediateContext = GLUtil.makeOpenGL(
                TEST_OPENGL_ES ? GLES.createCapabilities() : GL.createCapabilities(),
                contextOptions
        );
        if (immediateContext == null) {
            throw new RuntimeException();
        }
        if (!TEST_OPENGL_ES) {
            TestDrawPass.glSetupDebugCallback();
        }
        //LOGGER.info(immediateContext.getCaps().toString());
        Painter painter = CompletableFuture.supplyAsync(
                () -> new Painter(immediateContext),
                RECORDING_THREAD
        ).join();
        /*for (int y = 0; y < 8; ++y) {
            for (int x = 0; x < 8; ++x) {
                int m = (int) (Bayer8(x, y) * 64);
                System.out.printf("%02d ", m);
            }
            System.out.println();
        }*/

        /*double maxRadError = 0;
        boolean valid = true;
        for (int i = 0; i < 10000; i++) {
            float cx = random.nextFloat(1000);
            float cy = random.nextFloat(1000);
            float rad = random.nextFloat(1000);
            RoundRect rrect = new RoundRect();
            rrect.setRectXY(cx - rad, cy - rad, cx + rad, cy + rad,
                    rad, rad);
            if (rrect.getType() > RoundRect.kEllipse_Type) {
                valid = false;
                LOGGER.info("{}", rrect.getType());
            }
        }
        LOGGER.info("max rad error {}, valid {}",  maxRadError, valid);*/

        long frame = 0;

        while (!GLFW.glfwWindowShouldClose(window)) {

            if (true) {
                @SharedPtr
                RootTask rootTask = CompletableFuture.supplyAsync(
                        painter::paint,
                        RECORDING_THREAD
                ).join();

                double time4 = GLFW.glfwGetTime();

                if (!immediateContext.addTask(rootTask)) {
                    LOGGER.error("Failed to add recording: {}", rootTask);
                }
                RefCnt.move(rootTask);

                double time5 = GLFW.glfwGetTime();

                int filter = CANVAS_WIDTH == WINDOW_WIDTH && CANVAS_HEIGHT == WINDOW_HEIGHT
                        ? GL33C.GL_LINEAR : GL33C.GL_NEAREST;
                if (TEST_OPENGL_ES) {
                    GLES30.glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER, 0);
                    GLES30.glBlitFramebuffer(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT,
                            0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, GL33C.GL_COLOR_BUFFER_BIT,
                            filter);
                } else {
                    GL33C.glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER, 0);
                    GL33C.glBlitFramebuffer(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT,
                            0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, GL33C.GL_COLOR_BUFFER_BIT,
                            filter);
                }
                if (!immediateContext.submit()) {
                    LOGGER.error("Failed to submit queue");
                }

                double time6 = GLFW.glfwGetTime();

                GLFW.glfwSwapBuffers(window);

                double time7 = GLFW.glfwGetTime();
                /*LOGGER.info("AddCommands: {}, Blit/Submit: {}, Swap: {}",
                        formatMicroseconds(time5, time4),
                        formatMicroseconds(time6, time5),
                        formatMicroseconds(time7, time6));*/
            }
            frame++;

            GLFW.glfwPollEvents();
        }
        RECORDING_THREAD.submit(painter::close);
        RECORDING_THREAD.shutdown();
        try {
            boolean terminated = RECORDING_THREAD.awaitTermination(5, TimeUnit.SECONDS);
            LOGGER.info("Terminated painter {}", terminated);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        {
            long rowBytes = (long) CANVAS_WIDTH * 4;
            long srcPixels = MemoryUtil.nmemAlloc(rowBytes * CANVAS_HEIGHT);
            long dstPixels = MemoryUtil.nmemAlloc(rowBytes * CANVAS_HEIGHT);
            if (TEST_OPENGL_ES) {
                GLES20.glReadPixels(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT,
                        GL33C.GL_RGBA, GL33C.GL_UNSIGNED_BYTE, srcPixels);
            } else {
                GL33C.glReadPixels(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT,
                        GL33C.GL_RGBA, GL33C.GL_UNSIGNED_BYTE, srcPixels);
            }
            // premul to unpremul, and flip Y
            ImageInfo srcInfo = ImageInfo.make(CANVAS_WIDTH, CANVAS_HEIGHT,
                    ColorInfo.CT_RGBA_8888, ColorInfo.AT_PREMUL, null);
            ImageInfo dstInfo = srcInfo.makeAlphaType(ColorInfo.AT_UNPREMUL);
            boolean res = PixelUtils.convertPixels(
                    srcInfo, null, srcPixels, rowBytes,
                    dstInfo, null, dstPixels, rowBytes,
                    true
            );
            assert res;
            MemoryUtil.nmemFree(srcPixels);
            STBImageWrite.stbi_write_png_compression_level.put(0, 15);
            STBImageWrite.stbi_write_png("test_granite.png", CANVAS_WIDTH, CANVAS_HEIGHT, 4,
                    MemoryUtil.memByteBuffer(dstPixels, CANVAS_WIDTH * CANVAS_HEIGHT * 4), (int) rowBytes);
            MemoryUtil.nmemFree(dstPixels);
        }
        immediateContext.unref();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static String formatMicroseconds(double e, double st) {
        return String.format("%.1f us", (e - st) * 1000000.0D);
    }

    public static class Painter {

        final Random mRandom = new Random();

        @SharedPtr
        RecordingContext mRC;
        @SharedPtr
        Surface mSurface;

        @SharedPtr
        Surface mPostSurface;

        @SharedPtr
        Image mTestImage = null;
        @SharedPtr
        Shader mTestShader1;
        @SharedPtr
        Shader mTestShader2;
        @SharedPtr
        Shader mTestShader3;

        @SharedPtr
        Shader mGradShader;

        TextBlob mTextBlob1;
        TextBlob mTextBlob2;

        Vertices mVertices1;
        Vertices mVertices2;

        final ColorFilter[] mBlendModeColorFilters = new ColorFilter[BlendMode.COUNT];

        public Painter(ImmediateContext immediateContext) {
            mRC = immediateContext.makeRecordingContext();
            {
                @SharedPtr
                var device = GraniteDevice.make(
                        mRC,
                        ImageInfo.make(CANVAS_WIDTH, CANVAS_HEIGHT, ColorInfo.CT_RGBA_8888,
                                ColorInfo.AT_PREMUL, ColorSpace.get(ColorSpace.Named.SRGB)),
                        ISurface.FLAG_SAMPLED_IMAGE | ISurface.FLAG_RENDERABLE | ISurface.FLAG_BUDGETED,
                        Engine.SurfaceOrigin.kLowerLeft,
                        Engine.LoadOp.kLoad,
                        "TestDevice",
                        true
                );
                Objects.requireNonNull(device);
                mSurface = new GraniteSurface(device); // move
            }
            if (POST_PROCESS) {
                mPostSurface = GraniteSurface.makeRenderTarget(
                        mRC,
                        ImageInfo.make(CANVAS_WIDTH, CANVAS_HEIGHT, ColorInfo.CT_RGBA_8888,
                                ColorInfo.AT_PREMUL, ColorSpace.get(ColorSpace.Named.SRGB)),
                        false,
                        Engine.SurfaceOrigin.kLowerLeft,
                        "TestDevice2"
                );
                Objects.requireNonNull(mPostSurface);
            }

            {
                int[] x = {0}, y = {0}, channels = {0};
                var imgData = STBImage.stbi_load(
                        "F:/123459857_p0.png",
                        x, y, channels, 4
                );
                if (imgData != null) {
                    Pixmap testPixmap = new Pixmap(
                            ImageInfo.make(x[0], y[0], ColorInfo.CT_RGBA_8888, ColorInfo.AT_UNPREMUL, null),
                            null,
                            MemoryUtil.memAddress(imgData),
                            4 * x[0]
                    );
                    var newInfo = ImageInfo.make(x[0], y[0], ColorInfo.CT_RGBA_F16, ColorInfo.AT_UNPREMUL, null);
                    long newPixels = MemoryUtil.nmemAlloc(newInfo.computeMinByteSize());
                    Pixmap convertedPixmap = new Pixmap(
                            newInfo, null, newPixels, newInfo.minRowBytes()
                    );
                    boolean res = PixelUtils.convertPixels(testPixmap, convertedPixmap);
                    assert res;
                    mTestImage = TextureUtils.makeFromPixmap(mRC,
                            convertedPixmap,
                            false,
                            true,
                            "TestLocalImage");
                    LOGGER.info("Loaded texture image {}", mTestImage);
                    STBImage.stbi_image_free(imgData);
                    MemoryUtil.nmemFree(newPixels);
                }
            }

            if (mTestImage != null) {
                var scalingMatrix = new Matrix();
                //scalingMatrix.setScaleTranslate(13, 13, 0, 0);
                mTestShader1 = ImageShader.make(
                        RefCnt.create(mTestImage),
                        Shader.TILE_MODE_CLAMP,
                        Shader.TILE_MODE_CLAMP,
                        SamplingOptions.LINEAR,
                        scalingMatrix);
                mTestShader2 = ImageShader.make(
                        RefCnt.create(mTestImage),
                        Shader.TILE_MODE_CLAMP,
                        Shader.TILE_MODE_CLAMP,
                        SamplingOptions.MITCHELL,
                        scalingMatrix);
                mTestShader3 = ImageShader.make(
                        RefCnt.create(mTestImage),
                        Shader.TILE_MODE_CLAMP,
                        Shader.TILE_MODE_CLAMP,
                        SamplingOptions.CUBIC_BSPLINE,
                        scalingMatrix);
            } else {
                mTestShader1 = new ColorShader(0xFF8888FF);
                mTestShader2 = RefCnt.create(mTestShader1);
                mTestShader3 = RefCnt.create(mTestShader1);
            }
            /* = AngularGradient.make(
                584, 534, 0, 360,
                new float[]{
                        0.2f, 0.85f, 0.95f, 1,
                        0.85f, 0.5f, 0.75f, 1,
                        0.95f, 0.5f, 0.05f, 1,
                        0.75f, 0.95f, 0.7f, 1,
                        0.6f, 0.25f, 0.65f, 1},
                ColorSpace.get(ColorSpace.Named.SRGB),
                null,//new float[]{0.0f, 0.2f, 0.7f, 1.0f},
                5,
                Shader.TILE_MODE_CLAMP,
                GradientShader.Interpolation.make(false,
                        GradientShader.Interpolation.kSRGBLinear_ColorSpace,
                        GradientShader.Interpolation.kShorter_HueMethod),
                null
        );*/
            mGradShader = LinearGradient.make(
                    400, 350, 800, 350,
                    new float[]{
                            45 / 255f, 212 / 255f, 191 / 255f, 1,
                            14 / 255f, 165 / 255f, 233 / 255f, 1},
                    ColorSpace.get(ColorSpace.Named.SRGB),
                    null,
                    2,
                    Shader.TILE_MODE_MIRROR,
                    GradientShader.Interpolation.make(false,
                            GradientShader.Interpolation.kSRGBLinear_ColorSpace,
                            GradientShader.Interpolation.kShorter_HueMethod),
                    null
            );

            Typeface_JDK typeface = new Typeface_JDK(
                    new java.awt.Font("Microsoft YaHei", java.awt.Font.PLAIN, 1));
            {
                char[] text = "9-nine-天色天歌天籁音".toCharArray();
                float fontSize = 14;
                var vector = typeface.getFont().deriveFont(fontSize).layoutGlyphVector(
                        new FontRenderContext(null, true, true),
                        text, 0, text.length, java.awt.Font.LAYOUT_LEFT_TO_RIGHT);
                int nGlyphs = vector.getNumGlyphs();
                int[] glyphs = vector.getGlyphCodes(0, nGlyphs, null);
                float[] positions = vector.getGlyphPositions(0, nGlyphs, null);

                Font font = new Font();
                font.setTypeface(typeface);
                font.setSize(fontSize);
                font.setEdging(Font.kAntiAlias_Edging);
                font.setLinearMetrics(true);
                font.setSubpixel(true);

                /*Paint paint = new Paint();
                paint.setStyle(Paint.STROKE);
                paint.setStrokeJoin(Paint.JOIN_MITER);
                paint.setStrokeWidth(2);*/

                mTextBlob1 = TextBlob.make(
                        glyphs, 0,
                        positions, 0,
                        nGlyphs, font, null
                );
                mTextBlob2 = TextBlob.make(
                        glyphs, 0,
                        positions, 0,
                        nGlyphs, font, null
                );

                /*GlyphRunBuilder builder = new GlyphRunBuilder();
                mSubRunContainer = SubRunContainer.make(
                        builder.setGlyphRunList(
                                glyphs, 0,
                                positions, 0,
                                nGlyphs, font,
                                paint,
                                400, 400
                        ),
                        Matrix.identity(),
                        paint,
                        StrikeCache.getGlobalStrikeCache()
                );
                LOGGER.info("SubRunContainer size: {}", mSubRunContainer.getMemorySize());*/
            }

            {
                FloatBuffer linePoints = FloatBuffer.allocate(16);
                IntBuffer lineColors = IntBuffer.allocate(linePoints.capacity() / 2);

                FloatBuffer trianglePoints = FloatBuffer.allocate(12);
                IntBuffer triangleColors = IntBuffer.allocate(trianglePoints.capacity() / 2);

                linePoints
                        .put(100).put(100)
                        .put(110).put(200)
                        .put(120).put(100)
                        .put(130).put(300)
                        .put(140).put(100)
                        .put(150).put(400)
                        .put(160).put(100)
                        .put(170).put(500)
                        .flip();
                lineColors
                        .put(0xAAFF0000)
                        .put(0xFFFF00FF)
                        .put(0xAA0000FF)
                        .put(0xFF00FF00)
                        .put(0xAA00FFFF)
                        .put(0xFF00FF00)
                        .put(0xAAFFFF00)
                        .put(0xFFFFFFFF)
                        .flip();
                trianglePoints
                        .put(420).put(20)
                        .put(420).put(100)
                        .put(490).put(60)
                        .put(300).put(130)
                        .put(250).put(180)
                        .put(350).put(180)
                        .flip();
                triangleColors
                        .put(0xAAFF0000)
                        .put(0xFFFF00FF)
                        .put(0xAA0000FF)
                        .put(0xAA00FFFF)
                        .put(0xFF00FF00)
                        .put(0xAAFFFF00)
                        .flip();

                mVertices1 = Vertices.makeCopy(
                        Vertices.kLines_VertexMode, linePoints, null, lineColors, null
                );
                mVertices2 = Vertices.makeCopy(
                        Vertices.kTriangles_VertexMode, trianglePoints, null, triangleColors, null
                );
            }

            {
                float[] src = {33 / 255f, 150 / 255f, 243 / 255f, 204 / 255f};
                for (int i = 0; i < BlendMode.COUNT; i++) {
                    mBlendModeColorFilters[i] = BlendModeColorFilter.make(
                            src, null, BlendMode.modeAt(i)
                    );
                }
            }
        }

        private void drawScene(Canvas canvas) {
            final int nRects = 10000;
            canvas.clear(0x00000000);
            canvas.save();
            Paint paint = new Paint();
            if (TEST_SCENE == 0) {
                Matrix4 mat = new Matrix4();
                mat.m34 = -1 / 1920f;
                mat.preRotateX(MathUtil.PI_O_6);
                canvas.concat(mat);
                Rect2f rrect = new Rect2f();
                paint.setStroke(true);
                paint.setStrokeWidth(10);
                for (int i = 0; i < nRects; i++) {
                    int cx = mRandom.nextInt(MAX_RECT_WIDTH / 2, CANVAS_WIDTH - MAX_RECT_WIDTH / 2);
                    int cy = mRandom.nextInt(MAX_RECT_HEIGHT / 2, CANVAS_HEIGHT - MAX_RECT_HEIGHT / 2);
                    int w = (int) (mRandom.nextDouble() * mRandom.nextDouble() * mRandom.nextDouble() * mRandom.nextDouble() *
                            (MAX_RECT_WIDTH - MIN_RECT_WIDTH)) + MIN_RECT_WIDTH;
                    int h = (int) (mRandom.nextDouble() * mRandom.nextDouble() * mRandom.nextDouble() * mRandom.nextDouble() *
                            (MAX_RECT_HEIGHT - MIN_RECT_HEIGHT)) + MIN_RECT_HEIGHT;
                    int rad = Math.min(mRandom.nextInt(MAX_CORNER_RADIUS), Math.min(w, h) / 2);
                    rrect.set(
                            cx - (int) Math.ceil(w / 2d),
                            cy - (int) Math.ceil(h / 2d),
                            cx + (int) Math.floor(w / 2d),
                            cy + (int) Math.floor(h / 2d)/*,
                            rad, rad*/
                    );
                    int stroke = mRandom.nextInt(50);
                    //paint.setStyle(stroke < 25 ? Paint.FILL : Paint.STROKE);
                    //paint.setStrokeWidth((stroke - 20) * 2);
                    paint.setRGBA(mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256),
                            mRandom.nextInt(128));
                    canvas.drawRect(rrect, paint);
                    canvas.drawArc(cx, cy, rad, 20, 130, paint);
                    if ((i & 15) == 0) {
                        canvas.translate(CANVAS_WIDTH/2, CANVAS_HEIGHT/2);
                        canvas.rotate(1);
                        canvas.translate(-CANVAS_WIDTH/2, -CANVAS_HEIGHT/2);
                    }
                }
            } else if (TEST_SCENE == 1) {
                RRect rrect = new RRect();
                rrect.setRectXY(30, 60, 220, 120, 10, 10);
                paint.setStyle(Paint.STROKE);
                int[] aligns = {Paint.ALIGN_INSIDE, Paint.ALIGN_CENTER, Paint.ALIGN_OUTSIDE};
                paint.setStrokeWidth(10);
                for (int i = 0; i < 3; i++) {
                    paint.setStrokeAlign(aligns[i]);
                    paint.setRGBA(mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256), 255);
                    canvas.drawRRect(rrect, paint);
                    canvas.translate(230, 0);
                    canvas.rotate(9);
                }
                Rect2f rect = new Rect2f();
                rrect.getRect(rect);
                //paint.setStrokeAlign(Paint.ALIGN_CENTER);
                paint.setStrokeJoin(Paint.JOIN_MITER);
                paint.setRGBA(mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256), 255);
                canvas.drawRect(rect, paint);
                Runnable lines = () -> {
                    paint.setRGBA(mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256), 255);
                    canvas.drawLine(300, 220 - 30, 20, 220, Paint.CAP_BUTT, 10f, paint);
                    paint.setRGBA(mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256), 255);
                    canvas.drawLine(300, 240 - 20, 20, 240, Paint.CAP_ROUND, 10f, paint);
                    paint.setRGBA(mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256), 255);
                    canvas.drawLine(300, 260 - 10, 20, 260, Paint.CAP_SQUARE, 10f, paint);
                };
                canvas.resetMatrix();
                paint.setStyle(Paint.FILL);
                lines.run();

                canvas.translate(0, 100);
                paint.setStyle(Paint.STROKE);
                paint.setStrokeWidth(4);
                paint.setStrokeJoin(Paint.JOIN_MITER);
                paint.setStrokeAlign(Paint.ALIGN_CENTER);
                lines.run();

                canvas.translate(0, 100);
                paint.setStrokeJoin(Paint.JOIN_ROUND);
                lines.run();

                canvas.translate(0, 100);
                paint.setStrokeAlign(Paint.ALIGN_INSIDE);
                lines.run();

                canvas.translate(0, 100);
                paint.setStrokeAlign(Paint.ALIGN_OUTSIDE);
                lines.run();

                canvas.resetMatrix();

                paint.setRGBA(255, 255, 255, 255);
                paint.setShader(RefCnt.create(mTestShader1));
                paint.setStyle(Paint.FILL);
                //paint.setRGBA(mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256), 255);
                canvas.drawCircle(300, 300, 20, paint);

                paint.setStyle(Paint.STROKE);
                paint.setStrokeJoin(Paint.JOIN_BEVEL);
                paint.setStrokeCap(Paint.CAP_SQUARE);
                //paint.setRGBA(mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256), 255);
                canvas.drawCircle(400, 300, 20, paint);

                paint.setStrokeAlign(Paint.ALIGN_CENTER);
                paint.setRGBA(mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256), 255);

                float[] pts = new float[22];
                pts[0] = 300;
                pts[1] = 500;
                for (int i = 2; i < pts.length; i += 2) {
                    pts[i] = pts[i - 2] + mRandom.nextFloat(30, 60);
                    pts[i + 1] = mRandom.nextFloat(450, 550);
                }
                paint.setStrokeCap(Paint.CAP_ROUND);
                canvas.drawPoints(Canvas.POINT_MODE_POLYGON, pts, 0, 11, paint);

                paint.setRGBA(255, 255, 255, 255);
                canvas.drawCircle(500, 300, 20, paint);

                paint.setStyle(Paint.FILL);
                paint.setShader(RefCnt.create(mGradShader));
                paint.setDither(true);
                rrect.setRectXY(100, 650, 1500, 950, 30, 30);
                canvas.drawRRect(rrect, paint);

                Matrix4 perspectiveMatrix = new Matrix4();
                perspectiveMatrix.setIdentity();
                perspectiveMatrix.m34 = -1.0f / 1920f;
                //perspectiveMatrix.preRotateY(Math.toRadians(30));
                perspectiveMatrix.preRotateX((MathUtil.DEG_TO_RAD * 45) *
                        Math.sin(System.currentTimeMillis() / 1000.0 * (116 / 60.0) / 4 * 2 * Math.PI));
                perspectiveMatrix.preTranslate(-CANVAS_WIDTH / 2f, -CANVAS_HEIGHT / 2f);
                perspectiveMatrix.postTranslate(CANVAS_WIDTH / 2f, CANVAS_HEIGHT / 2f);
                canvas.save();
                canvas.setMatrix(perspectiveMatrix);
                paint.setStyle(Paint.STROKE);
                paint.setStrokeJoin(Paint.JOIN_MITER);
                paint.setStrokeWidth(2);
                canvas.drawTextBlob(mTextBlob1 != null ? mTextBlob1 : mTextBlob2, 400, 400, paint);
                canvas.restore();

                if (mRandom.nextDouble() < 0.001) {
                    mTextBlob1 = null;
                }

                canvas.concat(perspectiveMatrix);

                //canvas.scale(4, 4, 1100, 300);

                paint.setStyle(Paint.FILL);
                /*canvas.drawArc(1100, 300, 150, 90,
                        180 + mRandom.nextFloat(-30, 30),
                        Paint.CAP_SQUARE, 60, paint);*/
                paint.setStyle(Paint.FILL);
                paint.setStrokeCap(Paint.CAP_BUTT);
                paint.setStrokeWidth(60);
                canvas.drawArc(1100, 300, 150, 90, 180, paint);
                paint.setStyle(Paint.STROKE);
                paint.setStrokeJoin(Paint.JOIN_ROUND);
                paint.setStrokeAlign(Paint.ALIGN_CENTER);
                paint.setStrokeWidth(4);
                canvas.drawArc(1100, 300, 200, 45, 210,
                        Paint.CAP_BUTT, 60, paint);
                canvas.drawArc(1100, 300, 100, 90, 210,
                        Paint.CAP_ROUND, 20, paint);
                canvas.drawPie(1100, 300, 50, 90, 120, paint);
                canvas.drawChord(1100, 300, 50, 270, 120, paint);

                paint.setDither(false);

                paint.setShader(RefCnt.create(mTestShader1));
                paint.setStyle(Paint.FILL);
                paint.setAlphaF(0.7f);
                /*var mat = new Matrix4();
                mat.setTranslate(1000, 100, 0);
                canvas.setMatrix(mat);*/
                canvas.translate(1000, 100);
                rrect.setRectXY(200, 100, 600, 500, 20, 20);
                canvas.drawRRect(rrect, paint);

                paint.setShader(null);
                canvas.translate(-1000, 0);
                if (!TEST_OPENGL_ES) {
                    canvas.drawVertices(mVertices1, BlendMode.MODULATE, paint);
                    canvas.drawVertices(mVertices2, BlendMode.MODULATE, paint);
                }

                paint.setARGB(255, 233, 30, 99);
                rect.set(0, 0, 16, 16);
                for (int i = 0; i < 14; i++) {
                    for (int j = 0; j < 3; j++) {
                        /*if (((i ^ j) & 1) != 0) {
                            paint.setColor(0xFF00FF00);
                        } else {
                            paint.setColor(0xFFFFFFFF);
                        }*/
                        paint.setColorFilter(RefCnt.create(mBlendModeColorFilters[j * 14 + i]));
                        rect.offsetTo(400 + i * 24 + mRandom.nextInt(6),
                                450 + j * 24 + mRandom.nextInt(6));
                        canvas.drawRect(rect, paint);
                    }
                }

            } else if (TEST_SCENE == 2) {
                Rect2f rect = new Rect2f();
                rect.set(0, 0, canvas.getBaseLayerWidth(), canvas.getBaseLayerHeight());
                int wid = (int) (rect.mRight / 3);
                rect.mLeft = 2;
                rect.mRight = wid - 2;
                paint.setStyle(Paint.FILL);

                paint.setShader(RefCnt.create(mTestShader1));
                float scale = (float) (1 + 0.1 * Math.sin(System.currentTimeMillis() / 1000.0 * 2.0));
                canvas.scale(scale, scale);
                canvas.drawRect(rect, paint);

                paint.setShader(RefCnt.create(mTestShader2));
                canvas.translate(wid, 0);
                canvas.drawRect(rect, paint);

                paint.setShader(RefCnt.create(mTestShader3));
                canvas.translate(wid, 0);
                canvas.drawRect(rect, paint);
            } else if (TEST_SCENE == 3) {
                /*paint.setStyle(Paint.STROKE);
                paint.setStrokeJoin(Paint.JOIN_MITER);
                paint.setStrokeWidth(2);*/
                float scale = glfwGetTime() > 6.0 ? 1.5f : 1.0f;
                canvas.scale(scale, scale);
                canvas.drawTextBlob(mTextBlob1 != null ? mTextBlob1 : mTextBlob2, 400 + (System.currentTimeMillis() % 1000) / 100f, 400, paint);
                //canvas.drawTextBlob(mTextBlob1 != null ? mTextBlob1 : mTextBlob2, 800, 620, paint);
            }
            paint.close();
            canvas.restore();
        }

        public RootTask paint() {
            double time1 = GLFW.glfwGetTime();

            if (POST_PROCESS) {
                @SharedPtr
                Image snapshot;
                {
                    Canvas canvas = mSurface.getCanvas();
                    drawScene(canvas);
                    snapshot = mSurface.makeImageSnapshot();
                }
                if (snapshot != null) {
                    Canvas canvas = mPostSurface.getCanvas();
                    canvas.clear(0xFF000000);
                    Paint paint = new Paint();
                    paint.setAlphaF(0.5f);
                    paint.setDither(true);
                    canvas.drawImage(snapshot, 0, 0, SamplingOptions.LINEAR, paint);
                    paint.close();
                    snapshot.unref();
                } else {
                    LOGGER.error("Failed to create image snapshot");
                }
            } else {
                Canvas canvas = mSurface.getCanvas();
                drawScene(canvas);
            }

            RootTask rootTask = mRC.snap();

            double time2 = GLFW.glfwGetTime();

            /*LOGGER.info("Painting: {}",
                    formatMicroseconds(time2, time1));*/

            return rootTask;
        }

        public void close() {
            mTestShader1 = RefCnt.move(mTestShader1);
            mTestShader2 = RefCnt.move(mTestShader2);
            mTestShader3 = RefCnt.move(mTestShader3);
            mGradShader = RefCnt.move(mGradShader);
            mTestImage = RefCnt.move(mTestImage);
            for (int i = 0; i < BlendMode.COUNT; i++) {
                mBlendModeColorFilters[i] = RefCnt.move(mBlendModeColorFilters[i]);
            }

            mPostSurface = RefCnt.move(mPostSurface);
            mSurface = RefCnt.move(mSurface);
            mRC = RefCnt.move(mRC);

            LOGGER.info("Closed painter");
        }
    }
}
