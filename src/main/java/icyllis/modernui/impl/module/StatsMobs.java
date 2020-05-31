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

package icyllis.modernui.impl.module;

import com.google.common.collect.Lists;
import icyllis.modernui.ui.test.Module;
import icyllis.modernui.ui.scroll.ScrollWindow;
import icyllis.modernui.impl.stats.MobStatsGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.stats.StatisticsManager;

import java.util.Objects;

public class StatsMobs extends Module {

    private final StatisticsManager manager;

    private final MobStatsGroup group;

    private int updateCycle = 2;

    public StatsMobs() {
        Minecraft minecraft = Minecraft.getInstance();
        manager = Objects.requireNonNull(minecraft.player).getStats();

        ScrollWindow<MobStatsGroup> window = new ScrollWindow<>(this, w -> 40f, h -> 36f, w -> w - 80f, h -> h - 72f);

        group = new MobStatsGroup(window);
        group.updateValues(manager);

        window.addGroups(Lists.newArrayList(group));

        addWidget(window);
    }

    @Override
    public void tick(int ticks) {
        super.tick(ticks);
        if (ticks % updateCycle == 0) {
            group.updateValues(manager);
            updateCycle = Math.min(updateCycle + 1, 40);
        }
    }
}
