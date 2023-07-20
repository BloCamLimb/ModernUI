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
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * OpenGL readback render target.
 */
public final class GLRenderTexture extends GLTexture {

    @SharedPtr
    private GLRenderTarget mRenderTarget;

    GLRenderTexture(GLEngine engine,
                    int width, int height,
                    GLTextureInfo info,
                    BackendFormat format,
                    boolean budgeted,
                    Function<GLTexture, GLRenderTarget> function) {
        super(engine, width, height, info, format, budgeted, false);
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
    public GLRenderTarget getRenderTarget() {
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
