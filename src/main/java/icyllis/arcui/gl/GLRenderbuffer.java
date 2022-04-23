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
 */
public final class GLRenderbuffer extends Attachment {

    private final int mSampleCount;
    private final int mFormat;

    // may be zero for external stencil buffers associated with external render targets
    // (we don't require the client to give us the id, just tell us how many bits of stencil there are)
    private int mRenderbuffer;

    private BackendFormat mBackendFormat;

    private final long mMemorySize;

    public GLRenderbuffer(GLServer server, int width, int height,
                          int sampleCount, int format, int renderbuffer) {
        super(server, width, height);
        assert sampleCount > 0;
        mSampleCount = sampleCount;
        mFormat = format;
        mRenderbuffer = renderbuffer;

        // color buffers may be compressed
        int compression = GLUtil.getGLFormatCompressionType(format);
        long size = DataUtils.numBlocks(compression, width, height);
        size *= GLUtil.getGLFormatBytesPerBlock(format);
        size *= sampleCount;
        mMemorySize = size;

        registerWithCache(true);
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        if (mBackendFormat == null) {
            mBackendFormat = new GLBackendFormat(GLUtil.getGLEnumFromGLFormat(mFormat), Types.TEXTURE_TYPE_NONE);
        }
        return mBackendFormat;
    }

    @Override
    public int getSampleCount() {
        return mSampleCount;
    }

    public int getFormat() {
        return mFormat;
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
