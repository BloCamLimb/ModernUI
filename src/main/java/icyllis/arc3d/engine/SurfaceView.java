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
 * Views a {@link SurfaceDelegate} in the pipeline.
 */
public class SurfaceView implements AutoCloseable {

    @SharedPtr
    SurfaceDelegate mDelegate;
    int mOrigin;
    short mSwizzle;

    public SurfaceView(@SharedPtr SurfaceDelegate delegate) {
        mDelegate = delegate; // std::move()
        mOrigin = SurfaceOrigin.kUpperLeft;
        mSwizzle = Swizzle.RGBA;
    }

    public SurfaceView(@SharedPtr SurfaceDelegate delegate, int origin, short swizzle) {
        mDelegate = delegate; // std::move()
        mOrigin = origin;
        mSwizzle = swizzle;
    }

    public int getWidth() {
        return mDelegate.getWidth();
    }

    public int getHeight() {
        return mDelegate.getHeight();
    }

    public boolean isMipmapped() {
        TextureDelegate delegate = mDelegate.asTexture();
        return delegate != null && delegate.isMipmapped();
    }

    /**
     * Returns smart pointer value (raw ptr).
     */
    public SurfaceDelegate getSurface() {
        return mDelegate;
    }

    /**
     * Returns a smart pointer (as if on the stack).
     */
    @SharedPtr
    public SurfaceDelegate refSurface() {
        mDelegate.ref();
        return mDelegate;
    }

    /**
     * This does not reset the origin or swizzle, so the view can still be used to access those
     * properties associated with the detached proxy.
     */
    @SharedPtr
    public SurfaceDelegate detachSurface() {
        // just like std::move(), R-value reference
        SurfaceDelegate surfaceDelegate = mDelegate;
        mDelegate = null;
        return surfaceDelegate;
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
        if (mDelegate != null) {
            mDelegate.unref();
        }
        mDelegate = null;
        mOrigin = SurfaceOrigin.kUpperLeft;
        mSwizzle = Swizzle.RGBA;
    }

    /**
     * Destructs this view.
     */
    @Override
    public void close() {
        if (mDelegate != null) {
            mDelegate.unref();
        }
        mDelegate = null;
    }
}
