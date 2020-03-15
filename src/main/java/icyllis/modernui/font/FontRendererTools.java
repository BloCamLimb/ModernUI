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

import java.util.ArrayList;
import java.util.List;

public class FontRendererTools {

    public static IFontRenderer CURRENT_RENDERER;

    static {
        CURRENT_RENDERER = TrueTypeRenderer.INSTANCE;
    }

    public static void switchRenderer(boolean ttf) {
        if (ttf) {
            CURRENT_RENDERER = TrueTypeRenderer.INSTANCE;
        } else {
            CURRENT_RENDERER = VanillaFontRenderer.INSTANCE;
        }
    }

    public static String[] splitStringToWidth(String string, float width) {
        List<String> list = new ArrayList<>();
        int currentIndex = 0;
        int size;
        do {
            size = CURRENT_RENDERER.sizeStringToWidth(string.substring(currentIndex), width);
            list.add(string.substring(currentIndex, currentIndex + size));
            currentIndex += size;
        } while (currentIndex < string.length());
        return list.toArray(new String[0]);
    }
}
