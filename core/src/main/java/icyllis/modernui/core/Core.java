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

import icyllis.arc3d.core.RefCounted;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.opengl.GLUtil;
import icyllis.modernui.annotation.*;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import java.io.*;
import java.lang.ref.Cleaner;
import java.net.URI;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import static icyllis.arc3d.opengl.GLUtil.*;
import static icyllis.modernui.ModernUI.*;
import static org.lwjgl.opengl.AMDDebugOutput.glDebugMessageCallbackAMD;
import static org.lwjgl.opengl.ARBDebugOutput.glDebugMessageCallbackARB;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL43C.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * The core class for thread management and sub-system initializing, also provides utility methods of
 * memory operations and thread scheduling.
 */
public final class Core {

    private static final Cleaner sCleaner = Cleaner.create();

    private static volatile Thread sMainThread;
    private static volatile Thread sRenderThread;
    private static volatile Thread sUiThread;

    private static volatile Handler sMainHandlerAsync;
    private static volatile Handler sUiHandler;
    private static volatile Handler sUiHandlerAsync;

    private static final ConcurrentLinkedQueue<Runnable> sMainCalls = new ConcurrentLinkedQueue<>();

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
    public static Cleaner.Cleanable registerNativeResource(@NonNull Object target, @NonNull RefCounted resource) {
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

    @NonNull
    private static ContextOptions initContextOptions(@NonNull ContextOptions options) {
        if (options.mLogger == null || options.mLogger == NOPLogger.NOP_LOGGER) {
            options.mLogger = LoggerFactory.getLogger("Arc3D");
        }
        return options;
    }

    @RenderThread
    public static boolean initOpenGL() {
        return initOpenGL(new ContextOptions());
    }

    /**
     * Initializes OpenGL pipeline and the render thread.
     * <p>
     * Before calling this method, it is necessary to ensure that the GL library is loaded
     * and that the current thread has an OpenGL context for a certain platform window.
     *
     * @return true if successful
     */
    @RenderThread
    public static boolean initOpenGL(@NonNull ContextOptions options) {
        final ImmediateContext dc;
        synchronized (Core.class) {
            if (sImmediateContext != null) {
                if (sImmediateContext.getBackend() != Engine.BackendApi.kOpenGL) {
                    throw new IllegalStateException();
                }
                return true;
            }
            if (sRenderThread == null) {
                sRenderThread = Thread.currentThread();
            } else if (Thread.currentThread() != sRenderThread) {
                throw new IllegalStateException();
            }
            initContextOptions(options);
            dc = GLUtil.makeOpenGL(options);
            if (dc == null) {
                return false;
            }
            sImmediateContext = dc;
        }
        final String glVendor = GL11C.glGetString(GL11C.GL_VENDOR);
        final String glRenderer = GL11C.glGetString(GL11C.GL_RENDERER);
        final String glVersion = GL11C.glGetString(GL11C.GL_VERSION);

        LOGGER.info(MARKER, "OpenGL vendor: {}", glVendor);
        LOGGER.info(MARKER, "OpenGL renderer: {}", glRenderer);
        LOGGER.info(MARKER, "OpenGL version: {}", glVersion);
        StringBuilder sb = new StringBuilder("\n");
        ((icyllis.arc3d.opengl.GLCaps) dc.getCaps()).dump(sb, false);
        LOGGER.info(MARKER, "OpenGL caps: {}", sb);
        return true;
    }

    @RenderThread
    public static void glSetupDebugCallback() {

        GLCapabilities caps = GL.getCapabilities();

        if (glGetPointer(GL_DEBUG_CALLBACK_FUNCTION) == NULL) {
            if (caps.OpenGL43 || caps.GL_KHR_debug) {
                LOGGER.debug(MARKER, "Using OpenGL 4.3 for debug logging");
                glDebugMessageCallback(Core::glDebugMessage, NULL);
                glEnable(GL_DEBUG_OUTPUT);
            } else if (caps.GL_ARB_debug_output) {
                LOGGER.debug(MARKER, "Using ARB_debug_output for debug logging");
                GLDebugMessageARBCallback proc = new GLDebugMessageARBCallback() {
                    @Override
                    public void invoke(int source, int type, int id, int severity, int length, long message,
                                       long userParam) {
                        LOGGER.info(MARKER, "0x{}[{},{},{}]: {}", Integer.toHexString(id),
                                getSourceARB(source), getTypeARB(type), getSeverityARB(severity),
                                GLDebugMessageARBCallback.getMessage(length, message));
                    }
                };
                glDebugMessageCallbackARB(proc, NULL);
            } else if (caps.GL_AMD_debug_output) {
                LOGGER.debug(MARKER, "Using AMD_debug_output for debug logging");
                GLDebugMessageAMDCallback proc = new GLDebugMessageAMDCallback() {
                    @Override
                    public void invoke(int id, int category, int severity, int length, long message,
                                       long userParam) {
                        LOGGER.info(MARKER, "0x{}[{},{}]: {}", Integer.toHexString(id),
                                getCategoryAMD(category), getSeverityAMD(severity),
                                GLDebugMessageAMDCallback.getMessage(length, message));
                    }
                };
                glDebugMessageCallbackAMD(proc, NULL);
            } else {
                LOGGER.debug(MARKER, "No debug callback function was used...");
            }
        } else {
            LOGGER.debug(MARKER, "The debug callback function is already set.");
        }
    }

    public static void glDebugMessage(int source, int type, int id, int severity, int length, long message,
                                      long userParam) {
        switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH -> LOGGER.error(MARKER, "({}|{}|0x{}) {}",
                    getDebugSource(source), getDebugType(type), Integer.toHexString(id),
                    GLDebugMessageCallback.getMessage(length, message));
            case GL_DEBUG_SEVERITY_MEDIUM -> LOGGER.warn(MARKER, "({}|{}|0x{}) {}",
                    getDebugSource(source), getDebugType(type), Integer.toHexString(id),
                    GLDebugMessageCallback.getMessage(length, message));
            case GL_DEBUG_SEVERITY_LOW -> LOGGER.info(MARKER, "({}|{}|0x{}) {}",
                    getDebugSource(source), getDebugType(type), Integer.toHexString(id),
                    GLDebugMessageCallback.getMessage(length, message));
            case GL_DEBUG_SEVERITY_NOTIFICATION -> LOGGER.debug(MARKER, "({}|{}|0x{}) {}",
                    getDebugSource(source), getDebugType(type), Integer.toHexString(id),
                    GLDebugMessageCallback.getMessage(length, message));
        }
    }

    /**
     * Show a dialog that lists unsupported extensions after initialized.
     */
    @RenderThread
    public static void glShowCapsErrorDialog() {
        Core.checkRenderThread();
        if (sImmediateContext != null) {
            return;
        }
        final String glVendor = GL11C.glGetString(GL11C.GL_VENDOR);
        final String glRenderer = GL11C.glGetString(GL11C.GL_RENDERER);
        final String glVersion = GL11C.glGetString(GL11C.GL_VERSION);
        new Thread(() -> {
            String solution = "Please make sure you have up-to-date GPU drivers. " +
                    "Also make sure Java applications run with the discrete GPU if you have multiple GPUs.";
            TinyFileDialogs.tinyfd_messageBox("Failed to launch Modern UI",
                    "GPU: " + glVendor + " " + glRenderer + ", OpenGL: " + glVersion + ". " +
                            "OpenGL 3.3 or OpenGL ES 3.0 is required.\n" + solution,
                    "ok", "error", true);
        }, "GL-Error-Dialog").start();
    }

    //TODO WIP
    @RenderThread
    public static boolean initVulkan() {
        return initVulkan(new ContextOptions());
    }

    /**
     * Initializes Vulkan pipeline and the render thread.
     * <p>
     * Before calling this method, it is necessary to ensure that the VK library is loaded
     * and that Vulkan is available for the current platform.
     *
     * @return true if successful
     */
    //TODO WIP
    @RenderThread
    public static boolean initVulkan(@NonNull ContextOptions options) {
        final ImmediateContext dc;
        synchronized (Core.class) {
            if (sImmediateContext != null) {
                if (sImmediateContext.getBackend() != Engine.BackendApi.kVulkan) {
                    throw new IllegalStateException();
                }
                return true;
            }
            if (sRenderThread == null) {
                sRenderThread = Thread.currentThread();
            } else if (Thread.currentThread() != sRenderThread) {
                throw new IllegalStateException();
            }
            try {
                var vkManager = VulkanManager.getInstance();
                vkManager.initialize();
                initContextOptions(options);
                dc = vkManager.createContext(options);
                if (dc == null) {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            sImmediateContext = dc;
        }
        return true;
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
                    if (Looper.getMainLooper() == null) {
                        throw new IllegalStateException("The main event loop does not exist.");
                    }
                    sMainHandlerAsync = Handler.createAsync(Looper.getMainLooper());
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

    @NonNull
    public static Executor getMainThreadExecutor() {
        return sMainThreadExecutor;
    }

    /**
     * Flush main thread calls if the main thread is not a looper thread.
     */
    public static void flushMainCalls() {
        //noinspection UnnecessaryLocalVariable
        final ConcurrentLinkedQueue<Runnable> queue = sMainCalls;
        Runnable r;
        while ((r = queue.poll()) != null) r.run();
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
                    looper = Looper.getMainLooper();
                } else {
                    looper = Looper.prepare();
                }
                sUiHandler = new Handler(looper);
                sUiHandlerAsync = Handler.createAsync(looper);

                if (sImmediateContext != null) {
                    sUiRecordingContext = sImmediateContext.makeRecordingContext();
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
            memFree(p);
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
        try {
            String s = uri.toString();
            String[] cmd = switch (Platform.get()) {
                case WINDOWS -> new String[]{"rundll32", "url.dll,FileProtocolHandler", s};
                case MACOSX -> new String[]{"open", s};
                default -> new String[]{"xdg-open", "file".equals(uri.getScheme())
                        ? s.replace("file:", "file://")
                        : s};
            };
            Process proc = Runtime.getRuntime().exec(cmd);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
                reader.lines().forEach(line -> LOGGER.error(MARKER, line));
            }
            return true;
        } catch (Exception e) {
            LOGGER.error(MARKER, "Failed to open URI: {}", uri, e);
            return false;
        }
    }

    /**
     * Launches the associated application to open the URI.
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
