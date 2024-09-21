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

import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nonnull;
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
@NotThreadSafe
public final class ResourceCache {

    private static final Comparator<Resource> TIMESTAMP_COMPARATOR =
            (lhs, rhs) -> Integer.compareUnsigned(lhs.mTimestamp, rhs.mTimestamp);

    private ImageProxyCache mImageProxyCache = null;
    private ThreadSafeCache mThreadSafeCache = null;

    private final Context mContext;

    // Whenever a resource is added to the cache or the result of a cache lookup, mTimestamp is
    // assigned as the resource's timestamp and then incremented. mPurgeableQueue orders the
    // purgeable resources by this value, and thus is used to purge resources in LRU order.
    // Resources with a size of zero are set to have max uint32_t value. This will also put them at
    // the end of the LRU priority queue. This will allow us to not purge these resources even when
    // we are over budget.
    private int mTimestamp = 0;
    private static final int MAX_TIMESTAMP = 0xFFFFFFFF;

    private final PriorityQueue<Resource> mPurgeableQueue;
    private Resource[] mNonPurgeableList;
    private int mNonPurgeableSize = 0;

    // This map holds all resources that can be used as scratch/shareable resources.
    private final LinkedListMultimap<IResourceKey, Resource> mResourceMap;

    private final Object mReturnLock = new Object();
    // two arrays are parallel
    @GuardedBy("mReturnLock")
    private Resource[] mReturnQueue;
    @GuardedBy("mReturnLock")
    private int[] mReturnQueueRefTypes;
    @GuardedBy("mReturnLock")
    private int mReturnQueueSize = 0;

    // Our budget
    private long mMaxBytes;

    // Our current stats for resources that count against the budget
    private int mBudgetedCount = 0;
    private long mBudgetedBytes = 0;
    private long mPurgeableBytes = 0;

    @GuardedBy("mReturnLock")
    private boolean mShutdown = false;

    /**
     * Created by {@link ResourceProvider}.
     */
    ResourceCache(Context context, long maxBytes) {
        mContext = context;
        mMaxBytes = maxBytes;

        mPurgeableQueue = new PriorityQueue<>(TIMESTAMP_COMPARATOR, Resource.QUEUE_ACCESS);
        // initial size must > 2
        mNonPurgeableList = new Resource[10];
        mReturnQueue = new Resource[10];
        mReturnQueueRefTypes = new int[10];

        mResourceMap = new LinkedListMultimap<>();
    }

    /**
     * Sets the max GPU memory byte size of the cache.
     * A {@link #purgeAsNeeded()} is followed by this method call.
     * The passed value can be retrieved by {@link #getMaxBudget()}.
     */
    @VisibleForTesting
    public void setMaxBudget(long maxBytes) {
        mMaxBytes = maxBytes;
        processReturnedResources();
        purgeAsNeeded();
    }

    /**
     * Returns the number of cached resources.
     */
    public int getResourceCount() {
        return mPurgeableQueue.size() + mNonPurgeableSize;
    }

    /**
     * Returns the number of resources that count against the budget.
     */
    public int getBudgetedResourceCount() {
        return mBudgetedCount;
    }

    /**
     * Returns the number of bytes consumed by budgeted resources.
     */
    public long getBudgetedBytes() {
        return mBudgetedBytes;
    }

    /**
     * Returns the number of bytes held by unlocked resources which are available for cleanup.
     */
    public long getPurgeableBytes() {
        return mPurgeableBytes;
    }

    /**
     * Returns the number of bytes that cached resources should count against.
     * This can be changed by {@link #setMaxBudget(long)}.
     */
    public long getMaxBudget() {
        return mMaxBytes;
    }

    /**
     * Called by the ResourceProvider when it is dropping its ref to the ResourceCache. After this
     * is called no more Resources can be returned to the ResourceCache (besides those already in
     * the return queue). Also no new Resources can be retrieved from the ResourceCache.
     */
    public void shutdown() {
        assert mContext.isOwnerThread();

        assert !mShutdown;

        synchronized (mReturnLock) {
            mShutdown = true;
        }

        processReturnedResources();

        while (mNonPurgeableSize > 0) {
            Resource back = mNonPurgeableList[mNonPurgeableSize - 1];
            assert !back.isDestroyed();
            removeFromNonPurgeableArray(back);
            back.unrefCache();
        }

        while (!mPurgeableQueue.isEmpty()) {
            Resource top = mPurgeableQueue.peek();
            assert !top.isDestroyed();
            removeFromPurgeableQueue(top);
            top.unrefCache();
        }
    }

    /**
     * Find a resource that matches a key.
     */
    @Nullable
    public Resource findAndRefResource(@Nonnull IResourceKey key,
                                       boolean budgeted) {
        assert mContext.isOwnerThread();
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
            assert resource.isBudgeted();
            if (!key.isShareable()) {
                // If a resource is not shareable (i.e. scratch resource) then we remove it from the map
                // so that it isn't found again.
                mResourceMap.removeFirstEntry(key, resource);
                if (!budgeted) {
                    resource.makeBudgeted(false);
                    mBudgetedCount--;
                    mBudgetedBytes -= resource.getMemorySize();
                }
                resource.mNonShareableInCache = false;
            } else {
                // Shareable resources should never be requested as non budgeted
                assert budgeted;
            }
            refAndMakeResourceMRU(resource);
        }

        // processReturnedResources may have added resources back into our budget if they were being
        // using in an SkImage or SkSurface previously. However, instead of calling purgeAsNeeded in
        // processReturnedResources, we delay calling it until now so we don't end up purging a resource
        // we're looking for in this function.
        //
        // We could avoid calling this if we didn't return any resources from processReturnedResources.
        // However, when not overbudget purgeAsNeeded is very cheap. When overbudget there may be some
        // really niche usage patterns that could cause us to never actually return resources to the
        // cache, but still be overbudget due to shared resources. So to be safe we just always call it
        // here.
        purgeAsNeeded();

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
    @VisibleForTesting
    public void purgeAsNeeded() {
        assert mContext.isOwnerThread();

        if (isOverBudget() && mImageProxyCache != null) {
            mImageProxyCache.dropUniqueRefs();

            // After the image cache frees resources we need to return those resources to the cache
            processReturnedResources();
        }

        while (isOverBudget() && !mPurgeableQueue.isEmpty()) {
            Resource resource = mPurgeableQueue.peek();
            assert !resource.isDestroyed();
            assert mResourceMap.containsKey(resource.getKey());

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
     * Deallocates unlocked resources as much as possible. All purgeable
     * resources will be deleted.
     */
    public void purgeResources() {
        assert mContext.isOwnerThread();
        purgeResources(false, -1);
    }

    /**
     * Deallocates unlocked resources not used since the passed point in time. The time-base is
     * {@link System#currentTimeMillis()}. Otherwise, all purgeable resources older than
     * <code>timeMillis</code> will be deleted.
     *
     * @param timeMillis the resources older than this time will be deleted
     */
    public void purgeResourcesNotUsedSince(long timeMillis) {
        assert mContext.isOwnerThread();
        purgeResources(true, timeMillis);
    }

    /**
     * Returns true if {@link #getBudgetedBytes()} is greater than {@link #getMaxBudget()}.
     */
    private boolean isOverBudget() {
        return mBudgetedBytes > mMaxBytes;
    }

    private void purgeResources(boolean useTime, long timeMillis) {
        if (mImageProxyCache != null) {
            mImageProxyCache.dropUniqueRefsOlderThan(timeMillis);
        }
        processReturnedResources();

        // Early out if the very first item is too new to purge to avoid sorting the queue when
        // nothing will be deleted.
        if (useTime && !mPurgeableQueue.isEmpty() &&
                mPurgeableQueue.peek().getLastUsedTime() >= timeMillis) {
            return;
        }

        // Sort the queue
        mPurgeableQueue.sort();

        // Make a list of the resources to delete
        ArrayList<Resource> resourcesToPurge = new ArrayList<>();
        for (int i = 0; i < mPurgeableQueue.size(); i++) {
            Resource resource = mPurgeableQueue.elementAt(i);

            if (useTime && resource.getLastUsedTime() >= timeMillis) {
                // scratch or not, all later iterations will be too recently used to clean up.
                break;
            }
            assert resource.isPurgeable();
            resourcesToPurge.add(resource);
        }

        // Delete the scratch resources. This must be done as a separate pass
        // to avoid messing up the sorted order of the queue
        resourcesToPurge.forEach(this::purgeResource);

        // Since we called process returned resources at the start of this call, we could still end up
        // over budget even after purging resources based on timeMillis. So we call purgeAsNeeded at the
        // end here.
        purgeAsNeeded();

        // trim internal arrays
        mPurgeableQueue.trim();
    }

    // This is a thread safe call. If it fails the ResourceCache is no longer valid and the
    // Resource should clean itself up if it is the last ref.
    boolean returnResource(@Nonnull Resource resource, int refType) {
        // We should never be trying to return a LastRemovedRef of kCache.
        assert refType != Resource.REF_TYPE_CACHE;
        synchronized (mReturnLock) {
            if (mShutdown) {
                return false;
            }

            // We only allow one instance of a Resource to be in the return queue at a time. We do this so
            // that the ReturnQueue stays small and quick to process.
            //
            // Because we take CacheRefs to all Resources added to the ReturnQueue, we would be safe if we
            // decided to have multiple instances of a Resource. Even if an earlier returned instance of a
            // Resource triggers that Resource to get purged from the cache, the Resource itself wouldn't
            // get deleted until we drop all the CacheRefs in this ReturnQueue.
            if (resource.mReturnIndex >= 0) {
                // If the resource is already in the return queue we promote the LastRemovedRef to be
                // kUsage if that is what is returned here.
                if (refType == Resource.REF_TYPE_USAGE) {
                    assert resource.mReturnIndex < mReturnQueueSize;
                    mReturnQueueRefTypes[resource.mReturnIndex] = refType;
                }
                return true;
            }

            Resource[] es = mReturnQueue;
            final int s = mReturnQueueSize;
            if (s == es.length) {
                // Grow the array, we assume (s >> 1) > 0;
                int newCap = s + (s >> 1);
                mReturnQueue = es = Arrays.copyOf(es, newCap);
                mReturnQueueRefTypes = Arrays.copyOf(mReturnQueueRefTypes, newCap);
            }
            es[s] = resource;
            mReturnQueueRefTypes[s] = refType;
            resource.mReturnIndex = s;
            mReturnQueueSize = s + 1;
            resource.refCache();
            return true;
        }
    }

    // This will return true if any resources were actually returned to the cache
    @VisibleForTesting
    public boolean processReturnedResources() {
        final Resource[] tempQueue;
        final int[] tempRefTypes;

        synchronized (mReturnLock) {
            if (mReturnQueueSize == 0) {
                return false;
            }
            //TODO: Instead of doing a copy of the array, we may be able to improve the performance
            // here by storing some form of linked list, then just move the pointer the first element
            // and reset the ReturnQueue's top element to nullptr.
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
            // Remove cache ref held by ReturnQueue
            resource.unrefCache();
        }

        return true;
    }

    private void returnResourceToCache(@Nonnull Resource resource, int refType) {
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
                assert mResourceMap.containsKey(resource.getKey());
            } else {
                resource.mNonShareableInCache = true;
                mResourceMap.addFirstEntry(resource.getKey(), resource);
                if (!resource.isBudgeted()) {
                    resource.makeBudgeted(true);
                    mBudgetedCount++;
                    mBudgetedBytes += resource.getMemorySize();
                }
            }
        }

        // If we weren't using multiple threads, it is ok to assume a resource that isn't purgeable must
        // be in the non-purgeable array. However, since resources can be unreffed from multiple
        // threads, it is possible that a resource became purgeable while we are in the middle of
        // returning resources. For example, a resource could have 1 usage and 1 command buffer ref. We
        // then unref the usage which puts the resource in the return queue. Then the ResourceCache
        // thread locks the ReturnQueue as it returns the Resource. At this same time another thread
        // unrefs the command buffer usage but can't add the Resource to the ReturnQueue as it is
        // locked (but the command buffer ref has been reduced to zero). When we are processing the
        // Resource (from the kUsage ref) to return it to the cache it will look like it is purgeable
        // since all refs are zero. Thus we will move the Resource from the non-purgeable to purgeable
        // queue. Then later when we return the command buffer ref, the Resource will have already been
        // moved to purgeable queue and we don't need to do it again.
        if (!resource.isPurgeable() || isInPurgeableQueue(resource)) {
            return;
        }

        setResourceTimestamp(resource, getNextTimestamp());

        removeFromNonPurgeableArray(resource);

        if (resource.isCacheable()) {
            assert resource.isPurgeable();
            resource.setLastUsedTime();
            mPurgeableQueue.add(resource);
            mPurgeableBytes += resource.getMemorySize();
        } else {
            purgeResource(resource);
        }
    }

    public void insertResource(@Nonnull Resource resource) {
        assert mContext.isOwnerThread();
        assert !isInCache(resource);
        assert !resource.isDestroyed();
        assert !resource.isPurgeable();
        assert resource.getKey() != null;
        // All resources in the cache are owned. If we track wrapped resources in the cache we'll need
        // to update this check.
        assert !resource.isWrapped();

        // The reason to call processReturnedResources here is to get an accurate accounting of our
        // memory usage as some resources can go from unbudgeted to budgeted when they return. So we
        // want to have them all returned before adding the budget for the new resource in case we need
        // to purge things. However, if the new resource has a memory size of 0, then we just skip
        // returning resources (which has overhead for each call) since the new resource won't be
        // affecting whether we're over or under budget.
        if (resource.getMemorySize() > 0) {
            processReturnedResources();
        }

        resource.registerWithCache(this);
        resource.refCache();

        // We must set the timestamp before adding to the array in case the timestamp wraps, and we wind
        // up iterating over all the resources that already have timestamps.
        setResourceTimestamp(resource, getNextTimestamp());
        resource.setLastUsedTime();

        addToNonPurgeableArray(resource);

        if (resource.getKey().isShareable()) {
            mResourceMap.addFirstEntry(resource.getKey(), resource);
        }

        if (resource.isBudgeted()) {
            mBudgetedCount++;
            mBudgetedBytes += resource.getMemorySize();
        }

        purgeAsNeeded();
    }

    private void purgeResource(@Nonnull Resource resource) {
        assert resource.isPurgeable();

        mResourceMap.removeFirstEntry(resource.getKey(), resource);

        if (resource.isCacheable()) {
            assert isInPurgeableQueue(resource);
            removeFromPurgeableQueue(resource);
        } else {
            assert !isInCache(resource);
        }

        mBudgetedCount--;
        mBudgetedBytes -= resource.getMemorySize();
        resource.unrefCache();
    }

    private void refAndMakeResourceMRU(@Nonnull Resource resource) {
        assert isInCache(resource);

        if (isInPurgeableQueue(resource)) {
            // It's about to become non-purgeable.
            mPurgeableQueue.removeAt(resource.mCacheIndex);
            mPurgeableBytes -= resource.getMemorySize();
            addToNonPurgeableArray(resource);
        }
        resource.addInitialUsageRef();

        setResourceTimestamp(resource, getNextTimestamp());
    }

    private void addToNonPurgeableArray(@Nonnull Resource resource) {
        Resource[] es = mNonPurgeableList;
        final int s = mNonPurgeableSize;
        if (s == es.length) {
            // Grow the array, we assume (s >> 1) > 0;
            mNonPurgeableList = es = Arrays.copyOf(es, s + (s >> 1));
        }
        es[s] = resource;
        resource.mCacheIndex = s;
        mNonPurgeableSize = s + 1;
    }

    private void removeFromNonPurgeableArray(@Nonnull Resource resource) {
        final Resource[] es = mNonPurgeableList;
        // Fill the hole we will create in the array with the tail object, adjust its index, and
        // then pop the array
        final int pos = resource.mCacheIndex;
        assert es[pos] == resource;
        final int s = --mNonPurgeableSize;
        final Resource tail = es[s];
        es[s] = null;
        es[pos] = tail;
        tail.mCacheIndex = pos;
        resource.mCacheIndex = -1;
    }

    private void removeFromPurgeableQueue(@Nonnull Resource resource) {
        mPurgeableQueue.removeAt(resource.mCacheIndex);
        mPurgeableBytes -= resource.getMemorySize();
        // we are using the index as a
        // flag for whether the Resource has been purged from the cache or not. So we need to make sure
        // it always gets set.
        resource.mCacheIndex = -1;
    }

    private int getNextTimestamp() {
        // If we wrap then all the existing resources will appear older than any resources that get
        // a timestamp after the wrap. We wrap one value early when we reach kMaxTimestamp so that we
        // can continue to use kMaxTimestamp as a special case for zero sized resources.
        if (mTimestamp == MAX_TIMESTAMP) {
            mTimestamp = 0;
            int count = getResourceCount();
            if (count > 0) {
                // Reset all the timestamps. We sort the resources by timestamp and then assign
                // sequential timestamps beginning with 0. This is O(n*lg(n)) but it should be extremely
                // rare.
                int purgeableSize = mPurgeableQueue.size();
                Resource[] sortedPurgeable = new Resource[purgeableSize];

                for (int i = 0; i < purgeableSize; i++) {
                    sortedPurgeable[i] = mPurgeableQueue.remove();
                }

                Arrays.sort(mNonPurgeableList, 0, mNonPurgeableSize, TIMESTAMP_COMPARATOR);

                // Pick resources out of the purgeable and non-purgeable arrays based on lowest
                // timestamp and assign new timestamps.
                int currP = 0;
                int currNP = 0;
                while (currP < purgeableSize &&
                        currNP < mNonPurgeableSize) {
                    int tsP = sortedPurgeable[currP].mTimestamp;
                    int tsNP = mNonPurgeableList[currNP].mTimestamp;
                    assert tsP != tsNP;
                    if (tsP < tsNP) {
                        setResourceTimestamp(sortedPurgeable[currP++], mTimestamp++);
                    } else {
                        // Correct the index in the non-purgeable array stored on the resource post-sort.
                        mNonPurgeableList[currNP].mCacheIndex = currNP;
                        setResourceTimestamp(mNonPurgeableList[currNP++], mTimestamp++);
                    }
                }

                // The above loop ended when we hit the end of one array. Finish the other one.
                while (currP < purgeableSize) {
                    setResourceTimestamp(sortedPurgeable[currP++], mTimestamp++);
                }
                while (currNP < mNonPurgeableSize) {
                    mNonPurgeableList[currNP].mCacheIndex = currNP;
                    setResourceTimestamp(mNonPurgeableList[currNP++], mTimestamp++);
                }

                // Rebuild the queue.
                Collections.addAll(mPurgeableQueue, sortedPurgeable);

                // Count should be the next timestamp we return.
                assert mTimestamp == count;
                assert mTimestamp == getResourceCount();
            }
        }
        return mTimestamp++;
    }

    private void setResourceTimestamp(@Nonnull Resource resource, int timestamp) {
        // We always set the timestamp for zero sized resources to be kMaxTimestamp
        if (resource.getMemorySize() == 0) {
            timestamp = MAX_TIMESTAMP;
        }
        resource.mTimestamp = timestamp;
    }

    private boolean isInPurgeableQueue(@Nonnull Resource resource) {
        assert isInCache(resource);
        int index = resource.mCacheIndex;
        return index < mPurgeableQueue.size() && mPurgeableQueue.elementAt(index) == resource;
    }

    private boolean isInCache(@Nonnull Resource resource) {
        int index = resource.mCacheIndex;
        if (index < 0) {
            return false;
        }
        if (index < mPurgeableQueue.size() && mPurgeableQueue.elementAt(index) == resource) {
            return true;
        }
        if (index < mNonPurgeableSize && mNonPurgeableList[index] == resource) {
            return true;
        }
        throw new AssertionError("Resource index should be -1 or the resource should be in the cache.");
    }
}
