/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.hgi;

import icyllis.arcui.core.ImageInfo;
import icyllis.arcui.core.ImageInfo.ColorType;
import icyllis.arcui.text.TextBlobCache;
import org.jetbrains.annotations.ApiStatus;

/**
 * Context that can add draw ops.
 */
public abstract sealed class RecordingContext extends Context permits DeferredContext, DirectContext {

    private ProxyProvider mProxyProvider;
    private DrawingManager mDrawingManager;

    protected RecordingContext(ContextThreadSafeProxy proxy) {
        super(proxy);
    }

    /**
     * Reports whether the DirectContext associated with this RecordingContext is discarded.
     * When called on a DirectContext it may actively check whether the underlying 3D API
     * device/context has been disconnected before reporting the status. If so, calling this
     * method will transition the DirectContext to the discarded state.
     */
    public boolean isDiscarded() {
        return mThreadSafeProxy.isDiscarded();
    }

    /**
     * Can a {@link icyllis.arcui.core.Image} be created with the given color type.
     */
    public final boolean isImageCapable(@ColorType int colorType) {
        return getDefaultBackendFormat(colorType, false) != null;
    }

    /**
     * Can a {@link icyllis.arcui.core.Surface} be created with the given color type.
     * To check whether MSAA is supported use {@link #getMaxSurfaceSampleCount(int)}.
     */
    public final boolean isSurfaceCapable(@ColorType int colorType) {
        if (ImageInfo.COLOR_R16G16_UNORM == colorType ||
                ImageInfo.COLOR_A16_UNORM == colorType ||
                ImageInfo.COLOR_A16_FLOAT == colorType ||
                ImageInfo.COLOR_R16G16_FLOAT == colorType ||
                ImageInfo.COLOR_R16G16B16A16_UNORM == colorType ||
                ImageInfo.COLOR_GRAY_8 == colorType) {
            return false;
        }

        return getMaxSurfaceSampleCount(colorType) > 0;
    }

    /**
     * Gets the maximum supported texture size.
     */
    public final int getMaxTextureSize() {
        return getCaps().mMaxTextureSize;
    }

    /**
     * Gets the maximum supported render target size.
     */
    public final int getMaxRenderTargetSize() {
        return getCaps().mMaxRenderTargetSize;
    }

    @ApiStatus.Internal
    public final ProxyProvider getProxyProvider() {
        return mProxyProvider;
    }

    @ApiStatus.Internal
    public final DrawingManager getDrawingManager() {
        return mDrawingManager;
    }

    @ApiStatus.Internal
    public final TextBlobCache getTextBlobCache() {
        return mThreadSafeProxy.mTextBlobCache;
    }

    @ApiStatus.Internal
    public final ThreadSafeCache getThreadSafeCache() {
        return mThreadSafeProxy.mThreadSafeCache;
    }

    @Override
    protected boolean init() {
        if (!super.init()) {
            return false;
        }
        //TODO init DrawingManager
        return true;
    }

    protected void discard() {
        mThreadSafeProxy.discard();
        //TODO destroy DrawingManager
    }

    @Override
    public void close() {

    }
}
