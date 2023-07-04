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

import icyllis.arc3d.Rect2i;
import icyllis.arc3d.SharedPtr;
import icyllis.arc3d.engine.ops.OpsTask;
import icyllis.arc3d.opengl.GLEngine;
import icyllis.arc3d.shaderc.Compiler;
import icyllis.modernui.graphics.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Set;

/**
 * Represents the logical device of the backend 3D API, holding a reference to
 * {@link DirectContext}. It is responsible for creating/deleting 3D API objects,
 * transferring data, submitting 3D API commands, etc. Most methods are expected
 * on render thread (direct thread).
 */
public abstract class Engine {

    /**
     * Block engine-private values.
     */
    public static int colorTypeToPublic(int ct) {
        return switch (ct) {
            case ImageInfo.CT_UNKNOWN,
                    ImageInfo.CT_ALPHA_8,
                    ImageInfo.CT_RGB_565,
                    ImageInfo.CT_RGBA_8888,
                    ImageInfo.CT_RGBA_8888_SRGB,
                    ImageInfo.CT_RGB_888x,
                    ImageInfo.CT_RG_88,
                    ImageInfo.CT_BGRA_8888,
                    ImageInfo.CT_RGBA_1010102,
                    ImageInfo.CT_BGRA_1010102,
                    ImageInfo.CT_GRAY_8,
                    ImageInfo.CT_ALPHA_F16,
                    ImageInfo.CT_RGBA_F16,
                    ImageInfo.CT_RGBA_F16_CLAMPED,
                    ImageInfo.CT_RGBA_F32,
                    ImageInfo.CT_ALPHA_16,
                    ImageInfo.CT_RG_1616,
                    ImageInfo.CT_RG_F16,
                    ImageInfo.CT_RGBA_16161616,
                    ImageInfo.CT_R_8 -> ct;
            case ImageInfo.CT_ALPHA_8xxx,
                    ImageInfo.CT_ALPHA_F32xxx,
                    ImageInfo.CT_GRAY_8xxx,
                    ImageInfo.CT_R_8xxx,
                    ImageInfo.CT_RGB_888,
                    ImageInfo.CT_R_16,
                    ImageInfo.CT_R_F16,
                    ImageInfo.CT_GRAY_ALPHA_88 -> ImageInfo.CT_UNKNOWN;
            default -> throw new AssertionError(ct);
        };
    }

    /**
     * Possible 3D APIs that may be used by Arc 3D Engine.
     */
    public interface BackendApi {

        /**
         * OpenGL 3.3 or 4.5 core profile (desktop)
         */
        int kOpenGL = 0;
        /**
         * Vulkan 1.1 (desktop and mobile)
         */
        int kVulkan = 1;
        /**
         * Mock draws nothing. It is used for unit tests and to measure CPU overhead.
         */
        int kMock = 2;

        static String toString(int value) {
            return switch (value) {
                case kOpenGL -> "OpenGL";
                case kVulkan -> "Vulkan";
                case kMock -> "Mock";
                default -> throw new AssertionError(value);
            };
        }
    }

    /**
     * Image and Surfaces can be stored such that (0, 0) in texture space may correspond to
     * either the upper-left or lower-left content pixel.
     */
    public interface SurfaceOrigin {

        int kUpperLeft = 0; // top left, Vulkan
        int kLowerLeft = 1; // bottom left, OpenGL
    }

    /**
     * A Context's cache of backend context state can be partially invalidated.
     * These enums are specific to the GL backend and we'd add a new set for an alternative backend.
     *
     * @see GLEngine#markContextDirty(int)
     */
    public interface GLBackendState {

        int kRenderTarget = 1;
        int kPixelStore = 1 << 1;
        /**
         * Shader stages, vertex array and input buffers
         */
        int kPipeline = 1 << 2;
        /**
         * Also includes samplers bound to texture units
         */
        int kTexture = 1 << 3;
        int kStencil = 1 << 4;
        /**
         * Antialiasing and conservative raster
         */
        int kRaster = 1 << 5;
        int kBlend = 1 << 6;
        /**
         * View state stands for scissor and viewport
         */
        int kView = 1 << 7;
        int kMisc = 1 << 8;
    }

    /**
     * Indicates the type of pending IO operations that can be recorded for GPU resources.
     */
    public interface IOType {

        int kRead = 0;
        int kWrite = 1;
        int kRW = 2;
    }

    /**
     * Describes the intended usage a GPU buffer.
     */
    public interface BufferUsageFlags {

        /**
         * Vertex buffer (also includes instance buffer).
         */
        int kVertex = 1;
        /**
         * Index buffer, also known as element buffer.
         */
        int kIndex = 1 << 1;
        /**
         * Indirect buffer, also known as argument buffer.
         */
        int kDrawIndirect = 1 << 2;

        int kTransferSrc = 1 << 3; // transfer src only
        int kTransferDst = 1 << 4; // transfer dst only

        /**
         * For VBO, data store will be respecified once by Host and used at most a frame.
         * (Per-frame updates, VBO, IBO, etc.)
         */
        int kStream = 1 << 5;
        /**
         * Data store will be specified by Host once and may be respecified
         * repeatedly by Device. (Fixed vertex/index buffer, etc.)
         */
        int kStatic = 1 << 6;
        /**
         * Data store will be respecified randomly by Host and Device.
         * (Uniform buffer, staging buffer, etc.)
         */
        int kDynamic = 1 << 7;

        int kUniform = 1 << 8; // TODO remove, UBO is special buffers
    }

    /**
     * Shader flags.
     */
    public interface ShaderFlags {

        int kVertex = 1;
        int kFragment = 1 << 1;
    }

    public static int colorTypeChannelFlags(int ct) {
        return switch (ct) {
            case ImageInfo.CT_UNKNOWN -> 0;
            case ImageInfo.CT_ALPHA_8,
                    ImageInfo.CT_ALPHA_16,
                    ImageInfo.CT_ALPHA_F16,
                    ImageInfo.CT_ALPHA_8xxx,
                    ImageInfo.CT_ALPHA_F32xxx -> Color.COLOR_CHANNEL_FLAG_ALPHA;
            case ImageInfo.CT_RGB_565,
                    ImageInfo.CT_RGB_888,
                    ImageInfo.CT_RGB_888x -> Color.COLOR_CHANNEL_FLAGS_RGB;
            case ImageInfo.CT_RGBA_16161616,
                    ImageInfo.CT_RGBA_F32,
                    ImageInfo.CT_RGBA_F16_CLAMPED,
                    ImageInfo.CT_RGBA_F16,
                    ImageInfo.CT_BGRA_1010102,
                    ImageInfo.CT_RGBA_1010102,
                    ImageInfo.CT_BGRA_8888,
                    ImageInfo.CT_RGBA_8888_SRGB,
                    ImageInfo.CT_RGBA_8888 -> Color.COLOR_CHANNEL_FLAGS_RGBA;
            case ImageInfo.CT_RG_88,
                    ImageInfo.CT_RG_1616,
                    ImageInfo.CT_RG_F16 -> Color.COLOR_CHANNEL_FLAGS_RG;
            case ImageInfo.CT_GRAY_8,
                    ImageInfo.CT_GRAY_8xxx -> Color.COLOR_CHANNEL_FLAG_GRAY;
            case ImageInfo.CT_R_8,
                    ImageInfo.CT_R_16,
                    ImageInfo.CT_R_F16,
                    ImageInfo.CT_R_8xxx -> Color.COLOR_CHANNEL_FLAG_RED;
            case ImageInfo.CT_GRAY_ALPHA_88 -> Color.COLOR_CHANNEL_FLAG_GRAY | Color.COLOR_CHANNEL_FLAG_ALPHA;
            default -> throw new AssertionError(ct);
        };
    }

    /**
     * Describes the encoding of channel data in a ColorType.
     *
     * @see #colorTypeEncoding(int)
     */
    public static final int
            COLOR_ENCODING_UNORM = 0,
            COLOR_ENCODING_SRGB_UNORM = 1,
            COLOR_ENCODING_FLOAT = 2;

    public static int colorTypeEncoding(int ct) {
        return switch (ct) {
            case ImageInfo.CT_UNKNOWN,
                    ImageInfo.CT_ALPHA_8,
                    ImageInfo.CT_RGB_565,
                    ImageInfo.CT_RGBA_8888,
                    ImageInfo.CT_RGB_888x,
                    ImageInfo.CT_RG_88,
                    ImageInfo.CT_BGRA_8888,
                    ImageInfo.CT_RGBA_1010102,
                    ImageInfo.CT_BGRA_1010102,
                    ImageInfo.CT_GRAY_8,
                    ImageInfo.CT_ALPHA_8xxx,
                    ImageInfo.CT_GRAY_8xxx,
                    ImageInfo.CT_R_8xxx,
                    ImageInfo.CT_ALPHA_16,
                    ImageInfo.CT_RG_1616,
                    ImageInfo.CT_RGBA_16161616,
                    ImageInfo.CT_RGB_888,
                    ImageInfo.CT_R_8,
                    ImageInfo.CT_R_16,
                    ImageInfo.CT_GRAY_ALPHA_88 -> COLOR_ENCODING_UNORM;
            case ImageInfo.CT_RGBA_8888_SRGB -> COLOR_ENCODING_SRGB_UNORM;
            case ImageInfo.CT_ALPHA_F16,
                    ImageInfo.CT_RGBA_F16,
                    ImageInfo.CT_RGBA_F16_CLAMPED,
                    ImageInfo.CT_RGBA_F32,
                    ImageInfo.CT_ALPHA_F32xxx,
                    ImageInfo.CT_RG_F16,
                    ImageInfo.CT_R_F16 -> COLOR_ENCODING_FLOAT;
            default -> throw new AssertionError(ct);
        };
    }

    /**
     * Some pixel configs are inherently clamped to [0,1], some are allowed to go outside that range,
     * and some are FP but manually clamped in the XP.
     *
     * @see #colorTypeClampType(int)
     */
    public static final int
            CLAMP_TYPE_AUTO = 0,    // Normalized, fixed-point configs
            CLAMP_TYPE_MANUAL = 1,  // Clamped FP configs
            CLAMP_TYPE_NONE = 2;    // Normal (un-clamped) FP configs

    public static int colorTypeClampType(int ct) {
        return switch (ct) {
            case ImageInfo.CT_UNKNOWN,
                    ImageInfo.CT_ALPHA_8,
                    ImageInfo.CT_RGB_565,
                    ImageInfo.CT_RGBA_8888,
                    ImageInfo.CT_RGBA_8888_SRGB,
                    ImageInfo.CT_RGB_888x,
                    ImageInfo.CT_RG_88,
                    ImageInfo.CT_BGRA_8888,
                    ImageInfo.CT_RGBA_1010102,
                    ImageInfo.CT_BGRA_1010102,
                    ImageInfo.CT_GRAY_8,
                    ImageInfo.CT_ALPHA_8xxx,
                    ImageInfo.CT_GRAY_8xxx,
                    ImageInfo.CT_R_8xxx,
                    ImageInfo.CT_ALPHA_16,
                    ImageInfo.CT_RG_1616,
                    ImageInfo.CT_RGBA_16161616,
                    ImageInfo.CT_RGB_888,
                    ImageInfo.CT_R_8,
                    ImageInfo.CT_R_16 -> CLAMP_TYPE_AUTO;
            case ImageInfo.CT_RGBA_F16_CLAMPED -> CLAMP_TYPE_MANUAL;
            case ImageInfo.CT_ALPHA_F16,
                    ImageInfo.CT_RGBA_F16,
                    ImageInfo.CT_RGBA_F32,
                    ImageInfo.CT_ALPHA_F32xxx,
                    ImageInfo.CT_RG_F16,
                    ImageInfo.CT_R_F16 -> CLAMP_TYPE_NONE;
            default -> throw new AssertionError(ct);
        };
    }

    /**
     * Geometric primitives used for drawing.
     * <p>
     * We can't simply use point or line, because both OpenGL and Vulkan can only guarantee
     * the rasterization of one pixel in screen coordinates, may or may not anti-aliased.
     */
    public interface PrimitiveType {
        byte PointList = 0; // 1 px only
        byte LineList = 1; // 1 px wide only
        byte LineStrip = 2; // 1 px wide only
        byte TriangleList = 3; // separate triangle
        byte TriangleStrip = 4; // connected triangle
    }

    /**
     * Mask formats. Used by the font atlas. Important that these are 0-based.
     * <p>
     * Using L-shift to get the number of bytes-per-pixel for the specified mask format.
     */
    public static final int
            MASK_FORMAT_A8 = 0,     // 1-byte per pixel
            MASK_FORMAT_A565 = 1,   // 2-bytes per pixel, RGB represent 3-channel LCD coverage
            MASK_FORMAT_ARGB = 2;   // 4-bytes per pixel, color format

    /**
     * Budget types. Used with resources that have a large memory allocation.
     *
     * @see Resource
     */
    // @formatter:off
    public interface BudgetType {
        /**
         * The resource is budgeted and is subject to cleaning up under budget pressure.
         */
        byte Budgeted       = 0;
        /**
         * The resource is not budgeted and is cleaned up as soon as it has no refs regardless
         * of whether it has a unique or scratch key.
         */
        byte NotBudgeted    = 1;
        /**
         * The resource is not budgeted and is allowed to remain in the cache with no refs
         * if it has a unique key. Scratch keys are ignored.
         */
        byte WrapCacheable  = 2;
    }

    /**
     * Load ops. Used to specify the load operation to be used when an OpsTask/OpsRenderPass
     * begins execution.
     */
    public interface LoadOp {
        byte Load       = 0;
        byte Clear      = 1;
        byte DontCare   = 2;
        byte Count      = 3;
    }

    /**
     * Store ops. Used to specify the store operation to be used when an OpsTask/OpsRenderPass
     * ends execution.
     */
    public interface StoreOp {
        byte Store      = 0;
        byte DontCare   = 1;
        byte Count      = 2;
    }

    /**
     * Combination of load ops and store ops.
     */
    public interface LoadStoreOps {

        byte StoreOpShift = Byte.SIZE / 2;

        /**
         * Combination of load ops and store ops.
         * 0-4 bits: LoadOp;
         * 4-8 bits: StoreOp
         */
        byte
                Load_Store          = LoadOp.Load     | (StoreOp.Store    << StoreOpShift),
                Clear_Store         = LoadOp.Clear    | (StoreOp.Store    << StoreOpShift),
                DontLoad_Store      = LoadOp.DontCare | (StoreOp.Store    << StoreOpShift),
                Load_DontStore      = LoadOp.Load     | (StoreOp.DontCare << StoreOpShift),
                Clear_DontStore     = LoadOp.Clear    | (StoreOp.DontCare << StoreOpShift),
                DontLoad_DontStore  = LoadOp.DontCare | (StoreOp.DontCare << StoreOpShift);

        static byte make(byte load, byte store) {
            assert (load  < (1 << StoreOpShift));
            assert (store < (1 << StoreOpShift));
            return (byte) (load | (store << StoreOpShift));
        }

        /**
         * @see LoadOp
         */
        static byte loadOp(byte ops) {
            return (byte) (ops & ((1 << StoreOpShift) - 1));
        }

        /**
         * @see StoreOp
         */
        static byte storeOp(byte ops) {
            return (byte) (ops >>> StoreOpShift);
        }
    }

    static {
        assert (LoadStoreOps.Load_Store         == LoadStoreOps.make(LoadOp.Load,       StoreOp.Store));
        assert (LoadStoreOps.Clear_Store        == LoadStoreOps.make(LoadOp.Clear,      StoreOp.Store));
        assert (LoadStoreOps.DontLoad_Store     == LoadStoreOps.make(LoadOp.DontCare,   StoreOp.Store));
        assert (LoadStoreOps.Load_DontStore     == LoadStoreOps.make(LoadOp.Load,       StoreOp.DontCare));
        assert (LoadStoreOps.Clear_DontStore    == LoadStoreOps.make(LoadOp.Clear,      StoreOp.DontCare));
        assert (LoadStoreOps.DontLoad_DontStore == LoadStoreOps.make(LoadOp.DontCare,   StoreOp.DontCare));
        //noinspection ConstantValue
        assert (LoadOp.Count  <= (1 << LoadStoreOps.StoreOpShift)) &&
                (StoreOp.Count <= (1 << LoadStoreOps.StoreOpShift));
    }
    // @formatter:on

    /**
     * Specifies if the holder owns the backend, OpenGL or Vulkan, object.
     */
    public static final boolean
            Ownership_Borrowed = false, // Holder does not destroy the backend object.
            Ownership_Owned = true;     // Holder destroys the backend object.

    /**
     * Types used to describe format of vertices in arrays.
     */
    public interface VertexAttribType {

        byte
                kFloat = 0,
                kFloat2 = 1,
                kFloat3 = 2,
                kFloat4 = 3,
                kHalf = 4,
                kHalf2 = 5,
                kHalf4 = 6;
        byte
                kInt2 = 7,   // vector of 2 32-bit ints
                kInt3 = 8,   // vector of 3 32-bit ints
                kInt4 = 9;   // vector of 4 32-bit ints
        byte
                kByte = 10,   // signed byte
                kByte2 = 11,  // vector of 2 8-bit signed bytes
                kByte4 = 12,  // vector of 4 8-bit signed bytes
                kUByte = 13,  // unsigned byte
                kUByte2 = 14, // vector of 2 8-bit unsigned bytes
                kUByte4 = 15; // vector of 4 8-bit unsigned bytes
        byte
                kUByte_norm = 16,  // unsigned byte, e.g. coverage, 0 -> 0.0f, 255 -> 1.0f.
                kUByte4_norm = 17; // vector of 4 unsigned bytes, e.g. colors, 0 -> 0.0f, 255 -> 1.0f.
        byte
                kShort2 = 18,       // vector of 2 16-bit shorts.
                kShort4 = 19;       // vector of 4 16-bit shorts.
        byte
                kUShort2 = 20,      // vector of 2 unsigned shorts. 0 -> 0, 65535 -> 65535.
                kUShort2_norm = 21; // vector of 2 unsigned shorts. 0 -> 0.0f, 65535 -> 1.0f.
        byte
                kInt = 22,
                kUInt = 23;
        byte
                kUShort_norm = 24;
        byte
                kUShort4_norm = 25; // vector of 4 unsigned shorts. 0 -> 0.0f, 65535 -> 1.0f.
        byte
                kLast = kUShort4_norm;

        /**
         * @return size in bytes
         */
        static int size(byte type) {
            switch (type) {
                case kFloat:
                    return Float.BYTES;
                case kFloat2:
                    return 2 * Float.BYTES;
                case kFloat3:
                    return 3 * Float.BYTES;
                case kFloat4:
                    return 4 * Float.BYTES;
                case kHalf:
                case kUShort_norm:
                    return Short.BYTES;
                case kHalf2:
                case kShort2:
                case kUShort2:
                case kUShort2_norm:
                    return 2 * Short.BYTES;
                case kHalf4:
                case kShort4:
                case kUShort4_norm:
                    return 4 * Short.BYTES;
                case kInt2:
                    return 2 * Integer.BYTES;
                case kInt3:
                    return 3 * Integer.BYTES;
                case kInt4:
                    return 4 * Integer.BYTES;
                case kByte:
                case kUByte:
                case kUByte_norm:
                    return Byte.BYTES;
                case kByte2:
                case kUByte2:
                    return 2 * Byte.BYTES;
                case kByte4:
                case kUByte4:
                case kUByte4_norm:
                    return 4 * Byte.BYTES;
                case kInt:
                case kUInt:
                    return Integer.BYTES;
            }
            throw new AssertionError(type);
        }
    }

    /**
     * ResourceHandle is an opaque handle to a resource, actually a table index.
     */
    public static final int INVALID_RESOURCE_HANDLE = -1;


    // this device is managed by this context
    protected final DirectContext mContext;
    protected final Caps mCaps;
    protected final Compiler mCompiler;

    protected final Stats mStats = new Stats();

    private final ArrayList<FlushInfo.SubmittedCallback> mSubmittedCallbacks = new ArrayList<>();
    private int mResetBits = ~0;

    protected Engine(DirectContext context, Caps caps) {
        assert context != null && caps != null;
        mContext = context;
        mCaps = caps;
        mCompiler = new Compiler();
    }

    public final DirectContext getContext() {
        return mContext;
    }

    /**
     * Gets the capabilities of the context.
     */
    public Caps getCaps() {
        return mCaps;
    }

    /**
     * Gets the compiler used for compiling DSL into backend shader code.
     */
    public final Compiler getShaderCompiler() {
        return mCompiler;
    }

    public abstract PipelineStateCache getPipelineStateCache();

    /**
     * Called by context when the underlying backend context is already or will be destroyed
     * before {@link DirectContext}.
     * <p>
     * If cleanup is true, free allocated resources (other than {@link ResourceCache}) before
     * returning and ensure no backend 3D API calls will be made after this method returns.
     * Otherwise, no cleanup should be attempted, immediately cease making backend API calls.
     */
    public void disconnect(boolean cleanup) {
    }

    public final Stats getStats() {
        return mStats;
    }

    /**
     * The engine object normally assumes that no outsider is setting state
     * within the underlying 3D API's context/device/whatever. This call informs
     * the engine that the state was modified, and it shouldn't make assumptions
     * about the state.
     */
    public final void markContextDirty(int state) {
        mResetBits |= state;
    }

    protected final void handleDirtyContext() {
        if (mResetBits != 0) {
            onResetContext(mResetBits);
            mResetBits = 0;
        }
    }

    /**
     * Called when the 3D context state is unknown. Subclass should emit any
     * assumed 3D context state and dirty any state cache.
     */
    protected void onResetContext(int resetBits) {
    }

    public abstract BufferAllocPool getVertexPool();

    public abstract BufferAllocPool getInstancePool();

    /**
     * Creates a texture object and allocates its GPU memory. In other words, the
     * image data is dirty and needs to be uploaded later. If mipmapped, also allocates
     * <code>(31 - CLZ(max(width,height)))</code> mipmaps in addition to the base level.
     * NPoT (non-power-of-two) dimensions are always supported. Compressed format are
     * supported.
     *
     * @param width  the width of the texture to be created
     * @param height the height of the texture to be created
     * @param format the backend format for the texture
     * @return the texture object if successful, otherwise nullptr
     * @see Surface#FLAG_BUDGETED
     * @see Surface#FLAG_MIPMAPPED
     * @see Surface#FLAG_RENDERABLE
     * @see Surface#FLAG_PROTECTED
     */
    @Nullable
    @SharedPtr
    public final Texture createTexture(int width, int height,
                                       BackendFormat format,
                                       int sampleCount,
                                       int surfaceFlags,
                                       String label) {
        if (format.isCompressed()) {
            // use createCompressedTexture
            return null;
        }
        if (!mCaps.validateSurfaceParams(width, height, format,
                sampleCount, surfaceFlags)) {
            return null;
        }
        int maxMipLevel = (surfaceFlags & Surface.FLAG_MIPMAPPED) != 0
                ? MathUtil.floorLog2(Math.max(width, height))
                : 0;
        int mipLevelCount = maxMipLevel + 1; // +1 base level 0
        if ((surfaceFlags & Surface.FLAG_RENDERABLE) != 0) {
            sampleCount = mCaps.getRenderTargetSampleCount(sampleCount, format);
        }
        assert (sampleCount > 0 && sampleCount <= 64);
        handleDirtyContext();
        final Texture texture = onCreateTexture(width, height, format,
                mipLevelCount, sampleCount, surfaceFlags);
        if (texture != null) {
            // we don't copy the backend format object, use identity rather than equals()
            assert texture.getBackendFormat() == format;
            assert (surfaceFlags & Surface.FLAG_RENDERABLE) == 0 || texture.getRenderTarget() != null;
            if (label != null) {
                texture.setLabel(label);
            }
            mStats.incTextureCreates();
        }
        return texture;
    }

    /**
     * Overridden by backend-specific derived class to create objects.
     * <p>
     * Texture size and format support will have already been validated in base class
     * before onCreateTexture is called.
     */
    @Nullable
    @SharedPtr
    protected abstract Texture onCreateTexture(int width, int height,
                                               BackendFormat format,
                                               int mipLevelCount,
                                               int sampleCount,
                                               int surfaceFlags);

    /**
     * This makes the backend texture be renderable. If <code>sampleCount</code> is > 1 and
     * the underlying API uses separate MSAA render buffers then a MSAA render buffer is created
     * that resolves to the texture.
     * <p>
     * Ownership specifies rules for external GPU resources imported into Engine. If false,
     * Engine will assume the client will keep the resource alive and Engine will not free it.
     * If true, Engine will assume ownership of the resource and free it.
     *
     * @param texture the backend texture must be single sample
     * @return a non-cacheable render target, or null if failed
     */
    @Nullable
    @SharedPtr
    public Texture wrapRenderableBackendTexture(BackendTexture texture,
                                                int sampleCount,
                                                boolean ownership) {
        handleDirtyContext();
        if (sampleCount < 1) {
            return null;
        }

        final Caps caps = mCaps;

        if (!caps.isFormatTexturable(texture.getBackendFormat()) ||
                !caps.isFormatRenderable(texture.getBackendFormat(), sampleCount)) {
            return null;
        }

        if (texture.getWidth() > caps.maxRenderTargetSize() ||
                texture.getHeight() > caps.maxRenderTargetSize()) {
            return null;
        }
        return onWrapRenderableBackendTexture(texture, sampleCount, ownership);
    }

    @Nullable
    @SharedPtr
    protected abstract Texture onWrapRenderableBackendTexture(BackendTexture texture,
                                                              int sampleCount,
                                                              boolean ownership);

    /**
     * Updates the pixels in a rectangle of a texture. No sRGB/linear conversions are performed.
     * The write operation can fail because of the surface doesn't support writing (e.g. read only),
     * the color type is not allowed for the format of the texture or if the rectangle written
     * is not contained in the texture.
     *
     * @param texture      the texture to write to
     * @param dstColorType the color type for this use of the texture
     * @param srcColorType the color type of the source buffer
     * @param rowBytes     the row bytes, must be a multiple of srcColorType's bytes-per-pixel.
     * @param pixels       the pointer to the texel data for base level image
     * @return true if succeeded, false if not
     */
    public boolean writePixels(Texture texture,
                               int x, int y,
                               int width, int height,
                               int dstColorType,
                               int srcColorType,
                               int rowBytes, long pixels) {
        assert (texture != null);
        if (x < 0 || y < 0 || width <= 0 || height <= 0) {
            return false;
        }
        if (texture.isReadOnly()) {
            return false;
        }
        assert (texture.getWidth() > 0 && texture.getHeight() > 0);
        if (x + width > texture.getWidth() || y + height > texture.getHeight()) {
            return false;
        }
        int bpp = ImageInfo.bytesPerPixel(srcColorType);
        int minRowBytes = width * bpp;
        if (rowBytes < minRowBytes) {
            return false;
        }
        if (rowBytes % bpp != 0) {
            return false;
        }
        if (pixels == 0) {
            return true;
        }
        handleDirtyContext();
        if (!onWritePixels(texture,
                x, y, width, height,
                dstColorType,
                srcColorType,
                rowBytes, pixels)) {
            return false;
        }
        if (texture.isMipmapped()) {
            texture.setMipmapsDirty(true);
        }
        mStats.incTextureUploads();
        return true;
    }

    // overridden by backend-specific derived class to perform the surface write
    protected abstract boolean onWritePixels(Texture texture,
                                             int x, int y,
                                             int width, int height,
                                             int dstColorType,
                                             int srcColorType,
                                             int rowBytes,
                                             long pixels);

    /**
     * Uses the base level of the texture to compute the contents of the other mipmap levels.
     *
     * @return success or not
     */
    public final boolean generateMipmaps(Texture texture) {
        assert texture != null;
        assert texture.isMipmapped();
        if (!texture.isMipmapsDirty()) {
            return true;
        }
        if (texture.isReadOnly()) {
            return false;
        }
        if (onGenerateMipmaps(texture)) {
            texture.setMipmapsDirty(false);
            return true;
        }
        return false;
    }

    protected abstract boolean onGenerateMipmaps(Texture texture);

    /**
     * Returns a {@link OpsRenderPass} which {@link OpsTask OpsTasks} record draw commands to.
     *
     * @param writeView       the render target to be rendered to
     * @param contentBounds   the clipped content bounds of the render pass
     * @param colorOps        the color load/store ops
     * @param stencilOps      the stencil load/store ops
     * @param clearColor      the color used to clear the color buffer
     * @param sampledTextures list of all textures to be sampled in the render pass (no refs)
     * @param pipelineFlags   combination of flags of all pipelines to be used in the render pass
     * @return a render pass used to record draw commands, or null if failed
     */
    @Nullable
    public final OpsRenderPass getOpsRenderPass(SurfaceProxyView writeView,
                                                Rect2i contentBounds,
                                                byte colorOps,
                                                byte stencilOps,
                                                float[] clearColor,
                                                Set<TextureProxy> sampledTextures,
                                                int pipelineFlags) {
        mStats.incRenderPasses();
        return onGetOpsRenderPass(writeView, contentBounds,
                colorOps, stencilOps, clearColor,
                sampledTextures, pipelineFlags);
    }

    protected abstract OpsRenderPass onGetOpsRenderPass(SurfaceProxyView writeView,
                                                        Rect2i contentBounds,
                                                        byte colorOps,
                                                        byte stencilOps,
                                                        float[] clearColor,
                                                        Set<TextureProxy> sampledTextures,
                                                        int pipelineFlags);

    /**
     * Resolves MSAA. The resolve rectangle must already be in the native destination space.
     */
    public void resolveRenderTarget(RenderTarget renderTarget,
                                    int resolveLeft, int resolveTop,
                                    int resolveRight, int resolveBottom) {
        assert (renderTarget != null);
        handleDirtyContext();
        onResolveRenderTarget(renderTarget, resolveLeft, resolveTop, resolveRight, resolveBottom);
    }

    // overridden by backend-specific derived class to perform the resolve
    protected abstract void onResolveRenderTarget(RenderTarget renderTarget,
                                                  int resolveLeft, int resolveTop,
                                                  int resolveRight, int resolveBottom);

    @Nullable
    @SharedPtr
    public final Buffer createBuffer(int size, int flags) {
        if (size <= 0) {
            new Throwable("RHICreateBuffer, invalid size: " + size)
                    .printStackTrace(getContext().getErrorWriter());
            return null;
        }
        if ((flags & (BufferUsageFlags.kTransferSrc | BufferUsageFlags.kTransferDst)) != 0 &&
                (flags & BufferUsageFlags.kStatic) != 0) {
            return null;
        }
        handleDirtyContext();
        return onCreateBuffer(size, flags);
    }

    @Nullable
    @SharedPtr
    protected abstract Buffer onCreateBuffer(int size, int flags);

    /**
     * Creates a new fence and inserts it into the graphics queue.
     * Calls {@link #deleteFence(long)} if the fence is no longer used.
     *
     * @return the handle to the fence, or null if failed
     */
    public abstract long insertFence();

    /**
     * Checks a fence on client side to see if signalled. This method returns immediately.
     *
     * @param fence the handle to the fence
     * @return true if signalled, false otherwise
     */
    public abstract boolean checkFence(long fence);

    /**
     * Deletes an existing fence that previously returned by {@link #insertFence()}.
     *
     * @param fence the handle to the fence, cannot be null
     */
    public abstract void deleteFence(long fence);

    public abstract void addFinishedCallback(FlushInfo.FinishedCallback callback);

    public abstract void checkFinishedCallbacks();

    /**
     * Blocks the current thread and waits for GPU to finish outstanding works.
     */
    public abstract void waitForQueue();

    public static final class Stats {

        private int mTextureCreates = 0;
        private int mTextureUploads = 0;
        private int mTransfersToTexture = 0;
        private int mTransfersFromSurface = 0;
        private int mStencilAttachmentCreates = 0;
        private int mMSAAAttachmentCreates = 0;
        private int mNumDraws = 0;
        private int mNumFailedDraws = 0;
        private int mNumSubmitToGpus = 0;
        private int mNumScratchTexturesReused = 0;
        private int mNumScratchMSAAAttachmentsReused = 0;
        private int mRenderPasses = 0;
        private int mNumReorderedDAGsOverBudget = 0;

        public Stats() {
        }

        public void reset() {
            mTextureCreates = 0;
            mTextureUploads = 0;
            mTransfersToTexture = 0;
            mTransfersFromSurface = 0;
            mStencilAttachmentCreates = 0;
            mMSAAAttachmentCreates = 0;
            mNumDraws = 0;
            mNumFailedDraws = 0;
            mNumSubmitToGpus = 0;
            mNumScratchTexturesReused = 0;
            mNumScratchMSAAAttachmentsReused = 0;
            mRenderPasses = 0;
            mNumReorderedDAGsOverBudget = 0;
        }

        public int numTextureCreates() {
            return mTextureCreates;
        }

        public void incTextureCreates() {
            mTextureCreates++;
        }

        public int numTextureUploads() {
            return mTextureUploads;
        }

        public void incTextureUploads() {
            mTextureUploads++;
        }

        public int numTransfersToTexture() {
            return mTransfersToTexture;
        }

        public void incTransfersToTexture() {
            mTransfersToTexture++;
        }

        public int numTransfersFromSurface() {
            return mTransfersFromSurface;
        }

        public void incTransfersFromSurface() {
            mTransfersFromSurface++;
        }

        public int numStencilAttachmentCreates() {
            return mStencilAttachmentCreates;
        }

        public void incStencilAttachmentCreates() {
            mStencilAttachmentCreates++;
        }

        public int msaaAttachmentCreates() {
            return mMSAAAttachmentCreates;
        }

        public void incMSAAAttachmentCreates() {
            mMSAAAttachmentCreates++;
        }

        public int numDraws() {
            return mNumDraws;
        }

        public void incNumDraws() {
            mNumDraws++;
        }

        public int numFailedDraws() {
            return mNumFailedDraws;
        }

        public void incNumFailedDraws() {
            mNumFailedDraws++;
        }

        public int numSubmitToGpus() {
            return mNumSubmitToGpus;
        }

        public void incNumSubmitToGpus() {
            mNumSubmitToGpus++;
        }

        public int numScratchTexturesReused() {
            return mNumScratchTexturesReused;
        }

        public void incNumScratchTexturesReused() {
            mNumScratchTexturesReused++;
        }

        public int numScratchMSAAAttachmentsReused() {
            return mNumScratchMSAAAttachmentsReused;
        }

        public void incNumScratchMSAAAttachmentsReused() {
            mNumScratchMSAAAttachmentsReused++;
        }

        public int numRenderPasses() {
            return mRenderPasses;
        }

        public void incRenderPasses() {
            mRenderPasses++;
        }

        public int numReorderedDAGsOverBudget() {
            return mNumReorderedDAGsOverBudget;
        }

        public void incNumReorderedDAGsOverBudget() {
            mNumReorderedDAGsOverBudget++;
        }

        @Override
        public String toString() {
            return "Stats{" +
                    "mTextureCreates=" + mTextureCreates +
                    ", mTextureUploads=" + mTextureUploads +
                    ", mTransfersToTexture=" + mTransfersToTexture +
                    ", mTransfersFromSurface=" + mTransfersFromSurface +
                    ", mStencilAttachmentCreates=" + mStencilAttachmentCreates +
                    ", mMSAAAttachmentCreates=" + mMSAAAttachmentCreates +
                    ", mNumDraws=" + mNumDraws +
                    ", mNumFailedDraws=" + mNumFailedDraws +
                    ", mNumSubmitToGpus=" + mNumSubmitToGpus +
                    ", mNumScratchTexturesReused=" + mNumScratchTexturesReused +
                    ", mNumScratchMSAAAttachmentsReused=" + mNumScratchMSAAAttachmentsReused +
                    ", mRenderPasses=" + mRenderPasses +
                    ", mNumReorderedDAGsOverBudget=" + mNumReorderedDAGsOverBudget +
                    '}';
        }
    }
}
