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

package icyllis.modernui.core;

import icyllis.modernui.annotation.NonNull;

import java.util.*;

/**
 * LocaleList is an immutable list of {@link Locale Locales}, typically used to
 * keep an ordered list of user preferences for locales.
 */
public final class LocaleList {

    private static final Locale[] EMPTY_LOCALES = {};
    private static final LocaleList EMPTY_LOCALE_LIST = new LocaleList();

    private final Locale[] mLocales;
    private final String mStringRepresentation;

    /**
     * Creates a new {@link LocaleList}.
     * <p>
     * If two or more same locales are passed, the repeated locales will be dropped.
     * <p>For empty lists of {@link Locale} items it is better to use {@link #getEmptyLocaleList()},
     * which returns a pre-constructed empty list.</p>
     *
     * @throws NullPointerException if any of the input locales is <code>null</code>.
     */
    public LocaleList(@NonNull Locale... list) {
        if (list.length == 0) {
            mLocales = EMPTY_LOCALES;
            mStringRepresentation = "";
        } else {
            final var ll = new ArrayList<Locale>();
            final var dedup = new HashSet<Locale>();
            final var sb = new StringBuilder();
            for (int i = 0; i < list.length; i++) {
                final Locale l = list[i];
                if (l == null) {
                    throw new NullPointerException("list[" + i + "] is null");
                } else if (dedup.add(l)) {
                    final var clone = (Locale) l.clone();
                    ll.add(clone);
                    sb.append(clone.toLanguageTag());
                    if (i < list.length - 1) {
                        sb.append(',');
                    }
                }
            }
            mLocales = ll.toArray(new Locale[0]);
            mStringRepresentation = sb.toString();
        }
    }
}
