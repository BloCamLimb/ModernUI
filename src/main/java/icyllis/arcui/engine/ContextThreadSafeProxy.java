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

package icyllis.arcui.engine;

import icyllis.arcui.core.Image.CompressionType;
import icyllis.arcui.core.*;
import icyllis.arcui.core.Surface;
import icyllis.arcui.core.ImageInfo.ColorType;
import icyllis.arcui.text.TextBlobCache;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Can be used to perform actions related to the generating Context in a thread safe manner. The
 * proxy does not access the 3D API (e.g. OpenGL) that backs the generating Context.
 */
public final class ContextThreadSafeProxy {

    private static final AtomicInteger sNextId = new AtomicInteger(1);

    final int mBackend;
    final ContextOptions mOptions;
    final int mContextID;

    volatile Caps mCaps;
    volatile TextBlobCache mTextBlobCache;
    volatile ThreadSafeCache mThreadSafeCache;
    volatile ThreadSafePipelineBuilder mPipelineBuilder;

    private final AtomicBoolean mDropped = new AtomicBoolean(false);

    ContextThreadSafeProxy(int backend, ContextOptions options) {
        mBackend = backend;
        mOptions = options;
        mContextID = sNextId.getAndIncrement();
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
     *                                 {@link Surface} that the DDL created
     *                                 with this characterization will be replayed into.
     *                                 Note: Engine doesn't make use of the
     *                                 {@link ImageInfo#alphaType()}.
     * @param backendFormat            Information about the format of the GPU surface that
     *                                 will back the {@link Surface} upon
     *                                 replay.
     * @param origin                   The origin of the {@link Surface} that
     *                                 the DDL created with this characterization will be
     *                                 replayed into.
     * @param sampleCount              The sample count of the {@link Surface}
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

        if (backendFormat.getBackend() != Types.OPENGL && glWrapDefaultFramebuffer) {
            // The flag can only be used for a OpenGL backend.
            return null;
        }

        if (backendFormat.getBackend() != Types.VULKAN &&
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
     * Retrieve the default {@link BackendFormat} for a given ColorType and renderability.
     * It is guaranteed that this backend format will be the one used by the following
     * ColorType and {@link SurfaceCharacterization#createBackendFormat(int, BackendFormat)}.
     * <p>
     * The caller should check that the returned format is valid (nullability).
     *
     * @param renderable true if the format will be used as color attachments
     */
    @Nullable
    public BackendFormat getDefaultBackendFormat(@ColorType int colorType, boolean renderable) {
        assert mCaps != null;

        BackendFormat format = mCaps.getDefaultBackendFormat(colorType, renderable);
        if (format == null) {
            return null;
        }

        assert !renderable || mCaps.isFormatRenderable(colorType, format, 1);

        return format;
    }

    /**
     * Retrieve the {@link BackendFormat} for a given CompressionType. This is
     * guaranteed to match the backend format used by the following
     * createCompressedBackendTexture methods that take a CompressionType.
     * <p>
     * The caller should check that the returned format is valid (nullability).
     */
    @Nullable
    public BackendFormat getCompressedBackendFormat(@CompressionType int compressionType) {
        assert mCaps != null;

        BackendFormat format = mCaps.getCompressedBackendFormat(compressionType);

        assert format == null ||
                (format.getTextureType() == Types.TEXTURE_TYPE_2D && mCaps.isFormatTexturable(format));
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

    /**
     * @return initialized or not, if {@link DirectContext} is created, it must be true
     */
    public boolean isValid() {
        return mCaps != null;
    }

    @ApiStatus.Internal
    public boolean matches(Context c) {
        return this == c.mThreadSafeProxy;
    }

    @ApiStatus.Internal
    public Caps getCaps() {
        return mCaps;
    }

    void init(Caps caps, ThreadSafePipelineBuilder pipelineBuilder) {
        assert caps != null && pipelineBuilder != null;
        mCaps = caps;
        mTextBlobCache = new TextBlobCache(mContextID);
        mThreadSafeCache = new ThreadSafeCache();
        mPipelineBuilder = pipelineBuilder;
    }

    void drop() {
        if (!mDropped.compareAndExchange(false, true)) {
            mTextBlobCache.freeAll();
        }
    }

    boolean isDropped() {
        return mDropped.get();
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return mContextID;
    }
}
