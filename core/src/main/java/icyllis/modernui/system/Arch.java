/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
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

package icyllis.modernui.system;

import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.core.RefCounted;
import icyllis.arc3d.engine.ImmediateContext;
import icyllis.arc3d.granite.RecordingContext;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.util.Log;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLMisc;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.ref.Cleaner;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Global registry and access point for system-level execution architecture and
 * hardware facilities.
 * <p>
 * This class serves as a static information holder for core runtime primitives,
 * including threads, event loops, executors, GPU contexts, and cleaners.
 * <p>
 * Note: This class is purely passive and performs no initialization or lifecycle
 * management. External launchers (whether embedded hosts or standalone runners)
 * are responsible for instantiating these facilities and registering them here
 * during startup.
 *
 * @since 3.14
 */
public final class Arch {

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static final Marker MARKER = MarkerFactory.getMarker("Arch");

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

    private static final Executor sMainThreadExecutor = Arch::postOnMainThread;
    private static final Executor sUiThreadExecutor = Arch::postOnUiThread;

    private static volatile ImmediateContext sImmediateContext;
    private static volatile RecordingContext sUiRecordingContext;

    private Arch() {}

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
     * Returns a common cleaner used by framework.
     */
    @NonNull
    public static Cleaner cleaner() {
        return sCleaner;
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static void setMainThread() {
        sMainThread = Thread.currentThread();
        sMainLooper = Looper.myLooper();
        sMainHandler = new Handler(sMainLooper);
        sMainHandlerAsync = Handler.createAsync(sMainLooper);
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

    /**
     * Returns the shared {@link Handler} that created on main thread, if initialized.
     * It can be used for thread scheduling of callback operations.
     *
     * @return the shared main handler
     * @see #getMainHandlerAsync()
     */
    public static Handler getMainHandler() {
        return sMainHandler;
    }

    /**
     * Returns the shared {@link Handler} that created on main thread, if initialized.
     * It can be used for thread scheduling of callback operations.
     * <p>
     * Differently from {@link #getMainHandler()}, this is an async version.
     * Messages sent to an async handler are guaranteed to be ordered with respect to one another,
     * but not necessarily with respect to messages from other Handlers.
     *
     * @return the shared main handler
     * @see #getMainHandler()
     */
    public static Handler getMainHandlerAsync() {
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

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static void setRenderThread(@RawPtr ImmediateContext immediateContext) {
        synchronized (Arch.class) {
            sRenderThread = Thread.currentThread();
            sImmediateContext = immediateContext;
        }
    }

    /**
     * Ensures that the current thread is the render thread, otherwise a runtime exception will be thrown.
     */
    public static void checkRenderThread() {
        if (Thread.currentThread() != sRenderThread)
            synchronized (Arch.class) {
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
     * @hidden
     */
    @ApiStatus.Internal
    public static void setUiThread(@RawPtr RecordingContext uiRecordingContext) {
        synchronized (Arch.class) {
            sUiThread = Thread.currentThread();
            if (sUiThread == sMainThread) {
                sUiLooper = sMainLooper;
                sUiHandler = sMainHandler;
                sUiHandlerAsync = sMainHandlerAsync;
            } else {
                sUiLooper = Looper.myLooper();
                sUiHandler = new Handler(sUiLooper);
                sUiHandlerAsync = Handler.createAsync(sUiLooper);
            }
            sUiRecordingContext = uiRecordingContext;
        }
    }

    /**
     * Ensures that the current thread is the UI thread, otherwise a runtime exception will be thrown.
     */
    public static void checkUiThread() {
        if (Thread.currentThread() != sUiThread)
            synchronized (Arch.class) {
                if (sUiThread == null)
                    throw new IllegalStateException("The UI thread has not been initialized yet.");
                else
                    throw new IllegalStateException("Not called from the UI thread " + sUiThread +
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
     * Launches the associated application to open the URI.
     *
     * @return true on success, false on failure
     */
    public static boolean openURI(@NonNull URI uri) {
        String s = uri.toString();
        if (!SDLMisc.SDL_OpenURL(s)) {
            Log.LOGGER.error(MARKER, "Failed to open URI {}, error: {}", uri, SDLError.SDL_GetError());
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
            Log.LOGGER.error(MARKER, "Failed to open URI {}", uri, e);
            return false;
        }
    }
}
