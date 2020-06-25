/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
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

import icyllis.modernui.graphics.font.TextAlign;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.ui.test.ScrollWindow;
import icyllis.modernui.ui.widget.UniformScrollEntry;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityType;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.stats.Stats;
import net.minecraft.util.Util;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;

public class MobStatsEntry extends UniformScrollEntry {

    private final EntityType<?> entity;

    private final String entityName;

    private String kill = "";

    private String death = "";

    public MobStatsEntry(ScrollWindow<?> window, EntityType<?> entity) {
        super(window, 240, MobStatsGroup.ENTRY_HEIGHT);
        this.entity = entity;
        entityName = I18n.format(Util.makeTranslationKey("entity", EntityType.getKey(entity)));
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        canvas.setTextAlign(TextAlign.LEFT);
        canvas.resetColor();
        canvas.drawText(entityName, x1 + 2, y1 + 1);
        canvas.drawText(kill, x1 + 12, y1 + 10);
        canvas.drawText(death, x1 + 12, y1 + 19);
    }

    public void updateValue(@Nonnull StatisticsManager manager) {
        int kills = manager.getValue(Stats.ENTITY_KILLED.get(entity));
        int deaths = manager.getValue(Stats.ENTITY_KILLED_BY.get(entity));
        String key = Stats.ENTITY_KILLED.getTranslationKey();
        if (kills == 0) {
            kill = TextFormatting.DARK_GRAY + I18n.format(key + ".none", entityName);
        } else {
            kill = TextFormatting.GRAY + I18n.format(key, kills, entityName);
        }
        key = Stats.ENTITY_KILLED_BY.getTranslationKey();
        if (deaths == 0) {
            death = TextFormatting.DARK_GRAY + I18n.format(key + ".none", entityName);
        } else {
            death = TextFormatting.GRAY + I18n.format(key, deaths, entityName);
        }
    }

    @Override
    public void onMouseHoverEnter(double mouseX, double mouseY) {

    }

    @Override
    public void onMouseHoverExit() {

    }
}
