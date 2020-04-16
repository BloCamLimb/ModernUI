/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.font;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class FontTools {

    private static IFontRenderer FONT_RENDERER;

    static {
        FONT_RENDERER = TrueTypeRenderer.INSTANCE;
    }

    // vanilla renderer is not supported (have bugs in modern ui screens)
    @Deprecated
    public static void switchRenderer(boolean mui) {
        if (mui) {
            FONT_RENDERER = TrueTypeRenderer.INSTANCE;
        } else {
            FONT_RENDERER = VanillaFontRenderer.INSTANCE;
        }
        throw new RuntimeException();
    }

    public static float getStringWidth(String string) {
        return FONT_RENDERER.getStringWidth(string);
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
