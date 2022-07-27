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
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * The cache key to iterate base {@link net.minecraft.network.chat.FormattedText} or
 * {@link net.minecraft.util.FormattedCharSequence}. To builder multilayer texts.
 *
 * @author BloCamLimb
 * @see CharacterStyle
 * @see VanillaTextKey
 */
public class MultilayerTextKey {

    /**
     * Layers.
     */
    private CharSequence[] mSequences;

    /**
     * Reference to vanilla's {@link Style}, we extract the value that will only affect the rendering effect
     * of the string, and store it as an integer
     */
    private int[] mStyles;

    int mHash;

    private MultilayerTextKey() {
    }

    private MultilayerTextKey(CharSequence[] sequences, int[] styles, int hash) {
        // chat formatting may render same as style, but we can't separate them easily
        mSequences = sequences;
        mStyles = styles;
        mHash = hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // we never compare with a LookupKey
        if (o.getClass() != MultilayerTextKey.class) return false;

        MultilayerTextKey key = (MultilayerTextKey) o;
        return Arrays.equals(mStyles, key.mStyles) &&
                Arrays.equals(mSequences, key.mSequences);
    }

    @Override
    public int hashCode() {
        int h = mHash;
        if (h == 0) {
            h = 1;
            for (CharSequence s : mSequences)
                h = 31 * h + s.hashCode();
            int r = 1;
            for (int i : mStyles)
                r = 31 * r + i;
            mHash = h = 31 * h + r;
        }
        return h;
    }

    @Override
    public String toString() {
        return "MultilayerTextKey{" +
                "mSequences=" + Arrays.toString(mSequences) +
                ", mStyles=" + Arrays.toString(mStyles) +
                ", mHash=" + mHash +
                '}';
    }

    /**
     * @return approximate memory usage
     */
    public int getMemorySize() {
        int size = 0;
        for (CharSequence s : mSequences) {
            size += (((s.length() + 3) >> 1) << 2);
        }
        // same length though
        size += 16 + (((mSequences.length + 1) >> 1) << 3);
        size += 16 + (((mStyles.length + 1) >> 1) << 3);
        return size + 24;
    }

    /**
     * Designed for performance, and ensure strictly matching hashCode and equals for Key.
     */
    public static class Lookup extends MultilayerTextKey {

        private final ArrayList<CharSequence> mSequences = new ArrayList<>();
        private final IntArrayList mStyles = new IntArrayList();

        // always logical order
        private final FormattedText.StyledContentConsumer<Object> mContentBuilder =
                new FormattedText.StyledContentConsumer<>() {
                    @Nonnull
                    @Override
                    public Optional<Object> accept(@Nonnull Style style, @Nonnull String text) {
                        mSequences.add(text);
                        mStyles.add(CharacterStyle.getAppearanceFlags(style));
                        return Optional.empty(); // continue
                    }
                };

        private final SequenceBuilder mSequenceBuilder = new SequenceBuilder();

        /**
         * Always LTR. Build multilayer text.
         *
         * @see FormattedTextWrapper#accept(FormattedCharSink)
         */
        private class SequenceBuilder implements FormattedCharSink {

            private final Pool<CharSequenceBuilder> mPool = Pools.simple(20);

            private CharSequenceBuilder mBuilder = null;
            private Style mStyle = null;

            private void allocate() {
                mBuilder = mPool.acquire();
                if (mBuilder == null) {
                    mBuilder = new CharSequenceBuilder();
                } else {
                    mBuilder.clear();
                }
            }

            @Override
            public boolean accept(int index, @Nonnull Style style, int codePoint) {
                if (mStyle == null) {
                    allocate();
                    mStyle = style;
                } else if (CharacterStyle.affectsAppearance(mStyle, style)) {
                    // add last
                    if (!mBuilder.isEmpty()) {
                        mSequences.add(mBuilder);
                        mStyles.add(CharacterStyle.getAppearanceFlags(mStyle));
                        allocate();
                    }
                    mStyle = style;
                }
                mBuilder.addCodePoint(codePoint);
                // continue
                return true;
            }

            private void end() {
                // add last
                if (mStyle != null && mBuilder != null && !mBuilder.isEmpty()) {
                    mSequences.add(mBuilder);
                    mStyles.add(CharacterStyle.getAppearanceFlags(mStyle));
                }
                for (CharSequence s : mSequences) {
                    mPool.release((CharSequenceBuilder) s);
                }
                mBuilder = null;
                mStyle = null;
            }
        }

        private void reset() {
            mSequences.clear();
            mStyles.clear();
            mHash = 0;
        }

        @Nonnull
        public MultilayerTextKey update(@Nonnull FormattedText text, @Nonnull Style style) {
            reset();
            text.visit(mContentBuilder, style);
            return this;
        }

        @Nonnull
        public MultilayerTextKey update(@Nonnull FormattedCharSequence sequence) {
            reset();
            sequence.accept(mSequenceBuilder);
            mSequenceBuilder.end();
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            // we never compare with a LookupKey
            if (o.getClass() != MultilayerTextKey.class) return false;

            MultilayerTextKey key = (MultilayerTextKey) o;

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
                for (CharSequence s : mSequences)
                    h = 31 * h + s.hashCode();
                int r = 1;
                for (int i : mStyles)
                    r = 31 * r + i;
                mHash = h = 31 * h + r;
            }
            return h;
        }

        @Override
        public String toString() {
            return "Lookup{" +
                    "mSequences=" + mSequences +
                    ", mStyles=" + mStyles +
                    ", mHash=" + mHash +
                    '}';
        }

        /**
         * Make a cache key. We always use String.hashCode() implementation.
         *
         * @return a storage key
         */
        @Nonnull
        public MultilayerTextKey copy() {
            CharSequence[] sequences = new CharSequence[mSequences.size()];
            for (int i = 0; i < sequences.length; i++) {
                sequences[i] = mSequences.get(i).toString();
            }
            return new MultilayerTextKey(sequences, mStyles.toIntArray(), mHash);
        }
    }
}
