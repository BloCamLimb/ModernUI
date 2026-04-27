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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;

/**
 * Represents an asset file.
 *
 * @since 3.13
 */
public interface Asset {

    /**
     * Opens a stream for reading.
     * <p>
     * The returned InputStream may support <em>seek</em> (skip forwards and backwards).
     * If the asset represents a file on the OS file system, then the returned stream
     * should be a {@link java.io.FileInputStream}.
     * <p>
     * The caller must close the returned InputStream after use.
     *
     * @return an input stream that reads this asset
     * @throws IOException           I/O exception occurred
     * @throws IllegalStateException associated zip file or file system is closed
     */
    @NonNull
    InputStream openStream() throws IOException;

    /**
     * Opens a byte channel for reading.
     * <p>
     * This returns a SeekableByteChannel only if the asset is seekable (uncompressed).
     * Otherwise (e.g., for compressed assets), a {@link java.util.zip.ZipException} is thrown.
     * <p>
     * This method is designed to avoid internal memory buffering. If the caller requires seek
     * functionality for non-seekable assets, they should read the InputStream into a byte array
     * or foreign memory and operate on it directly, rather than using a SeekableByteChannel abstraction.
     * <p>
     * This method may return a {@link java.nio.channels.FileChannel} directly if available.
     * There is no guarantee whether the channel is backed by off-heap or on-heap memory,
     * <p>
     * The caller must close the returned SeekableByteChannel after use.
     *
     * @return a byte channel that reads this asset
     * @throws IOException           I/O exception occurred
     * @throws IllegalStateException associated zip file or file system is closed
     * @see #isCompressed()
     */
    @NonNull
    SeekableByteChannel openChannel() throws IOException;

    /**
     * Returns the size of the file, in bytes.
     * <p>
     * For compressed files, this is the uncompressed size, not its actual size on the file system.
     * <p>
     * May return 0 or -1 if unknown or there is an error.
     *
     * @return the file size, in bytes
     * @see #isCompressed()
     */
    long getSize();

    /**
     * Returns whether the asset is compressed.
     *
     * @return true if compressed, false if uncompressed
     * @see #getSize()
     * @see #openChannel()
     */
    boolean isCompressed();

    /**
     * Returns a {@link File} object representing this asset.
     * <p>
     * This method returns a non-null value only if the asset represents a file on
     * the <em>default file system</em>. For other cases, such as ZIP entries or
     * virtual file systems, this returns {@code null}.
     *
     * @return a {@code File} object representing this asset, null otherwise
     */
    @Nullable
    File toFile();
}
