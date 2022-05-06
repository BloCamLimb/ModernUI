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

package icyllis.arcui.gl;

import icyllis.arcui.hgi.*;
import org.lwjgl.opengl.GL45C;

import javax.annotation.Nonnull;

/**
 * Renderbuffer can be only used as attachments of framebuffers as an optimization.
 * It's something like Vulkan <code>VK_IMAGE_LAYOUT_*_READ_ONLY_OPTIMAL</code>.
 * Renderbuffer can neither be accessed by shaders nor have mipmaps, but can be
 * multi-sampled.
 */
public final class GLRenderbuffer extends Surface {

    private final int mFormat;
    private final int mSampleCount;

    // may be zero for external stencil buffers associated with external render targets
    // (we don't require the client to give us the id, just tell us how many bits of stencil there are)
    private int mRenderbuffer;

    private BackendFormat mBackendFormat;

    private final long mMemorySize;

    public GLRenderbuffer(GLServer server, int width, int height,
                          int format, int sampleCount, int renderbuffer) {
        super(server, width, height);
        assert sampleCount > 0;
        mSampleCount = sampleCount;
        mFormat = format;
        mRenderbuffer = renderbuffer;

        // color buffers may be compressed
        int compression = GLUtil.glFormatCompressionType(format);
        long size = DataUtils.numBlocks(compression, width, height);
        size *= GLUtil.glFormatBytesPerBlock(format);
        size *= sampleCount;
        mMemorySize = size;

        registerWithCache(true);
    }

    @Nonnull
    public static GLRenderbuffer makeWrapped(GLServer server, int width, int height,
                                             int format, int sampleCount, int renderbuffer) {
        return new GLRenderbuffer(server, width, height, format, sampleCount, renderbuffer);
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        if (mBackendFormat == null) {
            mBackendFormat = new GLBackendFormat(GLUtil.glFormatToEnum(mFormat), Types.TEXTURE_TYPE_NONE);
        }
        return mBackendFormat;
    }

    public int getFormat() {
        return mFormat;
    }

    @Override
    public int getSampleCount() {
        return mSampleCount;
    }

    public int getRenderbuffer() {
        return mRenderbuffer;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @Override
    public long getMemorySize() {
        return mMemorySize;
    }

    @Override
    protected void onRelease() {
        if (mRenderbuffer != 0) {
            GL45C.glDeleteRenderbuffers(mRenderbuffer);
            mRenderbuffer = 0;
        }
    }

    @Override
    protected void onDiscard() {
        mRenderbuffer = 0;
    }
}
