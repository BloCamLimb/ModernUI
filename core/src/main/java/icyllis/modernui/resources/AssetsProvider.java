/*
 * Modern UI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
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
import icyllis.modernui.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;

/**
 * Interface responsible for opening and iterating through asset files.
 * <p>
 * A valid path must:
 * <ul>
 * <li>use only forward slashes as separators, not backslashes</li>
 * <li>contain only lowercase letters a to z, digits 0 to 9, dash (-), dot (.), underscore (_) in each segment</li>
 * <li>not start with or end with a slash</li>
 * <li>not use single dot (.) or dotdot (..) as a path segment</li>
 * <li>not contain consecutive slashes (//)</li>
 * </ul>
 *
 * @since 3.13
 */
public interface AssetsProvider extends AutoCloseable {

    /**
     * Try to locate an asset file, or null if not found.
     */
    @Nullable
    Asset getAsset(@NonNull String path);

    /**
     * @see Asset#openStream()
     */
    @NonNull
    default InputStream openStream(@NonNull String path) throws IOException {
        Asset asset = getAsset(path);
        if (asset == null) {
            throw new FileNotFoundException(path);
        }
        return asset.openStream();
    }

    /**
     * @see Asset#openChannel()
     */
    @NonNull
    default SeekableByteChannel openChannel(@NonNull String path) throws IOException {
        Asset asset = getAsset(path);
        if (asset == null) {
            throw new FileNotFoundException(path);
        }
        return asset.openChannel();
    }
}
