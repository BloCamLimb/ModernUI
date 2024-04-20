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

import icyllis.arc3d.engine.ImageInfo;
import icyllis.arc3d.engine.Engine;
import org.lwjgl.opengl.*;

/**
 * Types for interacting with GL resources created externally to pipeline. BackendObjects for GL
 * textures are really const GLTexture*. The {@link #mFormat} here should be a sized, internal format
 * for the texture. We use the sized format since the base internal formats are deprecated.
 * <p>
 * Note the target can be {@link GL30C#GL_RENDERBUFFER}.
 */
public final class GLImageInfo extends ImageInfo {

    /**
     * <code>GLenum</code> - image namespace
     */
    public final int mTarget;
    /**
     * <code>GLenum</code> - sized internal format
     */
    public final int mFormat;

    public GLImageInfo(int target, int format,
                       int width, int height,
                       int depth, int arraySize,
                       int mipLevelCount, int sampleCount,
                       int flags) {
        super(width, height, depth, arraySize, mipLevelCount, sampleCount, flags);
        mTarget = target;
        mFormat = format;
    }

    @Override
    public int getBackend() {
        return Engine.BackendApi.kOpenGL;
    }

    @Override
    public byte getImageType() {
        return switch (mTarget) {
            case GL30C.GL_TEXTURE_2D -> Engine.ImageType.k2D;
            case GL30C.GL_TEXTURE_2D_ARRAY -> Engine.ImageType.k2DArray;
            case GL30C.GL_TEXTURE_3D -> Engine.ImageType.k3D;
            case GL30C.GL_TEXTURE_CUBE_MAP -> Engine.ImageType.kCube;
            case GL40C.GL_TEXTURE_CUBE_MAP_ARRAY -> Engine.ImageType.kCubeArray;
            default -> Engine.ImageType.kNone;
        };
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @Override
    public int getGLFormat() {
        return mFormat;
    }

    @Override
    public int getChannelFlags() {
        return GLUtil.glFormatChannels(mFormat);
    }

    @Override
    public boolean isSRGB() {
        return GLUtil.glFormatIsSRGB(mFormat);
    }

    @Override
    public int getCompressionType() {
        return GLUtil.glFormatCompressionType(mFormat);
    }

    @Override
    public int getBytesPerBlock() {
        return GLUtil.glFormatBytesPerBlock(mFormat);
    }

    @Override
    public int getDepthBits() {
        return GLUtil.glFormatDepthBits(mFormat);
    }

    @Override
    public int getStencilBits() {
        return GLUtil.glFormatStencilBits(mFormat);
    }

    @Override
    public int hashCode() {
        int result = mTarget;
        result = 31 * result + mFormat;
        result = 31 * result + mWidth;
        result = 31 * result + mHeight;
        result = 31 * result + mDepth;
        result = 31 * result + mArraySize;
        result = 31 * result + mMipLevelCount;
        result = 31 * result + mSampleCount;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof GLImageInfo info) {
            return mTarget == info.mTarget &&
                    mFormat == info.mFormat &&
                    mWidth == info.mWidth &&
                    mHeight == info.mHeight &&
                    mDepth == info.mDepth &&
                    mArraySize == info.mArraySize &&
                    mMipLevelCount != info.mMipLevelCount &&
                    mSampleCount != info.mSampleCount;
        }
        return false;
    }

    @Override
    public String toString() {
        return '{' +
                "target=" + mTarget +
                ", format=" + GLUtil.glFormatName(mFormat) +
                ", width=" + mWidth +
                ", height=" + mHeight +
                ", depth=" + mDepth +
                ", arraySize=" + mArraySize +
                ", mipLevelCount=" + mMipLevelCount +
                ", sampleCount=" + mSampleCount +
                '}';
    }
}
