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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.core.Surface;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

/**
 * This class is a public API, except where noted.
 */
public sealed class RecordingContext extends Context
        permits ImmediateContext {

    private final Thread mOwnerThread;

    private final SurfaceProvider mSurfaceProvider;
    private RenderTaskManager mRenderTaskManager;

    private final PipelineKey mLookupDesc = new PipelineKey();

    protected RecordingContext(SharedContext context) {
        super(context);
        mOwnerThread = Thread.currentThread();
        mSurfaceProvider = new SurfaceProvider(this);
    }

    @Nullable
    public static RecordingContext makeRecording(SharedContext context) {
        RecordingContext rContext = new RecordingContext(context);
        if (rContext.init()) {
            return rContext;
        }
        rContext.unref();
        return null;
    }

    /**
     * Reports whether the {@link ImmediateContext} associated with this {@link RecordingContext}
     * is discarded. When called on a {@link ImmediateContext} it may actively check whether the
     * underlying 3D API device/context has been disconnected before reporting the status. If so,
     * calling this method will transition the {@link ImmediateContext} to the discarded state.
     */
    public boolean isDiscarded() {
        return mContextInfo.isDiscarded();
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
    public final SurfaceProvider getSurfaceProvider() {
        return mSurfaceProvider;
    }

    @ApiStatus.Internal
    public final RenderTaskManager getRenderTaskManager() {
        return mRenderTaskManager;
    }

    @ApiStatus.Internal
    public final ThreadSafeCache getThreadSafeCache() {
        return mContextInfo.getThreadSafeCache();
    }

    @ApiStatus.Internal
    public final PipelineCache getPipelineCache() {
        return mContextInfo.getPipelineCache();
    }

    @ApiStatus.Internal
    public final GraphicsPipeline findOrCreateGraphicsPipeline(
            final GraphicsPipelineDesc graphicsPipelineDesc) {
        mLookupDesc.clear();
        return getPipelineCache().findOrCreateGraphicsPipeline(
                mLookupDesc,
                graphicsPipelineDesc
        );
    }

    @Override
    protected boolean init() {
        if (!super.init()) {
            return false;
        }
        if (mRenderTaskManager != null) {
            mRenderTaskManager.destroy();
        }
        mRenderTaskManager = new RenderTaskManager(this);
        return true;
    }

    protected void discard() {
        if (mContextInfo.discard() && mRenderTaskManager != null) {
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

    /**
     * @return the context-creating thread
     */
    public final Thread getOwnerThread() {
        return mOwnerThread;
    }

    /**
     * @return true if calling from the context-creating thread
     */
    public final boolean isOwnerThread() {
        return Thread.currentThread() == mOwnerThread;
    }

    /**
     * Checks if calling from the context-creating thread, or throws a runtime exception.
     */
    public final void checkOwnerThread() {
        if (Thread.currentThread() != mOwnerThread)
            throw new IllegalStateException("Method expected to call from " + mOwnerThread +
                    ", current " + Thread.currentThread() + ", deferred " + !(this instanceof ImmediateContext));
    }
}
