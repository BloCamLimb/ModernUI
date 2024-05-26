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

import icyllis.arc3d.core.*;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

/**
 * Factory class used to obtain GPU resources with cache. A subclass can
 * provide backend-specific resources.
 * <p>
 * This can only be used on render thread. To create Surface-like resources
 * in other threads, use {@link SurfaceProxy}. To obtain Pipeline resources,
 * use {@link GlobalResourceCache}.
 */
public abstract class ResourceProvider {

    protected final Device mDevice;
    protected final Context mContext;

    // Each ResourceProvider owns one local cache; for some resources it also refers out to the
    // global cache of the Device, which is assumed to outlive the ResourceProvider.
    protected final ResourceCache mResourceCache;

    // lookup key
    private IResourceKey mImageScratchKey;
    private final GpuRenderTarget.ResourceKey mRenderTargetScratchKey = new GpuRenderTarget.ResourceKey();

    protected ResourceProvider(Device device, Context context) {
        mDevice = device;
        mContext = context;
        //TODO not contextID, but...
        mResourceCache = new ResourceCache(context.getContextID());
    }

    protected void destroy() {
        mResourceCache.shutdown();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Images

    /**
     * Finds or creates a texture that matches the descriptor. The texture's format will always
     * match the request. The contents of the texture are undefined.
     * <p>
     * When {@link ISurface#FLAG_BUDGETED} is set, the texture will count against the resource
     * cache budget. If {@link ISurface#FLAG_APPROX_FIT} is also set, it's always budgeted.
     * <p>
     * When {@link ISurface#FLAG_APPROX_FIT} is set, the method returns a potentially approx fit
     * texture that approximately matches the descriptor. Will be at least as large in width and
     * height as desc specifies. In this case, {@link ISurface#FLAG_MIPMAPPED} and
     * {@link ISurface#FLAG_BUDGETED} are ignored. Otherwise, the method returns an exact fit
     * texture.
     * <p>
     * When {@link ISurface#FLAG_MIPMAPPED} is set, the texture will be allocated with mipmaps.
     * If {@link ISurface#FLAG_APPROX_FIT} is also set, it always has no mipmaps.
     * <p>
     * When {@link ISurface#FLAG_RENDERABLE} is set, the texture can be rendered to and
     * can be used as attachments of a framebuffer. The <code>sampleCount</code>
     * specifies the number of samples to use for rendering.
     * <p>
     * When {@link ISurface#FLAG_PROTECTED} is set, the texture will be created as protected.
     *
     * @param width        the desired width of the texture to be created
     * @param height       the desired height of the texture to be created
     * @param format       the backend format for the texture
     * @param sampleCount  the number of samples to use for rendering if renderable is set,
     *                     otherwise this must be 1
     * @param surfaceFlags the combination of the above flags
     * @param label        the label for debugging purposes, can be empty to clear the label,
     *                     or null to leave the label unchanged
     * @see ISurface#FLAG_BUDGETED
     * @see ISurface#FLAG_APPROX_FIT
     * @see ISurface#FLAG_MIPMAPPED
     * @see ISurface#FLAG_SAMPLED_IMAGE
     * @see ISurface#FLAG_RENDERABLE
     * @see ISurface#FLAG_MEMORYLESS
     * @see ISurface#FLAG_PROTECTED
     */
    @Nullable
    @SharedPtr
    public final Image createImage(int width, int height,
                                   BackendFormat format,
                                   int sampleCount,
                                   int surfaceFlags,
                                   @Nullable String label) {
        assert mContext.isOwnerThread();
        if (mContext.isDeviceLost()) {
            return null;
        }

        if (format.isCompressed()) {
            return null;
        }

        // hide invalid flags
        surfaceFlags &= ISurface.FLAG_BUDGETED | ISurface.FLAG_APPROX_FIT |
                ISurface.FLAG_MIPMAPPED | ISurface.FLAG_SAMPLED_IMAGE |
                ISurface.FLAG_RENDERABLE | ISurface.FLAG_MEMORYLESS |
                ISurface.FLAG_PROTECTED;

        if ((surfaceFlags & ISurface.FLAG_SAMPLED_IMAGE) != 0) {
            // texturable cannot be memoryless
            surfaceFlags &= ~ISurface.FLAG_MEMORYLESS;
        }

        if ((surfaceFlags & ISurface.FLAG_MEMORYLESS) != 0) {
            // memoryless cannot be approx fit and must be renderable
            surfaceFlags &= ~ISurface.FLAG_APPROX_FIT;
            surfaceFlags |= ISurface.FLAG_RENDERABLE;
        }

        // approx fit is create-time or surface proxy flag
        if ((surfaceFlags & ISurface.FLAG_APPROX_FIT) != 0) {
            width = ISurface.getApproxSize(width);
            height = ISurface.getApproxSize(height);
            // approx fit cannot be mipmapped and must be budgeted
            surfaceFlags &= ISurface.FLAG_SAMPLED_IMAGE | ISurface.FLAG_RENDERABLE | ISurface.FLAG_PROTECTED;
            surfaceFlags |= ISurface.FLAG_BUDGETED;
        }

        if (!mDevice.getCaps().validateSurfaceParams(width, height, format,
                sampleCount, surfaceFlags)) {
            return null;
        }

        /*final Image image = findAndRefScratchImage(width, height, format,
                sampleCount, surfaceFlags, label);
        if (image != null) {
            if ((surfaceFlags & ISurface.FLAG_BUDGETED) == 0) {
                image.makeBudgeted(false);
            }
            return image;
        }*/

        /*return mDevice.createImage(width, height, format,
                sampleCount, surfaceFlags, label);*/
        return null;
    }

    @Nullable
    @SharedPtr
    public final Image findOrCreateImage(ImageDesc desc,
                                         boolean budgeted,
                                         @Nullable String label) {
        var key = mDevice.getCaps().computeImageKey(desc,
                mImageScratchKey);
        if (key == null) {
            return null;
        }
        mImageScratchKey = key;

        Image image = findAndRefScratchImage(key, budgeted, label);
        if (image != null) {
            return image;
        }

        image = createNewImage(desc, budgeted, label);
        if (image == null) {
            return null;
        }

        image.setKey(key.copy());
        mResourceCache.insertResource(image);

        return image;
    }

    /**
     * Creates a new GPU image object and allocates its GPU memory. In other words, the
     * image data is dirty and needs to be uploaded later. If mipmapped, also allocates
     * <code>(31 - CLZ(max(width,height)))</code> mipmaps in addition to the base level.
     * NPoT (non-power-of-two) dimensions are always supported. Compressed format are
     * supported.
     *
     * @return the image object if successful, otherwise nullptr
     * @see ISurface#FLAG_BUDGETED
     * @see ISurface#FLAG_MIPMAPPED
     * @see ISurface#FLAG_RENDERABLE
     * @see ISurface#FLAG_PROTECTED
     */
    @Nullable
    @SharedPtr
    public final Image createNewImage(ImageDesc desc,
                                      boolean budgeted,
                                      @Nullable String label) {
        if (desc.isCompressed()) {
            return null;
        }
        final Image image = onCreateNewImage(desc, budgeted);
        if (image != null) {
            assert image.getDesc() == desc;
            if (label != null) {
                image.setLabel(label);
            }
           /* mStats.incImageCreates();
            if (image.isSampledImage()) {
                mStats.incTextureCreates();
            }*/
        }
        return image;
    }

    /**
     * Overridden by backend-specific derived class to create objects.
     * <p>
     * Image size and format support will have already been validated in base class
     * before onCreateImage is called.
     */
    @ApiStatus.OverrideOnly
    @Nullable
    @SharedPtr
    protected abstract Image onCreateNewImage(ImageDesc desc,
                                              boolean budgeted);

    /**
     * Search the cache for a scratch texture matching the provided arguments. Failing that
     * it returns null. If non-null, the resulting texture is always budgeted.
     *
     * @param label the label for debugging purposes, can be empty to clear the label,
     *              or null to leave the label unchanged
     */
    @Nullable
    @SharedPtr
    public final Image findAndRefScratchImage(IResourceKey key,
                                              boolean budgeted,
                                              @Nullable String label) {
        assert mContext.isOwnerThread();
        assert !mDevice.isDeviceLost();

        Resource resource = mResourceCache.findAndRefResource(key, budgeted);
        if (resource != null) {
            mDevice.getStats().incNumScratchTexturesReused();
            if (label != null) {
                resource.setLabel(label);
            }
            return (Image) resource;
        }
        return null;
    }

    /**
     * Search the cache for a scratch texture matching the provided arguments. Failing that
     * it returns null. If non-null, the resulting texture is always budgeted.
     *
     * @param label the label for debugging purposes, can be empty to clear the label,
     *              or null to leave the label unchanged
     * @see ISurface#FLAG_MIPMAPPED
     * @see ISurface#FLAG_RENDERABLE
     * @see ISurface#FLAG_PROTECTED
     */
    @Nullable
    @SharedPtr
    public final Image findAndRefScratchImage(ImageDesc desc,
                                              boolean budgeted,
                                              @Nullable String label) {
        assert mContext.isOwnerThread();
        assert !mDevice.isDeviceLost();

        var key = mDevice.getCaps().computeImageKey(desc,
                mImageScratchKey);
        if (key == null) {
            return null;
        }
        mImageScratchKey = key;

        return findAndRefScratchImage(key, budgeted, label);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Textures

    /**
     * Finds or creates a texture that matches the descriptor. The texture's format will always
     * match the request. The contents of the texture are undefined.
     * <p>
     * When {@link ISurface#FLAG_BUDGETED} is set, the texture will count against the resource
     * cache budget. If {@link ISurface#FLAG_APPROX_FIT} is also set, it's always budgeted.
     * <p>
     * When {@link ISurface#FLAG_APPROX_FIT} is set, the method returns a potentially approx fit
     * texture that approximately matches the descriptor. Will be at least as large in width and
     * height as desc specifies. In this case, {@link ISurface#FLAG_MIPMAPPED} and
     * {@link ISurface#FLAG_BUDGETED} are ignored. Otherwise, the method returns an exact fit
     * texture.
     * <p>
     * When {@link ISurface#FLAG_MIPMAPPED} is set, the texture will be allocated with mipmaps.
     * If {@link ISurface#FLAG_APPROX_FIT} is also set, it always has no mipmaps.
     * <p>
     * When {@link ISurface#FLAG_RENDERABLE} is set, the texture can be rendered to and
     * can be used as attachments of a framebuffer. The <code>sampleCount</code>
     * specifies the number of samples to use for rendering.
     * <p>
     * When {@link ISurface#FLAG_PROTECTED} is set, the texture will be created as protected.
     *
     * @param width        the desired width of the texture to be created
     * @param height       the desired height of the texture to be created
     * @param format       the backend format for the texture
     * @param sampleCount  the number of samples to use for rendering if renderable is set,
     *                     otherwise this must be 1
     * @param surfaceFlags the combination of the above flags
     * @param label        the label for debugging purposes, can be empty to clear the label,
     *                     or null to leave the label unchanged
     * @see ISurface#FLAG_BUDGETED
     * @see ISurface#FLAG_APPROX_FIT
     * @see ISurface#FLAG_MIPMAPPED
     * @see ISurface#FLAG_SAMPLED_IMAGE
     * @see ISurface#FLAG_RENDERABLE
     * @see ISurface#FLAG_MEMORYLESS
     * @see ISurface#FLAG_PROTECTED
     */
    @Nullable
    @SharedPtr
    public final Image createTexture(int width, int height,
                                     BackendFormat format,
                                     int sampleCount,
                                     int surfaceFlags,
                                     String label) {
        assert mContext.isOwnerThread();
        if (mDevice.isDeviceLost()) {
            return null;
        }

        if (format.isCompressed()) {
            // Currently, we don't recycle compressed textures as scratch. Additionally, all compressed
            // textures should be created through the createCompressedTexture function.
            return null;
        }

        surfaceFlags |= ISurface.FLAG_SAMPLED_IMAGE;
        return createImage(width, height,
                format,
                sampleCount,
                surfaceFlags,
                label);
    }

    /**
     * Same as {@link #createTexture(int, int, BackendFormat, int, int, String)} but with initial
     * data to upload. The color type must be valid for the format and also describe the texel data.
     * This will ensure any conversions that need to get applied to the data before upload are applied.
     *
     * @param width        the desired width of the texture to be created
     * @param height       the desired height of the texture to be created
     * @param format       the backend format for the texture
     * @param sampleCount  the number of samples to use for rendering if renderable is set,
     *                     otherwise this must be 1
     * @param surfaceFlags the combination of the above flags
     * @param dstColorType the format and type of the use of the texture, used to validate
     *                     srcColorType with texture's internal format
     * @param srcColorType the format and type of the texel data to upload
     * @param rowBytes     row size in bytes if data is greater than proper value
     * @param pixels       the pointer to the texel data for base level image
     * @param label        the label for debugging purposes, can be empty to clear the label,
     *                     or null to leave the label unchanged
     * @see ISurface#FLAG_BUDGETED
     * @see ISurface#FLAG_APPROX_FIT
     * @see ISurface#FLAG_MIPMAPPED
     * @see ISurface#FLAG_RENDERABLE
     * @see ISurface#FLAG_PROTECTED
     */
    @Nullable
    @SharedPtr
    public final Image createTexture(int width, int height,
                                     BackendFormat format,
                                     int sampleCount,
                                     int surfaceFlags,
                                     int dstColorType,
                                     int srcColorType,
                                     int rowBytes,
                                     long pixels,
                                     String label) {
        assert mContext.isOwnerThread();
        if (mDevice.isDeviceLost()) {
            return null;
        }

        if (srcColorType == ColorInfo.CT_UNKNOWN ||
                dstColorType == ColorInfo.CT_UNKNOWN) {
            return null;
        }

        int minRowBytes = width * ColorInfo.bytesPerPixel(srcColorType);
        int actualRowBytes = rowBytes > 0 ? rowBytes : minRowBytes;
        if (actualRowBytes < minRowBytes) {
            return null;
        }
        int actualColorType = (int) mDevice.getCaps().getSupportedWriteColorType(
                dstColorType,
                format,
                srcColorType);
        if (actualColorType != srcColorType) {
            return null;
        }

        final Image texture = createTexture(width, height, format,
                sampleCount, surfaceFlags, label);
        if (texture == null) {
            return null;
        }
        if (pixels == 0) {
            return texture;
        }
        boolean result = mDevice.writePixels(texture, 0, 0, width, height,
                dstColorType, actualColorType, actualRowBytes, pixels);
        assert result;

        return texture;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Framebuffers

    /**
     * Create a single color target, resolve target and optional depth/stencil target,
     * then create the framebuffer for these targets.
     * <p>
     * Search the cache for a combination of attachments and its {@link GpuRenderTarget}
     * matching the provided arguments. If none, find or create one color target, resolve target,
     * and optional depth/stencil target. Then creates a new {@link GpuRenderTarget} object
     * for the combination of attachments.
     * <p>
     * This method is responsible for canonicalization of surface flags.
     *
     * @param width
     * @param height
     * @param colorFormat
     * @param depthStencilFormat
     * @param surfaceFlags
     * @param label
     * @return
     */
    @Nullable
    @SharedPtr
    public final GpuRenderTarget createRenderTarget(int width, int height,
                                                    @Nullable BackendFormat colorFormat,
                                                    int colorFlags,
                                                    @Nullable BackendFormat resolveFormat,
                                                    int resolveFlags,
                                                    @Nullable BackendFormat depthStencilFormat,
                                                    int depthStencilFlags,
                                                    int sampleCount,
                                                    int surfaceFlags,
                                                    @Nullable String label) {
        assert mContext.isOwnerThread();
        assert !mDevice.isDeviceLost();
        Caps caps = mDevice.getCaps();

        if (sampleCount <= 1) {
            resolveFormat = null;
        }

        // all targets must be color/resolve/depth/stencil attachable
        if (colorFormat != null) {
            colorFlags |= ISurface.FLAG_RENDERABLE;
        } else {
            colorFlags = 0;
        }
        if (resolveFormat != null) {
            resolveFlags |= ISurface.FLAG_RENDERABLE;
        } else {
            resolveFlags = 0;
        }
        if (depthStencilFormat != null) {
            depthStencilFlags |= ISurface.FLAG_RENDERABLE;
        } else {
            depthStencilFlags = 0;
        }

        surfaceFlags |= ISurface.FLAG_RENDERABLE;

        // approx fit is create-time or surface proxy flag
        if ((surfaceFlags & ISurface.FLAG_APPROX_FIT) != 0) {
            width = ISurface.getApproxSize(width);
            height = ISurface.getApproxSize(height);
            // approx fit cannot be mipmapped and must be budgeted
            /*surfaceFlags &= ISurface.FLAG_TEXTURABLE | ISurface.FLAG_RENDERABLE | ISurface.FLAG_PROTECTED;
            surfaceFlags |= ISurface.FLAG_BUDGETED;*/
        }

        if (colorFormat != null && !caps.validateSurfaceParams(
                width, height, colorFormat, sampleCount, colorFlags)) {
            return null;
        }

        if (resolveFormat != null && !caps.validateSurfaceParams(
                width, height, resolveFormat, 1, resolveFlags)) {
            return null;
        }

        if (depthStencilFormat != null && !caps.validateSurfaceParams(
                width, height, depthStencilFormat, sampleCount, depthStencilFlags)) {
            return null;
        }

        GpuRenderTarget renderTarget = findAndRefScratchRenderTarget(width, height,
                colorFormat, colorFlags,
                resolveFormat, resolveFlags,
                depthStencilFormat, depthStencilFlags,
                sampleCount, surfaceFlags, label);
        if (renderTarget != null) {
            if ((surfaceFlags & ISurface.FLAG_BUDGETED) == 0) {
                renderTarget.makeBudgeted(false);
            }
            return renderTarget;
        }

        Image colorAtt = null;
        if (colorFormat != null) {
            colorAtt = createImage(width, height,
                    colorFormat,
                    sampleCount,
                    colorFlags,
                    label == null ? null : label.isEmpty() ? "" : label + "_C0");
            if (colorAtt == null) {
                return null;
            }
        }

        Image resolveAtt = null;
        if (resolveFormat != null) {
            resolveAtt = createImage(width, height,
                    resolveFormat,
                    1,
                    resolveFlags,
                    label == null ? null : label.isEmpty() ? "" : label + "_R0");
            if (resolveAtt == null) {
                RefCnt.move(colorAtt);
                return null;
            }
        }

        Image depthStencilAtt = null;
        if (depthStencilFormat != null) {
            depthStencilAtt = createImage(width, height,
                    depthStencilFormat,
                    sampleCount,
                    depthStencilFlags,
                    label == null ? null : label.isEmpty() ? "" : label + "_DS");
            if (depthStencilAtt == null) {
                RefCnt.move(colorAtt);
                RefCnt.move(resolveAtt);
                return null;
            }
        }

        renderTarget = createRenderTarget(1,
                colorFormat != null ? new Image[]{colorAtt} : null,
                resolveFormat != null ? new Image[]{resolveAtt} : null,
                null,
                depthStencilAtt,
                surfaceFlags);
        if (renderTarget != null) {
            return renderTarget;
        }

        RefCnt.move(colorAtt);
        RefCnt.move(resolveAtt);
        RefCnt.move(depthStencilAtt);

        return null;
    }

    /**
     * Create a new {@link GpuRenderTarget} object for the given attachments (RTs and resolve targets).
     * If and only if successful, this method transfers the ownership of all the given attachments.
     * <p>
     * The <var>colorTargets</var> array may contain nullptr elements, meaning that draw buffer index
     * is unused.
     *
     * @param numColorTargets    the number of color targets
     * @param colorTargets       array of color attachments
     * @param resolveTargets     array of color resolve attachments
     * @param mipLevels          array of which mip level will be used for color & resolve attachments
     * @param depthStencilTarget optional depth/stencil attachment
     * @param surfaceFlags       framebuffer flags
     * @return a new RenderTarget
     */
    @Nullable
    @SharedPtr
    public final GpuRenderTarget createRenderTarget(int numColorTargets,
                                                    @Nullable Image[] colorTargets,
                                                    @Nullable Image[] resolveTargets,
                                                    @Nullable int[] mipLevels,
                                                    @Nullable Image depthStencilTarget,
                                                    int surfaceFlags) {
        return mDevice.createRenderTarget(numColorTargets,
                colorTargets, resolveTargets, mipLevels, depthStencilTarget, surfaceFlags);
    }

    @Nullable
    @SharedPtr
    public final GpuRenderTarget createRenderTarget(int width, int height,
                                                    int sampleCount) {
        //TODO framebuffer with no attachments, ARB_framebuffer_no_attachments
        return null;
    }

    @Nullable
    @SharedPtr
    public final GpuRenderTarget findAndRefScratchRenderTarget(IResourceKey key, String label) {
        assert mContext.isOwnerThread();
        assert !mDevice.isDeviceLost();
        assert key instanceof GpuRenderTarget.ResourceKey;

        Resource resource = mResourceCache.findAndRefResource(key, true);
        if (resource != null) {
            mDevice.getStats().incNumScratchRenderTargetsReused();
            if (label != null) {
                resource.setLabel(label);
            }
            return (GpuRenderTarget) resource;
        }
        return null;
    }

    @Nullable
    @SharedPtr
    public final GpuRenderTarget findAndRefScratchRenderTarget(int width, int height,
                                                               BackendFormat colorFormat,
                                                               int colorFlags,
                                                               BackendFormat resolveFormat,
                                                               int resolveFlags,
                                                               BackendFormat depthStencilFormat,
                                                               int depthStencilFlags,
                                                               int sampleCount,
                                                               int surfaceFlags,
                                                               String label) {

        return findAndRefScratchRenderTarget(mRenderTargetScratchKey.compute(
                width, height,
                colorFormat, colorFlags,
                resolveFormat, resolveFlags,
                depthStencilFormat, depthStencilFlags,
                sampleCount,
                surfaceFlags
        ), label);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Wrapped Backend Surfaces

    /**
     * This makes the backend texture be renderable. If <code>sampleCount</code> is > 1 and
     * the underlying API uses separate MSAA render buffers then a MSAA render buffer is created
     * that resolves to the texture.
     * <p>
     * Ownership specifies rules for external GPU resources imported into Engine. If false,
     * Engine will assume the client will keep the resource alive and Engine will not free it.
     * If true, Engine will assume ownership of the resource and free it. If this method failed,
     * then ownership doesn't work.
     *
     * @param texture     the backend texture must be single sample
     * @param sampleCount the desired sample count
     * @return a managed, non-recycled render target, or null if failed
     */
    @Nullable
    @SharedPtr
    public final GpuRenderTarget wrapRenderableBackendTexture(BackendImage texture,
                                                              int sampleCount,
                                                              boolean ownership) {
        if (mDevice.isDeviceLost()) {
            return null;
        }
        return mDevice.wrapRenderableBackendTexture(texture, sampleCount, ownership);
    }

    /**
     * Wraps OpenGL default framebuffer (FBO 0). Currently wrapped render targets
     * always are not cacheable and not owned by returned object (you must free it
     * manually, releasing RenderSurface doesn't release the backend framebuffer).
     *
     * @return RenderSurface object or null on failure.
     */
    @Nullable
    @SharedPtr
    public final GpuRenderTarget wrapGLDefaultFramebuffer(int width, int height,
                                                          int sampleCount,
                                                          int depthBits,
                                                          int stencilBits,
                                                          BackendFormat format) {
        return mDevice.wrapGLDefaultFramebuffer(width, height, sampleCount, depthBits, stencilBits, format);
    }

    /**
     * Wraps an existing render target with a RenderSurface object. It is
     * similar to wrapBackendTexture but can be used to draw into surfaces
     * that are not also textures (e.g. FBO 0 in OpenGL, or an MSAA buffer that
     * the client will resolve to a texture). Currently wrapped render targets
     * always are not cacheable and not owned by returned object (you must free it
     * manually, releasing RenderSurface doesn't release the backend framebuffer).
     *
     * @return RenderSurface object or null on failure.
     */
    @Nullable
    @SharedPtr
    public final GpuRenderTarget wrapBackendRenderTarget(BackendRenderTarget backendRenderTarget) {
        if (mDevice.isDeviceLost()) {
            return null;
        }
        return mDevice.wrapBackendRenderTarget(backendRenderTarget);
    }

    /**
     * Returns a buffer.
     *
     * @param size  minimum size of buffer to return.
     * @param usage hint to the graphics subsystem about what the buffer will be used for.
     * @return the buffer if successful, otherwise nullptr.
     * @see Device.BufferUsageFlags
     */
    @Nullable
    @SharedPtr
    public final Buffer createBuffer(long size, int usage) {
        if (mDevice.isDeviceLost()) {
            return null;
        }
        //TODO scratch
        return mDevice.createBuffer(size, usage);
    }

    public final void assignUniqueKeyToResource(IUniqueKey key, Resource resource) {
        //assert mDevice.getContext().isOwnerThread();
        if (mDevice.isDeviceLost() || resource == null) {
            return;
        }
        //resource.setUniqueKey(key);
    }
}
