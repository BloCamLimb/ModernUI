/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2025 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.sketch;

import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.core.Rect2i;
import icyllis.arc3d.core.Rect2ic;
import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.BackendFormat;
import icyllis.arc3d.engine.BackendImage;
import icyllis.arc3d.engine.Caps;
import icyllis.arc3d.engine.Context;
import icyllis.arc3d.granite.RecordingContext;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Surface is responsible for managing the pixels that a canvas draws into.
 * The pixels will be allocated on the GPU (a RenderTarget surface).
 * Surface takes care of allocating a {@link Canvas} that will draw into the surface.
 * Call {@link #getCanvas()} to use that canvas (it is owned by the surface).
 * Surface always has non-zero dimensions. If there is a request for a new surface,
 * and either of the requested dimensions are zero, then null will be returned.
 */
public abstract class Surface extends RefCnt {

    private final int mWidth;
    private final int mHeight;
    private Object mGenerationID;

    // unique ptr
    private Canvas mCachedCanvas;
    @SharedPtr
    private Image mCachedImage;

    protected Surface(int width, int height) {
        assert width > 0 && height > 0;
        mWidth = width;
        mHeight = height;
    }

    /**
     * Wraps a GPU-backed texture into Surface. Caller must ensure the texture is
     * valid for the lifetime of returned Surface. If <code>sampleCount</code> greater
     * than one, creates an intermediate MSAA Surface which is used for drawing
     * <code>backendTexture</code>.
     * <p>
     * Surface is returned if all parameters are valid. <code>backendTexture</code>
     * is valid if its pixel configuration agrees with <code>context</code>; for instance,
     * if <code>backendTexture</code> has an sRGB configuration, then <code>context</code>
     * must support sRGB. Further, <code>backendTexture</code> width and height must
     * not exceed <code>context</code> capabilities, and the <code>context</code> must
     * be able to support back-end textures.
     * <p>
     * Upon success <code>releaseCallback</code> is called when it is safe to delete the
     * texture in the backend API (accounting only for use of the texture by this surface).
     * If Surface creation fails <code>releaseCallback</code> is called before this method
     * returns.
     *
     * @param context         GPU context
     * @param backendImage    texture residing on GPU
     * @param sampleCount     samples per pixel, or 1 to disable full scene anti-aliasing
     * @param releaseCallback function called when texture can be released, may be null
     * @return Surface if all parameters are valid; otherwise, null
     */
    @Nullable
    public static Surface makeFromBackendTexture(RecordingContext context,
                                                 BackendImage backendImage,
                                                 int origin, int sampleCount,
                                                 int colorType,
                                                 Runnable releaseCallback) {
        if (context == null || sampleCount < 1 || colorType == ColorInfo.CT_UNKNOWN) {
            if (releaseCallback != null) {
                releaseCallback.run();
            }
            return null;
        }

        if (!validateBackendTexture(context.getCaps(), backendImage, sampleCount, colorType, true)) {
            if (releaseCallback != null) {
                releaseCallback.run();
            }
            return null;
        }

        return null;
    }

    /**
     * Returns Surface on GPU indicated by context. Allocates memory for pixels,
     * based on the width, height, and ColorType in ColorInfo. <code>budgeted</code>
     * selects whether allocation for pixels is tracked by context. <code>imageInfo</code>
     * describes the pixel format in ColorType, and transparency in AlphaType.
     * <p>
     * <code>sampleCount</code> requests the number of samples per pixel.
     * Pass one to disable multi-sample anti-aliasing.  The request is rounded
     * up to the next supported count, or rounded down if it is larger than the
     * maximum supported count.
     * <p>
     * <code>origin</code> pins either the top-left or the bottom-left corner to the origin.
     * <p>
     * <code>mipmapped</code> hints that Image returned by makeImageSnapshot() has mipmaps.
     *
     * @param context     GPU context
     * @param imageInfo   width, height, ColorType, AlphaType; width, or height, or both, may be zero
     * @param sampleCount samples per pixel, or 1 to disable full scene anti-aliasing
     * @param mipmapped   hint that Surface will host mipmap images
     * @return Surface if all parameters are valid; otherwise, null
     */
    @Nullable
    public static Surface makeRenderTarget(RecordingContext context,
                                           ImageInfo imageInfo,
                                           int origin,
                                           int sampleCount,
                                           boolean mipmapped,
                                           boolean budgeted) {
        if (context == null || imageInfo == null || sampleCount < 1) {
            return null;
        }

        return null;
    }

    /*@Nullable
    public static Surface wrapBackendRenderTarget(RecordingContext rContext,
                                                  BackendRenderTarget backendRenderTarget,
                                                  int origin,
                                                  int colorType,
                                                  ColorSpace colorSpace) {
        if (colorType == ColorInfo.CT_UNKNOWN) {
            return null;
        }
        var provider = rContext.getSurfaceProvider();
        var rtProxy = provider.wrapBackendRenderTarget(backendRenderTarget, null);
        if (rtProxy == null) {
            return null;
        }
        var dev = SurfaceDevice.make(rContext,
                colorType,
                colorSpace,
                rtProxy,
                origin,
                false);
        if (dev == null) {
            return null;
        }
        return new Surface(dev);
    }*/

    private static boolean validateBackendTexture(Caps caps,
                                                  BackendImage backendImage,
                                                  int sampleCount,
                                                  int colorType,
                                                  boolean texturable) {
        if (backendImage == null) {
            return false;
        }

        BackendFormat backendFormat = backendImage.getBackendFormat();

        if (!caps.isFormatCompatible(colorType, backendFormat)) {
            return false;
        }

        if (caps.isFormatRenderable(colorType, backendFormat, sampleCount)) {
            return false;
        }

        return !texturable || caps.isFormatTexturable(backendFormat);
    }

    @Override
    protected void deallocate() {
        if (mCachedCanvas != null) {
            mCachedCanvas.mSurface = null;
            mCachedCanvas.close();
            mCachedCanvas = null;
        }
        mCachedImage = RefCnt.move(mCachedImage);
    }

    /**
     * Returns pixel count in each row; may be zero or greater.
     *
     * @return number of pixel columns
     */
    public final int getWidth() {
        return mWidth;
    }

    /**
     * Returns pixel row count; may be zero or greater.
     *
     * @return number of pixel rows
     */
    public final int getHeight() {
        return mHeight;
    }

    /**
     * Returns an ImageInfo describing the Surface.
     */
    @NonNull
    public abstract ImageInfo getImageInfo();

    /**
     * Returns unique value identifying the content of Surface. Returned value changes
     * each time the content changes. Content is changed by drawing, or by calling
     * {@link #notifyWillChange()}.
     *
     * @return unique content identifier
     */
    public final Object getGenerationID() {
        if (mGenerationID == null) {
            assert mCachedCanvas == null || mCachedCanvas.mSurface == this;
            mGenerationID = new Object();
        }
        return mGenerationID;
    }

    protected static final int
            kPreserve_ContentChangeMode = 0,    // preserves surface on change
            kDiscard_ContentChangeMode = 1;     // discards surface on change

    /**
     * Notifies that Surface contents will be changed externally.
     * Subsequent calls to {@link #getGenerationID()} return a different value.
     */
    public final void notifyWillChange() {
        aboutToDraw(kDiscard_ContentChangeMode);
    }

    /**
     * Returns the GPU context being used by the Surface.
     *
     * @return the GPU context, if available; null otherwise
     */
    @RawPtr
    public final Context getCommandContext() {
        return onGetCommandContext();
    }

    /**
     * Returns Canvas that draws into Surface. Subsequent calls return the same Canvas.
     * Canvas returned is managed and owned by Surface, and is deleted when Surface
     * is deleted.
     *
     * @return drawing Canvas for Surface
     */
    @RawPtr
    public final Canvas getCanvas() {
        return getCachedCanvas();
    }

    /**
     * Returns Image capturing Surface contents. Subsequent drawing to Surface contents
     * are not captured. Image allocation is accounted for if Surface was created with
     * Budgeted flag.
     *
     * @return Image initialized with Surface contents
     */
    @Nullable
    @SharedPtr
    public final Image makeImageSnapshot() {
        return RefCnt.create(getCachedImage());
    }

    /**
     * Like the no-parameter version, this returns an image of the current surface contents.
     * This variant takes a rectangle specifying the subset of the surface that is of interest.
     * These bounds will be sanitized before being used.
     * <p>
     * - If bounds extends beyond the surface, it will be trimmed to just the intersection of
     * it and the surface.<br>
     * - If bounds does not intersect the surface, then this returns nullptr.<br>
     * - If bounds == the surface, then this is the same as calling the no-parameter variant.
     */
    @Nullable
    @SharedPtr
    public final Image makeImageSnapshot(@NonNull Rect2ic subset) {
        var bounds = new Rect2i(subset);
        if (!bounds.intersect(0, 0, mWidth, mHeight)) {
            return null;
        }
        assert !bounds.isEmpty();
        if (bounds.mLeft == 0 && bounds.mTop == 0 && bounds.mRight == mWidth && bounds.mBottom == mHeight) {
            return makeImageSnapshot();
        } else {
            return onNewImageSnapshot(bounds);
        }
    }

    @ApiStatus.Internal
    @RawPtr
    public final Canvas getCachedCanvas() {
        if (mCachedCanvas == null) {
            mCachedCanvas = onNewCanvas();
            if (mCachedCanvas != null) {
                mCachedCanvas.mSurface = this;
            }
        }
        return mCachedCanvas;
    }

    @ApiStatus.Internal
    @RawPtr
    public final Image getCachedImage() {
        if (mCachedImage != null) {
            return mCachedImage;
        }

        mCachedImage = onNewImageSnapshot(null);

        assert mCachedCanvas == null || mCachedCanvas.mSurface == this;
        return mCachedImage;
    }

    @ApiStatus.Internal
    public final boolean hasCachedImage() {
        return mCachedImage != null;
    }

    @ApiStatus.Internal
    @RawPtr
    protected Context onGetCommandContext() {
        return null;
    }

    /**
     * Allocate a canvas that will draw into this surface. We will cache this
     * canvas, to return the same object to the caller multiple times. We
     * take ownership, and will call unref() on the canvas when we go out of
     * scope.
     */
    @ApiStatus.Internal
    @RawPtr
    protected abstract Canvas onNewCanvas();

    /**
     * Allocate an Image that represents the current contents of the surface.
     * This needs to be able to outlive the surface itself (if need be), and
     * must faithfully represent the current contents, even if the surface
     * is changed after this called (e.g. it is drawn to via its canvas).
     * <p>
     * If a subset is specified, the impl must make a copy, rather than try to wait
     * on copy-on-write.
     */
    @ApiStatus.Internal
    @Nullable
    @SharedPtr
    protected abstract Image onNewImageSnapshot(@Nullable Rect2ic subset);

    /**
     * Called as a performance hint when the Surface is allowed to make its contents
     * undefined.
     */
    @ApiStatus.Internal
    protected void onDiscard() {
    }

    /**
     * If the surface is about to change, we call this so that our subclass
     * can optionally fork their backend (copy-on-write) in case it was
     * being shared with the cachedImage.
     * <p>
     * Returns false if the backing cannot be un-shared.
     */
    @ApiStatus.Internal
    protected abstract boolean onCopyOnWrite(int changeMode);

    /**
     * Signal the surface to remind its backing store that it's mutable again.
     * Called only when we _didn't_ copy-on-write; we assume the copies start mutable.
     */
    @ApiStatus.Internal
    protected void onRestoreBackingMutability() {
    }

    // Returns true if there is an outstanding image-snapshot, indicating that a call to aboutToDraw
    // would trigger a copy-on-write.
    final boolean hasOutstandingImageSnapshot() {
        return mCachedImage != null && !mCachedImage.unique();
    }

    // Returns false if drawing should not take place (allocation failure).
    final boolean aboutToDraw(int changeMode) {
        mGenerationID = null;

        assert mCachedCanvas == null || mCachedCanvas.mSurface == this;

        if (mCachedImage != null) {
            // the surface may need to fork its backend, if it's sharing it with
            // the cached image. Note: we only call if there is an outstanding owner
            // on the image (besides us).
            boolean unique = mCachedImage.unique();
            if (!unique) {
                if (!onCopyOnWrite(changeMode)) {
                    return false;
                }
            }

            // regardless of copy-on-write, we must drop our cached image now, so
            // that the next request will get our new contents.
            mCachedImage = RefCnt.move(mCachedImage);

            if (unique) {
                // Our content isn't held by any image now, so we can consider that content mutable.
                // Raster surfaces need to be told it's safe to consider its pixels mutable again.
                // We make this call after the ->unref() so the subclass can assert there are no images.
                onRestoreBackingMutability();
            }
        } else if (changeMode == kDiscard_ContentChangeMode) {
            onDiscard();
        }
        return true;
    }
}
