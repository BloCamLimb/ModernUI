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

import icyllis.arc3d.core.*;
import icyllis.arc3d.core.Image;
import icyllis.arc3d.core.shaders.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.*;
import icyllis.arc3d.opengl.GLUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Random;

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

    public static final int TEST_SCENE = 1;

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
        GLFW.glfwInit();
        TinyFileDialogs.tinyfd_messageBox(
                "Arc3D Test",
                "Arc3D starting with pid: " + ProcessHandle.current().pid(),
                "ok",
                "info",
                true
        );
        Objects.requireNonNull(GL.getFunctionProvider());
        GLFW.glfwDefaultWindowHints();
        //GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Test Window", 0, 0);
        if (window == 0) {
            throw new RuntimeException();
        }
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);

        ContextOptions contextOptions = new ContextOptions();
        contextOptions.mLogger = LOGGER;
        contextOptions.mSkipGLErrorChecks = Boolean.TRUE;
        ImmediateContext immediateContext = GLUtil.makeOpenGL(
                GL.createCapabilities(),
                contextOptions
        );
        if (immediateContext == null) {
            throw new RuntimeException();
        }
        RecordingContext recordingContext = immediateContext.makeRecordingContext();

        @SharedPtr
        Surface_Granite surface;
        {
            @SharedPtr
            var device = Device_Granite.make(
                    recordingContext,
                    ImageInfo.make(CANVAS_WIDTH, CANVAS_HEIGHT, ColorInfo.CT_RGBA_8888,
                            ColorInfo.AT_PREMUL, ColorSpace.get(ColorSpace.Named.SRGB)),
                    ISurface.FLAG_SAMPLED_IMAGE | ISurface.FLAG_RENDERABLE | ISurface.FLAG_BUDGETED,
                    Engine.SurfaceOrigin.kLowerLeft,
                    Engine.LoadOp.kLoad,
                    "TestDevice"
            );
            Objects.requireNonNull(device);
            surface = new Surface_Granite(device); // move
        }

        @SharedPtr
        Image testImage = null;
        {
            int[] x = {0}, y = {0}, channels = {0};
            var imgData = STBImage.stbi_load(
                    "F:/119937433_p0.jpg",
                    x, y, channels, 4
            );
            if (imgData != null) {
                Pixmap testPixmap = new Pixmap(
                        ImageInfo.make(x[0], y[0], ColorInfo.CT_RGBA_8888, ColorInfo.AT_UNPREMUL, null),
                        null,
                        MemoryUtil.memAddress(imgData),
                        4 * x[0]
                );
                testImage = ImageUtils.makeFromPixmap(recordingContext,
                        testPixmap,
                        false,
                        true,
                        "TestLocalImage");
                LOGGER.info("Loaded texture image {}", testImage);
                STBImage.stbi_image_free(imgData);
            }
        }
        for (int y = 0; y < 8; ++y) {
            for (int x = 0; x < 8; ++x) {
                int m = (int) (Bayer8(x, y) * 64);
                System.out.printf("%02d ", m);
            }
            System.out.println();
        }

        Random random = new Random();
        double maxRadError = 0;
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
        LOGGER.info("max rad error {}, valid {}",  maxRadError, valid);

        /*GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT);
        GLFW.glfwSwapBuffers(window);*/
        TestDrawPass.glSetupDebugCallback();

        @SharedPtr
        Shader testShader1;
        @SharedPtr
        Shader testShader2;
        @SharedPtr
        Shader testShader3;
        if (testImage != null) {
            var scalingMatrix = new Matrix();
            //scalingMatrix.setScaleTranslate(13, 13, 0, 0);
            testShader1 = ImageShader.make(
                    RefCnt.create(testImage),
                    Shader.TILE_MODE_CLAMP,
                    Shader.TILE_MODE_CLAMP,
                    SamplingOptions.LINEAR,
                    scalingMatrix);
            testShader2 = ImageShader.make(
                    RefCnt.create(testImage),
                    Shader.TILE_MODE_CLAMP,
                    Shader.TILE_MODE_CLAMP,
                    SamplingOptions.MITCHELL,
                    scalingMatrix);
            testShader3 = ImageShader.make(
                    RefCnt.create(testImage),
                    Shader.TILE_MODE_CLAMP,
                    Shader.TILE_MODE_CLAMP,
                    SamplingOptions.CUBIC_BSPLINE,
                    scalingMatrix);
        } else {
            testShader1 = new ColorShader(0xFF8888FF);
            testShader2 = RefCnt.create(testShader1);
            testShader3 = RefCnt.create(testShader1);
        }
        @SharedPtr
        Shader gradShader = AngularGradient.makeAngular(
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
        );
        /*LinearGradient.makeLinear(
                400, 350, 800, 350,
                new float[]{
                        0.4f, 0.4f, 0.4f, 1,
                        0.6f, 0.6f, 0.6f, 1},
                ColorSpace.get(ColorSpace.Named.SRGB),
                null,
                2,
                Shader.TILE_MODE_MIRROR,
                GradientShader.Interpolation.make(false,
                        GradientShader.Interpolation.kSRGBLinear_ColorSpace,
                        GradientShader.Interpolation.kShorter_HueMethod),
                null
        );*/

        Paint paint = new Paint();
        Canvas canvas = surface.getCanvas();
        while (!GLFW.glfwWindowShouldClose(window)) {

            long time1 = System.nanoTime();

            int nRects = 4000;
            paint.reset();
            paint.setColor(0x00000000);
            paint.setBlendMode(BlendMode.SRC);
            canvas.drawPaint(paint);
            paint.reset();
            if (TEST_SCENE == 0) {
                RoundRect rrect = new RoundRect();
                for (int i = 0; i < nRects; i++) {
                    int cx = random.nextInt(MAX_RECT_WIDTH / 2, CANVAS_WIDTH - MAX_RECT_WIDTH / 2);
                    int cy = random.nextInt(MAX_RECT_HEIGHT / 2, CANVAS_HEIGHT - MAX_RECT_HEIGHT / 2);
                    int w = (int) (random.nextDouble() * random.nextDouble() * random.nextDouble() * random.nextDouble() *
                            (MAX_RECT_WIDTH - MIN_RECT_WIDTH)) + MIN_RECT_WIDTH;
                    int h = (int) (random.nextDouble() * random.nextDouble() * random.nextDouble() * random.nextDouble() *
                            (MAX_RECT_HEIGHT - MIN_RECT_HEIGHT)) + MIN_RECT_HEIGHT;
                    int rad = Math.min(random.nextInt(MAX_CORNER_RADIUS), Math.min(w, h) / 2);
                    rrect.setRectXY(
                            cx - (int) Math.ceil(w / 2d),
                            cy - (int) Math.ceil(h / 2d),
                            cx + (int) Math.floor(w / 2d),
                            cy + (int) Math.floor(h / 2d),
                            rad, rad
                    );
                    int stroke = random.nextInt(50);
                    paint.setStyle(stroke < 25 ? Paint.FILL : Paint.STROKE);
                    paint.setStrokeWidth((stroke - 20) * 2);
                    paint.setRGBA(random.nextInt(256), random.nextInt(256), random.nextInt(256), random.nextInt(128));
                    canvas.drawRoundRect(rrect, paint);
                }
            } else if (TEST_SCENE == 1) {
                RoundRect rrect = new RoundRect();
                rrect.setRectXY(30, 60, 260, 120, 10, 10);
                paint.setStyle(Paint.STROKE);
                int[] aligns = {Paint.ALIGN_INSIDE, Paint.ALIGN_CENTER, Paint.ALIGN_OUTSIDE};
                paint.setStrokeWidth(10);
                Matrix4 mat = Matrix4.identity();
                for (int i = 0; i < 3; i++) {
                    paint.setStrokeAlign(aligns[i]);
                    canvas.setMatrix(mat);
                    paint.setRGBA(random.nextInt(256), random.nextInt(256), random.nextInt(256), 255);
                    canvas.drawRoundRect(rrect, paint);
                    mat.preTranslateX(270);
                    mat.preRotateZ(MathUtil.PI / 20);
                }
                Rect2f rect = new Rect2f();
                rrect.getRect(rect);
                //paint.setStrokeAlign(Paint.ALIGN_CENTER);
                paint.setStrokeJoin(Paint.JOIN_MITER);
                canvas.setMatrix(mat);
                paint.setRGBA(random.nextInt(256), random.nextInt(256), random.nextInt(256), 255);
                canvas.drawRect(rect, paint);
                Runnable lines = () -> {
                    paint.setRGBA(random.nextInt(256), random.nextInt(256), random.nextInt(256), 255);
                    //drawDevice.drawLine(300, 220 - 30, 20, 220, Paint.CAP_BUTT, 10f, paint);
                    paint.setRGBA(random.nextInt(256), random.nextInt(256), random.nextInt(256), 255);
                    //drawDevice.drawLine(300, 240 - 20, 20, 240, Paint.CAP_ROUND, 10f, paint);
                    paint.setRGBA(random.nextInt(256), random.nextInt(256), random.nextInt(256), 255);
                    //drawDevice.drawLine(300, 260 - 10, 20, 260, Paint.CAP_SQUARE, 10f, paint);
                };
                mat.setIdentity();
                canvas.setMatrix(mat);
                paint.setStyle(Paint.FILL);
                lines.run();

                mat.preTranslateY(100);
                canvas.setMatrix(mat);
                paint.setStyle(Paint.STROKE);
                paint.setStrokeWidth(4);
                paint.setStrokeJoin(Paint.JOIN_MITER);
                paint.setStrokeAlign(Paint.ALIGN_CENTER);
                lines.run();

                mat.preTranslateY(100);
                canvas.setMatrix(mat);
                paint.setStrokeJoin(Paint.JOIN_ROUND);
                lines.run();

                mat.preTranslateY(100);
                canvas.setMatrix(mat);
                paint.setStrokeAlign(Paint.ALIGN_INSIDE);
                lines.run();

                mat.preTranslateY(100);
                canvas.setMatrix(mat);
                paint.setStrokeAlign(Paint.ALIGN_OUTSIDE);
                lines.run();

                mat.setIdentity();
                canvas.setMatrix(mat);

                paint.setShader(RefCnt.create(testShader1));
                paint.setStyle(Paint.FILL);
                paint.setRGBA(random.nextInt(256), random.nextInt(256), random.nextInt(256), 255);
                canvas.drawCircle(500, 300, 20, paint);

                paint.setStyle(Paint.STROKE);
                paint.setStrokeJoin(Paint.JOIN_BEVEL);
                paint.setStrokeCap(Paint.CAP_SQUARE);
                paint.setRGBA(random.nextInt(256), random.nextInt(256), random.nextInt(256), 255);
                canvas.drawCircle(600, 300, 20, paint);

                paint.setStrokeAlign(Paint.ALIGN_CENTER);
                paint.setRGBA(random.nextInt(256), random.nextInt(256), random.nextInt(256), 255);

                canvas.drawCircle(700, 300, 20, paint);


                paint.setStyle(Paint.FILL);
                paint.setShader(RefCnt.create(gradShader));
                paint.setDither(true);
                rect.set(400, 350, 800, 750);
                /*rect.set(0, 0, 16, 16);
                for (int i = 0; i < 16; i++) {
                    for (int j = 0; j < 16; j++) {
                        if (((i ^ j) & 1) != 0) {
                            paint.setColor(0xFF00FF00);
                        } else {
                            paint.setColor(0xFFFFFFFF);
                        }
                        rect.offsetTo(400 + i * 24 + random.nextInt(6),
                                350 + j * 24 + random.nextInt(6));
                        canvas.drawRect(rect, paint);
                    }
                }*/
                canvas.drawRect(rect, paint);
                paint.setDither(false);

                paint.setShader(RefCnt.create(testShader1));
                mat.setTranslate(600, 100, 0);
                canvas.setMatrix(mat);
                rrect.setRectXY(200, 100, 600, 500, 20, 20);
                canvas.drawRoundRect(rrect, paint);

            } else if (TEST_SCENE == 2) {
                Rect2f rect = new Rect2f();
                rect.set(0, 0, canvas.getBaseLayerWidth(), canvas.getBaseLayerHeight());
                int wid = (int) (rect.mRight / 3);
                rect.mLeft = 2;
                rect.mRight = wid - 2;
                paint.setStyle(Paint.FILL);

                paint.setShader(RefCnt.create(testShader1));
                Matrix4 mat = Matrix4.identity();
                float scale = (float) (1+0.1*Math.sin(System.currentTimeMillis()/1000.0*2.0));
                mat.preScale(scale, scale);
                canvas.setMatrix(mat);
                canvas.drawRect(rect, paint);

                paint.setShader(RefCnt.create(testShader2));
                mat.preTranslateX(wid);
                canvas.setMatrix(mat);
                canvas.drawRect(rect, paint);

                paint.setShader(RefCnt.create(testShader3));
                mat.preTranslateX(wid);
                canvas.setMatrix(mat);
                canvas.drawRect(rect, paint);
            }

            long time2 = System.nanoTime();

            surface.flush();

            long time3 = System.nanoTime();

            RootTask rootTask = recordingContext.snap();

            long time4 = System.nanoTime();

            if (!immediateContext.addTask(rootTask)) {
                LOGGER.error("Failed to add recording");
            }
            RefCnt.move(rootTask);

            long time5 = System.nanoTime();

            GL33C.glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER, 0);
            boolean filter = CANVAS_WIDTH == WINDOW_WIDTH && CANVAS_HEIGHT == WINDOW_HEIGHT;
            GL33C.glBlitFramebuffer(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT,
                    0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, GL33C.GL_COLOR_BUFFER_BIT,
                    filter ? GL33C.GL_LINEAR : GL33C.GL_NEAREST);
            if (!immediateContext.submit()) {
                LOGGER.error("Failed to submit queue");
            }

            long time6 = System.nanoTime();

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwWaitEvents();

            long time7 = System.nanoTime();
            /*LOGGER.info("Painting: {}, CreateRenderPass: {}, CreateFrameTask: {}, AddCommands: {}, " +
                            "Blit/Submit/CheckFence: {}, Swap/Event: {}",
                    formatMicroseconds(time2, time1),
                    formatMicroseconds(time3, time2),
                    formatMicroseconds(time4, time3),
                    formatMicroseconds(time5, time4),
                    formatMicroseconds(time6, time5),
                    formatMicroseconds(time7, time6));*/
        }
        paint.close();
        testShader1 = RefCnt.move(testShader1);
        testShader2 = RefCnt.move(testShader2);
        testShader3 = RefCnt.move(testShader3);
        gradShader = RefCnt.move(gradShader);
        testImage = RefCnt.move(testImage);

        {
            long rowStride = (long) CANVAS_WIDTH * 4;
            long srcPixels = MemoryUtil.nmemAlloc(rowStride * CANVAS_HEIGHT);
            long dstPixels = MemoryUtil.nmemAlloc(rowStride * CANVAS_HEIGHT);
            GL33C.glReadPixels(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, GL33C.GL_RGBA, GL33C.GL_UNSIGNED_BYTE, srcPixels);
            // premul to unpremul, and flip Y
            ImageInfo srcInfo = ImageInfo.make(CANVAS_WIDTH, CANVAS_HEIGHT,
                    ColorInfo.CT_RGBA_8888, ColorInfo.AT_PREMUL, null);
            ImageInfo dstInfo = srcInfo.makeAlphaType(ColorInfo.AT_UNPREMUL);
            boolean res = PixelUtils.convertPixels(
                    srcInfo, null, srcPixels, rowStride,
                    dstInfo, null, dstPixels, rowStride,
                    true
            );
            assert res;
            MemoryUtil.nmemFree(srcPixels);
            STBImageWrite.stbi_write_png_compression_level.put(0, 15);
            STBImageWrite.stbi_write_png("test_granite.png", CANVAS_WIDTH, CANVAS_HEIGHT, 4,
                    MemoryUtil.memByteBuffer(dstPixels, CANVAS_WIDTH * CANVAS_HEIGHT * 4), (int) rowStride);
            MemoryUtil.nmemFree(dstPixels);
        }
        surface = RefCnt.move(surface);
        recordingContext.unref();
        immediateContext.unref();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static String formatMicroseconds(long e, long st) {
        return String.format("%.1f us", (e - st) / 1000D);
    }
}
