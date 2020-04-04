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

import com.google.common.collect.Lists;
import icyllis.modernui.gui.layout.WidgetLayout;
import icyllis.modernui.gui.master.*;
import icyllis.modernui.gui.background.MenuSettingsBG;
import icyllis.modernui.gui.popup.PopupMenu;
import icyllis.modernui.gui.util.DelayedRunnable;
import icyllis.modernui.gui.widget.DropDownMenu;
import icyllis.modernui.gui.widget.LineTextButton;
import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class IngameMenuSetting extends ModuleGroup {

    private List<LineTextButton> buttons = new ArrayList<>();

    private WidgetLayout buttonLayout;

    public IngameMenuSetting() {
        addBackground(new MenuSettingsBG());
        buttonLayout = new WidgetLayout(buttons, WidgetLayout.Direction.HORIZONTAL_CENTER, 16);
        Consumer<LineTextButton> consumer = s -> {
            addWidget(s);
            buttons.add(s);
        };
        consumer.accept(new LineTextButton(I18n.format("gui.modernui.settings.tab.general"), 48f, () -> switchChildModule(1), i -> i == 1));
        consumer.accept(new LineTextButton(I18n.format("gui.modernui.settings.tab.video"), 48f, () -> switchChildModule(2), i -> i == 2));
        consumer.accept(new LineTextButton(I18n.format("gui.modernui.settings.tab.audio"), 48f, () -> switchChildModule(3), i -> i == 3));
        consumer.accept(new LineTextButton(I18n.format("gui.modernui.settings.tab.controls"), 48f, () -> switchChildModule(4), i -> i == 4));
        consumer.accept(new LineTextButton(I18n.format("gui.modernui.settings.tab.assets"), 48f, this::openAssetsMenu, i -> i >= 5 && i <= 8));
        addChildModule(1, SettingGeneral::new);
        addChildModule(2, SettingVideo::new);
        addChildModule(3, SettingAudio::new);
        addChildModule(4, SettingControls::new);
        addChildModule(5, SettingResourcePack::new);
        switchChildModule(1);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        buttonLayout.layout(width / 2f, 20);
    }

    private void openAssetsMenu() {
        List<String> tabs = Lists.newArrayList("Resource Packs", "Shaders", "Fonts", "Language");
        DropDownMenu list = new DropDownMenu(tabs, getCid() - 5, 12, this::assetsActions, DropDownMenu.Align.RIGHT);
        LineTextButton t = buttons.get(4);
        list.setPos(t.getRight() - 8, t.getBottom() + 1);
        GlobalModuleManager.INSTANCE.openPopup(new PopupMenu(list), false);
    }

    private void assetsActions(int index) {
        if (index >= 0 && index <= 3) {
            switchChildModule(5 + index);
        }
    }

    @Override
    public void moduleChanged(int id) {
        buttons.forEach(e -> e.onModuleChanged(id));
    }

}
