/*
 * Modern UI.
 * Copyright (C) 2021-2025 BloCamLimb. All rights reserved.
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

import icyllis.arc3d.core.ColorSpace;
import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.granite.RecordingContext;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Core;
import icyllis.modernui.graphics.drawable.ImageDrawable;
import icyllis.modernui.resources.IOUtil;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.util.DisplayMetrics;
import org.jetbrains.annotations.ApiStatus;

import java.lang.ref.Cleaner;
import java.lang.ref.Reference;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

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
public class Image implements AutoCloseable {

    /**
     * Indicates that the image was created for an unknown pixel density
     * and will not be scaled.
     *
     * @see Image#getDensity()
     * @see Image#setDensity(int)
     */
    public static final int DENSITY_NONE = 0;

    // managed by cleaner
    private volatile icyllis.arc3d.sketch.Image mImage;
    private final Cleaner.Cleanable mCleanup;

    // keep a strong reference to prevent it from being garbage collected
    private ConstantState mConstantState;

    int mDensity = DisplayMetrics.DENSITY_DEFAULT;

    private Image(@SharedPtr icyllis.arc3d.sketch.Image image) {
        mCleanup = Core.registerNativeResource(this, image);
        mImage = Objects.requireNonNull(image);
    }

    private Image(@SharedPtr icyllis.arc3d.sketch.Image image, int density) {
        this(image);
        mDensity = density;
    }

    /**
     * Creates an image backed by a GPU texture from the given bitmap.
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
        var uiRecordingContext = Core.peekUiRecordingContext();
        if (uiRecordingContext == null) {
            return null;
        }
        if (Core.isOnUiThread()) {
            return createTextureFromBitmap(
                    uiRecordingContext,
                    bitmap
            );
        }
        if (Core.isOnRenderThread()) {
            throw new IllegalStateException("Cannot be called from rendering thread");
        }
        FutureTask<Image> future = new FutureTask<>(() ->
                createTextureFromBitmap(
                        uiRecordingContext,
                        bitmap
                )
        );
        if (!Core.getUiHandlerAsync().post(future)) {
            return null;
        }
        boolean interrupted = false;
        try {
            for (;;) {
                try {
                    return future.get();
                } catch (ExecutionException e) {
                    var cause = e.getCause();
                    throw IOUtil.sneakyThrow(cause != null ? cause : e);
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted)
                Thread.currentThread().interrupt();
        }
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
        return ImageStore.getInstance().getOrCreate(namespace, "textures/" + entry);
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
     * Convenience method that returns the width of this image divided
     * by the density scale factor.
     * <p>
     * Returns the image's width multiplied by the ratio of the target density to the image's
     * source density
     *
     * @param targetDensity The density of the target canvas of the image.
     * @return The scaled width of this image, according to the density scale factor.
     */
    public int getScaledWidth(int targetDensity) {
        return scaleFromDensity(getWidth(), mDensity, targetDensity);
    }

    /**
     * Convenience method that returns the height of this image divided
     * by the density scale factor.
     * <p>
     * Returns the image's height multiplied by the ratio of the target density to the image's
     * source density
     *
     * @param targetDensity The density of the target canvas of the image.
     * @return The scaled height of this image, according to the density scale factor.
     */
    public int getScaledHeight(int targetDensity) {
        return scaleFromDensity(getHeight(), mDensity, targetDensity);
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public static int scaleFromDensity(int size, int sourceDensity, int targetDensity) {
        if (sourceDensity == DENSITY_NONE || targetDensity == DENSITY_NONE || sourceDensity == targetDensity) {
            return size;
        }

        // Scale by targetDensity / sourceDensity, rounding up.
        return ((size * targetDensity) + (sourceDensity >> 1)) / sourceDensity;
    }

    /**
     * <p>Returns the density for this image.</p>
     *
     * <p>The default density is {@link DisplayMetrics#DENSITY_DEFAULT}..</p>
     *
     * @return A scaling factor of the default density or {@link #DENSITY_NONE}
     * if the scaling factor is unknown.
     * @see #setDensity(int)
     * @see DisplayMetrics#DENSITY_DEFAULT
     * @see DisplayMetrics#densityDpi
     * @see #DENSITY_NONE
     */
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
     */
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
     * Returns the color space associated with this image. If the color
     * space is unknown, this method returns null.
     */
    @Nullable
    public ColorSpace getColorSpace() {
        return mImage.getColorSpace();
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
     * This method is thread safe and can be called multiple times.
     */
    @Override
    public void close() {
        if (mImage == null) {
            return;
        }
        // lock to ensure clone() thread-safe
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
        icyllis.arc3d.sketch.Image image;
        try {
            synchronized (this) {
                // this operation is not atomic
                image = RefCnt.create(mImage);
            }
        } finally {
            // there may be lock elimination, this is still needed
            Reference.reachabilityFence(this);
        }
        if (image == null) {
            throw new IllegalStateException("Cannot clone a closed image!");
        }
        // we disown this.mConstantState which is only used for the Resources API;
        // it shouldn't be used in other scenarios.
        return new Image(image, mDensity);
    }

    /**
     * Internally used by Resources API.
     *
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public ConstantState getConstantState() {
        // set mutual references
        if (mConstantState == null) {
            mConstantState = new ConstantState(this);
        }
        return mConstantState;
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    @RawPtr
    public icyllis.arc3d.sketch.Image getNativeImage() {
        return mImage;
    }

    /**
     * Internally used by Resources API.
     *
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public static final class ConstantState {

        // CS -> Img0
        // Img0 -> CS
        // Img1 -> CS
        // Img2 -> CS
        // Drawable -> Img
        // Cache -> weak CS
        // Cache -> weak Drawable

        private Image mImage; // Img0
        private int mChangingConfigurations;

        public ConstantState(Image image) {
            mImage = image;
        }

        public void setChangingConfigurations(int changeConfigs) {
            mChangingConfigurations = changeConfigs;
        }

        public int getChangingConfigurations() {
            return mChangingConfigurations;
        }

        // the shared instance
        public Image getImage() {
            return mImage;
        }

        // a new instance that will not be closed when this.close() is called,
        // and prevent this (and original mImage) from being GC.
        public Image newImage() {
            Image image = mImage.clone();
            image.mConstantState = this;
            return image;
        }

        public void close() {
            mImage.close();
            mImage = null;
        }
    }
}
