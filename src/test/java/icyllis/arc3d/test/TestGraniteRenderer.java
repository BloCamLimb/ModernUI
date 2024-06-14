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
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.RootTask;
import icyllis.arc3d.granite.SurfaceDevice;
import icyllis.arc3d.opengl.GLUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Random;

public class TestGraniteRenderer {

    public static final Logger LOGGER = LoggerFactory.getLogger("Arc3D");

    public static final int CANVAS_WIDTH = 3840;
    public static final int CANVAS_HEIGHT = 2160;

    public static final int MAX_RECT_WIDTH = 370;
    public static final int MIN_RECT_WIDTH = 20;
    public static final int MAX_RECT_HEIGHT = 270;
    public static final int MIN_RECT_HEIGHT = 20;
    public static final int MAX_CORNER_RADIUS = 50;

    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;

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

        SurfaceDevice drawDevice = SurfaceDevice.make(
                recordingContext,
                ImageInfo.make(CANVAS_WIDTH, CANVAS_HEIGHT, ColorInfo.CT_RGBA_8888,
                        ColorInfo.AT_PREMUL, ColorSpace.get(ColorSpace.Named.SRGB)),
                ISurface.FLAG_SAMPLED_IMAGE | ISurface.FLAG_RENDERABLE | ISurface.FLAG_BUDGETED,
                Engine.SurfaceOrigin.kLowerLeft,
                Engine.LoadOp.kLoad,
                "TestDevice"
        );
        assert drawDevice != null;



        /*GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT);
        GLFW.glfwSwapBuffers(window);*/


        while (!GLFW.glfwWindowShouldClose(window)) {
            Paint paint = new Paint();
            Random random = new Random();

            long time1 = System.nanoTime();

            int nRects = 4000;
            paint.setColor(0x00000000);
            drawDevice.drawPaint(paint);
            for (int i = 0; i < nRects; i++) {
                RoundRect rrect = new RoundRect();
                int cx = random.nextInt(MAX_RECT_WIDTH / 2, CANVAS_WIDTH - MAX_RECT_WIDTH / 2);
                int cy = random.nextInt(MAX_RECT_HEIGHT / 2, CANVAS_HEIGHT - MAX_RECT_HEIGHT / 2);
                int w = (int) (random.nextDouble() * random.nextDouble() * random.nextDouble() * random.nextDouble() *
                        (MAX_RECT_WIDTH - MIN_RECT_WIDTH)) + MIN_RECT_WIDTH;
                int h = (int) (random.nextDouble() * random.nextDouble() * random.nextDouble() * random.nextDouble() *
                        (MAX_RECT_HEIGHT - MIN_RECT_HEIGHT)) + MIN_RECT_HEIGHT;
                rrect.mLeft = cx - (int) Math.ceil(w / 2d);
                rrect.mTop = cy - (int) Math.ceil(h / 2d);
                rrect.mRight = cx + (int) Math.floor(w / 2d);
                rrect.mBottom = cy + (int) Math.floor(h / 2d);
                rrect.mRadiusUL = Math.min(random.nextInt(MAX_CORNER_RADIUS), Math.min(w, h) / 2);
                int stroke = random.nextInt(50);
                paint.setStyle(stroke < 25 ? Paint.FILL : Paint.STROKE);
                paint.setStrokeWidth((stroke - 20) * 2);
                paint.setRGBA(random.nextInt(256), random.nextInt(256), random.nextInt(256), random.nextInt(128));
                drawDevice.drawRoundRect(rrect, paint);
            }

            long time2 = System.nanoTime();

            drawDevice.flush();

            long time3 = System.nanoTime();

            RootTask rootTask = recordingContext.snap();
            assert rootTask != null;

            long time4 = System.nanoTime();

            if (!immediateContext.addTask(rootTask)) {
                LOGGER.info("Failed to add recording");
            }
            rootTask.unref();

            long time5 = System.nanoTime();

            GL33C.glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER, 0);
            GL33C.glBlitFramebuffer(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT,
                    0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, GL33C.GL_COLOR_BUFFER_BIT, GL33C.GL_LINEAR);
            if (!immediateContext.submit()) {
                LOGGER.info("Failed to submit queue");
            }

            long time6 = System.nanoTime();

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwWaitEvents();

            long time7 = System.nanoTime();
            LOGGER.info("Painting: {}, CreateRenderPass: {}, CreateFrameTask: {}, AddCommands: {}, " +
                            "Blit/Submit/CheckFence: {}, Swap/Event: {}",
                    formatMicroseconds(time2, time1),
                    formatMicroseconds(time3, time2),
                    formatMicroseconds(time4, time3),
                    formatMicroseconds(time5, time4),
                    formatMicroseconds(time6, time5),
                    formatMicroseconds(time7, time6));
        }
        long pixels = MemoryUtil.nmemAlloc(CANVAS_WIDTH * CANVAS_HEIGHT * 4);
        GL33C.glReadPixels(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, GL33C.GL_RGBA, GL33C.GL_UNSIGNED_BYTE, pixels);
        STBImageWrite.stbi_flip_vertically_on_write(true);
        STBImageWrite.stbi_write_png_compression_level.put(0, 15);
        STBImageWrite.stbi_write_png("E:/test_granite.png", CANVAS_WIDTH, CANVAS_HEIGHT, 4,
                MemoryUtil.memByteBuffer(pixels, CANVAS_WIDTH * CANVAS_HEIGHT * 4), CANVAS_WIDTH * 4);
        MemoryUtil.nmemFree(pixels);
        drawDevice.unref();
        recordingContext.unref();
        immediateContext.unref();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static String formatMicroseconds(long e, long st) {
        return String.format("%.1f us", (e - st) / 1000D);
    }
}
