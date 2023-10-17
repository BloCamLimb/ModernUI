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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.SharedPtr;

import static icyllis.arc3d.engine.Engine.SurfaceOrigin;

/**
 * Views a {@link Surface} in the pipeline.
 */
public class SurfaceView implements AutoCloseable {

    @SharedPtr
    Surface mSurface;
    int mOrigin;
    short mSwizzle;

    public SurfaceView(@SharedPtr Surface surface) {
        mSurface = surface; // std::move()
        mOrigin = SurfaceOrigin.kUpperLeft;
        mSwizzle = Swizzle.RGBA;
    }

    public SurfaceView(@SharedPtr Surface surface, int origin, short swizzle) {
        mSurface = surface; // std::move()
        mOrigin = origin;
        mSwizzle = swizzle;
    }

    public int getWidth() {
        return mSurface.getWidth();
    }

    public int getHeight() {
        return mSurface.getHeight();
    }

    public boolean isMipmapped() {
        Texture textureProxy = mSurface.asTexture();
        return textureProxy != null && textureProxy.isMipmapped();
    }

    /**
     * Returns smart pointer value (raw ptr).
     */
    public Surface getSurface() {
        return mSurface;
    }

    /**
     * Returns a smart pointer (as if on the stack).
     */
    @SharedPtr
    public Surface refSurface() {
        mSurface.ref();
        return mSurface;
    }

    /**
     * This does not reset the origin or swizzle, so the view can still be used to access those
     * properties associated with the detached proxy.
     */
    @SharedPtr
    public Surface detachSurface() {
        // just like std::move(), R-value reference
        Surface surface = mSurface;
        mSurface = null;
        return surface;
    }

    /**
     * @see SurfaceOrigin
     */
    public int getOrigin() {
        return mOrigin;
    }

    /**
     * @see Swizzle
     */
    public short getSwizzle() {
        return mSwizzle;
    }

    /**
     * Concat swizzle.
     */
    public void concat(short swizzle) {
        mSwizzle = Swizzle.concat(mSwizzle, swizzle);
    }

    /**
     * Recycle this view.
     */
    public void reset() {
        if (mSurface != null) {
            mSurface.unref();
        }
        mSurface = null;
        mOrigin = SurfaceOrigin.kUpperLeft;
        mSwizzle = Swizzle.RGBA;
    }

    /**
     * Destructs this view.
     */
    @Override
    public void close() {
        if (mSurface != null) {
            mSurface.unref();
        }
        mSurface = null;
    }
}
