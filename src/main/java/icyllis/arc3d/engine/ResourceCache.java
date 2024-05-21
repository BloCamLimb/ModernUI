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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;

import static icyllis.arc3d.engine.Engine.BudgetType;

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
//TODO still WIP and under review (ref/unref functionality)
@NotThreadSafe
public final class ResourceCache implements AutoCloseable {

    private static final Comparator<Resource> TIMESTAMP_COMPARATOR =
            (lhs, rhs) -> Integer.compareUnsigned(lhs.mTimestamp, rhs.mTimestamp);

    private ImageProxyCache mImageProxyCache = null;
    private ThreadSafeCache mThreadSafeCache = null;

    // Whenever a resource is added to the cache or the result of a cache lookup, mTimestamp is
    // assigned as the resource's timestamp and then incremented. mFreeQueue orders the
    // free resources by this value, and thus is used to clean up resources in LRU order.
    private int mTimestamp = 0;

    private final PriorityQueue<Resource> mFreeQueue;
    private Resource[] mNonFreeList;
    private int mNonFreeSize;

    // This map holds all resources that can be used as scratch resources.
    private final LinkedListMultimap<IScratchKey, Resource> mScratchMap;
    // This map holds all resources that have unique keys.
    private final HashMap<IUniqueKey, Resource> mUniqueMap;

    // our budget, used in clean()
    private long mMaxBytes = 1 << 28;

    // our current stats for all resources
    private int mCount = 0;
    private long mBytes = 0;

    // our current stats for resources that count against the budget
    private int mBudgetedCount = 0;
    private long mBudgetedBytes = 0;
    private long mFreeBytes = 0;
    // the number of resources will become free after flushing command buffer
    private int mDirtyCount = 0;

    private final int mContextID;

    /**
     * Created by DirectContext.
     */
    ResourceCache(int contextID) {
        mContextID = contextID;

        mFreeQueue = new PriorityQueue<>(TIMESTAMP_COMPARATOR, Resource.QUEUE_ACCESS);
        mNonFreeList = new Resource[10]; // initial size must > 2

        mScratchMap = new LinkedListMultimap<>();
        mUniqueMap = new HashMap<>();
    }

    /**
     * @return unique ID of the owning context
     */
    public int getContextID() {
        return mContextID;
    }

    /**
     * Sets the max GPU memory byte size of the cache.
     * A {@link #cleanup()} is followed by this method call.
     * The passed value can be retrieved by {@link #getMaxResourceBytes()}.
     */
    public void setCacheLimit(long maxBytes) {
        mMaxBytes = maxBytes;
        cleanup();
    }

    /**
     * Returns the number of cached resources.
     */
    public int getResourceCount() {
        return mFreeQueue.size() + mNonFreeSize;
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
     * Returns the number of bytes held by unlocked resources which are available for cleanup.
     */
    public long getFreeResourceBytes() {
        return mFreeBytes;
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

        while (mNonFreeSize > 0) {
            Resource back = mNonFreeList[mNonFreeSize - 1];
            assert !back.isDestroyed();
            back.release();
        }

        while (!mFreeQueue.isEmpty()) {
            Resource top = mFreeQueue.peek();
            assert !top.isDestroyed();
            top.release();
        }

        assert mScratchMap.isEmpty();
        assert mUniqueMap.isEmpty();
        assert mCount == 0 : mCount;
        assert getResourceCount() == 0;
        assert mBytes == 0;
        assert mBudgetedCount == 0;
        assert mBudgetedBytes == 0;
        assert mFreeBytes == 0;
    }

    /**
     * Drops the backend API resources owned by all Resource objects and removes them from
     * the cache.
     */
    public void discardAll() {
        while (mNonFreeSize > 0) {
            Resource back = mNonFreeList[mNonFreeSize - 1];
            assert !back.isDestroyed();
            back.discard();
        }

        while (!mFreeQueue.isEmpty()) {
            Resource top = mFreeQueue.peek();
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
        assert mFreeBytes == 0;
    }

    /**
     * Find a resource that matches a scratch key.
     */
    @Nullable
    public Resource findAndRefScratchResource(IScratchKey key) {
        assert key != null;
        Resource resource = mScratchMap.pollFirstEntry(key);
        if (resource != null) {
            refAndMakeResourceMRU(resource);
            return resource;
        }
        return null;
    }

    /**
     * Find a resource that matches a unique key.
     */
    @Nullable
    public Resource findAndRefUniqueResource(IUniqueKey key) {
        assert key != null;
        Resource resource = mUniqueMap.get(key);
        if (resource != null) {
            refAndMakeResourceMRU(resource);
        }
        return resource;
    }

    /**
     * Query whether a unique key exists in the cache.
     */
    public boolean hasUniqueKey(IUniqueKey key) {
        return mUniqueMap.containsKey(key);
    }

    public void setSurfaceProvider(ImageProxyCache imageProxyCache) {
        mImageProxyCache = imageProxyCache;
    }

    public void setThreadSafeCache(ThreadSafeCache threadSafeCache) {
        mThreadSafeCache = threadSafeCache;
    }

    /**
     * Performs any pending maintenance operations needed by the cache. In particular,
     * deallocates resources to become under budget and processes resources with invalidated
     * unique keys.
     *
     * @return true if still over budget
     */
    public boolean cleanup() {
        //this->processFreedGpuResources();

        boolean stillOverBudget = isOverBudget();
        while (stillOverBudget && !mFreeQueue.isEmpty()) {
            Resource resource = mFreeQueue.peek();
            assert (resource.isFree());
            resource.release();
            stillOverBudget = isOverBudget();
        }

        if (stillOverBudget) {
            mThreadSafeCache.dropUniqueRefs(this);

            stillOverBudget = isOverBudget();
            while (stillOverBudget && !mFreeQueue.isEmpty()) {
                Resource resource = mFreeQueue.peek();
                assert (resource.isFree());
                resource.release();
                stillOverBudget = isOverBudget();
            }
        }

        return stillOverBudget;
    }

    /**
     * Deallocates unlocked resources as much as possible. If <code>scratchOnly</code> is true,
     * the free resources containing persistent data are skipped. Otherwise, all free
     * resources will be deleted.
     *
     * @param scratchOnly if true, only scratch resources will be deleted
     */
    public void purgeFreeResources(boolean scratchOnly) {
        purgeFreeResourcesOlderThan(0, scratchOnly);
    }

    /**
     * Deallocates unlocked resources not used since the passed point in time. The time-base is
     * {@link System#currentTimeMillis()}. If <code>scratchOnly</code> is true, the free resources
     * containing persistent data are skipped. Otherwise, all free resources older than
     * <code>timeMillis</code> will be deleted.
     *
     * @param timeMillis  the resources older than this time will be deleted
     * @param scratchOnly if true, only scratch resources will be deleted
     */
    public void purgeFreeResourcesOlderThan(long timeMillis, boolean scratchOnly) {
        if (scratchOnly) {
            // Early out if the very first item is too new to clean up to avoid sorting the queue when
            // nothing will be deleted.
            if (timeMillis >= 0 && !mFreeQueue.isEmpty() &&
                    mFreeQueue.peek().getLastUsedTime() >= timeMillis) {
                return;
            }

            // Sort the queue
            mFreeQueue.sort();

            // Make a list of the scratch resources to delete
            List<Resource> scratchResources = new ArrayList<>();
            for (int i = 0; i < mFreeQueue.size(); i++) {
                Resource resource = mFreeQueue.elementAt(i);

                if (timeMillis >= 0 && resource.getLastUsedTime() >= timeMillis) {
                    // scratch or not, all later iterations will be too recently used to clean up.
                    break;
                }
                assert (resource.isFree());
                if (resource.mUniqueKey == null) {
                    scratchResources.add(resource);
                }
            }

            // Delete the scratch resources. This must be done as a separate pass
            // to avoid messing up the sorted order of the queue
            scratchResources.forEach(Resource::release);
        } else {
            if (timeMillis >= 0) {
                mThreadSafeCache.dropUniqueRefsOlderThan(timeMillis);
            } else {
                mThreadSafeCache.dropUniqueRefs(null);
            }

            // We could disable maintaining the heap property here, but it would add a lot of
            // complexity. Moreover, this is rarely called.
            while (!mFreeQueue.isEmpty()) {
                Resource resource = mFreeQueue.peek();

                if (timeMillis >= 0 && resource.getLastUsedTime() >= timeMillis) {
                    // Resources were given both LRU timestamps and tagged with a frame number when
                    // they first became cleanable. The LRU timestamp won't change again until the
                    // resource is made non-cleanable again. So, at this point all the remaining
                    // resources in the timestamp-sorted queue will have a frame number >= to this
                    // one.
                    break;
                }

                assert (resource.isFree());
                resource.release();
            }
        }

        // trim internal arrays
        mFreeQueue.trim();
    }

    /**
     * Purge unlocked resources from the cache until the provided byte count has been reached,
     * or we have purged all unlocked resources. The default policy is to purge in LRU order,
     * but can be overridden to prefer purging scratch resources (in LRU order) prior to
     * purging other resource types.
     *
     * @param bytesToPurge  the desired number of bytes to be purged
     * @param preferScratch if true, scratch resources will be purged prior to other resource types
     */
    public void purgeFreeResourcesUpToBytes(long bytesToPurge, boolean preferScratch) {
        //TODO
    }

    /**
     * If it's possible to clean up enough resources to get the provided amount of budget
     * headroom, do so and return true. If it's not possible, do nothing and return false.
     */
    public boolean purgeFreeResourcesToReserveBytes(long bytesToReserve) {
        return false;
    }

    /**
     * Returns true if {@link #getBudgetedResourceBytes()} is greater than {@link #getMaxResourceBytes()}.
     */
    public boolean isOverBudget() {
        return mBudgetedBytes > mMaxBytes;
    }

    /**
     * Returns true if the cache would like a flush to occur in order to make more resources
     * cleanable.
     */
    public boolean isFlushNeeded() {
        return isOverBudget() && mFreeQueue.isEmpty() && mDirtyCount > 0;
    }

    void notifyACntReachedZero(Resource resource, boolean commandBufferUsage) {
        assert !resource.isDestroyed();
        assert isInCache(resource);
        // This resource should always be in the non-cleanable array when this function is called. It
        // will be moved to the queue if it is newly cleanable.
        assert mNonFreeList[resource.mCacheIndex] == resource;

        if (!commandBufferUsage) {
            if (resource.isUsableAsScratch()) {
                mScratchMap.addFirstEntry(resource.mScratchKey, resource);
            }
        }

        if (resource.hasRefOrCommandBufferUsage()) {
            return;
        }

        resource.mTimestamp = getNextTimestamp();

        if (!resource.isFree() &&
                resource.getBudgetType() == BudgetType.Budgeted) {
            mDirtyCount++;
        }

        if (!resource.isFree()) {
            return;
        }

        removeFromNonFreeArray(resource);
        mFreeQueue.add(resource);
        resource.setLastUsedTime();
        mFreeBytes += resource.getMemorySize();

        boolean hasUniqueKey = resource.mUniqueKey != null;

        int budgetedType = resource.getBudgetType();

        if (budgetedType == BudgetType.Budgeted) {
            // Purge the resource immediately if we're over budget
            // Also purge if the resource has neither a valid scratch key nor a unique key.
            boolean hasKey = hasUniqueKey || resource.mScratchKey != null;
            if (!isOverBudget() && hasKey) {
                return;
            }
        } else {
            // We keep un-budgeted resources with a unique key in the cleanable queue of the cache,
            // so they can be reused again by the image connected to the unique key.
            if (hasUniqueKey && budgetedType == BudgetType.WrapCacheable) {
                return;
            }
            // Check whether this resource could still be used as a scratch resource.
            if (!resource.isWrapped() && resource.mScratchKey != null) {
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
        assert !resource.isFree();

        // We must set the timestamp before adding to the array in case the timestamp wraps, and we wind
        // up iterating over all the resources that already have timestamps.
        resource.mTimestamp = getNextTimestamp();

        addToNonFreeArray(resource);

        long size = resource.getMemorySize();
        mCount++;
        mBytes += size;
        if (resource.getBudgetType() == BudgetType.Budgeted) {
            mBudgetedCount++;
            mBudgetedBytes += size;
        }

        assert !resource.isUsableAsScratch();
        cleanup();
    }

    void removeResource(Resource resource) {
        assert isInCache(resource);

        long size = resource.getMemorySize();
        if (resource.isFree()) {
            mFreeQueue.removeAt(resource.mCacheIndex);
            mFreeBytes -= size;
        } else {
            removeFromNonFreeArray(resource);
        }

        mCount--;
        mBytes -= size;
        if (resource.getBudgetType() == BudgetType.Budgeted) {
            mBudgetedCount--;
            mBudgetedBytes -= size;
        }

        if (resource.isUsableAsScratch()) {
            mScratchMap.removeFirstEntry(resource.mScratchKey, resource);
        }
        if (resource.mUniqueKey != null) {
            mUniqueMap.remove(resource.mUniqueKey);
        }
    }

    void changeUniqueKey(Resource resource, IUniqueKey newKey) {
        assert isInCache(resource);

        // If another resource has the new key, remove its key then install the key on this resource.
        if (newKey != null) {
            Resource old;
            if ((old = mUniqueMap.get(newKey)) != null) {
                // If the old resource using the key is cleanable and is unreachable, then remove it.
                if (old.mScratchKey == null && old.isFree()) {
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
            if (resource.mUniqueKey != null) {
                assert mUniqueMap.get(resource.mUniqueKey) == resource;
                mUniqueMap.remove(resource.mUniqueKey);
                assert !mUniqueMap.containsKey(resource.mUniqueKey);
            } else {
                // 'resource' didn't have a valid unique key before, so it is switching sides. Remove it
                // from the ScratchMap. The isUsableAsScratch call depends on us not adding the new
                // unique key until after this check.
                if (resource.isUsableAsScratch()) {
                    mScratchMap.removeFirstEntry(resource.mScratchKey, resource);
                }
            }

            resource.mUniqueKey = newKey;
            mUniqueMap.put(resource.mUniqueKey, resource);
        } else {
            removeUniqueKey(resource);
        }
    }

    void removeUniqueKey(Resource resource) {
        // Someone has a ref to this resource in order to have removed the key. When the ref count
        // reaches zero we will get a ref cnt notification and figure out what to do with it.
        if (resource.mUniqueKey != null) {
            assert mUniqueMap.get(resource.mUniqueKey) == resource;
            mUniqueMap.remove(resource.mUniqueKey);
        }
        if (resource.mUniqueKey != null) {
            resource.mUniqueKey = null;
        }
        if (resource.isUsableAsScratch()) {
            mScratchMap.addFirstEntry(resource.mScratchKey, resource);
        }

        // Removing a unique key from a partial budgeted resource would make the resource
        // require cleaning. However, the resource must be referenced to get here and therefore can't
        // be cleanable. We'll purge it when the refs reach zero.
        assert !resource.isFree();
    }

    void didChangeBudgetStatus(Resource resource) {
        assert isInCache(resource);

        long size = resource.getMemorySize();
        // Changing from partial budgeted state to another budgeted type could make
        // resource become cleanable. However, we should never allow that transition. Wrapped
        // resources are the only resources that can be in that state, and they aren't allowed to
        // transition from one budgeted state to another.
        boolean wasCleanable = resource.isFree();
        if (resource.getBudgetType() == BudgetType.Budgeted) {
            mBudgetedCount++;
            mBudgetedBytes += size;
            if (!resource.isFree() &&
                    !resource.hasRefOrCommandBufferUsage()) {
                mDirtyCount++;
            }
            if (resource.isUsableAsScratch()) {
                mScratchMap.addFirstEntry(resource.mScratchKey, resource);
            }
            cleanup();
        } else {
            assert resource.getBudgetType() != BudgetType.WrapCacheable;
            mBudgetedCount--;
            mBudgetedBytes -= size;
            if (!resource.isFree() &&
                    !resource.hasRefOrCommandBufferUsage()) {
                mDirtyCount--;
            }
            if (!resource.hasRef() && resource.mUniqueKey == null &&
                    resource.mScratchKey != null) {
                mScratchMap.removeFirstEntry(resource.mScratchKey, resource);
            }
        }
        assert wasCleanable == resource.isFree();
    }

    void willRemoveScratchKey(Resource resource) {
        assert resource.mScratchKey != null;
        if (resource.isUsableAsScratch()) {
            mScratchMap.removeFirstEntry(resource.mScratchKey, resource);
        }
    }

    private void refAndMakeResourceMRU(Resource resource) {
        assert isInCache(resource);

        if (resource.isFree()) {
            // It's about to become non-cleanable
            mFreeBytes -= resource.getMemorySize();
            mFreeQueue.removeAt(resource.mCacheIndex);
            addToNonFreeArray(resource);
        } else if (!resource.hasRefOrCommandBufferUsage() &&
                resource.getBudgetType() == BudgetType.Budgeted) {
            assert mDirtyCount > 0;
            mDirtyCount--;
        }
        resource.addInitialRef();

        resource.mTimestamp = getNextTimestamp();
    }

    private void addToNonFreeArray(Resource resource) {
        Resource[] es = mNonFreeList;
        final int s = mNonFreeSize;
        if (s == es.length) {
            // Grow the array, we assume (s >> 1) > 0;
            mNonFreeList = es = Arrays.copyOf(es, s + (s >> 1));
        }
        es[s] = resource;
        resource.mCacheIndex = s;
        mNonFreeSize = s + 1;
    }

    private void removeFromNonFreeArray(Resource resource) {
        final Resource[] es = mNonFreeList;
        // Fill the hole we will create in the array with the tail object, adjust its index, and
        // then pop the array
        final int pos = resource.mCacheIndex;
        assert es[pos] == resource;
        final int s = --mNonFreeSize;
        final Resource tail = es[s];
        es[s] = null;
        es[pos] = tail;
        tail.mCacheIndex = pos;
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
                int freeSize = mFreeQueue.size();
                Resource[] sortedFree = new Resource[freeSize];

                for (int i = 0; i < freeSize; i++) {
                    sortedFree[i] = mFreeQueue.remove();
                }

                Arrays.sort(mNonFreeList, 0, mNonFreeSize, TIMESTAMP_COMPARATOR);

                // Pick resources out of the free and non-free arrays based on lowest
                // timestamp and assign new timestamps.
                int currF = 0;
                int currNF = 0;
                while (currF < freeSize &&
                        currNF < mNonFreeSize) {
                    int tsP = sortedFree[currF].mTimestamp;
                    int tsNP = mNonFreeList[currNF].mTimestamp;
                    // They never conflicts.
                    assert tsP != tsNP;
                    if (tsP < tsNP) {
                        sortedFree[currF++].mTimestamp = mTimestamp++;
                    } else {
                        // Correct the index in the non-cleanable array stored on the resource post-sort.
                        mNonFreeList[currNF].mCacheIndex = currNF;
                        mNonFreeList[currNF++].mTimestamp = mTimestamp++;
                    }
                }

                // The above loop ended when we hit the end of one array. Finish the other one.
                while (currF < freeSize) {
                    sortedFree[currF++].mTimestamp = mTimestamp++;
                }
                while (currNF < mNonFreeSize) {
                    mNonFreeList[currNF].mCacheIndex = currNF;
                    mNonFreeList[currNF++].mTimestamp = mTimestamp++;
                }

                // Rebuild the queue.
                Collections.addAll(mFreeQueue, sortedFree);

                // Count should be the next timestamp we return.
                assert mTimestamp == count;
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
        if (index < mFreeQueue.size() && mFreeQueue.elementAt(index) == resource) {
            return true;
        }
        if (index < mNonFreeSize && mNonFreeList[index] == resource) {
            return true;
        }
        throw new IllegalStateException("Resource index should be -1 or the resource should be in the cache.");
    }

    @Override
    public void close() {
        releaseAll();
    }
}
