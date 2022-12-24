/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.core;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.FMath;
import icyllis.modernui.graphics.opengl.GLFramebuffer;
import icyllis.modernui.graphics.opengl.GLTexture;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBIWriteCallbackI;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

import static icyllis.modernui.graphics.opengl.GLCore.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Represents a native image, with its image data in native. It is used for operations
 * on the client side, such as reading from/writing to a stream/channel. Compared
 * with {@link Image Images}, this data is completely stored in RAM with an
 * uncompressed format. Losing the reference of a native image object will automatically
 * free the native memory.
 */
@SuppressWarnings("unused")
public final class NativeImage implements AutoCloseable {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    @Nonnull
    private final Format mFormat;

    private final int mWidth;
    private final int mHeight;

    private Ref mRef;

    /**
     * Creates a native image, the type of all components is unsigned byte.
     *
     * @param format number of channels
     * @param width  width in pixels, ranged from 1 to 16384
     * @param height height in pixels, ranged from 1 to 16384
     * @param clear  {@code true} to initialize pixels data to 0, or just allocate memory
     * @throws IllegalArgumentException width or height out of range
     */
    public NativeImage(@Nonnull Format format, int width, int height, boolean clear) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image size must be positive");
        }
        if (width > 16384) {
            throw new IllegalArgumentException("Image width is too large");
        }
        if (height > 16384) {
            throw new IllegalArgumentException("Image height is too large");
        }
        mFormat = format;
        mWidth = width;
        mHeight = height;
        mRef = new Ref(this, format.channels * width * height, clear);
    }

    private NativeImage(@Nonnull Format format, int width, int height, @Nonnull ByteBuffer data) throws IOException {
        if (data.capacity() != format.channels * width * height) {
            throw new IOException("Not tightly packed (should this happen?)");
        }
        mFormat = format;
        mWidth = width;
        mHeight = height;
        mRef = new Ref(this, MemoryUtil.memAddress(data));
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
     * Display a file save dialog to select the path to save this native image.
     *
     * @param format the format used as a file filter
     * @param name   the file name without extension name
     * @return the path or {@code null} if selects nothing
     */
    @Nullable
    public static String saveDialogGet(@Nonnull SaveFormat format, @Nullable String name) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = format.getFilters(stack);
            return TinyFileDialogs.tinyfd_saveFileDialog(null,
                    format.getFileName(name), filters, format.getDescription());
        }
    }

    /**
     * Opens a file select dialog, then decodes the selected file and creates a native image,
     * using format in file.
     *
     * @return a native image or {@code null} if selects nothing
     */
    @Nullable
    public static NativeImage openDialog() throws IOException {
        return openDialog(null);
    }

    /**
     * Opens a file select dialog, then decodes the selected file and creates a native image.
     *
     * @param format the format to convert to, or {@code null} to use format in file
     * @return a native image or {@code null} if selects nothing
     */
    @Nullable
    public static NativeImage openDialog(@Nullable Format format) throws IOException {
        String path = openDialogGet();
        if (path != null) {
            try (FileChannel channel = FileChannel.open(Path.of(path), StandardOpenOption.READ)) {
                return decode(format, channel);
            }
        }
        return null;
    }

    /**
     * Creates a native image whose image downloaded from the given texture. The image of
     * the level-of-detail 0 will be taken.
     *
     * @param format  the native image format to convert the image to
     * @param texture the texture to download from
     * @param flipY   flip the image vertically, such as the texture is from a framebuffer
     * @return the created native image
     */
    @Nonnull
    @RenderThread
    public static NativeImage download(@Nonnull Format format, @Nonnull GLTexture texture, boolean flipY) {
        Core.checkRenderThread();
        final int width = texture.getWidth();
        final int height = texture.getHeight();
        final NativeImage nativeImage = new NativeImage(format, width, height, false);
        final long p = nativeImage.getPixels();
        glPixelStorei(GL_PACK_ROW_LENGTH, 0);
        glPixelStorei(GL_PACK_SKIP_ROWS, 0);
        glPixelStorei(GL_PACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        glGetTextureImage(texture.get(), 0, format.glFormat, GL_UNSIGNED_BYTE,
                nativeImage.getSize(), p);
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
        return nativeImage;
    }

    /**
     * Creates a native image whose image downloaded from the given off-screen rendering target
     * bound as a read framebuffer.
     *
     * @param format      the native image format to convert the image to
     * @param framebuffer the framebuffer to download from
     * @param colorBuffer the color attachment to read
     * @param flipY       flip the image vertically, such as the texture is from a framebuffer
     * @return the created native image
     */
    @Nonnull
    @RenderThread
    public static NativeImage download(@Nonnull Format format, @Nonnull GLFramebuffer framebuffer,
                                       int colorBuffer, boolean flipY) {
        Core.checkRenderThread();
        if (framebuffer.isMultisampled()) {
            throw new IllegalArgumentException("Cannot get pixels from a multisampling target");
        }
        final GLFramebuffer.Attachment attachment = framebuffer.getAttachment(colorBuffer);
        final int width = attachment.getWidth();
        final int height = attachment.getHeight();
        final NativeImage nativeImage = new NativeImage(format, width, height, false);
        final long p = nativeImage.getPixels();
        framebuffer.bindRead();
        framebuffer.setReadBuffer(colorBuffer);
        glPixelStorei(GL_PACK_ROW_LENGTH, 0);
        glPixelStorei(GL_PACK_SKIP_ROWS, 0);
        glPixelStorei(GL_PACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        glReadPixels(0, 0, width, height, format.glFormat, GL_UNSIGNED_BYTE, p);
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
        return nativeImage;
    }

    /**
     * Decodes an image from channel. This method closes the channel automatically.
     *
     * @param format  the format to convert to, or {@code null} to use format in file
     * @param channel input channel
     */
    @Nonnull
    public static NativeImage decode(@Nullable Format format, @Nonnull ReadableByteChannel channel) throws IOException {
        ByteBuffer p = null;
        try (channel) {
            p = Core.readBuffer(channel);
            return decode(format, p.rewind());
        } finally {
            MemoryUtil.memFree(p);
        }
    }

    /**
     * Decodes an image from input stream. This method closes the input stream automatically.
     *
     * @param format the format to convert to, or {@code null} to use format in file
     * @param stream input stream
     */
    @Nonnull
    public static NativeImage decode(@Nullable Format format, @Nonnull InputStream stream) throws IOException {
        ByteBuffer p = null;
        try (stream) {
            p = Core.readBuffer(stream);
            return decode(format, p.rewind());
        } finally {
            MemoryUtil.memFree(p);
        }
    }

    // this method doesn't close/free the buffer
    @Nonnull
    public static NativeImage decode(@Nullable Format format, @Nonnull ByteBuffer buffer) throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            ByteBuffer data = STBImage.stbi_load_from_memory(buffer, width, height, channels,
                    format == null ? 0 : format.channels);
            if (data == null) {
                throw new IOException("Failed to read image: " + STBImage.stbi_failure_reason());
            }
            assert format == null || Format.of(channels.get(0)) == format;
            return new NativeImage(format == null ? Format.of(channels.get(0)) : format,
                    width.get(0), height.get(0), data);
        }
    }

    @Nonnull
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
    public int getGlFormat() {
        return mFormat.glFormat;
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
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getSize() {
        return mFormat.channels * mWidth * mHeight;
    }

    /**
     * The head address of {@code unsigned char *pixels} in native.
     *
     * @return the pointer of pixels data, or NULL if released
     */
    public long getPixels() {
        if (mRef != null) {
            return mRef.mPixels;
        }
        return NULL;
    }

    /**
     * Save this native image to specified path as specified format. This will
     * open a save dialog to select the path, with a quality of 100 for JPEG format.
     *
     * @param format the format of the saved image
     * @return true if selected a path, otherwise canceled
     */
    public boolean saveDialog(@Nonnull SaveFormat format) throws IOException {
        return saveDialog(format, null, 100);
    }

    /**
     * Save this native image to specified path as specified format. This will
     * open a save dialog to select the path, with a quality of 100 for JPEG format.
     *
     * @param format the format of the saved image
     * @param name   the file name without extension name
     * @return true if selected a path, otherwise canceled
     */
    public boolean saveDialog(@Nonnull SaveFormat format, @Nullable String name) throws IOException {
        return saveDialog(format, name, 100);
    }

    /**
     * Save this native image to specified path as specified format. This will
     * open a save dialog to select the path.
     *
     * @param format  the format of the saved image
     * @param name    the file name without extension name
     * @param quality the compress quality, 1-100, only work for JPEG format.
     * @return true if selected a path, otherwise canceled
     */
    public boolean saveDialog(@Nonnull SaveFormat format, @Nullable String name, int quality) throws IOException {
        String path = saveDialogGet(format, name);
        if (path != null) {
            saveToPath(Path.of(path), format, quality);
            return true;
        }
        return false;
    }

    /**
     * Save this native image to specified path with specified format.
     *
     * @param path    the image path
     * @param format  the format of the saved image
     * @param quality the compress quality, 1-100, only work for JPEG format.
     * @throws IOException saving is not successful
     */
    public void saveToPath(@Nonnull Path path, @Nonnull SaveFormat format, int quality) throws IOException {
        checkReleased();
        final IOException[] exception = new IOException[1];
        final FileChannel channel = FileChannel.open(path,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        final STBIWriteCallback func = STBIWriteCallback.create((ctx, data, size) -> {
            try {
                channel.write(STBIWriteCallback.getData(data, size));
            } catch (IOException e) {
                exception[0] = e;
            }
        });
        try (channel; func) {
            final boolean success = format.write(func, mWidth, mHeight, mFormat, mRef.mPixels, quality);
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

    private void checkReleased() {
        if (mRef == null) {
            throw new IllegalStateException("Cannot operate released native image");
        }
    }

    @Override
    public void close() {
        if (mRef != null) {
            mRef.mCleanup.clean();
            mRef = null;
        }
    }

    @Nonnull
    @Override
    public String toString() {
        return "NativeImage{" +
                "mFormat=" + mFormat +
                ", mWidth=" + mWidth +
                ", mHeight=" + mHeight +
                ", mRef=" + mRef +
                '}';
    }

    /**
     * Describes the number of channels/components in memory.
     */
    public enum Format {
        RED(1, GL_RED, GL_R8),
        RG(2, GL_RG, GL_RG8),
        RGB(3, GL_RGB, GL_RGB8),
        RGBA(4, GL_RGBA, GL_RGBA8);

        private static final Format[] VALUES = values();

        public final int channels;
        public final int glFormat;
        public final int internalGlFormat;

        Format(int channels, int glFormat, int internalGlFormat) {
            this.channels = channels;
            this.glFormat = glFormat;
            this.internalGlFormat = internalGlFormat;
        }

        @Nonnull
        public static Format of(int channels) {
            if (channels < 1 || channels > 4) {
                throw new IllegalArgumentException("Specified channels should be ranged from 1 to 4 but " + channels);
            }
            return VALUES[channels - 1];
        }
    }

    /**
     * Lists supported formats a native image can be saved as.
     */
    public enum SaveFormat {
        /**
         * Save as the PNG format. PNG is lossless and compressed, and {@code quality}
         * is ignored.
         */
        PNG("*.png") {
            @Override
            public boolean write(@Nonnull STBIWriteCallbackI func, int width, int height, @Nonnull Format format,
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
            public boolean write(@Nonnull STBIWriteCallbackI func, int width, int height, @Nonnull Format format,
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
            public boolean write(@Nonnull STBIWriteCallbackI func, int width, int height, @Nonnull Format format,
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
            public boolean write(@Nonnull STBIWriteCallbackI func, int width, int height, @Nonnull Format format,
                                 long data, int quality) {
                quality = FMath.clamp(quality, 1, 120);
                return STBImageWrite.nstbi_write_jpg_to_func(func.address(),
                        NULL, width, height, format.channels, data, quality) != 0;
            }
        };

        private static final SaveFormat[] VALUES = values();

        @Nonnull
        private final String[] filters;

        SaveFormat(@Nonnull String... filters) {
            this.filters = filters;
        }

        public abstract boolean write(@Nonnull STBIWriteCallbackI func, int width, int height,
                                      @Nonnull Format format, long data, int quality) throws IOException;

        @Nonnull
        private static PointerBuffer getAllFilters(@Nonnull MemoryStack stack) {
            int length = 0;
            for (SaveFormat format : VALUES) {
                length += format.filters.length;
            }
            PointerBuffer buffer = stack.mallocPointer(length);
            for (SaveFormat format : VALUES) {
                for (String filter : format.filters) {
                    stack.nUTF8Safe(filter, true);
                    buffer.put(stack.getPointerAddress());
                }
            }
            return buffer.rewind();
        }

        @Nonnull
        private static String getAllDescription() {
            return "Images (" + Arrays.stream(VALUES).flatMap(f -> Arrays.stream(f.filters))
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
        private String getFileName(@Nullable String name) {
            return name == null ? DATE_FORMAT.format(new Date()) : name + filters[0].substring(1);
        }

        @Nonnull
        private String getDescription() {
            return name() + " (" + String.join(", ", filters) + ")";
        }
    }

    private static class Ref implements Runnable {

        private final long mPixels;
        private final boolean mFromSTB;
        private final Cleaner.Cleanable mCleanup;

        private Ref(NativeImage owner, int size, boolean init) {
            mFromSTB = false;
            if (init) {
                mPixels = MemoryUtil.nmemCallocChecked(1L, size);
            } else {
                mPixels = MemoryUtil.nmemAllocChecked(size);
            }
            mCleanup = ModernUI.registerCleanup(owner, this);
        }

        private Ref(NativeImage owner, long pointer) {
            mFromSTB = true;
            mPixels = pointer;
            mCleanup = ModernUI.registerCleanup(owner, this);
        }

        @Override
        public void run() {
            if (mFromSTB) {
                STBImage.nstbi_image_free(mPixels);
            } else {
                MemoryUtil.nmemFree(mPixels);
            }
        }

        @Nonnull
        @Override
        public String toString() {
            return "Ref{" +
                    "mPixels=0x" + Long.toHexString(mPixels) +
                    ", mFromSTB=" + mFromSTB +
                    '}';
        }
    }
}
