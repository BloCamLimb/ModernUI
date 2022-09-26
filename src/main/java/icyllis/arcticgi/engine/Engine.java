/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.engine;

import icyllis.arcticgi.core.*;

/**
 * Shared constants, enums and utilities for Arctic Engine.
 */
public final class Engine {

    /**
     * Possible 3D APIs that may be used by Arctic.
     */
    public static final int
            OpenGL = 0, // OpenGL 4.5 core profile (desktop)
            Vulkan = 1, // Vulkan 1.1 (desktop and mobile)
            Mock = 2;   // Mock draws nothing. It is used for unit tests and to measure CPU overhead.

    /**
     * Used to say whether a texture has mip levels allocated or not.
     */
    public static final boolean
            Mipmapped_No = false,
            Mipmapped_Yes = true;

    /**
     * Image and Surfaces can be stored such that (0, 0) in texture space may correspond to
     * either the top-left or bottom-left content pixel.
     */
    public static final int
            SurfaceOrigin_UpperLeft = 0,
            SurfaceOrigin_LowerLeft = 1;

    /**
     * A Context's cache of backend context state can be partially invalidated.
     * These enums are specific to the GL backend and we'd add a new set for an alternative backend.
     *
     * @see icyllis.arcticgi.opengl.GLServer#markContextDirty(int)
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
            GpuBufferType_Vertex = 0,       // vertex buffer
            GpuBufferType_Index = 1,        // element buffer or index buffer
            GpuBufferType_Uniform = 2,      // uniform buffer
            GpuBufferType_XferSrcToDst = 3, // transfer src only
            GpuBufferType_XferDstToSrc = 4; // transfer dst only
    public static final int GpuBufferType_Last = GpuBufferType_XferDstToSrc;

    // Debug tool.
    public static boolean checkGpuBufferType(int bufferType) {
        return bufferType >= 0 && bufferType <= GpuBufferType_Last;
    }

    /**
     * Provides a pattern regarding the frequency at which a data store will be accessed.
     * <ul>
     *     <li>Dynamic: Data store will be respecified randomly by Host and Device.
     *     (Sparse read and writes, uniform buffer, staging buffer, etc.)</li>
     *     <li>Static: Data store will be specified by Host once and may be respecified
     *     repeatedly by Device. (Fixed index buffer, etc.)</li>
     *     <li>Stream: Data store will be respecified once by Host and used at most a frame.
     *     (Per-frame updates, VBO, etc.)</li>
     * </ul>
     */
    public static final int
            AccessPattern_Dynamic = 0,
            AccessPattern_Static = 1,
            AccessPattern_Stream = 2;
    public static final int AccessPattern_Last = AccessPattern_Stream;

    // Debug tool.
    public static boolean checkAccessPattern(int accessPattern) {
        return accessPattern >= 0 && accessPattern <= AccessPattern_Last;
    }

    /**
     * The type of texture. There are only 2D.
     * <p>
     * NONE: Represents a general purpose that is not considered a texture, e.g. OpenGL renderbuffer.
     * <p>
     * 2D: OpenGL 2D texture or Vulkan 2D image.
     * <p>
     * EXTERNAL: May be memory object, imports POSIX FD or Win32 NT handle (Windows 8+, KMT is not used).
     * Currently, OpenGL texture wraps Vulkan image, Vulkan image wraps Vulkan image or Linux DRM.
     * We assume external textures are read-only and don't track their memory usage.
     */
    public static final int
            TextureType_None = 0,
            TextureType_2D = 1,
            TextureType_External = 2;
    public static final int TextureType_Last = TextureType_External;

    /**
     * Rectangle and external textures only support the clamp wrap mode and do not support
     * MIP maps.
     */
    public static boolean textureTypeHasRestrictedSampling(int type) {
        return switch (type) {
            case TextureType_2D -> false;
            case TextureType_External -> true;
            default -> throw new IllegalArgumentException(String.valueOf(type));
        };
    }

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

    /**
     * Describes the encoding of channel data in a ColorType.
     */
    public static final int
            COLOR_ENCODING_UNORM = 0,
            COLOR_ENCODING_SRGB_UNORM = 1,
            COLOR_ENCODING_SNORM = 2,
            COLOR_ENCODING_FLOAT = 3;

    public static int colorTypeBytesPerPixel(int ct) {
        return ImageInfo.bytesPerPixel(ct);
    }

    public static int colorTypeChannelFlags(int ct) {
        return switch (ct) {
            case ImageInfo.ColorType_Unknown -> 0;
            case ImageInfo.ColorType_Alpha_8,
                    ImageInfo.ColorType_Alpha_16,
                    ImageInfo.ColorType_Alpha_F32xxx,
                    ImageInfo.ColorType_Alpha_8xxx,
                    ImageInfo.ColorType_Alpha_F16 -> Color.ALPHA_CHANNEL_FLAG;
            case ImageInfo.ColorType_BGR_565,
                    ImageInfo.ColorType_RGB_888,
                    ImageInfo.ColorType_RGB_888x -> Color.RGB_CHANNEL_FLAGS;
            case ImageInfo.ColorType_ABGR_4444,
                    ImageInfo.ColorType_RGBA_16161616,
                    ImageInfo.ColorType_RGBA_F32,
                    ImageInfo.ColorType_RGBA_F16_Clamped,
                    ImageInfo.ColorType_RGBA_F16,
                    ImageInfo.ColorType_BGRA_1010102,
                    ImageInfo.ColorType_RGBA_1010102,
                    ImageInfo.ColorType_BGRA_8888,
                    ImageInfo.ColorType_RGBA_8888_SRGB,
                    ImageInfo.ColorType_RGBA_8888 -> Color.RGBA_CHANNEL_FLAGS;
            case ImageInfo.ColorType_RG_88,
                    ImageInfo.ColorType_RG_F16,
                    ImageInfo.ColorType_RG_1616 -> Color.RG_CHANNEL_FLAGS;
            case ImageInfo.ColorType_Gray_8,
                    ImageInfo.ColorType_Gray_8xxx -> Color.GRAY_CHANNEL_FLAG;
            case ImageInfo.ColorType_R_8,
                    ImageInfo.ColorType_R_F16,
                    ImageInfo.ColorType_R_16 -> Color.RED_CHANNEL_FLAG;
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
            PrimitiveType_Triangles = 0,        // separate triangle
            PrimitiveType_TriangleStrip = 1,    // connected triangle
            PrimitiveType_Points = 2,           // 1 px only
            PrimitiveType_Lines = 3,            // 1 px wide only
            PrimitiveType_LineStrip = 4;        // 1 px wide only
    public static final byte PrimitiveType_Last = PrimitiveType_LineStrip;

    /**
     * Mask formats. Used by the font cache. Important that these are 0-based.
     * <p>
     * Using L-shift to get the number of bytes-per-pixel for the specified mask format.
     */
    public static final int
            MaskFormat_A8 = 0,     // 1-byte per pixel
            MaskFormat_A565 = 1,   // 2-bytes per pixel, RGB represent 3-channel LCD coverage
            MaskFormat_ARGB = 2;   // 4-bytes per pixel, color format

    /**
     * Budget types. Used with resources with a large memory allocation, such as Buffers and Textures.
     * <p>
     * BUDGETED: The resource is budgeted and is subject to cleaning up under budget pressure.
     * <p>
     * NONE: The resource is not budgeted and is cleaned up as soon as it has no refs regardless of whether
     * it has a unique or scratch key.
     * <p>
     * CACHEABLE: The resource is not budgeted and is allowed to remain in the cache with no refs if it
     * has a unique key. Scratch keys are ignored.
     */
    public static final byte
            BudgetType_Budgeted = 0,
            BudgetType_None = 1,
            BudgetType_Cacheable = 2;

    /**
     * Load ops. Used to specify the load operation to be used when an OpsTask/OpsRenderPass
     * begins execution.
     */
    public static final int
            LoadOp_Load = 0,
            LoadOp_Clear = 1,
            LoadOp_Discard = 2;
    public static final int LoadOp_Last = LoadOp_Discard;

    /**
     * Store ops. Used to specify the store operation to be used when an OpsTask/OpsRenderPass
     * ends execution.
     */
    public static final int
            StoreOp_Store = 0,
            StoreOp_Discard = 1;

    /**
     * Surface flags shared between the Surface & SurfaceProxy class hierarchies.
     * <b>WARNING: Don't abuse the combination of flags or result in unexpected behaviors.</b>
     *
     * <ul>
     * <li>{@link #SurfaceFlag_Budgeted} -
     *  Indicates whether an allocation should count against a cache budget. Budgeted when
     *  set, otherwise not budgeted. {@link Texture} and {@link TextureProxy} only.
     * </li>
     *
     * <li>{@link #SurfaceFlag_BackingFit} -
     *  Indicates whether a backing store needs to be an exact match or can be larger than
     *  is strictly necessary. Exact when set, otherwise approx. {@link SurfaceProxy} only.
     * </li>
     *
     * <li>{@link #SurfaceFlag_Mipmapped} -
     *  Used to say whether a texture has mip levels allocated or not. Mipmaps are allocated
     *  when set, otherwise mipmaps are not allocated. {@link Texture} and {@link TextureProxy} only.
     * </li>
     *
     * <li>{@link #SurfaceFlag_Renderable} -
     *  Used to say whether a surface can be rendered to, whether a texture can be used as
     *  color attachments. Renderable when set, otherwise not renderable.
     * </li>
     *
     * <li>{@link #SurfaceFlag_Protected} -
     *  Used to say whether texture is backed by protected memory. Protected when set, otherwise
     *  not protected.
     * </li>
     *
     * <li>{@link #SurfaceFlag_ReadOnly} -
     *  Means the pixels in the texture are read-only. {@link Texture} and {@link TextureProxy}
     *  only.
     * </li>
     *
     * <li>{@link #SurfaceFlag_SkipAllocator} -
     *  When set, the proxy will be instantiated outside the allocator (e.g. for proxies that are
     *  instantiated in on-flush callbacks). Otherwise, {@link ResourceAllocator} should instantiate
     *  the proxy. {@link SurfaceProxy} only.
     * </li>
     *
     * <li>{@link #SurfaceFlag_DeferredProvider} -
     *  For TextureProxies created in a deferred list recording thread it is possible for the
     *  unique key to be cleared on the backing {@link Texture} while the unique key remains on
     *  the proxy. When set, it loosens up asserts that the key of an instantiated uniquely-keyed
     *  texture proxy is also always set on the backing {@link Texture}. {@link TextureProxy} only.
     * </li>
     *
     * <li>{@link #SurfaceFlag_GLWrapDefaultFramebuffer} -
     *  This is a OpenGL only flag. It tells us that the internal render target wraps the default
     *  framebuffer (on-screen) that preserved by window (id 0). {@link RenderTarget} only.
     * </li>
     *
     * <li>{@link #SurfaceFlag_RequireManualMSAAResolve} -
     *  This means the render target is multi-sampled, and internally holds a non-msaa texture
     *  for resolving into. The render target resolves itself by blit-ting into this internal
     *  texture. (asTexture() might or might not return the internal texture, but if it does, we
     *  always resolve the render target before accessing this texture's data.) {@link RenderTarget}
     *  only.
     * </li>
     *
     * <li>{@link #SurfaceFlag_VkSupportInputAttachment} -
     *  This is a Vulkan only flag. If set the surface can be used as an input attachment in a
     *  shader. This is used for doing in shader blending where we want to sample from the same
     *  image we are drawing to. {@link RenderTarget} only.
     * </li>
     * </ul>
     */
    public static final int
            SurfaceFlag_Default = Core.SurfaceFlag_Default,
            SurfaceFlag_Budgeted = Core.SurfaceFlag_Budgeted,
            SurfaceFlag_BackingFit = Core.SurfaceFlag_BackingFit,
            SurfaceFlag_Mipmapped = Core.SurfaceFlag_Mipmapped,
            SurfaceFlag_Renderable = Core.SurfaceFlag_Renderable,
            SurfaceFlag_Protected = Core.SurfaceFlag_Protected,
            SurfaceFlag_ReadOnly = Core.SurfaceFlag_Protected << 1,
            SurfaceFlag_SkipAllocator = Core.SurfaceFlag_Protected << 2,
            SurfaceFlag_DeferredProvider = Core.SurfaceFlag_Protected << 3,
            SurfaceFlag_GLWrapDefaultFramebuffer = Core.SurfaceFlag_Protected << 4,
            SurfaceFlag_RequireManualMSAAResolve = Core.SurfaceFlag_Protected << 5,
            SurfaceFlag_VkSupportInputAttachment = Core.SurfaceFlag_Protected << 6;

    /**
     * DstSampleFlags
     */
    public static final int
            DstSampleFlag_RequiresTextureBarrier = 1,
            DstSampleFlag_AsInputAttachment = 1 << 1;

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
