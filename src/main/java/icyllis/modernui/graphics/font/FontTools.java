/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package icyllis.modernui.graphics.font;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class FontTools {

    private static final TrueTypeRenderer FONT_RENDERER;

    static {
        FONT_RENDERER = TrueTypeRenderer.INSTANCE;
    }

    public static float getStringWidth(String string) {
        return FONT_RENDERER.getStringWidth(string);
    }

    public static String trimStringToWidth(String str, float width, boolean reverse) {
        return FONT_RENDERER.trimStringToWidth(str, width, reverse);
    }

    @Nonnull
    public static String[] splitStringToWidth(@Nonnull String string, float width) {
        List<String> list = new ArrayList<>();
        String str;
        int currentIndex = 0;
        int size;
        do {
            str = string.substring(currentIndex);
            size = FONT_RENDERER.sizeStringToWidth(str, width);
            list.add(str.substring(0, size));
            currentIndex += size;
        } while (currentIndex < string.length());
        return list.toArray(new String[0]);
    }
}
