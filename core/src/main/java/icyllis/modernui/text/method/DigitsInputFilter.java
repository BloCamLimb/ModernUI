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

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.DecimalFormatSymbols;
import icyllis.modernui.text.SpannableStringBuilder;
import icyllis.modernui.text.Spanned;
import it.unimi.dsi.fastutil.chars.CharLinkedOpenHashSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.HashMap;
import java.util.Locale;

/**
 * Input filter for digits-only text.
 */
public class DigitsInputFilter extends NumberInputFilter {

    private static final String DEFAULT_DECIMAL_POINT_CHARS = ".";
    private static final String DEFAULT_SIGN_CHARS = "-+";

    private static final char HYPHEN_MINUS = '-';
    // Various locales use this as minus sign
    private static final char MINUS_SIGN = '\u2212';
    // Slovenian uses this as minus sign (a bug?): http://unicode.org/cldr/trac/ticket/10050
    private static final char EN_DASH = '\u2013';

    private static final int SIGN = 1;
    private static final int DECIMAL = 2;

    /**
     * The characters that are used in compatibility mode.
     *
     * @see #getAcceptedChars
     */
    private static final char[][] COMPATIBILITY_CHARACTERS = {
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'},
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '+'},
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.'},
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '+', '.'}
    };

    private char[] mAccepted;
    private final boolean mSign;
    private final boolean mDecimal;
    private final boolean mStringMode;

    private String mDecimalPointChars = DEFAULT_DECIMAL_POINT_CHARS;
    private String mSignChars = DEFAULT_SIGN_CHARS;

    // Takes a sign string and strips off its bidi controls, if any.
    @Nonnull
    private static String stripBidiControls(@Nonnull String sign) {
        // For the sake of simplicity, we operate on code units, since all bidi controls are
        // in the BMP. We also expect the string to be very short (almost always 1 character), so we
        // don't need to use StringBuilder.
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < sign.length(); i++) {
            final char c = sign.charAt(i);
            if (!UCharacter.hasBinaryProperty(c, UProperty.BIDI_CONTROL)) {
                result.append(c);
            }
        }
        return result.toString();
    }

    private DigitsInputFilter(@Nonnull final String accepted) {
        mSign = false;
        mDecimal = false;
        mStringMode = true;
        mAccepted = new char[accepted.length()];
        accepted.getChars(0, accepted.length(), mAccepted, 0);
    }

    private DigitsInputFilter(@Nullable Locale locale, boolean sign, boolean decimal) {
        mSign = sign;
        mDecimal = decimal;
        mStringMode = false;
        if (locale == null) {
            setToCompat();
            return;
        }
        final DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        final CharLinkedOpenHashSet chars = new CharLinkedOpenHashSet();
        final String[] digits = symbols.getDigitStrings();
        for (int i = 0; i < 10; i++) {
            if (digits[i].length() > 1) { // multi-codepoint digits. Not supported.
                setToCompat();
                return;
            }
            chars.add(digits[i].charAt(0));
        }
        if (sign || decimal) {
            if (sign) {
                final String minusString = stripBidiControls(symbols.getMinusSignString());
                final String plusString = stripBidiControls(symbols.getPlusSignString());
                if (minusString.length() > 1 || plusString.length() > 1) {
                    // non-BMP and multi-character signs are not supported.
                    setToCompat();
                    return;
                }
                final char minus = minusString.charAt(0);
                final char plus = plusString.charAt(0);
                chars.add(minus);
                chars.add(plus);
                mSignChars = "" + minus + plus;

                if (minus == MINUS_SIGN || minus == EN_DASH) {
                    // If the minus sign is U+2212 MINUS SIGN or U+2013 EN DASH, we also need to
                    // accept the ASCII hyphen-minus.
                    chars.add(HYPHEN_MINUS);
                    mSignChars += HYPHEN_MINUS;
                }
            }
            if (decimal) {
                final String separatorString = symbols.getDecimalSeparatorString();
                if (separatorString.length() > 1) {
                    // non-BMP and multi-character decimal separators are not supported.
                    setToCompat();
                    return;
                }
                final char separatorChar = separatorString.charAt(0);
                chars.add(separatorChar);
                mDecimalPointChars = String.valueOf(separatorChar);
            }
        }
        mAccepted = chars.toCharArray();
    }

    private void setToCompat() {
        mDecimalPointChars = DEFAULT_DECIMAL_POINT_CHARS;
        mSignChars = DEFAULT_SIGN_CHARS;
        final int kind = (mSign ? SIGN : 0) | (mDecimal ? DECIMAL : 0);
        mAccepted = COMPATIBILITY_CHARACTERS[kind];
    }

    @Nonnull
    @Override
    protected char[] getAcceptedChars() {
        return mAccepted;
    }

    private boolean isSignChar(final char c) {
        return mSignChars.indexOf(c) != -1;
    }

    private boolean isDecimalPointChar(final char c) {
        return mDecimalPointChars.indexOf(c) != -1;
    }

    /**
     * Returns a DigitsKeyListener that accepts the locale-appropriate digits.
     */
    @Nonnull
    public static DigitsInputFilter getInstance(@Nullable Locale locale) {
        return getInstance(locale, false, false);
    }

    private static final Object sLocaleCacheLock = new Object();
    @GuardedBy("sLocaleCacheLock")
    private static final HashMap<Locale, DigitsInputFilter[]> sLocaleInstanceCache = new HashMap<>();

    /**
     * Returns a DigitsKeyListener that accepts the locale-appropriate digits, plus the
     * locale-appropriate plus or minus sign (only at the beginning) and/or the locale-appropriate
     * decimal separator (only one per field) if specified.
     */
    @Nonnull
    public static DigitsInputFilter getInstance(@Nullable Locale locale, boolean sign, boolean decimal) {
        final int kind = (sign ? SIGN : 0) | (decimal ? DECIMAL : 0);
        synchronized (sLocaleCacheLock) {
            DigitsInputFilter[] cachedValue = sLocaleInstanceCache.get(locale);
            if (cachedValue != null && cachedValue[kind] != null) {
                return cachedValue[kind];
            }
            if (cachedValue == null) {
                cachedValue = new DigitsInputFilter[4];
                sLocaleInstanceCache.put(locale, cachedValue);
            }
            return cachedValue[kind] = new DigitsInputFilter(locale, sign, decimal);
        }
    }

    private static final Object sStringCacheLock = new Object();
    @GuardedBy("sStringCacheLock")
    private static final HashMap<String, DigitsInputFilter> sStringInstanceCache = new HashMap<>();

    /**
     * Returns a DigitsKeyListener that accepts only the characters
     * that appear in the specified String.  Note that not all characters
     * may be available on every keyboard.
     */
    @Nonnull
    public static DigitsInputFilter getInstance(@Nonnull String accepted) {
        DigitsInputFilter result;
        synchronized (sStringCacheLock) {
            result = sStringInstanceCache.get(accepted);
            if (result == null) {
                result = new DigitsInputFilter(accepted);
                sStringInstanceCache.put(accepted, result);
            }
        }
        return result;
    }

    /**
     * Returns a DigitsKeyListener based on the settings of an existing instance, with
     * the locale modified.
     *
     * @hide
     */
    @Nonnull
    public static DigitsInputFilter getInstance(@Nullable Locale locale, @Nonnull DigitsInputFilter listener) {
        if (listener.mStringMode) {
            return listener; // string-mode has no locale
        } else {
            return getInstance(locale, listener.mSign, listener.mDecimal);
        }
    }

    @Override
    public CharSequence filter(@Nonnull CharSequence source, int start, int end,
                               @Nonnull Spanned dest, int dstart, int dend) {
        CharSequence out = super.filter(source, start, end, dest, dstart, dend);

        if (!mSign && !mDecimal) {
            return out;
        }

        if (out != null) {
            source = out;
            start = 0;
            end = out.length();
        }

        int sign = -1;
        int decimal = -1;
        int dlen = dest.length();

        /*
         * Find out if the existing text has a sign or decimal point characters.
         */

        for (int i = 0; i < dstart; i++) {
            char c = dest.charAt(i);

            if (isSignChar(c)) {
                sign = i;
            } else if (isDecimalPointChar(c)) {
                decimal = i;
            }
        }
        for (int i = dend; i < dlen; i++) {
            char c = dest.charAt(i);

            if (isSignChar(c)) {
                return "";    // Nothing can be inserted in front of a sign character.
            } else if (isDecimalPointChar(c)) {
                decimal = i;
            }
        }

        /*
         * If it does, we must strip them out from the source.
         * In addition, a sign character must be the very first character,
         * and nothing can be inserted before an existing sign character.
         * Go in reverse order so the offsets are stable.
         */

        SpannableStringBuilder stripped = null;

        for (int i = end - 1; i >= start; i--) {
            char c = source.charAt(i);
            boolean strip = false;

            if (isSignChar(c)) {
                if (i != start || dstart != 0) {
                    strip = true;
                } else if (sign >= 0) {
                    strip = true;
                } else {
                    sign = i;
                }
            } else if (isDecimalPointChar(c)) {
                if (decimal >= 0) {
                    strip = true;
                } else {
                    decimal = i;
                }
            }

            if (strip) {
                if (end == start + 1) {
                    return "";  // Only one character, and it was stripped.
                }

                if (stripped == null) {
                    stripped = new SpannableStringBuilder(source, start, end);
                }

                stripped.delete(i - start, i + 1 - start);
            }
        }

        if (stripped != null) {
            return stripped;
        } else return out;
    }
}
