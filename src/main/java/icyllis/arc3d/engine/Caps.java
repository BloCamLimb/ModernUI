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

import icyllis.arc3d.core.Color;
import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.engine.trash.GraphicsPipelineDesc_Old;
import icyllis.arc3d.engine.trash.PipelineKey_old;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents the capabilities of a 3D API Context.
 * <p>
 * Capabilities are used to test if something is capable or not. In other words,
 * these are optional and there are alternatives, required features are not listed
 * here. For historical reasons, there are still some capability methods, but only
 * return constants (become required).
 */
@SuppressWarnings("unused")
public abstract class Caps {

    /**
     * Most implementations support 4 or 8 simultaneous color targets.
     */
    public static final int MAX_COLOR_TARGETS = 8;
    /**
     * Max allowed number of vertex attribute locations.
     * Most implementations support 16 or 32 attributes, and our engine uses
     * 'int' as enable mask type, then this is 32, see {@link VertexInputLayout}.
     */
    public static final int MAX_VERTEX_ATTRIBUTES = 32;
    public static final int MAX_VERTEX_BINDINGS = 32;
    // the constants above are used to create fixed length arrays, however, there is no such thing in Java

    /**
     * Indicates the capabilities of the fixed function blend unit.
     */
    public enum BlendEquationSupport {
        /**
         * Support to select the operator that combines src and dst terms.
         */
        BASIC,
        /**
         * Additional fixed function support for specific SVG/PDF blend modes. Requires blend barriers.
         */
        ADVANCED,
        /**
         * Advanced blend equation support that does not require blend barriers, and permits overlap.
         */
        ADVANCED_COHERENT
    }

    protected final ShaderCaps mShaderCaps = new ShaderCaps();

    protected boolean mAnisotropySupport = false;
    protected boolean mGpuTracingSupport = false;
    protected boolean mConservativeRasterSupport = false;
    protected boolean mTransferPixelsToRowBytesSupport = false;
    protected boolean mMustSyncGpuDuringDiscard = true;
    protected boolean mTextureBarrierSupport = false;
    protected boolean mUseCpuStagingBuffers = false;

    // Not (yet) implemented in VK backend.
    protected boolean mDynamicStateArrayGeometryProcessorTextureSupport = false;

    protected BlendEquationSupport mBlendEquationSupport = BlendEquationSupport.BASIC;

    protected int mMapBufferFlags;

    protected int mMaxRenderTargetSize = 1;
    protected int mMaxPreferredRenderTargetSize = 1;
    protected int mMaxVertexAttributes = 16;
    protected int mMaxVertexBindings = 16;
    protected int mMaxTextureSize = 1;
    protected int mInternalMultisampleCount = 0;
    protected int mMaxPushConstantsSize = 0;
    protected int mMaxColorAttachments = 4;
    protected int mMinUniformBufferOffsetAlignment = 256;
    protected int mMinStorageBufferOffsetAlignment = 256;

    protected final DriverBugWorkarounds mDriverBugWorkarounds = new DriverBugWorkarounds();

    public Caps(ContextOptions options) {
        mDriverBugWorkarounds.applyOverrides(options.mDriverBugWorkarounds);
    }

    /**
     * @return the set of capabilities for shaders
     */
    public final ShaderCaps shaderCaps() {
        return mShaderCaps;
    }

    /**
     * Non-power-of-two texture tile.
     */
    public final boolean npotTextureTileSupport() {
        return true;
    }

    /**
     * To avoid as-yet-unnecessary complexity we don't allow any partial support of MIP Maps (e.g.
     * only for POT textures)
     */
    public final boolean mipmapSupport() {
        return true;
    }

    /**
     * Anisotropic filtering (AF).
     */
    public final boolean hasAnisotropySupport() {
        return mAnisotropySupport;
    }

    public final boolean gpuTracingSupport() {
        return mGpuTracingSupport;
    }

    /**
     * Allows mixed size FBO attachments.
     */
    public final boolean oversizedStencilSupport() {
        return true;
    }

    public final boolean textureBarrierSupport() {
        return true;
    }

    public final boolean sampleLocationsSupport() {
        return true;
    }

    public final boolean drawInstancedSupport() {
        return true;
    }

    public final boolean conservativeRasterSupport() {
        return mConservativeRasterSupport;
    }

    public final boolean wireframeSupport() {
        return true;
    }

    /**
     * This flag indicates that we never have to resolve MSAA. In practice, it means that we have
     * an MSAA-render-to-texture extension: Any render target we create internally will use the
     * extension, and any wrapped render target is the client's responsibility.
     */
    public final boolean msaaResolvesAutomatically() {
        return false;
    }

    /**
     * If true then when doing MSAA draws, we will prefer to discard the msaa attachment on load
     * and stores. The use of this feature for specific draws depends on the render target having a
     * resolve attachment, and if we need to load previous data the resolve attachment must be
     * usable as an input attachment/texture. Otherwise, we will just write out and store the msaa
     * attachment like normal.
     * <p>
     * This flag is similar to enabling gl render to texture for msaa rendering.
     */
    public final boolean preferDiscardableMSAAAttachment() {
        return false;
    }

    public final boolean halfFloatVertexAttributeSupport() {
        return true;
    }

    /**
     * Primitive restart functionality is core in ES 3.0, but using it will cause slowdowns on some
     * systems. This cap is only set if primitive restart will improve performance.
     */
    public final boolean usePrimitiveRestart() {
        return false;
    }

    public final boolean preferClientSideDynamicBuffers() {
        return false;
    }

    /**
     * On tilers, an initial fullscreen clear is an OPTIMIZATION. It allows the hardware to
     * initialize each tile with a constant value rather than loading each pixel from memory.
     */
    public final boolean preferFullscreenClears() {
        return false;
    }

    /**
     * Should we discard stencil values after a render pass? (Tilers get better performance if we
     * always load stencil buffers with a "clear" op, and then discard the content when finished.)
     */
    public final boolean discardStencilValuesAfterRenderPass() {
        //TODO review
        return preferFullscreenClears();
    }

    /**
     * D3D does not allow the refs or masks to differ on a two-sided stencil draw.
     */
    public final boolean twoSidedStencilRefsAndMasksMustMatch() {
        return false;
    }

    public final boolean preferVRAMUseOverFlushes() {
        return true;
    }

    public final boolean avoidStencilBuffers() {
        return false;
    }

    public final boolean avoidWritePixelsFastPath() {
        return false;
    }

    public final boolean requiresManualFBBarrierAfterTessellatedStencilDraw() {
        return false;
    }

    public final boolean nativeDrawIndexedIndirectIsBroken() {
        return false;
    }

    public final BlendEquationSupport blendEquationSupport() {
        return mBlendEquationSupport;
    }

    public final boolean advancedBlendEquationSupport() {
        return mBlendEquationSupport != BlendEquationSupport.BASIC;
    }

    public final boolean advancedCoherentBlendEquationSupport() {
        return mBlendEquationSupport == BlendEquationSupport.ADVANCED_COHERENT;
    }

    /**
     * On some GPUs it is a performance win to disable blending instead of doing src-over with a src
     * alpha equal to 1. To disable blending we collapse src-over to src and the backends will
     * handle the disabling of blending.
     */
    public final boolean shouldCollapseSrcOverToSrcWhenAble() {
        return false;
    }

    /**
     * When discarding the DirectContext do we need to sync the GPU before we start discarding
     * resources.
     */
    public final boolean mustSyncGpuDuringDiscard() {
        return mMustSyncGpuDuringDiscard;
    }

    public final boolean supportsTextureBarrier() {
        return mTextureBarrierSupport;
    }

    public final boolean reducedShaderMode() {
        return mShaderCaps.mReducedShaderMode;
    }

    /**
     * Scratch textures not being reused means that those scratch textures
     * that we upload to (i.e., don't have a render target) will not be
     * recycled in the texture cache. This is to prevent ghosting by drivers
     * (in particular for deferred architectures).
     */
    public final boolean reuseScratchTextures() {
        return true;
    }

    public final boolean reuseScratchBuffers() {
        return true;
    }

    public final boolean useCpuStagingBuffers() {
        return mUseCpuStagingBuffers;
    }

    /**
     * @return minimum required alignment, in bytes, for the offset
     * member of the {@link BufferViewInfo} structure for uniform buffers
     */
    public final int minUniformBufferOffsetAlignment() {
        return mMinUniformBufferOffsetAlignment;
    }

    /**
     * @return minimum required alignment, in bytes, for the offset
     * member of the {@link BufferViewInfo} structure for shader storage buffers
     */
    public final int minStorageBufferOffsetAlignment() {
        return mMinStorageBufferOffsetAlignment;
    }

    /**
     * Maximum number of attribute values per vertex input
     */
    public final int maxVertexAttributes() {
        return mMaxVertexAttributes;
    }

    public final int maxVertexBindings() {
        return mMaxVertexBindings;
    }

    public final int maxRenderTargetSize() {
        return mMaxRenderTargetSize;
    }

    /**
     * This is the largest render target size that can be used without incurring extra performance
     * cost. It is usually the max RT size, unless larger render targets are known to be slower.
     */
    public final int maxPreferredRenderTargetSize() {
        return mMaxPreferredRenderTargetSize;
    }

    public final int maxTextureSize() {
        return mMaxTextureSize;
    }

    public final int maxPushConstantsSize() {
        return mMaxPushConstantsSize;
    }

    /**
     * Max number of color attachments in a render pass.
     * This is ranged from 4 (typically on mobile) to 8 (typically on desktop).
     */
    public final int maxColorAttachments() {
        return mMaxColorAttachments;
    }

    public final int transferBufferAlignment() {
        return 1;
    }

    /**
     * Can a texture be made with the BackendFormat, and then be bound and sampled in a shader.
     * It must be a color format, you cannot pass a stencil format here.
     * <p>
     * For OpenGL: Formats that deprecated in core profile are not supported; Compressed formats
     * from extensions are uncertain; Others are always supported.
     */
    public abstract boolean isFormatTexturable(BackendFormat format);

    /**
     * Returns the maximum supported sample count for a format. 0 means the format is not renderable
     * 1 means the format is renderable but doesn't support MSAA.
     */
    public abstract int getMaxRenderTargetSampleCount(BackendFormat format);

    /**
     * Returns the number of samples to use when performing draws to the given config with internal
     * MSAA. If 0, we should not attempt to use internal multisampling.
     */
    public final int getInternalMultisampleCount(BackendFormat format) {
        return Math.min(mInternalMultisampleCount, getMaxRenderTargetSampleCount(format));
    }

    public abstract boolean isFormatRenderable(int colorType, BackendFormat format, int sampleCount);

    public abstract boolean isFormatRenderable(BackendFormat format, int sampleCount);

    /**
     * Find a sample count greater than or equal to the requested count which is supported for a
     * render target of the given format or 0 if no such sample count is supported. If the requested
     * sample count is 1 then 1 will be returned if non-MSAA rendering is supported, otherwise 0.
     *
     * @param sampleCount requested samples
     */
    public abstract int getRenderTargetSampleCount(int sampleCount, BackendFormat format);

    /**
     * Given a dst pixel config and a src color type what color type must the caller coax
     * the data into in order to use writePixels().
     * <p>
     * Low 32bits - colorType ((int) value).
     * High 32bits - transferOffsetAlignment (value >>> 32).
     * If the <code>write</code> is occurring using transferPixelsTo() then this provides
     * the minimum alignment of the offset into the transfer buffer.
     */
    public abstract long getSupportedWriteColorType(int dstColorType,
                                                    BackendFormat dstFormat,
                                                    int srcColorType);

    /**
     * Given a src surface's color type and its backend format as well as a color type the caller
     * would like read into, this provides a legal color type that the caller may pass to
     * readPixels(). The returned color type may differ from the passed dstColorType, in
     * which case the caller must convert the read pixel data (see ConvertPixels). When converting
     * to dstColorType the swizzle in the returned struct should be applied. The caller must check
     * the returned color type for UNKNOWN.
     * <p>
     * Low 32bits - colorType ((int) value).
     * High 32bits - transferOffsetAlignment (value >>> 32).
     * If the <code>write</code> is occurring using transferPixelsTo() then this provides
     * the minimum alignment of the offset into the transfer buffer.
     */
    public final long getSupportedReadColorType(int srcColorType,
                                                BackendFormat srcFormat,
                                                int dstColorType) {
        long read = onSupportedReadColorType(srcColorType, srcFormat, dstColorType);
        int colorType = (int) (read & 0xFFFFFFFFL);
        long transferOffsetAlignment = read >>> 32;

        // There are known problems with 24 vs 32 bit BPP with this color type. Just fail for now if
        // using a transfer buffer.
        if (colorType == ColorInfo.CT_RGB_888x) {
            transferOffsetAlignment = 0;
        }
        // It's very convenient to access 1 byte-per-channel 32-bit color types as uint32_t on the CPU.
        // Make those aligned reads out of the buffer even if the underlying API doesn't require it.
        int channelFlags = ColorInfo.colorTypeChannelFlags(colorType);
        if ((channelFlags == Color.COLOR_CHANNEL_FLAGS_RGBA || channelFlags == Color.COLOR_CHANNEL_FLAGS_RGB ||
                channelFlags == Color.COLOR_CHANNEL_FLAG_ALPHA || channelFlags == Color.COLOR_CHANNEL_FLAG_GRAY) &&
                ColorInfo.bytesPerPixel(colorType) == 4) {
            switch ((int) (transferOffsetAlignment & 0b11)) {
                // offset alignment already a multiple of 4
                case 0:
                    break;
                // offset alignment is a multiple of 2 but not 4.
                case 2:
                    transferOffsetAlignment *= 2;
                    break;
                // offset alignment is not a multiple of 2.
                default:
                    transferOffsetAlignment *= 4;
                    break;
            }
        }
        return colorType | (transferOffsetAlignment << 32);
    }

    protected abstract long onSupportedReadColorType(int srcColorType,
                                                     BackendFormat srcFormat,
                                                     int dstColorType);

    /**
     * Does writePixels() support a src buffer where the row bytes is not equal to bpp * w?
     */
    public final boolean writePixelsRowBytesSupport() {
        return true;
    }

    /**
     * Does transferPixelsTo() support a src buffer where the row bytes is not equal to
     * bpp * w?
     */
    public final boolean transferPixelsToRowBytesSupport() {
        return mTransferPixelsToRowBytesSupport;
    }

    /**
     * Does readPixels() support a dst buffer where the row bytes is not equal to bpp * w?
     */
    public final boolean readPixelsRowBytesSupport() {
        return true;
    }

    public final boolean transferFromSurfaceToBufferSupport() {
        return true;
    }

    public final boolean transferFromBufferToTextureSupport() {
        return true;
    }

    /**
     * True in environments that will issue errors if memory uploaded to buffers
     * is not initialized (even if not read by draw calls).
     */
    public final boolean mustClearUploadedBufferData() {
        return false;
    }

    /**
     * For some environments, there is a performance or safety concern to not
     * initializing textures. For example, with WebGL and Firefox, there is a large
     * performance hit to not doing it.
     */
    public final boolean shouldInitializeTextures() {
        return false;
    }

    /**
     * Supports using Fence.
     */
    public final boolean fenceSyncSupport() {
        return true;
    }

    /**
     * Supports using Semaphore.
     */
    public final boolean semaphoreSupport() {
        return true;
    }

    public final boolean crossContextTextureSupport() {
        return true;
    }

    public final boolean dynamicStateArrayGeometryProcessorTextureSupport() {
        return mDynamicStateArrayGeometryProcessorTextureSupport;
    }

    // Not all backends support clearing with a scissor test (e.g. Metal), this will always
    // return true if performColorClearsAsDraws() returns true.
    public final boolean performPartialClearsAsDraws() {
        return false;
    }

    // Many drivers have issues with color clears.
    public final boolean performColorClearsAsDraws() {
        return false;
    }

    public final boolean avoidLargeIndexBufferDraws() {
        return false;
    }

    public final boolean performStencilClearsAsDraws() {
        return false;
    }

    // Should we disable TessellationPathRenderer due to a faulty driver?
    public final boolean disableTessellationPathRenderer() {
        return false;
    }

    /**
     * The CLAMP_TO_BORDER wrap mode for texture coordinates was added to desktop GL in 1.3, and
     * GLES 3.2, but is also available in extensions. Vulkan and Metal always have support.
     */
    public final boolean clampToBorderSupport() {
        return true;
    }

    /**
     * If a texture or render target can be created with these params.
     */
    public final boolean validateSurfaceParams(int width, int height,
                                               BackendFormat format,
                                               int sampleCount,
                                               int surfaceFlags) {
        if (width < 1 || height < 1) {
            return false;
        }
        if ((surfaceFlags & ISurface.FLAG_SAMPLED_IMAGE) != 0 &&
                !isFormatTexturable(format)) {
            return false;
        }
        if ((surfaceFlags & ISurface.FLAG_RENDERABLE) != 0) {
            final int maxSize = maxRenderTargetSize();
            if (width > maxSize || height > maxSize) {
                return false;
            }
            return isFormatRenderable(format, sampleCount);
        }
        final int maxSize = maxTextureSize();
        if (width > maxSize || height > maxSize) {
            return false;
        }
        //TODO allow multisample textures?
        return sampleCount == 1;
    }

    /**
     * If an attachment can be created with these params.
     */
    public final boolean validateAttachmentParams(int width, int height,
                                                  BackendFormat format,
                                                  int sampleCount) {
        if (width < 1 || height < 1) {
            return false;
        }
        final int maxSize = maxRenderTargetSize();
        if (width > maxSize || height > maxSize) {
            return false;
        }
        return isFormatRenderable(format, sampleCount);
    }

    public final boolean isFormatCompatible(int colorType, BackendFormat format) {
        if (colorType == ColorInfo.CT_UNKNOWN) {
            return false;
        }
        int compression = format.getCompressionType();
        if (compression != ColorInfo.COMPRESSION_NONE) {
            return colorType == (DataUtils.compressionTypeIsOpaque(compression) ?
                    ColorInfo.CT_RGB_888x :
                    ColorInfo.CT_RGBA_8888);
        }
        return onFormatCompatible(colorType, format);
    }

    protected abstract boolean onFormatCompatible(int colorType, BackendFormat format);

    /**
     * @param imageType
     * @param colorType
     * @param width
     * @param height
     * @param depthOrArraySize
     * @param imageFlags
     * @return
     * @see ISurface#FLAG_MIPMAPPED
     * @see ISurface#FLAG_SAMPLED_IMAGE
     * @see ISurface#FLAG_STORAGE_IMAGE
     * @see ISurface#FLAG_RENDERABLE
     * @see ISurface#FLAG_MEMORYLESS
     * @see ISurface#FLAG_PROTECTED
     */
    public ImageDesc getDefaultColorImageDesc(int imageType,
                                              int colorType,
                                              int width, int height,
                                              int depthOrArraySize,
                                              int imageFlags) {
        return getDefaultColorImageDesc(imageType, colorType, width, height, depthOrArraySize,
                0, 0, imageFlags);
    }

    /**
     * @param imageType
     * @param colorType
     * @param width
     * @param height
     * @param depthOrArraySize
     * @param mipLevelCount
     * @param sampleCount
     * @param imageFlags
     * @return
     * @see ISurface#FLAG_MIPMAPPED
     * @see ISurface#FLAG_SAMPLED_IMAGE
     * @see ISurface#FLAG_STORAGE_IMAGE
     * @see ISurface#FLAG_RENDERABLE
     * @see ISurface#FLAG_MEMORYLESS
     * @see ISurface#FLAG_PROTECTED
     */
    @Nullable
    public ImageDesc getDefaultColorImageDesc(int imageType,
                                              int colorType,
                                              int width, int height,
                                              int depthOrArraySize,
                                              int mipLevelCount,
                                              int sampleCount,
                                              int imageFlags) {
        return null;
    }

    @Nullable
    public ImageDesc getDefaultDepthStencilImageDesc(int depthBits,
                                                     int stencilBits,
                                                     int width, int height,
                                                     int sampleCount,
                                                     int imageFlags) {
        return null;
    }

    /**
     * These are used when creating a new texture internally.
     */
    @Nullable
    public final BackendFormat getDefaultBackendFormat(int colorType,
                                                       boolean renderable) {
        // Unknown color types are always an invalid format.
        if (colorType == ColorInfo.CT_UNKNOWN) {
            return null;
        }
        BackendFormat format = onGetDefaultBackendFormat(colorType);
        if (format == null || !isFormatTexturable(format)) {
            return null;
        }
        if (!isFormatCompatible(colorType, format)) {
            return null;
        }
        // Currently, we require that it be possible to write pixels into the "default" format. Perhaps,
        // that could be a separate requirement from the caller. It seems less necessary if
        // renderability was requested.
        if ((getSupportedWriteColorType(colorType, format, colorType) & 0xFFFFFFFFL) == ColorInfo.CT_UNKNOWN) {
            return null;
        }
        if (renderable && !isFormatRenderable(colorType, format, 1)) {
            return null;
        }
        return format;
    }

    @Nullable
    protected abstract BackendFormat onGetDefaultBackendFormat(int colorType);

    @Nullable
    public abstract BackendFormat getCompressedBackendFormat(int compressionType);

    @Nonnull
    public abstract PipelineKey_old makeDesc(PipelineKey_old desc,
                                             GpuRenderTarget renderTarget,
                                             final GraphicsPipelineDesc_Old graphicsPipelineDesc);

    @Nonnull
    public abstract PipelineKey makeGraphicsPipelineKey(
            PipelineKey old,
            PipelineDesc pipelineDesc,
            RenderPassDesc renderPassDesc);

    public final short getReadSwizzle(ImageDesc desc, int colorType) {
        int compression = desc.getCompressionType();
        if (compression != ColorInfo.COMPRESSION_NONE) {
            if (colorType == ColorInfo.CT_RGB_888x || colorType == ColorInfo.CT_RGBA_8888) {
                return Swizzle.RGBA;
            }
            assert false;
            return Swizzle.RGBA;
        }

        return onGetReadSwizzle(desc, colorType);
    }

    protected abstract short onGetReadSwizzle(ImageDesc desc, int colorType);

    public abstract short getWriteSwizzle(ImageDesc desc, int colorType);

    public abstract IResourceKey computeImageKey(ImageDesc desc,
                                                 IResourceKey recycle);

    // unmodifiable
    public final DriverBugWorkarounds workarounds() {
        return mDriverBugWorkarounds;
    }

    protected final void finishInitialization(ContextOptions options) {
        mShaderCaps.applyOptionsOverrides(options);
        onApplyOptionsOverrides(options);

        mInternalMultisampleCount = options.mInternalMultisampleCount;

        // Our render targets are always created with textures as the color attachment, hence this min:
        mMaxRenderTargetSize = Math.min(mMaxRenderTargetSize, mMaxTextureSize);
        mMaxPreferredRenderTargetSize = Math.min(mMaxPreferredRenderTargetSize, mMaxRenderTargetSize);
    }

    protected void onApplyOptionsOverrides(ContextOptions options) {
        mDriverBugWorkarounds.applyOverrides(options.mDriverBugWorkarounds);
    }
}
