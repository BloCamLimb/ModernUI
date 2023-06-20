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

import icyllis.modernui.graphics.RefCnt;
import icyllis.modernui.graphics.SharedPtr;
import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * OpenGL readback render target.
 */
public final class GLRenderTexture extends GLTexture implements RenderTarget {

    @SharedPtr
    private GLFramebufferSet mGLFramebufferSet;

    GLRenderTexture(GLDevice device,
                    int width, int height,
                    GLTextureInfo info,
                    BackendFormat format,
                    boolean budgeted,
                    Function<GLTexture, GLFramebufferSet> function) {
        super(device, width, height, info, format, budgeted, false);
        mGLFramebufferSet = function.apply(this);
        mFlags |= Surface.FLAG_RENDERABLE;

        registerWithCache(budgeted);
    }

    @Override
    public int getSampleCount() {
        return mGLFramebufferSet.getSampleCount();
    }

    @Nonnull
    @Override
    public GLFramebufferSet getFramebufferSet() {
        return mGLFramebufferSet;
    }

    @Override
    protected void onRelease() {
        mGLFramebufferSet = RefCnt.move(mGLFramebufferSet);
        super.onRelease();
    }

    @Override
    protected void onDiscard() {
        mGLFramebufferSet = RefCnt.move(mGLFramebufferSet);
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
                ", mFramebufferSet=" + mGLFramebufferSet +
                '}';
    }
}
