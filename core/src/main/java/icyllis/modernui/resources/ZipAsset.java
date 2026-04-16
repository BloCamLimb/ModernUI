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
import icyllis.modernui.util.SeekableInputStreamByteChannel;
import org.jetbrains.annotations.ApiStatus;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @hide
 * @hidden
 */
@ApiStatus.Internal
public class ZipAsset implements Asset {

    private final ZipFile zipFile;
    private final ZipEntry zipEntry;

    /**
     * This will not close the ZipFile. Caller should check !isDirectory.
     */
    public ZipAsset(@NonNull ZipFile zipFile, @NonNull ZipEntry zipEntry) {
        assert !zipEntry.isDirectory();
        this.zipFile = zipFile;
        this.zipEntry = zipEntry;
    }

    @NonNull
    @Override
    public InputStream openStream() throws IOException {
        var is = zipFile.getInputStream(zipEntry);
        if (is == null) {
            throw new FileNotFoundException(zipEntry.getName());
        }
        return is;
    }

    @NonNull
    @Override
    public SeekableByteChannel openChannel() throws IOException {
        // we know that STORED entry is seekable
        if (zipEntry.getMethod() == ZipEntry.STORED) {
            return new SeekableInputStreamByteChannel(openStream(), 0, zipEntry.getSize());
        }
        throw new FileNotFoundException(zipEntry.getName() + " cannot be opened as a seekable byte channel, " +
                "because it is compressed");
    }

    @Override
    public long getSize() {
        return zipEntry.getSize();
    }

    @Override
    public boolean isCompressed() {
        return zipEntry.getMethod() != ZipEntry.STORED;
    }

    @NonNull
    public ZipEntry getZipEntry() {
        return zipEntry;
    }
}
