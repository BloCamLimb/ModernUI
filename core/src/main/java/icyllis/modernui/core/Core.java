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
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.net.URI;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.concurrent.Executor;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * The core class for thread management and sub-system initializing, also provides utility methods of
 * memory operations and thread scheduling.
 *
 * @deprecated use {@link Arch}
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
