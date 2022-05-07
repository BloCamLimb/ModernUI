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

import icyllis.arcui.core.Image.CompressionType;
import icyllis.arcui.core.ImageInfo.ColorType;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class Context implements AutoCloseable {

    protected final ContextThreadSafeProxy mThreadSafeProxy;

    protected Context(ContextThreadSafeProxy threadSafeProxy) {
        mThreadSafeProxy = threadSafeProxy;
    }

    /**
     * The 3D API backing this context.
     */
    public final int getBackend() {
        return mThreadSafeProxy.mBackend;
    }

    /**
     * Retrieve the default BackendFormat for a given ColorType and renderability.
     * It is guaranteed that this backend format will be the one used by the Context
     * ColorType and SurfaceCharacterization-based createBackendTexture methods.
     * <p>
     * The caller should check that the returned format is valid.
     */
    @Nullable
    public final BackendFormat getDefaultBackendFormat(@ColorType int colorType, boolean renderable) {
        return mThreadSafeProxy.getDefaultBackendFormat(colorType, renderable);
    }

    @Nullable
    public final BackendFormat getCompressedBackendFormat(@CompressionType int compressionType) {
        return mThreadSafeProxy.getCompressedBackendFormat(compressionType);
    }

    /**
     * Gets the maximum supported sample count for a color type. 1 is returned if only non-MSAA
     * rendering is supported for the color type. 0 is returned if rendering to this color type
     * is not supported at all.
     */
    public final int getMaxSurfaceSampleCount(@ColorType int colorType) {
        return mThreadSafeProxy.getMaxSurfaceSampleCount(colorType);
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
        return mThreadSafeProxy.mContextID;
    }

    @ApiStatus.Internal
    public final boolean matches(Context c) {
        return mThreadSafeProxy.matches(c);
    }

    @ApiStatus.Internal
    public final ContextOptions getOptions() {
        return mThreadSafeProxy.mOptions;
    }

    @ApiStatus.Internal
    public final Caps getCaps() {
        return mThreadSafeProxy.mCaps;
    }

    @ApiStatus.Internal
    public final ShaderErrorHandler getShaderErrorHandler() {
        return Objects.requireNonNullElse(getOptions().mShaderErrorHandler, ShaderErrorHandler.DEFAULT);
    }

    @ApiStatus.Internal
    public final ContextThreadSafeProxy getThreadSafeProxy() {
        return mThreadSafeProxy;
    }

    protected boolean init() {
        assert mThreadSafeProxy.isValid();
        return true;
    }
}
