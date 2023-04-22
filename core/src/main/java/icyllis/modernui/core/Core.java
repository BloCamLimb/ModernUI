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

import icyllis.modernui.akashi.opengl.GLCore;
import icyllis.modernui.annotation.*;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

import static icyllis.modernui.ModernUI.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * The core class for thread management and sub-system initializing, also provides utility methods of
 * memory operations and thread scheduling.
 */
public final class Core {

    private static volatile Thread sMainThread;
    private static Thread sRenderThread;
    private static Thread sUiThread;

    private static volatile Handler sMainHandlerAsync;
    private static Handler sUiHandler;
    private static Handler sUiHandlerAsync;

    private static final ConcurrentLinkedQueue<Runnable> sMainCalls = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Runnable> sRenderCalls = new ConcurrentLinkedQueue<>();

    private Core() {
    }

    /**
     * Initializes the GLFW and the main thread.
     * <p>
     * If the GLFW has already been initialized, this method just specifies the current thread as
     * the main thread. If both are done, it will cause an assertion error.
     */
    @MainThread
    public static void initialize() {
        synchronized (Core.class) {
            if (sMainThread == null) {
                GLFWErrorCallback cb = GLFW.glfwSetErrorCallback(null);
                if (cb != null) {
                    GLFW.glfwSetErrorCallback(cb);
                } else {
                    LOGGER.info(MARKER, "Backend Library: LWJGL {}", Version.getVersion());
                    GLFW.glfwSetErrorCallback(new GLFWErrorCallback() {
                        @Override
                        public void invoke(int error, long description) {
                            LOGGER.error(MARKER, "GLFW Error: 0x{} {}",
                                    Integer.toHexString(error), memUTF8Safe(description));
                        }
                    });
                }
                if (!GLFW.glfwInit()) {
                    throw new IllegalStateException("Failed to initialize GLFW");
                }
                sMainThread = Thread.currentThread();
            } else {
                assert false;
            }
        }
    }

    /**
     * Terminates the GLFW.
     */
    @MainThread
    public static void terminate() {
        GLFWErrorCallback cb = GLFW.glfwSetErrorCallback(null);
        if (cb != null) {
            cb.close();
        }
        GLFW.glfwTerminate();
        LOGGER.info(MARKER, "Terminated GLFW");
    }

    // not locked, but visible
    public static void checkMainThread() {
        if (Thread.currentThread() != sMainThread)
            throw new IllegalStateException("Not called from main thread. Current " + Thread.currentThread());
    }

    // not locked, but visible on checking, and locked on failure
    public static void checkRenderThread() {
        if (Thread.currentThread() != sRenderThread)
            synchronized (Core.class) {
                if (sRenderThread == null)
                    throw new IllegalStateException("Render thread was never initialized. " +
                            "Please check whether the loader threw an exception before.");
                else
                    throw new IllegalStateException("Not called from render thread. Desired " + sRenderThread +
                            " current " + Thread.currentThread());
            }
    }

    // not locked, but visible
    public static Thread getMainThread() {
        return sMainThread;
    }

    // not locked, but visible
    public static Thread getRenderThread() {
        return sRenderThread;
    }

    // not locked, but visible
    public static boolean isOnMainThread() {
        return Thread.currentThread() == sMainThread;
    }

    // not locked, but visible
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
            synchronized (Core.class) {
                if (sMainHandlerAsync == null) {
                    if (Looper.getMainLooper() == null) {
                        throw new RuntimeException("The main event loop does not exist. Modern UI may be embedded.");
                    }
                    sMainHandlerAsync = Handler.createAsync(Looper.getMainLooper());
                }
            }
        }
        return sMainHandlerAsync;
    }

    /**
     * Post a delayed operation that will be executed on main thread.
     *
     * @param r the runnable
     */
    public static void postOnMainThread(@NonNull Runnable r) {
        if (Looper.getMainLooper() == null) {
            sMainCalls.offer(r);
        } else {
            getMainHandlerAsync().post(r);
        }
    }

    /**
     * This should be rarely used. Only when the render thread and the main thread are the same thread,
     * and you need to call some methods that must be called on the main thread.
     *
     * @param r the runnable
     */
    public static void executeOnMainThread(@NonNull Runnable r) {
        if (isOnMainThread()) {
            r.run();
        } else {
            postOnMainThread(r);
        }
    }

    /**
     * Post a delayed operation that will be executed on render thread.
     * Render thread is not a looper thread.
     *
     * @param r the runnable
     */
    public static void postOnRenderThread(@NonNull Runnable r) {
        sRenderCalls.offer(r);
    }

    /**
     * Post a runnable to the render thread queue or execute the runnable immediately.
     *
     * @param r the runnable
     */
    public static void executeOnRenderThread(@NonNull Runnable r) {
        if (isOnRenderThread()) {
            r.run();
        } else {
            postOnRenderThread(r);
        }
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
        synchronized (Core.class) {
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
        GLCore.initialize(caps);
    }

    /**
     * Return whether render thread is initialized. Not locked, but visible.
     *
     * @return whether render thread is initialized
     */
    public static boolean hasRenderThread() {
        return sRenderThread != null;
    }

    /**
     * Initialize UI thread and its event loop.
     *
     * @return the event loop
     */
    @NonNull
    @UiThread
    public static Looper initUiThread() {
        synchronized (Core.class) {
            if (sUiThread == null) {
                sUiThread = Thread.currentThread();
                final Looper looper = Looper.prepare();
                sUiHandler = new Handler(looper);
                sUiHandlerAsync = Handler.createAsync(looper);
                return looper;
            } else {
                throw new IllegalStateException("Initialize twice");
            }
        }
    }

    public static void checkUiThread() {
        if (Thread.currentThread() != sUiThread)
            synchronized (Core.class) {
                if (sUiThread == null)
                    throw new IllegalStateException("UI thread was never initialized. " +
                            "Please check whether the loader threw an exception before.");
                else
                    throw new IllegalStateException("Not called from UI thread. Desired " + sUiThread +
                            " current " + Thread.currentThread());
            }
    }

    // not locked, but visible
    public static Thread getUiThread() {
        return sUiThread;
    }

    // not locked, but visible
    public static boolean isOnUiThread() {
        return Thread.currentThread() == sUiThread;
    }

    /**
     * Return whether UI thread is initialized. Not locked, but visible.
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
     * @see #getUiHandler()
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
     * Calling this method is faster than {@link System#nanoTime()}. This time base
     * is used in all input events and frame events, but not in high-level API
     * (such as animations).
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
     * You should NOT use this time base in high-level API (such as animations).
     *
     * @return current time in milliseconds
     */
    public static long timeMillis() {
        return (long) (GLFW.glfwGetTime() * 1.0E3);
    }

    /**
     * Allocates native memory and read buffered resource. The memory <b>MUST</b> be
     * manually freed by {@link MemoryUtil#memFree(Buffer)}. The stream can NOT be
     * larger than 2 GB. This method does NOT close the channel.
     *
     * @param channel where to read input from
     * @return the native pointer to {@code unsigned char *data}
     * @throws IOException some errors occurred while reading
     */
    @NonNull
    public static ByteBuffer readIntoNativeBuffer(@NonNull ReadableByteChannel channel) throws IOException {
        ByteBuffer p = null;
        try {
            if (channel instanceof final SeekableByteChannel ch) {
                long rem = ch.size() - ch.position() + 1; // +1 EOF
                if (rem > Integer.MAX_VALUE) {
                    throw new IOException("File is too big, found " + (rem - 1) + " bytes");
                }
                p = memAlloc((int) rem);
                //noinspection StatementWithEmptyBody
                while (ch.read(p) != -1)
                    ;
            } else {
                p = memAlloc(4096);
                while (channel.read(p) != -1) {
                    if (!p.hasRemaining()) {
                        long cap = p.capacity();
                        if (cap == Integer.MAX_VALUE) {
                            throw new IOException("File is too big");
                        }
                        p = memRealloc(p, (int) Math.min(cap + (cap >> 1), // grow 50%
                                Integer.MAX_VALUE));
                    }
                }
            }
        } catch (Throwable t) {
            // IMPORTANT: we cannot return the pointer, so it must be freed here
            memFree(p);
            throw t;
        }
        return p;
    }

    /**
     * Allocates native memory and read buffered resource. The memory <b>MUST</b> be
     * manually freed by {@link MemoryUtil#memFree(Buffer)}. The stream can NOT be
     * larger than 2 GB. This method does NOT close the channel.
     *
     * @param stream where to read input from
     * @return the native pointer to {@code unsigned char *data}
     * @throws IOException some errors occurred while reading
     */
    @NonNull
    public static ByteBuffer readIntoNativeBuffer(@NonNull InputStream stream) throws IOException {
        return readIntoNativeBuffer(Channels.newChannel(stream));
    }

    /**
     * This method doesn't close channel. No code point validation.
     *
     * @param channel read from
     * @return string or null if an IOException occurred
     */
    @Nullable
    public static String readUTF8(@NonNull ReadableByteChannel channel) {
        ByteBuffer p = null;
        try {
            p = readIntoNativeBuffer(channel);
            int n = p.position();
            byte[] bytes = new byte[n];
            p.get(0, bytes, 0, n);
            return new String(bytes, 0, n, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        } finally {
            memFree(p);
        }
    }

    /**
     * This method doesn't close channel. No code point validation.
     *
     * @param stream read from
     * @return string or null if an IOException occurred
     */
    @Nullable
    public static String readUTF8(@NonNull InputStream stream) {
        return readUTF8(Channels.newChannel(stream));
    }

    /**
     * Launches the associated application to open the URL.
     *
     * @return true on success, false on failure
     */
    public static boolean openURL(@NonNull URL url) {
        try {
            String s = url.toString();
            String[] cmd = switch (Platform.get()) {
                case WINDOWS -> new String[]{"rundll32", "url.dll,FileProtocolHandler", s};
                case MACOSX -> new String[]{"open", s};
                default -> new String[]{"xdg-open", url.getProtocol().equals("file")
                        ? s.replace("file:", "file://")
                        : s};
            };
            Process proc = Runtime.getRuntime().exec(cmd);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
                reader.lines().forEach(line -> LOGGER.error(MARKER, line));
            }
            return true;
        } catch (IOException e) {
            LOGGER.error(MARKER, "Failed to open URL: {}", url, e);
            return false;
        }
    }

    /**
     * Launches the associated application to open the URL.
     *
     * @return true on success, false on failure
     */
    public static boolean openURI(@NonNull URI uri) {
        try {
            return openURL(uri.toURL());
        } catch (Exception e) {
            LOGGER.error(MARKER, "Failed to open URI: {}", uri, e);
            return false;
        }
    }

    /**
     * Launches the associated application to open the URL.
     *
     * @return true on success, false on failure
     */
    public static boolean openURI(@NonNull String uri) {
        try {
            return openURI(URI.create(uri));
        } catch (Exception e) {
            LOGGER.error(MARKER, "Failed to open URI: {}", uri, e);
            return false;
        }
    }
}
