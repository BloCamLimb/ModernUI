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
import icyllis.arcui.text.TextBlobCache;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Can be used to perform actions related to the generating Context in a thread safe manner. The
 * proxy does not access the 3D API (e.g. OpenGL) that backs the generating Context.
 */
public final class ThreadSafeProxy {

    private static final AtomicInteger sNextId = new AtomicInteger(1);

    final int mBackend;
    final ContextOptions mOptions;
    final int mContextID;

    volatile Caps mCaps;
    volatile TextBlobCache mTextBlobCache;
    volatile ThreadSafeCache mThreadSafeCache;

    private final AtomicBoolean mDiscarded = new AtomicBoolean(false);

    ThreadSafeProxy(int backend, ContextOptions options) {
        mBackend = backend;
        mOptions = options;
        mContextID = sNextId.getAndIncrement();
    }

    /**
     * Retrieve the default BackendFormat for a given ColorType and renderability.
     * It is guaranteed that this backend format will be the one used by the following
     * ColorType and SurfaceCharacterization-based createBackendTexture methods.
     * <p>
     * The caller should check that the returned format is valid.
     *
     * @param colorType  all possible values in {@link icyllis.arcui.core.ImageInfo}
     * @param renderable true if the format will be used as color attachments
     */
    @Nullable
    public BackendFormat getDefaultBackendFormat(@ColorType int colorType, boolean renderable) {
        assert mCaps != null;

        BackendFormat format = mCaps.getDefaultBackendFormat(colorType, renderable);
        if (format == null) {
            return null;
        }

        assert !renderable || mCaps.isFormatRenderable(format, 1, colorType);

        return format;
    }

    /**
     * Retrieve the BackendFormat for a given CompressionType. This is
     * guaranteed to match the backend format used by the following
     * createCompressedBackendTexture methods that take a CompressionType.
     * <p>
     * The caller should check that the returned format is valid.
     *
     * @param compressionType see {@link icyllis.arcui.core.Image}
     */
    @Nullable
    public BackendFormat getCompressedBackendFormat(@CompressionType int compressionType) {
        assert mCaps != null;

        BackendFormat format = mCaps.getCompressedBackendFormat(compressionType);

        assert format == null || mCaps.isFormatTexturable(format);
        return format;
    }

    /**
     * Gets the maximum supported sample count for a color type. 1 is returned if only non-MSAA
     * rendering is supported for the color type. 0 is returned if rendering to this color type
     * is not supported at all.
     */
    public int getMaxSurfaceSampleCount(@ColorType int colorType) {
        assert mCaps != null;

        BackendFormat format = mCaps.getDefaultBackendFormat(colorType, true);
        return mCaps.getMaxRenderTargetSampleCount(format);
    }

    public boolean isValid() {
        return mCaps != null;
    }

    void init(Caps caps) {
        mCaps = caps;
        mTextBlobCache = new TextBlobCache(mContextID);
        mThreadSafeCache = new ThreadSafeCache();
    }

    boolean matches(Context candidate) {
        return this == candidate.mThreadSafeProxy;
    }

    void discard() {
        if (!mDiscarded.compareAndExchange(false, true)) {
            mTextBlobCache.freeAll();
        }
    }

    boolean isDiscarded() {
        return mDiscarded.get();
    }
}
