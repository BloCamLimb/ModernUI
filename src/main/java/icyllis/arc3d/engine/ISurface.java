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

import icyllis.arc3d.core.MathUtil;
import org.jetbrains.annotations.ApiStatus;

/**
 * Defines Surface-hierarchy constants. Do NOT directly this class as type.
 */
public interface ISurface {

    /**
     * Surface flags shared between the Surface & SurfaceProxy class hierarchies.
     * These flags can be used to create a surface or represent the state of the created surface.
     * An arbitrary combination of flags may result in unexpected behaviors.
     */
    int FLAG_NONE = 0;
    /**
     * Indicates whether an allocation should count against a cache budget. Budgeted when
     * set, otherwise not budgeted. This can be used for {@link SurfaceProxy} and
     * {@link GpuSurface} at creation-time. Since a single budgeted GPU surface can be used by
     * multiple {@link SurfaceProxy} at different times, its budgeted state may alter
     * (SurfaceProxy can make a budgeted GPU surface become un-budgeted).
     */
    int FLAG_BUDGETED = 1;
    /**
     * Indicates whether a backing store needs to be an exact match or can be larger than
     * is strictly necessary. Approx fit when set, otherwise exact fit. This is a
     * {@link SurfaceProxy} flag and a {@link GpuSurface} at creation-time flag.
     */
    int FLAG_APPROX_FIT = 1 << 1;
    /**
     * Used to say whether an image has or should have mip levels allocated or not.
     * Mipmaps are allocated when set, otherwise mipmaps are not allocated.
     */
    int FLAG_MIPMAPPED = 1 << 2;
    /**
     * Used to say whether an image can be a sampled image (i.e. a texture). This is not
     * compatible with {@link #FLAG_MEMORYLESS}.
     * <p>
     * A valid SurfaceFlags should have at least one of {@link #FLAG_SAMPLED_IMAGE},
     * {@link #FLAG_STORAGE_IMAGE} and {@link #FLAG_RENDERABLE} set.
     */
    int FLAG_SAMPLED_IMAGE = 1 << 3;
    /**
     * Used to say whether an image can be a storage image. This is not compatible with
     * {@link #FLAG_MEMORYLESS}.
     */
    int FLAG_STORAGE_IMAGE = 1 << 4;
    /**
     * Used to say whether a surface can be rendered to, whether an image can be used as
     * color or depth/stencil attachments. Renderable when set, otherwise not renderable.
     */
    int FLAG_RENDERABLE = 1 << 5;
    /**
     * Used to create memoryless images, especially for MSAA attachments. If so,
     * load op must NOT be {@link Engine.LoadOp#kLoad} and store op must NOT be
     * {@link Engine.StoreOp#kStore}, and rendering may be efficient on TBDR GPU.
     * This is also known as discardable and transient attachments.
     * <p>
     * Note: Memoryless must be {@link #FLAG_RENDERABLE} and must NOT be either
     * {@link #FLAG_SAMPLED_IMAGE} or {@link #FLAG_STORAGE_IMAGE}.
     */
    int FLAG_MEMORYLESS = 1 << 6;
    /**
     * Used to say whether image is backed by protected memory. Protected when set, otherwise
     * not protected. Vulkan only.
     *
     * @see <a href="https://github.com/KhronosGroup/Vulkan-Guide/blob/master/chapters/protected.adoc">
     * Protected Memory</a>
     */
    int FLAG_PROTECTED = 1 << 7;
    // the following flags are internal only
    /**
     * Means the pixels in the image are read-only. {@link Image} and {@link ImageViewProxy}
     * only, typically for wrapped images. Read-only images cannot be renderable.
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
     * For {@link ImageViewProxy} created in a deferred list recording thread it is possible for the
     * unique key to be cleared on the backing {@link Image} while the unique key remains on
     * the proxy. When set, it loosens up asserts that the key of an instantiated uniquely-keyed
     * texture proxy is also always set on the backing {@link Image}. {@link ImageViewProxy} only.
     */
    @ApiStatus.Internal
    int FLAG_DEFERRED_PROVIDER = FLAG_PROTECTED << 3;

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
        final int MIN_SCRATCH_IMAGE_SIZE = 16;
        size = Math.max(MIN_SCRATCH_IMAGE_SIZE, size);

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
}
