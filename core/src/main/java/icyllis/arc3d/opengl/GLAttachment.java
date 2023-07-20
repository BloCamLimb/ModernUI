/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static icyllis.arc3d.opengl.GLCore.*;

/**
 * Renderbuffer can be only used as attachments of framebuffers as an optimization.
 * Renderbuffer can neither be accessed by shaders nor have mipmaps, but can be
 * multisampled.
 */
public final class GLAttachment extends Attachment {

    // may be zero for external stencil buffers associated with external render targets
    // (we don't require the client to give us the id, just tell us how many bits of stencil there are)
    private int mRenderbuffer;

    private final int mFormat;

    private BackendFormat mBackendFormat;

    private final long mMemorySize;

    private GLAttachment(GLEngine engine, int width, int height,
                         int sampleCount, int format, int renderbuffer) {
        super(engine, width, height, sampleCount);
        mRenderbuffer = renderbuffer;
        mFormat = format;

        // color buffers may be compressed
        mMemorySize = DataUtils.numBlocks(GLCore.glFormatCompressionType(format), width, height) *
                GLCore.glFormatBytesPerBlock(format) * sampleCount;

        registerWithCache(true);
    }

    @Nullable
    @SharedPtr
    public static GLAttachment makeStencil(GLEngine engine,
                                           int width, int height,
                                           int sampleCount,
                                           int format) {
        assert sampleCount > 0 && GLCore.glFormatStencilBits(format) > 0;

        int renderbuffer = glGenRenderbuffers();
        if (renderbuffer == 0) {
            return null;
        }
        glBindRenderbuffer(GL_RENDERBUFFER, renderbuffer);
        if (engine.getCaps().skipErrorChecks()) {
            // GL has a concept of MSAA rasterization with a single sample, but we do not.
            if (sampleCount > 1) {
                glRenderbufferStorageMultisample(GL_RENDERBUFFER, sampleCount, format, width, height);
            } else {
                // glNamedRenderbufferStorage is equivalent to calling glNamedRenderbufferStorageMultisample
                // with the samples set to zero. But we don't think sampleCount=1 is multisampled.
                glRenderbufferStorage(GL_RENDERBUFFER, format, width, height);
            }
        } else {
            glClearErrors();
            if (sampleCount > 1) {
                glRenderbufferStorageMultisample(GL_RENDERBUFFER, sampleCount, format, width, height);
            } else {
                glRenderbufferStorage(GL_RENDERBUFFER, format, width, height);
            }
            if (glGetError() != GL_NO_ERROR) {
                glDeleteRenderbuffers(renderbuffer);
                return null;
            }
        }

        return new GLAttachment(engine, width, height, sampleCount, format, renderbuffer);
    }

    @Nullable
    @SharedPtr
    public static GLAttachment makeColor(GLEngine engine,
                                         int width, int height,
                                         int sampleCount,
                                         int format) {
        assert sampleCount > 1;

        int renderbuffer = glGenRenderbuffers();
        if (renderbuffer == 0) {
            return null;
        }
        glBindRenderbuffer(GL_RENDERBUFFER, renderbuffer);
        int internalFormat = engine.getCaps().getRenderbufferInternalFormat(format);
        if (engine.getCaps().skipErrorChecks()) {
            glRenderbufferStorageMultisample(GL_RENDERBUFFER, sampleCount, internalFormat, width, height);
        } else {
            glClearErrors();
            glRenderbufferStorageMultisample(GL_RENDERBUFFER, sampleCount, internalFormat, width, height);
            if (glGetError() != GL_NO_ERROR) {
                glDeleteRenderbuffers(renderbuffer);
                return null;
            }
        }

        return new GLAttachment(engine, width, height, sampleCount, format, renderbuffer);
    }

    @Nonnull
    @SharedPtr
    public static GLAttachment makeWrapped(GLEngine engine,
                                           int width, int height,
                                           int sampleCount,
                                           int format,
                                           int renderbuffer) {
        assert sampleCount > 0;
        return new GLAttachment(engine, width, height, sampleCount, format, renderbuffer);
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        if (mBackendFormat == null) {
            mBackendFormat = GLBackendFormat.make(mFormat);
        }
        return mBackendFormat;
    }

    public int getRenderbufferID() {
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
    protected void onRelease() {
        if (mRenderbuffer != 0) {
            glDeleteRenderbuffers(mRenderbuffer);
        }
        mRenderbuffer = 0;
    }

    @Override
    protected void onDiscard() {
        mRenderbuffer = 0;
    }

    @Override
    public String toString() {
        return "GLAttachment{" +
                "mRenderbuffer=" + mRenderbuffer +
                ", mFormat=" + glFormatName(mFormat) +
                ", mMemorySize=" + mMemorySize +
                ", mWidth=" + mWidth +
                ", mHeight=" + mHeight +
                ", mSampleCount=" + mSampleCount +
                '}';
    }
}
