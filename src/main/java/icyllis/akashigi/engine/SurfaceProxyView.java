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

package icyllis.akashigi.engine;

import icyllis.akashigi.core.SharedPtr;

import static icyllis.akashigi.engine.Engine.SurfaceOrigin;

/**
 * Views a {@link SurfaceProxy} in the pipeline.
 */
public final class SurfaceProxyView implements AutoCloseable {

    @SharedPtr
    SurfaceProxy mProxy;
    int mOrigin;
    short mSwizzle;

    public SurfaceProxyView(@SharedPtr SurfaceProxy proxy) {
        mProxy = proxy; // std::move()
        mOrigin = SurfaceOrigin.kUpperLeft;
        mSwizzle = Swizzle.RGBA;
    }

    public SurfaceProxyView(@SharedPtr SurfaceProxy proxy, int origin, short swizzle) {
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
        TextureProxy textureProxy = mProxy.asTextureProxy();
        return textureProxy != null && textureProxy.isMipmapped();
    }

    /**
     * Returns smart pointer value (raw ptr).
     */
    public SurfaceProxy getProxy() {
        return mProxy;
    }

    /**
     * Returns a smart pointer (as if on the stack).
     */
    @SharedPtr
    public SurfaceProxy refProxy() {
        mProxy.ref();
        return mProxy;
    }

    /**
     * This does not reset the origin or swizzle, so the view can still be used to access those
     * properties associated with the detached proxy.
     */
    @SharedPtr
    public SurfaceProxy detachProxy() {
        // just like std::move(), R-value reference
        SurfaceProxy proxy = mProxy;
        mProxy = null;
        return proxy;
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
     * Merge swizzle.
     */
    public void merge(short swizzle) {
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
}
