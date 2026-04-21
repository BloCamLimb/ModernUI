/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.resources;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Core;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.WillCloseWhenClosed;
import java.lang.ref.Cleaner;

/**
 * Represents a loaded asset pack.
 * <p>
 * API users should use {@link ResourcesProvider} instead.
 *
 * @hide
 * @hidden
 */
@ApiStatus.Internal
public final class PackAssets implements AutoCloseable {

    private final AssetsProvider assetsProvider;

    private final LoadedResources loadedResources;

    private final Cleaner.Cleanable cleanup;

    public PackAssets(@NonNull @WillCloseWhenClosed AssetsProvider assetsProvider,
                      @NonNull LoadedResources loadedResources) {
        this.assetsProvider = assetsProvider;
        this.loadedResources = loadedResources;
        cleanup = Core.registerNativeResource(this, assetsProvider);
    }

    public AssetsProvider getAssetsProvider() {
        return assetsProvider;
    }

    public LoadedResources getLoadedResources() {
        return loadedResources;
    }

    @Override
    public void close() {
        cleanup.clean();
    }
}
