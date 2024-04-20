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
import javax.annotation.Nullable;

import static icyllis.arc3d.engine.Engine.IOType;
import static org.lwjgl.opengl.GL30C.*;

/**
 * Represents OpenGL textures and renderbuffers.
 */
public final class GLImage extends GpuImage {

    private int mHandle;
    private final boolean mOwnership;

    private final long mMemorySize;

    // Constructor for instances created by ourselves.
    GLImage(GLDevice device,
            GLImageInfo info,
            GLImageMutableState mutableState,
            int handle,
            boolean budgeted) {
        super(device, info, mutableState);
        assert GLUtil.glFormatIsSupported(info.mFormat);
        mOwnership = true;

        mHandle = handle;

        if (GLUtil.glFormatIsCompressed(info.mFormat)) {
            mFlags |= ISurface.FLAG_READ_ONLY;
        }

        mMemorySize = DataUtils.computeSize(info);
        registerWithCache(budgeted);

        if (mHandle == 0) {
            device.recordRenderCall(dev -> {
                if (isDestroyed()) {
                    return;
                }
                mHandle = dev.createImage(getInfo());
                if (mHandle == 0) {
                    makeBudgeted(false);
                }
            });
        }
    }

    // Constructor for instances wrapping backend objects.
    public GLImage(GLDevice device,
                   GLImageInfo info,
                   GLImageMutableState mutableState,
                   int handle,
                   int ioType,
                   boolean cacheable,
                   boolean ownership) {
        super(device, info, mutableState);
        assert handle != 0;
        assert GLUtil.glFormatIsSupported(info.mFormat);
        mOwnership = ownership;

        // compressed formats always set 'ioType' to READ
        assert (ioType == IOType.kRead || info.isCompressed());
        if (ioType == IOType.kRead) {
            mFlags |= ISurface.FLAG_READ_ONLY;
        }

        mMemorySize = DataUtils.computeSize(info);
        registerWithCacheWrapped(cacheable);
    }

    @Nullable
    @SharedPtr
    public static GLImage make(GLDevice device,
                               GLImageInfo info,
                               boolean budgeted) {
        final int handle;
        if (device.isOnExecutingThread()) {
            handle = device.createImage(info);
            if (handle == 0) {
                return null;
            }
        } else {
            handle = 0;
        }
        return new GLImage(device, info,
                info.mTarget != GL_RENDERBUFFER ? new GLImageMutableState() : null,
                handle,
                budgeted);
    }

    @Nonnull
    @SharedPtr
    public static GLImage makeWrappedRenderbuffer(GLDevice device,
                                                  int width, int height,
                                                  int sampleCount,
                                                  int format,
                                                  int renderbuffer) {
        GLImageInfo info = new GLImageInfo(GL_RENDERBUFFER, format,
                width, height, 1, 1, 1, sampleCount, ISurface.FLAG_RENDERABLE);
        return new GLImage(device,
                info,
                null,
                renderbuffer,
                IOType.kWrite,
                false,
                false);
    }

    @Nonnull
    @Override
    public GLImageInfo getInfo() {
        return (GLImageInfo) mInfo;
    }

    /**
     * Note: Null for renderbuffers.
     */
    @Override
    public GLImageMutableState getMutableState() {
        return (GLImageMutableState) mMutableState;
    }

    public int getHandle() {
        return mHandle;
    }

    public int getTarget() {
        return getInfo().mTarget;
    }

    public int getGLFormat() {
        return getInfo().mFormat;
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
                getDevice().getGL().glObjectLabel(GL_TEXTURE, mHandle, 0, MemoryUtil.NULL);
            } else {
                label = label.substring(0, Math.min(label.length(),
                        getDevice().getCaps().maxLabelLength()));
                getDevice().getGL().glObjectLabel(GL_TEXTURE, mHandle, label);
            }
        }
    }

    @Override
    protected void onRelease() {
        if (mOwnership) {
            if (mHandle != 0) {
                getDevice().getGL().glDeleteTextures(mHandle);
            }
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
        mHandle = 0;
        super.onRelease();
    }

    @Override
    protected void onDiscard() {
        mHandle = 0;
        super.onDiscard();
    }

    @Override
    protected GLDevice getDevice() {
        return (GLDevice) super.getDevice();
    }

    @Override
    public String toString() {
        return "GLImage{" +
                ", mInfo=" + mInfo +
                ", mHandle=" + mHandle +
                ", mDestroyed=" + isDestroyed() +
                ", mOwnership=" + mOwnership +
                ", mLabel=" + getLabel() +
                ", mMemorySize=" + mMemorySize +
                '}';
    }
}
