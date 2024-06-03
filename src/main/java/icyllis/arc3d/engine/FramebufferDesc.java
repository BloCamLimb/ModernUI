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

import javax.annotation.Nullable;

/**
 * Descriptor to create a framebuffer.
 */
public final class FramebufferDesc {

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
