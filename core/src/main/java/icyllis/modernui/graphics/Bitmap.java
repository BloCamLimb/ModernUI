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

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.*;
import icyllis.modernui.core.Core;
import icyllis.modernui.graphics.opengl.GLFramebufferCompat;
import icyllis.modernui.graphics.opengl.GLTextureCompat;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Date;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;

import static icyllis.modernui.graphics.opengl.GLCore.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Describes a 2D raster image, with its pixels in native memory. It is used for
 * operations totally on the CPU side, such as decoding or encoding. Compared
 * with {@link Image}, Bitmap data is completely backed by CPU memory with an
 * uncompressed format.
 */
@SuppressWarnings("unused")
public final class Bitmap extends Pixmap implements AutoCloseable {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

    @NonNull
    private final Format mFormat;

    private Ref mRef;

    private Bitmap(@NonNull Format format, @NonNull ImageInfo info, long addr, int rowStride,
                   @NonNull LongConsumer freeFn) {
        super(info, addr, rowStride);
        mFormat = format;
        mRef = new SafeRef(this, info.width(), info.height(), addr, rowStride, freeFn);
    }

    /**
     * Creates a bitmap and its allocation, the type of all components is unsigned byte.
     *
     * @param width  width in pixels, ranged from 1 to 32768
     * @param height height in pixels, ranged from 1 to 32768
     * @param format number of channels
     * @throws IllegalArgumentException width or height out of range
     */
    @NonNull
    public static Bitmap createBitmap(@Size(min = 1, max = 32768) int width,
                                      @Size(min = 1, max = 32768) int height,
                                      @NonNull Format format) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions " + width + "x" + height
                    + " must be positive");
        }
        if (width > 32768 || height > 32768) {
            throw new IllegalArgumentException("Image dimensions " + width + "x" + height
                    + " must be less than or equal to 32768");
        }
        long size = (long) format.channels * width * height;
        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Image allocation size " + size
                    + " must be less than or equal to 2147483647 bytes");
        }
        final long address = nmemCalloc(size, 1);
        if (address == NULL) {
            throw new OutOfMemoryError("Failed to allocate " + size + " bytes");
        }
        return new Bitmap(format,
                ImageInfo.make(width, height, format.colorType, ImageInfo.AT_PREMUL,
                        ColorSpace.get(ColorSpace.Named.SRGB)),
                address, format.channels * width, MemoryUtil::nmemFree);
    }

    /**
     * Display a file open dialog to select a supported image file.
     *
     * @return the path or {@code null} if selects nothing
     */
    @Nullable
    public static String openDialogGet() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = SaveFormat.getAllFilters(stack);
            return TinyFileDialogs.tinyfd_openFileDialog(null, null,
                    filters, SaveFormat.getAllDescription(), false);
        }
    }

    /**
     * Display a file open dialog to select multiple supported image files.
     *
     * @return the paths or {@code null} if selects nothing
     */
    @Nullable
    public static String[] openDialogGets() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = SaveFormat.getAllFilters(stack);
            String s = TinyFileDialogs.tinyfd_openFileDialog(null, null,
                    filters, SaveFormat.getAllDescription(), true);
            return s == null ? null : s.split("\\|");
        }
    }

    /**
     * Display a file save dialog to select the path to save this bitmap.
     *
     * @param format the format used as a file filter
     * @param name   the file name without extension name
     * @return the path or {@code null} if selects nothing
     */
    @Nullable
    public static String saveDialogGet(@NonNull SaveFormat format, @Nullable String name) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = format.getFilters(stack);
            return TinyFileDialogs.tinyfd_saveFileDialog(null,
                    format.getFileName(name), filters, format.getDescription());
        }
    }

    /**
     * Opens a file select dialog, then decodes the selected file and creates a bitmap,
     * using format in file.
     *
     * @return a bitmap or {@code null} if selects nothing
     */
    @Nullable
    public static Bitmap openDialog() throws IOException {
        return openDialog(null);
    }

    /**
     * Opens a file select dialog, then decodes the selected file and creates a bitmap.
     *
     * @param format the format to convert to, or {@code null} to use format in file
     * @return a bitmap or {@code null} if selects nothing
     */
    @Nullable
    public static Bitmap openDialog(@Nullable Format format) throws IOException {
        String path = openDialogGet();
        if (path != null) {
            try (FileChannel channel = FileChannel.open(Path.of(path), StandardOpenOption.READ)) {
                return decode(format, channel);
            }
        }
        return null;
    }

    /**
     * Creates a bitmap whose image downloaded from the given texture. The image of
     * the level-of-detail 0 will be taken.
     *
     * @param format  the bitmap format to convert the image to
     * @param texture the texture to download from
     * @param flipY   flip the image vertically, such as the texture is from a framebuffer
     * @return the created bitmap
     */
    @NonNull
    @RenderThread
    public static Bitmap download(@NonNull Format format, @NonNull GLTextureCompat texture, boolean flipY) {
        Core.checkRenderThread();
        final int width = texture.getWidth();
        final int height = texture.getHeight();
        final Bitmap bitmap = createBitmap(width, height, format);
        final long addr = bitmap.getPixels();
        glPixelStorei(GL_PACK_ROW_LENGTH, 0);
        glPixelStorei(GL_PACK_SKIP_ROWS, 0);
        glPixelStorei(GL_PACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        glGetTextureImage(texture.get(), 0, format.externalGlFormat, GL_UNSIGNED_BYTE,
                bitmap.getSize(), addr);
        if (flipY) {
            final int stride = width * format.channels;
            final long temp = nmemAllocChecked(stride);
            for (int i = 0, e = height >> 1; i < e; i++) {
                final int a = i * stride;
                final int b = (height - i - 1) * stride;
                memCopy(addr + a, temp, stride);
                memCopy(addr + b, addr + a, stride);
                memCopy(temp, addr + b, stride);
            }
            nmemFree(temp);
        }
        return bitmap;
    }

    /**
     * Creates a bitmap whose image downloaded from the given off-screen rendering target
     * bound as a read framebuffer.
     *
     * @param format      the bitmap format to convert the image to
     * @param framebuffer the framebuffer to download from
     * @param colorBuffer the color attachment to read
     * @param flipY       flip the image vertically, such as the texture is from a framebuffer
     * @return the created bitmap
     */
    @NonNull
    @RenderThread
    public static Bitmap download(@NonNull Format format, @NonNull GLFramebufferCompat framebuffer,
                                  int colorBuffer, boolean flipY) {
        Core.checkRenderThread();
        if (framebuffer.isMultisampled()) {
            throw new IllegalArgumentException("Cannot get pixels from a multisampling target");
        }
        final GLFramebufferCompat.Attachment attachment = framebuffer.getAttachment(colorBuffer);
        final int width = attachment.getWidth();
        final int height = attachment.getHeight();
        final Bitmap bitmap = createBitmap(width, height, format);
        final long p = bitmap.getPixels();
        framebuffer.bindRead();
        framebuffer.setReadBuffer(colorBuffer);
        glPixelStorei(GL_PACK_ROW_LENGTH, 0);
        glPixelStorei(GL_PACK_SKIP_ROWS, 0);
        glPixelStorei(GL_PACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        glReadPixels(0, 0, width, height, format.externalGlFormat, GL_UNSIGNED_BYTE, p);
        if (flipY) {
            final int stride = width * format.channels;
            final long temp = nmemAllocChecked(stride);
            for (int i = 0, e = height >> 1; i < e; i++) {
                final int a = i * stride;
                final int b = (height - i - 1) * stride;
                memCopy(p + a, temp, stride);
                memCopy(p + b, p + a, stride);
                memCopy(temp, p + b, stride);
            }
            nmemFree(temp);
        }
        return bitmap;
    }

    /**
     * Decodes an image from channel. This method closes the channel automatically.
     *
     * @param format  the format to convert to, or {@code null} to use format in file
     * @param channel input channel
     */
    @NonNull
    public static Bitmap decode(@Nullable Format format, @NonNull ReadableByteChannel channel) throws IOException {
        ByteBuffer p = null;
        try (channel) {
            p = Core.readBuffer(channel);
            return decode(format, p.rewind());
        } finally {
            memFree(p);
        }
    }

    /**
     * Decodes an image from input stream. This method closes the input stream automatically.
     *
     * @param format the format to convert to, or {@code null} to use format in file
     * @param stream input stream
     */
    @NonNull
    public static Bitmap decode(@Nullable Format format, @NonNull InputStream stream) throws IOException {
        ByteBuffer p = null;
        try (stream) {
            p = Core.readBuffer(stream);
            return decode(format, p.rewind());
        } finally {
            memFree(p);
        }
    }

    // this method doesn't close/free the buffer
    @NonNull
    public static Bitmap decode(@Nullable Format format, @NonNull ByteBuffer buffer) throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long pWidth = stack.nmalloc(4, 4),
                    pHeight = stack.nmalloc(4, 4),
                    pChannels = stack.nmalloc(4, 4);
            long address = STBImage.nstbi_load_from_memory(
                    memAddress(buffer), buffer.remaining(), pWidth, pHeight, pChannels,
                    format == null ? STBImage.STBI_default : format.channels);
            if (address == NULL) {
                throw new IOException("Failed to read image: " + STBImage.stbi_failure_reason());
            }
            int width = memGetInt(pWidth),
                    height = memGetInt(pHeight),
                    channels = memGetInt(pChannels);
            assert format == null || Format.of(channels) == format;
            if (format == null) {
                format = Format.of(channels);
            }
            return new Bitmap(format,
                    ImageInfo.make(width, height, format.colorType, ImageInfo.AT_UNPREMUL,
                            ColorSpace.get(ColorSpace.Named.SRGB)),
                    address, format.channels * width, STBImage::nstbi_image_free);
        }
    }

    @NonNull
    public Format getFormat() {
        return mFormat;
    }

    public int getChannels() {
        return mFormat.channels;
    }

    /**
     * Describes the format for OpenGL uploading.
     *
     * @return pixels format in OpenGL
     */
    public int getExternalGlFormat() {
        return mFormat.externalGlFormat;
    }

    /**
     * Describes the internal format for OpenGL texture allocation.
     *
     * @return internal format in OpenGL
     */
    public int getInternalGlFormat() {
        return mFormat.internalGlFormat;
    }

    public int getWidth() {
        return mRef.mWidth;
    }

    public int getHeight() {
        return mRef.mHeight;
    }

    public int getSize() {
        return mFormat.channels * getWidth() * getHeight();
    }

    /**
     * The base address of {@code unsigned char *pixels} in native.
     *
     * @return the pointer of pixel data, or NULL if released
     */
    public long getPixels() {
        if (mRef != null) {
            return mRef.mPixels;
        }
        return NULL;
    }

    public int getRowStride() {
        return mRef.mRowStride;
    }

    /**
     * The ref of current pixel data, which may be shared across instances.
     * Calling this method won't affect the ref cnt.
     *
     * @return the ref of pixel data, or null if released
     */
    @SharedPtr
    public Ref getRef() {
        return mRef;
    }

    /**
     * Save this bitmap to specified path as specified format. This will
     * open a save dialog to select the path, with a quality of 100 for JPEG format.
     *
     * @param format the format of the saved image
     * @return true if selected a path, otherwise canceled
     */
    public boolean saveDialog(@NonNull SaveFormat format) throws IOException {
        return saveDialog(format, null, 100);
    }

    /**
     * Save this bitmap to specified path as specified format. This will
     * open a save dialog to select the path, with a default quality for JPEG format.
     *
     * @param format the format of the saved image
     * @param name   the file name without extension name
     * @return true if selected a path, otherwise canceled
     */
    public boolean saveDialog(@NonNull SaveFormat format, @Nullable String name) throws IOException {
        return saveDialog(format, name, 0);
    }

    /**
     * Save this bitmap to specified path as specified format. This will
     * open a save dialog to select the path.
     *
     * @param format  the format of the saved image
     * @param name    the file name without extension name
     * @param quality the compress quality, 0-100, only work for JPEG format.
     * @return true if selected a path, otherwise canceled
     */
    public boolean saveDialog(@NonNull SaveFormat format, @Nullable String name, int quality) throws IOException {
        String path = saveDialogGet(format, name);
        if (path != null) {
            saveToPath(Path.of(path), format, quality);
            return true;
        }
        return false;
    }

    /**
     * Save this bitmap to specified path with specified format.
     *
     * @param path    the image path
     * @param format  the format of the saved image
     * @param quality the compress quality, 0-100, only work for JPEG format.
     * @throws IOException saving is not successful
     */
    public void saveToPath(@NonNull Path path, @NonNull SaveFormat format, int quality) throws IOException {
        checkReleased();
        if (quality < 0 || quality > 100) {
            throw new IllegalArgumentException("Bad quality " + quality);
        }
        final var exception = new IOException[1];
        final var channel = FileChannel.open(path,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        final var func = STBIWriteCallback.create((__, data, size) -> {
            try {
                channel.write(STBIWriteCallback.getData(data, size));
            } catch (IOException e) {
                exception[0] = e;
            }
        });
        try (channel; func) {
            final boolean success = format.write(func, mRef.mWidth, mRef.mHeight, mFormat, mRef.mPixels, quality);
            if (success) {
                if (exception[0] != null) {
                    throw new IOException("An error occurred while saving image to the path \"" +
                            path.toAbsolutePath() + "\"", exception[0]);
                }
            } else {
                throw new IOException("Failed to encode image to the path \"" +
                        path.toAbsolutePath() + "\": " + STBImage.stbi_failure_reason());
            }
        }
    }

    /**
     * Save this bitmap to specified channel with specified format. The channel
     * will NOT be closed by this method.
     *
     * @param channel the channel
     * @param format  the format of the saved image
     * @param quality the compress quality, 0-100, only work for JPEG format.
     * @throws IOException saving is not successful
     */
    public void saveToChannel(@NonNull WritableByteChannel channel,
                              @NonNull SaveFormat format, int quality) throws IOException {
        checkReleased();
        if (quality < 0 || quality > 100) {
            throw new IllegalArgumentException("Bad quality " + quality);
        }
        final var exception = new IOException[1];
        final var func = STBIWriteCallback.create((__, data, size) -> {
            try {
                channel.write(STBIWriteCallback.getData(data, size));
            } catch (IOException e) {
                exception[0] = e;
            }
        });
        try (func) {
            final boolean success = format.write(func, mRef.mWidth, mRef.mHeight, mFormat, mRef.mPixels, quality);
            if (success) {
                if (exception[0] != null) {
                    throw new IOException("An error occurred while saving image", exception[0]);
                }
            } else {
                throw new IOException("Failed to encode image: " + STBImage.stbi_failure_reason());
            }
        }
    }

    private void checkReleased() {
        if (mRef == null) {
            throw new IllegalStateException("Cannot operate released bitmap");
        }
    }

    @Override
    public void close() {
        if (mRef != null) {
            mRef.safeClean();
            mRef = null;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Bitmap{" +
                "format=" + mFormat +
                ", info=" + mInfo +
                ", ref=" + mRef +
                '}';
    }

    /**
     * Describes the number of channels/components in memory.
     */
    public enum Format {
        // STBI_grey       = 1,
        // STBI_grey_alpha = 2,
        // STBI_rgb        = 3,
        // STBI_rgb_alpha  = 4;
        GRAY_8       (1, GL_RED , GL_R8   , ImageInfo.CT_GRAY_8       ),
        GRAY_ALPHA_88(2, GL_RG  , GL_RG8  , ImageInfo.CT_GRAY_ALPHA_88),
        RGB_888      (3, GL_RGB , GL_RGB8 , ImageInfo.CT_RGB_888      ),
        RGBA_8888    (4, GL_RGBA, GL_RGBA8, ImageInfo.CT_RGBA_8888    );

        private static final Format[] VALUES = values();

        public final int channels;
        public final int externalGlFormat;
        public final int internalGlFormat;
        public final int colorType;

        Format(int channels, int externalGlFormat, int internalGlFormat, int colorType) {
            this.channels = channels;
            this.externalGlFormat = externalGlFormat;
            this.internalGlFormat = internalGlFormat;
            this.colorType = colorType;
            assert ordinal() == channels - 1;
        }

        @NonNull
        public static Format of(int channels) {
            if (channels < 1 || channels > 4) {
                throw new IllegalArgumentException("Specified channels should be ranged from 1 to 4 but " + channels);
            }
            return VALUES[channels - 1];
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
            public boolean write(@NonNull STBIWriteCallbackI func, int width, int height, @NonNull Format format,
                                 long data, int quality) {
                // leave stride as 0, it will use (width * channels)
                return STBImageWrite.nstbi_write_png_to_func(func.address(),
                        NULL, width, height, format.channels, data, 0) != 0;
            }
        },

        /**
         * Save as the TGA format. TGA is lossless and compressed, and {@code quality}
         * is ignored.
         */
        TGA("*.tga", "*.vda", "*.icb", "*.vst") {
            @Override
            public boolean write(@NonNull STBIWriteCallbackI func, int width, int height, @NonNull Format format,
                                 long data, int quality) {
                return STBImageWrite.nstbi_write_tga_to_func(func.address(),
                        NULL, width, height, format.channels, data) != 0;
            }
        },

        /**
         * Save as the BMP format. BMP is lossless but almost uncompressed, so it takes
         * up a lot of space, and {@code quality} is ignored as well.
         */
        BMP("*.bmp", "*.dib") {
            @Override
            public boolean write(@NonNull STBIWriteCallbackI func, int width, int height, @NonNull Format format,
                                 long data, int quality) {
                return STBImageWrite.nstbi_write_bmp_to_func(func.address(),
                        NULL, width, height, format.channels, data) != 0;
            }
        },

        /**
         * Save as the JPEG baseline format. {@code quality} of {@code 1} means
         * compress for the smallest size. {@code 100} means compress for max
         * visual quality. The file extension can be {@code .jpg}.
         */
        JPEG("*.jpg", "*.jpeg", "*.jpe", "*.jfif", "*.jif") {
            @Override
            public boolean write(@NonNull STBIWriteCallbackI func, int width, int height, @NonNull Format format,
                                 long data, int quality) {
                return STBImageWrite.nstbi_write_jpg_to_func(func.address(),
                        NULL, width, height, format.channels, data, quality) != 0;
            }
        };

        private static final SaveFormat[] VALUES = values();

        @NonNull
        private final String[] filters;

        SaveFormat(@NonNull String... filters) {
            this.filters = filters;
        }

        public abstract boolean write(@NonNull STBIWriteCallbackI func, int width, int height,
                                      @NonNull Format format, long data, int quality) throws IOException;

        @NonNull
        private static PointerBuffer getAllFilters(@NonNull MemoryStack stack) {
            int length = 0;
            for (SaveFormat format : VALUES) {
                length += format.filters.length;
            }
            PointerBuffer buffer = stack.mallocPointer(length);
            for (SaveFormat format : VALUES) {
                for (String filter : format.filters) {
                    stack.nUTF8(filter, true);
                    buffer.put(stack.getPointerAddress());
                }
            }
            return buffer.rewind();
        }

        @NonNull
        private static String getAllDescription() {
            return "Images (" + Arrays.stream(VALUES).flatMap(f -> Arrays.stream(f.filters))
                    .sorted().collect(Collectors.joining(";")) + ")";
        }

        @NonNull
        private PointerBuffer getFilters(@NonNull MemoryStack stack) {
            PointerBuffer buffer = stack.mallocPointer(filters.length);
            for (String filter : filters) {
                stack.nUTF8(filter, true);
                buffer.put(stack.getPointerAddress());
            }
            return buffer.rewind();
        }

        @NonNull
        private String getFileName(@Nullable String name) {
            return (name != null ? name : "image-" + DATE_FORMAT.format(new Date()))
                    + filters[0].substring(1);
        }

        @NonNull
        private String getDescription() {
            return name() + " (" + String.join(", ", filters) + ")";
        }
    }

    /**
     * This class is the smart container for pixel memory, and is used with {@link Bitmap}.
     * <br>This class can be shared/accessed between multiple threads.
     */
    public static class Ref extends RefCnt {

        private final int mWidth;
        private final int mHeight;
        private final long mPixels;
        private final int mRowStride;
        @NonNull
        private final LongConsumer mFreeFn;

        private Ref(int width, int height, long pixels, int rowStride,
                    @NonNull LongConsumer freeFn) {
            mWidth = width;
            mHeight = height;
            mPixels = pixels;
            mRowStride = rowStride;
            mFreeFn = freeFn;
        }

        // used with Bitmap
        void safeClean() {
            unref();
        }

        @Override
        protected void dispose() {
            mFreeFn.accept(mPixels);
        }

        public int getWidth() {
            return mWidth;
        }

        public int getHeight() {
            return mHeight;
        }

        /**
         * @return the address whether freed or not
         */
        public long getPixels() {
            return mPixels;
        }

        public int getRowStride() {
            return mRowStride;
        }

        @NonNull
        @Override
        public String toString() {
            return "PixelRef{" +
                    "address=0x" + Long.toHexString(mPixels) +
                    ", dimensions=" + mWidth + "x" + mHeight +
                    ", rowStride=" + mRowStride +
                    '}';
        }
    }

    // this ensures unref being called when Bitmap become phantom-reachable
    // but never called to close
    private static class SafeRef extends Ref implements Runnable {

        private final Cleaner.Cleanable mCleanup;

        private SafeRef(@NonNull Bitmap owner, int width, int height,
                        long pixels, int rowStride, @NonNull LongConsumer freeFn) {
            super(width, height, pixels, rowStride, freeFn);
            mCleanup = ModernUI.registerCleanup(owner, this);
        }

        @Override
        void safeClean() {
            mCleanup.clean();
        }

        @Override
        public void run() {
            unref();
        }
    }
}
