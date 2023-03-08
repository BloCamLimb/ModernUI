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

package icyllis.modernui.graphics.engine;

import icyllis.modernui.graphics.Color;
import icyllis.modernui.graphics.ImageInfo;

/**
 * Shared constants, enums and utilities for Akashi Engine.
 */
public final class Engine {

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
     * Possible 3D APIs that may be used by Akashi Engine.
     */
    public interface BackendApi {

        /**
         * OpenGL 4.5 core profile (desktop)
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
     * @see icyllis.modernui.graphics.opengl.GLServer#markContextDirty(int)
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

    private Engine() {
    }
}
