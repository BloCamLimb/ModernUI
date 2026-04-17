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

package icyllis.modernui.util;

import icyllis.modernui.annotation.NonNull;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;

/**
 * A {@link SeekableByteChannel} implementation that wraps a seekable {@link InputStream}.
 * Seekable means that {@link InputStream#skip} accepts any N for {@link #position(long)},
 * so it can skip forwards, and skip backwards if needed.
 * <p>
 * This class is specifically designed to avoid buffering data into memory.
 * If the {@link InputStream} is not seekable, you should avoid using the
 * {@link SeekableByteChannel} abstraction altogether. Instead, read the stream into
 * a byte array or foreign memory and operate on that data directly.
 * <ul>
 * <li>This is optimized for reading into heap arrays</li>
 * <li>This is thread-safe by Channel requirements.</li>
 * <li>This works in blocking mode only, and blocks until dst.remaining() bytes read.</li>
 * </ul>
 * <p>
 * Since Java 17, the following InputStream are seekable (allowing skip backwards):
 * <ul>
 *     <li>{@code FileInputStream} (but not every subclass)</li>
 *     <li>{@code ZipFileInputStream} (from ZipFile, if entry is STORED)</li>
 *     <li>{@code EntryInputStream} (from ZipFileSystem, if entry is STORED)</li>
 *     <li>{@code ChannelInputStream} (if wraps SeekableByteChannel, may be returned by
 *     Channels.newInputStream and Files.newInputStream)</li>
 * </ul>
 * For FileInputStream, call FileInputStream.getChannel() may be preferred.
 *
 * @hide
 * @hidden
 */
@ApiStatus.Internal
public class SeekableInputStreamChannel implements SeekableByteChannel {

    private static final int BUFFER_SIZE = 8192;

    private final Object lock = new Object();

    private final InputStream in;

    // temp buf when read() dst is a direct buffer
    @GuardedBy("lock")
    private byte[] buf;

    @GuardedBy("lock")
    private long pos;
    private final long size;

    private volatile boolean closed;

    /**
     * Will close the input stream when this channel is closed.
     */
    public SeekableInputStreamChannel(@NonNull InputStream in, long pos, long size) {
        Objects.requireNonNull(in);
        Objects.checkIndex(pos, size);
        this.pos = pos;
        this.size = size;
        this.in = in;
    }

    @Override
    public int read(@NonNull ByteBuffer dst) throws IOException {
        synchronized (lock) {
            ensureOpen();

            if (dst.isReadOnly())
                throw new IllegalArgumentException("buffer is read-only");

            if (pos >= size)
                return -1;
            int len = (int) Math.min(dst.remaining(), size - pos);
            int read = 0;
            int n = 0;

            if (dst.hasArray()) {
                // directly read into the array
                while (read < len) {
                    int request = Math.min(len - read, BUFFER_SIZE);
                    n = in.read(dst.array(), dst.arrayOffset() + dst.position(), request);
                    if (n < 0)
                        break;
                    read += n;
                    pos += n;
                    dst.position(dst.position() + n);
                }
            } else {
                while (read < len) {
                    int request = Math.min(len - read, BUFFER_SIZE);
                    if (buf == null || buf.length < request)
                        buf = new byte[request];
                    n = in.read(buf, 0, request);
                    if (n < 0)
                        break;
                    read += n;
                    pos += n;
                    dst.put(buf, 0, n);
                }
            }
            if (n < 0 && read == 0)
                return -1;

            return read;
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throw new NonWritableChannelException();
    }

    @Override
    public long position() throws IOException {
        synchronized (lock) {
            ensureOpen();
            return pos;
        }
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        if (newPosition < 0)
            throw new IllegalArgumentException("Negative position " + newPosition);

        newPosition = Math.min(newPosition, size);

        synchronized (lock) {
            ensureOpen();
            long n = newPosition - pos;
            if (n != 0) {
                // if the input stream is seekable, then a single skip call can skip n exactly
                // without a loop, for both positive n and negative n
                long skipped = in.skip(n);
                pos += skipped;
                if (skipped != n)
                    throw new IOException("Cannot seek to position " + newPosition);
            }
            return this;
        }
    }

    @Override
    public long size() throws IOException {
        ensureOpen();
        return size;
    }

    @Override
    public SeekableByteChannel truncate(long size) {
        throw new NonWritableChannelException();
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;
        // double-checked Lock
        synchronized (lock) {
            if (closed)
                return;
            closed = true;
            buf = null;
            in.close();
        }
    }

    private void ensureOpen() throws IOException {
        if (closed)
            throw new ClosedChannelException();
    }
}
