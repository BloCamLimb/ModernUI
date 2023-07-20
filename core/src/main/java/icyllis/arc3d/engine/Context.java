/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.SurfaceCharacterization;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.*;
import org.jetbrains.annotations.ApiStatus;

import java.io.PrintWriter;
import java.util.Objects;

/**
 * This class is a public API, except where noted.
 */
public abstract class Context extends RefCnt {

    protected final ContextThreadSafeProxy mThreadSafeProxy;

    protected Context(ContextThreadSafeProxy threadSafeProxy) {
        mThreadSafeProxy = threadSafeProxy;
    }

    /**
     * The 3D API backing this context.
     *
     * @return see {@link Engine.BackendApi}
     */
    public final int getBackend() {
        return mThreadSafeProxy.getBackend();
    }

    /**
     * Retrieve the default {@link BackendFormat} for a given {@code ColorType} and renderability.
     * It is guaranteed that this backend format will be the one used by the following
     * {@code ColorType} and {@link SurfaceCharacterization#createBackendFormat(int, BackendFormat)}.
     * <p>
     * The caller should check that the returned format is valid (nullability).
     *
     * @param colorType  see {@link ImageInfo}
     * @param renderable true if the format will be used as color attachments
     */
    @Nullable
    public final BackendFormat getDefaultBackendFormat(int colorType, boolean renderable) {
        return mThreadSafeProxy.getDefaultBackendFormat(colorType, renderable);
    }

    /**
     * Retrieve the {@link BackendFormat} for a given {@code CompressionType}. This is
     * guaranteed to match the backend format used by the following
     * createCompressedBackendTexture methods that take a {@code CompressionType}.
     * <p>
     * The caller should check that the returned format is valid (nullability).
     *
     * @param compressionType see {@link ImageInfo}
     */
    @Nullable
    public final BackendFormat getCompressedBackendFormat(int compressionType) {
        return mThreadSafeProxy.getCompressedBackendFormat(compressionType);
    }

    /**
     * Gets the maximum supported sample count for a color type. 1 is returned if only non-MSAA
     * rendering is supported for the color type. 0 is returned if rendering to this color type
     * is not supported at all.
     *
     * @param colorType see {@link ImageInfo}
     */
    public final int getMaxSurfaceSampleCount(int colorType) {
        return mThreadSafeProxy.getMaxSurfaceSampleCount(colorType);
    }

    public final ContextThreadSafeProxy getThreadSafeProxy() {
        return mThreadSafeProxy;
    }

    @ApiStatus.Internal
    public final boolean matches(Context c) {
        return mThreadSafeProxy.matches(c);
    }

    @ApiStatus.Internal
    public final ContextOptions getOptions() {
        return mThreadSafeProxy.getOptions();
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
        return mThreadSafeProxy.getContextID();
    }

    @ApiStatus.Internal
    public final Caps getCaps() {
        return mThreadSafeProxy.getCaps();
    }

    @ApiStatus.Internal
    public final PrintWriter getErrorWriter() {
        return Objects.requireNonNullElseGet(getOptions().mErrorWriter, Context::getDefaultErrorWriter);
    }

    protected boolean init() {
        return mThreadSafeProxy.isValid();
    }

    private static PrintWriter sDefaultErrorWriter;

    private static PrintWriter getDefaultErrorWriter() {
        PrintWriter err = sDefaultErrorWriter;
        if (err == null) {
            sDefaultErrorWriter = err = new PrintWriter(System.err, true);
        }
        return err;
    }
}
