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

package icyllis.modernui.graphics;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.RecordingContext;
import icyllis.arc3d.granite.ImageUtils;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Core;
import org.jetbrains.annotations.ApiStatus;

import java.lang.ref.Cleaner;
import java.lang.ref.Reference;
import java.util.Objects;

/**
 * {@code Image} describes a two-dimensional array of pixels to sample from. The pixels
 * may be located in CPU memory, in GPU memory as a GPU texture, or be lazily-generated.
 * <ul>
 * <li>{@code Image} cannot be modified by CPU after it is created.</li>
 * <li>{@code Image} width and height are greater than zero.</li>
 * <li>{@code Image} may be created from {@link Bitmap}, Surface, Picture
 * and GPU texture.</li>
 * </ul>
 */
//TODO add more usages, now image can only create from Bitmap, only used for drawImage()
public class Image implements AutoCloseable {

    // managed by cleaner
    private volatile icyllis.arc3d.core.Image mImage;
    private final Cleaner.Cleanable mCleanup;

    private Image(@SharedPtr icyllis.arc3d.core.Image image) {
        mImage = Objects.requireNonNull(image);
        mCleanup = Core.registerNativeResource(this, image);
    }

    /**
     * Create an image that backed by a GPU texture with the given bitmap.
     * Whether the bitmap is immutable or not, the bitmap can be safely closed
     * after the call.
     * <p>
     * Must be called after render system and UI system are initialized successfully,
     * <p>
     * Must be called from UI thread
     *
     * @param bitmap the source bitmap
     * @return image, or null if failed
     * @throws NullPointerException bitmap is null or released
     * @throws IllegalStateException not call from UI thread, or no context
     */
    @Nullable
    public static Image createTextureFromBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isClosed()) {
            return null;
        }
        return createTextureFromBitmap(
                Core.requireUiRecordingContext(),
                bitmap
        );
    }

    /**
     * Create an image that backed by a GPU texture with the given bitmap.
     * Whether the bitmap is immutable or not, the bitmap can be safely closed
     * after the call.
     *
     * @param rc     the recording graphics context on the current thread
     * @param bitmap the source bitmap
     * @return image, or null if failed
     * @throws NullPointerException bitmap is null or released
     * @throws IllegalStateException not call from context thread, or no context
     */
    @Nullable
    public static Image createTextureFromBitmap(@NonNull RecordingContext rc,
                                                @NonNull Bitmap bitmap) {
        rc.checkOwnerThread();
        @SharedPtr
        icyllis.arc3d.core.Image image;
        try {
            //TODO we previously make all images Mipmapped, but Arc3D currently does not support
            // Mipmapping correctly
            image = ImageUtils.makeFromPixmap(
                    rc,
                    bitmap.getPixmap(),
                    /*mipmapped*/ false,
                    /*budgeted*/ true,
                    "ImageFromBitmap"
            );
        } finally {
            // Pixels container must be alive!
            Reference.reachabilityFence(bitmap);
        }
        if (image == null) {
            return null;
        }
        return new Image(image); // move
    }

    /**
     * Creates a new image object representing the target resource image.
     * You should use a single image as the UI texture to avoid each icon creating its own image.
     * Underlying resources are automatically released.
     *
     * @param namespace the application namespace
     * @param entry     the sub path to the resource
     * @return the image
     */
    @Nullable
    public static Image create(@NonNull String namespace, @NonNull String entry) {
        return ImageStore.getInstance().getOrCreate(namespace, "textures/" + entry);
    }

    /**
     * Returns the {@link ImageInfo} describing the width, height, color type, alpha type
     * and color space of this image.
     *
     * @return image info
     */
    @ApiStatus.Experimental
    @NonNull
    public ImageInfo getInfo() {
        return mImage.getInfo();
    }

    /**
     * Returns the view width of this image (as its texture).
     *
     * @return image width in pixels
     */
    public int getWidth() {
        return mImage.getWidth();
    }

    /**
     * Returns the view height of this image (as its texture).
     *
     * @return image height in pixels
     */
    public int getHeight() {
        return mImage.getHeight();
    }

    /**
     * Manually mark the underlying resources closed, if needed. After this, you
     * will not be able to operate this object.
     * <p>
     * When this object becomes phantom-reachable, the system will automatically
     * do this cleanup operation.
     */
    @Override
    public void close() {
        mImage = null;
        // cleaner is thread safe
        mCleanup.clean();
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

    /**
     * @hidden
     */
    @ApiStatus.Internal
    @RawPtr
    public icyllis.arc3d.core.Image getNativeImage() {
        return mImage;
    }
}
