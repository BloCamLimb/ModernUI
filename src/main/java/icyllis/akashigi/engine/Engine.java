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

import icyllis.akashigi.core.Color;
import icyllis.akashigi.core.Core;

/**
 * Shared constants, enums and utilities for Engine.
 */
public final class Engine {

    /**
     * Possible 3D APIs that may be used by Engine.
     */
    public static final class BackendApi {

        /**
         * OpenGL 4.5 core profile (desktop)
         */
        public static final int kOpenGL = 0;
        /**
         * Vulkan 1.1 (desktop and mobile)
         */
        public static final int kVulkan = 1;
        /**
         * Mock draws nothing. It is used for unit tests and to measure CPU overhead.
         */
        public static final int kMock = 2;

        private BackendApi() {
        }
    }

    /**
     * Image and Surfaces can be stored such that (0, 0) in texture space may correspond to
     * either the upper-left or lower-left content pixel.
     */
    public static final class SurfaceOrigin {

        public static final int kUpperLeft = 0; // top left, Vulkan
        public static final int kLowerLeft = 1; // bottom left, OpenGL

        private SurfaceOrigin() {
        }
    }

    /**
     * A Context's cache of backend context state can be partially invalidated.
     * These enums are specific to the GL backend and we'd add a new set for an alternative backend.
     *
     * @see icyllis.akashigi.opengl.GLServer#markContextDirty(int)
     */
    public static final class GLBackendState {

        public static final int kRenderTarget = 1;
        public static final int kPixelStore = 1 << 1;
        /**
         * Shader stages, vertex array and input buffers
         */
        public static final int kPipeline = 1 << 2;
        /**
         * Also includes samplers bound to texture units
         */
        public static final int kTexture = 1 << 3;
        public static final int kStencil = 1 << 4;
        /**
         * Antialiasing and conservative raster
         */
        public static final int kRaster = 1 << 5;
        public static final int kBlend = 1 << 6;
        /**
         * View state stands for scissor and viewport
         */
        public static final int kView = 1 << 7;
        public static final int kMisc = 1 << 8;

        private GLBackendState() {
        }
    }

    /**
     * Indicates the type of pending IO operations that can be recorded for GPU resources.
     */
    public static final class IOType {

        public static final int kRead = 0;
        public static final int kWrite = 1;
        public static final int kRW = 2;

        private IOType() {
        }
    }

    /**
     * Describes the intended usage a GPU buffer.
     */
    public static final class BufferUsageFlags {

        /**
         * Vertex buffer (also includes instance buffer).
         */
        public static final int kVertex = 1;
        /**
         * Index buffer, also known as element buffer.
         */
        public static final int kIndex = 1 << 1;
        /**
         * Indirect buffer, also known as argument buffer.
         */
        public static final int kDrawIndirect = 1 << 2;

        public static final int kTransferSrc = 1 << 3; // transfer src only
        public static final int kTransferDst = 1 << 4; // transfer dst only

        /**
         * For VBO, Data store will be respecified once by Host and used at most a frame.
         * (Per-frame updates, VBO, IBO, etc.)
         */
        public static final int kStream = 1 << 5;
        /**
         * Data store will be specified by Host once and may be respecified
         * repeatedly by Device. (Fixed index buffer, etc.)
         */
        public static final int kStatic = 1 << 6;
        /**
         * Data store will be respecified randomly by Host and Device.
         * (Uniform buffer, staging buffer, etc.)
         */
        public static final int kDynamic = 1 << 7;

        public static final int kUniform = 1 << 8; // TODO remove, UBO is special buffers

        private BufferUsageFlags() {
        }
    }

    /**
     * Shader flags.
     */
    public static final class ShaderFlags {

        public static final int kVertex = 1;
        public static final int kFragment = 1 << 1;

        private ShaderFlags() {
        }
    }

    public static final class ColorType extends Core.ColorType {

        /**
         * Engine values.
         * <p>
         * Unusual types that come up after reading back in cases where we are reassigning the meaning
         * of a texture format's channels to use for a particular color format but have to read back the
         * data to a full RGBA quadruple. (e.g. using a R8 texture format as A8 color type but the API
         * only supports reading to RGBA8.)
         */
        public static final int
                kAlpha_8xxx = 21,
                kAlpha_F32xxx = 22,
                kGray_8xxx = 23,
                kR_8xxx = 24;
        /**
         * Engine values.
         * <p>
         * Types used to initialize backend textures.
         */
        public static final int
                kRGB_888 = 25,
                kR_16 = 26,
                kR_F16 = 27;
        public static final int kLast = kR_F16;

        private ColorType() {
        }

        /**
         * @return bpp
         */
        public static int bytesPerPixel(int colorType) {
            return switch (colorType) {
                case kUnknown -> 0;
                case kAlpha_8,
                        kR_8,
                        kGray_8 -> 1;
                case kBGR_565,
                        kABGR_4444,
                        kR_F16,
                        kR_16,
                        kAlpha_16,
                        kAlpha_F16,
                        kRG_88 -> 2;
                case kRGB_888 -> 3;
                case kRGBA_8888,
                        kRG_F16,
                        kRG_1616,
                        kR_8xxx,
                        kGray_8xxx,
                        kAlpha_8xxx,
                        kBGRA_1010102,
                        kRGBA_1010102,
                        kBGRA_8888,
                        kRGB_888x,
                        kRGBA_8888_SRGB -> 4;
                case kRGBA_F16,
                        kRGBA_16161616,
                        kRGBA_F16_Clamped -> 8;
                case kRGBA_F32,
                        kAlpha_F32xxx -> 16;
                default -> throw new AssertionError(colorType);
            };
        }

        public static int channelFlags(int colorType) {
            return switch (colorType) {
                case kUnknown -> 0;
                case kAlpha_8,
                        kAlpha_16,
                        kAlpha_F16,
                        kAlpha_8xxx,
                        kAlpha_F32xxx -> Color.COLOR_CHANNEL_FLAG_ALPHA;
                case kBGR_565,
                        kRGB_888,
                        kRGB_888x -> Color.COLOR_CHANNEL_FLAGS_RGB;
                case kABGR_4444,
                        kRGBA_16161616,
                        kRGBA_F32,
                        kRGBA_F16_Clamped,
                        kRGBA_F16,
                        kBGRA_1010102,
                        kRGBA_1010102,
                        kBGRA_8888,
                        kRGBA_8888_SRGB,
                        kRGBA_8888 -> Color.COLOR_CHANNEL_FLAGS_RGBA;
                case kRG_88,
                        kRG_1616,
                        kRG_F16 -> Color.COLOR_CHANNEL_FLAGS_RG;
                case kGray_8,
                        kGray_8xxx -> Color.COLOR_CHANNEL_FLAG_GRAY;
                case kR_8,
                        kR_16,
                        kR_F16,
                        kR_8xxx -> Color.COLOR_CHANNEL_FLAG_RED;
                default -> throw new AssertionError(colorType);
            };
        }

        /**
         * Block engine-private values.
         */
        public static int toPublic(int colorType) {
            return switch (colorType) {
                case kUnknown,
                        kAlpha_8,
                        kBGR_565,
                        kABGR_4444,
                        kRGBA_8888,
                        kRGBA_8888_SRGB,
                        kRGB_888x,
                        kRG_88,
                        kBGRA_8888,
                        kRGBA_1010102,
                        kBGRA_1010102,
                        kGray_8,
                        kAlpha_F16,
                        kRGBA_F16,
                        kRGBA_F16_Clamped,
                        kRGBA_F32,
                        kAlpha_16,
                        kRG_1616,
                        kRG_F16,
                        kRGBA_16161616,
                        kR_8 -> colorType;
                case kAlpha_8xxx,
                        kAlpha_F32xxx,
                        kGray_8xxx,
                        kR_8xxx,
                        kRGB_888,
                        kR_16,
                        kR_F16 -> kUnknown;
                default -> throw new AssertionError(colorType);
            };
        }
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
            case ColorType.kUnknown,
                    ColorType.kAlpha_8,
                    ColorType.kBGR_565,
                    ColorType.kABGR_4444,
                    ColorType.kRGBA_8888,
                    ColorType.kRGB_888x,
                    ColorType.kRG_88,
                    ColorType.kBGRA_8888,
                    ColorType.kRGBA_1010102,
                    ColorType.kBGRA_1010102,
                    ColorType.kGray_8,
                    ColorType.kAlpha_8xxx,
                    ColorType.kGray_8xxx,
                    ColorType.kR_8xxx,
                    ColorType.kAlpha_16,
                    ColorType.kRG_1616,
                    ColorType.kRGBA_16161616,
                    ColorType.kRGB_888,
                    ColorType.kR_8,
                    ColorType.kR_16 -> COLOR_ENCODING_UNORM;
            case ColorType.kRGBA_8888_SRGB -> COLOR_ENCODING_SRGB_UNORM;
            case ColorType.kAlpha_F16,
                    ColorType.kRGBA_F16,
                    ColorType.kRGBA_F16_Clamped,
                    ColorType.kRGBA_F32,
                    ColorType.kAlpha_F32xxx,
                    ColorType.kRG_F16,
                    ColorType.kR_F16 -> COLOR_ENCODING_FLOAT;
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
            case ColorType.kUnknown,
                    ColorType.kAlpha_8,
                    ColorType.kBGR_565,
                    ColorType.kABGR_4444,
                    ColorType.kRGBA_8888,
                    ColorType.kRGBA_8888_SRGB,
                    ColorType.kRGB_888x,
                    ColorType.kRG_88,
                    ColorType.kBGRA_8888,
                    ColorType.kRGBA_1010102,
                    ColorType.kBGRA_1010102,
                    ColorType.kGray_8,
                    ColorType.kAlpha_8xxx,
                    ColorType.kGray_8xxx,
                    ColorType.kR_8xxx,
                    ColorType.kAlpha_16,
                    ColorType.kRG_1616,
                    ColorType.kRGBA_16161616,
                    ColorType.kRGB_888,
                    ColorType.kR_8,
                    ColorType.kR_16 -> CLAMP_TYPE_AUTO;
            case ColorType.kRGBA_F16_Clamped -> CLAMP_TYPE_MANUAL;
            case ColorType.kAlpha_F16,
                    ColorType.kRGBA_F16,
                    ColorType.kRGBA_F32,
                    ColorType.kAlpha_F32xxx,
                    ColorType.kRG_F16,
                    ColorType.kR_F16 -> CLAMP_TYPE_NONE;
            default -> throw new AssertionError(ct);
        };
    }

    /**
     * Geometric primitives used for drawing.
     * <p>
     * We can't simply use POINTS or LINES, because both OpenGL and Vulkan can only guarantee
     * the rasterization of one pixel in screen coordinates, may or may not anti-aliased.
     */
    public static final class PrimitiveType {

        public static final byte kTriangleList = 0;     // separate triangle
        public static final byte kTriangleStrip = 1;    // connected triangle
        public static final byte kPointList = 2;        // 1 px only
        public static final byte kLineList = 3;         // 1 px wide only
        public static final byte kLineStrip = 4;        // 1 px wide only

        private PrimitiveType() {
        }
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
     * Budget types. Used with resources with a large memory allocation.
     *
     * @see Resource
     */
    public static final class BudgetType {

        /**
         * The resource is budgeted and is subject to cleaning up under budget pressure.
         */
        public static final byte kBudgeted = 0;
        /**
         * The resource is not budgeted and is cleaned up as soon as it has no refs regardless
         * of whether it has a unique or scratch key.
         */
        public static final byte kNotBudgeted = 1;
        /**
         * The resource is not budgeted and is allowed to remain in the cache with no refs
         * if it has a unique key. Scratch keys are ignored.
         */
        public static final byte kWrapCacheable = 2;

        private BudgetType() {
        }
    }

    /**
     * Load ops. Used to specify the load operation to be used when an OpsTask/OpsRenderPass
     * begins execution.
     */
    public static final class LoadOp {

        public static final byte kLoad = 0;
        public static final byte kClear = 1;
        public static final byte kDontCare = 2;

        private LoadOp() {
        }
    }

    /**
     * Store ops. Used to specify the store operation to be used when an OpsTask/OpsRenderPass
     * ends execution.
     */
    public static final class StoreOp {

        public static final byte kStore = 0;
        public static final byte kDontCare = 1;

        private StoreOp() {
        }
    }

    /**
     * Combination of load ops and store ops.
     */
    public static final class LoadStoreOps {

        // ensure LoadOp.kLast < (1 << kLoadOpShift)
        private static final int kLoadOpShift = 2;

        /**
         * Combination of load ops and store ops.
         * 0-2 bits: LoadOp
         * 2-3 bits: StoreOp
         */
        public static final byte
                kLoad_Store = (LoadOp.kLoad << kLoadOpShift) | StoreOp.kStore,
                kClear_Store = (LoadOp.kClear << kLoadOpShift) | StoreOp.kStore,
                kDontLoad_Store = (LoadOp.kDontCare << kLoadOpShift) | StoreOp.kStore,
                kLoad_DontStore = (LoadOp.kLoad << kLoadOpShift) | StoreOp.kDontCare,
                kClear_DontStore = (LoadOp.kClear << kLoadOpShift) | StoreOp.kDontCare,
                kDontLoad_DontStore = (LoadOp.kDontCare << kLoadOpShift) | StoreOp.kDontCare;

        private LoadStoreOps() {
        }

        public static byte make(byte load, byte store) {
            return (byte) ((load << kLoadOpShift) | store);
        }

        public static byte loadOp(byte ops) {
            return (byte) (ops >>> kLoadOpShift);
        }

        public static byte storeOp(byte ops) {
            return (byte) (ops & ((1 << kLoadOpShift) - 1));
        }
    }

    /**
     * Specifies if the holder owns the backend, OpenGL or Vulkan, object.
     */
    public static final boolean
            Ownership_Borrowed = false, // Holder does not destroy the backend object.
            Ownership_Owned = true;     // Holder destroys the backend object.

    /**
     * Surface flags shared between the Surface & SurfaceProxy class hierarchies.
     * An arbitrary combination of flags may result in unexpected behaviors.
     */
    public static final class SurfaceFlags extends Core.SurfaceFlags {

        /**
         * Means the pixels in the texture are read-only. {@link Texture} and {@link TextureProxy}
         * only.
         */
        public static final int kReadOnly = kProtected << 1;
        /**
         * When set, the proxy will be instantiated outside the allocator (e.g. for proxies that are
         * instantiated in on-flush callbacks). Otherwise, {@link ResourceAllocator} should instantiate
         * the proxy. {@link SurfaceProxy} only.
         */
        public static final int kSkipAllocator = kProtected << 2;
        /**
         * For TextureProxies created in a deferred list recording thread it is possible for the
         * unique key to be cleared on the backing {@link Texture} while the unique key remains on
         * the proxy. When set, it loosens up asserts that the key of an instantiated uniquely-keyed
         * texture proxy is also always set on the backing {@link Texture}. {@link TextureProxy} only.
         */
        public static final int kDeferredProvider = kProtected << 3;
        /**
         * This is a OpenGL only flag. It tells us that the internal render target wraps the OpenGL
         * default framebuffer (id=0) that preserved by window. {@link RenderTarget} only.
         */
        public static final int kGLWrapDefaultFB = kProtected << 4;
        /**
         * This means the render target is multi-sampled, and internally holds a non-msaa texture
         * for resolving into. The render target resolves itself by blit-ting into this internal
         * texture. (It might or might not have the internal texture access, but if it does, we
         * always resolve the render target before accessing this texture's data.) {@link RenderTarget}
         * only.
         */
        public static final int kManualMSAAResolve = kProtected << 5;
        /**
         * This is a Vulkan only flag. It tells us that the internal render target is wrapping a raw
         * Vulkan secondary command buffer. {@link RenderTarget} only.
         */
        public static final int kVkWrapSecondaryCB = kProtected << 6;

        private SurfaceFlags() {
        }
    }

    /**
     * Types used to describe format of vertices in arrays.
     */
    public static final class VertexAttribType {

        public static final byte
                kFloat = 0,
                kFloat2 = 1,
                kFloat3 = 2,
                kFloat4 = 3,
                kHalf = 4,
                kHalf2 = 5,
                kHalf4 = 6;
        public static final byte
                kInt2 = 7,   // vector of 2 32-bit ints
                kInt3 = 8,   // vector of 3 32-bit ints
                kInt4 = 9;   // vector of 4 32-bit ints
        public static final byte
                kByte = 10,   // signed byte
                kByte2 = 11,  // vector of 2 8-bit signed bytes
                kByte4 = 12,  // vector of 4 8-bit signed bytes
                kUByte = 13,  // unsigned byte
                kUByte2 = 14, // vector of 2 8-bit unsigned bytes
                kUByte4 = 15; // vector of 4 8-bit unsigned bytes
        public static final byte
                kUByte_norm = 16,  // unsigned byte, e.g. coverage, 0 -> 0.0f, 255 -> 1.0f.
                kUByte4_norm = 17; // vector of 4 unsigned bytes, e.g. colors, 0 -> 0.0f, 255 -> 1.0f.
        public static final byte
                kShort2 = 18,       // vector of 2 16-bit shorts.
                kShort4 = 19;       // vector of 4 16-bit shorts.
        public static final byte
                kUShort2 = 20,      // vector of 2 unsigned shorts. 0 -> 0, 65535 -> 65535.
                kUShort2_norm = 21; // vector of 2 unsigned shorts. 0 -> 0.0f, 65535 -> 1.0f.
        public static final byte
                kInt = 22,
                kUInt = 23;
        public static final byte
                kUShort_norm = 24;
        public static final byte
                kUShort4_norm = 25; // vector of 4 unsigned shorts. 0 -> 0.0f, 65535 -> 1.0f.
        public static final byte kLast = kUShort4_norm;

        private VertexAttribType() {
        }

        /**
         * @return size in bytes
         */
        public static int size(byte type) {
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
