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

import icyllis.arc3d.core.Kernel32;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;
import org.lwjgl.opengl.EXTMemoryObject;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

import javax.annotation.Nonnull;

import static icyllis.arc3d.engine.Engine.IOType;
import static org.lwjgl.opengl.GL30C.*;

/**
 * Represents OpenGL textures and renderbuffers.
 */
public final class GLImage extends GpuImage {

    private GLImageInfo mInfo;
    private BackendFormat mFormat;
    private final GLBackendImage mBackendTexture;
    private final boolean mOwnership;

    private final long mMemorySize;

    // Constructor for instances created by ourselves.
    GLImage(GLDevice device,
            int width, int height,
            GLImageInfo info,
            BackendFormat format,
            int flags) {
        super(device, width, height);
        assert GLUtil.glFormatIsSupported(format.getGLFormat());
        mInfo = info;
        mFormat = format;
        mOwnership = true;

        assert info.handle != 0;
        mBackendTexture = new GLBackendImage(width, height, info,
                info.target != GL_RENDERBUFFER ? new GLTextureParameters() : null, format);

        if (GLUtil.glFormatIsCompressed(format.getGLFormat()) || format.isExternal()) {
            mFlags |= ISurface.FLAG_READ_ONLY;
        }
        if (info.levels > 1) {
            mFlags |= ISurface.FLAG_MIPMAPPED;
        }
        mFlags |= flags & (ISurface.FLAG_MEMORYLESS |
                ISurface.FLAG_TEXTURABLE |
                ISurface.FLAG_RENDERABLE |
                ISurface.FLAG_PROTECTED);

        mMemorySize = computeSize(format, width, height, info.samples, info.levels);
        registerWithCache((flags & ISurface.FLAG_BUDGETED) != 0);
    }

    // Constructor for instances wrapping backend objects.
    public GLImage(GLDevice device,
                   int width, int height,
                   GLImageInfo info,
                   GLTextureParameters params,
                   BackendFormat format,
                   int ioType,
                   boolean cacheable,
                   boolean ownership) {
        super(device, width, height);
        assert info.handle != 0;
        assert GLUtil.glFormatIsSupported(format.getGLFormat());
        mInfo = info;
        mOwnership = ownership;

        mBackendTexture = new GLBackendImage(width, height, info, params, format);

        // compressed formats always set 'ioType' to READ
        assert (ioType == IOType.kRead || format.isCompressed());
        if (ioType == IOType.kRead || format.isExternal()) {
            mFlags |= ISurface.FLAG_READ_ONLY;
        }
        if (info.levels > 1) {
            mFlags |= ISurface.FLAG_MIPMAPPED;
        }
        //TODO?
        if (info.target != GL_RENDERBUFFER) {
            mFlags |= ISurface.FLAG_TEXTURABLE;
        } else {
            mFlags |= ISurface.FLAG_RENDERABLE;
        }

        mMemorySize = computeSize(format, width, height, info.samples, info.levels);
        registerWithCacheWrapped(cacheable);
    }

    @Nonnull
    @SharedPtr
    public static GLImage makeWrappedRenderbuffer(GLDevice device,
                                                  int width, int height,
                                                  int sampleCount,
                                                  int format,
                                                  int renderbuffer) {
        GLImageInfo info = new GLImageInfo();
        info.target = GL_RENDERBUFFER;
        info.handle = renderbuffer;
        info.format = format;
        info.samples = sampleCount;
        return new GLImage(device,
                width, height,
                info,
                null,
                GLBackendFormat.make(format),
                IOType.kWrite,
                false,
                false);
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        return mFormat;
    }

    @Nonnull
    @Override
    public GLBackendImage getBackendTexture() {
        return mBackendTexture;
    }

    /**
     * Note: Null for renderbuffers.
     */
    public GLTextureParameters getParameters() {
        return mBackendTexture.mParams;
    }

    @Override
    public boolean isExternal() {
        return mBackendTexture.isExternal();
    }

    @Override
    public int getSampleCount() {
        return mInfo.samples;
    }

    public int getTarget() {
        return mInfo.target;
    }

    public int getHandle() {
        return mInfo.handle;
    }

    public int getGLFormat() {
        return getBackendFormat().getGLFormat();
    }

    @Override
    public int getMipLevelCount() {
        return mInfo.levels;
    }

    @Override
    public long getMemorySize() {
        return mMemorySize;
    }

    @Override
    protected void onSetLabel(@Nonnull String label) {
        if (getDevice().getCaps().hasDebugSupport()) {
            assert mInfo != null;
            if (label.isEmpty()) {
                getDevice().getGL().glObjectLabel(GL_TEXTURE, mInfo.handle, 0, MemoryUtil.NULL);
            } else {
                label = label.substring(0, Math.min(label.length(),
                        getDevice().getCaps().maxLabelLength()));
                getDevice().getGL().glObjectLabel(GL_TEXTURE, mInfo.handle, label);
            }
        }
    }

    @Override
    protected void onRelease() {
        final GLImageInfo info = mInfo;
        if (mOwnership) {
            if (info.handle != 0) {
                getDevice().getGL().glDeleteTextures(info.handle);
            }
            if (info.memoryObject != 0) {
                EXTMemoryObject.glDeleteMemoryObjectsEXT(info.memoryObject);
            }
            if (info.memoryHandle != -1) {
                if (Platform.get() == Platform.WINDOWS) {
                    Kernel32.CloseHandle(info.memoryHandle);
                } // Linux transfers the fd
            }
        }
        mInfo = null;
        super.onRelease();
    }

    @Override
    protected void onDiscard() {
        mInfo = null;
        super.onDiscard();
    }

    @Override
    protected GLDevice getDevice() {
        return (GLDevice) super.getDevice();
    }

    @Override
    public String toString() {
        return "GLImage{" +
                "mWidth=" + mWidth +
                ", mHeight=" + mHeight +
                ", mInfo=" + mInfo +
                ", mDestroyed=" + isDestroyed() +
                ", mOwnership=" + mOwnership +
                ", mLabel=" + getLabel() +
                ", mMemorySize=" + mMemorySize +
                '}';
    }
}
