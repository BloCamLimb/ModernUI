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
import icyllis.arc3d.core.*;
import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.annotation.Size;
import icyllis.modernui.annotation.*;
import icyllis.modernui.core.Core;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.*;
import org.lwjgl.system.*;
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
import java.util.stream.Stream;

import static icyllis.modernui.ModernUI.LOGGER;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Describes a 2D raster image (pixel map), with its pixels in native memory.
 * This class can be used for CPU-side encoding, decoding and resampling;
 * pixel transfer between CPU and GPU. You cannot draw content to Bitmap.
 * <p>
 * Bitmap is created with immutable width, height and memory allocation
 * (memory address), its contents may be changed. You can obtain Bitmap from
 * encoded data (PNG, TGA, BMP, JPEG, HDR, PSD, PBM, PGM, and PPM) via
 * {@link BitmapFactory}.
 * <p>
 * The color space of Bitmap defaults to sRGB and can only be in RGB space.
 * The alpha defaults to non-premultiplied (independent of RGB channels).
 * <p>
 * This class is not thread safe, but memory safe. Its internal state may
 * be shared by multiple threads. Nevertheless, it's recommended to call
 * {@link #close()} explicitly, or within a try-with-resource block.
 *
 * @see BitmapFactory
 */
@SuppressWarnings("unused")
public final class Bitmap implements AutoCloseable {

    public static final Marker MARKER = MarkerManager.getMarker("Bitmap");
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

    @NonNull
    private Format mFormat;
    @NonNull
    private Pixmap mPixmap;
    @Nullable
    private SafePixels mPixels;

    /**
     * Represents whether the Bitmap's content is requested to be premultiplied.
     * Note that isPremultiplied() does not directly return this value, because
     * isPremultiplied() may never return true for a 565 Bitmap or a bitmap
     * without alpha.
     * <p>
     * setPremultiplied() does directly set the value so that setFormat() and
     * setPremultiplied() aren't order dependent, despite being setters.
     * <p>
     * The actual bitmap's alpha type is kept up to date by
     * pushing down this preference for every format change.
     */
    private boolean mRequestPremultiplied;

    Bitmap(@NonNull Format format, @NonNull ImageInfo info, long addr, int rowBytes,
           @Nullable LongConsumer freeFn) {
        mFormat = format;
        mPixmap = new Pixmap(info, null, addr, rowBytes);
        mPixels = new SafePixels(this, info, addr, rowBytes, freeFn);
        mRequestPremultiplied = isPremultiplied();
    }

    /**
     * Creates a mutable bitmap and its allocation, the content are initialized to zeros.
     * <p>
     * The newly created bitmap will have non-premultiplied alpha if the format has an alpha channel.
     * The newly created bitmap is in the {@link ColorSpace.Named#SRGB sRGB} color space,
     * unless the format is alpha only, in which case the color space is null.
     * You may change the format, alpha type, and color space after creating the bitmap.
     *
     * @param width  width in pixels, must be > 0
     * @param height height in pixels, must be > 0
     * @param format the number of channels and the bit depth
     * @throws IllegalArgumentException width or height out of range
     * @throws OutOfMemoryError         out of native memory
     */
    @NonNull
    public static Bitmap createBitmap(@Size(min = 1) int width,
                                      @Size(min = 1) int height,
                                      @NonNull Format format) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions " + width + "x" + height
                    + " must be positive");
        }
        int bpp = format.getBytesPerPixel();
        if (bpp == 0) {
            throw new IllegalArgumentException("Cannot create bitmap with format " + format);
        }
        if (width > Integer.MAX_VALUE / bpp) {
            throw new IllegalArgumentException("Image width " + width + " is too large for format " + format);
        }
        int rowBytes = width * bpp; // no overflow
        long size = (long) rowBytes * height; // no overflow
        long address = nmemCalloc(size, 1);
        if (address == NULL) {
            // execute ref.Cleaner
            System.gc();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            address = nmemCalloc(size, 1);
            if (address == NULL) {
                throw new OutOfMemoryError("Failed to allocate " + size + " bytes");
            }
        }
        var cs = format.isChannelHDR()
                ? ColorSpace.get(ColorSpace.Named.LINEAR_EXTENDED_SRGB)
                : ColorSpace.get(ColorSpace.Named.SRGB);
        var at = format.hasAlpha() && !format.isChannelHDR()
                ? ColorInfo.AT_UNPREMUL
                : ColorInfo.AT_OPAQUE;
        return new Bitmap(format,
                ImageInfo.make(width, height, format.getColorType(), at, cs),
                address, rowBytes, MemoryUtil::nmemFree);
    }

    /**
     * Create a mutable bitmap by wrapping an existing address in an unsafe manner.
     *
     * @param address         base address
     * @param rowBytes        size of one row of buffer; width times bpp, or larger
     * @param freeFn          free function for address
     * @param width           width of pixels
     * @param height          height of pixels
     * @param format          a color interpretation
     * @param isPremultiplied an alpha interpretation
     * @param colorSpace      a color space describing the pixels
     * @return a bitmap
     */
    @ApiStatus.Experimental
    @NonNull
    public static Bitmap wrap(@NativeType("const void *") long address, int rowBytes,
                              @Nullable LongConsumer freeFn,
                              @Size(min = 1) int width, @Size(min = 1) int height,
                              @NonNull Format format, boolean isPremultiplied,
                              @Nullable ColorSpace colorSpace) {
        int alphaType = format.hasAlpha()
                ? isPremultiplied
                ? ColorInfo.AT_PREMUL
                : ColorInfo.AT_UNPREMUL
                : ColorInfo.AT_OPAQUE;
        alphaType = ColorInfo.validateAlphaType(format.getColorType(), alphaType);
        ImageInfo info = ImageInfo.make(width, height,
                format.getColorType(), alphaType, colorSpace);
        return new Bitmap(format, info, address, rowBytes, freeFn);
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
    public static String[] openDialogGets(@Nullable SaveFormat format,
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

    @ApiStatus.Internal
    public static void flipVertically(@NonNull Bitmap bitmap) {
        assert !bitmap.isImmutable();
        final int height = bitmap.getHeight();
        final long rowBytes = bitmap.getRowBytes();
        final long addr = bitmap.getAddress();
        if (addr == NULL) {
            throw new IllegalStateException("src pixels is null");
        }
        final long temp = nmemAlloc(rowBytes);
        if (temp == NULL) {
            throw new IllegalStateException("temp pixels is null");
        }
        try {
            for (int i = 0, lim = height >> 1; i < lim; i++) {
                final long srcOff = i * rowBytes;
                final long dstOff = (height - i - 1) * rowBytes;
                memCopy(addr + srcOff, temp, rowBytes);
                memCopy(addr + dstOff, addr + srcOff, rowBytes);
                memCopy(temp, addr + dstOff, rowBytes);
            }
        } finally {
            nmemFree(temp);
        }
    }

    @ApiStatus.Internal
    @NonNull
    public ImageInfo getInfo() {
        return mPixmap.getInfo();
    }

    /**
     * Returns the width of the bitmap.
     */
    public int getWidth() {
        if (mPixels == null) {
            LOGGER.warn(MARKER, "Called getWidth() on a recycle()'d bitmap! This is undefined behavior!");
        }
        return mPixmap.getWidth();
    }

    /**
     * Returns the height of the bitmap.
     */
    public int getHeight() {
        if (mPixels == null) {
            LOGGER.warn(MARKER, "Called getHeight() on a recycle()'d bitmap! This is undefined behavior!");
        }
        return mPixmap.getHeight();
    }

    /**
     * Returns the number of bytes that can be used to store this bitmap's pixels.
     */
    public long getSize() {
        if (mPixels == null) {
            LOGGER.warn(MARKER, "Called getSize() on a recycle()'d bitmap! This is undefined behavior!");
            return 0;
        }
        return (long) getRowBytes() * getHeight();
    }

    @ApiStatus.Internal
    public int getColorType() {
        return mPixmap.getColorType();
    }

    @ApiStatus.Internal
    public int getAlphaType() {
        return mPixmap.getAlphaType();
    }

    /**
     * Returns the current pixel format used to interpret pixel data.
     * The bitmap's format can be changed using {@link #setFormat(Format)}.
     *
     * @return the pixel format used to interpret colors
     */
    @NonNull
    public Format getFormat() {
        return mFormat;
    }

    /**
     * Modifies the bitmap to have a specified {@link Format}, without affecting
     * the underlying allocation backing the bitmap. Bitmap pixel data is not
     * re-initialized for the new format. By calling this method, further operations
     * will reinterpret the colors using the specified color type.
     * <p>
     * This method can be used to map a channel to another channel. For example,
     * {@link Format#GRAY_8}, {@link Format#ALPHA_8}, {@link Format#R_8} have the
     * same pixel layout and are interchangeable.
     * If the bytes-per-pixel of new format and original format do not match, an
     * {@link IllegalArgumentException} will be thrown and the bitmap will not
     * be modified.
     *
     * @param format a new interpretation of existing pixel data
     * @throws IllegalArgumentException bpp of new format is not equal to bpp of original format
     */
    public void setFormat(@NonNull Format format) {
        if (format.getBytesPerPixel() == 0) {
            throw new IllegalArgumentException("Format " + format + " is not supported");
        }
        if (format.getBytesPerPixel() != mFormat.getBytesPerPixel()) {
            throw new IllegalArgumentException(
                    "Bpp mismatch between new format " + format + " and old format " + mFormat);
        }
        var info = getInfo();
        var newColorType = format.getColorType();
        if (info.colorType() != newColorType) {
            var newAlphaType = ColorInfo.validateAlphaType(newColorType,
                    mRequestPremultiplied ? ColorInfo.AT_PREMUL : ColorInfo.AT_UNPREMUL);
            var newInfo = new ImageInfo(
                    info.width(), info.height(),
                    newColorType, newAlphaType,
                    info.colorSpace()
            );
            mPixmap = new Pixmap(newInfo, mPixmap);
            mFormat = format;
        }
    }

    /**
     * Returns the number of channels.
     */
    public int getChannels() {
        return mFormat.getChannels();
    }

    /**
     * Returns true if the bitmap's format supports per-pixel alpha, and
     * if the pixels may contain non-opaque alpha values. For some formats,
     * this is always false (e.g. {@link Format#RGB_888}), since they do
     * not support per-pixel alpha. However, for formats that do, the
     * bitmap may be flagged to be known that all of its pixels are opaque.
     * In this case hasAlpha() will also return false. If a format such as
     * {@link Format#RGBA_8888} is not so flagged, it will return true
     * by default.
     */
    public boolean hasAlpha() {
        assert mPixels != null;
        return !getInfo().isOpaque();
    }

    /**
     * <p>Indicates whether pixels stored in this bitmaps are stored premultiplied.
     * When a pixel is premultiplied, the RGB components have been multiplied by
     * the alpha component. For instance, if the original color is a 50%
     * translucent red <code>(128, 255, 0, 0)</code>, the premultiplied form is
     * <code>(128, 128, 0, 0)</code>.</p>
     *
     * <p>This method only returns true if {@link #hasAlpha()} returns true.
     * A bitmap with no alpha channel can be used both as a premultiplied and
     * as a non premultiplied bitmap.</p>
     *
     * @return true if the underlying pixels have been premultiplied, false
     * otherwise
     */
    public boolean isPremultiplied() {
        assert mPixels != null;
        return getAlphaType() == ColorInfo.AT_PREMUL;
    }

    /**
     * Sets whether the bitmap should treat its data as premultiplied or not.
     * <p>
     * By calling this method, further operations will reinterpret the colors
     * using the specified alpha type, existing pixel data will remain unchanged.
     * <p>
     * If the current format does not have an alpha channel ({@link #hasAlpha()} returns false),
     * then the specified alpha type is recorded and will work in the future,
     * see {@link #setFormat(Format)}.
     *
     * @param premultiplied whether premultiplied alpha type is requested
     */
    public void setPremultiplied(boolean premultiplied) {
        assert mPixels != null;
        mRequestPremultiplied = premultiplied;
        if (hasAlpha()) {
            var info = getInfo();
            var newAlphaType = ColorInfo.validateAlphaType(info.colorType(),
                    premultiplied ? ColorInfo.AT_PREMUL : ColorInfo.AT_UNPREMUL);
            if (info.alphaType() != newAlphaType) {
                mPixmap = new Pixmap(info.makeAlphaType(newAlphaType), mPixmap);
            }
        }
    }

    /**
     * Returns the color space associated with this bitmap. If the color
     * space is unknown, this method returns null.
     */
    @Nullable
    public ColorSpace getColorSpace() {
        return mPixmap.getColorSpace();
    }

    /**
     * <p>Modifies the bitmap to have the specified {@link ColorSpace}, without
     * affecting the underlying allocation backing the bitmap.</p>
     *
     * <p>This affects how the framework will interpret the color at each pixel.</p>
     *
     * @param newColorSpace to assign to the bitmap
     * @throws IllegalArgumentException If the specified color space is not {@link ColorSpace.Model#RGB RGB},
     *                                  has a transfer function that is not an
     *                                  {@link ColorSpace.Rgb.TransferParameters ICC parametric curve}, or whose
     *                                  components min/max values reduce the numerical range compared to the
     *                                  previously assigned color space.
     */
    public void setColorSpace(@Nullable ColorSpace newColorSpace) {
        var oldInfo = getInfo();
        var oldColorSpace = oldInfo.colorSpace();
        if (oldColorSpace == newColorSpace) {
            return;
        }
        if (newColorSpace != null) {
            if (!(newColorSpace instanceof ColorSpace.Rgb rgbColorSpace)) {
                throw new IllegalArgumentException("The new ColorSpace must use the RGB color model");
            }
            if (rgbColorSpace.getTransferParameters() == null) {
                throw new IllegalArgumentException("The new ColorSpace must use an ICC parametric transfer function");
            }
        }
        if (oldColorSpace != null && newColorSpace != null) {
            for (int i = 0; i < oldColorSpace.getComponentCount(); i++) {
                if (oldColorSpace.getMinValue(i) < newColorSpace.getMinValue(i)) {
                    throw new IllegalArgumentException("The new ColorSpace cannot increase the "
                            + "minimum value for any of the components compared to the current "
                            + "ColorSpace. To perform this type of conversion create a new "
                            + "Bitmap in the desired ColorSpace and draw this Bitmap into it.");
                }
                if (oldColorSpace.getMaxValue(i) > newColorSpace.getMaxValue(i)) {
                    throw new IllegalArgumentException("The new ColorSpace cannot decrease the "
                            + "maximum value for any of the components compared to the current "
                            + "ColorSpace/ To perform this type of conversion create a new "
                            + "Bitmap in the desired ColorSpace and draw this Bitmap into it.");
                }
            }
        }
        mPixmap = new Pixmap(oldInfo.makeColorSpace(newColorSpace), mPixmap);
    }

    /**
     * The address of {@code void *pixels} in native.
     * The address is valid until bitmap closed.
     *
     * @return the pointer of pixel data, or NULL if released
     */
    @ApiStatus.Internal
    public long getAddress() {
        if (mPixels == null) {
            return NULL;
        }
        return mPixmap.getAddress();
    }

    /**
     * The distance, in bytes, between the start of one pixel row and the next,
     * including any unused space between them.
     * <p>
     * This method is obsolete in favor of {@link #getRowBytes()}.
     *
     * @return the scanline size in bytes
     */
    @ApiStatus.Obsolete
    public int getRowStride() {
        return getRowBytes();
    }

    /**
     * Return the distance, in bytes, between the start of one pixel row and the next,
     * including any unused space between them.
     *
     * @return number of bytes between rows of the native bitmap pixels
     */
    public int getRowBytes() {
        if (mPixels == null) {
            LOGGER.warn(MARKER, "Called getRowBytes() on a recycle()'d bitmap! This is undefined behavior!");
        }
        return mPixmap.getRowBytes();
    }

    /**
     * Returns true if the bitmap is marked as immutable.
     */
    public boolean isImmutable() {
        if (mPixels != null) {
            return mPixels.isImmutable();
        }
        assert false;
        return false;
    }

    /**
     * Marks the Bitmap as immutable. Further modifications to this Bitmap are disallowed.
     * After this method is called, this Bitmap cannot be made mutable again.
     */
    public void setImmutable() {
        if (mPixels != null) {
            mPixels.setImmutable();
        }
    }

    private void checkOutOfBounds(int x, int y) {
        if (x < 0) {
            throw new IllegalArgumentException("x " + x + " must be >= 0");
        }
        if (y < 0) {
            throw new IllegalArgumentException("y " + y + " must be >= 0");
        }
        if (x >= getWidth()) {
            throw new IllegalArgumentException("x " + x + " must be < bitmap.width() " + getWidth());
        }
        if (y >= getHeight()) {
            throw new IllegalArgumentException("y " + y + " must be < bitmap.height() " + getHeight());
        }
    }

    /**
     * Returns the {@link Color} at the specified location. Throws an exception
     * if x or y are out of bounds (negative or >= to the width or height
     * respectively). The returned color is a non-premultiplied ARGB value in
     * the {@link ColorSpace.Named#SRGB sRGB} color space, the format is the
     * same as {@link Format#BGRA_8888_PACK32}.
     *
     * @param x The x coordinate (0...width-1) of the pixel to return
     * @param y The y coordinate (0...height-1) of the pixel to return
     * @return The argb {@link Color} at the specified coordinate
     * @throws IllegalArgumentException if x, y exceed the bitmap's bounds
     */
    @ColorInt
    public int getPixelARGB(int x, int y) {
        checkReleased();
        checkOutOfBounds(x, y);
        if (getColorType() == ColorInfo.CT_UNKNOWN) {
            return 0;
        }
        return mPixmap.getColor(x, y);
    }

    /**
     * Gets the pixel value at the specified location. Throws an exception
     * if x or y are out of bounds (negative or >= to the width or height
     * respectively). This method won't perform alpha type or color space conversion,
     * then the result color is converted to RGBA float color in
     * the source color space {@link #getColorSpace()}, its alpha type is
     * left unchanged (the same as {@link #isPremultiplied()}).
     *
     * @param x   The x coordinate (0...width-1) of the pixel to return
     * @param y   The y coordinate (0...height-1) of the pixel to return
     * @param dst The result RGBA float color at the specified coordinate
     * @return the passed float array that contains r,g,b,a values
     * @throws IllegalStateException    the bitmap is released
     * @throws IllegalArgumentException if x, y exceed the bitmap's bounds
     */
    @NonNull
    @Size(4)
    public float[] getColor4f(int x, int y, @NonNull @Size(4) float[] dst) {
        checkReleased();
        checkOutOfBounds(x, y);
        if (getColorType() == ColorInfo.CT_UNKNOWN) {
            Arrays.fill(dst, 0.0f);
        } else {
            mPixmap.getColor4f(x, y, dst);
        }
        return dst;
    }

    /**
     * Sets the pixel value at the specified location. The given float color
     * will convert to bitmap's format. This method won't perform alpha type
     * or color space conversion, then the given color must be in the
     * bitmap's color space {@link #getColorSpace()}, its alpha type must be the
     * same as {@link #isPremultiplied()}). This bitmap must be mutable,
     * {@link #isImmutable()} must return false.
     *
     * @param x   The x coordinate of the pixel to replace (0...width-1)
     * @param y   The y coordinate of the pixel to replace (0...height-1)
     * @param src The RGBA float color to write into the bitmap
     * @throws IllegalStateException    the bitmap is released or immutable
     * @throws IllegalArgumentException if x, y exceed the bitmap's bounds
     */
    public void setColor4f(int x, int y, @NonNull @Size(4) float[] src) {
        checkReleased();
        if (isImmutable()) {
            throw new IllegalStateException("Cannot write to immutable bitmap");
        }
        checkOutOfBounds(x, y);
        if (getColorType() != ColorInfo.CT_UNKNOWN) {
            mPixmap.setColor4f(x, y, src);
        }
    }

    /**
     * The current pixel map, which is usually paired with {@link Pixels}.
     * This method is <b>UNSAFE</b>, use with caution!
     *
     * @return the current pixel map
     */
    @ApiStatus.Internal
    @NonNull
    public Pixmap getPixmap() {
        return mPixmap;
    }

    /**
     * The ref of current pixel data, which may be shared across instances.
     * Calling this method won't affect the ref cnt. Every bitmap object
     * ref the {@link Pixels} on create, and unref on {@link #close()}.
     * <p>
     * This method is <b>UNSAFE</b>, use with caution!
     *
     * @return the ref of pixel data, or null if released
     */
    @ApiStatus.Internal
    @RawPtr
    @Nullable
    public Pixels getPixels() {
        return mPixels;
    }

    /**
     * Save this bitmap to specified path as specified format. This will
     * open a save dialog to select the path, block the current thread until
     * the encoding is done.
     *
     * @param format  the format of the saved image
     * @param quality the compress quality, 0-100, only work for JPEG format.
     * @param name    the file name without extension name
     * @return true if selected a path, otherwise canceled
     * @throws IOException              selected a path, but saving is not successful
     * @throws IllegalArgumentException bad arguments
     * @throws IllegalStateException    failed to process
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
     * @throws IOException              saving is not successful
     * @throws IllegalArgumentException bad arguments
     * @throws IllegalStateException    failed to process
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
     * @throws IOException              saving is not successful
     * @throws IllegalArgumentException bad arguments
     * @throws IllegalStateException    failed to process
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
     * @throws IOException              saving is not successful
     * @throws IllegalArgumentException bad arguments
     * @throws IllegalStateException    failed to process
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
     * @throws IOException              saving is not successful
     * @throws IllegalArgumentException bad arguments
     * @throws IllegalStateException    failed to process
     */
    @WorkerThread
    public void saveToChannel(@NonNull SaveFormat format, int quality,
                              @NonNull WritableByteChannel channel) throws IOException {
        checkReleased();
        if (quality < 0 || quality > 100) {
            throw new IllegalArgumentException("Bad quality " + quality + ", must be 0..100");
        }
        if (Core.isOnMainThread() || Core.isOnRenderThread()) {
            LOGGER.warn(MARKER, "Called save() on core thread! This will hang the application!",
                    new Exception().fillInStackTrace());
        }
        if (getRowBytes() != getWidth() * getFormat().getBytesPerPixel()) {
            // not implemented yet
            throw new IllegalStateException("Pixel data is not tightly packed");
        }
        final var callback = new STBIWriteCallback() {
            private IOException exception;

            @Override
            public void invoke(long context, long data, int size) {
                try {
                    int n = channel.write(STBIWriteCallback.getData(data, size));
                    if (n != size) {
                        exception = new IOException("Channel does not consume all the data");
                    }
                } catch (IOException e) {
                    exception = e;
                }
            }
        };
        try (callback) {
            final boolean success = format.write(callback, getWidth(), getHeight(),
                    mFormat, getAddress(), quality);
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
        if (mPixels == null) {
            throw new IllegalStateException("Cannot operate a recycled bitmap!");
        }
    }

    /**
     * Clear the reference to the pixel data. The bitmap is marked as "dead",
     * and then it is an error to try to access its pixels.
     * <p>
     * The system can ensure that the native allocation of a Bitmap to be
     * released when the Bitmap object becomes phantom-reachable. However,
     * it tends to take a very long time to perform automatic cleanup.
     */
    @Override
    public void close() {
        if (mPixels != null) {
            // Cleaner is synchronized
            mPixels.mCleanup.clean();
            mPixels = null;
        }
    }

    /**
     * Same as {@link #close()}.
     */
    public void recycle() {
        close();
    }

    /**
     * Returns true if this bitmap has been closed. If so, then it is an error
     * to try to access its pixels.
     *
     * @return true if the bitmap has been closed
     */
    public boolean isClosed() {
        return mPixels == null;
    }

    /**
     * @return the same as {@link #isClosed()}
     */
    public boolean isRecycled() {
        return mPixels == null;
    }

    @NonNull
    @Override
    public String toString() {
        return "Bitmap{" +
                "mFormat=" + mFormat +
                ", mInfo=" + getInfo() +
                ", mPixels=" + mPixels +
                '}';
    }

    /**
     * Describes a layout of pixel data in CPU memory. A pixel may be an alpha mask, a grayscale,
     * RGB, or RGBA. It specifies the channels, their type, and width.
     * <p>
     * Bitmap formats are divided into two classes: array and packed.<br>
     * For array types, the components are listed in order of where they appear in memory. For example,
     * {@link #RGBA_8888} means that the pixel memory should be interpreted as an array of uint8 values,
     * and the R channel appears at the first uint8 value. This is the same naming convention as Vulkan.<br>
     * For packed types, the first component appear in the least-significant bits. For example,
     * {@link #BGR_565} means that each pixel is packed as {@code (b << 0) | (g << 5) | (r << 11)},
     * an uint16 value. This is in the reverse order of Vulkan's naming convention.
     * <p>
     * Note that if bytes-per-pixel of a color type is 1, 2, 4, or 8, then ModernUI requires pixel memory
     * to be aligned to bytes-per-pixel, otherwise it should be aligned to the size of basic data type.
     */
    public enum Format {
        //@formatter:off
        /**
         * Grayscale, one channel, 8-bit unsigned per channel.
         * Basic data type: byte.
         * <pre>
         * 0       1  byte
         * | gray  |
         * </pre>
         */
        GRAY_8          (1, ColorInfo.CT_GRAY_8         ),
        /**
         * Grayscale, with alpha, two channels, 8-bit unsigned per channel.
         * Basic data type: byte.
         * <pre>
         * 0       1       2  byte
         * | gray  | alpha |
         * </pre>
         */
        GRAY_ALPHA_88   (2, ColorInfo.CT_GRAY_ALPHA_88  ),
        /**
         * RGB, three channels, 8-bit unsigned per channel.
         * Basic data type: byte.
         * <pre>
         * 0       1       2       3  byte
         * |   r   |   g   |   b   |
         * </pre>
         * Because the bpp of this format is not a power of 2,
         * operations on this format will be slower.
         */
        RGB_888         (3, ColorInfo.CT_RGB_888        ),
        /**
         * RGB, with alpha, four channels, 8-bit unsigned per channel.
         * Basic data type: byte.
         * <pre>
         * 0       1       2       3       4  byte
         * |   r   |   g   |   b   |   a   |
         * </pre>
         */
        RGBA_8888       (4, ColorInfo.CT_RGBA_8888      ),
        /**
         * Unsupported, DO NOT USE.
         * <pre>
         * 0       2  byte
         * | gray  |
         * </pre>
         */
        @ApiStatus.Internal
        GRAY_16         (1, ColorInfo.CT_UNKNOWN        ),
        /**
         * Unsupported, DO NOT USE.
         * <pre>
         * 0       2       4  byte
         * | gray  | alpha |
         * </pre>
         */
        @ApiStatus.Internal
        GRAY_ALPHA_1616 (2, ColorInfo.CT_UNKNOWN        ),
        /**
         * Unsupported, DO NOT USE.
         * <pre>
         * 0       2       4       6  byte
         * |   r   |   g   |   b   |
         * </pre>
         */
        @ApiStatus.Internal
        RGB_161616      (3, ColorInfo.CT_UNKNOWN        ),
        /**
         * RGB, with alpha, four channels, 16-bit unsigned per channel.
         * Basic data type: short.
         * <pre>
         * 0       2       4       6       8  byte
         * |   r   |   g   |   b   |   a   |
         * </pre>
         */
        RGBA_16161616   (4, ColorInfo.CT_RGBA_16161616  ),
        /**
         * Unsupported, DO NOT USE.
         */
        @ApiStatus.Internal
        GRAY_F32        (1, ColorInfo.CT_UNKNOWN        ),
        /**
         * Unsupported, DO NOT USE.
         */
        @ApiStatus.Internal
        GRAY_ALPHA_F32  (2, ColorInfo.CT_UNKNOWN        ),
        /**
         * Unsupported, DO NOT USE.
         */
        @ApiStatus.Internal
        RGB_F32         (3, ColorInfo.CT_UNKNOWN        ),
        /**
         * RGB, with alpha, four channels, 32-bit floating-point per channel.
         * Basic data type: float.
         * <pre>
         * 0       4       8      12      16  byte
         * |   r   |   g   |   b   |   a   |
         * </pre>
         */
        RGBA_F32        (4, ColorInfo.CT_RGBA_F32       ),
        /**
         * Alpha mask, one channel, 8-bit unsigned per channel.
         * Basic data type: byte.
         * <pre>
         * 0       1  byte
         * | alpha |
         * </pre>
         */
        ALPHA_8         (1, ColorInfo.CT_ALPHA_8        ),
        /**
         * Red, one channel, 8-bit unsigned per channel.
         * Basic data type: byte.
         * <pre>
         * 0       1  byte
         * |   r   |
         * </pre>
         */
        R_8             (1, ColorInfo.CT_R_8            ),
        /**
         * RG, two channels, 8-bit unsigned per channel.
         * Basic data type: byte.
         * <pre>
         * 0       1       2  byte
         * |   r   |   g   |
         * </pre>
         */
        RG_88           (2, ColorInfo.CT_RG_88          ),
        /**
         * Alpha mask, one channel, 16-bit unsigned per channel.
         * Basic data type: short.
         * <pre>
         * 0       2  byte
         * | alpha |
         * </pre>
         */
        ALPHA_16        (1, ColorInfo.CT_ALPHA_16       ),
        /**
         * Red, one channel, 16-bit unsigned per channel.
         * Basic data type: short.
         * <pre>
         * 0       2  byte
         * |   r   |
         * </pre>
         */
        R_16            (1, ColorInfo.CT_R_16           ),
        /**
         * RG, two channels, 16-bit unsigned per channel.
         * Basic data type: short.
         * <pre>
         * 0       2       4  byte
         * |   r   |   g   |
         * </pre>
         */
        RG_1616         (2, ColorInfo.CT_RG_1616        ),
        /**
         * Alpha mask, one channel, 16-bit half float per channel.
         * Basic data type: short.
         * <pre>
         * 0       2  byte
         * | alpha |
         * </pre>
         */
        ALPHA_F16        (1, ColorInfo.CT_ALPHA_F16     ),
        /**
         * Red, one channel, 16-bit half float per channel.
         * Basic data type: short.
         * <pre>
         * 0       2  byte
         * |   r   |
         * </pre>
         */
        R_F16            (1, ColorInfo.CT_R_F16         ),
        /**
         * RG, two channels, 16-bit half float per channel.
         * Basic data type: short.
         * <pre>
         * 0       2       4  byte
         * |   r   |   g   |
         * </pre>
         */
        RG_F16          (2, ColorInfo.CT_RG_F16         ),
        /**
         * RGB, with alpha, four channels, 32-bit half float per channel.
         * Basic data type: short.
         * <pre>
         * 0       2       4       6       8  byte
         * |   r   |   g   |   b   |   a   |
         * </pre>
         */
        RGBA_F16        (4, ColorInfo.CT_RGBA_F16       ),
        /**
         * RGB, packed three channels, 16-bit unsigned per pixel.
         * Basic data type: short.
         * <pre>
         * 0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16  bit
         * |      b       |        g        |      r       |
         * </pre>
         * <pre>
         * short color = (B & 0x1f) | (G & 0x3f) << 5 | (R & 0x1f) << 11;
         * </pre>
         */
        BGR_565         (3, ColorInfo.CT_BGR_565        ),
        /**
         * RGB, with alpha, packed four channels, 32-bit unsigned per pixel.
         * Basic data type: int.
         * <pre>
         * 0  2  4  6  8 10 12 14 16 18 20 22 24 26 28 30 32  bit
         * |      r       |       g      |       b      | a|
         * </pre>
         * <pre>
         * int color = (R & 0x3ff) | (G & 0x3ff) << 10 | (B & 0x3ff) << 20 | (A & 0x3) << 30;
         * </pre>
         */
        RGBA_1010102    (4, ColorInfo.CT_RGBA_1010102   ),
        /**
         * RGB, with alpha, packed four channels, 32-bit unsigned per pixel.
         * Basic data type: int.
         * <pre>
         * 0  2  4  6  8 10 12 14 16 18 20 22 24 26 28 30 32  bit
         * |      b       |       g      |       r      | a|
         * </pre>
         * <pre>
         * int color = (B & 0x3ff) | (G & 0x3ff) << 10 | (R & 0x3ff) << 20 | (A & 0x3) << 30;
         * </pre>
         */
        @ApiStatus.Experimental
        BGRA_1010102    (4, ColorInfo.CT_BGRA_1010102   ),
        /**
         * RGB, four channels, 8-bit unsigned per channel.
         * Basic data type: byte.
         * <pre>
         * 0       1       2       3       4  byte
         * |   r   |   g   |   b   |   x   |
         * </pre>
         * This format has the same memory layout as {@link #RGBA_8888},
         * but the alpha channel is always filled with 0xFF and/or considered opaque.
         * This may be faster than {@link #RGB_888} when doing pixel operations.
         */
        RGBX_8888       (4, ColorInfo.CT_RGBX_8888      ),
        /**
         * RGB, with alpha, four channels, 8-bit unsigned per channel.
         * Basic data type: byte.
         * <pre>
         * 0       1       2       3       4  byte
         * |   b   |   g   |   r   |   a   |
         * </pre>
         */
        BGRA_8888       (4, ColorInfo.CT_BGRA_8888      ),
        /**
         * RGB, with alpha, four channels, 8-bit unsigned per channel.
         * Basic data type: byte.
         * <pre>
         * 0       1       2       3       4  byte
         * |   a   |   b   |   g   |   r   |
         * </pre>
         */
        @ApiStatus.Experimental
        ABGR_8888       (4, ColorInfo.CT_ABGR_8888      ),
        /**
         * RGB, with alpha, four channels, 8-bit unsigned per channel.
         * Basic data type: byte.
         * <pre>
         * 0       1       2       3       4  byte
         * |   a   |   r   |   g   |   b   |
         * </pre>
         */
        @ApiStatus.Experimental
        ARGB_8888       (4, ColorInfo.CT_ARGB_8888      ),
        /**
         * RGB, with alpha, packed four channels, 32-bit unsigned per pixel.
         * Basic data type: int.
         * <pre>
         * 0  2  4  6  8 10 12 14 16 18 20 22 24 26 28 30 32  bit
         * |     r     |     g     |     b     |     a     |
         * </pre>
         * <pre>
         * int color = (R & 0xff) | (G & 0xff) << 8 | (B & 0xff) << 16 | (A & 0xff) << 24;
         * </pre>
         * On little-endian machine, this is the same as {@link #RGBA_8888}.
         * On big-endian machine, this is the same as {@link #ABGR_8888}.
         */
        @ApiStatus.Experimental
        RGBA_8888_PACK32(4, ColorInfo.CT_RGBA_8888_NATIVE),
        /**
         * RGB, with alpha, packed four channels, 32-bit unsigned per pixel.
         * Basic data type: int.
         * <pre>
         * 0  2  4  6  8 10 12 14 16 18 20 22 24 26 28 30 32  bit
         * |     b     |     g     |     r     |     a     |
         * </pre>
         * <pre>
         * int color = (B & 0xff) | (G & 0xff) << 8 | (R & 0xff) << 16 | (A & 0xff) << 24;
         * </pre>
         * On little-endian machine, this is the same as {@link #BGRA_8888}.
         * On big-endian machine, this is the same as {@link #ARGB_8888}.
         * This is actually used for int values that marked as {@link ColorInt}.
         */
        BGRA_8888_PACK32(4, ColorInfo.CT_BGRA_8888_NATIVE);
        //@formatter:on

        private static final Format[] FORMATS = values();

        private final int mChannels;
        private final int mColorType;

        Format(int chs, int ct) {
            mChannels = chs;
            mColorType = ct;
        }

        /**
         * Returns the number of channels.
         */
        public int getChannels() {
            return mChannels;
        }

        /**
         * The source (in CPU memory) color type of this format.
         * <p>
         * RGB is special, it's 3 bytes per pixel in CPU memory, but
         * 4 bytes per pixel in GPU memory (implicitly).
         *
         * @see #getBytesPerPixel()
         */
        @ColorInfo.ColorType
        public int getColorType() {
            return mColorType;
        }

        public int getBytesPerPixel() {
            if (mColorType != ColorInfo.CT_UNKNOWN) {
                return ColorInfo.bytesPerPixel(mColorType);
            }
            return switch (this) {
                case GRAY_16 -> 2;
                case GRAY_ALPHA_1616, GRAY_F32 -> 4;
                case RGB_161616 -> 6;
                case GRAY_ALPHA_F32 -> 8;
                case RGB_F32 -> 12;
                default -> 0;
            };
        }

        /**
         * Is this format 8-bit per channel and encoded as unsigned byte?
         */
        public boolean isChannelU8() {
            return switch (this) {
                case GRAY_8,
                     GRAY_ALPHA_88,
                     RGB_888,
                     RGBA_8888,
                     ALPHA_8,
                     R_8,
                     RG_88,
                     RGBX_8888,
                     BGRA_8888,
                     ABGR_8888,
                     ARGB_8888 -> true;
                default -> false;
            };
        }

        /**
         * Is this format 16-bit per channel and encoded as unsigned short?
         */
        public boolean isChannelU16() {
            return switch (this) {
                case GRAY_16,
                     GRAY_ALPHA_1616,
                     RGB_161616,
                     RGBA_16161616,
                     ALPHA_16,
                     R_16,
                     RG_1616 -> true;
                default -> false;
            };
        }

        /**
         * Is this format 32-bit per channel and encoded as float?
         */
        public boolean isChannelHDR() {
            return ordinal() >> 2 == 2;
        }

        /**
         * Does this format have alpha channel?
         */
        public boolean hasAlpha() {
            if (mColorType != ColorInfo.CT_UNKNOWN) {
                return (ColorInfo.colorTypeChannelFlags(mColorType) & icyllis.arc3d.core.Color.COLOR_CHANNEL_FLAG_ALPHA) != 0;
            }
            return (ordinal() & 1) == 1;
        }

        /**
         * @hidden
         */
        @ApiStatus.Internal
        @NonNull
        public static Format get(int chs, boolean u16, boolean hdr) {
            if (chs < 1 || chs > 4) {
                throw new IllegalArgumentException();
            }
            if (u16 && hdr) {
                throw new IllegalArgumentException();
            }
            return FORMATS[(chs - 1) | (u16 ? 4 : 0) | (hdr ? 8 : 0)];
        }
    }

    /**
     * List of supported formats a bitmap can be saved as (encoding or compressing).
     */
    public enum SaveFormat {
        /**
         * Save as the PNG format. PNG is lossless and compressed, and {@code quality}
         * is ignored.
         * <p>
         * Only supports 8-bit per channel images ({@link Format#isChannelU8()} is true).
         */
        PNG("*.png"),

        /**
         * Save as the TGA format. TGA is lossless and compressed (by default), and {@code quality}
         * is ignored.
         * <p>
         * Only supports 8-bit per channel images ({@link Format#isChannelU8()} is true).
         */
        TGA("*.tga", "*.icb", "*.vda", "*.vst"),

        /**
         * Save as the BMP format. BMP is lossless but almost uncompressed, so it takes
         * up a lot of space, and {@code quality} is ignored as well.
         * <p>
         * Only supports 8-bit per channel images ({@link Format#isChannelU8()} is true).
         */
        BMP("*.bmp", "*.dib"),

        /**
         * Save as the JPEG baseline format. {@code quality} of {@code 1} means
         * compress for the smallest size. {@code 100} means compress for max
         * visual quality. The file extension can be {@code .jpg}.
         * <p>
         * Only supports 8-bit per channel images ({@link Format#isChannelU8()} is true).
         */
        JPEG("*.jpg", "*.jpeg", "*.jpe", "*.jif", "*.jfif", "*.jfi"),

        /**
         * Save as the Radiance RGBE format. RGBE allows pixels to have the dynamic range
         * and precision of floating-point values, and {@code quality} is ignored.
         * <p>
         * Only supports 32-bit per channel images ({@link Format#isChannelHDR()} is true).
         */
        HDR("*.hdr"),

        /**
         * Save as the raw binary data, this is simply a memory dump.
         * The byte order is determined by host endianness {@link java.nio.ByteOrder#nativeOrder()}.
         */
        // this format must be the last enum
        RAW("*.bin");

        private static final SaveFormat[] OPEN_FORMATS;

        static {
            SaveFormat[] values = values();
            // remove the last "raw" format
            OPEN_FORMATS = Arrays.copyOf(values, values.length - 1);
        }

        // read only formats
        private static final String[] EXTRA_FILTERS = {"*.psd", "*.gif", "*.pic", "*.pnm", "*.pgm", "*.ppm"};

        @NonNull
        private final String[] filters;

        SaveFormat(@NonNull String... filters) {
            this.filters = filters;
        }

        public boolean write(@NonNull STBIWriteCallbackI func, int width, int height,
                             @NonNull Format format, long data, int quality) throws IOException {
            switch (this) {
                case PNG -> {
                    if (!format.isChannelU8()) {
                        throw new IOException("Only 8-bit per channel images can be saved as "
                                + this + ", found " + format);
                    }
                    return STBImageWrite.nstbi_write_png_to_func(func.address(),
                            NULL, width, height, format.getChannels(), data, 0) != 0;
                }
                case TGA -> {
                    if (!format.isChannelU8()) {
                        throw new IOException("Only 8-bit per channel images can be saved as "
                                + this + ", found " + format);
                    }
                    return STBImageWrite.nstbi_write_tga_to_func(func.address(),
                            NULL, width, height, format.getChannels(), data) != 0;
                }
                case BMP -> {
                    if (!format.isChannelU8()) {
                        throw new IOException("Only 8-bit per channel images can be saved as "
                                + this + ", found " + format);
                    }
                    return STBImageWrite.nstbi_write_bmp_to_func(func.address(),
                            NULL, width, height, format.getChannels(), data) != 0;
                }
                case JPEG -> {
                    if (!format.isChannelU8()) {
                        throw new IOException("Only 8-bit per channel images can be saved as "
                                + this + ", found " + format);
                    }
                    return STBImageWrite.nstbi_write_jpg_to_func(func.address(),
                            NULL, width, height, format.getChannels(), data, quality) != 0;
                }
                case HDR -> {
                    if (!format.isChannelHDR()) {
                        throw new IOException("Only 32-bit per channel images can be saved as "
                                + this + ", found " + format);
                    }
                    return STBImageWrite.nstbi_write_hdr_to_func(func.address(),
                            NULL, width, height, format.getChannels(), data) != 0;
                }
                case RAW -> {
                    func.invoke(NULL, data, width * height * format.getBytesPerPixel());
                    return true;
                }
                default -> {
                    return false;
                }
            }
        }

        @NonNull
        public static PointerBuffer getAllFilters(@NonNull MemoryStack stack) {
            int length = EXTRA_FILTERS.length;
            for (SaveFormat format : OPEN_FORMATS) {
                length += format.filters.length;
            }
            PointerBuffer buffer = stack.mallocPointer(length);
            for (SaveFormat format : OPEN_FORMATS) {
                for (String filter : format.filters) {
                    stack.nUTF8(filter, true);
                    buffer.put(stack.getPointerAddress());
                }
            }
            for (String filter : EXTRA_FILTERS) {
                stack.nUTF8(filter, true);
                buffer.put(stack.getPointerAddress());
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
            return header + " (" + Stream.concat(
                            Arrays.stream(OPEN_FORMATS).flatMap(f -> Arrays.stream(f.filters)),
                            Arrays.stream(EXTRA_FILTERS))
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

    // this ensures unref being called when Bitmap become phantom-reachable
    // but never called to close
    private static final class SafePixels extends Pixels implements Runnable {

        final Cleaner.Cleanable mCleanup;

        private SafePixels(@NonNull Bitmap owner,
                           @NonNull ImageInfo info,
                           long address,
                           int rowBytes,
                           @Nullable LongConsumer freeFn) {
            super(info.width(), info.height(), null, address, rowBytes, freeFn);
            mCleanup = Core.registerCleanup(owner, this);
        }

        @Override
        public void run() {
            unref();
        }
    }
}
