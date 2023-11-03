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

import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Core;
import org.jetbrains.annotations.ApiStatus;

import java.lang.ref.Cleaner;

/**
 * {@code Image} describes a two-dimensional array of pixels to draw. The pixels are
 * located in GPU memory as a GPU texture.
 * <ul>
 * <li>{@code Image} cannot be modified by CPU after it is created.</li>
 * <li>{@code Image} width and height are greater than zero.</li>
 * <li>{@code Image} may be created from {@link Bitmap}, Surface, Picture
 * and GPU texture.</li>
 * </ul>
 */
//TODO wip, only create from Bitmap, only drawImage works now
public class Image implements AutoCloseable {

    private final ImageInfo mInfo;

    private final RecordingContext mContext;
    private ViewReference mView;

    private Image(ImageInfo info, RecordingContext context, @SharedPtr Texture texture, short swizzle) {
        mInfo = info;
        mContext = context;
        mView = new ViewReference(this, texture, swizzle);
    }

    // must be called after render system and UI system are initialized successfully,
    // must be called from either render thread or UI thread
    @Nullable
    public static Image createTextureFromBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isClosed()) {
            return null;
        }
        return createTextureFromBitmap(
                Core.isOnUiThread()
                        ? Core.requireUiRecordingContext()
                        : Core.requireDirectContext(),
                bitmap
        );
    }

    /**
     * Create an image that backed by a GPU texture with the given bitmap.
     * The bitmap should be immutable and can be safely closed after the call.
     *
     * @param rContext the recording graphics context on the current thread
     * @param bitmap   the source bitmap
     * @return image, or null if failed
     */
    @Nullable
    public static Image createTextureFromBitmap(@NonNull RecordingContext rContext,
                                                @NonNull Bitmap bitmap) {
        var caps = rContext.getCaps();
        var ct = (bitmap.getFormat() == Bitmap.Format.RGB_888)
                ? ImageInfo.CT_RGB_888x
                : bitmap.getColorType();
        if (caps.getDefaultBackendFormat(ct, /*renderable*/false) == null) {
            return null;
        }
        var flags = Surface.FLAG_BUDGETED;
        if (bitmap.getWidth() > 1 || bitmap.getHeight() > 1) {
            flags |= Surface.FLAG_MIPMAPPED;
        }
        @SharedPtr
        var texture = rContext.getSurfaceProvider().createTextureFromPixmap(
                bitmap.getPixels(), ct, flags
        );
        if (texture == null) {
            return null;
        }
        var swizzle = caps.getReadSwizzle(texture.getBackendFormat(), ct);
        return new Image(bitmap.getInfo(),
                rContext,
                texture,
                swizzle);
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
    public ImageInfo getInfo() {
        return mInfo;
    }

    /**
     * Returns the view width of this image (as its texture).
     *
     * @return image width in pixels
     */
    public int getWidth() {
        return mInfo.width();
    }

    /**
     * Returns the view height of this image (as its texture).
     *
     * @return image height in pixels
     */
    public int getHeight() {
        return mInfo.height();
    }

    // do not close this!
    @ApiStatus.Internal
    public SurfaceView asTextureView() {
        return mView;
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
        if (mView != null) {
            mView.mCleanup.clean();
            mView = null;
        }
    }

    public boolean isClosed() {
        return mView == null;
    }

    private static final class ViewReference extends SurfaceView implements Runnable {

        private final Cleaner.Cleanable mCleanup;

        private ViewReference(Image owner, @SharedPtr Texture texture, short swizzle) {
            super(texture,
                    Engine.SurfaceOrigin.kUpperLeft,
                    swizzle);
            mCleanup = Core.registerCleanup(owner, this);
        }

        @Override
        public void run() {
            // texture can be created from any thread, but must be closed on render thread
            Core.executeOnRenderThread(super::close);
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException();
        }
    }
}
