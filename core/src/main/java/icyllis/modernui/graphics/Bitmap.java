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

import java.io.*;
import java.lang.ref.Cleaner;
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
 * CPU-side operations such as decoding or encoding. It can also be used to
 * upload to GPU-side {@link Image} for drawing on the screen or download from
 * GPU-side {@link Image} for saving to disk.
 * <p>
 * This class is not thread safe, but memory safe.
 *
 * @see BitmapFactory
 */
@SuppressWarnings("unused")
public final class Bitmap extends Pixmap implements AutoCloseable {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

    @NonNull
    private final Format mFormat;

    private Ref mRef;

    Bitmap(@NonNull Format format, @NonNull ImageInfo info, long addr, int rowStride,
           @NonNull LongConsumer freeFn) {
        super(info, addr, rowStride);
        mFormat = format;
        mRef = new SafeRef(this, info.width(), info.height(), addr, rowStride, freeFn);
    }

    /**
     * Creates a bitmap and its allocation, the type of all components is unsigned byte.
     *
     * @param width  width in pixels, ranged from 1 to 32767
     * @param height height in pixels, ranged from 1 to 32767
     * @param format number of channels
     * @throws IllegalArgumentException width or height out of range, or allocation size >= 2GB
     * @throws OutOfMemoryError         out of off-heap memory
     */
    @NonNull
    public static Bitmap createBitmap(@Size(min = 1, max = 32767) int width,
                                      @Size(min = 1, max = 32767) int height,
                                      @NonNull Format format) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions " + width + "x" + height
                    + " must be positive");
        }
        if (width > 32767 || height > 32767) {
            throw new IllegalArgumentException("Image dimensions " + width + "x" + height
                    + " must be less than or equal to 32767");
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
                ImageInfo.make(width, height, format.getColorType(), ImageInfo.AT_PREMUL,
                        ColorSpace.get(ColorSpace.Named.SRGB)),
                address, width * format.getChannelCount(), MemoryUtil::nmemFree);
    }

    /**
     * Display an OS file open dialog to select a supported image file.
     * The dialog will block the current thread until method return.
     *
     * @param format             the specified image file format to open,
     *                           or {@code null} to use all supported formats
     * @param title              the dialog title or {@code null} to use OS default
     * @param defaultPathAndFile the default path and/or file or {@code null} to use OS default
     * @return the selected path or {@code null} if selects nothing (dismissed or closed)
     */
    @Nullable
    public static String openDialogGet(@Nullable SaveFormat format,
                                       @Nullable CharSequence title,
                                       @Nullable CharSequence defaultPathAndFile) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = format != null
                    ? format.getFilters(stack)
                    : SaveFormat.getAllFilters(stack);
            return TinyFileDialogs.tinyfd_openFileDialog(title, defaultPathAndFile,
                    filters, format != null
                            ? format.getDescription()
                            : SaveFormat.getAllDescription(), false);
        }
    }

    /**
     * Display an OS file open dialog to select multiple supported image files.
     * The dialog will block the current thread until method return.
     *
     * @param format             the specified image file format to open,
     *                           or {@code null} to use all supported formats
     * @param title              the dialog title or {@code null} to use OS default
     * @param defaultPathAndFile the default path and/or file or {@code null} to use OS default
     * @return the selected paths or {@code null} if selects nothing (dismissed or closed)
     */
    @Nullable
    public static String[] openDialogGetMulti(@Nullable SaveFormat format,
                                              @Nullable CharSequence title,
                                              @Nullable CharSequence defaultPathAndFile) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = format != null
                    ? format.getFilters(stack)
                    : SaveFormat.getAllFilters(stack);
            String s = TinyFileDialogs.tinyfd_openFileDialog(title, defaultPathAndFile,
                    filters, format != null
                            ? format.getDescription()
                            : SaveFormat.getAllDescription(), true);
            return s != null ? s.split("\\|") : null;
        }
    }

    /**
     * Display an OS file save dialog to select the path to save the bitmap.
     * The dialog will block the current thread until method return.
     *
     * @param format the specified image file format to filter,
     *               or {@code null} to filter all supported formats
     * @param title  the dialog title or {@code null} to use OS default
     * @param name   the file name without extension name
     * @return the path or {@code null} if selects nothing
     */
    @Nullable
    public static String saveDialogGet(@Nullable SaveFormat format,
                                       @Nullable CharSequence title,
                                       @Nullable String name) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = format != null
                    ? format.getFilters(stack)
                    : SaveFormat.getAllFilters(stack);
            return TinyFileDialogs.tinyfd_saveFileDialog(title,
                    SaveFormat.getFileName(format, name), filters, format != null
                            ? format.getDescription()
                            : SaveFormat.getAllDescription());
        }
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
    @Deprecated
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
    @Deprecated
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

    @NonNull
    public Format getFormat() {
        return mFormat;
    }

    public int getChannelCount() {
        return mFormat.channels;
    }

    /**
     * Describes the format for OpenGL uploading.
     *
     * @return pixels format in OpenGL
     */
    @Deprecated
    public int getExternalGlFormat() {
        return mFormat.externalGlFormat;
    }

    /**
     * Describes the internal format for OpenGL texture allocation.
     *
     * @return internal format in OpenGL
     */
    @Deprecated
    public int getInternalGlFormat() {
        return mFormat.internalGlFormat;
    }

    /**
     * Returns the width of the bitmap.
     */
    @Override
    public int getWidth() {
        if (mRef != null) {
            return mRef.mWidth;
        }
        assert false;
        return super.getWidth();
    }

    /**
     * Returns the height of the bitmap.
     */
    @Override
    public int getHeight() {
        if (mRef != null) {
            return mRef.mHeight;
        }
        assert false;
        return super.getHeight();
    }

    public int getSize() {
        return mFormat.channels * getWidth() * getHeight();
    }

    /**
     * The base address of {@code unsigned char *pixels} in native.
     *
     * @return the pointer of pixel data, or NULL if released
     */
    @Override
    public long getPixels() {
        if (mRef != null) {
            return mRef.mPixels;
        }
        return NULL;
    }

    @Override
    public int getRowStride() {
        return mRef.mRowStride;
    }

    /**
     * Returns true if the bitmap's format supports per-pixel alpha, and
     * if the pixels may contain non-opaque alpha values. For some configs,
     * this is always false (e.g. RGB_888), since they do not support per-pixel
     * alpha. However, for formats that do, the bitmap may be flagged to be
     * known that all of its pixels are opaque. In this case hasAlpha() will
     * also return false. If a config such as RGBA_8888 is not so flagged,
     * it will return true by default.
     */
    public boolean hasAlpha() {
        assert mRef != null;
        return !mInfo.isOpaque();
    }

    /**
     * Returns true if the bitmap is marked as immutable.
     */
    public boolean isImmutable() {
        if (mRef != null) {
            return mRef.isImmutable();
        }
        assert false;
        return false;
    }

    /**
     * Marks the Bitmap as immutable. Further modifications to this Bitmap are disallowed.
     * After this method is called, this Bitmap cannot be made mutable again.
     */
    public void setImmutable() {
        if (mRef != null) {
            mRef.setImmutable();
        }
    }

    /**
     * <p>Indicates whether pixels stored in this bitmaps are stored pre-multiplied.
     * When a pixel is pre-multiplied, the RGB components have been multiplied by
     * the alpha component. For instance, if the original color is a 50%
     * translucent red <code>(128, 255, 0, 0)</code>, the pre-multiplied form is
     * <code>(128, 128, 0, 0)</code>.</p>
     *
     * <p>This method only returns true if {@link #hasAlpha()} returns true.
     * A bitmap with no alpha channel can be used both as a pre-multiplied and
     * as a non pre-multiplied bitmap.</p>
     *
     * <p>Only pre-multiplied bitmaps may be drawn by the view system or
     * {@link Canvas}. If a non-pre-multiplied bitmap with an alpha channel is
     * drawn to a Canvas, a RuntimeException will be thrown.</p>
     *
     * @return true if the underlying pixels have been pre-multiplied, false
     * otherwise
     * @see Bitmap#setPremultiplied(boolean)
     * @see BitmapFactory.Options#inPremultiplied
     */
    public boolean isPremultiplied() {
        assert mRef != null;
        return mInfo.alphaType() == ImageInfo.AT_PREMUL;
    }

    /**
     * Sets whether the bitmap should treat its data as pre-multiplied.
     *
     * <p>Bitmaps are always treated as pre-multiplied by the view system and
     * {@link Canvas} for performance reasons. Storing un-pre-multiplied data in
     * a Bitmap (through {@link #setPixel}, {@link #setPixels}, or {@link
     * BitmapFactory.Options#inPremultiplied BitmapFactory.Options.inPremultiplied})
     * can lead to incorrect blending if drawn by the framework.</p>
     *
     * <p>This method will not affect the behavior of a bitmap without an alpha
     * channel, or if {@link #hasAlpha()} returns false.</p>
     *
     * @see Bitmap#isPremultiplied()
     * @see BitmapFactory.Options#inPremultiplied
     */
    public void setPremultiplied(boolean premultiplied) {
        checkReleased();
    }

    private void checkPixelAccess(int x, int y) {
        if (x < 0) {
            throw new IllegalArgumentException("x must be >= 0");
        }
        if (y < 0) {
            throw new IllegalArgumentException("y must be >= 0");
        }
        if (x >= getWidth()) {
            throw new IllegalArgumentException("x must be < bitmap.width()");
        }
        if (y >= getHeight()) {
            throw new IllegalArgumentException("y must be < bitmap.height()");
        }
    }

    /**
     * Returns the {@link Color} at the specified location. Throws an exception
     * if x or y are out of bounds (negative or >= to the width or height
     * respectively). The returned color is a non-premultiplied ARGB value in
     * the {@link ColorSpace.Named#SRGB sRGB} color space.
     *
     * @param x The x coordinate (0...width-1) of the pixel to return
     * @param y The y coordinate (0...height-1) of the pixel to return
     * @return The argb {@link Color} at the specified coordinate
     * @throws IllegalArgumentException if x, y exceed the bitmap's bounds
     */
    @ColorInt
    public int getPixelARGB(int x, int y) {
        checkReleased();
        checkPixelAccess(x, y);
        int word = MemoryUtil.memGetInt(mRef.mPixels + (long) y * getRowStride() + (long) x * mFormat.channels);
        int argb = switch (mFormat) {
            case GRAY_8 -> {
                int lum = word & 0xFF;
                yield 0xFF000000 | (lum << 16) | (lum << 8) | lum;
            }
            case GRAY_ALPHA_88 -> {
                int lum = word & 0xFF;
                yield ((word & 0xFF00) << 16) | (lum << 16) | (lum << 8) | lum;
            }
            case RGB_888 -> // to BGRA
                    0xFF000000 | ((word & 0xFF) << 16) | (word & 0xFF00) | ((word >> 16) & 0xFF);
            case RGBA_8888 -> // to BGRA
                    (word & 0xFF000000) | ((word & 0xFF) << 16) | (word & 0xFF00) | ((word >> 16) & 0xFF);
        };
        if (getColorSpace() != null && !getColorSpace().isSrgb()) {
            float[] v = ColorSpace.connect(getColorSpace()).transform(
                    Color.red(argb) / 255.0f, Color.green(argb) / 255.0f, Color.blue(argb) / 255.0f);
            argb |= Color.argb(0, v[0], v[1], v[2]);
        }
        return argb;
    }

    /**
     * The ref of current pixel data, which may be shared across instances.
     * Calling this method won't affect the ref cnt.
     * <p>
     * This method is <b>UNSAFE</b>, use with caution!
     *
     * @return the ref of pixel data, or null if released
     */
    @SharedPtr
    public Ref getRef() {
        return mRef;
    }

    /**
     * Save this bitmap to specified path as specified format. This will
     * open a save dialog to select the path.
     *
     * @param format  the format of the saved image
     * @param quality the compress quality, 0-100, only work for JPEG format.
     * @param name    the file name without extension name
     * @return true if selected a path, otherwise canceled
     * @throws IOException selected a path, but saving is not successful
     */
    @WorkerThread
    public boolean saveDialog(@NonNull SaveFormat format, int quality,
                              @Nullable String name) throws IOException {
        String path = saveDialogGet(format, null, name);
        if (path != null) {
            saveToPath(format, quality, Path.of(path));
            return true;
        }
        return false;
    }

    /**
     * Save this bitmap to specified path with specified format.
     *
     * @param format  the format of the saved image
     * @param quality the compress quality, 0-100, only work for JPEG format.
     * @param file    the image file
     * @throws IOException saving is not successful
     */
    @WorkerThread
    public void saveToFile(@NonNull SaveFormat format, int quality,
                           @NonNull File file) throws IOException {
        checkReleased();
        try (final var stream = new FileOutputStream(file)) {
            saveToChannel(format, quality, stream.getChannel());
        } catch (IOException e) {
            throw new IOException("Failed to save image to path \"" +
                    file.getAbsolutePath() + "\"", e);
        }
    }

    /**
     * Save this bitmap to specified path with specified format.
     *
     * @param format  the format of the saved image
     * @param quality the compress quality, 0-100, only work for JPEG format.
     * @param path    the image path
     * @throws IOException saving is not successful
     */
    @WorkerThread
    public void saveToPath(@NonNull SaveFormat format, int quality,
                           @NonNull Path path) throws IOException {
        checkReleased();
        try (final var channel = FileChannel.open(path,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            saveToChannel(format, quality, channel);
        } catch (IOException e) {
            throw new IOException("Failed to save image to path \"" +
                    path.toAbsolutePath() + "\"", e);
        }
    }

    /**
     * Save this bitmap to specified stream with specified format. The stream
     * will NOT be closed by this method.
     *
     * @param format  the format of the saved image
     * @param quality the compress quality, 0-100, only work for JPEG format
     * @param stream  the stream to write image data
     * @throws IOException saving is not successful
     */
    @WorkerThread
    public void saveToStream(@NonNull SaveFormat format, int quality,
                             @NonNull OutputStream stream) throws IOException {
        saveToChannel(format, quality, Channels.newChannel(stream));
    }

    /**
     * Save this bitmap to specified channel with specified format. The channel
     * will NOT be closed by this method.
     *
     * @param format  the format of the saved image
     * @param quality the compress quality, 0-100, only work for JPEG format
     * @param channel the channel to write image data
     * @throws IOException saving is not successful
     */
    @WorkerThread
    public void saveToChannel(@NonNull SaveFormat format, int quality,
                              @NonNull WritableByteChannel channel) throws IOException {
        checkReleased();
        if (quality < 0 || quality > 100) {
            throw new IllegalArgumentException("Bad quality " + quality + ", must be 0..100");
        }
        final var callback = new STBIWriteCallback() {
            private IOException exception;

            @Override
            public void invoke(long context, long data, int size) {
                try {
                    channel.write(STBIWriteCallback.getData(data, size));
                } catch (IOException e) {
                    exception = e;
                }
            }
        };
        try (callback) {
            final boolean success = format.write(callback, mRef.mWidth, mRef.mHeight,
                    mFormat, mRef.mPixels, quality);
            if (success) {
                if (callback.exception != null) {
                    throw new IOException("Failed to save image", callback.exception);
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

    /**
     * Clear the reference to the pixel data. The bitmap is marked as "dead",
     * and then it is an error to try to access its pixels.
     * <p>
     * Note: Even you forgot to call this, the system will clean the underlying pixels
     * when the bitmap object become phantom-reachable.
     */
    @Override
    public void close() {
        if (mRef != null) {
            mRef.safeClean();
            mRef = null;
        }
    }

    /**
     * Returns true if this bitmap has been closed. If so, then it is an error
     * to try to access its pixels.
     *
     * @return true if the bitmap has been closed
     */
    public boolean isClosed() {
        return mRef == null;
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
        GRAY_8(1, GL_RED, GL_R8, ImageInfo.CT_GRAY_8),
        GRAY_ALPHA_88(2, GL_RG, GL_RG8, ImageInfo.CT_GRAY_ALPHA_88),
        RGB_888(3, GL_RGB, GL_RGB8, ImageInfo.CT_RGB_888),
        RGBA_8888(4, GL_RGBA, GL_RGBA8, ImageInfo.CT_RGBA_8888);

        private static final Format[] FORMATS = values();

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

        public int getChannelCount() {
            return channels;
        }

        @ImageInfo.ColorType
        public int getColorType() {
            return colorType;
        }

        @NonNull
        public static Format of(int channels) {
            if (channels < 1 || channels > 4) {
                throw new IllegalArgumentException("Specified channels should be 1..4 but " + channels);
            }
            return FORMATS[channels - 1];
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

        private static final SaveFormat[] FORMATS = values();

        @NonNull
        private final String[] filters;

        SaveFormat(@NonNull String... filters) {
            this.filters = filters;
        }

        public abstract boolean write(@NonNull STBIWriteCallbackI func, int width, int height,
                                      @NonNull Format format, long data, int quality) throws IOException;

        @NonNull
        public static PointerBuffer getAllFilters(@NonNull MemoryStack stack) {
            int length = 0;
            for (SaveFormat format : FORMATS) {
                length += format.filters.length;
            }
            PointerBuffer buffer = stack.mallocPointer(length);
            for (SaveFormat format : FORMATS) {
                for (String filter : format.filters) {
                    stack.nUTF8(filter, true);
                    buffer.put(stack.getPointerAddress());
                }
            }
            return buffer.rewind();
        }

        /**
         * Reads: "Image Files (*.png;*.jpg;*.bmp)"
         */
        @NonNull
        public static String getAllDescription() {
            return getAllDescription("Image Files");
        }

        /**
         * Reads: "[header] (*.png;*.jpg;*.bmp)"
         */
        @NonNull
        public static String getAllDescription(@NonNull String header) {
            return header + " (" + Arrays.stream(FORMATS).flatMap(f -> Arrays.stream(f.filters))
                    .sorted().collect(Collectors.joining(";")) + ")";
        }

        @NonNull
        public PointerBuffer getFilters(@NonNull MemoryStack stack) {
            PointerBuffer buffer = stack.mallocPointer(filters.length);
            for (String filter : filters) {
                stack.nUTF8(filter, true);
                buffer.put(stack.getPointerAddress());
            }
            return buffer.rewind();
        }

        @NonNull
        public String getDescription() {
            return name() + " (" + String.join(";", filters) + ")";
        }

        @NonNull
        public static String getFileName(@Nullable SaveFormat format,
                                         @Nullable String name) {
            String s = name != null ? name : "image-" + DATE_FORMAT.format(new Date());
            if (format != null) {
                return s + format.filters[0].substring(1);
            }
            return s;
        }
    }

    /**
     * This class is the smart container for pixel memory, and is used with {@link Bitmap}.
     * <br>This class can be shared/accessed between multiple threads.
     * <p>
     * This class is <b>UNSAFE</b>, use with caution!
     */
    public static class Ref extends RefCnt {

        private final int mWidth;
        private final int mHeight;
        private final long mPixels;
        private final int mRowStride;

        @NonNull
        private final LongConsumer mFreeFn;

        private boolean mImmutable;

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

        /**
         * Returns true if this ref is marked as immutable, meaning that the
         * contents of its pixels will not change for the lifetime of the ref.
         */
        public boolean isImmutable() {
            return mImmutable;
        }

        /**
         * Marks this ref is immutable, meaning that the contents of its
         * pixels will not change for the lifetime of the ref. This state can
         * be set on a ref, but it cannot be cleared once it is set.
         */
        public void setImmutable() {
            mImmutable = true;
        }

        @NonNull
        @Override
        public String toString() {
            return "PixelRef{" +
                    "address=0x" + Long.toHexString(mPixels) +
                    ", dimensions=" + mWidth + "x" + mHeight +
                    ", rowStride=" + mRowStride +
                    ", immutable=" + mImmutable +
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
