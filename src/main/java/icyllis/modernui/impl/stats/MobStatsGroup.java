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

package icyllis.modernui.impl.stats;

import com.google.common.collect.Sets;
import icyllis.modernui.gui.scroll.ScrollWindow;
import icyllis.modernui.gui.scroll.UniformScrollGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.stats.Stats;

import java.util.Set;

public class MobStatsGroup extends UniformScrollGroup<MobStatsEntry> {

    public static int ENTRY_HEIGHT = 36;

    public MobStatsGroup(ScrollWindow<?> window) {
        super(window, ENTRY_HEIGHT);

        Set<EntityType<?>> set = Sets.newIdentityHashSet();
        Stats.ENTITY_KILLED.forEach(e -> set.add(e.getValue()));
        Stats.ENTITY_KILLED_BY.forEach(e -> set.add(e.getValue()));

        set.forEach(e -> entries.add(new MobStatsEntry(window, e)));

        height = entries.size() * entryHeight;
    }

    @Override
    public void locate(float px, float py) {
        super.locate(px, py);
        int i = 0;
        for (MobStatsEntry entry : entries) {
            float cy = py + i * entryHeight;
            entry.locate(px, cy);
            i++;
        }
    }

    /*@Override
    public void onLayout(float left, float right, float y) {
        super.onLayout(left, right, y);
        left = centerX - 120;
        right = centerX + 120;
        int i = 0;
        for (MobStatsEntry entry : entries) {
            float cy = y + i * entryHeight;
            entry.onLayout(left, right, cy);
            i++;
        }
    }*/

    public void updateValues(StatisticsManager manager) {
        entries.forEach(e -> e.updateValue(manager));
    }
}
