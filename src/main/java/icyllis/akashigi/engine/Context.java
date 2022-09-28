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

import icyllis.akashigi.core.ImageInfo.CompressionType;
import icyllis.akashigi.core.*;
import icyllis.akashigi.core.Surface;
import icyllis.akashigi.core.ImageInfo.ColorType;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class Context extends RefCnt {

    protected final ContextThreadSafeProxy mThreadSafeProxy;

    protected Context(ContextThreadSafeProxy threadSafeProxy) {
        mThreadSafeProxy = threadSafeProxy;
    }

    /**
     * The 3D API backing this context.
     *
     * @return see {@link Engine}
     */
    public final int getBackend() {
        return mThreadSafeProxy.mBackend;
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
    public final SurfaceCharacterization createCharacterization(
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
        return mThreadSafeProxy.createCharacterization(cacheMaxResourceBytes, imageInfo,
                backendFormat, origin, sampleCount, texturable, mipmapped,
                glWrapDefaultFramebuffer, vkSupportInputAttachment,
                vkSecondaryCommandBuffer, isProtected);
    }

    /**
     * Retrieve the default {@link BackendFormat} for a given ColorType and renderability.
     * It is guaranteed that this backend format will be the one used by the {@link Context}
     * ColorType and {@link SurfaceCharacterization#createBackendFormat(int, BackendFormat)}.
     * <p>
     * The caller should check that the returned format is valid (nullability).
     *
     * @param renderable true if the format will be used as color attachments
     */
    @Nullable
    public final BackendFormat getDefaultBackendFormat(@ColorType int colorType, boolean renderable) {
        return mThreadSafeProxy.getDefaultBackendFormat(colorType, renderable);
    }

    /**
     * Retrieve the {@link BackendFormat} for a given CompressionType. This is
     * guaranteed to match the backend format used by the following
     * createCompressedBackendTexture methods that take a CompressionType.
     * <p>
     * The caller should check that the returned format is valid (nullability).
     */
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
