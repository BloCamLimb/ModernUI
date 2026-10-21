/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.system;

import icyllis.modernui.annotation.Immutable;
import icyllis.modernui.annotation.IntRange;
import icyllis.modernui.annotation.Size;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;

/**
 * LocaleList is an immutable list of {@link Locale Locales}, typically used to
 * keep an ordered list of user preferences for locales.
 */
@Immutable
public final class LocaleList implements Parcelable, Iterable<Locale> {

    private static final Locale[] sEmptyList = {};
    private static final LocaleList sEmptyLocaleList = new LocaleList();

    /**
     * Whether to synchronize {@link #getDefault()} with {@link Locale#getDefault()}.
     * If you want to avoid altering the global Locale in an embedded environment,
     * you may set this to false. But this field must be set as early as possible;
     * real-time modifications is undefined.
     * <p>
     * The default is true.
     *
     * @hidden
     */
    @ApiStatus.Internal
    public static boolean sSyncJavaLocale = true;

    private final @NonNull Locale @NonNull[] mList;
    private final @NonNull String mStringRepresentation;

    /**
     * Creates a new {@link LocaleList}.
     * <p>
     * If two or more same locales are passed, the repeated locales will be dropped.
     * <p>For empty lists of {@link Locale} items it is better to use {@link #getEmptyLocaleList()},
     * which returns a pre-constructed empty list.</p>
     *
     * @throws NullPointerException if any of the input locales is <code>null</code>.
     */
    public LocaleList(@NonNull Locale @NonNull... list) {
        if (list.length == 0) {
            mList = sEmptyList;
            mStringRepresentation = "";
            return;
        }
        Locale[] locales = new Locale[list.length];
        int size = 0;
        HashSet<Locale> seenLocales = new HashSet<>(
                (int) Math.ceil(list.length / 0.75), 0.75f
        );
        int stringRepresentationLen = 0;
        for (final Locale l : list) {
            if (seenLocales.add(l)) {
                locales[size++] = l;
                stringRepresentationLen += l.toLanguageTag().length();
            }
        }
        assert size > 0;

        stringRepresentationLen += size - 1;

        final StringBuilder sb = new StringBuilder(stringRepresentationLen);
        for (int i = 0; i < size; i++) {
            sb.append(locales[i].toLanguageTag());
            if (i < size - 1) {
                sb.append(',');
            }
        }
        mList = size == locales.length
                ? locales
                : Arrays.copyOf(locales, size);
        mStringRepresentation = sb.toString();
    }

    @SuppressWarnings("unused")
    public LocaleList(@NonNull Parcel in) {
        this(in.readString8());
    }

    private LocaleList(@Nullable String list) {
        if (list == null || list.isEmpty()) {
            mList = sEmptyList;
            mStringRepresentation = "";
            return;
        }
        final String[] tags = list.split(",");
        if (tags.length == 0) {
            mList = sEmptyList;
            mStringRepresentation = "";
            return;
        }
        Locale[] localeList = new Locale[tags.length];
        int size = 0;
        HashSet<Locale> seenLocales = new HashSet<>(
                (int) Math.ceil(tags.length / 0.75), 0.75f
        );
        int stringRepresentationLen = 0;
        for (int i = 0; i < tags.length; i++) {
            final Locale l = Locale.forLanguageTag(tags[i]);
            if (l == null) {
                throw new NullPointerException("list[" + i + "] is null");
            }
            if (seenLocales.add(l)) {
                localeList[size++] = l;
                stringRepresentationLen += l.toLanguageTag().length();
            }
        }
        assert size > 0;

        stringRepresentationLen += size - 1;

        final StringBuilder sb = new StringBuilder(stringRepresentationLen);
        for (int i = 0; i < size; i++) {
            sb.append(localeList[i].toLanguageTag());
            if (i < size - 1) {
                sb.append(',');
            }
        }
        mList = size == localeList.length
                ? localeList
                : Arrays.copyOf(localeList, size);
        mStringRepresentation = sb.toString();
    }

    public LocaleList(@NonNull Locale topLocale, @Nullable LocaleList otherLocales) {
        final int inputLength = (otherLocales == null) ? 0 : otherLocales.mList.length;
        int topLocaleIndex = -1;
        for (int i = 0; i < inputLength; i++) {
            if (topLocale.equals(otherLocales.mList[i])) {
                topLocaleIndex = i;
                break;
            }
        }

        final int outputLength = inputLength + (topLocaleIndex == -1 ? 1 : 0);
        final Locale[] localeList = new Locale[outputLength];
        localeList[0] = topLocale;
        if (topLocaleIndex == -1) {
            // topLocale was not in otherLocales
            if (inputLength > 0)
                System.arraycopy(otherLocales.mList, 0, localeList, 1, inputLength);
        } else {
            System.arraycopy(otherLocales.mList, 0, localeList, 1, topLocaleIndex);
            if (inputLength - (topLocaleIndex + 1) >= 0)
                System.arraycopy(otherLocales.mList, topLocaleIndex + 1,
                        localeList, topLocaleIndex + 1, inputLength - (topLocaleIndex + 1));
        }

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < outputLength; i++) {
            sb.append(localeList[i].toLanguageTag());
            if (i < outputLength - 1) {
                sb.append(',');
            }
        }

        mList = localeList;
        mStringRepresentation = sb.toString();
    }

    /**
     * Retrieves the {@link Locale} at the specified index.
     *
     * @param index The position to retrieve.
     * @return The {@link Locale} in the given index.
     * @throws IndexOutOfBoundsException index is out of [0..size-1]
     */
    public @NonNull Locale get(int index) {
        return mList[index];
    }

    /**
     * Retrieves the first {@link Locale} in this list.
     * If this list is empty, returns null; otherwise returns the item
     * at index 0.
     *
     * @return The first Locale or null if empty.
     */
    @Contract(pure = true)
    public @Nullable Locale top() {
        final Locale[] list = mList;
        return list.length == 0 ? null : list[0];
    }

    /**
     * Returns whether the {@link LocaleList} contains no {@link Locale} items.
     *
     * @return {@code true} if this {@link LocaleList} has no {@link Locale} items, {@code false}
     * otherwise.
     */
    public boolean isEmpty() {
        return mList.length == 0;
    }

    /**
     * Returns the number of {@link Locale} items in this {@link LocaleList}.
     */
    @IntRange(from = 0)
    public int size() {
        return mList.length;
    }

    /**
     * Searches this {@link LocaleList} for the specified {@link Locale} and returns the index of
     * the first occurrence.
     *
     * @param locale The {@link Locale} to search for.
     * @return The index of the first occurrence of the {@link Locale} or {@code -1} if the item
     * wasn't found.
     */
    @IntRange(from = -1)
    public int indexOf(Locale locale) {
        final Locale[] list = mList;
        for (int i = 0; i < list.length; i++) {
            if (list[i].equals(locale)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public @NonNull Iterator<Locale> iterator() {
        return new Iterator<>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < mList.length;
            }

            @Override
            public Locale next() {
                return mList[i++];
            }
        };
    }

    @Override
    public void forEach(Consumer<? super Locale> action) {
        for (Locale locale : mList) {
            action.accept(locale);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (!(other instanceof LocaleList))
            return false;
        final Locale[] list = mList;
        final Locale[] otherList = ((LocaleList) other).mList;
        if (list.length != otherList.length)
            return false;
        for (int i = 0; i < list.length; i++) {
            if (!list[i].equals(otherList[i]))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (Locale locale : mList) {
            result = 31 * result + locale.hashCode();
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        final Locale[] list = mList;
        for (int i = 0; i < list.length; i++) {
            sb.append(list[i]);
            if (i < list.length - 1) {
                sb.append(',');
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString8(mStringRepresentation);
    }

    /**
     * Retrieves a String representation of the language tags in this list.
     */
    @NonNull
    public String toLanguageTags() {
        return mStringRepresentation;
    }

    /**
     * Retrieve an empty instance of {@link LocaleList}.
     */
    @NonNull
    public static LocaleList getEmptyLocaleList() {
        return sEmptyLocaleList;
    }

    /**
     * Generates a new LocaleList with the given language tags.
     *
     * @param list The language tags to be included as a single {@link String} separated by commas.
     * @return A new instance with the {@link Locale} items identified by the given tags.
     */
    @NonNull
    public static LocaleList forLanguageTags(@Nullable String list) {
        if (list == null || list.isEmpty()) {
            return getEmptyLocaleList();
        } else {
            return new LocaleList(list);
        }
    }

    // ModernUI changed: use StampedLock for better performance
    private static final StampedLock sLock = new StampedLock();

    private static LocaleList sLastExplicitlySetLocaleList = null;
    private static LocaleList sDefaultLocaleList = null;
    private static Locale sLastDefaultLocale = null;

    @NonNull
    @Size(min = 1)
    public static LocaleList getDefault() {
        long stamp = sLock.tryOptimisticRead();
        LocaleList defaultLocaleList = sDefaultLocaleList;
        Locale lastDefaultLocale = sLastDefaultLocale;
        if (!sLock.validate(stamp)) {
            stamp = sLock.readLock();
            defaultLocaleList = sDefaultLocaleList;
            lastDefaultLocale = sLastDefaultLocale;
        }
        try {
            if (sSyncJavaLocale) {
                Locale defaultLocale = Locale.getDefault();
                while (!defaultLocale.equals(lastDefaultLocale)) {
                    long ws = sLock.tryConvertToWriteLock(stamp);
                    if (ws != 0) {
                        stamp = ws;

                        defaultLocale = Locale.getDefault();
                        sLastDefaultLocale = defaultLocale;
                        if (sDefaultLocaleList != null
                                && defaultLocale.equals(sDefaultLocaleList.get(0))) {
                            return sDefaultLocaleList;
                        }
                        sDefaultLocaleList = new LocaleList(defaultLocale, sLastExplicitlySetLocaleList);

                        return sDefaultLocaleList;
                    }

                    if (StampedLock.isLockStamp(stamp)) {
                        sLock.unlock(stamp);
                    }
                    stamp = sLock.writeLock();
                    defaultLocaleList = sDefaultLocaleList;
                    lastDefaultLocale = sLastDefaultLocale;
                }
            } else {
                while (defaultLocaleList == null) {
                    long ws = sLock.tryConvertToWriteLock(stamp);
                    if (ws != 0) {
                        stamp = ws;

                        Locale defaultLocale = Locale.getDefault();
                        sDefaultLocaleList = new LocaleList(defaultLocale, null);

                        return sDefaultLocaleList;
                    }

                    if (StampedLock.isLockStamp(stamp)) {
                        sLock.unlock(stamp);
                    }
                    stamp = sLock.writeLock();
                    defaultLocaleList = sDefaultLocaleList;
                }
            }
        } finally {
            if (StampedLock.isLockStamp(stamp)) {
                sLock.unlock(stamp);
            }
        }

        return defaultLocaleList;
    }

    /**
     * Changes the default locale list. If {@link #sSyncJavaLocale} is true,
     * also sets the default locale by calling Locale.setDefault() with the
     * first locale in the list.
     *
     * @throws NullPointerException     if the input is <code>null</code>.
     * @throws IllegalArgumentException if the input is empty.
     */
    public static void setDefault(@NonNull @Size(min = 1) LocaleList locales) {
        Objects.requireNonNull(locales);
        if (locales.isEmpty()) {
            throw new IllegalArgumentException("locales is empty");
        }
        long ws = sLock.writeLock();
        try {
            if (sSyncJavaLocale) {
                sLastDefaultLocale = locales.get(0);
                Locale.setDefault(sLastDefaultLocale);
            }
            sLastExplicitlySetLocaleList = locales;
            sDefaultLocaleList = locales;
        } finally {
            sLock.unlock(ws);
        }
    }
}
