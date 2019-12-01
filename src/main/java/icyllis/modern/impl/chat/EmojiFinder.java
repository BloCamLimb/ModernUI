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

package icyllis.modern.impl.chat;

import icyllis.modern.system.ModernUI;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Help to search emoji code by name, notice that emoji codes are may mutable
 */
public final class EmojiFinder {

    private static final Object2IntArrayMap<String> MAP = new Object2IntArrayMap<>();
    private static final List<Integer> HISTORY = new ArrayList<>();
    static {
        //MAP.put("horse", 0x000e);
        R:
        for(int i = 0; i < 20; i++) {
            for(int j = 0; j < 22; j++) {
                if(i == 19 && j > 12) {
                    break R;
                }
                MAP.put("s"+ (i * 22 + j), i + (j << 8));
            }
        }
    }

    /**
     * Find emoji code by given keyword
     * @param keyword keyword
     * @return emoji code collection
     */
    public static List<Integer> findEmoji(String keyword) {
        return MAP.object2IntEntrySet().stream().filter(e -> e.getKey().contains(keyword)).map(Object2IntMap.Entry::getIntValue).collect(Collectors.toList());
    }

    public static void addToHistory(int emoji) {
        HISTORY.removeIf(e -> e.equals(emoji));
        HISTORY.add(0, emoji);
        if(HISTORY.size() > 15) {
            HISTORY.remove(15);
        }
    }

    public static List<Integer> getHistory() {
        return HISTORY;
    }
}
