/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.core;

import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.engine.ImmediateContext;
import icyllis.arc3d.granite.RecordingContext;
import icyllis.modernui.annotation.MainThread;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.annotation.UiThread;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.sdl.SDLMisc;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.Cleaner;
import java.net.URI;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import static icyllis.modernui.util.Log.LOGGER;
import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLMisc.SDL_OpenURL;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * The core class for thread management and sub-system initializing, also provides utility methods of
 * memory operations and thread scheduling.
 */
public final class Core {

    public static final Marker MARKER = MarkerFactory.getMarker("Core");

    private static final Cleaner sCleaner = Cleaner.create();

    private static volatile Looper sMainLooper;
    private static volatile Looper sUiLooper;

    private static volatile Thread sMainThread;
    private static volatile Thread sRenderThread;
    private static volatile Thread sUiThread;

    private static volatile Handler sMainHandler;
    private static volatile Handler sMainHandlerAsync;
    private static volatile Handler sUiHandler;
    private static volatile Handler sUiHandlerAsync;

    private static final Executor sMainThreadExecutor = Core::executeOnMainThread;
    private static final Executor sUiThreadExecutor = Core::executeOnUiThread;

    private static volatile ImmediateContext sImmediateContext;
    private static volatile RecordingContext sUiRecordingContext;

    private Core() {
    }

    /**
     * Registers a target and a cleaning action to run when the target becomes phantom
     * reachable. The action object should never hold any reference to the target object.
     *
     * @param target the target to monitor
     * @param action a {@code Runnable} to invoke when the target becomes phantom reachable
     * @return a {@code Cleanable} instance representing the registry entry
     */
    @NonNull
    public static Cleaner.Cleanable registerCleanup(@NonNull Object target, @NonNull Runnable action) {
        return sCleaner.register(target, action);
    }

    /**
     * Registers a target and a resource object to unref when the target becomes phantom
     * reachable. The resource object should never hold any reference to the target object.
     *
     * @param target   the target to monitor
     * @param resource a {@code RefCounted} to invoke when the target becomes phantom reachable
     * @return a {@code Cleanable} instance representing the registry entry
     * @hidden
     */
    @ApiStatus.Internal
    @NonNull
    public static Cleaner.Cleanable registerNativeResource(@NonNull Object target, @NonNull icyllis.arc3d.core.RefCounted resource) {
        return sCleaner.register(target, resource::unref);
    }

    /**
     * Registers a target and a resource object to close when the target becomes phantom
     * reachable. The resource object should never hold any reference to the target object.
     *
     * @param target   the target to monitor
     * @param resource a {@code AutoCloseable} to invoke when the target becomes phantom reachable
     * @return a {@code Cleanable} instance representing the registry entry
     * @hidden
     */
    @ApiStatus.Internal
    @NonNull
    public static Cleaner.Cleanable registerNativeResource(@NonNull Object target, @NonNull AutoCloseable resource) {
        return sCleaner.register(target, () -> {
            try {
                resource.close();
            } catch (Exception ignored) {
            }
        });
    }

    @NonNull
    public static Cleaner cleaner() {
        return sCleaner;
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
                    Objects.requireNonNull(GLFW.glfwSetErrorCallback(null)).free();
                    throw new UnsupportedOperationException("Failed to initialize GLFW");
                }
                sMainThread = Thread.currentThread();
            } else {
                assert false;
            }
        }
    }

    public static void setMainThread() {
        sMainThread = Thread.currentThread();
        sMainLooper = Looper.myLooper();
    }

    /**
     * Prepare the main event loop. This must be called from the entry point of the application.
     */
    @ApiStatus.Internal
    @MainThread
    public static void prepareMainLooper() {
        Core.checkMainThread();
        if (sMainLooper != null) {
            throw new IllegalStateException();
        }
        sMainLooper = Looper.prepare(new Poller() {
            @Override
            public void poll(Thread thread, long timeoutMillis) {
                if (timeoutMillis < 0) {
                    GLFW.glfwWaitEvents();
                } else if (timeoutMillis == 0) {
                    GLFW.glfwPollEvents();
                } else {
                    // There is a GLFW bug on Windows:
                    // glfwWaitEventsTimeout doesn't process QS_SENDMESSAGE on Windows, if our
                    // MessageQueue is not empty, running TinyFileDialogs on a background thread
                    // will cause glfwWaitEventsTimeout won't process Dialogs input event.
                    //
                    // Workaround is running TinyFileDialogs on main thread and cause our UI to
                    // block, or always use glfwWaitEvents/glfwPollEvents (Minecraft does this).
                    // If ModernUI runs independently, UI thread is main thread. If ModernUI
                    // runs with Minecraft, UI thread is another thread. Thus, the conclusion is
                    // to run TinyFileDialogs on the UI thread.
                    //
                    // UI thread never block render thread, so this doesn't matter.
                    GLFW.glfwWaitEventsTimeout(timeoutMillis / 1000D);
                }
            }

            @Override
            public void wake(Thread thread) {
                GLFW.glfwPostEmptyEvent();
            }

            @Override
            public void destroy(Thread thread) {
            }
        });
    }

    /**
     * Terminates the GLFW.
     */
    @MainThread
    public static void terminate() {
        checkMainThread();
        GLFWErrorCallback cb = GLFW.glfwSetErrorCallback(null);
        if (cb != null) {
            cb.close();
        }
        GLFW.glfwTerminate();
        LOGGER.info(MARKER, "Terminated GLFW");
    }

    /**
     * Ensures that the current thread is the main thread, otherwise a runtime exception will be thrown.
     */
    public static void checkMainThread() {
        if (Thread.currentThread() != sMainThread)
            throw new IllegalStateException("Not called from the main thread, current " + Thread.currentThread());
    }

    /**
     * @return the main thread if initialized, or null
     */
    public static Thread getMainThread() {
        return sMainThread;
    }

    /**
     * @return whether the current thread is the main thread
     */
    public static boolean isOnMainThread() {
        return Thread.currentThread() == sMainThread;
    }

    /**
     * Returns the application's main looper, which lives in the main thread of the application.
     */
    public static Looper getMainLooper() {
        return sMainLooper;
    }

    public static void setRenderThread(@RawPtr ImmediateContext immediateContext) {
        sRenderThread = Thread.currentThread();
        sImmediateContext = immediateContext;
    }

    /**
     * Ensures that the current thread is the render thread, otherwise a runtime exception will be thrown.
     */
    public static void checkRenderThread() {
        if (Thread.currentThread() != sRenderThread)
            synchronized (Core.class) {
                if (sRenderThread == null)
                    throw new IllegalStateException("The render thread has not been initialized yet.");
                else
                    throw new IllegalStateException("Not called from the render thread " + sRenderThread +
                            ", current " + Thread.currentThread());
            }
    }

    /**
     * @return the render thread if initialized, or null
     */
    public static Thread getRenderThread() {
        return sRenderThread;
    }

    /**
     * @return whether the current thread is the render thread
     */
    public static boolean isOnRenderThread() {
        return Thread.currentThread() == sRenderThread;
    }

    @NonNull
    @RenderThread
    public static ImmediateContext requireImmediateContext() {
        checkRenderThread();
        return Objects.requireNonNull(sImmediateContext,
                "Immediate context has not been created yet, or creation failed");
    }

    public static ImmediateContext peekImmediateContext() {
        return sImmediateContext;
    }

    /**
     * Returns a shared main thread handler. The handler is not always available. Consider
     * {@link #executeOnMainThread(Runnable)} instead.
     *
     * @return async main handler
     */
    @NonNull
    public static Handler getMainHandlerAsync() {
        if (sMainHandlerAsync == null) {
            synchronized (Core.class) {
                if (sMainHandlerAsync == null) {
                    if (getMainLooper() == null) {
                        throw new IllegalStateException("The main event loop does not exist.");
                    }
                    sMainHandlerAsync = Handler.createAsync(getMainLooper());
                }
            }
        }
        return sMainHandlerAsync;
    }

    /**
     * Post an async operation that will be executed on main thread.
     *
     * @param r the runnable
     */
    public static void postOnMainThread(@NonNull Runnable r) {
        getMainHandlerAsync().post(r);
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

    @NonNull
    public static Executor getMainThreadExecutor() {
        return sMainThreadExecutor;
    }

    public static void setUiThread(@RawPtr RecordingContext uiRecordingContext) {
        sUiThread = Thread.currentThread();
        if (sUiThread == sMainThread) {
            sUiLooper = getMainLooper();
        }
    }

    /**
     * Initializes UI thread and its event loop.
     * <p>
     * UI thread can be the main thread iff the main thread is a looper thread.
     *
     * @return the event loop
     */
    @NonNull
    @UiThread
    public static Looper initUiThread() {
        synchronized (Core.class) {
            if (sUiThread == null) {
                sUiThread = Thread.currentThread();

                final Looper looper;
                if (sUiThread == sMainThread) {
                    looper = getMainLooper();
                } else {
                    looper = Looper.prepare();
                }
                sUiHandler = new Handler(looper);
                sUiHandlerAsync = Handler.createAsync(looper);

                if (sImmediateContext != null) {
                    sUiRecordingContext = RecordingContext.makeRecordingContext(sImmediateContext,
                            new RecordingContext.Options());
                    Objects.requireNonNull(sUiRecordingContext);
                } else {
                    LOGGER.warn(MARKER, "UI thread initializing without a GPU device");
                }

                return looper;
            } else {
                throw new IllegalStateException();
            }
        }
    }

    /**
     * Ensures that the current thread is the UI thread, otherwise a runtime exception will be thrown.
     */
    public static void checkUiThread() {
        if (Thread.currentThread() != sUiThread)
            synchronized (Core.class) {
                if (sUiThread == null)
                    throw new IllegalStateException("The UI thread has not been initialized yet.");
                else
                    throw new IllegalStateException("Not called from the UI thread " + sRenderThread +
                            ", current " + Thread.currentThread());
            }
    }

    /**
     * @return the UI thread if initialized, or null
     */
    public static Thread getUiThread() {
        return sUiThread;
    }

    /**
     * @return whether the current thread is the UI thread
     */
    public static boolean isOnUiThread() {
        return Thread.currentThread() == sUiThread;
    }

    @NonNull
    @UiThread
    public static RecordingContext requireUiRecordingContext() {
        checkUiThread();
        return Objects.requireNonNull(sUiRecordingContext,
                "UI recording context has not been created yet, or creation failed");
    }

    public static RecordingContext peekUiRecordingContext() {
        return sUiRecordingContext;
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
     * Post an async operation that will be executed on main thread.
     *
     * @param r the runnable
     */
    public static void postOnUiThread(@NonNull Runnable r) {
        getUiHandlerAsync().post(r);
    }

    /**
     * This should be rarely used. Only when the render thread and the main thread are the same thread,
     * and you need to call some methods that must be called on the main thread.
     *
     * @param r the runnable
     */
    public static void executeOnUiThread(@NonNull Runnable r) {
        if (isOnUiThread()) {
            r.run();
        } else {
            postOnUiThread(r);
        }
    }

    @NonNull
    public static Executor getUiThreadExecutor() {
        return sUiThreadExecutor;
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
     * manually freed by {@link MemoryUtil#memFree(Buffer)}. This method can read up
     * to 2GB. This method does NOT close the channel.
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
                long rem = ch.size() - ch.position() + 1;
                p = memAlloc((int) Math.min(rem,
                        Integer.MAX_VALUE));
                //noinspection StatementWithEmptyBody
                while (ch.read(p) > 0)
                    ;
            } else {
                p = memAlloc(4096);
                while (channel.read(p) != -1) {
                    if (p.hasRemaining()) {
                        continue;
                    }
                    long cap = p.capacity();
                    if (cap == Integer.MAX_VALUE) {
                        break;
                    }
                    p = memRealloc(p, (int) Math.min(cap + (cap >> 1), // grow 50%
                            Integer.MAX_VALUE));
                }
            }
        } catch (Throwable t) {
            memFree((Buffer) p);
            throw t;
        }
        return p;
    }

    /**
     * Allocates native memory and read buffered resource. The memory <b>MUST</b> be
     * manually freed by {@link MemoryUtil#memFree(Buffer)}. This method can read up
     * to 2GB. This method does NOT close the stream.
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
     * Launches the associated application to open the URI.
     *
     * @return true on success, false on failure
     */
    public static boolean openURI(@NonNull URI uri) {
        String s = uri.toString();
        if (!SDL_OpenURL(s)) {
            LOGGER.error(MARKER, "Failed to open URI {}, error: {}", uri, SDL_GetError());
            return false;
        }
        return true;
    }

    /**
     * Launches the associated application to open the URI.
     *
     * @return true on success, false on failure
     */
    public static boolean openURI(@NonNull String uri) {
        try {
            return openURI(new URI(uri));
        } catch (Exception e) {
            LOGGER.error(MARKER, "Failed to open URI {}", uri, e);
            return false;
        }
    }
}
