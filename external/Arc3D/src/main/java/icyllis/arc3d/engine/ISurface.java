/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.core.MathUtil;
import icyllis.arc3d.core.RefCounted;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;

/**
 * Defines Surface-hierarchy constants. Do NOT directly this class as type.
 */
public interface ISurface extends RefCounted {

    /**
     * Surface flags shared between the Surface & SurfaceProxy class hierarchies.
     * An arbitrary combination of flags may result in unexpected behaviors.
     */
    int FLAG_NONE = 0;
    /**
     * Indicates whether an allocation should count against a cache budget. Budgeted when
     * set, otherwise not budgeted.
     */
    int FLAG_BUDGETED = 1;
    /**
     * Indicates whether a backing store needs to be an exact match or can be larger than
     * is strictly necessary. Approx fit when set, otherwise exact fit.
     */
    int FLAG_APPROX_FIT = 1 << 1;
    /**
     * Used to say whether a texture has mip levels allocated or not. Mipmaps are allocated
     * when set, otherwise mipmaps are not allocated.
     */
    int FLAG_MIPMAPPED = 1 << 2;
    /**
     * Used to say whether a surface can be rendered to, whether a texture can be used as
     * color attachments. Renderable when set, otherwise not renderable.
     */
    int FLAG_RENDERABLE = 1 << 3;
    /**
     * Used to say whether texture is backed by protected memory. Protected when set, otherwise
     * not protected.
     *
     * @see <a href="https://github.com/KhronosGroup/Vulkan-Guide/blob/master/chapters/protected.adoc">
     * Protected Memory</a>
     */
    int FLAG_PROTECTED = 1 << 4;
    /**
     * Means the pixels in the texture are read-only. {@link GpuTexture} and {@link TextureProxy}
     * only.
     */
    @ApiStatus.Internal
    int FLAG_READ_ONLY = FLAG_PROTECTED << 1;
    /**
     * When set, the proxy will be instantiated outside the allocator (e.g. for proxies that are
     * instantiated in on-flush callbacks). Otherwise, {@link SurfaceAllocator} should instantiate
     * the proxy. {@link SurfaceProxy} only.
     */
    @ApiStatus.Internal
    int FLAG_SKIP_ALLOCATOR = FLAG_PROTECTED << 2;
    /**
     * For TextureProxies created in a deferred list recording thread it is possible for the
     * unique key to be cleared on the backing {@link GpuTexture} while the unique key remains on
     * the proxy. When set, it loosens up asserts that the key of an instantiated uniquely-keyed
     * texture proxy is also always set on the backing {@link GpuTexture}. {@link TextureProxy} only.
     */
    @ApiStatus.Internal
    int FLAG_DEFERRED_PROVIDER = FLAG_PROTECTED << 3;
    /**
     * This is a OpenGL only flag. It tells us that the internal render target wraps the OpenGL
     * default framebuffer (id=0) that preserved by window. RT only.
     */
    @ApiStatus.Internal
    int FLAG_GL_WRAP_DEFAULT_FB = FLAG_PROTECTED << 4;
    /**
     * This means the render target is multi-sampled, and internally holds a non-msaa texture
     * for resolving into. The render target resolves itself by blit-ting into this internal
     * texture. (It might or might not have the internal texture access, but if it does, we
     * always resolve the render target before accessing this texture's data.) RT only.
     */
    @ApiStatus.Internal
    int FLAG_MANUAL_MSAA_RESOLVE = FLAG_PROTECTED << 5;
    /**
     * This is a Vulkan only flag. It tells us that the internal render target is wrapping a raw
     * Vulkan secondary command buffer. RT only.
     */
    @ApiStatus.Internal
    int FLAG_VK_WRAP_SECONDARY_CB = FLAG_PROTECTED << 6;

    /**
     * Map <code>size</code> to a larger multiple of 2. Values <= 1024 will pop up to
     * the next power of 2. Those above 1024 will only go up half the floor power of 2.
     * <p>
     * Possible values: 16, 32, 64, 128, 256, 512, 1024, 1536, 2048, 3072, 4096, 6144, 8192,
     * 12288, 16384
     *
     * @see #FLAG_APPROX_FIT
     */
    static int getApproxSize(int size) {
        final int MIN_SCRATCH_TEXTURE_SIZE = 16;
        size = Math.max(MIN_SCRATCH_TEXTURE_SIZE, size);

        if (MathUtil.isPow2(size)) {
            return size;
        }

        int ceilPow2 = MathUtil.ceilPow2(size);
        if (size <= 1024) {
            return ceilPow2;
        }

        if (size <= 16384) {
            int floorPow2 = ceilPow2 >> 1;
            int mid = floorPow2 + (floorPow2 >> 1);

            if (size <= mid) {
                return mid;
            }
            return ceilPow2;
        }

        return MathUtil.alignTo(size, 4096);
    }

    /**
     * Increases the reference count by 1 on the client pipeline.
     */
    void ref();

    /**
     * Decreases the reference count by 1 on the client pipeline.
     */
    void unref();

    /**
     * @return the width of the surface in pixels, greater than zero
     */
    int getWidth();

    /**
     * @return the height of the surface in pixels, greater than zero
     */
    int getHeight();

    /**
     * @return the backend format of the surface
     */
    @Nonnull
    BackendFormat getBackendFormat();

    ///// Common interface between RenderTexture and RenderSurface
    ///// The following methods are only valid when FLAG_RENDERABLE is set

    /**
     * Returns the number of samples per pixel in color buffers (one if non-MSAA).
     *
     * @return the number of samples, greater than (multi-sampled) or equal to one
     */
    default int getSampleCount() {
        return 1;
    }
}
