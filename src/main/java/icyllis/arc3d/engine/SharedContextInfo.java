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

import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.core.SurfaceCharacterization;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static icyllis.arc3d.engine.Engine.*;

/**
 * Can be used to perform actions related to the generating {@link Context} in a thread safe manner. The
 * proxy does not access the 3D API (e.g. OpenGL) that backs the generating {@link Context}.
 * <p>
 * This class is a public API, except where noted.
 */
public final class SharedContextInfo {

    private static final AtomicInteger sNextID = new AtomicInteger(1);

    private static int createUniqueID() {
        for (;;) {
            final int value = sNextID.get();
            final int newValue = value == -1 ? 1 : value + 1; // 0 is reserved
            if (sNextID.weakCompareAndSetVolatile(value, newValue)) {
                return value;
            }
        }
    }

    private final int mBackend;
    private final ContextOptions mOptions;
    private final int mContextID;

    private volatile Caps mCaps;
    private volatile ThreadSafeCache mThreadSafeCache;
    private volatile PipelineStateCache mPipelineStateCache;

    private final AtomicBoolean mDiscarded = new AtomicBoolean(false);

    SharedContextInfo(int backend, ContextOptions options) {
        mBackend = backend;
        mOptions = options;
        mContextID = createUniqueID();
    }

    /**
     * Create a surface characterization for a DDL that will be replayed into the {@link Context}
     * that created this proxy. On failure the resulting characterization will be null.
     *
     * @param cacheMaxResourceBytes    The max resource bytes limit that will be in effect when
     *                                 the DDL created with this characterization is replayed.
     *                                 Note: the contract here is that the DDL will be created as
     *                                 if it had a full 'cacheMaxResourceBytes' to use. If
     *                                 replayed into a {@link Context} that already has locked
     *                                 GPU memory, the replay can exceed the budget. To rephrase,
     *                                 all resource allocation decisions are made at record time
     *                                 and at playback time the budget limits will be ignored.
     * @param imageInfo                The image info specifying properties of the
     *                                 {@link IGPUSurface} that the DDL created
     *                                 with this characterization will be replayed into.
     *                                 Note: Engine doesn't make use of the
     *                                 {@link ImageInfo#alphaType()}.
     * @param backendFormat            Information about the format of the GPU surface that
     *                                 will back the {@link IGPUSurface} upon
     *                                 replay.
     * @param origin                   The origin of the {@link IGPUSurface} that
     *                                 the DDL created with this characterization will be
     *                                 replayed into.
     * @param sampleCount              The sample count of the {@link IGPUSurface}
     *                                 that the DDL created with this characterization will be
     *                                 replayed into.
     * @param texturable               Will the surface be able to act as a texture?
     * @param mipmapped                Will the surface the DDL will be replayed into have space
     *                                 allocated for mipmaps?
     * @param glWrapDefaultFramebuffer Will the surface the DDL will be replayed into be backed
     *                                 by GL FBO 0. This flag is only valid if using an GL backend.
     * @param vkSupportInputAttachment Can the vulkan surface be used as in input attachment?
     * @param vkSecondaryCommandBuffer Will the surface be wrapping a vulkan secondary command
     *                                 buffer via a VkSecondaryCBDrawContext? If
     *                                 this is true then the following is required:
     *                                 texturable = false
     *                                 mipmapped = false
     *                                 glWrapDefaultFramebuffer = false
     *                                 vkSupportInputAttachment = false
     * @param isProtected              Will the (Vulkan) surface be DRM protected?
     */
    @Nullable
    public SurfaceCharacterization createCharacterization(
            long cacheMaxResourceBytes,
            ImageInfo imageInfo,
            BackendFormat backendFormat,
            int origin,
            int sampleCount,
            boolean texturable,
            boolean mipmapped,
            boolean glWrapDefaultFramebuffer,
            boolean vkSupportInputAttachment,
            boolean vkSecondaryCommandBuffer,
            boolean isProtected) {
        assert mCaps != null;

        if (!texturable && mipmapped) {
            return null;
        }

        if (mBackend != backendFormat.getBackend()) {
            return null;
        }

        if (backendFormat.getBackend() != BackendApi.kOpenGL && glWrapDefaultFramebuffer) {
            // The flag can only be used for a OpenGL backend.
            return null;
        }

        if (backendFormat.getBackend() != BackendApi.kVulkan &&
                (vkSupportInputAttachment || vkSecondaryCommandBuffer)) {
            // The flags can only be used for a Vulkan backend.
            return null;
        }

        if (imageInfo.width() < 1 || imageInfo.width() > mCaps.maxRenderTargetSize() ||
                imageInfo.height() < 1 || imageInfo.height() > mCaps.maxRenderTargetSize()) {
            return null;
        }

        int colorType = imageInfo.colorType();

        if (!mCaps.isFormatCompatible(colorType, backendFormat)) {
            return null;
        }

        if (!mCaps.isFormatRenderable(colorType, backendFormat, sampleCount)) {
            return null;
        }

        sampleCount = mCaps.getRenderTargetSampleCount(sampleCount, backendFormat);
        assert sampleCount > 0;

        if (glWrapDefaultFramebuffer && texturable) {
            return null;
        }

        if (texturable && !mCaps.isFormatTexturable(backendFormat)) {
            // Engine doesn't agree that this is texturable.
            return null;
        }

        if (vkSecondaryCommandBuffer &&
                (texturable || glWrapDefaultFramebuffer || vkSupportInputAttachment)) {
            return null;
        }

        return new SurfaceCharacterization(this,
                cacheMaxResourceBytes, imageInfo, backendFormat,
                origin, sampleCount, texturable, mipmapped,
                glWrapDefaultFramebuffer, vkSupportInputAttachment,
                vkSecondaryCommandBuffer, isProtected);
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
    public BackendFormat getDefaultBackendFormat(int colorType, boolean renderable) {
        assert (mCaps != null);

        colorType = colorTypeToPublic(colorType);
        BackendFormat format = mCaps.getDefaultBackendFormat(colorType, renderable);
        if (format == null) {
            return null;
        }
        assert (!renderable ||
                mCaps.isFormatRenderable(colorType, format, 1));
        return format;
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
    public BackendFormat getCompressedBackendFormat(int compressionType) {
        assert (mCaps != null);

        BackendFormat format = mCaps.getCompressedBackendFormat(compressionType);
        assert (format == null) ||
                (!format.isExternal() && mCaps.isFormatTexturable(format));
        return format;
    }

    /**
     * Gets the maximum supported sample count for a color type. 1 is returned if only non-MSAA
     * rendering is supported for the color type. 0 is returned if rendering to this color type
     * is not supported at all.
     *
     * @param colorType see {@link ImageInfo}
     */
    public int getMaxSurfaceSampleCount(int colorType) {
        assert (mCaps != null);

        colorType = colorTypeToPublic(colorType);
        BackendFormat format = mCaps.getDefaultBackendFormat(colorType, true);
        if (format == null) {
            return 0;
        }
        return mCaps.getMaxRenderTargetSampleCount(format);
    }

    /**
     * @return initialized or not, if {@link DirectContext} is created, it must be true
     */
    public boolean isValid() {
        return mCaps != null;
    }

    @ApiStatus.Internal
    public boolean matches(Context c) {
        return c != null && this == c.mContextInfo;
    }

    @ApiStatus.Internal
    public int getBackend() {
        return mBackend;
    }

    @ApiStatus.Internal
    public ContextOptions getOptions() {
        return mOptions;
    }

    @ApiStatus.Internal
    public int getContextID() {
        return mContextID;
    }

    @ApiStatus.Internal
    public Caps getCaps() {
        return mCaps;
    }

    @ApiStatus.Internal
    public ThreadSafeCache getThreadSafeCache() {
        return mThreadSafeCache;
    }

    @ApiStatus.Internal
    public PipelineStateCache getPipelineStateCache() {
        return mPipelineStateCache;
    }

    void init(Caps caps, PipelineStateCache psc) {
        assert (caps != null);
        mCaps = caps;
        mThreadSafeCache = new ThreadSafeCache();
        mPipelineStateCache = psc;
    }

    boolean discard() {
        return !mDiscarded.compareAndExchange(false, true);
    }

    boolean isDiscarded() {
        return mDiscarded.get();
    }

    @Override
    public int hashCode() {
        return mContextID;
    }

    // use reference equality
}
