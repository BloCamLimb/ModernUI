/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.image;

import icyllis.arcticgi.core.ImageInfo;
import icyllis.arcticgi.core.Surface;
import icyllis.arcticgi.engine.*;

import javax.annotation.Nullable;

public final class SurfaceGpu extends Surface {

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
     * @param backendTexture  texture residing on GPU
     * @param sampleCount     samples per pixel, or 1 to disable full scene anti-aliasing
     * @param releaseCallback function called when texture can be released, may be null
     * @return Surface if all parameters are valid; otherwise, null
     */
    @Nullable
    public static Surface makeFromBackendTexture(RecordingContext context,
                                                 BackendTexture backendTexture,
                                                 int origin, int sampleCount,
                                                 int colorType,
                                                 Runnable releaseCallback) {
        if (context == null || sampleCount < 1 || colorType == ImageInfo.COLOR_UNKNOWN) {
            if (releaseCallback != null) {
                releaseCallback.run();
            }
            return null;
        }

        if (!validateBackendTexture(context.caps(), backendTexture, sampleCount, colorType, true)) {
            if (releaseCallback != null) {
                releaseCallback.run();
            }
            return null;
        }

        return null;
    }

    /**
     * Returns Surface on GPU indicated by context. Allocates memory for pixels,
     * based on the width, height, and ColorType in ImageInfo. <code>budgeted</code>
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

    private static boolean validateBackendTexture(Caps caps,
                                                  BackendTexture backendTexture,
                                                  int sampleCount,
                                                  int colorType,
                                                  boolean texturable) {
        if (backendTexture == null) {
            return false;
        }

        BackendFormat backendFormat = backendTexture.getBackendFormat();

        if (!caps.isFormatCompatible(colorType, backendFormat)) {
            return false;
        }

        if (caps.isFormatRenderable(colorType, backendFormat, sampleCount)) {
            return false;
        }

        return !texturable || caps.isFormatTexturable(backendFormat);
    }
}
