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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.core.SharedPtr;

import static icyllis.arc3d.engine.Engine.SurfaceOrigin;

/**
 * Surface views contain additional metadata for pipeline operations on surfaces.
 * This class is a tuple of {@link SurfaceProxy}, SurfaceOrigin and Swizzle.
 */
public class ImageProxyView implements AutoCloseable {

    @SharedPtr
    ImageViewProxy mProxy;
    int mOrigin;
    short mSwizzle;

    public ImageProxyView(@SharedPtr ImageViewProxy proxy) {
        mProxy = proxy; // std::move()
        mOrigin = SurfaceOrigin.kUpperLeft;
        mSwizzle = Swizzle.RGBA;
    }

    public ImageProxyView(@SharedPtr ImageViewProxy proxy, int origin, short swizzle) {
        mProxy = proxy; // std::move()
        mOrigin = origin;
        mSwizzle = swizzle;
    }

    public int getWidth() {
        return mProxy.getWidth();
    }

    public int getHeight() {
        return mProxy.getHeight();
    }

    public boolean isMipmapped() {
        return mProxy.isMipmapped();
    }

    /**
     * Returns smart pointer value (raw ptr).
     */
    @RawPtr
    public ImageViewProxy getProxy() {
        return mProxy;
    }

    /**
     * Returns a smart pointer (as if on the stack).
     */
    @SharedPtr
    public ImageViewProxy refProxy() {
        mProxy.ref();
        return mProxy;
    }

    /**
     * This does not reset the origin or swizzle, so the view can still be used to access those
     * properties associated with the detached proxy.
     */
    @SharedPtr
    public ImageViewProxy detachProxy() {
        // just like std::move(), R-value reference
        ImageViewProxy surfaceProxy = mProxy;
        mProxy = null;
        return surfaceProxy;
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
        if (mProxy != null) {
            mProxy.unref();
        }
        mProxy = null;
        mOrigin = SurfaceOrigin.kUpperLeft;
        mSwizzle = Swizzle.RGBA;
    }

    /**
     * Destructs this view.
     */
    @Override
    public void close() {
        if (mProxy != null) {
            mProxy.unref();
        }
        mProxy = null;
    }

    /*@Override
    public int hashCode() {
        int result = mProxy != null ? mProxy.getUniqueID().hashCode() : 0;
        result = 31 * result + mOrigin;
        result = 31 * result + (int) mSwizzle;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageProxyView that = (ImageProxyView) o;
        if (mOrigin != that.mOrigin) return false;
        if (mSwizzle != that.mSwizzle) return false;
        return (mProxy == null && that.mProxy == null) ||
                (mProxy != null && that.mProxy != null && mProxy.getUniqueID() == that.mProxy.getUniqueID());
    }*/
}
