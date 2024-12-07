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
import icyllis.arc3d.engine.Context;
import icyllis.arc3d.engine.ISurface;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL30C.*;

/**
 * Represents OpenGL textures.
 */
public final class GLTexture extends GLImage {

    private volatile int mHandle;
    private final boolean mOwnership;

    // Constructor for instances created by ourselves.
    GLTexture(Context context,
              GLImageDesc desc,
              GLTextureMutableState mutableState,
              int handle,
              boolean budgeted) {
        super(context, budgeted, false, desc, mutableState);
        assert GLUtil.glFormatIsSupported(desc.mFormat);
        mOwnership = true;

        mHandle = handle;

        if (GLUtil.glFormatIsCompressed(desc.mFormat)) {
            mFlags |= ISurface.FLAG_READ_ONLY;
        }

        getGLMutableState().mMaxMipmapLevel = getMipLevelCount() - 1;

        if (mHandle == 0) {
            getDevice().recordRenderCall(dev -> {
                if (isDestroyed()) {
                    return;
                }
                mHandle = internalCreateTexture(dev, getGLDesc());
                if (mHandle == 0) {
                    setNonCacheable();
                }
            });
        }
    }

    // Constructor for instances wrapping backend objects.
    //TODO wrapping
    /*public GLTexture(GLDevice device,
                     GLImageDesc desc,
                     GLTextureMutableState mutableState,
                     int handle,
                     int ioType,
                     boolean cacheable,
                     boolean ownership) {
        super(device, desc, mutableState);
        assert handle != 0;
        assert GLUtil.glFormatIsSupported(desc.mFormat);
        mOwnership = ownership;

        // compressed formats always set 'ioType' to READ
        assert (ioType == IOType.kRead || desc.isCompressed());
        if (ioType == IOType.kRead) {
            mFlags |= ISurface.FLAG_READ_ONLY;
        }

        mMemorySize = DataUtils.computeSize(desc);
        registerWithCacheWrapped(cacheable);
    }*/

    @Nullable
    @SharedPtr
    public static GLTexture make(Context context,
                                 GLImageDesc desc,
                                 boolean budgeted) {
        final GLDevice device = (GLDevice) context.getDevice();
        final int handle;
        if (device.isOnExecutingThread()) {
            handle = internalCreateTexture(device, desc);
            if (handle == 0) {
                return null;
            }
        } else {
            handle = 0;
        }
        return new GLTexture(context, desc,
                new GLTextureMutableState(),
                handle,
                budgeted);
    }

    static int internalCreateTexture(GLDevice device, GLImageDesc desc) {
        assert desc.mTarget != GL_RENDERBUFFER;
        int width = desc.getWidth(), height = desc.getHeight();
        //TODO create textures other than 2D
        int handle;
        handle = internalCreateTexture2D(device,
                width, height, desc.mFormat, desc.getMipLevelCount());
        if (handle != 0) {
            device.getStats().incImageCreates();
            if (desc.isSampledImage()) {
                device.getStats().incTextureCreates();
            }
        }
        return handle;
    }

    static int internalCreateTexture2D(GLDevice device,
                                       int width, int height,
                                       int format, int levels) {
        assert (GLUtil.glFormatIsSupported(format));
        assert (!GLUtil.glFormatIsCompressed(format));
        GLCaps caps = device.getCaps();

        int internalFormat = caps.getTextureInternalFormat(format);
        if (internalFormat == 0) {
            return 0;
        }

        GLInterface gl = device.getGL();

        assert (caps.isFormatTexturable(format));
        int texture;
        // It is known that using DSA on some older drivers can cause texture completeness
        // validation issues, but it's not easy to determine which drivers have problems.
        // So we only use DSA for texture creation on NVIDIA cards, considering that it uses
        // a threaded driver, and querying the currently bound texture can be slow.
        if (caps.hasDSASupport() && caps.getVendor() == GLUtil.GLVendor.NVIDIA) {
            assert (caps.isTextureStorageCompatible(format));
            texture = gl.glCreateTextures(GL_TEXTURE_2D);
            if (texture == 0) {
                return 0;
            }
            gl.glTextureParameteri(texture, GL_TEXTURE_MAX_LEVEL, levels - 1);
            if (caps.skipErrorChecks()) {
                gl.glTextureStorage2D(texture, levels, internalFormat, width, height);
            } else {
                device.clearErrors();
                gl.glTextureStorage2D(texture, levels, internalFormat, width, height);
                if (device.getError() != GL_NO_ERROR) {
                    gl.glDeleteTextures(texture);
                    return 0;
                }
            }
        } else {
            texture = device.getGL().glGenTextures();
            if (texture == 0) {
                return 0;
            }
            int boundTexture = gl.glGetInteger(GL_TEXTURE_BINDING_2D);
            gl.glBindTexture(GL_TEXTURE_2D, texture);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, levels - 1);
            if (caps.isTextureStorageCompatible(format)) {
                if (caps.skipErrorChecks()) {
                    gl.glTexStorage2D(GL_TEXTURE_2D, levels, internalFormat, width, height);
                } else {
                    device.clearErrors();
                    gl.glTexStorage2D(GL_TEXTURE_2D, levels, internalFormat, width, height);
                    if (device.getError() != GL_NO_ERROR) {
                        gl.glDeleteTextures(texture);
                        texture = 0;
                    }
                }
            } else {
                int error = GL_NO_ERROR;
                final boolean checkError = !caps.skipErrorChecks();
                if (checkError) {
                    device.clearErrors();
                }
                final int externalFormat = caps.getFormatDefaultExternalFormat(format);
                final int externalType = caps.getFormatDefaultExternalType(format);
                for (int level = 0; level < levels; level++) {
                    int currentWidth = Math.max(1, width >> level);
                    int currentHeight = Math.max(1, height >> level);
                    gl.glTexImage2D(GL_TEXTURE_2D, level, internalFormat,
                            currentWidth, currentHeight,
                            0, externalFormat, externalType, MemoryUtil.NULL);
                    if (checkError) {
                        error |= device.getError();
                    }
                }
                if (error != GL_NO_ERROR) {
                    gl.glDeleteTextures(texture);
                    texture = 0;
                }
            }
            gl.glBindTexture(GL_TEXTURE_2D, boundTexture);
        }

        return texture;
    }

    public GLTextureMutableState getGLMutableState() {
        return (GLTextureMutableState) getMutableState();
    }

    public int getHandle() {
        return mHandle;
    }

    @Override
    protected void onSetLabel(@Nullable String label) {
        getDevice().executeRenderCall(dev -> {
            if (dev.getCaps().hasDebugSupport()) {
                assert mHandle != 0;
                if (label == null) {
                    dev.getGL().glObjectLabel(GL_TEXTURE, mHandle, 0, MemoryUtil.NULL);
                } else {
                    String subLabel = "Arc3D_TEX_" + label;
                    subLabel = subLabel.substring(0, Math.min(subLabel.length(),
                            dev.getCaps().maxLabelLength()));
                    dev.getGL().glObjectLabel(GL_TEXTURE, mHandle, subLabel);
                }
            }
        });
    }

    @Override
    protected void onRelease() {
        if (mOwnership) {
            getDevice().executeRenderCall(dev -> {
                if (mHandle != 0) {
                    dev.getGL().glDeleteTextures(mHandle);
                }
                mHandle = 0;
            });
            //TODO?
            /*if (info.memoryObject != 0) {
                EXTMemoryObject.glDeleteMemoryObjectsEXT(info.memoryObject);
            }
            if (info.memoryHandle != -1) {
                if (Platform.get() == Platform.WINDOWS) {
                    Kernel32.CloseHandle(info.memoryHandle);
                } // Linux transfers the fd
            }*/
        }
        super.onRelease();
    }

    @Override
    public String toString() {
        return "GLTexture{" +
                "mDesc=" + getDesc() +
                ", mHandle=" + mHandle +
                ", mDestroyed=" + isDestroyed() +
                ", mOwnership=" + mOwnership +
                ", mLabel=" + getLabel() +
                ", mMemorySize=" + getMemorySize() +
                '}';
    }
}
