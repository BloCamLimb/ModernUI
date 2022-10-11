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

import icyllis.akashigi.core.*;

/**
 * Shared constants, enums and utilities for Akashi Engine.
 */
public final class Engine {

    /**
     * Possible 3D APIs that may be used by Akashi Engine.
     */
    public static final int
            OPENGL = 0, // OpenGL 4.5 core profile (desktop)
            VULKAN = 1, // Vulkan 1.1 (desktop and mobile)
            MOCK = 2;   // Mock draws nothing. It is used for unit tests and to measure CPU overhead.

    /**
     * Used to say whether a texture has mip levels allocated or not.
     */
    public static final boolean
            Mipmapped_No = false,
            Mipmapped_Yes = true;

    /**
     * Image and Surfaces can be stored such that (0, 0) in texture space may correspond to
     * either the upper-left or lower-left content pixel.
     */
    public static final int
            SurfaceOrigin_UpperLeft = 0,
            SurfaceOrigin_LowerLeft = 1;

    /**
     * A Context's cache of backend context state can be partially invalidated.
     * These enums are specific to the GL backend and we'd add a new set for an alternative backend.
     *
     * @see icyllis.akashigi.opengl.GLServer#markContextDirty(int)
     */
    public static final int
            GLBackendState_RenderTarget = 1,
            GLBackendState_PixelStore = 1 << 1,
            GLBackendState_Pipeline = 1 << 2,   // Shader stages, vertex array and input buffers
            GLBackendState_Texture = 1 << 3,    // Also includes samplers bound to texture units
            GLBackendState_Stencil = 1 << 4,
            GLBackendState_Raster = 1 << 5,     // Antialiasing and conservative raster
            GLBackendState_Blend = 1 << 6,
            GLBackendState_View = 1 << 7,       // View state stands for scissor and viewport
            GLBackendState_Misc = 1 << 8;

    /**
     * Indicates the type of pending IO operations that can be recorded for GPU resources.
     */
    public static final int
            IOType_Read = 0,
            IOType_Write = 1,
            IOType_RW = 2;

    /**
     * Describes the intended usage or the purpose of a GPU buffer.
     * This will affect memory allocation, etc.
     * <ul>
     *     <li>Vertex: Vertex buffer and instance buffer. Per-vertex attributes and
     *     per-instance attributes.</li>
     *     <li>Index: Index buffer (element buffer).</li>
     *     <li>Uniform: Uniform block storage.</li>
     *     <li>XferSrcToDst: Transfer buffer, used for uploading data.</li>
     *     <li>XferDstToSrc: Transfer buffer, used for downloading data.</li>
     * </ul>
     */
    public static final int
            BufferType_Vertex = 0,       // vertex buffer
            BufferType_Index = 1,        // index buffer
            BufferType_Uniform = 2,      // uniform buffer
            BufferType_XferSrcToDst = 3, // transfer src only
            BufferType_XferDstToSrc = 4; // transfer dst only

    /**
     * Provides a pattern regarding the frequency at which a data store will be accessed.
     * <ul>
     *     <li>Dynamic: Data store will be respecified randomly by Host and Device.
     *     (Sparse read and writes, uniform buffer, staging buffer, etc.)
     *     For VBO, Data store will be respecified once by Host and used at most a frame.
     *     Per-frame updates, VBO, etc.)</li>
     *     <li>Static: Data store will be specified by Host once and may be respecified
     *     repeatedly by Device. (Fixed index buffer, etc.)</li>
     * </ul>
     */
    public static final int
            AccessPattern_Dynamic = 0,
            AccessPattern_Static = 1;

    /**
     * Shader types. Geometry shader and tessellation shaders are removed.
     */
    public static final int
            Vertex_ShaderType = 0,
            Fragment_ShaderType = 1;

    /**
     * Shader flags. Tessellation shaders are removed.
     */
    public static final int
            Vertex_ShaderFlag = 1,
            Fragment_ShaderFlag = 1 << 1;

    public static int colorTypeBytesPerPixel(int ct) {
        return ImageInfo.bytesPerPixel(ct);
    }

    public static int colorTypeChannelFlags(int ct) {
        return switch (ct) {
            case ImageInfo.COLOR_TYPE_UNKNOWN -> 0;
            case ImageInfo.COLOR_TYPE_ALPHA_8,
                    ImageInfo.COLOR_TYPE_ALPHA_16,
                    ImageInfo.COLOR_TYPE_ALPHA_F16,
                    ImageInfo.COLOR_TYPE_ALPHA_8XXX,
                    ImageInfo.COLOR_TYPE_ALPHA_F32XXX -> Color.COLOR_CHANNEL_FLAG_ALPHA;
            case ImageInfo.COLOR_TYPE_BGR_565,
                    ImageInfo.COLOR_TYPE_RGB_888,
                    ImageInfo.COLOR_TYPE_RGB_888X -> Color.COLOR_CHANNEL_FLAGS_RGB;
            case ImageInfo.COLOR_TYPE_ABGR_4444,
                    ImageInfo.COLOR_TYPE_RGBA_16161616,
                    ImageInfo.COLOR_TYPE_RGBA_F32,
                    ImageInfo.COLOR_TYPE_RGBA_F16_CLAMPED,
                    ImageInfo.COLOR_TYPE_RGBA_F16,
                    ImageInfo.COLOR_TYPE_BGRA_1010102,
                    ImageInfo.COLOR_TYPE_RGBA_1010102,
                    ImageInfo.COLOR_TYPE_BGRA_8888,
                    ImageInfo.COLOR_TYPE_RGBA_8888_SRGB,
                    ImageInfo.COLOR_TYPE_RGBA_8888 -> Color.COLOR_CHANNEL_FLAGS_RGBA;
            case ImageInfo.COLOR_TYPE_RG_88,
                    ImageInfo.COLOR_TYPE_RG_1616,
                    ImageInfo.COLOR_TYPE_RG_F16 -> Color.COLOR_CHANNEL_FLAGS_RG;
            case ImageInfo.COLOR_TYPE_GRAY_8,
                    ImageInfo.COLOR_TYPE_GRAY_8XXX -> Color.COLOR_CHANNEL_FLAG_GRAY;
            case ImageInfo.COLOR_TYPE_R_8,
                    ImageInfo.COLOR_TYPE_R_16,
                    ImageInfo.COLOR_TYPE_R_F16,
                    ImageInfo.COLOR_TYPE_R_8XXX -> Color.COLOR_CHANNEL_FLAG_RED;
            default -> throw new IllegalArgumentException(String.valueOf(ct));
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
            case ImageInfo.COLOR_TYPE_UNKNOWN,
                    ImageInfo.COLOR_TYPE_ALPHA_8,
                    ImageInfo.COLOR_TYPE_BGR_565,
                    ImageInfo.COLOR_TYPE_ABGR_4444,
                    ImageInfo.COLOR_TYPE_RGBA_8888,
                    ImageInfo.COLOR_TYPE_RGB_888X,
                    ImageInfo.COLOR_TYPE_RG_88,
                    ImageInfo.COLOR_TYPE_BGRA_8888,
                    ImageInfo.COLOR_TYPE_RGBA_1010102,
                    ImageInfo.COLOR_TYPE_BGRA_1010102,
                    ImageInfo.COLOR_TYPE_GRAY_8,
                    ImageInfo.COLOR_TYPE_ALPHA_8XXX,
                    ImageInfo.COLOR_TYPE_GRAY_8XXX,
                    ImageInfo.COLOR_TYPE_R_8XXX,
                    ImageInfo.COLOR_TYPE_ALPHA_16,
                    ImageInfo.COLOR_TYPE_RG_1616,
                    ImageInfo.COLOR_TYPE_RGBA_16161616,
                    ImageInfo.COLOR_TYPE_RGB_888,
                    ImageInfo.COLOR_TYPE_R_8,
                    ImageInfo.COLOR_TYPE_R_16 -> COLOR_ENCODING_UNORM;
            case ImageInfo.COLOR_TYPE_RGBA_8888_SRGB -> COLOR_ENCODING_SRGB_UNORM;
            case ImageInfo.COLOR_TYPE_ALPHA_F16,
                    ImageInfo.COLOR_TYPE_RGBA_F16,
                    ImageInfo.COLOR_TYPE_RGBA_F16_CLAMPED,
                    ImageInfo.COLOR_TYPE_RGBA_F32,
                    ImageInfo.COLOR_TYPE_ALPHA_F32XXX,
                    ImageInfo.COLOR_TYPE_RG_F16,
                    ImageInfo.COLOR_TYPE_R_F16 -> COLOR_ENCODING_FLOAT;
            default -> throw new IllegalArgumentException(String.valueOf(ct));
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
            case ImageInfo.COLOR_TYPE_UNKNOWN,
                    ImageInfo.COLOR_TYPE_ALPHA_8,
                    ImageInfo.COLOR_TYPE_BGR_565,
                    ImageInfo.COLOR_TYPE_ABGR_4444,
                    ImageInfo.COLOR_TYPE_RGBA_8888,
                    ImageInfo.COLOR_TYPE_RGBA_8888_SRGB,
                    ImageInfo.COLOR_TYPE_RGB_888X,
                    ImageInfo.COLOR_TYPE_RG_88,
                    ImageInfo.COLOR_TYPE_BGRA_8888,
                    ImageInfo.COLOR_TYPE_RGBA_1010102,
                    ImageInfo.COLOR_TYPE_BGRA_1010102,
                    ImageInfo.COLOR_TYPE_GRAY_8,
                    ImageInfo.COLOR_TYPE_ALPHA_8XXX,
                    ImageInfo.COLOR_TYPE_GRAY_8XXX,
                    ImageInfo.COLOR_TYPE_R_8XXX,
                    ImageInfo.COLOR_TYPE_ALPHA_16,
                    ImageInfo.COLOR_TYPE_RG_1616,
                    ImageInfo.COLOR_TYPE_RGBA_16161616,
                    ImageInfo.COLOR_TYPE_RGB_888,
                    ImageInfo.COLOR_TYPE_R_8,
                    ImageInfo.COLOR_TYPE_R_16 -> CLAMP_TYPE_AUTO;
            case ImageInfo.COLOR_TYPE_RGBA_F16_CLAMPED -> CLAMP_TYPE_MANUAL;
            case ImageInfo.COLOR_TYPE_ALPHA_F16,
                    ImageInfo.COLOR_TYPE_RGBA_F16,
                    ImageInfo.COLOR_TYPE_RGBA_F32,
                    ImageInfo.COLOR_TYPE_ALPHA_F32XXX,
                    ImageInfo.COLOR_TYPE_RG_F16,
                    ImageInfo.COLOR_TYPE_R_F16 -> CLAMP_TYPE_NONE;
            default -> throw new IllegalArgumentException(String.valueOf(ct));
        };
    }

    /**
     * Geometric primitives used for drawing.
     * <p>
     * We can't simply use POINTS or LINES, because both OpenGL and Vulkan can only guarantee
     * the rasterization of one pixel in screen coordinates, may or may not anti-aliased.
     */
    public static final byte
            PRIMITIVE_TYPE_TRIANGLE_LIST = 0,   // separate triangle
            PRIMITIVE_TYPE_TRIANGLE_STRIP = 1,  // connected triangle
            PRIMITIVE_TYPE_POINT_LIST = 2,      // 1 px only
            PRIMITIVE_TYPE_LINE_LIST = 3,       // 1 px wide only
            PRIMITIVE_TYPE_LINE_STRIP = 4;      // 1 px wide only
    public static final byte LAST_PRIMITIVE_TYPE = PRIMITIVE_TYPE_LINE_STRIP;

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
     * Budget types. Used with resources with a large memory allocation, such as Buffers and Textures.
     * <p>
     * BUDGETED: The resource is budgeted and is subject to cleaning up under budget pressure.
     * <p>
     * NOT_BUDGETED: The resource is not budgeted and is cleaned up as soon as it has no refs regardless
     * of whether it has a unique or scratch key.
     * <p>
     * WRAP_CACHEABLE: The resource is not budgeted and is allowed to remain in the cache with no refs
     * if it has a unique key. Scratch keys are ignored.
     */
    public static final byte
            BUDGET_TYPE_BUDGETED = 0,
            BUDGET_TYPE_NOT_BUDGETED = 1,
            BUDGET_TYPE_WRAP_CACHEABLE = 2;

    /**
     * Load ops. Used to specify the load operation to be used when an OpsTask/OpsRenderPass
     * begins execution.
     */
    public static final int
            LOAD_OP_LOAD = 0,
            LOAD_OP_CLEAR = 1,
            LOAD_OP_DISCARD = 2;

    /**
     * Store ops. Used to specify the store operation to be used when an OpsTask/OpsRenderPass
     * ends execution.
     */
    public static final int
            STORE_OP_STORE = 0,
            STORE_OP_DISCARD = 1;

    private static final int LOAD_OP_MASK = 0x3;
    private static final int STORE_OP_SHIFT = 2;

    /**
     * Combination of load ops and store ops.
     * 0-2 bits: LoadOp
     * 2-3 bits: StoreOp
     */
    public static final int
            ACTION_LOAD_STORE = LOAD_OP_LOAD | (STORE_OP_STORE << STORE_OP_SHIFT),
            ACTION_CLEAR_STORE = LOAD_OP_CLEAR | (STORE_OP_STORE << STORE_OP_SHIFT),
            ACTION_DISCARD_STORE = LOAD_OP_DISCARD | (STORE_OP_STORE << STORE_OP_SHIFT),
            ACTION_LOAD_DISCARD = LOAD_OP_LOAD | (STORE_OP_DISCARD << STORE_OP_SHIFT),
            ACTION_CLEAR_DISCARD = LOAD_OP_CLEAR | (STORE_OP_DISCARD << STORE_OP_SHIFT),
            ACTION_DISCARD_DISCARD = LOAD_OP_DISCARD | (STORE_OP_DISCARD << STORE_OP_SHIFT);

    public static int makeAction(int loadOp, int storeOp) {
        return (loadOp & LOAD_OP_MASK) | (storeOp << STORE_OP_SHIFT);
    }

    public static int loadOp(int action) {
        assert ((action & ~0x7) == 0);
        return action & LOAD_OP_MASK;
    }

    public static int storeOp(int action) {
        assert ((action & ~0x7) == 0);
        return action >> STORE_OP_SHIFT;
    }

    /**
     * Specifies if the holder owns the backend, OpenGL or Vulkan, object.
     */
    public static final boolean
            Ownership_Borrowed = false, // Holder does not destroy the backend object.
            Ownership_Owned = true;     // Holder destroys the backend object.

    /**
     * Surface flags shared between the Surface & SurfaceProxy class hierarchies.
     * Don't abuse the combination of flags or result in unexpected behaviors.
     *
     * <ul>
     * <li>{@link #SURFACE_FLAG_BUDGETED} -
     *  Indicates whether an allocation should count against a cache budget. Budgeted when
     *  set, otherwise not budgeted. {@link Texture} and {@link TextureProxy} only.
     * </li>
     *
     * <li>{@link #SURFACE_FLAG_LOOSE_FIT} -
     *  Indicates whether a backing store needs to be an exact match or can be larger than
     *  is strictly necessary. Loose fit when set, otherwise exact fit.
     * </li>
     *
     * <li>{@link #SURFACE_FLAG_MIPMAPPED} -
     *  Used to say whether a texture has mip levels allocated or not. Mipmaps are allocated
     *  when set, otherwise mipmaps are not allocated. {@link Texture} and {@link TextureProxy} only.
     * </li>
     *
     * <li>{@link #SURFACE_FLAG_RENDERABLE} -
     *  Used to say whether a surface can be rendered to, whether a texture can be used as
     *  color attachments. Renderable when set, otherwise not renderable.
     * </li>
     *
     * <li>{@link #SURFACE_FLAG_PROTECTED} -
     *  Used to say whether texture is backed by protected memory. Protected when set, otherwise
     *  not protected.
     * </li>
     *
     * <li>{@link #SURFACE_FLAG_READ_ONLY} -
     *  Means the pixels in the texture are read-only. {@link Texture} and {@link TextureProxy}
     *  only.
     * </li>
     *
     * <li>{@link #SURFACE_FLAG_SKIP_ALLOCATOR} -
     *  When set, the proxy will be instantiated outside the allocator (e.g. for proxies that are
     *  instantiated in on-flush callbacks). Otherwise, {@link ResourceAllocator} should instantiate
     *  the proxy. {@link SurfaceProxy} only.
     * </li>
     *
     * <li>{@link #SURFACE_FLAG_DEFERRED_PROVIDER} -
     *  For TextureProxies created in a deferred list recording thread it is possible for the
     *  unique key to be cleared on the backing {@link Texture} while the unique key remains on
     *  the proxy. When set, it loosens up asserts that the key of an instantiated uniquely-keyed
     *  texture proxy is also always set on the backing {@link Texture}. {@link TextureProxy} only.
     * </li>
     *
     * <li>{@link #SURFACE_FLAG_GL_WRAP_DEFAULT_FB} -
     *  This is a OpenGL only flag. It tells us that the internal render target wraps the OpenGL
     *  default framebuffer (id=0) that preserved by window. {@link RenderTarget} only.
     * </li>
     *
     * <li>{@link #SURFACE_FLAG_MANUAL_MSAA_RESOLVE} -
     *  This means the render target is multi-sampled, and internally holds a non-msaa texture
     *  for resolving into. The render target resolves itself by blit-ting into this internal
     *  texture. (It might or might not has the internal texture access, but if it does, we
     *  always resolve the render target before accessing this texture's data.) {@link RenderTarget}
     *  only.
     * </li>
     *
     * <li>{@link #SURFACE_FLAG_VK_WRAP_SECONDARY_CB} -
     *  This is a Vulkan only flag. It tells us that the internal render target is wrapping a raw
     *  Vulkan secondary command buffer. {@link RenderTarget} only.
     * </li>
     * </ul>
     */
    public static final int
            SURFACE_FLAG_NONE = Core.SURFACE_FLAG_NONE,
            SURFACE_FLAG_BUDGETED = Core.SURFACE_FLAG_BUDGETED,
            SURFACE_FLAG_LOOSE_FIT = Core.SURFACE_FLAG_LOOSE_FIT,
            SURFACE_FLAG_MIPMAPPED = Core.SURFACE_FLAG_MIPMAPPED,
            SURFACE_FLAG_RENDERABLE = Core.SURFACE_FLAG_RENDERABLE,
            SURFACE_FLAG_PROTECTED = Core.SURFACE_FLAG_PROTECTED,
            SURFACE_FLAG_READ_ONLY = Core.SURFACE_FLAG_PROTECTED << 1,
            SURFACE_FLAG_SKIP_ALLOCATOR = Core.SURFACE_FLAG_PROTECTED << 2,
            SURFACE_FLAG_DEFERRED_PROVIDER = Core.SURFACE_FLAG_PROTECTED << 3,
            SURFACE_FLAG_GL_WRAP_DEFAULT_FB = Core.SURFACE_FLAG_PROTECTED << 4,
            SURFACE_FLAG_MANUAL_MSAA_RESOLVE = Core.SURFACE_FLAG_PROTECTED << 5,
            SURFACE_FLAG_VK_WRAP_SECONDARY_CB = Core.SURFACE_FLAG_PROTECTED << 6;

    /**
     * Types used to describe format of vertices in arrays.
     */
    public static final byte
            Float_VertexAttribType = 0,
            Float2_VertexAttribType = 1,
            Float3_VertexAttribType = 2,
            Float4_VertexAttribType = 3,
            Half_VertexAttribType = 4,
            Half2_VertexAttribType = 5,
            Half4_VertexAttribType = 6;
    public static final byte
            Int2_VertexAttribType = 7,   // vector of 2 32-bit ints
            Int3_VertexAttribType = 8,   // vector of 3 32-bit ints
            Int4_VertexAttribType = 9;   // vector of 4 32-bit ints
    public static final byte
            Byte_VertexAttribType = 10,   // signed byte
            Byte2_VertexAttribType = 11,  // vector of 2 8-bit signed bytes
            Byte4_VertexAttribType = 12,  // vector of 4 8-bit signed bytes
            UByte_VertexAttribType = 13,  // unsigned byte
            UByte2_VertexAttribType = 14, // vector of 2 8-bit unsigned bytes
            UByte4_VertexAttribType = 15; // vector of 4 8-bit unsigned bytes
    public static final byte
            UByte_norm_VertexAttribType = 16,  // unsigned byte, e.g. coverage, 0 -> 0.0f, 255 -> 1.0f.
            UByte4_norm_VertexAttribType = 17; // vector of 4 unsigned bytes, e.g. colors, 0 -> 0.0f, 255 -> 1.0f.
    public static final byte
            Short2_VertexAttribType = 18,       // vector of 2 16-bit shorts.
            Short4_VertexAttribType = 19;       // vector of 4 16-bit shorts.
    public static final byte
            UShort2_VertexAttribType = 20,      // vector of 2 unsigned shorts. 0 -> 0, 65535 -> 65535.
            UShort2_norm_VertexAttribType = 21; // vector of 2 unsigned shorts. 0 -> 0.0f, 65535 -> 1.0f.
    public static final byte
            Int_VertexAttribType = 22,
            UInt_VertexAttribType = 23;
    public static final byte
            UShort_norm_VertexAttribType = 24;
    public static final byte
            UShort4_norm_VertexAttribType = 25; // vector of 4 unsigned shorts. 0 -> 0.0f, 65535 -> 1.0f.
    public static final byte Last_VertexAttribType = UShort4_norm_VertexAttribType;

    /**
     * @return size in bytes
     */
    public static int vertexAttribTypeSize(byte type) {
        switch (type) {
            case Float_VertexAttribType:
                return Float.BYTES;
            case Float2_VertexAttribType:
                return 2 * Float.BYTES;
            case Float3_VertexAttribType:
                return 3 * Float.BYTES;
            case Float4_VertexAttribType:
                return 4 * Float.BYTES;
            case Half_VertexAttribType:
            case UShort_norm_VertexAttribType:
                return Short.BYTES;
            case Half2_VertexAttribType:
            case Short2_VertexAttribType:
            case UShort2_VertexAttribType:
            case UShort2_norm_VertexAttribType:
                return 2 * Short.BYTES;
            case Half4_VertexAttribType:
            case Short4_VertexAttribType:
            case UShort4_norm_VertexAttribType:
                return 4 * Short.BYTES;
            case Int2_VertexAttribType:
                return 2 * Integer.BYTES;
            case Int3_VertexAttribType:
                return 3 * Integer.BYTES;
            case Int4_VertexAttribType:
                return 4 * Integer.BYTES;
            case Byte_VertexAttribType:
            case UByte_VertexAttribType:
            case UByte_norm_VertexAttribType:
                return Byte.BYTES;
            case Byte2_VertexAttribType:
            case UByte2_VertexAttribType:
                return 2 * Byte.BYTES;
            case Byte4_VertexAttribType:
            case UByte4_VertexAttribType:
            case UByte4_norm_VertexAttribType:
                return 4 * Byte.BYTES;
            case Int_VertexAttribType:
            case UInt_VertexAttribType:
                return Integer.BYTES;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * ResourceHandle is an opaque handle to a resource. It's actually a table index.
     */
    public static final int INVALID_RESOURCE_HANDLE = -1;

    private Engine() {
    }
}
