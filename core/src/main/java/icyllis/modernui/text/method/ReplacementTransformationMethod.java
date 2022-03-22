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

package icyllis.modernui.text.method;

import icyllis.modernui.math.Rect;
import icyllis.modernui.text.*;
import icyllis.modernui.view.View;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * This transformation method causes the characters in the {@link #getOriginal}
 * array to be replaced by the corresponding characters in the
 * {@link #getReplacement} array.
 */
public abstract class ReplacementTransformationMethod implements TransformationMethod {

    protected ReplacementTransformationMethod() {
    }

    /**
     * Returns the list of characters that are to be replaced by other
     * characters when displayed.
     */
    protected abstract char[] getOriginal();

    /**
     * Returns a parallel array of replacement characters for the ones
     * that are to be replaced.
     */
    protected abstract char[] getReplacement();

    /**
     * Returns a CharSequence that will mirror the contents of the
     * source CharSequence but with the characters in {@link #getOriginal}
     * replaced by ones from {@link #getReplacement}.
     */
    @Nonnull
    public CharSequence getTransformation(@Nonnull CharSequence source, @Nonnull View v) {
        char[] original = getOriginal();
        char[] replacement = getReplacement();

        /*
         * Short circuit for faster display if the text will never change.
         */
        if (!(source instanceof Editable)) {
            /*
             * Check whether the text does not contain any of the
             * source characters so can be used unchanged.
             */
            boolean doNothing = true;
            for (char c : original) {
                if (TextUtils.indexOf(source, c) >= 0) {
                    doNothing = false;
                    break;
                }
            }
            if (doNothing) {
                return source;
            }

            if (!(source instanceof Spannable)) {
                /*
                 * The text contains some source characters,
                 * but they can be flattened out now instead of
                 * at display time.
                 */
                if (source instanceof Spanned) {
                    return new SpannedString(new SpannedReplacementCharSequence(
                            (Spanned) source,
                            original, replacement));
                } else {
                    return new ReplacementCharSequence(source,
                            original,
                            replacement).toString();
                }
            }
        }

        return new SpannedReplacementCharSequence((Spanned) source,
                original, replacement);
    }

    @Override
    public void onFocusChanged(@Nonnull View view, @Nonnull CharSequence sourceText,
                               boolean focused, int direction, @Nullable Rect previouslyFocusedRect) {
    }

    private static class ReplacementCharSequence implements CharSequence, GetChars {

        private final CharSequence mSource;
        private final char[] mOriginal;
        private final char[] mReplacement;

        public ReplacementCharSequence(CharSequence source, char[] original,
                                       char[] replacement) {
            mSource = source;
            mOriginal = original;
            mReplacement = replacement;
        }

        @Override
        public int length() {
            return mSource.length();
        }

        @Override
        public char charAt(int i) {
            char c = mSource.charAt(i);

            int n = mOriginal.length;
            for (int j = 0; j < n; j++) {
                if (c == mOriginal[j]) {
                    c = mReplacement[j];
                }
            }

            return c;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            char[] c = new char[end - start];

            getChars(start, end, c, 0);
            return new String(c);
        }

        @Override
        public String toString() {
            char[] c = new char[length()];

            getChars(0, length(), c, 0);
            return new String(c);
        }

        @Override
        public void getChars(int start, int end, char[] dest, int off) {
            TextUtils.getChars(mSource, start, end, dest, off);
            int offend = end - start + off;
            int n = mOriginal.length;

            for (int i = off; i < offend; i++) {
                char c = dest[i];

                for (int j = 0; j < n; j++) {
                    if (c == mOriginal[j]) {
                        dest[i] = mReplacement[j];
                    }
                }
            }
        }
    }

    private static class SpannedReplacementCharSequence extends ReplacementCharSequence implements Spanned {

        private final Spanned mSpanned;

        public SpannedReplacementCharSequence(Spanned source, char[] original,
                                              char[] replacement) {
            super(source, original, replacement);
            mSpanned = source;
        }

        @Nonnull
        @Override
        public CharSequence subSequence(int start, int end) {
            return new SpannedString(this).subSequence(start, end);
        }

        @Nullable
        @Override
        public <T> T[] getSpans(int start, int end, Class<? extends T> type, @Nullable List<T> out) {
            return mSpanned.getSpans(start, end, type, out);
        }

        @Override
        public int getSpanStart(@Nonnull Object tag) {
            return mSpanned.getSpanStart(tag);
        }

        @Override
        public int getSpanEnd(@Nonnull Object tag) {
            return mSpanned.getSpanEnd(tag);
        }

        @Override
        public int getSpanFlags(@Nonnull Object tag) {
            return mSpanned.getSpanFlags(tag);
        }

        @Override
        public int nextSpanTransition(int start, int end, Class<?> type) {
            return mSpanned.nextSpanTransition(start, end, type);
        }
    }
}
