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

import icyllis.modernui.annotation.NonNull;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.WillCloseWhenClosed;
import java.io.Closeable;
import java.io.IOException;

/**
 * Represents a loaded asset pack.
 * <p>
 * API users should use {@link ResourcesProvider} instead.
 *
 * @hide
 * @hidden
 */
@ApiStatus.Internal
public final class AssetPack implements Closeable {

    private final AssetProvider assets;

    private final ResourceMap resources;

    public AssetPack(@NonNull @WillCloseWhenClosed AssetProvider assets,
                     @NonNull ResourceMap resources) {
        this.assets = assets;
        this.resources = resources;
    }

    public AssetProvider getAssets() {
        return assets;
    }

    public ResourceMap getResources() {
        return resources;
    }

    @Override
    public void close() throws IOException {
        assets.close();
    }
}
