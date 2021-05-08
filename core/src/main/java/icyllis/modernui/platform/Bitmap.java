/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.platform;

import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Represents a bitmap whose image data is in native. It is used for operations
 * on the application side, such as read from or write to stream. Compared with
 * {@link icyllis.modernui.graphics.Image}, this data is completely stored in
 * RAM rather than in GPU memory.
 */
public final class Bitmap implements AutoCloseable {

    @Nonnull
    private final Format mFormat;

    private final int mWidth;
    private final int mHeight;

    private long mPixels;
    private final boolean mFromSTB;

    /**
     * Creates a bitmap, the type of all components is unsigned byte.
     *
     * @param format channels
     * @param width  width in pixels
     * @param height height in pixels
     * @param init   {@code true} to initialize pixels data to 0
     * @throws IllegalArgumentException width or height is less than or equal to zero
     */
    public Bitmap(@Nonnull Format format, int width, int height, boolean init) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Bitmap size must be positive");
        }
        mFormat = format;
        mWidth = width;
        mHeight = height;
        mFromSTB = false;
        final long size = (long) format.channels() * width * height;
        if (init) {
            mPixels = MemoryUtil.nmemCallocChecked(1L, size);
        } else {
            mPixels = MemoryUtil.nmemAllocChecked(size);
        }
    }

    private Bitmap(@Nonnull Format format, int width, int height, long pixels) {
        mFormat = format;
        mWidth = width;
        mHeight = height;
        mPixels = pixels;
        mFromSTB = true;
    }

    /**
     * Read an image from input stream. This method doesn't close input stream.
     *
     * @param format the format to convert to, or {@code null} to use file format
     * @param stream input stream
     */
    @Nonnull
    public static Bitmap read(@Nullable Format format, @Nonnull InputStream stream) throws IOException {
        ByteBuffer buffer = null;
        try {
            buffer = RenderCore.readRawBuffer(stream);
            buffer.rewind();
            return read(format, buffer);
        } finally {
            if (buffer != null)
                MemoryUtil.memFree(buffer);
        }
    }

    // this method doesn't close/free the buffer
    @Nonnull
    public static Bitmap read(@Nullable Format format, @Nonnull ByteBuffer buffer) throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            ByteBuffer data = STBImage.stbi_load_from_memory(buffer, width, height, channels,
                    format == null ? 0 : format.channels());
            if (data == null) {
                throw new IOException("Failed to read image: " + STBImage.stbi_failure_reason());
            }
            return new Bitmap(format == null ? Format.of(channels.get(0)) : format,
                    width.get(0), height.get(0), MemoryUtil.memAddress(data));
        }
    }

    @Nonnull
    public Format getFormat() {
        return mFormat;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    /**
     * The head address of {@code unsigned char *pixels}
     *
     * @return the pointer of pixels data
     */
    public long getPixels() {
        return mPixels;
    }

    /**
     * @see #saveToPath(Path, SaveFormat, int)
     */
    public void saveToPath(@Nonnull Path path, @Nonnull SaveFormat format) throws IOException {
        saveToPath(path, format, 100);
    }

    /**
     * Save this bitmap image to specified path with specified format.
     *
     * @param path    the image path
     * @param format  the format of the saved image
     * @param quality the compress quality, 1-100, only work for JPEG format.
     */
    public void saveToPath(@Nonnull Path path, @Nonnull SaveFormat format, int quality) throws IOException {
        checkReleased();
        try (ByteChannel channel = Files.newByteChannel(path, EnumSet.of(
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING))) {
            final IOException[] exception = new IOException[1];
            try (STBIWriteCallback callback = STBIWriteCallback.create((context, data, size) -> {
                try {
                    channel.write(STBIWriteCallback.getData(data, size));
                } catch (IOException e) {
                    exception[0] = e;
                }
            })) {
                final boolean success;
                switch (format) {
                    case PNG:
                        success = STBImageWrite.nstbi_write_png_to_func(callback.address(),
                                NULL, mWidth, mHeight, mFormat.channels(), mPixels, 0) != 0;
                        break;
                    case TGA:
                        success = STBImageWrite.nstbi_write_tga_to_func(callback.address(),
                                NULL, mWidth, mHeight, mFormat.channels(), mPixels) != 0;
                        break;
                    case BMP:
                        success = STBImageWrite.nstbi_write_bmp_to_func(callback.address(),
                                NULL, mWidth, mHeight, mFormat.channels(), mPixels) != 0;
                        break;
                    case JPEG:
                        success = STBImageWrite.nstbi_write_jpg_to_func(callback.address(),
                                NULL, mWidth, mHeight, mFormat.channels(), mPixels, quality) != 0;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown save format " + format);
                }
                if (success) {
                    if (exception[0] != null) {
                        throw new IOException("Failed to save image to the path \"" + path.toAbsolutePath() + "\"", exception[0]);
                    }
                } else {
                    throw new IOException("Failed to save image to the path \"" + path.toAbsolutePath() + "\": " + STBImage.stbi_failure_reason());
                }
            }
        }
    }

    private void checkReleased() {
        if (mPixels == NULL) {
            throw new IllegalStateException("Cannot operate released bitmap");
        }
    }

    /**
     * Frees native image data.
     */
    public void release() {
        if (mPixels != NULL) {
            if (mFromSTB) {
                STBImage.nstbi_image_free(mPixels);
            } else {
                MemoryUtil.nmemFree(mPixels);
            }
            mPixels = NULL;
        }
    }

    @Override
    public void close() throws Exception {
        release();
    }

    /**
     * Describes the number of channels/components.
     */
    public enum Format {
        R(1),
        RG(2),
        RGB(3),
        RGBA(4);

        private final int channels;

        Format(int channels) {
            this.channels = channels;
        }

        /**
         * @return the number of channels or components
         */
        public final int channels() {
            return channels;
        }

        @Nonnull
        public static Format of(int channels) {
            if (channels < 1 || channels > 4) {
                throw new IllegalArgumentException("Specified channels ranged from 1 to 4");
            }
            return values()[channels - 1];
        }
    }

    /**
     * Lists supported formats a bitmap can be saved as.
     */
    public enum SaveFormat {
        /**
         * Save as the PNG format. PNG is lossless and compressed, and {@code quality}
         * is ignored.
         */
        PNG,

        /**
         * Save as the TGA format. TGA is lossless and compressed, and {@code quality}
         * is ignored.
         */
        TGA,

        /**
         * Save as the BMP format. BMP is lossless but almost uncompressed, so it takes
         * up a lot of space, and {@code quality} is ignored as well.
         */
        BMP,

        /**
         * Save as the JPEG baseline format. {@code quality} of {@code 1} means
         * compress for the smallest size. {@code 100} means compress for max
         * visual quality. The file extension can be {@code .jpg}.
         */
        JPEG
    }
}
