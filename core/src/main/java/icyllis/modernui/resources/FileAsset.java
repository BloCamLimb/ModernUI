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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @hide
 * @hidden
 */
@ApiStatus.Internal
public class FileAsset implements Asset {

    private static final OpenOption[] OPEN_OPTIONS = {StandardOpenOption.READ};

    private final Path path;
    private final BasicFileAttributes attributes;

    /**
     * Caller should check isRegularFile.
     */
    public FileAsset(@NonNull Path path, @NonNull BasicFileAttributes attributes) {
        assert attributes.isRegularFile();
        this.path = path;
        this.attributes = attributes;
    }

    @NonNull
    @Override
    public InputStream openStream() throws IOException {
        try {
            File file = path.toFile();
            // if the Path is a File, open FileInputStream so that we can support seek
            // or obtain the FileChannel through getChannel()
            return new FileInputStream(file);
        } catch (RuntimeException ignored) {
            // not a file, fallback to general case
        }
        return Files.newInputStream(path, OPEN_OPTIONS);
    }

    @NonNull
    @Override
    public SeekableByteChannel openChannel() throws IOException {
        if ("jar".equalsIgnoreCase(path.getFileSystem().provider().getScheme())) {
            // if path is ZipFS, open a stream because it does not support seek natively
            var is = Files.newInputStream(path, OPEN_OPTIONS);
            // we know that STORED entry is seekable, test it
            if (IOUtil.testSeekable(is) < -1) {
                return new SeekableInputStreamByteChannel(is, 0, attributes.size());
            }
            try {
                is.close();
            } catch (IOException ignored) {
            }
            throw new FileNotFoundException(path + " cannot be opened as a seekable byte channel, " +
                    "because it is compressed");
        }
        return Files.newByteChannel(path, OPEN_OPTIONS);
    }

    @Override
    public long getSize() {
        return attributes.size();
    }

    @NonNull
    public Path getPath() {
        return path;
    }

    @NonNull
    public BasicFileAttributes getAttributes() {
        return attributes;
    }
}
