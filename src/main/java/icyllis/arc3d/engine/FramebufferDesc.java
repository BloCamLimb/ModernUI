/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.RawPtr;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

/**
 * Descriptor to create a framebuffer.
 */
//TODO experimental, to be reviewed
public final class FramebufferDesc {

    /**
     * This is a OpenGL only flag. It tells us that the internal render target wraps the OpenGL
     * default framebuffer (id=0) that preserved by window. RT only.
     */
    @ApiStatus.Internal
    public static final
    int FLAG_GL_WRAP_DEFAULT_FB = ISurface.FLAG_PROTECTED << 4;
    /**
     * This means the render target is multi-sampled, and internally holds a non-msaa image
     * for resolving into. The render target resolves itself by blit-ting into this internal
     * image. (It might or might not have the internal image access, but if it does, we
     * always resolve the render target before accessing this image's data.) RT only.
     */
    @ApiStatus.Internal
    public static final
    int FLAG_MANUAL_MSAA_RESOLVE = ISurface.FLAG_PROTECTED << 5;
    /**
     * This is a Vulkan only flag. It tells us that the internal render target is wrapping a raw
     * Vulkan secondary command buffer. RT only.
     */
    @ApiStatus.Internal
    public static final
    int FLAG_VK_WRAP_SECONDARY_CB = ISurface.FLAG_PROTECTED << 6;

    public static class ColorAttachmentDesc {
        @Nullable
        public @RawPtr Image mAttachment;
        @Nullable
        public @RawPtr Image mResolveAttachment;
        public int mMipLevel;
        public int mArraySlice;
    }

    public int mNumColorAttachments;
    public final ColorAttachmentDesc[] mColorAttachments =
            new ColorAttachmentDesc[Caps.MAX_COLOR_TARGETS];

    public static class DepthStencilAttachmentDesc {
        @Nullable
        public @RawPtr Image mAttachment;
    }

    public final DepthStencilAttachmentDesc mDepthStencilAttachment =
            new DepthStencilAttachmentDesc();

    /**
     * If there are any attachments, then framebuffer bounds must be the intersection of
     * all attachment bounds.
     */
    public int mWidth, mHeight;
    //TODO TBD reserved for future use
    public int mSampleCount;
    public int mFramebufferFlags;
}
