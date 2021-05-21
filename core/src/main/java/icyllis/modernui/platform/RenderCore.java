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
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static icyllis.modernui.ModernUI.LOGGER;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Represents the window system and its backend class.
 */
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

    /**
     * Allocates memory in native and read buffered resource.
     * The memory must be manually freed by {@link MemoryUtil#memFree(Buffer)}.
     *
     * @param channel where to read input from
     * @return the native pointer to {@code unsigned char *data}
     * @throws IOException some errors occurred while reading
     */
    @Nonnull
    public static ByteBuffer readResource(ReadableByteChannel channel) throws IOException {
        ByteBuffer ptr = null;
        try {
            if (channel instanceof SeekableByteChannel) {
                final SeekableByteChannel ch = (SeekableByteChannel) channel;
                ptr = MemoryUtil.memAlloc((int) ch.size() + 1); // +1 EOF
                //noinspection StatementWithEmptyBody
                while (ch.read(ptr) != -1) ;
            } else {
                ptr = MemoryUtil.memAlloc(4096);
                while (channel.read(ptr) != -1)
                    if (ptr.remaining() <= 0)
                        ptr = MemoryUtil.memRealloc(ptr, ptr.capacity() << 1);
            }
        } catch (Throwable t) {
            MemoryUtil.memFree(ptr);
            throw t;
        }
        return ptr;
    }

    /**
     * Allocates memory in native and read resource to buffer.
     * The memory must be manually freed by {@link MemoryUtil#memFree(Buffer)}.
     *
     * @param stream where to read input from
     * @return the native pointer to {@code unsigned char *data}
     * @throws IOException some errors occurred while reading
     */
    @Nonnull
    public static ByteBuffer readResource(InputStream stream) throws IOException {
        return readResource(Channels.newChannel(stream));
    }

    // this method doesn't close stream,
    @Nullable
    public static String readStringUTF8(InputStream stream) {
        ByteBuffer ptr = null;
        try {
            ptr = readResource(stream);
            final int len = ptr.position();
            ptr.rewind();
            return MemoryUtil.memUTF8(ptr, len);
        } catch (IOException e) {
            return null;
        } finally {
            MemoryUtil.memFree(ptr);
        }
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
