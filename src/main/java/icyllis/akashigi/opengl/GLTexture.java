/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.opengl;

import icyllis.akashigi.core.*;
import icyllis.akashigi.engine.*;
import org.lwjgl.opengl.EXTMemoryObject;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

import javax.annotation.Nonnull;
import java.util.function.Function;

import static icyllis.akashigi.engine.Engine.*;
import static icyllis.akashigi.opengl.GLCore.*;

/**
 * Represents OpenGL 2D textures.
 */
public final class GLTexture extends Texture {

    private GLTextureInfo mInfo;
    private final GLBackendTexture mBackendTexture;
    private final boolean mOwnership;

    @SharedPtr
    private GLRenderTarget mRenderTarget;

    private final long mMemorySize;

    // Constructor for instances created by ourselves.
    GLTexture(GLServer server,
              int width, int height,
              GLTextureInfo info,
              BackendFormat format,
              boolean budgeted,
              Function<GLTexture, GLRenderTarget> target) {
        super(server, width, height);
        assert info.mTexture != 0;
        assert glFormatIsSupported(format.getGLFormat());
        mInfo = info;
        mBackendTexture = new GLBackendTexture(width, height, info, new GLTextureParameters(), format);
        mOwnership = true;

        if (glFormatIsCompressed(format.getGLFormat()) || format.isExternal()) {
            mFlags |= SurfaceFlags.kReadOnly;
        }
        if (mBackendTexture.isMipmapped()) {
            mFlags |= SurfaceFlags.kMipmapped;
        }
        if (target != null) {
            mRenderTarget = target.apply(this);
            mFlags |= SurfaceFlags.kRenderable;
        }

        mMemorySize = computeSize(format, width, height, 1, info.mLevelCount);
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
        assert glFormatIsSupported(format.getGLFormat());
        mInfo = info;
        mBackendTexture = new GLBackendTexture(width, height, info, params, format);
        mOwnership = ownership;

        // compressed formats always set 'ioType' to READ
        assert (ioType == IOType.kRead || format.isCompressed());
        if (ioType == IOType.kRead || format.isExternal()) {
            mFlags |= SurfaceFlags.kReadOnly;
        }
        if (mBackendTexture.isMipmapped()) {
            mFlags |= SurfaceFlags.kMipmapped;
        }

        mMemorySize = computeSize(format, width, height, 1, info.mLevelCount);
        registerWithCacheWrapped(cacheable);
    }

    @Override
    public int getSampleCount() {
        return mRenderTarget != null ? mRenderTarget.getSampleCount() : 1;
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        return mBackendTexture.getBackendFormat();
    }

    public int getTextureID() {
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
        return mInfo.mLevelCount - 1; // minus base level
    }

    @Override
    public long getMemorySize() {
        return mMemorySize;
    }

    @Override
    public GLRenderTarget getRenderTarget() {
        return mRenderTarget;
    }

    @Override
    protected void onSetLabel(@Nonnull String label) {
        assert mInfo != null;
        if (label.isEmpty()) {
            nglObjectLabel(GL_TEXTURE, mInfo.mTexture, 0, MemoryUtil.NULL);
        } else {
            label = label.substring(0, Math.min(label.length(),
                    getServer().getCaps().maxLabelLength()));
            glObjectLabel(GL_TEXTURE, mInfo.mTexture, label);
        }
    }

    @Override
    protected void onRelease() {
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
        mInfo = null;
        mRenderTarget = RefCnt.move(mRenderTarget);
        super.onRelease();
    }

    @Override
    protected void onDiscard() {
        mInfo = null;
        mRenderTarget = RefCnt.move(mRenderTarget);
        super.onDiscard();
    }

    @Override
    protected GLServer getServer() {
        return (GLServer) super.getServer();
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
