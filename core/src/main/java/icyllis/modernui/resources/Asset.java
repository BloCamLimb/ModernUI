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

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;

/**
 * Represents an asset file.
 *
 * @since 3.13
 */
public interface Asset {

    /**
     * Opens a stream for reading.
     * <p>
     * The returned InputStream should support <em>seek</em> as much as possible.
     * If the asset represents a file on the OS file system, then the returned stream
     * should be a {@link java.io.FileInputStream}.
     * <p>
     * The caller must close the returned InputStream after use.
     *
     * @return an input stream that reads this asset
     * @throws IOException I/O exception occurred
     */
    @NonNull
    InputStream openStream() throws IOException;

    /**
     * Opens a channel for reading.
     * <p>
     * There is no guarantee whether the channel is backed by off-heap or on-heap memory,
     * nor whether the returned channel implements {@link java.nio.channels.SeekableByteChannel}.
     * Typically, if the asset represents a file on the OS file system, a {@link java.nio.channels.FileChannel}
     * is returned. In other cases, it is recommended to read via InputStream instead.
     * <p>
     * The caller must close the returned ReadableByteChannel after use.
     *
     * @return a byte channel that reads this asset
     * @throws IOException I/O exception occurred
     */
    @NonNull
    ReadableByteChannel openChannel() throws IOException;

    /**
     * Returns the size of the file, in bytes.
     * <p>
     * For compressed files, this is the uncompressed size, not its actual size on the file system.
     * <p>
     * May return 0 or -1 if unknown or there is an error.
     *
     * @return the file size, in bytes
     */
    long getSize();
}
