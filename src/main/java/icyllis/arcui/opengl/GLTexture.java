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

package icyllis.arcui.opengl;

import icyllis.arcui.core.Kernel32;
import icyllis.arcui.engine.*;
import org.lwjgl.opengl.EXTMemoryObject;
import org.lwjgl.system.Platform;

import javax.annotation.Nonnull;

import static icyllis.arcui.opengl.GLCore.*;

/**
 * Represents OpenGL 2D textures, can be used as textures and color attachments.
 */
public final class GLTexture extends Texture {

    private final GLTextureInfo mInfo;
    private final GLBackendTexture mBackendTexture;
    private final boolean mOwnership;

    private final long mMemorySize;

    public GLTexture(GLServer server,
                     int width, int height,
                     GLTextureInfo info,
                     BackendFormat format,
                     boolean budgeted,
                     boolean ownership) {
        super(server, width, height);
        assert info.mTexture != 0;
        assert format.getGLFormat() != GLTypes.FORMAT_UNKNOWN;
        mInfo = info;
        mBackendTexture = new GLBackendTexture(width, height, info, new GLTextureParameters(), format);
        mOwnership = ownership;

        if (glFormatIsCompressed(format.getGLFormat()) || format.getTextureType() == Types.TEXTURE_TYPE_EXTERNAL) {
            setReadOnly();
        }

        mMemorySize = computeSize(format, width, height, getSampleCount(), info.mLevelCount);
        registerWithCache(budgeted);
    }

    // Constructor for instances wrapping backend objects.
    public GLTexture(GLServer server,
                     int width, int height,
                     GLTextureInfo info,
                     GLTextureParameters params,
                     BackendFormat format,
                     int ioType,
                     boolean cacheable,
                     boolean ownership) {
        super(server, width, height);
        assert info.mTexture != 0;
        assert format.getGLFormat() != GLTypes.FORMAT_UNKNOWN;
        mInfo = info;
        mBackendTexture = new GLBackendTexture(width, height, info, params, format);
        mOwnership = ownership;

        // compressed formats always set 'ioType' to READ
        assert ioType == Types.IO_TYPE_READ || glFormatIsCompressed(format.getGLFormat());
        if (ioType == Types.IO_TYPE_READ || format.getTextureType() == Types.TEXTURE_TYPE_EXTERNAL) {
            setReadOnly();
        }

        mMemorySize = computeSize(format, width, height, getSampleCount(), info.mLevelCount);
        registerWithCacheWrapped(cacheable);
    }

    /**
     * We have no multisample textures, but multisample renderbuffers.
     */
    @Override
    public int getSampleCount() {
        return 1;
    }

    @Override
    public boolean isMipmapped() {
        return mBackendTexture.isMipmapped();
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        return mBackendTexture.getBackendFormat();
    }

    @Override
    public boolean isProtected() {
        return mBackendTexture.isProtected();
    }

    public int getTexture() {
        return mInfo.mTexture;
    }

    public int getFormat() {
        return getBackendFormat().getGLFormat();
    }

    @Nonnull
    public GLTextureParameters getParameters() {
        return mBackendTexture.mParams;
    }

    @Override
    public int getTextureType() {
        return mBackendTexture.getTextureType();
    }

    @Nonnull
    @Override
    public BackendTexture getBackendTexture() {
        return mBackendTexture;
    }

    @Override
    public int getMaxMipmapLevel() {
        return mInfo.mLevelCount - 1; // minus base level
    }

    @Override
    public long getMemorySize() {
        return mMemorySize;
    }

    @Override
    protected void onFree() {
        final GLTextureInfo info = mInfo;
        if (mOwnership) {
            if (info.mTexture != 0) {
                glDeleteTextures(info.mTexture);
            }
            if (info.mMemoryObject != 0) {
                EXTMemoryObject.glDeleteMemoryObjectsEXT(info.mMemoryObject);
            }
            if (info.mMemoryHandle != -1) {
                if (Platform.get() == Platform.WINDOWS) {
                    Kernel32.CloseHandle(info.mMemoryHandle);
                } // Linux transfers the fd
            }
        }
        info.mTexture = 0;
        info.mMemoryObject = 0;
        info.mMemoryHandle = -1;
        super.onFree();
    }

    @Override
    protected void onDrop() {
        final GLTextureInfo info = mInfo;
        info.mTexture = 0;
        info.mMemoryObject = 0;
        info.mMemoryHandle = -1;
        super.onDrop();
    }
}
