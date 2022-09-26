/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.engine;

import icyllis.arcticgi.core.RefCnt;
import icyllis.arcticgi.core.SharedPtr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static icyllis.arcticgi.engine.Engine.*;

/**
 * <code>SurfaceProxy</code> targets a Surface with three instantiation methods:
 * deferred, lazy-callback and wrapped. Note: the object itself is also used
 * as the scratch key, see {@link #hashCode()} and {@link #equals(Object)}
 * <p>
 * Target: The backing texture or render target that referenced by this proxy.
 * <p>
 * Instantiate: Create new Surfaces or find Surfaces in {@link ResourceCache}
 * when they are actually required on flush.
 * <p>
 * BackingFit: Indicates whether a backing store needs to be an exact match or
 * can be larger than is strictly necessary. True: Exact; False: Approx.
 * <p>
 * UseAllocator:
 * <ul>
 *     <li>False: This proxy will be instantiated outside the allocator (e.g.
 *     for proxies that are instantiated in on-flush callbacks).</li>
 *     <li>True: {@link ResourceAllocator} should instantiate this proxy.</li>
 * </ul>
 * LazyInstantiationKeyMode:
 * <ul>
 *     <li>False: Don't key the {@link Texture} with the proxy's key. The lazy
 *     instantiation callback is free to return a {@link Texture} that already
 *     has a unique key unrelated to the proxy's key.</li>
 *     <li>True: Keep the {@link Texture}'s unique key in sync with the proxy's
 *     unique key. The {@link Texture} returned from the lazy instantiation
 *     callback must not have a unique key or have the same same unique key as
 *     the proxy. If the proxy is later assigned a key it is in turn assigned
 *     to the {@link Texture}.</li>
 * </ul>
 * <p>
 * Use {@link ProxyProvider} to obtain {@link SurfaceProxy} objects.
 */
public abstract class SurfaceProxy extends RefCnt {

    /**
     * For wrapped resources, 'mFormat' and 'mDimensions' will always be filled in from the
     * wrapped resource.
     */
    final BackendFormat mFormat;
    int mWidth;
    int mHeight;

    /**
     * BackingFit: Indicates whether a backing store needs to be an exact match or can be
     * larger than is strictly necessary. Always approx for lazy-callback resources;
     * always exact for wrapped resources.
     * <p>
     * Budgeted: Always true for lazy-callback resources;
     * set from the backing resource for wrapped resources;
     * only meaningful if 'mLazyInstantiateCallback' is non-null.
     * <p>
     * UseAllocator:
     * <ul>
     *     <li>False: This proxy will be instantiated outside the allocator (e.g.
     *     for proxies that are instantiated in on-flush callbacks).</li>
     *     <li>True: {@link ResourceAllocator} should instantiate this proxy.</li>
     * </ul>
     * <p>
     * DeferredProvider: For TextureProxies created in a DDL recording thread it is possible
     * for the uniqueKey to be cleared on the backing Texture while the uniqueKey remains on
     * the proxy. A 'mDeferredProvider' of 'true' loosens up asserts that the key of an
     * instantiated uniquely-keyed textureProxy is also always set on the backing {@link Texture}.
     * <p>
     * In many cases these flags aren't actually known until the proxy has been instantiated.
     * However, Engine frequently needs to change its behavior based on these settings. For
     * internally create proxies we will know these properties ahead of time. For wrapped
     * proxies we will copy the properties off of the {@link Texture}. For lazy proxies we
     * force the call sites to provide the required information ahead of time. At
     * instantiation time we verify that the assumed properties match the actual properties.
     *
     * @see Engine#SurfaceFlag_Budgeted
     * @see Engine#SurfaceFlag_BackingFit
     * @see Engine#SurfaceFlag_SkipAllocator
     */
    protected int mSurfaceFlags;

    /**
     * Set from the backing resource for wrapped resources.
     */
    final Object mUniqueID;

    // Deferred version and lazy-callback version
    SurfaceProxy(BackendFormat format,
                 int width, int height,
                 int surfaceFlags) {
        assert (format != null);
        mFormat = format;
        mWidth = width;
        mHeight = height;
        mSurfaceFlags = surfaceFlags;
        if (format.getTextureType() == Engine.TextureType_External) {
            mSurfaceFlags |= Engine.SurfaceFlag_ReadOnly;
        }
        mUniqueID = this;
    }

    // Wrapped version
    SurfaceProxy(@SharedPtr Texture texture,
                 int surfaceFlags) {
        assert (texture != null);
        mFormat = texture.getBackendFormat();
        mWidth = texture.getWidth();
        mHeight = texture.getHeight();
        mSurfaceFlags = texture.getFlags() | surfaceFlags;
        assert (mSurfaceFlags & SurfaceFlag_BackingFit) != 0;
        assert mFormat.getTextureType() == texture.getTextureType();
        assert (texture.getBudgetType() == BudgetType_Budgeted) == ((mSurfaceFlags & SurfaceFlag_Budgeted) != 0);
        assert (texture.getTextureType() != TextureType_External) || ((mSurfaceFlags & SurfaceFlag_ReadOnly) != 0);
        mUniqueID = texture; // converting from unique resource ID to a proxy ID
    }

    public final int getWidth() {
        return mWidth;
    }

    public final int getHeight() {
        return mHeight;
    }

    /**
     * @return the backend format of this proxy
     */
    @Nonnull
    public final BackendFormat getBackendFormat() {
        return mFormat;
    }

    public final boolean isFormatCompressed() {
        return getBackendFormat().isCompressed();
    }

    /**
     * The contract for the unique ID is:
     * <ul>
     * <li>For wrapped resources:
     * the unique ID will match that of the wrapped resource</li>
     * <li>For deferred resources:
     *  <ul>
     *  <li>The unique ID will be different from the real resource, when it is allocated</li>
     *  <li>The proxy's unique ID will not change across the instantiates call</li>
     *  </ul>
     * </li>
     * <li> The unique IDs of the proxies and the resources draw from the same pool</li>
     * </ul>
     * What this boils down to is that the unique ID of a proxy can be used to consistently
     * track/identify a proxy but should never be used to distinguish between
     * resources and proxies - <b>beware!</b>
     */
    public final Object getUniqueID() {
        return mUniqueID;
    }

    public int getBackingWidth() {
        return 0;
    }

    public int getBackingHeight() {
        return 0;
    }

    public boolean isMipmapped() {
        return false;
    }

    @Nullable
    public Texture peekTexture() {
        return null;
    }

    @Nullable
    public RenderTarget peekRenderTarget() {
        return null;
    }

    @Nullable
    public TextureProxy asTextureProxy() {
        return null;
    }
}
