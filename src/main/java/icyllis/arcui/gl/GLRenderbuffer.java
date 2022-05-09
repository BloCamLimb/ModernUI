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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static icyllis.arcui.gl.GLCore.*;

/**
 * Renderbuffer can be only used as attachments of framebuffers as an optimization.
 * It's something like Vulkan <code>VK_IMAGE_LAYOUT_*_READ_ONLY_OPTIMAL</code>.
 * Renderbuffer can neither be accessed by shaders nor have mipmaps, but can be
 * multisampled.
 */
public final class GLRenderbuffer extends Surface {

    private final int mSampleCount;
    private final int mFormat;

    // may be zero for external stencil buffers associated with external render targets
    // (we don't require the client to give us the id, just tell us how many bits of stencil there are)
    private int mRenderbuffer;

    private BackendFormat mBackendFormat;

    private final long mMemorySize;

    private GLRenderbuffer(GLServer server, int width, int height,
                           int sampleCount, int format, int renderbuffer) {
        super(server, width, height);
        assert sampleCount > 0;
        mSampleCount = sampleCount;
        mFormat = format;
        mRenderbuffer = renderbuffer;

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
        assert glFormatStencilBits(format) > 0;
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
            clearErrors();
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

        return new GLRenderbuffer(server, width, height, sampleCount, format, renderbuffer);
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
            clearErrors();
            glNamedRenderbufferStorageMultisample(renderbuffer, sampleCount, internalFormat, width, height);
            if (glGetError() != GL_NO_ERROR) {
                glDeleteRenderbuffers(renderbuffer);
                return null;
            }
        }

        return new GLRenderbuffer(server, width, height, sampleCount, format, renderbuffer);
    }

    @Nonnull
    @SmartPtr
    public static GLRenderbuffer makeWrapped(GLServer server,
                                             int width, int height,
                                             int sampleCount,
                                             int format,
                                             int renderbuffer) {
        return new GLRenderbuffer(server, width, height, sampleCount, format, renderbuffer);
    }

    @Override
    public int getSampleCount() {
        return mSampleCount;
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        if (mBackendFormat == null) {
            mBackendFormat = new GLBackendFormat(glFormatToEnum(mFormat), GL_NONE);
        }
        return mBackendFormat;
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
    protected void onFree() {
        if (mRenderbuffer != 0) {
            glDeleteRenderbuffers(mRenderbuffer);
            mRenderbuffer = 0;
        }
    }

    @Override
    protected void onDrop() {
        mRenderbuffer = 0;
    }

    @Override
    protected Object computeScratchKey() {
        return computeScratchKey(getBackendFormat(), mWidth, mHeight, mSampleCount);
    }

    /**
     * Compute the ScratchKey as GLRenderbuffer resources.
     *
     * @return the new key
     */
    @Nonnull
    static Object computeScratchKey(BackendFormat format,
                                    int width, int height,
                                    int sampleCount) {
        assert width > 0 && height > 0;
        Key key = new Key();
        key.mWidth = width;
        key.mHeight = height;
        key.mFormat = format.getFormatKey();
        key.mFlags = sampleCount;
        return key;
    }

    private static final class Key {

        int mWidth;
        int mHeight;
        int mFormat;
        int mFlags;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            if (mWidth != key.mWidth) return false;
            if (mHeight != key.mHeight) return false;
            if (mFormat != key.mFormat) return false;
            return mFlags == key.mFlags;
        }

        @Override
        public int hashCode() {
            int result = mWidth;
            result = 31 * result + mHeight;
            result = 31 * result + mFormat;
            result = 31 * result + mFlags;
            return result;
        }
    }
}
