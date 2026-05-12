/*
 * Modern UI.
 * Copyright (C) 2023-2026 BloCamLimb. All rights reserved.
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
import icyllis.arc3d.core.ColorSpaceRGB;
import icyllis.arc3d.core.ColorSpaces;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
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
 * <p>
 * This class provides static methods for decoding encoded image data from various
 * sources into pixel maps (i.e. {@link Bitmap Bitmaps}).
 * <p>
 * The following image formats are supported:
 * <table border="1">
 * <caption>Supported Formats</caption>
 * <tr><th>Format Name</th><th>MIME Type</th><th>Typical Extensions</th></tr>
 * <tr><td>PNG</td><td>{@code image/png}</td><td>.png</td></tr>
 * <tr><td>GIF</td><td>{@code image/gif}</td><td>.gif</td></tr>
 * <tr><td>JPEG</td><td>{@code image/jpeg}</td><td>.jpg, .jpeg, .jfif</td></tr>
 * <tr><td>TIFF</td><td>{@code image/tiff}</td><td>.tiff, .tif</td></tr>
 * <tr><td>Radiance HDR</td><td>{@code image/vnd.radiance}</td><td>.hdr</td></tr>
 * <tr><td>Netpbm PGM</td><td>{@code image/x-portable-graymap}</td><td>.pgm</td></tr>
 * <tr><td>Netpbm PPM</td><td>{@code image/x-portable-pixmap}</td><td>.ppm, .pnm</td></tr>
 * </table>
 * </p>
 * <p>
 * Decoding is supported from the following input sources:
 * <ul>
 * <li>{@link File}</li>
 * <li>{@link Path}</li>
 * <li>{@link InputStream}</li>
 * <li>{@link ReadableByteChannel}</li>
 * <li>{@code byte[]}</li>
 * <li>{@link ByteBuffer}</li>
 * </ul>
 * </p>
 * <p>
 * For each input source, a corresponding {@code decode*Info} method is provided.
 * These methods parse only the metadata (dimensions, format, and MIME type)
 * without decoding or allocating memory for the underlying pixel data.
 * </p>
 * <p>
 * All methods can be called from any thread, but it is recommended to call them
 * from a worker thread.
 * </p>
 *
 * @since 3.7
 */
public final class BitmapFactory {

    //TODO get rid of stb_image and use ModernUI codec to support color management, PNGv3, HDR image...

    /**
     * Collects the options for the decoder and additional outputs from the decoder.
     *
     * <p>Unlike Android (with its Skia graphics engine), it is not necessary to
     * pre-multiply alpha of image data in ModernUI. We allow images
     * to be directly drawn by the view system or through a {@link Canvas} either
     * pre-multiplied or non-pre-multiplied. Although pre-multiplied alpha can
     * help draw-time blending, but it results in precision loss since images
     * are 8-bit per channel in memory. Instead, ModernUI will pre-multiply alpha
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
         * (for instance {@link ColorSpaces#SRGB sRGB} for
         * {@link Bitmap.Format#RGBA_8888} format.</p>
         *
         * <p class="note">Only {@link ColorSpace#MODEL_RGB} color spaces are
         * currently supported. An <code>IllegalArgumentException</code> will
         * be thrown by the decode methods when setting a non-RGB color space
         * such as {@link ColorSpaces#CIE_LAB Lab}.</p>
         *
         * <p class="note">The specified color space's transfer function must be
         * an {@link ColorSpaceRGB.TransferParameters ICC parametric curve}. An
         * <code>IllegalArgumentException</code> will be thrown by the decode methods
         * if calling {@link ColorSpaceRGB#getTransferParameters()} on the
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
         * <p>
         * Supported MIME types include:
         * <ul>
         * <li>{@code "image/png"} - Portable Network Graphics</li>
         * <li>{@code "image/gif"} - Graphics Interchange Format</li>
         * <li>{@code "image/bmp"} - Windows Bitmap</li>
         * <li>{@code "image/jpeg"} - Joint Photographic Experts Group</li>
         * <li>{@code "image/tiff"} - Tagged Image File Format</li>
         * <li>{@code "image/vnd.radiance"} - Radiance HDR</li>
         * <li>{@code "image/vnd.adobe.photoshop"} - Adobe Photoshop Document (PSD)</li>
         * <li>{@code "image/x-tga"} - Truevision TGA (Targa)</li>
         * <li>{@code "image/x-softimage-pic"} - Softimage PIC</li>
         * <li>{@code "image/x-portable-graymap"} - Netpbm Grayscale (PGM)</li>
         * <li>{@code "image/x-portable-pixmap"} - Netpbm Color (PPM)</li>
         * </ul>
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

        /**
         * For debug printing.
         *
         * @since 3.13
         */
        @Override
        public String toString() {
            //noinspection ImplicitArrayToString
            return "Bitmap.Options{" +
                    "inImmutable=" + inImmutable +
                    ", inDecodeMimeType=" + inDecodeMimeType +
                    ", inPreferredFormat=" + inPreferredFormat +
                    ", inPreferredColorSpace=" + inPreferredColorSpace +
                    ", inTempStorage=" + inTempStorage +
                    ", outWidth=" + outWidth +
                    ", outHeight=" + outHeight +
                    ", outMimeType='" + outMimeType + '\'' +
                    ", outFormat=" + outFormat +
                    ", outColorSpace=" + outColorSpace +
                    '}';
        }
    }

    private static void validate(Options opts) {
        if (opts == null) return;

        if (opts.inPreferredColorSpace != null) {
            if (!(opts.inPreferredColorSpace instanceof ColorSpaceRGB rgbColorSpace)) {
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
     * Decode a file path into an immutable bitmap.
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
     * on the halfway. If the specified color space is not {@link ColorSpace#MODEL_RGB RGB},
     * or if the specified color space's transfer function is not an
     * {@link ColorSpaceRGB.TransferParameters ICC parametric curve}, the method throws
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
        try (final var stream = new FileInputStream(file)) {
            decodeSeekableChannel(stream.getChannel(), opts, true);
        }
    }

    /**
     * Decode a file path into an immutable bitmap.
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
     * on the halfway. If the specified color space is not {@link ColorSpace#MODEL_RGB RGB},
     * or if the specified color space's transfer function is not an
     * {@link ColorSpaceRGB.TransferParameters ICC parametric curve}, the method throws
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
     * Decode an input stream into an immutable bitmap.
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
     * on the halfway. If the specified color space is not {@link ColorSpace#MODEL_RGB RGB},
     * or if the specified color space's transfer function is not an
     * {@link ColorSpaceRGB.TransferParameters ICC parametric curve}, the method throws
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
            bm = decodeSeekableChannel(ch, opts, false);
        } else {
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
            decodeSeekableChannel(ch, opts, true);
        } else {
            decodeSeekableStream(stream, opts, true);
        }
    }

    /**
     * Decode a readable channel into an immutable bitmap.
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
     * on the halfway. If the specified color space is not {@link ColorSpace#MODEL_RGB RGB},
     * or if the specified color space's transfer function is not an
     * {@link ColorSpaceRGB.TransferParameters ICC parametric curve}, the method throws
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
            bm = decodeSeekableChannel(ch, opts, false);
        } else {
            ByteBuffer p = null;
            try {
                p = Core.readIntoNativeBuffer(channel);
                bm = decodeBuffer(p.flip(), opts);
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
            decodeSeekableChannel(ch, opts, true);
        } else {
            ByteBuffer p = null;
            try {
                p = Core.readIntoNativeBuffer(channel);
                decodeBufferInfo(p.flip(), opts);
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
     * Decode a bitmap from the specified byte array.
     * <p>
     * If the data cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway. If the specified color space is not {@link ColorSpace#MODEL_RGB RGB},
     * or if the specified color space's transfer function is not an
     * {@link ColorSpaceRGB.TransferParameters ICC parametric curve}, the method throws
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
        final Bitmap bm = decodeHeapBuffer(data, offset, length,
                    opts, false);
        assert bm != null;
        return bm;
    }

    /**
     * Query the bitmap info from the specified byte array, without allocating the
     * memory for its pixels.
     * <p>
     * If the data cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway.
     *
     * @param data   byte array of compressed image data
     * @param offset offset into image data for where the decoder should begin parsing
     * @param length the number of bytes, beginning at offset, to parse
     * @param opts   the out values
     * @throws IOException the data cannot be decoded into a bitmap
     * @since 3.13
     */
    public static void decodeByteArrayInfo(byte[] data, int offset, int length,
                                           @NonNull Options opts) throws IOException {
        if ((offset | length) < 0 || data.length < offset + length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        //noinspection resource
        final Bitmap bm = decodeHeapBuffer(data, offset, length, opts, true);
        assert bm == null;
    }

    /**
     * Decode a bitmap from the specified byte buffer. The buffer may be a
     * heap buffer or a direct buffer. The decoder starts from the current
     * {@link ByteBuffer#position()} and read {@link ByteBuffer#remaining()} bytes at most.
     * And the buffer's position will remain unchanged after the call.
     * <p>
     * If the buffer cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway. If the specified color space is not {@link ColorSpace#MODEL_RGB RGB},
     * or if the specified color space's transfer function is not an
     * {@link ColorSpaceRGB.TransferParameters ICC parametric curve}, the method throws
     * {@link IllegalArgumentException}.
     *
     * @param buffer byte buffer of compressed image data
     * @param opts   the decoder options
     * @return the decoded bitmap
     * @throws IllegalArgumentException the options are invalid
     * @throws IOException              the data cannot be decoded into a bitmap
     * @since 3.13
     */
    @NonNull
    public static Bitmap decodeBuffer(@NonNull ByteBuffer buffer,
                                      @Nullable Options opts) throws IOException {
        if (buffer.hasArray()) {
            return decodeByteArray(buffer.array(),
                    buffer.arrayOffset() + buffer.position(),
                    buffer.remaining(), opts);
        }
        validate(opts);
        if (!buffer.isDirect()) {
            throw new IOException("No data provided");
        }
        if (opts != null && opts.inDecodeMimeType) {
            // scan header for setting MIME type
            decodeMimeType(opts, buffer);
        }
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

    /**
     * Query the bitmap info from the specified byte buffer, without allocating the
     * memory for its pixels. The buffer may be a
     * heap buffer or a direct buffer. The decoder starts from the current
     * {@link ByteBuffer#position()} and read {@link ByteBuffer#remaining()} bytes at most.
     * And the buffer's position will remain unchanged after the call.
     * <p>
     * If the data cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway.
     *
     * @param buffer byte buffer of compressed image data
     * @param opts   the out values
     * @throws IOException the data cannot be decoded into a bitmap
     * @since 3.13
     */
    public static void decodeBufferInfo(@NonNull ByteBuffer buffer,
                                        @NonNull Options opts) throws IOException {
        if (buffer.hasArray()) {
            decodeByteArrayInfo(buffer.array(),
                    buffer.arrayOffset() + buffer.position(),
                    buffer.remaining(), opts);
            return;
        }
        if (!buffer.isDirect()) {
            throw new IOException("No data provided");
        }
        final long buf = memAddress(buffer);
        final int len = buffer.remaining();
        if (opts.inDecodeMimeType) {
            // scan header for setting MIME type
            decodeMimeType(opts, buffer);
        }
        final boolean isU16, isHDR;
        isHDR = STBImage.nstbi_is_hdr_from_memory(buf, len) != 0;
        isU16 = !isHDR && STBImage.nstbi_is_16_bit_from_memory(buf, len) != 0;
        decodeInfo0(NULL, NULL, buf, len, opts, isU16, isHDR);
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
            // File channel can read directly into the native buffer without
            // going through the heap buffer. If it's not a file channel, then
            // this typically performs a copy from the heap to native.
            // Additionally, we require the channel to be in blocking mode.
            ByteBuffer dst = STBIReadCallback.getData(data, size);
            if (ctx.channel != null) {
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
                assert ctx.buffer != null;
                while (read < size) {
                    int request = size - read;
                    // If we have pushback buffer, consume it first
                    if (ctx.pos < ctx.end) {
                        request = Math.min(request, ctx.end - ctx.pos);
                        dst.put(ctx.buffer, ctx.pos, request);
                        ctx.pos += request;
                        read += request;
                        continue;
                    }
                    if (ctx.stream == null) {
                        ctx.eof = true;
                        break;
                    }
                    // Otherwise read from stream
                    try {
                        int n = ctx.stream.read(ctx.buffer, 0,
                                Math.min(ctx.buffer.length, SeekableContext.BUFFER_SIZE));
                        if (n <= 0) {
                            ctx.eof = true;
                            break;
                        }
                        request = Math.min(request, n);
                        dst.put(ctx.buffer, 0, request);
                        ctx.pos = request;
                        ctx.end = n;
                        read += request;
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
                assert ctx.buffer != null;
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
                    if (ctx.stream == null) {
                        ctx.eof = true;
                        return;
                    }
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
    private static Bitmap decodeHeapBuffer(byte[] buffer, int offset, int length,
                                           @Nullable Options opts, boolean info) throws IOException {
        if (opts != null && opts.inDecodeMimeType) {
            decodeMimeType(opts, ByteBuffer.wrap(buffer, offset, length));
        }

        var context = new SeekableContext(null, buffer);
        context.pos = offset;
        context.end = offset + length;

        final boolean isU16, isHDR;
        if (!info && opts != null && opts.inPreferredFormat != null) {
            isU16 = opts.inPreferredFormat.isChannelU16();
            isHDR = opts.inPreferredFormat.isChannelHDR();
        } else {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                int n = Math.min(length, 128);
                ByteBuffer p = stack.malloc(n);
                p.put(0, buffer, offset, n);
                isHDR = STBImage.stbi_is_hdr_from_memory(p);
                if (isHDR) {
                    isU16 = false;
                } else {
                    isU16 = STBImage.stbi_is_16_bit_from_memory(p);
                }
            }
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
    private static Bitmap decodeSeekableStream(@NonNull InputStream stream,
                                               @Nullable Options opts, boolean info) throws IOException {
        byte[] buffer = opts != null && opts.inTempStorage != null
                ? opts.inTempStorage : new byte[SeekableContext.BUFFER_SIZE];

        int n = stream.readNBytes(buffer, 0, Math.min(buffer.length, 128));
        if (n <= 0) {
            throw new IOException("No bytes read, or buffer is too small");
        }

        if (opts != null && opts.inDecodeMimeType) {
            decodeMimeType(opts, ByteBuffer.wrap(buffer, 0, n));
        }

        var context = new SeekableContext(stream, buffer);
        // rewind
        context.pos = 0;
        context.end = n;

        final boolean isU16, isHDR;
        if (!info && opts != null && opts.inPreferredFormat != null) {
            isU16 = opts.inPreferredFormat.isChannelU16();
            isHDR = opts.inPreferredFormat.isChannelHDR();
        } else {
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
        if (opts != null && opts.inDecodeMimeType) {
            long start = channel.position();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer seek = stack.malloc(128);
                //noinspection StatementWithEmptyBody
                while (channel.read(seek) > 0)
                    ;
                decodeMimeType(opts, seek.flip());
            }
            channel.position(start);
        }

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
                    ? ColorSpaces.LINEAR_EXTENDED_SRGB
                    : ColorSpaces.SRGB;
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
                    ? ColorSpaces.LINEAR_EXTENDED_SRGB
                    : ColorSpaces.SRGB;
            opts.outWidth = width;
            opts.outHeight = height;
            opts.outFormat = format;
            opts.outColorSpace = cs;
        }
    }

    //TODO TIFF is todo

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public static void decodeMimeType(@NonNull Options opts, @NonNull ByteBuffer input) {
        // the order is important
        if (test(input, BitmapFactory::filterPNG)) {
            opts.outMimeType = "image/png";
        } else if (test(input, BitmapFactory::filterBMP)) {
            opts.outMimeType = "image/bmp";
        } else if (test(input, BitmapFactory::filterGIF)) {
            opts.outMimeType = "image/gif";
        } else if (test(input, BitmapFactory::filterPSD)) {
            opts.outMimeType = "image/vnd.adobe.photoshop";
        } else if (test(input, BitmapFactory::filterPIC)) {
            opts.outMimeType = "image/x-softimage-pic";
        } else if (test(input, BitmapFactory::filterJPEG)) {
            opts.outMimeType = "image/jpeg";
        } else if (test(input, BitmapFactory::filterPGM)) {
            opts.outMimeType = "image/x-portable-graymap";
        } else if (test(input, BitmapFactory::filterPPM)) {
            opts.outMimeType = "image/x-portable-pixmap";
        } else if (test(input, BitmapFactory::filterHDR)) {
            opts.outMimeType = "image/vnd.radiance";
        } else if (test(input, BitmapFactory::filterTGA)) {
            opts.outMimeType = "image/x-tga";
        } else {
            // since 3.13.0, clear to null if unknown
            opts.outMimeType = null;
        }
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public static boolean test(@NonNull ByteBuffer input,
                               @NonNull Predicate<ByteBuffer> filter) {
        try {
            input.mark();
            try {
                return filter.test(input);
            } finally {
                input.reset();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public static boolean filterPNG(@NonNull ByteBuffer input) {
        return input.get() == (byte) 137 &&
                input.get() == (byte) 80 &&
                input.get() == (byte) 78 &&
                input.get() == (byte) 71 &&
                input.get() == (byte) 13 &&
                input.get() == (byte) 10 &&
                input.get() == (byte) 26 &&
                input.get() == (byte) 10;
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public static boolean filterBMP(@NonNull ByteBuffer input) {
        return input.get() == (byte) 'B' &&
                input.get() == (byte) 'M';
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public static boolean filterGIF(@NonNull ByteBuffer input) {
        int b;
        return input.get() == (byte) 'G' &&
                input.get() == (byte) 'I' &&
                input.get() == (byte) 'F' &&
                input.get() == (byte) '8' &&
                ((b = input.get()) == (byte) '7' || b == (byte) '9') &&
                input.get() == (byte) 'a';
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public static boolean filterJPEG(@NonNull ByteBuffer input) {
        return input.get() == (byte) 0xFF &&
                input.get() == (byte) 0xD8;
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public static boolean filterPSD(@NonNull ByteBuffer input) {
        return input.get() == (byte) '8' &&
                input.get() == (byte) 'B' &&
                input.get() == (byte) 'P' &&
                input.get() == (byte) 'S';
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public static boolean filterPIC(@NonNull ByteBuffer input) {
        if (input.get() != (byte) 0x53 ||
                input.get() != (byte) 0x80 ||
                input.get() != (byte) 0xF6 ||
                input.get() != (byte) 0x34)
            return false;
        input.position(input.position() + 84);
        return input.get() == (byte) 'P' &&
                input.get() == (byte) 'I' &&
                input.get() == (byte) 'C' &&
                input.get() == (byte) 'T';
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public static boolean filterPGM(@NonNull ByteBuffer input) {
        return input.get() == (byte) 'P' && input.get() == (byte) '5';
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public static boolean filterPPM(@NonNull ByteBuffer input) {
        return input.get() == (byte) 'P' && input.get() == (byte) '6';
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public static boolean filterHDR(@NonNull ByteBuffer input) {
        return input.get() == (byte) '#' &&
                input.get() == (byte) '?' &&
                input.get() == (byte) 'R' &&
                input.get() == (byte) 'A' &&
                input.get() == (byte) 'D' &&
                input.get() == (byte) 'I' &&
                input.get() == (byte) 'A' &&
                input.get() == (byte) 'N' &&
                input.get() == (byte) 'C' &&
                input.get() == (byte) 'E' &&
                input.get() == (byte) '\n';
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public static boolean filterTGA(@NonNull ByteBuffer input) {
        // TGA has no magic number, it must be the last
        input.get();
        int color_map_type = input.get() & 0xff;
        if (color_map_type != 0 && color_map_type != 1)
            return false;
        int data_type_code = input.get() & 0xff;
        if (color_map_type == 1) {
            // Uncompressed, color-mapped images.
            // Run-Length Encoded color-mapped images.
            if (data_type_code != 1 && data_type_code != 9)
                return false;
            input.getInt(); // color map range
            int color_map_depth = input.get() & 0xff;
            if (color_map_depth != 16 && color_map_depth != 24 && color_map_depth != 32)
                return false;
        } else {
            if (data_type_code != 2 && data_type_code != 3 && data_type_code != 10 && data_type_code != 11)
                return false;
            input.getInt(); // color map range
            input.get(); // color map depth
        }
        input.getInt(); // origin
        input.getInt(); // dimensions
        int bits_per_pixel = input.get() & 0xff;
        if (color_map_type == 1 && bits_per_pixel != 8 && bits_per_pixel != 16)
            return false;
        return bits_per_pixel == 16 || bits_per_pixel == 24 || bits_per_pixel == 32;
    }
}
