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

package icyllis.modernui.core;

import icyllis.modernui.annotation.MainThread;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.graphics.GLWrapper;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
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
import java.util.concurrent.ConcurrentLinkedQueue;

import static icyllis.modernui.ModernUI.LOGGER;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * The core class for the window system and graphics backend,
 * also provides utility methods of native operations and thread scheduling.
 */
public final class ArchCore {

    public static final Marker MARKER = MarkerManager.getMarker("ArchCore");

    private static volatile Thread sMainThread;
    private static Thread sRenderThread;
    private static Thread sUiThread;

    private static volatile Handler sMainHandlerAsync;
    private static Handler sUiHandler;
    private static Handler sUiHandlerAsync;

    private static final ConcurrentLinkedQueue<Runnable> sMainCalls = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Runnable> sRenderCalls = new ConcurrentLinkedQueue<>();

    /**
     * Initialize GLFW, call on JVM main thread. This method also specify the main thread.
     */
    @MainThread
    public static void initialize() {
        LOGGER.info(MARKER, "Backend Library: LWJGL {}", Version.getVersion());
        if (GLFW.glfwSetErrorCallback(ArchCore::onError) != null || !GLFW.glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }
        sMainThread = Thread.currentThread();
    }

    private static void onError(int error, long description) {
        LOGGER.error(MARKER, "GLFW Error: 0x{} {}", Integer.toHexString(error), memUTF8Safe(description));
    }

    /**
     * Terminates the GLFW.
     */
    @MainThread
    public static void terminate() {
        GLFWErrorCallback cb = GLFW.glfwSetErrorCallback(null);
        if (cb != null) {
            cb.free();
        }
        GLFW.glfwTerminate();
        LOGGER.info(MARKER, "Terminated GLFW");
    }

    /**
     * Initialize only the main thread. Cannot be used with {@link #initialize()}.
     */
    @MainThread
    public static void initMainThread() {
        synchronized (ArchCore.class) {
            if (sMainThread == null) {
                sMainThread = Thread.currentThread();
            } else {
                throw new IllegalStateException("Initialize twice");
            }
        }
    }

    public static void checkMainThread() {
        if (Thread.currentThread() != sMainThread)
            throw new IllegalStateException("Not called from main thread. Current " + Thread.currentThread());
    }

    public static void checkRenderThread() {
        if (Thread.currentThread() != sRenderThread)
            synchronized (ArchCore.class) {
                if (sRenderThread == null)
                    throw new IllegalStateException("Render thread was never initialized. " +
                            "Please check whether the loader threw an exception before.");
                else
                    throw new IllegalStateException("Not called from render thread. Desired " + sRenderThread +
                            " current " + Thread.currentThread());
            }
    }

    public static Thread getMainThread() {
        return sMainThread;
    }

    public static Thread getRenderThread() {
        return sRenderThread;
    }

    public static boolean isOnMainThread() {
        return Thread.currentThread() == sMainThread;
    }

    public static boolean isOnRenderThread() {
        return Thread.currentThread() == sRenderThread;
    }

    /**
     * Returns a shared main thread handler. The handler is not always available. Consider
     * {@link #executeOnMainThread(Runnable)} instead.
     *
     * @return async main handler
     */
    public static Handler getMainHandlerAsync() {
        if (sMainHandlerAsync == null) {
            synchronized (ArchCore.class) {
                if (sMainHandlerAsync == null) {
                    sMainHandlerAsync = Handler.createAsync(Looper.getMainLooper());
                }
            }
        }
        return sMainHandlerAsync;
    }

    /**
     * This should be rarely used. Only when the render thread and the main thread are the same thread,
     * and you need to call some methods that must be called on the main thread.
     *
     * @param r the runnable
     */
    public static void executeOnMainThread(@Nonnull Runnable r) {
        if (isOnMainThread()) {
            r.run();
        } else {
            if (Looper.getMainLooper() == null) {
                sMainCalls.offer(r);
            } else {
                getMainHandlerAsync().post(r);
            }
        }
    }

    /**
     * Post a delayed operation that will be executed on render thread.
     *
     * @param r the runnable
     */
    public static void postOnRenderThread(@Nonnull Runnable r) {
        sRenderCalls.offer(r);
    }

    /**
     * Flush main thread calls. Use only if the application is not running independently.
     */
    public static void flushMainCalls() {
        //noinspection UnnecessaryLocalVariable
        final ConcurrentLinkedQueue<Runnable> queue = sMainCalls;
        Runnable r;
        while ((r = queue.poll()) != null) r.run();
    }

    /**
     * Flush render thread calls.
     */
    public static void flushRenderCalls() {
        //noinspection UnnecessaryLocalVariable
        final ConcurrentLinkedQueue<Runnable> queue = sRenderCalls;
        Runnable r;
        while ((r = queue.poll()) != null) r.run();
    }

    /**
     * Call after creating a Window on render thread.
     */
    @RenderThread
    public static void initOpenGL() {
        synchronized (ArchCore.class) {
            if (sRenderThread == null) {
                sRenderThread = Thread.currentThread();
            } else {
                throw new IllegalStateException("Initialize twice");
            }
        }

        // get or create
        GLCapabilities caps;
        try {
            caps = GL.getCapabilities();
            //noinspection ConstantConditions
            if (caps == null) {
                // checks may be disabled
                caps = GL.createCapabilities();
            }
        } catch (IllegalStateException e) {
            caps = GL.createCapabilities();
        }
        //noinspection ConstantConditions
        if (caps == null) {
            throw new IllegalStateException("Failed to acquire OpenGL capabilities");
        }
        GLWrapper.initialize(caps);
    }

    /**
     * Return whether render thread is initialized.
     *
     * @return whether render thread is initialized
     */
    public static boolean hasRenderThread() {
        return sRenderThread != null;
    }

    @UiThread
    public static void initUiThread() {
        synchronized (ArchCore.class) {
            if (sUiThread == null) {
                sUiThread = Thread.currentThread();
                sUiHandler = new Handler(Looper.myLooper());
                sUiHandlerAsync = Handler.createAsync(Looper.myLooper());
            } else {
                throw new IllegalStateException("Initialize twice");
            }
        }
    }

    public static void checkUiThread() {
        if (Thread.currentThread() != sUiThread)
            synchronized (ArchCore.class) {
                if (sUiThread == null)
                    throw new IllegalStateException("UI thread was never initialized. " +
                            "Please check whether the loader threw an exception before.");
                else
                    throw new IllegalStateException("Not called from UI thread. Desired " + sUiThread +
                            " current " + Thread.currentThread());
            }
    }

    public static Thread getUiThread() {
        return sUiThread;
    }

    public static boolean isOnUiThread() {
        return Thread.currentThread() == sUiThread;
    }

    /**
     * Return whether UI thread is initialized.
     *
     * @return whether UI thread is initialized
     */
    public static boolean hasUiThread() {
        return sUiThread != null;
    }

    /**
     * Returns the shared {@link Handler} that created on UI thread, if initialized.
     * It can be used for thread scheduling of callback operations.
     *
     * @return the shared UI handler
     * @see #getUiHandlerAsync()
     */
    public static Handler getUiHandler() {
        return sUiHandler;
    }

    /**
     * Returns the shared {@link Handler} that created on UI thread, if initialized.
     * It can be used for thread scheduling of callback operations.
     * <p>
     * Differently from {@link #getUiHandler()}, this is an async version.
     * Messages sent to an async handler are guaranteed to be ordered with respect to one another,
     * but not necessarily with respect to messages from other Handlers.
     *
     * @return the shared UI handler
     */
    public static Handler getUiHandlerAsync() {
        return sUiHandlerAsync;
    }

    /**
     * Returns the current value of GLFW's highest-resolution monotonic time source,
     * in nanoseconds. The resolution of the timer is system dependent, but is usually
     * on the order of a few micro- or nanoseconds. The timer measures time elapsed
     * since GLFW was initialized.
     * <p>
     * This is a bit faster than {@link System#nanoTime()}. All input events and
     * frame events use this time base, but in advanced frameworks (such as animations),
     * you should NOT use this time base.
     *
     * @return current time in nanoseconds
     */
    public static long timeNanos() {
        return (long) (GLFW.glfwGetTime() * 1.0E9);
    }

    /**
     * Returns the current value of GLFW's highest-resolution monotonic time source,
     * in milliseconds. The resolution of the timer is system dependent, but is usually
     * on the order of a few micro- or nanoseconds. The timer measures time elapsed
     * since GLFW was initialized.
     * <p>
     * You should NOT use this time base in advanced frameworks (such as animations).
     *
     * @return current time in milliseconds
     */
    public static long timeMillis() {
        return (long) (GLFW.glfwGetTime() * 1.0E3);
    }

    /**
     * Allocates memory in native and read buffered resource.
     * The memory MUST be manually freed by {@link MemoryUtil#memFree(Buffer)}.
     *
     * @param channel where to read input from
     * @return the native pointer to {@code unsigned char *data}
     * @throws IOException some errors occurred while reading
     */
    @Nonnull
    public static ByteBuffer readInMemory(ReadableByteChannel channel) throws IOException {
        ByteBuffer p = null;
        try {
            if (channel instanceof final SeekableByteChannel ch) {
                long rem = ch.size() - ch.position();
                if (rem > 0x7FFFFFFE) {
                    throw new IOException("File is too big, found " + rem + " bytes");
                }
                p = memAlloc((int) (rem + 1)); // +1 EOF
                //noinspection StatementWithEmptyBody
                while (ch.read(p) != -1) ;
            } else {
                p = memAlloc(4096);
                while (channel.read(p) != -1) {
                    if (p.remaining() <= 0) {
                        int cap = p.capacity();
                        p = memRealloc(p, cap + (cap >> 1));
                    }
                }
            }
        } catch (Throwable t) {
            // IMPORTANT: in case of memory leakage
            memFree(p);
            throw t;
        }
        return p;
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
    public static ByteBuffer readInMemory(InputStream stream) throws IOException {
        return readInMemory(Channels.newChannel(stream));
    }

    /**
     * This method doesn't close channel.
     *
     * @param channel read from
     * @return string or null if an IOException occurred
     */
    @Nullable
    public static String readStringUTF8(ReadableByteChannel channel) {
        ByteBuffer p = null;
        try {
            p = readInMemory(channel);
            final int l = p.position();
            return memUTF8(p.rewind(), l);
        } catch (IOException e) {
            return null;
        } finally {
            memFree(p);
        }
    }

    // this method doesn't close stream
    @Nullable
    public static String readStringUTF8(InputStream stream) {
        return readStringUTF8(Channels.newChannel(stream));
    }
}
