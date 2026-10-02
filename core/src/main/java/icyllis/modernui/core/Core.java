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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.system.Arch;
import icyllis.modernui.system.SystemClock;

import java.lang.ref.Cleaner;
import java.net.URI;
import java.util.concurrent.Executor;

/**
 * The core class for thread management and sub-system initializing, also provides utility methods of
 * memory operations and thread scheduling.
 *
 * @deprecated use {@link Arch} and {@link SystemClock} since 3.14
 */
@Deprecated
public final class Core {

    private Core() {
    }

    @NonNull
    public static Cleaner.Cleanable registerCleanup(@NonNull Object target, @NonNull Runnable action) {
        return Arch.registerCleanup(target, action);
    }

    /**
     * Ensures that the current thread is the main thread, otherwise a runtime exception will be thrown.
     */
    public static void checkMainThread() {
        Arch.checkMainThread();
    }

    /**
     * @return the main thread if initialized, or null
     */
    public static Thread getMainThread() {
        return Arch.getMainThread();
    }

    /**
     * @return whether the current thread is the main thread
     */
    public static boolean isOnMainThread() {
        return Arch.isOnMainThread();
    }

    /**
     * Post an async operation that will be executed on main thread.
     *
     * @param r the runnable
     */
    public static void postOnMainThread(@NonNull Runnable r) {
        Arch.postOnMainThread(r);
    }

    /**
     * This should be rarely used. Only when the render thread and the main thread are the same thread,
     * and you need to call some methods that must be called on the main thread.
     *
     * @param r the runnable
     */
    public static void executeOnMainThread(@NonNull Runnable r) {
        Arch.executeOnMainThread(r);
    }

    @NonNull
    public static Executor getMainThreadExecutor() {
        return Arch.getMainThreadExecutor();
    }

    /**
     * Ensures that the current thread is the UI thread, otherwise a runtime exception will be thrown.
     */
    public static void checkUiThread() {
        Arch.checkUiThread();
    }

    /**
     * @return the UI thread if initialized, or null
     */
    public static Thread getUiThread() {
        return Arch.getUiThread();
    }

    /**
     * @return whether the current thread is the UI thread
     */
    public static boolean isOnUiThread() {
        return Arch.isOnUiThread();
    }

    /**
     * Post an async operation that will be executed on main thread.
     *
     * @param r the runnable
     */
    public static void postOnUiThread(@NonNull Runnable r) {
        Arch.postOnUiThread(r);
    }

    /**
     * This should be rarely used. Only when the render thread and the main thread are the same thread,
     * and you need to call some methods that must be called on the main thread.
     *
     * @param r the runnable
     */
    public static void executeOnUiThread(@NonNull Runnable r) {
        Arch.executeOnUiThread(r);
    }

    @NonNull
    public static Executor getUiThreadExecutor() {
        return Arch.getUiThreadExecutor();
    }

    public static long timeNanos() {
        return SystemClock.uptimeNanos();
    }

    public static long timeMillis() {
        return SystemClock.uptimeMillis();
    }

    /**
     * Launches the associated application to open the URI.
     *
     * @return true on success, false on failure
     */
    public static boolean openURI(@NonNull URI uri) {
        return Arch.openURI(uri);
    }

    /**
     * Launches the associated application to open the URI.
     *
     * @return true on success, false on failure
     */
    public static boolean openURI(@NonNull String uri) {
        return Arch.openURI(uri);
    }
}
