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

package icyllis.modernui.platform;

import icyllis.modernui.graphics.GLWrapper;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static icyllis.modernui.ModernUI.LOGGER;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class RenderCore {

    public static final Marker MARKER = MarkerManager.getMarker("Graphics");

    private static boolean sInitialized = false;
    static boolean sIgnoreFormatError = false;

    private static Thread sRenderThread;

    private static final Queue<Runnable> sRenderCalls = new ConcurrentLinkedQueue<>();

    /**
     * Initialize GLFW, call on JVM main thread.
     */
    public static void initBackend() {
        LOGGER.info(MARKER, "Backend Library: LWJGL {}", Version.getVersion());
        if (GLFW.glfwSetErrorCallback(RenderCore::onError) != null || !GLFW.glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }
    }

    private static void onError(int errorCode, long descPtr) {
        if (errorCode == GLFW.GLFW_FORMAT_UNAVAILABLE && sIgnoreFormatError)
            return;
        String desc = descPtr == NULL ? "" : MemoryUtil.memUTF8(descPtr);
        LOGGER.error(MARKER, "GLFW Error: 0x{}, {}", Integer.toHexString(errorCode), desc);
    }

    public static void ensureRenderThread() {
        if (Thread.currentThread() != sRenderThread)
            throw new IllegalStateException("Not called from render thread");
    }

    public static boolean isOnRenderThread() {
        return Thread.currentThread() == sRenderThread;
    }

    public static void interruptThread() {
        sRenderThread.interrupt();
    }

    public static void recordRenderCall(@Nonnull Runnable r) {
        sRenderCalls.offer(r);
    }

    public static void flushRenderCalls() {
        Runnable r;
        while ((r = sRenderCalls.poll()) != null)
            r.run();
    }

    /**
     * Call after creating a Window on render thread.
     */
    public static void initialize() {
        if (sInitialized) {
            return;
        }
        sRenderThread = Thread.currentThread();

        // get or create
        GLCapabilities caps;
        try {
            caps = GL.getCapabilities();
            LOGGER.debug(MARKER, "Sharing OpenGL context with an existing one");
        } catch (IllegalStateException e) {
            caps = GL.createCapabilities();
        }
        GLWrapper.initialize(caps);

        sInitialized = true;
    }

    public static boolean isInitialized() {
        return sInitialized;
    }

    public static long timeNanos() {
        return (long) (GLFW.glfwGetTime() * 1.0E9);
    }

    public static long timeMillis() {
        return (long) (GLFW.glfwGetTime() * 1.0E3);
    }

    // this method doesn't close stream, MemoryUtil.memFree(buffer) is required as well
    @Nonnull
    public static ByteBuffer readRawBuffer(InputStream stream) throws IOException {
        ByteBuffer buffer;
        if (stream instanceof FileInputStream) {
            final FileChannel channel = ((FileInputStream) stream).getChannel();
            buffer = MemoryUtil.memAlloc((int) channel.size() + 1);
            for (; ; )
                if (channel.read(buffer) == -1)
                    break;
        } else {
            final ReadableByteChannel channel = Channels.newChannel(stream);
            buffer = MemoryUtil.memAlloc(8192);
            for (; ; )
                if (channel.read(buffer) == -1)
                    break;
                else if (buffer.remaining() == 0)
                    buffer = MemoryUtil.memRealloc(buffer, buffer.capacity() << 1);
        }
        return buffer;
    }

    // this method doesn't close stream,
    @Nullable
    public static String readStringASCII(InputStream stream) {
        ByteBuffer buffer = null;
        try {
            buffer = readRawBuffer(stream);
            final int len = buffer.position();
            buffer.rewind();
            return MemoryUtil.memASCII(buffer, len);
        } catch (IOException ignored) {

        } finally {
            if (buffer != null)
                MemoryUtil.memFree(buffer);
        }
        return null;
    }

    /*@Nonnull
    public static UniformFloat getUniformFloat(@Nonnull ShaderProgram shader, String name) {
        return new UniformFloat(GL20.glGetUniformLocation(shader.getProgram(), name));
    }

    @Nonnull
    public static UniformMatrix4f getUniformMatrix4f(@Nonnull ShaderProgram shader, String name) {
        int loc = GL20.glGetUniformLocation(shader.getProgram(), name);
        if (loc == -1) {
            throw new RuntimeException();
        }
        return new UniformMatrix4f(loc);
    }*/
}
