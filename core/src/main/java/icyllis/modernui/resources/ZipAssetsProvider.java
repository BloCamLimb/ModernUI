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
import org.jetbrains.annotations.ApiStatus;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * An {@link AssetsProvider} that uses {@link ZipFile} to access entries within
 * a zip archive. Zip entries can be STORED or DEFLATED, but certain assets that
 * require seeking must be STORED.
 *
 * @hide
 * @hidden
 */
@ApiStatus.Internal
public class ZipAssetsProvider implements AssetsProvider {

    private final ZipFile zipFile;

    /**
     * Will close the zip file when this provider is closed.
     */
    public ZipAssetsProvider(@NonNull ZipFile zipFile) {
        this.zipFile = zipFile;
    }

    @Nullable
    @Override
    public Asset getAsset(@NonNull String path) {
        if (path.isEmpty()) {
            return null;
        }
        ZipEntry zipEntry = zipFile.getEntry(path);
        if (zipEntry == null || zipEntry.isDirectory()) {
            return null;
        }
        return new ZipAsset(zipFile, zipEntry);
    }

    @Override
    public void close() throws Exception {
        zipFile.close();
    }
}
