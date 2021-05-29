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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nonnull;
import java.awt.*;
import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * The core class of the client side of Modern UI
 */
public class ModernUI {

    public static final String ID = "modernui"; // as well as the namespace
    public static final String NAME_CPT = "ModernUI";

    public static final Logger LOGGER = LogManager.getLogger(NAME_CPT);
    public static final Marker MARKER = MarkerManager.getMarker("Core");

    protected static ModernUI sInstance;

    private static final Cleaner sCleaner = Cleaner.create();

    private final ExecutorService mLoaderPool;

    public ModernUI() {
        sInstance = this;
        mLoaderPool = Executors.newSingleThreadExecutor(target -> new Thread(target, "mui-loading-core"));
    }

    public static ModernUI get() {
        return sInstance;
    }

    /**
     * Registers a target and a cleaning action to run when the target becomes phantom
     * reachable. It will be registered with the global cleaner shared across Modern UI.
     * The action object should never hold any reference of the target object.
     *
     * @param target the target to monitor
     * @param action a {@code Runnable} to invoke when the target becomes phantom reachable
     * @return a {@code Cleanable} instance
     */
    @Nonnull
    public static Cleanable registerCleanup(@Nonnull Object target, @Nonnull Runnable action) {
        return sCleaner.register(target, action);
    }

    public void warnSetup(String key, Object... args) {
        // pass
    }

    @Nonnull
    public Locale getSelectedLocale() {
        return Locale.getDefault();
    }

    public void loadFont(String rl, Consumer<Font> setter) {

    }
}
