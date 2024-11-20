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
import sun.misc.Unsafe;

import java.io.*;
import java.lang.ref.Cleaner;
import java.lang.ref.Reference;
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
 * pixel transfer between CPU and GPU. You cannot use Canvas to draw content onto a Bitmap.
 * <p>
 * Bitmap is created with immutable width, height, row bytes, and memory allocation
 * (memory address), its contents may be changed. You can allocate zero-initialized
 * Bitmap via the static factory methods in this class, i.e. {@link #createBitmap(int, int, Format)}.
 * You can also obtain Bitmap from encoded data (PNG, TGA, BMP, JPEG, HDR, PSD, PBM, PGM, and PPM) via
 * {@link BitmapFactory}.
 * <p>
 * The color space of Bitmap defaults to sRGB and can only be in RGB model space
 * or null, color space may be changed via {@link #setColorSpace(ColorSpace)}.
 * The alpha defaults to non-premultiplied (independent of RGB channels),
 * alpha type may be changed via {@link #setPremultiplied(boolean)}.
 * The Bitmap's format may also be changed via {@link #setFormat(Format)},
 * as long as the bytes-per-pixel of the old and new formats are the same.
 * For example, when using BitmapFactory to read a single channel image,
 * the default format is GRAY_8. You can re-interpret it as ALPHA_8 type via
 * {@link #setFormat(Format)}, and the image data remains unchanged.
 * <p>
 * The Bitmap provides several methods for manipulating the pixel data,
 * there are single pixel and bulk access methods, which may perform zero or more
 * of the following transformations: pixel format (i.e. Format), alpha type
 * (i.e. premultiplied or not), and color space.<br>
 * The pixel data of a bitmap can be marked as immutable through {@link #setImmutable()}.
 * This way, only read-only operations are allowed in the future, avoiding the
 * memory copy in some operations.
 * <p>
 * This class is not thread safe, but memory safe. Its internal state may
 * be shared by multiple threads/owners. Nevertheless, it's recommended to call
 * {@link #close()} explicitly, or within a try-with-resource block.
 *
 * @see Image
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
    // managed by cleaner
    private volatile Pixels mPixels;
    // this ensures unref being called when Bitmap become phantom-reachable
    // but never called to close
    private final Cleaner.Cleanable mCleanup;

    /*
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
        mRequestPremultiplied = info.alphaType() == ColorInfo.AT_PREMUL;
        var pixels = new Pixels(info.width(), info.height(), null, addr, rowBytes, freeFn);
        mCleanup = Core.registerNativeResource(this, pixels);
        mPixels = pixels;
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
        var colorSpace = format.isChannelHDR()
                ? ColorSpace.get(ColorSpace.Named.LINEAR_EXTENDED_SRGB)
                : ColorSpace.get(ColorSpace.Named.SRGB);
        return createBitmap(width, height, format, false, colorSpace);
    }

    /**
     * Creates a mutable bitmap and its allocation, the content are initialized to zeros.
     * <p>
     * You may change the format, alpha type, and color space after creating the bitmap.
     *
     * @param width           width in pixels, must be > 0
     * @param height          height in pixels, must be > 0
     * @param format          a color interpretation
     * @param isPremultiplied an alpha interpretation
     * @param colorSpace      a color space describing the pixels
     * @throws IllegalArgumentException width or height out of range
     * @throws OutOfMemoryError         out of native memory
     */
    @NonNull
    public static Bitmap createBitmap(@Size(min = 1) int width,
                                      @Size(min = 1) int height,
                                      @NonNull Format format,
                                      boolean isPremultiplied,
                                      @Nullable ColorSpace colorSpace) {
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
                throw new OutOfMemoryError("Failed to allocate " + size + " bytes for bitmap");
            }
        }
        int alphaType = format.hasAlpha()
                ? isPremultiplied
                ? ColorInfo.AT_PREMUL
                : ColorInfo.AT_UNPREMUL
                : ColorInfo.AT_OPAQUE;
        alphaType = ColorInfo.validateAlphaType(format.getColorType(), alphaType);
        ImageInfo info = ImageInfo.make(width, height,
                format.getColorType(), alphaType, colorSpace);
        return new Bitmap(format, info, address, rowBytes, MemoryUtil::nmemFree);
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

    /*@ApiStatus.Internal
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
    }*/

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
    @ColorInfo.ColorType
    public int getColorType() {
        return mPixmap.getColorType();
    }

    @ApiStatus.Internal
    @ColorInfo.AlphaType
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

    private void checkOutOfBounds(int x, int y, int width, int height) {
        if (x < 0) {
            throw new IllegalArgumentException("x " + x + " must be >= 0");
        }
        if (y < 0) {
            throw new IllegalArgumentException("y " + y + " must be >= 0");
        }
        if (width < 0) {
            throw new IllegalArgumentException("width " + width + " must be >= 0");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height " + height + " must be >= 0");
        }
        if (x + width > getWidth()) {
            throw new IllegalArgumentException(
                    String.format("x %d + width %d must be <= bitmap.width() %d",
                            x, width, getWidth()));
        }
        if (y + height > getHeight()) {
            throw new IllegalArgumentException(
                    String.format("y %d + height %d must be <= bitmap.height() %d",
                            y, height, getHeight()));
        }
    }

    private void checkOutOfBounds(int x, int y, int width, int height,
                                  int offset, int stride, int length) {
        checkOutOfBounds(x, y, width, height);
        if (stride < width) {
            throw new IllegalArgumentException("stride " + stride + " must be >= width" + width);
        }
        int lastScanline = offset + (height - 1) * stride;
        if (offset < 0 || (offset + width > length)
                || lastScanline < 0
                || (lastScanline + width > length)) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Returns the {@link Color} at the specified location. Throws an exception
     * if x or y are out of bounds (negative or >= to the width or height
     * respectively). The returned color is a non-premultiplied ARGB value in
     * the {@link ColorSpace.Named#SRGB sRGB} color space, the format is the
     * same as {@link Format#BGRA_8888_PACK32}.
     * <p>
     * If the max bits per channel for the color type is greater than 8, or colors are premultiplied,
     * then color precision may be lost in the conversion. Otherwise, precision will not be lost.
     * If the color space is not sRGB, then this method will perform color space transformation,
     * which can be slow.
     * <p>
     * Batch version of this operation: {@link #getPixels(int[], int, int, int, int, int, int)}.
     *
     * @param x The x coordinate (0...width-1) of the pixel to return
     * @param y The y coordinate (0...height-1) of the pixel to return
     * @return The argb {@link Color} at the specified coordinate
     * @throws IllegalArgumentException if x, y exceed the bitmap's bounds
     * @see #getColor4f(int, int, float[])
     */
    @ColorInt
    public int getPixelARGB(int x, int y) {
        checkReleased();
        checkOutOfBounds(x, y);
        if (getColorType() == ColorInfo.CT_UNKNOWN) {
            return 0;
        }
        try {
            return mPixmap.getColor(x, y);
        } finally {
            Reference.reachabilityFence(this);
        }
    }

    /**
     * Gets the pixel value at the specified location. Throws an exception
     * if x or y are out of bounds (negative or >= to the width or height
     * respectively). This method won't perform alpha type or color space conversion,
     * then the result color is converted to RGBA float color, and
     * its color space and alpha type remain unchanged (see {@link #getColorSpace()}
     * and {@link #isPremultiplied()}).
     * <p>
     * Batch version of this operation: {@link #getPixels(float[], int, int, int, int, int, int)}.
     *
     * @param x   The x coordinate (0...width-1) of the pixel to return
     * @param y   The y coordinate (0...height-1) of the pixel to return
     * @param dst The result RGBA float color at the specified coordinate
     * @return the passed float array that contains r,g,b,a values
     * @throws IllegalStateException    the bitmap is released
     * @throws IllegalArgumentException if x, y exceed the bitmap's bounds
     * @see #setColor4f(int, int, float[])
     */
    @NonNull
    @Size(4)
    public float[] getColor4f(int x, int y, @NonNull @Size(4) float[] dst) {
        checkReleased();
        checkOutOfBounds(x, y);
        if (getColorType() == ColorInfo.CT_UNKNOWN) {
            for (int i = 0; i < 4; ++i) {
                dst[i] = 0.0f;
            }
        } else {
            try {
                mPixmap.getColor4f(x, y, dst);
            } finally {
                Reference.reachabilityFence(this);
            }
        }
        return dst;
    }

    /**
     * Sets the pixel value at the specified location. The given float color
     * will convert to bitmap's format. This method won't perform alpha type
     * or color space conversion, then the given color must be in the
     * bitmap's color space {@link #getColorSpace()}, its alpha type must be the
     * same as {@link #isPremultiplied()}). This bitmap must be mutable,
     * that is, {@link #isImmutable()} must return false.
     * <p>
     * Batch version of this operation: {@link #setPixels(float[], int, int, int, int, int, int)}.
     *
     * @param x   The x coordinate of the pixel to replace (0...width-1)
     * @param y   The y coordinate of the pixel to replace (0...height-1)
     * @param src The RGBA float color to write into the bitmap
     * @throws IllegalStateException    the bitmap is released or immutable
     * @throws IllegalArgumentException if x, y exceed the bitmap's bounds
     * @see #getColor4f(int, int, float[])
     */
    public void setColor4f(int x, int y, @NonNull @Size(4) float[] src) {
        checkReleased();
        if (isImmutable()) {
            throw new IllegalStateException("Cannot write to immutable bitmap");
        }
        checkOutOfBounds(x, y);
        if (getColorType() != ColorInfo.CT_UNKNOWN) {
            try {
                mPixmap.setColor4f(x, y, src);
            } finally {
                Reference.reachabilityFence(this);
            }
        }
    }

    /**
     * Copies a Rect of pixels to dst. Copy starts at (srcX, srcY), and does not
     * exceed (width, height). Each value is a packed int representing a {@link Color}.
     * The stride parameter allows the caller to allow for gaps in the returned pixels
     * array between rows. For normal packed results, just pass width for the stride value.
     * The returned colors are non-premultiplied ARGB values in the
     * {@link ColorSpace.Named#SRGB sRGB} color space, i.e. it has
     * {@link Format#BGRA_8888_PACK32} pixel format.
     * <p>
     * If the max bits per channel of {@link #getFormat()} is greater than 8, or colors are
     * premultiplied, then color precision may be lost in the conversion. See
     * {@link #getPixels(float[], int, int, int, int, int, int)}.
     *
     * @param dst    The destination array to receive the bitmap's colors
     * @param offset The first index to write into dst
     * @param stride The number of entries in dst to skip between
     *               rows (must be >= width)
     * @param srcX   The srcX coordinate of the first pixel to read from
     *               the bitmap
     * @param srcY   The srcY coordinate of the first pixel to read from
     *               the bitmap
     * @param width  The number of pixels to read from each row
     * @param height The number of rows to read
     * @throws IllegalStateException          the bitmap is released
     * @throws IllegalArgumentException       if srcX, srcY, width, height exceed the
     *                                        bounds of the bitmap, or if stride < width
     * @throws ArrayIndexOutOfBoundsException if the pixels array is too small
     *                                        to receive the specified number of pixels
     * @see #setPixels(int[], int, int, int, int, int, int)
     */
    public void getPixels(@NonNull @ColorInt int[] dst, int offset, int stride,
                          int srcX, int srcY, int width, int height) {
        checkReleased();
        if (width == 0 || height == 0) {
            return; // nothing to do
        }
        checkOutOfBounds(srcX, srcY, width, height, offset, stride, dst.length);
        if (getColorType() == ColorInfo.CT_UNKNOWN) {
            for (int j = 0; j < height; ++j) {
                int index = offset + stride * j;
                Arrays.fill(dst, index, index + width, 0);
            }
        } else {
            try {
                var dstInfo = new ImageInfo(width, height,
                        ColorInfo.CT_BGRA_8888_NATIVE, ColorInfo.AT_UNPREMUL,
                        ColorSpace.get(ColorSpace.Named.SRGB));
                var dstPixmap = new Pixmap(dstInfo, dst,
                        Unsafe.ARRAY_INT_BASE_OFFSET + (long) offset << 2,
                        stride << 2);
                boolean res = mPixmap.readPixels(dstPixmap, srcX, srcY);
                assert res;
            } finally {
                Reference.reachabilityFence(this);
            }
        }
    }

    /**
     * Copies a Rect of pixels from src. Copy starts at (dstX, dstY), and does not exceed
     * (width, height). Each element in the array is a packed int representing a non-premultiplied
     * ARGB {@link Color} in the {@link ColorSpace.Named#SRGB sRGB} color space, i.e. it has
     * {@link Format#BGRA_8888_PACK32} pixel format. The stride parameter allows the caller
     * to allow for gaps in the source pixels array between rows. For normal packed pixels,
     * just pass width for the stride value.
     * <p>
     * See {@link #setPixels(float[], int, int, int, int, int, int)}.
     *
     * @param src    The source colors to write to the bitmap
     * @param offset The index of the first color to read from src
     * @param stride The number of colors in src to skip between rows
     *               Normally this value will be the same as the width of
     *               the bitmap, but it can be larger
     * @param dstX   The x coordinate of the first pixel to write to in
     *               the bitmap.
     * @param dstY   The y coordinate of the first pixel to write to in
     *               the bitmap.
     * @param width  The number of colors to copy from src per row
     * @param height The number of rows to write to the bitmap
     * @see #getPixels(int[], int, int, int, int, int, int)
     */
    public void setPixels(@NonNull @ColorInt int[] src, int offset, int stride,
                          int dstX, int dstY, int width, int height) {
        checkReleased();
        if (isImmutable()) {
            throw new IllegalStateException("Cannot write to immutable bitmap");
        }
        if (width == 0 || height == 0) {
            return; // nothing to do
        }
        checkOutOfBounds(dstX, dstY, width, height, offset, stride, src.length);
        if (getColorType() != ColorInfo.CT_UNKNOWN) {
            try {
                var srcInfo = new ImageInfo(width, height,
                        ColorInfo.CT_BGRA_8888_NATIVE, ColorInfo.AT_UNPREMUL,
                        ColorSpace.get(ColorSpace.Named.SRGB));
                var srcPixmap = new Pixmap(srcInfo, src,
                        Unsafe.ARRAY_INT_BASE_OFFSET + (long) offset << 2,
                        stride << 2);
                boolean res = mPixmap.writePixels(srcPixmap, dstX, dstY);
                assert res;
            } finally {
                Reference.reachabilityFence(this);
            }
        }
    }

    /**
     * Copies a Rect of pixels to dst. Copy starts at (srcX, srcY), and does not
     * exceed (width, height).
     * The returned colors are (R,G,B,A) float color quadruples in the
     * source color space {@link #getColorSpace()}, in the source alpha type {@link #isPremultiplied()},
     * in {@link Format#RGBA_F32} pixel format.
     * The stride parameter allows the caller to allow for gaps in the returned pixels
     * array between rows. For normal packed results, just pass width for the stride value.
     * <p>
     * Throws {@link ArrayIndexOutOfBoundsException} if
     * {@code (offset + (height - 1) * stride + width) * 4 > dst.length}.
     *
     * @param dst    The destination array to receive the bitmap's colors
     * @param offset The first color to write into dst
     * @param stride The number of colors in dst to skip between
     *               rows (must be >= width)
     * @param srcX   The srcX coordinate of the first pixel to read from
     *               the bitmap
     * @param srcY   The srcY coordinate of the first pixel to read from
     *               the bitmap
     * @param width  The number of pixels to read from each row
     * @param height The number of rows to read
     * @throws IllegalStateException          the bitmap is released
     * @throws IllegalArgumentException       if srcX, srcY, width, height exceed the
     *                                        bounds of the bitmap, or if stride < width
     * @throws ArrayIndexOutOfBoundsException if the pixels array is too small
     *                                        to receive the specified number of pixels
     * @see #setPixels(float[], int, int, int, int, int, int)
     */
    public void getPixels(@NonNull @Size(multiple = 4) float[] dst, int offset, int stride,
                          int srcX, int srcY, int width, int height) {
        checkReleased();
        if (width == 0 || height == 0) {
            return; // nothing to do
        }
        checkOutOfBounds(srcX, srcY, width, height, offset, stride, dst.length >> 2);
        if (getColorType() == ColorInfo.CT_UNKNOWN) {
            for (int j = 0; j < height; ++j) {
                int index = (offset + stride * j) << 2;
                Arrays.fill(dst, index, index + (width << 2), 0.0f);
            }
        } else {
            try {
                var dstInfo = new ImageInfo(width, height,
                        ColorInfo.CT_RGBA_F32, getAlphaType(),
                        getColorSpace());
                var dstPixmap = new Pixmap(dstInfo, dst,
                        Unsafe.ARRAY_FLOAT_BASE_OFFSET + (long) offset << 4,
                        stride << 4);
                boolean res = mPixmap.readPixels(dstPixmap, srcX, srcY);
                assert res;
            } finally {
                Reference.reachabilityFence(this);
            }
        }
    }

    /**
     * Copies a Rect of pixels from src. Copy starts at (dstX, dstY), and does not exceed
     * (width, height). Each element in the array is a quadruple that contains (R,G,B,A)
     * float color components source color space {@link #getColorSpace()}, in the source
     * alpha type {@link #isPremultiplied()}, in {@link Format#RGBA_F32} pixel format.
     * The stride parameter allows the caller
     * to allow for gaps in the source pixels array between rows. For normal packed pixels,
     * just pass width for the stride value.
     * <p>
     * Throws {@link ArrayIndexOutOfBoundsException} if
     * {@code (offset + (height - 1) * stride + width) * 4 > dst.length}.
     *
     * @param src    The source colors to write to the bitmap
     * @param offset The index of the first color to read from src
     * @param stride The number of colors in src to skip between rows
     *               Normally this value will be the same as the width of
     *               the bitmap, but it can be larger
     * @param dstX   The x coordinate of the first pixel to write to in
     *               the bitmap.
     * @param dstY   The y coordinate of the first pixel to write to in
     *               the bitmap.
     * @param width  The number of colors to copy from src per row
     * @param height The number of rows to write to the bitmap
     * @see #getPixels(float[], int, int, int, int, int, int)
     */
    public void setPixels(@NonNull @Size(multiple = 4) float[] src, int offset, int stride,
                          int dstX, int dstY, int width, int height) {
        checkReleased();
        if (isImmutable()) {
            throw new IllegalStateException("Cannot write to immutable bitmap");
        }
        if (width == 0 || height == 0) {
            return; // nothing to do
        }
        checkOutOfBounds(dstX, dstY, width, height, offset, stride, src.length >> 2);
        if (getColorType() != ColorInfo.CT_UNKNOWN) {
            try {
                var srcInfo = new ImageInfo(width, height,
                        ColorInfo.CT_RGBA_F32, getAlphaType(),
                        getColorSpace());
                var srcPixmap = new Pixmap(srcInfo, src,
                        Unsafe.ARRAY_FLOAT_BASE_OFFSET + (long) offset << 4,
                        stride << 4);
                boolean res = mPixmap.writePixels(srcPixmap, dstX, dstY);
                assert res;
            } finally {
                Reference.reachabilityFence(this);
            }
        }
    }

    /**
     * Copies a Rect of pixels from this bitmap to dst bitmap at (dstX, dstY).
     * Copy starts at (srcX, srcY), and does not exceed (width, height).
     * This method will perform pixel format and alpha type conversion (see
     * {@link #getFormat()} and {@link #isPremultiplied()}), and color space transformation
     * (see {@link #getColorSpace()}).
     * <p>
     * The behavior is undefined if there is memory overlap between this bitmap and the
     * destination bitmap.
     *
     * @param dst    The destination bitmap to receive this bitmap's colors
     * @param dstX   The dstX coordinate of the first pixel to write to
     *               the bitmap
     * @param dstY   The dstY coordinate of the first pixel to write to
     *               the bitmap
     * @param srcX   The srcX coordinate of the first pixel to read from
     *               the bitmap
     * @param srcY   The srcY coordinate of the first pixel to read from
     *               the bitmap
     * @param width  The number of pixels to read from each row
     * @param height The number of rows to read
     * @throws IllegalStateException    the bitmap is released or immutable
     * @throws IllegalArgumentException if srcX, srcY, dstX, dstY, width, height exceed the
     *                                  bounds of the bitmap
     * @see #setPixels(Bitmap, int, int, int, int, int, int)
     */
    public void getPixels(@NonNull Bitmap dst, int dstX, int dstY,
                          int srcX, int srcY, int width, int height) {
        checkReleased();
        dst.checkReleased();
        if (dst.isImmutable()) {
            throw new IllegalStateException("Cannot write to immutable bitmap");
        }
        if (width == 0 || height == 0) {
            return; // nothing to do
        }
        checkOutOfBounds(srcX, srcY, width, height);
        dst.checkOutOfBounds(dstX, dstY, width, height);
        var dstRect = new Rect2i(dstX, dstY, dstX + width, dstY + height);
        try {
            if (getColorType() == ColorInfo.CT_UNKNOWN) {
                dst.getPixmap().clear(new float[]{0, 0, 0, 0}, dstRect);
            } else {
                var dstPixmap = dst.getPixmap().makeSubset(dstRect);
                assert dstPixmap != null;
                boolean res = mPixmap.readPixels(dstPixmap, srcX, srcY);
                assert res;
            }
        } finally {
            Reference.reachabilityFence(this);
        }
    }

    /**
     * Copies a Rect of pixels from src bitmap at (srcX, srcY) to this bitmap.
     * Copy starts at (dstX, dstY), and does not exceed (width, height).
     * This method will perform pixel format and alpha type conversion (see
     * {@link #getFormat()} and {@link #isPremultiplied()}), and color space transformation
     * (see {@link #getColorSpace()}).
     * <p>
     * The behavior is undefined if there is memory overlap between this bitmap and the
     * source bitmap.
     *
     * @param src    The source bitmap to write to this bitmap
     * @param srcX   The srcX coordinate of the first pixel to read from
     *               the bitmap
     * @param srcY   The srcY coordinate of the first pixel to read from
     *               the bitmap
     * @param dstX   The dstX coordinate of the first pixel to write to
     *               the bitmap
     * @param dstY   The dstY coordinate of the first pixel to write to
     *               the bitmap
     * @param width  The number of pixels to read from each row
     * @param height The number of rows to read
     * @throws IllegalStateException    the bitmap is released or immutable
     * @throws IllegalArgumentException if srcX, srcY, dstX, dstY, width, height exceed the
     *                                  bounds of the bitmap
     * @see #getPixels(Bitmap, int, int, int, int, int, int)
     */
    public void setPixels(@NonNull Bitmap src, int srcX, int srcY,
                          int dstX, int dstY, int width, int height) {
        checkReleased();
        src.checkReleased();
        if (isImmutable()) {
            throw new IllegalStateException("Cannot write to immutable bitmap");
        }
        if (width == 0 || height == 0) {
            return; // nothing to do
        }
        checkOutOfBounds(srcX, srcY, width, height);
        src.checkOutOfBounds(dstX, dstY, width, height);
        var srcRect = new Rect2i(srcX, srcY, srcX + width, srcY + height);
        try {
            if (getColorType() == ColorInfo.CT_UNKNOWN) {
                mPixmap.clear(new float[]{0, 0, 0, 0}, srcRect);
            } else {
                var srcPixmap = src.getPixmap().makeSubset(srcRect);
                assert srcPixmap != null;
                boolean res = mPixmap.writePixels(srcPixmap, dstX, dstY);
                assert res;
            }
        } finally {
            Reference.reachabilityFence(this);
        }
    }

    /**
     * Copy the bitmap's pixels into the specified buffer. The buffer can be a
     * heap buffer, a direct buffer, or a native buffer (by wrapping an address).
     * Buffer's base array, address and position will be used, other properties like
     * {@link java.nio.Buffer#limit()} are ignored, this method always assumes
     * the byte order is the host endianness, i.e. {@link java.nio.ByteOrder#nativeOrder()},
     * and reinterpret the data to buffer's basic type.
     * <p>
     * This method does <em>not</em> check for invalid, out of bounds, or misaligned
     * addresses. If the memory is managed by Buffer (non-wrapped direct buffer),
     * it will be guaranteed to be alive. Note {@link java.nio.Buffer#isReadOnly()}
     * must return false.
     * <p>
     * The content of the bitmap is copied into the buffer as-is. This means
     * that if this bitmap has premultiplied colors (see {@link #isPremultiplied()}),
     * the values in the buffer will also be premultiplied. The pixels remain in the
     * {@link Format} and color space of the bitmap.
     * <p>
     * After this method returns, the current position of the buffer remains unchanged.
     * <p>
     * This method may return false if dst is nullptr or address it not aligned to the
     * bytes-per-pixel or the size of basic data type (according to {@link Format}).
     * May return false if rowBytes is less than the minRowBytes (width * bpp).
     *
     * @return true if pixels are copied to dst
     * @throws IllegalStateException    the bitmap is released
     * @throws IllegalArgumentException if srcX, srcY, width, height exceed the
     *                                  bounds of the bitmap
     * @see #copyPixelsFromBuffer(java.nio.Buffer, int, int, int, int, int)
     */
    public boolean copyPixelsToBuffer(@NonNull java.nio.Buffer dst, int rowBytes,
                                      int srcX, int srcY, int width, int height) {
        checkReleased();
        if (dst.isReadOnly()) {
            throw new IllegalArgumentException("Cannot copy to read-only buffer");
        }
        if (width == 0 || height == 0) {
            return false; // nothing to do
        }
        checkOutOfBounds(srcX, srcY, width, height);
        try {
            var dstInfo = getInfo().makeWH(width, height);
            var dstPixmap = new Pixmap(dstInfo,
                    dst.hasArray() ? dst.array() : null,
                    MemoryUtil.memAddress(dst),
                    rowBytes);
            return mPixmap.readPixels(dstPixmap, srcX, srcY);
        } finally {
            Reference.reachabilityFence(dst);
            Reference.reachabilityFence(this);
        }
    }

    /**
     * Copy the pixels from the specified buffer into bitmap. The buffer can be a
     * heap buffer, a direct buffer, or a native buffer (by wrapping an address).
     * Buffer's base array, address and position will be used, other properties like
     * {@link java.nio.Buffer#limit()} are ignored, this method always assumes
     * the byte order is the host endianness, i.e. {@link java.nio.ByteOrder#nativeOrder()},
     * and reinterpret the data from buffer's basic type.
     * <p>
     * This method does <em>not</em> check for invalid, out of bounds, or misaligned
     * addresses. If the memory is managed by Buffer (non-wrapped direct buffer),
     * it will be guaranteed to be alive. Note {@link java.nio.Buffer#isReadOnly()}
     * must return false.
     * <p>
     * The content of the buffer is copied into the bitmap as-is. This means
     * that if this bitmap has premultiplied colors (see {@link #isPremultiplied()}),
     * the values in the buffer should also be premultiplied. The pixels remain in the
     * {@link Format} and color space of the bitmap.
     * <p>
     * After this method returns, the current position of the buffer remains unchanged.
     * <p>
     * This method may return false if dst is nullptr or address it not aligned to the
     * bytes-per-pixel or the size of basic data type (according to {@link Format}).
     * May return false if rowBytes is less than the minRowBytes (width * bpp).
     *
     * @return true if src pixels are copied to bitmap
     * @throws IllegalStateException    the bitmap is released
     * @throws IllegalArgumentException if dstX, dstY, width, height exceed the
     *                                  bounds of the bitmap
     * @see #copyPixelsToBuffer(java.nio.Buffer, int, int, int, int, int)
     */
    public boolean copyPixelsFromBuffer(@NonNull java.nio.Buffer src, int rowBytes,
                                        int dstX, int dstY, int width, int height) {
        checkReleased();
        if (isImmutable()) {
            throw new IllegalStateException("Cannot write to immutable bitmap");
        }
        if (src.isReadOnly()) {
            // if we want to peek the underlying src.array(), it must not be read-only
            throw new IllegalArgumentException("Cannot copy from read-only buffer");
        }
        if (width == 0 || height == 0) {
            return false; // nothing to do
        }
        checkOutOfBounds(dstX, dstY, width, height);
        try {
            var srcInfo = getInfo().makeWH(width, height);
            var srcPixmap = new Pixmap(srcInfo,
                    src.hasArray() ? src.array() : null,
                    MemoryUtil.memAddress(src),
                    rowBytes);
            return mPixmap.writePixels(srcPixmap, dstX, dstY);
        } finally {
            Reference.reachabilityFence(src);
            Reference.reachabilityFence(this);
        }
    }

    // this approach is slow
    /*public boolean clear(@ColorInt int color, @Nullable Rect area) {
        checkReleased();
        if (isImmutable()) {
            throw new IllegalStateException("Cannot clear immutable bitmaps");
        }
        var srcInfo = new ImageInfo(1, 1, ColorInfo.CT_BGRA_8888_NATIVE,
                ColorInfo.AT_UNPREMUL, ColorSpace.get(ColorSpace.Named.SRGB));
        var dstInfo = new ImageInfo(1, 1, ColorInfo.CT_RGBA_F32,
                getAlphaType(), getColorSpace());
        int[] src = {color};
        float[] dst = new float[4];
        boolean res = PixelUtils.convertPixels(
                srcInfo, src, Unsafe.ARRAY_INT_BASE_OFFSET, getRowBytes(),
                dstInfo, dst, Unsafe.ARRAY_FLOAT_BASE_OFFSET, getRowBytes()
        );
        assert res;
        return mPixmap.clear(dst, area == null ? null : new Rect2i(
                area.left, area.top, area.right, area.bottom
        ));
    }*/

    /**
     * Fills the bitmap's pixels with the specified color and rectangular area.
     * The given float color is in the source color space {@link #getColorSpace()}, in the
     * source alpha type {@link #isPremultiplied()}, in {@link Format#RGBA_F32} pixel format.
     * That is, this method will only perform pixel format conversion, and will not perform
     * alpha type or color space transformation.
     *
     * @param color the (R,G,B,A) color to fill
     * @param area  bounding box of pixels to fill; null means full bitmap
     * @return true if pixels are changed
     * @throws IllegalStateException the bitmap is released or immutable
     */
    public boolean clear(@NonNull @Size(4) float[] color, @Nullable Rect area) {
        checkReleased();
        if (isImmutable()) {
            throw new IllegalStateException("Cannot clear immutable bitmaps");
        }
        try {
            return mPixmap.clear(color, area == null ? null : new Rect2i(
                    area.left, area.top, area.right, area.bottom
            ));
        } finally {
            Reference.reachabilityFence(this);
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
            throw new IOException("Pixel data is not tightly packed");
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
        } finally {
            Reference.reachabilityFence(this);
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
        mPixels = null;
        // cleaner is thread safe
        mCleanup.clean();
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
            //TODO allow saving an bitmap whose rowBytes > minRowBytes
            //  also add TIFF support via ImageIO
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
        public String getDefaultExtension() {
            // remove the asterisk
            return filters[0].substring(1);
        }

        @NonNull
        public static String getFileName(@Nullable SaveFormat format,
                                         @Nullable String name) {
            String s = name != null ? name : "image-" + DATE_FORMAT.format(new Date());
            if (format != null) {
                return s + format.getDefaultExtension();
            }
            return s;
        }
    }
}
