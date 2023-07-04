/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.modernui.annotation.NonNull;
import icyllis.arc3d.SharedPtr;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

/**
 * The ResourceAllocator explicitly distributes {@link Resource Resources} at flush time. It
 * operates by being given the usage intervals of the various proxies. It keeps these intervals
 * in a singly linked list sorted by increasing start index. (It also maintains a h table
 * from proxyID to interval to find proxy reuse). The ResourceAllocator uses Registers (in the
 * sense of register allocation) to represent a future surface that will be used for each proxy
 * during {@link #simulate()}, and then assigns actual surfaces during {@link #allocate()}.
 * <p>
 * Note: the op indices (used in the usage intervals) come from the order of the ops in
 * their opsTasks after the opsTask DAG has been linearized.
 * <p>
 * The {@link #simulate()} method traverses the sorted list and:
 * <ul>
 *     <li>moves intervals from the active list that have completed (returning their registers
 *     to the free pool) into the finished list (sorted by increasing start)</li>
 *
 *     <li>allocates a new register (preferably from the free pool) for the new interval
 *     adds the new interval to the active list (that is sorted by increasing end index)</li>
 * </ul>
 * <p>
 * If the user wants to commit to the current simulation, they call {@link #allocate()} which:
 * <ul>
 *     <li>instantiates lazy proxies</li>
 *
 *     <li>instantiates new surfaces for all registers that need them</li>
 *
 *     <li>assigns the surface for each register to all the proxies that will use it</li>
 * </ul>
 * <p>
 * ************************************************************************************************
 * How does instantiation failure handling work when explicitly allocating?
 * <p>
 * In the gather usage intervals pass all the SurfaceProxies used in the flush should be
 * gathered (i.e., in {@link RenderTask#gatherProxyIntervals(ResourceAllocator)}).
 * <p>
 * During addInterval, read-only lazy proxies are instantiated. If that fails, the resource
 * allocator will note the failure and ignore pretty much anything else until `reset`.
 * <p>
 * During {@link #simulate()}, lazy-most proxies are instantiated so that we can know their size for
 * budgeting purposes. If this fails, return false.
 * <p>
 * During {@link #allocate()}, lazy proxies are instantiated and new surfaces are created for all other
 * proxies. If any of these fails, return false.
 * <p>
 * The drawing manager will drop the flush if any proxies fail to instantiate.
 */
public final class ResourceAllocator {

    private final DirectContext mContext;

    // All the intervals, hashed by proxy ID
    private final Reference2ObjectOpenHashMap<Object, Interval> mIntervalHash =
            new Reference2ObjectOpenHashMap<>();

    // All the intervals sorted by increasing start
    private final IntervalList mIntervalList = new IntervalList();
    // List of live intervals during dispatch
    // (sorted by increasing end)
    private final IntervalList mLiveIntervals = new IntervalList();
    // All the completed intervals
    // (sorted by increasing start)
    private final IntervalList mFinishedIntervals = new IntervalList();

    // Recently created/used textures
    private final ArrayDequeMultimap<TextureProxy, Register> mFreePool = new ArrayDequeMultimap<>();

    private final Object2ObjectOpenHashMap<Object, Register> mUniqueKeyRegisters =
            new Object2ObjectOpenHashMap<>();
    private int mNumOps;

    private boolean mSimulated;
    private boolean mAllocated;

    private boolean mInstantiationFailed;

    public ResourceAllocator(DirectContext context) {
        mContext = context;
    }

    public int curOp() {
        return mNumOps;
    }

    public void incOps() {
        mNumOps++;
    }

    /**
     * Add a usage interval from <code>start</code> to <code>end</code> inclusive.
     * This is usually used for render targets. If an existing interval already exists
     * it will be expanded to include the new range.
     * <p>
     * ActualUse: Indicates whether a given call to addInterval represents an actual usage of the
     * provided proxy. This is mainly here to accommodate deferred proxies attached to opsTasks.
     * In that case we need to create an extra long interval for them (due to the upload) but
     * don't want to count that usage/reference towards the proxy's recyclability.
     *
     * @param proxy the raw ptr to the surface proxy
     * @param start the start op
     * @param end   the end op
     */
    public void addInterval(@NonNull SurfaceProxy proxy, int start, int end, boolean actualUse) {
        assert (start <= end);
        // We shouldn't be adding any intervals after (or during) allocation
        assert (!mAllocated);

        if (proxy.shouldSkipAllocator()) {
            return;
        }

        // If a proxy is read only it must refer to a texture with specific content that cannot be
        // recycled. We don't need to assign a texture to it and no other proxy can be instantiated
        // with the same texture.
        if (proxy.isReadOnly()) {
            ResourceProvider resourceProvider = mContext.getResourceProvider();
            if (proxy.isLazy() && !proxy.doLazyInstantiation(resourceProvider)) {
                mInstantiationFailed = true;
            } else {
                // Since we aren't going to add an interval we won't revisit this proxy in allocate(). So
                // must already be instantiated or it must be a lazy proxy that we instantiated above.
                assert (proxy.isInstantiated());
            }
            return;
        }
        Object proxyID = proxy.getUniqueID();
        Interval interval = mIntervalHash.get(proxyID);
        if (interval != null) {
            // Revise the interval for an existing use
            if (actualUse) {
                interval.mUses++;
            }
            if (end > interval.mEnd) {
                interval.mEnd = end;
            }
            return;
        }
        Interval newInterval = makeInterval(proxy, start, end);

        if (actualUse) {
            newInterval.mUses++;
        }
        mIntervalList.insertByIncreasingStart(newInterval);
        mIntervalHash.put(proxyID, newInterval);
    }

    public boolean isInstantiationFailed() {
        return mInstantiationFailed;
    }

    /**
     * Generate an internal plan for resource allocation.
     * <p>
     * Lazy-most proxies are also instantiated at this point so that their size can
     * be known accurately. Returns false if any lazy proxy failed to instantiate, true otherwise.
     */
    public boolean simulate() {
        // we don't need the interval h anymore
        mIntervalHash.clear();

        assert (!mSimulated && !mAllocated);
        mSimulated = true;

        ResourceProvider resourceProvider = mContext.getResourceProvider();
        for (Interval cur = mIntervalList.peekHead(); cur != null; cur = cur.mNext) {
            expire(cur.mStart);
            mLiveIntervals.insertByIncreasingEnd(cur);

            // Already-instantiated proxies and lazy proxies don't use registers.
            if (cur.mProxy.isInstantiated()) {
                continue;
            }

            // Instantiate lazy-most proxies immediately. Ignore other lazy proxies at this stage.
            if (cur.mProxy.isLazy()) {
                if (cur.mProxy.isLazyMost()) {
                    mInstantiationFailed = !cur.mProxy.doLazyInstantiation(resourceProvider);
                    if (mInstantiationFailed) {
                        break;
                    }
                }
                continue;
            }

            // It must be a texture proxy in this case.
            // We don't know how to instantiate a pure render target without a texture.
            TextureProxy textureProxy = cur.mProxy.asTextureProxy();
            assert (textureProxy != null);
            Register r = findOrCreateRegister(textureProxy, resourceProvider);
            assert (textureProxy.peekTexture() == null);
            cur.mRegister = r;
        }

        // expire all the remaining intervals to drain the active interval list
        expire(Integer.MAX_VALUE);
        return !mInstantiationFailed;
    }

    /**
     * Instantiate and assign resources to all proxies.
     */
    public boolean allocate() {
        if (mInstantiationFailed) {
            return false;
        }
        assert (mSimulated && !mAllocated);
        mAllocated = true;
        ResourceProvider resourceProvider = mContext.getResourceProvider();
        Interval cur;
        while ((cur = mFinishedIntervals.popHead()) != null) {
            if (mInstantiationFailed) {
                break;
            }
            if (cur.mProxy.isInstantiated()) {
                continue;
            }
            if (cur.mProxy.isLazy()) {
                mInstantiationFailed = !cur.mProxy.doLazyInstantiation(resourceProvider);
                continue;
            }
            Register r = cur.mRegister;
            assert (r != null);
            // It must be a texture proxy in this case.
            // We don't know how to instantiate a pure render target without a texture.
            TextureProxy textureProxy = cur.mProxy.asTextureProxy();
            assert (textureProxy != null);
            mInstantiationFailed = !r.instantiateTexture(textureProxy, resourceProvider);
        }
        return !mInstantiationFailed;
    }

    /**
     * Called after {@link #simulate()} or {@link #allocate()} on the end of flush.
     */
    public void reset() {
        mNumOps = 0;
        mSimulated = false;
        mAllocated = false;
        mInstantiationFailed = false;
        assert (mLiveIntervals.isEmpty());
        mFinishedIntervals.clear();
        Interval cur;
        while ((cur = mIntervalList.popHead()) != null) {
            // registers may be shared
            if (cur.mRegister != null && cur.mRegister.reset()) {
                freeRegister(cur.mRegister);
            }
            if (cur.reset()) {
                freeInterval(cur);
            } else {
                assert false;
            }
        }
        mIntervalList.clear();
        mIntervalHash.clear();
        mFreePool.clear();
        mUniqueKeyRegisters.clear();
    }

    // Remove any intervals that end before the current index. Add their registers
    // to the free pool if possible.
    private void expire(int curIndex) {
        while (!mLiveIntervals.isEmpty() && mLiveIntervals.peekHead().mEnd < curIndex) {
            Interval interval = mLiveIntervals.popHead();
            assert (interval.mNext == null);

            Register r = interval.mRegister;
            if (r != null && r.isRecyclable(interval.mProxy, interval.mUses)) {
                mFreePool.addLastEntry(r.mProxy, r);
            }
            mFinishedIntervals.insertByIncreasingStart(interval);
        }
    }

    private Register findOrCreateRegister(@NonNull TextureProxy proxy,
                                          ResourceProvider provider) {
        Register r;
        // Handle uniquely keyed proxies
        Object uniqueKey = proxy.getUniqueKey();
        if (uniqueKey != null) {
            r = mUniqueKeyRegisters.get(uniqueKey);
            if (r != null) {
                return r;
            }
            // No need for a scratch key. These don't go in the free pool.
            r = makeRegister(proxy, provider, false);
            mUniqueKeyRegisters.put(uniqueKey, r);
            return r;
        }

        // Then look in the free pool
        r = mFreePool.pollFirstEntry(proxy);
        if (r != null) {
            return r;
        }

        return makeRegister(proxy, provider, true);
    }

    private static class Register {

        /**
         * The proxy is the originating proxy. The texture is queried from resource cache,
         * can be null.
         * <p>
         * When the proxy's unique key is null, we assume its scratch key is valid and
         * the key is the proxy itself.
         */
        private TextureProxy mProxy;
        @SharedPtr
        private Texture mTexture;

        private boolean mInit;

        public Register(TextureProxy proxy,
                        ResourceProvider provider,
                        boolean scratch) {
            init(proxy, provider, scratch);
        }

        public Register init(TextureProxy proxy,
                             ResourceProvider provider,
                             boolean scratch) {
            assert (!mInit);
            assert (proxy != null);
            assert (!proxy.isInstantiated());
            assert (!proxy.isLazy());
            mProxy = proxy;
            if (scratch) {
                mTexture = provider.findAndRefScratchTexture(proxy, null);
            } else {
                assert (proxy.getUniqueKey() != null);
                mTexture = provider.findByUniqueKey(proxy.getUniqueKey());
            }
            mInit = true;
            return this;
        }

        public boolean isRecyclable(SurfaceProxy proxy, int knownUseCount) {
            if (mProxy.getUniqueKey() != null) {
                // rely on the resource cache to hold onto uniquely-keyed textures.
                return false;
            }
            assert (proxy.asTextureProxy() != null);
            // If all the refs on the proxy are known to the resource allocator then no one
            // should be holding onto it outside of engine.
            return !refCntGreaterThan(proxy, knownUseCount);
        }

        /**
         * Internal only. This must be used with caution. It is only valid to call this when
         * <code>threadIsolatedTestCnt</code> refs are known to be isolated to the current thread.
         * That is, it is known that there are at least <code>threadIsolatedTestCnt</code> refs
         * for which no other thread may make a balancing {@link SurfaceProxy#unref()} call.
         * Assuming the contract is followed, if this returns false then no other thread has
         * ownership of this. If it returns true then another thread <em>may</em> have ownership.
         */
        public boolean refCntGreaterThan(SurfaceProxy proxy, int threadIsolatedTestCnt) {
            int cnt = proxy.getRefCntAcquire();
            // If this fails then the above contract has been violated.
            assert (cnt >= threadIsolatedTestCnt);
            return cnt > threadIsolatedTestCnt;
        }

        // Resolve the register allocation to an actual Texture. 'mProxy' is used
        // to cache the allocation when a given register is used by multiple proxies.
        public boolean instantiateTexture(TextureProxy proxy,
                                          ResourceProvider resourceProvider) {
            assert (proxy.peekTexture() == null);
            final Texture texture;
            if (mTexture == null) {
                if (mProxy == proxy) {
                    texture = proxy.createTexture(resourceProvider);
                } else {
                    texture = Resource.create(mProxy.peekTexture());
                }
                if (texture == null) {
                    return false;
                }
            } else {
                texture = Resource.create(mTexture);
            }
            assert (texture != null);
            assert (proxy.mSurfaceFlags & Surface.FLAG_RENDERABLE) == 0 || texture.getRenderTarget() != null;

            // Make texture budgeted if this proxy is budgeted.
            if (proxy.isBudgeted() && texture.getBudgetType() != Engine.BudgetType.Budgeted) {
                texture.makeBudgeted(true);
            }

            // Propagate the proxy unique key to the texture if we have one.
            if (proxy.getUniqueKey() != null) {
                if (texture.getUniqueKey() == null) {
                    resourceProvider.assignUniqueKeyToResource(proxy.getUniqueKey(), texture);
                }
                assert proxy.getUniqueKey().equals(texture.getUniqueKey());
            }
            assert proxy.mTexture == null;
            proxy.mTexture = texture;
            return true;
        }

        /**
         * Recycle this register if not.
         *
         * @return true if recycled, false if already recycled before the call
         */
        public boolean reset() {
            if (mInit) {
                mProxy = null;
                mTexture = Resource.move(mTexture);
                mInit = false;
                return true;
            }
            assert (mProxy == null);
            assert (mTexture == null);
            return false;
        }
    }

    private static class Interval {

        private SurfaceProxy mProxy;
        private int mStart;
        private int mEnd;
        private Interval mNext;
        private int mUses;
        private Register mRegister;

        private boolean mInit;

        public Interval(SurfaceProxy proxy, int start, int end) {
            init(proxy, start, end);
        }

        public Interval init(SurfaceProxy proxy, int start, int end) {
            assert (!mInit);
            assert (proxy != null);
            mProxy = proxy;
            mStart = start;
            mEnd = end;
            mNext = null;
            mUses = 0;
            mRegister = null;
            mInit = true;
            return this;
        }

        /**
         * Recycle this interval if not.
         *
         * @return true if recycled, false if already recycled before the call
         */
        public boolean reset() {
            if (mInit) {
                mProxy = null;
                mNext = null;
                mRegister = null;
                mInit = false;
                return true;
            }
            assert (mProxy == null);
            assert (mNext == null);
            assert (mRegister == null);
            return false;
        }
    }

    private static class IntervalList {

        private Interval mHead;
        private Interval mTail;

        public IntervalList() {
        }

        public void clear() {
            mHead = mTail = null;
        }

        public boolean isEmpty() {
            assert ((mHead == null) == (mTail == null));
            return mHead == null;
        }

        public Interval peekHead() {
            return mHead;
        }

        public Interval popHead() {
            Interval temp = mHead;
            if (temp != null) {
                mHead = temp.mNext;
                if (mHead == null) {
                    mTail = null;
                }
                temp.mNext = null;
            }
            return temp;
        }

        public void insertByIncreasingStart(@NonNull Interval interval) {
            assert (interval.mNext == null);

            if (mHead == null) {
                // 14%
                mHead = mTail = interval;
            } else if (interval.mStart <= mHead.mStart) {
                // 3%
                interval.mNext = mHead;
                mHead = interval;
            } else if (mTail.mStart <= interval.mStart) {
                // 83%
                mTail.mNext = interval;
                mTail = interval;
            } else {
                // almost never
                Interval prev = mHead;
                Interval next = prev.mNext;
                while (interval.mStart > next.mStart) {
                    prev = next;
                    next = next.mNext;
                }

                interval.mNext = next;
                prev.mNext = interval;
            }
        }

        public void insertByIncreasingEnd(@NonNull Interval interval) {
            assert (interval.mNext == null);

            if (mHead == null) {
                // 14%
                mHead = mTail = interval;
            } else if (interval.mEnd <= mHead.mEnd) {
                // 64%
                interval.mNext = mHead;
                mHead = interval;
            } else if (mTail.mEnd <= interval.mEnd) {
                // 3%
                mTail.mNext = interval;
                mTail = interval;
            } else {
                // 19% but 81% of those land right after the list's head
                Interval prev = mHead;
                Interval next = prev.mNext;
                while (interval.mEnd > next.mEnd) {
                    prev = next;
                    next = next.mNext;
                }

                interval.mNext = next;
                prev.mNext = interval;
            }
        }
    }

    // internal object pool

    private final Register[] mRegisterPool = new Register[128];
    private int mRegisterPoolSize;

    private final Interval[] mIntervalPool = new Interval[128];
    private int mIntervalPoolSize;

    private Register makeRegister(@NonNull TextureProxy proxy,
                                  ResourceProvider provider,
                                  boolean scratch) {
        if (mRegisterPoolSize == 0)
            return new Register(proxy, provider, scratch);
        return mRegisterPool[--mRegisterPoolSize].init(proxy, provider, scratch);
    }

    public void freeRegister(@NonNull Register register) {
        if (mRegisterPoolSize == mRegisterPool.length)
            return;
        mRegisterPool[mRegisterPoolSize++] = register;
    }

    private Interval makeInterval(@NonNull SurfaceProxy proxy, int start, int end) {
        if (mIntervalPoolSize == 0)
            return new Interval(proxy, start, end);
        return mIntervalPool[--mIntervalPoolSize].init(proxy, start, end);
    }

    private void freeInterval(@NonNull Interval interval) {
        if (mIntervalPoolSize == mIntervalPool.length)
            return;
        mIntervalPool[mIntervalPoolSize++] = interval;
    }
}
