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
import java.lang.ref.Cleaner;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
     * @return the global cleaner shared within Modern UI
     */
    @Nonnull
    public static Cleaner cleaner() {
        return sCleaner;
    }

    public void warnSetup(String key, Object... args) {
        // pass
    }

    @Nonnull
    public Locale getSelectedLocale() {
        return Locale.getDefault();
    }
}
