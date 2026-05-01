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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @hide
 * @hidden
 */
@ApiStatus.Internal
public final class IOUtil {

    /**
     * Used to determine whether the given {@link InputStream} is seekable.
     * Seekable means that {@link InputStream#skip} accepts negative numbers.
     * Non-seekable means it can only skip forwards, not skip backwards.
     * <p>
     * If seekable, the stream's position remains unchanged, and returns {@code -128}.
     * If EOF is reached, returns {@code -1}. If not seekable, returns the byte read.
     * <p>
     * Since Java 17, the following InputStream are seekable:
     * <ul>
     *     <li>{@link FileInputStream} (excluding its subclasses)</li>
     *     <li>{@code ZipFileInputStream} (from ZipFile, if entry is STORED)</li>
     *     <li>{@code EntryInputStream} (from ZipFileSystem, if entry is STORED)</li>
     *     <li>{@code ChannelInputStream} (if wraps SeekableByteChannel, may be returned by
     *     Channels.newInputStream and Files.newInputStream)</li>
     * </ul>
     *
     * For operations that require seek, this method can be used to determine whether
     * the entire stream needs to be read into memory to perform a seek.
     * <p>
     * You might ask why not just use Files.newByteChannel to get a SeekableByteChannel?
     * The reason is that for non-default FileSystem, its implementation may not be good enough.
     * For example, ZipFileSystem will read all the data into memory and return a ByteArrayChannel,
     * while the purpose of this method is to avoid reading all the data into memory.
     *
     * @param stream the input stream to test
     * @return -128 (seekable), -1 (EOF), or the byte read (non-seekable)
     * @throws IOException some errors occurred while reading
     */
    @SuppressWarnings("ConstantValue")
    public static int testSeekable(@NonNull InputStream stream) throws IOException {
        if (stream.getClass() == FileInputStream.class) {
            // seekable for regular files, but subclasses do not support seek
            return -128;
        }
        int next = stream.read();
        if (next < 0) {
            // EOF
            return -1;
        }
        // Since we got one byte, try to unget the byte.
        // Non-seekable streams may return 0 or throw an exception.
        // For example, InflaterInputStream throws IAE for negative inputs.
        try {
            if (stream.skip(-1) == -1) {
                return -128; // seekable
            }
        } catch (Exception ignored) {
            // non-seekable for IOException and any RuntimeException
        }
        return next;
    }

    @Nullable
    public static List<String> decomposePath(@NonNull String path) {
        List<String> result = new ArrayList<>();

        int start = 0, end;
        boolean stop = false;

        for (;;) {
            end = path.indexOf('/', start);
            if (end == -1) {
                end = path.length();
                stop = true;
            }

            String segment = path.substring(start, end);
            if (!isValidPathSegment(segment)) {
                return null;
            }

            result.add(segment);

            if (stop) {
                return result;
            }

            start = end + 1;
        }
    }

    @NonNull
    public static Path resolvePath(@NonNull Path base, @NonNull List<String> segments) {
        int size = segments.size();
        switch (size) {
            case 0:
                return base;
            case 1:
                return base.resolve(segments.get(0));
            default:
                String[] more = new String[size - 1];
                for (int i = 1; i < size; i++) {
                    more[i - 1] = segments.get(i);
                }
                return base.resolve(base.getFileSystem().getPath(segments.get(0), more));
        }
    }

    public static boolean isValidPathSegment(@NonNull String segment) {
        if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
            return false;
        }
        for (int i = 0, end = segment.length(); i < end; i++) {
            if (!isValidPathSegmentCharacter(segment.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidPathSegmentCharacter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '.' || c == '_';
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> RuntimeException sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    private IOUtil() {
    }
}
