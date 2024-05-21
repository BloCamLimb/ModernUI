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

/**
 * Specifies info to create a render pass and begin a render pass.
 */
public class RenderPassDesc {

    public static class ColorAttachmentDesc {
        public ImageDesc mDesc;
        public ImageDesc mResolveDesc;
        public int mMipLevel;
        public int mArraySlice;
        public byte mLoadOp;
        public byte mStoreOp;
        public float[] mClearColor;
    }

    public ColorAttachmentDesc[] mColorAttachments;

    public static class DepthStencilAttachmentDesc {
        public ImageDesc mDesc;
        // no resolve
        public byte mLoadOp;
        public byte mStoreOp;
        public float mClearDepth;
        public int mClearStencil;
    }

    public DepthStencilAttachmentDesc mDepthStencilAttachment;

    public int mSampleCount;
}
