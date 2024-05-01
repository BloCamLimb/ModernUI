/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.graphene.Device_Gpu;

import javax.annotation.Nullable;

/**
 * Surface is responsible for managing the pixels that a canvas draws into.
 * The pixels will be allocated on the GPU (a RenderTarget surface).
 * Surface takes care of allocating a {@link Canvas} that will draw into the surface.
 * Call {@link #getCanvas()} to use that canvas (it is owned by the surface).
 * Surface always has non-zero dimensions. If there is a request for a new surface,
 * and either of the requested dimensions are zero, then null will be returned.
 */
public class Surface extends RefCnt {

    @SharedPtr
    private Device mDevice;

    // unique ptr
    private Canvas mCachedCanvas;

    public Surface(@SharedPtr Device device) {
        mDevice = device;
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
     * @param backendImage  texture residing on GPU
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

    @Nullable
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
        var dev = Device_Gpu.make(rContext,
                colorType,
                colorSpace,
                rtProxy,
                origin,
                false);
        if (dev == null) {
            return null;
        }
        return new Surface(dev);
    }

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
        mDevice.unref();
        mDevice = null;
        if (mCachedCanvas != null) {
            mCachedCanvas.mSurface = null;
            mCachedCanvas.close();
            mCachedCanvas = null;
        }
    }

    /**
     * Returns pixel count in each row; may be zero or greater.
     *
     * @return number of pixel columns
     */
    public int getWidth() {
        return mDevice.width();
    }

    /**
     * Returns pixel row count; may be zero or greater.
     *
     * @return number of pixel rows
     */
    public int getHeight() {
        return mDevice.height();
    }

    /**
     * Returns an ImageInfo describing the surface.
     */
    public ImageInfo getImageInfo() {
        return mDevice.imageInfo();
    }

    /**
     * Returns the recording context being used by the SkSurface.
     *
     * @return the recording context, if available; nullptr otherwise
     */
    public RecordingContext getRecordingContext() {
        return mDevice.getRecordingContext();
    }

    /**
     * Returns the canvas that draws into this surface. Subsequent calls return the same canvas.
     * The canvas returned is managed and owned by this surface, and is deleted when this surface
     * is deleted.
     *
     * @return the raw ptr to the drawing canvas for this surface
     */
    @RawPtr
    public Canvas getCanvas() {
        if (mCachedCanvas == null) {
            mCachedCanvas = new Canvas(mDevice);
            mCachedCanvas.mSurface = this;
        }
        return mCachedCanvas;
    }
}
