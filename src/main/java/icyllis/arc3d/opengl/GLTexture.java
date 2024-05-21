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

import static icyllis.arc3d.engine.Engine.IOType;
import static org.lwjgl.opengl.GL30C.*;

/**
 * Represents OpenGL textures.
 */
public final class GLTexture extends GLImage {

    private int mHandle;
    private final boolean mOwnership;

    private final long mMemorySize;

    // Constructor for instances created by ourselves.
    GLTexture(GLDevice device,
              GLImageDesc desc,
              GLTextureMutableState mutableState,
              int handle,
              boolean budgeted) {
        super(device, desc, mutableState);
        assert GLUtil.glFormatIsSupported(desc.mFormat);
        mOwnership = true;

        mHandle = handle;

        if (GLUtil.glFormatIsCompressed(desc.mFormat)) {
            mFlags |= ISurface.FLAG_READ_ONLY;
        }

        mMemorySize = DataUtils.computeSize(desc);
        registerWithCache(budgeted);

        if (mHandle == 0) {
            device.recordRenderCall(dev -> {
                if (isDestroyed()) {
                    return;
                }
                mHandle = dev.createTexture(mDesc);
                if (mHandle == 0) {
                    makeBudgeted(false);
                }
            });
        }
    }

    // Constructor for instances wrapping backend objects.
    public GLTexture(GLDevice device,
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
    }

    @Nullable
    @SharedPtr
    public static GLTexture make(GLDevice device,
                                 GLImageDesc desc,
                                 boolean budgeted) {
        final int handle;
        if (device.isOnExecutingThread()) {
            handle = device.createTexture(desc);
            if (handle == 0) {
                return null;
            }
        } else {
            handle = 0;
        }
        return new GLTexture(device, desc,
                new GLTextureMutableState(),
                handle,
                budgeted);
    }

    public GLTextureMutableState getGLMutableState() {
        return (GLTextureMutableState) getMutableState();
    }

    public int getHandle() {
        return mHandle;
    }

    public int getTarget() {
        return mDesc.mTarget;
    }

    public int getGLFormat() {
        return mDesc.mFormat;
    }

    @Override
    public long getMemorySize() {
        return mMemorySize;
    }

    @Override
    protected void onSetLabel(@Nonnull String label) {
        if (getDevice().getCaps().hasDebugSupport()) {
            assert mDesc != null;
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
        return "GLTexture{" +
                "mDesc=" + mDesc +
                ", mHandle=" + mHandle +
                ", mDestroyed=" + isDestroyed() +
                ", mOwnership=" + mOwnership +
                ", mLabel=" + getLabel() +
                ", mMemorySize=" + mMemorySize +
                '}';
    }
}
