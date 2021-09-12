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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The key to iterate base {@link net.minecraft.network.chat.FormattedText} or
 * {@link net.minecraft.util.FormattedCharSequence}. To builder multilayer texts.
 *
 * @see VanillaTextKey
 */
public class MultilayerTextKey {

    private static final Pool<CharSequenceBuilder> sPool = Pools.simple(20);

    /**
     * Layers.
     */
    private CharSequence[] mSequences;

    /**
     * Reference to vanilla's {@link Style}, we extract the value that will only affect the rendering effect
     * of the string, and store it as an integer
     */
    private int[] mStyles;

    protected int mHash;

    private MultilayerTextKey() {
    }

    private MultilayerTextKey(CharSequence[] sequences, int[] styles, int hash) {
        // text formatting may render same as style, but we can't separate them easily
        mSequences = sequences;
        mStyles = styles;
        mHash = hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // we never compare with a LookupKey
        if (o.getClass() != MultilayerTextKey.class) return false;
        MultilayerTextKey that = (MultilayerTextKey) o;

        if (!Arrays.equals(mSequences, that.mSequences)) return false;
        return Arrays.equals(mStyles, that.mStyles);
    }

    @Override
    public int hashCode() {
        int h = mHash;
        if (h == 0) {
            h = Arrays.hashCode(mSequences);
            mHash = h = 31 * h + Arrays.hashCode(mStyles);
        }
        return h;
    }

    /**
     * For performance, and ensure strictly matching hashCode and equals for Key.
     */
    public static class Lookup extends MultilayerTextKey {

        private final List<CharSequence> mSequences = new ArrayList<>();
        private final IntList mStyles = new IntArrayList();

        private Style mStyle;
        private CharSequenceBuilder mBuffer;

        // always visual order
        private final FormattedText.StyledContentConsumer<?> mContentBuilder = (style, text) -> {
            mSequences.add(text);
            mStyles.add(CharacterStyleCarrier.getFlags(style));
            return Optional.empty();
        };

        /**
         * Always LTR. Build multilayer text.
         *
         * @see FormattedTextWrapper#accept(FormattedCharSink)
         */
        private final FormattedCharSink mBuilderSink = new FormattedCharSink() {

            @Override
            public boolean accept(int index, @Nonnull Style style, int codePoint) {
                if (style != mStyle) {
                    if (mStyle != null && !mBuffer.isEmpty()) {
                        mSequences.add(mBuffer);
                        mStyles.add(CharacterStyleCarrier.getFlags(mStyle));
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
            for (CharSequence s : mSequences) {
                if (s instanceof CharSequenceBuilder) {
                    sPool.release((CharSequenceBuilder) s);
                }
            }
            mSequences.clear();
            mStyles.clear();
            mStyle = null;
        }

        private void allocateBuffer() {
            mBuffer = sPool.acquire();
            if (mBuffer == null) {
                mBuffer = new CharSequenceBuilder();
            }
            mBuffer.clear();
        }

        public MultilayerTextKey update(@Nonnull FormattedText text, @Nonnull Style style) {
            release();
            text.visit(mContentBuilder, style);
            mHash = 0;
            return this;
        }

        public MultilayerTextKey update(@Nonnull FormattedCharSequence sequence) {
            release();
            allocateBuffer();
            sequence.accept(mBuilderSink);
            if (!mBuffer.isEmpty() && mStyle != null) {
                mSequences.add(mBuffer);
                mStyles.add(CharacterStyleCarrier.getFlags(mStyle));
            }
            mHash = 0;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            // we never compare with a LookupKey
            if (o.getClass() != MultilayerTextKey.class) return false;

            MultilayerTextKey key = (MultilayerTextKey) o;

            int length = key.mSequences.length;
            if (mSequences.size() != length) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (!mSequences.get(i).equals(key.mSequences[i])) {
                    return false;
                }
            }

            length = key.mStyles.length;
            if (mStyles.size() != length) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (mStyles.getInt(i) != key.mStyles[i]) {
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

        public MultilayerTextKey copy() {
            return new MultilayerTextKey(mSequences.stream().map(CharSequence::toString).toArray(CharSequence[]::new),
                    mStyles.toIntArray(), mHash);
        }
    }
}
