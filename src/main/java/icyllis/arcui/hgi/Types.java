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

package icyllis.arcui.hgi;

import icyllis.arcui.core.Color;
import icyllis.arcui.core.ImageInfo;

/**
 * Constants independent of graphics API.
 */
public final class Types {

    /**
     * Possible 3D APIs that may be used by Arc UI.
     */
    public static final int
            OPENGL = 0, // OpenGL 4.5 core profile
            VULKAN = 1; // Vulkan 1.1

    /**
     * Image and Surfaces can be stored such that (0, 0) in texture space may correspond to
     * either the top-left or bottom-left content pixel.
     */
    public static final int
            SURFACE_ORIGIN_TOP_LEFT = 0,
            SURFACE_ORIGIN_BOTTOM_LEFT = 1;

    /**
     * The type of texture. All are 2D.
     * <p>
     * NONE: Represents a general purpose that is not considered a texture, e.g. OpenGL renderbuffer.
     * <p>
     * 2D: OpenGL 2D texture or Vulkan 2D image.
     * <p>
     * EXTERNAL: Memory object, imports POSIX FD or Win32 NT (Windows 8+, KMT is not used).
     * Currently, OpenGL texture wraps Vulkan image, or Vulkan image wraps Linux DRM (using FD).
     * We assume external textures are read-only and have no memory size.
     */
    public static final int
            TEXTURE_TYPE_NONE = 0,
            TEXTURE_TYPE_2D = 1,
            TEXTURE_TYPE_EXTERNAL = 2;

    /**
     * Rectangle and external textures only support the clamp wrap mode and do not support
     * MIP maps.
     */
    public static boolean textureTypeHasRestrictedSampling(int type) {
        return switch (type) {
            case TEXTURE_TYPE_2D -> false;
            case TEXTURE_TYPE_EXTERNAL -> true;
            default -> throw new IllegalArgumentException();
        };
    }

    public static final int
            SHADER_TYPE_VERTEX = 0,
            SHADER_TYPE_GEOMETRY = 1,
            SHADER_TYPE_FRAGMENT = 2;

    public static final int
            SHADER_FLAG_VERTEX = 1,
            SHADER_FLAG_TESS_CONTROL = 1 << 1,
            SHADER_FLAG_TESS_EVALUATION = 1 << 2,
            SHADER_FLAG_GEOMETRY = 1 << 3,
            SHADER_FLAG_FRAGMENT = 1 << 4;

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
                    ImageInfo.COLOR_ALPHA_F32xxx,
                    ImageInfo.COLOR_ALPHA_8xxx,
                    ImageInfo.COLOR_ALPHA_F16 -> Color.ALPHA_CHANNEL_FLAG;
            case ImageInfo.COLOR_BGR_565,
                    ImageInfo.COLOR_RGB_888,
                    ImageInfo.COLOR_RGB_888x -> Color.RGB_CHANNEL_FLAGS;
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
                    ImageInfo.COLOR_GRAY_8xxx -> Color.GRAY_CHANNEL_FLAG;
            case ImageInfo.COLOR_GRAY_ALPHA_88 -> Color.GRAY_ALPHA_CHANNEL_FLAGS;
            case ImageInfo.COLOR_R_8,
                    ImageInfo.COLOR_R_F16,
                    ImageInfo.COLOR_R_16 -> Color.RED_CHANNEL_FLAG;
            default -> throw new IllegalArgumentException();
        };
    }

    /**
     * Geometric primitives used for drawing.
     * <p>
     * We can't use POINTS or LINES, because both OpenGL and Vulkan can only guarantee
     * the rasterization of one pixel in screen coordinates, may or may not anti-aliased.
     */
    public static final byte
            PRIMITIVE_TYPE_TRIANGLES = 0,       // separate triangle
            PRIMITIVE_TYPE_TRIANGLE_LIST = 0,   // separate triangle
            PRIMITIVE_TYPE_TRIANGLE_STRIP = 1,  // connected triangle
            PRIMITIVE_TYPE_PATCHES = 2,         // separate patch, tessellation
            PRIMITIVE_TYPE_PATCH_LIST = 2;      // separate patch, tessellation

    /**
     * Mask formats. Used by the font cache. Important that these are 0-based.
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
     * NONE: The resource is not budgeted and is cleaned up as soon as it has no refs regardless of whether
     * it has a unique or scratch key.
     * <p>
     * PARTIAL: The resource is not budgeted and is allowed to remain in the cache with no refs if it
     * has a unique key. Scratch keys are ignored.
     * <p>
     * COMPLETE: The resource is budgeted and is subject to cleaning up under budget pressure.
     */
    public static final byte
            BUDGET_TYPE_NONE = 0,
            BUDGET_TYPE_PARTIAL = 1,
            BUDGET_TYPE_COMPLETE = 2;

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

    /**
     * Flags shared between the Surface & SurfaceProxy class hierarchies.
     * <p>
     * READ_ONLY: Means the pixels in the texture are read-only.
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
            INTERNAL_SURFACE_FLAG_READ_ONLY = 1,
            INTERNAL_SURFACE_FLAG_PROTECTED = 1 << 1,
            INTERNAL_SURFACE_FLAG_REQUIRE_MANUAL_MSAA_RESOLVE = 1 << 2,
            INTERNAL_SURFACE_FLAG_GL_WRAP_DEFAULT_FRAMEBUFFER = 1 << 3,
            INTERNAL_SURFACE_FLAG_VK_SUPPORT_INPUT_ATTACHMENT = 1 << 4;

    /**
     * Used to describe the current state of Mips on a Texture
     */
    public static final int
            MIPMAP_STATUS_NONE = 0,     // Mips have not been allocated
            MIPMAP_STATUS_DIRTY = 1,    // Mips are allocated but the full mip tree does not have valid data
            MIPMAP_STATUS_VALID = 2;    // All levels fully allocated and have valid data in them

    private Types() {
    }
}
