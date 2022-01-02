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

package icyllis.modernui;

import icyllis.modernui.core.Handler;
import icyllis.modernui.core.Looper;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.view.ViewManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The core class of Modern UI.
 */
public class ModernUI {

    public static final String ID = "modernui"; // as well as the namespace
    public static final String NAME_CPT = "ModernUI";

    public static final Logger LOGGER = LogManager.getLogger(NAME_CPT);
    public static final Marker MARKER = MarkerManager.getMarker("Core");

    protected static volatile ModernUI sInstance;

    private static final Cleaner sCleaner = Cleaner.create();

    static {
        if (Runtime.version().feature() < 17) {
            throw new RuntimeException("JRE 17 or above is required");
        }
    }

    protected final Path mAssetsDir = Path.of(String.valueOf(System.getenv("APP_ASSETS")));

    private final Object mLock = new Object();

    private volatile Handler mMainHandler;

    protected volatile ExecutorService mBackgroundExecutor;
    protected volatile ViewManager mViewManager;

    public ModernUI() {
        synchronized (ModernUI.class) {
            if (sInstance == null) {
                sInstance = this;
            }
        }
    }

    /**
     * Initializes the Modern UI with the default application setups.
     *
     * @return the Modern UI
     */
    public static ModernUI initialize() {
        ModernUI it = new ModernUI();
        it.mBackgroundExecutor = Executors.newWorkStealingPool();
        return sInstance;
    }

    /**
     * Gets Modern UI instance.
     *
     * @return the Modern UI
     */
    public static ModernUI getInstance() {
        return sInstance;
    }

    /**
     * Registers a target and a cleaning action to run when the target becomes phantom
     * reachable. It will be registered with the global cleaner shared across Modern UI.
     * The action object should never hold any reference of the target object.
     *
     * @param target the target to monitor
     * @param action a {@code Runnable} to invoke when the target becomes phantom reachable
     * @return a {@code Cleanable} instance for explicit cleaning
     */
    @Nonnull
    public static Cleanable registerCleanup(@Nonnull Object target, @Nonnull Runnable action) {
        return sCleaner.register(target, action);
    }

    /**
     * Get the preferred locale set by user.
     *
     * @return selected locale
     */
    @Nonnull
    public Locale getSelectedLocale() {
        return Locale.getDefault();
    }

    /**
     * Get the preferred typeface set by user.
     *
     * @return selected typeface
     */
    @Nonnull
    public Typeface getSelectedTypeface() {
        return Typeface.INTERNAL;
    }

    /**
     * Whether to enable RTL support, it should always be true.
     *
     * @return whether RTL is supported
     */
    public boolean hasRtlSupport() {
        return true;
    }

    @Nonnull
    public Handler getMainHandler() {
        if (mMainHandler == null) {
            synchronized (mLock) {
                if (mMainHandler == null) {
                    mMainHandler = Handler.createAsync(Looper.getMainLooper());
                }
            }
        }
        return mMainHandler;
    }

    @Nonnull
    public InputStream getResourceAsStream(@Nonnull String namespace, @Nonnull String path) throws IOException {
        return new FileInputStream(mAssetsDir.resolve(namespace).resolve(path).toFile());
    }

    @Nonnull
    public ReadableByteChannel getResourceAsChannel(@Nonnull String namespace, @Nonnull String path) throws IOException {
        return FileChannel.open(mAssetsDir.resolve(namespace).resolve(path), StandardOpenOption.READ);
    }

    /**
     * Get the executor for background tasks. This should be a ForkJoinPool in asyncMode.
     *
     * @return background executor
     */
    @Nonnull
    public Executor getBackgroundExecutor() {
        return mBackgroundExecutor;
    }

    /**
     * Get the view manager of the application window.
     *
     * @return window view manager
     */
    @Nonnull
    public ViewManager getViewManager() {
        return mViewManager;
    }

    public void checkUiThread() {
    }

    /**
     * Post a task that will run on UI thread later.
     *
     * @param action runnable task
     * @return if successful
     */
    public boolean postOnUiThread(@Nonnull Runnable action) {
        return false;
    }

    /**
     * Post a task that will run on UI thread in specified milliseconds.
     *
     * @param action      runnable task
     * @param delayMillis delayed time to run the task in milliseconds
     * @return if successful
     */
    public boolean postOnUiThread(@Nonnull Runnable action, long delayMillis) {
        return false;
    }
}
