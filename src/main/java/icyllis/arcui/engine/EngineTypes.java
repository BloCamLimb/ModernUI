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

import icyllis.arcui.core.Color;
import icyllis.arcui.core.ImageInfo;

/**
 * Constants and utilities for Arc UI Engine.
 */
public final class EngineTypes {

    /**
     * Possible 3D APIs that may be used by Arc UI.
     */
    public static final int
            OpenGL = 0, // OpenGL 4.5 core profile
            Vulkan = 1; // Vulkan 1.1

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
            SurfaceOrigin_TopLeft = 0,
            SurfaceOrigin_BottomLeft = 1;

    /**
     * @see icyllis.arcui.opengl.GLServer#markDirty(int)
     */
    public static final int
            GLBackendState_RenderTarget = 1,
            GLBackendState_TextureBinding = 1 << 1, // Also includes samplers bound to texture units.
            GLBackendState_View = 1 << 2,           // View state stands for scissor and viewport
            GLBackendState_Blend = 1 << 3;

    /**
     * Indicates the type of pending IO operations that can be recorded for GPU resources.
     */
    public static final int
            IOType_Read = 0,
            IOType_Write = 1,
            IOType_RW = 2;

    /**
     * Describes the intended usage or the purpose of a GPU buffer.
     */
    public static final int
            GpuBufferType_Vertex = 0,       // vertex buffer
            GpuBufferType_Index = 1,        // element buffer or index buffer
            GpuBufferType_DrawIndirect = 2, // (draw) indirect buffer
            GpuBufferType_XferCpuToGpu = 3, // upload, unpack, transfer src only
            GpuBufferType_XferGpuToCpu = 4, // download, pack, transfer dst only
            GpuBufferType_Uniform = 5;      // uniform buffer
    public static final int GpuBufferTypeCount = GpuBufferType_Uniform + 1;

    /**
     * Provides a performance hint regarding the frequency at which a data store will be accessed.
     * <p>
     * DYNAMIC: Data store will be respecified repeatedly by CPU and GPU.
     * <p>
     * STATIC: Data store will be specified by CPU once and may be respecified repeatedly by GPU.
     */
    public static final int
            AccessPattern_Dynamic = 0,
            AccessPattern_Static = 1;

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
            ShaderType_Vertex = 0,
            ShaderType_Fragment = 1;

    /**
     * Shader flags. Tessellation shaders are removed.
     */
    public static final int
            ShaderFlag_Vertex = 1,
            ShaderFlag_Fragment = 1 << 1;

    /**
     * Describes the encoding of channel data in a ColorType.
     */
    public static final int
            COLOR_ENCODING_UNORM = 0,
            COLOR_ENCODING_SRGB_UNORM = 1,
            COLOR_ENCODING_SNORM = 2,
            COLOR_ENCODING_FLOAT = 3;

    public static int colorTypeChannelFlags(int ct) {
        return switch (ct) {
            case ImageInfo.COLOR_UNKNOWN -> 0;
            case ImageInfo.COLOR_ALPHA_8,
                    ImageInfo.COLOR_ALPHA_16,
                    ImageInfo.COLOR_ALPHA_F32XXX,
                    ImageInfo.COLOR_ALPHA_8XXX,
                    ImageInfo.COLOR_ALPHA_F16 -> Color.ALPHA_CHANNEL_FLAG;
            case ImageInfo.COLOR_BGR_565,
                    ImageInfo.COLOR_RGB_888,
                    ImageInfo.COLOR_RGB_888X -> Color.RGB_CHANNEL_FLAGS;
            case ImageInfo.COLOR_ABGR_4444,
                    ImageInfo.COLOR_BGRA_4444,
                    ImageInfo.COLOR_ARGB_4444,
                    ImageInfo.COLOR_RGBA_16161616,
                    ImageInfo.COLOR_RGBA_F32,
                    ImageInfo.COLOR_RGBA_F16_CLAMPED,
                    ImageInfo.COLOR_RGBA_F16,
                    ImageInfo.COLOR_BGRA_1010102,
                    ImageInfo.COLOR_RGBA_1010102,
                    ImageInfo.COLOR_BGRA_8888,
                    ImageInfo.COLOR_RGBA_8888_SRGB,
                    ImageInfo.COLOR_RGBA_8888 -> Color.RGBA_CHANNEL_FLAGS;
            case ImageInfo.COLOR_RG_88,
                    ImageInfo.COLOR_RG_F16,
                    ImageInfo.COLOR_RG_1616 -> Color.RG_CHANNEL_FLAGS;
            case ImageInfo.COLOR_GRAY_8,
                    ImageInfo.COLOR_GRAY_F16,
                    ImageInfo.COLOR_GRAY_8XXX -> Color.GRAY_CHANNEL_FLAG;
            case ImageInfo.COLOR_GRAY_ALPHA_88 -> Color.GRAY_ALPHA_CHANNEL_FLAGS;
            case ImageInfo.COLOR_R_8,
                    ImageInfo.COLOR_R_F16,
                    ImageInfo.COLOR_R_16 -> Color.RED_CHANNEL_FLAG;
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

    /**
     * Store ops. Used to specify the store operation to be used when an OpsTask/OpsRenderPass
     * ends execution.
     */
    public static final int
            StoreOp_Store = 0,
            StoreOp_Discard = 1;

    /**
     * Flags shared between the Surface & SurfaceProxy class hierarchies.
     * <p>
     * READ_ONLY: Means the pixels in the texture are read-only. Texture only.
     * <p>
     * PROTECTED: Means if we are working with protected content.
     * <p>
     * REQUIRE_MANUAL_MSAA_RESOLVE: This means the render target is multi-sampled, and internally
     * holds a non-msaa texture for resolving into. The render target resolves itself by blit-ting
     * into this internal texture. (asTexture() might or might not return the internal texture,
     * but if it does, we always resolve the render target before accessing this texture's data.)
     * <p>
     * GL_WRAP_DEFAULT_FRAMEBUFFER: This is a OpenGL only flag. It tells us that the internal
     * render target wraps the default framebuffer (on-screen) that preserved by window (id 0).
     * <p>
     * VK_SUPPORT_INPUT_ATTACHMENT: This is a Vulkan only flag. If set the surface can be used as
     * an input attachment in a shader. This is used for doing in shader blending where we want to
     * sample from the same image we are drawing to.
     */
    public static final int
            InternalSurfaceFlag_ReadOnly = 1,
            InternalSurfaceFlag_Protected = 1 << 1,
            InternalSurfaceFlag_RequiresManualMSAAResolve = 1 << 2,
            InternalSurfaceFlag_GLWrapsDefaultFramebuffer = 1 << 3,
            InternalSurfaceFlag_VkSupportsInputAttachment = 1 << 4;

    /**
     * Used to describe the current state of Mips on a Texture
     */
    public static final int
            MipmapStatus_None = 0,     // Mips have not been allocated
            MipmapStatus_Dirty = 1,    // Mips are allocated but the full mip tree does not have valid data
            MipmapStatus_Valid = 2;    // All levels fully allocated and have valid data in them

    /**
     * ResourceHandle is an opaque handle to a resource. It's actually a table index.
     */
    public static final int INVALID_RESOURCE_HANDLE = ~0;

    private EngineTypes() {
    }
}
