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

import icyllis.arc3d.engine.*;
import icyllis.modernui.core.Kernel32;
import org.lwjgl.opengl.EXTMemoryObject;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

import javax.annotation.Nonnull;

import static icyllis.arc3d.opengl.GLCore.*;

/**
 * Represents OpenGL 2D textures.
 */
public class GLTexture extends Texture {

    private GLTextureInfo mInfo;
    private final GLBackendTexture mBackendTexture;
    private final boolean mOwnership;

    private final long mMemorySize;

    // Constructor for instances created by ourselves.
    GLTexture(GLDevice device,
              int width, int height,
              GLTextureInfo info,
              BackendFormat format,
              boolean budgeted,
              boolean register) {
        super(device, width, height);
        assert info.texture != 0;
        assert GLCore.glFormatIsSupported(format.getGLFormat());
        mInfo = info;
        mBackendTexture = new GLBackendTexture(width, height, info, new GLTextureParameters(), format);
        mOwnership = true;

        if (GLCore.glFormatIsCompressed(format.getGLFormat()) || format.isExternal()) {
            mFlags |= Surface.FLAG_READ_ONLY;
        }
        if (mBackendTexture.isMipmapped()) {
            mFlags |= Surface.FLAG_MIPMAPPED;
        }

        mMemorySize = computeSize(format, width, height, 1, info.levelCount);
        if (register) {
            registerWithCache(budgeted);
        }
    }

    // Constructor for instances wrapping backend objects.
    public GLTexture(GLDevice device,
                     int width, int height,
                     GLTextureInfo info,
                     GLTextureParameters params,
                     BackendFormat format,
                     int ioType,
                     boolean cacheable,
                     boolean ownership) {
        super(device, width, height);
        assert info.texture != 0;
        assert GLCore.glFormatIsSupported(format.getGLFormat());
        mInfo = info;
        mBackendTexture = new GLBackendTexture(width, height, info, params, format);
        mOwnership = ownership;

        // compressed formats always set 'ioType' to READ
        assert (ioType == Engine.IOType.kRead || format.isCompressed());
        if (ioType == Engine.IOType.kRead || format.isExternal()) {
            mFlags |= FLAG_READ_ONLY;
        }
        if (mBackendTexture.isMipmapped()) {
            mFlags |= FLAG_MIPMAPPED;
        }

        mMemorySize = computeSize(format, width, height, 1, info.levelCount);
        registerWithCacheWrapped(cacheable);
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        return mBackendTexture.getBackendFormat();
    }

    public int getHandle() {
        return mInfo.texture;
    }

    public int getFormat() {
        return getBackendFormat().getGLFormat();
    }

    @Nonnull
    public GLTextureParameters getParameters() {
        return mBackendTexture.mParams;
    }

    @Override
    public boolean isExternal() {
        return mBackendTexture.isExternal();
    }

    @Nonnull
    @Override
    public BackendTexture getBackendTexture() {
        return mBackendTexture;
    }

    @Override
    public int getMaxMipmapLevel() {
        return mInfo.levelCount - 1; // minus base level
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
                nglObjectLabel(GL_TEXTURE, mInfo.texture, 0, MemoryUtil.NULL);
            } else {
                label = label.substring(0, Math.min(label.length(),
                        getDevice().getCaps().maxLabelLength()));
                glObjectLabel(GL_TEXTURE, mInfo.texture, label);
            }
        }
    }

    @Override
    protected void onRelease() {
        final GLTextureInfo info = mInfo;
        if (mOwnership) {
            if (info.texture != 0) {
                glDeleteTextures(info.texture);
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
        return "GLTexture{" +
                "mWidth=" + mWidth +
                ", mHeight=" + mHeight +
                ", mBackendTexture=" + mBackendTexture +
                ", mDestroyed=" + isDestroyed() +
                ", mOwnership=" + mOwnership +
                ", mLabel=" + getLabel() +
                ", mMemorySize=" + mMemorySize +
                '}';
    }
}
