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

package icyllis.modernui.gui.stats;

import com.google.common.collect.Sets;
import icyllis.modernui.gui.scroll.ScrollWindow;
import icyllis.modernui.gui.scroll.UniformScrollGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.stats.Stats;

import java.util.Set;

/**
 * Item/Block to variable type values statistics tab, can be ordered
 */
public class ItemStatsGroup extends UniformScrollGroup<ItemStatsEntry> {

    public static int ENTRY_HEIGHT = 20;

    public ItemStatsGroup(ScrollWindow<?> window, Type type) {
        super(window, ENTRY_HEIGHT);

        Set<Item> set = Sets.newIdentityHashSet();
        // if someone breaks the vanilla rules, he should be fucked away
        if (type == Type.ITEMS) {
            Stats.ITEM_CRAFTED.forEach(e -> {
                if (!(e.getValue() instanceof BlockItem)) {
                    set.add(e.getValue());
                }
            });
            Stats.ITEM_USED.forEach(e -> {
                if (!(e.getValue() instanceof BlockItem)) {
                    set.add(e.getValue());
                }
            });
            Stats.ITEM_BROKEN.forEach(e -> {
                if (!(e.getValue() instanceof BlockItem)) {
                    set.add(e.getValue());
                }
            });
            Stats.ITEM_PICKED_UP.forEach(e -> {
                if (!(e.getValue() instanceof BlockItem)) {
                    set.add(e.getValue());
                }
            });
            Stats.ITEM_DROPPED.forEach(e -> {
                if (!(e.getValue() instanceof BlockItem)) {
                    set.add(e.getValue());
                }
            });
        } else {
            Stats.BLOCK_MINED.forEach(e -> set.add(e.getValue().asItem()));
            Stats.ITEM_CRAFTED.forEach(e -> {
                if (e.getValue() instanceof BlockItem) {
                    set.add(e.getValue());
                }
            });
            Stats.ITEM_USED.forEach(e -> {
                if (e.getValue() instanceof BlockItem) {
                    set.add(e.getValue());
                }
            });
            Stats.ITEM_PICKED_UP.forEach(e -> {
                if (e.getValue() instanceof BlockItem) {
                    set.add(e.getValue());
                }
            });
            Stats.ITEM_DROPPED.forEach(e -> {
                if (e.getValue() instanceof BlockItem) {
                    set.add(e.getValue());
                }
            });
        }

        set.remove(Items.AIR);
        set.forEach(e -> entries.add(new ItemStatsEntry(e)));

        // 20 for labels header
        height = 20 + entries.size() * entryHeight;
    }

    @Override
    public void layout(float x1, float x2, float y) {
        super.layout(x1, x2, y);
        x1 = centerX - 144;
        x2 = centerX + 144;
        y += 20;
        int i = 0;
        for (ItemStatsEntry entry : entries) {
            float cy = y + i * entryHeight;
            entry.layout(x1, x2, cy);
            i++;
        }
    }

    public void updateValues(StatisticsManager manager) {
        entries.forEach(e -> e.updateValue(manager));
    }

    @Override
    public void draw(float time) {
        super.draw(time);
    }

    public enum Type {
        BLOCKS,
        ITEMS
    }
}
