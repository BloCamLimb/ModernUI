/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.sketch;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jspecify.annotations.NonNull;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.concurrent.GuardedBy;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The font strike holds the results from {@link ScalerContext}.
 */
@ApiStatus.Experimental
//TODO this class may be ref-cnted in the future
public final class Strike {

    // The following are const and need no lock protection.
    private final StrikeDesc mStrikeDesc;
    private final StrikeCache mStrikeCache;

    private final float mSubpixelRounding;
    private final int mSubpixelFieldMask;

    private final ReentrantLock mLock = new ReentrantLock();

    @GuardedBy("mLock")
    private final Int2ObjectOpenHashMap<Glyph> mGlyphs =
            new Int2ObjectOpenHashMap<>();
    @GuardedBy("mLock")
    private final ScalerContext mScalerContext;
    @GuardedBy("mLock")
    private long mMemoryIncrease;

    // The following are guarded by mStrikeCache.mLock
    Strike mNext;
    Strike mPrev;
    long mMemoryUsed;
    boolean mRemoved;

    // Use StrikeCache to obtain an instance
    @ApiStatus.Internal
    public Strike(@NonNull StrikeCache strikeCache,
                  @NonNull StrikeDesc strikeDesc, // immutable, no copy
                  @NonNull ScalerContext scalerContext) {
        assert strikeDesc.isImmutable();
        mStrikeDesc = strikeDesc;
        mStrikeCache = strikeCache;
        mScalerContext = scalerContext;
        if (scalerContext.isSubpixel()) {
            mSubpixelRounding = 1.0f / (1 << (Glyph.kSubPixelPosLen + 1)); // 1/8
            mSubpixelFieldMask = Glyph.kSubPixelXMask;
        } else {
            mSubpixelRounding = 0.5f; // 1/2
            mSubpixelFieldMask = 0;
        }
        // approximate bytes used
        mMemoryUsed = 16 + 56 + 8 + 8 + 8 + 8 + 80 + 8 + 8 + 8 + 8 + 8 + 8;
    }

    public void lock() {
        mLock.lock();
        mMemoryIncrease = 0;
    }

    public void unlock() {
        final long increase = mMemoryIncrease;
        mLock.unlock();
        if (increase > 0) {
            // mRemoved and the cache's total memory are managed under the cache's lock. This allows
            // them to be accessed under LRU operation.
            mStrikeCache.mLock.lock();
            try {
                mMemoryUsed += increase;
                if (!mRemoved) {
                    mStrikeCache.mTotalMemoryUsed += increase;
                }
            } finally {
                mStrikeCache.mLock.unlock();
            }
        }
    }

    /**
     * Find or create a glyph for the given glyph ID, return the pointer to it.
     * <p>
     * Requires lock.
     *
     * @param packedID typeface-specified glyph ID
     */
    @NonNull
    public Glyph getGlyph(int packedID) {
        return digestFor(Glyph.kDirectMask, packedID);
    }

    /**
     * Find or create a glyph for the given glyph ID, return the pointer to it.
     * Get or compute the digest for the given action type, ensure that
     * {@link Glyph#actionFor(int)} is set.
     * <p>
     * Requires lock.
     *
     * @param actionType e.g. {@link Glyph#kDirectMask}
     * @param packedID   typeface-specified glyph ID
     */
    @NonNull
    public Glyph digestFor(int actionType, int packedID) {
        assert mLock.isLocked();
        Glyph glyph = mGlyphs.get(packedID);
        if (glyph != null && glyph.actionFor(actionType) != Glyph.kUnset_Action) {
            return glyph;
        }

        if (glyph == null) {
            glyph = mScalerContext.makeGlyph(packedID);
            glyph.initActions();
            mGlyphs.put(packedID, glyph);
            mMemoryIncrease += Glyph.kSizeOf;
            if (glyph.setPathHasBeenCalled()) {
                Path path = glyph.getPath();
                if (path != null) {
                    mMemoryIncrease += path.estimatedByteSize();
                }
            }
        }

        glyph.setActionFor(actionType, this);

        return glyph;
    }

    /**
     * Prepare the glyph to draw an image, and return if the image exists.
     * <p>
     * Requires lock.
     */
    public boolean prepareForImage(@NonNull Glyph glyph) {
        assert mLock.isLocked();
        if (glyph.setImage(mScalerContext)) {
            mMemoryIncrease += glyph.getImageSize() + 16;
        }
        return glyph.getImageBase() != null;
    }

    /**
     * Prepare the glyph to draw a path, and return if the path exists.
     * <p>
     * Requires lock.
     */
    public boolean prepareForPath(Glyph glyph) {
        assert mLock.isLocked();
        if (glyph.setPath(mScalerContext)) {
            mMemoryIncrease += glyph.getPath().estimatedByteSize();
        }
        return glyph.getPath() != null;
    }


    /**
     * Compute metrics for a list of glyphs in bulk. The glyph metrics within
     * the given range will be computed, and the glyph pointers will be stored
     * in the results array and returned.
     * <p>
     * Excludes lock.
     */
    public Glyph @NonNull [] getMetrics(int @NonNull [] glyphs, int glyphOffset, int glyphCount,
                                        Glyph @NonNull [] results) {
        assert results.length >= glyphCount;
        lock();
        try {
            for (int i = glyphOffset, e = glyphOffset + glyphCount, j = 0; i < e; i++, j++) {
                Glyph glyph = getGlyph(glyphs[i]);
                results[j] = glyph;
            }
            return results;
        } finally {
            unlock();
        }
    }

    // immutable
    public @NonNull StrikeDesc getStrikeDesc() {
        return mStrikeDesc;
    }

    public float getSubpixelRounding() {
        return mSubpixelRounding;
    }

    public int getSubpixelFieldMask() {
        return mSubpixelFieldMask;
    }
}
