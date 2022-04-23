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

package icyllis.arcui.hgi;

import icyllis.arcui.core.PriorityQueue;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;

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
 */
@NotThreadSafe
public final class ResourceCache implements AutoCloseable {

    private static final Comparator<Resource> TIMESTAMP_COMPARATOR =
            Comparator.comparingInt(resource -> resource.mTimestamp);

    private ProxyProvider mProxyProvider = null;
    private ThreadSafeCache mThreadSafeCache = null;

    // Whenever a resource is added to the cache or the result of a cache lookup, fTimestamp is
    // assigned as the resource's timestamp and then incremented. mCleanableQueue orders the
    // cleanable resources by this value, and thus is used to purge resources in LRU order.
    private int mTimestamp = 0;
    private final PriorityQueue<Resource> mCleanableQueue;
    private Resource[] mNonCleanableList;
    private int mNonCleanableSize;

    // This map holds all resources that can be used as scratch resources.
    private final Object2ObjectOpenHashMap<ScratchKey, Resource> mScratchMap;
    // This map holds all resources that have unique keys.
    private final Object2ObjectOpenHashMap<UniqueKey, Resource> mUniqueMap;

    // our budget, used in clean()
    private long mMaxBytes = 1 << 28;

    // our current stats for all resources
    private int mCount = 0;
    private long mBytes = 0;

    // our current stats for resources that count against the budget
    private int mBudgetedCount = 0;
    private long mBudgetedBytes = 0;
    private long mCleanableBytes = 0;
    private int mFlushableCount = 0;

    private final int mContextID;

    /**
     * Created by DirectContext.
     */
    ResourceCache(int contextID) {
        mContextID = contextID;

        mCleanableQueue = new PriorityQueue<>(TIMESTAMP_COMPARATOR, Resource.INDEX_ACCESS);
        mNonCleanableList = new Resource[10]; // initial size must > 2

        mScratchMap = new Object2ObjectOpenHashMap<>();
        mUniqueMap = new Object2ObjectOpenHashMap<>();
    }

    /**
     * @return unique ID of the owning context
     */
    public int getContextID() {
        return mContextID;
    }

    /**
     * Sets the max server memory byte size of the cache.
     * A {@link #clean()} is followed by this method call.
     * The passed value can be retrieved by {@link #getMaxResourceBytes()}.
     */
    public void setCacheLimit(long bytes) {
        mMaxBytes = bytes;
        clean();
    }

    /**
     * Returns the number of cached resources.
     */
    public int getResourceCount() {
        return mCleanableQueue.size() + mNonCleanableSize;
    }

    /**
     * Returns the number of resources that count against the budget.
     */
    public int getBudgetedResourceCount() {
        return mBudgetedCount;
    }

    /**
     * Returns the number of bytes consumed by resources.
     */
    public long getResourceBytes() {
        return mBytes;
    }

    /**
     * Returns the number of bytes consumed by budgeted resources.
     */
    public long getBudgetedResourceBytes() {
        return mBudgetedBytes;
    }

    /**
     * Returns the number of bytes held by unlocked resources which are available for cleaning.
     */
    public long getCleanableResourceBytes() {
        return mCleanableBytes;
    }

    /**
     * Returns the number of bytes consumed by cached resources.
     * This can be changed by {@link #setCacheLimit(long)}.
     */
    public long getMaxResourceBytes() {
        return mMaxBytes;
    }

    /**
     * Releases the backend API resources owned by all Resource objects and removes them from
     * the cache.
     */
    public void releaseAll() {
        //fThreadSafeCache->dropAllRefs();

        //this->processFreedGpuResources();

        // We need to make sure to free any resources that were waiting on a free message but never
        // received one.
        //fTexturesAwaitingUnref.reset();

        //ASSERT(fProxyProvider); // better have called setProxyProvider
        //ASSERT(fThreadSafeCache); // better have called setThreadSafeCache too

        // We must remove the uniqueKeys from the proxies here. While they possess a uniqueKey
        // they also have a raw pointer back to this class (which is presumably going away)!
        //fProxyProvider->removeAllUniqueKeys();

        while (mNonCleanableSize > 0) {
            Resource back = mNonCleanableList[mNonCleanableSize - 1];
            assert !back.isDestroyed();
            back.release();
        }

        while (mCleanableQueue.size() > 0) {
            Resource top = mCleanableQueue.peek();
            assert !top.isDestroyed();
            top.release();
        }

        assert mScratchMap.isEmpty();
        assert mUniqueMap.isEmpty();
        assert mCount == 0;
        assert getResourceCount() == 0;
        assert mBytes == 0;
        assert mBudgetedCount == 0;
        assert mBudgetedBytes == 0;
        assert mCleanableBytes == 0;
    }

    /**
     * Discards the backend API resources owned by all Resource objects and removes them from
     * the cache.
     */
    public void discardAll() {
        while (mNonCleanableSize > 0) {
            Resource back = mNonCleanableList[mNonCleanableSize - 1];
            assert !back.isDestroyed();
            back.discard();
        }

        while (mCleanableQueue.size() > 0) {
            Resource top = mCleanableQueue.peek();
            assert !top.isDestroyed();
            top.discard();
        }

        //fThreadSafeCache -> dropAllRefs();

        assert mScratchMap.isEmpty();
        assert mUniqueMap.isEmpty();
        assert mCount == 0;
        assert getResourceCount() == 0;
        assert mBytes == 0;
        assert mBudgetedCount == 0;
        assert mBudgetedBytes == 0;
        assert mCleanableBytes == 0;
    }

    /**
     * Find a resource that matches a scratch key.
     */
    @Nullable
    public Resource findAndRefScratchResource(ScratchKey key) {
        assert key.isValid();

        Resource resource = mScratchMap.get(key);
        if (resource != null) {
            mScratchMap.remove(key, resource);
            refAndMakeResourceMRU(resource);
        }
        return resource;
    }

    /**
     * Find a resource that matches a unique key.
     */
    @Nullable
    public Resource findAndRefUniqueResource(UniqueKey key) {
        Resource resource = mUniqueMap.get(key);
        if (resource != null) {
            refAndMakeResourceMRU(resource);
        }
        return resource;
    }

    /**
     * Query whether a unique key exists in the cache.
     */
    public boolean hasUniqueKey(UniqueKey key) {
        return mUniqueMap.containsKey(key);
    }

    /**
     * Clean up resources to become under budget and processes resources with invalidated unique
     * keys.
     */
    public void clean() {
        //this->processFreedGpuResources();

        boolean stillOverrun = isOverrun();
        while (stillOverrun && mCleanableQueue.size() > 0) {
            Resource resource = mCleanableQueue.peek();
            assert resource.isCleanable();
            resource.release();
            stillOverrun = isOverrun();
        }

        if (stillOverrun) {
            //fThreadSafeCache -> dropUniqueRefs(this);

            stillOverrun = isOverrun();
            while (stillOverrun && mCleanableQueue.size() > 0) {
                Resource resource = mCleanableQueue.peek();
                assert resource.isCleanable();
                resource.release();
                stillOverrun = isOverrun();
            }
        }
    }

    /**
     * Clean up unlocked resources as much as possible. If <code>scratchOnly</code> is true,
     * the cleanable resources containing persistent data are skipped. Otherwise, all cleanable
     * resources will be deleted.
     *
     * @param scratchOnly if true, only shared resources will be cleaned up
     */
    public void cleanUp(boolean scratchOnly) {
        cleanUpTime(-1, scratchOnly);
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
    public void cleanUpTime(long cleanUpTime, boolean scratchOnly) {
        if (!scratchOnly) {
            if (cleanUpTime > 0) {
                //fThreadSafeCache->dropUniqueRefsOlderThan(*purgeTime);
            } else {
                //fThreadSafeCache->dropUniqueRefs(nullptr);
            }

            // We could disable maintaining the heap property here, but it would add a lot of
            // complexity. Moreover, this is rarely called.
            while (mCleanableQueue.size() > 0) {
                Resource resource = mCleanableQueue.peek();

                final long resourceTime = resource.getCleanUpTime();
                if (cleanUpTime > 0 && resourceTime >= cleanUpTime) {
                    // Resources were given both LRU timestamps and tagged with a frame number when
                    // they first became cleanable. The LRU timestamp won't change again until the
                    // resource is made non-cleanable again. So, at this point all the remaining
                    // resources in the timestamp-sorted queue will have a frame number >= to this
                    // one.
                    break;
                }

                assert resource.isCleanable();
                resource.release();
            }
        } else {
            // Early out if the very first item is too new to purge to avoid sorting the queue when
            // nothing will be deleted.
            if (cleanUpTime > 0 && mCleanableQueue.size() > 0 &&
                    mCleanableQueue.peek().getCleanUpTime() >= cleanUpTime) {
                return;
            }

            // Make a list of the scratch resources to delete
            ArrayList<Resource> scratchResources = new ArrayList<>();
            for (int i = 0; i < mCleanableQueue.size(); i++) {
                Resource resource = mCleanableQueue.get(i);

                final long resourceTime = resource.getCleanUpTime();
                if (cleanUpTime > 0 && resourceTime >= cleanUpTime) {
                    // scratch or not, all later iterations will be too recently used to purge.
                    break;
                }
                assert resource.isCleanable();
                if (resource.mUniqueKey == null || !resource.mUniqueKey.isValid()) {
                    scratchResources.add(resource);
                }
            }

            // Delete the scratch resources. This must be done as a separate pass
            // to avoid messing up the sorted order of the queue
            for (Resource scratchResource : scratchResources) {
                scratchResource.release();
            }
        }

        mCleanableQueue.trim();
        mScratchMap.trim();
        mUniqueMap.trim();
    }

    /**
     * Clean up unlocked resources from the cache until the provided byte count has been reached,
     * or we have cleaned up all unlocked resources. The default policy is to purge in LRU order,
     * but can be overridden to prefer cleaning up scratch resources (in LRU order) prior to
     * cleaning up other resource types.
     *
     * @param cleanUpBytes  the desired number of bytes to be purged
     * @param preferScratch uf true, scratch resources will be cleaned up prior to other resource types
     */
    public void cleanUpBytes(long cleanUpBytes, boolean preferScratch) {

    }

    /**
     * If it's possible to clean up enough resources to get the provided amount of budget
     * headroom, do so and return true. If it's not possible, do nothing and return false.
     */
    public boolean makeRoom(long bytes) {
        return false;
    }

    /**
     * Returns true if {@link #getBudgetedResourceBytes()} is greater than {@link #getMaxResourceBytes()}.
     */
    public boolean isOverrun() {
        return mBudgetedBytes > mMaxBytes;
    }

    /**
     * Returns true if the cache would like a flush to occur in order to make more resources
     * cleanable.
     */
    public boolean isFlushNeeded() {
        return isOverrun() && mCleanableQueue.isEmpty() && mFlushableCount > 0;
    }

    void notifyACntReachedZero(Resource resource, boolean commandBufferUsage) {
        assert !resource.isDestroyed();
        assert isInCache(resource);
        // This resource should always be in the non-cleanable array when this function is called. It
        // will be moved to the queue if it is newly cleanable.
        assert mNonCleanableList[resource.mCacheIndex] == resource;

        if (!commandBufferUsage) {
            if (resource.isUsableAsScratch()) {
                mScratchMap.put(resource.mScratchKey, resource);
            }
        }

        if (resource.hasRefOrCommandBufferUsage()) {
            return;
        }

        resource.mTimestamp = getNextTimestamp();

        if (!resource.isCleanable() &&
                resource.getBudgetType() == Types.BUDGET_TYPE_COMPLETE) {
            mFlushableCount++;
        }

        if (!resource.isCleanable()) {
            return;
        }

        removeFromNonCleanableArray(resource);
        mCleanableQueue.add(resource);
        resource.setCleanUpTime();
        mCleanableBytes += resource.getMemorySize();

        boolean hasUniqueKey = resource.mUniqueKey != null && resource.mUniqueKey.isValid();

        int budgetedType = resource.getBudgetType();

        if (budgetedType == Types.BUDGET_TYPE_COMPLETE) {
            // Purge the resource immediately if we're over budget
            // Also purge if the resource has neither a valid scratch key nor a unique key.
            boolean hasKey = hasUniqueKey ||
                    (resource.mScratchKey != null && resource.mScratchKey.isValid());
            if (!isOverrun() && hasKey) {
                return;
            }
        } else {
            // We keep un-budgeted resources with a unique key in the cleanable queue of the cache,
            // so they can be reused again by the image connected to the unique key.
            if (hasUniqueKey && budgetedType == Types.BUDGET_TYPE_PARTIAL) {
                return;
            }
            // Check whether this resource could still be used as a scratch resource.
            if (!resource.isWrapped() &&
                    (resource.mScratchKey != null && resource.mScratchKey.isValid())) {
                // We won't purge an existing resource to make room for this one.
                if (mBudgetedBytes + resource.getMemorySize() <= mMaxBytes) {
                    resource.makeBudgeted(true);
                    return;
                }
            }
        }

        int beforeCount = getResourceCount();
        resource.release();
        // We should at least free this resource, perhaps dependent resources as well.
        assert getResourceCount() < beforeCount;
    }

    void insertResource(Resource resource) {
        assert !isInCache(resource);
        assert !resource.isDestroyed();
        assert !resource.isCleanable();

        // We must set the timestamp before adding to the array in case the timestamp wraps, and we wind
        // up iterating over all the resources that already have timestamps.
        resource.mTimestamp = getNextTimestamp();

        addToNonCleanableArray(resource);
        mCount++;

        long size = resource.getMemorySize();
        mCount++;
        mBytes += size;
        if (resource.getBudgetType() == Types.BUDGET_TYPE_COMPLETE) {
            mBudgetedCount++;
            mBudgetedBytes += size;
        }

        assert !resource.isUsableAsScratch();
        clean();
    }

    void removeResource(Resource resource) {
        assert isInCache(resource);

        long size = resource.getMemorySize();
        if (resource.isCleanable()) {
            mCleanableQueue.removeAt(resource.mCacheIndex);
            mCleanableBytes -= size;
        } else {
            removeFromNonCleanableArray(resource);
        }

        mCount--;
        mBytes -= size;
        if (resource.getBudgetType() == Types.BUDGET_TYPE_COMPLETE) {
            mBudgetedCount--;
            mBudgetedBytes -= size;
        }

        if (resource.isUsableAsScratch()) {
            mScratchMap.remove(resource.mScratchKey, resource);
        }
        if (resource.mUniqueKey != null && resource.mUniqueKey.isValid()) {
            mUniqueMap.remove(resource.mUniqueKey);
        }
    }

    void changeUniqueKey(Resource resource, UniqueKey newKey) {
        assert isInCache(resource);

        // If another resource has the new key, remove its key then install the key on this resource.
        if (newKey.isValid()) {
            Resource old;
            if ((old = mUniqueMap.get(newKey)) != null) {
                // If the old resource using the key is cleanable and is unreachable, then remove it.
                if ((old.mScratchKey == null || !old.mScratchKey.isValid()) &&
                        old.isCleanable()) {
                    old.release();
                } else {
                    // removeUniqueKey expects an external owner of the resource.
                    old.ref();
                    removeUniqueKey(old);
                    old.unref();
                }
            }
            assert !mUniqueMap.containsKey(newKey);

            // Remove the entry for this resource if it already has a unique key.
            if (resource.mUniqueKey != null && resource.mUniqueKey.isValid()) {
                assert mUniqueMap.get(resource.mUniqueKey) == resource;
                mUniqueMap.remove(resource.mUniqueKey);
                assert !mUniqueMap.containsKey(resource.mUniqueKey);
            } else {
                // 'resource' didn't have a valid unique key before, so it is switching sides. Remove it
                // from the ScratchMap. The isUsableAsScratch call depends on us not adding the new
                // unique key until after this check.
                if (resource.isUsableAsScratch()) {
                    mScratchMap.remove(resource.mScratchKey, resource);
                }
            }

            if (resource.mUniqueKey == null) {
                resource.mUniqueKey = new UniqueKey();
            }
            resource.mUniqueKey.set(newKey);
            mUniqueMap.put(resource.mUniqueKey, resource);
        } else {
            removeUniqueKey(resource);
        }
    }

    void removeUniqueKey(Resource resource) {
        // Someone has a ref to this resource in order to have removed the key. When the ref count
        // reaches zero we will get a ref cnt notification and figure out what to do with it.
        if (resource.mUniqueKey != null && resource.mUniqueKey.isValid()) {
            assert mUniqueMap.get(resource.mUniqueKey) == resource;
            mUniqueMap.remove(resource.mUniqueKey);
        }
        if (resource.mUniqueKey != null) {
            resource.mUniqueKey.reset();
        }
        if (resource.isUsableAsScratch()) {
            mScratchMap.put(resource.mScratchKey, resource);
        }

        // Removing a unique key from a partial budgeted resource would make the resource
        // require cleaning. However, the resource must be referenced to get here and therefore can't
        // be cleanable. We'll purge it when the refs reach zero.
        assert !resource.isCleanable();
    }

    void didChangeBudgetStatus(Resource resource) {
        assert isInCache(resource);

        long size = resource.getMemorySize();
        // Changing from partial budgeted state to another budgeted type could make
        // resource become cleanable. However, we should never allow that transition. Wrapped
        // resources are the only resources that can be in that state, and they aren't allowed to
        // transition from one budgeted state to another.
        boolean wasCleanable = resource.isCleanable();
        if (resource.getBudgetType() == Types.BUDGET_TYPE_COMPLETE) {
            mBudgetedCount++;
            mBudgetedBytes += size;
            if (!resource.isCleanable() &&
                    !resource.hasRefOrCommandBufferUsage()) {
                mFlushableCount++;
            }
            if (resource.isUsableAsScratch()) {
                mScratchMap.put(resource.mScratchKey, resource);
            }
            clean();
        } else {
            assert resource.getBudgetType() == Types.BUDGET_TYPE_PARTIAL;
            mBudgetedCount--;
            mBudgetedBytes -= size;
            if (!resource.isCleanable() &&
                    !resource.hasRefOrCommandBufferUsage()) {
                mFlushableCount--;
            }
            if (!resource.hasRef() && (resource.mUniqueKey == null || !resource.mUniqueKey.isValid()) &&
                    (resource.mScratchKey != null && resource.mScratchKey.isValid())) {
                mScratchMap.remove(resource.mScratchKey, resource);
            }
        }
        assert wasCleanable == resource.isCleanable();
    }

    void willRemoveScratchKey(Resource resource) {
        assert resource.mScratchKey != null && resource.mScratchKey.isValid();
        if (resource.isUsableAsScratch()) {
            mScratchMap.remove(resource.mScratchKey, resource);
        }
    }

    private void refAndMakeResourceMRU(Resource resource) {
        assert isInCache(resource);

        if (resource.isCleanable()) {
            // It's about to become non-cleanable
            mCleanableBytes -= resource.getMemorySize();
            mCleanableQueue.removeAt(resource.mCacheIndex);
            addToNonCleanableArray(resource);
        } else if (!resource.hasRefOrCommandBufferUsage() &&
                resource.getBudgetType() == Types.BUDGET_TYPE_COMPLETE) {
            assert mFlushableCount > 0;
            mFlushableCount--;
        }
        resource.addInitialRef();

        resource.mTimestamp = getNextTimestamp();
    }

    private void addToNonCleanableArray(Resource resource) {
        Resource[] es = mNonCleanableList;
        final int s = mNonCleanableSize;
        if (s >= es.length) {
            // Grow the array, we assume (s >> 1) > 0;
            mNonCleanableList = es = Arrays.copyOf(es, s + (s >> 1));
        }
        es[s] = resource;
        resource.mCacheIndex = s;
        mNonCleanableSize = s + 1;
    }

    private void removeFromNonCleanableArray(Resource resource) {
        final Resource[] es = mNonCleanableList;
        // Fill the hole we will create in the array with the tail object, adjust its index, and
        // then pop the array
        final Resource tail = es[--mNonCleanableSize];
        assert es[resource.mCacheIndex] == resource;
        es[resource.mCacheIndex] = tail;
        tail.mCacheIndex = resource.mCacheIndex;
        resource.mCacheIndex = -1;
    }

    private int getNextTimestamp() {
        // If we wrap then all the existing resources will appear older than any resources that get
        // a timestamp after the wrap.
        if (mTimestamp == 0) {
            int count = getResourceCount();
            if (count > 0) {
                // Reset all the timestamps. We sort the resources by timestamp and then assign
                // sequential timestamps beginning with 0. This is O(n*lg(n)) but it should be extremely
                // rare.
                Arrays.sort(mNonCleanableList, TIMESTAMP_COMPARATOR);

                // Pick resources out of the cleanable and non-cleanable arrays based on lowest
                // timestamp and assign new timestamps.
                int currP = 0;
                int currNP = 0;
                while (currP < mCleanableQueue.size() &&
                        currNP < mNonCleanableSize) {
                    int tsP = mCleanableQueue.get(currP).mTimestamp;
                    int tsNP = mNonCleanableList[currNP].mTimestamp;
                    // They never conflicts.
                    assert tsP != tsNP;
                    if (tsP < tsNP) {
                        mCleanableQueue.get(currP++).mTimestamp = mTimestamp++;
                    } else {
                        // Correct the index in the non-cleanable array stored on the resource post-sort.
                        mNonCleanableList[currNP].mCacheIndex = currNP;
                        mNonCleanableList[currNP++].mTimestamp = mTimestamp++;
                    }
                }

                // The above loop ended when we hit the end of one array. Finish the other one.
                while (currP < mCleanableQueue.size()) {
                    mCleanableQueue.get(currP++).mTimestamp = mTimestamp++;
                }
                while (currNP < mNonCleanableSize) {
                    mNonCleanableList[currNP].mCacheIndex = currNP;
                    mNonCleanableList[currNP++].mTimestamp = mTimestamp++;
                }

                // Rebuild the queue.
                mCleanableQueue.sort();

                // Count should be the next timestamp we return.
                assert mTimestamp == getResourceCount();
            }
        }
        return mTimestamp++;
    }

    private boolean isInCache(Resource resource) {
        int index = resource.mCacheIndex;
        if (index < 0) {
            return false;
        }
        if (index < mCleanableQueue.size() && mCleanableQueue.get(index) == resource) {
            return true;
        }
        if (index < mNonCleanableSize && mNonCleanableList[index] == resource) {
            return true;
        }
        throw new IllegalStateException("Resource index should be -1 or the resource should be in the cache.");
    }

    @Override
    public void close() {
        releaseAll();
    }
}
