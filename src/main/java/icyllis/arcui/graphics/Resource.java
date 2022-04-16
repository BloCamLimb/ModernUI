/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.graphics;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for operating 3D API objects that can be kept in the ResourceCache.
 * Since all instances will always be strong referenced, an explicit ref/unref is
 * required to decide whether to recycle them or not.
 */
@NotThreadSafe
public abstract class Resource extends IORef {

    private static final AtomicInteger sNextID = new AtomicInteger(1);

    private Server mServer;
    private final int mUniqueID;

    // pointers to native data
    private long mScratchKey;
    private long mUniqueKey;

    public Resource(Server server) {
        mServer = server;
        mUniqueID = sNextID.getAndIncrement();
    }

    /**
     * Tests whether an object has been abandoned or released. All objects will
     * be in this state after their creating Context is destroyed or has
     * contextLost called. It's up to the client to test isDestroyed() before
     * attempting to use an object if it holds refs on objects across
     * Context.close(), freeResources with the force flag, or contextLost.
     *
     * @return true if the object has been released or abandoned,
     * false otherwise.
     */
    public final boolean isDestroyed() {
        return mServer == null;
    }

    /**
     * Retrieves the context that owns the object. Note that it is possible for
     * this to return NULL. When objects have been release()ed or abandon()ed
     * they no longer have an owning context. Destroying a DirectContext
     * automatically releases all its resources.
     */
    @Nullable
    public final DirectContext getContext() {
        return mServer != null ? mServer.getContext() : null;
    }

    /**
     * Retrieves the amount of server memory used by this resource in bytes. It is
     * approximate since we aren't aware of additional padding or copies made
     * by the driver.
     *
     * @return the amount of server memory used in bytes
     */
    public abstract int getMemorySize();

    /**
     * Gets an id that is unique for this Resource object. It is static in that it does
     * not change when the content of the Resource object changes. This will never return 0.
     */
    public final int getUniqueID() {
        return mUniqueID;
    }
}
