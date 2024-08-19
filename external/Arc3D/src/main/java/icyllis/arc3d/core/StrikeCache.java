/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.core;

import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public final class StrikeCache {

    private static final StrikeCache gCache = new StrikeCache();

    final ReentrantLock mLock = new ReentrantLock();

    @GuardedBy("mLock")
    Strike mHead;
    @GuardedBy("mLock")
    Strike mTail;
    @GuardedBy("mLock")
    private final HashMap<StrikeDesc, Strike> mStrikes =
            new HashMap<>();

    @GuardedBy("mLock")
    long mCacheSizeLimit = 2 << 20;
    @GuardedBy("mLock")
    long mTotalMemoryUsed;
    @GuardedBy("mLock")
    int mCacheCountLimit = 2 << 10;
    @GuardedBy("mLock")
    int mCacheCount;

    // Use global instance
    @ApiStatus.Internal
    public StrikeCache() {
    }

    @Nonnull
    public static StrikeCache getGlobalStrikeCache() {
        return gCache;
    }

    // this method excludes lock
    // this method will not modify 'desc'
    @Nonnull
    public Strike findOrCreateStrike(@Nonnull StrikeDesc desc) {
        mLock.lock();
        try {
            Strike strike = internalFindStrike(desc);
            if (strike == null) {
                strike = internalCreateStrike(desc.copy());
            }
            internalPurge(0);
            return strike;
        } finally {
            mLock.unlock();
        }
    }

    private long internalPurge(long minBytesNeeded) {
        if (mCacheCount == 0) {
            return 0;
        }

        long bytesNeeded = 0;
        if (mTotalMemoryUsed > mCacheSizeLimit) {
            bytesNeeded = mTotalMemoryUsed - mCacheSizeLimit;
        }
        bytesNeeded = Math.max(bytesNeeded, minBytesNeeded);
        if (bytesNeeded != 0) {
            // no small purges!
            bytesNeeded = Math.max(bytesNeeded, mTotalMemoryUsed >> 2);
        }

        int countNeeded = 0;
        if (mCacheCount > mCacheCountLimit) {
            countNeeded = mCacheCount - mCacheCountLimit;
            // no small purges!
            countNeeded = Math.max(countNeeded, mCacheCount >> 2);
        }

        // early exit
        if (countNeeded == 0 && bytesNeeded == 0) {
            return 0;
        }

        long bytesFreed = 0;
        int countFreed = 0;

        // Start at the tail and proceed backwards deleting; the list is in LRU
        // order, with unimportant entries at the tail.
        Strike strike = mTail;
        while (strike != null && (bytesFreed < bytesNeeded || countFreed < countNeeded)) {
            Strike prev = strike.mPrev;

            // Only delete if the strike is not pinned.
            bytesFreed += strike.mMemoryUsed;
            countFreed += 1;

            mCacheCount -= 1;
            mTotalMemoryUsed -= strike.mMemoryUsed;

            if (strike.mPrev != null) {
                strike.mPrev.mNext = strike.mNext;
            } else {
                mHead = strike.mNext;
            }
            if (strike.mNext != null) {
                strike.mNext.mPrev = strike.mPrev;
            } else {
                mTail = strike.mPrev;
            }

            strike.mPrev = strike.mNext = null;
            strike.mRemoved = true;
            var old = mStrikes.remove(strike.getStrikeDesc());
            assert old == strike;

            strike = prev;
        }

        return bytesFreed;
    }

    @Nullable
    private Strike internalFindStrike(@Nonnull StrikeDesc desc) {
        // Check head because it is likely the strike we are looking for.
        if (mHead != null && mHead.getStrikeDesc().equals(desc)) {
            return mHead;
        }

        // Do the heavy search looking for the strike.
        Strike strike = mStrikes.get(desc);
        if (strike == null) {
            return null;
        }
        if (mHead != strike) {
            // Make most recently used
            strike.mPrev.mNext = strike.mNext;
            if (strike.mNext != null) {
                strike.mNext.mPrev = strike.mPrev;
            } else {
                mTail = strike.mPrev;
            }
            mHead.mPrev = strike;
            strike.mNext = mHead;
            strike.mPrev = null;
            mHead = strike;
        }
        return strike;
    }

    @Nonnull
    private Strike internalCreateStrike(@Nonnull StrikeDesc desc) {
        var scalerContext = desc.createScalerContext();
        var strike = new Strike(this, desc, scalerContext);

        var old = mStrikes.put(desc, strike);
        assert old == null;
        assert strike.mPrev == null && strike.mNext == null;

        mCacheCount += 1;
        mTotalMemoryUsed += strike.mMemoryUsed;

        if (mHead != null) {
            mHead.mPrev = strike;
            strike.mNext = mHead;
        }

        if (mTail == null) {
            mTail = strike;
        }

        mHead = strike;
        return strike;
    }

    // this method excludes lock
    public long getTotalMemoryUsed() {
        mLock.lock();
        try {
            return mTotalMemoryUsed;
        } finally {
            mLock.unlock();
        }
    }
}
