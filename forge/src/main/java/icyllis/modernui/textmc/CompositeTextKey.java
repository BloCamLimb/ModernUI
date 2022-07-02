/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.textmc;

import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * The key to iterate base {@link net.minecraft.network.chat.FormattedText} or
 * {@link net.minecraft.util.FormattedCharSequence}. To builder multilayer texts.
 *
 * @see VanillaTextKey
 */
public class CompositeTextKey {

    /**
     * Layers.
     */
    private Object[] mSequences;

    /**
     * Reference to vanilla's {@link Style}, we extract the value that will only affect the rendering effect
     * of the string, and store it as an integer
     */
    private int[] mStyles;

    int mHash;

    private CompositeTextKey() {
    }

    private CompositeTextKey(Object[] sequences, int[] styles, int hash) {
        // chat formatting may render same as style, but we can't separate them easily
        mSequences = sequences;
        mStyles = styles;
        mHash = hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // we never compare with a LookupKey
        if (o.getClass() != CompositeTextKey.class) return false;
        CompositeTextKey that = (CompositeTextKey) o;

        if (!Arrays.equals(mSequences, that.mSequences)) return false;
        return Arrays.equals(mStyles, that.mStyles);
    }

    @Override
    public int hashCode() {
        int h = mHash;
        if (h == 0) {
            h = 1;
            for (Object s : mSequences)
                h = 31 * h + s.hashCode();
            int r = 1;
            for (int i : mStyles)
                r = 31 * r + i;
            mHash = h = 31 * h + r;
        }
        return h;
    }

    /**
     * Designed for performance, and ensure strictly matching hashCode and equals for Key.
     */
    public static class Lookup extends CompositeTextKey {

        private final Pool<CharSequenceBuilder> mPool = Pools.simple(20);

        private final List<Object> mSequences = new ArrayList<>();
        private final IntList mStyles = new IntArrayList();

        private Style mStyle;
        private CharSequenceBuilder mBuffer;

        private boolean mFromSequence;

        // always visual order
        private final FormattedText.StyledContentConsumer<?> mContentBuilder =
                new FormattedText.StyledContentConsumer<>() {
                    @Nonnull
                    @Override
                    public Optional<Object> accept(@Nonnull Style style, @Nonnull String text) {
                        mSequences.add(text);
                        mStyles.add(CharacterStyle.getFlags(style));
                        return Optional.empty(); // continue
                    }
                };

        /**
         * Always LTR. Build multilayer text.
         *
         * @see FormattedTextWrapper#accept(FormattedCharSink)
         */
        private final FormattedCharSink mBuilderSink = new FormattedCharSink() {

            @Override
            public boolean accept(int index, @Nonnull Style style, int codePoint) {
                if (!Objects.equals(mStyle, style)) {
                    // add last
                    if (!mBuffer.isEmpty() && mStyle != null) {
                        mSequences.add(mBuffer);
                        mStyles.add(CharacterStyle.getFlags(mStyle));
                        allocateBuffer();
                    }
                    mStyle = style;
                }
                mBuffer.addCodePoint(codePoint);
                // continue
                return true;
            }
        };

        private void release() {
            for (Object s : mSequences) {
                if (s instanceof CharSequenceBuilder) {
                    mPool.release((CharSequenceBuilder) s);
                }
            }
            mSequences.clear();
            mStyles.clear();
            mStyle = null;
        }

        private void allocateBuffer() {
            mBuffer = mPool.acquire();
            if (mBuffer == null) {
                mBuffer = new CharSequenceBuilder();
            } else {
                mBuffer.clear();
            }
        }

        public CompositeTextKey update(@Nonnull FormattedText text, @Nonnull Style style) {
            // deferred release if hit last time, or use it as backing buffers
            release();
            text.visit(mContentBuilder, style);
            mHash = 0;
            mFromSequence = false;
            return this;
        }

        public CompositeTextKey update(@Nonnull FormattedCharSequence sequence) {
            // deferred release if hit last time, or use it as backing buffers
            release();
            allocateBuffer();
            sequence.accept(mBuilderSink);
            // add last
            if (!mBuffer.isEmpty() && mStyle != null) {
                mSequences.add(mBuffer);
                mStyles.add(CharacterStyle.getFlags(mStyle));
            }
            mHash = 0;
            mFromSequence = true;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            // we never compare with a LookupKey
            if (o.getClass() != CompositeTextKey.class) return false;

            CompositeTextKey key = (CompositeTextKey) o;

            int length = key.mStyles.length;
            if (mStyles.size() != length) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (mStyles.getInt(i) != key.mStyles[i]) {
                    return false;
                }
            }

            length = key.mSequences.length;
            if (mSequences.size() != length) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (!mSequences.get(i).equals(key.mSequences[i])) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode() {
            int h = mHash;
            if (h == 0) {
                h = 1;
                for (Object s : mSequences)
                    h = 31 * h + s.hashCode();
                int r = 1;
                for (int i : mStyles)
                    r = 31 * r + i;
                mHash = h = 31 * h + r;
            }
            return h;
        }

        /**
         * Make a cache key. For different hashCode implementation, we keep FormattedText
         * and FormattedCharSequence separated even they are equal.
         *
         * @return base key
         */
        public CompositeTextKey copy() {
            Object[] sequences = new Object[mSequences.size()];
            if (mFromSequence) {
                for (int i = 0; i < sequences.length; i++) {
                    sequences[i] = ((CharSequenceBuilder) mSequences.get(i)).trimChars();
                }
            } else {
                // Strings
                for (int i = 0; i < sequences.length; i++) {
                    sequences[i] = mSequences.get(i);
                }
            }
            // do not recycle
            mSequences.clear();
            return new CompositeTextKey(sequences, mStyles.toIntArray(), mHash);
        }
    }
}
