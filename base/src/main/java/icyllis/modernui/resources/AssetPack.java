/*
 * ModernUI.
 * Copyright (C) 2025-2026 BloCamLimb. All rights reserved.
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

package icyllis.modernui.resources;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

import java.io.Closeable;
import java.io.IOException;

/**
 * Represents a loaded asset pack.
 */
public final class AssetPack implements Closeable {

    private final AssetProvider assets;

    private final ResourceMap resources;

    // cache of GPU textures or CPU bitmaps
    private final ImageCache imageCache = new ImageCache();

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public AssetPack(@NonNull AssetProvider assets, // will close
                     @NonNull ResourceMap resources) {
        this.assets = assets;
        this.resources = resources;
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public AssetProvider getAssets() {
        return assets;
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public ResourceMap getResources() {
        return resources;
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public ImageCache getImageCache() {
        return imageCache;
    }

    @Override
    public void close() throws IOException {
        assets.close();
        imageCache.close();
    }
}
