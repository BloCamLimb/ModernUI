/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.core.Surface;
import org.jetbrains.annotations.ApiStatus;

/**
 * This class is a public API, except where noted.
 */
public final class RecordingContext extends Context {

    private final ImageProxyCache mImageProxyCache;
    private RenderTaskManager mRenderTaskManager;
    private DynamicBufferManager mDynamicBufferManager;

    protected RecordingContext(Device device) {
        super(device);
        mImageProxyCache = new ImageProxyCache(this);
    }

    /**
     * Reports whether the {@link ImmediateContext} associated with this {@link RecordingContext}
     * is discarded. When called on a {@link ImmediateContext} it may actively check whether the
     * underlying 3D API device/context has been disconnected before reporting the status. If so,
     * calling this method will transition the {@link ImmediateContext} to the discarded state.
     */
    public boolean isDiscarded() {
        return mDevice.isDiscarded();
    }

    /**
     * Can a {@link icyllis.arc3d.core.Image} be created with the given color type.
     *
     * @param colorType see {@link ColorInfo}
     */
    public final boolean isImageCompatible(int colorType) {
        return getDefaultBackendFormat(colorType, false) != null;
    }

    /**
     * Can a {@link Surface} be created with the given color type.
     * To check whether MSAA is supported use {@link #getMaxSurfaceSampleCount(int)}.
     *
     * @param colorType see {@link ColorInfo}
     */
    public final boolean isSurfaceCompatible(int colorType) {
        colorType = Engine.colorTypeToPublic(colorType);
        if (ColorInfo.CT_RG_1616 == colorType ||
                ColorInfo.CT_A16_UNORM == colorType ||
                ColorInfo.CT_A16_FLOAT == colorType ||
                ColorInfo.CT_RG_F16 == colorType ||
                ColorInfo.CT_R16G16B16A16_UNORM == colorType ||
                ColorInfo.CT_GRAY_8 == colorType) {
            return false;
        }

        return getMaxSurfaceSampleCount(colorType) > 0;
    }

    @ApiStatus.Internal
    public final ImageProxyCache getSurfaceProvider() {
        return mImageProxyCache;
    }

    @ApiStatus.Internal
    public final RenderTaskManager getRenderTaskManager() {
        return mRenderTaskManager;
    }

    /*@ApiStatus.Internal
    public final ThreadSafeCache getThreadSafeCache() {
        return mDevice.getThreadSafeCache();
    }*/

    @ApiStatus.Internal
    public final DynamicBufferManager getDynamicBufferManager() {
        return mDynamicBufferManager;
    }

    @Override
    public boolean init() {
        if (!super.init()) {
            return false;
        }
        if (mRenderTaskManager != null) {
            mRenderTaskManager.destroy();
        }
        mRenderTaskManager = new RenderTaskManager(this);
        mDynamicBufferManager = new DynamicBufferManager(getCaps(), getResourceProvider());
        return true;
    }

    protected void discard() {
        if (mDevice.discard() && mRenderTaskManager != null) {
            throw new AssertionError();
        }
        if (mRenderTaskManager != null) {
            mRenderTaskManager.destroy();
        }
        mRenderTaskManager = null;
    }

    @Override
    protected void deallocate() {
        if (mRenderTaskManager != null) {
            mRenderTaskManager.destroy();
        }
        mRenderTaskManager = null;
    }
}
