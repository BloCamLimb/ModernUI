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

import javax.annotation.Nullable;

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

        if (mHandle == 0) {
            getDevice().recordRenderCall(device -> {
                if (isDestroyed()) {
                    return;
                }
                mHandle = device.createTexture(getGLDesc());
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
            handle = device.createTexture(desc);
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

    public GLTextureMutableState getGLMutableState() {
        return (GLTextureMutableState) getMutableState();
    }

    public int getHandle() {
        return mHandle;
    }

    public int getTarget() {
        return getGLDesc().mTarget;
    }

    public int getFormat() {
        return getGLDesc().mFormat;
    }

    @Override
    protected void onSetLabel(@Nullable String label) {
        if (getDevice().getCaps().hasDebugSupport()) {
            assert mHandle != 0;
            if (label == null) {
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
