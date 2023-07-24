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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.text.*;

import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * The Typeface specifies a set of font families that can be used
 * in Paint. This determines how text appears when drawn and measured.
 */
@Immutable
public class Typeface extends FontCollection {

    public static final Typeface SANS_SERIF;
    public static final Typeface SERIF;
    public static final Typeface MONOSPACED;

    private static final Map<String, Typeface> sSystemFontMap;

    static {
        Map<String, Typeface> map = new HashMap<>();
        for (var entry : FontFamily.getSystemFontMap().entrySet()) {
            map.putIfAbsent(entry.getKey(), createTypeface(entry.getValue()));
        }
        SANS_SERIF = Objects.requireNonNull(map.get(java.awt.Font.SANS_SERIF));
        SERIF = Objects.requireNonNull(map.get(java.awt.Font.SERIF));
        MONOSPACED = Objects.requireNonNull(map.get(java.awt.Font.MONOSPACED));
        sSystemFontMap = map;
    }

    /**
     * Font style constant to request the plain/regular/normal style
     */
    public static final int NORMAL      = Paint.NORMAL;
    /**
     * Font style constant to request the bold style
     */
    public static final int BOLD        = Paint.BOLD;
    /**
     * Font style constant to request the italic style
     */
    public static final int ITALIC      = Paint.ITALIC;
    /**
     * Font style constant to request the bold and italic style
     */
    public static final int BOLD_ITALIC = Paint.BOLD_ITALIC;

    @NonNull
    public static Typeface createTypeface(@NonNull FontFamily... families) {
        if (families.length == 0) {
            return SANS_SERIF;
        }
        return new Typeface(families);
    }

    @NonNull
    public static Typeface getSystemFont(@NonNull String familyName) {
        Typeface typeface = sSystemFontMap.get(familyName);
        return typeface == null ? SANS_SERIF : typeface;
    }

    private Typeface(@NonNull FontFamily... families) {
        super(families);
    }
}
