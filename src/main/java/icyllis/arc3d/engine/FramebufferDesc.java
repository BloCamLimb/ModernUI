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
import icyllis.arc3d.core.UniqueID;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Descriptor to create a framebuffer.
 */
//TODO experimental, to be reviewed
@Immutable
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

    @Immutable
    public static final class ColorAttachmentDesc {
        @Nullable
        public final WeakReference<@RawPtr Image> mAttachment;
        @Nullable
        public final WeakReference<@RawPtr Image> mResolveAttachment;
        @Nullable
        public final UniqueID mAttachmentID;
        @Nullable
        public final UniqueID mResolveAttachmentID;
        public final int mMipLevel;
        public final int mArraySlice;

        public ColorAttachmentDesc() {
            mAttachment = null;
            mAttachmentID = null;
            mResolveAttachment = null;
            mResolveAttachmentID = null;
            mMipLevel = 0;
            mArraySlice = 0;
        }

        public ColorAttachmentDesc(@Nullable @RawPtr Image attachment,
                                   @Nullable @RawPtr Image resolveAttachment,
                                   int mipLevel, int arraySlice) {
            if (attachment != null) {
                mAttachment = new WeakReference<>(attachment);
                mAttachmentID = attachment.getUniqueID();
            } else {
                mAttachment = null;
                mAttachmentID = null;
            }
            if (resolveAttachment != null) {
                mResolveAttachment = new WeakReference<>(resolveAttachment);
                mResolveAttachmentID = resolveAttachment.getUniqueID();
            } else {
                mResolveAttachment = null;
                mResolveAttachmentID = null;
            }
            assert mipLevel >= 0 && arraySlice >= 0;
            mMipLevel = mipLevel;
            mArraySlice = arraySlice;
        }

        @SuppressWarnings("DataFlowIssue")
        public boolean isStale() {
            Image e;
            return (mAttachmentID != null && ((e = mAttachment.get()) == null || e.isDestroyed())) ||
                    (mResolveAttachmentID != null && ((e = mResolveAttachment.get()) == null || e.isDestroyed()));
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(mAttachmentID);
            result = 31 * result + Objects.hashCode(mResolveAttachmentID);
            result = 31 * result + mMipLevel;
            result = 31 * result + mArraySlice;
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof ColorAttachmentDesc that) {
                return mMipLevel == that.mMipLevel &&
                        mArraySlice == that.mArraySlice &&
                        mAttachmentID == that.mAttachmentID &&
                        mResolveAttachmentID == that.mResolveAttachmentID;
            }
            return false;
        }
    }

    @Nonnull
    public final ColorAttachmentDesc[] mColorAttachments;

    public static final ColorAttachmentDesc[] NO_COLOR_ATTACHMENTS = new ColorAttachmentDesc[0];

    @Immutable
    public static final class DepthStencilAttachmentDesc {
        @Nullable
        public final WeakReference<@RawPtr Image> mAttachment;
        @Nullable
        public final UniqueID mAttachmentID;

        public DepthStencilAttachmentDesc() {
            mAttachment = null;
            mAttachmentID = null;
        }

        public DepthStencilAttachmentDesc(@Nullable @RawPtr Image attachment) {
            if (attachment != null) {
                mAttachment = new WeakReference<>(attachment);
                mAttachmentID = attachment.getUniqueID();
            } else {
                mAttachment = null;
                mAttachmentID = null;
            }
        }

        @SuppressWarnings("DataFlowIssue")
        public boolean isStale() {
            Image e;
            return (mAttachmentID != null && ((e = mAttachment.get()) == null || e.isDestroyed()));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(mAttachmentID);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof DepthStencilAttachmentDesc that) {
                return mAttachmentID == that.mAttachmentID;
            }
            return false;
        }
    }

    @Nonnull
    public final DepthStencilAttachmentDesc mDepthStencilAttachment;

    public static final DepthStencilAttachmentDesc NO_DEPTH_STENCIL_ATTACHMENT = new DepthStencilAttachmentDesc();

    /**
     * If there are any attachments, then framebuffer bounds must be the intersection of
     * all attachment bounds.
     */
    public final int mWidth, mHeight;
    //TODO TBD reserved for future use
    public final int mSampleCount;
    //TODO WIP
    public int mFramebufferFlags;

    public FramebufferDesc(int width, int height, int sampleCount,
                           @Nullable ColorAttachmentDesc colorAttachment,
                           @Nullable DepthStencilAttachmentDesc depthStencilAttachment) {
        this(width, height, sampleCount,
                colorAttachment != null ? new ColorAttachmentDesc[]{colorAttachment} : null,
                depthStencilAttachment);
    }

    public FramebufferDesc(int width, int height, int sampleCount,
                           @Nullable ColorAttachmentDesc[] colorAttachments,
                           @Nullable DepthStencilAttachmentDesc depthStencilAttachment) {
        mWidth = width;
        mHeight = height;
        mSampleCount = sampleCount;
        mColorAttachments = colorAttachments != null
                ? colorAttachments
                : NO_COLOR_ATTACHMENTS;
        mDepthStencilAttachment = depthStencilAttachment != null
                ? depthStencilAttachment
                : NO_DEPTH_STENCIL_ATTACHMENT;
        assert mColorAttachments.length <= Caps.MAX_COLOR_TARGETS;
        for (var colorAttachment : mColorAttachments) {
            assert colorAttachment != null;
        }
    }

    /**
     * Should the framebuffer keyed by this be deleted now? Used to delete framebuffers
     * if one of the attachments has already been deleted.
     *
     * @return true to delete, false to keep
     */
    public boolean isStale() {
        for (var colorAttachment : mColorAttachments)
            if (colorAttachment.isStale())
                return true;
        return mDepthStencilAttachment.isStale();
    }

    @Override
    public int hashCode() {
        int result = mWidth;
        result = 31 * result + mHeight;
        result = 31 * result + mSampleCount;
        for (var colorAttachment : mColorAttachments) {
            result = 31 * result + colorAttachment.hashCode();
        }
        result = 31 * result + mDepthStencilAttachment.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof FramebufferDesc that) {
            if (mWidth == that.mWidth &&
                    mHeight == that.mHeight &&
                    mSampleCount == that.mSampleCount &&
                    mColorAttachments.length == that.mColorAttachments.length &&
                    mDepthStencilAttachment.equals(that.mDepthStencilAttachment)) {
                for (int i = 0; i < mColorAttachments.length; i++) {
                    if (!mColorAttachments[i].equals(that.mColorAttachments[i]))
                        return false;
                }
                return true;
            }
        }
        return false;
    }
}
