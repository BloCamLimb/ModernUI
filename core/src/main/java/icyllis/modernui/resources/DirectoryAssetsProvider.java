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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * An {@link AssetsProvider} that accesses entries under a root {@link Path}.
 * <p>
 * Although any {@link FileSystem} may be used, this provider is primarily intended
 * for directories in the default file system ({@link FileSystems#getDefault})
 * or other {@link java.io.File}-based file systems (i.e. where {@link Path#toFile()}
 * is supported). For zip file systems, {@link ZipAssetsProvider} typically offers
 * better performance.
 *
 * @hide
 * @hidden
 */
@ApiStatus.Internal
public class DirectoryAssetsProvider implements AssetsProvider {

    // follow links
    private static final LinkOption[] LINK_OPTIONS = {};

    private final Path root;

    /**
     * Caller should check isDirectory.
     */
    public DirectoryAssetsProvider(@NonNull Path root) {
        this.root = root;
    }

    @Nullable
    @Override
    public Asset getAsset(@NonNull String path) {
        List<String> segments = IOUtil.decomposePath(path);
        if (segments == null) {
            // invalid path string
            return null;
        }

        Path assetPath = IOUtil.resolvePath(root, segments);
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    assetPath, BasicFileAttributes.class, LINK_OPTIONS);

            if (!attributes.isRegularFile()) {
                return null;
            }

            return new FileAsset(assetPath, attributes);
        } catch (IOException e) {
            // does not exist
            return null;
        }
    }

    @Override
    public void close() {
    }

    /**
     * @return the directory (root path of all assets)
     */
    @NonNull
    public Path getRoot() {
        return root;
    }
}
