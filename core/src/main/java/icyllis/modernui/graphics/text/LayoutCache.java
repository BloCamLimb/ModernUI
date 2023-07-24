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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import icyllis.arc3d.core.MathUtil;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.util.Pools;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Arrays;
import java.util.Locale;

/**
 * Globally shared layout cache. Useful when recycling layouts, or raw data source and
 * layout information are separated. Max memory usage: 3~4 MB.
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

    private static final Pools.Pool<LookupKey> sLookupKeys = Pools.newSynchronizedPool(3);
    private static volatile Cache<Key, LayoutPiece> sCache;

    /**
     * Get or create the layout piece from the global cache with given requirements.
     * <p>
     * In particular, the given range cannot exceed {@link #MAX_PIECE_LENGTH} to
     * increase the reuse rate of pieced text. The given text must be in the same
     * paragraph and should not break the grapheme cluster.
     *
     * @param buf   text buffer, cannot be null or empty, only referred in stack
     * @param start start char offset
     * @param end   end char index
     * @param isRtl whether to layout in right-to-left
     * @param paint the font paint affecting measurement
     * @return the layout piece
     */
    @NonNull
    public static LayoutPiece getOrCreate(@NonNull char[] buf, int contextStart, int contextEnd,
                                          int start, int end, boolean isRtl, FontPaint paint) {
        if (contextStart < 0 || contextStart >= contextEnd || buf.length == 0 ||
                contextEnd > buf.length || start < contextStart || end > contextEnd) {
            throw new IndexOutOfBoundsException();
        }
        if (end - start > MAX_PIECE_LENGTH) {
            return new LayoutPiece(buf, contextStart, contextEnd, start, end, isRtl, paint);
        }
        if (sCache == null) {
            synchronized (LayoutCache.class) {
                if (sCache == null) {
                    sCache = Caffeine.newBuilder()
                            .maximumSize(2000)
                            //.expireAfterAccess(5, TimeUnit.MINUTES)
                            .build();
                }
            }
        }
        LookupKey key = sLookupKeys.acquire();
        if (key == null) {
            key = new LookupKey();
        }
        LayoutPiece piece = sCache.getIfPresent(
                key.update(buf, contextStart, contextEnd, start, end, paint, isRtl));
        // create new or re-compute for more params
        if (piece == null) {
            final Key k = key.copy();
            // recycle the lookup key earlier, since creating layout is heavy
            sLookupKeys.release(key);
            piece = new LayoutPiece(buf, contextStart, contextEnd, start, end, isRtl, paint);
            // there may be a race, but we don't care
            sCache.put(k, piece);
        } else {
            sLookupKeys.release(key);
        }
        return piece;
    }

    /**
     * This only returns measurable memory usage, in other words, at least
     *
     * @return memory usage in bytes
     */
    public static int getMemoryUsage() {
        if (sCache == null) {
            return 0;
        }
        int size = 0;
        for (var entry : sCache.asMap().entrySet()) {
            size += entry.getKey().getMemoryUsage();
            size += entry.getValue().getMemoryUsage();
            size += 40; // a node object
        }
        return size;
    }

    /**
     * Clear the cache.
     */
    public static void clear() {
        if (sCache != null) {
            sCache.invalidateAll();
        }
    }

    /**
     * The cache key.
     */
    private static class Key {

        // for Lookup case, this is only a pointer to the argument
        char[] mChars;
        int mStart;
        int mEnd;
        FontCollection mFont;
        int mFlags;
        int mSize;
        Locale mLocale;
        boolean mIsRtl;

        private Key() {
        }

        /**
         * Copy constructor, used as a key stored in the cache
         */
        private Key(@NonNull LookupKey key) {
            // deep copy chars
            mChars = new char[key.mContextEnd - key.mContextStart];
            System.arraycopy(key.mChars, key.mContextStart,
                    mChars, 0, mChars.length);
            mStart = key.mStart;
            mEnd = key.mEnd;
            // shared pointers
            mFont = key.mFont;
            mFlags = key.mFlags;
            mSize = key.mSize;
            mLocale = key.mLocale;
            mIsRtl = key.mIsRtl;
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
            if (mEnd != key.mEnd) return false;
            if (mFlags != key.mFlags) return false;
            if (mSize != key.mSize) return false;
            if (mIsRtl != key.mIsRtl) return false;
            if (!Arrays.equals(mChars, key.mChars))
                return false;
            if (!mFont.equals(key.mFont)) return false;
            return mLocale.equals(key.mLocale);
        }

        @Override
        public int hashCode() {
            int result = 1;
            for (char c : mChars) {
                result = 31 * result + c;
            }
            result = 31 * result + mFont.hashCode();
            result = 31 * result + mFlags;
            result = 31 * result + mSize;
            result = 31 * result + mStart;
            result = 31 * result + mEnd;
            result = 31 * result + mLocale.hashCode();
            result = 31 * result + (mIsRtl ? 1 : 0);
            return result;
        }

        private int getMemoryUsage() {
            return MathUtil.align8(12 + 16 + 8 + 8 + 4 + 4 + 8 + 1 + (mChars.length << 1));
        }
    }

    /**
     * A reusable key used for looking-up, compared against the base Key class.
     */
    private static class LookupKey extends Key {

        private int mContextStart;
        private int mContextEnd;

        public LookupKey() {
        }

        @NonNull
        public Key update(@NonNull char[] text, int contextStart, int contextEnd,
                          int start, int end, @NonNull FontPaint paint, boolean dir) {
            mChars = text;
            mContextStart = contextStart;
            mContextEnd = contextEnd;
            // relative to contextual range
            mStart = start - contextStart;
            mEnd = end - contextStart;
            mFont = paint.mFont;
            mFlags = paint.mFlags;
            mSize = paint.mSize;
            mLocale = paint.mLocale;
            mIsRtl = dir;
            return this;
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
            if (mEnd != key.mEnd) return false;
            if (mFlags != key.mFlags) return false;
            if (mSize != key.mSize) return false;
            if (mIsRtl != key.mIsRtl) return false;
            if (!Arrays.equals(mChars, mContextStart, mContextEnd,
                    key.mChars, 0, key.mChars.length)) {
                return false;
            }
            if (!mFont.equals(key.mFont)) return false;
            return mLocale.equals(key.mLocale);
        }

        @Override
        public int hashCode() {
            int result = 1;
            for (int i = mContextStart; i < mContextEnd; i++) {
                result = 31 * result + mChars[i];
            }
            result = 31 * result + mFont.hashCode();
            result = 31 * result + mFlags;
            result = 31 * result + mSize;
            result = 31 * result + mStart;
            result = 31 * result + mEnd;
            result = 31 * result + mLocale.hashCode();
            result = 31 * result + (mIsRtl ? 1 : 0);
            return result;
        }

        @NonNull
        public Key copy() {
            return new Key(this);
        }
    }
}
