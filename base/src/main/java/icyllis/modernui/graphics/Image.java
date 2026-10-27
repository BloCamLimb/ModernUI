/*
 * ModernUI.
 * Copyright (C) 2021-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.graphics;

import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.core.ColorSpace;
import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.granite.RecordingContext;
import icyllis.modernui.graphics.drawable.ImageDrawable;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.system.Arch;
import icyllis.modernui.util.DisplayMetrics;
import icyllis.modernui.util.Unchecked;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.ref.Cleaner;
import java.lang.ref.Reference;
import java.util.Objects;
import java.util.concurrent.FutureTask;
import java.util.function.BiFunction;

/**
 * {@code Image} describes a two-dimensional array of pixels to sample from. The pixels
 * may be located in CPU memory, in GPU memory as a GPU texture, or be lazily-generated.
 * <ul>
 * <li>{@code Image} cannot be modified by CPU or GPU after it is created.</li>
 * <li>{@code Image} width and height are greater than zero.</li>
 * <li>{@code Image} may be created from {@link Bitmap}, Surface, Picture
 * and GPU texture.</li>
 * </ul>
 */
//TODO add more usages, now image can only create from Bitmap, only used for drawImage()
// mark this immutable and thread-safe, once `density` field is completely removed
public class Image implements AutoCloseable {

    /**
     * Indicates that the image was created for an unknown pixel density
     * and will not be scaled.
     *
     * @see Image#getDensity()
     * @see Image#setDensity(int)
     *
     * @deprecated use {@link DisplayMetrics#DENSITY_NONE}
     */
    @Deprecated
    public static final int DENSITY_NONE = DisplayMetrics.DENSITY_NONE;

    // managed by cleaner
    private volatile icyllis.arc3d.sketch.Image mImage;
    private final Cleaner.Cleanable mCleanup;

    // keep a strong reference to prevent it from being garbage collected
    private Object mAttachment;

    int mDensity = DisplayMetrics.DENSITY_DEFAULT;

    private Image(@SharedPtr icyllis.arc3d.sketch.Image image) {
        mCleanup = Arch.registerNativeResource(this, image);
        mImage = Objects.requireNonNull(image);
    }

    private Image(@SharedPtr icyllis.arc3d.sketch.Image image, int density, Object attachment) {
        this(image);
        mDensity = density;
        mAttachment = attachment;
    }

    /**
     * Creates an image backed by a GPU texture from the given bitmap.
     * <p>
     * If you want to create an image for a specific sub-region of the given bitmap,
     * you should first call {@link Bitmap#subImage(int, int, int, int)} to obtain
     * the sub-image view, and then pass the resulting bitmap to this method.
     * <p>
     * The bitmap may be safely closed after this call returns, regardless of whether
     * it is immutable. However, it must remain open during the execution of this call.
     * <p>
     * As of version 3.13.0, this method is thread-safe and can be invoked from any
     * thread except the internal rendering thread. Note that calling this from a
     * non-UI thread will block the caller until the operation is completed on the UI thread.
     * <p>
     * <b>Legacy Note (Pre-3.13.0):</b> This method was restricted to the UI thread and
     * required both the render and UI systems to be fully initialized before invocation.
     * <p>
     * This method may return {@code null} if:
     * <ul>
     * <li>The bitmap is null or already closed.</li>
     * <li>The GPU device is lost or disconnected.</li>
     * <li>The GPU device is not yet initialized.</li>
     * <li>The width or height of the bitmap exceeds the maximum texture dimension supported by the GPU.</li>
     * <li>The bitmap format is not supported (directly or indirectly) by the GPU.</li>
     * <li>Failed to allocate sufficient GPU-only memory for the texture.</li>
     * <li>Failed to allocate sufficient host memory for the staging buffer.</li>
     * <li>The UI thread is quitting.</li>
     * </ul>
     *
     * @param bitmap the source bitmap
     * @return the created image, or {@code null} if the operation failed.
     */
    @Nullable
    public static Image createTextureFromBitmap(@Nullable Bitmap bitmap) {
        if (bitmap == null || bitmap.isClosed()) {
            return null;
        }
        var uiRecordingContext = Arch.peekUiRecordingContext();
        if (uiRecordingContext == null) {
            return null;
        }
        if (Arch.isOnUiThread()) {
            return createTextureFromBitmap(
                    uiRecordingContext,
                    bitmap
            );
        }
        if (Arch.isOnRenderThread()) {
            throw new IllegalStateException("Cannot be called from rendering thread");
        }
        FutureTask<Image> future = new FutureTask<>(() ->
                createTextureFromBitmap(
                        uiRecordingContext,
                        bitmap
                )
        );
        if (!Arch.getUiHandlerAsync().post(future)) {
            return null;
        }
        return Unchecked.getUninterruptibly(future);
    }

    /**
     * Create an image that backed by a GPU texture with the given bitmap.
     * Whether the bitmap is immutable or not, the bitmap can be safely closed
     * after the call.
     *
     * @param recordingContext the recording graphics context on the current thread
     * @param bitmap           the source bitmap
     * @return image, or null if failed
     * @throws NullPointerException  bitmap is null or released
     * @throws IllegalStateException not call from context thread, or no context
     */
    @ApiStatus.Experimental
    @Nullable
    public static Image createTextureFromBitmap(@NonNull RecordingContext recordingContext,
                                                @NonNull Bitmap bitmap) {
        recordingContext.checkOwnerThread();
        assert !bitmap.isClosed();
        @SharedPtr
        icyllis.arc3d.sketch.Image nativeImage;
        try {
            //TODO we previously make all images Mipmapped, but Arc3D currently does not support
            // Mipmapping correctly

            // because the return value is owned by client, budgeted = false to
            // ensure Arc3D GPU ResourceCache work correctly
            nativeImage = icyllis.arc3d.granite.TextureUtils.makeFromPixmap(
                    recordingContext,
                    bitmap.getPixmap(),
                    /*mipmapped*/ false,
                    /*budgeted*/ false,
                    "ImageFromBitmap"
            );
        } finally {
            // Pixels container must be alive!
            Reference.reachabilityFence(bitmap);
        }
        if (nativeImage == null) {
            return null;
        }
        return new Image(nativeImage); // move
    }

    private static volatile BiFunction<String, String, Image> sLegacyFactory;

    /**
     * Creates a new image object representing the target resource image.
     * You should use a single image as the UI texture to avoid each icon creating its own image.
     * Underlying resources are automatically released.
     * <p>
     * Do NOT close the returned Image.
     *
     * @param namespace the application namespace
     * @param entry     the sub path to the resource
     * @return the image
     * @deprecated use the new Resources API, {@link Resources#getImage(ResourceId, boolean)},
     * {@link Resources#getDrawable(ResourceId, Resources.Theme)}
     */
    @Deprecated
    @Nullable
    public static Image create(@NonNull String namespace, @NonNull String entry) {
        var factory = sLegacyFactory;
        if (factory != null) {
            return factory.apply(namespace, "textures/" + entry);
        }
        return null;
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public static void setLegacyFactory(BiFunction<String, String, /*nullable*/ Image> /*noexcept*/ legacyFactory) {
        sLegacyFactory = legacyFactory;
    }

    /**
     * Returns the {@link ImageInfo} describing the width, height, color type, alpha type
     * and color space of this image.
     *
     * @return image info
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    @NonNull
    public ImageInfo getInfo() {
        return mImage.getInfo();
    }

    /**
     * Returns the intrinsic width of this image.
     *
     * @return image width in pixels
     */
    public int getWidth() {
        return mImage.getWidth();
    }

    /**
     * Returns the intrinsic height of this image.
     *
     * @return image height in pixels
     */
    public int getHeight() {
        return mImage.getHeight();
    }

    /**
     * <p>Returns the density for this image.</p>
     *
     * <p>The default density is {@link DisplayMetrics#DENSITY_DEFAULT}.</p>
     *
     * @return A scaling factor of the default density or {@link #DENSITY_NONE}
     * if the scaling factor is unknown.
     * @see #setDensity(int)
     * @see DisplayMetrics#DENSITY_DEFAULT
     * @see DisplayMetrics#densityDpi
     * @see #DENSITY_NONE
     *
     * @deprecated to keep Image immutable and pure, additional information such as density
     * should be represented externally, such as {@link ImageDrawable#setSourceDensity(int)}
     */
    @Deprecated
    public int getDensity() {
        return mDensity;
    }

    /**
     * <p>Specifies the density for this image.  When the image is
     * drawn to a {@link Canvas} or with a {@link ImageDrawable}
     * that also has a density, it will be scaled appropriately.</p>
     *
     * @param density The density scaling factor to use with this image or
     *                {@link #DENSITY_NONE} if the density is unknown.
     * @see #getDensity()
     * @see DisplayMetrics#DENSITY_DEFAULT
     * @see DisplayMetrics#densityDpi
     * @see #DENSITY_NONE
     * @see #clone()
     *
     * @deprecated to keep Image immutable and pure, additional information such as density
     * should be represented externally, such as {@link ImageDrawable#setSourceDensity(int)}
     */
    @Deprecated
    public void setDensity(int density) {
        mDensity = density;
    }

    /**
     * Copy the image's bounds to the specified Rect.
     *
     * @param bounds Rect to receive the image's bounds
     */
    public void getBounds(@NonNull Rect bounds) {
        bounds.set(0, 0, getWidth(), getHeight());
    }

    /**
     * Copy the image's bounds to the specified RectF.
     *
     * @param bounds RectF to receive the image's bounds
     */
    public void getBounds(@NonNull RectF bounds) {
        bounds.set(0, 0, getWidth(), getHeight());
    }

    /**
     * Returns whether this Image has only an alpha channel (i.e. a transparency mask).
     * If so, then when drawn its color depends on the solid color of Paint, or
     * Paint's shader if set.
     *
     * @return whether the image is alpha only
     */
    public boolean isAlphaMask() {
        return mImage.isAlphaOnly();
    }

    /**
     * Returns whether the Image is completely opaque. Returns true if this
     * Image has no alpha channel, or is flagged to be known that all of its
     * pixels are opaque.
     *
     * @return whether the image is opaque
     */
    public boolean isOpaque() {
        return mImage.isOpaque();
    }

    /**
     * Indicates whether the pixels stored in this bitmap are stored in a premultiplied format.
     * <p>
     * When a pixel is premultiplied, the RGB components have already been multiplied by
     * the alpha component. For instance, if the original color is a 50% translucent red
     * {@code (1.0, 0.0, 0.0, 0.5)}, its premultiplied form is {@code (0.5, 0.0, 0.0, 0.5)}.
     *
     * @return true if the underlying pixels are premultiplied, false otherwise
     * @since 3.13.0
     */
    public boolean isPremultiplied() {
        return mImage.getAlphaType() == ColorInfo.AT_PREMUL;
    }

    /**
     * Returns the color space associated with this image. If the color
     * space is unknown, this method returns null.
     */
    @Nullable
    public ColorSpace getColorSpace() {
        return mImage.getColorSpace();
    }

    /**
     * Returns whether this image is backed by a CPU bitmap.
     *
     * @since 3.13.0
     */
    public boolean isRasterBacked() {
        return mImage.isRasterBacked();
    }

    /**
     * Returns whether this image is backed by a GPU texture.
     *
     * @since 3.13.0
     */
    public boolean isTextureBacked() {
        return mImage.isTextureBacked();
    }

    /**
     * Perform a deferred cleanup if the underlying resource is not released.
     * Manually mark the underlying resources closed, if needed. After this, you
     * will not be able to operate this object, and its GPU resource will be reclaimed
     * as soon as possible after use.
     * <p>
     * If this Image object was not created by you (for example, from Resources),
     * then you must <em>not</em> call this method.
     * <p>
     * When this object becomes phantom-reachable, the system will automatically
     * do this cleanup operation.
     * <p>
     * This method can be called multiple times.
     */
    @Override
    public void close() {
        if (mImage == null) {
            return;
        }
        // lock to ensure refNativeImage() thread-safe
        synchronized (this) {
            mImage = null;
            // cleaner is thread safe, so no double check
            mCleanup.clean();
        }
    }

    /**
     * Returns true if this image has been closed. If so, then it is an error
     * to try to access it.
     *
     * @return true if the image has been closed
     */
    public boolean isClosed() {
        return mImage == null;
    }

    @Override
    public String toString() {
        return "Image{" +
                "mImage=" + mImage +
                ", mDensity=" + mDensity +
                '}';
    }

    /**
     * Create a shallow copy of this image, this is equivalent to creating a
     * shared owner for the image. You may change the density or close the
     * returned Image without affecting the original Image.
     * <p>
     * This method is rarely used. If this image originates from the Resources API
     * and you wish to manage its lifecycle yourself, set the <var>needNewInstance</var>
     * of {@link Resources#getImage(ResourceId, boolean)}
     * to true instead of calling this method. If you created the image yourself,
     * this method is typically unnecessary unless you intend to pass it to another owner;
     * in cases where both you and the new owner will close their respective objects,
     * using this method ensures mutual independence and timely release.
     * <p>
     * This method is thread safe.
     *
     * @return a shallow copy of image
     * @throws IllegalStateException this image is already closed
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @NonNull
    public final Image clone() {
        icyllis.arc3d.sketch.Image image = refNativeImage();
        if (image == null) {
            throw new IllegalStateException("Cannot clone a closed image!");
        }
        Object attachment = mAttachment;
        return new Image(image, mDensity, attachment != null ? attachment : this);
    }

    /**
     * Internally used by Resources API.
     *
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public void clearAttachment() {
        mAttachment = null;
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    @SharedPtr
    public icyllis.arc3d.sketch.@Nullable Image refNativeImage() {
        try {
            synchronized (this) {
                // protect against concurrent manual close()
                return RefCnt.create(mImage);
            }
        } finally {
            // protect against concurrent auto GC
            Reference.reachabilityFence(this);
        }
    }
}
