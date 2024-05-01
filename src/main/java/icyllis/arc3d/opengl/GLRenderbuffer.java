/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.lwjgl.opengl.GL11C.GL_NO_ERROR;
import static org.lwjgl.opengl.GL30C.GL_RENDERBUFFER;

/**
 * Represents OpenGL renderbuffers.
 * <p>
 * Renderbuffer can be only used as attachments of framebuffers as an optimization.
 * Renderbuffer can neither be accessed by shaders nor have mipmaps, but can be
 * multisampled.
 */
public final class GLRenderbuffer extends GLImage {

    // may be zero for external stencil buffers associated with external render targets
    // (we don't require the client to give us the id, just tell us how many bits of stencil there are)
    private int mRenderbuffer;

    private BackendFormat mBackendFormat;

    private final long mMemorySize;

    private GLRenderbuffer(GLDevice device, GLImageDesc desc, int renderbuffer, boolean budgeted) {
        super(device, desc, null);
        mRenderbuffer = renderbuffer;

        // color buffers may be compressed
        mMemorySize = DataUtils.computeSize(desc);
        registerWithCache(budgeted);
    }

    @Nullable
    @SharedPtr
    public static GLRenderbuffer makeStencil(GLDevice device,
                                             int width, int height,
                                             int sampleCount,
                                             int format) {
        assert sampleCount > 0 && GLUtil.glFormatStencilBits(format) > 0;

        int renderbuffer = device.getGL().glGenRenderbuffers();
        if (renderbuffer == 0) {
            return null;
        }
        device.getGL().glBindRenderbuffer(GL_RENDERBUFFER, renderbuffer);
        if (device.getCaps().skipErrorChecks()) {
            // GL has a concept of MSAA rasterization with a single sample, but we do not.
            if (sampleCount > 1) {
                device.getGL().glRenderbufferStorageMultisample(GL_RENDERBUFFER, sampleCount, format, width, height);
            } else {
                // glNamedRenderbufferStorage is equivalent to calling glNamedRenderbufferStorageMultisample
                // with the samples set to zero. But we don't think sampleCount=1 is multisampled.
                device.getGL().glRenderbufferStorage(GL_RENDERBUFFER, format, width, height);
            }
        } else {
            device.clearErrors();
            if (sampleCount > 1) {
                device.getGL().glRenderbufferStorageMultisample(GL_RENDERBUFFER, sampleCount, format, width, height);
            } else {
                device.getGL().glRenderbufferStorage(GL_RENDERBUFFER, format, width, height);
            }
            if (device.getError() != GL_NO_ERROR) {
                device.getGL().glDeleteRenderbuffers(renderbuffer);
                return null;
            }
        }

        //return new GLRenderbuffer(device, width, height, sampleCount, format, renderbuffer);
        return null;
    }

    @Nullable
    @SharedPtr
    public static GLRenderbuffer makeColor(GLDevice device,
                                           int width, int height,
                                           int sampleCount,
                                           int format) {
        assert sampleCount > 1;

        int renderbuffer = device.getGL().glGenRenderbuffers();
        if (renderbuffer == 0) {
            return null;
        }
        device.getGL().glBindRenderbuffer(GL_RENDERBUFFER, renderbuffer);
        int internalFormat = device.getCaps().getRenderbufferInternalFormat(format);
        if (device.getCaps().skipErrorChecks()) {
            device.getGL().glRenderbufferStorageMultisample(GL_RENDERBUFFER, sampleCount, internalFormat, width,
                    height);
        } else {
            device.clearErrors();
            device.getGL().glRenderbufferStorageMultisample(GL_RENDERBUFFER, sampleCount, internalFormat, width,
                    height);
            if (device.getError() != GL_NO_ERROR) {
                device.getGL().glDeleteRenderbuffers(renderbuffer);
                return null;
            }
        }

        //return new GLRenderbuffer(device, width, height, sampleCount, format, renderbuffer);
        return null;
    }

    @Nonnull
    @SharedPtr
    public static GLRenderbuffer makeWrapped(GLDevice device,
                                             int width, int height,
                                             int sampleCount,
                                             int format,
                                             int renderbuffer) {
        assert sampleCount > 0;
        //return new GLRenderbuffer(device, width, height, sampleCount, format, renderbuffer);
        return null;
    }

    @Nonnull
    @SharedPtr
    public static GLRenderbuffer makeWrappedRenderbuffer(GLDevice device,
                                                         int width, int height,
                                                         int sampleCount,
                                                         int format,
                                                         int renderbuffer) {
        GLImageDesc desc = new GLImageDesc(GL_RENDERBUFFER, format,
                width, height, 1, 1, 1, sampleCount, ISurface.FLAG_RENDERABLE);
        return new GLRenderbuffer(device,
                desc,
                renderbuffer,
                false); //TODO should be cacheable
    }

    public static GLRenderbuffer make(GLDevice device,
                                      GLImageDesc desc,
                                      boolean budgeted) {
        final int handle;
        if (device.isOnExecutingThread()) {
            handle = device.createRenderbuffer(desc);
            if (handle == 0) {
                return null;
            }
        } else {
            handle = 0;
        }
        return new GLRenderbuffer(device, desc,
                handle,
                budgeted);
    }

    @Nonnull
    public BackendFormat getBackendFormat() {
        return mBackendFormat;
    }

    public int getRenderbufferID() {
        return mRenderbuffer;
    }

    public long getMemorySize() {
        return mMemorySize;
    }

    @Override
    public String toString() {
        return "GLAttachment{" +
                "mRenderbuffer=" + mRenderbuffer +
                ", mMemorySize=" + mMemorySize +
                '}';
    }
}
