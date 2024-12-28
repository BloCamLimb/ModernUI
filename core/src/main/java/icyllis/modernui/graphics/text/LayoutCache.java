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

package icyllis.modernui.graphics.text;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.util.Pools;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Globally shared layout cache. Useful when recycling layouts, or raw data source and
 * layout information are separated. Max memory usage: 7~10 MB.
 *
 * @see LayoutPiece
 * @since 2.6
 */
@ThreadSafe
public final class LayoutCache {

    /**
     * The internal policy on the maximum length of a text run, shared globally.
     */
    public static final int MAX_PIECE_LENGTH = 128;

    /**
     * Also computes per-cluster advances.
     *
     * @see LayoutPiece#getAdvances()
     */
    public static final int COMPUTE_CLUSTER_ADVANCES = 0x1;
    /**
     * Also computes total pixel bounds of all glyph images.
     *
     * @see LayoutPiece#getBoundsX()
     * @see LayoutPiece#getBoundsY()
     */
    public static final int COMPUTE_GLYPHS_PIXEL_BOUNDS = 0x2;

    //TODO make this configurable
    private static final int MAX_ENTRIES = 5000;

    private static final Pools.Pool<LookupKey> sLookupKeys = Pools.newSynchronizedPool(3);
    @GuardedBy("itself")
    private static final LinkedHashMap<Key, LayoutPiece> sCache = new LinkedHashMap<>(
            /*initialCapacity*/ (int) (MAX_ENTRIES / 0.75f),
            /*loadFactor*/      0.75f,
            /*accessOrder*/     true
    ) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, LayoutPiece> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    /**
     * Get or create the layout piece from the global cache with given requirements.
     * <p>
     * In particular, the given range cannot exceed {@link #MAX_PIECE_LENGTH} to
     * increase the reuse rate of pieced text. The given text must be in the same
     * paragraph and should not break the grapheme cluster.
     * <p>
     * Use computeFlags to compute optional info lazily.
     *
     * @param buf          text buffer, cannot be null or empty, only referred in stack
     * @param start        start char offset
     * @param limit        end char index
     * @param isRtl        whether to layout in right-to-left
     * @param paint        the font paint affecting measurement
     * @param computeFlags additional desired info to compute, or 0
     * @return the layout piece
     */
    @NonNull
    public static LayoutPiece getOrCreate(@NonNull char[] buf, int contextStart, int contextLimit,
                                          int start, int limit, boolean isRtl, @NonNull FontPaint paint,
                                          int computeFlags) {
        if (contextStart < 0 || contextStart >= contextLimit || buf.length == 0 ||
                contextLimit > buf.length || start < contextStart || limit > contextLimit) {
            throw new IndexOutOfBoundsException();
        }
        if (limit - start > MAX_PIECE_LENGTH) {
            return new LayoutPiece(buf, contextStart, contextLimit, start, limit, isRtl, paint,
                    null, computeFlags);
        }
        LookupKey key = sLookupKeys.acquire();
        if (key == null) {
            key = new LookupKey();
        }
        key.update(buf, contextStart, contextLimit, start, limit, paint, isRtl);
        LayoutPiece piece;
        synchronized (sCache) {
            piece = sCache.get(key);
        }
        if (piece == null) {
            // create new
            final Key k = key.copy();
            // recycle the lookup key earlier, since creating layout is heavy
            key.clear();
            sLookupKeys.release(key);
            piece = new LayoutPiece(buf, contextStart, contextLimit, start, limit, isRtl, paint,
                    null, computeFlags);
            // there may be a race, but we don't care
            synchronized (sCache) {
                sCache.put(k, piece);
            }
        } else {
            int currFlags = (piece.mComputeFlags & computeFlags);
            if (currFlags != computeFlags) {
                // re-compute for more info
                final Key k = key.copy();
                // recycle the lookup key earlier, since creating layout is heavy
                key.clear();
                sLookupKeys.release(key);
                piece = new LayoutPiece(buf, contextStart, contextLimit, start, limit, isRtl, paint,
                        piece, currFlags ^ computeFlags); // <- compute the difference
                // override old value
                synchronized (sCache) {
                    sCache.put(k, piece);
                }
            } else {
                // normal cache hit
                key.clear();
                sLookupKeys.release(key);
            }
        }
        return piece;
    }

    /**
     * Returns the approximate number of entries in this cache.
     */
    public static int getSize() {
        int size;
        synchronized (sCache) {
            size = sCache.size();
        }
        return size;
    }

    /**
     * This only returns measurable memory usage, in other words, at least
     *
     * @return memory usage in bytes
     */
    public static int getMemoryUsage() {
        int size = 0;
        synchronized (sCache) {
            for (var entry : sCache.entrySet()) {
                size += entry.getKey().getMemoryUsage();
                size += entry.getValue().getMemoryUsage();
                size += 56; // a node object
            }
        }
        return size;
    }

    /**
     * Clear the cache.
     */
    public static void clear() {
        synchronized (sCache) {
            sCache.clear();
        }
    }

    /**
     * The cache key.
     */
    private static class Key {

        // for Lookup case, this is only a pointer to the argument
        char[] mChars;
        int mStart;
        int mLimit;
        FontCollection mFont;
        int mFlags;
        float mSize;
        Locale mLocale;
        boolean mIsRtl;
        transient int mHash;

        private Key() {
        }

        /**
         * Copy constructor, used as a key stored in the cache
         */
        private Key(@NonNull LookupKey key) {
            // deep copy chars
            mChars = new char[key.mContextLimit - key.mContextStart];
            System.arraycopy(key.mChars, key.mContextStart,
                    mChars, 0, mChars.length);
            mStart = key.mStart;
            mLimit = key.mLimit;
            // shared pointers
            mFont = key.mFont;
            mFlags = key.mFlags;
            mSize = key.mSize;
            mLocale = key.mLocale;
            mIsRtl = key.mIsRtl;
            mHash = key.mHash;
            assert mHash == computeHash();
        }

        @Override
        public int hashCode() {
            return mHash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            // we never compare with a LookupKey
            if (o.getClass() != Key.class) {
                throw new IllegalStateException();
            }
            Key key = (Key) o;

            if (mStart != key.mStart) return false;
            if (mLimit != key.mLimit) return false;
            if (mFlags != key.mFlags) return false;
            if (mSize != key.mSize) return false;
            if (mIsRtl != key.mIsRtl) return false;
            if (!Arrays.equals(mChars, key.mChars))
                return false;
            if (!mFont.equals(key.mFont)) return false;
            return mLocale.equals(key.mLocale);
        }

        private int getMemoryUsage() {
            return 64 + 16 + MathUtil.align8(mChars.length << 1);
        }

        private int computeHash() {
            int h = 1;
            for (char c : mChars) {
                h = 31 * h + c;
            }
            h = 31 * h + mFont.hashCode();
            h = 31 * h + mFlags;
            h = 31 * h + Float.floatToIntBits(mSize);
            h = 31 * h + mStart;
            h = 31 * h + mLimit;
            h = 31 * h + mLocale.hashCode();
            h = 31 * h + (mIsRtl ? 1 : 0);
            return h;
        }
    }

    /**
     * A reusable key used for looking-up, compared against the base Key class.
     */
    private static class LookupKey extends Key {

        private int mContextStart;
        private int mContextLimit;

        public LookupKey() {
        }

        public void update(@NonNull char[] text, int contextStart, int contextLimit,
                           int start, int limit, @NonNull FontPaint paint, boolean dir) {
            mChars = text;
            mContextStart = contextStart;
            mContextLimit = contextLimit;
            // relative to contextual range
            mStart = start - contextStart;
            mLimit = limit - contextStart;
            mFont = paint.mFont;
            mFlags = paint.mFlags;
            mSize = paint.getFontSize();
            mLocale = paint.mLocale;
            mIsRtl = dir;
            {
                int h = 1;
                for (int i = mContextStart; i < mContextLimit; i++) {
                    h = 31 * h + mChars[i];
                }
                h = 31 * h + mFont.hashCode();
                h = 31 * h + mFlags;
                h = 31 * h + Float.floatToIntBits(mSize);
                h = 31 * h + mStart;
                h = 31 * h + mLimit;
                h = 31 * h + mLocale.hashCode();
                h = 31 * h + (mIsRtl ? 1 : 0);
                mHash = h;
            }
        }

        public void clear() {
            mChars = null;
            mFont = null;
            mLocale = null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            // we never compare with a LookupKey
            if (o.getClass() != Key.class) {
                throw new IllegalStateException();
            }
            Key key = (Key) o;

            if (mStart != key.mStart) return false;
            if (mLimit != key.mLimit) return false;
            if (mFlags != key.mFlags) return false;
            if (mSize != key.mSize) return false;
            if (mIsRtl != key.mIsRtl) return false;
            if (!Arrays.equals(mChars, mContextStart, mContextLimit,
                    key.mChars, 0, key.mChars.length)) {
                return false;
            }
            if (!mFont.equals(key.mFont)) return false;
            return mLocale.equals(key.mLocale);
        }

        @NonNull
        public Key copy() {
            return new Key(this);
        }
    }
}
