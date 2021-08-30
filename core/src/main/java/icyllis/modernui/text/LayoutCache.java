/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.text;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import icyllis.modernui.math.MathUtil;
import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;

import javax.annotation.Nonnull;
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
public class LayoutCache {

    public static final int MAX_PIECE_LENGTH = 128;

    private static final Pool<LookupKey> sLookupKeys = Pools.concurrent(2);
    private static volatile Cache<Key, LayoutPiece> sCache;

    /**
     * Get or create the layout piece from the global cache with given requirements.
     * <p>
     * In particular, the given range cannot exceed {@link #MAX_PIECE_LENGTH} to
     * increase the reuse rate of pieced text. The given text must be in the same
     * paragraph and should not break the grapheme cluster.
     *
     * @param buf   text buffer, cannot be null or empty
     * @param start start char offset
     * @param end   end char index
     * @param dir   whether to layout in right-to-left
     * @param paint the font paint affecting measurement
     * @return the layout piece
     */
    @Nonnull
    public static LayoutPiece getOrCreate(@Nonnull char[] buf, int start, int end,
                                          boolean dir, @Nonnull FontPaint paint) {
        if (start < 0 || start > end || end - start > MAX_PIECE_LENGTH
                || buf.length == 0 || end > buf.length) {
            throw new IllegalArgumentException();
        }
        if (sCache == null) {
            synchronized (LayoutCache.class) {
                if (sCache == null) {
                    sCache = Caffeine.newBuilder()
                            .maximumSize(1000)
                            .build();
                }
            }
        }
        LookupKey key = sLookupKeys.acquire();
        if (key == null) {
            key = new LookupKey();
        }
        LayoutPiece piece = sCache.getIfPresent(
                key.update(buf, start, end, paint, dir));
        if (piece == null) {
            final Key k = key.copy();
            // recycle the lookup key earlier, since creating layout is heavy
            sLookupKeys.release(key);
            piece = new LayoutPiece(buf, start, end, dir, paint);
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

    private static class Key {

        // for Lookup case, this is only a pointer to the requester
        char[] mChars;
        Typeface mTypeface;
        int mFontStyle;
        int mFontSize;
        Locale mLocale;
        boolean mIsRtl;

        private Key() {
        }

        /**
         * Copy constructor, used as a key stored in the cache
         */
        private Key(@Nonnull LookupKey key) {
            // deep copy chars
            mChars = new char[key.mEnd - key.mStart];
            System.arraycopy(key.mChars, key.mStart, mChars, 0, mChars.length);
            // shared pointers
            mTypeface = key.mTypeface;
            mFontStyle = key.mFontStyle;
            mFontSize = key.mFontSize;
            mLocale = key.mLocale;
            mIsRtl = key.mIsRtl;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            // we never compare with a LookupKey
            if (o.getClass() != Key.class) return false;
            Key key = (Key) o;

            if (mFontStyle != key.mFontStyle) return false;
            if (mFontSize != key.mFontSize) return false;
            if (mIsRtl != key.mIsRtl) return false;
            if (!Arrays.equals(mChars, key.mChars))
                return false;
            if (!mTypeface.equals(key.mTypeface)) return false;
            return mLocale.equals(key.mLocale);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(mChars);
            result = 31 * result + mTypeface.hashCode();
            result = 31 * result + mFontStyle;
            result = 31 * result + mFontSize;
            result = 31 * result + mLocale.hashCode();
            result = 31 * result + (mIsRtl ? 1 : 0);
            return result;
        }

        private int getMemoryUsage() {
            return MathUtil.roundUp(12 + 16 + 8 + 8 + 4 + 4 + 8 + 1 + (mChars.length << 1), 8);
        }
    }

    private static class LookupKey extends Key {

        private int mStart;
        private int mEnd;

        public LookupKey() {
        }

        @Nonnull
        public Key update(@Nonnull char[] text, int start, int end, @Nonnull FontPaint paint, boolean dir) {
            mChars = text;
            mStart = start;
            mEnd = end;
            mTypeface = paint.mTypeface;
            mFontStyle = paint.mFlags;
            mFontSize = paint.mFontSize;
            mLocale = paint.mLocale;
            mIsRtl = dir;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            // we never compare with a LookupKey
            if (o.getClass() != Key.class) return false;

            Key key = (Key) o;

            if (mFontStyle != key.mFontStyle) return false;
            if (mFontSize != key.mFontSize) return false;
            if (mIsRtl != key.mIsRtl) return false;
            if (!Arrays.equals(mChars, mStart, mEnd,
                    key.mChars, 0, key.mChars.length))
                return false;
            if (!mTypeface.equals(key.mTypeface)) return false;
            return mLocale.equals(key.mLocale);
        }

        @Override
        public int hashCode() {
            int result = 1;
            for (int i = mStart; i < mEnd; i++)
                result = 31 * result + mChars[i];
            result = 31 * result + mTypeface.hashCode();
            result = 31 * result + mFontStyle;
            result = 31 * result + mFontSize;
            result = 31 * result + mLocale.hashCode();
            result = 31 * result + (mIsRtl ? 1 : 0);
            return result;
        }

        @Nonnull
        public Key copy() {
            return new Key(this);
        }
    }
}
