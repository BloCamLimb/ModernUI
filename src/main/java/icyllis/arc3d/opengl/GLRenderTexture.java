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

import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.BackendFormat;
import icyllis.arc3d.engine.Surface;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * OpenGL readback render target.
 */
public final class GLRenderTexture extends GLTexture {

    @SharedPtr
    private GLRenderTarget mRenderTarget;

    GLRenderTexture(GLServer server,
                    int width, int height,
                    GLTextureInfo info,
                    BackendFormat format,
                    boolean budgeted,
                    Function<GLTexture, GLRenderTarget> function) {
        super(server, width, height, info, format, budgeted, false);
        mRenderTarget = function.apply(this);
        mFlags |= Surface.FLAG_RENDERABLE;

        registerWithCache(budgeted);
    }

    @Override
    public int getSampleCount() {
        return mRenderTarget.getSampleCount();
    }

    @Nonnull
    @Override
    public GLRenderTarget asRenderTarget() {
        return mRenderTarget;
    }

    @Override
    protected void onRelease() {
        mRenderTarget = RefCnt.move(mRenderTarget);
        super.onRelease();
    }

    @Override
    protected void onDiscard() {
        mRenderTarget = RefCnt.move(mRenderTarget);
        super.onDiscard();
    }

    @Override
    protected ScratchKey computeScratchKey() {
        return new ScratchKey().compute(
                getBackendFormat(),
                mWidth, mHeight,
                getSampleCount(),
                mFlags); // budgeted flag is not included, this method is called only when budgeted
    }

    @Override
    public String toString() {
        return "GLRenderTexture{" +
                "mWidth=" + mWidth +
                ", mHeight=" + mHeight +
                ", mDestroyed=" + isDestroyed() +
                ", mLabel=" + getLabel() +
                ", mMemorySize=" + getMemorySize() +
                ", mRenderTarget=" + mRenderTarget +
                '}';
    }
}
