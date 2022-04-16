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

import icyllis.arcui.core.PriorityQueue;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenCustomHashMap;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Manages the lifetime of all {@link Resource} instances.
 * <p>
 * Resources may optionally have two types of keys:
 * <ol>
 *  <li> <b>Scratch key</b>.
 *      This is for resources whose allocations are cached but not their contents.
 *      Multiple resources can share the same scratch key. This is so a caller can have two
 *      resource instances with the same properties (e.g. multi-pass rendering that ping-pongs
 *      between two temporary surfaces). The scratch key is set at resource creation time and
 *      should never change. Resources need not have a scratch key.
 *  <li> <b>Unique key</b>.
 *      This key's meaning is specific to the domain that created the key. Only one
 *      resource may have a given unique key. The unique key can be set, cleared, or changed
 *      anytime after resource creation.
 * </ol>
 * <p>
 * A unique key always takes precedence over a shared key when a resource has both types of keys.
 * If a resource has neither key type then it will be deleted as soon as the last reference to it
 * is dropped.
 * <p>
 * This class is designed to be accessible only by the render thread.
 */
@NotThreadSafe
public final class ResourceCache implements AutoCloseable {

    private final int mContextID;

    private PriorityQueue<Resource> mCleanableQueue;

    // This map holds all resources that can be used as scratch resources.
    private Long2ObjectOpenCustomHashMap<Resource> mScratchMap;

    /**
     * Created by DirectContext.
     */
    ResourceCache(int contextID) {
        mContextID = contextID;
    }

    /**
     * @return unique ID of the owning Context
     */
    public int getContextID() {
        return mContextID;
    }

    /**
     * Sets the max server memory byte size of the cache.
     */
    public void setMaxBytes(int bytes) {
    }

    /**
     * Returns the number of cached resources.
     */
    public int getResourceCount() {
        return 0;
    }

    /**
     * Clean up resources to become under budget and processes resources with invalidated unique
     * keys.
     */
    public void clean() {
    }

    /**
     * Clean up unlocked resources as much as possible. If <code>scratchOnly</code> is true,
     * the cleanable resources containing persistent data are skipped. Otherwise, all cleanable
     * resources will be deleted.
     *
     * @param scratchOnly if true, only shared resources will be cleaned up
     */
    public void cleanUp(boolean scratchOnly) {
    }

    /**
     * Clean up unlocked resources not used since the passed point in time. <b>The time-base is
     * {@link System#currentTimeMillis()}.</b> There is an error of about tens of milliseconds.
     * If <code>scratchOnly</code> is true, the cleanable resources containing persistent
     * data are skipped. Otherwise, all cleanable resources older than <code>cleanUpTime</code>
     * will be deleted.
     *
     * @param cleanUpTime the resources older than this time will be cleaned up
     * @param scratchOnly if true, only shared resources will be cleaned up
     */
    public void cleanUp(long cleanUpTime, boolean scratchOnly) {
    }

    /**
     * If it's possible to clean up enough resources to get the provided amount of budget
     * headroom, do so and return true. If it's not possible, do nothing and return false.
     */
    public boolean makeRoom(int bytes) {
        return false;
    }

    @Override
    public void close() {
    }
}
