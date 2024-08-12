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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.*;
import java.util.Collection;
import java.util.HashMap;

/**
 * TextBlobCache reuses data from previous drawing operations using multiple criteria
 * to pick the best data for the draw. In addition, it provides a central service for managing
 * resource usage through a weak reference.
 * <p>
 * The draw data is stored in a three-tiered system. The first tier is keyed by the TextBlob's
 * identity. The second tier uses the {@link FeatureKey} to get a general match for the
 * draw. The last tier queries each sub run using canReuse to determine if each sub run can handle
 * the drawing parameters.
 */
//TODO not well tested
public final class TextBlobCache {

    /**
     * Secondary cache key.
     */
    public static final class FeatureKey {

        // from position matrix
        private float mScaleX;
        private float mScaleY;
        private float mShearX;
        private float mShearY;

        private float mFrameWidth;
        private float mMiterLimit;

        private boolean mHasDirectSubRuns;

        private byte mStyle;
        private byte mStrokeJoin;

        public FeatureKey() {
        }

        public FeatureKey(FeatureKey other) {
            mScaleX = other.mScaleX;
            mScaleY = other.mScaleY;
            mShearX = other.mShearX;
            mShearY = other.mShearY;
            mFrameWidth = other.mFrameWidth;
            mMiterLimit = other.mMiterLimit;
            mHasDirectSubRuns = other.mHasDirectSubRuns;
            mStyle = other.mStyle;
            mStrokeJoin = other.mStrokeJoin;
        }

        public void update(GlyphRunList glyphRunList,
                           Paint paint,
                           Matrixc positionMatrix) {
            mStyle = (byte) paint.getStyle();
            if (mStyle != Paint.FILL) {
                mFrameWidth = paint.getStrokeWidth();
                mStrokeJoin = (byte) paint.getStrokeJoin();
                if (mStrokeJoin == Paint.JOIN_MITER) {
                    mMiterLimit = paint.getStrokeMiter();
                } else {
                    mMiterLimit = 0;
                }
            } else {
                mFrameWidth = -1;
                mStrokeJoin = 0;
                mMiterLimit = 0;
            }

            //TODO try to avoid use direct sub runs for animations

            //TODO keep sync with SubRunContainer factory method
            mHasDirectSubRuns = !positionMatrix.hasPerspective();
            if (mHasDirectSubRuns) {
                int typeMask = positionMatrix.getType();
                if ((typeMask & Matrixc.kScale_Mask) != 0) {
                    mScaleX = StrikeDesc.round_mat_elem(positionMatrix.getScaleX());
                    mScaleY = StrikeDesc.round_mat_elem(positionMatrix.getScaleY());
                } else {
                    mScaleX = mScaleY = 1;
                }
                if ((typeMask & Matrixc.kAffine_Mask) != 0) {
                    mShearX = StrikeDesc.round_mat_elem(positionMatrix.getShearX());
                    mShearY = StrikeDesc.round_mat_elem(positionMatrix.getShearY());
                } else {
                    mShearX = mShearY = 0;
                }
            } else {
                mScaleX = 1;
                mScaleY = 1;
                mShearX = 0;
                mShearY = 0;
            }
        }

        @Override
        public int hashCode() {
            int h = Float.floatToIntBits(mScaleX);
            h = 31 * h + Float.floatToIntBits(mScaleY);
            h = 31 * h + Float.floatToIntBits(mShearX);
            h = 31 * h + Float.floatToIntBits(mShearY);
            h = 31 * h + Float.floatToIntBits(mFrameWidth);
            h = 31 * h + Float.floatToIntBits(mMiterLimit);
            h = 31 * h + (mHasDirectSubRuns ? 1 : 0);
            h = 31 * h + (int) mStyle;
            h = 31 * h + (int) mStrokeJoin;
            return h;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof FeatureKey that) {
                return mHasDirectSubRuns == that.mHasDirectSubRuns &&
                        mScaleX == that.mScaleX &&
                        mScaleY == that.mScaleY &&
                        mShearX == that.mShearX &&
                        mShearY == that.mShearY &&
                        mFrameWidth == that.mFrameWidth &&
                        mMiterLimit == that.mMiterLimit &&
                        mStyle == that.mStyle &&
                        mStrokeJoin == that.mStrokeJoin;
            }
            return false;
        }
    }

    /**
     * Primary cache key, identity weak reference, see {@link TextBlob#equals(Object)}
     */
    static final class PrimaryKey extends WeakReference<TextBlob> {

        private final int hash;

        PrimaryKey(TextBlob referent, ReferenceQueue<TextBlob> q) {
            super(referent, q);
            hash = referent.hashCode();
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            // use WeakReference identity
            if (this == o) {
                return true;
            }
            // use TextBlob identity
            if (o instanceof Reference<?> r) {
                return get() == r.get();
            }
            return false;
        }
    }

    private final Object mLock = new Object();

    private final HashMap<Object, Bucket> mMap = new HashMap<>();
    private final ReferenceQueue<TextBlob> mQueue = new ReferenceQueue<>();

    private BakedTextBlob mHead;
    private BakedTextBlob mTail;

    private long mSizeBudget = 1 << 22;
    private long mCurrentSize;

    @Nullable
    public BakedTextBlob find(@Nonnull TextBlob blob, @Nonnull FeatureKey key) {
        synchronized (mLock) {
            Bucket bucket = mMap.get(blob);
            if (bucket == null) {
                return null;
            }
            BakedTextBlob entry = bucket.find(key);
            if (entry != null) {
                moveToHead(entry);
            }
            return entry;
        }
    }

    @Nonnull
    public BakedTextBlob insert(@Nonnull TextBlob blob, @Nonnull FeatureKey key,
                                @Nonnull BakedTextBlob entry) {
        synchronized (mLock) {
            return internalInsert(blob, key, entry);
        }
    }

    public void remove(@Nonnull BakedTextBlob entry) {
        synchronized (mLock) {
            internalRemove(entry);
        }
    }

    public void purgeStaleEntries() {
        synchronized (mLock) {
            internalPurgeStaleEntries();
        }
    }

    @Nonnull
    private BakedTextBlob internalInsert(@Nonnull TextBlob blob, @Nonnull FeatureKey key,
                                         @Nonnull BakedTextBlob entry) {
        Bucket bucket = mMap.get(blob);
        if (bucket == null) {
            PrimaryKey primaryKey = new PrimaryKey(blob, mQueue);
            bucket = new Bucket(primaryKey);
            mMap.put(primaryKey, bucket);
            assert mMap.get(blob) == bucket;
        }

        BakedTextBlob existing = bucket.find(key);
        if (existing != null) {
            entry = existing;
        } else {
            FeatureKey copiedKey = new FeatureKey(key);
            entry.mPrimaryKey = bucket.mPrimaryKey;
            entry.mFeatureKey = copiedKey;
            addToHead(entry);
            mCurrentSize += entry.getMemorySize();
            bucket.insertEntry(entry);
        }

        internalCheckPurge(entry);
        return entry;
    }

    private void internalRemove(@Nonnull BakedTextBlob entry) {
        Bucket bucket = mMap.get(entry.mPrimaryKey);

        if (bucket != null) {
            var old = bucket.find(entry.mFeatureKey);
            if (entry == old) {
                mCurrentSize -= entry.getMemorySize();
                unlink(entry);
                bucket.removeEntry(entry);
                if (bucket.isEmpty()) {
                    mMap.remove(entry.mPrimaryKey);
                }
            }
        }
    }

    private void internalCheckPurge(BakedTextBlob mru) {
        internalPurgeStaleEntries();

        if (mCurrentSize > mSizeBudget) {
            for (var lru = mTail; lru != null && lru != mru && mCurrentSize > mSizeBudget; lru = lru.mPrev) {
                internalRemove(lru);
            }
        }
    }

    private void internalPurgeStaleEntries() {
        Object r;
        while ((r = mQueue.poll()) != null) {
            Bucket bucket = mMap.remove(r);
            if (bucket == null) {
                continue;
            }
            for (var e : bucket.entries()) {
                mCurrentSize -= e.getMemorySize();
                unlink(e);
            }
        }
    }

    private void unlink(@Nonnull BakedTextBlob entry) {
        BakedTextBlob prev = entry.mPrev;
        BakedTextBlob next = entry.mNext;

        if (prev != null) {
            prev.mNext = next;
        } else {
            mHead = next;
        }
        if (next != null) {
            next.mPrev = prev;
        } else {
            mTail = prev;
        }

        entry.mPrev = null;
        entry.mNext = null;
    }

    private void addToHead(@Nonnull BakedTextBlob entry) {
        entry.mPrev = null;
        entry.mNext = mHead;
        if (mHead != null) {
            mHead.mPrev = entry;
        }
        mHead = entry;
        if (mTail == null) {
            mTail = entry;
        }
    }

    private void moveToHead(@Nonnull BakedTextBlob entry) {
        assert mHead != null && mTail != null;
        if (mHead == entry) {
            return;
        }

        BakedTextBlob prev = entry.mPrev;
        BakedTextBlob next = entry.mNext;

        if (prev != null) {
            prev.mNext = next;
        } else {
            mHead = next;
        }
        if (next != null) {
            next.mPrev = prev;
        } else {
            mTail = prev;
        }

        entry.mPrev = null;
        entry.mNext = mHead;
        if (mHead != null) {
            mHead.mPrev = entry;
        }
        mHead = entry;
        if (mTail == null) {
            mTail = entry;
        }
    }

    private static class Bucket {

        final PrimaryKey mPrimaryKey;

        // If there are not too many entries, use linear search, otherwise use HashMap
        private ObjectArrayList<BakedTextBlob> mList = new ObjectArrayList<>(8);
        private HashMap<FeatureKey, BakedTextBlob> mMap;

        Bucket(PrimaryKey primaryKey) {
            mPrimaryKey = primaryKey;
        }

        @Nullable
        BakedTextBlob find(@Nonnull FeatureKey key) {
            if (mMap != null) {
                return mMap.get(key);
            } else {
                for (BakedTextBlob e : mList) {
                    if (key.equals(e.mFeatureKey)) {
                        return e;
                    }
                }
                return null;
            }
        }

        void insertEntry(@Nonnull BakedTextBlob entry) {
            if (mMap != null) {
                mMap.put(entry.mFeatureKey, entry);
            } else if (mList.size() >= 8) {
                mMap = new HashMap<>();
                for (var e : mList) {
                    mMap.put(e.mFeatureKey, e);
                }
                mList = null;
                mMap.put(entry.mFeatureKey, entry);
            } else {
                mList.add(entry);
            }
        }

        void removeEntry(@Nonnull BakedTextBlob entry) {
            if (mMap != null) {
                var old = mMap.remove(entry.mFeatureKey);
                assert old == entry;
            } else {
                for (int i = 0; i < mList.size(); i++) {
                    if (entry.mFeatureKey.equals(mList.get(i).mFeatureKey)) {
                        mList.remove(i);
                        return;
                    }
                }
            }
        }

        boolean isEmpty() {
            if (mMap != null) {
                return mMap.isEmpty();
            } else {
                return mList.isEmpty();
            }
        }

        Collection<BakedTextBlob> entries() {
            if (mMap != null) {
                return mMap.values();
            } else {
                return mList;
            }
        }
    }
}
