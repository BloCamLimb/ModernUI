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

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.stream.Collectors;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Represents a bitmap whose image data is in native. It is used for operations
 * on the application side, such as read from or write to stream. Compared with
 * {@link icyllis.modernui.graphics.Image}, this data is completely stored in
 * RAM rather than in GPU memory.
 */
@SuppressWarnings("unused")
public final class Bitmap implements AutoCloseable {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

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
     * @param init   {@code true} to initialize pixels data to 0, or just allocate memory
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

    private Bitmap(@Nonnull Format format, int width, int height, @Nonnull ByteBuffer data) throws IOException {
        if (data.capacity() != format.channels() * width * height) {
            throw new IOException("Not tightly packed");
        }
        mFormat = format;
        mWidth = width;
        mHeight = height;
        mPixels = MemoryUtil.memAddress(data);
        mFromSTB = true;
    }

    /**
     * Display a file open dialog to select an supported image file.
     *
     * @return the path or {@code null} if selects nothing
     */
    @Nullable
    public static String getOpenDialog() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = SaveFormat.getAllFilters(stack);
            // for multiple selects, s.split("\\|")
            return TinyFileDialogs.tinyfd_openFileDialog(null, null,
                    filters, SaveFormat.getAllDescription(), false);
        }
    }

    /**
     * Display a file save dialog to select the path to save this bitmap.
     *
     * @param format the format used as a file filter
     * @return the path or {@code null} if selects nothing
     */
    @Nullable
    public static String getSaveDialog(@Nonnull SaveFormat format) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = format.getFilters(stack);
            return TinyFileDialogs.tinyfd_saveFileDialog(null,
                    format.getFileName(), filters, format.getDescription());
        }
    }

    /**
     * Opens a file select dialog, then decodes the selected file and creates a bitmap.
     *
     * @param format the format to convert to, or {@code null} to use format in file
     * @return a bitmap or {@code null} if selects nothing
     */
    @Nullable
    public static Bitmap openDialog(@Nullable Format format) throws IOException {
        String path = getOpenDialog();
        if (path != null) {
            try (InputStream stream = new FileInputStream(path)) {
                // not to close bitmap but the stream
                return decode(format, stream);
            }
        }
        return null;
    }

    /**
     * Decodes an image from input stream. This method doesn't close input stream.
     *
     * @param format the format to convert to, or {@code null} to use format in file
     * @param stream input stream
     */
    @Nonnull
    public static Bitmap decode(@Nullable Format format, @Nonnull InputStream stream) throws IOException {
        ByteBuffer buffer = null;
        try {
            buffer = RenderCore.readRawBuffer(stream);
            buffer.rewind();
            return decode(format, buffer);
        } finally {
            if (buffer != null)
                MemoryUtil.memFree(buffer);
        }
    }

    // this method doesn't close/free the buffer
    @Nonnull
    public static Bitmap decode(@Nullable Format format, @Nonnull ByteBuffer buffer) throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            ByteBuffer data = STBImage.stbi_load_from_memory(buffer, width, height, channels,
                    format == null ? 0 : format.channels());
            if (data == null) {
                throw new IOException("Failed to read image: " + STBImage.stbi_failure_reason());
            }
            return new Bitmap(format == null ? Format.of(channels.get(0)) : format, width.get(0), height.get(0), data);
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
     * Save this bitmap image to specified path as specified format. This will
     * open a save dialog to select the path.
     *
     * @param format  the format of the saved image
     * @param quality the compress quality, 1-100, only work for JPEG format.
     */
    public void saveDialog(@Nonnull SaveFormat format, int quality) throws IOException {
        String path = getSaveDialog(format);
        if (path != null) {
            saveToPath(Paths.get(path), format, quality);
        }
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
        try (final ByteChannel channel = Files.newByteChannel(path, EnumSet.of(
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING))) {
            final IOException[] exception = new IOException[1];
            try (STBIWriteCallback func = STBIWriteCallback.create((context, data, size) -> {
                try {
                    channel.write(STBIWriteCallback.getData(data, size));
                } catch (IOException e) {
                    exception[0] = e;
                }
            })) {
                final boolean success = format.write(func, mWidth, mHeight, mFormat, mPixels, quality);
                if (success) {
                    if (exception[0] != null) {
                        throw new IOException("An error occurred while saving image to the path \"" +
                                path.toAbsolutePath() + "\"", exception[0]);
                    }
                } else {
                    throw new IOException("Failed to save image to the path \"" +
                            path.toAbsolutePath() + "\": " + STBImage.stbi_failure_reason());
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
     * Describes the number of channels/components in memory.
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
        PNG("*.png") {
            @Override
            protected boolean write(@Nonnull Pointer func, int width, int height, @Nonnull Format format,
                                    long data, int quality) {
                // leave stride as 0, it will use (width * channels)
                return STBImageWrite.nstbi_write_png_to_func(func.address(),
                        NULL, width, height, format.channels(), data, 0) != 0;
            }
        },

        /**
         * Save as the TGA format. TGA is lossless and compressed, and {@code quality}
         * is ignored.
         */
        TGA("*.tga", "*.vda", "*.icb", "*.vst") {
            @Override
            protected boolean write(@Nonnull Pointer func, int width, int height, @Nonnull Format format,
                                    long data, int quality) {
                return STBImageWrite.nstbi_write_tga_to_func(func.address(),
                        NULL, width, height, format.channels(), data) != 0;
            }
        },

        /**
         * Save as the BMP format. BMP is lossless but almost uncompressed, so it takes
         * up a lot of space, and {@code quality} is ignored as well.
         */
        BMP("*.bmp", "*.dib") {
            @Override
            protected boolean write(@Nonnull Pointer func, int width, int height, @Nonnull Format format,
                                    long data, int quality) {
                return STBImageWrite.nstbi_write_bmp_to_func(func.address(),
                        NULL, width, height, format.channels(), data) != 0;
            }
        },

        /**
         * Save as the JPEG baseline format. {@code quality} of {@code 1} means
         * compress for the smallest size. {@code 100} means compress for max
         * visual quality. The file extension can be {@code .jpg}.
         */
        JPEG("*.jpg", "*.jpeg", "*.jpe") {
            @Override
            protected boolean write(@Nonnull Pointer func, int width, int height, @Nonnull Format format,
                                    long data, int quality) {
                if (quality < 1)
                    quality = 1;
                else if (quality > 120)
                    quality = 120;
                return STBImageWrite.nstbi_write_jpg_to_func(func.address(),
                        NULL, width, height, format.channels(), data, quality) != 0;
            }
        };

        @Nonnull
        private final String[] filters;

        SaveFormat(@Nonnull String... filters) {
            this.filters = filters;
        }

        protected boolean write(@Nonnull Pointer func, int width, int height, @Nonnull Format format,
                                long data, int quality) throws IOException {
            throw new IOException("Unsupported save format");
        }

        @Nonnull
        private static PointerBuffer getAllFilters(@Nonnull MemoryStack stack) {
            int length = 0;
            for (SaveFormat format : values()) {
                length += format.filters.length;
            }
            PointerBuffer buffer = stack.mallocPointer(length);
            for (SaveFormat format : values()) {
                for (String filter : format.filters) {
                    stack.nUTF8Safe(filter, true);
                    buffer.put(stack.getPointerAddress());
                }
            }
            return buffer.rewind();
        }

        @Nonnull
        private static String getAllDescription() {
            return "Images (" + Arrays.stream(values()).flatMap(f -> Arrays.stream(f.filters))
                    .sorted().collect(Collectors.joining(";")) + ")";
        }

        @Nonnull
        private PointerBuffer getFilters(@Nonnull MemoryStack stack) {
            PointerBuffer buffer = stack.mallocPointer(filters.length);
            for (String filter : filters) {
                stack.nUTF8Safe(filter, true);
                buffer.put(stack.getPointerAddress());
            }
            return buffer.rewind();
        }

        @Nonnull
        private String getFileName() {
            return DATE_FORMAT.format(new Date()) + filters[0].substring(1);
        }

        @Nonnull
        private String getDescription() {
            return name() + " (" + String.join(", ", filters) + ")";
        }
    }
}
