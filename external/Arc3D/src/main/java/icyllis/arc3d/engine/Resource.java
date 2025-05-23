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

import icyllis.arc3d.core.*;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.*;

/**
 * Base class for operating GPU resources that can be kept in the {@link ResourceCache}.
 * <p>
 * Resources that have memory allocation:
 * <ul>
 *   <li>GLBuffer</li>
 *   <li>GLTexture</li>
 *   <li>GLRenderbuffer</li>
 *   <li>VulkanBuffer</li>
 *   <li>VulkanImage</li>
 * </ul>
 * Resources that are zero-sized:
 * <ul>
 *     <li>GLSampler</li>
 *     <li>VulkanSampler</li>
 *     <li>VulkanRenderPass</li>
 * </ul>
 * <p>
 * Register resources into the cache to track their GPU memory usage. Since all
 * Java objects will always be strong referenced, an explicit {@link #ref()} and
 * {@link #unref()} is required to determine if they need to be recycled or not.
 * When used as smart pointers, they need to be annotated as {@link SharedPtr},
 * otherwise they tend to be used as raw pointers (no ref/unref calls should be
 * made). A paired {@link UniqueID} object can be used as unique identifiers.
 * <p>
 * Each {@link Resource} should be created with immutable GPU memory allocation.
 * {@link Resource} can be only created/operated on the creating thread of
 * {@link #getContext()}, but may be recycled from other threads. Use
 * {@link ResourceProvider} to obtain {@link Resource} objects.
 */
@NotThreadSafe
public abstract class Resource implements RefCounted {

    private static final VarHandle USAGE_REF_CNT;
    private static final VarHandle COMMAND_BUFFER_REF_CNT;
    private static final VarHandle CACHE_REF_CNT;
    private static final ConcurrentHashMap<Resource, Boolean> TRACKER;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            USAGE_REF_CNT = lookup.findVarHandle(Resource.class, "mUsageRefCnt", int.class);
            COMMAND_BUFFER_REF_CNT = lookup.findVarHandle(Resource.class, "mCommandBufferRefCnt", int.class);
            CACHE_REF_CNT = lookup.findVarHandle(Resource.class, "mCacheRefCnt", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        TRACKER = new ConcurrentHashMap<>();
        try {
            assert false;
        } catch (AssertionError e) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                // subclasses should override toString() for debug purposes
                TRACKER.forEach((o, __) -> System.err.printf(
                        "UsageRefCnt %d, CommandBufferRefCnt %d, CacheRefCnt %d: %s%n",
                        o.mUsageRefCnt, o.mCommandBufferRefCnt, o.mCacheRefCnt, o));
                assert TRACKER.isEmpty() : "Memory leaks in GPU resources";
            }, "Resource-Tracker"));
        }
    }

    /*
     * This enum is used to notify the ResourceCache which type of ref just dropped to zero on a
     * Resource.
     */
    static final int REF_TYPE_USAGE = 0;
    static final int REF_TYPE_COMMAND_BUFFER = 1;
    static final int REF_TYPE_CACHE = 2;

    @SuppressWarnings("FieldMayBeFinal")
    private volatile int mUsageRefCnt = 1;
    @SuppressWarnings("FieldMayBeFinal")
    private volatile int mCommandBufferRefCnt = 0;
    @SuppressWarnings("FieldMayBeFinal")
    private volatile int mCacheRefCnt = 0;

    static final PriorityQueue.Access<Resource> QUEUE_ACCESS = new PriorityQueue.Access<>() {
        @Override
        public void setIndex(Resource resource, int index) {
            resource.mCacheIndex = index;
        }

        @Override
        public int getIndex(Resource resource) {
            return resource.mCacheIndex;
        }
    };

    // set once in constructor, clear to null after being destroyed
    volatile Context mContext;

    // null meaning invalid, lazy initialized
    volatile IResourceKey mKey;
    // the resource key and return cache are both set at most once, during registerWithCache().
    volatile ResourceCache mReturnCache;
    // An index into the return cache so we know whether the resource is already waiting to
    // be returned or not.
    volatile int mReturnIndex = -1;

    // the index into a heap when this resource is cleanable or an array when not,
    // this is maintained by the cache
    int mCacheIndex = -1;
    // the value reflects how recently this resource was accessed in the cache,
    // this is maintained by the cache
    int mTimestamp;
    private long mLastUsedTime;

    // All resources created internally by Engine that are held in the ResourceCache as shared or
    // available scratch resources are considered budgeted. Resources that back client-owned objects
    // (e.g. Surface or Image) and wrapper objects (e.g. BackendTexture) do not count against
    // cache limits and therefore should never be budgeted.
    private volatile boolean mBudgeted;
    private final boolean mWrapped; // non-wrapped means we have ownership
    // All resources start out as non-shareable (the strictest mode) and revert to non-shareable
    // when they are returned to the cache and have no more usage refs. An available resource can
    // be returned if its shareable type matches the request, or if it was non-shareable at which
    // point the resource is upgraded to the more permissive mode (until all shared usages are
    // dropped at which point it can be used for any purpose again).
    private volatile boolean mShareable;
    private volatile boolean mCacheable = true;
    boolean mNonShareableInCache = false;

    private final long mMemorySize;

    @NonNull
    private volatile String mLabel = "";
    private final UniqueID mUniqueID = new UniqueID();

    @SuppressWarnings("AssertWithSideEffects")
    protected Resource(Context context,
                       boolean wrapped,
                       long memorySize) {
        assert (context != null);
        mContext = context;
        mWrapped = wrapped;
        mMemorySize = memorySize;
        assert TRACKER.put(this, Boolean.TRUE) == null;
    }

    /**
     * Increases the reference count by 1 on the client pipeline.
     * It's an error to call this method if the reference count has already reached zero.
     */
    @Override
    public final void ref() {
        // only the cache should be able to add the first ref to a resource.
        assert hasUsageRef();
        // stronger than std::memory_order_relaxed
        USAGE_REF_CNT.getAndAddRelease(this, 1);
    }

    /**
     * Decreases the reference count by 1 on the client pipeline.
     * It's an error to call this method if the reference count has already reached zero.
     */
    @Override
    public final void unref() {
        boolean shouldRelease = false;
        synchronized (this) {
            assert hasUsageRef();
            // stronger than std::memory_order_acq_rel
            if ((int) USAGE_REF_CNT.getAndAdd(this, -1) == 1) {
                shouldRelease = notifyACntReachedZero(REF_TYPE_USAGE);
            }
        }
        if (shouldRelease) {
            release();
        }
    }

    /**
     * Increases the usage count by 1 on the tracked backend pipeline.
     * <p>
     * This is designed to be used by Resources that need to track when they are in use on
     * backend (usually via a command buffer) separately from tracking if there are any current logical
     * usages in client. This allows for a scratch Resource to be reused for new draw calls even
     * if it is in use on the backend.
     */
    public final void refCommandBuffer() {
        // stronger than std::memory_order_relaxed
        COMMAND_BUFFER_REF_CNT.getAndAddRelease(this, 1);
    }

    /**
     * Decreases the usage count by 1 on the tracked backend pipeline.
     * It's an error to call this method if the usage count has already reached zero.
     */
    public final void unrefCommandBuffer() {
        boolean shouldRelease = false;
        synchronized (this) {
            assert hasCommandBufferRef();
            // stronger than std::memory_order_acq_rel
            if ((int) COMMAND_BUFFER_REF_CNT.getAndAdd(this, -1) == 1) {
                shouldRelease = notifyACntReachedZero(REF_TYPE_COMMAND_BUFFER);
            }
        }
        if (shouldRelease) {
            release();
        }
    }

    // Adds a cache ref to the resource. This is only called by ResourceCache. A Resource will only
    // ever add a ref when the Resource is part of the cache (i.e. when insertResource is called)
    // and while the Resource is in the ResourceCache::ReturnQueue.
    final void refCache() {
        // stronger than std::memory_order_relaxed
        CACHE_REF_CNT.getAndAddRelease(this, 1);
    }

    // Removes a cache ref from the resource. The unref here should only ever be called from the
    // ResourceCache and only in the Recorder thread the ResourceCache is part of.
    final void unrefCache() {
        boolean shouldRelease = false;
        synchronized (this) {
            assert hasCacheRef();
            // stronger than std::memory_order_acq_rel
            if ((int) CACHE_REF_CNT.getAndAdd(this, -1) == 1) {
                shouldRelease = notifyACntReachedZero(REF_TYPE_CACHE);
            }
        }
        if (shouldRelease) {
            release();
        }
    }

    protected final boolean hasUsageRef() {
        // The acquire barrier is only really needed if we return true.  It
        // prevents code conditioned on the result of hasUsageRef() from running until previous
        // owners are all totally done calling unref().
        return (int) USAGE_REF_CNT.getAcquire(this) > 0;
    }

    protected final boolean hasCommandBufferRef() {
        // The acquire barrier is only really needed if we return true.  It
        // prevents code conditioned on the result of hasCommandBufferRef() from running
        // until previous owners are all totally done calling unrefCommandBuffer().
        return (int) COMMAND_BUFFER_REF_CNT.getAcquire(this) > 0;
    }

    protected final boolean hasCacheRef() {
        // The acquire barrier is only really needed if we return true. It
        // prevents code conditioned on the result of hasUsageRef() from running until previous
        // owners are all totally done calling unref().
        return (int) CACHE_REF_CNT.getAcquire(this) > 0;
    }

    // Privileged method that allows going from ref count = 0 to ref count = 1
    final void addInitialUsageRef() {
        // assert (int) REF_CNT.getAcquire(this) >= 0;
        // stronger than std::memory_order_relaxed
        USAGE_REF_CNT.getAndAddRelease(this, 1);
    }

    // One of usage, command buffer, or cache ref count reached zero
    @GuardedBy("this")
    private boolean notifyACntReachedZero(int refCntType) {
        // No resource should have been destroyed if there was still any sort of ref on it.
        assert (!isDestroyed());

        if (refCntType != REF_TYPE_CACHE &&
                mReturnCache != null &&
                mReturnCache.returnResource(this, refCntType)) {
            return false;
        }

        return !hasAnyRefs();
    }

    /**
     * Checks whether an object has been released or discarded. All objects will
     * be in this state after their creating Context is destroyed or has
     * contextLost called. It's up to the client to test isDestroyed() before
     * attempting to use an object if it holds refs on objects across
     * Context.close(), freeResources with the force flag, or contextLost.
     *
     * @return true if the object has been released or discarded, false otherwise.
     */
    public final boolean isDestroyed() {
        return mContext == null;
    }

    /**
     * Retrieves the context that owns the object. Note that it is possible for
     * this to return null. When objects have been release()ed or discard()ed
     * they no longer have an owning context. Destroying a {@link Context}
     * automatically releases all its resources.
     */
    @Nullable
    public final Context getContext() {
        return mContext;
    }

    /**
     * Retrieves the amount of GPU memory used by this resource in bytes. It is
     * approximate since we aren't aware of additional padding or copies made
     * by the driver.
     *
     * @return the amount of GPU memory used in bytes
     */
    public final long getMemorySize() {
        return mMemorySize;
    }

    /**
     * Get the resource's budget type which indicates whether it counts against the resource cache
     * budget.
     */
    public final boolean isBudgeted() {
        return mBudgeted;
    }

    /**
     * Can the resource be held by multiple users at the same time?
     * For example, samplers, etc.
     *
     * @return true if shareable, false if scratch
     */
    public final boolean isShareable() {
        return mShareable;
    }

    /**
     * Gets a tag that is unique for this Resource object. It is static in that it does
     * not change when the content of the Resource object changes. It has identity and
     * never hold a reference to this Resource object, so it can be used to track state
     * changes through '=='.
     */
    @NonNull
    public UniqueID getUniqueID() {
        return mUniqueID;
    }

    /**
     * @return the label for the resource, or empty
     */
    @NonNull
    public final String getLabel() {
        return mLabel;
    }

    /**
     * Sets a label for the resource for debugging purposes, the label must
     * contain only ASCII characters.
     *
     * @param label the new label to set, null or empty to clear
     */
    public final void setLabel(@Nullable String label) {
        label = label != null ? label.trim() : "";
        if (!mLabel.equals(label)) {
            mLabel = label;
            onSetLabel(!label.isEmpty() ? label : null);
        }
    }

    /**
     * Budgeted: If the resource is uncached make it cached. Has no effect on resources that
     * are wrapped or already cached.
     * <p>
     * Not Budgeted: If the resource is cached make it uncached. Has no effect on resources that
     * are wrapped or already uncached. Furthermore, resources with unique keys cannot be made
     * not budgeted.
     */
    final void setBudgeted(boolean budgeted) {
        // If we don't own the resource that must mean its wrapped in a client object. Thus we should
        // not be budgeted
        assert !budgeted || !mWrapped;
        mBudgeted = budgeted;
    }

    final void setShareable(boolean shareable) {
        assert !shareable || mBudgeted;
        mShareable = shareable;
    }

    /**
     * Is the resource object wrapping an externally allocated GPU resource?
     */
    @ApiStatus.Internal
    public final boolean isWrapped() {
        return mWrapped;
    }

    /**
     * If this resource can be used as a scratch resource this returns a valid scratch key.
     * Otherwise, it returns a key for which isNullScratch is true. The resource may currently be
     * used as a uniquely keyed resource rather than scratch. Check isScratch().
     */
    @ApiStatus.Internal
    public final IResourceKey getKey() {
        return mKey;
    }

    @ApiStatus.Internal
    public final boolean isPurgeable() {
        // For being purgeable we don't care if there are cacheRefs on the object since the cacheRef
        // will always be greater than 1 since we add one on insert and don't remove that ref until
        // the Resource is removed from the cache.
        return !hasUsageRef() && !hasCommandBufferRef();
    }

    @ApiStatus.Internal
    public final boolean hasAnyRefs() {
        return hasUsageRef() || hasCommandBufferRef() || hasCacheRef();
    }

    protected final void setNonCacheable() {
        mCacheable = false;
    }

    final boolean isCacheable() {
        return mCacheable;
    }

    final void registerWithCache(ResourceCache returnCache,
                                 IResourceKey key,
                                 boolean initialBudgeted,
                                 boolean initialShareable) {
        assert mReturnCache == null;
        assert mKey == null;
        assert returnCache != null;
        assert key != null;

        mReturnCache = returnCache;
        mKey = key;
        setBudgeted(initialBudgeted);
        setShareable(initialShareable);
    }

    /**
     * @return the device or null if destroyed
     */
    protected Device getDevice() {
        return mContext.getDevice();
    }

    /**
     * Subclass should override this method to free GPU resources in the backend API.
     */
    protected abstract void onRelease();

    /**
     * Subclass should override this method to set object label in the backend API.
     */
    protected void onSetLabel(@Nullable String label) {
    }

    final boolean isUsableAsScratch() {
        return !mShareable && !hasUsageRef() && mNonShareableInCache;
    }

    /**
     * Called by the last owner to delete the resource, inherently guarded by "this".
     */
    @SuppressWarnings("AssertWithSideEffects")
    private void release() {
        assert mContext != null;
        onRelease();
        mContext = null;
        assert TRACKER.remove(this) == Boolean.TRUE;
    }

    final void setLastUsedTime() {
        mLastUsedTime = System.currentTimeMillis();
    }

    /**
     * Called by the cache to determine whether this resource should be cleaned up based on the length
     * of time it has been available for cleaning.
     */
    final long getLastUsedTime() {
        assert isPurgeable();
        return mLastUsedTime;
    }
}
