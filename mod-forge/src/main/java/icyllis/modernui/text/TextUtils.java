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

import icyllis.modernui.resources.LocaleChangeListener;

import java.util.Locale;

public final class TextUtils implements LocaleChangeListener {

    @Override
    public void onLocaleChanged(Locale locale) {
        GraphemeBreak.setLocale(locale);
    }

    public static void getChars(CharSequence s, int start, int end,
                                char[] dest, int destoff) {
        if (s instanceof String)
            ((String) s).getChars(start, end, dest, destoff);
        else if (s instanceof StringBuffer)
            ((StringBuffer) s).getChars(start, end, dest, destoff);
        else if (s instanceof StringBuilder)
            ((StringBuilder) s).getChars(start, end, dest, destoff);
        else if (s instanceof GetChars)
            ((GetChars) s).getChars(start, end, dest, destoff);
        else
            for (int i = start; i < end; i++)
                dest[destoff++] = s.charAt(i);
    }
}
