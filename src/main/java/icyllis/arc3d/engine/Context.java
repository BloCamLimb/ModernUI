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

import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SurfaceCharacterization;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import javax.annotation.Nullable;

/**
 * This class is a public API, except where noted.
 */
public abstract sealed class Context extends RefCnt
        permits ImmediateContext, RecordingContext {

    protected final Device mDevice;
    protected final Thread mOwnerThread;
    protected ResourceProvider mResourceProvider;

    protected Context(Device device) {
        mDevice = device;
        mOwnerThread = Thread.currentThread();
    }

    /**
     * The 3D API backing this context.
     *
     * @return see {@link Device.BackendApi}
     */
    public final int getBackend() {
        return mDevice.getBackend();
    }

    /**
     * Retrieve the default {@link BackendFormat} for a given {@code ColorType} and renderability.
     * It is guaranteed that this backend format will be the one used by the following
     * {@code ColorType} and {@link SurfaceCharacterization#createBackendFormat(int, BackendFormat)}.
     * <p>
     * The caller should check that the returned format is valid (nullability).
     *
     * @param colorType  see {@link ImageDesc}
     * @param renderable true if the format will be used as color attachments
     */
    @Nullable
    public final BackendFormat getDefaultBackendFormat(int colorType, boolean renderable) {
        return mDevice.getDefaultBackendFormat(colorType, renderable);
    }

    /**
     * Retrieve the {@link BackendFormat} for a given {@code CompressionType}. This is
     * guaranteed to match the backend format used by the following
     * createCompressedBackendTexture methods that take a {@code CompressionType}.
     * <p>
     * The caller should check that the returned format is valid (nullability).
     *
     * @param compressionType see {@link ImageDesc}
     */
    @Nullable
    public final BackendFormat getCompressedBackendFormat(int compressionType) {
        return mDevice.getCompressedBackendFormat(compressionType);
    }

    /**
     * Gets the maximum supported sample count for a color type. 1 is returned if only non-MSAA
     * rendering is supported for the color type. 0 is returned if rendering to this color type
     * is not supported at all.
     *
     * @param colorType see {@link ImageDesc}
     */
    public final int getMaxSurfaceSampleCount(int colorType) {
        return mDevice.getMaxSurfaceSampleCount(colorType);
    }

    public final boolean isImmediate() {
        return this instanceof ImmediateContext;
    }

    @ApiStatus.Internal
    public final Device getDevice() {
        return mDevice;
    }

    @ApiStatus.Internal
    public final boolean matches(Context c) {
        return c != null && mDevice == c.mDevice;
    }

    @ApiStatus.Internal
    public final ContextOptions getOptions() {
        return mDevice.getOptions();
    }

    /**
     * An identifier for this context. The id is used by all compatible contexts. For example,
     * if Images are created on one thread using an image creation context, then fed into a
     * Recorder on second thread (which has a recording context) and finally replayed on
     * a third thread with a direct context, then all three contexts will report the same id.
     * It is an error for an image to be used with contexts that report different ids.
     */
    @ApiStatus.Internal
    public final int getContextID() {
        return mDevice.getContextID();
    }

    //TODO
    public boolean isDeviceLost() {
        if (mDevice != null && mDevice.isDeviceLost()) {
            //discard();
            return true;
        }
        return false;
    }

    @ApiStatus.Internal
    public final Caps getCaps() {
        return mDevice.getCaps();
    }

    @ApiStatus.Internal
    public final ResourceProvider getResourceProvider() {
        return mResourceProvider;
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
    public final SharedResourceCache getSharedResourceCache() {
        return mDevice.getSharedResourceCache();
    }

    public final Logger getLogger() {
        return mDevice.getLogger();
    }

    public boolean init() {
        mResourceProvider = mDevice.makeResourceProvider(this);
        return mDevice.isValid();
    }

    @Override
    protected void deallocate() {
        if (mResourceProvider != null) {
            mResourceProvider.destroy();
            mResourceProvider = null;
        }
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
