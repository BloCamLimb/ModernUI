/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;
import org.lwjgl.system.MemoryUtil;

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

    private GLRenderbuffer(Context context, GLImageDesc desc, int renderbuffer, boolean budgeted) {
        super(context, budgeted, false, desc, null);
        mRenderbuffer = renderbuffer;

        if (mRenderbuffer == 0) {
            getDevice().recordRenderCall(dev -> {
                if (isDestroyed()) {
                    return;
                }
                mRenderbuffer = internalCreateRenderbuffer(dev, getGLDesc());
                if (mRenderbuffer == 0) {
                    setNonCacheable();
                }
            });
        }
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
    public static GLRenderbuffer makeWrappedRenderbuffer(Context context,
                                                         int width, int height,
                                                         int sampleCount,
                                                         int format,
                                                         int renderbuffer) {
        GLImageDesc desc = new GLImageDesc(GL_RENDERBUFFER, format,
                width, height, 1, 1, 1, sampleCount, ISurface.FLAG_RENDERABLE);
        return new GLRenderbuffer(context,
                desc,
                renderbuffer,
                false); //TODO should be cacheable
    }

    public static GLRenderbuffer make(Context context,
                                      GLImageDesc desc,
                                      boolean budgeted) {
        final GLDevice device = (GLDevice) context.getDevice();
        final int handle;
        if (device.isOnExecutingThread()) {
            handle = internalCreateRenderbuffer(device, desc);
            if (handle == 0) {
                return null;
            }
        } else {
            handle = 0;
        }
        return new GLRenderbuffer(context, desc,
                handle,
                budgeted);
    }

    static int internalCreateRenderbuffer(GLDevice device, GLImageDesc desc) {
        assert desc.mTarget == GL_RENDERBUFFER;
        int width = desc.getWidth(), height = desc.getHeight();
         /*int internalFormat = glFormat;
            if (GLUtil.glFormatStencilBits(glFormat) == 0) {
                internalFormat = getCaps().getRenderbufferInternalFormat(glFormat);
            }*/
        int handle = internalCreateRenderbuffer(device, width, height, desc.getSampleCount(), desc.mFormat);
        if (handle != 0) {
            device.getStats().incImageCreates();
        }
        return handle;
    }

    static int internalCreateRenderbuffer(GLDevice device,
                                          int width, int height,
                                          int sampleCount, int internalFormat) {
        GLInterface gl = device.getGL();
        int renderbuffer = gl.glGenRenderbuffers();
        if (renderbuffer == 0) {
            return 0;
        }
        gl.glBindRenderbuffer(GL_RENDERBUFFER, renderbuffer);
        boolean checkError = !device.getCaps().skipErrorChecks();
        if (checkError) {
            device.clearErrors();
        }
        // GL has a concept of MSAA rasterization with a single sample, but we do not.
        if (sampleCount > 1) {
            gl.glRenderbufferStorageMultisample(GL_RENDERBUFFER, sampleCount, internalFormat, width, height);
        } else {
            // glRenderbufferStorage is equivalent to calling glRenderbufferStorageMultisample
            // with the samples set to zero. But we don't think sampleCount=1 is multisampled.
            gl.glRenderbufferStorage(GL_RENDERBUFFER, internalFormat, width, height);
        }
        if (checkError && device.getError() != GL_NO_ERROR) {
            gl.glDeleteRenderbuffers(renderbuffer);
            return 0;
        }

        return renderbuffer;
    }

    @Nonnull
    public BackendFormat getBackendFormat() {
        return mBackendFormat;
    }

    @Override
    protected void onSetLabel(@Nullable String label) {
        getDevice().executeRenderCall(dev -> {
            if (dev.getCaps().hasDebugSupport()) {
                assert mRenderbuffer != 0;
                if (label == null) {
                    dev.getGL().glObjectLabel(GL_RENDERBUFFER, mRenderbuffer, 0, MemoryUtil.NULL);
                } else {
                    String subLabel = label.substring(0, Math.min(label.length(),
                            dev.getCaps().maxLabelLength()));
                    dev.getGL().glObjectLabel(GL_RENDERBUFFER, mRenderbuffer, subLabel);
                }
            }
        });
    }

    @Override
    protected void onRelease() {
        getDevice().executeRenderCall(dev -> {
            if (mRenderbuffer != 0) {
                dev.getGL().glDeleteRenderbuffers(mRenderbuffer);
            }
            mRenderbuffer = 0;
        });
        super.onRelease();
    }

    public int getHandle() {
        return mRenderbuffer;
    }

    @Override
    public String toString() {
        return "GLRenderbuffer{" +
                "mRenderbuffer=" + mRenderbuffer +
                ", mMemorySize=" + getMemorySize() +
                '}';
    }
}
