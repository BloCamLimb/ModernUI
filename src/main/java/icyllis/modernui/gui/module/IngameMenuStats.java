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

package icyllis.modernui.gui.module;

import icyllis.modernui.gui.background.MenuSettingsBG;
import icyllis.modernui.gui.layout.WidgetLayout;
import icyllis.modernui.gui.master.ModuleGroup;
import icyllis.modernui.gui.widget.LineTextButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.resources.I18n;
import net.minecraft.network.play.client.CClientStatusPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class IngameMenuStats extends ModuleGroup {

    private List<LineTextButton> buttons = new ArrayList<>();

    private WidgetLayout buttonLayout;

    private final ClientPlayNetHandler netHandler;

    public IngameMenuStats() {
        netHandler = Objects.requireNonNull(Minecraft.getInstance().getConnection());
        netHandler.sendPacket(new CClientStatusPacket(CClientStatusPacket.State.REQUEST_STATS));

        addBackground(new MenuSettingsBG());

        buttonLayout = new WidgetLayout(buttons, WidgetLayout.Direction.HORIZONTAL_CENTER, 16);

        Consumer<LineTextButton> consumer = s -> {
            addWidget(s);
            buttons.add(s);
        };
        consumer.accept(new LineTextButton(I18n.format("stat.generalButton"), 48f,
                () -> switchChildModule(1), i -> i == 1));
        consumer.accept(new LineTextButton(I18n.format("stat.blocksButton"), 48f,
                () -> switchChildModule(2), i -> i == 2));
        consumer.accept(new LineTextButton(I18n.format("stat.itemsButton"), 48f,
                () -> switchChildModule(3), i -> i == 3));
        consumer.accept(new LineTextButton(I18n.format("stat.mobsButton"), 48f,
                () -> {}, i -> i == 4));

        int i = 0;
        addChildModule(++i, StatsGeneral::new);
        addChildModule(++i, StatsBlocks::new);
        addChildModule(++i, StatsItems::new);

        switchChildModule(1);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        buttonLayout.layout(width / 2f, 20);
    }

    @Override
    public void tick(int ticks) {
        super.tick(ticks);
        if ((ticks & 127) == 0) {
            netHandler.sendPacket(new CClientStatusPacket(CClientStatusPacket.State.REQUEST_STATS));
        }
    }

    @Override
    public void moduleChanged(int id) {
        super.moduleChanged(id);
        buttons.forEach(e -> e.onModuleChanged(id));
    }
}
