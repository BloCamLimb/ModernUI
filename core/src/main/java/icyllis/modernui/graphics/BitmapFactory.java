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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Core;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * Creates {@link Bitmap Bitmaps} from encoded data.
 *
 * @since 3.7
 */
public final class BitmapFactory {

    /**
     * Collects the options for the decoder and additional outputs from the decoder.
     */
    public static class Options {

        /**
         * If set to true, the decoder will return null (no bitmap), but
         * the <code>out...</code> fields will still be set, allowing the caller to
         * query the bitmap without having to allocate the memory for its pixels.
         */
        public boolean inJustDecodeBounds;

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
         * If true, the resulting bitmap will have its color channels pre-multiplied
         * by the alpha channel in advance. <em>The default is false</em>.
         *
         * <p>Unlike Android (with its Skia graphics engine), it is not necessary to
         * pre-multiply alpha of image data in Modern UI framework. We allow images
         * to be directly drawn by the view system or through a {@link Canvas} either
         * pre-multiplied or un-pre-multiplied. Although pre-multiplied alpha can
         * simplify draw-time blending, but it results in precision loss since images
         * are 8-bit per channel in memory. Instead, Modern UI will pre-multiply alpha
         * when sampling textures.</p>
         *
         * <p>This does not affect bitmaps without an alpha channel.</p>
         *
         * @see Bitmap#hasAlpha()
         * @see Bitmap#isPremultiplied()
         */
        public boolean inPremultiplied;

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
            if (!(opts.inPreferredColorSpace instanceof ColorSpace.Rgb)) {
                throw new IllegalArgumentException("The destination color space must use the " +
                        "RGB color model");
            }
            if (((ColorSpace.Rgb) opts.inPreferredColorSpace).getTransferParameters() == null) {
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
        return Objects.requireNonNull(decodeFile(file, null));
    }

    /**
     * Decode a file path into a bitmap.
     * <p>
     * If <var>opts</var> is non-null and {@link Options#inJustDecodeBounds} is true,
     * the method returns null. Otherwise, the method returns non-null.
     * <p>
     * If the file cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway. If the specified color space is not {@link ColorSpace.Model#RGB RGB},
     * or if the specified color space's transfer function is not an
     * {@link ColorSpace.Rgb.TransferParameters ICC parametric curve}, the method throws
     * {@link IllegalArgumentException}.
     *
     * @param file the file to be decoded
     * @param opts the decoder options
     * @return the decoded bitmap, or null if opts is non-null, if opts requested only the
     * size be returned (in opts.outWidth and opts.outHeight)
     * @throws IllegalArgumentException the options are invalid
     * @throws IOException              the file cannot be decoded into a bitmap
     */
    @Nullable
    public static Bitmap decodeFile(@NonNull File file,
                                    @Nullable Options opts) throws IOException {
        validate(opts);
        if (opts != null) {
            setMimeType(opts, file);
        }
        final Bitmap bm;
        try (final var stream = new FileInputStream(file)) {
            bm = decodeSeekableChannel(stream.getChannel(), opts);
        }
        return bm;
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
        return Objects.requireNonNull(decodePath(path, null));
    }

    /**
     * Decode a file path into a bitmap.
     * <p>
     * If <var>opts</var> is non-null and {@link Options#inJustDecodeBounds} is true,
     * the method returns null. Otherwise, the method returns non-null.
     * <p>
     * If the file cannot be decoded into a bitmap, the method throws {@link IOException}
     * on the halfway. If the specified color space is not {@link ColorSpace.Model#RGB RGB},
     * or if the specified color space's transfer function is not an
     * {@link ColorSpace.Rgb.TransferParameters ICC parametric curve}, the method throws
     * {@link IllegalArgumentException}.
     *
     * @param path the file to be decoded
     * @param opts the decoder options
     * @return the decoded bitmap, or null if opts is non-null, if opts requested only the
     * size be returned (in opts.outWidth and opts.outHeight)
     * @throws IllegalArgumentException the options are invalid
     * @throws IOException              the file cannot be decoded into a bitmap
     */
    @Nullable
    public static Bitmap decodePath(@NonNull Path path,
                                    @Nullable Options opts) throws IOException {
        validate(opts);
        if (opts != null) {
            setMimeType(opts, path.toFile());
        }
        final Bitmap bm;
        try (final var channel = FileChannel.open(path, StandardOpenOption.READ)) {
            bm = decodeSeekableChannel(channel, opts);
        }
        return bm;
    }

    /**
     * Decode an input stream into a bitmap.
     * <p>
     * The stream's position will be at the end of the encoded data. This method
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
        return Objects.requireNonNull(decodeStream(stream, null));
    }

    /**
     * Decode an input stream into a bitmap.
     * <p>
     * If <var>opts</var> is non-null and {@link Options#inJustDecodeBounds} is true,
     * the method returns null. Otherwise, the method returns non-null.
     * <p>
     * The stream's position will be at the end of the encoded data. This method
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
     * @return the decoded bitmap, or null if opts is non-null, if opts requested only the
     * size be returned (in opts.outWidth and opts.outHeight)
     * @throws IllegalArgumentException the options are invalid
     * @throws IOException              the data cannot be decoded into a bitmap
     */
    @Nullable
    public static Bitmap decodeStream(@NonNull InputStream stream,
                                      @Nullable Options opts) throws IOException {
        validate(opts);
        final Bitmap bm;
        if (stream.getClass() == FileInputStream.class) {
            FileChannel ch = ((FileInputStream) stream).getChannel();
            if (opts != null) {
                long pos = ch.position();
                setMimeType(opts, stream); // no need to close
                ch.position(pos);
            }
            bm = decodeSeekableChannel(ch, opts);
        } else {
            ByteBuffer p = null;
            try {
                p = Core.readBuffer(stream);
                bm = decodeBuffer(p.rewind(), opts);
            } finally {
                MemoryUtil.memFree(p);
            }
        }
        return bm;
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
     * @param channel the readable stream to be decoded
     * @return the decoded bitmap
     * @throws IOException the data cannot be decoded into a bitmap
     */
    @NonNull
    public static Bitmap decodeChannel(@NonNull ReadableByteChannel channel) throws IOException {
        return Objects.requireNonNull(decodeChannel(channel, null));
    }

    /**
     * Decode a readable channel into a bitmap.
     * <p>
     * If <var>opts</var> is non-null and {@link Options#inJustDecodeBounds} is true,
     * the method returns null. Otherwise, the method returns non-null.
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
     * @param channel the readable stream to be decoded
     * @param opts    the decoder options
     * @return the decoded bitmap, or null if opts is non-null, if opts requested only the
     * size be returned (in opts.outWidth and opts.outHeight)
     * @throws IllegalArgumentException the options are invalid
     * @throws IOException              the data cannot be decoded into a bitmap
     */
    @Nullable
    public static Bitmap decodeChannel(@NonNull ReadableByteChannel channel,
                                       @Nullable Options opts) throws IOException {
        validate(opts);
        final Bitmap bm;
        if (channel instanceof SeekableByteChannel ch) {
            if (opts != null) {
                long pos = ch.position();
                setMimeType(opts, Channels.newInputStream(channel)); // no need to close
                ch.position(pos);
            }
            bm = decodeSeekableChannel(ch, opts);
        } else {
            ByteBuffer p = null;
            try {
                p = Core.readBuffer(channel);
                bm = decodeBuffer(p.rewind(), opts);
            } finally {
                MemoryUtil.memFree(p);
            }
        }
        return bm;
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
     * If <var>opts</var> is non-null and {@link Options#inJustDecodeBounds} is true,
     * the method returns null. Otherwise, the method returns non-null.
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
     * @return the decoded bitmap, or null if opts is non-null, if opts requested only the
     * size be returned (in opts.outWidth and opts.outHeight)
     * @throws IllegalArgumentException the options are invalid
     * @throws IOException              the data cannot be decoded into a bitmap
     */
    @Nullable
    public static Bitmap decodeByteArray(byte[] data, int offset, int length,
                                         @Nullable Options opts) throws IOException {
        if ((offset | length) < 0 || data.length < offset + length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        validate(opts);
        if (opts != null) {
            setMimeType(opts, new ByteArrayInputStream(data, offset, length));  // no need to close (nop)
        }
        ByteBuffer p = null;
        final Bitmap bm;
        try {
            p = MemoryUtil.memAlloc(length);
            bm = decode(null,
                    p.put(data, offset, length).rewind(), opts, null);
        } finally {
            MemoryUtil.memFree(p);
        }
        return bm;
    }

    // INTERNAL
    @Nullable
    public static Bitmap decodeBuffer(@NonNull ByteBuffer buffer,
                                      @Nullable Options opts) throws IOException {
        validate(opts);
        assert buffer.isDirect();
        if (opts != null) {
            // scan header for setting MIME type, 64 bytes is enough
            byte[] seek = new byte[Math.min(buffer.limit(), 64)];
            buffer.get(0, seek, 0, seek.length);
            setMimeType(opts, new ByteArrayInputStream(seek)); // no need to close (nop)
        }
        return decode(null, buffer, opts, null);
    }

    private static void setMimeType(@NonNull Options opts, @NonNull Object input) {
        try (var stream = ImageIO.createImageInputStream(input)) {
            if (stream != null) {
                var readers = ImageIO.getImageReaders(stream);
                if (readers.hasNext()) {
                    String[] mimeTypes = readers.next().getOriginatingProvider().getMIMETypes();
                    if (mimeTypes != null) {
                        opts.outMimeType = mimeTypes[0];
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Nullable
    private static Bitmap decodeSeekableChannel(@NonNull SeekableByteChannel channel,
                                                @Nullable Options opts) throws IOException {
        final boolean[] eof = {false};
        final IOException[] ioe = {null};
        final var readcb = new STBIReadCallback() {
            @Override
            public int invoke(long user, long data, int size) {
                if (eof[0]) {
                    return 0;
                }
                int n;
                try {
                    n = channel.read(STBIReadCallback.getData(data, size));
                } catch (IOException e) {
                    ioe[0] = e;
                    eof[0] = true;
                    return 0;
                }
                if (n < 0) {
                    eof[0] |= true;
                    return 0;
                }
                return n;
            }
        };
        final var skipcb = new STBISkipCallback() {
            @Override
            public void invoke(long user, int n) {
                if (eof[0]) {
                    return;
                }
                try {
                    channel.position(channel.position() + n);
                } catch (IOException e) {
                    ioe[0] = e;
                    eof[0] = true;
                }
            }
        };
        final var eofcb = new STBIEOFCallback() {
            @Override
            public int invoke(long user) {
                return eof[0] ? 1 : 0;
            }
        };
        try (MemoryStack stack = MemoryStack.stackPush();
             readcb; skipcb; eofcb) {
            return decode(STBIIOCallbacks.malloc(stack)
                    .set(readcb, skipcb, eofcb), null, opts, ioe);
        }
    }

    @Nullable
    private static Bitmap decode(@Nullable STBIIOCallbacks callbacks, @Nullable ByteBuffer buffer, // either
                                 @Nullable Options opts, @Nullable IOException[] ioe) throws IOException {
        if (opts != null && opts.inJustDecodeBounds) {
            decodeInfo(callbacks, buffer, opts, ioe);
            return null;
        }
        Bitmap.Format prefFormat = null;
        if (opts != null) {
            if (opts.inPreferredFormat != null) {
                prefFormat = opts.inPreferredFormat;
            }
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long pOuts = stack.nmalloc(4, 12);
            final long address;
            if (callbacks != null) {
                address = STBImage.nstbi_load_from_callbacks(
                        callbacks.address(), NULL, pOuts, pOuts + 4, pOuts + 8,
                        prefFormat != null ? prefFormat.getChannelCount() : STBImage.STBI_default);
            } else {
                assert buffer != null;
                address = STBImage.nstbi_load_from_memory(
                        memAddress(buffer), buffer.remaining(), pOuts, pOuts + 4, pOuts + 8,
                        prefFormat != null ? prefFormat.getChannelCount() : STBImage.STBI_default);
            }
            if (address == NULL) {
                throw new IOException("Failed to decode image: " + STBImage.stbi_failure_reason());
            }
            if (ioe != null && ioe[0] != null) {
                throw new IOException("Failed to read image", ioe[0]);
            }
            int width = memGetInt(pOuts),
                    height = memGetInt(pOuts + 4),
                    channels = memGetInt(pOuts + 8);
            Bitmap.Format format = prefFormat != null ? prefFormat : Bitmap.Format.of(channels);
            if (opts != null) {
                opts.outWidth = width;
                opts.outHeight = height;
                opts.outFormat = format;
                opts.outColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
            }
            Bitmap bitmap = new Bitmap(format,
                    ImageInfo.make(width, height, format.getColorType(), ImageInfo.AT_UNPREMUL,
                            ColorSpace.get(ColorSpace.Named.SRGB)),
                    address, width * format.getChannelCount(), STBImage::nstbi_image_free);
            bitmap.setImmutable();
            return bitmap;
        }
    }

    private static void decodeInfo(@Nullable STBIIOCallbacks callbacks, @Nullable ByteBuffer buffer,
                                   @NonNull Options opts, @Nullable IOException[] ioe) throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long pOuts = stack.nmalloc(4, 12);
            final boolean success;
            if (callbacks != null) {
                success = STBImage.nstbi_info_from_callbacks(
                        callbacks.address(), NULL, pOuts, pOuts + 4, pOuts + 8) != 0;
            } else {
                assert buffer != null;
                success = STBImage.nstbi_info_from_memory(
                        memAddress(buffer), buffer.remaining(), pOuts, pOuts + 4, pOuts + 8) != 0;
            }
            if (!success) {
                throw new IOException("Failed to decode image: " + STBImage.stbi_failure_reason());
            }
            if (ioe != null && ioe[0] != null) {
                throw new IOException("Failed to read image", ioe[0]);
            }
            int width = memGetInt(pOuts),
                    height = memGetInt(pOuts + 4),
                    channels = memGetInt(pOuts + 8);
            Bitmap.Format format = Bitmap.Format.of(channels);
            opts.outWidth = width;
            opts.outHeight = height;
            opts.outFormat = format;
            opts.outColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
        }
    }
}
