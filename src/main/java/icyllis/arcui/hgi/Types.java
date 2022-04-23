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
import icyllis.arcui.core.ColorType;

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
     * The type of texture. Backends other than GL currently only use the 2D value but the type must
     * still be known at the API-neutral layer as it used to determine whether MIP maps, render-ability,
     * and sampling parameters are legal for proxies that will be instantiated with wrapped textures.
     */
    public static final int
            TEXTURE_TYPE_NONE = 0,
            TEXTURE_TYPE_2D = 1,
            TEXTURE_TYPE_RECTANGLE = 2, // Rectangle uses un-normalized texture coordinates.
            TEXTURE_TYPE_EXTERNAL = 3;

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

    public static int getColorTypeChannelFlags(int ct) {
        return switch (ct) {
            case ColorType.UNKNOWN -> 0;
            case ColorType.ALPHA_8,
                    ColorType.ALPHA_16,
                    ColorType.ALPHA_F32xxx,
                    ColorType.ALPHA_8xxx,
                    ColorType.ALPHA_F16 -> Color.ALPHA_CHANNEL_FLAG;
            case ColorType.BGR_565,
                    ColorType.RGB_888,
                    ColorType.RGB_888x -> Color.RGB_CHANNEL_FLAGS;
            case ColorType.ABGR_4444,
                    ColorType.BGRA_4444,
                    ColorType.ARGB_4444,
                    ColorType.RGBA_16161616,
                    ColorType.RGBA_F32,
                    ColorType.RGBA_F16_CLAMPED,
                    ColorType.RGBA_F16,
                    ColorType.BGRA_1010102,
                    ColorType.RGBA_1010102,
                    ColorType.BGRA_8888,
                    ColorType.RGBA_8888_SRGB,
                    ColorType.RGBA_8888 -> Color.RGBA_CHANNEL_FLAGS;
            case ColorType.RG_88,
                    ColorType.RG_F16,
                    ColorType.RG_1616 -> Color.RG_CHANNEL_FLAGS;
            case ColorType.GRAY_8,
                    ColorType.GRAY_F16,
                    ColorType.GRAY_8xxx -> Color.GRAY_CHANNEL_FLAG;
            case ColorType.GRAY_ALPHA_88 -> Color.GRAY_ALPHA_CHANNEL_FLAGS;
            case ColorType.R_8,
                    ColorType.R_F16,
                    ColorType.R_16 -> Color.RED_CHANNEL_FLAG;
            default -> throw new IllegalArgumentException();
        };
    }

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

    private Types() {
    }
}
