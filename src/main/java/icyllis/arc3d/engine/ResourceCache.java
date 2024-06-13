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
import javax.annotation.concurrent.GuardedBy;
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
    private static final int MAX_TIMESTAMP = 0xFFFFFFFF;

    private final PriorityQueue<Resource> mFreeQueue;
    private Resource[] mNonFreeList;
    private int mNonFreeSize;

    // This map holds all resources that can be used as scratch resources.
    private final LinkedListMultimap<IResourceKey, Resource> mResourceMap;

    private final Object mReturnLock = new Object();
    // two arrays are parallel
    @GuardedBy("mReturnLock")
    private Resource[] mReturnQueue;
    @GuardedBy("mReturnLock")
    private int[] mReturnQueueRefTypes;
    @GuardedBy("mReturnLock")
    private int mReturnQueueSize = 0;

    // our budget
    private long mMaxBytes = 1 << 28;

    // our current stats for all resources
    private int mCount = 0;
    private long mBytes = 0;

    // our current stats for resources that count against the budget
    private int mBudgetedCount = 0;
    private long mBudgetedBytes = 0;
    private long mFreeBytes = 0;

    private final int mContextID;

    @GuardedBy("mReturnLock")
    private boolean mShutdown = false;

    /**
     * Created by {@link ResourceProvider}.
     */
    ResourceCache(int contextID) {
        mContextID = contextID;

        mFreeQueue = new PriorityQueue<>(TIMESTAMP_COMPARATOR, Resource.QUEUE_ACCESS);
        // initial size must > 2
        mNonFreeList = new Resource[10];
        mReturnQueue = new Resource[10];
        mReturnQueueRefTypes = new int[10];

        mResourceMap = new LinkedListMultimap<>();
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
        processReturnedResources();
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

    public void shutdown() {
        assert !mShutdown;

        synchronized (mReturnLock) {
            mShutdown = true;
        }

        processReturnedResources();

        while (mNonFreeSize > 0) {
            Resource back = mNonFreeList[mNonFreeSize - 1];
            assert !back.isDestroyed();
            removeFromNonFreeArray(back);
            back.unrefCache();
        }

        while (!mFreeQueue.isEmpty()) {
            Resource top = mFreeQueue.peek();
            assert !top.isDestroyed();
            removeFromFreeQueue(top);
            top.unrefCache();
        }

        //TODO do we need to track these data and validate?
        /*assert mResourceMap.isEmpty() : mResourceMap;
        assert mCount == 0 : mCount;
        assert getResourceCount() == 0;
        assert mBytes == 0;
        assert mBudgetedCount == 0;
        assert mBudgetedBytes == 0;
        assert mFreeBytes == 0;*/
    }

    boolean processReturnedResources() {

        Resource[] tempQueue;
        int[] tempRefTypes;

        synchronized (mReturnLock) {
            if (mReturnQueueSize == 0) {
                return false;
            }
            tempQueue = Arrays.copyOf(mReturnQueue, mReturnQueueSize);
            tempRefTypes = Arrays.copyOf(mReturnQueueRefTypes, mReturnQueueSize);
            Arrays.fill(mReturnQueue, 0, mReturnQueueSize, null);
            mReturnQueueSize = 0;
            for (Resource resource : tempQueue) {
                assert resource.mReturnIndex >= 0;
                resource.mReturnIndex = -1;
            }
        }

        for (int i = 0; i < tempQueue.length; i++) {
            // We need this check here to handle the following scenario. A Resource is sitting in the
            // ReturnQueue (say from kUsage last ref) and the Resource still has a command buffer ref
            // out in the wild. When the ResourceCache calls processReturnedResources it locks the
            // ReturnMutex. Immediately after this, the command buffer ref is released on another
            // thread. The Resource cannot be added to the ReturnQueue since the lock is held. Back in
            // the ResourceCache (we'll drop the ReturnMutex) and when we try to return the Resource we
            // will see that it is purgeable. If we are overbudget it is possible that the Resource gets
            // purged from the ResourceCache at this time setting its cache index to -1. The unrefCache
            // call will actually block here on the Resource's UnrefMutex which is held from the command
            // buffer ref. Eventually the command bufer ref thread will get to run again and with the
            // ReturnMutex lock dropped it will get added to the ReturnQueue. At this point the first
            // unrefCache call will continue on the main ResourceCache thread. When we call
            // processReturnedResources the next time, we don't want this Resource added back into the
            // cache, thus we have the check here. The Resource will then get deleted when we call
            // unrefCache below to remove the cache ref added from the ReturnQueue.
            Resource resource = tempQueue[i];
            if (resource.mCacheIndex != -1) {
                returnResourceToCache(resource, tempRefTypes[i]);
            }
            resource.unrefCache();
        }

        return true;
    }

    /**
     * Find a resource that matches a key.
     */
    @Nullable
    public Resource findAndRefResource(IResourceKey key,
                                       boolean budgeted) {
        assert key != null;

        Resource resource = mResourceMap.peekFirstEntry(key);
        if (resource == null) {
            // The main reason to call processReturnedResources in this call is to see if there are any
            // resources that we could match with the key. However, there is overhead into calling it.
            // So we only call it if we first failed to find a matching resource.
            if (processReturnedResources()) {
                resource = mResourceMap.peekFirstEntry(key);
            }
        }

        if (resource != null) {
            // All resources we pull out of the cache for use should be budgeted
            assert (resource.isBudgeted());
            if (!key.isShareable()) {
                // If a resource is not shareable (i.e. scratch resource) then we remove it from the map
                // so that it isn't found again.
                mResourceMap.removeFirstEntry(key, resource);
                if (!budgeted) {
                    resource.makeBudgeted(false);
                    mBudgetedBytes -= resource.getMemorySize();
                }
                resource.mNonShareableInCache = false;
            } else {
                // Shareable resources should never be requested as non budgeted
                assert (budgeted);
            }
            refAndMakeResourceMRU(resource);
        }

        cleanup();

        return resource;
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
     */
    public void cleanup() {

        if (isOverBudget() && mImageProxyCache != null) {
            mImageProxyCache.dropUniqueRefs();

            // After the image cache frees resources we need to return those resources to the cache
            processReturnedResources();
        }

        while (isOverBudget() && !mFreeQueue.isEmpty()) {
            Resource resource = mFreeQueue.peek();
            assert !resource.isDestroyed();
            assert mResourceMap.peekFirstEntry(resource.mKey) != null;

            if (resource.mTimestamp == MAX_TIMESTAMP) {
                // If we hit a resource that is at kMaxTimestamp, then we've hit the part of the
                // purgeable queue with all zero sized resources. We don't want to actually remove those
                // so we just break here.
                assert resource.getMemorySize() == 0;
                break;
            }

            purgeResource(resource);
        }
    }

    /**
     * Deallocates unlocked resources as much as possible. If <code>scratchOnly</code> is true,
     * the free resources containing persistent data are skipped. Otherwise, all free
     * resources will be deleted.
     */
    public void purgeFreeResources() {
        purgeFreeResourcesOlderThan(-1);
    }

    /**
     * Deallocates unlocked resources not used since the passed point in time. The time-base is
     * {@link System#currentTimeMillis()}. If <code>scratchOnly</code> is true, the free resources
     * containing persistent data are skipped. Otherwise, all free resources older than
     * <code>timeMillis</code> will be deleted.
     *
     * @param timeMillis  the resources older than this time will be deleted
     */
    public void purgeFreeResourcesOlderThan(long timeMillis) {

        if (mImageProxyCache != null) {
            mImageProxyCache.dropUniqueRefsOlderThan(timeMillis);
        }
        processReturnedResources();

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
            scratchResources.add(resource);
        }

        // Delete the scratch resources. This must be done as a separate pass
        // to avoid messing up the sorted order of the queue
        scratchResources.forEach(this::purgeResource);

        cleanup();

        // trim internal arrays
        mFreeQueue.trim();
    }

    /**
     * Returns true if {@link #getBudgetedResourceBytes()} is greater than {@link #getMaxResourceBytes()}.
     */
    public boolean isOverBudget() {
        return mBudgetedBytes > mMaxBytes;
    }

    boolean returnResource(Resource resource, int refType) {
        assert resource != null;
        assert refType != Resource.REF_TYPE_CACHE;
        synchronized (mReturnLock) {
            if (mShutdown) {
                return false;
            }

            if (resource.mReturnIndex >= 0) {
                if (refType == Resource.REF_TYPE_USAGE) {
                    assert resource.mReturnIndex < mReturnQueueSize;
                    mReturnQueueRefTypes[resource.mReturnIndex] = Resource.REF_TYPE_USAGE;
                }
                return true;
            }

            Resource[] returnQueue = mReturnQueue;
            final int s = mReturnQueueSize;
            if (s == returnQueue.length) {
                // Grow the array, we assume (s >> 1) > 0;
                int newCap = s + (s >> 1);
                mReturnQueue = returnQueue = Arrays.copyOf(returnQueue, newCap);
                mReturnQueueRefTypes = Arrays.copyOf(mReturnQueueRefTypes, newCap);
            }
            returnQueue[s] = resource;
            mReturnQueueRefTypes[s] = refType;
            resource.mReturnIndex = s;
            mReturnQueueSize = s + 1;
            resource.refCache();
            return true;
        }
    }

    private void returnResourceToCache(Resource resource, int refType) {
        // A resource should not have been destroyed when placed into the return queue. Also before
        // purging any resources from the cache itself, it should always empty the queue first. When the
        // cache releases/abandons all of its resources, it first invalidates the return queue so no new
        // resources can be added. Thus we should not end up in a situation where a resource gets
        // destroyed after it was added to the return queue.
        assert !resource.isDestroyed();
        assert isInCache(resource);

        if (refType == Resource.REF_TYPE_USAGE) {
            if (resource.getKey().isShareable()) {
                // Shareable resources should still be in the cache
                assert mResourceMap.containsKey(resource.mKey);
            } else {
                resource.mNonShareableInCache = true;
                mResourceMap.addFirstEntry(resource.mKey, resource);
                if (!resource.isBudgeted()) {
                    resource.makeBudgeted(true);
                    mBudgetedBytes += resource.getMemorySize();
                }
            }
        }

        if (!resource.isFree() || isInFreeQueue(resource)) {
            return;
        }

        setResourceTimestamp(resource, getNextTimestamp());

        removeFromNonFreeArray(resource);

        if (resource.isCacheable()) {
            assert resource.isFree();
            resource.setLastUsedTime();
            mFreeQueue.add(resource);
            mFreeBytes += resource.getMemorySize();
        } else {
            purgeResource(resource);
        }
    }

    public void insertResource(Resource resource) {
        assert !isInCache(resource);
        assert !resource.isDestroyed();
        assert !resource.isFree();
        assert resource.getKey() != null;
        // All resources in the cache are owned. If we track wrapped resources in the cache we'll need
        // to update this check.
        assert !resource.isWrapped();

        if (resource.getMemorySize() > 0) {
            processReturnedResources();
        }

        resource.registerWithCache(this);
        resource.refCache();

        // We must set the timestamp before adding to the array in case the timestamp wraps, and we wind
        // up iterating over all the resources that already have timestamps.
        setResourceTimestamp(resource, getNextTimestamp());
        resource.setLastUsedTime();

        addToNonFreeArray(resource);

        long size = resource.getMemorySize();
        mCount++;
        mBytes += size;

        if (resource.getKey().isShareable()) {
            mResourceMap.addFirstEntry(resource.getKey(), resource);
        }

        if (resource.isBudgeted()) {
            mBudgetedCount++;
            mBudgetedBytes += size;
        }

        cleanup();
    }

    void setResourceTimestamp(Resource resource, int timestamp) {
        // We always set the timestamp for zero sized resources to be kMaxTimestamp
        if (resource.getMemorySize() == 0) {
            timestamp = MAX_TIMESTAMP;
        }
        resource.mTimestamp = timestamp;
    }

    void purgeResource(Resource resource) {
        assert resource.isFree();

        mResourceMap.removeFirstEntry(resource.mKey, resource);

        if (resource.isCacheable()) {
            assert isInFreeQueue(resource);
            removeFromFreeQueue(resource);
        } else {
            assert !isInCache(resource);
        }

        mBudgetedBytes -= resource.getMemorySize();
        resource.unrefCache();
    }

    void removeFromFreeQueue(Resource resource) {
        mFreeQueue.removeAt(resource.mCacheIndex);
        // we are using the index as a
        // flag for whether the Resource has been purged from the cache or not. So we need to make sure
        // it always gets set.
        resource.mCacheIndex = -1;
    }

    private void refAndMakeResourceMRU(Resource resource) {
        assert isInCache(resource);

        if (isInFreeQueue(resource)) {
            // It's about to become non-cleanable
            mFreeBytes -= resource.getMemorySize();
            mFreeQueue.removeAt(resource.mCacheIndex);
            addToNonFreeArray(resource);
        }
        resource.addInitialUsageRef();

        setResourceTimestamp(resource, getNextTimestamp());
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
        if (mTimestamp == MAX_TIMESTAMP) {
            mTimestamp = 0;
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

    private boolean isInFreeQueue(Resource resource) {
        assert isInCache(resource);
        int index = resource.mCacheIndex;
        return index < mFreeQueue.size() && mFreeQueue.elementAt(index) == resource;
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
        shutdown();
    }
}
