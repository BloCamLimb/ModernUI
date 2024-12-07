/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
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

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Descriptor to create a render pass.
 */
//TODO experimental, to be reviewed
public final class RenderPassDesc {

    public static class ColorAttachmentDesc {
        @Nullable
        public ImageDesc mDesc;
        @Nullable
        public ImageDesc mResolveDesc;
        public byte mLoadOp;
        //TODO MSAA resolve?
        public byte mStoreOp;
    }

    public int mNumColorAttachments;
    public final ColorAttachmentDesc[] mColorAttachments =
            new ColorAttachmentDesc[Caps.MAX_COLOR_TARGETS];

    public static class DepthStencilAttachmentDesc {
        @Nullable
        public ImageDesc mDesc;
        public byte mLoadOp;
        public byte mStoreOp;
    }

    public final DepthStencilAttachmentDesc mDepthStencilAttachment =
            new DepthStencilAttachmentDesc();

    //TODO TBD reserved for future use
    public int mSampleCount;
}
