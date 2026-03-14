/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.core.ColorSpace;
import icyllis.arc3d.core.ImageInfo;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Core;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.stb.STBIEOFCallback;
import org.lwjgl.stb.STBIIOCallbacks;
import org.lwjgl.stb.STBIReadCallback;
import org.lwjgl.stb.STBISkipCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.jni.JNINativeInterface;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.function.Predicate;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * Creates {@link Bitmap Bitmaps} from encoded data.
 *
 * @since 3.7
 */
public final class BitmapFactory {

    /**
     * Collects the options for the decoder and additional outputs from the decoder.
     *
     * <p>Unlike Android (with its Skia graphics engine), it is not necessary to
     * pre-multiply alpha of image data in Modern UI framework. We allow images
     * to be directly drawn by the view system or through a {@link Canvas} either
     * pre-multiplied or non-pre-multiplied. Although pre-multiplied alpha can
     * help draw-time blending, but it results in precision loss since images
     * are 8-bit per channel in memory. Instead, Modern UI will pre-multiply alpha
     * in the shading pipeline.</p>
     */
    public static class Options {

        /**
         * If false, decode methods will always return a mutable Bitmap instead of
         * an immutable one. This can be used for instance to programmatically apply
         * effects to a Bitmap loaded through BitmapFactory.
         */
        public boolean inImmutable = true;

        /**
         * If set to true, the decoder will populate the mimetype of the decoded image.
         *
         * @see #outMimeType
         */
        public boolean inDecodeMimeType = false;

        /**
         * If this is non-null, the decoder will try to decode into this
         * internal format. If it is null, or the request cannot be met,
         * the decoder will try to pick the best matching format based on the
         * system's screen depth, and characteristics of the original image such
         * as if it has per-pixel alpha (requiring a format that also does).
         */
        public Bitmap.Format inPreferredFormat = null;

        /**
         * If this is non-null, the decoder will try to decode into this
         * color space. If it is null, or the request cannot be met,
         * the decoder will pick either the color space embedded in the image
         * or the color space best suited for the requested image format
         * (for instance {@link ColorSpace.Named#SRGB sRGB} for
         * {@link Bitmap.Format#RGBA_8888} format.</p>
         *
         * <p class="note">Only {@link ColorSpace.Model#RGB} color spaces are
         * currently supported. An <code>IllegalArgumentException</code> will
         * be thrown by the decode methods when setting a non-RGB color space
         * such as {@link ColorSpace.Named#CIE_LAB Lab}.</p>
         *
         * <p class="note">The specified color space's transfer function must be
         * an {@link ColorSpace.Rgb.TransferParameters ICC parametric curve}. An
         * <code>IllegalArgumentException</code> will be thrown by the decode methods
         * if calling {@link ColorSpace.Rgb#getTransferParameters()} on the
         * specified color space returns null.</p>
         *
         * <p>After decode, the bitmap's color space is stored in
         * {@link #outColorSpace}.</p>
         */
        public ColorSpace inPreferredColorSpace = null;

        /**
         * Temporary storage used for decoding from a stream.
         * The minimum size is 128; 8192 is recommended.
         */
        public byte[] inTempStorage = null;

        /**
         * The width of the bitmap. If there is an error, it is undefined.
         */
        public int outWidth;

        /**
         * The height of the bitmap. If there is an error, it is undefined.
         */
        public int outHeight;

        /**
         * If known, this string is set to the mimetype of the decoded image.
         * If not known, or there is an error, it is undefined.
         * <p>
         * Set only when {@link #inDecodeMimeType} is true.
         */
        public String outMimeType;

        /**
         * If known, the config the decoded bitmap will have.
         * If not known, or there is an error, it is undefined.
         */
        public Bitmap.Format outFormat;

        /**
         * If known, the color space the decoded bitmap will have. Note that the
         * output color space is not guaranteed to be the color space the bitmap
         * is encoded with. If not known, or there is an error, it is undefined.
         */
        public ColorSpace outColorSpace;
    }

    private static void validate(Options opts) {
        if (opts == null) return;

        if (opts.inPreferredColorSpace != null) {
            if (!(opts.inPreferredColorSpace instanceof ColorSpace.Rgb rgbColorSpace)) {
                throw new IllegalArgumentException("The destination color space must use the " +
                        "RGB color model");
            }
            if (rgbColorSpace.getTransferParameters() == null) {
                throw new IllegalArgumentException("The destination color space must use an " +
                        "ICC parametric transfer function");
            }
        }
    }

    /**
     * Decode a file path into a bitmap.
     * <p>
     * If the file cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway.
     *
     * @param file the file to be decoded
     * @return the decoded bitmap
     * @throws IOException the file cannot be decoded into a bitmap
     */
    @NonNull
    public static Bitmap decodeFile(@NonNull File file) throws IOException {
        return decodeFile(file, null);
    }

    /**
     * Decode a file path into a bitmap.
     * <p>
     * If the file cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway. If the specified color space is not {@link ColorSpace.Model#RGB RGB},
     * or if the specified color space's transfer function is not an
     * {@link ColorSpace.Rgb.TransferParameters ICC parametric curve}, the method throws
     * {@link IllegalArgumentException}.
     *
     * @param file the file to be decoded
     * @param opts the decoder options
     * @return the decoded bitmap
     * @throws IllegalArgumentException the options are invalid
     * @throws IOException              the file cannot be decoded into a bitmap
     */
    @NonNull
    public static Bitmap decodeFile(@NonNull File file,
                                    @Nullable Options opts) throws IOException {
        validate(opts);
        if (opts != null && opts.inDecodeMimeType) {
            decodeMimeType(opts, file);
        }
        final Bitmap bm;
        try (final var stream = new FileInputStream(file)) {
            bm = decodeSeekableChannel(stream.getChannel(), opts, false);
        }
        assert bm != null;
        return bm;
    }

    /**
     * Query the bitmap info from a file path, without allocating the memory for its pixels.
     * <p>
     * If the file cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway.
     *
     * @param file the file to be decoded
     * @param opts the out values
     * @throws IOException the file cannot be decoded into a bitmap
     */
    public static void decodeFileInfo(@NonNull File file,
                                      @NonNull Options opts) throws IOException {
        if (opts.inDecodeMimeType) {
            decodeMimeType(opts, file);
        }
        try (final var stream = new FileInputStream(file)) {
            decodeSeekableChannel(stream.getChannel(), opts, true);
        }
    }

    /**
     * Decode a file path into a bitmap.
     * <p>
     * If the file cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway.
     *
     * @param path the file to be decoded
     * @return the decoded bitmap
     * @throws IOException the file cannot be decoded into a bitmap
     */
    @NonNull
    public static Bitmap decodePath(@NonNull Path path) throws IOException {
        return decodePath(path, null);
    }

    /**
     * Decode a file path into a bitmap.
     * <p>
     * If the file cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway. If the specified color space is not {@link ColorSpace.Model#RGB RGB},
     * or if the specified color space's transfer function is not an
     * {@link ColorSpace.Rgb.TransferParameters ICC parametric curve}, the method throws
     * {@link IllegalArgumentException}.
     *
     * @param path the file to be decoded
     * @param opts the decoder options
     * @return the decoded bitmap
     * @throws IllegalArgumentException the options are invalid
     * @throws IOException              the file cannot be decoded into a bitmap
     */
    @NonNull
    public static Bitmap decodePath(@NonNull Path path,
                                    @Nullable Options opts) throws IOException {
        validate(opts);
        if (opts != null && opts.inDecodeMimeType) {
            decodeMimeType(opts, path.toFile());
        }
        try {
            // If the path represents a file on the OS default file system, then this is faster
            File file = path.toFile();
            return decodeFile(file, opts);
        } catch (RuntimeException ignored) {
        }
        final Bitmap bm;
        try (final var stream = Files.newInputStream(path, StandardOpenOption.READ)) {
            bm = decodeStream(stream, opts);
        }
        return bm;
    }

    /**
     * Query the bitmap info from a file path, without allocating the memory for its pixels.
     * <p>
     * If the file cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway.
     *
     * @param path the file to be decoded
     * @param opts the out values
     * @throws IOException the file cannot be decoded into a bitmap
     */
    public static void decodePathInfo(@NonNull Path path,
                                      @NonNull Options opts) throws IOException {
        if (opts.inDecodeMimeType) {
            decodeMimeType(opts, path.toFile());
        }
        try {
            // If the path represents a file on the OS default file system, then this is faster
            File file = path.toFile();
            decodeFileInfo(file, opts);
            return;
        } catch (RuntimeException ignored) {
        }
        try (final var stream = Files.newInputStream(path, StandardOpenOption.READ)) {
            decodeStreamInfo(stream, opts);
        }
    }

    /**
     * Decode an input stream into a bitmap.
     * <p>
     * The stream's position will be undefined after being called. This method
     * <em>does not</em> closed the given stream after read operation has completed.
     * <p>
     * If the stream cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway.
     *
     * @param stream the input stream to be decoded
     * @return the decoded bitmap
     * @throws IOException the data cannot be decoded into a bitmap
     */
    @NonNull
    public static Bitmap decodeStream(@NonNull InputStream stream) throws IOException {
        return decodeStream(stream, null);
    }

    /**
     * Decode an input stream into a bitmap.
     * <p>
     * The stream's position will be undefined after being called. This method
     * <em>does not</em> closed the given stream after read operation has completed.
     * <p>
     * If the stream cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway. If the specified color space is not {@link ColorSpace.Model#RGB RGB},
     * or if the specified color space's transfer function is not an
     * {@link ColorSpace.Rgb.TransferParameters ICC parametric curve}, the method throws
     * {@link IllegalArgumentException}.
     *
     * @param stream the input stream to be decoded
     * @param opts   the decoder options
     * @return the decoded bitmap
     * @throws IllegalArgumentException the options are invalid
     * @throws IOException              the data cannot be decoded into a bitmap
     */
    @NonNull
    public static Bitmap decodeStream(@NonNull InputStream stream,
                                      @Nullable Options opts) throws IOException {
        validate(opts);
        final Bitmap bm;
        if (stream.getClass() == FileInputStream.class) {
            FileChannel ch = ((FileInputStream) stream).getChannel();
            if (opts != null && opts.inDecodeMimeType) {
                long pos = ch.position();
                decodeMimeType(opts, stream); // no need to close
                ch.position(pos);
            }
            bm = decodeSeekableChannel(ch, opts, false);
        } else {
            //TODO decode mime type
            bm = decodeSeekableStream(stream, opts, false);
        }
        assert bm != null;
        return bm;
    }

    /**
     * Query the bitmap info from an input stream, without allocating the memory for its pixels.
     * <p>
     * If the stream cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway.
     *
     * @param stream the input stream to be decoded
     * @param opts   the out values
     * @throws IOException the data cannot be decoded into a bitmap
     */
    public static void decodeStreamInfo(@NonNull InputStream stream,
                                        @NonNull Options opts) throws IOException {
        if (stream.getClass() == FileInputStream.class) {
            FileChannel ch = ((FileInputStream) stream).getChannel();
            if (opts.inDecodeMimeType) {
                long pos = ch.position();
                decodeMimeType(opts, stream); // no need to close
                ch.position(pos);
            }
            decodeSeekableChannel(ch, opts, true);
        } else {
            //TODO decode mime type
            decodeSeekableStream(stream, opts, true);
        }
    }

    /**
     * Decode a readable channel into a bitmap.
     * <p>
     * The channel's position will be at the end of the encoded data. This method
     * <em>does not</em> closed the given channel after read operation has completed.
     * <p>
     * If the channel cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway.
     *
     * @param channel the readable channel to be decoded
     * @return the decoded bitmap
     * @throws IOException the data cannot be decoded into a bitmap
     */
    @NonNull
    public static Bitmap decodeChannel(@NonNull ReadableByteChannel channel) throws IOException {
        return decodeChannel(channel, null);
    }

    /**
     * Decode a readable channel into a bitmap.
     * <p>
     * The channel's position will be at the end of the encoded data. This method
     * <em>does not</em> closed the given channel after read operation has completed.
     * <p>
     * If the channel cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway. If the specified color space is not {@link ColorSpace.Model#RGB RGB},
     * or if the specified color space's transfer function is not an
     * {@link ColorSpace.Rgb.TransferParameters ICC parametric curve}, the method throws
     * {@link IllegalArgumentException}.
     *
     * @param channel the readable channel to be decoded
     * @param opts    the decoder options
     * @return the decoded bitmap
     * @throws IllegalArgumentException the options are invalid
     * @throws IOException              the data cannot be decoded into a bitmap
     */
    @NonNull
    public static Bitmap decodeChannel(@NonNull ReadableByteChannel channel,
                                       @Nullable Options opts) throws IOException {
        validate(opts);
        final Bitmap bm;
        if (channel instanceof SeekableByteChannel ch) {
            if (opts != null && opts.inDecodeMimeType) {
                long pos = ch.position();
                decodeMimeType(opts, Channels.newInputStream(channel)); // no need to close
                ch.position(pos);
            }
            bm = decodeSeekableChannel(ch, opts, false);
        } else {
            ByteBuffer p = null;
            try {
                p = Core.readIntoNativeBuffer(channel);
                bm = decodeBuffer(p.rewind(), opts);
            } finally {
                memFree((Buffer) p);
            }
        }
        assert bm != null;
        return bm;
    }

    /**
     * Query the bitmap info from a readable channel, without allocating the memory for its pixels.
     * <p>
     * If the channel cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway.
     *
     * @param channel the readable channel to be decoded
     * @param opts    the out values
     * @throws IOException the data cannot be decoded into a bitmap
     */
    public static void decodeChannelInfo(@NonNull ReadableByteChannel channel,
                                         @NonNull Options opts) throws IOException {
        if (channel instanceof SeekableByteChannel ch) {
            if (opts.inDecodeMimeType) {
                long pos = ch.position();
                decodeMimeType(opts, Channels.newInputStream(channel)); // no need to close
                ch.position(pos);
            }
            decodeSeekableChannel(ch, opts, true);
        } else {
            ByteBuffer p = null;
            try {
                p = Core.readIntoNativeBuffer(channel);
                decodeBufferInfo(p.rewind(), opts);
            } finally {
                memFree((Buffer) p);
            }
        }
    }

    /**
     * Decode an immutable bitmap from the specified byte array.
     * <p>
     * If the data cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway.
     *
     * @param data   byte array of compressed image data
     * @param offset offset into image data for where the decoder should begin parsing
     * @param length the number of bytes, beginning at offset, to parse
     * @return the decoded bitmap
     * @throws IOException the data cannot be decoded into a bitmap
     */
    @NonNull
    public static Bitmap decodeByteArray(byte[] data, int offset, int length) throws IOException {
        return Objects.requireNonNull(decodeByteArray(data, offset, length, null));
    }

    /**
     * Decode an immutable bitmap from the specified byte array.
     * <p>
     * If the data cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway. If the specified color space is not {@link ColorSpace.Model#RGB RGB},
     * or if the specified color space's transfer function is not an
     * {@link ColorSpace.Rgb.TransferParameters ICC parametric curve}, the method throws
     * {@link IllegalArgumentException}.
     *
     * @param data   byte array of compressed image data
     * @param offset offset into image data for where the decoder should begin parsing
     * @param length the number of bytes, beginning at offset, to parse
     * @param opts   the decoder options
     * @return the decoded bitmap
     * @throws IllegalArgumentException the options are invalid
     * @throws IOException              the data cannot be decoded into a bitmap
     */
    @NonNull
    public static Bitmap decodeByteArray(byte[] data, int offset, int length,
                                         @Nullable Options opts) throws IOException {
        if ((offset | length) < 0 || data.length < offset + length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        validate(opts);
        if (opts != null && opts.inDecodeMimeType) {
            decodeMimeType(opts, new ByteArrayInputStream(data, offset, length));  // no need to close (nop)
        }
        ByteBuffer p = null;
        final Bitmap bm;
        try {
            p = memAlloc(length);
            bm = decodeBuffer0(p
                    .put(data, offset, length)
                    .rewind(), opts);
        } finally {
            memFree((Buffer) p);
        }
        return bm;
    }

    @ApiStatus.Internal
    @NonNull
    public static Bitmap decodeBuffer(@NonNull ByteBuffer buffer,
                                      @Nullable Options opts) throws IOException {
        validate(opts);
        assert buffer.isDirect() && !buffer.isReadOnly();
        if (opts != null && opts.inDecodeMimeType) {
            // scan header for setting MIME type, 96 bytes is enough
            byte[] seek = new byte[Math.min(buffer.limit(), 96)];
            buffer.get(0, seek, 0, seek.length);
            decodeMimeType(opts, new ByteArrayInputStream(seek)); // no need to close (nop)
        }
        return decodeBuffer0(buffer, opts);
    }

    @ApiStatus.Internal
    public static void decodeBufferInfo(@NonNull ByteBuffer buffer,
                                        @NonNull Options opts) throws IOException {
        assert buffer.isDirect() && !buffer.isReadOnly();
        final long buf = memAddress(buffer);
        final int len = buffer.remaining();
        if (opts.inDecodeMimeType) {
            // scan header for setting MIME type, 96 bytes is enough
            byte[] seek = new byte[Math.min(len, 96)];
            buffer.get(buffer.position(), seek, 0, seek.length);
            decodeMimeType(opts, new ByteArrayInputStream(seek)); // no need to close (nop)
        }
        final boolean isU16, isHDR;
        isHDR = STBImage.nstbi_is_hdr_from_memory(buf, len) != 0;
        isU16 = !isHDR && STBImage.nstbi_is_16_bit_from_memory(buf, len) != 0;
        decodeInfo0(NULL, NULL, buf, len, opts, isU16, isHDR);
    }

    @NonNull
    private static Bitmap decodeBuffer0(@NonNull ByteBuffer buffer,
                                        @Nullable Options opts) throws IOException {
        final long buf = memAddress(buffer);
        final int len = buffer.remaining();
        final boolean isU16, isHDR;
        if (opts != null && opts.inPreferredFormat != null) {
            isU16 = opts.inPreferredFormat.isChannelU16();
            isHDR = opts.inPreferredFormat.isChannelHDR();
        } else {
            isHDR = STBImage.nstbi_is_hdr_from_memory(buf, len) != 0;
            isU16 = !isHDR && STBImage.nstbi_is_16_bit_from_memory(buf, len) != 0;
        }
        return decode0(NULL, NULL, buf, len, opts, isU16, isHDR);
    }

    static final class SeekableContext {
        static final int BUFFER_SIZE = 8192; // jdk convention

        boolean eof = false;
        IOException ioe = null;

        final SeekableByteChannel channel;

        final InputStream stream;
        final byte[] buffer;
        int pos, end;

        SeekableContext(SeekableByteChannel channel) {
            this.channel = channel;
            stream = null;
            buffer = null;
        }

        SeekableContext(InputStream stream, byte[] buffer) {
            this.stream = stream;
            this.buffer = buffer;
            channel = null;
        }
    }

    static final class ReadCallback extends STBIReadCallback {
        @Override
        public int invoke(long user, long data, int size) {
            SeekableContext ctx = memGlobalRefToObject(user);
            if (ctx.eof) {
                return 0;
            }
            int read = 0;
            if (ctx.channel != null) {
                // File channel can read directly into the native buffer without
                // going through the heap buffer. If it's not a file channel, then
                // this typically performs a copy from the heap to native.
                // Additionally, we require the channel to be in blocking mode.
                ByteBuffer dst = STBIReadCallback.getData(data, size);
                while (dst.hasRemaining()) {
                    try {
                        int n = ctx.channel.read(dst);
                        if (n <= 0) {
                            ctx.eof = true;
                            break;
                        }
                        read += n;
                    } catch (IOException e) {
                        ctx.ioe = e;
                        ctx.eof = true;
                        break;
                    }
                }
            } else {
                assert ctx.stream != null && ctx.buffer != null;
                while (read < size) {
                    int request = size - read;
                    // If we have pushback buffer, consume it first
                    if (ctx.pos < ctx.end) {
                        request = Math.min(request, ctx.end - ctx.pos);
                        JNINativeInterface.nGetByteArrayRegion(
                                ctx.buffer, ctx.pos, request, data + read
                        );
                        ctx.pos += request;
                        read += request;
                        continue;
                    }
                    // Otherwise read from stream
                    try {
                        int n = ctx.stream.read(ctx.buffer, 0, Math.min(request,
                                Math.min(ctx.buffer.length, SeekableContext.BUFFER_SIZE)));
                        if (n <= 0) {
                            ctx.eof = true;
                            break;
                        }
                        JNINativeInterface.nGetByteArrayRegion(
                                ctx.buffer, 0, n, data + read
                        );
                        ctx.pos = ctx.end = n;
                        read += n;
                    } catch (IOException e) {
                        ctx.ioe = e;
                        ctx.eof = true;
                        break;
                    }
                }
            }
            return read;
        }
    }

    // After reviewing the stb_image.h, I found that its skip function never
    // takes a negative n, therefore, any InputStream can work with it.
    static final class SkipCallback extends STBISkipCallback {
        @Override
        public void invoke(long user, int n) {
            SeekableContext ctx = memGlobalRefToObject(user);
            if (ctx.eof) {
                return;
            }
            if (ctx.channel != null) {
                try {
                    ctx.channel.position(ctx.channel.position() + n);
                } catch (IOException e) {
                    ctx.ioe = e;
                    ctx.eof = true;
                }
            } else {
                assert ctx.stream != null && ctx.buffer != null;
                if (n <= 0) {
                    // n <= 0 should not happen, but we allow it in release build
                    if (ctx.pos >= n) {
                        ctx.pos -= n;
                    } else {
                        ctx.eof = true;
                    }
                    return;
                }
                if (ctx.pos < ctx.end) {
                    int request = Math.min(n, ctx.end - ctx.pos);
                    ctx.pos += request;
                    n -= request;
                }
                if (n > 0) {
                    ctx.pos = ctx.end = 0;
                    try {
                        ctx.stream.skipNBytes(n);
                    } catch (IOException e) {
                        ctx.ioe = e;
                        ctx.eof = true;
                    }
                }
            }
        }
    }

    static final class EOFCallback extends STBIEOFCallback {
        @Override
        public int invoke(long user) {
            SeekableContext ctx = memGlobalRefToObject(user);
            return ctx.eof ? 1 : 0;
        }
    }

    //TODO we should untrack this global memory to help with org.lwjgl.util.DebugAllocator=true
    private static volatile STBIIOCallbacks g_io_callbacks;

    private static long get_io_callbacks() {
        if (g_io_callbacks == null) {
            synchronized (BitmapFactory.class) {
                if (g_io_callbacks == null) {
                    final var readcb = new ReadCallback();
                    final var skipcb = new SkipCallback();
                    final var eofcb = new EOFCallback();
                    g_io_callbacks = STBIIOCallbacks.malloc()
                            .set(readcb, skipcb, eofcb);
                }
            }
        }
        return g_io_callbacks.address();
    }

    @Nullable
    private static Bitmap decodeSeekableStream(@NonNull InputStream stream,
                                               @Nullable Options opts, boolean info) throws IOException {
        byte[] buffer = opts != null && opts.inTempStorage != null
                ? opts.inTempStorage : new byte[SeekableContext.BUFFER_SIZE];
        var context = new SeekableContext(stream, buffer);

        final boolean isU16, isHDR;
        if (!info && opts != null && opts.inPreferredFormat != null) {
            isU16 = opts.inPreferredFormat.isChannelU16();
            isHDR = opts.inPreferredFormat.isChannelHDR();
        } else {
            int n = stream.readNBytes(buffer, 0, Math.min(buffer.length, 128));
            if (n <= 0) {
                throw new IOException("No bytes read, or buffer is too small");
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer p = stack.malloc(n);
                p.put(0, buffer, 0, n);
                isHDR = STBImage.stbi_is_hdr_from_memory(p);
                if (isHDR) {
                    isU16 = false;
                } else {
                    isU16 = STBImage.stbi_is_16_bit_from_memory(p);
                }
            }
            // rewind
            context.pos = 0;
            context.end = n;
        }

        var callbacks = get_io_callbacks();
        var user = JNINativeInterface.NewGlobalRef(context);
        try {
            if (info) {
                assert opts != null;
                decodeInfo0(callbacks, user, NULL, 0, opts, isU16, isHDR);
                return null;
            }
            return decode0(callbacks, user, NULL, 0, opts, isU16, isHDR);
        } finally {
            JNINativeInterface.DeleteGlobalRef(user);
        }
    }

    @Nullable
    private static Bitmap decodeSeekableChannel(@NonNull SeekableByteChannel channel,
                                                @Nullable Options opts, boolean info) throws IOException {
        var callbacks = get_io_callbacks();
        var context = new SeekableContext(channel);
        var user = JNINativeInterface.NewGlobalRef(context);
        try {
            final boolean isU16, isHDR;
            if (!info && opts != null && opts.inPreferredFormat != null) {
                isU16 = opts.inPreferredFormat.isChannelU16();
                isHDR = opts.inPreferredFormat.isChannelHDR();
            } else {
                final long start = channel.position();
                isHDR = STBImage.nstbi_is_hdr_from_callbacks(callbacks, user) != 0;
                channel.position(start);
                if (isHDR) {
                    isU16 = false;
                } else {
                    isU16 = STBImage.nstbi_is_16_bit_from_callbacks(callbacks, user) != 0;
                    channel.position(start);
                }
            }
            if (info) {
                assert opts != null;
                decodeInfo0(callbacks, user, NULL, 0, opts, isU16, isHDR);
                return null;
            }
            return decode0(callbacks, user, NULL, 0, opts, isU16, isHDR);
        } finally {
            JNINativeInterface.DeleteGlobalRef(user);
        }
    }

    @NonNull
    private static Bitmap decode0(long callbacks, long context,
                                  long buffer, int length, // either
                                  @Nullable Options opts, boolean isU16, boolean isHDR) throws IOException {
        assert callbacks != NULL || buffer != NULL;
        Bitmap.Format requiredFormat = null;
        if (opts != null) {
            if (opts.inPreferredFormat != null) {
                requiredFormat = opts.inPreferredFormat;
            }
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long pOuts = stack.nmalloc(4, 12);
            final long address;
            final int requiredChannels = requiredFormat != null
                    ? requiredFormat.getChannels()
                    : STBImage.STBI_default;
            if (callbacks != NULL) {
                if (isHDR) {
                    address = STBImage.nstbi_loadf_from_callbacks(callbacks, context,
                            pOuts, pOuts + 4, pOuts + 8, requiredChannels);
                } else if (isU16) {
                    address = STBImage.nstbi_load_16_from_callbacks(callbacks, context,
                            pOuts, pOuts + 4, pOuts + 8, requiredChannels);
                } else {
                    address = STBImage.nstbi_load_from_callbacks(callbacks, context,
                            pOuts, pOuts + 4, pOuts + 8, requiredChannels);
                }
            } else {
                if (isHDR) {
                    address = STBImage.nstbi_loadf_from_memory(buffer, length,
                            pOuts, pOuts + 4, pOuts + 8, requiredChannels);
                } else if (isU16) {
                    address = STBImage.nstbi_load_16_from_memory(buffer, length,
                            pOuts, pOuts + 4, pOuts + 8, requiredChannels);
                } else {
                    address = STBImage.nstbi_load_from_memory(buffer, length,
                            pOuts, pOuts + 4, pOuts + 8, requiredChannels);
                }
            }
            if (address == NULL) {
                throw new IOException("Failed to decode image: " + STBImage.stbi_failure_reason());
            }
            if (callbacks != NULL && context != NULL) {
                SeekableContext ctx = memGlobalRefToObject(context);
                if (ctx.ioe != null) {
                    throw new IOException("Failed to read image", ctx.ioe);
                }
            }
            int width = memGetInt(pOuts),
                    height = memGetInt(pOuts + 4),
                    channels_in_file = memGetInt(pOuts + 8);
            // determine the final format we got
            Bitmap.Format format = requiredFormat != null
                    ? requiredFormat
                    : Bitmap.Format.get(channels_in_file, isU16, isHDR);
            ColorSpace cs = isHDR
                    ? ColorSpace.get(ColorSpace.Named.LINEAR_EXTENDED_SRGB)
                    : ColorSpace.get(ColorSpace.Named.SRGB);
            if (opts != null) {
                opts.outWidth = width;
                opts.outHeight = height;
                opts.outFormat = format;
                opts.outColorSpace = cs;
            }
            // images have un-premultiplied alpha by default, HDR has alpha of 1.0
            int at = !isHDR && (channels_in_file == 2 || channels_in_file == 4) && format.hasAlpha()
                    ? ColorInfo.AT_UNPREMUL
                    : ColorInfo.AT_OPAQUE;
            // XXX: row stride is always (width * bpp) in STB
            Bitmap bitmap = new Bitmap(format,
                    ImageInfo.make(width, height, format.getColorType(), at, cs),
                    address, width * format.getBytesPerPixel(), STBImage::nstbi_image_free);
            if (opts == null || opts.inImmutable) {
                bitmap.setImmutable();
            }
            return bitmap;
        }
    }

    private static void decodeInfo0(long callbacks, long context,
                                    long buffer, int length, // either
                                    @NonNull Options opts, boolean isU16, boolean isHDR) throws IOException {
        assert callbacks != NULL || buffer != NULL;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long pOuts = stack.nmalloc(4, 12);
            final boolean success;
            if (callbacks != NULL) {
                success = STBImage.nstbi_info_from_callbacks(
                        callbacks, context,
                        pOuts, pOuts + 4, pOuts + 8) != 0;
            } else {
                success = STBImage.nstbi_info_from_memory(
                        buffer, length,
                        pOuts, pOuts + 4, pOuts + 8) != 0;
            }
            if (!success) {
                throw new IOException("Failed to decode image: " + STBImage.stbi_failure_reason());
            }
            if (callbacks != NULL && context != NULL) {
                SeekableContext ctx = memGlobalRefToObject(context);
                if (ctx.ioe != null) {
                    throw new IOException("Failed to read image", ctx.ioe);
                }
            }
            int width = memGetInt(pOuts),
                    height = memGetInt(pOuts + 4),
                    channels_in_file = memGetInt(pOuts + 8);
            Bitmap.Format format = Bitmap.Format.get(channels_in_file, isU16, isHDR);
            ColorSpace cs = isHDR
                    ? ColorSpace.get(ColorSpace.Named.LINEAR_EXTENDED_SRGB)
                    : ColorSpace.get(ColorSpace.Named.SRGB);
            opts.outWidth = width;
            opts.outHeight = height;
            opts.outFormat = format;
            opts.outColorSpace = cs;
        }
    }

    @ApiStatus.Internal
    public static void decodeMimeType(@NonNull Options opts, @NonNull Object input) {
        try (var stream = ImageIO.createImageInputStream(input)) {
            if (stream != null) {
                var readers = ImageIO.getImageReaders(stream);
                if (readers.hasNext()) {
                    String[] mimeTypes = readers.next().getOriginatingProvider().getMIMETypes();
                    if (mimeTypes != null) {
                        opts.outMimeType = mimeTypes[0];
                        return;
                    }
                }
                // the order is important
                if (test(stream, BitmapFactory::filterPSD)) {
                    opts.outMimeType = "image/vnd.adobe.photoshop";
                } else if (test(stream, BitmapFactory::filterPIC)) {
                    opts.outMimeType = "image/x-pict";
                } else if (test(stream, BitmapFactory::filterPGM)) {
                    opts.outMimeType = "image/x-portable-graymap";
                } else if (test(stream, BitmapFactory::filterPPM)) {
                    opts.outMimeType = "image/x-portable-pixmap";
                } else if (test(stream, BitmapFactory::filterHDR)) {
                    opts.outMimeType = "image/vnd.radiance";
                } else if (test(stream, BitmapFactory::filterTGA)) {
                    opts.outMimeType = "image/x-tga";
                }
            }
        } catch (Exception ignored) {
        }
    }

    @ApiStatus.Internal
    public static boolean test(@NonNull ImageInputStream stream,
                               @NonNull Predicate<ImageInputStream> filter) {
        try {
            stream.mark();
            try {
                return filter.test(stream);
            } finally {
                stream.reset();
            }
        } catch (Exception e) {
            return false;
        }
    }

    @ApiStatus.Internal
    public static boolean filterPSD(@NonNull ImageInputStream stream) {
        try {
            return stream.read() == '8' &&
                    stream.read() == 'B' &&
                    stream.read() == 'P' &&
                    stream.read() == 'S';
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean filterPIC(@NonNull ImageInputStream stream) {
        try {
            if (stream.read() != 0x53 ||
                    stream.read() != 0x80 ||
                    stream.read() != 0xF6 ||
                    stream.read() != 0x34)
                return false;
            stream.seek(stream.getStreamPosition() + 84);
            return stream.read() == 'P' &&
                    stream.read() == 'I' &&
                    stream.read() == 'C' &&
                    stream.read() == 'T';
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ApiStatus.Internal
    public static boolean filterPGM(@NonNull ImageInputStream stream) {
        try {
            return stream.read() == 'P' && stream.read() == '5';
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ApiStatus.Internal
    public static boolean filterPPM(@NonNull ImageInputStream stream) {
        try {
            return stream.read() == 'P' && stream.read() == '6';
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ApiStatus.Internal
    public static boolean filterHDR(@NonNull ImageInputStream stream) {
        try {
            return stream.read() == '#' &&
                    stream.read() == '?' &&
                    stream.read() == 'R' &&
                    stream.read() == 'A' &&
                    stream.read() == 'D' &&
                    stream.read() == 'I' &&
                    stream.read() == 'A' &&
                    stream.read() == 'N' &&
                    stream.read() == 'C' &&
                    stream.read() == 'E' &&
                    stream.read() == '\n';
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ApiStatus.Internal
    public static boolean filterTGA(@NonNull ImageInputStream stream) {
        // TGA has no magic number, it must be the last
        try {
            stream.read();
            int color_map_type = stream.read();
            if (color_map_type != 0 && color_map_type != 1)
                return false;
            int data_type_code = stream.read();
            if (color_map_type == 1) {
                // Uncompressed, color-mapped images.
                // Run-Length Encoded color-mapped images.
                if (data_type_code != 1 && data_type_code != 9)
                    return false;
                stream.readInt(); // color map range
                int color_map_depth = stream.read();
                if (color_map_depth != 16 && color_map_depth != 24 && color_map_depth != 32)
                    return false;
            } else {
                if (data_type_code != 2 && data_type_code != 3 && data_type_code != 10 && data_type_code != 11)
                    return false;
                stream.readInt(); // color map range
                stream.read(); // color map depth
            }
            stream.readInt(); // origin
            stream.readInt(); // dimensions
            int bits_per_pixel = stream.read();
            if (color_map_type == 1 && bits_per_pixel != 8 && bits_per_pixel != 16)
                return false;
            return bits_per_pixel == 16 || bits_per_pixel == 24 || bits_per_pixel == 32;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
