/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.impl.module;

import com.google.common.collect.Lists;
import icyllis.modernui.ui.discard.Module;
import icyllis.modernui.ui.discard.ScrollWindow;
import icyllis.modernui.impl.stats.GeneralStatsGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.stats.StatisticsManager;

import java.util.Objects;

public class StatsGeneral extends Module {

    private final StatisticsManager manager;

    private final GeneralStatsGroup group;

    private int updateCycle = 2;

    public StatsGeneral() {
        Minecraft minecraft = Minecraft.getInstance();
        manager = Objects.requireNonNull(minecraft.player).getStats();

        ScrollWindow<GeneralStatsGroup> window = new ScrollWindow<>(this, w -> 40f, h -> 36f, w -> w - 80f, h -> h - 72f);

        group = new GeneralStatsGroup(window);
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
