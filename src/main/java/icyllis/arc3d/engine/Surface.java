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

import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;

/**
 * Interface representing primary surface data.
 */
public interface Surface {

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
     * Means the pixels in the texture are read-only. {@link Texture} and {@link TextureProxy}
     * only.
     */
    @ApiStatus.Internal
    int FLAG_READ_ONLY = FLAG_PROTECTED << 1;
    /**
     * When set, the proxy will be instantiated outside the allocator (e.g. for proxies that are
     * instantiated in on-flush callbacks). Otherwise, {@link ResourceAllocator} should instantiate
     * the proxy. {@link SurfaceProxy} only.
     */
    @ApiStatus.Internal
    int FLAG_SKIP_ALLOCATOR = FLAG_PROTECTED << 2;
    /**
     * For TextureProxies created in a deferred list recording thread it is possible for the
     * unique key to be cleared on the backing {@link Texture} while the unique key remains on
     * the proxy. When set, it loosens up asserts that the key of an instantiated uniquely-keyed
     * texture proxy is also always set on the backing {@link Texture}. {@link TextureProxy} only.
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

    /**
     * Surface flags.
     *
     * <ul>
     * <li>{@link #FLAG_BUDGETED} -
     *  Indicates whether an allocation should count against a cache budget. Budgeted when
     *  set, otherwise not budgeted. {@link Texture} or RenderTexture only.
     * </li>
     *
     * <li>{@link #FLAG_MIPMAPPED} -
     *  Used to say whether a texture has mip levels allocated or not. Mipmaps are allocated
     *  when set, otherwise mipmaps are not allocated. {@link Texture} or RenderTexture only.
     * </li>
     *
     * <li>{@link #FLAG_RENDERABLE} -
     *  Used to say whether a surface can be rendered to, whether a texture can be used as
     *  color attachments. Renderable when set, otherwise not renderable.
     * </li>
     *
     * <li>{@link #FLAG_PROTECTED} -
     *  Used to say whether texture is backed by protected memory. Protected when set, otherwise
     *  not protected.
     * </li>
     *
     * <li>{@link #FLAG_READ_ONLY} -
     *  Means the pixels in the texture are read-only. Non-renderable {@link Texture} only.
     * </li>
     *
     * @return combination of the above flags
     */
    int getSurfaceFlags();

    /**
     * If this object is texture, returns this.
     * <p>
     * If this object is framebuffer, returns the associated color buffer 0 if available,
     * or null (only OpenGL can return null).
     *
     * @return raw ptr to the texture
     */
    default Texture asTexture() {
        return null;
    }

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

    /**
     * If this object is framebuffer, returns this.
     * <p>
     * If this object is texture, returns the associated framebuffer if renderable, otherwise null.
     *
     * @return raw ptr to the framebuffer set associated with the RT, or null if non-renderable
     */
    default RenderTarget asRenderTarget() {
        return null;
    }
}
