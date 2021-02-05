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

/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package icyllis.modernui.text;

import com.ibm.icu.util.ULocale;
import icyllis.modernui.ModernUI;

import java.nio.CharBuffer;
import java.util.Locale;

public final class TextDirectionHeuristics {

    /**
     * Always decides that the direction is left to right.
     */
    public static final TextDirectionHeuristic LTR =
            new TextDirectionHeuristicInternal(null, false);

    /**
     * Always decides that the direction is right to left.
     */
    public static final TextDirectionHeuristic RTL =
            new TextDirectionHeuristicInternal(null, true);

    /**
     * Determines the direction based on the first strong directional character, including bidi
     * format chars, falling back to left to right if it finds none. This is the default behavior
     * of the Unicode Bidirectional Algorithm.
     */
    public static final TextDirectionHeuristic FIRSTSTRONG_LTR =
            new TextDirectionHeuristicInternal(FirstStrong.INSTANCE, false);

    /**
     * Determines the direction based on the first strong directional character, including bidi
     * format chars, falling back to right to left if it finds none. This is similar to the default
     * behavior of the Unicode Bidirectional Algorithm, just with different fallback behavior.
     */
    public static final TextDirectionHeuristic FIRSTSTRONG_RTL =
            new TextDirectionHeuristicInternal(FirstStrong.INSTANCE, true);

    /**
     * If the text contains any strong right to left non-format character, determines that the
     * direction is right to left, falling back to left to right if it finds none.
     */
    public static final TextDirectionHeuristic ANYRTL_LTR =
            new TextDirectionHeuristicInternal(AnyStrong.INSTANCE_RTL, false);

    /**
     * Force the paragraph direction to the Locale direction. Falls back to left to right.
     */
    public static final TextDirectionHeuristic LOCALE = new TextDirectionHeuristicLocale();

    /**
     * State constants for taking care about true / false / unknown
     */
    private static final int STATE_TRUE = 0;
    private static final int STATE_FALSE = 1;
    private static final int STATE_UNKNOWN = 2;

    private TextDirectionHeuristics() {
    }

    /* Returns STATE_TRUE for strong RTL characters, STATE_FALSE for strong LTR characters, and
     * STATE_UNKNOWN for everything else.
     */
    private static int isRtlCodePoint(int codePoint) {
        switch (Character.getDirectionality(codePoint)) {
            case Character.DIRECTIONALITY_LEFT_TO_RIGHT:
                return STATE_FALSE;
            case Character.DIRECTIONALITY_RIGHT_TO_LEFT:
            case Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC:
                return STATE_TRUE;
            case Character.DIRECTIONALITY_UNDEFINED:
                // Unassigned characters still have bidi direction, defined at:
                // http://www.unicode.org/Public/UCD/latest/ucd/extracted/DerivedBidiClass.txt

                if ((0x0590 <= codePoint && codePoint <= 0x08FF) ||
                        (0xFB1D <= codePoint && codePoint <= 0xFDCF) ||
                        (0xFDF0 <= codePoint && codePoint <= 0xFDFF) ||
                        (0xFE70 <= codePoint && codePoint <= 0xFEFF) ||
                        (0x10800 <= codePoint && codePoint <= 0x10FFF) ||
                        (0x1E800 <= codePoint && codePoint <= 0x1EFFF)) {
                    // Unassigned RTL character
                    return STATE_TRUE;
                } else if (
                    // Potentially-unassigned Default_Ignorable. Ranges are from unassigned
                    // characters that have Unicode property Other_Default_Ignorable_Code_Point
                    // plus some enlargening to cover bidi isolates and simplify checks.
                        (0x2065 <= codePoint && codePoint <= 0x2069) ||
                                (0xFFF0 <= codePoint && codePoint <= 0xFFF8) ||
                                (0xE0000 <= codePoint && codePoint <= 0xE0FFF) ||
                                // Non-character
                                (0xFDD0 <= codePoint && codePoint <= 0xFDEF) ||
                                ((codePoint & 0xFFFE) == 0xFFFE) ||
                                // Currency symbol
                                (0x20A0 <= codePoint && codePoint <= 0x20CF) ||
                                // Unpaired surrogate
                                (0xD800 <= codePoint && codePoint <= 0xDFFF)) {
                    return STATE_UNKNOWN;
                } else {
                    // Unassigned LTR character
                    return STATE_FALSE;
                }
            default:
                return STATE_UNKNOWN;
        }
    }

    /**
     * Computes the text direction based on an algorithm.  Subclasses implement
     * {@link #isDefaultRtl} to handle cases where the algorithm cannot determine the
     * direction from the text alone.
     */
    private static abstract class TextDirectionHeuristicImpl implements TextDirectionHeuristic {

        private final TextDirectionAlgorithm mAlgorithm;

        public TextDirectionHeuristicImpl(TextDirectionAlgorithm algorithm) {
            mAlgorithm = algorithm;
        }

        /**
         * Return true if the default text direction is rtl.
         */
        protected abstract boolean isDefaultRtl();

        @Override
        public boolean isRtl(char[] array, int start, int count) {
            return isRtl(CharBuffer.wrap(array), start, count);
        }

        @Override
        public boolean isRtl(CharSequence cs, int start, int count) {
            if (cs == null || start < 0 || count < 0 || cs.length() - count < start) {
                throw new IllegalArgumentException();
            }
            if (mAlgorithm == null) {
                return isDefaultRtl();
            }
            return doCheck(cs, start, count);
        }

        private boolean doCheck(CharSequence cs, int start, int count) {
            switch (mAlgorithm.checkRtl(cs, start, count)) {
                case STATE_TRUE:
                    return true;
                case STATE_FALSE:
                    return false;
                default:
                    return isDefaultRtl();
            }
        }
    }

    private static class TextDirectionHeuristicInternal extends TextDirectionHeuristicImpl {

        private final boolean mDefaultRtl;

        private TextDirectionHeuristicInternal(TextDirectionAlgorithm algorithm,
                                               boolean defaultRtl) {
            super(algorithm);
            mDefaultRtl = defaultRtl;
        }

        @Override
        protected boolean isDefaultRtl() {
            return mDefaultRtl;
        }
    }

    /**
     * Interface for an algorithm to guess the direction of a paragraph of text.
     */
    private interface TextDirectionAlgorithm {

        /**
         * Returns whether the range of text is RTL according to the algorithm.
         */
        int checkRtl(CharSequence cs, int start, int count);
    }

    /**
     * Algorithm that uses the first strong directional character to determine the paragraph
     * direction. This is the standard Unicode Bidirectional Algorithm (steps P2 and P3), with the
     * exception that if no strong character is found, UNKNOWN is returned.
     */
    private static class FirstStrong implements TextDirectionAlgorithm {

        public static final FirstStrong INSTANCE = new FirstStrong();

        private FirstStrong() {
        }

        @Override
        public int checkRtl(CharSequence cs, int start, int count) {
            int result = STATE_UNKNOWN;
            int openIsolateCount = 0;
            for (int cp, i = start, end = start + count;
                 i < end && result == STATE_UNKNOWN;
                 i += Character.charCount(cp)) {
                cp = Character.codePointAt(cs, i);
                if (0x2066 <= cp && cp <= 0x2068) { // Opening isolates
                    openIsolateCount += 1;
                } else if (cp == 0x2069) { // POP DIRECTIONAL ISOLATE (PDI)
                    if (openIsolateCount > 0) openIsolateCount -= 1;
                } else if (openIsolateCount == 0) {
                    // Only consider the characters outside isolate pairs
                    result = isRtlCodePoint(cp);
                }
            }
            return result;
        }
    }

    /**
     * Algorithm that uses the presence of any strong directional character of the type indicated
     * in the constructor parameter to determine the direction of text.
     * <p>
     * Characters inside isolate pairs are skipped.
     */
    private static class AnyStrong implements TextDirectionAlgorithm {

        public static final AnyStrong INSTANCE_RTL = new AnyStrong(true);
        public static final AnyStrong INSTANCE_LTR = new AnyStrong(false);

        private final boolean mLookForRtl;

        private AnyStrong(boolean lookForRtl) {
            this.mLookForRtl = lookForRtl;
        }

        @Override
        public int checkRtl(CharSequence cs, int start, int count) {
            boolean haveUnlookedFor = false;
            int openIsolateCount = 0;
            for (int cp, i = start, end = start + count; i < end; i += Character.charCount(cp)) {
                cp = Character.codePointAt(cs, i);
                if (0x2066 <= cp && cp <= 0x2068) { // Opening isolates
                    openIsolateCount += 1;
                } else if (cp == 0x2069) { // POP DIRECTIONAL ISOLATE (PDI)
                    if (openIsolateCount > 0) openIsolateCount -= 1;
                } else if (openIsolateCount == 0) {
                    // Only consider the characters outside isolate pairs
                    switch (isRtlCodePoint(cp)) {
                        case STATE_TRUE:
                            if (mLookForRtl) {
                                return STATE_TRUE;
                            }
                            haveUnlookedFor = true;
                            break;
                        case STATE_FALSE:
                            if (!mLookForRtl) {
                                return STATE_FALSE;
                            }
                            haveUnlookedFor = true;
                            break;
                        default:
                            break;
                    }
                }
            }
            if (haveUnlookedFor) {
                return mLookForRtl ? STATE_FALSE : STATE_TRUE;
            }
            return STATE_UNKNOWN;
        }
    }

    /**
     * Algorithm that uses the Locale direction to force the direction of a paragraph.
     */
    private static class TextDirectionHeuristicLocale extends TextDirectionHeuristicImpl {

        public TextDirectionHeuristicLocale() {
            super(null);
        }

        @Override
        protected boolean isDefaultRtl() {
            Locale locale = ModernUI.get().getSelectedLocale();
            return !locale.equals(Locale.ROOT) && ULocale.forLocale(locale).isRightToLeft();
        }
    }
}
