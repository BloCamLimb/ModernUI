/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.engine;

import icyllis.akashigi.core.Core;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

/**
 * This class is a public API, except where noted.
 */
public abstract sealed class RecordingContext extends Context
        permits DeferredContext, DirectContext {

    private final Thread mOwnerThread;

    private final ProxyProvider mProxyProvider;
    private DrawingManager mDrawingManager;

    protected RecordingContext(ContextThreadSafeProxy proxy) {
        super(proxy);
        mOwnerThread = Thread.currentThread();
        mProxyProvider = new ProxyProvider(this);
    }

    @Nullable
    public static RecordingContext makeDeferred(ContextThreadSafeProxy proxy) {
        RecordingContext context = new DeferredContext(proxy);
        if (context.init()) {
            return context;
        }
        context.unref();
        return null;
    }

    /**
     * Reports whether the {@link DirectContext} associated with this {@link RecordingContext}
     * is discarded. When called on a {@link DirectContext} it may actively check whether the
     * underlying 3D API device/context has been disconnected before reporting the status. If so,
     * calling this method will transition the {@link DirectContext} to the discarded state.
     */
    public boolean isDiscarded() {
        return mThreadSafeProxy.isDiscarded();
    }

    /**
     * Can a {@link icyllis.akashigi.core.Image} be created with the given color type.
     *
     * @param colorType see {@link Core.ColorType}
     */
    public final boolean isImageCompatible(int colorType) {
        return getDefaultBackendFormat(colorType, false) != null;
    }

    /**
     * Can a {@link icyllis.akashigi.core.Surface} be created with the given color type.
     * To check whether MSAA is supported use {@link #getMaxSurfaceSampleCount(int)}.
     *
     * @param colorType see {@link Core.ColorType}
     */
    public final boolean isSurfaceCompatible(int colorType) {
        colorType = Engine.ColorType.toPublic(colorType);
        if (Core.ColorType.kR16G16_unorm == colorType ||
                Core.ColorType.kA16_unorm == colorType ||
                Core.ColorType.kA16_float == colorType ||
                Core.ColorType.kR16G16_float == colorType ||
                Core.ColorType.kR16G16B16A16_unorm == colorType ||
                Core.ColorType.kGray_8 == colorType) {
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
    public final ThreadSafeCache getThreadSafeCache() {
        return mThreadSafeProxy.getThreadSafeCache();
    }

    @Override
    protected boolean init() {
        if (!super.init()) {
            return false;
        }
        if (mDrawingManager != null) {
            mDrawingManager.destroy();
        }
        mDrawingManager = new DrawingManager(this);
        return true;
    }

    protected void discard() {
        if (mThreadSafeProxy.discard() && mDrawingManager != null) {
            throw new AssertionError();
        }
        if (mDrawingManager != null) {
            mDrawingManager.destroy();
        }
        mDrawingManager = null;
    }

    @Override
    protected void dispose() {
        if (mDrawingManager != null) {
            mDrawingManager.destroy();
        }
        mDrawingManager = null;
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
}
