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

package icyllis.arcui.opengl;

import icyllis.arcui.core.SmartPtr;
import icyllis.arcui.engine.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static icyllis.arcui.opengl.GLCore.*;

/**
 * Renderbuffer can be only used as attachments of framebuffers as an optimization.
 * Renderbuffer can neither be accessed by shaders nor have mipmaps, but can be
 * multisampled.
 */
public final class GLRenderbuffer extends Surface {

    // may be zero for external stencil buffers associated with external render targets
    // (we don't require the client to give us the id, just tell us how many bits of stencil there are)
    private int mRenderbuffer;

    private final int mSampleCount;
    private final int mFormat;

    private BackendFormat mBackendFormat;

    private final long mMemorySize;

    private GLRenderbuffer(GLServer server, int width, int height,
                           int renderbuffer, int sampleCount, int format) {
        super(server, width, height);
        assert sampleCount > 0;
        mRenderbuffer = renderbuffer;
        mSampleCount = sampleCount;
        mFormat = format;

        // color buffers may be compressed
        mMemorySize = DataUtils.numBlocks(glFormatCompressionType(format), width, height) *
                glFormatBytesPerBlock(format) * sampleCount;

        registerWithCache(true);
    }

    @Nullable
    @SmartPtr
    public static GLRenderbuffer makeStencil(GLServer server,
                                             int width, int height,
                                             int sampleCount,
                                             int format) {
        assert sampleCount > 0 && glFormatStencilBits(format) > 0;
        int renderbuffer = glCreateRenderbuffers();
        if (renderbuffer == 0) {
            return null;
        }
        int internalFormat = glFormatToEnum(format);
        if (server.mCaps.skipErrorChecks()) {
            // GL has a concept of MSAA rasterization with a single sample, but we do not.
            if (sampleCount > 1) {
                glNamedRenderbufferStorageMultisample(renderbuffer, sampleCount, internalFormat, width, height);
            } else {
                // glNamedRenderbufferStorage is equivalent to calling glNamedRenderbufferStorageMultisample
                // with the samples set to zero. But we don't think sampleCount=1 is multisampled.
                glNamedRenderbufferStorage(renderbuffer, internalFormat, width, height);
            }
        } else {
            glClearErrors();
            if (sampleCount > 1) {
                glNamedRenderbufferStorageMultisample(renderbuffer, sampleCount, internalFormat, width, height);
            } else {
                glNamedRenderbufferStorage(renderbuffer, internalFormat, width, height);
            }
            if (glGetError() != GL_NO_ERROR) {
                glDeleteRenderbuffers(renderbuffer);
                return null;
            }
        }

        return new GLRenderbuffer(server, width, height, renderbuffer, sampleCount, format);
    }

    @Nullable
    @SmartPtr
    public static GLRenderbuffer makeMSAA(GLServer server,
                                          int width, int height,
                                          int sampleCount,
                                          int format) {
        assert sampleCount > 1;
        int renderbuffer = glCreateRenderbuffers();
        if (renderbuffer == 0) {
            return null;
        }
        int internalFormat = server.mCaps.getRenderbufferInternalFormat(format);
        if (server.mCaps.skipErrorChecks()) {
            glNamedRenderbufferStorageMultisample(renderbuffer, sampleCount, internalFormat, width, height);
        } else {
            glClearErrors();
            glNamedRenderbufferStorageMultisample(renderbuffer, sampleCount, internalFormat, width, height);
            if (glGetError() != GL_NO_ERROR) {
                glDeleteRenderbuffers(renderbuffer);
                return null;
            }
        }

        return new GLRenderbuffer(server, width, height, renderbuffer, sampleCount, format);
    }

    @Nonnull
    @SmartPtr
    public static GLRenderbuffer makeWrapped(GLServer server,
                                             int width, int height,
                                             int sampleCount,
                                             int format,
                                             int renderbuffer) {
        assert sampleCount > 0;
        return new GLRenderbuffer(server, width, height, renderbuffer, sampleCount, format);
    }

    @Override
    public int getSampleCount() {
        return mSampleCount;
    }

    @Override
    public boolean isMipmapped() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        if (mBackendFormat == null) {
            mBackendFormat = new GLBackendFormat(glFormatToEnum(mFormat), Types.TEXTURE_TYPE_NONE);
        }
        return mBackendFormat;
    }

    public int getRenderbuffer() {
        return mRenderbuffer;
    }

    public int getFormat() {
        return mFormat;
    }

    @Override
    public long getMemorySize() {
        return mMemorySize;
    }

    @Override
    protected void onFree() {
        if (mRenderbuffer != 0) {
            glDeleteRenderbuffers(mRenderbuffer);
        }
        mRenderbuffer = 0;
    }

    @Override
    protected void onDrop() {
        mRenderbuffer = 0;
    }
}
